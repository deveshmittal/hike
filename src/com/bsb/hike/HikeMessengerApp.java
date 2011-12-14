package com.bsb.hike;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.DbConversationListener;
import com.bsb.hike.utils.HikeWebSocketClient;

public class HikeMessengerApp extends Application implements HikePubSub.Listener {
	public static final String ACCOUNT_SETTINGS = "accountsettings";
	public static final String MSISDN_SETTING = "msisdn";
	public static final String CARRIER_SETTING = "carrier";
	public static final String NAME_SETTING = "name";
	public static final String TOKEN_SETTING = "token";
	public static final String MESSAGES_SETTING = "messageid";
	private static HikePubSub mPubSubInstance;
	private HikeWebSocketClient mWebSocket;
	private Handler mHandler;
	private long mDelay;

	static {
		mPubSubInstance = new HikePubSub();
	}

	public void onCreate() {
		super.onCreate();
		HikePubSub pubSub = HikeMessengerApp.getPubSub();

		/* add the db write listener */
		Listener listener = new DbConversationListener(getApplicationContext());

		/* add the generic websocket listener.  This will turn strings into objects and re-broadcast them */
		listener = new WebSocketPublisher(getApplicationContext());

		/* add a handler to handle toasts.  The object initializes itself it it's constructor */
		ToastListener toastListener = new ToastListener(getApplicationContext());

		/* lastly add ourselves to restart the connection if it terminates
		 * TODO add exponential backoff
		 * TODO keep track of the connected state
		 * TODO this totally doesn't need to be here
		 */
		pubSub.addListener(HikePubSub.WS_CLOSE, this);
		pubSub.addListener(HikePubSub.WS_OPEN, this);
		mDelay = 1000;
		mHandler = new Handler();
	}

	public static HikePubSub getPubSub() {
		return mPubSubInstance;
	}

	public HikeWebSocketClient getWebSocket() {
		return mWebSocket;
	}

	public synchronized void startWebSocket() {
		if (mWebSocket == null) {
			Log.d("HikeMessengerApp", "starting websocket");
			mWebSocket = AccountUtils.startWebSocketConnection();
			mWebSocket.connect();
		}
	}

	public void restartWebSocket() {
		if (mDelay <= 1000*30) { /*30 seconds is our max delay*/
			mDelay = mDelay * 2;
			Log.d("HikeMessengerApp", "Increasing delay to " + mDelay);
		}

		Runnable restart = new Runnable() {
			@Override
			public void run() {
				mWebSocket.releaseAndInitialize();
				mWebSocket.connect();
			}
		};
		System.out.println("restarting...");
		mHandler.postDelayed(restart, mDelay);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		System.out.println("Type is " + type);
		if (HikePubSub.WS_CLOSE.equals(type)) {
			Log.i("HikeMessengerApp", "Websocket closed");
			restartWebSocket();
		} else if (HikePubSub.WS_OPEN.equals(type)) {
			mDelay = 500;
		}
	}
}
