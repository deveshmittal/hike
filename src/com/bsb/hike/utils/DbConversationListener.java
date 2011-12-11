package com.bsb.hike.utils;

import android.content.Context;

import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.models.ConvMessage;

public class DbConversationListener implements Listener {

	HikeConversationsDatabase mConversationDb;
	HikeUserDatabase mUserDb;

	public DbConversationListener(Context context) {
		mConversationDb = new HikeConversationsDatabase(context);
		mUserDb = new HikeUserDatabase(context);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.MESSAGE_SENT.equals(type)) {
			ConvMessage message = (ConvMessage) object;
			mConversationDb.addConversation(message);
		} else if (HikePubSub.MESSAGE_RECEIVED.equals(type)) {
			ConvMessage message = (ConvMessage) object;
			mConversationDb.addConversation(message);
			//TODO update the unread flags here
		}
	}

}
