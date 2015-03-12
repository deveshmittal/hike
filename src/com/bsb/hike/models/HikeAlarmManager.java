package com.bsb.hike.models;


import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsSender;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.HAManager.EventPriority;
import com.bsb.hike.db.DBBackupRestore;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.platform.PlatformAlarmManager;
import com.bsb.hike.service.PreloadNotificationSchedular;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;


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
 * 
 * TODO:Handling the alarms on forceStop.
 */

		
public class HikeAlarmManager
{

	private static final String INTENT_ALARM = "com.bsb.hike.START_ALARM";
	
	public static final String ALARM_TIME="time";

	// Declare all the request code here .Should be unique.//

	public static final int REQUESTCODE_NOTIFICATION_PRELOAD = 4567;

	public static final int REQUESTCODE_RETRY_LOCAL_NOTIFICATION = 4568;
	
	public static final int REQUESTCODE_HIKE_ANALYTICS = 3456;

	public static final int REQUESTCODE_PERIODIC_BACKUP = 4569;

	public static final int PLATFORM_ALARMS = 4570;

	public static final int REQUESTCODE_DEFAULT = 0;
	
	public static final int REQUESTCODE_REPOPULATE_ALARM_DATABASE=4570;
	
	// ******************************************************//
	
	public static final String INTENT_EXTRA = "intent_extra";

	public static final String TAG = "HikeAlarmManager";

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
		setAlarmWithIntent(context, time, requestCode, WillWakeCPU, in);
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

	public static void setAlarmWithIntent(Context context, long time, int requestCode, boolean WillWakeCPU, Intent intent)
	{

		AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		intent.setAction(INTENT_ALARM);
		intent.putExtra(INTENT_EXTRA, requestCode);
		intent.putExtra(ALARM_TIME,time);
		
		PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (Utils.isKitkatOrHigher())
		{
			mAlarmManager.setExact(WillWakeCPU ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC, time, mPendingIntent);
		}
		else
		{

			mAlarmManager.set(WillWakeCPU ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC, time, mPendingIntent);
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
	public static void setInExact(int requestCode, Context context, long triggerAtMillis, long intervalMillis, boolean WillWakeCPU)
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
	 * @param time
	 * @param requestCode
	 * @param WillWakeCPU
	 * @param persistance---A variable when true saves your request in a Database .so that you dont have to handle it on onboot receive.
	 * 
	 * Also see:
	 * {@link HikeAlarmManager#setAlarm(Context, long, int, boolean)}
	 * 
	 */
	
	public static void setAlarmPersistance(Context context, long time, int requestCode, boolean WillWakeCPU,boolean persistance)
	{
		Intent in = new Intent();
		setAlarmwithIntentPersistance(context, time, requestCode, WillWakeCPU, in,persistance);
	}
	
	/**
	 * 
	 * @param context
	 * @param time
	 * @param requestCode
	 * @param WillWakeCPU
	 * @param intent
	 * @param persistance-A variable when true saves your request in a Database .so that you dont have to handle it on onboot receive.
	 * 
	 * Also see:
	 * {@link HikeAlarmManager#setAlarmwithIntent(Context, long, int, boolean, Intent) }
	 */
	public static void setAlarmwithIntentPersistance(Context context, long time, int requestCode, boolean WillWakeCPU, Intent intent,boolean persistance)
	{

		AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		intent.setAction(INTENT_ALARM);
		intent.putExtra(INTENT_EXTRA, requestCode);
		intent.putExtra(ALARM_TIME, time);

		if (persistance)
			HikeContentDatabase.getInstance(context).insertIntoAlarmManagerDB(time, requestCode, WillWakeCPU, intent);
		
		PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (Utils.isKitkatOrHigher())
		{
			mAlarmManager.setExact(WillWakeCPU ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC, time, mPendingIntent);
		}
		else
		{

			mAlarmManager.set(WillWakeCPU ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC, time, mPendingIntent);
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

	public static void cancelAlarm(Context context, int requestCode)
	{
		AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(INTENT_ALARM);

		PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE);

		if (mPendingIntent != null)
		{
			HikeContentDatabase.getInstance(context).deleteFromAlarmManagerDB(requestCode);
			mAlarmManager.cancel(mPendingIntent);
		}
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
		HikeContentDatabase.getInstance(context).deleteFromAlarmManagerDB(requestCode);
		switch (requestCode)
		{
		case HikeAlarmManager.REQUESTCODE_NOTIFICATION_PRELOAD:
			PreloadNotificationSchedular.run(context);
			break;
		case HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION:
			int retryCount  = intent.getExtras().getInt(HikeConstants.RETRY_COUNT, 0);
			Logger.i(TAG, "processTasks called with request Code "+requestCode+ "time = "+System.currentTimeMillis() +" retryCount = "+retryCount);
			
			try
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.EVENT_KEY, HikeConstants.LogEvent.RETRY_NOTIFICATION_SENT);
				HAManager.getInstance().record(AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT, EventPriority.HIGH, metadata);
			}
			catch(JSONException e)
			{
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
			}

			HikeNotification.getInstance(context).showNotificationForCurrentMsgStack(true, retryCount);
			break;
			
		case HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS:
		{				
			AnalyticsSender.getInstance(context).startUploadAndScheduleNextAlarm();
		}
		break;
		
		case HikeAlarmManager.REQUESTCODE_PERIODIC_BACKUP:
			DBBackupRestore.getInstance(context).backupDB();
			DBBackupRestore.getInstance(context).scheduleNextAutoBackup();
			break;
		default:
			PlatformAlarmManager.processTasks(intent, context);
			break;
		}

	}

	/*
	 * 
	 * This method is used to schedules the alarms again.It fetches the alarm from the database and schedule it again.
	 
	// TODO:calling this function form a background thread
	*/
	
	public static void repopulateAlarm(Context context)
	{
		HikeContentDatabase.getInstance(context).rePopulateAlarmWhenClosed();
	}

	/*
	 * 
	 * This is a callback which is called when the alarm is called at un even time due to any reason
	 * 
	 * Ex:When you schedule your alarm for 4:00pm and the mobile is off from 2-6 pm then the alarm will be received at 6pm in the following function so that the user can handle
	 * appropriately
	 */
	public static void processExpiredTask(Intent intent, Context context)
	{
		int requestCode = intent.getIntExtra(HikeAlarmManager.INTENT_EXTRA, HikeAlarmManager.REQUESTCODE_DEFAULT);

		HikeContentDatabase.getInstance(context).deleteFromAlarmManagerDB(requestCode);

		switch (requestCode)
		{
		case HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS:
			AnalyticsSender.getInstance(context).startUploadAndScheduleNextAlarm();
			break;
		case HikeAlarmManager.REQUESTCODE_NOTIFICATION_PRELOAD:
			PreloadNotificationSchedular.run(context);
			break;
		case HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION:
			processTasks(intent, context);
			break;
		case HikeAlarmManager.REQUESTCODE_PERIODIC_BACKUP:
			processTasks(intent, context);
			break;
		default:
			PlatformAlarmManager.processTasks(intent, context);
			break;
		}

	}

}
