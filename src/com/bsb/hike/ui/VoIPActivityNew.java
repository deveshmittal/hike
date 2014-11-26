package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
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
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.VoIPServiceNew;
import com.bsb.hike.smartImageLoader.ProfilePicImageLoader;


public class VoIPActivityNew extends Activity implements HikePubSub.Listener{

	private String callerId;
	private String dialedId;
	private ImageButton endCall;
	private ImageView acceptCall;
	private ImageView declineCall;
	private ImageView callSlider;
	private ImageView muteButton;
	private ImageView speakerSound;
	private FrameLayout miniAvatar;
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
	private FrameLayout avatarContainer;
	private ImageView speakerButton;
	private float delY;
	public Vibrator vibrator;
	private FrameLayout muteFrame;
	private FrameLayout speakerFrame;
	private TranslateAnimation yTranslator;
	protected boolean translationStarted;
	protected boolean dpMoveStarted;
	protected AnimatorSet DPReposition;
	private RelativeLayout innerLayout;
	
	private final BroadcastReceiver endCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
              finish();                                   
        }
	};
	protected TextView preCallTimer;
	
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
		    avatarContainer.clearAnimation();
		    displayHandler.postDelayed(new CallLengthManager(), 500);
		    DPReposition.end();
			
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
		                Log.d("TouchEvent","Xp"+((Float)callSlider.getPivotX()).toString());
		                Log.d("TouchEvent","Yp"+((Float)callSlider.getPivotY()).toString());
		                float rotAng = (float)(120)*(sliderContainer.getTranslationX()/(float)((greenX-sliderX)));
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
		            case MotionEvent.ACTION_DOWN :
		                DownPT.x = event.getX();
		                Log.d("TouchEvent","Start PT"+( (Float) ( v.getX() ) ).toString()  );
		                DownPT.y = event.getY();
		                callSlider.clearAnimation();
		                StartPT = new PointF( (sliderContainer).getX(), sliderContainer.getY() );
		                animsetAccept.end();
		                animsetDecline.end();
		                break;
		            case MotionEvent.ACTION_UP :
		            	
		            	PointF mvevent = new PointF( event.getX() - DownPT.x, event.getY() - DownPT.y);
		            	float xcoord = StartPT.x+mvevent.x;
		            	if (xcoord<7.0f*redX/6.0f)
							{
		            		
//		        				r.stop();
		        				vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
								vibrator.vibrate(100);
		        				Intent intent = new Intent(getApplicationContext(),com.bsb.hike.service.VoIPServiceNew.class);;
		        				intent.putExtra("decline", true);	
		        				intent.putExtras(getIntent().getExtras());
			    				callConnected = true;
			    				vService = VoIPServiceNew.getVoIPSerivceInstance();
			    				vService.startCall(intent);
			    				finish();
		        			
							}
						else if (xcoord>5.0f*greenX/6.0f)
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
								AnimatorSet animset = (AnimatorSet)AnimatorInflater.loadAnimator(getBaseContext(), R.animator.voip_avatar_translator);
								animset.setInterpolator(new OvershootInterpolator(2.5f));
								animset.setTarget(sliderContainer);
								animset.start();


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
		registerReceiver(endCallReceiver, new IntentFilter("FinishVoipActivites"));
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
					mContactName = mContactNumber = callerId;
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
		if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL ){
//			r.play();
			isPlaying = true;
			Log.w("Ringer Starting", "Audio Starting");
		} else if (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
			vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			long[] vibTimings = {0,300,1000,1300,2000,2300};
			if(vibrator.hasVibrator()){
				vibrator.vibrate(vibTimings, 0);				
			}
		}
		setContentView(R.layout.full_call_accept_decline);
		displayPic = (ImageView)this.findViewById(R.id.fullvoipContactPicture);
		avatarContainer = (FrameLayout)this.findViewById(R.id.voip_avatar_container);
		dpAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.voip_dp_bounce);
		avatarContainer.setAnimation(dpAnim);
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
		preCallTimer = (TextView)this.findViewById(R.id.fullPhoneNumberView1);
		innerLayout  = (RelativeLayout) this.findViewById(R.id.full_voip_inner_layout);
		
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
				delY = callNo.getY()-preCallTimer.getY();
				Animation fadeIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.call_fade_in);
				if(!dpMoveStarted){
					innerLayout.setAnimation(fadeIn);
					yTranslator  = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, -1000, Animation.RELATIVE_TO_SELF, 0);
					yTranslator.setInterpolator(new DecelerateInterpolator());
					yTranslator.setDuration(1000);
					yTranslator.setFillAfter(true);
					yTranslator.setFillEnabled(true);
					avatarContainer.setAnimation(yTranslator);
					yTranslator.setAnimationListener(new AnimationListener() {
						
						@Override
						public void onAnimationStart(Animation animation) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							avatarContainer.clearAnimation();
							avatarContainer.setAnimation(dpAnim);
							dpAnim.start();
							
						}
					});
					yTranslator.start();
				
					fadeIn.setAnimationListener(new AnimationListener() {
						
						@Override
						public void onAnimationStart(Animation animation) {
							
							
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {
							// TODO Auto-generated method stub
							
						}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							preCallTimer.setVisibility(View.VISIBLE);
							callSlider.setVisibility(View.VISIBLE);
							sliderContainer.setVisibility(View.VISIBLE);
							callNo.setVisibility(View.VISIBLE);
							Animation sliderFadeIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.call_fade_in);
							callSlider.setAnimation(sliderFadeIn);
							Animation textFadeIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.call_fade_in);
							textFadeIn.setFillAfter(true);
							textFadeIn.setFillEnabled(true);
							sliderFadeIn.setFillAfter(true);
							sliderFadeIn.setFillEnabled(true);
							preCallTimer.setAnimation(textFadeIn);
							callNo.setAnimation(textFadeIn);
							sliderFadeIn.start();
							textFadeIn.start();
							
							
						}
					});
					fadeIn.start();
					dpMoveStarted = true;
					DPReposition = (AnimatorSet) AnimatorInflater.loadAnimator(getBaseContext(), R.animator.dp_translate_scale_anim);
				}
		    }  
		}); 
		
		
		
	}
	
	public void prepareInCall(){
		final Intent i = new Intent(this, com.bsb.hike.service.VoIPServiceNew.class);		
		i.putExtras(getIntent().getExtras());
		callConnected = true;
		vActivity = this;
		vService = VoIPServiceNew.getVoIPSerivceInstance();
		setContentView(R.layout.full_call_accept_decline);
		if (VoIPServiceNew.vService == null)
			Log.d("vService", "NULL HAI!!!");
		vService.startCall(i);
		acceptCall = (ImageView)this.findViewById(R.id.fullacceptButton);
		callNo = (TextView)this.findViewById(R.id.fullCallerId);
		
		miniAvatar = (FrameLayout)this.findViewById(R.id.full_small_dp_container);
		avatarContainer = (FrameLayout)this.findViewById(R.id.voip_avatar_container);
		displayPic = (ImageView)this.findViewById(R.id.fullvoipContactPicture);
		innerLayout  = (RelativeLayout) this.findViewById(R.id.full_voip_inner_layout);
		
		final ViewTreeObserver vto = acceptCall.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener(){

			@Override
			public void onGlobalLayout() {				
//				vto.removeOnGlobalLayoutListener(this);
				preCallTimer = (TextView)findViewById(R.id.fullPhoneNumberView1);
				preCallTimer.setVisibility(View.INVISIBLE);
				inCallTimer = (TextView)findViewById(R.id.fullCallTimer);
				if (!translationStarted){
					delY = callNo.getY()-inCallTimer.getY();
					yTranslator  = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, delY);
					yTranslator.setDuration(500);
					yTranslator.setFillAfter(true);
					yTranslator.setFillEnabled(true);
					callNo.setAnimation(yTranslator);
					preCallTimer = (TextView)findViewById(R.id.fullPhoneNumberView1);
					preCallTimer.setVisibility(View.INVISIBLE);
					yTranslator.start();
					
					translationStarted = true;
				}
				float x = miniAvatar.getX();
				float y = miniAvatar.getY();
				callSlider.setVisibility(View.VISIBLE);
				sliderContainer.setVisibility(View.VISIBLE);
				
				if(!dpMoveStarted){
					avatarContainer.setScaleX(0.28f);
					avatarContainer.setScaleY(0.28f);
					avatarContainer.setX(x);
					avatarContainer.setY(y);
					dpMoveStarted = true;
					Animation fadeIn = AnimationUtils.loadAnimation(getBaseContext(), R.anim.call_fade_in);
					innerLayout.setAnimation(fadeIn);
					fadeIn.setAnimationListener(new AnimationListener() {
						
						@Override
						public void onAnimationStart(Animation animation) {
								
						}
						
						@Override
						public void onAnimationRepeat(Animation animation) {
							
						}
						
						@Override
						public void onAnimationEnd(Animation animation) {
														
						}
					});
					fadeIn.start();
					DPReposition = (AnimatorSet) AnimatorInflater.loadAnimator(getBaseContext(), R.animator.dp_translate_scale_anim);
					DPReposition.setTarget(avatarContainer);
					DPReposition.start();
				}
			}
			
		});
		drawInCall();
	}
	
	public void prepareResume(){
		vService = VoIPServiceNew.getVoIPSerivceInstance();
		setContentView(R.layout.full_call_accept_decline);
		sliderContainer = (FrameLayout) this.findViewById(R.id.voip_slider_container);
		callSlider = (ImageView)this.findViewById(R.id.fullcallSlider);
		sliderContainer.setVisibility(View.VISIBLE);
		callSlider.setVisibility(View.VISIBLE);
		callNo = (TextView)this.findViewById(R.id.fullCallerId);
		ViewTreeObserver vto = callSlider.getViewTreeObserver();
		preCallTimer = (TextView)this.findViewById(R.id.fullPhoneNumberView1);
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				
				callNo.setTop(preCallTimer.getTop());
				
			}
		});
		
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
//		vibrator.cancel();
		acceptCall = (ImageView)this.findViewById(R.id.fullacceptButton);
		acceptCall.setVisibility(View.INVISIBLE);
		preCallTimer = (TextView)this.findViewById(R.id.fullPhoneNumberView1);
		preCallTimer.setVisibility(View.INVISIBLE);
		
		declineCall = (ImageView)this.findViewById(R.id.fulldeclineButton);
		declineCall.setVisibility(View.INVISIBLE);
		dpAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.voip_dp_bounce);
		screenOff();
		callSlider = (ImageView)this.findViewById(R.id.fullcallSlider);
//		callSlider.setClickable(true);
		callSlider.setBackgroundResource(R.drawable.slider_oval_red);
		callSlider.setImageResource(R.drawable.cut_call);
		callSlider.setRotation(0);
		AnimatorSet animset = (AnimatorSet)AnimatorInflater.loadAnimator(getBaseContext(), R.animator.voip_avatar_translator);
		animset.setInterpolator(new OvershootInterpolator(2.5f));
		sliderContainer = (FrameLayout)this.findViewById(R.id.voip_slider_container);
		animset.setTarget(sliderContainer);
		animset.start();

		callNo = (TextView)this.findViewById(R.id.fullCallerId);
		if(!translationStarted){
			yTranslator  = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, -1*delY);
			yTranslator.setDuration(500);
			yTranslator.setFillAfter(true);
			yTranslator.setFillEnabled(true);
			preCallTimer = (TextView)this.findViewById(R.id.fullPhoneNumberView1);
			preCallTimer.setAlpha(0.0f);
			preCallTimer.setVisibility(View.INVISIBLE);
			callNo.setAnimation(yTranslator);
			yTranslator.start();
		}
		callNo.setText(mContactName);
		avatarContainer = (FrameLayout)this.findViewById(R.id.voip_avatar_container);
		inCallTimer = (TextView)this.findViewById(R.id.fullCallTimer);
		inCallTimer.setText("CONNECTING...");
//		inCallTimer.setAlpha(0);
		Animation timerAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation);
		inCallTimer.setAnimation(timerAnim);
//		setContentView(R.layout.incall_layout);
		displayPic = (ImageView)this.findViewById(R.id.fullvoipContactPicture);
//		displayPic = (ImageView)this.findViewById(R.id.inCallContactPicture1);
		setDisplayPic();
//		dpAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.voip_dp_bounce);
//		displayPic.startAnimation(dpAnim);
//		displayPic.animate();
		muteFrame = (FrameLayout) this.findViewById(R.id.fullMicButtonFrame);
		muteButton =(ImageView)this.findViewById(R.id.fullMicButton);
		muteButton.setVisibility(View.VISIBLE);
		muteFrame.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v){
				VoIPServiceNew.getVoIPSerivceInstance().muteClicked();
				changeMuteButton();
			}
			
		});
		
		speakerFrame = (FrameLayout) this.findViewById(R.id.fullSpeakerButtonFrame);
		speakerButton = (ImageView)this.findViewById(R.id.fullSpeakerButton);
		speakerButton.setVisibility(View.VISIBLE);
		speakerSound = (ImageView)this.findViewById(R.id.fullSpeakerSound);
		speakerSound.setVisibility(View.VISIBLE);
		speakerFrame.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				VoIPServiceNew.getVoIPSerivceInstance().speakerClicked();	
				changeSpeakerButton();
			}
			
		});
		sliderContainer.setOnTouchListener(null);
		sliderContainer.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				int eid = event.getAction();
		        switch (eid)
		        {
		        	case MotionEvent.ACTION_DOWN:
		        		callSlider.setImageResource(R.drawable.callicon);
		        		break;
		        	case MotionEvent.ACTION_UP:
		        		Log.d("TRYING", "to end call");
		        		callSlider.setImageResource(R.drawable.cut_call);
		        		VoIPServiceNew.getVoIPSerivceInstance().endCall();
						finish();
						break;
		        }
				
				return true;
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
//		inCallTimer = (TextView)this.findViewById(R.id.fullPhoneNumberView1);
		if (VoIPServiceNew.getVoIPSerivceInstance().client.connectionState != "CONNECTED"){
			inCallTimer.setText("CONNECTING...");
			inCallTimer.setVisibility(View.VISIBLE);
			timerAnim.start();
		}
		else{
			avatarContainer.clearAnimation();
			Log.d("Animation","Seriously?");
			avatarContainer.clearAnimation();
			dpAnim.cancel();
			dpAnim.reset();
			startTime = vService.client.startTime;
			inCallTimer.setVisibility(View.VISIBLE);
			displayHandler.post(new CallLengthManager());
			
			timerAnim.start();
		}
		
		
		
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (type == HikePubSub.VOIP_FINISHED){
//			finish();
		}
		else if(type == HikePubSub.VOIP_HANDSHAKE){
			try {
				JSONObject json = (JSONObject) object;
				JSONObject data = (JSONObject) json.get(HikeConstants.DATA);
				JSONObject metadata = (JSONObject) data.get(HikeConstants.METADATA);
				String mdType = metadata.getString("type");
				if (mdType.equals(HikeConstants.MqttMessageTypes.VOIP_END_CALL));
//					finish();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (type == HikePubSub.VOIP_CALL_STATUS_CHANGED){
			String state = (String) object;
//			inCallTimer.setText(state);
			if(state == PeerConnection.IceConnectionState.CONNECTED.toString()){
//				displayPic.clearAnimation();
//				dpAnim.cancel();
//				dpAnim.reset();
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
		unregisterReceiver(endCallReceiver);
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
//			speakerButton.setImageResource(R.drawable.speaker_on);
			isSpeakerOn = true;
		} else {
			speakerSound.setVisibility(ImageView.VISIBLE);
//			speakerButton.setImageResource(R.drawable.speaker_off);
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
	    registerReceiver(endCallReceiver, new IntentFilter("FinishVoipActivities"));
	  }

	
	@Override
	  protected void onPause() {
	    // Be sure to unregister the sensor when the activity pauses.
//	    mSensorManager.unregisterListener(this);
//	    mMediaPlayer.release();
	    super.onPause();
	  }



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
		screenOffLock.release();
		
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

//		Drawable drawable = HikeMessengerApp.getLruCache().getIconFromCache(mContactNumber, true);
		int mBigImageSize = getResources().getDimensionPixelSize(R.dimen.voip_avatar_size);
		(new ProfilePicImageLoader(this, mBigImageSize)).loadImage(mContactNumber+"pp", displayPic, false, false, true);
		Bitmap img = getRoundedShape(((BitmapDrawable)displayPic.getDrawable()).getBitmap());
		displayPic.setImageBitmap(img);
		Log.d("ContactNumber",mContactNumber);
//		if (drawable != null)
//		{
//			displayPic.setScaleType(ScaleType.FIT_CENTER);
//			displayPic.setImageDrawable(drawable);
//			displayPic.setBackgroundDrawable(null);
//		}
//		else
//		{
//			displayPic.setScaleType(ScaleType.CENTER_INSIDE);
//			displayPic.setImageResource(R.drawable.ic_default_avatar);
//			displayPic.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(mContactNumber, true));
//		}
	}


	public void calleeNotOnWifi() {
		
		Log.d("DialogBulderVoip", "activity");
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				new AlertDialog.Builder(VoIPActivityNew.this)
			    .setTitle("Callee Not on Wifi")
			    .setMessage("Your friend is not on Wifi. Would you like us to send a message to show up?")
			    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
			        	try {
			        		JSONObject message = new JSONObject();
			        		JSONObject data = new JSONObject();
			        		
			        		
			        		
			        		message.put(HikeConstants.TO, dialedId);
			        		message.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE);
			        		data.put(HikeConstants.MESSAGE_ID, 3745);
			        		
			        		data.put("hm", "WIFI PE AAJA!!!");
			        		long time = (long) System.currentTimeMillis();
							data.put(HikeConstants.TIMESTAMP, time);
							message.put(HikeConstants.DATA,data);
							HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, message);
			        		
			        	} catch (JSONException e) {
			        		e.printStackTrace();
			        	}
			        }
			     })
			    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) { 
			            // do nothing
			        }
			     })
			    .setIcon(android.R.drawable.ic_dialog_alert)
			     .show();
				
			}
		});
	}
	
	
	public Bitmap getRoundedShape(Bitmap scaleBitmapImage) {
	    float targetWidth = 125.0f*1.5f;
	    float targetHeight = 125.0f*1.5f;
	    Bitmap targetBitmap = Bitmap.createBitmap((int)targetWidth, 
	                        (int)targetHeight,Bitmap.Config.ARGB_8888);

	    Canvas canvas = new Canvas(targetBitmap);
	    Path path = new Path();
	    path.addCircle(((float) targetWidth - 1) / 2,
	        ((float) targetHeight - 1) / 2,
	        (Math.min(((float) targetWidth), 
	        ((float) targetHeight)) / 2),
	        Path.Direction.CCW);

	    canvas.clipPath(path);
	    Bitmap sourceBitmap = scaleBitmapImage;
	    canvas.drawBitmap(sourceBitmap, 
	        new Rect(0, 0, sourceBitmap.getWidth(),
	        sourceBitmap.getHeight()), 
	        new Rect(0, 0, (int)targetWidth, (int)targetHeight), null);
	    return targetBitmap;
	}



	
}
