package com.bsb.hike.service;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsSender;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class BootService extends BroadcastReceiver
{

	@Override
	public void onReceive(Context ctx, Intent intent)
	{
		Logger.i("HikeBootService", "Received onBoot intent");
		HikeSharedPreferenceUtil mprefs = HikeSharedPreferenceUtil.getInstance(ctx);

		// GCM_ID_SENT_PRELOAD=true,User Auth=false-->Best Scenario

		if (mprefs.getData(HikeMessengerApp.GCM_ID_SENT_PRELOAD, false) && (!Utils.isUserAuthenticated(ctx)))
		{
			PreloadNotificationSchedular.scheduleNextAlarm(ctx);
		}
		if (TextUtils.isEmpty(mprefs.getData(HikeMessengerApp.TOKEN_SETTING, null)))
		{
			return;
		}		
		Intent startServiceIntent = new Intent(ctx, HikeService.class);
		ctx.startService(startServiceIntent);
		
		long whenToSend = Utils.getTimeInMillis(Calendar.getInstance(), HAManager.getInstance(ctx).getWhenToSend(), 0, 0);
		HikeAlarmManager.setAlarm(ctx, whenToSend, HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS, false);
	}

}
