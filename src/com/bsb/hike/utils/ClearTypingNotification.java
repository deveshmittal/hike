package com.bsb.hike.utils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

public class ClearTypingNotification implements Runnable {
	String msisdn;

	public ClearTypingNotification(String msisdn) {
		this.msisdn = msisdn;
	}

	@Override
	public void run() {
		HikeMessengerApp.getTypingNotificationSet().remove(msisdn);

		HikeMessengerApp.getPubSub().publish(
				HikePubSub.END_TYPING_CONVERSATION, msisdn);
	}
};