package com.bsb.im.ui;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;

import com.bsb.im.BeemService;
import com.bsb.im.service.Contact;
import com.bsb.im.service.PresenceAdapter;
import com.bsb.im.ui.wizard.AccountConfigure;
import com.bsb.im.utils.BeemBroadcastReceiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.im.R;
import com.bsb.im.service.aidl.IXmppFacade;

/**
 * This activity is used to accept a subscription request.
 * 
 */
public class Subscription extends Activity {

	private static final Intent SERVICE_INTENT = new Intent();
	private static final String TAG = Subscription.class.getSimpleName();
	private IXmppFacade mService;
	private String mContact;
	private ServiceConnection mServConn = new BeemServiceConnection();
	private final BeemBroadcastReceiver mReceiver = new BeemBroadcastReceiver();
	private MyOnClickListener mClickListener = new MyOnClickListener();

	static {
		SERVICE_INTENT.setComponent(new ComponentName("com.bsb.im",
				"com.bsb.im.BeemService"));
	}

	/**
	 * Constructor.
	 */
	public Subscription() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subscription);
		findViewById(R.id.SubscriptionAccept)
				.setOnTouchListener(mClickListener);
		findViewById(R.id.SubscriptionRefuse)
				.setOnTouchListener(mClickListener);
		Contact c = new Contact(getIntent().getData());
		mContact = c.getJID();
		TextView tv = (TextView) findViewById(R.id.SubscriptionText);
		String str = String.format(getString(R.string.SubscriptText), mContact);
		tv.setText(str);
		this.registerReceiver(mReceiver, new IntentFilter(
				BeemBroadcastReceiver.BEEM_CONNECTION_CLOSED));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(this, BeemService.class), mServConn,
				BIND_AUTO_CREATE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(mServConn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mReceiver);
	}

	/**
	 * Send the presence stanza.
	 * 
	 * @param p
	 *            presence stanza
	 */
	private void sendPresence(Presence p) {
		PresenceAdapter preAdapt = new PresenceAdapter(p);
		try {
			mService.sendPresencePacket(preAdapt);
		} catch (RemoteException e) {
			Log.e(TAG, "Error while sending subscription response", e);
		}
	}

	/**
	 * Event simple click on buttons.
	 */
	private class MyOnClickListener implements OnTouchListener {

		/**
		 * Constructor.
		 */
		public MyOnClickListener() {
		}

		@Override
		public boolean onTouch(View v, MotionEvent arg1) {
			// TODO Auto-generated method stub
			if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
				//v.setBackgroundResource(R.drawable.button1_pressed);
			} else if (arg1.getAction() == MotionEvent.ACTION_UP) {
				//v.setBackgroundResource(R.drawable.button1);
				Presence presence = null;
				switch (v.getId()) {
				case R.id.SubscriptionAccept:
					presence = new Presence(Type.subscribed);
					Toast.makeText(Subscription.this,
							getString(R.string.SubscriptAccept), Toast.LENGTH_SHORT)
							.show();
					break;
				case R.id.SubscriptionRefuse:
					presence = new Presence(Type.unsubscribed);
					Toast.makeText(Subscription.this,
							getString(R.string.SubscriptRefused),
							Toast.LENGTH_SHORT).show();
					break;
				default:
					Toast.makeText(Subscription.this,
							getString(R.string.SubscriptError), Toast.LENGTH_SHORT)
							.show();
				}
				if (presence != null) {
					presence.setTo(mContact);
					sendPresence(presence);
				}
				finish();
			}
			return false;
		}
	};

	/**
	 * The ServiceConnection used to connect to the Beem service.
	 */
	private class BeemServiceConnection implements ServiceConnection {

		/**
		 * Constructor.
		 */
		public BeemServiceConnection() {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IXmppFacade.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	}
}
