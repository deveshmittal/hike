package com.bsb.hike;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.service.HikeService;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
	public static final String DEV_TYPE = "dev_type";
	public static final String DEV_TOKEN = "dev_token";

	private SharedPreferences prefs;

	public GCMIntentService() {
		super(HikeConstants.APP_PUSH_ID);
	}

	@Override
	protected void onError(Context context, String errorId) {
		Log.e(getClass().getSimpleName(), "ERROR OCCURRED " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(getClass().getSimpleName(), "Message received: " + intent);

		prefs = prefs == null ? context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0) : prefs;
		if (!TextUtils.isEmpty(prefs.getString(HikeMessengerApp.TOKEN_SETTING,
				null)) && prefs.getBoolean(HikeMessengerApp.NUX2_DONE, false)) {
			HikeMessengerApp app = (HikeMessengerApp) context
					.getApplicationContext();
			app.connectToService();
			context.sendBroadcast(new Intent(HikeService.MQTT_PING_ACTION));
		}
	}

	@Override
	protected void onRegistered(final Context context, String regId) {
		Log.d(getClass().getSimpleName(), "REGISTERED ID: " + regId);
		context.sendBroadcast(new Intent(HikeService.SEND_TO_SERVER_ACTION));
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.d(getClass().getSimpleName(), "UNREGISTERED ID: " + regId);
	}
}
