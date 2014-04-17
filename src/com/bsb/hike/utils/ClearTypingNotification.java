package com.bsb.hike.utils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.TypingNotification;

public class ClearTypingNotification implements Runnable
{
	String id;

	public ClearTypingNotification(String id)
	{
		this.id = id;
	}

	@Override
	public void run()
	{
		TypingNotification typingNotification = HikeMessengerApp.getTypingNotificationSet().remove(id);

		HikeMessengerApp.getPubSub().publish(HikePubSub.END_TYPING_CONVERSATION, typingNotification);
	}
};