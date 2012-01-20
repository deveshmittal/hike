package com.bsb.hike.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.ui.ChatThread;

public class HikeToast
{
	private static final int NEW_MESSAGE_ID = 1;

	private Context context;

	private NotificationManager notificationManager;

	public HikeToast(Context context)
	{
		this.context = context;
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void toast(ContactInfo contactInfo,ConvMessage convMsg)
	{
		String msisdn = convMsg.getMsisdn();
		String message = convMsg.getMessage();
		long timestamp = convMsg.getTimestamp();
		String key = (contactInfo != null) ? contactInfo.name : msisdn;
		
		int icon = R.drawable.ic_contact_logo;

		// TODO this doesn't turn the text bold :(
		Spanned text = Html.fromHtml(String.format("<bold>%1$s</bold>", key, msisdn));
		Notification notification = new Notification(icon, text, timestamp * 1000);

		notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;

		int notificationId = (int)convMsg.getConversation().getConvId();
		Intent notificationIntent = new Intent(context, ChatThread.class);

		/* notifications appear to be cached, and their .equals doesn't check 'Extra's.
		 * In order to prevent the wrong intent being fired, set a data field that's unique to the
		 * conversation we want to open.
		 * http://groups.google.com/group/android-developers/browse_thread/thread/e61ec1e8d88ea94d/1fe953564bd11609?#1fe953564bd11609
		 */

		notificationIntent.setData((Uri.parse("custom://"+notificationId)));

		notificationIntent.putExtra("msisdn", msisdn);
		if (contactInfo != null)
		{
			if (contactInfo.id != null)
			{
				notificationIntent.putExtra("id", contactInfo.id);
			}
			if (contactInfo.name != null)
			{
				notificationIntent.putExtra("name", contactInfo.name);
			}
		}

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, key, message, contentIntent);

		Log.d("HIKE TOAST","CONVERSATION ID : " + notificationId);
		notificationManager.notify(notificationId, notification);
	}
}
