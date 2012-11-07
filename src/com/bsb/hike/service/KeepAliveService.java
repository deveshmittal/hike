package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;

/**
 * 
 * @author Rishabh Triggered whenever the user turns on his/her screen. Had to
 *         be added to fix bug in v2.3 and below where the service would
 *         randomly die out.
 */
public class KeepAliveService extends BroadcastReceiver {
	private SharedPreferences prefs;

	@Override
	public void onReceive(Context context, Intent intent) {
		prefs = prefs == null ? context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0) : prefs;
		Log.d(getClass().getSimpleName(), "KeepAliveService Triggered");
		if (!TextUtils.isEmpty(prefs.getString(HikeMessengerApp.TOKEN_SETTING,
				null))
				&& prefs.getBoolean(HikeMessengerApp.SHOWN_TUTORIAL, false)) {
			HikeMessengerApp app = (HikeMessengerApp) context
					.getApplicationContext();
			app.connectToService();
		}
	}

}
