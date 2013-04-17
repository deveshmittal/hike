package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

public class LocaleChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		/*
		 * If the user has not signed up yet, don't do anything.
		 */
		if (TextUtils.isEmpty(prefs.getString(HikeMessengerApp.TOKEN_SETTING,
				null))) {
			return;
		}

		Log.d(getClass().getSimpleName(),
				"Locale: "
						+ context.getResources().getConfiguration().locale
								.getLanguage());
		JSONObject object = new JSONObject();
		JSONObject data = new JSONObject();

		try {
			data.put(HikeConstants.LOCALE, context.getResources()
					.getConfiguration().locale.getLanguage());

			object.put(HikeConstants.TYPE,
					HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
			object.put(HikeConstants.DATA, data);

			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
					object);
		} catch (JSONException e) {
			Log.w(getClass().getSimpleName(), "Invalid JSON", e);
		}

	}

}
