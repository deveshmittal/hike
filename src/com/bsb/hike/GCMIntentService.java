package com.bsb.hike;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gcm.GCMBaseIntentService;
//import com.bsb.hike.service.HikeMqttManager;

public class GCMIntentService extends GCMBaseIntentService
{
	public static final String DEV_TYPE = "dev_type";

	public static final String DEV_TOKEN = "dev_token";

	private SharedPreferences prefs;

	public GCMIntentService()
	{
		super(HikeConstants.APP_PUSH_ID);
	}

	@Override
	protected void onError(Context context, String errorId)
	{
		Logger.e(getClass().getSimpleName(), "ERROR OCCURRED " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent)
	{
		Logger.d(getClass().getSimpleName(), "Message received: " + intent);

		prefs = prefs == null ? context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0) : prefs;
		if (!Utils.isUserAuthenticated(context))
		{
			return;
		}

		HikeMessengerApp app = (HikeMessengerApp) context.getApplicationContext();
		app.connectToService();
		String reconnectVal = intent.getStringExtra("pushReconnect");
		boolean reconnect = false;
		Logger.d(getClass().getSimpleName(), "Server sent packet pushReconnect : " + reconnectVal);
		if("1".equals(reconnectVal))
			reconnect = true;
		String jsonString = intent.getStringExtra(HikeConstants.Extras.OFFLINE_PUSH_KEY);
		if(null != jsonString && jsonString.length() > 0)
		{
			Logger.d("HikeToOffline", "Gcm push received : json :" + jsonString);
			/*
			 * if user has turned off hike offline notification setting then dont
			 * show hike offline push
			 */
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HikeConstants.HIKE_OFFLINE_NOTIFICATION_PREF, true))
			{
				Bundle bundle =  new Bundle();
				bundle.putString(HikeConstants.Extras.OFFLINE_PUSH_KEY, jsonString);
				HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_TO_OFFLINE_PUSH, bundle);
			}
		}
		context.sendBroadcast(new Intent(HikeMqttManagerNew.MQTT_CONNECTION_CHECK_ACTION).putExtra("reconnect", reconnect));
	}

	@Override
	protected void onRegistered(final Context context, String regId)
	{
		Logger.d(getClass().getSimpleName(), "REGISTERED ID: " + regId);
		context.sendBroadcast(new Intent(HikeService.SEND_TO_SERVER_ACTION));
	}

	@Override
	protected void onUnregistered(Context context, String regId)
	{
		Logger.d(getClass().getSimpleName(), "UNREGISTERED ID: " + regId);
	}
}
