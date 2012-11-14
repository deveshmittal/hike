package com.bsb.hike;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {
	private static final String DEV_TYPE = "dev_type";
	private static final String DEV_TOKEN = "dev_token";

	private SharedPreferences prefs;

	public GCMIntentService() {
		super(HikeConstants.APP_PUSH_ID);
	}

	@Override
	protected void onError(Context context, String errorId) {
		Log.e(getClass().getSimpleName(), "ERROR OCCURRED " + errorId);
		scheduleNextGCMRegistration();
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(getClass().getSimpleName(), "Message received: " + intent);

		prefs = prefs == null ? context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0) : prefs;
		if (!TextUtils.isEmpty(prefs.getString(HikeMessengerApp.TOKEN_SETTING,
				null))
				&& prefs.getBoolean(HikeMessengerApp.SHOWN_TUTORIAL, false)) {
			HikeMessengerApp app = (HikeMessengerApp) context
					.getApplicationContext();
			app.connectToService();
		}
	}

	@Override
	protected void onRegistered(final Context context, String regId) {
		Log.d(getClass().getSimpleName(), "REGISTERED ID: " + regId);
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
				"/account/device", new HikeHttpCallback() {
					public void onSuccess(JSONObject response) {
					}

					public void onFailure() {
						// Could not send our server the device token.
						// Unregister from GCM
						GCMRegistrar.unregister(context);
						scheduleNextGCMRegistration();
					}
				});
		JSONObject request = new JSONObject();
		try {
			request.put(DEV_TYPE, HikeConstants.ANDROID);
			request.put(DEV_TOKEN, regId);
		} catch (JSONException e) {
			Log.d(getClass().getSimpleName(), "Invalid JSON", e);
		}
		hikeHttpRequest.setJSONData(request);

		HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
		hikeHTTPTask.execute(hikeHttpRequest);
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.d(getClass().getSimpleName(), "UNREGISTERED ID: " + regId);
	}

	private void scheduleNextGCMRegistration() {
		Log.d(getClass().getSimpleName(), "Scheduling next GCM registration");

		SharedPreferences preferences = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		int lastBackOffTime = preferences.getInt(
				HikeMessengerApp.LAST_BACK_OFF_TIME, 0);

		lastBackOffTime = lastBackOffTime == 0 ? HikeConstants.RECONNECT_TIME
				: (lastBackOffTime * 2);
		lastBackOffTime = Math.min(HikeConstants.MAX_RECONNECT_TIME,
				lastBackOffTime);

		Log.d(getClass().getSimpleName(), "Scheduling the next disconnect");
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				new Intent(HikeService.REGISTER_TO_GCM_ACTION),
				PendingIntent.FLAG_UPDATE_CURRENT);

		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.SECOND, lastBackOffTime);

		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		// Cancel any pending alarms with this pending intent
		aMgr.cancel(pendingIntent);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
				pendingIntent);

		Editor editor = preferences.edit();
		editor.putInt(HikeMessengerApp.LAST_BACK_OFF_TIME, lastBackOffTime);
		editor.commit();
	}
}
