package com.bsb.hike.utils;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.MessagesList;

public class ToastListener implements Listener {

	private WeakReference<Activity> currentActivity;

    private HikeToast toaster;

    private HikeUserDatabase db;

	public ToastListener(Context context) {
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_ACTIVITY, this);
		this.toaster = new HikeToast(context);
        this.db = new HikeUserDatabase(context);
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
			ContactInfo contactInfo = this.db.getContactInfoFromMSISDN(message.getMsisdn());
			this.toaster.toast(contactInfo, message.getMsisdn(), message.getMessage(), message.getTimestamp());
		}
	}

}
