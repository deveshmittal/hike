package com.bsb.hike.VOIP;


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.service.VoIPServiceNew;
import com.bsb.hike.utils.Logger;

public class WebRtcClient {
	private final static int MAX_PEER = 4;
	private boolean[] endPoints = new boolean[MAX_PEER];
	private PeerConnectionFactory factory;
	private HashMap<String, Peer> peers = new HashMap<String, Peer>();
	private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
	private MediaConstraints pcConstraints = new MediaConstraints();
	private MediaStream lMS;
	private RTCListener mListener;
	private final MessageHandler messageHandler = new MessageHandler();
	private final static String TAG = WebRtcClient.class.getSimpleName();
	private String storedId;
	private HikePubSub mPubSub;
	private Random random = new Random();
	private int index = random.nextInt(10000);
	public boolean answerpressed = false;
	private Boolean callReceived = false;
	private Handler mHandler = new Handler();
	public long startTime = 0;
	private Handler heartBeatHandler = new Handler();
	public String connectionState = "CHECKING";
	private boolean answering = true;
	public long callLength = 0;
	
	class HeartBeat implements Runnable{
		@Override
		public void run() {
			if (connectionState != "CONNECTED" ){
				((VoIPServiceNew)mListener).onStatusChanged("DISCONNECTED");				
			}
		}		
	};
	
	class HeartBeater implements Runnable{

		@Override
		public void run() {
			Looper.prepare();
			heartBeatHandler.postDelayed(new HeartBeat(), 30000);
		}
		
	}
	
	public interface RTCListener {
		void onCallReady();

		void onStatusChanged(String newStatus);

		void onLocalStream(MediaStream localStream);

		void onAddRemoteStream(MediaStream remoteStream, int endPoint);

		void onRemoveRemoteStream(MediaStream remoteStream, int endPoint);

		void endCall();

		void closeActivity();
		
		Boolean isConnected();

		void onRemoveRemoteStream(MediaStream lMS);
	}

	private interface Command {
		void execute(String peerId, JSONObject payload) throws JSONException;
	}

	private class CreateOfferCommand implements Command {
		public void execute(String peerId, JSONObject payload)
				throws JSONException {
			Peer peer = peers.get(peerId);
			peer.pc.createOffer(peer, pcConstraints);
			Thread handThread = new Thread(new HeartBeater());
			handThread.start();
			callReceived = true;
			answering = false;
		}
	}

	private class CreateAnswerCommand implements Command {
		public void execute(String peerId, JSONObject payload)
				throws JSONException {
			callReceived = true;
			Thread handThread = new Thread(new HeartBeater());
			handThread.start();
			Log.d(TAG, "CreateAnswerCommand");
			Log.d(TAG, "CreateAnswerCommand");
			Peer peer = peers.get(peerId);
			Log.d(TAG, "CreateAnswerCommand");
			SessionDescription sdp = new SessionDescription(
					SessionDescription.Type.fromCanonicalForm(payload
							.getString("type")), payload.getString("sdp"));
			Log.d(TAG, "CreateAnswerCommand");
			peer.pc.setRemoteDescription(peer, sdp);
			Log.d(TAG, "CreateAnswerCommand");
			peer.pc.createAnswer(peer, pcConstraints);
			Log.d(TAG, "CreateAnswerCommand");
		}
	}

	private class SetRemoteSDPCommand implements Command {
		public void execute(String peerId, JSONObject payload)
				throws JSONException {
//			Log.d(TAG, "SetRemoteSDPCommand");
			Peer peer = peers.get(peerId);
			SessionDescription sdp = new SessionDescription(
					SessionDescription.Type.fromCanonicalForm(payload
							.getString("type")), payload.getString("sdp"));
			peer.pc.setRemoteDescription(peer, sdp);
		}
	}

	private class AddIceCandidateCommand implements Command {
		public void execute(String peerId, JSONObject payload)
				throws JSONException {
//			Log.d(TAG, "AddIceCandidateCommand");
			PeerConnection pc = peers.get(peerId).pc;
			if (pc.getRemoteDescription() != null) {
//				if(payload.getString("candidate").contains("relay")){
					Log.d("Adding ICE",payload.getString("candidate") );
				IceCandidate candidate = new IceCandidate(
						payload.getString("id"), payload.getInt("label"),
						payload.getString("candidate"));
				pc.addIceCandidate(candidate);
			}
//				}
		}
	}
	
	private class EndCallCommand implements Command{
		public void execute(String peerId, JSONObject payload) throws JSONException{
			Log.d("ENDING CALL", "?????");
			mListener.closeActivity();
		}
	}

	public void sendMessage(String to, String type, JSONObject payload)
			throws JSONException {
		JSONObject message = new JSONObject();
		JSONObject voipSubPayload = new JSONObject();
		JSONObject data = new JSONObject();
		JSONObject metadata = new JSONObject();
		
		message.put(HikeConstants.TO, to);
		message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE);
		message.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.VOIP_HANDSHAKE);
		data.put(HikeConstants.MESSAGE_ID, ++index);
		voipSubPayload.put("payload", payload);
		long time = (long) System.currentTimeMillis();
		data.put(HikeConstants.TIMESTAMP, time);
		metadata.put("type", type);
		
		metadata.put(HikeConstants.VOIP_PAYLOAD, voipSubPayload);
		data.put("md", metadata);
		data.put("hm", "you shouldn't see it as call. catch aaryaman@hike.in");
		message.put(HikeConstants.DATA, data);
		
//		mPubSub.publish(HikePubSub.VOIP_HANDSHAKE_SENT, message);
		if( !(message.toString().contains("recvonly")) )
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, message);
		
		Log.d("Sent", message.toString());
	}
	
	public class MessageHandler implements Listener {
		private HashMap<String, Command> commandMap;

		public MessageHandler() {
			mPubSub = HikeMessengerApp.getPubSub();
			this.commandMap = new HashMap<String, Command>();
			commandMap.put("init", new CreateOfferCommand());
			commandMap.put("offer", new CreateAnswerCommand());
			commandMap.put("answer", new SetRemoteSDPCommand());
			commandMap.put("candidate", new AddIceCandidateCommand());
			commandMap.put(HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, new EndCallCommand());
			commandMap.put(HikeConstants.MqttMessageTypes.VOIP_END_CALL, new EndCallCommand());
			
		//	DONE: change HikePubSub.MESSAGE_SENT to correct PubSubHandler
			mPubSub.addListener(HikePubSub.VOIP_HANDSHAKE, this);
			mPubSub.addListener(HikePubSub.VOIP_TIMEOUT, this);

		}

	//	DONE: change override to corresponding override from PubSub onEventReceived
		@Override
		public void onEventReceived(String type, Object object) {
			//if(HikePubSub.VOIP_HANDSHAKE.equals(type)) {
			Log.d("HELLO", "1");
			try {
				Log.d("HELLO", "1");
				JSONObject payload = (JSONObject) object;
				Log.d("HELLO", "2");
				String from = payload.getString(HikeConstants.FROM);
				Log.d("HELLO", "3");
				JSONObject data = payload.getJSONObject(HikeConstants.DATA);
				Log.d("HELLO", "4");
				JSONObject metadata=data.getJSONObject(HikeConstants.METADATA);
				Log.d("HELLO", "5");
				JSONObject voipPayload = metadata.getJSONObject(HikeConstants.VOIP_PAYLOAD);
				Log.d("HELLO", "6");
				String payload_type = metadata.getString("type");
				Log.d("HELLO", "7");
				JSONObject voipSubPayload = null;
				Log.d("HELLO", "8");
				if (!((payload_type.equals("init") || (payload_type.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE))) )) {
					voipSubPayload = voipPayload.getJSONObject("payload");
					Log.d("HELLO", "9");
				}
				executeCommand(from,payload_type,voipSubPayload);
				Log.d("HELLO", "10");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		//}
		public void executeCommand(String from, String payloadType, JSONObject voipSubPayload) throws JSONException
		{
			// if peer is unknown, try to add him
			Log.d("Hii", "1");
			if (!peers.containsKey(from)) {
				// if MAX_PEER is reach, ignore the call
				Log.d("Hii", "2");
				int endPoint = findEndPoint();
				Log.d("Hii", "3");
				if (endPoint != MAX_PEER) {
					Log.d("Hii", "4");
					storedId = from;
					Log.d("Hii", "5");
					addPeer(from, endPoint);
					Log.d("Hii", "6");
	
					commandMap.get(payloadType).execute(from, voipSubPayload);
					Log.d("Hii", "7");
				}
			} else {
				Log.d("Hii", "8");
				commandMap.get(payloadType).execute(from, voipSubPayload);
			}
		}
	}

	private class Peer implements SdpObserver, PeerConnection.Observer {
		private PeerConnection pc;
		private String id;
		private int endPoint;
		private SessionDescription sdown;

		@Override
		public void onCreateSuccess(final SessionDescription sdp) {
			try {
				JSONObject payload = new JSONObject();
				payload.put("type", sdp.type.canonicalForm());
				payload.put("sdp", sdp.description);
				sendMessage(id, sdp.type.canonicalForm(), payload);
				Log.d("PeerSent", payload.toString());
				pc.setLocalDescription(Peer.this, sdp);
				sdown = sdp;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onSetSuccess() {
		}

		public SessionDescription getSDP() {
			return sdown;
		}

		@Override
		public void onCreateFailure(String s) {
			Logger.d(TAG, "create failure");
		}

		@Override
		public void onSetFailure(String s) {
		}

		@Override
		public void onSignalingChange(
				PeerConnection.SignalingState signalingState) {
		}

		@Override
		public void onIceConnectionChange(
				PeerConnection.IceConnectionState iceConnectionState) {
			connectionState = iceConnectionState.toString();
			Logger.d(TAG, "on ice connection  changed above");
			Logger.d(TAG, "on ice connection chnaged state : " + iceConnectionState.toString());
			HikeMessengerApp.getPubSub().publish(HikePubSub.VOIP_CALL_STATUS_CHANGED, iceConnectionState.toString());
			if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
				Logger.d(TAG, "on ice connection  changed");
				removePeer(id);
				mListener.onStatusChanged("DISCONNECTED");
				
			}
			if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED){
				
//				startTime = System.currentTimeMillis();
//				callTimeHandler.post(new CallLengthCalculator());
			}
		}

		@Override
		public void onIceGatheringChange(
				PeerConnection.IceGatheringState iceGatheringState) {
		}

		@Override
		public void onIceCandidate(final IceCandidate candidate) {
			try {
				JSONObject payload = new JSONObject();
				payload.put("label", candidate.sdpMLineIndex);
				payload.put("id", candidate.sdpMid);
				payload.put("candidate", candidate.sdp);
				sendMessage(id, "candidate", payload);
				Log.d("IceCandidateSent", payload.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onError() {
			Logger.d(TAG, "error ocuured");
		}

		@Override
		public void onAddStream(MediaStream mediaStream) {
			Log.d(TAG,"onAddStream " + mediaStream.label());

			// remote streams are displayed from 1 to MAX_PEER (0 is
			// localStream)
			mListener.onAddRemoteStream(mediaStream, endPoint + 1);
		}

		@Override
		public void onRemoveStream(MediaStream mediaStream) {
			mListener.onRemoveRemoteStream(mediaStream, endPoint);
			Logger.d(TAG, "removed stream");
			removePeer(id);
		}

		@Override
		public void onDataChannel(DataChannel dataChannel) {
		}

		public Peer(String id, int endPoint) {
			Log.d(TAG, "new Peer: " + id + " " + endPoint);
			Log.d("NewPeer", "1");
			if(factory == null)
			{
				Logger.d("NewPeer", "factoey is null");
				factory = new PeerConnectionFactory();
			}
			if(iceServers == null)
			{
				Logger.d("NewPeer", "iceservers is null");
			}
			if(pcConstraints == null)
			{
				Logger.d("NewPeer", "pcconstrations is null");
			}
			pc = factory.createPeerConnection(iceServers, pcConstraints,
					this);
			Log.d("NewPeer", "2");
			this.id = id;
			Log.d("NewPeer", "3");
			this.endPoint = endPoint;
			Log.d("NewPeer", "4");
			if(lMS==null)
				Log.d("NewPeer", "lmsNul");
			MediaConstraints mediaConstraints = new MediaConstraints();
			Log.d("NewPeer", "lms=" + " pc=" );
			try
			{
				if(!pc.addStream(lMS, mediaConstraints))
				{
					Logger.d("NewPeer", "addstream to peer failed");
				}
				else
				{
					Logger.d("NewPeer", "addstream to peer sucessful");
				}
			}
			catch(Exception e)
			{
				Logger.e("NewPeer","exception occured : ", e);
			}
			Log.d("CrashLine","asd");
			mListener.onStatusChanged("CONNECTING");
			Log.d("NewPeer", "6");
		}

		public void onRenegotiationNeeded() {			
		}
	}


//	DONE: dont need to connect to host. Only add the ice candidates. Removed arg "String:host"
	public WebRtcClient(RTCListener listener) {
		Logger.d("NewPeer", "initialising webtc client");
		mListener = listener;
		factory = new PeerConnectionFactory();
		long sysTime = System.currentTimeMillis();
		
		Runnable checkCallReceived = new Runnable(){

			@Override
			public void run() {
				if (callReceived == false){
//					try {
//						sendMessage(storedId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
//					} catch (JSONException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					mListener.closeActivity();
				}
				
			}
			
		};
		
		mHandler.removeCallbacks(checkCallReceived);
        mHandler.postDelayed(checkCallReceived, 30000);

	
		iceServers.add(new PeerConnection.IceServer(
				"stun:stun.l.google.com:19302"));
//		iceServers.add(new PeerConnection.IceServer(
//				"stun:stun.voipstunt.com"));
//		iceServers.add(new PeerConnection.IceServer(
//				"turn:numb.viagenie.ca:3478","anub018@gmail.com","123456"));

		iceServers.add(new PeerConnection.IceServer(
				"turn:54.179.186.147:3478","hike","123456"));
		
//		iceServers.add(new PeerConnection.IceServer(
//				"turn:54.255.154.209:3478","hike","123456"));
		
		pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
				"OfferToReceiveAudio", "true"));
		pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
				"OfferToReceiveVideo", "false"));
	}

	public void setAudio(String cameraFacing, String height, String width) {
		Random rand = new Random();
		Integer x = rand.nextInt();
		lMS = factory.createLocalMediaStream("ARDAMS"+x);
		Log.d("libjingle StreamName",lMS.label());
		lMS.addTrack(factory.createAudioTrack("ARDAMS"+x+"a0"/*, factory.createAudioSource(new MediaConstraints())*/));
		Log.d("camera added", "cameraadded");

		mListener.onLocalStream(lMS);
	}

	private int findEndPoint() {
		for (int i = 0; i < MAX_PEER; i++) {
			if (!endPoints[i])
				return i;
		}
		return MAX_PEER;
	}

	public void start(String name, boolean privacy) {
	}

	private void addPeer(String id, int endPoint) {
		Log.d("PEER","1");
		Peer peer = new Peer(id, endPoint);
		Log.d("PEER","2");
		peers.put(id, peer);
		endPoints[endPoint] = true;
	}

	private void removePeer(String id) {
		Log.d("NewPeer", "remove peer caled");
		if(peers.get(id) != null)
		{
		Peer peer = peers.get(id);
		try {
			peer.pc.removeStream(lMS);
			peer.pc.close();
			peer.pc.dispose();
			peer.pc = null; 
			lMS.dispose();
			lMS = null;
		} catch (NullPointerException e) {
			Log.d("WebRTCClient", "THIS SHOULD NEVER HAPPEN!");
			e.printStackTrace();
		}
		peers.remove(peer.id);
		Log.d("NewPeer","disposing video");

		endPoints[peer.endPoint] = false;
		}
	}

	public void destroyPeer()
	{
		Set<String>keys= peers.keySet();
		Iterator<String> itr = keys.iterator();
		while(itr.hasNext()){			
			removePeer(itr.next());
		}
	}

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}
	
}
