package com.bsb.hike.service;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
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
	public void onReceive(final Context context, Intent intent) 
	{
		if(context.getPackageName().equals(intent.getData().getSchemeSpecificPart()))
		{
			Log.d(getClass().getSimpleName(), "App has been updated");
			/*
			 * Have to add the delay since the service is null initially.
			 */
			new Handler().postDelayed(new Runnable() 
			{
				@Override
				public void run() 
				{
					// Send the device details again which includes the new app version
					JSONObject obj = Utils.getDeviceDetails(context);
					if (obj != null) 
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, obj);
					}
					Utils.requestAccountInfo();
				}
			}, 5*1000);

			/*
			 *  Checking if the current version is the latest version. If it is we reset the preference which
			 *  prompts the user to update the app.
			 */
			SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			if(!Utils.isUpdateRequired(prefs.getString(HikeConstants.Extras.LATEST_VERSION, ""), context))
			{
				Editor editor = prefs.edit();
				editor.remove(HikeConstants.Extras.UPDATE_AVAILABLE);
				editor.remove(HikeConstants.Extras.SHOW_UPDATE_OVERLAY);
				editor.remove(HikeConstants.Extras.SHOW_UPDATE_TOOL_TIP);
				editor.remove(HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
				editor.commit();
			}
		}
	}
}