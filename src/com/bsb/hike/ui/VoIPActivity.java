package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.content.ServiceConnection;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.service.VOIPService.VoIPBinder;
import com.bsb.hike.service.VOIPService;

public class VoIPActivity extends Activity implements HikePubSub.Listener {
	
	private String callerId;
	private String dialedId;
	private Button endCall;
	private Button acceptCall;
	private Button declineCall;
	private TextView callNo;
	private int serviceId = BIND_AUTO_CREATE;
//	private static VoIPBinder serviceBinder= new VoIPBinder();
	private HikePubSub mPubSub = HikeMessengerApp.getPubSub();
	public static Handler messageHandler = new MessageHandler();
	public static MessageHandler serviceHandler = new MessageHandler();

	private String resumeId = null;
	private static VOIPService vService = new VOIPService();
	private static VoIPActivity vActivity;
	public static boolean mBound = false;
	public boolean callConnected = false;
	
	public static class MessageHandler extends Handler {
	    @Override
	    public void handleMessage(Message message) {
	    	switch(message.what){
	    	
	    	case 0:
		    	if(mBound ){
		    		vActivity.unbindService(vsc);
			    	mBound=false;
		    	}
//	    	case 1:
//	    		callConnected = true;
	    	}
	    }
	}
	
	
	private static ServiceConnection vsc = new ServiceConnection(){
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	        vService = ((VOIPService.VoIPBinder)service).getService();

	        // Tell the user about this for our demo.
//	        Toast.makeText(Binding.this, R.string.local_service_connected,
//	                Toast.LENGTH_SHORT).show();
	    }

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		};
	};

	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		vActivity = this;
		Log.d("VOIP ACTIVITY STARTED", "duh");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mPubSub.addListener(HikePubSub.VOIP_HANDSHAKE, this);
		Bundle extras = getIntent().getExtras();
		if (getIntent().hasExtra("callerID")) {
	        callerId = extras.getString("callerID");
	        setAnswerLayout();
		}
		else if(getIntent().hasExtra("dialedID")) {
			dialedId = extras.getString("dialedID");
			setInCallLayout();			
		}
		else {
			resumeId = extras.getString("resumeID");
			setInCallLayout();
		}
	}
	
	public void setInCallLayout()
	  {
		  setContentView(R.layout.incall_layout);
		  final Intent i = new Intent(this,com.bsb.hike.service.VOIPService.class);
		  i.putExtras(getIntent().getExtras());
//		  i.putExtra("handler", messageHandler);
//		  bindService(i, vsc, serviceId);
//		  Log.d("WTF?", resumeId);
		  if (resumeId == null && !mBound){
			  startService(i);
			  if(!mBound){
				  bindService(i, vsc, serviceId);
				  mBound = true;
			  }
			  /*if (vsc == null)
				  Log.d("VSC", "ggwp");
			  vService = ((vsc.voipService));
			  if (vsc.voipService == null)
				  Log.d("voipserv","GGWP");
			  if(vService == null)
				  Log.d("vService", "vService GGWP");*/
//			  unbindService(vsc);
		  }
		  Log.d("VoIPActivity", "startService Called!");
		  endCall = (Button)this.findViewById(R.id.endCallButton);
		  endCall.setBackgroundColor(Color.RED);
		  endCall.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    //TODO: Replace with a method to stop activity
//		    	endCall();
		    	Intent i = new Intent(getApplicationContext(),com.bsb.hike.service.VOIPService.class);
//		    	unbindService(vsc);
//		    	if(vService!=null)
		    	vService.endCall();
//		    	else
//		    		Log.d("vService", "GGWP");
		    	finish();
		    }

		  });
	  }
	
	public void drawInCallLayout(){
		setContentView(R.layout.incall_layout);
		endCall = (Button)this.findViewById(R.id.endCallButton);
		  endCall.setBackgroundColor(Color.RED);
		  endCall.setOnClickListener(new OnClickListener() {
		    @Override
		    public void onClick(View v) {
		    //TODO: Replace with a method to stop activity
//		    	endCall();
		    	Intent i = new Intent(getApplicationContext(),com.bsb.hike.service.VOIPService.class);
//		    	unbindService(vsc);
//		    	if(vService!=null)
		    	vService.endCall();
//		    	else
//		    		Log.d("vService", "GGWP");
		    	finish();
		    }

		  });
	}
	
	public void setAnswerLayout()
	  {
		  final Intent i = new Intent(this,com.bsb.hike.service.VOIPService.class);
		  i.putExtras(getIntent().getExtras());
		  i.putExtra("decline", false);
//		  i.putExtra("Activity", this);
		  setContentView(R.layout.call_accept_decline);
	      callNo = (TextView)this.findViewById(R.id.CallerId);
	      callNo.setText(callerId);
	      acceptCall = (Button)this.findViewById(R.id.acceptButton);
	      acceptCall.setBackgroundColor(Color.GREEN);
	      acceptCall.setTextColor(Color.WHITE);
	      acceptCall.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = i;
				Log.d("Anser Clicked", "!!!");
				callConnected = true;
//				getApplicationContext().startService(intent);
				startService(i);
				Log.d("mBound", String.valueOf(mBound));
				 if(!mBound){
					  bindService(i, vsc, serviceId);
					  mBound = true;
				  }
				 drawInCallLayout();
//				vService = ((VOIPService)(vsc.voipService));
			}
	      });
	      
	      declineCall = (Button)this.findViewById(R.id.declineButton);
	      declineCall.setBackgroundColor(Color.RED);
	      declineCall.setTextColor(Color.WHITE);
	      
	      declineCall.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
			//TODO: replace with method to end call by starting activity with decline intent
				Intent intent = i;
				intent.putExtra("decline", true);
//				declineCall();
//				getApplicationContext().startService(intent);
				startService(i);
				 if(!mBound){
					  bindService(i, vsc, serviceId);
					  mBound = true;
				  }
//				vService = ((VOIPService)(vsc.voipService));
				if(vService == null)
					  Log.d("vService", "GGWP");
//				unbindService(vsc);
				finish();
			}
	    	  
	      });
	  }
	
	/*private class VoIPServiceConnection implements ServiceConnection{
		
		public VOIPService voipService; 

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			voipService =((VoIPBinder)service).voipService;
			
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
		
	}*/


	@Override
	public void onEventReceived(String type, Object object) {
		try{
		JSONObject json = (JSONObject) object;
		JSONObject data = (JSONObject) json.get(HikeConstants.DATA);
		JSONObject metadata = (JSONObject) data.get(HikeConstants.METADATA);
		String mdType = metadata.getString("type");
		if (mdType.equals(HikeConstants.MqttMessageTypes.VOIP_CALL_DECLINE) && !callConnected){
//			if(vsc.voipService != null){
				finish();
//				unbindService(vsc);
//			}
		}
		}
		catch(JSONException e){
			e.printStackTrace();
		}
		
	}

}

