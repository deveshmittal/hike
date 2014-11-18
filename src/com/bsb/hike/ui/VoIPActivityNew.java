package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.Shape;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.VoIPServiceNew;
import com.google.android.gms.internal.dp;
//import com.bsb.hike.db.HikeUserDatabase;

public class VoIPActivityNew extends Activity implements HikePubSub.Listener{

	private String callerId;
	private String dialedId;
	private ImageButton endCall;
	private ImageView acceptCall;
	private ImageView declineCall;
	private ImageView callSlider;
	private ImageView muteButton;
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
	private String mContactName;
	private String mContactNumber;
	private ImageView displayPic;
	private Animation dpAnim; 
	private float redX;
	private float greenX;
	private float sliderX;
	private int sliderWidth;
	private SliderOnTouchListener sliderListener;
	private AnimatorSet animsetAccept;
	private AnimatorSet animsetDecline;
	protected Animation sliderRotator;
	protected float sliderY;
	private FrameLayout sliderContainer;
	private ImageView speakerButton;
	
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
		    displayPic.clearAnimation();
		    displayHandler.postDelayed(new CallLengthManager(), 500);
			
		}
		
	};
	
	class SliderOnTouchListener implements OnTouchListener{		
		    PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
		    PointF StartPT = new PointF(); // Record Start Position of 'img'

		    @Override
		    public boolean onTouch(View v, MotionEvent event)
		    {
		        int eid = event.getAction();
		        switch (eid)
		        {
		            case MotionEvent.ACTION_MOVE :
		                PointF mv = new PointF( event.getX() - DownPT.x, event.getY() - DownPT.y);


//		                if((((StartPT.x+mv.x)<(acceptCall.getX()-(callSlider.getWidth()/2)))&&((StartPT.x+mv.x)>(declineCall.getX()))))
		                
		                
		                Log.d("TouchEvent", "getRot"+((Float)callSlider.getRotation()));
//		                float pivotX = sliderX+callSlider.getTranslationX();
//		                float pivotY = sliderY+callSlider.getTranslationY();
//		                callSlider.setPivotX(60.0f);
//		                callSlider.setPivotY(60.0f);
		                Log.d("TouchEvent","Xp"+((Float)callSlider.getPivotX()).toString());
		                Log.d("TouchEvent","Yp"+((Float)callSlider.getPivotY()).toString());
//		                Log.d("TouchEvent","5 "+( (Float) ( 120.0f*callSlider.getTranslationX()/(float)(greenX-sliderX) ) ).toString()  );
//		                Matrix matrix = new Matrix();
//		                callSlider.setScaleType(ScaleType.MATRIX);
//		                matrix.postRotate((float)(120)*(), callSlider.getDrawable().getBounds().width()/2, callSlider.getDrawable().getBounds().height()/2);
//		                callSlider.setImageMatrix(matrix);
		                float rotAng = (float)(120)*(sliderContainer.getTranslationX()/(float)((greenX-sliderX)));
//		                callSlider.setBackgroundResource(R.drawable.slider_oval_red);
		                if(sliderContainer.getTranslationX()>0)
		                	callSlider.setBackgroundResource(R.drawable.slider_oval);
		                else
		                	callSlider.setBackgroundResource(R.drawable.slider_oval_red);
		                
		                if (rotAng < 120 && rotAng > -120){
		                	callSlider.setRotation((-1.0f)*rotAng);
		                	callSlider.getBackground().setAlpha((int) ( 255.0f / 120 * Math.abs(rotAng)));
		                }
		                	
		                (sliderContainer).setX((int)(StartPT.x+mv.x));
		                
//		                callSlider.getBackground().setAlpha((int)(255.0f * (float) Math.abs(callSlider.getTranslationX()/(float)(greenX-sliderX))));
//		                Log.d("TouchEvent","6 "+( (Float) ( callSlider.getTranslationX()/(float)(greenX-sliderX) ) ).toString()  );
		                StartPT = new PointF( (sliderContainer).getX(), sliderContainer.getY() );
		                break;
		            case MotionEvent.ACTION_DOWN :
		                DownPT.x = event.getX();
		                Log.d("TouchEvent","Start PT"+( (Float) ( v.getX() ) ).toString()  );
		                DownPT.y = event.getY();
		                ;
//		                callSlider.setVisibility(View.INVISIBLE);
		                callSlider.clearAnimation();
//		                callSlider.setRotation(120);
		                StartPT = new PointF( (sliderContainer).getX(), sliderContainer.getY() );
		                animsetAccept.end();
		                animsetDecline.end();
		                break;
		            case MotionEvent.ACTION_UP :
		            	
		            	PointF mvevent = new PointF( event.getX() - DownPT.x, event.getY() - DownPT.y);
		            	float xcoord = StartPT.x+mvevent.x;
		            	if (xcoord<redX)
							{
		            		
//		        				r.stop();
		        				Vibrator v1 = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
								v1.vibrate(100);
		        				Intent intent = new Intent(getApplicationContext(),com.bsb.hike.service.VoIPServiceNew.class);;
		        				intent.putExtra("decline", true);	
		        				intent.putExtras(getIntent().getExtras());
//		        				vService = VoIPServiceNew.getVoIPSerivceInstance();
//		        				vService.startCall(intent);
//		        				finish();
//			            		Intent intent = i;
//			    				intent.putExtra("decline", true);
			    				callConnected = true;
			    				vService = VoIPServiceNew.getVoIPSerivceInstance();
			    				vService.startCall(intent);
			    				finish();
		        			
							}
						else if (xcoord>greenX)
							{
							Intent intent = new Intent(getApplicationContext(),com.bsb.hike.service.VoIPServiceNew.class);
							intent.putExtra("decline", false);
							Vibrator v1 = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
							v1.vibrate(100);
							intent.putExtras(getIntent().getExtras());
							callConnected = true;				
							vService = VoIPServiceNew.getVoIPSerivceInstance();
							vService.startCall(intent);
							drawInCall();
							} else {
								animsetAccept.start();
								animsetDecline.start();
								callSlider.getBackground().setAlpha(0);
								callSlider.setRotation(0);
//								Animator sliderTranslate = 
//								Animator sliderRotate = (Animator)AnimatorInflater.loadAnimator(getBaseContext(), R.animator.voip_slider_rotator);
								
								AnimatorSet animset = (AnimatorSet)AnimatorInflater.loadAnimator(getBaseContext(), R.animator.voip_avatar_translator);
								animset.setInterpolator(new OvershootInterpolator(2.5f));
								animset.setTarget(sliderContainer);
//								animset.play(sliderRotate).after(sliderTranslate);
//								callSlider.getRot
								animset.start();
//								callSlider.startAnimation(dpAnim);
//								animset.addListener(new AnimatorListener() {
//									
//									@Override
//									public void onAnimationStart(Animator animation) {
//										// TODO Auto-generated method stub
//										
//									}
//									
//									@Override
//									public void onAnimationRepeat(Animator animation) {
//										// TODO Auto-generated method stub
//										
//									}
//									
//									@Override
//									public void onAnimationEnd(Animator animation) {
////										sliderRotator.reset();
////										callSlider.setAnimation(sliderRotator);
////										sliderRotator.start();
//									}
//									
//									@Override
//									public void onAnimationCancel(Animator animation) {
//										// TODO Auto-generated method stub
//										
//									}
//								});

							}
			                break;
		            default :
		                break;
		        }
		        return true;
		    }
	}
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		vActivity = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mPubSub.addListener(HikePubSub.VOIP_HANDSHAKE, this);
		mPubSub.addListener(HikePubSub.VOIP_CALL_STATUS_CHANGED, this);
		mPubSub.addListener(HikePubSub.VOIP_DURATION, this);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		
		am = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		if(getIntent().hasExtra("callerID")){
			callerId = getIntent().getStringExtra("callerID");
			storedId = callerId;
			ContactInfo contactInfo = ContactManager.getInstance().getContactInfoFromPhoneNo(callerId);
			if (contactInfo != null)
			{
				mContactName = contactInfo.getName();
				mContactNumber = contactInfo.getMsisdn();
				if( mContactName == null)
				{
					mContactName = mContactNumber = dialedId;
					Log.d("contactName", mContactName);
				}
			}
			prepareAnswer();
		} else if (getIntent().hasExtra("dialedID")){
			dialedId = getIntent().getStringExtra("dialedID");
			storedId = dialedId;
			ContactInfo contactInfo = ContactManager.getInstance().getContactInfoFromPhoneNo(dialedId);
			if (contactInfo != null)
			{
				mContactName = contactInfo.getName();
				mContactNumber = contactInfo.getMsisdn();
				if( mContactName == null)
				{
					mContactName = mContactNumber = dialedId;
					Log.d("contactName", mContactName);
				}
//				Log.d("contactName from info", mContactName);
			}
			prepareInCall();
		} else {
			resumeId = getIntent().getStringExtra("resumeID");
			storedId = resumeId;
			Log.d("STOREDID", storedId);
			ContactInfo contactInfo = ContactManager.getInstance().getContactInfoFromPhoneNo(storedId);
			if (contactInfo != null)
			{
				mContactName = contactInfo.getName();
				mContactNumber = contactInfo.getMsisdn();
				if( mContactName == null)
				{
					mContactName = mContactNumber = storedId;
					Log.d("contactName", mContactName);
				}
			}
			prepareResume();			
		}
	}
	
	
	public void prepareAnswer(){
		
		final Intent i = new Intent(this,com.bsb.hike.service.VoIPServiceNew.class);
		i.putExtras(getIntent().getExtras());
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		r.setStreamType(AudioManager.STREAM_ALARM);
//		r.play();
		Log.w("Audio Starting", "Audio Starting");
		isPlaying = true;
		setContentView(R.layout.full_call_accept_decline);
		displayPic = (ImageView)this.findViewById(R.id.fullvoipContactPicture);
		setDisplayPic();
		dpAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.voip_dp_bounce);
		vActivity = this;
		callSlider = (ImageView)this.findViewById(R.id.fullcallSlider);
		callNo = (TextView)this.findViewById(R.id.fullCallerId);
		callNo.setText(mContactName);
		sliderContainer = (FrameLayout)this.findViewById(R.id.voip_slider_container);
		acceptCall = (ImageView)this.findViewById(R.id.fullacceptButton);
		
		acceptCall.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				Intent intent = i;
				intent.putExtra("decline", false);
				callConnected = true;				
				vService = VoIPServiceNew.getVoIPSerivceInstance();
				vService.startCall(intent);
				drawInCall();
			}
			
		});
		
		declineCall = (ImageView)this.findViewById(R.id.fulldeclineButton);

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
		
		//ADDING ANIMATIONS HERE
		ViewTreeObserver vto = acceptCall.getViewTreeObserver();  
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {  
		    @Override  
		    public void onGlobalLayout() {  
		        acceptCall.getViewTreeObserver().removeGlobalOnLayoutListener(this);  
		        redX = declineCall.getX();
				greenX = acceptCall.getX();
				
				sliderX = (sliderContainer.getRight()+sliderContainer.getLeft())/(float)2;
				sliderY = (callSlider.getTop()+callSlider.getBottom())/2.0f;
				sliderWidth = sliderContainer.getRight()-sliderContainer.getLeft();
				Display display = getWindowManager().getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				float proportion = (float)(0);
				
				float greenStart = ((float)(0.2)*( greenX - (float)sliderContainer.getRight() ) + (float)sliderContainer.getRight());
				
				float redStart = ( (float)sliderContainer.getLeft() - ((float)(0.2)*(  (float)sliderContainer.getLeft() - (float)declineCall.getRight() )) );
				
				Log.d("Difference",((Float)(sliderX)).toString());
				Log.d("Difference",((Float)(greenX)).toString());
				Log.d("Difference",((Float)(redX)).toString());
				
				acceptCall.setX(greenStart);
				declineCall.setX(redStart-(declineCall.getRight()-declineCall.getLeft()));
				animsetAccept = (AnimatorSet)AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.voip_arrow_alpha_translation);
				animsetDecline = (AnimatorSet)AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.voip_arrow_alpha_translation);
				animsetAccept.setTarget(acceptCall);
				animsetDecline.setTarget(declineCall);
				animsetDecline.start();
				animsetAccept.start();
				sliderRotator = AnimationUtils.loadAnimation(getBaseContext(), R.anim.voip_slider_rotation_anim);
				callSlider.setAnimation(sliderRotator);
				sliderRotator.start();
		        sliderListener = new SliderOnTouchListener();
				sliderContainer.setOnTouchListener(sliderListener);
				callSlider.getBackground().setAlpha((int) ( 0 ));
				
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
		acceptCall.setVisibility(View.INVISIBLE);
		declineCall.setVisibility(View.INVISIBLE);
		screenOff();
		callSlider.setBackgroundResource(R.drawable.slider_oval_red);
		callSlider.setRotation(0);
		AnimatorSet animset = (AnimatorSet)AnimatorInflater.loadAnimator(getBaseContext(), R.animator.voip_avatar_translator);
		animset.setInterpolator(new OvershootInterpolator(2.5f));
		animset.setTarget(sliderContainer);
		animset.start();
//		setContentView(R.layout.incall_layout);
//		displayPic = (ImageView)this.findViewById(R.id.inCallContactPicture1);
//		setDisplayPic();
//		dpAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.voip_dp_bounce);
//		displayPic.startAnimation(dpAnim);
//		displayPic.animate();
		muteButton =(ImageView)this.findViewById(R.id.fullMicButton);
		muteButton.setVisibility(View.VISIBLE);
		muteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v){
				VoIPServiceNew.getVoIPSerivceInstance().muteClicked();
				changeMuteButton();
			}
			
		});
		speakerButton = (ImageView)this.findViewById(R.id.fullSpeakerButton);
		speakerButton.setVisibility(View.VISIBLE);
//		speakerSound = (ImageView)this.findViewById(R.id.speakerSound);
//		micSlash = (ImageView)this.findViewById(R.id.micSlash);
		speakerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				VoIPServiceNew.getVoIPSerivceInstance().speakerClicked();	
				changeSpeakerButton();
			}
		});
		sliderContainer.setOnTouchListener(null);
		sliderContainer.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				raiseEndCallToast();
				VoIPServiceNew.getVoIPSerivceInstance().endCall();
				finish();
			}
			
		});
//		endCall = (ImageButton)this.findViewById(R.id.endCallButton1);
//		endCall.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				raiseEndCallToast();
//				VoIPServiceNew.getVoIPSerivceInstance().endCall();
//				finish();
//			}
//		});
		
//		inCallCallNo = (TextView)this.findViewById(R.id.PhoneNumberView1);
//		inCallCallNo.setText(HikeUserDatabase.getInstance().getContactInfoFromPhoneNo(storedId).getNameOrMsisdn());
//		inCallCallNo.setText(mContactName);
		inCallTimer = (TextView)this.findViewById(R.id.fullPhoneNumberView1);
		if (VoIPServiceNew.getVoIPSerivceInstance().client.connectionState != "CONNECTED")
			inCallTimer.setText(VoIPServiceNew.getVoIPSerivceInstance().client.connectionState);
		else{
			displayPic.clearAnimation();
			Log.d("Animation","Seriously?");
			dpAnim.cancel();
			dpAnim.reset();
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
//				displayPic.clearAnimation();
				dpAnim.cancel();
				dpAnim.reset();
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
			muteButton.setImageResource(R.drawable.voip_mute_mic);
//			micSlash.setVisibility(ImageView.VISIBLE);
			isMute = false;
		} else {
			muteButton.setImageResource(R.drawable.voip_mute_off);
//			micSlash.setVisibility(ImageView.INVISIBLE);
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
	
	private void setDisplayPic()
	{
		if (displayPic == null)
		{
			Log.d("displayPic","is Null");
			return;
		}

		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(mContactNumber, true);
		Log.d("ContactNumber",mContactNumber);
		if (drawable != null)
		{
			displayPic.setScaleType(ScaleType.FIT_CENTER);
			displayPic.setImageDrawable(drawable);
			displayPic.setBackgroundDrawable(null);
		}
		else
		{
			displayPic.setScaleType(ScaleType.CENTER_INSIDE);
			displayPic.setImageResource(R.drawable.ic_default_avatar);
			displayPic.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(mContactNumber, true));
		}
	}
	
}
