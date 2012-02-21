package com.bsb.hike.ui;

import android.os.Handler;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.NetworkManager;
import com.bsb.hike.models.Conversation;

class ResetTypingNotification implements Runnable
{
	private Conversation mConversation;
	private long mTextLastChanged = 0;
	private Handler mUIThreadHandler;
	private HikePubSub mPubSub;

	public ResetTypingNotification(Conversation conversation)
	{
		this.mConversation = conversation;
		this.mUIThreadHandler = new Handler();
		this.mPubSub = HikeMessengerApp.getPubSub();
	}

	public void onTextLastChanged()
	{
		long lastChanged = System.currentTimeMillis();
		if (mTextLastChanged == 0)
		{
			// we're currently not in 'typing' mode
			mTextLastChanged = lastChanged;

			// fire an event
			mPubSub.publish(HikePubSub.MQTT_PUBLISH_LOW, mConversation.serialize(NetworkManager.START_TYPING));

			// create a timer to clear the event
			mUIThreadHandler.removeCallbacks(this); // clear
																		// any
																		// existing
																		// ones
			mUIThreadHandler.postDelayed(this, 10 * 1000);
		}

		mTextLastChanged = lastChanged;
	}

	@Override
	public void run()
	{
		long current = System.currentTimeMillis();
		if (current - mTextLastChanged >= 5 * 1000)
		{
			/* text hasn't changed in 10 seconds, send an event */
			mPubSub.publish(HikePubSub.MQTT_PUBLISH_LOW, mConversation.serialize(NetworkManager.END_TYPING));
			mTextLastChanged = 0;
		}
		else
		{
			/* text has changed, fire a new event */
			long delta = 10 * 1000 - (current - mTextLastChanged);
			mUIThreadHandler.postDelayed(this, delta);
		}
	}

	public void clearCallbacks()
	{
		mUIThreadHandler.removeCallbacks(this);	
	}
};