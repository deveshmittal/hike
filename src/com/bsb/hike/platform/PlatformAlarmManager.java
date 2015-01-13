package com.bsb.hike.platform;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.notifications.HikeNotification;
import com.bsb.hike.utils.Logger;

public class PlatformAlarmManager
{
	private static final String tag = "platformAlarmManager";

	public static final String PLATFORM_ALARM = "platform_alarm";
	
	public static final String NOTIFICATION = "notification";

	public static final String NOTIFICATION_SOUND = "notification_sound";

	public static final String INCREASE_UNREAD = "inc_unread";

	public static final String MESSAGE_ID = "message_id";

	public static final String CONV_MSISDN = "conv_msisdn";

	public static final void setAlarm(Context context, String data, long messageId, long timeInMills)
	{
		Intent intent = new Intent();
		intent.putExtra(MESSAGE_ID, messageId); // for us uniqueness of a card is message id
		intent.putExtra(PLATFORM_ALARM, data);
		HikeAlarmManager.setAlarmwithIntentPersistance(context, timeInMills, (int) (100000 + messageId), true, intent, true); // 100000 is added to avoid conflict between
	}

	/*
	 * Assuming Format of metadata for platform is :
	 * 
	 * metadata:{'layout_id':'','file_id':'','card_data':{},'helper_data':{}}
	 */
	public static final void processTasks(Intent intent, Context context)
	{
		Logger.i(tag, "Process Tasks Invoked with intent :  " + intent.toString());
		Bundle data = intent.getExtras();
		if (data != null && data.containsKey(PLATFORM_ALARM))
		{
			int messageId = data.getInt(MESSAGE_ID);
			if (messageId != 0) // validation
			{
				HikeContentDatabase.getInstance(context).insertUpdateAppAlarm(messageId, data.getString(PLATFORM_ALARM));
				increaseUnreadCount(data, context);
				showNotification(data,context);
				Message m = Message.obtain();
				m.arg1 = messageId;
				m.obj = data.get(PLATFORM_ALARM);
				HikeMessengerApp.getPubSub().publish(HikePubSub.PLATFORM_CARD_ALARM, m);
			}
		}
	}


	private static void increaseUnreadCount(Bundle data, Context context)
	{
		if (data.containsKey(CONV_MSISDN) && data.containsKey(INCREASE_UNREAD))
		{

			// increase unread count
			HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
			String msisdn = data.getString(CONV_MSISDN);
			int dbUnreadCount = db.getConvUnreadCount(msisdn);
			if (dbUnreadCount != -1)
			{
				int count = db.getExtraConvUnreadCount(data.getString(CONV_MSISDN));
				count++;
				db.setExtraConvUnreadCount(msisdn, count);
				Message ms = Message.obtain();
				ms.arg1 = count + dbUnreadCount; // db + extra unread
				ms.obj = msisdn;
				HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_UNREAD_COUNT_MODIFIED, ms);
			}
		}
	}

	private static void showNotification(Bundle data,Context context)
	{
		if (data.containsKey(CONV_MSISDN))
		{
			String message = data.getString(NOTIFICATION);
			if(TextUtils.isEmpty(message)){
				boolean playSound = data.getBoolean(NOTIFICATION_SOUND);
				HikeNotification.getInstance(context).notifyStringMessage(data.getString(CONV_MSISDN), message, !playSound);
			}
		}
	}
}
