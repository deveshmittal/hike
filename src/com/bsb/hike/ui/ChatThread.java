package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeUserDatabase;

public class ChatThread extends Activity implements HikePubSub.Listener, TextWatcher
{

	private HikePubSub mPubSub;
	
	private HikeConversationsDatabase mConversationDb; 

	private HikeUserDatabase mDbhelper;

	private Cursor mCursor;

	private String mContactId;

	private String mContactName;

	private String mContactNumber;

	private MessagesAdapter mAdapter;

	private EditText mComposeView;

	private ListView mConversationsView;

	private Conversation mConversation;

	private long mTextLastChanged;

	private TextView mNameView;

	private SetTypingText mClearTypingCallback;

	private ResetTypingNotification mResetTypingNotification;

	private Handler mUiThreadHandler;

	private Button mSendBtn;

	private int mCredits;

	private View mMetadataView;

	private TextView mMetadataNumChars;

	private TextView mMetadataCreditsLeft;

	int mMaxSmsLength = 160;

	private String mLabel;

	/* notifies that the adapter has been updated */
	private Runnable mUpdateAdapter;

	@Override
	protected void onPause()
	{
		super.onPause();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		/* TODO evidently a better way to do this is to check for onWindowFocusChanged */
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);

		/* we set a sentinal value while we're rendering the UI to avoid typing notifications.
		 * If one is actually set by the onRestoreInstanceState code prefer that
		 */
		mTextLastChanged = (mTextLastChanged == Long.MAX_VALUE) ? 0 : mTextLastChanged;
	}

	private void createAutoCompleteView()
	{
		View bottomPanel = findViewById(R.id.bottom_panel);
		bottomPanel.setVisibility(View.GONE);
		View nameView = findViewById(R.id.name_field);
		nameView.setVisibility(View.GONE);
		View metadataView = findViewById(R.id.sms_chat_metadata);
		metadataView.setVisibility(View.GONE);
		final AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
		mDbhelper = new HikeUserDatabase(this);
		String[] columns = new String[] { "name", "msisdn", "onhike", "_id" };
		int[] to = new int[] { R.id.name, R.id.number, R.id.onhike };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.name_item, null, columns, to);
		adapter.setViewBinder(new DropDownViewBinder());
		adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter()
		{
			@Override
			public CharSequence convertToString(Cursor cursor)
			{
				mContactName = cursor.getString(cursor.getColumnIndex("name"));
				return mContactName;
			}
		});

		adapter.setFilterQueryProvider(new FilterQueryProvider()
		{
			@Override
			public Cursor runQuery(CharSequence constraint)
			{
				String str = (constraint != null) ? constraint + "%" : "%";
				mCursor = mDbhelper.findUsers(str);
				return mCursor;
			}
		});

		inputNumberView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> list, View _empty, int position, long id)
			{
				/* Extract selected values from the cursor */
				Cursor cursor = (Cursor) list.getItemAtPosition(position);
				mContactId = cursor.getString(cursor.getColumnIndex("_id"));
				mContactNumber = cursor.getString(cursor.getColumnIndex("msisdn"));
				mContactName = cursor.getString(cursor.getColumnIndex("name"));

				/* close the db */
				mDbhelper.close();
				mDbhelper = null;

				/* initialize the conversation */
				createConversation();

				/*
				 * set the focus on the input text box TODO can this be done in createConversation?
				 */
				mComposeView.requestFocus();
			}
		});

		inputNumberView.setAdapter(adapter);
		inputNumberView.setVisibility(View.VISIBLE);
		inputNumberView.requestFocus();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.TYPING_CONVERSATION, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.END_TYPING_CONVERSATION, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		if (mDbhelper != null)
		{
			mDbhelper.close();
			mDbhelper = null;
		}
		if(mConversationDb != null)
		{
			mConversationDb.close();
			mConversationDb = null;
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		Intent intent = new Intent();
		if (mContactName != null)
		{
			intent.putExtra("name", mContactName);
		}
		if (mContactId != null)
		{
			intent.putExtra("id", mContactId);
		}

		if (mContactNumber != null)
		{
			intent.putExtra("msisdn", mContactNumber);
		}

		return intent;
	}

	static final String TEXT_CHANGED_KEY = "text_last_changed";
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putLong(TEXT_CHANGED_KEY, mTextLastChanged);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		mTextLastChanged = savedInstanceState.getLong(TEXT_CHANGED_KEY, 0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		/* disable typing notifications until the UI is rendered.
		 * This is so if any callbacks that are fired due to UI changes will not
		 * cause messages to be sent. */
		mTextLastChanged = Long.MAX_VALUE;

		setContentView(R.layout.chatthread);
		mPubSub = HikeMessengerApp.getPubSub();
		Object o = getLastNonConfigurationInstance();
		Intent intent = (o instanceof Intent) ? (Intent) o : getIntent();
		mContactNumber = intent.getStringExtra("msisdn");
		Uri dataURI = intent.getData();

		if ((dataURI != null) && ("smsto".equals(dataURI.getScheme())))
		{
			// Intent received externally
			String phoneNumber = dataURI.getSchemeSpecificPart();
			ContactInfo contactInfo = ContactUtils.getContactInfo(phoneNumber, this);
			/*
			 * phone lookup fails for a *lot* of people. If that happens, fall back to using their msisdn
			 */
			if (contactInfo != null)
			{
				mContactId = contactInfo.id;
				mContactName = contactInfo.name;
				mContactNumber = contactInfo.number;
			}
			else
			{
				mContactId = null;
				mContactName = mContactNumber = phoneNumber;
			}
			createConversation();
		}
		else if (mContactNumber == null) // wt if i have this number , then
											// removes it and again store it.
		{
			// new conversation thread
			createAutoCompleteView();
		}
		else
		{
			// selected chat from conversation list
			mContactId = intent.getStringExtra("id");
			mContactName = intent.getStringExtra("name");
			createConversation();
		}
		mUiThreadHandler = new Handler();
	}

	public void onSendClick(View v)
	{
		String message = mComposeView.getText().toString();
		mComposeView.setText("");
		long time = (long) System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(message, mContactNumber, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setConversation(mConversation);

		mAdapter.add(convMessage);

		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
		mSendBtn.setEnabled(false);
	}

	/**
	 * Renders the chats for a given user
	 */
	private void createConversation()
	{
		final AutoCompleteTextView inputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
		inputNumberView.setVisibility(View.GONE);

		mLabel = TextUtils.isEmpty(mContactName) ? mContactNumber : mContactName;

		View bottomPanel = findViewById(R.id.bottom_panel);
		bottomPanel.setVisibility(View.VISIBLE);
		mNameView = (TextView) findViewById(R.id.name_field);
		mNameView.setVisibility(View.VISIBLE);
		mNameView.setText(mLabel);

		/*
		 * strictly speaking we shouldn't be reading from the db in the UI Thread
		 */
		mConversationDb = new HikeConversationsDatabase(this);
		mConversation = mConversationDb.getConversation(mContactNumber, 10);
		if (mConversation == null)
		{
			mConversation = mConversationDb.addConversation(mContactNumber);
		}

		mConversationsView = (ListView) findViewById(R.id.conversations_list);
		mConversationsView.setStackFromBottom(true);

		/* make a copy of the message list since it's used internally by the adapter */
		List<ConvMessage> messages = new ArrayList<ConvMessage>(mConversation.getMessages());

		mAdapter = new MessagesAdapter(this, messages, mConversation);
		mConversationsView.setAdapter(mAdapter);
		mComposeView = (EditText) findViewById(R.id.msg_compose);

		HikeMessengerApp.getPubSub().addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.TYPING_CONVERSATION, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.END_TYPING_CONVERSATION, this);
		/* add a text changed listener */
		mComposeView.addTextChangedListener(this);

		mSendBtn = (Button) findViewById(R.id.send_message);

		/* get the number of credits and also listen for changes */
		mCredits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);
		mPubSub.addListener(HikePubSub.SMS_CREDIT_CHANGED, this);

		mMetadataView = findViewById(R.id.sms_chat_metadata);
		mMetadataNumChars = (TextView) findViewById(R.id.sms_chat_metadata_num_chars);
		mMetadataCreditsLeft = (TextView) findViewById(R.id.sms_chat_metadata_text_credits_left);
		if (mConversation.isOnhike())
		{
			mMetadataView.setVisibility(View.GONE);
		}
		else
		{
			mMetadataView.setVisibility(View.VISIBLE);
			updateChatMetadata();
		}

		setBtnEnabled();
		/* create an object that we can notify when the contents of the thread are updated */
		mUpdateAdapter = new UpdateAdapter(mAdapter);
	}

	private class SetTypingText implements Runnable
	{
		public SetTypingText(boolean direction)
		{
			this.direction = direction;
		}

		boolean direction;

		@Override
		public void run()
		{
			if (direction)
			{
				mNameView.setText(mLabel + " is typing");
			}
			else
			{
				mNameView.setText(mLabel);
			}
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.MESSAGE_RECEIVED.equals(type))
		{
			final ConvMessage message = (ConvMessage) object;
			if (message.getMsisdn().indexOf(mContactNumber) != -1)
			{
				/* unset the typing notification */
				runOnUiThread(mClearTypingCallback);
				mUiThreadHandler.removeCallbacks(mClearTypingCallback);
				/*
				 * we publish the message before the conversation is created, so it's safer to just tack it on here
				 */
				message.setConversation(mConversation);
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						mAdapter.add(message);
					}
				});
				mConversationDb.updateMsgStatus(mConversation.getConvId(), message.getMsgID(), ConvMessage.State.RECEIVED_READ.ordinal());
				mPubSub.publish(HikePubSub.WS_SEND, message.serializeDeliveryReport("msgDeliveredRead")); // handle return to sender
			}
		}
		else if (HikePubSub.END_TYPING_CONVERSATION.equals(type))
		{
			if (mContactNumber.equals(object))
			{
				if (mClearTypingCallback != null)
				{
					// we can assume that if we don't have the callback, then
					// the UI should be in the right state already
					runOnUiThread(mClearTypingCallback);
					mUiThreadHandler.removeCallbacks(mClearTypingCallback);
				}
			}
		}
		else if (HikePubSub.TYPING_CONVERSATION.equals(type))
		{
			if (mContactNumber.equals(object))
			{
				runOnUiThread(new SetTypingText(true));
				// Lazily create the callback to reset the label
				if (mClearTypingCallback == null)
				{
					mClearTypingCallback = new SetTypingText(false);
				}
				else
				{
					// we've got another typing notification, so we want to
					// clear it a while from now
					mUiThreadHandler.removeCallbacks(mClearTypingCallback);
				}
				mUiThreadHandler.postDelayed(mClearTypingCallback, 20 * 1000);
			}
		} else if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			mCredits = ((Integer) object).intValue();
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run()
				{
					updateChatMetadata();
				}
			});
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))
		{
			long msgID = (Long)object;
			//TODO we could keep a map of msgId -> conversation objects somewhere to make this faster
			ConvMessage msg = findMessageById(msgID);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				runOnUiThread(mUpdateAdapter);
			}
		}
	}

	private ConvMessage findMessageById(long msgID)
	{
		int count = mAdapter.getCount();
		for(int i = 0; i < count; ++i)
		{
			ConvMessage msg = mAdapter.getItem(i);
			if (msg.getMsgID() == msgID)
			{
				return msg;
			}
		}
		return null;
	}

	public String getContactNumber()
	{
		return mContactNumber;
	}

	class ResetTypingNotification implements Runnable
	{
		@Override
		public void run()
		{
			long current = System.currentTimeMillis();
			if (current - mTextLastChanged >= 5 * 1000)
			{ // text hasn't changed
				// in 10 seconds,
				// send an event
				mPubSub.publish(HikePubSub.WS_SEND, mConversation.serialize("stop_typing"));
				mTextLastChanged = 0;
			}
			else
			{ // text has changed, fire a new event
				long delta = 10 * 1000 - (current - mTextLastChanged);
				mUiThreadHandler.postDelayed(mResetTypingNotification, delta);
			}
		}
	};

	private void setBtnEnabled()
	{
		CharSequence seq = mComposeView.getText();
		/* the button is enabled iff there is text AND (this is an IP conversation or we have credits available) */
		boolean canSend = (!TextUtils.isEmpty(seq) && ((mConversation.isOnhike() || mCredits > 0)));
		mSendBtn.setEnabled(canSend);
	}

	@Override
	public void afterTextChanged(Editable editable)
	{
		/* only update the chat metadata if this is an SMS chat */
		if (!mConversation.isOnhike()) {
			updateChatMetadata();
		}

		setBtnEnabled();
	}

	/* must be called on the UI Thread */
	private void updateChatMetadata()
	{
		/* set the bottom bar to red if we're out of sms credits */
		if (mCredits <= 0)
		{
			mMetadataView.setBackgroundResource(R.color.red);
		} else {
			mMetadataView.setBackgroundResource(R.color.grey);
		}

		int length = mComposeView.getText().length();
		//set the max sms length to a length appropriate to the number of characters we have
		mMaxSmsLength = 160 * (1 + length/160);
		mMetadataNumChars.setText(Integer.toString(length) + "/" + Integer.toString(mMaxSmsLength));
		String formatted = String.format(getResources().getString(R.string.credits_left), mCredits);
		mMetadataCreditsLeft.setText(formatted);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int before, int count)
	{
		// blank
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		if ((before == 0) && (count == 0))
		{
			//we were called on config changed, just ignore
			return;
		}

		/* don't send typing notifications for non-hike chats */
		if (!mConversation.isOnhike())
		{
			return;
		}

		if (mResetTypingNotification == null)
		{
			mResetTypingNotification = new ResetTypingNotification();
		}

		if (mTextLastChanged == 0)
		{
			// we're currently not in 'typing' mode
			mTextLastChanged = System.currentTimeMillis();
			// fire an event
			mPubSub.publish(HikePubSub.WS_SEND, mConversation.serialize("typing"));

			// create a timer to clear the event
			mUiThreadHandler.removeCallbacks(mResetTypingNotification); // clear
																		// any
																		// existing
																		// ones
			mUiThreadHandler.postDelayed(mResetTypingNotification, 10 * 1000);
		}
	}
}
