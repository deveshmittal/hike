package com.bsb.hike.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

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
				 
				 int maxRetryCount = HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.MAX_REPLY_RETRY_NOTIF_COUNT, HikeConstants.DEFAULT_MAX_REPLY_RETRY_NOTIF_COUNT);
				if (retryCount < maxRetryCount && !HikeNotificationMsgStack.getInstance(context).isEmpty())
				{
					long retryTime = HikeNotification.getInstance(context).getNextRetryNotificationTime(retryCount);
					Logger.i("NotificationDismissedReceiver", "NotificationDismissedReceiver called alarm time = "
							+retryTime  + "retryCount = "+retryCount);
					
					Intent retryNotificationIntent = new Intent();
					retryNotificationIntent.putExtra(HikeConstants.RETRY_COUNT, retryCount+1);
					HikeAlarmManager.setAlarmWithIntent(context, retryTime,
							HikeAlarmManager.REQUESTCODE_RETRY_LOCAL_NOTIFICATION, false, retryNotificationIntent);
				}
			}
		}

	}

}
