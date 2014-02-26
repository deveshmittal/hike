package com.bsb.hike.utils;

import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;

public class ActivityTimeLogger implements Listener
{

	private String activityName;

	private long onTime;

	public ActivityTimeLogger()
	{
		HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_ACTIVITY, this);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (object != null)
		{
			activityName = object.getClass().getSimpleName();
			onTime = System.currentTimeMillis();
			Log.d("ActivityTimeLogger", "CURRENTLY IN: " + activityName);
		}
		else
		{
			onTime = System.currentTimeMillis() - onTime;
			Log.d("ActivityTimeLogger", "Stayed in " + activityName + " for " + onTime);
		}
	}

}
