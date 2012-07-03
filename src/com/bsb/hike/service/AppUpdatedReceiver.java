package com.bsb.hike.service;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Utils;

/**
 * @author Rishabh
 *	This receiver is used to notify that the app has been updated.
 */
public class AppUpdatedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if(context.getPackageName().equals(intent.getData().getSchemeSpecificPart()))
		{
			Log.d(getClass().getSimpleName(), "App has been updated");
			// Send the device details again which includes the new app version
			JSONObject obj = Utils.getDeviceDetails(context);
			if (obj != null) 
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, Utils.getDeviceDetails(context));
			}

		}
	}
}