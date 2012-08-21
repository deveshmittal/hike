package com.bsb.hike.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.adapters.HikeSearchContactAdapter;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.adapters.UpdateAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeFileTransferHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.tasks.DownloadFileTask;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;

public class ChatThread extends Activity implements HikePubSub.Listener, TextWatcher, OnEditorActionListener, OnSoftKeyboardListener, View.OnKeyListener, FinishableEvent, OnItemClickListener
{
	private HikePubSub mPubSub;

	private HikeConversationsDatabase mConversationDb;

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

	private TextView mMetadataNumChars;

	private View mBottomView;

	private EditText mComposeView;

	private ListView mConversationsView;

	int mMaxSmsLength = 140;

	private String mLabel;

	/* notifies that the adapter has been updated */
	public Runnable mUpdateAdapter;

	private TextView mLabelView;

	private LinearLayout mInputNumberContainer;

	private boolean mUserIsBlocked;
	
	private View mOverlayLayout;

	private ArrayList<ConvMessage> messages;

	private boolean shouldScrollToBottom = false;

	private CustomLinearLayout chatLayout;

	private Handler mHandler;

	private boolean blockOverlay;

	private Configuration config;

	private SharedPreferences prefs;

	private Animation slideUp;

	private Animation slideDown;

	private TextView smsCount;

	private boolean animatedOnce = false;

	private ViewGroup toolTipLayout;

	private boolean isToolTipShowing = false;

	private boolean isOverlayShowing = false;

	private ViewPager emoticonViewPager;

	private EmoticonAdapter emoticonAdapter;

	private ViewGroup emoticonLayout;

	private ImageView titleIconView;

	private Button titleBtn;

	private TabHost tabHost;

	private boolean isTabInitialised = false;
	
	private EditText mInputNumberView;
	
	private ListView mContactSearchView;

	private GroupParticipant myInfo;

	private File selectedFile;

	private Dialog filePickerDialog;

	public static Map<Long, AsyncTask<?, ?, ?>> fileTransferTaskMap;

	@Override
	protected void onPause()
	{
		super.onPause();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		setMessagesRead();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		/* mark any messages unread as read */
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
		boolean isGroupChat = getIntent().getBooleanExtra(HikeConstants.Extras.GROUP_CHAT, false); 
		// Getting the group id. This will be a valid value if the intent was passed to add group participants.
		String existingGroupId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);

		mComposeView.removeTextChangedListener(this);

		mLabelView.setText(!TextUtils.isEmpty(existingGroupId) ? R.string.add_group : (isGroupChat ? R.string.new_group : R.string.new_message));
		
		/* if we've got some pre-filled text, add it here */
		if (!TextUtils.isEmpty(msg)) {
			mComposeView.setText(msg);
			/* disable the send button */
			mSendBtn.setEnabled(false);
			//Doing a toggle instead of a show since the show method was not working here
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
		}
		mBottomView.setVisibility(View.GONE);

		if(isGroupChat)
		{
			titleBtn = (Button) findViewById(R.id.title_icon);
			titleBtn.setText(R.string.done);
			titleBtn.setEnabled(false);
			titleBtn.setVisibility(View.VISIBLE);
			findViewById(R.id.button_bar_2).setVisibility(View.VISIBLE);
		}

		mInputNumberView.setText("");
		HikeSearchContactAdapter adapter = new HikeSearchContactAdapter(
				this, HikeUserDatabase.getInstance().getContactsOrderedByOnHike(), mInputNumberView, isGroupChat, !TextUtils.isEmpty(msg), titleBtn, existingGroupId);
		mContactSearchView.setAdapter(adapter);
		mContactSearchView.setOnItemClickListener(adapter);
		mInputNumberView.addTextChangedListener(adapter);
		mInputNumberView.setSingleLine(!isGroupChat);

		mInputNumberContainer.setVisibility(View.VISIBLE);
		mInputNumberView.setVisibility(View.VISIBLE);
		mContactSearchView.setVisibility(View.VISIBLE);
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
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.GROUP_NAME_CHANGED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.GROUP_END, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.CONTACT_ADDED, this);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
			mComposeViewWatcher = null;
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

		if(ChatThread.fileTransferTaskMap != null)
		{
			for(Entry<Long, AsyncTask<?, ?, ?>> fileTransferTaskEntry : fileTransferTaskMap.entrySet())
			{
				AsyncTask<?, ?, ?> fileTransferTask = fileTransferTaskEntry.getValue();
				if(fileTransferTask instanceof HikeHTTPTask)
				{
					((HikeHTTPTask) fileTransferTask).setChatThread(ChatThread.this);
				}
				else
				{
					((DownloadFileTask) fileTransferTask).setChatThread(ChatThread.this);
				}
			}
		}
		else
		{
			ChatThread.fileTransferTaskMap = new HashMap<Long, AsyncTask<?,?,?>>();
		}

		prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		Utils.setDensityMultiplier(ChatThread.this);

		isToolTipShowing = savedInstanceState == null ? false : savedInstanceState.getBoolean(HikeConstants.Extras.TOOLTIP_SHOWING);
		isOverlayShowing  = savedInstanceState == null ? false : savedInstanceState.getBoolean(HikeConstants.Extras.OVERLAY_SHOWING);

		config = getResources().getConfiguration();
		
		/* bind views to variables */
		chatLayout = (CustomLinearLayout) findViewById(R.id.chat_layout);
		mBottomView = findViewById(R.id.bottom_panel);
		mInputNumberContainer = (LinearLayout) findViewById(R.id.input_number_container);
		mConversationsView = (ListView) findViewById(R.id.conversations_list);
		mComposeView = (EditText) findViewById(R.id.msg_compose);
		mSendBtn = (Button) findViewById(R.id.send_message);
		mMetadataNumChars = (TextView) findViewById(R.id.sms_chat_metadata_num_chars);
		mLabelView = (TextView) findViewById(R.id.title);
		mOverlayLayout = findViewById(R.id.overlay_layout);
		mInputNumberView = (EditText) findViewById(R.id.input_number);
		mContactSearchView = (ListView) findViewById(R.id.contact_search_result);

		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();

		/* register for long-press's */
		registerForContextMenu(mConversationsView);

		/* ensure that when the softkeyboard Done button is pressed (different than the sen
		 * button we have), we send the message.
		 */
		mComposeView.setOnEditorActionListener(this);

		/* ensure that when we hit Alt+Enter, we insert a newline */
		mComposeView.setOnKeyListener(this);

		mConversationDb = HikeConversationsDatabase.getInstance();

		chatLayout.setOnSoftKeyboardListener(this);
		mPubSub = HikeMessengerApp.getPubSub();
		Object o = getLastNonConfigurationInstance();
		Intent intent = (o instanceof Intent) ? (Intent) o : getIntent();
		onNewIntent(intent);

		if(savedInstanceState == null ? false : savedInstanceState.getBoolean(HikeConstants.Extras.EMOTICON_SHOWING))
		{
			onEmoticonBtnClicked(null);
		}
		if(savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.FILE_TRANSFER_DIALOG_SHOWING))
		{
			showFilePickerDialog(Utils.getExternalStorageState());
		}

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
		HikeMessengerApp.getPubSub().addListener(HikePubSub.CONTACT_ADDED, this);

		HikeMessengerApp.getPubSub().addListener(HikePubSub.GROUP_NAME_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.GROUP_END, this);
	}

	@Override
	public void onBackPressed()
	{
		if (!getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT) && this.mConversation != null) 
		{
			if ((mConversation instanceof GroupConversation)) {
				Utils.incrementNumTimesScreenOpen(prefs,
						HikeMessengerApp.NUM_TIMES_CHAT_THREAD_GROUP);
			} else if (!this.mConversation.isOnhike()) {
				Utils.incrementNumTimesScreenOpen(prefs,
						HikeMessengerApp.NUM_TIMES_CHAT_THREAD_INVITE);
			}
		}
		if (emoticonLayout == null || emoticonLayout.getVisibility() != View.VISIBLE) 
		{
			Intent intent = null;
			if (!getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT)) 
			{
				intent = new Intent(this, MessagesList.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
			
			/* slide down if we're still selecting a user, otherwise slide back */
			if (mConversation == null) {
				overridePendingTransition(R.anim.no_animation,
						R.anim.slide_down_noalpha);
			} else {
				overridePendingTransition(R.anim.slide_in_left_noalpha,
						R.anim.slide_out_right_noalpha);
			}
			finish();
		}
		else
		{
			onEmoticonBtnClicked(null);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		ConvMessage message = mAdapter.getItem((int) info.id);
		if (message.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			return false;
		}

		switch (item.getItemId())
		{
		case R.id.copy:
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(message.getMessage());
			return true;
		case R.id.forward:
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.FORWARD_MSG);
			Intent intent = new Intent(this, ChatThread.class);
			intent.putExtra(HikeConstants.Extras.MSG, message.getMessage());
			startActivity(intent);
			return true;
		case R.id.delete:
			mPubSub.publish(HikePubSub.MESSAGE_DELETED, message.getMsgID());
			removeMessage(message);
			return true;
		case R.id.resend:
			/* we treat resend as delete the failed message, and paste the text in the compose buffer */
			String m = message.getMessage();
			mComposeView.setText(m);
			mPubSub.publish(HikePubSub.MESSAGE_DELETED, message.getMsgID());
			removeMessage(message);
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

		/*if the number ends with Hike, disable blocking, add to contacts and call */
		if ((mContactNumber != null) &&
		    (mContactNumber.endsWith("HIKE")))
		{
		    return false;
		}

		boolean amIGroupOwner = false;
		if(mConversation instanceof GroupConversation)
		{
			amIGroupOwner = ((GroupConversation)mConversation).getGroupOwner().equals(myInfo.getContactInfo().getMsisdn());
		}
		/* don't show a menu item for unblock (since the overlay will be present */
		MenuItem item = menu.findItem(R.id.block_menu);
		item.setTitle(mConversation instanceof GroupConversation ? R.string.block_owner : R.string.block_title);
		item.setVisible(!mUserIsBlocked && !amIGroupOwner);

		MenuItem item2 = menu.findItem(R.id.add_contact_menu);
		item2.setVisible(mConversation!= null && 
				TextUtils.isEmpty(mConversation.getContactName()) && 
				!(mConversation instanceof GroupConversation) && 
				!mUserIsBlocked);

		MenuItem item3 = menu.findItem(R.id.leave_menu);
		item3.setVisible((mConversation != null) && (mConversation instanceof GroupConversation) && !mUserIsBlocked);

		MenuItem item4 = menu.findItem(R.id.call);
		item4.setVisible(!mUserIsBlocked && !(mConversation instanceof GroupConversation));

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

		if (item.getItemId() == R.id.block_menu)
		{
				Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.MENU_BLOCK);
				mPubSub.publish(HikePubSub.BLOCK_USER, getMsisdnMainUser());
				mUserIsBlocked = true;
				showOverlay(true);
		}
		else if(item.getItemId() == R.id.add_contact_menu)
		{
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.MENU_ADD_TO_CONTACTS);
			Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
			i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
			i.putExtra(Insert.PHONE, mConversation.getMsisdn());
			startActivity(i);
		}
		else if(item.getItemId() == R.id.leave_menu)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.GROUP_LEFT, mConversation.getMsisdn());
			/*
			 * Fix for when the user opens the app from a notification of the group and leaves the group,
			 * the user would not leave the group.
			 */
			Intent intent = new Intent(this, MessagesList.class);
			intent.putExtra(HikeConstants.Extras.GROUP_LEFT, mConversation.getMsisdn());
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			overridePendingTransition(R.anim.slide_in_left_noalpha,
					R.anim.slide_out_right_noalpha);
			
		}
		else if(item.getItemId() == R.id.call)
		{
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.MENU_CALL);
			Intent callIntent = new Intent(Intent.ACTION_CALL);
	        callIntent.setData(Uri.parse("tel:"+mContactNumber));
	        startActivity(callIntent);
		}

		return true;
	}

	public void onOverlayButtonClick(View v)
	{
		/* user clicked the unblock button in the chat-screen */
		if (v.getId() != R.id.overlay_layout && blockOverlay) 
		{
			mPubSub.publish(HikePubSub.UNBLOCK_USER, getMsisdnMainUser());
			mUserIsBlocked = false;
			mComposeView.setEnabled(true);
			hideOverlay();
		}
		else if(v.getId() != R.id.overlay_layout)
		{
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.INVITE_OVERLAY_BUTTON);
			inviteUser();
			hideOverlay();
		}

		if(!blockOverlay)
		{
			hideOverlay();
			mConversationDb.setOverlay(true, mConversation.getMsisdn());
		}
	}

	private void hideOverlay()
	{
		if (mOverlayLayout.getVisibility() == View.VISIBLE) 
		{
			Animation fadeOut = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_out);
			mOverlayLayout.setAnimation(fadeOut);
			mOverlayLayout.setVisibility(View.INVISIBLE);
			isOverlayShowing = false;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);

		/* enable resend options on failed messages */
		AdapterView.AdapterContextMenuInfo adapterInfo =
	            (AdapterView.AdapterContextMenuInfo) menuInfo;
		ConvMessage message = mAdapter.getItem(adapterInfo.position);
		if (message == null || message.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
		{
			return;
		}

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.message_menu, menu);
		if ((message.getState() == ConvMessage.State.SENT_FAILED))
		{
			MenuItem item = menu.findItem(R.id.resend);
			item.setVisible(true);
		}
	}

	private void sendMessage(ConvMessage convMessage)
	{
		addMessage(convMessage);

		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
		mSendBtn.setEnabled(!TextUtils.isEmpty(mComposeView.getText()));
	}

	public void onSendClick(View v)
	{
		if ((!mConversation.isOnhike() &&
			mCredits <= 0) ||
			(TextUtils.isEmpty(mComposeView.getText())))
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
		if (emoticonLayout != null && emoticonLayout.getVisibility() == View.VISIBLE)
		{
			onEmoticonBtnClicked(null);
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
		Log.d(getClass().getSimpleName(), "Intent: " + intent.toString());
		titleIconView = (ImageView) findViewById(R.id.title_image_btn);
		View btnBar = findViewById(R.id.button_bar);
		titleIconView.setVisibility(View.GONE);
		btnBar.setVisibility(View.GONE);

		shouldScrollToBottom = true;
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
			messages.clear();
			mAdapter.notifyDataSetChanged();
		}

		mConversation = null;

		if ((dataURI != null) && ("smsto".equals(dataURI.getScheme()) || "sms".equals(dataURI.getScheme())))
		{
			// Intent received externally
			String phoneNumber = dataURI.getSchemeSpecificPart();
			// We were getting msisdns with spaces in them. Replacing all spaces so that lookup is correct
			phoneNumber = phoneNumber.replaceAll(" ", "");
			Log.d(getClass().getSimpleName(), "SMS To: " + phoneNumber);
			ContactInfo contactInfo = HikeUserDatabase.getInstance().getContactInfoFromPhoneNo(phoneNumber);
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
		else if (intent.hasExtra(HikeConstants.Extras.MSISDN) && !intent.hasExtra(HikeConstants.Extras.GROUP_CHAT))
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

			if (!intent.getBooleanExtra(HikeConstants.Extras.KEEP_MESSAGE, false))
			{
				mComposeView.setText("");
			}
		}
		else
		{
			/*
			 * Checking if intent was received from the 'tap to invite' box in the invite screen.
			 * If it was we fill in the text of that intent
			 */
			createAutoCompleteView(("text/plain".equals(intent.getType())) ? 
					intent.getStringExtra(Intent.EXTRA_TEXT) : intent.getStringExtra(HikeConstants.Extras.MSG));
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

			// Adding the user's invite token to the invite url
			String inviteMessage = getString(R.string.invite_message);
			String defaultInviteURL = getString(R.string.default_invite_url);
			String inviteToken = this.prefs.getString(HikeConstants.INVITE_TOKEN, "");
			inviteMessage = inviteMessage.replace(defaultInviteURL, defaultInviteURL + inviteToken);

			ConvMessage convMessage = new ConvMessage(inviteMessage, mContactNumber, time, ConvMessage.State.SENT_UNCONFIRMED);
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
		// This prevent the activity from simply finishing and opens up the last screen.
		getIntent().removeExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);

		findViewById(R.id.title_icon).setVisibility(View.GONE);
		findViewById(R.id.button_bar_2).setVisibility(View.GONE);
		findViewById(R.id.title_image_btn2).setVisibility(View.VISIBLE);
		findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);

		mComposeView.setFocusable(true);
		mComposeView.setVisibility(View.VISIBLE);
		mComposeView.requestFocus();
		/* hide the number picker */
		mInputNumberView.setVisibility(View.GONE);
		mContactSearchView.setVisibility(View.GONE);
		mInputNumberContainer.setVisibility(View.GONE);

		/*
		 * strictly speaking we shouldn't be reading from the db in the UI Thread
		 */
		mConversation = mConversationDb.getConversation(mContactNumber, 1000);
		if (mConversation == null)
		{
			if (Utils.isGroupConversation(mContactNumber))
			{
				/* the user must have deleted the chat.  */
				Toast toast = Toast.makeText(this, "Group chat no longer exists", Toast.LENGTH_LONG);
				toast.show();
				onBackPressed();
				return;
			}

			mConversation = mConversationDb.addConversation(mContactNumber, false, "", null);
		}

		mLabel = mConversation.getLabel();

		mLabelView.setText(mLabel);

		HikeUserDatabase db = HikeUserDatabase.getInstance();
		mUserIsBlocked = db.isBlocked(getMsisdnMainUser());
		if (mUserIsBlocked)
		{
			showOverlay(true);
		}

		changeInviteButtonVisibility();
		if((mConversation instanceof GroupConversation) && !((GroupConversation)mConversation).getIsGroupAlive())
		{
			groupChatDead();
		}
		/* make a copy of the message list since it's used internally by the adapter */
		messages = new ArrayList<ConvMessage>(mConversation.getMessages());

		mAdapter = new MessagesAdapter(this, messages, mConversation);
		mConversationsView.setAdapter(mAdapter);
		mConversationsView.setOnItemClickListener(this);

		if (messages.isEmpty() && mBottomView.getVisibility() != View.VISIBLE) 
		{
			Animation alphaIn = AnimationUtils.loadAnimation(
					getApplicationContext(), R.anim.slide_up_noalpha);
			alphaIn.setDuration(400);
			mBottomView.setAnimation(alphaIn);
			mBottomView.setVisibility(View.VISIBLE);
		}
		else
		{
			mBottomView.setVisibility(View.VISIBLE);
		}

		if(shouldScrollToBottom)
		{
			shouldScrollToBottom = false;
			mConversationsView.setSelection(messages.size()-1);
		}

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

		if (mConversation instanceof GroupConversation) 
		{
			myInfo = new GroupParticipant(Utils.getUserContactInfo(prefs));
		}
	}

	/*
	 * Update the UI to show SMS Credits/etc if the conversation is on hike
	 */
	private void updateUIForHikeStatus()
	{
		if (mConversation.isOnhike() ||
				(mConversation instanceof GroupConversation))
		{
			((ImageButton)findViewById(R.id.emo_btn)).setImageResource(R.drawable.emoticon_hike_btn);
			mSendBtn.setTextColor(getResources().getColorStateList(R.color.send_hike));
			mSendBtn.setBackgroundResource(R.drawable.send_hike_btn);
			mComposeView.setHint("Free Message...");
		}
		else
		{
			updateChatMetadata();
			((ImageButton)findViewById(R.id.emo_btn)).setImageResource(R.drawable.emoticon_sms_btn);
			mSendBtn.setTextColor(getResources().getColorStateList(R.color.send_sms));
			mSendBtn.setBackgroundResource(R.drawable.send_sms_btn);
			mComposeView.setHint("SMS Message...");
		}
	}

	/* returns TRUE iff the last message was received and unread */
	private boolean isLastMsgReceivedAndUnread()
	{
		int count = (mAdapter != null) ? mAdapter.getCount() : 0;
		ConvMessage lastMsg = count > 0 ? mAdapter.getItem(count - 1) : null;
		if (lastMsg == null)
		{
			return false;
		}

		return lastMsg.getState() == ConvMessage.State.RECEIVED_UNREAD;
	}

	/*
	 * marks messages read
	 */
	private void setMessagesRead()
	{
		if (!hasWindowFocus())
		{
			return;
		}

		if (isLastMsgReceivedAndUnread())
		{
			long convID = mConversation.getConvId();
			JSONArray ids = mConversationDb.updateStatusAndSendDeliveryReport(convID);
			/* If there are msgs which are RECEIVED UNREAD then only broadcast a msg that these are read
			 * avoid sending read notifications for group chats */
			if (ids != null)
			{
				mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());

				JSONObject object = new JSONObject();
				try
				{
					object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE_READ);
					object.put(HikeConstants.TO, mConversation.getMsisdn());
					object.put(HikeConstants.DATA, ids);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

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
				if(messages.isEmpty() || messages.get(messages.size() - 1) != null)
				{
					addMessage(null);
				}
			}
			else
			{
				if(!messages.isEmpty() && messages.get(messages.size() - 1) == null)
				{
					messages.remove(messages.size() -1 );
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (mContactNumber == null)
		{
			Log.w("ChatThread", "received message when contactNumber is null type=" + type + " object=" + object);
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
				/*
				 * we publish the message before the conversation is created, so it's safer to just tack it on here
				 */
				message.setConversation(mConversation);

				if (hasWindowFocus() && message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
				{
					message.setState(ConvMessage.State.RECEIVED_READ);
					mConversationDb.updateMsgStatus(message.getMsgID(), ConvMessage.State.RECEIVED_READ.ordinal());
					mPubSub.publish(HikePubSub.MQTT_PUBLISH, message.serializeDeliveryReportRead()); // handle return to sender

					mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());
				}

				if(message.getParticipantInfoState() != ParticipantInfoState.NO_INFO && mConversation instanceof GroupConversation)
				{
					HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
					((GroupConversation) mConversation).setGroupParticipantList(hCDB.getGroupParticipants(mConversation.getMsisdn(), false));
				}

				final String label = message.getParticipantInfoState() != ParticipantInfoState.NO_INFO ? mConversation.getLabel() : null;
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (label != null)
						{
							mLabelView.setText(label);
						}

						addMessage(message);
					}
				});

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
					if (!animatedOnce) 
					{
						animatedOnce = prefs.getBoolean(HikeConstants.Extras.ANIMATED_ONCE, false);
						if (!animatedOnce) {
							Editor editor = prefs.edit();
							editor.putBoolean(
									HikeConstants.Extras.ANIMATED_ONCE, true);
							editor.commit();
						}
					}

					if((mCredits % 5 == 0 || !animatedOnce)  && !mConversation.isOnhike())
					{
						animatedOnce = true;
						showSMSCounter();
					}
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
			if (mContactNumber.equals(msisdn))
			{
				/* update the image drawable */
				runOnUiThread(mUpdateAdapter);
			}
		}
		else if ((HikePubSub.USER_LEFT.equals(type)) || (HikePubSub.USER_JOINED.equals(type)))
		{
			/* only update the UI if the message is for this conversation */
			String msisdn = (String) object;
			if (!mContactNumber.equals(msisdn))
			{
				return;
			}

			mConversation.setOnhike(HikePubSub.USER_JOINED.equals(type));
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					changeInviteButtonVisibility();
					updateUIForHikeStatus();
					mUpdateAdapter.run();
				}
			});
		}
		else if (HikePubSub.GROUP_NAME_CHANGED.equals(type))
		{
			String groupId = (String) object;
			if (mContactNumber.equals(groupId))
			{
				HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
				final String groupName = db.getGroupName(groupId);
				mConversation.setContactName(groupName);

				runOnUiThread(new Runnable() {
					public void run()
					{
						mLabelView.setText(groupName);
					}
				});
			}
		}
		else if (HikePubSub.GROUP_END.equals(type))
		{
			String groupId = ((JSONObject)object).optString(HikeConstants.TO);
			if(mContactNumber.equals(groupId))
			{
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run() 
					{
						groupChatDead();
					}
				});
			}
		}
		else if (HikePubSub.CONTACT_ADDED.equals(type))
		{
			ContactInfo contactInfo = (ContactInfo) object;
			if (this.mContactNumber.equals(contactInfo.getMsisdn())) 
			{
				this.mContactName = contactInfo.getName();
				mConversation.setContactName(this.mContactName);
				this.mLabel = contactInfo.getName();
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run() 
					{
						mLabelView.setText(mLabel);
					}
				});
			}
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
			if (msg == null)
			{
				continue;
			}
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
		mMetadataNumChars.setVisibility(View.VISIBLE);
		if (mCredits <= 0)
		{
			mSendBtn.setEnabled(false);

			if (!TextUtils.isEmpty(mComposeView.getText())) {
				mComposeView.setText("");
			}
			mComposeView.setHint("0 Free SMS left...");
			mComposeView.setEnabled(false);
			findViewById(R.id.info_layout).setVisibility(View.VISIBLE);

			boolean show = mConversationDb.wasOverlayDismissed(mConversation.getMsisdn());
			if (!show) {
				showOverlay(false);
			}
		}
		else
		{
			if (!mComposeView.isEnabled()) {
				if (!TextUtils.isEmpty(mComposeView.getText())) {
					mComposeView.setText("");
				}
				mComposeView.setHint(R.string.type_to_compose);
				mComposeView.setEnabled(true);
			}
			findViewById(R.id.info_layout).setVisibility(View.GONE);

			if (!blockOverlay) {
				hideOverlay();
			}

			if(mComposeView.getLineCount()>2)
			{
				mMetadataNumChars.setVisibility(View.VISIBLE);
				int length = mComposeView.getText().length();
				// set the max sms length to a length appropriate to the number of characters we have
				int charNum = length % 140;
				int numSms = ((int)(length/140)) + 1;
				String charNumString = Integer.toString(charNum);
				SpannableString ss = new SpannableString(charNumString + "/#" + Integer.toString(numSms));
				ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.send_green)), 0, charNumString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				mMetadataNumChars.setText(ss);
			}
			else
			{
				mMetadataNumChars.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void showSMSCounter()
	{
		slideUp = AnimationUtils.loadAnimation(ChatThread.this,
					R.anim.slide_up_noalpha);
		slideUp.setDuration(2000);

			slideDown = AnimationUtils.loadAnimation(ChatThread.this,
					R.anim.slide_down_noalpha);
		slideDown.setDuration(2000);
		slideDown.setStartOffset(2000);
		
		if (smsCount == null) 
		{
			smsCount = (TextView) findViewById(R.id.sms_counter);
		}
		smsCount.setAnimation(slideUp);
		smsCount.setVisibility(View.VISIBLE);
		smsCount.setText(mCredits + " SMS left");

		slideUp.setAnimationListener(new AnimationListener() 
		{
			@Override
			public void onAnimationStart(Animation animation) {}
		
			@Override
			public void onAnimationRepeat(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) 
			{
				smsCount.setAnimation(slideDown);
				smsCount.setVisibility(View.INVISIBLE);
			}
		});
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int before, int count)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent keyEvent)
	{
		if (mConversation == null)
		{
			return false;
		}

		if ((view == mComposeView) &&
				(
					(actionId == EditorInfo.IME_ACTION_SEND) ||
					(
						(keyEvent != null) &&
						(keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) && 
						(keyEvent.getAction() != KeyEvent.ACTION_UP) && 
						(config.keyboard != Configuration.KEYBOARD_NOKEYS)
					)
				)
			)
		{
			boolean ret = mSendBtn.performClick();
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mComposeView.getWindowToken(), 0);
			return ret;
		}
		return false;
	}

	private void addMessage(ConvMessage convMessage)
	{
		if (messages != null && mAdapter != null) 
		{
			boolean wasShowingTypingItem = false;
			/*
			 *  If we were showing the typing bubble, we remove it from the add the new message
			 *  and add the typing bubble back again
			 */
			if(!messages.isEmpty() && messages.get(messages.size() - 1) == null)
			{
				messages.remove(messages.size() - 1);
				wasShowingTypingItem = true;
			}
			messages.add(convMessage);
			if(convMessage != null && convMessage.isSent() && wasShowingTypingItem)
			{
				messages.add(null);
			}
			mAdapter.notifyDataSetChanged();
			//Smooth scroll by the minimum distance in the opposite direction, to fix the bug where the list does not scroll at all.
			mConversationsView.smoothScrollBy(-1, 1);
			int itemsToScroll = messages.size()
					- (mConversationsView.getFirstVisiblePosition() + mConversationsView
							.getChildCount());
			if (itemsToScroll > 3) {
				mConversationsView.setSelection(messages.size() - 3);
			}
			mConversationsView.smoothScrollToPosition(messages.size() - 1);
		}
	}

	private void removeMessage(ConvMessage convMessage)
	{
		messages.remove(convMessage);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onShown() 
	{
		if (messages != null) 
		{
			if (mHandler == null) 
			{
				mHandler = new Handler();
			}
			mHandler.post(new Runnable() 
			{
				@Override
				public void run() 
				{
					mConversationsView.setSelection(messages.size() - 1);
				}
			});
		}
	}

	@Override
	public void onHidden() 
	{}

	public void onInviteButtonClick(View v)
	{
		inviteUser();
	}
	
	private void showOverlay(boolean blockOverlay)
	{
		this.blockOverlay = blockOverlay;

		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(this.mComposeView.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);

		if(mOverlayLayout.getVisibility() != View.VISIBLE && !isOverlayShowing)
		{
			Animation fadeIn = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_in);
			mOverlayLayout.setAnimation(fadeIn);
		}
		mOverlayLayout.setVisibility(View.VISIBLE);
		// To prevent the views in the background from being clickable
		mOverlayLayout.setOnClickListener(new OnClickListener() 
		{	
			@Override
			public void onClick(View v) 
			{}
		});

		TextView message = (TextView) mOverlayLayout.findViewById(R.id.overlay_message);
		Button overlayBtn = (Button) mOverlayLayout.findViewById(R.id.overlay_button);
		ImageView overlayImg = (ImageView) mOverlayLayout.findViewById(R.id.overlay_image);

		mComposeView.setEnabled(false);
		String label = mConversation instanceof GroupConversation ? 
				((GroupConversation)mConversation).getGroupParticipant(getMsisdnMainUser()).getContactInfo().getFirstName() : mLabel;
		String formatString;
		if (blockOverlay) 
		{
			overlayImg.setImageResource(R.drawable.ic_no);
			formatString = getResources().getString(R.string.block_overlay_message);
			overlayBtn.setText(R.string.unblock_title);
		}
		else
		{
			mConversationDb.setOverlay(false, mConversation.getMsisdn());
			formatString = getResources().getString(R.string.no_credits);
			overlayImg.setImageResource(R.drawable.ic_no_credits);
			overlayBtn.setText(R.string.invite_now);
			mOverlayLayout.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.INVITE_OVERLAY_DISMISS);
					onOverlayButtonClick(mOverlayLayout);
				}
			});
		}
		/* bold the blocked users name */
		String formatted = String.format(formatString,
				label);
		SpannableString str = new SpannableString(formatted);
		int start = formatString.indexOf("%1$s");
		str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start,
				start + label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		message.setText(str);
	}

	public void onTitleIconClick(View v)
	{
		
		if (v.getId() == R.id.title_image_btn) {
			dismissToolTip();
			if (!(mConversation instanceof GroupConversation)) 
			{
				Utils.logEvent(ChatThread.this,
						HikeConstants.LogEvent.CHAT_INVITE_TOP_BUTTON);
				inviteUser();
				
			}
			else
			{
				Utils.logEvent(ChatThread.this,
						HikeConstants.LogEvent.GROUP_INFO_TOP_BUTTON);
				Intent intent = getIntent();
				intent.setClass(ChatThread.this, ProfileActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
				intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, this.mConversation.getMsisdn());
				startActivity(intent);
				
				overridePendingTransition(R.anim.slide_in_right_noalpha,
						R.anim.slide_out_left_noalpha);
			}
		}
		else if (v.getId() == R.id.info_layout) {
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.I_BUTTON);
			showOverlay(false);
		}
		else if (v.getId() == R.id.title_icon) 
		{
			String groupId = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);
			if (TextUtils.isEmpty(groupId))
			{
				// Create new group
				String uid = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.UID_SETTING, "");
				mContactNumber = uid + ":" +System.currentTimeMillis();
			}
			else
			{
				// Group alredy exists. Fetch existing participants.
				mContactNumber = groupId;
			}
			String selectedContacts = this.mInputNumberView.getText().toString();
			selectedContacts = selectedContacts.substring(0, selectedContacts.lastIndexOf(HikeConstants.GROUP_PARTICIPANT_SEPARATOR));
			List<String> selectedParticipants = Utils.splitSelectedContacts(selectedContacts);
			List<String> selectedParticipantNames = Utils.splitSelectedContactsName(selectedContacts);
			Map<String, GroupParticipant> participantList = new HashMap<String, GroupParticipant>();

			for(int i = 0; i < selectedParticipants.size(); i++)
			{
				String msisdn = selectedParticipants.get(i);
				String name = selectedParticipantNames.get(i);
				GroupParticipant groupParticipant = new GroupParticipant(new ContactInfo(msisdn, msisdn, name, msisdn));
				participantList.put(msisdn, groupParticipant);
			}
			ContactInfo userContactInfo = Utils.getUserContactInfo(prefs);

			GroupConversation groupConversation= new GroupConversation(mContactNumber, 0, mContactNumber, null, userContactInfo.getMsisdn(), true);
			groupConversation.setGroupParticipantList(participantList);

			Log.d(getClass().getSimpleName(), "Creating group: " + mContactNumber);
			mConversationDb.addGroupParticipants(mContactNumber, groupConversation.getGroupParticipantList());
			mConversationDb.addConversation(groupConversation.getMsisdn(), false, "", groupConversation.getGroupOwner());

			try 
			{
				sendMessage(new ConvMessage(groupConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN), groupConversation, ChatThread.this, true));
			}
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, groupConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN));
			createConversation();
			mComposeViewWatcher.init();
			mComposeView.requestFocus();

			mContactName = groupConversation.getLabel();
			mContactId = groupConversation.getMsisdn();

			// To prevent the Contact picker layout from being shown on orientation change
			setIntentFromField();
		}
		else if(v.getId() == R.id.title_image_btn2)
		{
			showFilePickerDialog(Utils.getExternalStorageState());
		}
	}

	private void showFilePickerDialog(final ExternalStorageState externalStorageState)
	{
		if(externalStorageState == ExternalStorageState.NONE)
		{
			Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
			return;
		}
		final CharSequence[] options = getResources().getStringArray(R.array.file_transfer_items);

		AlertDialog.Builder builder = new AlertDialog.Builder(ChatThread.this);

		builder.setTitle(R.string.share_file);
		builder.setItems(options, new DialogInterface.OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				int requestCode;
				Intent pickIntent = new Intent();
				Intent newMediaFileIntent = null;
				switch (which) 
				{
				case 0:
					requestCode = HikeConstants.IMAGE_TRANSFER_CODE;
					pickIntent.setType("image/*");
					newMediaFileIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					selectedFile = Utils.getOutputMediaFile(HikeFileType.IMAGE, null, null);
					break;

				case 1:
					requestCode = HikeConstants.VIDEO_TRANSFER_CODE;
					pickIntent.setType("video/*");
					newMediaFileIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
					selectedFile = Utils.getOutputMediaFile(HikeFileType.VIDEO, null, null);
					break;
				
				case 2:
					requestCode = HikeConstants.AUDIO_TRANSFER_CODE;
					pickIntent.setData(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
					newMediaFileIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
					selectedFile = Utils.getOutputMediaFile(HikeFileType.AUDIO, null, null);
					break;
					
				default:
					requestCode = HikeConstants.IMAGE_TRANSFER_CODE;
					pickIntent.setType("image/*");
					newMediaFileIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					selectedFile = Utils.getOutputMediaFile(HikeFileType.IMAGE, null, null);
					break;
				}
				pickIntent.setAction(Intent.ACTION_PICK);

				Intent chooserIntent = Intent.createChooser(pickIntent, "");
				if(externalStorageState == ExternalStorageState.WRITEABLE)
				{
					if(which != 2)
					{
						newMediaFileIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFile));
					}
					chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { newMediaFileIntent });
				}
				startActivityForResult(chooserIntent, requestCode);
			}
		});

		filePickerDialog = builder.create();
		filePickerDialog.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if((requestCode == HikeConstants.IMAGE_TRANSFER_CODE || requestCode == HikeConstants.VIDEO_TRANSFER_CODE || requestCode == HikeConstants.AUDIO_TRANSFER_CODE) 
				&& resultCode==RESULT_OK)
		{
			try
			{
				String filePath = data == null ? selectedFile.getAbsolutePath() : Utils.getRealPathFromUri(data.getData(), this);
				Log.d(getClass().getSimpleName(), "File Path; " + filePath);

				File file = new File(filePath);
				String fileName = file.getName();

				Log.d(getClass().getSimpleName(), "File size: " + file.length() + " File name: " + fileName);

				if(HikeConstants.MAX_FILE_SIZE != -1 && HikeConstants.MAX_FILE_SIZE < file.length())
				{
					Toast.makeText(ChatThread.this, "File Size is too large", Toast.LENGTH_SHORT).show();
					return;
				}

				byte[] fileBytes = Utils.fileToBytes(file);

				if(fileBytes == null)
				{
					Toast.makeText(ChatThread.this, "Unable to read file", Toast.LENGTH_SHORT).show();
					return;
				}

				HikeFileType mediaType = requestCode == HikeConstants.IMAGE_TRANSFER_CODE ? 
						HikeFileType.IMAGE : 
							requestCode ==HikeConstants.VIDEO_TRANSFER_CODE ? 
								HikeFileType.VIDEO : HikeFileType.AUDIO;

				if(data != null)
				{
					selectedFile = Utils.getOutputMediaFile(mediaType, fileName, null);
					// Saving the file to hike local folder
					Utils.bytesToFile(fileBytes, selectedFile);
				}
				Bitmap thumbnail = null;
				String thumbnailString = null;
				if(mediaType == HikeFileType.IMAGE)
				{
					thumbnail = Utils.makeThumbnail(filePath);
				}
				else if(mediaType == HikeFileType.VIDEO)
				{
					thumbnail = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);
				}
				if(thumbnail != null)
				{
					thumbnailString = Base64.encodeToString(Utils.bitmapToBytes(thumbnail), Base64.DEFAULT);
				}

				long time = System.currentTimeMillis()/1000;

				JSONArray files = new JSONArray();
				files.put(new HikeFile(fileName, HikeFileType.toString(mediaType), thumbnailString, thumbnail).serialize());
				JSONObject metadata = new JSONObject();
				metadata.putOpt(HikeConstants.FILES, files);

				ConvMessage convMessage = new ConvMessage(fileName, mContactNumber, time, ConvMessage.State.SENT_UNCONFIRMED);
				convMessage.setMetadata(metadata);

				addMessage(convMessage);
				mConversationDb.addConversationMessages(convMessage);
				mSendBtn.setEnabled(!TextUtils.isEmpty(mComposeView.getText()));

				beginFileUpload(convMessage, fileName, fileBytes);
			}
			catch (JSONException e)
			{
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
		}
	}

	@Override
	public boolean onKey(View view, int keyCode, KeyEvent event)
	{
		if ((event.getAction() == KeyEvent.ACTION_UP) &&
				(keyCode == KeyEvent.KEYCODE_ENTER)
				&& event.isAltPressed())
		{
			mComposeView.append("\n");
			/* micromax phones appear to fire this event twice.
			 * Doing this seems to fix the problem.
			 */
			KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN);
			return true;
		}
		return false;
	}

	private void changeInviteButtonVisibility()
	{
		titleIconView = (ImageView) findViewById(R.id.title_image_btn);
		View btnBar = findViewById(R.id.button_bar);
		titleIconView.setVisibility(mConversation.isOnhike() && !(mConversation instanceof GroupConversation) ? View.GONE : View.VISIBLE);
		titleIconView.setImageResource(mConversation instanceof GroupConversation ? R.drawable.ic_i : R.drawable.ic_invite_top);
		btnBar.setVisibility(mConversation.isOnhike() && !(mConversation instanceof GroupConversation) ? View.GONE : View.VISIBLE);

 		if(!prefs.getBoolean((mConversation instanceof GroupConversation) ? 
				HikeMessengerApp.CHAT_GROUP_INFO_TOOL_TIP_DISMISSED : HikeMessengerApp.CHAT_INVITE_TOOL_TIP_DISMISSED, false) 
				&& Utils.wasScreenOpenedNNumberOfTimes(prefs, (mConversation instanceof GroupConversation) ? 
						HikeMessengerApp.NUM_TIMES_CHAT_THREAD_GROUP : HikeMessengerApp.NUM_TIMES_CHAT_THREAD_INVITE))
		{
			showInviteToolTip();
		}
 		else
 		{
 			// Fix for bug where the tool tip would remain visible in a hike thread.
 			hideToolTip();
 		}
	}
	
	private void showInviteToolTip()
	{
		toolTipLayout = (ViewGroup) findViewById(R.id.credits_help_layout);
		if (toolTipLayout.getVisibility() != View.VISIBLE && !isToolTipShowing) 
		{
			Animation fadeIn = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_in);
			fadeIn.setStartOffset(1000);
			toolTipLayout.setAnimation(fadeIn);
		}
		toolTipLayout.setVisibility(mConversation.isOnhike() ? View.GONE : View.VISIBLE);
		TextView toolTipTxt = (TextView) toolTipLayout.findViewById(R.id.tool_tip);
		String formatString = (mConversation instanceof GroupConversation) ? 
				getString(R.string.tap_group_info) : String.format(getString(R.string.press_btn_invite), mLabel.split(" ", 2)[0]);
		toolTipTxt.setText(formatString); 
	}
	
	public void onToolTipClosed(View v)
	{
		Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.CHAT_INVITE_TOOL_TIP_CLOSED);
		dismissToolTip();
	}

	public void onToolTipClicked(View v)
	{}
	
	private void dismissToolTip()
	{
		Editor editor = prefs.edit();
		editor.putBoolean((mConversation instanceof GroupConversation) ? 
				HikeMessengerApp.CHAT_GROUP_INFO_TOOL_TIP_DISMISSED : HikeMessengerApp.CHAT_INVITE_TOOL_TIP_DISMISSED, true);
		editor.commit();

		hideToolTip();
	}

	private void hideToolTip()
	{
		if (toolTipLayout != null && toolTipLayout.getVisibility() == View.VISIBLE) 
		{
			Animation fadeOut = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_out);
			toolTipLayout.setAnimation(fadeOut);
			toolTipLayout.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		//For preventing the tool tip from animating again if its already showing
		outState.putBoolean(HikeConstants.Extras.TOOLTIP_SHOWING, toolTipLayout != null && toolTipLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.OVERLAY_SHOWING, mOverlayLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.EMOTICON_SHOWING, emoticonLayout != null && emoticonLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.FILE_TRANSFER_DIALOG_SHOWING, filePickerDialog != null && filePickerDialog.isShowing());
		super.onSaveInstanceState(outState);
	}

	public void onEmoticonBtnClicked(View v)
	{
		emoticonLayout = emoticonLayout == null ? (ViewGroup) findViewById(R.id.emoticon_layout) : emoticonLayout;
		emoticonViewPager = emoticonViewPager == null ? (ViewPager) findViewById(R.id.emoticon_pager) : emoticonViewPager;

		if(tabHost != null && !isTabInitialised)
		{
			isTabInitialised  = true;
			Log.d(getClass().getSimpleName(), "Initialising boolean for emoticon layout setup.: " + isTabInitialised);

			View tabHead = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.emoticon_tab_layout, null);
			TabSpec ts1 = tabHost.newTabSpec("tab1");
			((ImageView)tabHead.findViewById(R.id.tab_header_img)).setImageResource(R.drawable.emo_im_01_bigsmile);
			tabHead.findViewById(R.id.divider_left).setVisibility(View.GONE);
			ts1.setIndicator(tabHead);
			ts1.setContent(new TabFactory());
			tabHost.addTab(ts1);

			View tabHead2 = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.emoticon_tab_layout, null);
			TabSpec ts2 = tabHost.newTabSpec("tab2");
			((ImageView)tabHead2.findViewById(R.id.tab_header_img)).setImageResource(R.drawable.emo_im_81_exciting);
			ts2.setIndicator(tabHead2);
			ts2.setContent(new TabFactory());
			tabHost.addTab(ts2);

			View tabHead3 = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.emoticon_tab_layout, null);
			TabSpec ts3 = tabHost.newTabSpec("tab3");
			((ImageView)tabHead3.findViewById(R.id.tab_header_img)).setImageResource(R.drawable.emo_im_111_grin);
			tabHead3.findViewById(R.id.divider_right).setVisibility(View.GONE);
			ts3.setIndicator(tabHead3);
			ts3.setContent(new TabFactory());
			tabHost.addTab(ts3);
		}

		if (emoticonAdapter == null) 
		{
			emoticonAdapter = new EmoticonAdapter(ChatThread.this, mComposeView);
			emoticonViewPager.setAdapter(emoticonAdapter);
		}
		if(emoticonLayout.getVisibility() == View.VISIBLE)
		{
			emoticonLayout.setVisibility(View.INVISIBLE);
		}
		else
		{
			Animation slideUp = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_in);
			slideUp.setDuration(400);
			emoticonLayout.setAnimation(slideUp);
			emoticonLayout.setVisibility(View.VISIBLE);
		}
		emoticonViewPager.setOnPageChangeListener(new OnPageChangeListener() 
		{
			@Override
			public void onPageSelected(int arg0) 
			{
				tabHost.setCurrentTab(arg0);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		
		tabHost.setOnTabChangedListener(new OnTabChangeListener() 
		{
			@Override
			public void onTabChanged(String tabId) 
			{
				emoticonViewPager.setCurrentItem(tabHost.getCurrentTab(), true);
			}
		});
	}
	
	private class TabFactory implements TabContentFactory {

		@Override
		public View createTabContent(String tag) {
			View v = new View(getApplicationContext());
			v.setMinimumWidth(0);
			v.setMinimumHeight(0);
			return v;

		}

	}

	private void groupChatDead()
	{
		this.mComposeView.setVisibility(View.INVISIBLE);
		this.titleIconView.setEnabled(false);
		findViewById(R.id.emo_btn).setEnabled(false);
	}

	private String getMsisdnMainUser()
	{
		return mConversation instanceof GroupConversation ? ((GroupConversation)mConversation).getGroupOwner() : mContactNumber;
	}

	@Override
	public void onFinish(boolean success) {}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) 
	{
		try
		{
			ConvMessage convMessage = (ConvMessage) ((MessagesAdapter)adapterView.getAdapter()).getItem(position);
			if(convMessage != null && convMessage.isFileTransferMessage())
			{
				if(Utils.getExternalStorageState() == ExternalStorageState.NONE)
				{
					Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
					return;
				}
				Log.d(getClass().getSimpleName(), "Message: " + convMessage.getMessage());
				HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
				if(convMessage.isSent())
				{
					Log.d(getClass().getSimpleName(), "Hike File name: " + hikeFile.getFileName() + " File key: " + hikeFile.getFileKey());
					File sentFile = Utils.getOutputMediaFile(HikeFileType.fromString(hikeFile.getFileTypeString()), hikeFile.getFileName(), hikeFile.getFileKey());
					// If uploading failed then we try again.
					if(TextUtils.isEmpty(hikeFile.getFileKey()) && !fileTransferTaskMap.containsKey(convMessage.getMsgID()))
					{
						beginFileUpload(convMessage, 
								hikeFile.getFileName(), 
								Utils.fileToBytes(sentFile));
					}
					// Else we open it for the use to see
					else
					{
						Log.d(getClass().getSimpleName(), "Opening file");
						Intent openFile = new Intent(Intent.ACTION_VIEW);
						openFile.setDataAndType(Uri.fromFile(sentFile), hikeFile.getFileTypeString());
						startActivity(openFile);
					}
				}
				else
				{
					File receivedFile = Utils.getOutputMediaFile(HikeFileType.fromString(hikeFile.getFileTypeString()), hikeFile.getFileName(), hikeFile.getFileKey());
					if(receivedFile.exists())
					{
						Log.d(getClass().getSimpleName(), "Opening file");
						Intent openFile = new Intent(Intent.ACTION_VIEW);
						openFile.setDataAndType(Uri.fromFile(receivedFile), hikeFile.getFileTypeString());
						startActivity(openFile);
					}
					else
					{
						Log.d(getClass().getSimpleName(), "HIKEFILE: NAME: " + hikeFile.getFileName() + " KEY: " + hikeFile.getFileKey() + " TYPE: " + hikeFile.getFileTypeString());
						DownloadFileTask downloadFile = new DownloadFileTask(ChatThread.this, receivedFile, hikeFile.getFileKey(), convMessage.getMsgID());
						downloadFile.execute();
						fileTransferTaskMap.put(convMessage.getMsgID(), downloadFile);
					}
				}
			}
		}
		catch(ActivityNotFoundException e)
		{
			Log.w(getClass().getSimpleName(), "Trying to open an unknown format", e);
			Toast.makeText(ChatThread.this, "Unable to open file (Unknown format)", Toast.LENGTH_SHORT).show();
		}
	}

	private void beginFileUpload(final ConvMessage convMessage, String fileName, byte[] fileBytes)
	{
		HikeFileTransferHttpRequest hikeHttpRequest = new HikeFileTransferHttpRequest("/user/ft", new HikeHttpCallback() 
		{
			public void onFailure()
			{
				Log.d(getClass().getSimpleName(), "FAILURE");
				fileTransferTaskMap.remove(convMessage.getMsgID());
			}

			public void onSuccess(JSONObject response)
			{
				try 
				{
					fileTransferTaskMap.remove(convMessage.getMsgID());
					Log.d(getClass().getSimpleName(), "SUCCESS " + response.toString());

					JSONObject metadata = new JSONObject();
					JSONArray filesArray = new JSONArray();

					filesArray.put(response.optJSONObject("data"));
					HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);

					JSONObject fileJSON = filesArray.getJSONObject(0);
					fileJSON.put(HikeConstants.THUMBNAIL, hikeFile.getThumbnailString());

					hikeFile.setFileKey(fileJSON.optString(HikeConstants.FILE_KEY));

					metadata.put(HikeConstants.FILES, filesArray);
					Log.d(getClass().getSimpleName(), "Response after uploading file: " + metadata.toString());

					convMessage.setMetadata(metadata);
					mConversationDb.addFile(hikeFile.getFileKey(), hikeFile.getFileName());

					mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
					runOnUiThread(mUpdateAdapter);
				}
				catch (JSONException e) 
				{
					Log.e(getClass().getSimpleName(), "Invalid JSON", e);
				}
			}
		}, fileName);
		hikeHttpRequest.setPostData(fileBytes);

		HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(this, R.string.upload_failed);

		Log.d(getClass().getSimpleName(), "Adding message with msg id: " + convMessage.getMsgID());
		fileTransferTaskMap.put(convMessage.getMsgID(), hikeHTTPTask);
		mAdapter.notifyDataSetChanged();

		hikeHTTPTask.execute(hikeHttpRequest);

	}
}
