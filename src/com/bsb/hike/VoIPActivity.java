package com.bsb.hike;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.bsb.hike.voip.VoIPCaller;
import com.bsb.hike.voip.VoIPClient;
import com.bsb.hike.voip.VoIPService;
import com.bsb.hike.voip.VoIPService.LocalBinder;

public class VoIPActivity extends Activity {

	public static final String logTag = "VoIPCaller";
	
	private VoIPService voipService;
	private VoIPClient clientSelf = new VoIPClient(), clientPartner = new VoIPClient();
	private boolean isBound = false;
	private boolean mute = false, speaker = false;
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	private int initialAudioMode, initialRingerMode;
	private boolean initialSpeakerMode;
	private WakeLock wakeLock = null;
	
	public static final int MSG_SHUTDOWN_CALL = 1;
	public static final int MSG_CONNECTION_ESTABLISHED = 2;
	public static final int MSG_AUDIO_START = 3;
	public static final int MSG_ENCRYPTION_INITIALIZED = 4;
	public static final int MSG_CALL_DECLINED = 5;
	public static final int MSG_CONNECTION_FAILURE = 6;
	public static final int MSG_CURRENT_BITRATE = 7;
	
	@SuppressLint("HandlerLeak") class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SHUTDOWN_CALL:
				Log.d(VoIPCaller.logTag, "Received shutdown message.");
				showMessage("Shutting down.");
				shutdown();
				break;
			case MSG_CONNECTION_ESTABLISHED:
				showMessage("Connection established.");
				break;
			case MSG_AUDIO_START:
				showMessage("Starting audio.");
				break;
			case MSG_ENCRYPTION_INITIALIZED:
				showMessage("Encryption initialized.");
				break;
			case MSG_CALL_DECLINED:
				showMessage("Call was declined.");
				break;
			case MSG_CONNECTION_FAILURE:
				showMessage("Error: Unable to establish connection.");
				shutdown();
				break;
			case MSG_CURRENT_BITRATE:
				int bitrate = voipService.getBitrate();
				showMessage("Bitrate: " + bitrate);
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
			Log.d(VoIPCaller.logTag, "VoIPService disconnected.");
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(VoIPCaller.logTag, "VoIPService connected.");
			LocalBinder binder = (LocalBinder) service;
			voipService = binder.getService();
			isBound = true;
			startService();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_voip);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		Log.d(VoIPCaller.logTag, "Binding to service..");
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
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isBound) {
			voipService.stop();
			unbindService(myConnection);
		}
		restoreAudioSettings();
		releaseWakeLock();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(logTag, "VoIPActivity onNewIntent().");
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		String action = intent.getStringExtra("action");

		if (action != null && action.equals("outgoingcall")) {
			// we are making an outgoing call
			clientPartner.setPhoneNumber(intent.getStringExtra("msisdn"));
			clientSelf.setInitiator(true);
			clientPartner.setInitiator(false);
			Log.d(logTag, "Making outgoing call to: " + clientPartner.getPhoneNumber());
		}

		if (action.equals("setpartnerinfo")) {
			clientPartner.setInternalIPAddress(intent.getStringExtra("internalIP"));
			clientPartner.setInternalPort(intent.getIntExtra("internalPort", 0));
			clientPartner.setExternalIPAddress(intent.getStringExtra("externalIP"));
			clientPartner.setExternalPort(intent.getIntExtra("externalPort", 0));
			clientPartner.setPhoneNumber(intent.getStringExtra("msisdn"));
			clientPartner.setInitiator(intent.getBooleanExtra("initiator", true));
			clientSelf.setInitiator(!clientPartner.isInitiator());
			
			if (clientPartner.isInitiator()) {
				Log.d(logTag, "Detected incoming VoIP call.");
				// voipService.retrieveExternalSocket();
			} else {
				// We have already sent our socket info to partner
				// And now they have sent us their's, so let's establish connection
				voipService.establishConnection();
			}
		}
	}

	private void startService() {
		try {
			Log.d(VoIPCaller.logTag, "Retrieving socket through service..");
			voipService.setClientSelf(clientSelf);
			voipService.setClientPartner(clientPartner);
			voipService.setMessenger(mMessenger);
			voipService.retrieveExternalSocket();
		} catch (Exception e) {
			Log.d(VoIPCaller.logTag, "Exception: " + e.toString());
		}
	}
	
	private void setButtonHandlers() {
		Button closeButton = (Button) findViewById(R.id.btn_close);
		closeButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				voipService.hangUp();
			}
		});
		
		Button answerButton = (Button) findViewById(R.id.btn_answer);
		answerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (clientSelf.isInitiator() == false)
					voipService.startAudio();
			}
		});
		
		Button declineButton = (Button) findViewById(R.id.btn_decline);
		declineButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (clientSelf.isInitiator() == false)
					voipService.rejectIncomingCall();
			}
		});
		
		final Button muteButton = (Button) findViewById(R.id.btn_mute);
		muteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mute == false) {
					mute = true;
					muteButton.setText("Mute On");
					voipService.setMute(mute);
				} else {
					mute = false;
					muteButton.setText("Mute Off");
					voipService.setMute(mute);
				}
			}
		});
		
		final Button speakerButton = (Button) findViewById(R.id.btn_speaker);
		speakerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (speaker == false) {
					speaker = true;
					speakerButton.setText("Speaker On");
				} else {
					speaker = false;
					speakerButton.setText("Speaker Off");
				}

				AudioManager audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				audiomanager.setSpeakerphoneOn(speaker);
			}
		});
		
		final Button increaseBRButton = (Button) findViewById(R.id.btn_increase_bitrate);
		increaseBRButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				voipService.adjustBitrate(1000);
			}
		});
		
		final Button decreaseBRButton = (Button) findViewById(R.id.btn_decrease_bitrate);
		decreaseBRButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				voipService.adjustBitrate(-1000);
			}
		});
		
	}
	
	private void shutdown() {
		voipService.stop();
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
		if (wakeLock == null)
			wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HikeWL");
		if (!wakeLock.isHeld()) {
			wakeLock.acquire();
			Log.d(VoIPCaller.logTag, "Wakelock acquired.");
		}
	}
	
	private void releaseWakeLock() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			Log.d(VoIPCaller.logTag, "Wakelock released.");
		}
	}
	
	private void showMessage(final String message) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(VoIPActivity.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}
}
