package com.bsb.hike.utils;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.ui.ChatThread;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

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
		String name = (contactInfo != null) ? contactInfo.name : msisdn;
		
		Log.d("HIKE TOAST","MSISDN : "+msisdn+" , message : "+message+" , name : "+name);
		int icon = R.drawable.ic_contact_logo;

		// TODO this doesn't turn the text bold :(
		Spanned text = Html.fromHtml(String.format("<bold>%1$s</bold>:%2$s", name, msisdn));
		Notification notification = new Notification(icon, text, timestamp * 1000);

		notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;

		Intent notificationIntent = new Intent(context, ChatThread.class);
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
		notification.setLatestEventInfo(context, name, message, contentIntent);
		
		Log.d("HIKE TOAST","CONVERSATION ID : "+(int)convMsg.getConversation().getConvId());
		notificationManager.notify((int)convMsg.getConversation().getConvId(), notification);
	}
}
