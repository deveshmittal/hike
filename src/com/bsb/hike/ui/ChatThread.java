package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
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

	private Conversation mConversation;

	private long mTextLastChanged;

	private TextView mNameView;

	private SetTypingText mClearTypingCallback;

	private ResetTypingNotification mResetTypingNotification;

	private Handler mUiThreadHandler;

	/* View element */
	private Button mSendBtn;

	private int mCredits;

	private View mMetadataView;

	private TextView mMetadataNumChars;

	private TextView mMetadataCreditsLeft;

	private View mBottomView;

	private EditText mComposeView;

	private ListView mConversationsView;

	private AutoCompleteTextView mInputNumberView;

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

		/*
		 * we set a sentinal value while we're rendering the UI to avoid typing notifications. If one is actually set by the onRestoreInstanceState code prefer that
		 */
		mTextLastChanged = (mTextLastChanged == Long.MAX_VALUE) ? 0 : mTextLastChanged;
	}

	/* msg is any text we want to show initially */
	private void createAutoCompleteView(String msg)
	{
		mNameView.setVisibility(View.GONE);
		mMetadataView.setVisibility(View.GONE);
		mComposeView.removeTextChangedListener(this);

		/* if we've got some pre-filled text, add it here */
		if (TextUtils.isEmpty(msg)) {
			mBottomView.setVisibility(View.GONE);
		} else {
			mComposeView.setText(msg);
			/* make sure that the autoselect text is empty */
			mInputNumberView.setText("");
		}

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
				String str = (constraint != null) ? "%" + constraint + "%" : "%";
				mCursor = mDbhelper.findUsers(str);
				return mCursor;
			}
		});

		mInputNumberView.setOnItemClickListener(new AdapterView.OnItemClickListener()
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

		mInputNumberView.setAdapter(adapter);
		mInputNumberView.setVisibility(View.VISIBLE);
		mInputNumberView.requestFocus();

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
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_DELIVERED_READ, this);
		if (mDbhelper != null)
		{
			mDbhelper.close();
			mDbhelper = null;
		}
		if (mConversationDb != null)
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
		/*
		 * disable typing notifications until the UI is rendered. This is so if any callbacks that are fired due to UI changes will not cause messages to be sent.
		 */
		mTextLastChanged = Long.MAX_VALUE;

		// TODO this is being called everytime this activity is created. Way too often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		setContentView(R.layout.chatthread);

		/* bind views to variables */
		mBottomView = findViewById(R.id.bottom_panel);
		mNameView = (TextView) findViewById(R.id.name_field);
		mMetadataView = findViewById(R.id.sms_chat_metadata);
		mInputNumberView = (AutoCompleteTextView) findViewById(R.id.input_number);
		mConversationsView = (ListView) findViewById(R.id.conversations_list);
		mComposeView = (EditText) findViewById(R.id.msg_compose);
		mSendBtn = (Button) findViewById(R.id.send_message);
		mMetadataView = findViewById(R.id.sms_chat_metadata);
		mMetadataNumChars = (TextView) findViewById(R.id.sms_chat_metadata_num_chars);
		mMetadataCreditsLeft = (TextView) findViewById(R.id.sms_chat_metadata_text_credits_left);

		/* register for long-press's */
		registerForContextMenu(mConversationsView);

		mConversationDb = new HikeConversationsDatabase(this);

		mPubSub = HikeMessengerApp.getPubSub();
		Object o = getLastNonConfigurationInstance();
		Intent intent = (o instanceof Intent) ? (Intent) o : getIntent();
		onNewIntent(intent);

		/* add a handler on the UI thread so we can post delayed messages */
		mUiThreadHandler = new Handler();

		/* register listeners */
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.TYPING_CONVERSATION, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.END_TYPING_CONVERSATION, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED_READ, this);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		ConvMessage message = mAdapter.getItem((int) info.id);
		switch (item.getItemId())
		{
		case R.id.copy:
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(message.getMessage());
			return true;
		case R.id.forward:
			Intent intent = new Intent(this, ChatThread.class);
			intent.putExtra("msg", message.getMessage());
			startActivity(intent);
			return true;
		case R.id.delete:
			mPubSub.publish(HikePubSub.MESSAGE_DELETED, message.getMsgID());
			mAdapter.remove(message);
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.message_menu, menu);
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

	/*
	 * this function is called externally when our Activity is on top and the user selects an Intent for this same Activity
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent)
	{
		/* setIntent so getIntent returns the right values */
		setIntent(intent);

		Uri dataURI = intent.getData();

		if (mAdapter != null)
		{
			mAdapter.clear();
		}

		mConversation = null;

		if ((dataURI != null) && "smsto".equals(dataURI.getScheme()))
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
		else if (intent.hasExtra("msisdn"))
		{
			// selected chat from conversation list
			mContactNumber = intent.getStringExtra("msisdn");
			mContactId = intent.getStringExtra("id");
			mContactName = intent.getStringExtra("name");

			createConversation();
		}
		else
		{
			createAutoCompleteView(intent.getStringExtra("msg"));
		}
	}

	/**
	 * Renders the chats for a given user
	 */
	private void createConversation()
	{
		mInputNumberView.setVisibility(View.GONE);

		mLabel = TextUtils.isEmpty(mContactName) ? mContactNumber : mContactName;

		mBottomView.setVisibility(View.VISIBLE);

		mNameView.setVisibility(View.VISIBLE);
		mNameView.setText(mLabel);

		/*
		 * strictly speaking we shouldn't be reading from the db in the UI Thread
		 */
		mConversation = mConversationDb.getConversation(mContactNumber, 25);
		if (mConversation == null)
		{
			mConversation = mConversationDb.addConversation(mContactNumber);
		}

		mConversationsView.setStackFromBottom(true);

		/* make a copy of the message list since it's used internally by the adapter */
		List<ConvMessage> messages = new ArrayList<ConvMessage>(mConversation.getMessages());

		mAdapter = new MessagesAdapter(this, messages, mConversation);
		mConversationsView.setAdapter(mAdapter);

		/* add a text changed listener */
		mComposeView.addTextChangedListener(this);

		/* get the number of credits and also listen for changes */
		mCredits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);
		mPubSub.addListener(HikePubSub.SMS_CREDIT_CHANGED, this);

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
		long convID = mConversation.getConvId();
		/* this is to check if last msg was a sent msg category for a particular conversation id */
		if (!isLastMsgSent())
		{
			JSONArray ids = mConversationDb.updateStatusAndSendDeliveryReport(convID);
			/* If there are msgs which are RECEIVED UNREAD then only broadcast a msg that these are read. */
			if (ids != null)
			{
				JSONObject object = new JSONObject();
				try
				{
					object.put("t", "mr");
					object.put("r", mConversation.getMsisdn());
					object.put("d", ids);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				mPubSub.publish(HikePubSub.MSG_READ,mConversation.getMsisdn()); 
				mPubSub.publish(HikePubSub.MQTT_PUBLISH, object);
			}
		}

		/* clear any toast notifications */
		NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mgr.cancel((int) mConversation.getConvId());
	}

	private boolean isLastMsgSent()
	{
		List<ConvMessage> msgList = mConversation.getMessages();

		if ((msgList == null) || (msgList.isEmpty()))
		{
			return false;
		}

		ConvMessage lastMsg = msgList.get(msgList.size() - 1);

		if (lastMsg.isSent() || lastMsg.getState() == ConvMessage.State.RECEIVED_READ)
			return true;
		else
		{
			return false;
		}
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
				mConversationDb.updateMsgStatus(message.getMsgID(), ConvMessage.State.RECEIVED_READ.ordinal());
				mPubSub.publish(HikePubSub.MQTT_PUBLISH, message.serializeDeliveryReportRead()); // handle return to sender
			}
			mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn()); 
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
		}
		else if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
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
			long msgID = (Long) object;
			// TODO we could keep a map of msgId -> conversation objects somewhere to make this faster
			ConvMessage msg = findMessageById(msgID);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				runOnUiThread(mUpdateAdapter);
			}
		}
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))
		{
			long[] ids = (long[]) object;
			// TODO we could keep a map of msgId -> conversation objects somewhere to make this faster
			for (int i = 0; i < ids.length; i++)
			{
				ConvMessage msg = findMessageById(ids[i]);
				if (msg != null)
				{
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				}
			}
			runOnUiThread(mUpdateAdapter);
		}
	}

	private ConvMessage findMessageById(long msgID)
	{
		if (mAdapter == null)
		{
			return null;
		}

		int count = mAdapter.getCount();
		for (int i = 0; i < count; ++i)
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
				mPubSub.publish(HikePubSub.MQTT_PUBLISH, mConversation.serialize("et"));
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
		if (!mConversation.isOnhike())
		{
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
		}
		else
		{
			mMetadataView.setBackgroundResource(R.color.grey);
		}

		int length = mComposeView.getText().length();
		// set the max sms length to a length appropriate to the number of characters we have
		mMaxSmsLength = 160 * (1 + length / 160);
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
			// we were called on config changed, just ignore
			return;
		}

		/* don't send typing notifications for non-hike chats */
		if ((mConversation == null) || (!mConversation.isOnhike()))
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
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, mConversation.serialize("st"));

			// create a timer to clear the event
			mUiThreadHandler.removeCallbacks(mResetTypingNotification); // clear
																		// any
																		// existing
																		// ones
			mUiThreadHandler.postDelayed(mResetTypingNotification, 10 * 1000);
		}
	}
}
