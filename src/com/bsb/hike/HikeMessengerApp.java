package com.bsb.hike;

import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.utils.DbConversationListener;

import android.app.Application;

public class HikeMessengerApp extends Application {
	public static final String ACCOUNT_SETTINGS = "accountsettings";
	public static final String MSISDN_SETTING = "msisdn";
	public static final String CARRIER_SETTING = "carrier";
	public static final String NAME_SETTING = "name";
	public static final String TOKEN_SETTING = "token";
	private static HikePubSub mPubSubInstance;
	static {
		mPubSubInstance = new HikePubSub();
	}

	public void onCreate() {
		super.onCreate();
		HikePubSub pubSub = HikeMessengerApp.getPubSub();
		Listener listener = new DbConversationListener(getApplicationContext());
		pubSub.addListener(HikePubSub.MESSAGE, listener);
	}

	public static HikePubSub getPubSub() {
		return mPubSubInstance;
	}

}
