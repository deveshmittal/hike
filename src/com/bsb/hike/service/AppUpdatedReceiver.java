package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
		}
	}
}