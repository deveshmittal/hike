package com.bsb.hike.service;

import android.app.IntentService;
import android.content.Intent;

import com.bsb.hike.models.HikeAlarmManager;

/**
 * 
 * @author himanshu
 * 
 *         This class is used to perform task that are received in AlarmBroadcastReceiver.The method onHandle Intent runs on backgroundthread.
 */
public class SimpleWakefulService extends IntentService
{

	private final long TIME_DIFFERENCE =10 * 60 * 1000;
	
	
	public SimpleWakefulService()
	{
		super("SimpleWakeFulService");
	}

	public SimpleWakefulService(String name)
	{
		super("SimpleWakeFulService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{

		try
		{
			long time = intent.getLongExtra(HikeAlarmManager.ALARM_TIME, HikeAlarmManager.REQUESTCODE_DEFAULT);
			/**
			 * 
			 Could not remember why I did this ...so commenting it ....
			//boolean cpuflag=intent.getBooleanExtra(HikeAlarmManager.WAKE_CPU_FLAG, true);
			//if (!cpuflag || Math.abs(time - System.currentTimeMillis()) < (10 * 60 * 1000))
			*/
			
			/*
			 * Here I am taking into account the optimization that the CPU will do to alarms ...So if you schedule an alarm at 3:00 pm and the alarm gets fired at 3:05 
			 *  we are assuming that it is due to system optimization.
			 */
			
			if(Math.abs(time - System.currentTimeMillis()) < (TIME_DIFFERENCE))
				HikeAlarmManager.processTasks(intent, this);
			else
				HikeAlarmManager.processExpiredTask(intent, this);
		}
		catch (Exception e)
		{
			e.printStackTrace();

		}
		finally
		{
			AlarmBroadcastReceiver.completeWakefulIntent(intent);
		}

	}

}
