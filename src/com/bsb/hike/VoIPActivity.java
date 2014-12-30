package com.bsb.hike;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.voip.VoIPClient;
import com.bsb.hike.voip.VoIPConstants;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPService.LocalBinder;
import com.bsb.hike.voip.VoIPUtils;

public class VoIPActivity extends Activity {

	static final int PROXIMITY_SCREEN_OFF_WAKELOCK = 32;
	public static boolean isRunning = false;

	private VoIPService voipService;
	// private VoIPClient clientSelf = new VoIPClient(), clientPartner = new VoIPClient();
	private boolean isBound = false;
	private boolean mute = false, speaker = false;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private int initialAudioMode, initialRingerMode;
	private boolean initialSpeakerMode;
	private WakeLock wakeLock = null;
	private WakeLock proximityWakeLock;
	private SensorManager sensorManager;
	private float proximitySensorMaximumRange;

	public static final int MSG_SHUTDOWN_ACTIVITY = 1;
	public static final int MSG_CONNECTION_ESTABLISHED = 2;
	public static final int MSG_AUDIO_START = 3;
	public static final int MSG_ENCRYPTION_INITIALIZED = 4;
	public static final int MSG_OUTGOING_CALL_DECLINED = 5;
	public static final int MSG_CONNECTION_FAILURE = 6;
	public static final int MSG_CURRENT_BITRATE = 7;
	public static final int MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE = 8;
	public static final int MSG_PARTNER_SOCKET_INFO_TIMEOUT = 10;
	public static final int MSG_PARTNER_ANSWER_TIMEOUT = 11;
	public static final int MSG_HANGUP = 12;
	public static final int MSG_INCOMING_CALL_DECLINED = 14;

	@SuppressLint("HandlerLeak") class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			Logger.d(VoIPConstants.TAG, "VoIPActivity handler received: " + msg.what);
			switch (msg.what) {
			case MSG_SHUTDOWN_ACTIVITY:
				Logger.d(VoIPConstants.TAG, "Shutting down..");
				shutdown();
				break;
			case MSG_CONNECTION_ESTABLISHED:
				showMessage("Connection established.");
				inCallTimer.setText("RINGING...");
				break;
			case MSG_AUDIO_START:
				startCallDuration();
				drawCallAccepted();
				break;
			case MSG_ENCRYPTION_INITIALIZED:
				showMessage("Encryption initialized.");
				break;
			case MSG_INCOMING_CALL_DECLINED:
				// VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0);
				break;
			case MSG_OUTGOING_CALL_DECLINED:
				showMessage("Call was declined.");
				break;
			case MSG_CONNECTION_FAILURE:
				showMessage("Error: Unable to establish connection.");
//				if (clientSelf.isInitiator())
//					VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0);
//				else
//					VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0);
//				voipService.stop();
				break;
			case MSG_CURRENT_BITRATE:
				int bitrate = voipService.getBitrate();
				showMessage("Bitrate: " + bitrate);
				break;
			case MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE:
				showMessage("Unable to connect to network. Please try again later.");
				voipService.stop();
				break;
			case MSG_PARTNER_SOCKET_INFO_TIMEOUT:
				showMessage("Partner is not responding.");

//				if (clientSelf.isInitiator())
//					VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0);
//				else
//					VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0);
//
//				voipService.stop();
				break;
			case MSG_PARTNER_ANSWER_TIMEOUT:
				showMessage("No response.");
//				if (!clientSelf.isInitiator())
//					VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_INCOMING, 0);
//				else
//					VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_MISSED_CALL_OUTGOING, 0);
//				voipService.stop();
				break;
			case MSG_HANGUP:	// TODO in service
				int seconds = 0;
				if (callDuration != null)
					seconds = (int) ((SystemClock.elapsedRealtime() - callDuration.getBase()) / 1000);
				// VoIPUtils.addMessageToChatThread(VoIPActivity.this, clientPartner, HikeConstants.MqttMessageTypes.VOIP_MSG_TYPE_CALL_SUMMARY, seconds);
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
			Logger.d(VoIPConstants.TAG, "VoIPService disconnected.");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Logger.d(VoIPConstants.TAG, "VoIPService connected.");
			LocalBinder binder = (LocalBinder) service;
			voipService = binder.getService();
			isBound = true;
			connectMessenger();
		}
	};
	private String mContactName;
	private Chronometer callDuration;
	protected Toast toast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_voip);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		Logger.d(VoIPConstants.TAG, "Binding to service..");
		// Calling start service as well so an activity unbind doesn't cause the service to stop
		startService(new Intent(getApplicationContext(), VoIPService.class));
		Intent intent = new Intent(getApplicationContext(), VoIPService.class);
		bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

		intent = getIntent();
		if (intent != null) {
			handleIntent(intent);
		}

		saveCurrentAudioSettings();
		setButtonHandlers();
		acquireWakeLock();
		isRunning = true;
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
		Logger.w(VoIPConstants.TAG, "VoIPActivity onPause()");
	}

	@Override
	protected void onDestroy() {
		try {
			if (isBound) {
				unbindService(myConnection);
			}
		} catch (IllegalArgumentException e) {
			Logger.d(VoIPConstants.TAG, "unbindService IllegalArgumentException: " + e.toString());
		}
		
		isRunning = false;
		Logger.w(VoIPConstants.TAG, "VoIPActivity onDestroy()");
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Logger.d(VoIPConstants.TAG, "VoIPActivity onNewIntent().");
		handleIntent(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		boolean retval = true; 

		if (voipService != null && voipService.isAudioRunning() && 
				(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
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

		if (action == null || action.isEmpty())
			return;
		else
			Logger.d(VoIPConstants.TAG, "Intent action: " + action);
		
		if (action.equals(VoIPConstants.PARTNER_REQUIRES_UPGRADE)) {
			String message = intent.getStringExtra("message");
			if (message == null || message.isEmpty())
				message = "Callee needs to upgrade their client.";
			showMessage(message);
			voipService.stop();
		}
		
		if (action.equals(VoIPConstants.PARTNER_INCOMPATIBLE)) {
			String message = intent.getStringExtra("message");
			if (message == null || message.isEmpty())
				message = "Callee is on an incompatible client.";
			showMessage(message);
			voipService.stop();
		}
		
		if (action.equals(VoIPConstants.PARTNER_HAS_BLOCKED_YOU)) {
			String message = intent.getStringExtra("message");
			if (message == null || message.isEmpty())
				message = "You have been blocked by the person you are trying to call.";
			showMessage(message);
			voipService.stop();
		}
		
		if (action.equals(VoIPConstants.PARTNER_IN_CALL)) {
			showMessage("Callee is currently busy in a call.");
			if (voipService != null)
				voipService.stop();
		}
		
		if (action.equals(VoIPConstants.PUT_CALL_ON_HOLD)) {
			showMessage("Putting call on hold.");
			voipService.setHold(true);
		}
		
		// Clear the intent so the activity doesn't process intent again on resume
		getIntent().removeExtra("action");
	}

	private void connectMessenger() {
		voipService.setMessenger(mMessenger);
		
		VoIPClient clientPartner = voipService.getPartnerClient();
		if (clientPartner.isInitiator())
			drawAsCallee();
		else
			drawAsCaller();
	}
	
	public void drawCallAccepted() {
		acceptCall.setVisibility(View.GONE);
		declineCall.setVisibility(View.GONE);
		ImageView speakerView = (ImageView) this.findViewById(R.id.fullSpeakerSound);
		speakerView.setVisibility(View.VISIBLE);
		ImageView muteView = (ImageView) this.findViewById(R.id.fullMicButton);
		muteView.setVisibility(View.VISIBLE);
		callSlider.setBackgroundResource(R.drawable.slider_oval_red);
		callSlider.setRotation(0);
		callSlider.setImageResource(R.drawable.cut_call);
		startSliderAnimation(sliderContainer);
		
		TextView preCallTimer = ((TextView)(VoIPActivity.this.findViewById(R.id.fullPhoneNumberView1)));
		preCallTimer.setVisibility(View.INVISIBLE);
		float delY = callNo.getY()-preCallTimer .getY();
		TranslateAnimation yTranslator  = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, (-1)*delY);
		yTranslator.setDuration(500);
		yTranslator.setFillAfter(true);
		yTranslator.setFillEnabled(true);
		callNo.setAnimation(yTranslator);		// y-translate phone number.
		yTranslator.start();
		preCallTimer.setAlpha(0.0f);
		Animation fadeIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.call_fade_in);
		callDuration.setAnimation(fadeIn);
		fadeIn.start();
		callSlider.setOnTouchListener(null);
		callSlider.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Logger.d(VoIPConstants.TAG, "Trying to hang up.");
				voipService.hangUp();
			}
		});
		
		
	}

	private void startCallDuration() {
		inCallTimer = (TextView)this.findViewById(R.id.fullCallTimer);
		inCallTimer.setText("CONNECTING...");
		inCallTimer.setVisibility(View.INVISIBLE);
		callDuration = (Chronometer)VoIPActivity.this.findViewById(R.id.callDurationChrono);
		callDuration.setVisibility(View.VISIBLE);
		callDuration.setBase(SystemClock.elapsedRealtime());
		callDuration.start();
		
	}


	private void setButtonHandlers() {
		
		final ImageView muteLayout = (ImageView) findViewById(R.id.fullMicButton);
		muteLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ImageView muteButton =(ImageView)VoIPActivity.this.findViewById(R.id.fullMicButton);
				
				if (mute == false) {
					mute = true;
					muteButton.setImageResource(R.drawable.voip_mute);
					voipService.setMute(mute);
				} else {
					mute = false;
					muteButton.setImageResource(R.drawable.voip_unmute);
					voipService.setMute(mute);
				}
			}
		});

		final ImageView speakerView = (ImageView) findViewById(R.id.fullSpeakerSound);
		speakerView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				if (speaker == false) {
					speaker = true;
					speakerView.setImageResource(R.drawable.voip_speaker);
				} else {
					speaker = false;
					speakerView.setImageResource(R.drawable.voip_speaker_mute);
				}

				AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				audiomanager.setSpeakerphoneOn(speaker);
			}
		});

		final Button increaseBRButton = (Button) findViewById(R.id.btn_increase_bitrate);
		increaseBRButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (voipService != null)
					voipService.adjustBitrate(2000);
			}
		});

		final Button decreaseBRButton = (Button) findViewById(R.id.btn_decrease_bitrate);
		decreaseBRButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (voipService != null)
					voipService.adjustBitrate(-2000);
			}
		});

	}

	private void shutdown() {
		
		try {
			if (isBound) {
				unbindService(myConnection);
			}
		} catch (IllegalArgumentException e) {
			Logger.d(VoIPConstants.TAG, "shutdown() exception: " + e.toString());
		}
		
		restoreAudioSettings();
		releaseWakeLock();

		isRunning = false;
		
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
			Logger.d(VoIPConstants.TAG, "Wakelock acquired.");
		}
	}

	private void releaseWakeLock() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			Logger.d(VoIPConstants.TAG, "Wakelock released.");
		}
		if (proximityWakeLock != null && proximityWakeLock.isHeld())
			proximityWakeLock.release();
	}

	private void showMessage(final String message) {
		Logger.d(VoIPConstants.TAG, "Toast: " + message);
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
			Logger.d(VoIPConstants.TAG, "No proximity sensor found.");
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
	
	public ImageView callSlider;
	public FrameLayout sliderContainer;
	private AnimatorSet DPReposition;
	private AnimatorSet animsetAccept;
	private AnimatorSet animsetDecline;
	private ObjectAnimator sliderRotator;
	private int apiLevel = android.os.Build.VERSION.SDK_INT;
	private ImageView displayPic;
	private FrameLayout avatarContainer;
	private TextView callNo;
	private ImageView acceptCall;
	private ImageView declineCall;
	private TextView inCallTimer;
	protected boolean startAnimPlayed = false;
	private Ringtone r;
	private boolean isPlaying;
	private Vibrator vibrator;
	private boolean isVibrating;

	public void drawAsCallee(){
		getCallerInfo();
//		Drawing incoming call layout, fetching resources, animations
		
		displayPic = (ImageView)this.findViewById(R.id.fullvoipContactPicture);
		avatarContainer = (FrameLayout)this.findViewById(R.id.voip_avatar_container);
		callSlider = (ImageView)this.findViewById(R.id.fullcallSlider);
		callNo = (TextView)this.findViewById(R.id.fullCallerId);
		callNo.setText(mContactName);
		sliderContainer = (FrameLayout)this.findViewById(R.id.voip_slider_container);
		sliderContainer.setVisibility(View.VISIBLE);
		callSlider.setVisibility(View.VISIBLE);
		callSlider.getBackground().setAlpha(0);
		sliderContainer.setOnTouchListener(new SliderOnTouchListener());
		acceptCall = (ImageView)this.findViewById(R.id.fullacceptButton);
		declineCall = (ImageView)this.findViewById(R.id.fulldeclineButton);
		startIncomingCallAnimation();
		startArrowAnimations();
		startSliderAnimation(callSlider);
		
		
	}

	private void getCallerInfo() {

		// Get the other party's contact information
		VoIPClient clientPartner = voipService.getPartnerClient();
		ContactInfo contactInfo = ContactManager.getInstance().getContact(clientPartner.getPhoneNumber());
		if (contactInfo != null)
		{
			mContactName = contactInfo.getName();
			if( mContactName == null)
			{
				mContactName = clientPartner.getPhoneNumber();
				Logger.d("contactName", mContactName);
			}
		}
		setContactPicture();
	}
	private void setContactPicture() {
		// TODO Get Contact Picture from HikeDB and display
		
	}

	public void drawAsCaller(){
		//TODO: Play dialertone
		
		getCallerInfo();
		Logger.d(VoIPConstants.TAG, "Setting view for caller.");
		displayPic = (ImageView)this.findViewById(R.id.fullvoipContactPicture);
		avatarContainer = (FrameLayout)this.findViewById(R.id.voip_avatar_container);
		callSlider = (ImageView)this.findViewById(R.id.fullcallSlider);
		callNo = (TextView)this.findViewById(R.id.fullCallerId);
		callNo.setText(mContactName);
		sliderContainer = (FrameLayout)this.findViewById(R.id.voip_slider_container);
		sliderContainer.setVisibility(View.VISIBLE);
		callSlider.setVisibility(View.VISIBLE);
		callSlider.setBackgroundResource(R.drawable.slider_oval_red);
		callSlider.setRotation(0);
		callSlider.setImageResource(R.drawable.cut_call);
		startSliderAnimation(sliderContainer);
		acceptCall = (ImageView)this.findViewById(R.id.fullacceptButton);
		declineCall = (ImageView)this.findViewById(R.id.fulldeclineButton);
		acceptCall.setVisibility(View.INVISIBLE);
		declineCall.setVisibility(View.INVISIBLE);
		ImageView speakerView = (ImageView) this.findViewById(R.id.fullSpeakerSound);
		speakerView.setVisibility(View.VISIBLE);
		ImageView muteView = (ImageView) this.findViewById(R.id.fullMicButton);
		muteView.setVisibility(View.VISIBLE);
		final TextView preCallTimer = (TextView)this.findViewById(R.id.fullPhoneNumberView1);
		preCallTimer.setVisibility(View.INVISIBLE);
		inCallTimer = (TextView)this.findViewById(R.id.fullCallTimer);
		inCallTimer.setText("CONNECTING...");
		
		ImageView closeView = (ImageView) findViewById(R.id.fullcallSlider);
		closeView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Logger.d(VoIPConstants.TAG, "Trying to hang up.");
				voipService.hangUp();
			}
		});
		
		final ViewTreeObserver vto = inCallTimer.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				float delY = callNo.getY()-inCallTimer.getY();
				if(!startAnimPlayed ){
					
					TranslateAnimation yTranslator  = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, 
							delY);
					yTranslator.setDuration(500);
					yTranslator.setFillAfter(true);
					yTranslator.setFillEnabled(true);
					callNo.setAnimation(yTranslator);		// y-translate phone number.
					inCallTimer.setVisibility(View.VISIBLE);
					Animation fadeIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.call_fade_in);
					RelativeLayout innerLayout = (RelativeLayout) VoIPActivity.this.findViewById(R.id.full_voip_inner_layout);
					innerLayout.setAnimation(fadeIn);
					startCallerDPAnimation();				
					yTranslator.start();
					fadeIn.start();
					startAnimPlayed = true;
				}
			}
			
		});
		
//		TextView inCallTimer = (TextView)findViewById(R.id.fullCallTimer);
//		float delY = callNo.getY()-inCallTimer.getY();
//		TranslateAnimation nameTranslator  = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, delY);
//		nameTranslator.setDuration(500);
//		nameTranslator.setFillAfter(true);
//		nameTranslator.setFillEnabled(true);

		
		
		
	}

	class SliderOnTouchListener implements OnTouchListener{		
		PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
		PointF StartPT = new PointF(); // Record Start Position of 'img'

		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			int eid = event.getAction();
			switch (eid)
			{
			// Call slider rotation.
			case MotionEvent.ACTION_MOVE :
				PointF mv = new PointF( event.getX() - DownPT.x, event.getY() - DownPT.y);
				//	                if((((StartPT.x+mv.x)<(acceptCall.getX()-(callSlider.getWidth()/2)))&&((StartPT.x+mv.x)>(declineCall.getX()))))
				Logger.d("TouchEvent", "getRot"+((Float)callSlider.getRotation()));
				Logger.d("TouchEvent","Xp"+((Float)callSlider.getPivotX()).toString());
				Logger.d("TouchEvent","Yp"+((Float)callSlider.getPivotY()).toString());
				float rotAng = (float)(120)*(sliderContainer.getTranslationX()/(float)((acceptCall.getX()-((sliderContainer.getLeft()+sliderContainer.getRight())/2.0f))));
				if(sliderContainer.getTranslationX()>0)
					callSlider.setBackgroundResource(R.drawable.slider_oval);
				else
					callSlider.setBackgroundResource(R.drawable.slider_oval_red);

				if (rotAng < 120 && rotAng > -120){
					callSlider.setRotation((-1.0f)*rotAng);
					callSlider.getBackground().setAlpha((int) ( 255.0f / 120 * Math.abs(rotAng)));
				}
				(sliderContainer).setX((int)(StartPT.x+mv.x));
				StartPT = new PointF( (sliderContainer).getX(), sliderContainer.getY() );
				break;

				// Animation ends on ACTION_DOWN, call is accepted or declined
			case MotionEvent.ACTION_DOWN :
				DownPT.x = event.getX();
				Logger.d("TouchEvent","Start PT"+( (Float) ( v.getX() ) ).toString()  );
				DownPT.y = event.getY();
				callSlider.clearAnimation();
				if(apiLevel>=11)
					sliderRotator.end();
				StartPT = new PointF( (sliderContainer).getX(), sliderContainer.getY() );
				if(apiLevel>=11){
					animsetAccept.end();
					animsetDecline.end();
				}
				break;
				// VoIP service started on ACTION_UP
			case MotionEvent.ACTION_UP :
				PointF mvevent = new PointF( event.getX() - DownPT.x, event.getY() - DownPT.y);
				float xcoord = StartPT.x+mvevent.x;
				VoIPClient clientPartner = voipService.getPartnerClient();
				if (xcoord<7.0f*declineCall.getX()/6.0f)		//decline call
				{
					if (clientPartner.isInitiator() == true)
					{
						voipService.rejectIncomingCall();
					}
				}
				else if (xcoord>5.0f*acceptCall.getX()/6.0f)		//accept call
				{
					if (clientPartner.isInitiator() == true)
					{
						voipService.acceptIncomingCall();
					}
				} 
				else {								//if no ACTION_UP, then start animation again
					startArrowAnimations();
					callSlider.getBackground().setAlpha(0);
					callSlider.setRotation(0);
					startSliderAnimation(sliderContainer);
					//							slider Animation

				}
				break;
			default :
				break;
			}
			return true;
		}
	}

	public void startCallerDPAnimation()
	{
		if(apiLevel>=11)
		{
			ImageView miniAvatar = (ImageView)this.findViewById(R.id.full_small_dp);
			float x = miniAvatar.getX();
			float y = miniAvatar.getY();
			avatarContainer.setScaleX(0.28f);
			avatarContainer.setScaleY(0.28f);
			avatarContainer.setX(x);
			avatarContainer.setY(y);
			DPReposition = (AnimatorSet) AnimatorInflater.loadAnimator(getBaseContext(), R.animator.dp_translate_scale_anim);
			DPReposition.setTarget(avatarContainer);
			DPReposition.start();}
	}

	

	/**
	 * Start arrow animations
	 */
	public void startArrowAnimations()
	{
		if(apiLevel>=11)
		{
			float greenStart = ((float)(0.2)*( acceptCall.getX() - (float)sliderContainer.getRight() ) + (float)sliderContainer.getRight());

			float redStart = ( (float)sliderContainer.getLeft() - ((float)(0.2)*(  (float)sliderContainer.getLeft() - (float)declineCall.getRight() )) );
			
			acceptCall.setX(greenStart);
			declineCall.setX(redStart-(declineCall.getRight()-declineCall.getLeft()));
			animsetAccept = (AnimatorSet)AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.voip_arrow_alpha_translation);
			animsetDecline = (AnimatorSet)AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.voip_arrow_alpha_translation);
			animsetAccept.setTarget(acceptCall);
			animsetDecline.setTarget(declineCall);
			animsetDecline.start();
			animsetAccept.start();
		}
	}

	/**
	 * Start call slider translation rotation animation
	 * @param v
	 */
	public void startSliderAnimation(View v)
	{	
		if(apiLevel >=11){
			AnimatorSet animset = (AnimatorSet)AnimatorInflater.loadAnimator(getBaseContext(), R.animator.voip_avatar_translator);
			animset.setInterpolator(new OvershootInterpolator(2.2f));
			animset.setTarget(v);
			animset.start();
			VoIPClient clientPartner = voipService.getPartnerClient();
			if(clientPartner.isInitiator())
			{
				sliderRotator = (ObjectAnimator)AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.voip_slider_rotator);
				sliderRotator.setTarget(callSlider);
				sliderRotator.start();
			}
		}
		else
		{
			v.setTranslationX(0);
		}
		
	}

	public void startIncomingCallAnimation()
	{
		Animation fadeIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.call_fade_in);
		RelativeLayout innerLayout = (RelativeLayout)VoIPActivity.this.findViewById(R.id.full_voip_inner_layout);
		innerLayout.setAnimation(fadeIn);
		TranslateAnimation yTranslator = new TranslateAnimation(Animation.RELATIVE_TO_SELF,0,Animation.RELATIVE_TO_SELF,0,Animation.ABSOLUTE,-1000,Animation.RELATIVE_TO_SELF,0);
		yTranslator.setInterpolator(new DecelerateInterpolator());
		yTranslator.setDuration(1000);
		yTranslator.setFillAfter(true);
		yTranslator.setFillEnabled(true);
		avatarContainer.setAnimation(yTranslator);
		yTranslator.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				callNo.setVisibility(View.VISIBLE);
				callSlider.setVisibility(View.VISIBLE);
				sliderContainer.setVisibility(View.VISIBLE);
				((TextView)(VoIPActivity.this.findViewById(R.id.fullPhoneNumberView1))).setVisibility(View.VISIBLE);
				
			}
		});
		yTranslator.start();
		fadeIn.start();
		fadeIn.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				callNo.setVisibility(View.VISIBLE);
				callSlider.setVisibility(View.VISIBLE);
				sliderContainer.setVisibility(View.VISIBLE);
				((TextView)(VoIPActivity.this.findViewById(R.id.fullPhoneNumberView1))).setVisibility(View.VISIBLE);
				Animation newFade = AnimationUtils.loadAnimation(getBaseContext(), R.anim.call_fade_in);
				newFade.setFillEnabled(true);
				newFade.setFillAfter(true);
				callNo.setAnimation(newFade);
				callSlider.setAnimation(newFade);
				((TextView)(VoIPActivity.this.findViewById(R.id.fullPhoneNumberView1))).setAnimation(newFade);
				newFade.start();
			}
		});

	}

//	public native String  stringFromJNI();
//    static {
//        System.loadLibrary("hello-jni");
//    }

}
