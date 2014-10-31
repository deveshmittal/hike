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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

public class ReceiveCallActivity extends Activity implements HikePubSub.Listener {
	
	private String callerId;
	private TextView callNo;
	private ImageButton acceptCall;
	private ImageButton declineCall;
	private ImageView displayPic;
	private VoIPServiceNew vService;
	private Uri notification;
	private Ringtone r;
	private boolean callStarted;
	private String mContactName;
	private String mContactNumber;
	private Context prefs;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(getIntent().hasExtra("callerID")){
			callerId = getIntent().getStringExtra("callerID");
		}
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
		setFinishOnTouchOutside(false);
		final Intent i = new Intent(this,com.bsb.hike.service.VoIPServiceNew.class);
		i.putExtras(getIntent().getExtras());
//		setTheme(android.R.style.Theme_Dialog);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		setTheme(android.R.style.Theme_DeviceDefault_DialogWhenLarge);
		setContentView(R.layout.call_accept_decline);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		r.setStreamType(AudioManager.STREAM_ALARM);
		r.play();
		displayPic = (ImageView)this.findViewById(R.id.voipContactPicture);
		setDisplayPic();
		callNo = (TextView)this.findViewById(R.id.CallerId);
		callNo.setText(mContactName);
		acceptCall = (ImageButton)this.findViewById(R.id.acceptButton);
//		acceptCall.setBackgroundColor(Color.GREEN);
//		acceptCall.setTextColor(Color.WHITE);
		acceptCall.setOnClickListener(new OnClickListener(){


			@Override
			public void onClick(View v) {
				r.stop();
				Intent intent = i;
				intent.putExtra("decline", false);				
				vService = VoIPServiceNew.getVoIPSerivceInstance();
				vService.startCall(intent);
				intent.removeExtra("callerID");
				intent.putExtra("dialedID", callerId);				
				Intent inCallIntent = new Intent(getApplicationContext(),com.bsb.hike.ui.VoIPActivityNew.class);
				inCallIntent.putExtras(intent);
				callStarted = true;
				startActivity(inCallIntent);
//				drawInCall();
				//TODO: CALL OTHER VOIP ACTIVITY
			}
			
		});
		
		declineCall = (ImageButton)this.findViewById(R.id.declineButton);
//		declineCall.setBackgroundColor(Color.RED);
//		declineCall.setTextColor(Color.WHITE);
		declineCall.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				r.stop();
				Intent intent = i;
				intent.putExtra("decline", true);				
				vService = VoIPServiceNew.getVoIPSerivceInstance();
				vService.startCall(intent);
				finish();
			}
			
		});
		
	}
	
	public void onBackPressed(){
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
