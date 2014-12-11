package com.bsb.hike.service;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.VOIP.WebRtcClient;
import com.bsb.hike.ui.VoIPActivityNew;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class VoIPServiceNew extends Service implements com.bsb.hike.VOIP.WebRtcClient.RTCListener {
	
	public WebRtcClient client;
	private String callerId;
	private String manufacturer;
	private Context mContext;
	private String dialedId;
	private int notifId = 10000;
	private NotificationManager mNotificationManager;
	private boolean rejectCall;
	public static VoIPServiceNew vService;
	public static boolean serviceStarted = false;
	private static boolean factoryStaticInitialized=false;
	private static int initialAudioMode;
	
	public boolean callConnected = false;
	private boolean onSpeakers = false;
	private boolean onMute = false;
	private boolean run = true;
	private PowerManager pm;
	private PowerManager.WakeLock wl;
	private boolean callNotified;

	public void onCreate() {
	    super.onCreate();
	    vService = this;
	    HikeService.voipServiceRunning = true;
	    Log.d("Vservice", "Creating");
	    initialAudioMode=((AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).getMode();
	}
	
	public void onConfigurationChanged(Configuration newConfig){
	    super.onConfigurationChanged(newConfig);
	}
	
	
	  @Override
	  public void onCallReady() {	  
		  if(callerId != null) {
			  try {
				  answer(callerId);
			  } catch (JSONException e) {
				  e.printStackTrace();
			  }
	      
		  } else {
			  call();
		  }
	  }
	  
	  public void answer(String callerId) throws JSONException{
		  startCam();
		  client.answerpressed = true;
		  try {
			  client.getMessageHandler().executeCommand(callerId, "init", null);
			  callConnected = true;
		  } catch (JSONException e) {
			  e.printStackTrace();
		  }
		  
	  }
	  
	  public void call(){
		  startCam();
	  }
	  
	  public void startCam(){
		  client.setAudio("front","1600", "700");
		  client.start("android_test3", true);
	  }
	  
	  public static VoIPServiceNew getVoIPSerivceInstance(){
			if (vService == null)
				Log.d("vService from service", "NULL HAI!!!");
		  return vService;
	  }
	  
	  @Override
	  public void onStatusChanged(final String newStatus){
		  if (newStatus == "DISCONNECTED"){
			  stopForeground(true);
			  if (mNotificationManager != null){
				  mNotificationManager.cancel(notifId);
			  }
//		  	VoIPActivityNew.getVoIPActivityInstance().raiseEndCallToast();
//			  VoIPActivityNew.getVoIPActivityInstance().finish();
			  client.mPubSub.removeListener(HikePubSub.VOIP_HANDSHAKE, client.getMessageHandler());
			  client.mPubSub.removeListener(HikePubSub.VOIP_TIMEOUT, client.getMessageHandler());
			  stopSelf();
		  }
	  }
	  
	  @Override
	  public void onLocalStream(MediaStream localStream){
		  localStream.audioTracks.get(0);
		  Log.d("Local Stream","got audio track");
	  }
	  
	  @Override
	  public void onAddRemoteStream(MediaStream remoteStream, int endPoint){

	  }
	  
	  @Override
	  public void onRemoveRemoteStream(MediaStream remoteStream, int endPoint){
		  remoteStream.audioTracks.get(0).dispose();

	  }
	  
	  public void endCall(){
		  declineCall();
		  stopService();		  
	  }
	  
	  public void stopService(){
		  Log.d("VOIPSERVICE","1");
		  client.destroyPeer();
		  Log.d("VOIPSERVICE","2");
		  Log.d("VOIPSERVICE","3");
		  
		  Log.d("VOIPSERVICE","4");
		  stopForeground(true);
		  if (mNotificationManager != null){
			  mNotificationManager.cancel(notifId);
		  }
		  Log.d("VOIPSERVICE","5");

		  if(!callNotified){
			  try {
					MqttMessagesManager.getInstance(getApplicationContext()).setVoipSystemMessage(client.createStateMessageJSON(), (callerId==null)?dialedId:callerId);
			  } catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			  }
			  callNotified = true;
		  }
		  
		  sendBroadcast(new Intent("FinishVoipActivities"));

		  Log.d("VOIPSERVICE","6");
		  HikeService.voipServiceRunning = false;		
		  stopSelf();
		  Log.d("VOIPSERVICE","7");

	  }
	  
	  public void declineCall(){
		  try {
			  if ( callConnected ){
				  if (callerId != null){
					  client.sendMessage(callerId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
				  }
				  else if (dialedId != null)
					  client.sendMessage(dialedId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
			  } else {
				  if (callerId != null){
					  client.sendMessage(callerId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
				  }
				  else if (dialedId != null)
					  client.sendMessage(dialedId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
			  }
		  } catch (JSONException e) {
			  e.printStackTrace();
		  }
	  }
	  
	  @Override
	  public int onStartCommand(Intent intent, int flags, int startId){
		  HikeService.voipServiceRunning = true;
		  vService = getInstance();
		  if(vService == null)
			  Log.d("Vservice", "not assigned");
		  serviceStarted =true;
		  initialAudioMode=((AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).getMode();
			
		  return START_NOT_STICKY;
	  }
	  
	  public void startUp(Intent intent){
		  HikeService.voipServiceRunning = true;
		  NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		  mBuilder.setSmallIcon(R.drawable.ic_hike_user);
		  mBuilder.setContentTitle("In Call");
		  pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		  wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WL for keeping service running");
		  wl.acquire();
		  
		  manufacturer = Build.MANUFACTURER;
		  mContext = getApplicationContext();
		  if(manufacturer.equalsIgnoreCase("samsung")||manufacturer.equalsIgnoreCase("karbonn"))
			  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_COMMUNICATION);
		  else
			  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_CALL);
		  
		  mContext = getApplicationContext();
		  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(false);
		  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setParameters("noise_suppression=off");
		  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMicrophoneMute(false);
		  if(!factoryStaticInitialized)
			  factoryStaticInitialized=PeerConnectionFactory.initializeAndroidGlobals(this, true, false, null);
		  client = new WebRtcClient(this);
		  Intent resultIntent = new Intent(this, com.bsb.hike.ui.VoIPActivityNew.class);
		  if(intent.getExtras() != null){
			  if(intent.hasExtra("callerID")){
				  callerId = intent.getStringExtra("callerID");
				  rejectCall = intent.getBooleanExtra("decline", false);
				  mBuilder.setContentText(callerId);
				  resultIntent.putExtra("resumeID", callerId);
				  try {
					  if (!rejectCall)
						  answer(callerId);
					  else
						  endCall();
				  } catch (JSONException e) {
					  e.printStackTrace();
				  }
			  }
			  else {
				  dialedId = intent.getStringExtra("dialedID");
				  mBuilder.setContentText(dialedId);
				  resultIntent.putExtra("resumeID", dialedId);
				  call();
			  }
		  }
		  resultIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
		  PendingIntent notifyIntent = 
				  PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT );
		  mBuilder.setContentIntent(notifyIntent);
		  mBuilder.setOngoing(true);
		  mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		  Notification notif = mBuilder.build();
		  startForeground(notifId, notif);
		  run  = false;
	  }
	  
	  public Boolean isConnected(){
		  return callConnected;
	  }

	@Override
	public void closeActivity() {
//		blah = 2;
		stopService();		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onDestroy(){
		Log.d("onDestroy", "Destroying");
		((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(initialAudioMode);
		wl.release();
		HikeService.voipServiceRunning = false;
		super.onDestroy();
	}
	
	public void startCall(Intent intent){
		  final Intent i = intent;

				  startUp(i);

	}
	
	public VoIPServiceNew getInstance(){
		Log.d("VSercice", "getinstance called");
		return this;
	}
	
	public void muteClicked(){
		if (onMute){
			((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMicrophoneMute(false);
			onMute = false;
		} else {
			((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMicrophoneMute(true);
			onMute = true;
		}
	}
	
	public void speakerClicked(){
		if (onSpeakers){
			((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(false);
			onSpeakers = false;
		} else {
			((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(true);
			onSpeakers = true;
		}
	}

	@Override
	public void onRemoveRemoteStream(MediaStream lMS) {
		// TODO Auto-generated method stub
		
	}

	public void notOnWifi() {
		Log.d("DialogBulderVoip", "Service");
		VoIPActivityNew.getVoIPActivityInstance().calleeNotOnWifi();
		
	}


}
