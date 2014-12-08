package com.bsb.hike.notifications;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This receiver is responsible for capturing notification dismissed/deleted events and consequently clear notification message stack. This is done so that messages already shown
 * once in a notification are not repeated in subsequent notifications.
 * 
 * @author Atul M
 * 
 */
public class NotificationDismissedReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent != null)
		{
			int notificationId = intent.getIntExtra(HikeNotification.HIKE_NOTIFICATION_ID_KEY, 0);

			if (notificationId == HikeNotification.HIKE_SUMMARY_NOTIFICATION_ID)
			{
				//Get current count of retry.
				 int retryCount  = intent.getExtras().getInt(HikeConstants.RETRY_COUNT, 0);
				 //Right now we retry only once.
				if (retryCount < 1 && !HikeNotificationMsgStack.getInstance(context).isEmpty())
				{
					Logger.i("NotificationDismissedReceiver", "NotificationDismissedReceiver called alarm time = "
							+ HikeNotification.getInstance(context).getNextRetryNotificationTime() + "retryCount = "+retryCount);
					
					Intent retryNotificationIntent = new Intent();
					retryNotificationIntent.putExtra(HikeConstants.RETRY_COUNT, retryCount+1);
					HikeAlarmManager.setAlarmwithIntent(context, HikeNotification.getInstance(context).getNextRetryNotificationTime(),
							HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION, false, retryNotificationIntent);
				}
			}
		}

	}

}
