package com.bsb.hike.ui;

import org.json.JSONObject;
import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.service.VoIPServiceNew;
import com.bsb.hike.ui.VoIPActivity.MessageHandler;

public class VoIPActivityNew extends Activity implements HikePubSub.Listener {

	private String callerId;
	private String dialedId;
	private Button endCall;
	private Button acceptCall;
	private Button declineCall;
	private TextView callNo;
	private int serviceId = BIND_AUTO_CREATE;	
	private HikePubSub mPubSub = HikeMessengerApp.getPubSub();
	public static Handler messageHandler = new MessageHandler();
	public static MessageHandler serviceHandler = new MessageHandler();
	private String resumeId = null;
	private VoIPServiceNew vService;
	private static VoIPActivityNew vActivity;
	public boolean callConnected = false;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		vActivity = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mPubSub.addListener(HikePubSub.VOIP_HANDSHAKE, this);
		if(getIntent().hasExtra("callerID")){
			callerId = getIntent().getStringExtra("callerID");
			prepareAnswer();
		} else if (getIntent().hasExtra("dialedID")){
			dialedId = getIntent().getStringExtra("dialedID");
			prepareInCall();
		} else {
			resumeId = getIntent().getStringExtra("resumeId");
			prepareResume();			
		}
	}
	
	public void prepareAnswer(){
		final Intent i = new Intent(this,com.bsb.hike.service.VoIPServiceNew.class);
		i.putExtras(getIntent().getExtras());
		setContentView(R.layout.call_accept_decline);
		vActivity = this;
		callNo = (TextView)this.findViewById(R.id.CallerId);
		callNo.setText(callerId);
		acceptCall = (Button)this.findViewById(R.id.acceptButton);
		acceptCall.setBackgroundColor(Color.GREEN);
		acceptCall.setTextColor(Color.WHITE);
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
		
		declineCall = (Button)this.findViewById(R.id.declineButton);
		declineCall.setBackgroundColor(Color.RED);
		declineCall.setTextColor(Color.WHITE);
		declineCall.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
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

//		try {
//			wait(500);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		vService = VoIPServiceNew.getVoIPSerivceInstance();
		drawInCall();
	}
	
	public void prepareResume(){
		vService = VoIPServiceNew.getVoIPSerivceInstance();
		drawInCall();
	}
	
	public void drawInCall(){
		setContentView(R.layout.incall_layout);
		endCall = (Button)this.findViewById(R.id.endCallButton);
		endCall.setBackgroundColor(Color.RED);
		endCall.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				VoIPServiceNew.getVoIPSerivceInstance().endCall();
				finish();
			}
		});
	}

	@Override
	public void onEventReceived(String type, Object object) {
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
	}
	
	public static VoIPActivityNew getVoIPActivityInstance(){
		return vActivity;
	}
	
}
