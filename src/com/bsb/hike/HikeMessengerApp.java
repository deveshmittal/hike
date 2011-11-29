package com.bsb.hike;

import android.app.Application;

public class HikeMessengerApp extends Application {
	public static final String ACCOUNT_SETTINGS = "accountsettings";
	public static final String MSISDN_SETTING = "msisdn";
	public static final String CARRIER_SETTING = "carrier";
	public static final String NAME_SETTING = "name";
	public static final String TOKEN_SETTING = "token";
	public void onCreate() {
		super.onCreate();
	}

}
