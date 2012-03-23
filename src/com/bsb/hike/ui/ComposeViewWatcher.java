package com.bsb.hike.ui;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.NetworkManager;
import com.bsb.hike.models.Conversation;

public class ComposeViewWatcher implements Runnable, TextWatcher, Listener
{
	private Conversation mConversation;

	private long mTextLastChanged = 0;

	private Handler mUIThreadHandler;

	private HikePubSub mPubSub;

	boolean mInitialized;

	private Button mButton;

	private EditText mComposeView;

	private int mCredits;

	public ComposeViewWatcher(Conversation conversation, EditText composeView, Button sendButton, int initialCredits)
	{
		this.mConversation = conversation;
		this.mUIThreadHandler = new Handler();
		this.mPubSub = HikeMessengerApp.getPubSub();
		this.mComposeView = composeView;
		this.mButton = sendButton;
		this.mCredits = initialCredits;
		setBtnEnabled();
	}

	public void init()
	{
		if (mInitialized)
		{
			return;
		}

		mInitialized = true;
		mPubSub.addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		mComposeView.addTextChangedListener(this);
	}

	public void uninit()
	{
		mPubSub.removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		mUIThreadHandler.removeCallbacks(this);
		mComposeView.removeTextChangedListener(this);
		mInitialized = false;
	}

	public void setBtnEnabled()
	{
		CharSequence seq = mComposeView.getText();
		/* the button is enabled iff there is text AND (this is an IP conversation or we have credits available) */
		boolean canSend = (!TextUtils.isEmpty(seq) && ((mConversation.isOnhike() || mCredits > 0)));
		mButton.setEnabled(canSend);
	}

	public void onTextLastChanged()
	{
		if (!mInitialized)
		{
			Log.d("ComposeViewWatcher", "not initialized");
			return;
		}

		long lastChanged = System.currentTimeMillis();
		if (mTextLastChanged == 0)
		{
			// we're currently not in 'typing' mode
			mTextLastChanged = lastChanged;

			// fire an event
			mPubSub.publish(HikePubSub.MQTT_PUBLISH_LOW, mConversation.serialize(NetworkManager.START_TYPING));

			// create a timer to clear the event
			mUIThreadHandler.removeCallbacks(this); 

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

	@Override
	public void afterTextChanged(Editable editable)
	{
		onTextLastChanged();
		setBtnEnabled();
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			mCredits = ((Integer) object).intValue();
		}
	}
};