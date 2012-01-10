package com.bsb.hike.utils;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.ui.ChatThread;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;

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

	public void toast(ContactInfo contactInfo, String msisdn, String message, long timestamp)
	{
		String name = (contactInfo != null) ? contactInfo.name : msisdn;
		int icon = R.drawable.ic_launcher;

		// TODO this doesn't turn the text bold :(
		Spanned text = Html.fromHtml(String.format("<bold>%1$s</bold>:%2$s", name, msisdn));
		Notification notification = new Notification(icon, text, timestamp);
		notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;

		Intent notificationIntent = new Intent(context, ChatThread.class);
		notificationIntent.putExtra("msisdn", msisdn);

		if (contactInfo != null)
		{
			if (contactInfo.id != null)
			{
				notificationIntent.putExtra("id", Long.parseLong(contactInfo.id));
			}
			if (contactInfo.name != null)
			{
				notificationIntent.putExtra("name", contactInfo.name);
			}
		}

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, name, message, contentIntent);
		notificationManager.notify(NEW_MESSAGE_ID, notification);
	}
}
