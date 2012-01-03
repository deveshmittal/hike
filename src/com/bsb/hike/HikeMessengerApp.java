package com.bsb.hike;

import android.app.Application;
import android.os.Handler;

import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.utils.DbConversationListener;

public class HikeMessengerApp extends Application {
	public static final String ACCOUNT_SETTINGS = "accountsettings";
	public static final String MSISDN_SETTING = "msisdn";
	public static final String CARRIER_SETTING = "carrier";
	public static final String NAME_SETTING = "name";
	public static final String TOKEN_SETTING = "token";
	public static final String MESSAGES_SETTING = "messageid";
    public static final String UID_SETTING = "uid";
	private static HikePubSub mPubSubInstance;
    private NetworkManager mNetworkManager;

	static {
		mPubSubInstance = new HikePubSub();
	}

	public void onCreate() {
		super.onCreate();

		/* add the db write listener */
		new DbConversationListener(getApplicationContext());

		/* add the generic websocket listener.  This will turn strings into objects and re-broadcast them */
		mNetworkManager = new NetworkManager(getApplicationContext());

		/* add a handler to handle toasts.  The object initializes itself it it's constructor */
		new ToastListener(getApplicationContext());
	}

	public static HikePubSub getPubSub() {
		return mPubSubInstance;
	}
}
