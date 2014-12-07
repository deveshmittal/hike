package com.bsb.hike.models;

import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.Logger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * A AlarmManager Utility class to set alarms at specific times to perform functions.
 * 
 * The alarm has to be scheduled again if the device gets rebooted.
 * 
 * The request codes should be unique.
 * 
 * Kindly declare a unique request code for each request which is going to fire at different intervals
 * 
 * If you set two alarms consecutively with the same request code,by default android cancels your first request and replace it with the other.
 * 
 * When the application is killed(by swiping) the alarms are not cancelled.But When the application is force closed through settings,the alarms are reset and has to be Rescheduled
 * again by the user.
 * 
 */

public class HikeAlarmManager
{

	private static final String INTENT_ALARM = "com.bsb.hike.START_ALARM";

	// Declare all the request code here .Should be unique.//

	public static final int REQUESTCODE_NOTIFICATION_PRELOAD = 4567;
	
	public static final int REQUESTCODE_RETRY_LOCAL_NOTIFICATION = 4568;

	public static final int REQUESTCODE_DEFAULT = 0;

	// ******************************************************//
	public static final String INTENT_EXTRA = "intent_extra";
	
	public static final String LOG_TAG = "HikeAlarmManager";

	/**
	 * 
	 * @param context
	 * @param time
	 * @param requestCode
	 * @param WillWakeCPU
	 *            \n
	 * 
	 * 
	 * @see <a href = "http://developer.android.com/reference/android/app/AlarmManager.html#set(int, long, android.app.PendingIntent)"> setAlarm </a>
	 */
	public static void setAlarm(Context context, long time, int requestCode, boolean WillWakeCPU)
	{
		Intent in = new Intent();
		setAlarmwithIntent(context, time, requestCode, WillWakeCPU, in);
	}

	/**
	 * A function where you can pass your own intent with extras. Using FLAG :FLAG_UPDATE_CURRENT:It will update the previous intent with the new intent.
	 * 
	 * @param context
	 * @param time
	 * @param requestCode
	 * @param WillWakeCPU
	 * @param intent
	 * 
	 * @see <a href = "http://developer.android.com/reference/android/app/AlarmManager.html#set(int, long, android.app.PendingIntent)"> setAlarm </a>
	 */

	public static void setAlarmwithIntent(Context context, long time, int requestCode, boolean WillWakeCPU, Intent intent)
	{

		AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		intent.setAction(INTENT_ALARM);
		intent.putExtra(INTENT_EXTRA, requestCode);

		PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (WillWakeCPU)
		{
			mAlarmManager.set(AlarmManager.RTC_WAKEUP, time, mPendingIntent);
		}

		else
		{
			mAlarmManager.set(AlarmManager.RTC, time, mPendingIntent);
		}
	}

	/**
	 * 
	 * @param requestCode
	 * @param context
	 * @param type
	 * @param triggerAtMillis
	 * @param intervalMillis
	 * 
	 *            Provides a setInexact implementation of alarmanager.
	 * 
	 * 
	 * @see <a href = "http://developer.android.com/reference/android/app/AlarmManager.html#setInexactRepeating(int, long, long, android.app.PendingIntent)"> setInExactAlarm </a>
	 */
	public static void setInexact(int requestCode, Context context, long triggerAtMillis, long intervalMillis, boolean WillWakeCPU)
	{

		AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(INTENT_ALARM);

		intent.putExtra(INTENT_EXTRA, requestCode);
		// Determine if the alarm has already been set
		PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (WillWakeCPU)
		{
			mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, mPendingIntent);

		}
		else
		{
			mAlarmManager.setInexactRepeating(AlarmManager.RTC, triggerAtMillis, intervalMillis, mPendingIntent);
		}
	}

	/**
	 * 
	 * @param context
	 * 
	 *            Cancel all the alarms.
	 * 
	 *            Using FLAG:FLAG_NO_CREATE It returns the pending intent if it already exists else it will return null.The documentation says the opposite thing:(
	 * 
	 * @see <a href = "http://developer.android.com/reference/android/app/AlarmManager.html#cancel(android.app.PendingIntent)"> CancelAlarm </a>
	 */

	public static void cancelAlaram(Context context, int requestCode)
	{
		AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(INTENT_ALARM);

		PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE);

		if (mPendingIntent != null)
			mAlarmManager.cancel(mPendingIntent);

	}

	/**
	 * Process the tasks at specific time.
	 * 
	 * @param intent
	 * @param context
	 * 
	 * 
	 */
	public static void processTasks(Intent intent, Context context)
	{

		int requestCode = intent.getIntExtra(HikeAlarmManager.INTENT_EXTRA, HikeAlarmManager.REQUESTCODE_DEFAULT);

		Logger.i(LOG_TAG, "processTasks called with request Code "+requestCode+ "time = "+System.currentTimeMillis());
		
		switch (requestCode)
		{
		case HikeAlarmManager.REQUESTCODE_NOTIFICATION_PRELOAD:
			// PreloadNotificationSchedular.performActionWhenAlarmReceived(context);
			break;
		case HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION:
			HikeNotification.getInstance(context).showNotificationForCurrentMsgStack(true);
			break;
		default:

		}

	}

}
