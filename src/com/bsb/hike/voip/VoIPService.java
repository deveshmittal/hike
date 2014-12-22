package com.bsb.hike.voip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.VoIPActivity;
import com.bsb.hike.voip.VoIPClient.ConnectionMethods;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPEncryptor.EncryptionStage;
import com.bsb.hike.voip.protobuf.VoIPSerializer;

public class VoIPService extends Service {
	
	private final IBinder myBinder = new LocalBinder();
	private final int AUDIO_SAMPLE_RATE = 48000; 
	private final int PACKET_TRACKING_SIZE = 128;
	private final int HEARTBEAT_INTERVAL = 1000;
	private final int HEARTBEAT_TIMEOUT = HEARTBEAT_INTERVAL * 5;
	private final int MAX_SAMPLES_BUFFER = 3;

	private int localBitrate = 12000;
	private boolean cryptoEnabled = true;
	private Messenger mMessenger;
	private boolean connectionEstablished = false;
	private int currentPacketNumber = 0;
	private int previousHighestRemotePacketNumber = 0;
	private boolean keepRunning = true;
	private DatagramSocket socket = null;
	private VoIPClient clientPartner = null, clientSelf = null;
	private BitSet packetTrackingBits = new BitSet(PACKET_TRACKING_SIZE);
	private Date lastHeartbeat;
	private int totalBytesSent = 0, totalBytesReceived = 0, rawVoiceSent = 0;
	private VoIPEncryptor encryptor = new VoIPEncryptor();
	private VoIPEncryptor.EncryptionStage encryptionStage = EncryptionStage.STAGE_INITIAL;
	private boolean mute = false;
	private boolean audioStarted = false;
	private int droppedDecodedPackets = 0;
	private int minBufSizePlayback;
	private int gain = 0;
	private boolean answerOnConnectionEstablished = false;
	private OpusWrapper opusWrapper;
	private Thread partnerTimeoutThread = null, waitingBeepThread = null;
	private AudioTrack audioTrack;
	private ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
	private int callId = 0;
	private int totalPacketsSent;

	private final ConcurrentLinkedQueue<VoIPDataPacket> samplesToDecodeQueue     = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentLinkedQueue<VoIPDataPacket> samplesToEncodeQueue     = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentLinkedQueue<VoIPDataPacket> encodedBuffersQueue      = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentLinkedQueue<VoIPDataPacket> decodedBuffersQueue      = new ConcurrentLinkedQueue<VoIPDataPacket>();
	private final ConcurrentHashMap<Integer, VoIPDataPacket> ackWaitQueue		 = new ConcurrentHashMap<Integer, VoIPDataPacket>();
	
	// Packet prefixes
	private static final byte PP_RAW_VOICE_PACKET = 0x01;
	private static final byte PP_ENCRYPTED_VOICE_PACKET = 0x02;
	private static final byte PP_PROTOCOL_BUFFER = 0x03;
	
	@Override
	public IBinder onBind(Intent intent) {
		return myBinder;
	}

	public class LocalBinder extends Binder {
		public VoIPService getService() {
			return VoIPService.this;
		}
	}
	
	public void setClientPartner(VoIPClient clientPartner) {
		this.clientPartner = clientPartner;
	}
	
	@SuppressLint("InlinedApi") @Override
	public void onCreate() {
		super.onCreate();
		
		AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			String rate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
			String size = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
			Log.d(VoIPConstants.TAG, "Device frames/buffer:" + size + ", sample rate: " + rate);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stop();
		Log.d(VoIPConstants.TAG, "VoIP Service destroyed.");
	}

	public void setClientSelf(VoIPClient clientSelf) {
		this.clientSelf = clientSelf;
		if (socket == null)
			try {
				socket = new DatagramSocket(clientSelf.getInternalPort());
			} catch (SocketException e) {
				Log.d(VoIPConstants.TAG, "VoIPService SocketException: " + e.toString());
			}
	}
	
	public void setMessenger(Messenger messenger) {
		this.mMessenger = messenger;
	}
	
	public void setCallid(int callId) {
		this.callId = callId;
	}
	
	public int getCallId() {
		return callId;
	}
	
	public void startStreaming() throws Exception {
		encryptionStage = EncryptionStage.STAGE_INITIAL;
		
		if (clientPartner == null || clientSelf == null) {
			throw new Exception("Clients (partner and/or self) not set.");
		}
		
		startCodec(); 
		startSendingAndReceiving();
		startHeartbeat();
		exchangeCryptoInfo();
		
		Log.d(VoIPConstants.TAG, "Streaming started.");
	}
	
	public void stop() {
		if (keepRunning == false) {
			Log.w(VoIPConstants.TAG, "Trying to stop a stopped service?");
			return;
		}
		
		keepRunning = false;
		sendHandlerMessage(VoIPActivity.MSG_SHUTDOWN_ACTIVITY);
		Log.d(VoIPConstants.TAG, "Bytes sent: " + totalBytesSent + 
				"\nVoice sent: " + rawVoiceSent + "\nBytes received: " + totalBytesReceived +
				"\nDropped decoded packets: " + droppedDecodedPackets +
				"\nPackets sent: " + totalPacketsSent);
		
		if (waitingBeepThread != null)
			waitingBeepThread.interrupt();

		// Hangup tone
		tg.startTone(ToneGenerator.TONE_CDMA_PIP);
	}
	
	public void hangUp() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.END_CALL);
				sendPacket(dp, true);
				stop();
			}
		}).start();
	}
	
	public void rejectIncomingCall() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.CALL_DECLINED);
				sendPacket(dp, true);
				stop();
			}
		}).start();
	}
	
	public void setMute(boolean mute) {
		this.mute = mute;
	}
	
	private void sendHandlerMessage(int message) {
		Message msg = Message.obtain();
		msg.what = message;
		try {
			mMessenger.send(msg);
		} catch (RemoteException e) {
			Log.e(VoIPConstants.TAG, "Messenger RemoteException: " + e.toString());
		}
	}

	private void startHeartbeat() {
	
		// Sending heart beat
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.HEARTBEAT);
				while (keepRunning == true) {
					sendPacket(dp, false);
					try {
						Thread.sleep(HEARTBEAT_INTERVAL);
					} catch (InterruptedException e) {
						Log.d(VoIPConstants.TAG, "Heartbeat InterruptedException: " + e.toString());
					}
				}
			}
		}).start();
		
		// Listening for heartbeat, and housekeeping
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				lastHeartbeat = new Date();
				while (keepRunning == true) {
					Date currentDate = new Date();
					if (currentDate.getTime() - lastHeartbeat.getTime() > HEARTBEAT_TIMEOUT) {
						Log.d(VoIPConstants.TAG, "Heartbeat failure.");
						hangUp();
						break;
					}
					
					sendPacketsWaitingForAck();
					
					// Drop packets if getting left behind
					while (samplesToDecodeQueue.size() > MAX_SAMPLES_BUFFER) {
						Log.d(VoIPConstants.TAG, "Dropping to_decode packet.");
						samplesToDecodeQueue.poll();
					}
					
					while (samplesToEncodeQueue.size() > MAX_SAMPLES_BUFFER) {
						Log.d(VoIPConstants.TAG, "Dropping to_encode packet.");
						samplesToEncodeQueue.poll();
					}
					
					while (decodedBuffersQueue.size() > MAX_SAMPLES_BUFFER + 1) {
						Log.d(VoIPConstants.TAG, "Dropping decoded packet.");
						droppedDecodedPackets++;
						decodedBuffersQueue.poll();
					}
					
					while (encodedBuffersQueue.size() > MAX_SAMPLES_BUFFER) {
						Log.d(VoIPConstants.TAG, "Dropping encoded packet.");
						encodedBuffersQueue.poll();
					}

					try {
						Thread.sleep(HEARTBEAT_INTERVAL);
					} catch (InterruptedException e) {
						Log.d(VoIPConstants.TAG, "Heartbeat InterruptedException: " + e.toString());
					}
					
				}
			}
		}).start();
		
	}
	
	private void startCodec() {
		try {
			opusWrapper = new OpusWrapper();
			opusWrapper.getDecoder(AUDIO_SAMPLE_RATE, 1);
			opusWrapper.getEncoder(AUDIO_SAMPLE_RATE, 1, localBitrate);
		} catch (Exception e) {
			Log.e(VoIPConstants.TAG, "Codec exception: " + e.toString());
		}
		
		startCodecDecompression();
		startCodecCompression();
		
		// Set audio gain
		SharedPreferences preferences = getSharedPreferences(HikeMessengerApp.VOIP_SETTINGS, Context.MODE_PRIVATE);
		gain = preferences.getInt(HikeMessengerApp.VOIP_AUDIO_GAIN, 0);
		opusWrapper.setDecoderGain(gain);		
	}
	
	private void startCodecDecompression() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				int lastPacketReceived = 0;
				int uncompressedLength = 0;
				while (keepRunning == true) {
					VoIPDataPacket dpdecode = samplesToDecodeQueue.peek();
					if (dpdecode != null) {
						samplesToDecodeQueue.poll();
						byte[] uncompressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 10];	// Just to be safe, we make a big buffer
						
						if (dpdecode.getVoicePacketNumber() > 0 && dpdecode.getVoicePacketNumber() <= lastPacketReceived)
							continue;	// We received an old packet again
						
						// Handle packet loss (unused as on Dec 16, 2014)
						if (dpdecode.getVoicePacketNumber() > lastPacketReceived + 1) {
							Log.d(VoIPConstants.TAG, "Packet loss! (" + (dpdecode.getVoicePacketNumber() - lastPacketReceived) + ")");
							lastPacketReceived = dpdecode.getVoicePacketNumber();
							try {
								uncompressedLength = opusWrapper.plc(dpdecode.getData(), uncompressedData);
								uncompressedLength *= 2;	
								if (uncompressedLength > 0) {
									VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
									dp.write(uncompressedData);
									dp.setLength(uncompressedLength);
									synchronized (decodedBuffersQueue) {
										decodedBuffersQueue.add(dp);
										decodedBuffersQueue.notify();
									}
								}
							} catch (Exception e) {
								Log.d(VoIPConstants.TAG, "PLC exception: " + e.toString());
							}
						}
						
						// Regular decoding
						try {
							// Log.d(VoIPActivity.logTag, "Decompressing data of length: " + dpdecode.getLength());
							uncompressedLength = opusWrapper.decode(dpdecode.getData(), uncompressedData);
							uncompressedLength = uncompressedLength * 2;
							if (uncompressedLength > 0) {
								// We have a decoded packet
								lastPacketReceived = dpdecode.getVoicePacketNumber();

								byte[] packetData = new byte[uncompressedLength];
								System.arraycopy(uncompressedData, 0, packetData, 0, uncompressedLength);
								VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
								dp.write(packetData);
								synchronized (decodedBuffersQueue) {
									decodedBuffersQueue.add(dp);
									decodedBuffersQueue.notify();
								}
								
								/*
								// Break it into smaller chunks
								while (index < uncompressedLength) {
									int size = Math.min(uncompressedLength - index, minBufSizePlayback);
									byte[] packetData = new byte[size];
									System.arraycopy(uncompressedData, index, packetData, 0, size);
									VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
									dp.write(packetData);
									index += size;
									synchronized (decodedBuffersQueue) {
										decodedBuffersQueue.add(dp);
										decodedBuffersQueue.notify();
									}
								}
								index = 0;
								*/
							}
						} catch (Exception e) {
							Log.d(VoIPConstants.TAG, "Opus decode exception: " + e.toString());
						}
					} else {
						// Wait till we have a packet to decompress
						synchronized (samplesToDecodeQueue) {
							try {
								samplesToDecodeQueue.wait();
							} catch (InterruptedException e) {
								Log.d(VoIPConstants.TAG, "samplesToDecodeQueue interrupted: " + e.toString());
							}
						}
					}
				}
			}
		}).start();
	}
	
	private void startCodecCompression() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				byte[] compressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 10];
				int compressedDataLength = 0;
				while (keepRunning == true) {
					VoIPDataPacket dpencode = samplesToEncodeQueue.peek();
					if (dpencode != null) {
						samplesToEncodeQueue.poll();
						try {
							// Add the uncompressed audio to the compression buffer
							opusWrapper.queue(dpencode.getData());
							// Get compressed data from the encoder
							while ((compressedDataLength = opusWrapper.getEncodedData(OpusWrapper.OPUS_FRAME_SIZE, compressedData, compressedData.length)) > 0) {
								byte[] trimmedCompressedData = new byte[compressedDataLength];
								System.arraycopy(compressedData, 0, trimmedCompressedData, 0, compressedDataLength);
								VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
								dp.write(trimmedCompressedData);
								synchronized (encodedBuffersQueue) { 
									encodedBuffersQueue.add(dp);
									encodedBuffersQueue.notify();
								}

							}
						} catch (Exception e) {
							Log.e(VoIPConstants.TAG, "Compression error: " + e.toString());
						}
						
					} else {
						synchronized (samplesToEncodeQueue) {
							try {
								samplesToEncodeQueue.wait();
							} catch (InterruptedException e) {
								Log.d(VoIPConstants.TAG, "samplesToEncodeQueue interrupted: " + e.toString());
							}
						}
					}
				}
			}
		}).start();
	}
	
	public void startAudio() {
		
		if (connectionEstablished == false) {
			// We are still trying to establish a connection
			answerOnConnectionEstablished = true;
			sendHandlerMessage(VoIPActivity.MSG_ANSWER_BEFORE_CONNECTION_ESTB);
			return;
		}
		
		startRecordingAndPlayback();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.START_VOICE);
				sendPacket(dp, true);
			}
		}).start();
	}
	
	@SuppressLint("InlinedApi") private void startRecordingAndPlayback() {

		if (audioStarted == true) {
			Log.d(VoIPConstants.TAG, "Audio already started.");
			return;
		}
		
		Log.d(VoIPConstants.TAG, "Starting audio record / playback.");

		startRecording();
		startPlayBack();
		audioStarted = true;
		partnerTimeoutThread.interrupt();
		sendHandlerMessage(VoIPActivity.MSG_AUDIO_START);
	}
	
	private void startRecording() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				AudioRecord recorder;
				int minBufSizeRecording = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
				Log.d(VoIPConstants.TAG, "minBufSizeRecording: " + minBufSizeRecording);

				// Initialize the audio source
				recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizeRecording);

				if (android.os.Build.VERSION.SDK_INT >= 16) {
					// Attach noise suppressor
					if (NoiseSuppressor.isAvailable()) {
						NoiseSuppressor ns = NoiseSuppressor.create(recorder.getAudioSessionId());
						Log.w(VoIPConstants.TAG, "Current NS status: " + ns.getEnabled());
						if (ns != null) ns.setEnabled(true);
					} else {
						Log.w(VoIPConstants.TAG, "Noise suppression not available.");
					}
				}
				
				// Start recording audio from the mic
				try {
					recorder.startRecording();
				} catch (IllegalStateException e) {
					Log.d(VoIPConstants.TAG, "Recorder exception: " + e.toString());
				}
				
				// Start processing recorded data
				byte[] recordedData = new byte[minBufSizeRecording];
				int retVal;
            	int index = 0;
            	int newSize = 0;
				while (keepRunning == true) {
					retVal = recorder.read(recordedData, 0, recordedData.length);
					if (retVal != recordedData.length)
						Log.w(VoIPConstants.TAG, "Unexpected recorded data length. Expected: " + recordedData.length + ", Recorded: " + retVal);
					if (mute == true)
						continue;

					// Break input audio into smaller chunks
                	while (index < retVal) {
                		if (retVal - index < OpusWrapper.OPUS_FRAME_SIZE * 2)
                			newSize = retVal - index;
                		else
                			newSize = OpusWrapper.OPUS_FRAME_SIZE * 2;

                		byte[] data = new byte[newSize];
                		byte[] withoutEcho = null;
                		System.arraycopy(recordedData, index, data, 0, newSize);
                		index += newSize;

						withoutEcho = data;
                		
	                	// Add it to the samples to encode queue
						VoIPDataPacket dp = new VoIPDataPacket(VoIPDataPacket.PacketType.VOICE_PACKET);
	                	dp.write(withoutEcho);

	                	synchronized (samplesToEncodeQueue) {
		                	samplesToEncodeQueue.add(dp);
	                		samplesToEncodeQueue.notify();
						}
                	}
					index = 0;
				}
				
				// Stop recording
				if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
					recorder.stop();
				
				recorder.release();
			}
		}).start();
	}
	
	private void startPlayBack() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				byte[] silence = new byte[1000];
				final VoIPDataPacket voiceCache = new VoIPDataPacket(PacketType.VOICE_PACKET);
				voiceCache.setData(silence);

				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				int index = 0, size = 0;
				minBufSizePlayback = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				Log.d(VoIPConstants.TAG, "minBufSizePlayback: " + minBufSizePlayback);
				audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizePlayback, AudioTrack.MODE_STREAM);
				
				// Audiotrack monitor
				audioTrack.setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
					
					@Override
					public void onPeriodicNotification(AudioTrack track) {
						// do nothing
					}
					
					@Override
					public void onMarkerReached(AudioTrack track) {
						Log.w(VoIPConstants.TAG, "Buffer underrun expected.");

						if (voiceCache != null) {
		                	synchronized (decodedBuffersQueue) {
			                	decodedBuffersQueue.add(voiceCache);
			                	decodedBuffersQueue.notify();
							}
						}

					}
				});
				
				audioTrack.play();
				
				while (keepRunning == true) {
					VoIPDataPacket dp = decodedBuffersQueue.peek();
					if (dp != null) {
						decodedBuffersQueue.poll();

						// audioTrack.write(dp.getData(), 0, dp.getLength());

						// For streaming mode, we must write data in chunks <= buffer size
						index = 0;
						while (index < dp.getLength()) {
							size = Math.min(minBufSizePlayback, dp.getLength() - index);
							audioTrack.write(dp.getData(), index, size);
							index += size; 
						}
						audioTrack.setNotificationMarkerPosition(audioTrack.getPlaybackHeadPosition() + (dp.getLength() / 2));

					} else {
						synchronized (decodedBuffersQueue) {
							try {
								decodedBuffersQueue.wait();
							} catch (InterruptedException e) {
								Log.d(VoIPConstants.TAG, "decodedBuffersQueue interrupted: " + e.toString());
							}
						}
					}
				}
				
				audioTrack.pause();
				audioTrack.flush();
				audioTrack.release();
				audioTrack = null;
			}
		}).start();
		
	}
	
	private void startSendingAndReceiving() {
		startSending();
		startReceiving();
	}
	
	private void startSending() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				int voicePacketCount = 1;
				while (keepRunning == true) {
					VoIPDataPacket dp = encodedBuffersQueue.peek();
					if (dp != null) {
						encodedBuffersQueue.poll();
						dp.voicePacketNumber = voicePacketCount++;
						
						// Encrypt packet
						if (encryptionStage == EncryptionStage.STAGE_READY) {
							byte[] origData = dp.getData();
							dp.write(encryptor.aesEncrypt(origData));
							dp.setEncrypted(true);
						}
						
						sendPacket(dp, false);
					} else {
						synchronized (encodedBuffersQueue) {
							try {
								encodedBuffersQueue.wait();
							} catch (InterruptedException e) {
								Log.d(VoIPConstants.TAG, "encodedBuffersQueue interrupted: " + e.toString());
							}
						}
					}
				}
			}
		}).start();
	}
	
	private void startReceiving() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				byte[] buffer = new byte[50000];
				while (keepRunning == true) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					try {
						socket.setSoTimeout(0);
						socket.receive(packet);
						totalBytesReceived += packet.getLength();
					} catch (IOException e) {
						Log.e(VoIPConstants.TAG, "startReceiving() IOException: " + e.toString());
					}
					
					byte[] realData = new byte[packet.getLength()];
					System.arraycopy(packet.getData(), 0, realData, 0, packet.getLength());
					VoIPDataPacket dataPacket = getPacketFromUDPData(realData);
					
					if (dataPacket == null)
						continue;
					
					// ACK tracking
					if (dataPacket.getType() != PacketType.ACK)
						markPacketReceived(dataPacket.getPacketNumber());

					// ACK response
					if (dataPacket.isRequiresAck() == true) {
						VoIPDataPacket dp = new VoIPDataPacket(PacketType.ACK);
						dp.setPacketNumber(dataPacket.getPacketNumber());
						sendPacket(dp, false);
					}
					
					// Latency tracking
					if (dataPacket.getTimestamp() > 0) {
						// TODO
					}
					
					if (dataPacket.getType() == null) {
						Log.w(VoIPConstants.TAG, "Unknown packet type.");
						continue;
					}
					
					switch (dataPacket.getType()) {
					case VOICE_PACKET:
						if (dataPacket.isEncrypted()) {
							byte[] encryptedData = dataPacket.getData();
							dataPacket.write(encryptor.aesDecrypt(encryptedData));
							dataPacket.setEncrypted(false);
						}
						
						synchronized (samplesToDecodeQueue) {
							samplesToDecodeQueue.add(dataPacket);
							samplesToDecodeQueue.notify();
						}
						break;
						
					case HEARTBEAT:
						// Log.d(VoIPActivity.logTag, "Received heartbeat.");
						lastHeartbeat = new Date();
						break;
						
					case ACK:
						removePacketFromAckWaitQueue(dataPacket.getPacketNumber());
						break;
						
					case ENCRYPTION_PUBLIC_KEY:
						if (clientSelf.isInitiator() == true) {
							Log.e(VoIPConstants.TAG, "Was not expecting a public key.");
							continue;
						}
						Log.d(VoIPConstants.TAG, "Received public key.");
						encryptor.setPublicKey(dataPacket.getData());
						encryptionStage = EncryptionStage.STAGE_GOT_PUBLIC_KEY;
						exchangeCryptoInfo();
						break;
						
					case ENCRYPTION_SESSION_KEY:
						if (clientSelf.isInitiator() != true) {
							Log.e(VoIPConstants.TAG, "Was not expecting a session key.");
							continue;
						}
						encryptor.setSessionKey(encryptor.rsaDecrypt(dataPacket.getData()));
						Log.d(VoIPConstants.TAG, "Received session key.");
						encryptionStage = EncryptionStage.STAGE_GOT_SESSION_KEY;
						exchangeCryptoInfo();
						break;
						
					case ENCRYPTION_RECEIVED_SESSION_KEY:
						Log.d(VoIPConstants.TAG, "Encryption ready.");
						encryptionStage = EncryptionStage.STAGE_READY;
						break;
						
					case END_CALL:
						Log.d(VoIPConstants.TAG, "Other party hung up.");
						hangUp();
						break;
						
					case START_VOICE:
						startRecordingAndPlayback();
						break;
						
					case CALL_DECLINED:
						sendHandlerMessage(VoIPActivity.MSG_CALL_DECLINED);
						stop();
						break;
						
					default:
						Log.w(VoIPConstants.TAG, "Received unexpected packet: " + dataPacket.getType());
						break;
					}
				}
			}
		}).start();
	}
	
	private void sendPacket(VoIPDataPacket dp, boolean requiresAck) {
		
		if (dp == null)
			return;
		
		if (clientPartner.getPreferredConnectionMethod() == ConnectionMethods.RELAY) {
			dp.setDestinationIP(clientPartner.getExternalIPAddress());
			dp.setDestinationPort(clientPartner.getExternalPort());
		}
		
		if (dp.getType() != PacketType.ACK && dp.getPacketNumber() == 0)
			dp.setPacketNumber(currentPacketNumber++);
		
		if (dp.getType() == PacketType.VOICE_PACKET)
			rawVoiceSent += dp.getLength();
		
		dp.setRequiresAck(requiresAck);
		dp.setTimestamp(System.currentTimeMillis());
		
		if (requiresAck == true)
			addPacketToAckWaitQueue(dp);

		// Serialize everything except for P2P voice data packets
		byte[] packetData = getUDPDataFromPacket(dp);
		
		try {
			DatagramPacket packet = new DatagramPacket(packetData, packetData.length, clientPartner.getCachedInetAddress(), clientPartner.getPreferredPort());
			socket.send(packet);
			totalBytesSent += packet.getLength();
			totalPacketsSent++;
		} catch (IOException e) {
			Log.e(VoIPConstants.TAG, "IOException: " + e.toString());
		}
		
	}
	
	private byte[] getUDPDataFromPacket(VoIPDataPacket dp) {
		
		// Serialize everything except for P2P voice data packets
		byte[] packetData = null;
		byte prefix;
		
		if (dp.getType() == PacketType.VOICE_PACKET && clientPartner.getPreferredConnectionMethod() != ConnectionMethods.RELAY) {
			packetData = dp.getData();
			if (dp.isEncrypted()) {
				prefix = PP_ENCRYPTED_VOICE_PACKET;
			} else {
				prefix = PP_RAW_VOICE_PACKET;
			}
		} else {
			packetData = VoIPSerializer.serialize(dp);
			prefix = PP_PROTOCOL_BUFFER;
		}
		
		if (clientPartner.getPreferredConnectionMethod() != ConnectionMethods.RELAY) { // If RELAY, the server will put the prefix on the packet
			byte[] finalData = new byte[packetData.length + 1];		// 
			finalData[0] = prefix;
			System.arraycopy(packetData, 0, finalData, 1, packetData.length);
			packetData = finalData;
		}

		return packetData;
	}
	
	private VoIPDataPacket getPacketFromUDPData(byte[] data) {
		VoIPDataPacket dp = null;
		byte prefix = data[0];
		byte[] packetData = new byte[data.length - 1];
		System.arraycopy(data, 1, packetData, 0, packetData.length);

		if (prefix == PP_PROTOCOL_BUFFER) {
			dp = (VoIPDataPacket) VoIPSerializer.deserialize(packetData);
		} else {
			dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
			dp.setData(packetData);
			if (prefix == PP_ENCRYPTED_VOICE_PACKET)
				dp.setEncrypted(true);
			else
				dp.setEncrypted(false);
		}
		
		return dp;
	}
	
	private void addPacketToAckWaitQueue(VoIPDataPacket dp) {
		synchronized (ackWaitQueue) {
			if (ackWaitQueue.containsKey(dp.getPacketNumber()))
				return;

			ackWaitQueue.put(dp.getPacketNumber(), dp);
		}
	}
	
	private void markPacketReceived(int packetNumber) {
		if (packetNumber > previousHighestRemotePacketNumber) {
			// New highest packet received
			// Set all bits between this and previous highest packet to zero
			int mod1 = packetNumber % PACKET_TRACKING_SIZE;
			int mod2 = previousHighestRemotePacketNumber % PACKET_TRACKING_SIZE;
			if (mod1 > mod2)
				packetTrackingBits.clear(mod2 + 1, mod1);
			else {
				if (mod2 + 1 < PACKET_TRACKING_SIZE - 1)
					packetTrackingBits.clear(mod2 + 1, PACKET_TRACKING_SIZE - 1);
				packetTrackingBits.clear(0, mod1);
			}
			previousHighestRemotePacketNumber = packetNumber;
		}
		
		// Mark packet as received
		int mod = packetNumber % PACKET_TRACKING_SIZE;
		packetTrackingBits.set(mod);
	}
	
	private void sendPacketsWaitingForAck() {
		if (ackWaitQueue.isEmpty())
			return;
		
		synchronized (ackWaitQueue) {
			Iterator<Integer> iterator = ackWaitQueue.keySet().iterator();;
			long currentTime = System.currentTimeMillis();

			while (iterator.hasNext()) {
				Integer i = iterator.next();
				if (ackWaitQueue.get(i).getTimestamp() < currentTime - 1000) {	// Give each packet 1 second to get ack
					Log.d(VoIPConstants.TAG, "Re-Requesting ack for: " + ackWaitQueue.get(i).getType());
					sendPacket(ackWaitQueue.get(i), true);
				}
			}
		}		
	}
	
	private void removePacketFromAckWaitQueue(int packetNumber) {
		synchronized (ackWaitQueue) {
			ackWaitQueue.remove(packetNumber);
		}
	}
	
	private void exchangeCryptoInfo() {
		
		if (cryptoEnabled == false)
			return;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (encryptionStage == EncryptionStage.STAGE_INITIAL && clientSelf.isInitiator() == true) {
					// The initiator (caller) generates and sends a public key
					encryptor.initKeys();
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_PUBLIC_KEY);
					dp.write(encryptor.getPublicKey());
					sendPacket(dp, true);
					Log.d(VoIPConstants.TAG, "Sending public key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_PUBLIC_KEY && clientSelf.isInitiator() == false) {
					// Generate and send the AES session key
					encryptor.initSessionKey();
					byte[] encryptedSessionKey = encryptor.rsaEncrypt(encryptor.getSessionKey(), encryptor.getPublicKey());
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_SESSION_KEY);
					dp.write(encryptedSessionKey);
					sendPacket(dp, true);
					Log.d(VoIPConstants.TAG, "Sending AES key.");
				}
				
				if (encryptionStage == EncryptionStage.STAGE_GOT_SESSION_KEY) {
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_RECEIVED_SESSION_KEY);
					sendPacket(dp, true);
					encryptionStage = EncryptionStage.STAGE_READY;
					Log.d(VoIPConstants.TAG, "Encryption ready.");
				}
			}
		}).start();
	}

	public int adjustBitrate(int delta) {
		if (delta > 0 && localBitrate + delta < 64000)
			localBitrate += delta;
		if (delta < 0 && localBitrate + delta >= 3000)
			localBitrate += delta;
		
		if (opusWrapper == null)
			return localBitrate;
		
		opusWrapper.setEncoderBitrate(localBitrate);
		sendHandlerMessage(VoIPActivity.MSG_CURRENT_BITRATE);
		return localBitrate;
	}
	
	public int getBitrate() {
		return localBitrate;
	}
	
	public void adjustGain(int gainDelta) {
		if (gainDelta > 0 && gain > 5000)
			return;
		if (gainDelta < 0 && gain < -5000)
			return;
		gain += gainDelta;
		opusWrapper.setDecoderGain(gain);
		
		// Save the gain preference
		SharedPreferences preferences = getSharedPreferences(HikeMessengerApp.VOIP_SETTINGS, Context.MODE_PRIVATE);
		Editor edit = preferences.edit();
		edit.putInt(HikeMessengerApp.VOIP_AUDIO_GAIN, gain);
		edit.commit();
	}
	
	public boolean isConnected() {
		return connectionEstablished;
	}
	
	public boolean isAudioRunning() {
		return audioStarted;
	}
	
	public void retrieveExternalSocket() {

		// Beeping thread
		waitingBeepThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					tg.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 75);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		});

		Thread iceThread = new Thread(new Runnable() {

			@Override
			public void run() {

				byte[] receiveData = new byte[10240];
				
				try {
					InetAddress host = InetAddress.getByName(VoIPConstants.ICEServerName);
					socket = new DatagramSocket();
					socket.setReuseAddress(true);
					socket.setSoTimeout(2000);

					VoIPDataPacket dp = new VoIPDataPacket(PacketType.RELAY_INIT);
					byte[] dpData = VoIPSerializer.serialize(dp);
					DatagramPacket outgoingPacket = new DatagramPacket(dpData, dpData.length, host, VoIPConstants.ICEServerPort);
					DatagramPacket incomingPacket = new DatagramPacket(receiveData, receiveData.length);

					clientSelf.setInternalIPAddress(VoIPUtils.getLocalIpAddress(getApplicationContext())); 
					clientSelf.setInternalPort(socket.getLocalPort());
					
					boolean continueSending = true;
					int counter = 0;

					while (continueSending && counter < 10) {
						counter++;
						try {
							// Log.d(VoIPActivity.logTag, "ICE Sending: " + outgoingPacket.getData().toString() + " to " + host.getHostAddress() + ":" + ICEServerPort);
							socket.send(outgoingPacket);
							socket.receive(incomingPacket);
							
							String serverResponse = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
							Log.d(VoIPConstants.TAG, "ICE Received: " + serverResponse);
							setExternalSocketInfo(serverResponse);
							continueSending = false;
							
						} catch (SocketTimeoutException e) {
							Log.d(VoIPConstants.TAG, "UDP timeout on ICE. #" + counter);
						} catch (IOException e) {
							Log.d(VoIPConstants.TAG, "IOException: " + e.toString());
						} catch (JSONException e) {
							Log.d(VoIPConstants.TAG, "JSONException: " + e.toString());
							continueSending = true;
						}
					}

				} catch (SocketException e) {
					Log.d(VoIPConstants.TAG, "SocketException: " + e.toString());
				} catch (UnknownHostException e) {
					Log.d(VoIPConstants.TAG, "UnknownHostException: " + e.toString());
				}
				
				if (haveExternalSocketInfo())
					try {
						sendSocketInfoToPartner();
						if (clientPartner.isInitiator())
							establishConnection();
					} catch (JSONException e) {
						Log.d(VoIPConstants.TAG, "JSONException: " + e.toString());
					}
				else {
					Log.d(VoIPConstants.TAG, "Failed to retrieve external socket.");
					sendHandlerMessage(VoIPActivity.MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE);
				}
			}
		});
		
		iceThread.start();
		
		// Beeps while connecting to recipient
		if (clientSelf.isInitiator())
			waitingBeepThread.start();
	}

	private void setExternalSocketInfo(String ICEResponse) throws JSONException {
		JSONObject jsonObject = new JSONObject(ICEResponse);
		clientSelf.setExternalIPAddress(jsonObject.getString("IP"));
		clientSelf.setExternalPort(Integer.parseInt(jsonObject.getString("Port")));
		Log.d(VoIPConstants.TAG, "External socket - " + clientSelf.getExternalIPAddress() + ":" + clientSelf.getExternalPort());
		Log.d(VoIPConstants.TAG, "Internal socket - " + clientSelf.getInternalIPAddress() + ":" + clientSelf.getInternalPort());
	}
	
	private boolean haveExternalSocketInfo() {
		if (clientSelf.getExternalIPAddress() != null && 
				!clientSelf.getExternalIPAddress().isEmpty() && 
				clientSelf.getExternalPort() > 0)
			return true;
		else
			return false;
	}
	
	private void sendSocketInfoToPartner() throws JSONException {
		if (clientPartner.getPhoneNumber() == null || clientPartner.getPhoneNumber().isEmpty()) {
			Log.e(VoIPConstants.TAG, "Have no partner info. Quitting.");
			return;
		}

		JSONObject socketData = new JSONObject();
		socketData.put("internalIP", clientSelf.getInternalIPAddress()); 
		socketData.put("internalPort", clientSelf.getInternalPort());
		socketData.put("externalIP", clientSelf.getExternalIPAddress());
		socketData.put("externalPort", clientSelf.getExternalPort());
		socketData.put("initiator", clientSelf.isInitiator());
		
		JSONObject data = new JSONObject();
		data.put(HikeConstants.MESSAGE_ID, new Random().nextInt(10000));	// TODO: possibly needs to changed
		data.put(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000); 
		data.put(HikeConstants.METADATA, socketData);

		JSONObject message = new JSONObject();
		message.put(HikeConstants.TO, clientPartner.getPhoneNumber());
		message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_VOIP_0);
		message.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.VOIP_SOCKET_INFO);
		message.put(HikeConstants.DATA, data);
		
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, message);
		Log.d(VoIPConstants.TAG, "Sent socket information to partner.");
		
		// Wait for partner to send us their socket information
		// Set timeout so we don't wait indefinitely
		partnerTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(VoIPConstants.TIMEOUT_PARTNER_SOCKET_INFO);
					sendHandlerMessage(VoIPActivity.MSG_PARTNER_SOCKET_INFO_TIMEOUT);
				} catch (InterruptedException e) {
					Log.d(VoIPConstants.TAG, "Received partner socket info.");
				}
			}
		});
		
		partnerTimeoutThread.start();
	}
	
	/**
	 * Once socket information for the partner has been received, this
	 * function should be called to establish and verify a UDP connection.
	 */
	public void establishConnection() {
		connectionEstablished = false;
		partnerTimeoutThread.interrupt();
		Log.d(VoIPConstants.TAG, "Trying to establish P2P connection..");
		Log.d(VoIPConstants.TAG, "Listening to local socket (for p2p) on port: " + socket.getLocalPort());
		
		// Sender thread
		final Thread senderThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				int count = 0;
				while (true) {
					if (Thread.currentThread().isInterrupted())
						break;

					try {
						count++;
						sendUDPData(VoIPConstants.COMM_UDP_SYN_PRIVATE.getBytes(), clientPartner.getInternalIPAddress(), clientPartner.getInternalPort());
						sendUDPData(VoIPConstants.COMM_UDP_SYN_PUBLIC.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
						if (count > 10)
							sendUDPData(VoIPConstants.COMM_UDP_SYN_RELAY.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
						Thread.sleep(200);
					} catch (InterruptedException e) {
						Log.d(VoIPConstants.TAG, "Stopping sending thread.");
						break;
					}
				}
			}
		});
		
		// Receiving thread
		final Thread receivingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				String lastPacketReceived = null;
				while (true) {
					if (Thread.currentThread().isInterrupted())
						break;
					byte[] receiveData = new byte[10240];
					DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
					try {
						socket.setSoTimeout(500);
						socket.receive(packet);

						String data = new String(packet.getData(), 0, packet.getLength());
						Log.d(VoIPConstants.TAG, "CS Received: " + data);
						
						if (data.equals(lastPacketReceived))
							continue;
						
						lastPacketReceived = data;
						
						if (lastPacketReceived.equals(VoIPConstants.COMM_UDP_SYN_PRIVATE)) {
							senderThread.interrupt();
							sendUDPData(VoIPConstants.COMM_UDP_SYNACK_PRIVATE.getBytes(), clientPartner.getInternalIPAddress(), clientPartner.getInternalPort());
						}
						
						if (lastPacketReceived.equals(VoIPConstants.COMM_UDP_SYN_PUBLIC)) {
							senderThread.interrupt();
							sendUDPData(VoIPConstants.COMM_UDP_SYNACK_PUBLIC.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
						}
						
						if (lastPacketReceived.equals(VoIPConstants.COMM_UDP_SYN_RELAY)) {
							senderThread.interrupt();
							sendUDPData(VoIPConstants.COMM_UDP_SYNACK_RELAY.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
						}
						
						if (lastPacketReceived.equals(VoIPConstants.COMM_UDP_SYNACK_PRIVATE) ||
								lastPacketReceived.equals(VoIPConstants.COMM_UDP_ACK_PRIVATE)) {
							senderThread.interrupt();
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							sendUDPData(VoIPConstants.COMM_UDP_ACK_PRIVATE.getBytes(), clientPartner.getInternalIPAddress(), clientPartner.getInternalPort());
							connectionEstablished = true;
						}
						
						if (lastPacketReceived.equals(VoIPConstants.COMM_UDP_SYNACK_PUBLIC) ||
								lastPacketReceived.equals(VoIPConstants.COMM_UDP_ACK_PUBLIC)) {
							if (clientPartner.getPreferredConnectionMethod() != ConnectionMethods.PRIVATE) {	// Private interface takes priority
								senderThread.interrupt();
								clientPartner.setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
								sendUDPData(VoIPConstants.COMM_UDP_ACK_PUBLIC.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
								connectionEstablished = true;
							}
						}

						if (lastPacketReceived.equals(VoIPConstants.COMM_UDP_SYNACK_RELAY) ||
								lastPacketReceived.equals(VoIPConstants.COMM_UDP_ACK_RELAY)) {
							if (clientPartner.getPreferredConnectionMethod() != ConnectionMethods.PRIVATE &&
									clientPartner.getPreferredConnectionMethod() != ConnectionMethods.PUBLIC) {	// Private and public interface takes priority
								senderThread.interrupt();
								clientPartner.setPreferredConnectionMethod(ConnectionMethods.RELAY);
								sendUDPData(VoIPConstants.COMM_UDP_ACK_RELAY.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
								connectionEstablished = true;
							}
						}
						
					} catch (IOException e) {
						Log.d(VoIPConstants.TAG, "CS receiving exception: " + e.toString());
					}
				}
			}
		});
		
		receivingThread.start();
		senderThread.start();
		
		// Monitoring / timeout thread
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					for (int i = 0; i < 20; i++) {
						if (connectionEstablished == true) {
							Thread.sleep(500);		// To let the last message(s) go through
							break;
						}
						if (keepRunning == false) {
							// We probably declined an incoming call
							break;
						}
						
						Thread.sleep(500);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				senderThread.interrupt();
				receivingThread.interrupt();
				waitingBeepThread.interrupt();
				
				if (connectionEstablished == true) {
					Log.d(VoIPConstants.TAG, "UDP connection established :) " + clientPartner.getPreferredConnectionMethod());
					sendHandlerMessage(VoIPActivity.MSG_CONNECTION_ESTABLISHED);
					try {
						startStreaming();
						startResponseTimeout();
					} catch (Exception e) {
						Log.e(VoIPConstants.TAG, "Exception: " + e.toString());
					}
					// We have already answered the incoming calls
					if (answerOnConnectionEstablished == true) {
						startAudio();
					}
				}
				else {
					Log.d(VoIPConstants.TAG, "UDP connection failure! :(");
					sendHandlerMessage(VoIPActivity.MSG_CONNECTION_FAILURE);
				}
				
			}
		}).start();

	}

	private void startResponseTimeout() {
		partnerTimeoutThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(VoIPConstants.TIMEOUT_PARTNER_ANSWER);
					if (!isAudioRunning()) {
						// Call not answered yet?
						sendHandlerMessage(VoIPActivity.MSG_PARTNER_ANSWER_TIMEOUT);
					}
				} catch (InterruptedException e) {
					// Do nothing, all is good
				}
			}
		});
		
		partnerTimeoutThread.start();
	}

	private void sendUDPData(byte[] data, String IpAddress, int port) {
		
		// For relay packets, we must send them as PB since the server needs to parse them
		if (Arrays.equals(data, VoIPConstants.COMM_UDP_SYN_RELAY.getBytes()) ||
				Arrays.equals(data, VoIPConstants.COMM_UDP_SYNACK_RELAY.getBytes()) ||
				Arrays.equals(data, VoIPConstants.COMM_UDP_ACK_RELAY.getBytes())) {
		
			VoIPDataPacket dp = new VoIPDataPacket(PacketType.RELAY);
			dp.setData(data);
			dp.setDestinationIP(IpAddress);
			dp.setDestinationPort(port);
			byte[] serializedData = VoIPSerializer.serialize(dp);
			
			IpAddress = VoIPConstants.ICEServerName;
			port = VoIPConstants.ICEServerPort;
			data = serializedData;
		}
		
		try {
			InetAddress address = InetAddress.getByName(IpAddress);
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
			socket.send(packet);
			Log.d(VoIPConstants.TAG, "CS sending: " + data);
		} catch (IOException e) {
			Log.d(VoIPConstants.TAG, "IOException: " + e.toString());
		}
	}	
	
	
}

