package com.bsb.hike.notifications;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EarPhonePluginReceiver extends BroadcastReceiver
{
	public static final int PLUGGED = 1;
	
	public static final int UNPLUGGED = 0;
	
	private static final String TAG = "EarPhonePluginReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		// TODO Auto-generated method stub
		if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
            case 0:
                Log.d(TAG, "Headset is unplugged");
                HikeSharedPreferenceUtil.getInstance(context).saveData(HikeConstants.EARPHONE_STATE, UNPLUGGED);
                break;
            case 1:
                Log.d(TAG, "Headset is plugged");
                HikeSharedPreferenceUtil.getInstance(context).saveData(HikeConstants.EARPHONE_STATE, PLUGGED);
                break;
            default:
                Log.d(TAG, "I have no idea what the headset state is");
            }
        }
		
	}
}
