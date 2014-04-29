package com.bsb.hike.utils;

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
			Logger.d("ActivityTimeLogger", "CURRENTLY IN: " + activityName);
		}
		else
		{
			onTime = System.currentTimeMillis() - onTime;
			Logger.d("ActivityTimeLogger", "Stayed in " + activityName + " for " + onTime);
		}
	}

}
