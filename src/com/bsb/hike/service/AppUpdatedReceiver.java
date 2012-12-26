package com.bsb.hike.service;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.utils.Utils;

/**
 * @author Rishabh This receiver is used to notify that the app has been
 *         updated.
 */
public class AppUpdatedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) {
		if (context.getPackageName().equals(
				intent.getData().getSchemeSpecificPart())) {
			Log.d(getClass().getSimpleName(), "App has been updated");
			/*
			 * Have to add the delay since the service is null initially.
			 */
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					// Send the device details again which includes the new app
					// version
					JSONObject obj = Utils.getDeviceDetails(context);
					if (obj != null) {
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.MQTT_PUBLISH, obj);
					}
					Utils.requestAccountInfo();
				}
			}, 5 * 1000);

			/*
			 * Checking if the current version is the latest version. If it is
			 * we reset the preference which prompts the user to update the app.
			 */
			SharedPreferences prefs = context.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			if (!Utils.isUpdateRequired(
					prefs.getString(HikeConstants.Extras.LATEST_VERSION, ""),
					context)) {
				Editor editor = prefs.edit();
				editor.remove(HikeConstants.Extras.UPDATE_AVAILABLE);
				editor.remove(HikeConstants.Extras.SHOW_UPDATE_OVERLAY);
				editor.remove(HikeConstants.Extras.SHOW_UPDATE_TOOL_TIP);
				editor.remove(HikeConstants.Extras.UPDATE_TO_IGNORE);
				editor.remove(HikeConstants.Extras.LATEST_VERSION);
				editor.remove(HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
				editor.commit();
			}
			/*
			 * Adding auto recommended favorites based on the recency of the
			 * contact. One time thing only
			 */
			if (!prefs.getBoolean(
					HikeMessengerApp.AUTO_RECOMMENDED_FAVORITES_ADDED, false)) {
				HikeUserDatabase.getInstance().addAutoRecommendedFavorites();
				Editor editor = prefs.edit();
				editor.putBoolean(
						HikeMessengerApp.AUTO_RECOMMENDED_FAVORITES_ADDED, true);
				editor.commit();
			}
			/*
			 * This will happen for builds older than 1.1.15
			 */
			if (!prefs.contains(HikeMessengerApp.COUNTRY_CODE)
					&& prefs.getString(HikeMessengerApp.MSISDN_SETTING, "")
							.startsWith(HikeConstants.INDIA_COUNTRY_CODE)) {
				Editor editor = prefs.edit();
				editor.putString(HikeMessengerApp.COUNTRY_CODE,
						HikeConstants.INDIA_COUNTRY_CODE);
				editor.commit();
				HikeMessengerApp.setIndianUser(true);
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.REFRESH_RECENTS, null);
			}

			SharedPreferences appPrefs = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (!appPrefs.contains(HikeConstants.FREE_SMS_PREF)) {
				Editor editor = appPrefs.edit();
				boolean freeSMSOn = prefs.getString(
						HikeMessengerApp.COUNTRY_CODE, "").equals(
						HikeConstants.INDIA_COUNTRY_CODE);
				editor.putBoolean(HikeConstants.FREE_SMS_PREF, freeSMSOn);
				editor.commit();
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.FREE_SMS_TOGGLED, freeSMSOn);
			}
		}
	}
}