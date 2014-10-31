package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.service.VoIPServiceNew;
//import com.bsb.hike.db.HikeUserDatabase;

public class VoIPActivityNew extends Activity implements HikePubSub.Listener{

	private String callerId;
	private String dialedId;
	private ImageButton endCall;
	private ImageButton acceptCall;
	private ImageButton declineCall;
	private ImageButton speakerButton;
	private ImageButton muteButton;
	private ImageView speakerSound;
	private ImageView micSlash;
	private TextView callNo;
	private TextView inCallCallNo;
	private TextView inCallTimer;	
	private HikePubSub mPubSub = HikeMessengerApp.getPubSub();
	private String resumeId = null;
	private VoIPServiceNew vService;
	private static VoIPActivityNew vActivity;
	public boolean callConnected = false;
	public boolean isMute = false;
	public boolean isSpeakerOn = false;
	MediaPlayer mMediaPlayer = new MediaPlayer();
	private String storedId;
	private Handler displayHandler = new Handler();
	private long startTime = 0;
	private long callLength = 0;
	private boolean isPlaying = false;
	Uri notification;
	private boolean sensorDisabled = false;
	private static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
	private PowerManager pm;
	private WakeLock screenOffLock;
	private Ringtone r;
	private AudioManager am;
	OnAudioFocusChangeListener afChangeListener = null;
	
	class CallLengthManager implements Runnable{

		@Override
		public void run() {
			callLength = System.currentTimeMillis() - startTime ;
			int seconds = (int) (callLength / 1000);
		    int minutes = seconds / 60;
		    int hours = minutes/60;
		    seconds = seconds % 60;
		    minutes = minutes % 60;
		    inCallTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
		    displayHandler.postDelayed(new CallLengthManager(), 500);
			
		}
		
	};
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		vActivity = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mPubSub.addListener(HikePubSub.VOIP_HANDSHAKE, this);
		mPubSub.addListener(HikePubSub.VOIP_CALL_STATUS_CHANGED, this);
		mPubSub.addListener(HikePubSub.VOIP_DURATION, this);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//		mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
//		sensorMaxRange = mProximity.getMaximumRange();
		am = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if(getIntent().hasExtra("callerID")){
			callerId = getIntent().getStringExtra("callerID");
			storedId = callerId;
			prepareAnswer();
		} else if (getIntent().hasExtra("dialedID")){
			dialedId = getIntent().getStringExtra("dialedID");
			storedId = dialedId;
			prepareInCall();
		} else {
			resumeId = getIntent().getStringExtra("resumeID");
			storedId = resumeId;
			Log.d("STOREDID", storedId);
			prepareResume();			
		}
	}
	
	public void prepareAnswer(){
		final Intent i = new Intent(this,com.bsb.hike.service.VoIPServiceNew.class);
		i.putExtras(getIntent().getExtras());
//		mMediaPlayer = new MediaPlayer();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
				notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		r.setStreamType(AudioManager.STREAM_ALARM);
		r.play();
//		am.requestAudioFocus(afChangeListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);

//		mMediaPlayer = MediaPlayer.create(this, R.raw.hike_jingle_15);
//		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
//		mMediaPlayer.setLooping(true);
//		final MediaPlayer player = mMediaPlayer; 
//		player.start();
		Log.w("Audio Starting", "Audio Starting");
//		mMediaPlayer.start();
		isPlaying = true;
		setContentView(R.layout.call_accept_decline);
		vActivity = this;
		callNo = (TextView)this.findViewById(R.id.CallerId);
//		callNo.setText(HikeUserDatabase.getInstance().getContactInfoFromPhoneNo(callerId).getNameOrMsisdn());
		callNo.setText("Incoming Number Goes Here!");
		acceptCall = (ImageButton)this.findViewById(R.id.acceptButton);

		acceptCall.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
//				player.stop();
				Intent intent = i;
				intent.putExtra("decline", false);
				callConnected = true;				
				vService = VoIPServiceNew.getVoIPSerivceInstance();
				vService.startCall(intent);
				drawInCall();
			}
			
		});
		
		declineCall = (ImageButton)this.findViewById(R.id.declineButton);

		declineCall.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
//				player.stop();
				Intent intent = i;
				intent.putExtra("decline", true);
				callConnected = true;
				vService = VoIPServiceNew.getVoIPSerivceInstance();
				vService.startCall(intent);
				finish();
			}
			
		});
	}
	
	public void prepareInCall(){
		final Intent i = new Intent(this, com.bsb.hike.service.VoIPServiceNew.class);		
		i.putExtras(getIntent().getExtras());
		callConnected = true;
		vActivity = this;
		vService = VoIPServiceNew.getVoIPSerivceInstance();
		if (VoIPServiceNew.vService == null)
			Log.d("vService", "NULL HAI!!!");
		vService.startCall(i);

		drawInCall();
	}
	
	public void prepareResume(){
		vService = VoIPServiceNew.getVoIPSerivceInstance();
		drawInCall();
	}
	
	public void drawInCall(){
		if (isPlaying){
//			mMediaPlayer.stop();
//			mMediaPlayer.reset();
//			mMediaPlayer.release();
			isPlaying = false;
//			mMediaPlayer = null;
			r.stop();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		screenOff();
		setContentView(R.layout.incall_layout);
		muteButton =(ImageButton)this.findViewById(R.id.muteButton1);
		muteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v){
				VoIPServiceNew.getVoIPSerivceInstance().muteClicked();
				changeMuteButton();
			}
			
		});
		speakerButton = (ImageButton)this.findViewById(R.id.SpeakerButton1);
		speakerSound = (ImageView)this.findViewById(R.id.speakerSound);
		micSlash = (ImageView)this.findViewById(R.id.micSlash);
		speakerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				VoIPServiceNew.getVoIPSerivceInstance().speakerClicked();	
				changeSpeakerButton();
			}
		});
		endCall = (ImageButton)this.findViewById(R.id.endCallButton);
//		endCall.setBackgroundColor(Color.RED);
		endCall.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				raiseEndCallToast();
				VoIPServiceNew.getVoIPSerivceInstance().endCall();
				finish();
			}
		});
		
		inCallCallNo = (TextView)this.findViewById(R.id.PhoneNumberView1);
//		inCallCallNo.setText(HikeUserDatabase.getInstance().getContactInfoFromPhoneNo(storedId).getNameOrMsisdn());
		inCallCallNo.setText("Phone Number goes Here!");
		inCallTimer = (TextView)this.findViewById(R.id.timerView1);
		if (VoIPServiceNew.getVoIPSerivceInstance().client.connectionState != "CONNECTED")
			inCallTimer.setText(VoIPServiceNew.getVoIPSerivceInstance().client.connectionState);
		else{
			startTime = vService.client.startTime;
			displayHandler.post(new CallLengthManager());
		}
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (type == HikePubSub.VOIP_FINISHED){
			finish();
		}
		else if(type == HikePubSub.VOIP_HANDSHAKE){
			try {
				JSONObject json = (JSONObject) object;
				JSONObject data = (JSONObject) json.get(HikeConstants.DATA);
				JSONObject metadata = (JSONObject) data.get(HikeConstants.METADATA);
				String mdType = metadata.getString("type");
				if (mdType.equals(HikeConstants.MqttMessageTypes.VOIP_END_CALL))
					finish();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (type == HikePubSub.VOIP_CALL_STATUS_CHANGED){
			String state = (String) object;
//			inCallTimer.setText(state);
			if(state == PeerConnection.IceConnectionState.CONNECTED.toString()){
				startTime = System.currentTimeMillis();
				displayHandler.post(new CallLengthManager() );
			}
		} else if (type == HikePubSub.VOIP_DURATION){			
//			this.callLength = (Long) object;		   
		}
	}
	
	public void onDestroy(){
		if(isPlaying){
//			mMediaPlayer.stop();
//			mMediaPlayer.reset();
//			mMediaPlayer.release();
//			mMediaPlayer = null;
			r.stop();
//			if (am.)
//			am.abandonAudioFocus(afChangeListener);
		}
		mPubSub.removeListener(HikePubSub.VOIP_HANDSHAKE, this);
		mPubSub.removeListener(HikePubSub.VOIP_CALL_STATUS_CHANGED, this);
		mPubSub.removeListener(HikePubSub.VOIP_DURATION, this);
		screenOn();
		((AudioManager) getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_NORMAL);
//		screenOffLock.release();
		super.onDestroy();
	}
	
	public static VoIPActivityNew getVoIPActivityInstance(){
		return vActivity;
	}
	
	private void changeMuteButton() {
		if(isMute){
			micSlash.setVisibility(ImageView.VISIBLE);
			isMute = false;
		} else {
			micSlash.setVisibility(ImageView.INVISIBLE);
			isMute = true;
		}
		
	}
	
	private void changeSpeakerButton(){
		if(!isSpeakerOn){
			speakerSound.setVisibility(ImageView.INVISIBLE);
			isSpeakerOn = true;
		} else {
			speakerSound.setVisibility(ImageView.VISIBLE);
			isSpeakerOn = false;
		}
		
	}
	
	public void raiseEndCallToast(){
		Toast.makeText(getApplicationContext(), "CALL ENDED", Toast.LENGTH_LONG).show();
	}

//	@Override
//	public void onSensorChanged(SensorEvent event) {
//		float distance = event.values[0];
//		Log.d("Proximity","Sensor event");
//		if ( event.values[0] == 0 ){
//			screenOff();
//		} else {
//			screenOn();
//		}
//		
//	}
	
	@Override
	  protected void onResume() {
	    // Register a listener for the sensor.
	    super.onResume();
//	    mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
	  }

	
	@Override
	  protected void onPause() {
	    // Be sure to unregister the sensor when the activity pauses.
//	    mSensorManager.unregisterListener(this);
//	    mMediaPlayer.release();
	    super.onPause();
	  }


//	@Override
//	public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		// TODO Auto-generated method stub		
//	}
	
	private void screenOff(){
//		Activity activity = getActivity();
		if(sensorDisabled )
			return;
		if(screenOffLock == null){
			this.screenOffLock = pm.newWakeLock( PROXIMITY_SCREEN_OFF_WAKE_LOCK,"proximity_off" );
		}
			if(!screenOffLock.isHeld())
		{
		Log.d("Proximity","Acquire lock");
//		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		screenOffLock.acquire();
		}
	}
	
	private void screenOn()
	{
		if(screenOffLock == null || !screenOffLock.isHeld())
		{
			return;
		}
		Log.d("Proximity","Release lock");
//		logger.debug("Release lock");
		screenOffLock.release();
		
//		PowerManager pm = JitsiApplication.getPowerManager();
		PowerManager.WakeLock onLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "full_on");
		onLock.acquire();
		if(onLock.isHeld())
		{
			onLock.release();
		}
	}
	
}
