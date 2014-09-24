package com.bsb.hike.VOIP;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.Button;
import android.widget.Toast;
import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoRenderer;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.voiceengine.*;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.view.CustomFontEditText;
import com.bsb.hike.view.CustomFontEditText.BackKeyListener;

import java.util.List;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

// DONE: Impement pubsublistener
public class RTCActivity extends Activity implements WebRtcClient.RTCListener{
  private final static int VIDEO_CALL_SENT = 666;
//  private VideoStreamsView vsv;
  private WebRtcClient client;
  private String mSocketAddress;
  private String callerId;
  private int minbuf=AudioTrack.getMinBufferSize(16000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
  private AudioTrack aud1=new AudioTrack(AudioManager.STREAM_VOICE_CALL,16000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,(int) (1*minbuf),AudioTrack.MODE_STREAM);
  private AudioManager am1;
  String manufacturer;
  private Context mContext;
  private Button endCall;
  private Button acceptCall;
  private Button declineCall;
  private String dialedId;
  private int notifId = 0;
  NotificationManager mNotificationManager;
  
  private TextView callNo;
  //  private WebRTCAudioDevice aud1;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    startUp();
  }

  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  }

  @Override
  public void onPause() {
    super.onPause();
//    vsv.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
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
      setAnswerLayout();
      
    } else {
      call();
    }
  }

  public void startUp()
  {
    mContext=getApplicationContext();
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    
    mBuilder.setSmallIcon(R.drawable.ic_hike_user);
    mBuilder.setContentTitle("In Call!");
    mBuilder.setOngoing(true);
/*    Intent resultIntent = new Intent(this, RTCActivity.class);
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addParentStack(RTCActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent =
            stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
            );
    mBuilder.setContentIntent(resultPendingIntent);
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);*/
    
    Intent resultIntent = new Intent(this, RTCActivity.class);
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    // Adds the back stack
    stackBuilder.addParentStack(RTCActivity.class);
    // Adds the Intent to the top of the stack
    stackBuilder.addNextIntent(resultIntent);
    // Gets a PendingIntent containing the entire back stack
    PendingIntent resultPendingIntent =
            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

//    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    mBuilder.setContentIntent(resultPendingIntent);
    mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	mNotificationManager.notify(notifId, mBuilder.build());
    




    
    
    manufacturer = Build.MANUFACTURER;
    mContext = getApplicationContext();
    if(manufacturer.equalsIgnoreCase("samsung"))
        ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_COMMUNICATION);
    else
        ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_IN_CALL);
    
    ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(false);
    ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setParameters("noise_suppression=off");
    ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).setMicrophoneMute(false);

    requestWindowFeature(Window.FEATURE_NO_TITLE);

    mSocketAddress = "http://54.179.186.147/";
    Log.d("DEBUG", "Before initializeAndroidGlobals()");
    PeerConnectionFactory.initializeAndroidGlobals(this);
//   Set phone to earpiece
   

    // Camera display view
    Point displaySize = new Point();

//  TODO: Change constructor according to new one created
    client = new WebRtcClient(this);

/*    final Intent intent = getIntent();
    final String action = intent.getAction();

    if (Intent.ACTION_VIEW.equals(action)) {
      final List<String> segments = intent.getData().getPathSegments(); */

//  TODO: PASS THE CORRECT INTENT FROM THE HIKE SERVICE TO CALL SOMEONE WITH THE PASSED CALLER ID
    Log.d("DEBUG", "Before getting intents");
      Bundle extras = getIntent().getExtras();
      if (this.getIntent().hasExtra("callerID")) {
        callerId = extras.getString("callerID");
        mBuilder.setContentText(callerId);
        try {
        	Log.d("VOIP STARTUP", "CALLERID:" + callerId);
			answer(callerId);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Log.d("MyID",callerId );
        setAnswerLayout();
      }
      else{
    	  dialedId = extras.getString("dialedID");
    	  Log.d("VOIP STARTUP", "NO CALLERID");
    	  mBuilder.setContentText(dialedId);
    	  call();    
      }
  }

/* This method is called when we know that the activity has been called by the hike sevice and not by clicking on the call button
 * DONE: Ensure proper calling
 *
 */
  public void answer(String callerId) throws JSONException {
	  //client.sendMessage(callerId,"candidate",)
//    client.sendMessage(callerId, "init", null);
	startCam();
    Log.d("Sent", callerId);

  }

//DONE: removed arg from method
  public void call() {
  //  DONE: get rid of everything other than startcam()
/*    Intent msg = new Intent(Intent.ACTION_SEND);
    msg.putExtra(Intent.EXTRA_TEXT, mSocketAddress + callId);
    msg.setType("text/plain");
    msg.setPackage("com.bsb.hike");

    
    Toast.makeText(getBaseContext(),"Share your Call Link with Someone", Toast.LENGTH_LONG).show();
//    startActivityForResult(msg, VIDEO_CALL_SENT);
    startActivity(msg); */
	  Log.d("DEBUG", "Before startCam()");
    startCam();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == VIDEO_CALL_SENT) {
//    	if(resultCode == Activity.RESULT_OK)
    	{
    			startCam();
//    			finishActivity(VIDEO_CALL_SENT);
    	}
    }
  }

  public void startCam() {
	if(callerId == null )  
		setInCallLayout();
	else
		setAnswerLayout();
    client.setAudio("front", "1600", "700");
    client.start("android_test3", true);
    Log.d("DEBUG", "CamStarted");
  }
  
  public void setInCallLayout()
  {
	  setContentView(R.layout.incall_layout);
	  endCall = (Button)this.findViewById(R.id.endCallButton);
	  endCall.setBackgroundColor(Color.RED);
	  endCall.setOnClickListener(new OnClickListener() {
	    @Override
	    public void onClick(View v) {
	    	endCall();
	    }

	  });
  }

  @Override
  public void onStatusChanged(final String newStatus) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
      }
    });
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

  public void run()
  {
    startUp();
  }
  
  @Override
  public void endCall()
  {
	mNotificationManager.cancel(notifId);
	declineCall();
	closeActivity();
  }
  
  @Override
  public void closeActivity() {
	  client.destroyPeer();
	  aud1.stop();
	  aud1.release();
	  finish();
	  android.os.Process.killProcess(android.os.Process.myPid());
	
}

public void declineCall()
  {
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
  
  public void setAnswerLayout()
  {
	  setContentView(R.layout.call_accept_decline);
      callNo = (TextView)this.findViewById(R.id.CallerId);
      callNo.setText(callerId);
      acceptCall = (Button)this.findViewById(R.id.acceptButton);
      acceptCall.setBackgroundColor(Color.GREEN);
      acceptCall.setTextColor(Color.WHITE);
      
      declineCall = (Button)this.findViewById(R.id.declineButton);
      declineCall.setBackgroundColor(Color.RED);
      declineCall.setTextColor(Color.WHITE);
      acceptCall.setOnClickListener(new OnClickListener(){

		@Override
		public void onClick(View v) {
			client.answerpressed = true;
			try {
				client.getMessageHandler().executeCommand(callerId,"init",null);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setInCallLayout();
		}
    	  
      });
      
      declineCall.setOnClickListener(new OnClickListener(){

		@Override
		public void onClick(View v) {
			declineCall();
			finish();
		}
    	  
      });
  }
  
  @Override
  public void onBackPressed()
  {
	  super.onBackPressed();
  }

@Override
public Boolean isConnected() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void onRemoveRemoteStream(MediaStream lMS) {
	// TODO Auto-generated method stub
	
}


} 