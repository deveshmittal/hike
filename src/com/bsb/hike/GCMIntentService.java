package com.bsb.hike;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService 
{
	private static final String DEV_TYPE = "dev_type";
	private static final String DEV_TOKEN = "dev_token";

	private SharedPreferences prefs;

	public GCMIntentService() 
	{
		super(HikeConstants.APP_PUSH_ID);
	}

	@Override
	protected void onError(Context context, String errorId)
	{
		Log.d(getClass().getSimpleName(), "ERROR OCCURRED " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent)
	{
		Log.d(getClass().getSimpleName(), "Message received: " + intent);

		prefs = prefs == null ? context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0) : prefs;
		if (!TextUtils.isEmpty(prefs.getString(HikeMessengerApp.TOKEN_SETTING, null)) && prefs.getBoolean(HikeMessengerApp.SHOWN_TUTORIAL, false)) 
		{
			HikeMessengerApp app = (HikeMessengerApp) context
					.getApplicationContext();
			app.connectToService();
		}
	}

	@Override
	protected void onRegistered(final Context context, String regId)
	{
		Log.d(getClass().getSimpleName(), "REGISTERED ID: " + regId);
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/device", new HikeHttpCallback() 
		{
			public void onSuccess(JSONObject response) 
			{}
			public void onFailure() 
			{
				// Could not send our server the device token. Unregister from GCM
				GCMRegistrar.unregister(context);
			}
		});
		JSONObject request = new JSONObject();
		try 
		{
			request.put(DEV_TYPE, HikeConstants.ANDROID);
			request.put(DEV_TOKEN, regId);
		}
		catch (JSONException e) 
		{
			Log.d(getClass().getSimpleName(), "Invalid JSON", e);
		}
		hikeHttpRequest.setJSONData(request);

		HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
		hikeHTTPTask.execute(hikeHttpRequest);
	}

	@Override
	protected void onUnregistered(Context context, String regId)
	{
		Log.d(getClass().getSimpleName(), "UNREGISTERED ID: " + regId);
	}
}
