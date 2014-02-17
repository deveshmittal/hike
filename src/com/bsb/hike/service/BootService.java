package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;

public class BootService extends BroadcastReceiver
{

	@Override
	public void onReceive(Context ctx, Intent intent)
	{
		Log.i("HikeBootService", "Received onBoot intent");
		SharedPreferences prefs = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		/*
		 * If the user has not signed up yet, don't do anything.
		 */
		if (TextUtils.isEmpty(prefs.getString(HikeMessengerApp.TOKEN_SETTING, null)))
		{
			return;
		}
		Intent startServiceIntent = new Intent(ctx, HikeService.class);
		ctx.startService(startServiceIntent);
	}

}
