package com.bsb.hike.voip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.VoIPConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.VoIPActivity;
import com.bsb.hike.voip.VoIPClient.ConnectionMethods;
import com.bsb.hike.voip.VoIPDataPacket.PacketType;
import com.bsb.hike.voip.protobuf.VoIPSerializer;

public final class VoIPCaller {		// TODO: Remove class. 
	
	public static final String logTag = "VoIPCaller";
	
	private Context context = null;
	private static VoIPCaller instance = null;
	private static final String ICEServerName = "awaaz.anujjain.com";
	private static final int ICEServerPort = 9999;
	private Thread iceThread;
	
	VoIPClient clientSelf = new VoIPClient();
	VoIPClient clientPartner = new VoIPClient();
	
	private DatagramSocket outgoingUDPSocket;
	private boolean connectionEstablished = false;
	private String currentUDPMessage = VoIPConstants.COMM_UDP_SYN_ALL;

	public static VoIPCaller getInstance(Context context) {
		if (instance == null)
			instance = new VoIPCaller(context);
		return instance;
	}
	
	private VoIPCaller(Context c) {
		// Exists only to defeat instantiation
		this.context = c;
	}
	
	/**
	 * Initiates a VoIP call to a phone number.
	 * @param receiver
	 * @throws JSONException 
	 */
	public void initiateOutgoing(String receiver) throws JSONException {
		Log.d(logTag, "Initiating outgoing VoIP call.");
		clientPartner.setPhoneNumber(receiver);
		clientSelf.setInitiator(true);
		clientPartner.setInitiator(false);
		retrieveExternalSocket();
	}
	
	/**
	 * The client receiving the call request should call this function.
	 * @param jsonObj
	 * @throws JSONException
	 */
	public void initiateIncoming(JSONObject jsonObj) throws JSONException {
		clientPartner.setPhoneNumber(jsonObj.getString(HikeConstants.FROM));
		Log.d(logTag, "Detected incoming VoIP call from: " + clientPartner.getPhoneNumber());
		clientSelf.setInitiator(false);
		clientPartner.setInitiator(true);
		retrieveExternalSocket();
	}
	
	private void retrieveExternalSocket() {

		iceThread = new Thread(new Runnable() {

			@Override
			public void run() {

				byte[] receiveData = new byte[10240];
				
				try {
					InetAddress host = InetAddress.getByName(ICEServerName);
					outgoingUDPSocket = new DatagramSocket();
					outgoingUDPSocket.setReuseAddress(true);
					outgoingUDPSocket.setSoTimeout(2000);

					VoIPDataPacket dp = new VoIPDataPacket(PacketType.RELAY_INIT);
					byte[] dpData = VoIPSerializer.serialize(dp);
					DatagramPacket outgoingPacket = new DatagramPacket(dpData, dpData.length, host, ICEServerPort);
					DatagramPacket incomingPacket = new DatagramPacket(receiveData, receiveData.length);

					clientSelf.setInternalIPAddress(VoIPUtils.getLocalIpAddress(context)); 
					clientSelf.setInternalPort(outgoingUDPSocket.getLocalPort());
					
					boolean continueSending = true;
					int counter = 0;

					while (continueSending && counter < 10) {
						counter++;
						try {
							// Log.d(logTag, "ICE Sending: " + outgoingPacket.getData().toString() + " to " + host.getHostAddress() + ":" + ICEServerPort);
							outgoingUDPSocket.send(outgoingPacket);
							outgoingUDPSocket.receive(incomingPacket);
							
							String serverResponse = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
							Log.d(logTag, "ICE Received: " + serverResponse);
							setExternalSocketInfo(serverResponse);
							continueSending = false;
							
						} catch (SocketTimeoutException e) {
							Log.d(logTag, "UDP timeout on ICE.");
						} catch (IOException e) {
							Log.d(logTag, "IOException: " + e.toString());
						} catch (JSONException e) {
							Log.d(logTag, "JSONException: " + e.toString());
							continueSending = true;
						}
					}

					if (continueSending == true) {
						Log.d(logTag, "Unable to retrieve external socket.");
					}

				} catch (SocketException e) {
					Log.d(logTag, "SocketException: " + e.toString());
				} catch (UnknownHostException e) {
					Log.d(logTag, "UnknownHostException: " + e.toString());
				}
				
				if (haveExternalSocketInfo())
					try {
						sendSocketInfoToPartner();
						if (clientPartner.isInitiator())
							establishConnection();
					} catch (JSONException e) {
						Log.d(logTag, "JSONException: " + e.toString());
					}
				else
					Log.d(logTag, "Failed to retrieve external socket.");
				
			}
		});
		
		iceThread.start();
	}
	
	private void setExternalSocketInfo(String ICEResponse) throws JSONException {
		JSONObject jsonObject = new JSONObject(ICEResponse);
		clientSelf.setExternalIPAddress(jsonObject.getString("IP"));
		clientSelf.setExternalPort(Integer.parseInt(jsonObject.getString("Port")));
		Log.d(logTag, "External socket - " + clientSelf.getExternalIPAddress() + ":" + clientSelf.getExternalPort());
		Log.d(logTag, "Internal socket - " + clientSelf.getInternalIPAddress() + ":" + clientSelf.getInternalPort());
	}
	
	/**
	 * Set the partner's socket information. 
	 * (Internal and External IP Address, and port)
	 * @param jsonObject
	 */
	public void setPartnerInfo(JSONObject jsonObject) {
		
		try {
			JSONObject metadataJSON = jsonObject.getJSONObject(HikeConstants.DATA).getJSONObject(HikeConstants.METADATA);
			clientPartner.setInternalIPAddress(metadataJSON.getString("internalIP"));
			clientPartner.setInternalPort(Integer.parseInt(metadataJSON.getString("internalPort")));
			clientPartner.setExternalIPAddress(metadataJSON.getString("externalIP"));
			clientPartner.setExternalPort(Integer.parseInt(metadataJSON.getString("externalPort")));
			clientPartner.setInitiator(metadataJSON.getBoolean("initiator"));

			if (clientPartner.isInitiator())
				initiateIncoming(jsonObject);
			else
				establishConnection();
			
		} catch (JSONException e) {
			Log.d(logTag, "JSONException: " + e.toString() + " - " + jsonObject.toString());
		}
		
		Log.d(logTag, "Received partner socket information.");
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
			Log.e(logTag, "Have no partner info. Quitting.");
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
		Log.d(logTag, "Sent socket information to partner.");
	}
	
	private void sendUDPData(byte[] data, String IpAddress, int port) {
		try {
			InetAddress address = InetAddress.getByName(IpAddress);
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
			outgoingUDPSocket.send(packet);
			Log.d(logTag, "Sending to: " + IpAddress + ":" + port + ", data: " + new String(data, "UTF-8"));
		} catch (IOException e) {
			Log.d(logTag, "IOException: " + e.toString());
		}
	}
	
	/**
	 * Once socket information for the partner has been received, this
	 * function should be called to establish and verify a UDP connection.
	 */
	private void establishConnection() {
		connectionEstablished = false;
		Log.d(logTag, "Trying to establish P2P connection..");
		Log.d(logTag, "Listening to local socket (for p2p) on port: " + outgoingUDPSocket.getLocalPort());
		
		// Receiving thread
		final Thread receivingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					if (Thread.currentThread().isInterrupted())
						break;
					byte[] receiveData = new byte[10240];
					DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
					try {
						outgoingUDPSocket.setSoTimeout(0);
						outgoingUDPSocket.receive(packet);

						String data = new String(packet.getData(), 0, packet.getLength());
						Log.d(logTag, "Received: " + data);
						
						if (data.equals(VoIPConstants.COMM_UDP_SYN_PRIVATE)) {
							currentUDPMessage = VoIPConstants.COMM_UDP_SYNACK_PRIVATE;
						}
						
						if (data.equals(VoIPConstants.COMM_UDP_SYN_PUBLIC)) {
							currentUDPMessage = VoIPConstants.COMM_UDP_SYNACK_PUBLIC;
						}
						
						if (data.equals(VoIPConstants.COMM_UDP_SYNACK_PRIVATE) ||
								data.equals(VoIPConstants.COMM_UDP_ACK_PRIVATE)) {
							clientPartner.setPreferredConnectionMethod(ConnectionMethods.PRIVATE);
							connectionEstablished = true;
							currentUDPMessage = VoIPConstants.COMM_UDP_ACK_PRIVATE;
						}
						
						if (data.equals(VoIPConstants.COMM_UDP_SYNACK_PUBLIC) ||
								data.equals(VoIPConstants.COMM_UDP_ACK_PUBLIC)) {
							if (clientPartner.getPreferredConnectionMethod() != ConnectionMethods.PRIVATE) {	// Private interface takes priority
								clientPartner.setPreferredConnectionMethod(ConnectionMethods.PUBLIC);
								connectionEstablished = true;
								currentUDPMessage = VoIPConstants.COMM_UDP_ACK_PUBLIC;
							}
						}
					} catch (IOException e) {
						Log.d(logTag, "VoIPCaller Socket IOException: " + e.toString());
					}
				}
			}
		});
		
		receivingThread.start();

		// Sender thread
		final Thread senderThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					if (Thread.currentThread().isInterrupted())
						break;

					try {
						if (currentUDPMessage.equals(VoIPConstants.COMM_UDP_SYN_ALL)) {
							sendUDPData(VoIPConstants.COMM_UDP_SYN_PRIVATE.getBytes(), clientPartner.getInternalIPAddress(), clientPartner.getInternalPort());
							sendUDPData(VoIPConstants.COMM_UDP_SYN_PUBLIC.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
						}
						else if (currentUDPMessage.equals(VoIPConstants.COMM_UDP_SYNACK_PRIVATE) || currentUDPMessage.equals(VoIPConstants.COMM_UDP_SYNACK_PUBLIC)) {
							sendUDPData(currentUDPMessage.getBytes(), clientPartner.getInternalIPAddress(), clientPartner.getInternalPort());
							sendUDPData(currentUDPMessage.getBytes(), clientPartner.getExternalIPAddress(), clientPartner.getExternalPort());
						} else if (currentUDPMessage.equals(VoIPConstants.COMM_UDP_ACK_PRIVATE) ||
								currentUDPMessage.equals(VoIPConstants.COMM_UDP_ACK_PUBLIC)) {
							sendUDPData(currentUDPMessage.getBytes(), clientPartner.getPreferredIPAddress(),clientPartner.getPreferredPort());
							connectionEstablished = true;
							break;
						} else {
							sendUDPData(currentUDPMessage.getBytes(), clientPartner.getPreferredIPAddress(),clientPartner.getPreferredPort());
						}
						
						Thread.sleep(200);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		});
		
		currentUDPMessage = VoIPConstants.COMM_UDP_SYN_ALL;
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
					Log.d(logTag, "P2P UDP connection established :)");
					startActivity();
				}
				else
					Log.d(logTag, "P2P UDP connection failure! :(");
				
			}
		}).start();

	}

	private void startActivity() {
		
		Log.d(logTag, "Launching activity..");
		outgoingUDPSocket.close();
		
		Intent i = new Intent(context, VoIPActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		Bundle extras = new Bundle();
		extras.putSerializable("clientSelf", clientSelf);
		extras.putSerializable("clientPartner", clientPartner);
	
		i.putExtras(extras);
		context.startActivity(i);
	}

	
}
