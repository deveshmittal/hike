package com.bsb.hike.notifications;

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
				if (!HikeNotificationMsgStack.getInstance(context).isEmpty())
				{
					Logger.i("NotificationDismissedReceiver", "NotificationDismissedReceiver called alarm time = "
							+ HikeNotification.getInstance(context).getNextRetryNotificationTime());
					Intent retryNotificationIntent = new Intent();
					HikeAlarmManager.setAlarmwithIntent(context, HikeNotification.getInstance(context).getNextRetryNotificationTime(),
							HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION, true, retryNotificationIntent);
				}
			}
		}

	}

}
