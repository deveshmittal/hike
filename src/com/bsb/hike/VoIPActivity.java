package com.bsb.hike;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;
import com.bsb.hike.smartImageLoader.VoipProfilePicImageLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPClient;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPService.LocalBinder;
import com.fima.glowpadview.GlowPadView;

public class VoIPActivity extends Activity
{

	public static final String logTag = "VoIPCaller";
	static final int PROXIMITY_SCREEN_OFF_WAKELOCK = 32;
	static final int NOTIFICATION_IDENTIFIER = 1;
	public static boolean isRunning = false;

	private VoIPService voipService;
	private VoIPClient clientSelf = new VoIPClient(), clientPartner = new VoIPClient();
	private boolean isBound = false;
	private boolean mute = false, speaker = false;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private int initialAudioMode, initialRingerMode;
	private boolean initialSpeakerMode;
	private WakeLock wakeLock = null;
	private WakeLock proximityWakeLock;
	private SensorManager sensorManager;
	private float proximitySensorMaximumRange;
	private NotificationManager notificationManager;

	private enum ConnectionStatus
	{
		INCOMING_RINGING, OUTGOING_RINGING, CALL_ESTABLISHED
	}

	public static final int MSG_SHUTDOWN_ACTIVITY = 1;
	public static final int MSG_CONNECTION_ESTABLISHED = 2;
	public static final int MSG_AUDIO_START = 3;
	public static final int MSG_ENCRYPTION_INITIALIZED = 4;
	public static final int MSG_CALL_DECLINED = 5;
	public static final int MSG_CONNECTION_FAILURE = 6;
	public static final int MSG_CURRENT_BITRATE = 7;
	public static final int MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE = 8;
	public static final int MSG_ANSWER_BEFORE_CONNECTION_ESTB = 9;

	private GlowPadView mGlowPadView;

	@SuppressLint("HandlerLeak") class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SHUTDOWN_ACTIVITY:
				Log.d(VoIPActivity.logTag, "Shutting down..");
				shutdown();
				break;
			case MSG_CONNECTION_ESTABLISHED:
				showMessage("Connection established.");
				break;
			case MSG_AUDIO_START:
				setConnectionStatus(ConnectionStatus.CALL_ESTABLISHED);
				break;
			case MSG_ENCRYPTION_INITIALIZED:
				showMessage("Encryption initialized.");
				break;
			case MSG_CALL_DECLINED:
				showMessage("Call was declined.");
				break;
			case MSG_CONNECTION_FAILURE:
				showMessage("Error: Unable to establish connection.");
				voipService.stop();
				break;
			case MSG_CURRENT_BITRATE:
				int bitrate = voipService.getBitrate();
				showMessage("Bitrate: " + bitrate);
				break;
			case MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE:
				showMessage("Error: Unable to retrieve external socket.");
				voipService.stop();
				break;
			case MSG_ANSWER_BEFORE_CONNECTION_ESTB:
				showMessage("Still connecting..");
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private ServiceConnection myConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			isBound = false;
			Log.d(VoIPActivity.logTag, "VoIPService disconnected.");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(VoIPActivity.logTag, "VoIPService connected.");
			LocalBinder binder = (LocalBinder) service;
			voipService = binder.getService();
			isBound = true;
			startService();
		}
	};
	protected Toast toast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voip_activity);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		Log.d(VoIPActivity.logTag, "Binding to service..");
		Intent intent = new Intent(this, VoIPService.class);
		bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

		clientSelf.setInitiator(false);
		clientPartner.setInitiator(true);

		intent = getIntent();
		if (intent != null) {
			handleIntent(intent);
		}

		saveCurrentAudioSettings();
		acquireWakeLock();
		showNotification();
		isRunning = true;
	}
	
	private void showNotification() {
		Intent myIntent = new Intent(this, VoIPActivity.class);
		myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, myIntent, 0);
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		
		Notification myNotification = builder
		.setContentTitle("Hike Ongoing Call")
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentIntent(pendingIntent)
		.setAutoCancel(true)
		.build();
		
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(null, NOTIFICATION_IDENTIFIER, myNotification);
	}

	@Override
	protected void onDestroy() {
		isRunning = false;
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(logTag, "VoIPActivity onNewIntent().");
		handleIntent(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		boolean retval = true; 

		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				voipService.adjustGain(-1000);
			else
				voipService.adjustGain(1000);
		} else
			retval = super.onKeyDown(keyCode, event);

		return retval;
	}

	private void handleIntent(Intent intent) {
		String action = intent.getStringExtra("action");

		if (action != null && action.equals("outgoingcall")) {
			// we are making an outgoing call
			clientPartner.setPhoneNumber(intent.getStringExtra("msisdn"));
			clientSelf.setInitiator(true);
			clientPartner.setInitiator(false);
			Log.d(logTag, "Making outgoing call to: " + clientPartner.getPhoneNumber());
			setupCallerLayout();
		}

		if (action != null && action.equals("setpartnerinfo")) {
			clientPartner.setInternalIPAddress(intent.getStringExtra("internalIP"));
			clientPartner.setInternalPort(intent.getIntExtra("internalPort", 0));
			clientPartner.setExternalIPAddress(intent.getStringExtra("externalIP"));
			clientPartner.setExternalPort(intent.getIntExtra("externalPort", 0));
			clientPartner.setPhoneNumber(intent.getStringExtra("msisdn"));
			clientPartner.setInitiator(intent.getBooleanExtra("initiator", true));
			clientSelf.setInitiator(!clientPartner.isInitiator());

			if (clientPartner.isInitiator()) {
				Log.d(logTag, "Detected incoming VoIP call.");
				setupCalleeLayout();
				// voipService.retrieveExternalSocket();
			} else {
				// We have already sent our socket info to partner
				// And now they have sent us their's, so let's establish connection
				voipService.establishConnection();
			}
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		initProximitySensor();
	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(proximitySensorEventListener);
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
		return;
	}

	private void startService() {
		try {
			Log.d(VoIPActivity.logTag, "Retrieving socket through service..");
			voipService.setClientSelf(clientSelf);
			voipService.setClientPartner(clientPartner);
			voipService.setMessenger(mMessenger);

			voipService.retrieveExternalSocket();
		} catch (Exception e) {
			Log.d(VoIPActivity.logTag, "Exception: " + e.toString());
		}
	}

	private void shutdown() {
		if (isBound) {
			unbindService(myConnection);
		}
		restoreAudioSettings();
		releaseWakeLock();
		stopRinging();		
		isRunning = false;
		
		// Dismiss notification
		if (notificationManager != null)
			notificationManager.cancel(NOTIFICATION_IDENTIFIER);
		
		finish();
	}

	private void saveCurrentAudioSettings() {
		AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		initialAudioMode = audiomanager.getMode();
		initialRingerMode = audiomanager.getRingerMode();
		initialSpeakerMode = audiomanager.isSpeakerphoneOn();
	}

	private void restoreAudioSettings() {
		AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audiomanager.setMode(initialAudioMode);
		audiomanager.setRingerMode(initialRingerMode);
		audiomanager.setSpeakerphoneOn(initialSpeakerMode);
	}

	private void acquireWakeLock() {
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		if (wakeLock == null) {
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HikeWL");
			wakeLock.setReferenceCounted(false);
		}
		if (!wakeLock.isHeld()) {
			wakeLock.acquire();
			Log.d(VoIPActivity.logTag, "Wakelock acquired.");
		}
	}

	private void releaseWakeLock() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			Log.d(VoIPActivity.logTag, "Wakelock released.");
		}
		if (proximityWakeLock != null && proximityWakeLock.isHeld())
			proximityWakeLock.release();
	}

	private void showMessage(final String message) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (toast != null)
					toast.cancel();
				toast = Toast.makeText(VoIPActivity.this, message, Toast.LENGTH_SHORT);
				toast.show();
			}
		});
	}

	private void initProximitySensor() {

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		if (proximitySensor == null) {
			Log.d(logTag, "No proximity sensor found.");
			return;
		}
		// Set proximity sensor
		proximitySensorMaximumRange = proximitySensor.getMaximumRange();
		proximityWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PROXIMITY_SCREEN_OFF_WAKELOCK, "ProximityLock");
		proximityWakeLock.setReferenceCounted(false);
		sensorManager.registerListener(proximitySensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

	}

	SensorEventListener proximitySensorEventListener = new SensorEventListener() {

		@SuppressLint("Wakelock") @Override
		public void onSensorChanged(SensorEvent event) {

			if (event.values[0] != proximitySensorMaximumRange) {
				if (!proximityWakeLock.isHeld()) {
					proximityWakeLock.acquire();
				}
			} else {
				if (proximityWakeLock.isHeld()) {
					proximityWakeLock.release();
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	
	protected boolean startAnimPlayed = false;
	private Ringtone r;
	private boolean isPlaying;
	private Vibrator vibrator;
	private boolean isVibrating;

	private void startRinging() {
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		r.setStreamType(AudioManager.STREAM_ALARM);
		AudioManager am = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL ){
			r.play();			//commented because it's irritating while testing. :/
			isPlaying = true;
		} else if (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
			vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			long[] vibTimings = {0,300,700};
			if(vibrator.hasVibrator()){
				isVibrating = true;
				vibrator.vibrate(vibTimings, 0);				
			}
		}
		
	}
	
	public void stopRinging() 
	{
		AudioManager am = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if ((am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL)&&isPlaying){
			r.stop();
		} else if ((am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)&&isVibrating) {
			vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			if(vibrator.hasVibrator()){
				vibrator.cancel();				
			}
		}
		
	}

	private void setupCallerLayout()
	{
		setAvatar();
		setContactDetails();
		showActiveCallLayout();
		setConnectionStatus(ConnectionStatus.OUTGOING_RINGING);
	}

	private void setupCalleeLayout()
	{
		startRinging();
		setAvatar();
		setContactDetails();
		showCallGlowPad();
		setConnectionStatus(ConnectionStatus.INCOMING_RINGING);	

		mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
		mGlowPadView.setOnTriggerListener(new CallGlowPadViewListener());
	}

	class CallGlowPadViewListener implements GlowPadView.OnTriggerListener
	{

		@Override
		public void onGrabbed(View v, int handle) {
			Logger.d(logTag,"Call glow pad view - Grabbed");
		}

		@Override
		public void onReleased(View v, int handle) 
		{
			Logger.d(logTag,"Call glow pad view - onRelease");
			mGlowPadView.ping();	
		}

		@Override
		public void onTrigger(View v, int target) 
		{
			int resId = mGlowPadView.getResourceIdForTarget(target);
			if(resId == R.drawable.ic_item_call_hang)
			{
				declineCall();
			}
			else if(resId == R.drawable.ic_item_call_pick)
			{
				acceptCall();
			}
		}

		@Override
		public void onGrabbedStateChange(View v, int handle) {
			Logger.d(logTag,"Call glow pad view - Grabbed state changed");
		}

		@Override
		public void onFinishFinalAnimation() {
			Logger.d(logTag,"Call glow pad view - Finish final anim");
		}
		
	}

	private void acceptCall()
	{
		Logger.d(logTag, "Accepted call, starting audio...");
		voipService.startAudio();
		stopRinging();
    	showActiveCallLayout();
	}

	private void declineCall()
	{
		Logger.d(logTag, "Declined call, rejecting...");
		voipService.rejectIncomingCall();
		stopRinging();
	}
	
	private void showActiveCallLayout()
	{
		findViewById(R.id.glow_pad_view).setVisibility(View.GONE);

		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(1000);

		findViewById(R.id.active_call_group).setVisibility(View.VISIBLE);
		findViewById(R.id.hang_up_btn).setVisibility(View.VISIBLE);

		findViewById(R.id.hang_up_btn).startAnimation(anim);
		findViewById(R.id.mute_btn).startAnimation(anim);
		findViewById(R.id.hide_btn).startAnimation(anim);
		findViewById(R.id.hold_btn).startAnimation(anim);
		findViewById(R.id.speaker_btn).startAnimation(anim);
		
		setupActiveCallButtonActions();		
	}
	
	private void setupActiveCallButtonActions()
	{
		findViewById(R.id.hang_up_btn).setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Log.d(logTag, "Trying to hang up.");
				voipService.hangUp();
			}
		});

		findViewById(R.id.mute_btn).setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				mute = !mute;
				voipService.setMute(mute);
			}
		});

		findViewById(R.id.speaker_btn).setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{				
				speaker = !speaker;
				AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				audiomanager.setSpeakerphoneOn(speaker);
			}
		});
	}
	
	private void startCallDuration()
	{	
		AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(1000);

		Chronometer callDuration = (Chronometer)VoIPActivity.this.findViewById(R.id.call_duration);
		callDuration.startAnimation(anim);
		callDuration.setVisibility(View.VISIBLE);
		callDuration.setBase(SystemClock.elapsedRealtime());
		callDuration.start();
	}

	private void setConnectionStatus(ConnectionStatus id)
	{
		final TextView connStatus = (TextView) findViewById(R.id.connection_status);
		String text = "";
		if(id == ConnectionStatus.INCOMING_RINGING)
		{
			text = getString(R.string.voip_incoming_call);
			connStatus.setText(text);
		}
		else if(id == ConnectionStatus.OUTGOING_RINGING)
		{
			text = getString(R.string.voip_ringing);
			connStatus.setText(text);
		}
		else
		{
			connStatus.setVisibility(View.GONE);
			startCallDuration();
		}		
	}

	public void setAvatar()
	{
		String mappedId = clientPartner.getPhoneNumber() + "pp";
		int mBigImageSize = getResources().getDimensionPixelSize(R.dimen.timeine_big_picture_size);

		VoipProfilePicImageLoader profileImageLoader = new VoipProfilePicImageLoader(this, mBigImageSize);
	    profileImageLoader.setDefaultAvatarIfNoCustomIcon(true);
	    profileImageLoader.setDefaultAvatarScaleType(ScaleType.FIT_START);
		profileImageLoader.loadImage(mappedId, (ImageView)findViewById(R.id.profile_image));
	}

	public void setContactDetails()
	{
		TextView contactNameView = (TextView) findViewById(R.id.contact_name);
		TextView contactMsisdnView = (TextView) findViewById(R.id.contact_msisdn);

		ContactInfo contactInfo = ContactManager.getInstance().getContact(clientPartner.getPhoneNumber());
		String name = contactInfo.getNameOrMsisdn();
		if(name.length() > 16)
		{
			contactNameView.setTextSize(24);
		}
		contactNameView.setText(name);

		if(contactInfo.getName() != null)
		{
			contactMsisdnView.setVisibility(View.VISIBLE);
			contactMsisdnView.setText(contactInfo.getMsisdn());
		}
	}
	
	public void showCallGlowPad()
	{
		View callGlowPadView = findViewById(R.id.glow_pad_view);

		TranslateAnimation anim = new TranslateAnimation(0, 0.0f, 0, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_SELF, 0f);
		anim.setDuration(1500);
		anim.setInterpolator(new DecelerateInterpolator(4f));

		callGlowPadView.setVisibility(View.VISIBLE);
		callGlowPadView.startAnimation(anim);
	}
}
