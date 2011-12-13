package com.bsb.hike;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.Html;
import android.text.Spanned;

import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.utils.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeUserDatabase;

public class ToastListener implements Listener {

	private static final int NEW_MESSAGE_ID = 1;

	private Context context;
	private WeakReference<Activity> currentActivity;
	private NotificationManager notificationManager;

	public ToastListener(Context ctx) {
		this.context = ctx;
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_ACTIVITY, this);
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.NEW_ACTIVITY.equals(type)) {
			System.out.println("new activity is front " + object);
			currentActivity = new WeakReference<Activity>((Activity) object);
		} else if (HikePubSub.MESSAGE_RECEIVED.equals(type)) {
			System.out.println("new message received");
			ConvMessage message = (ConvMessage) object;
			Activity activity = currentActivity.get();
			if ((activity instanceof ChatThread )) {
				String contactNumber = ((ChatThread) activity).getContactNumber();
				if (message.getMsisdn().equals(contactNumber)) {
					return;
				}
			} else if (activity instanceof MessagesList) {
				return;
			}
			/* the foreground activity isn't going to show this message so Toast it */

			HikeUserDatabase db = new HikeUserDatabase(context);
			ContactInfo contactInfo = db.getContactInfoFromMSISDN(message.getMsisdn());
			db.close();

			String name = (contactInfo != null) ? contactInfo.name : message.getMsisdn();
			int icon = R.drawable.ic_launcher;
			Spanned text = Html.fromHtml(String.format("<bold>%1$s</bold>:%2$s", name, message.getMessage()));
			Notification notification = new Notification(icon, text, message.getTimestamp());
			notification.flags = notification.flags | Notification.FLAG_AUTO_CANCEL;

			Intent notificationIntent = new Intent(context, ChatThread.class);
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
			notification.setLatestEventInfo(context, name, message.getMessage(), contentIntent);
			notificationManager.notify(NEW_MESSAGE_ID, notification);
		}
	}

}
