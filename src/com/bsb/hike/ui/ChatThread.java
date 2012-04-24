package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.NetworkManager;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeSearchContactAdapter;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.adapters.UpdateAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Utils;

public class ChatThread extends Activity implements HikePubSub.Listener, TextWatcher, OnEditorActionListener, OnItemClickListener
{
	private HikePubSub mPubSub;

	private HikeConversationsDatabase mConversationDb;

	private HikeUserDatabase mDbhelper;

	private String mContactId;

	private String mContactName;

	private String mContactNumber;

	private MessagesAdapter mAdapter;

	private Conversation mConversation;

	private SetTypingText mClearTypingCallback;

	private ComposeViewWatcher mComposeViewWatcher;

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

	private EditText mInputNumberView;
	
	private ListView mContactSearchView;

	int mMaxSmsLength = 140;

	private String mLabel;

	/* notifies that the adapter has been updated */
	private Runnable mUpdateAdapter;

	private TextView mLabelView;

	private LinearLayout mInputNumberContainer;

	private boolean mUserIsBlocked;

	private boolean mPaused = false; /* is the activity currently paused */

	private View mBlockedUserOverlay;

	@Override
	protected void onPause()
	{
		super.onPause();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
		mPaused = true;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mPaused = false;
		/* mark any messages unread as read. */
		setMessagesRead();
		/* clear any pending notifications */
		/* clear any toast notifications */
		if (mConversation != null)
		{
			NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mgr.cancel((int) mConversation.getConvId());				
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.init();

			/* check if the send button should be enabled */
			mComposeViewWatcher.setBtnEnabled();
		}
	}

	/* msg is any text we want to show initially */
	private void createAutoCompleteView(String msg)
	{
		mMetadataView.setVisibility(View.GONE);
		mComposeView.removeTextChangedListener(this);

		mLabelView.setText("New Message");

		/* if we've got some pre-filled text, add it here */
		if (TextUtils.isEmpty(msg)) {
			mBottomView.setVisibility(View.GONE);
		} else {
			mComposeView.setText(msg);
			/* make sure that the autoselect text is empty */
			mInputNumberView.setText("");
			/* disable the send button */
			mSendBtn.setEnabled(false);
		}

		/* if we've got some pre-filled text, add it here */
		if (TextUtils.isEmpty(msg)) {
			mBottomView.setVisibility(View.GONE);
		} else {
			mComposeView.setText(msg);
		}

		HikeSearchContactAdapter adapter = new HikeSearchContactAdapter(this);
		mContactSearchView.setAdapter(adapter);
		mInputNumberView.addTextChangedListener(adapter);

		mInputNumberContainer.setVisibility(View.VISIBLE);
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
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_DELIVERED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.MESSAGE_FAILED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.ICON_CHANGED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.USER_JOINED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.USER_LEFT, this);
		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
			mComposeViewWatcher = null;
		}

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
		return getIntent();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		/* add a handler on the UI thread so we can post delayed messages */
		mUiThreadHandler = new Handler();

		/* force the user into the reg-flow process if the token isn't set */
		if (Utils.requireAuth(this))
		{
			return;
		}

		// TODO this is being called everytime this activity is created. Way too often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		setContentView(R.layout.chatthread);
		/* bind views to variables */
		mBottomView = findViewById(R.id.bottom_panel);
		mMetadataView = findViewById(R.id.sms_chat_metadata);
		mInputNumberView = (EditText) findViewById(R.id.input_number);
		mContactSearchView = (ListView) findViewById(R.id.contact_search_result);
		mInputNumberContainer = (LinearLayout) findViewById(R.id.input_number_container);
		mConversationsView = (ListView) findViewById(R.id.conversations_list);
		mComposeView = (EditText) findViewById(R.id.msg_compose);
		mSendBtn = (Button) findViewById(R.id.send_message);
		mMetadataView = findViewById(R.id.sms_chat_metadata);
		mMetadataNumChars = (TextView) findViewById(R.id.sms_chat_metadata_num_chars);
		mMetadataCreditsLeft = (TextView) findViewById(R.id.sms_chat_metadata_text_credits_left);
		mLabelView = (TextView) findViewById(R.id.title);
		mBlockedUserOverlay = findViewById(R.id.block_overlay);

		/* register for long-press's */
		registerForContextMenu(mConversationsView);

		/* ensure that when the softkeyboard Done button is pressed (different than the sen
		 * button we have), we send the message.
		 */
		mComposeView.setOnEditorActionListener(this);

		mConversationDb = new HikeConversationsDatabase(this);

		mPubSub = HikeMessengerApp.getPubSub();
		Object o = getLastNonConfigurationInstance();
		Intent intent = (o instanceof Intent) ? (Intent) o : getIntent();
		onNewIntent(intent);

		mContactSearchView.setOnItemClickListener(this);
		/* register listeners */
		HikeMessengerApp.getPubSub().addListener(HikePubSub.TYPING_CONVERSATION, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.END_TYPING_CONVERSATION, this);

		HikeMessengerApp.getPubSub().addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED_READ, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_FAILED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED, this);

		HikeMessengerApp.getPubSub().addListener(HikePubSub.ICON_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.USER_JOINED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.USER_LEFT, this);
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
			intent.putExtra(HikeConstants.Extras.MSG, message.getMessage());
			startActivity(intent);
			return true;
		case R.id.delete:
			mPubSub.publish(HikePubSub.MESSAGE_DELETED, message.getMsgID());
			mAdapter.remove(message);
			return true;
		case R.id.resend:
			/* we treat resend as delete the failed message, and paste the text in the compose buffer */
			String m = message.getMessage();
			mComposeView.setText(m);
			mPubSub.publish(HikePubSub.MESSAGE_DELETED, message.getMsgID());
			mAdapter.remove(message);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	/* this function is called right before the options menu
	 * is shown.  Disable fields here as appropriate
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		/* disable invite if this is a hike user */
		if (mConversation.isOnhike())
		{
			MenuItem item = menu.findItem(R.id.invite_menu);
			item.setVisible(false);
		}

		/* don't show a menu item for unblock (since the overlay will be present */
		MenuItem item = menu.findItem(R.id.block_menu);
		item.setVisible(!mUserIsBlocked);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		/* only enable the options menu
		 * after we've selected a conversation */
		if (mConversation == null)
		{
			return false;
		}

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.chatthread_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mConversation == null)
		{
			Log.w("ChatThread", "OptionItem menu selected when conversation was null");
			return false;
		}

		if (item.getItemId() == R.id.invite_menu)
		{
			inviteUser();
		}
		else if (item.getItemId() == R.id.block_menu)
		{
			mPubSub.publish(HikePubSub.BLOCK_USER, mContactNumber);
			mUserIsBlocked = true;
			initializeBlockOverlay();
		}

		return true;
	}

	public void onUnblockClick(View v)
	{
		/* user clicked the unblock button in the chat-screen */
		mBlockedUserOverlay.setVisibility(View.GONE);
		mPubSub.publish(HikePubSub.UNBLOCK_USER, mContactNumber);
		mUserIsBlocked = false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.message_menu, menu);

		/* enable resend options on failed messages */
		AdapterView.AdapterContextMenuInfo adapterInfo =
	            (AdapterView.AdapterContextMenuInfo) menuInfo;
		ConvMessage message = mAdapter.getItem(adapterInfo.position);
		if ((message.getState() == ConvMessage.State.SENT_FAILED))
		{
			MenuItem item = menu.findItem(R.id.resend);
			item.setVisible(true);
		}
	}

	private void sendMessage(ConvMessage convMessage)
	{
		mAdapter.add(convMessage);

		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
		mSendBtn.setEnabled(false);
	}

	public void onSendClick(View v)
	{
		if (!mConversation.isOnhike() &&
			mCredits <= 0)
		{
			return;
		}

		String message = mComposeView.getText().toString();
		mComposeView.setText("");

		long time = (long) System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(message, mContactNumber, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setConversation(mConversation);
		sendMessage(convMessage);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.onMessageSent();
		}
	}

	/*
	 * this function is called externally when our Activity is on top and the user selects an Intent for this same Activity
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent)
	{
		String prevContactNumber = null;
		/* prevent any callbacks from previous instances of this activity from being fired now */
		if (mClearTypingCallback != null)
		{
			mUiThreadHandler.removeCallbacks(mClearTypingCallback);
		}

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
			mComposeViewWatcher = null;
		}

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
				mContactId = contactInfo.getId();
				mContactName = contactInfo.getName();
				prevContactNumber = mContactNumber;
				mContactNumber = contactInfo.getMsisdn();
				setIntentFromField();
			}
			else
			{
				mContactId = null;
				prevContactNumber = mContactNumber;
				mContactName = mContactNumber = phoneNumber;
			}

			createConversation();
		}
		else if (intent.hasExtra(HikeConstants.Extras.MSISDN))
		{
			
			prevContactNumber = mContactNumber;
			// selected chat from conversation list
			mContactNumber = intent.getStringExtra(HikeConstants.Extras.MSISDN);
			mContactId = intent.getStringExtra(HikeConstants.Extras.ID);
			mContactName = intent.getStringExtra(HikeConstants.Extras.NAME);

			createConversation();
			if (intent.getBooleanExtra(HikeConstants.Extras.INVITE, false))
			{
				intent.removeExtra(HikeConstants.Extras.INVITE);
				inviteUser();
			}

			mComposeView.setText("");
		}
		else
		{
			createAutoCompleteView(intent.getStringExtra(HikeConstants.Extras.MSG));
		}
		/* close context menu(if open) if the previous MSISDN is different from the current one)*/
		if (prevContactNumber != null && !prevContactNumber.equalsIgnoreCase(mContactNumber)) {
				Log.w("ChatThread",
						"DIFFERENT MSISDN CLOSING CONTEXT MENU!!");
				closeContextMenu();
		}
	}

	private void inviteUser()
	{
		if (mConversation == null)
		{
			return;
		}

		if (!mConversation.isOnhike())
		{
			long time = (long) System.currentTimeMillis() / 1000;
			ConvMessage convMessage = new ConvMessage(getResources().getString(R.string.invite_message),
												mContactNumber, time, ConvMessage.State.SENT_UNCONFIRMED);
			convMessage.setInvite(true);
			convMessage.setConversation(mConversation);
			sendMessage(convMessage);
		}
		else
		{
			Toast toast = Toast.makeText(this, R.string.already_hike_user, Toast.LENGTH_LONG);
			toast.show();
		}
	}

	/* sets the intent for this screen based on the fields we've assigned.
	 * useful if the user has entered information or we've determined information
	 * that indicates the type of data on this screen.
	 */
	private void setIntentFromField()
	{
		Intent intent = new Intent();
		if (mContactName != null)
		{
			intent.putExtra(HikeConstants.Extras.NAME, mContactName);
		}
		if (mContactId != null)
		{
			intent.putExtra(HikeConstants.Extras.ID, mContactId);
		}

		if (!TextUtils.isEmpty(mContactNumber))
		{
			intent.putExtra(HikeConstants.Extras.MSISDN, mContactNumber);
		}

		setIntent(intent);
	}

	/**
	 * Renders the chats for a given user
	 */
	private void createConversation()
	{
		mComposeView.setFocusable(true);
		mComposeView.requestFocus();
		/* hide the number picker */
		mInputNumberContainer.setVisibility(View.GONE);

		/*
		 * strictly speaking we shouldn't be reading from the db in the UI Thread
		 */
		mConversation = mConversationDb.getConversation(mContactNumber, 1000);
		if (mConversation == null)
		{
			mConversation = mConversationDb.addConversation(mContactNumber, false);
		}

		mLabel = mConversation.getLabel();

		mBottomView.setVisibility(View.VISIBLE);

		mLabelView.setText(mLabel);

		mConversationsView.setStackFromBottom(true);

		HikeUserDatabase db = new HikeUserDatabase(this);
		mUserIsBlocked = db.isBlocked(mContactNumber);
		if (mUserIsBlocked)
		{
			initializeBlockOverlay();
		}

		db.close();

		/* make a copy of the message list since it's used internally by the adapter */
		List<ConvMessage> messages = new ArrayList<ConvMessage>(mConversation.getMessages());

		mAdapter = new MessagesAdapter(this, messages, mConversation);
		mConversationsView.setAdapter(mAdapter);

		/* add a text changed listener */
		mComposeView.addTextChangedListener(this);

		/* get the number of credits and also listen for changes */
		mCredits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);
		mPubSub.addListener(HikePubSub.SMS_CREDIT_CHANGED, this);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
		}

		updateUIForHikeStatus();

		mComposeViewWatcher = new ComposeViewWatcher(mConversation, mComposeView, mSendBtn, mCredits);

		/* create an object that we can notify when the contents of the thread are updated */
		mUpdateAdapter = new UpdateAdapter(mAdapter);

		/* clear any toast notifications */
		NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mgr.cancel((int) mConversation.getConvId());
	}

	private void initializeBlockOverlay()
	{
		mBlockedUserOverlay.setVisibility(View.VISIBLE);
		TextView message = (TextView) mBlockedUserOverlay.findViewById(R.id.block_overlay_message);

		/* bold the blocked users name */
		String label = mConversation.getLabel();
		String formatString = getResources().getString(R.string.block_overlay_message);
		String formatted = String.format(formatString, mConversation.getLabel());
		SpannableString str = new SpannableString(formatted);
		int start = formatString.indexOf("%1$s");
		str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
						start, start + label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		message.setText(str);
	}

	/*
	 * Update the UI to show SMS Credits/etc if the conversation is on hike
	 */
	private void updateUIForHikeStatus()
	{
		if (mConversation.isOnhike())
		{
			mMetadataView.setVisibility(View.GONE);
			mSendBtn.setBackgroundResource(R.drawable.sendbutton);
			mComposeView.setHint("Free Message...");
		}
		else
		{
			mMetadataView.setVisibility(View.VISIBLE);
			updateChatMetadata();
			mSendBtn.setBackgroundResource(R.drawable.sendbutton_sms);
			mComposeView.setHint("SMS Message...");
		}
	}

	/* returns TRUE iff the last message was received and unread */
	private boolean isLastMsgReceivedAndUnread()
	{
		List<ConvMessage> msgList = (mConversation != null) ? mConversation.getMessages() : null;

		if ((msgList == null) || (msgList.isEmpty()))
		{
			return false;
		}

		ConvMessage lastMsg = msgList.get(msgList.size() - 1);

		return lastMsg.getState() == ConvMessage.State.RECEIVED_UNREAD;
	}

	/*
	 * marks messages read
	 */
	private void setMessagesRead()
	{
		if (isLastMsgReceivedAndUnread())
		{
			long convID = mConversation.getConvId();
			JSONArray ids = mConversationDb.updateStatusAndSendDeliveryReport(convID);
			/* If there are msgs which are RECEIVED UNREAD then only broadcast a msg that these are read. */
			if (ids != null)
			{
				JSONObject object = new JSONObject();
				try
				{
					object.put(HikeConstants.TYPE, NetworkManager.MESSAGE_READ);
					object.put(HikeConstants.TO, mConversation.getMsisdn());
					object.put(HikeConstants.DATA, ids);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());
				mPubSub.publish(HikePubSub.MQTT_PUBLISH, object);
			}
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
				mLabelView.setText(mLabel + " is typing");
			}
			else
			{
				mLabelView.setText(mLabel);
			}
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (mContactNumber == null)
		{
			Log.d("ChatThread", "received message when contactNumber is null type=" + type + " object=" + object);
			return;
		}

		if (HikePubSub.MESSAGE_RECEIVED.equals(type))
		{
			final ConvMessage message = (ConvMessage) object;
			String msisdn = message.getMsisdn();
			if (msisdn == null)
			{
				Log.wtf("ChatThread", "Message with missing msisdn:" + message.toString());
			}
			if (msisdn.equals(mContactNumber))
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

				if (!mPaused)
				{
					mConversationDb.updateMsgStatus(message.getMsgID(), ConvMessage.State.RECEIVED_READ.ordinal());
					mPubSub.publish(HikePubSub.MQTT_PUBLISH, message.serializeDeliveryReportRead()); // handle return to sender
				}
				mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());
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
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))
		{
			long msgID = (Long) object;
			// TODO we could keep a map of msgId -> conversation objects somewhere to make this faster
			ConvMessage msg = findMessageById(msgID);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_DELIVERED);
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
		else if (HikePubSub.MESSAGE_FAILED.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_FAILED);
				runOnUiThread(mUpdateAdapter);
			}
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				runOnUiThread(mUpdateAdapter);
			}
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			String msisdn = (String) object;
			if (msisdn.equals(mContactNumber))
			{
				/* update the image drawable */
				runOnUiThread(mUpdateAdapter);
			}
		}
		else if ((HikePubSub.USER_LEFT.equals(type)) || (HikePubSub.USER_JOINED.equals(type)))
		{
			mConversation.setOnhike(HikePubSub.USER_JOINED.equals(type));
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					updateUIForHikeStatus();
					mUpdateAdapter.run();
				}
			});
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

	@Override
	public void afterTextChanged(Editable editable)
	{
		if (mConversation == null)
		{
			return;
		}

		/* only update the chat metadata if this is an SMS chat */
		if (!mConversation.isOnhike())
		{
			updateChatMetadata();
		}
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
			mMetadataView.setBackgroundResource(R.color.compose_background);
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
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent keyEvent)
	{
		if ((view == mComposeView) &&
			(actionId == EditorInfo.IME_ACTION_SEND))
		{
			boolean ret = mSendBtn.performClick();
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mComposeView.getWindowToken(), 0);
			return ret;
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) 
	{
		ContactInfo contactInfo = (ContactInfo) view.getTag();
		Intent intent = Utils.createIntentFromContactInfo(contactInfo);
		intent.setClass(this, ChatThread.class);
		startActivity(intent);
	}
}
