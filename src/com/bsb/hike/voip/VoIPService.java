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
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.VoIPConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.VoIPActivity;
import com.bsb.hike.voip.VoIPClient.ConnectionMethods;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.VoIPEncryptor.EncryptionStage;
import com.bsb.hike.voip.protobuf.VoIPSerializer;

public class VoIPService extends Service {
	
	public static final String ICEServerName = "174.37.122.202";
	public static final int ICEServerPort = 9999;

	private final IBinder myBinder = new LocalBinder();
	private final int SAMPLE_RATE = 48000; 
	private final int PACKET_TRACKING_SIZE = 128;
	private final int HEARTBEAT_INTERVAL = 1000;
	private final int HEARTBEAT_TIMEOUT = HEARTBEAT_INTERVAL * 10;
	private final int MAX_SAMPLES_BUFFER = 3;

	private int localBitrate = 6000;
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
	
	private OpusWrapper opusWrapper;
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
	
	@Override
	public void onCreate() {
		super.onCreate();
		opusWrapper = new OpusWrapper();
		opusWrapper.getEncoder(SAMPLE_RATE, 1, localBitrate);
		opusWrapper.getDecoder(SAMPLE_RATE, 1);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(VoIPCaller.logTag, "VoIP Service destroyed.");
	}

	public void setClientSelf(VoIPClient clientSelf) {
		this.clientSelf = clientSelf;
		if (socket == null)
			try {
				socket = new DatagramSocket(clientSelf.getInternalPort());
			} catch (SocketException e) {
				Log.d(VoIPCaller.logTag, "VoIPService SocketException: " + e.toString());
			}
	}
	
	public void setMessenger(Messenger messenger) {
		this.mMessenger = messenger;
	}
	
	public void startStreaming() throws Exception {
		encryptionStage = EncryptionStage.STAGE_INITIAL;
		
		if (clientPartner == null || clientSelf == null) {
			throw new Exception("Clients (partner and/or self) not set.");
		}
		
		startCodec();
		// startRecordingAndPlayback();
		startSendingAndReceiving();
		startHeartbeat();
		exchangeCryptoInfo();
		
		Log.d(VoIPCaller.logTag, "Streaming started.");
	}
	
	public void stop() {
		if (keepRunning == false)
			return;
		
		keepRunning = false;
		sendHandlerMessage(VoIPActivity.MSG_SHUTDOWN_CALL);
		Log.d(VoIPCaller.logTag, "Bytes sent: " + totalBytesSent + 
				", voice sent: " + rawVoiceSent + ", bytes received: " + totalBytesReceived +
				", dropped decoded packets: " + droppedDecodedPackets);
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
			Log.e(VoIPCaller.logTag, "Messenger RemoteException: " + e.toString());
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
						Log.d(VoIPCaller.logTag, "Heartbeat InterruptedException: " + e.toString());
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
						Log.d(VoIPCaller.logTag, "Heartbeat failure.");
						hangUp();
						break;
					}
					
					sendPacketsWaitingForAck();
					
					// Drop packets if getting left behind
					while (samplesToDecodeQueue.size() > MAX_SAMPLES_BUFFER) {
						Log.d(VoIPCaller.logTag, "Dropping to_decode packet.");
						samplesToDecodeQueue.poll();
					}
					
					while (samplesToEncodeQueue.size() > MAX_SAMPLES_BUFFER) {
						Log.d(VoIPCaller.logTag, "Dropping to_encode packet.");
						samplesToEncodeQueue.poll();
					}
					
					while (decodedBuffersQueue.size() > MAX_SAMPLES_BUFFER) {
						// Log.d(VoIPCaller.logTag, "Dropping decoded packet.");
						droppedDecodedPackets++;
						decodedBuffersQueue.poll();
					}
					
					while (encodedBuffersQueue.size() > MAX_SAMPLES_BUFFER) {
						Log.d(VoIPCaller.logTag, "Dropping encoded packet.");
						encodedBuffersQueue.poll();
					}

					try {
						Thread.sleep(HEARTBEAT_INTERVAL);
					} catch (InterruptedException e) {
						Log.d(VoIPCaller.logTag, "Heartbeat InterruptedException: " + e.toString());
					}
					
				}
			}
		}).start();
		
	}
	
	private void startCodec() {
		startCodecDecompression();
		startCodecCompression();
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
						// Log.d(VoIPCaller.logTag, "Decoding queue size: " + samplesToDecodeQueue.size());
						byte[] uncompressedData = new byte[OpusWrapper.OPUS_FRAME_SIZE * 10];	// Just to be safe, we make a big buffer
						
						if (dpdecode.getVoicePacketNumber() > 0 && dpdecode.getVoicePacketNumber() <= lastPacketReceived)
							continue;	// We received an old packet again
						
						// Handle packet loss
						if (dpdecode.getVoicePacketNumber() > lastPacketReceived + 1) {
							Log.d(VoIPCaller.logTag, "Packet loss! (" + (dpdecode.getVoicePacketNumber() - lastPacketReceived) + ")");
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
								Log.d(VoIPCaller.logTag, "PLC exception: " + e.toString());
							}
						}
						
						// Regular decoding
						try {
							// Log.d(VoIPCaller.logTag, "Decompressing data of length: " + dpdecode.getLength());
							uncompressedLength = opusWrapper.decode(dpdecode.getData(), uncompressedData);
							uncompressedLength = uncompressedLength * 2;
							// Log.d(VoIPCaller.logTag, "Decompressed: " + uncompressedLength);
							if (uncompressedLength > 0) {
								lastPacketReceived = dpdecode.getVoicePacketNumber();
								VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
								dp.write(uncompressedData);
								dp.setLength(uncompressedLength);
								synchronized (decodedBuffersQueue) {
									decodedBuffersQueue.add(dp);
									decodedBuffersQueue.notify();
								}
							}
						} catch (Exception e) {
							Log.d(VoIPCaller.logTag, "Opus decode exception: " + e.toString());
						}
					} else {
						// Wait till we have a packet to decompress
						synchronized (samplesToDecodeQueue) {
							try {
								samplesToDecodeQueue.wait();
							} catch (InterruptedException e) {
								Log.d(VoIPCaller.logTag, "samplesToDecodeQueue interrupted: " + e.toString());
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
							// Log.d(VoIPCaller.logTag, "Queued for compression: " + dpencode.getLength());
							// Get compressed data from the encoder
							while ((compressedDataLength = opusWrapper.getEncodedData(OpusWrapper.OPUS_FRAME_SIZE, compressedData, compressedData.length)) > 0) {
								// Log.d(VoIPCaller.logTag, "Compressed audio length: " + compressedDataLength);
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
							Log.e(VoIPCaller.logTag, "Compression error: " + e.toString());
						}
						
					} else {
						synchronized (samplesToEncodeQueue) {
							try {
								samplesToEncodeQueue.wait();
							} catch (InterruptedException e) {
								Log.d(VoIPCaller.logTag, "samplesToEncodeQueue interrupted: " + e.toString());
							}
						}
					}
				}
			}
		}).start();
	}
	
	private void startRecordingAndPlayback() {

		if (audioStarted == true) {
			Log.d(VoIPCaller.logTag, "Audio already started.");
			return;
		}
		
		Log.d(VoIPCaller.logTag, "Starting audio record / playback.");
		startRecording();
		startPlayBack();
		audioStarted = true;
		sendHandlerMessage(VoIPActivity.MSG_AUDIO_START);
	}
	
	public void startAudio() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				startRecordingAndPlayback();
				VoIPDataPacket dp = new VoIPDataPacket(PacketType.START_VOICE);
				sendPacket(dp, true);
			}
		}).start();
	}
	
	private void startRecording() {
		new Thread(new Runnable() {
			
			@SuppressLint("InlinedApi") @Override
			public void run() {
				AudioRecord recorder;
				int minBufSizeRecording = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

				// Initialize the audio source
				if (android.os.Build.VERSION.SDK_INT >= 11)
					recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizeRecording);
				else
					recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_CALL, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizeRecording);

				// Start recording audio from the mic
				try {
					recorder.startRecording();
				} catch (IllegalStateException e) {
					Log.d(VoIPCaller.logTag, "Recorder exception: " + e.toString());
				}
				
				// Start processing recorded data
				byte[] recordedData = new byte[minBufSizeRecording];
				int retVal;
				while (keepRunning == true) {
					retVal = recorder.read(recordedData, 0, recordedData.length);
					// Log.d(VoIPCaller.logTag, "Recorded data of length: " + retVal);
					if (retVal != recordedData.length)
						Log.w(VoIPCaller.logTag, "Unexpected recorded data length. Expected: " + recordedData.length + ", Recorded: " + retVal);
					if (mute == true)
						continue;
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.VOICE_PACKET);
					dp.write(recordedData);
					synchronized (samplesToEncodeQueue) {	
						samplesToEncodeQueue.add(dp);
						samplesToEncodeQueue.notify();
					}
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
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				AudioTrack speaker;
				int index = 0, size = 0;
				int minBufSizePlayback = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				speaker = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufSizePlayback, AudioTrack.MODE_STREAM);
				speaker.play();
				
				while (keepRunning == true) {
					VoIPDataPacket dp = decodedBuffersQueue.peek();
					if (dp != null) {
						// Log.d(VoIPCaller.logTag, "decodedBuffersQueue size: " + decodedBuffersQueue.size());
						decodedBuffersQueue.poll();
						// For streaming mode, we must write data in chunks <= buffer size
						index = 0;
						while (index < dp.getLength()) {
							size = Math.min(minBufSizePlayback, dp.getLength() - index);
							speaker.write(dp.getData(), index, size);
							// Log.d(VoIPCaller.logTag, "Playing data of length: " + size);
							index += minBufSizePlayback; // TODO should be += size?
						}
					} else {
						synchronized (decodedBuffersQueue) {
							try {
								decodedBuffersQueue.wait();
							} catch (InterruptedException e) {
								Log.d(VoIPCaller.logTag, "decodedBuffersQueue interrupted: " + e.toString());
							}
						}
					}
				}
				
				speaker.flush();
				speaker.release();
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
								Log.d(VoIPCaller.logTag, "encodedBuffersQueue interrupted: " + e.toString());
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
						Log.e(VoIPCaller.logTag, "startReceiving() IOException: " + e.toString());
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
						Log.w(VoIPActivity.logTag, "Unknown packet type.");
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
						// Log.d(VoIPCaller.logTag, "Received heartbeat.");
						lastHeartbeat = new Date();
						break;
						
					case ACK:
						removePacketFromAckWaitQueue(dataPacket.getPacketNumber());
						break;
						
					case ENCRYPTION_PUBLIC_KEY:
						if (clientSelf.isInitiator() == true) {
							Log.e(VoIPCaller.logTag, "Was not expecting a public key.");
							continue;
						}
						Log.d(VoIPCaller.logTag, "Received public key.");
						encryptor.setPublicKey(dataPacket.getData());
						encryptionStage = EncryptionStage.STAGE_GOT_PUBLIC_KEY;
						exchangeCryptoInfo();
						break;
						
					case ENCRYPTION_SESSION_KEY:
						if (clientSelf.isInitiator() != true) {
							Log.e(VoIPCaller.logTag, "Was not expecting a session key.");
							continue;
						}
						encryptor.setSessionKey(encryptor.rsaDecrypt(dataPacket.getData()));
						Log.d(VoIPCaller.logTag, "Received session key.");
						encryptionStage = EncryptionStage.STAGE_GOT_SESSION_KEY;
						exchangeCryptoInfo();
						break;
						
					case ENCRYPTION_RECEIVED_SESSION_KEY:
						Log.d(VoIPCaller.logTag, "Encryption ready.");
						encryptionStage = EncryptionStage.STAGE_READY;
						break;
						
					case END_CALL:
						Log.d(VoIPCaller.logTag, "Other party hung up.");
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
						Log.w(VoIPCaller.logTag, "Received unexpected packet: " + dataPacket.getType());
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
		} catch (IOException e) {
			Log.e(VoIPCaller.logTag, "IOException: " + e.toString());
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
			byte[] finalData = new byte[packetData.length + 1];		// TODO: NPE
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
					Log.d(VoIPCaller.logTag, "Re-Requesting ack for: " + ackWaitQueue.get(i).getType());
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
					Log.d(VoIPCaller.logTag, "Sending public key.");
				}

				if (encryptionStage == EncryptionStage.STAGE_GOT_PUBLIC_KEY && clientSelf.isInitiator() == false) {
					// Generate and send the AES session key
					encryptor.initSessionKey();
					byte[] encryptedSessionKey = encryptor.rsaEncrypt(encryptor.getSessionKey(), encryptor.getPublicKey());
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_SESSION_KEY);
					dp.write(encryptedSessionKey);
					sendPacket(dp, true);
					Log.d(VoIPCaller.logTag, "Sending AES key.");
				}
				
				if (encryptionStage == EncryptionStage.STAGE_GOT_SESSION_KEY) {
					VoIPDataPacket dp = new VoIPDataPacket(PacketType.ENCRYPTION_RECEIVED_SESSION_KEY);
					sendPacket(dp, true);
					encryptionStage = EncryptionStage.STAGE_READY;
					Log.d(VoIPCaller.logTag, "Encryption ready.");
				}
			}
		}).start();
	}

	public int adjustBitrate(int delta) {
		if (delta > 0 && localBitrate < 64000)
			localBitrate += delta;
		if (delta < 0 && localBitrate > 4000)
			localBitrate += delta;
		opusWrapper.setEncoderBitrate(localBitrate);
		sendHandlerMessage(VoIPActivity.MSG_CURRENT_BITRATE);
		return localBitrate;
	}
	
	public int getBitrate() {
		return localBitrate;
	}
	
	public void retrieveExternalSocket() {

		Thread iceThread = new Thread(new Runnable() {

			@Override
			public void run() {

				byte[] receiveData = new byte[10240];
				
				try {
					InetAddress host = InetAddress.getByName(ICEServerName);
					socket = new DatagramSocket();
					socket.setReuseAddress(true);
					socket.setSoTimeout(2000);

					VoIPDataPacket dp = new VoIPDataPacket(PacketType.RELAY_INIT);
					byte[] dpData = VoIPSerializer.serialize(dp);
					DatagramPacket outgoingPacket = new DatagramPacket(dpData, dpData.length, host, ICEServerPort);
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
							Log.d(VoIPActivity.logTag, "ICE Received: " + serverResponse);
							setExternalSocketInfo(serverResponse);
							continueSending = false;
							
						} catch (SocketTimeoutException e) {
							Log.d(VoIPActivity.logTag, "UDP timeout on ICE. #" + counter);
						} catch (IOException e) {
							Log.d(VoIPActivity.logTag, "IOException: " + e.toString());
						} catch (JSONException e) {
							Log.d(VoIPActivity.logTag, "JSONException: " + e.toString());
							continueSending = true;
						}
					}

					if (continueSending == true) {
						Log.d(VoIPActivity.logTag, "Unable to retrieve external socket.");
					}

				} catch (SocketException e) {
					Log.d(VoIPActivity.logTag, "SocketException: " + e.toString());
				} catch (UnknownHostException e) {
					Log.d(VoIPActivity.logTag, "UnknownHostException: " + e.toString());
				}
				
				if (haveExternalSocketInfo())
					try {
						sendSocketInfoToPartner();
						if (clientPartner.isInitiator())
							establishConnection();
					} catch (JSONException e) {
						Log.d(VoIPActivity.logTag, "JSONException: " + e.toString());
					}
				else
					Log.d(VoIPActivity.logTag, "Failed to retrieve external socket.");
				
			}
		});
		
		iceThread.start();
	}

	private void setExternalSocketInfo(String ICEResponse) throws JSONException {
		JSONObject jsonObject = new JSONObject(ICEResponse);
		clientSelf.setExternalIPAddress(jsonObject.getString("IP"));
		clientSelf.setExternalPort(Integer.parseInt(jsonObject.getString("Port")));
		Log.d(VoIPActivity.logTag, "External socket - " + clientSelf.getExternalIPAddress() + ":" + clientSelf.getExternalPort());
		Log.d(VoIPActivity.logTag, "Internal socket - " + clientSelf.getInternalIPAddress() + ":" + clientSelf.getInternalPort());
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
			Log.e(VoIPActivity.logTag, "Have no partner info. Quitting.");
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
		data.put(HikeConstants.HIKE_MESSAGE, "--External Socket Info--\n" + socketData.toString()); // TODO: Create a new mqttmessagetype instead?
		data.put(HikeConstants.METADATA, socketData);

		JSONObject message = new JSONObject();
		message.put(HikeConstants.TO, clientPartner.getPhoneNumber());
		message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE);
		message.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.VOIP_SOCKET_INFO);
		message.put(HikeConstants.DATA, data);
		
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, message);
		Log.d(VoIPActivity.logTag, "Sent socket information to partner.");
	}
	
	/**
	 * Once socket information for the partner has been received, this
	 * function should be called to establish and verify a UDP connection.
	 */
	public void establishConnection() {
		connectionEstablished = false;
		Log.d(VoIPActivity.logTag, "Trying to establish P2P connection..");
		Log.d(VoIPActivity.logTag, "Listening to local socket (for p2p) on port: " + socket.getLocalPort());
		
		// Sender thread
		final Thread senderThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					if (Thread.currentThread().isInterrupted())
						break;

					try {
						sendUDPData(VoIPConstants.COMM_UDP_SYN_PRIVATE.getBytes(), clientPartner.getInternalIPAddress(), clientPartner.getInternalPort());
						sendUDPData(VoIPConstants.COMM_UDP_SYN_PUBLIC.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
						sendUDPData(VoIPConstants.COMM_UDP_SYN_RELAY.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
						Thread.sleep(200);
					} catch (InterruptedException e) {
						Log.d(VoIPActivity.logTag, "Stopping sending thread.");
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
						Log.d(VoIPActivity.logTag, "Received: " + data);
						
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
						Log.d(VoIPActivity.logTag, "CS receiving exception: " + e.toString());
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
						Thread.sleep(500);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				senderThread.interrupt();
				receivingThread.interrupt();
				
				if (connectionEstablished == true) {
					Log.d(VoIPActivity.logTag, "UDP connection established :) " + clientPartner.getPreferredConnectionMethod());
					sendHandlerMessage(VoIPActivity.MSG_CONNECTION_ESTABLISHED);
					try {
						startStreaming();
					} catch (Exception e) {
						Log.e(VoIPActivity.logTag, "Exception: " + e.toString());
					}
				}
				else {
					Log.d(VoIPActivity.logTag, "UDP connection failure! :(");
					sendHandlerMessage(VoIPActivity.MSG_CONNECTION_FAILURE);
				}
				
			}
		}).start();

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
			
			IpAddress = ICEServerName;
			port = ICEServerPort;
			data = serializedData;
		}
		
		try {
			InetAddress address = InetAddress.getByName(IpAddress);
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
			socket.send(packet);
			// Log.d(VoIPActivity.logTag, "Sending to: " + IpAddress + ":" + port + ", data: " + new String(data, "UTF-8"));
		} catch (IOException e) {
			Log.d(VoIPActivity.logTag, "IOException: " + e.toString());
		}
	}	
	
	
}

