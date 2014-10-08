package com.bsb.hike.ui;

import org.json.JSONObject;
import org.json.JSONException;
import org.webrtc.PeerConnection;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
	private ImageButton speakerButton;
	private ImageButton muteButton;
	private TextView callNo;
	private TextView inCallCallNo;
	private TextView inCallTimer;	
	private HikePubSub mPubSub = HikeMessengerApp.getPubSub();
	public static Handler messageHandler = new MessageHandler();
	public static MessageHandler serviceHandler = new MessageHandler();
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
	
	class CallLengthManager implements Runnable{

		@Override
		public void run() {
			callLength = System.currentTimeMillis() - startTime;
			int seconds = (int) (callLength / 1000);
		    int minutes = seconds / 60;
		    int hours = minutes/60;
		    seconds = seconds % 60;
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
		if(getIntent().hasExtra("callerID")){
			callerId = getIntent().getStringExtra("callerID");
			storedId = callerId;
			prepareAnswer();
		} else if (getIntent().hasExtra("dialedID")){
			dialedId = getIntent().getStringExtra("dialedID");
			storedId = dialedId;
			prepareInCall();
		} else {
			resumeId = getIntent().getStringExtra("resumeId");
			storedId = resumeId;
			prepareResume();			
		}
	}
	
	public void prepareAnswer(){
		final Intent i = new Intent(this,com.bsb.hike.service.VoIPServiceNew.class);
		i.putExtras(getIntent().getExtras());
//		mMediaPlayer = new MediaPlayer();
		mMediaPlayer = MediaPlayer.create(this, R.raw.hike_jingle_15);
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mMediaPlayer.setLooping(true);
		final MediaPlayer player = mMediaPlayer; 
		player.start();
		mMediaPlayer = player;
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
				player.stop();
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
				player.stop();
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
		speakerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				VoIPServiceNew.getVoIPSerivceInstance().speakerClicked();	
				changeSpeakerButton();
			}
		});
		endCall = (Button)this.findViewById(R.id.endCallButton);
		endCall.setBackgroundColor(Color.RED);
		endCall.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				raiseEndCallToast();
				VoIPServiceNew.getVoIPSerivceInstance().endCall();
				finish();
			}
		});
		
		inCallCallNo = (TextView)this.findViewById(R.id.PhoneNumberView1);
		inCallCallNo.setText(storedId);
		
		inCallTimer = (TextView)this.findViewById(R.id.timerView1);
		if (VoIPServiceNew.getVoIPSerivceInstance().client.connectionState != "CONNECTED")
			inCallTimer.setText(VoIPServiceNew.getVoIPSerivceInstance().client.connectionState);
		else
			inCallTimer.setText(callerId);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if(type == HikePubSub.VOIP_HANDSHAKE){
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
			if(state == PeerConnection.IceConnectionState.CONNECTED.toString()){
				startTime = System.currentTimeMillis();
				displayHandler.post(new CallLengthManager() );
			}
		}
	}
	
	public void onDestroy(){
		if(mMediaPlayer.isPlaying())
			mMediaPlayer.stop();
		mPubSub.removeListener(HikePubSub.VOIP_HANDSHAKE, this);
		mPubSub.removeListener(HikePubSub.VOIP_CALL_STATUS_CHANGED, this);
		super.onDestroy();
	}
	
	public static VoIPActivityNew getVoIPActivityInstance(){
		return vActivity;
	}
	
	private void changeMuteButton() {
		if(isMute){
			muteButton.setBackgroundColor(Color.RED);
			isMute = false;
		} else {
			muteButton.setBackgroundColor(Color.GREEN);
			isMute = true;
		}
		
	}
	
	private void changeSpeakerButton(){
		if(!isSpeakerOn){
			speakerButton.setImageResource(R.drawable.ic_sound_unchecked);
			isSpeakerOn = true;
		} else {
			speakerButton.setImageResource(R.drawable.ic_sound_checked);
			isSpeakerOn = false;
		}
		
	}
	
	public void raiseEndCallToast(){
		Toast.makeText(getApplicationContext(), "CALL ENDED", Toast.LENGTH_LONG).show();
	}
	
}
