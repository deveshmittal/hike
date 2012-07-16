package com.bsb.hike.service;

import com.bsb.hike.HikeMessengerApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 
 * @author Rishabh
 *	Triggered whenever the user turns on his/her screen. Had to be added to fix bug in v2.3 and below
 * 	where the service would randomly die out.
 */
public class KeepAliveService extends BroadcastReceiver 
{
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		Log.d(getClass().getSimpleName(), "KeepAliveService Triggered");
		HikeMessengerApp app = (HikeMessengerApp) context.getApplicationContext();
		app.connectToService();
	}

}
