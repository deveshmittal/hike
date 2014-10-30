package com.bsb.hike.ui;

import com.bsb.hike.R;
import com.bsb.hike.service.VoIPServiceNew;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ReceiveCallActivity extends Activity {
	
	private String callerId;
	private TextView callNo;
	private Button acceptCall;
	private Button declineCall;
	private VoIPServiceNew vService;
	private Uri notification;
	private Ringtone r;
	private boolean callStarted;
	
	public void onCreate(){
		if(getIntent().hasExtra("callerID")){
			callerId = getIntent().getStringExtra("callerID");
		}
		final Intent i = new Intent(this,com.bsb.hike.service.VoIPServiceNew.class);
		i.putExtras(getIntent().getExtras());
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
		callNo = (TextView)this.findViewById(R.id.CallerId);
		callNo.setText("Incoming Number Goes Here!");
		acceptCall = (Button)this.findViewById(R.id.acceptButton);
		acceptCall.setBackgroundColor(Color.GREEN);
		acceptCall.setTextColor(Color.WHITE);
		acceptCall.setOnClickListener(new OnClickListener(){


			@Override
			public void onClick(View v) {
//				player.stop();
				Intent intent = i;
				intent.putExtra("decline", false);				
				vService = VoIPServiceNew.getVoIPSerivceInstance();
				vService.startCall(intent);
				intent.removeExtra("callerID");
				intent.putExtra("dialedID", callerId);				
				Intent inCallIntent = new Intent(getApplicationContext(),com.bsb.hike.ui.VoIPActivityNew.class);
				callStarted = true;
				startActivity(inCallIntent);
//				drawInCall();
				//TODO: CALL OTHER VOIP ACTIVITY
			}
			
		});
		
		declineCall = (Button)this.findViewById(R.id.declineButton);
		declineCall.setBackgroundColor(Color.RED);
		declineCall.setTextColor(Color.WHITE);
		declineCall.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
//				player.stop();
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
	
}
