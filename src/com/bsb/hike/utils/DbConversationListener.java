package com.bsb.hike.utils;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.models.ConvMessage;

public class DbConversationListener implements Listener
{

	HikeConversationsDatabase mConversationDb;

	HikeUserDatabase mUserDb;

	private Editor mEditor;

	public DbConversationListener(Context context)
	{
		mConversationDb = new HikeConversationsDatabase(context);
		mUserDb = new HikeUserDatabase(context);
		mEditor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_SENT, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.MESSAGE_SENT.equals(type))
		{
			ConvMessage message = (ConvMessage) object;
			mConversationDb.addConversationMessages(message);
		}
		else if (HikePubSub.MESSAGE_RECEIVED.equals(type))
		{
			ConvMessage message = (ConvMessage) object;
			mConversationDb.addConversationMessages(message);
			// TODO update the unread flags here
		} if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			Integer credits = (Integer) object;
			mEditor.putInt(HikeMessengerApp.SMS_SETTING, credits.intValue());
			mEditor.commit();
		}
	}

}
