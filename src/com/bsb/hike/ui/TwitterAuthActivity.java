package com.bsb.hike.ui;

import android.os.Bundle;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.utils.AuthSocialAccountBaseActivity;

public class TwitterAuthActivity extends AuthSocialAccountBaseActivity implements Listener
{

	private String[] pubSubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED, HikePubSub.SOCIAL_AUTH_FAILED };

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		startTwitterAuth(getIntent().getBooleanExtra(HikeConstants.Extras.POST_TO_TWITTER, false));
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onBackPressed()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED, false);
		super.onBackPressed();
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type) || HikePubSub.SOCIAL_AUTH_FAILED.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					finish();
				}
			});
		}
	}
}
