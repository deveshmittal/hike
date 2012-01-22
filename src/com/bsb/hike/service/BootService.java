package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootService extends BroadcastReceiver
{

	@Override
	public void onReceive(Context ctx, Intent intent)
	{
		Log.i("BootService", "Received onBoot intent");
		Intent startServiceIntent = new Intent(ctx, HikeService.class);
        ctx.startService(startServiceIntent);
	}

}
