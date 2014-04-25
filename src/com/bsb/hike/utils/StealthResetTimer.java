package com.bsb.hike.utils;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

public class StealthResetTimer implements Runnable
{
	private static final int RESET_TIME_MS = 10 * 1000;

	private static StealthResetTimer stealthResetTimer;

	private Handler handler;

	private Context context;

	private StealthResetTimer(Context context)
	{
		this.handler = new Handler();
		this.context = context;
	}

	public static StealthResetTimer getInstance(Context context)
	{
		if (stealthResetTimer == null)
		{
			stealthResetTimer = new StealthResetTimer(context.getApplicationContext());
		}
		return stealthResetTimer;
	}

	public void resetStealth()
	{
		clearScheduledTimer();
		handler.postDelayed(this, RESET_TIME_MS);
	}

	public void clearScheduledTimer()
	{
		handler.removeCallbacks(this);
	}

	@Override
	public void run()
	{
		HikeSharedPreferenceUtil.getInstance(context).saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
	}
}
