package com.bsb.hike.service;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.VOIP.WebRtcClient;
import com.bsb.hike.service.VOIPService.VoIPBinder;
import com.bsb.hike.ui.VoIPActivity;
import com.bsb.hike.ui.VoIPActivityNew;
import com.bsb.hike.ui.VoIPActivity.MessageHandler;
import com.bsb.hike.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class VoIPServiceNew extends Service implements com.bsb.hike.VOIP.WebRtcClient.RTCListener {
	
	private WebRtcClient client;
	private String mSocketAddress;
	private String callerId;
	private int minbuf=AudioTrack.getMinBufferSize(16000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
//	private AudioTrack aud1=new AudioTrack(AudioManager.STREAM_VOICE_CALL,16000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,(int) (1*minbuf),AudioTrack.MODE_STREAM);
//	private AudioTrack aud2=new AudioTrack(AudioManager.STREAM_VOICE_CALL,16000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,(int) (1*minbuf),AudioTrack.MODE_STREAM);
	private AudioManager am1;
	private String manufacturer;
	private Context mContext;
	private Button endCall;
	private Button acceptCall; 
	private Button declineCall;
	private String dialedId;
	private int notifId = 10000;
	private NotificationManager mNotificationManager;
	private Boolean rejectCall;
	private TextView callNo;
	public static VoIPServiceNew vService;
	private MessageHandler mHandler = VoIPActivity.serviceHandler ;
	public static boolean serviceStarted = false;
	private static boolean factoryStaticInitialized=false;
	public static int blah = 1;
	
	public Boolean callConnected = false;
	private boolean run = true;
	
	public void onCreate() {
	    super.onCreate();
	    vService = this;
	    Log.d("Vservice", "Creating");
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
	//	      setAnswerLayout();
	      
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
//		  runOnUiThread(new Runnable() {
//			  @Override
//			  public void run() {
//				  Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
//			  }
//			});
//		  
	  }
	  
	  @Override
	  public void onLocalStream(MediaStream localStream){
		  localStream.audioTracks.get(0);
		  Log.d("Local Stream","got audio track");
	  }
	  
	  @Override
	  public void onAddRemoteStream(MediaStream remoteStream, int endPoint){
//		  if ( blah == 1){
//		  aud1.play();
//		  aud1.write(remoteStream.audioTracks.get(0).toString().getBytes(), 0, 2*minbuf);
//		  }
//		  else{
//			  aud2.play();
//			  aud2.write(remoteStream.audioTracks.get(0).toString().getBytes(), 0, 2*minbuf);
//		  }
//		  
	  }
	  
	  @Override
	  public void onRemoveRemoteStream(MediaStream remoteStream, int endPoint){
		  remoteStream.audioTracks.get(0).dispose();
		  if ( blah == 1){
//		  aud1.stop();
//		  aud1.release();
//		  }
//		  else{
//			  aud2.stop();
//			  aud2.release();
		  }
	  }
	  
	  public void endCall(){
		  declineCall();
		  blah = 2;
		  stopService();		  
	  }
	  
	  public void stopService(){
		  Log.d("VOIPSERVICE","1");
		  client.destroyPeer();
		  Log.d("VOIPSERVICE","2");
		  if (blah == 1){
//		  aud1.stop();
//		  aud1.release();}
//		  else {
//			  aud2.stop();
//			  aud2.release();
		  }
		  Log.d("VOIPSERVICE","3");
		  
		  Log.d("VOIPSERVICE","4");
		  stopForeground(true);
		  if (mNotificationManager != null){
			  mNotificationManager.cancel(notifId);
		  }
		  Log.d("VOIPSERVICE","5");
		  VoIPActivityNew.getVoIPActivityInstance().finish();
		  Log.d("VOIPSERVICE","6");
		  while(run)
			  Log.d("Oh", "Look at it go");
		  stopSelf();
		  Log.d("VOIPSERVICE","7");
//		  android.os.Process.killProcess(android.os.Process.myPid());
	  }
	  
	  public void declineCall(){
		  try {
			  if ( callConnected ){
				  if (callerId != null){
					  client.sendMessage(callerId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
				  }
				  else
					  client.sendMessage(dialedId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
			  } else {
				  if (callerId != null){
					  client.sendMessage(callerId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
				  }
				  else
					  client.sendMessage(dialedId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
			  }
		  } catch (JSONException e) {
			  e.printStackTrace();
		  }
	  }
	  
	  @Override
	  public int onStartCommand(Intent intent, int flags, int startId){
		  
		  vService = getInstance();
		  if(vService == null)
			  Log.d("Vservice", "not assigned");
		  serviceStarted =true;
			
		  return START_NOT_STICKY;
	  }
	  
	  public void startUp(Intent intent){
		  NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		  mBuilder.setSmallIcon(R.drawable.ic_hike_user);
		  mBuilder.setContentTitle("In Call");
		  
		  manufacturer = Build.MANUFACTURER;
		  mContext = getApplicationContext();
		  if(manufacturer.equalsIgnoreCase("samsung"))
			  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_COMMUNICATION);
		  else
			  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_CALL);
		  
		  mContext = getApplicationContext();
		  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(false);
		  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setParameters("noise_suppression=off");
		  ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMicrophoneMute(false);
		  if(!factoryStaticInitialized)
			  factoryStaticInitialized=PeerConnectionFactory.initializeAndroidGlobals(this);
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
		blah = 2;
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

	@Override
	public void onRemoveRemoteStream(MediaStream lMS) {
		// TODO Auto-generated method stub
		
	}


}
