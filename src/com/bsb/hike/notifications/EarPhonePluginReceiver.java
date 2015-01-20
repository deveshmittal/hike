package com.bsb.hike.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.utils.Logger;

public class EarPhonePluginReceiver extends BroadcastReceiver
{
	public static final int PLUGGED = 1;
	
	public static final int UNPLUGGED = 0;

	public static int EAR_PHONE_STATE = 0;
	
	private static final String TAG = "EarPhonePluginReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
            case UNPLUGGED:
                Logger.d(TAG, "Headset is unplugged");
                EAR_PHONE_STATE = UNPLUGGED;
                break;
            case PLUGGED:
                Logger.d(TAG, "Headset is plugged");
                EAR_PHONE_STATE = PLUGGED;
                break;
            default:
                Logger.d(TAG, "I have no idea what the headset state is");
            }
        }
		
	}
}
