package com.bsb.hike;

import net.tootallnate.websocket.WebSocketClient;
import android.app.Application;
import android.os.Handler;
import android.util.Log;

import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.DbConversationListener;

public class HikeMessengerApp extends Application implements HikePubSub.Listener {
	public static final String ACCOUNT_SETTINGS = "accountsettings";
	public static final String MSISDN_SETTING = "msisdn";
	public static final String CARRIER_SETTING = "carrier";
	public static final String NAME_SETTING = "name";
	public static final String TOKEN_SETTING = "token";
	public static final String MESSAGES_SETTING = "messageid";
	private static HikePubSub mPubSubInstance;
	private WebSocketPublisher mPublisher;
	private WebSocketClient mWebSocket;
	private Handler mHandler;

	static {
		mPubSubInstance = new HikePubSub();
	}

	public void onCreate() {
		super.onCreate();
		HikePubSub pubSub = HikeMessengerApp.getPubSub();
		Listener listener = new DbConversationListener(getApplicationContext());
		pubSub.addListener(HikePubSub.MESSAGE_SENT, listener);
		mPublisher = new WebSocketPublisher(getApplicationContext());
		pubSub.addListener(HikePubSub.WS_MESSAGE, mPublisher);
		pubSub.addListener(HikePubSub.WS_CLOSE, this);
		mHandler = new Handler();
	}

	public static HikePubSub getPubSub() {
		return mPubSubInstance;
	}

	public WebSocketClient getWebSocket() {
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
		Runnable restart = new Runnable() {
			@Override
			public void run() {
				System.out
						.println("HikeMessengerApp.restartWebSocket().new Runnable() {...}.run()");
				mWebSocket = null;
				startWebSocket();
			}
		};
		System.out.println("restarting...");
		mHandler.postDelayed(restart, 1000);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		System.out.println("Type is " + type);
		if (HikePubSub.WS_CLOSE.equals(type)) {
			Log.i("HikeMessengerApp", "Websocket closed");
			restartWebSocket();
		}
	}
}
