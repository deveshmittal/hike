package com.bsb.hike.service;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.VOIP.RTCActivity;
import com.bsb.hike.VOIP.WebRtcClient;
import com.bsb.hike.ui.VoIPActivity;
import com.bsb.hike.ui.VoIPActivity.MessageHandler;



public class VOIPService extends Service implements com.bsb.hike.VOIP.WebRtcClient.RTCListener {

	private final static int VIDEO_CALL_SENT = 666;
//  private VideoStreamsView vsv;
	private WebRtcClient client;
	private String mSocketAddress;
	private String callerId;
	private int minbuf=AudioTrack.getMinBufferSize(16000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
	private AudioTrack aud1=new AudioTrack(AudioManager.STREAM_VOICE_CALL,16000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,(int) (1*minbuf),AudioTrack.MODE_STREAM);
	private AudioManager am1;
	private String manufacturer;
	private Context mContext;
	private Button endCall;
	private Button acceptCall;
	private Button declineCall;
	private String dialedId;
	private int notifId = 0;
	private NotificationManager mNotificationManager;
	private Boolean rejectCall;
	private TextView callNo;
	private final VoIPBinder binder = new VoIPBinder();
	private MessageHandler mHandler = VoIPActivity.serviceHandler ;
	
	public Boolean callConnected = false;
  //  private WebRTCAudioDevice aud1;
  
  public void onCreate() {
    super.onCreate();    
  }

  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
//    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  public void onPause() {
//    super.onPause();
//    vsv.onPause();
  }

  public void onResume() {
//    super.onResume();
//    vsv.onResume();
  }

// DONE: Removed arg "String:callId to match overridden function"
// DONE: removed arg "String:callId" from method call()
  @Override
  public void onCallReady() {
	  
    if(callerId != null) {
      try {
        answer(callerId);
      } catch (JSONException e) {
        e.printStackTrace();
      }
//      setAnswerLayout();
      
    } else {
      call();
    }
  }

  public void startUp(Intent intent)
  {
	Log.d("VOIPService", "Starting UP!");
    mContext=getApplicationContext();
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    
    mBuilder.setSmallIcon(R.drawable.ic_hike_user);
    mBuilder.setContentTitle("In Call!");
//    mBuilder.setOngoing(true);
    manufacturer = Build.MANUFACTURER;
    mContext = getApplicationContext();
    if(manufacturer.equalsIgnoreCase("samsung"))
        ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_COMMUNICATION);
    else
        ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_CALL);
    
    ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(false);
    ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setParameters("noise_suppression=off");
    ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMicrophoneMute(false);

//    requestWindowFeature(Window.FEATURE_NO_TITLE);

    mSocketAddress = "http://54.179.186.147/";
    Log.d("DEBUG", "Before initializeAndroidGlobals()");
    PeerConnectionFactory.initializeAndroidGlobals(this);
//   Set phone to earpiece
   

    // Camera display view
//    Point displaySize = new Point();

//  TODO: Change constructor according to new one created
    client = new WebRtcClient(this);


//  TODO: PASS THE CORRECT INTENT FROM THE HIKE SERVICE TO CALL SOMEONE WITH THE PASSED CALLER ID
    Log.d("DEBUG", "Before getting intents");
    Intent resultIntent = new Intent(this, com.bsb.hike.ui.VoIPActivity.class);
    if(intent.getExtras() != null);{
      Bundle extras = intent.getExtras();
      if (intent.hasExtra("callerID")) {
        callerId = extras.getString("callerID");
        rejectCall = extras.getBoolean("decline");
        mBuilder.setContentText(callerId);
        resultIntent.putExtra("resumeID", callerId);
        try {
        	Log.d("VOIP STARTUP", "CALLERID:" + callerId);
			if (!rejectCall)
				answer(callerId);
			else
				endCall();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Log.d("MyID",callerId );
//        setAnswerLayout();
      }
      else{
    	  dialedId = extras.getString("dialedID");
    	  Log.d("VOIP STARTUP", "NO CALLERID");
    	  mBuilder.setContentText(dialedId);
    	  resultIntent.putExtra("resumeID", dialedId);
    	  call();    
      }
    }
      resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      PendingIntent notifyIntent =
    	        PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      mBuilder.setContentIntent(notifyIntent);
      mBuilder.setOngoing(true);
      mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      Notification notif = mBuilder.build();
      mNotificationManager.notify(notifId,notif);
      startForeground(notifId, notif);


//      Intent notifyIntent = new Intent(new ComponentName(this, ResultActivity.class));

/*      TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
      // Adds the back stack
      stackBuilder.addParentStack(com.bsb.hike.ui.VoIPActivity.class);
      // Adds the Intent to the top of the stack
      stackBuilder.addNextIntent(resultIntent);*/
      // Gets a PendingIntent containing the entire back stack
/*      PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
      mBuilder.setContentIntent(resultPendingIntent);
      
      mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      
      mNotificationManager.notify(notifId,notif);
      */
      
      
  }

/* This method is called when we know that the activity has been called by the hike sevice and not by clicking on the call button
 * DONE: Ensure proper calling
 *
 */
  public void answer(String callerId) throws JSONException {
	startCam();
	client.answerpressed = true;
	try {
		client.getMessageHandler().executeCommand(callerId,"init",null);
		callConnected=true;
	} catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    Log.d("Sent", callerId);

  }

//DONE: removed arg from method
  public void call() {
  //  DONE: get rid of everything other than startcam()

	  Log.d("DEBUG", "Before startCam()");
    startCam();
  }



  public void startCam() {

    client.setAudio("front", "1600", "700");
    client.start("android_test3", true);
    Log.d("DEBUG", "CamStarted");
  }
  


  @Override
  public void onStatusChanged(final String newStatus) {
//    runOnUiThread(new Runnable() {
//      @Override
//      public void run() {
//        Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
//      }
//    });
  }
  
  MediaStream medi;
  @Override
  public void onLocalStream(MediaStream localStream) {
	  medi=localStream;
	  localStream.audioTracks.get(0);
  }

  @Override
  public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {

	  aud1.play();
	  aud1.write(remoteStream.audioTracks.get(0).toString().getBytes(), 0, 2*minbuf);

  }

  @Override
  public void onRemoveRemoteStream(MediaStream remoteStream, int endPoint) {

	  remoteStream.audioTracks.get(0).dispose();
	  aud1.stop();
	  aud1.release();
  }
  
  @Override
  public void endCall()
  {
	Log.d("VOIPService", "endCall called");
	declineCall();
	closeActivity();
	
  }
  
  @Override
  public void closeActivity() {
  	client.destroyPeer();
  	aud1.stop();
  	aud1.release();
  	Message msg = new Message();
  	msg.what = 0;
	mHandler.sendMessage(msg);
	stopForeground(true);
	if (mNotificationManager != null)
		mNotificationManager.cancelAll();
	stopSelf();
	onDestroy();
//	android.os.Process.killProcess(android.os.Process.myPid());
//	  finish();
//	  android.os.Process.killProcess(android.os.Process.myPid());
	
}

public void declineCall()
  {
	if ( callConnected ){
	  Log.d("VOIPService", "declinceCall");
	  if(callerId != null)
		try {
			client.sendMessage(callerId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  else
		try {
			client.sendMessage(dialedId, HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE, null);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	else{
		if(callerId != null)
			try {
				client.sendMessage(callerId, HikeConstants.MqttMessageTypes.VOIP_END_CALL, null);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  else
			try {
				client.sendMessage(dialedId, HikeConstants.MqttMessageTypes.VOIP_END_CALL, null);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
  }

	
	@Override
	public IBinder onBind(Intent intent) {
		
		return binder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		Log.d("VOIPService", "Service started!");
		final Intent i = intent;
		class worker implements Runnable{

			@Override
			public void run() {
				Looper.prepare();
				startUp(i);				
			}
			
		}
//		worker w = new worker();
		Thread t = new Thread(new worker(),"thread");
		t.start();
		return START_STICKY;		
	}
	
	@Override
	public void onDestroy(){
		Log.d("VOIPService","destroying");
		super.onDestroy();
	}
	
	public class VoIPBinder extends Binder{
		public VOIPService getService(){
			return VOIPService.this;
		}
		
	}

	@Override
	public Boolean isConnected() {
		// TODO Auto-generated method stub
		return callConnected;
	}
	
	public void onRemoveRemoteStream(MediaStream remoteStream) {

		  remoteStream.audioTracks.get(0).dispose();
		  aud1.stop();
		  aud1.release();
	  }

}
