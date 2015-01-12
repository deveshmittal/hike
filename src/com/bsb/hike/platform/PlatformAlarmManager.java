package com.bsb.hike.platform;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.DBConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.Logger;

public class PlatformAlarmManager
{
	private static final String tag = "platformAlarmManager";

	public static final String HELPER_DATA = "helper_data";

	public static final String CARD_DATA = "card_data";

	public static final String LAYOUT_ID = "layout_id";

	public static final String NOTIFICATION = "notification";

	public static final String INCREASE_UNREAD = "inc_unread";

	public static final String MESSAGE_ID = "message_id";

	public static final String CONV_MSISDN = "conv_msisdn";

	/*
	 * Assuming Format of metadata for platform is :
	 * 
	 * metadata:{'layout_id':'','card_data':{},'helper_data':{}}
	 */
	public static final void processTasks(Intent intent, Context context)
	{
		Logger.i(tag, "Process Tasks Invoked with intent :  " + intent.toString());
		Bundle data = intent.getExtras();
		if (data != null)
		{
			long messageId = data.getInt(MESSAGE_ID);
			if (messageId != 0) // validation
			{
				Cursor cursor = HikeConversationsDatabase.getInstance().getMessage(String.valueOf(messageId));
				if (cursor != null && cursor.moveToFirst())
				{
					try
					{
						String metadataS = cursor.getString(cursor.getColumnIndex(DBConstants.MESSAGE_METADATA));
						JSONObject metadata = new JSONObject(metadataS);

						if (data.containsKey(CARD_DATA))
						{
							metadata.put(CARD_DATA, data.getString(CARD_DATA));
						}
						if (data.containsKey(HELPER_DATA))
						{
							updateHelperData(data.getString(HELPER_DATA), metadata);
						}
						if (data.containsKey(LAYOUT_ID))
						{
							metadata.put(LAYOUT_ID, data.getString(LAYOUT_ID));
						}

					}
					catch (Exception jsoException)
					{
						jsoException.printStackTrace();
					}
				}
				increaseUnreadCount(data, context);
				showNotification(data);
			}
		}
	}

	private static String updateHelperData(String helper, JSONObject metadataJSON)
	{
		try
		{
			JSONObject helperData = new JSONObject(helper);
			JSONObject oldHelper = metadataJSON.optJSONObject(HELPER_DATA);
			if (oldHelper == null)
			{
				oldHelper = new JSONObject();
			}
			Iterator<String> i = helperData.keys();
			while (i.hasNext())
			{
				String key = i.next();
				oldHelper.put(key, helperData.get(key));
			}
			metadataJSON.put(HELPER_DATA, oldHelper.toString());
			return metadataJSON.toString();
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private static void increaseUnreadCount(Bundle data, Context context)
	{
		if (data.containsKey(CONV_MSISDN) && data.containsKey(INCREASE_UNREAD))
		{
			 
			// increase unread count
			HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
			String msisdn = data.getString(CONV_MSISDN);
			int dbUnreadCount = db.getConvUnreadCount(msisdn);
			int count = db.getExtraConvUnreadCount(data.getString(CONV_MSISDN));
			count++;
			db.setExtraConvUnreadCount(msisdn, count);
			Message ms = Message.obtain();
			ms.arg1 = count + dbUnreadCount; // db + extra unread
			ms.obj = msisdn;
			HikeMessengerApp.getPubSub().publish(HikePubSub.CONV_UNREAD_COUNT_MODIFIED, ms);
		}
	}

	private static void showNotification(Bundle data)
	{
		if (data.containsKey(CONV_MSISDN))
		{
			// TODO: Show Notification
		}
	}
}
