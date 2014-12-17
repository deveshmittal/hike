package com.bsb.hike;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.bsb.hike.voip.VoIPClient;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPService.LocalBinder;

public class VoIPActivity extends Activity {

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

	public static final int MSG_SHUTDOWN_ACTIVITY = 1;
	public static final int MSG_CONNECTION_ESTABLISHED = 2;
	public static final int MSG_AUDIO_START = 3;
	public static final int MSG_ENCRYPTION_INITIALIZED = 4;
	public static final int MSG_CALL_DECLINED = 5;
	public static final int MSG_CONNECTION_FAILURE = 6;
	public static final int MSG_CURRENT_BITRATE = 7;
	public static final int MSG_EXTERNAL_SOCKET_RETRIEVAL_FAILURE = 8;
	public static final int MSG_ANSWER_BEFORE_CONNECTION_ESTB = 9;

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
				if (clientSelf.isInitiator()) {
					// TODO: Play ringing tone
					inCallTimer.setText("RINGING...");
				} 
				break;
			case MSG_AUDIO_START:
				startCallDuration();
				if (!clientSelf.isInitiator()) {
					drawCallAccepted();
				}
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
		setButtonHandlers();
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
			drawAsCaller();		
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
				drawAsCallee();
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
				Log.d(logTag, "Trying to hang up.");
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
				voipService.adjustBitrate(2000);
			}
		});

		final Button decreaseBRButton = (Button) findViewById(R.id.btn_decrease_bitrate);
		decreaseBRButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				voipService.adjustBitrate(-2000);
			}
		});

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
		startRinging();
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
	
	public void stopRinging() {
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

	private void getCallerInfo() {

		// Get the other party's contact information
		ContactInfo contactInfo = ContactManager.getInstance().getContact(clientPartner.getPhoneNumber());
		if (contactInfo != null)
		{
			mContactName = contactInfo.getName();
			if( mContactName == null)
			{
				mContactName = clientPartner.getPhoneNumber();
				Log.d("contactName", mContactName);
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
		Log.d(logTag, "Setting view for caller.");
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
				Log.d(logTag, "Trying to hang up.");
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
				Log.d("TouchEvent", "getRot"+((Float)callSlider.getRotation()));
				Log.d("TouchEvent","Xp"+((Float)callSlider.getPivotX()).toString());
				Log.d("TouchEvent","Yp"+((Float)callSlider.getPivotY()).toString());
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
				Log.d("TouchEvent","Start PT"+( (Float) ( v.getX() ) ).toString()  );
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
				if (xcoord<7.0f*declineCall.getX()/6.0f)		//decline call
				{
					if (clientSelf.isInitiator() == false)
					{
						voipService.rejectIncomingCall();
					}
					stopRinging();
				}
				else if (xcoord>5.0f*acceptCall.getX()/6.0f)		//accept call
				{
					if (clientSelf.isInitiator() == false)
					{
						voipService.startAudio();
					}
					stopRinging();
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
			if(!clientSelf.isInitiator()&&(voipService == null || !voipService.isConnected()))
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
