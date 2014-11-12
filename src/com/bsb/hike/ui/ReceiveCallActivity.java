package com.bsb.hike.ui;


import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.VoIPServiceNew;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.HikePubSub;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

public class ReceiveCallActivity extends Activity implements HikePubSub.Listener {
	
	private String callerId;
	private TextView callNo;
	private ImageView acceptCall;
	private ImageView declineCall;
	private ImageView displayPic;
	private VoIPServiceNew vService;
	private Uri notification;
	private Ringtone r;
	private boolean callStarted;
	private String mContactName;
	private String mContactNumber;
	private Context prefs;
	private Animation dpAnim;
	private Animation springAnim;
	private FrameLayout avatarLayout;
	private LayoutParams dpParams;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(getIntent().hasExtra("callerID")){
			callerId = getIntent().getStringExtra("callerID");
		}
		ContactInfo contactInfo = ContactManager.getInstance().getContact(callerId, true, true);
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
		setFinishOnTouchOutside(false);
		final Intent i = new Intent(this,com.bsb.hike.service.VoIPServiceNew.class);
		i.putExtras(getIntent().getExtras());
//		setTheme(android.R.style.Theme_Dialog);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		setTheme(android.R.style.Theme_DeviceDefault_DialogWhenLarge);
		setContentView(R.layout.call_accept_decline);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		WindowManager.LayoutParams params = getWindow().getAttributes();
		Log.d("Screen Size x",((Integer)(params.x)).toString());
		Log.d("Screen Size y",((Integer)(params.y)).toString());
		Log.d("Screen Size h",((Integer)(params.height)).toString());
		Log.d("Screen Size w",((Integer)(params.width)).toString());
		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize(size);
		int width = size.x;
		int height = size.y;
		getWindow().setLayout(((int)(float)(0.85*(float)(width))), (int)((float)(0.60*(float)height)));
		notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		r.setStreamType(AudioManager.STREAM_ALARM);
//		r.play();
		displayPic = (ImageView)this.findViewById(R.id.voipContactPicture);
		avatarLayout = (FrameLayout)this.findViewById(R.id.voip_avatar_container);
		setDisplayPic();
		dpAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.voip_dp_bounce);
		dpParams = avatarLayout.getLayoutParams();
		avatarLayout.startAnimation(dpAnim);
		callNo = (TextView)this.findViewById(R.id.CallerId);
		callNo.setText(mContactName);
		acceptCall = (ImageView)this.findViewById(R.id.acceptButton);
//		acceptCall.setBackgroundColor(Color.GREEN);
//		acceptCall.setTextColor(Color.WHITE);
//		acceptCall.setOnClickListener(new OnClickListener(){
//
//
//			@Override
//			public void onClick(View v) {
////				r.stop();
//				Intent intent = i;
//				intent.putExtra("decline", false);				
//				vService = VoIPServiceNew.getVoIPSerivceInstance();
//				vService.startCall(intent);
//				intent.removeExtra("callerID");
//				intent.putExtra("dialedID", callerId);				
//				Intent inCallIntent = new Intent(getApplicationContext(),com.bsb.hike.ui.VoIPActivityNew.class);
//				inCallIntent.putExtras(intent);
//				callStarted = true;
//				displayPic.clearAnimation();
//				dpAnim.cancel();
//				dpAnim.reset();
//				startActivity(inCallIntent);
////				drawInCall();
//				//TODO: CALL OTHER VOIP ACTIVITY
//			}
//			
//		});
		
		declineCall = (ImageView)this.findViewById(R.id.declineButton);
//		declineCall.setBackgroundColor(Color.RED);
//		declineCall.setTextColor(Color.WHITE);
//		declineCall.setOnClickListener(new OnClickListener(){
//
//			@Override
//			public void onClick(View v) {
//				r.stop();
//				Intent intent = i;
//				intent.putExtra("decline", true);				
//				vService = VoIPServiceNew.getVoIPSerivceInstance();
//				vService.startCall(intent);
//				finish();
//			}
//			
//		});
		setImageDrag();
		
	}
	
	public void onBackPressed(){
	}
	
	//For dragging the image///////////////////////////////////
	
	private void setImageDrag() {
        if (android.os.Build.VERSION.SDK_INT < 11) 
        	return;
        ImageView iv = (ImageView) findViewById(R.id.declineButton);
        final float x = avatarLayout.getPaddingLeft();
        Log.d("TouchEvent","1 "+( (Float) ( avatarLayout.getX() ) ).toString()  );
        Log.d("TouchEvent","2 "+( (Float) ( avatarLayout.getTranslationX() ) ).toString()  );
		
//		iv.setOnDragListener(new MyDragListener());
		iv = (ImageView) findViewById(R.id.acceptButton);
		iv.setTag("green");
        findViewById(R.id.voip_avatar_container).setOnTouchListener(new OnTouchListener()
		{
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
		                Log.d("TouchEvent","3 "+( (Float) ( v.getX() ) ).toString()  );
		                Log.d("TouchEvent","4 "+( (Float) ( avatarLayout.getX() ) ).toString()  );
		                Log.d("TouchEvent","5 "+( (Float) ( avatarLayout.getTranslationX() ) ).toString()  );
		                if(((v.getX()<acceptCall.getX())&&(v.getX()>declineCall.getX())))
		                avatarLayout.setX((int)(StartPT.x+mv.x));
//		                declineCall.setY((int)(StartPT.y+mv.y));
		                StartPT = new PointF( avatarLayout.getX(), avatarLayout.getY() );
		                break;
		            case MotionEvent.ACTION_DOWN :
		                DownPT.x = event.getX();
		                Log.d("TouchEvent","Start PT"+( (Float) ( v.getX() ) ).toString()  );
		                DownPT.y = event.getY();
		                avatarLayout.clearAnimation();
		                StartPT = new PointF( avatarLayout.getX(), avatarLayout.getY() );
		                break;
		            case MotionEvent.ACTION_UP :
		            	float xcoord = v.getX();
		            	if (xcoord<declineCall.getX())
							{
		            		
		        				r.stop();
		        				Vibrator v1 = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
								v1.vibrate(100);
		        				Intent intent = new Intent(getApplicationContext(),com.bsb.hike.service.VoIPServiceNew.class);;
		        				intent.putExtra("decline", true);	
		        				intent.putExtras(getIntent().getExtras());
		        				vService = VoIPServiceNew.getVoIPSerivceInstance();
		        				vService.startCall(intent);
		        				finish();
		        			
							}
						else if (xcoord>acceptCall.getX())
							{
							Intent intent = new Intent(getApplicationContext(),com.bsb.hike.service.VoIPServiceNew.class);
							intent.putExtra("decline", false);
							Vibrator v1 = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
							v1.vibrate(100);
							intent.putExtras(getIntent().getExtras());
							vService = VoIPServiceNew.getVoIPSerivceInstance();
							vService.startCall(intent);
							intent.removeExtra("callerID");
							intent.putExtra("dialedID", callerId);				
							Intent inCallIntent = new Intent(getApplicationContext(),com.bsb.hike.ui.VoIPActivityNew.class);
							inCallIntent.putExtras(intent);
							callStarted = true;
							displayPic.clearAnimation();
							dpAnim.cancel();
							dpAnim.reset();
							startActivity(inCallIntent);
							} else {
								TranslateAnimation ta = new TranslateAnimation(0,(-1) * avatarLayout.getTranslationX(), 0, (-1)*avatarLayout.getTranslationY());
								ta.setFillAfter(true);
								ta.setFillEnabled(true);
								ta.setDuration(150);
								ta.setRepeatCount(0);
								avatarLayout.startAnimation(ta);
								ta.setAnimationListener(new AnimationListener() {
									
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
//										avatarLayout.setLayoutParams(dpParams);
//										avatarLayout.setAnimation(dpAnim);
										drawAnswerCall();
//										setContentView(R.layout.call_accept_decline);
										
									}
							
								});
//								springAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.full_voip_button_translate);
//								avatarLayout.startAnimation(springAnim);
							}
			                break;
		            default :
		                break;
		        }
		        return true;
		    }
		});
        
	}
	
	public void onPause(){
		if (callStarted){
			finish();
		}
		super.onPause();
	}
	
	public void onDestroy(){
		((AudioManager) getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_NORMAL);
		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if(type == HikePubSub.VOIP_FINISHED)
		{
			finish();
		}
		
	}
	
	private void drawAnswerCall(){
		setContentView(R.layout.call_accept_decline);
		displayPic = (ImageView)this.findViewById(R.id.voipContactPicture);
		avatarLayout = (FrameLayout)this.findViewById(R.id.voip_avatar_container);
		setDisplayPic();
		dpAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.voip_dp_bounce);
		dpParams = avatarLayout.getLayoutParams();
		avatarLayout.startAnimation(dpAnim);
		callNo = (TextView)this.findViewById(R.id.CallerId);
		callNo.setText(mContactName);
		acceptCall = (ImageView)this.findViewById(R.id.acceptButton);
		declineCall = (ImageView)this.findViewById(R.id.declineButton);
		setImageDrag();
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
