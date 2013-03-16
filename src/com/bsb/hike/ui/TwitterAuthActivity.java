package com.bsb.hike.ui;

import android.os.Bundle;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.utils.AuthSocialAccountBaseActivity;

public class TwitterAuthActivity extends AuthSocialAccountBaseActivity
		implements Listener {

	private String[] pubSubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED,
			HikePubSub.SOCIAL_AUTH_FAILED };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startTwitterAuth(false);
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	protected void onDestroy() {
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type)
				|| HikePubSub.SOCIAL_AUTH_FAILED.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					finish();
				}
			});
		}
	}
}
