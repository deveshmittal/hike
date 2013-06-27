package com.bsb.hike.ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
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
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.TipType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AccountAdapter;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.adapters.EmoticonAdapter.EmoticonType;
import com.bsb.hike.adapters.HikeSearchContactAdapter;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.adapters.UpdateAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.AccountData;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ContactInfoData;
import com.bsb.hike.models.ContactInfoData.DataType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.mqtt.client.HikeSSLUtil;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.tasks.UploadContactOrLocationTask;
import com.bsb.hike.tasks.UploadFileTask;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactDialog;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.HikeAppStateBaseActivity;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;

public class ChatThread extends HikeAppStateBaseActivity implements
		HikePubSub.Listener, TextWatcher, OnEditorActionListener,
		OnSoftKeyboardListener, View.OnKeyListener, FinishableEvent,
		OnTouchListener, OnScrollListener, OnItemLongClickListener {

	private enum DialogShowing {
		SMS_SYNC_CONFIRMATION_DIALOG, SMS_SYNCING_DIALOG
	}

	private HikePubSub mPubSub;

	private HikeConversationsDatabase mConversationDb;

	private String mContactName;

	private String mContactNumber;

	private MessagesAdapter mAdapter;

	private Conversation mConversation;

	private FavoriteType favoriteType;

	private ComposeViewWatcher mComposeViewWatcher;

	/* View element */
	private ImageButton mSendBtn;

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

	private TextView mNameView;

	private TextView mLastSeenView;

	private View lastSeenContainer;

	private LinearLayout mInputNumberContainer;

	private boolean mUserIsBlocked;

	private View mOverlayLayout;

	private ArrayList<ConvMessage> messages;

	private CustomLinearLayout chatLayout;

	private Handler mHandler;

	private boolean blockOverlay;

	private Configuration config;

	private SharedPreferences prefs;

	private Animation slideUp;

	private Animation slideDown;

	private TextView smsCount;

	private boolean animatedOnce = false;

	private boolean isOverlayShowing = false;

	private ViewPager emoticonViewPager;

	private ViewGroup emoticonLayout;

	private ImageView titleIconView;

	private Button titleBtn;

	private TabHost tabHost;

	private boolean isTabInitialised = false;

	private EditText mInputNumberView;

	private ListView mContactSearchView;

	private GroupParticipant myInfo;

	private static File selectedFile;

	private Dialog filePickerDialog;

	private static MediaRecorder recorder;

	private static MediaPlayer player;

	public static Map<Long, FileTransferTaskBase> fileTransferTaskMap;

	public static Map<String, StickerTaskBase> stickerTaskMap;

	private Handler recordingHandler;

	private UpdateRecordingDuration updateRecordingDuration;

	private Dialog recordingDialog;

	private RecorderState recorderState;

	private ImageView pageSelected;

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED,
			HikePubSub.TYPING_CONVERSATION, HikePubSub.END_TYPING_CONVERSATION,
			HikePubSub.SMS_CREDIT_CHANGED, HikePubSub.MESSAGE_DELIVERED_READ,
			HikePubSub.MESSAGE_DELIVERED, HikePubSub.SERVER_RECEIVED_MSG,
			HikePubSub.MESSAGE_FAILED, HikePubSub.ICON_CHANGED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT,
			HikePubSub.GROUP_NAME_CHANGED, HikePubSub.GROUP_END,
			HikePubSub.CONTACT_ADDED, HikePubSub.UPLOAD_FINISHED,
			HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED,
			HikePubSub.FILE_MESSAGE_CREATED,
			HikePubSub.MUTE_CONVERSATION_TOGGLED, HikePubSub.BLOCK_USER,
			HikePubSub.UNBLOCK_USER,
			HikePubSub.REMOVE_MESSAGE_FROM_CHAT_THREAD,
			HikePubSub.GROUP_REVIVED, HikePubSub.CHANGED_MESSAGE_TYPE,
			HikePubSub.SHOW_SMS_SYNC_DIALOG, HikePubSub.SMS_SYNC_COMPLETE,
			HikePubSub.SMS_SYNC_FAIL, HikePubSub.SMS_SYNC_START,
			HikePubSub.SHOWN_UNDELIVERED_MESSAGE,
			HikePubSub.STICKER_DOWNLOADED,
			HikePubSub.STICKER_CATEGORY_DOWNLOADED,
			HikePubSub.STICKER_CATEGORY_DOWNLOAD_FAILED,
			HikePubSub.LAST_SEEN_TIME_UPDATED };

	private View currentEmoticonCategorySelected;

	private EmoticonType emoticonType;

	private EmoticonAdapter emoticonsAdapter;

	private boolean wasOrientationChanged = false;

	private GestureDetector gestureDetector;

	private boolean loadingMoreMessages;

	private boolean reachedEnd;

	private ContactDialog contactDialog;

	private DialogShowing dialogShowing;

	private Dialog smsDialog;

	private Dialog nativeSmsDialog;

	private LinearLayout stickerCatgoryContainer;

	private View currentStickerCategorySelected;

	private long recordStartTime;

	private long recordedTime = -1;

	private static boolean recording = false;

	/*
	 * Made this public so that its accessible to this activity's adapter
	 */
	public View tipView;

	@Override
	protected void onPause() {
		super.onPause();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		setMessagesRead();
	}

	@Override
	protected void onResume() {
		super.onResume();
		/* mark any messages unread as read */
		setMessagesRead();

		/* clear any pending notifications */
		/* clear any toast notifications */
		if (mConversation != null) {
			NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mgr.cancel((int) mConversation.getConvId());
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);

		if (mComposeViewWatcher != null) {
			mComposeViewWatcher.init();

			/* check if the send button should be enabled */
			mComposeViewWatcher.setBtnEnabled();
		}
	}

	private class CreateAutoCompleteViewTask extends
			AsyncTask<Void, Void, List<ContactInfo>> {

		private boolean isGroupChat;
		private boolean isForwardingMessage;
		private boolean isSharingFile;
		private boolean freeSMSOn;
		private boolean nativeSMSOn;
		private String userMsisdn;
		private String existingGroupId;
		boolean loadOnUiThread;

		@Override
		protected void onPreExecute() {
			isGroupChat = getIntent().getBooleanExtra(
					HikeConstants.Extras.GROUP_CHAT, false);
			isForwardingMessage = getIntent().getBooleanExtra(
					HikeConstants.Extras.FORWARD_MESSAGE, false);
			isSharingFile = getIntent().getType() != null;
			// Getting the group id. This will be a valid value if the intent
			// was
			// passed to add group participants.
			existingGroupId = getIntent().getStringExtra(
					HikeConstants.Extras.EXISTING_GROUP_CHAT);

			mComposeView.removeTextChangedListener(ChatThread.this);

			if (isSharingFile) {
				mLabelView.setText(R.string.share_file);
			} else if (isForwardingMessage) {
				mLabelView.setText(R.string.forward);
			} else if (!TextUtils.isEmpty(existingGroupId)) {
				mLabelView.setText(R.string.add_group);
			} else if (isGroupChat) {
				mLabelView.setText(R.string.new_group);
			} else {
				mLabelView.setText(R.string.new_message);
			}

			mBottomView.setVisibility(View.GONE);

			if (isGroupChat) {
				titleBtn = (Button) findViewById(R.id.title_icon);
				titleBtn.setText(R.string.done);
				titleBtn.setEnabled(false);
				titleBtn.setVisibility(View.VISIBLE);
				findViewById(R.id.button_bar_2).setVisibility(View.VISIBLE);
			} else {
				// Removing the attachment button that remains visible while
				// forwarding files
				findViewById(R.id.title_image_btn2).setVisibility(View.GONE);
				findViewById(R.id.title_image_btn2_container).setVisibility(
						View.GONE);
				findViewById(R.id.button_bar3).setVisibility(View.GONE);
			}
			SharedPreferences appPref = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());

			freeSMSOn = appPref.getBoolean(HikeConstants.FREE_SMS_PREF, true);

			nativeSMSOn = appPref
					.getBoolean(HikeConstants.SEND_SMS_PREF, false);

			userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

			loadOnUiThread = Utils.loadOnUiThread();
			/*
			 * Show the progress icon.
			 */
			findViewById(R.id.progress_container).setVisibility(
					loadOnUiThread ? View.GONE : View.VISIBLE);
		}

		@Override
		protected List<ContactInfo> doInBackground(Void... params) {
			if (!loadOnUiThread) {
				return getContactsForComposeScreen(userMsisdn, freeSMSOn,
						isGroupChat, isForwardingMessage, isSharingFile,
						nativeSMSOn);
			} else {
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<ContactInfo> contactList) {
			if (contactList == null) {
				contactList = getContactsForComposeScreen(userMsisdn,
						freeSMSOn, isGroupChat, isForwardingMessage,
						isSharingFile, nativeSMSOn);
			}
			/*
			 * Hide the progress icon.
			 */
			findViewById(R.id.progress_container).setVisibility(View.GONE);

			mInputNumberView.setText("");
			HikeSearchContactAdapter adapter = new HikeSearchContactAdapter(
					ChatThread.this, contactList, mInputNumberView,
					isGroupChat, titleBtn, existingGroupId, getIntent(),
					freeSMSOn, nativeSMSOn);
			mContactSearchView.setAdapter(adapter);
			mContactSearchView.setOnItemClickListener(adapter);
			mInputNumberView.addTextChangedListener(adapter);
			mInputNumberView.setSingleLine(!isGroupChat);

			mInputNumberContainer.setVisibility(View.VISIBLE);
			mInputNumberView.setVisibility(View.VISIBLE);
			mContactSearchView.setVisibility(View.VISIBLE);
			mInputNumberView.requestFocus();

			getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}

	}

	private List<ContactInfo> getContactsForComposeScreen(String userMsisdn,
			boolean freeSMSOn, boolean isGroupChat,
			boolean isForwardingMessage, boolean isSharingFile,
			boolean nativeSMSOn) {
		List<ContactInfo> contactList = HikeUserDatabase.getInstance()
				.getContactsForComposeScreen(freeSMSOn,
						(isGroupChat || isForwardingMessage || isSharingFile),
						userMsisdn, nativeSMSOn);

		if (isForwardingMessage || isSharingFile) {
			contactList.addAll(0, ChatThread.this.mConversationDb
					.getGroupNameAndParticipantsAsContacts());
		}
		return contactList;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);

		if (mComposeViewWatcher != null) {
			// If we didn't send an end typing. We should send one before
			// exiting
			if (!mComposeViewWatcher.wasEndTypingSent()) {
				mComposeViewWatcher.sendEndTyping();
			}
			mComposeViewWatcher.uninit();
			mComposeViewWatcher = null;
		}
		if (contactDialog != null) {
			contactDialog.dismiss();
			contactDialog = null;
		}
		if (smsDialog != null) {
			smsDialog.cancel();
			smsDialog = null;
		}
		if (nativeSmsDialog != null) {
			nativeSmsDialog.cancel();
			nativeSmsDialog = null;
		}
		if (mAdapter != null) {
			mAdapter.resetPlayerIfRunning();
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return getIntent();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setDensityMultiplier(ChatThread.this);

		/* force the user into the reg-flow process if the token isn't set */
		if (Utils.requireAuth(this)) {
			return;
		}

		// TODO this is being called everytime this activity is created. Way too
		// often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		setContentView(R.layout.chatthread);

		if (ChatThread.fileTransferTaskMap == null) {
			ChatThread.fileTransferTaskMap = new HashMap<Long, FileTransferTaskBase>();
		}

		if (ChatThread.stickerTaskMap == null) {
			ChatThread.stickerTaskMap = new HashMap<String, StickerTaskBase>();
		}

		mHandler = new Handler();

		prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);

		wasOrientationChanged = savedInstanceState != null;
		isOverlayShowing = savedInstanceState == null ? false
				: savedInstanceState
						.getBoolean(HikeConstants.Extras.OVERLAY_SHOWING);

		config = getResources().getConfiguration();

		/* bind views to variables */
		chatLayout = (CustomLinearLayout) findViewById(R.id.chat_layout);
		mBottomView = findViewById(R.id.bottom_panel);
		mInputNumberContainer = (LinearLayout) findViewById(R.id.input_number_container);
		mConversationsView = (ListView) findViewById(R.id.conversations_list);
		mComposeView = (EditText) findViewById(R.id.msg_compose);
		mSendBtn = (ImageButton) findViewById(R.id.send_message);
		mMetadataNumChars = (TextView) findViewById(R.id.sms_chat_metadata_num_chars);
		mLabelView = (TextView) findViewById(R.id.title);
		mOverlayLayout = findViewById(R.id.overlay_layout);
		mInputNumberView = (EditText) findViewById(R.id.input_number);
		mContactSearchView = (ListView) findViewById(R.id.contact_search_result);
		mNameView = (TextView) findViewById(R.id.name_txt);
		mLastSeenView = (TextView) findViewById(R.id.last_seen);
		lastSeenContainer = findViewById(R.id.last_seen_container);

		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();
		currentEmoticonCategorySelected = findViewById(savedInstanceState != null ? savedInstanceState
				.getInt(HikeConstants.Extras.WHICH_EMOTICON_CATEGORY,
						R.id.hike_emoticons_btn) : R.id.hike_emoticons_btn);
		currentEmoticonCategorySelected.setSelected(true);

		/*
		 * ensure that when the softkeyboard Done button is pressed (different
		 * than the sen button we have), we send the message.
		 */
		mComposeView.setOnEditorActionListener(this);

		/*
		 * Fix for android bug, where the focus is removed from the edittext
		 * when you have a layout with tabs (Emoticon layout) for hard keyboard
		 * devices http://code.google.com/p/android/issues/detail?id=2516
		 */
		if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS) {
			mComposeView.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					mComposeView.requestFocusFromTouch();
					return event == null;
				}
			});
		}

		/* ensure that when we hit Alt+Enter, we insert a newline */
		mComposeView.setOnKeyListener(this);

		mConversationDb = HikeConversationsDatabase.getInstance();

		chatLayout.setOnSoftKeyboardListener(this);
		mPubSub = HikeMessengerApp.getPubSub();
		if (prefs.contains(HikeMessengerApp.TEMP_NUM)) {
			mContactName = prefs.getString(HikeMessengerApp.TEMP_NAME, null);
			mContactNumber = prefs.getString(HikeMessengerApp.TEMP_NUM, null);
			clearTempData();
			setIntentFromField();
			onNewIntent(getIntent());
		} else {
			Object o = getLastNonConfigurationInstance();
			Intent intent = (o instanceof Intent) ? (Intent) o : getIntent();
			onNewIntent(intent);
		}

		if (savedInstanceState != null) {
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.FILE_TRANSFER_DIALOG_SHOWING)) {
				showFilePickerDialog(Utils.getExternalStorageState());
			}
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.EMOTICON_SHOWING)) {
				onEmoticonBtnClicked(null, savedInstanceState.getInt(
						HikeConstants.Extras.WHICH_EMOTICON_SUBCATEGORY, 0),
						false);
			}
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.RECORDER_DIALOG_SHOWING)) {
				recordStartTime = savedInstanceState
						.getLong(HikeConstants.Extras.RECORDER_START_TIME);
				recordedTime = savedInstanceState
						.getLong(HikeConstants.Extras.RECORDED_TIME);

				showRecordingDialog();
			}

			int dialogShowingOrdinal = savedInstanceState.getInt(
					HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1) {
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
				smsDialog = Utils
						.showSMSSyncDialog(
								this,
								dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG);
			}
		}

		/* register listeners */
		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private void clearTempData() {
		Editor editor = prefs.edit();
		editor.remove(HikeMessengerApp.TEMP_NAME);
		editor.remove(HikeMessengerApp.TEMP_NUM);
		editor.commit();
	}

	@Override
	public void onBackPressed() {
		if (!getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT)
				&& this.mConversation != null) {
			if ((mConversation instanceof GroupConversation)) {
				Utils.incrementNumTimesScreenOpen(prefs,
						HikeMessengerApp.NUM_TIMES_CHAT_THREAD_GROUP);
			} else if (!this.mConversation.isOnhike()) {
				Utils.incrementNumTimesScreenOpen(prefs,
						HikeMessengerApp.NUM_TIMES_CHAT_THREAD_INVITE);
			}
		}
		if (emoticonLayout == null
				|| emoticonLayout.getVisibility() != View.VISIBLE) {

			selectedFile = null;

			Intent intent = null;
			if (!getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT)
					&& !getIntent().hasExtra(
							HikeConstants.Extras.FORWARD_MESSAGE)
					&& !getIntent().getBooleanExtra(
							HikeConstants.Extras.FROM_CENTRAL_TIMELINE, false)
					&& !getIntent().getBooleanExtra(
							HikeConstants.Extras.FROM_CENTRAL_TIMELINE, false)) {
				intent = new Intent(this, MessagesList.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			} else if (getIntent().hasExtra(
					HikeConstants.Extras.FORWARD_MESSAGE)) {
				super.onBackPressed();
				intent = new Intent(this, ChatThread.class);
				intent.putExtra(HikeConstants.Extras.NAME, getIntent()
						.getStringExtra(HikeConstants.Extras.PREV_NAME));
				intent.putExtra(HikeConstants.Extras.MSISDN, getIntent()
						.getStringExtra(HikeConstants.Extras.PREV_MSISDN));
				startActivity(intent);
			}

			/* slide down if we're still selecting a user, otherwise slide back */
			if (mConversation == null
					&& !getIntent().hasExtra(
							HikeConstants.Extras.EXISTING_GROUP_CHAT)
					&& !getIntent().hasExtra(
							HikeConstants.Extras.FORWARD_MESSAGE)) {
				overridePendingTransition(R.anim.no_animation,
						R.anim.slide_down_noalpha);
			} else {
				overridePendingTransition(R.anim.slide_in_left_noalpha,
						R.anim.slide_out_right_noalpha);
			}
			/*
			 * If the user had typed something, we save it as a draft and will
			 * show it in the text box when he comes back to this conversation.
			 */
			if (mComposeView != null
					&& mComposeView.getVisibility() == View.VISIBLE) {
				Editor editor = getSharedPreferences(
						HikeConstants.DRAFT_SETTING, MODE_PRIVATE).edit();
				if (mComposeView.length() != 0) {
					editor.putString(mContactNumber, mComposeView.getText()
							.toString());
				} else {
					editor.remove(mContactNumber);
				}
				editor.commit();
			}
			super.onBackPressed();
		} else {
			onEmoticonBtnClicked(null, 0, true);
		}
	}

	public boolean performContextBasedOperationOnMessage(ConvMessage message,
			int id) {
		switch (id) {
		case R.id.copy:
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			if (message.isFileTransferMessage()) {
				HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
				clipboard.setText(AccountUtils.fileTransferBaseViewUrl
						+ hikeFile.getFileKey());
			} else {
				clipboard.setText(message.getMessage());
			}
			return true;
		case R.id.forward:
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.FORWARD_MSG);
			Intent intent = new Intent(this, ChatThread.class);
			String msg;
			intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
			if (message.isFileTransferMessage()) {
				HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
				intent.putExtra(HikeConstants.Extras.FILE_KEY,
						hikeFile.getFileKey());
				if (hikeFile.getHikeFileType() == HikeFileType.LOCATION) {
					intent.putExtra(HikeConstants.Extras.ZOOM_LEVEL,
							hikeFile.getZoomLevel());
					intent.putExtra(HikeConstants.Extras.LATITUDE,
							hikeFile.getLatitude());
					intent.putExtra(HikeConstants.Extras.LONGITUDE,
							hikeFile.getLongitude());
				} else if (hikeFile.getHikeFileType() == HikeFileType.CONTACT) {
					intent.putExtra(HikeConstants.Extras.CONTACT_METADATA,
							hikeFile.serialize().toString());
				} else {
					intent.putExtra(HikeConstants.Extras.FILE_PATH,
							hikeFile.getFilePath());
					intent.putExtra(HikeConstants.Extras.FILE_TYPE,
							hikeFile.getFileTypeString());
				}
			} else if (message.isStickerMessage()) {
				Sticker sticker = message.getMetadata().getSticker();
				intent.putExtra(HikeConstants.Extras.FWD_CATEGORY_ID,
						sticker.getCategoryId());
				intent.putExtra(HikeConstants.Extras.FWD_STICKER_ID,
						sticker.getStickerId());
			} else {
				msg = message.getMessage();
				intent.putExtra(HikeConstants.Extras.MSG, msg);
			}
			intent.putExtra(HikeConstants.Extras.PREV_MSISDN, mContactNumber);
			intent.putExtra(HikeConstants.Extras.PREV_NAME, mContactName);
			startActivity(intent);
			return true;
		case R.id.delete:
			removeMessage(message);
			if (message.isFileTransferMessage()) {
				FileTransferTaskBase fileTransferTask = ChatThread.fileTransferTaskMap
						.get(message.getMsgID());
				if (fileTransferTask != null) {
					fileTransferTask.cancelTask();
					ChatThread.fileTransferTaskMap.remove(message.getMsgID());
					mAdapter.notifyDataSetChanged();
				}
			}
			return true;
		case R.id.cancel_file_transfer:
			FileTransferTaskBase fileTransferTask = ChatThread.fileTransferTaskMap
					.get(message.getMsgID());
			if (fileTransferTask != null) {
				fileTransferTask.cancelTask();
				ChatThread.fileTransferTaskMap.remove(message.getMsgID());
				mAdapter.notifyDataSetChanged();
			}
			return true;
		case R.id.share:
			HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
			Utils.startShareIntent(
					ChatThread.this,
					getString(
							R.string.share_file_message,
							AccountUtils.fileTransferBaseViewUrl
									+ hikeFile.getFileKey()));
			return true;
		default:
			return false;
		}
	}

	@Override
	/*
	 * this function is called right before the options menu is shown. Disable
	 * fields here as appropriate
	 * 
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		/*
		 * if the number ends with Hike, disable blocking, add to contacts and
		 * call
		 */
		if ((mContactNumber != null) && (mContactNumber.endsWith("HIKE"))) {
			return false;
		}

		onTitleIconClick(findViewById(R.id.title_image_btn));
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * only enable the options menu after we've selected a conversation or
		 * the conversation is not a group conversation
		 */
		if (mConversation == null) {
			return false;
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mConversation == null) {
			Log.w("ChatThread",
					"OptionItem menu selected when conversation was null");
			return false;
		}

		if (item.getItemId() == R.id.block_menu) {
			mPubSub.publish(HikePubSub.BLOCK_USER, getMsisdnMainUser());
			blockUser();
		} else if (item.getItemId() == R.id.add_contact_menu) {
			Utils.logEvent(ChatThread.this,
					HikeConstants.LogEvent.MENU_ADD_TO_CONTACTS);
			Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
			i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
			i.putExtra(Insert.PHONE, mConversation.getMsisdn());
			startActivity(i);
		} else if (item.getItemId() == R.id.call) {
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.MENU_CALL);
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:" + mContactNumber));
			startActivity(callIntent);
		}

		return true;
	}

	private void blockUser() {
		Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.MENU_BLOCK);
		mUserIsBlocked = true;
		showOverlay(true);
	}

	private void unblockUser() {
		mUserIsBlocked = false;
		mComposeView.setEnabled(true);
		hideOverlay();
	}

	public void onOverlayButtonClick(View v) {
		/* user clicked the unblock button in the chat-screen */
		if (v.getId() != R.id.overlay_layout && blockOverlay) {
			mPubSub.publish(HikePubSub.UNBLOCK_USER, getMsisdnMainUser());
			unblockUser();
		} else if (v.getId() != R.id.overlay_layout) {
			Utils.logEvent(ChatThread.this,
					HikeConstants.LogEvent.INVITE_OVERLAY_BUTTON);
			inviteUser();
			hideOverlay();
		}

		if (!blockOverlay) {
			hideOverlay();
			mConversationDb.setOverlay(true, mConversation.getMsisdn());
		}
	}

	private void hideOverlay() {
		if (mOverlayLayout.getVisibility() == View.VISIBLE && hasWindowFocus()) {
			Animation fadeOut = AnimationUtils.loadAnimation(ChatThread.this,
					android.R.anim.fade_out);
			mOverlayLayout.setAnimation(fadeOut);
		}
		mOverlayLayout.setVisibility(View.INVISIBLE);
		if (mConversation instanceof GroupConversation) {
			mComposeView.setEnabled(true);
		}
		isOverlayShowing = false;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		ConvMessage message = mAdapter.getItem(position);
		return showMessageContextMenu(message);
	}

	public boolean showMessageContextMenu(final ConvMessage message) {
		if (message == null
				|| message.getParticipantInfoState() != ParticipantInfoState.NO_INFO) {
			return false;
		}

		ArrayList<String> optionsList = new ArrayList<String>();

		if (message.isFileTransferMessage()) {
			HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
			if (!TextUtils.isEmpty(hikeFile.getFileKey())
					&& hikeFile.wasFileDownloaded()) {
				optionsList.add(getString(R.string.forward));
			}
			if (ChatThread.fileTransferTaskMap.containsKey(message.getMsgID())) {
				optionsList
						.add(message.isSent() ? getString(R.string.cancel_upload)
								: getString(R.string.cancel_download));
			}
		} else if (message.getMetadata() == null
				|| !message.getMetadata().isPokeMessage()) {
			optionsList.add(getString(R.string.forward));
			if (!message.isStickerMessage()) {
				optionsList.add(getString(R.string.copy));
			}
		}

		optionsList.add(getString(R.string.delete));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this,
				R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String option = options[which];
						if (getString(R.string.forward).equals(option)) {
							performContextBasedOperationOnMessage(message,
									R.id.forward);
						} else if (getString(R.string.cancel_download).equals(
								option)
								|| getString(R.string.cancel_upload).equals(
										option)) {
							performContextBasedOperationOnMessage(message,
									R.id.cancel_file_transfer);
						} else if (getString(R.string.copy).equals(option)) {
							performContextBasedOperationOnMessage(message,
									R.id.copy);
						} else if (getString(R.string.delete).equals(option)) {
							performContextBasedOperationOnMessage(message,
									R.id.delete);
						}
					}
				});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(
				getResources()
						.getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	private void sendMessage(ConvMessage convMessage) {
		addMessage(convMessage);

		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
	}

	public void onSendClick(View v) {
		if (!mConversation.isOnhike() && mCredits <= 0) {
			return;
		}
		if (TextUtils.isEmpty(mComposeView.getText())) {
			if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE) {
				Toast.makeText(getApplicationContext(),
						R.string.no_external_storage, Toast.LENGTH_SHORT)
						.show();
				return;
			}
			if (tipView != null) {
				TipType viewTipType = (TipType) tipView.getTag();
				if (viewTipType == TipType.WALKIE_TALKIE) {
					Utils.closeTip(TipType.WALKIE_TALKIE, tipView, prefs);
					tipView = null;
				}
			}
			if (!prefs.getBoolean(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP,
					false)) {
				/*
				 * The user has already tapped on the walkie talkie button
				 * without seeing the tip no need to show it now.
				 */
				Editor editor = prefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP,
						true);
				editor.commit();
			}
			showRecordingDialog();
			return;
		}

		String message = mComposeView.getText().toString();

		mComposeView.setText("");

		ConvMessage convMessage = makeConvMessage(message);

		sendMessage(convMessage);

		if (mComposeViewWatcher != null) {
			mComposeViewWatcher.onMessageSent();
		}
	}

	private ConvMessage makeConvMessage(String message) {
		long time = (long) System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(message, mContactNumber,
				time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setConversation(mConversation);
		convMessage.setSMS(mConversation == null || !mConversation.isOnhike());

		return convMessage;
	}

	/*
	 * this function is called externally when our Activity is on top and the
	 * user selects an Intent for this same Activity
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(getClass().getSimpleName(), "Intent: " + intent.toString());
		titleIconView = (ImageView) findViewById(R.id.title_image_btn);
		View btnBar = findViewById(R.id.button_bar);
		titleIconView.setVisibility(View.GONE);
		btnBar.setVisibility(View.GONE);

		mLabelView.setVisibility(View.VISIBLE);
		lastSeenContainer.setVisibility(View.GONE);

		String prevContactNumber = null;

		if (mComposeViewWatcher != null) {
			mComposeViewWatcher.uninit();
			mComposeViewWatcher = null;
		}

		/* setIntent so getIntent returns the right values */
		setIntent(intent);

		Uri dataURI = intent.getData();

		if (mAdapter != null) {
			messages.clear();
			mAdapter.notifyDataSetChanged();
		}

		mConversation = null;

		if ((dataURI != null)
				&& ("smsto".equals(dataURI.getScheme()) || "sms".equals(dataURI
						.getScheme()))) {
			// Intent received externally
			String phoneNumber = dataURI.getSchemeSpecificPart();
			// We were getting msisdns with spaces in them. Replacing all spaces
			// so that lookup is correct
			phoneNumber = phoneNumber.replaceAll(" ", "");
			/*
			 * Replacing all '-' that we get in the number
			 */
			phoneNumber = phoneNumber.replaceAll("-", "");
			Log.d(getClass().getSimpleName(), "SMS To: " + phoneNumber);
			ContactInfo contactInfo = HikeUserDatabase.getInstance()
					.getContactInfoFromPhoneNo(phoneNumber);
			/*
			 * phone lookup fails for a *lot* of people. If that happens, fall
			 * back to using their msisdn
			 */
			if (contactInfo != null) {
				mContactName = contactInfo.getName();
				prevContactNumber = mContactNumber;
				mContactNumber = contactInfo.getMsisdn();
				setIntentFromField();
			} else {
				prevContactNumber = mContactNumber;
				mContactName = mContactNumber = Utils.normalizeNumber(
						phoneNumber, prefs.getString(
								HikeMessengerApp.COUNTRY_CODE,
								HikeConstants.INDIA_COUNTRY_CODE));
			}

			createConversation();
		} else if (intent.hasExtra(HikeConstants.Extras.MSISDN)
				&& !intent.hasExtra(HikeConstants.Extras.GROUP_CHAT)) {

			prevContactNumber = mContactNumber;
			// selected chat from conversation list
			mContactNumber = intent.getStringExtra(HikeConstants.Extras.MSISDN);
			mContactName = intent.getStringExtra(HikeConstants.Extras.NAME);

			createConversation();
			if (intent.getBooleanExtra(HikeConstants.Extras.INVITE, false)) {
				intent.removeExtra(HikeConstants.Extras.INVITE);
				inviteUser();
			}

			if (intent.hasExtra(HikeConstants.Extras.MSG)) {
				String msg = intent.getStringExtra(HikeConstants.Extras.MSG);
				mComposeView.setText(msg);
				mComposeView.setSelection(mComposeView.length());
				SmileyParser.getInstance().addSmileyToEditable(
						mComposeView.getText(), false);
			} else if (intent.hasExtra(HikeConstants.Extras.FILE_PATH)) {
				String fileKey = null;
				String filePath = intent
						.getStringExtra(HikeConstants.Extras.FILE_PATH);
				String fileType = intent
						.getStringExtra(HikeConstants.Extras.FILE_TYPE);
				HikeFileType hikeFileType = HikeFileType.fromString(fileType);

				Log.d(getClass().getSimpleName(), "Forwarding file- Type:"
						+ fileType + " Path: " + filePath);
				initialiseFileTransfer(filePath, hikeFileType, fileType,
						fileType, false, -1, true);

				// Making sure the file does not get forwarded again on
				// orientation change.
				intent.removeExtra(HikeConstants.Extras.FILE_PATH);
			} else if (intent.hasExtra(HikeConstants.Extras.LATITUDE)) {
				String fileKey = null;
				double latitude = intent.getDoubleExtra(
						HikeConstants.Extras.LATITUDE, 0);
				double longitude = intent.getDoubleExtra(
						HikeConstants.Extras.LONGITUDE, 0);
				int zoomLevel = intent.getIntExtra(
						HikeConstants.Extras.ZOOM_LEVEL, 0);

				initialiseLocationTransfer(latitude, longitude, zoomLevel);
				// Making sure the file does not get forwarded again on
				// orientation change.
				intent.removeExtra(HikeConstants.Extras.LATITUDE);
			} else if (intent.hasExtra(HikeConstants.Extras.CONTACT_METADATA)) {
				try {
					JSONObject contactJson = new JSONObject(
							intent.getStringExtra(HikeConstants.Extras.CONTACT_METADATA));
					HikeFile hikeFile = new HikeFile(contactJson);
					showContactDetails(
							Utils.getContactDataFromHikeFile(hikeFile),
							hikeFile.getDisplayName(), contactJson, false);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else if (intent.hasExtra(HikeConstants.Extras.FWD_CATEGORY_ID)) {
				String categoryId = intent
						.getStringExtra(HikeConstants.Extras.FWD_CATEGORY_ID);
				String stickerId = intent
						.getStringExtra(HikeConstants.Extras.FWD_STICKER_ID);
				Sticker sticker = new Sticker(categoryId, stickerId);
				sendSticker(sticker);
			}
			/*
			 * Since the message was not forwarded, we check if we have any
			 * drafts saved for this conversation, if we do we enter it in the
			 * compose box.
			 */
			else {
				String message = getSharedPreferences(
						HikeConstants.DRAFT_SETTING, MODE_PRIVATE).getString(
						mContactNumber, "");
				mComposeView.setText(message);
				mComposeView.setSelection(mComposeView.length());
				SmileyParser.getInstance().addSmileyToEditable(
						mComposeView.getText(), false);
			}
			intent.removeExtra(HikeConstants.Extras.FORWARD_MESSAGE);
		} else {
			/*
			 * The user chose to either start a new conversation or forward a
			 * message.
			 */
			new CreateAutoCompleteViewTask().execute();
		}
		/*
		 * close context menu(if open) if the previous MSISDN is different from
		 * the current one)
		 */
		if (prevContactNumber != null
				&& !prevContactNumber.equalsIgnoreCase(mContactNumber)) {
			Log.w("ChatThread", "DIFFERENT MSISDN CLOSING CONTEXT MENU!!");
			closeContextMenu();
		}
	}

	private void inviteUser() {
		if (mConversation == null) {
			return;
		}

		if (!mConversation.isOnhike()) {
			Utils.sendInvite(mContactNumber, this);
			Toast.makeText(this, R.string.invite_sent, Toast.LENGTH_SHORT)
					.show();
		} else if (mConversation instanceof GroupConversation) {
			startActivity(new Intent(ChatThread.this, HikeListActivity.class));
		} else {
			Toast toast = Toast.makeText(this, R.string.already_hike_user,
					Toast.LENGTH_LONG);
			toast.show();
		}
	}

	/*
	 * sets the intent for this screen based on the fields we've assigned.
	 * useful if the user has entered information or we've determined
	 * information that indicates the type of data on this screen.
	 */
	private void setIntentFromField() {
		Intent intent = new Intent();
		if (mContactName != null) {
			intent.putExtra(HikeConstants.Extras.NAME, mContactName);
		}

		if (!TextUtils.isEmpty(mContactNumber)) {
			intent.putExtra(HikeConstants.Extras.MSISDN, mContactNumber);
		}

		setIntent(intent);
	}

	/**
	 * Renders the chats for a given user
	 */
	private void createConversation() {
		// This prevent the activity from simply finishing and opens up the last
		// screen.
		getIntent().removeExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);

		findViewById(R.id.title_icon).setVisibility(View.GONE);
		findViewById(R.id.button_bar_2).setVisibility(View.GONE);
		findViewById(R.id.title_image_btn2).setVisibility(View.VISIBLE);
		findViewById(R.id.title_image_btn2_container).setVisibility(
				View.VISIBLE);
		findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);

		mComposeView.setFocusable(true);
		mComposeView.setVisibility(View.VISIBLE);
		mComposeView.requestFocus();
		/* hide the number picker */
		mInputNumberView.setVisibility(View.GONE);
		mContactSearchView.setVisibility(View.GONE);
		mInputNumberContainer.setVisibility(View.GONE);

		/*
		 * strictly speaking we shouldn't be reading from the db in the UI
		 * Thread
		 */
		mConversation = mConversationDb.getConversation(mContactNumber,
				HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY);
		if (mConversation == null) {
			if (Utils.isGroupConversation(mContactNumber)) {
				/* the user must have deleted the chat. */
				Toast toast = Toast.makeText(this, R.string.invalid_group_chat,
						Toast.LENGTH_LONG);
				toast.show();
				onBackPressed();
				return;
			}

			mConversation = mConversationDb.addConversation(mContactNumber,
					false, "", null);
		}

		/*
		 * Setting a flag which tells us whether the group contains sms users or
		 * not.
		 */
		if (mConversation instanceof GroupConversation) {
			boolean hasSmsUser = false;
			for (Entry<String, GroupParticipant> entry : ((GroupConversation) mConversation)
					.getGroupParticipantList().entrySet()) {
				GroupParticipant groupParticipant = entry.getValue();
				if (!groupParticipant.getContactInfo().isOnhike()) {
					hasSmsUser = true;
					break;
				}
			}
			((GroupConversation) mConversation).setHasSmsUser(hasSmsUser);
		}

		mLabel = mConversation.getLabel();

		titleIconView = (ImageView) findViewById(R.id.title_image_btn);
		titleIconView.setVisibility(View.VISIBLE);
		titleIconView.setImageResource(R.drawable.ic_i);

		View btnBar = findViewById(R.id.button_bar);
		btnBar.setVisibility(View.VISIBLE);

		gestureDetector = new GestureDetector(this, simpleOnGestureListener);

		mLabelView.setText(mLabel);
		if (!(mConversation instanceof GroupConversation)) {
			mNameView.setText(mLabel);

			favoriteType = HikeUserDatabase.getInstance()
					.getContactInfoFromMSISDN(mContactNumber, false)
					.getFavoriteType();

			if (!mConversation.isOnhike()) {
				HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
						"/account/profile/" + mContactNumber,
						RequestType.HIKE_JOIN_TIME, new HikeHttpCallback() {
							@Override
							public void onSuccess(JSONObject response) {
								Log.d(getClass().getSimpleName(), "Response: "
										+ response.toString());
								try {
									JSONObject profile = response
											.getJSONObject(HikeConstants.PROFILE);
									long hikeJoinTime = profile.optLong(
											HikeConstants.JOIN_TIME, 0);
									if (hikeJoinTime > 0) {
										hikeJoinTime = Utils
												.applyServerTimeOffset(
														ChatThread.this,
														hikeJoinTime);

										HikeMessengerApp
												.getPubSub()
												.publish(
														HikePubSub.HIKE_JOIN_TIME_OBTAINED,
														new Pair<String, Long>(
																mContactNumber,
																hikeJoinTime));
										ContactUtils.updateHikeStatus(
												ChatThread.this,
												mContactNumber, true);
										mConversationDb.updateOnHikeStatus(
												mContactNumber, true);
										HikeMessengerApp.getPubSub().publish(
												HikePubSub.USER_JOINED,
												mContactNumber);
									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						});
				HikeHTTPTask getHikeJoinTimeTask = new HikeHTTPTask(null, -1);
				getHikeJoinTimeTask.execute(hikeHttpRequest);
			}

			if (shouldShowLastSeen()) {
				/*
				 * Fetching last seen value.
				 */
				new FetchLastSeenTask(mContactNumber, false).execute();
			}
		}

		HikeUserDatabase db = HikeUserDatabase.getInstance();
		mUserIsBlocked = db.isBlocked(getMsisdnMainUser());
		if (mUserIsBlocked) {
			showOverlay(true);
		}

		/*
		 * make a copy of the message list since it's used internally by the
		 * adapter
		 */
		messages = new ArrayList<ConvMessage>(mConversation.getMessages());

		/*
		 * Add another item which translates to the SMS toggle option.
		 */
		if (!mConversation.isOnhike()
				&& !Utils.isContactInternational(mContactNumber)) {
			messages.add(0, new ConvMessage(null, null, -1,
					State.RECEIVED_READ, ConvMessage.SMS_TOGGLE_ID, -1));
		}

		mAdapter = new MessagesAdapter(this, messages, mConversation, this);
		mConversationsView.setAdapter(mAdapter);
		mConversationsView.setOnItemLongClickListener(this);
		mConversationsView.setOnTouchListener(this);
		mConversationsView.setOnScrollListener(this);

		if (messages.isEmpty() && mBottomView.getVisibility() != View.VISIBLE) {
			Animation alphaIn = AnimationUtils.loadAnimation(
					getApplicationContext(), R.anim.slide_up_noalpha);
			alphaIn.setDuration(400);
			mBottomView.setAnimation(alphaIn);
			mBottomView.setVisibility(View.VISIBLE);
		} else {
			mBottomView.setVisibility(View.VISIBLE);
		}

		// Scroll to the bottom if we just opened a new conversation
		if (!wasOrientationChanged) {
			mConversationsView.setSelection(messages.size() - 1);
		}

		/* add a text changed listener */
		mComposeView.addTextChangedListener(this);

		/* get the number of credits and also listen for changes */
		mCredits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0)
				.getInt(HikeMessengerApp.SMS_SETTING, 0);

		if (mComposeViewWatcher != null) {
			mComposeViewWatcher.uninit();
		}

		updateUIForHikeStatus();

		mComposeViewWatcher = new ComposeViewWatcher(mConversation,
				mComposeView, mSendBtn, mCredits);

		/*
		 * create an object that we can notify when the contents of the thread
		 * are updated
		 */
		mUpdateAdapter = new UpdateAdapter(mAdapter);

		/* clear any toast notifications */
		NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mgr.cancel((int) mConversation.getConvId());

		if (mConversation instanceof GroupConversation) {
			myInfo = new GroupParticipant(Utils.getUserContactInfo(prefs));
			toggleConversationMuteViewVisibility(((GroupConversation) mConversation)
					.isMuted());
		} else {
			toggleConversationMuteViewVisibility(false);
		}

		if ((mConversation instanceof GroupConversation)
				&& !((GroupConversation) mConversation).getIsGroupAlive()) {
			toggleGroupLife(false);
		}
		/*
		 * Check whether we have an existing typing notification for this
		 * conversation
		 */
		if (HikeMessengerApp.getTypingNotificationSet().containsKey(
				mContactNumber)) {
			runOnUiThread(new SetTypingText(true));
		}

		/*
		 * Only show these tips in a live group conversation or other
		 * conversations.
		 */
		if (!(mConversation instanceof GroupConversation)
				|| ((GroupConversation) mConversation).getIsGroupAlive()) {
			if (!prefs.getBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, false)) {
				tipView = findViewById(R.id.emoticon_tip);
				Utils.showTip(this, TipType.EMOTICON, tipView);
			} else if (!prefs.getBoolean(
					HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP, false)) {
				/*
				 * Only show the tip if we currently do not have any drafts
				 */
				if (TextUtils.isEmpty(getSharedPreferences(
						HikeConstants.DRAFT_SETTING, MODE_PRIVATE).getString(
						mContactNumber, ""))) {
					tipView = findViewById(R.id.walkie_talkie_tip);
					Utils.showTip(this, TipType.WALKIE_TALKIE, tipView);
				}
			}
			if (tipView == null
					&& !(mConversation instanceof GroupConversation)
					&& !prefs.getBoolean(HikeMessengerApp.NUDGE_INTRO_SHOWN,
							false)) {
				showNudgeDialog();
			}
		}
	}

	private boolean shouldShowLastSeen() {
		if ((favoriteType == FavoriteType.FRIEND
				|| favoriteType == FavoriteType.REQUEST_RECEIVED || favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED)
				&& mConversation.isOnhike()) {
			return PreferenceManager.getDefaultSharedPreferences(this)
					.getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		}
		return false;
	}

	private void showNudgeDialog() {

		final Dialog nudgeAlert = new Dialog(this, R.style.Theme_CustomDialog);
		nudgeAlert.setCancelable(true);
		nudgeAlert.setContentView(R.layout.nudge_dialog);

		nudgeAlert.setCancelable(true);

		Button okBtn = (Button) nudgeAlert.findViewById(R.id.ok_btn);
		okBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				nudgeAlert.cancel();
			}
		});
		nudgeAlert.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				Editor editor = prefs.edit();
				editor.putBoolean(HikeMessengerApp.NUDGE_INTRO_SHOWN, true);
				editor.commit();
			}
		});
		nudgeAlert.show();
	}

	/*
	 * Update the UI to show SMS Credits/etc if the conversation is on hike
	 */
	private void updateUIForHikeStatus() {
		if (mConversation.isOnhike()
				|| (mConversation instanceof GroupConversation)) {

			removeSMSToggle();

			setEmoticonButton();
			mSendBtn.setBackgroundResource(R.drawable.send_hike_btn);
			mComposeView
					.setHint(mConversation instanceof GroupConversation ? R.string.group_msg
							: R.string.hike_msg);
			findViewById(R.id.title_image_btn2).setEnabled(true);
			if ((mConversation instanceof GroupConversation)
					&& ((GroupConversation) mConversation).hasSmsUser()) {
				if (mCredits == 0) {
					zeroCredits();
				} else {
					nonZeroCredits();
				}
			} else {
				nonZeroCredits();
			}
		} else {
			updateChatMetadata();
			setEmoticonButton();
			mSendBtn.setBackgroundResource(R.drawable.send_sms_btn);
			mComposeView.setHint(R.string.sms_msg);
		}
	}

	private void removeSMSToggle() {
		if (!messages.isEmpty() && hasSMSToggle()) {
			mAdapter.removeMessage(0);
		}
	}

	private void setEmoticonButton() {
		if (emoticonLayout != null
				&& emoticonLayout.getVisibility() == View.VISIBLE) {
			((ImageButton) findViewById(R.id.emo_btn))
					.setImageResource(R.drawable.keyboard_btn);
		} else {
			if (mConversation == null) {
				return;
			}
			((ImageButton) findViewById(R.id.emo_btn))
					.setImageResource((mConversation.isOnhike() || (mConversation instanceof GroupConversation)) ? R.drawable.emoticon_hike_btn
							: R.drawable.emoticon_sms_btn);
		}
	}

	private boolean hasSMSToggle() {
		ConvMessage convMessage = messages.get(0);
		/*
		 * Typing notification
		 */
		if (convMessage == null) {
			return false;
		}
		if (convMessage.getMsgID() == ConvMessage.SMS_TOGGLE_ID) {
			return true;
		}
		return false;
	}

	/* returns TRUE iff the last message was received and unread */
	private boolean isLastMsgReceivedAndUnread() {
		int count = (mAdapter != null && mConversation != null) ? mAdapter
				.getCount() : 0;
		ConvMessage lastMsg = count > 0 ? mAdapter.getItem(count - 1) : null;
		if (lastMsg == null) {
			return false;
		}

		return lastMsg.getState() == ConvMessage.State.RECEIVED_UNREAD
				|| lastMsg.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE;
	}

	/*
	 * marks messages read
	 */
	private void setMessagesRead() {
		if (!hasWindowFocus()) {
			return;
		}

		if (isLastMsgReceivedAndUnread()) {
			if (PreferenceManager.getDefaultSharedPreferences(
					getApplicationContext()).getBoolean(
					HikeConstants.RECEIVE_SMS_PREF, false)) {
				setSMSReadInNative();
			}

			long convID = mConversation.getConvId();
			JSONArray ids = mConversationDb
					.updateStatusAndSendDeliveryReport(convID);
			/*
			 * If there are msgs which are RECEIVED UNREAD then only broadcast a
			 * msg that these are read avoid sending read notifications for
			 * group chats
			 */
			if (ids != null) {
				int lastReadIndex = messages.size() - ids.length();
				/*
				 * Showing an indicator as to which are the unread messages.
				 */
				if ((mConversation instanceof GroupConversation)
						&& lastReadIndex < messages.size() && lastReadIndex > 0) {
					mAdapter.addMessage(new ConvMessage(null, null, 0,
							State.SENT_DELIVERED_READ,
							MessagesAdapter.LAST_READ_CONV_MESSAGE_ID, 0),
							lastReadIndex);
					mAdapter.notifyDataSetChanged();
				}
				// Scroll to the last unread message
				mConversationsView.setSelection(lastReadIndex - 1);

				mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());

				JSONObject object = new JSONObject();
				try {
					object.put(HikeConstants.TYPE,
							HikeConstants.MqttMessageTypes.MESSAGE_READ);
					object.put(HikeConstants.TO, mConversation.getMsisdn());
					object.put(HikeConstants.DATA, ids);
				} catch (JSONException e) {
					e.printStackTrace();
				}

				mPubSub.publish(HikePubSub.MQTT_PUBLISH, object);
			}
		}
	}

	private void setSMSReadInNative() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Log.d(getClass().getSimpleName(), "Marking message as read: "
						+ mContactNumber);

				ContentValues contentValues = new ContentValues();
				contentValues.put(HikeConstants.SMSNative.READ, 1);

				getContentResolver().update(
						HikeConstants.SMSNative.INBOX_CONTENT_URI,
						contentValues, HikeConstants.SMSNative.NUMBER + "=?",
						new String[] { mContactNumber });
			}
		}).start();
	}

	private class SetTypingText implements Runnable {
		public SetTypingText(boolean direction) {
			this.direction = direction;
		}

		boolean direction;

		@Override
		public void run() {
			if (direction) {
				if (messages.isEmpty()
						|| messages.get(messages.size() - 1) != null) {
					addMessage(null);
				}
			} else {
				if (!messages.isEmpty()
						&& messages.get(messages.size() - 1) == null) {
					messages.remove(messages.size() - 1);
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	@Override
	public void onEventReceived(final String type, final Object object) {
		if (mContactNumber == null || mConversation == null) {
			Log.w("ChatThread",
					"received message when contactNumber is null type=" + type
							+ " object=" + object);
			return;
		}

		if (HikePubSub.MESSAGE_RECEIVED.equals(type)) {
			final ConvMessage message = (ConvMessage) object;
			String msisdn = message.getMsisdn();
			if (msisdn == null) {
				Log.wtf("ChatThread",
						"Message with missing msisdn:" + message.toString());
			}
			if (msisdn.equals(mContactNumber)) {
				/*
				 * we publish the message before the conversation is created, so
				 * it's safer to just tack it on here
				 */
				message.setConversation(mConversation);

				if (hasWindowFocus()) {
					message.setState(ConvMessage.State.RECEIVED_READ);
					mConversationDb.updateMsgStatus(message.getMsgID(),
							ConvMessage.State.RECEIVED_READ.ordinal(),
							mConversation.getMsisdn());
					mPubSub.publish(HikePubSub.MQTT_PUBLISH,
							message.serializeDeliveryReportRead()); // handle
																	// return to
																	// sender

					mPubSub.publish(HikePubSub.MSG_READ,
							mConversation.getMsisdn());
				}

				if (message.getParticipantInfoState() != ParticipantInfoState.NO_INFO
						&& mConversation instanceof GroupConversation) {
					HikeConversationsDatabase hCDB = HikeConversationsDatabase
							.getInstance();
					((GroupConversation) mConversation)
							.setGroupParticipantList(hCDB.getGroupParticipants(
									mConversation.getMsisdn(), false, false));
				}

				final String label = message.getParticipantInfoState() != ParticipantInfoState.NO_INFO ? mConversation
						.getLabel() : null;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (label != null) {
							mLabelView.setText(label);
							if (!(mConversation instanceof GroupConversation)) {
								mNameView.setText(label);
							}
						}

						addMessage(message);
					}
				});

			}
		} else if (HikePubSub.END_TYPING_CONVERSATION.equals(type)) {
			if (mContactNumber.equals(object)) {
				runOnUiThread(new SetTypingText(false));
			}
		} else if (HikePubSub.TYPING_CONVERSATION.equals(type)) {
			if (mContactNumber.equals(object)) {
				runOnUiThread(new SetTypingText(true));
				if (shouldShowLastSeen()
						&& HikeUserDatabase.getInstance().getIsOffline(
								mContactNumber) != -1) {
					/*
					 * Publishing an online event for this number.
					 */
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.LAST_SEEN_TIME_UPDATED,
							new Pair<String, Long>(mContactNumber, 0l));
				}
			}
		}
		// We only consider this case if there is a valid conversation in the
		// Chat Thread
		else if (mConversation != null
				&& HikePubSub.SMS_CREDIT_CHANGED.equals(type)) {
			mCredits = ((Integer) object).intValue();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateUIForHikeStatus();
					if (!animatedOnce) {
						animatedOnce = prefs.getBoolean(
								HikeConstants.Extras.ANIMATED_ONCE, false);
						if (!animatedOnce) {
							Editor editor = prefs.edit();
							editor.putBoolean(
									HikeConstants.Extras.ANIMATED_ONCE, true);
							editor.commit();
						}
					}

					if ((mCredits % HikeConstants.SHOW_CREDITS_AFTER_NUM == 0 || !animatedOnce)
							&& !mConversation.isOnhike()) {
						animatedOnce = true;
						showSMSCounter();
					}
				}
			});
		} else if (HikePubSub.MESSAGE_DELIVERED.equals(type)) {
			Pair<String, Long> pair = (Pair<String, Long>) object;
			// If the msisdn don't match we simply return
			if (!mConversation.getMsisdn().equals(pair.first)) {
				return;
			}
			long msgID = pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			ConvMessage msg = findMessageById(msgID);
			if (Utils.shouldChangeMessageState(msg,
					ConvMessage.State.SENT_DELIVERED.ordinal())) {
				msg.setState(ConvMessage.State.SENT_DELIVERED);
				runOnUiThread(mUpdateAdapter);
			}
		} else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type)) {
			Pair<String, long[]> pair = (Pair<String, long[]>) object;
			// If the msisdn don't match we simply return
			if (!mConversation.getMsisdn().equals(pair.first)) {
				return;
			}
			long[] ids = pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			for (int i = 0; i < ids.length; i++) {
				ConvMessage msg = findMessageById(ids[i]);
				if (Utils.shouldChangeMessageState(msg,
						ConvMessage.State.SENT_DELIVERED_READ.ordinal())) {
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				}
			}
			runOnUiThread(mUpdateAdapter);
		} else if (HikePubSub.MESSAGE_FAILED.equals(type)) {
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null) {
				msg.setState(ConvMessage.State.SENT_FAILED);
				runOnUiThread(mUpdateAdapter);
			}
		} else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type)) {
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg,
					ConvMessage.State.SENT_CONFIRMED.ordinal())) {
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				runOnUiThread(mUpdateAdapter);
			}
		} else if (HikePubSub.ICON_CHANGED.equals(type)) {
			String msisdn = (String) object;
			if (mContactNumber.equals(msisdn)) {
				/* update the image drawable */
				runOnUiThread(mUpdateAdapter);
			}
		} else if ((HikePubSub.USER_LEFT.equals(type))
				|| (HikePubSub.USER_JOINED.equals(type))) {
			/* only update the UI if the message is for this conversation */
			String msisdn = (String) object;
			if (!mContactNumber.equals(msisdn)) {
				return;
			}

			mConversation.setOnhike(HikePubSub.USER_JOINED.equals(type));
			runOnUiThread(new Runnable() {
				public void run() {
					updateUIForHikeStatus();
					mUpdateAdapter.run();
				}
			});
		} else if (HikePubSub.GROUP_NAME_CHANGED.equals(type)) {
			String groupId = (String) object;
			if (mContactNumber.equals(groupId)) {
				HikeConversationsDatabase db = HikeConversationsDatabase
						.getInstance();
				final String groupName = db.getGroupName(groupId);
				mConversation.setContactName(groupName);

				runOnUiThread(new Runnable() {
					public void run() {
						mLabelView.setText(groupName);
					}
				});
			}
		} else if (HikePubSub.GROUP_END.equals(type)) {
			String groupId = ((JSONObject) object).optString(HikeConstants.TO);
			if (mContactNumber.equals(groupId)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						toggleGroupLife(false);
					}
				});
			}
		} else if (HikePubSub.CONTACT_ADDED.equals(type)) {
			ContactInfo contactInfo = (ContactInfo) object;
			if (contactInfo == null) {
				return;
			}

			if (this.mContactNumber.equals(contactInfo.getMsisdn())) {
				this.mContactName = contactInfo.getName();
				mConversation.setContactName(this.mContactName);
				this.mLabel = contactInfo.getName();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mLabelView.setText(mLabel);
						mNameView.setText(mLabel);
					}
				});
			}
		} else if (HikePubSub.UPLOAD_FINISHED.equals(type)) {
			ConvMessage convMessage = (ConvMessage) object;
			if (!convMessage.getMsisdn().equals(this.mContactNumber)) {
				return;
			}
			ConvMessage adapterMessage = findMessageById(convMessage.getMsgID());
			if (adapterMessage != null) {
				try {
					adapterMessage.setMetadata(convMessage.getMetadata()
							.getJSON());
				} catch (JSONException e) {
					Log.e(getClass().getSimpleName(), "Invalid JSON", e);
				}
			}
			runOnUiThread(mUpdateAdapter);
		} else if (HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED.equals(type)) {
			runOnUiThread(mUpdateAdapter);
		} else if (HikePubSub.FILE_MESSAGE_CREATED.equals(type)) {
			final ConvMessage convMessage = (ConvMessage) object;
			selectedFile = null;

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					addMessage(convMessage);
				}
			});
		} else if (HikePubSub.MUTE_CONVERSATION_TOGGLED.equals(type)) {
			Pair<String, Boolean> groupMute = (Pair<String, Boolean>) object;
			if (!groupMute.first.equals(this.mContactNumber)) {
				return;
			}
			final Boolean isMuted = groupMute.second;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					toggleConversationMuteViewVisibility(isMuted);
				}
			});
		} else if (HikePubSub.BLOCK_USER.equals(type)
				|| HikePubSub.UNBLOCK_USER.equalsIgnoreCase(type)) {
			String msisdn = (String) object;
			final boolean blocked = HikePubSub.BLOCK_USER.equals(type);

			if (!msisdn.equals(getMsisdnMainUser())) {
				return;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (blocked) {
						blockUser();
					} else {
						unblockUser();
					}
				}
			});
		} else if (HikePubSub.REMOVE_MESSAGE_FROM_CHAT_THREAD.equals(type)) {
			final ConvMessage convMessage = (ConvMessage) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					removeMessage(convMessage);
				}
			});
		} else if (HikePubSub.GROUP_REVIVED.equals(type)) {
			String groupId = (String) object;
			if (mContactNumber.equals(groupId)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						toggleGroupLife(true);
					}
				});
			}
		} else if (HikePubSub.CHANGED_MESSAGE_TYPE.equals(type)) {
			updateAdapter();
		} else if (HikePubSub.SHOW_SMS_SYNC_DIALOG.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					smsDialog = Utils.showSMSSyncDialog(ChatThread.this, true);
					dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG;
				}
			});
		} else if (HikePubSub.SMS_SYNC_COMPLETE.equals(type)
				|| HikePubSub.SMS_SYNC_FAIL.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (smsDialog != null) {
						smsDialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		} else if (HikePubSub.SMS_SYNC_START.equals(type)) {
			dialogShowing = DialogShowing.SMS_SYNCING_DIALOG;
		} else if (HikePubSub.SHOWN_UNDELIVERED_MESSAGE.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mConversationsView.setSelection(messages.size() - 1);
				}
			});
		} else if (HikePubSub.STICKER_DOWNLOADED.equals(type)) {
			updateAdapter();
		} else if (HikePubSub.STICKER_CATEGORY_DOWNLOADED.equals(type)
				|| HikePubSub.STICKER_CATEGORY_DOWNLOAD_FAILED.equals(type)) {
			if (emoticonType == EmoticonType.STICKERS) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Pair<Integer, DownloadType> taskData = (Pair<Integer, DownloadType>) object;
						int categoryIndex = taskData.first;
						DownloadType downloadType = taskData.second;

						updateStickerCategoryUI(categoryIndex,
								HikePubSub.STICKER_CATEGORY_DOWNLOAD_FAILED
										.equals(type), downloadType);
					}
				});
			}
		} else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type)) {
			Pair<String, Long> lastSeenPair = (Pair<String, Long>) object;
			String msisdn = lastSeenPair.first;
			long lastSeenTime = lastSeenPair.second;

			if (!mContactNumber.equals(msisdn)
					|| (mConversation instanceof GroupConversation)
					|| !shouldShowLastSeen()) {
				return;
			}
			final String lastSeenString = Utils.getLastSeenTimeAsString(this,
					lastSeenTime);
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (lastSeenString == null) {
						lastSeenContainer.setVisibility(View.GONE);
						mLabelView.setVisibility(View.VISIBLE);
					} else {
						mLabelView.setVisibility(View.INVISIBLE);
						lastSeenContainer.setVisibility(View.VISIBLE);
						mLastSeenView.setText(lastSeenString);

						if (tipView == null
								&& !prefs.getBoolean(
										HikeMessengerApp.SHOWN_LAST_SEEN_TIP,
										false)) {
							tipView = findViewById(R.id.last_seen_tip);
							Utils.showTip(ChatThread.this, TipType.LAST_SEEN,
									tipView);
						}
					}
				}
			});
		}
	}

	private void updateAdapter() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mUpdateAdapter.run();
			}
		});
	}

	private ConvMessage findMessageById(long msgID) {
		if (mAdapter == null) {
			return null;
		}

		int count = mAdapter.getCount();
		for (int i = 0; i < count; ++i) {
			ConvMessage msg = mAdapter.getItem(i);
			if (msg == null) {
				continue;
			}
			if (msg.getMsgID() == msgID) {
				return msg;
			}
		}
		return null;
	}

	public String getContactNumber() {
		return mContactNumber;
	}

	@Override
	public void afterTextChanged(Editable editable) {
		if (mConversation == null) {
			return;
		}

		/* only update the chat metadata if this is an SMS chat */
		if (!mConversation.isOnhike()) {
			updateChatMetadata();
		}
	}

	/* must be called on the UI Thread */
	private void updateChatMetadata() {
		mMetadataNumChars.setVisibility(View.VISIBLE);
		if (mCredits <= 0) {
			zeroCredits();
		} else {
			nonZeroCredits();

			if (mComposeView.getLineCount() > 2) {
				mMetadataNumChars.setVisibility(View.VISIBLE);
				int length = mComposeView.getText().length();
				// set the max sms length to a length appropriate to the number
				// of characters we have
				int charNum = length % 140;
				int numSms = ((int) (length / 140)) + 1;
				String charNumString = Integer.toString(charNum);
				SpannableString ss = new SpannableString(charNumString + "/#"
						+ Integer.toString(numSms));
				ss.setSpan(
						new ForegroundColorSpan(getResources().getColor(
								R.color.send_green)), 0,
						charNumString.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				mMetadataNumChars.setText(ss);
			} else {
				mMetadataNumChars.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void zeroCredits() {
		Log.d(getClass().getSimpleName(), "Zero credits");
		mSendBtn.setEnabled(false);

		if (!TextUtils.isEmpty(mComposeView.getText())) {
			mComposeView.setText("");
		}
		if (!(mConversation instanceof GroupConversation)) {
			mComposeView.setHint("0 Free SMS left...");
			mComposeView.setEnabled(false);
			findViewById(R.id.title_image_btn2).setEnabled(false);
			findViewById(R.id.info_layout).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.group_info_layout).setVisibility(View.VISIBLE);
		}

		boolean show = mConversationDb.wasOverlayDismissed(mConversation
				.getMsisdn());
		if (!show) {
			showOverlay(false);
		}
	}

	private void nonZeroCredits() {
		Log.d(getClass().getSimpleName(), "Non Zero credits");
		if (!mComposeView.isEnabled()) {
			if (!TextUtils.isEmpty(mComposeView.getText())) {
				mComposeView.setText("");
			}
			if (mConversation instanceof GroupConversation) {
				mComposeView.setHint(R.string.group_msg);
			} else if (mConversation.isOnhike()) {
				mComposeView.setHint(R.string.hike_msg);
			} else {
				mComposeView.setHint(R.string.sms_msg);
			}
			mComposeView.setEnabled(true);
		}
		findViewById(R.id.title_image_btn2).setEnabled(true);
		findViewById(
				(mConversation instanceof GroupConversation) ? R.id.group_info_layout
						: R.id.info_layout).setVisibility(View.GONE);

		if (!blockOverlay) {
			hideOverlay();
		}
	}

	private void showSMSCounter() {
		slideUp = AnimationUtils.loadAnimation(ChatThread.this,
				R.anim.slide_up_noalpha);
		slideUp.setDuration(2000);

		slideDown = AnimationUtils.loadAnimation(ChatThread.this,
				R.anim.slide_down_noalpha);
		slideDown.setDuration(2000);
		slideDown.setStartOffset(2000);

		if (smsCount == null) {
			smsCount = (TextView) findViewById(R.id.sms_counter);
		}
		smsCount.setAnimation(slideUp);
		smsCount.setVisibility(View.VISIBLE);
		smsCount.setText(mCredits + " " + getString(R.string.sms_left));

		slideUp.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				smsCount.setAnimation(slideDown);
				smsCount.setVisibility(View.INVISIBLE);
			}
		});
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int before,
			int count) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent keyEvent) {
		if (mConversation == null) {
			return false;
		}

		if ((view == mComposeView)
				&& ((actionId == EditorInfo.IME_ACTION_SEND) || ((keyEvent != null)
						&& (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
						&& (keyEvent.getAction() != KeyEvent.ACTION_UP) && (config.keyboard != Configuration.KEYBOARD_NOKEYS)))) {
			boolean ret = mSendBtn.performClick();
			Utils.hideSoftKeyboard(this, mComposeView);
			return ret;
		}
		return false;
	}

	private void addMessage(ConvMessage convMessage) {
		if (messages != null && mAdapter != null) {
			boolean wasShowingTypingItem = false;
			/*
			 * If we were showing the typing bubble, we remove it from the add
			 * the new message and add the typing bubble back again
			 */
			if (!messages.isEmpty()
					&& messages.get(messages.size() - 1) == null) {
				messages.remove(messages.size() - 1);
				wasShowingTypingItem = true;
			}
			mAdapter.addMessage(convMessage);

			// Reset this boolean to load more messages when the user scrolls to
			// the top
			reachedEnd = false;

			if (convMessage != null && convMessage.isSent()
					&& wasShowingTypingItem) {
				mAdapter.addMessage(null);
			}
			mAdapter.notifyDataSetChanged();

			/*
			 * Don't scroll to bottom if the user is at older messages. It's
			 * possible that the user might be reading them.
			 */
			if (((convMessage != null && !convMessage.isSent()) || convMessage == null)
					&& mConversationsView.getLastVisiblePosition() < messages
							.size() - 4) {
				return;
			}

			// Smooth scroll by the minimum distance in the opposite direction,
			// to fix the bug where the list does not scroll at all.
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

	private void removeMessage(ConvMessage convMessage) {
		boolean lastMessage = convMessage
				.equals(messages.get(messages.size() - 1));
		mPubSub.publish(HikePubSub.DELETE_MESSAGE,
				new Pair<ConvMessage, Boolean>(convMessage, lastMessage));
		mAdapter.removeMessage(convMessage);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onShown() {
		if (messages != null) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mConversationsView.setSelection(messages.size() - 1);
				}
			});
		}
		Log.d(getClass().getSimpleName(), "Keyboard shown");
		if (emoticonLayout != null
				&& emoticonLayout.getVisibility() == View.VISIBLE) {
			onEmoticonBtnClicked(null);
		}
	}

	@Override
	public void onHidden() {
	}

	public void onInviteButtonClick(View v) {
		inviteUser();
	}

	private void showOverlay(boolean blockOverlay) {
		this.blockOverlay = blockOverlay;

		Utils.hideSoftKeyboard(this, mComposeView);

		if (mOverlayLayout.getVisibility() != View.VISIBLE && !isOverlayShowing
				&& hasWindowFocus()) {
			Animation fadeIn = AnimationUtils.loadAnimation(ChatThread.this,
					android.R.anim.fade_in);
			mOverlayLayout.setAnimation(fadeIn);
		}
		mOverlayLayout.setVisibility(View.VISIBLE);
		// To prevent the views in the background from being clickable
		mOverlayLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});

		TextView message = (TextView) mOverlayLayout
				.findViewById(R.id.overlay_message);
		Button overlayBtn = (Button) mOverlayLayout
				.findViewById(R.id.overlay_button);
		ImageView overlayImg = (ImageView) mOverlayLayout
				.findViewById(R.id.overlay_image);

		mComposeView.setEnabled(false);
		String label = mConversation instanceof GroupConversation ? ((GroupConversation) mConversation)
				.getGroupParticipant(getMsisdnMainUser()).getContactInfo()
				.getFirstName()
				: mLabel;
		String formatString;
		if (blockOverlay) {
			overlayImg.setImageResource(R.drawable.ic_no);
			formatString = getResources().getString(
					R.string.block_overlay_message);
			overlayBtn.setText(R.string.unblock_title);
		} else {
			mConversationDb.setOverlay(false, mConversation.getMsisdn());
			formatString = getResources()
					.getString(
							mConversation instanceof GroupConversation ? R.string.no_credits_gc
									: R.string.no_credits);
			overlayImg.setImageResource(R.drawable.ic_no_credits);
			overlayBtn
					.setText(mConversation instanceof GroupConversation ? R.string.invite_to_hike
							: R.string.invite_now);
			mOverlayLayout.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Utils.logEvent(ChatThread.this,
							HikeConstants.LogEvent.INVITE_OVERLAY_DISMISS);
					onOverlayButtonClick(mOverlayLayout);
				}
			});
		}
		/* bold the blocked users name */
		String formatted = String.format(formatString, label);
		SpannableString str = new SpannableString(formatted);
		if (!(mConversation instanceof GroupConversation) || blockOverlay) {
			int start = formatString.indexOf("%1$s");
			str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start,
					start + label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		message.setText(str);
	}

	public void onTitleIconClick(View v) {

		if (v.getId() == R.id.title_image_btn) {
			if (!(mConversation instanceof GroupConversation)) {
				String userMsisdn = prefs.getString(
						HikeMessengerApp.MSISDN_SETTING, "");

				Intent intent = new Intent();
				intent.setClass(ChatThread.this, ProfileActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				if (!userMsisdn.equals(mContactNumber)) {
					intent.putExtra(HikeConstants.Extras.CONTACT_INFO,
							mContactNumber);
					intent.putExtra(HikeConstants.Extras.ON_HIKE,
							mConversation.isOnhike());
				}
				startActivity(intent);
			} else {
				if (!((GroupConversation) mConversation).getIsGroupAlive()) {
					return;
				}

				Utils.logEvent(ChatThread.this,
						HikeConstants.LogEvent.GROUP_INFO_TOP_BUTTON);
				Intent intent = new Intent();
				intent.setClass(ChatThread.this, ProfileActivity.class);
				intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
				intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT,
						this.mConversation.getMsisdn());
				startActivity(intent);

				overridePendingTransition(R.anim.slide_in_right_noalpha,
						R.anim.slide_out_left_noalpha);
			}
		} else if (v.getId() == R.id.info_layout
				|| v.getId() == R.id.group_info_layout) {
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.I_BUTTON);
			showOverlay(false);
		} else if (v.getId() == R.id.title_icon) {
			String groupId = getIntent().getStringExtra(
					HikeConstants.Extras.EXISTING_GROUP_CHAT);
			boolean newGroup = false;

			if (TextUtils.isEmpty(groupId)) {
				// Create new group
				String uid = getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
						.getString(HikeMessengerApp.UID_SETTING, "");
				mContactNumber = uid + ":" + System.currentTimeMillis();
				newGroup = true;
			} else {
				// Group alredy exists. Fetch existing participants.
				mContactNumber = groupId;
				newGroup = false;
			}
			String selectedContacts = this.mInputNumberView.getText()
					.toString();
			selectedContacts = selectedContacts.substring(0, selectedContacts
					.lastIndexOf(HikeConstants.GROUP_PARTICIPANT_SEPARATOR));
			List<String> selectedParticipants = Utils
					.splitSelectedContacts(selectedContacts);
			List<String> selectedParticipantNames = Utils
					.splitSelectedContactsName(selectedContacts);
			Map<String, GroupParticipant> participantList = new HashMap<String, GroupParticipant>();

			for (int i = 0; i < selectedParticipants.size(); i++) {
				String msisdn = selectedParticipants.get(i);
				String name = selectedParticipantNames.get(i);
				ContactInfo contactInfo = HikeUserDatabase.getInstance()
						.getContactInfoFromMSISDN(msisdn, false);
				contactInfo.setName(name);
				GroupParticipant groupParticipant = new GroupParticipant(
						contactInfo);
				participantList.put(msisdn, groupParticipant);
			}
			ContactInfo userContactInfo = Utils.getUserContactInfo(prefs);

			GroupConversation groupConversation = new GroupConversation(
					mContactNumber, 0, null, userContactInfo.getMsisdn(), true);
			groupConversation.setGroupParticipantList(participantList);

			Log.d(getClass().getSimpleName(), "Creating group: "
					+ mContactNumber);
			mConversationDb.addGroupParticipants(mContactNumber,
					groupConversation.getGroupParticipantList());
			if (newGroup) {
				mConversationDb.addConversation(groupConversation.getMsisdn(),
						false, "", groupConversation.getGroupOwner());
			}

			try {
				// Adding this boolean value to show a different system message
				// if its a new group
				JSONObject gcjPacket = groupConversation
						.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
				gcjPacket.put(HikeConstants.NEW_GROUP, newGroup);

				sendMessage(new ConvMessage(gcjPacket, groupConversation,
						ChatThread.this, true));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, groupConversation
					.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN));
			createConversation();
			mComposeViewWatcher.init();
			mComposeView.requestFocus();

			mContactName = groupConversation.getLabel();

			// To prevent the Contact picker layout from being shown on
			// orientation change
			setIntentFromField();
		} else if (v.getId() == R.id.title_image_btn2) {
			showFilePickerDialog(Utils.getExternalStorageState());
		}
	}

	private void toggleConversationMuteViewVisibility(boolean isMuted) {
		findViewById(R.id.conversation_mute).setVisibility(
				isMuted ? View.VISIBLE : View.GONE);
	}

	private void showFilePickerDialog(
			final ExternalStorageState externalStorageState) {

		final boolean canShareLocation = getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_LOCATION);

		final boolean canShareContacts = mConversation.isOnhike();

		ArrayList<String> optionsList = new ArrayList<String>();

		optionsList.add(getString(R.string.camera));
		optionsList.add(getString(R.string.choose_photo));
		optionsList.add(getString(R.string.choose_video));
		optionsList.add(getString(R.string.choose_audio));
		if (canShareLocation) {
			optionsList.add(getString(R.string.share_location));
		}
		if (canShareContacts) {
			optionsList.add(getString(R.string.contact_info));
		}

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		ArrayList<Integer> optionImagesList = new ArrayList<Integer>();
		optionImagesList.add(R.drawable.ic_image_capture_item);
		optionImagesList.add(R.drawable.ic_image_item);
		optionImagesList.add(R.drawable.ic_video_item);
		optionImagesList.add(R.drawable.ic_music_item);
		if (canShareLocation) {
			optionImagesList.add(R.drawable.ic_share_location_item);
		}
		if (canShareContacts) {
			optionImagesList.add(R.drawable.ic_contact_item);
		}
		final Integer[] optionIcons = new Integer[optionImagesList.size()];
		optionImagesList.toArray(optionIcons);

		AlertDialog.Builder builder = new AlertDialog.Builder(ChatThread.this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this,
				R.layout.alert_item, R.id.item, options) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(R.id.item);
				tv.setCompoundDrawablesWithIntrinsicBounds(
						optionIcons[position], 0, 0, 0);
				return v;
			}

		};

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int requestCode;
						Intent pickIntent = new Intent();
						Intent newMediaFileIntent = null;
						/*
						 * If we're not doing a location/contact transfer, we
						 * need an external storage
						 */
						if (which != 5 && which != 6) {
							if (externalStorageState == ExternalStorageState.NONE) {
								Toast.makeText(getApplicationContext(),
										R.string.no_external_storage,
										Toast.LENGTH_SHORT).show();
								return;
							}
						}

						switch (which) {
						case 0:
							requestCode = HikeConstants.IMAGE_CAPTURE_CODE;
							pickIntent = new Intent(
									MediaStore.ACTION_IMAGE_CAPTURE);
							selectedFile = Utils.getOutputMediaFile(
									HikeFileType.IMAGE, null, null);

							pickIntent.putExtra(MediaStore.EXTRA_OUTPUT,
									Uri.fromFile(selectedFile));
							/*
							 * For images, save the file path as a preferences
							 * since in some devices the reference to the file
							 * becomes null.
							 */
							Editor editor = prefs.edit();
							editor.putString(HikeMessengerApp.FILE_PATH,
									selectedFile.getAbsolutePath());
							editor.commit();

							break;
						case 2:
							requestCode = HikeConstants.VIDEO_TRANSFER_CODE;
							pickIntent.setType("video/*");
							newMediaFileIntent = new Intent(
									MediaStore.ACTION_VIDEO_CAPTURE);
							newMediaFileIntent.putExtra(
									MediaStore.EXTRA_SIZE_LIMIT,
									(long) (0.9 * HikeConstants.MAX_FILE_SIZE));
							break;

						case 3:
							requestCode = HikeConstants.AUDIO_TRANSFER_CODE;
							break;

						case 4:
							if (canShareLocation) {
								requestCode = HikeConstants.SHARE_LOCATION_CODE;
								break;
							}
						case 5:
							requestCode = HikeConstants.SHARE_CONTACT_CODE;
							break;

						case 1:
						default:
							requestCode = HikeConstants.IMAGE_TRANSFER_CODE;
							pickIntent.setType("image/*");
							break;
						}
						if (requestCode == HikeConstants.SHARE_LOCATION_CODE) {
							startActivityForResult(new Intent(ChatThread.this,
									ShareLocation.class), requestCode);
							return;
						} else if (requestCode == HikeConstants.AUDIO_TRANSFER_CODE) {
							showAudioDialog();
							return;
						} else if (requestCode == HikeConstants.SHARE_CONTACT_CODE) {
							pickIntent = new Intent(Intent.ACTION_PICK,
									Contacts.CONTENT_URI);
							startActivityForResult(pickIntent, requestCode);
							return;
						}
						Intent chooserIntent;
						if (requestCode != HikeConstants.IMAGE_CAPTURE_CODE) {
							pickIntent.setAction(Intent.ACTION_PICK);

							chooserIntent = Intent
									.createChooser(pickIntent, "");
						} else {
							chooserIntent = pickIntent;
						}

						if (externalStorageState == ExternalStorageState.WRITEABLE) {
							/*
							 * Cannot send a file for new videos because of an
							 * android issue
							 * http://stackoverflow.com/questions/10494839
							 * /verifiyandsetparameter
							 * -error-when-trying-to-record-video
							 */
							if (requestCode == HikeConstants.IMAGE_CAPTURE_CODE) {
								if (selectedFile == null) {
									Log.w(getClass().getSimpleName(),
											"Unable to create file to store media.");
									Toast.makeText(
											ChatThread.this,
											ChatThread.this
													.getResources()
													.getString(
															R.string.no_external_storage),
											Toast.LENGTH_LONG).show();
									return;
								}
							}
							if (newMediaFileIntent != null) {
								chooserIntent.putExtra(
										Intent.EXTRA_INITIAL_INTENTS,
										new Intent[] { newMediaFileIntent });
							}
						}
						Editor editor = prefs.edit();
						editor.putString(HikeMessengerApp.TEMP_NUM,
								mContactNumber);
						editor.putString(HikeMessengerApp.TEMP_NAME,
								mContactName);
						editor.commit();
						startActivityForResult(chooserIntent, requestCode);
					}
				});

		filePickerDialog = builder.create();
		((AlertDialog) filePickerDialog).getListView().setDivider(
				getResources()
						.getDrawable(R.drawable.ic_thread_divider_profile));
		filePickerDialog.show();
	}

	private class AudioActivityInfo {
		CharSequence label;
		Drawable icon;
		String packageName;
		String activityName;

		public AudioActivityInfo(CharSequence label, Drawable icon,
				String packageName, String activityName) {
			this.label = label;
			this.icon = icon;
			this.packageName = packageName;
			this.activityName = activityName;
		}
	}

	private void showAudioDialog() {
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/*");
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(
				intent, 0);
		final List<AudioActivityInfo> audioActivityList = new ArrayList<AudioActivityInfo>();
		int maxSize = Math.min(list.size(), 2);
		for (int i = 0; i < maxSize; i++) {
			ActivityInfo activityInfo = list.get(i).activityInfo;
			audioActivityList.add(new AudioActivityInfo(getPackageManager()
					.getApplicationLabel(activityInfo.applicationInfo),
					getPackageManager().getApplicationIcon(
							activityInfo.applicationInfo),
					activityInfo.packageName, activityInfo.name));
		}
		Builder builder = new Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<AudioActivityInfo>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				audioActivityList) {

			public View getView(int position, View convertView, ViewGroup parent) {
				AudioActivityInfo audioActivityInfo = getItem(position);
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setText(audioActivityInfo.label);
				tv.setCompoundDrawablesWithIntrinsicBounds(
						audioActivityInfo.icon, null, null, null);
				tv.setCompoundDrawablePadding((int) (15 * Utils.densityMultiplier));
				return v;
			}
		};
		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						AudioActivityInfo audioActivityInfo = audioActivityList
								.get(which);
						intent.setClassName(audioActivityInfo.packageName,
								audioActivityInfo.activityName);
						startActivityForResult(intent,
								HikeConstants.AUDIO_TRANSFER_CODE);
					}
				});

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	private enum RecorderState {
		IDLE, RECORDING, RECORDED, PLAYING
	}

	/**
	 * Method for displaying the record audio dialog.
	 * 
	 * @param startTime
	 *            If the recording was already ongoing when this method is
	 *            called, this parameter denotes the time the recording was
	 *            started
	 */
	private void showRecordingDialog() {
		recordingDialog = new Dialog(ChatThread.this,
				R.style.Theme_CustomDialog);

		recordingDialog.setContentView(R.layout.record_audio_dialog);

		final TextView recordInfo = (TextView) recordingDialog
				.findViewById(R.id.record_info);
		final ImageView recordImage = (ImageView) recordingDialog
				.findViewById(R.id.record_img);
		final Button cancelBtn = (Button) recordingDialog
				.findViewById(R.id.cancel_btn);
		final Button sendBtn = (Button) recordingDialog
				.findViewById(R.id.send_btn);
		final ImageButton recordBtn = (ImageButton) recordingDialog
				.findViewById(R.id.btn_record);

		recordBtn.setEnabled(true);

		recordBtn.setImageResource(R.drawable.ic_record);
		sendBtn.setEnabled(false);

		recordingHandler = new Handler();

		recorderState = RecorderState.IDLE;
		// Recording already onGoing
		if (recorder != null) {
			initialiseRecorder(recordBtn, recordInfo, recordImage, cancelBtn,
					sendBtn);
			setupRecordingView(recordInfo, recordImage, recordStartTime);
		}
		// Player is playing the recording
		else if (player != null && selectedFile != null) {
			try {
				initialisePlayer(recordBtn, recordInfo, recordImage, sendBtn);
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(),
						"Error while playing the recording", e);
				Toast.makeText(getApplicationContext(),
						R.string.error_play_recording, Toast.LENGTH_SHORT)
						.show();
				setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage,
						sendBtn, 0);
				stopPlayer();
			}
			setUpPlayingRecordingLayout(recordBtn, recordInfo, recordImage,
					sendBtn, recordStartTime);
		}
		// Recording has been stopped and we have a valid file to be sent
		else if (recorder == null && selectedFile != null) {
			setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage,
					sendBtn, recordedTime);
		}

		recordBtn.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				if (recorderState == RecorderState.RECORDED
						|| recorderState == RecorderState.PLAYING) {
					return false;
				}
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					if (recording) {
						return false;
					}
					recordBtn.setPressed(true);
					// New recording
					if (recorder == null) {
						initialiseRecorder(recordBtn, recordInfo, recordImage,
								cancelBtn, sendBtn);
					}
					try {
						recorder.prepare();
						recorder.start();
						recordStartTime = System.currentTimeMillis();
						setupRecordingView(recordInfo, recordImage,
								recordStartTime);
					} catch (IOException e) {
						stopRecorder();
						recordingError(true);
						Log.e(getClass().getSimpleName(),
								"Failed to start recording", e);
					}
					recording = true;

					Utils.blockOrientationChange(ChatThread.this);
					getWindow().addFlags(
							WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					return true;
				case MotionEvent.ACTION_UP:
					if (!recording) {
						return false;
					}
					mHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							recordBtn.setPressed(false);
							recording = false;
							stopRecorder();
							recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
							setUpPreviewRecordingLayout(recordBtn, recordInfo,
									recordImage, sendBtn, recordedTime);
							getWindow()
									.clearFlags(
											WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
							Utils.unblockOrientationChange(ChatThread.this);
						}
					}, 1);

					return true;
				}
				return false;
			}
		});
		// recordBtn.setOnClickListener(new OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// switch (recorderState) {
		// case RECORDED:
		// try {
		// initialisePlayer(recordBtn, recordImage, sendBtn);
		// player.prepare();
		// player.start();
		// } catch (IOException e) {
		// Log.e(getClass().getSimpleName(),
		// "Error while playing the recording", e);
		// Toast.makeText(getApplicationContext(),
		// R.string.error_play_recording,
		// Toast.LENGTH_SHORT).show();
		// setUpPreviewRecordingLayout(recordBtn, recordImage,
		// sendBtn);
		// stopPlayer();
		// }
		// setUpPlayingRecordingLayout(recordBtn, recordInfo,
		// recordImage, sendBtn, System.currentTimeMillis());
		// break;
		// case PLAYING:
		// stopPlayer();
		// setUpPreviewRecordingLayout(recordBtn, recordImage, sendBtn);
		// break;
		// }
		// }
		// });

		cancelBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				recordingDialog.cancel();
			}
		});

		sendBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				recordingDialog.dismiss();
				initialiseFileTransfer(selectedFile.getPath(),
						HikeFileType.AUDIO_RECORDING,
						HikeConstants.VOICE_MESSAGE_CONTENT_TYPE, true,
						recordedTime, false);
			}
		});

		recordingDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				stopRecorder();
				stopPlayer();
				recordingError(false);
			}
		});

		recordingDialog.show();
	}

	private void initialiseRecorder(final ImageButton recordBtn,
			final TextView recordInfo, final ImageView recordImage,
			final Button cancelBtn, final Button sendBtn) {
		if (recorder == null) {
			recorder = new MediaRecorder();
			recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			recorder.setMaxDuration(HikeConstants.MAX_DURATION_RECORDING_SEC * 1000);
			recorder.setMaxFileSize(HikeConstants.MAX_FILE_SIZE);
			selectedFile = Utils.getOutputMediaFile(
					HikeFileType.AUDIO_RECORDING, null, null);
			recorder.setOutputFile(selectedFile.getPath());
		}
		recorder.setOnErrorListener(new OnErrorListener() {
			@Override
			public void onError(MediaRecorder mr, int what, int extra) {
				stopRecorder();
				recordingError(true);
			}
		});
		recorder.setOnInfoListener(new OnInfoListener() {
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				stopRecorder();
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
						|| what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
					setUpPreviewRecordingLayout(recordBtn, recordInfo,
							recordImage, sendBtn, recordedTime);
				} else {
					recordingError(true);
				}
			}
		});
	}

	private void initialisePlayer(final ImageButton recordBtn,
			final TextView recordInfo, final ImageView recordImage,
			final Button sendBtn) throws IOException {
		if (player == null) {
			player = new MediaPlayer();
			player.setDataSource(selectedFile.getPath());
		}

		player.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage,
						sendBtn, recordedTime);
				stopPlayer();
			}
		});
	}

	private void stopPlayer() {
		if (updateRecordingDuration != null) {
			updateRecordingDuration.stopUpdating();
			updateRecordingDuration = null;
		}
		if (player != null) {
			player.stop();
			player.reset();
			player.release();
			player = null;
		}
	}

	private void setupRecordingView(TextView recordInfo, ImageView recordImage,
			long startTime) {
		recorderState = RecorderState.RECORDING;

		updateRecordingDuration = new UpdateRecordingDuration(recordInfo,
				recordImage, startTime, R.drawable.ic_recording);
		recordingHandler.post(updateRecordingDuration);
	}

	private void setUpPreviewRecordingLayout(ImageButton recordBtn,
			TextView recordText, ImageView recordImage, Button sendBtn,
			long duration) {
		recorderState = RecorderState.RECORDED;

		recordBtn.setEnabled(false);
		recordImage.setImageResource(R.drawable.ic_recorded);
		Utils.setupFormattedTime(recordText, duration);

		sendBtn.setEnabled(true);
	}

	private void setUpPlayingRecordingLayout(ImageButton recordBtn,
			TextView recordInfo, ImageView recordImage, Button sendBtn,
			long startTime) {
		recorderState = RecorderState.PLAYING;

		sendBtn.setEnabled(true);

		updateRecordingDuration = new UpdateRecordingDuration(recordInfo,
				recordImage, startTime, 0);
		recordingHandler.post(updateRecordingDuration);
	}

	private class UpdateRecordingDuration implements Runnable {
		private long startTime;
		private TextView durationText;
		private ImageView recordImage;
		private boolean keepUpdating = true;
		private boolean imageSet = false;
		private int imageRes;

		public UpdateRecordingDuration(TextView durationText, ImageView iv,
				long startTime, int imageRes) {
			this.durationText = durationText;
			this.recordImage = iv;
			this.startTime = startTime;
			this.imageRes = imageRes;
		}

		public void stopUpdating() {
			keepUpdating = false;
		}

		public long getStartTime() {
			return startTime;
		}

		@Override
		public void run() {
			long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
			Utils.setupFormattedTime(durationText, timeElapsed);
			if (!imageSet) {
				recordImage.setImageResource(imageRes);
				imageSet = true;
			}
			if (keepUpdating) {
				recordingHandler.postDelayed(updateRecordingDuration, 500);
			}
		}
	};

	private void stopRecorder() {
		if (updateRecordingDuration != null) {
			updateRecordingDuration.stopUpdating();
			updateRecordingDuration = null;
		}
		if (recorder != null) {
			recorder.stop();
			recorder.reset();
			recorder.release();
			recorder = null;
		}
		recording = false;
	}

	private void recordingError(boolean showError) {
		recorderState = RecorderState.IDLE;

		if (showError) {
			Toast.makeText(getApplicationContext(), R.string.error_recording,
					Toast.LENGTH_SHORT).show();
		}
		if (selectedFile == null) {
			return;
		}
		if (selectedFile.exists()) {
			selectedFile.delete();
			selectedFile = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == HikeConstants.IMAGE_CAPTURE_CODE
				|| requestCode == HikeConstants.IMAGE_TRANSFER_CODE
				|| requestCode == HikeConstants.VIDEO_TRANSFER_CODE || requestCode == HikeConstants.AUDIO_TRANSFER_CODE)
				&& resultCode == RESULT_OK) {
			if (requestCode == HikeConstants.IMAGE_CAPTURE_CODE) {
				selectedFile = new File(prefs.getString(
						HikeMessengerApp.FILE_PATH, ""));

				Editor editor = prefs.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
			}
			if (data == null
					&& (selectedFile == null || !selectedFile.exists())) {
				Toast.makeText(getApplicationContext(), R.string.error_capture,
						Toast.LENGTH_SHORT).show();
				return;
			}

			HikeFileType hikeFileType = (requestCode == HikeConstants.IMAGE_TRANSFER_CODE || requestCode == HikeConstants.IMAGE_CAPTURE_CODE) ? HikeFileType.IMAGE
					: requestCode == HikeConstants.VIDEO_TRANSFER_CODE ? HikeFileType.VIDEO
							: HikeFileType.AUDIO;

			String filePath = null;
			if (data == null || data.getData() == null) {
				filePath = selectedFile.getAbsolutePath();
			} else {
				Uri selectedFileUri = Utils.makePicasaUri(data.getData());

				if (Utils.isPicasaUri(selectedFileUri.toString())) {
					// Picasa image
					UploadFileTask uploadFileTask = new UploadFileTask(
							selectedFileUri, hikeFileType, mContactNumber,
							getApplicationContext(), mConversation);
					uploadFileTask.execute();
					return;
				} else {
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart)) {
						selectedFile = new File(URI.create(fileUriString));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						filePath = selectedFile.getAbsolutePath();
					} else {
						filePath = Utils.getRealPathFromUri(selectedFileUri,
								this);
					}
					Log.d(getClass().getSimpleName(), "File path: " + filePath);
				}
			}

			initialiseFileTransfer(filePath, hikeFileType, null, false, -1,
					false);
		} else if (requestCode == HikeConstants.SHARE_LOCATION_CODE
				&& resultCode == RESULT_OK) {
			double latitude = data.getDoubleExtra(
					HikeConstants.Extras.LATITUDE, 0);
			double longitude = data.getDoubleExtra(
					HikeConstants.Extras.LONGITUDE, 0);
			int zoomLevel = data
					.getIntExtra(HikeConstants.Extras.ZOOM_LEVEL, 0);

			Log.d(getClass().getSimpleName(), "Share Location Lat: " + latitude
					+ " long:" + longitude + " zoom: " + zoomLevel);
			initialiseLocationTransfer(latitude, longitude, zoomLevel);
		} else if (requestCode == HikeConstants.SHARE_CONTACT_CODE
				&& resultCode == RESULT_OK) {
			String id = data.getData().getLastPathSegment();
			getContactData(id);
		} else if (resultCode == RESULT_CANCELED) {
			clearTempData();
			Log.d(getClass().getSimpleName(), "File transfer Cancelled");
			selectedFile = null;
		}
	}

	private void getContactData(String id) {
		StringBuilder mimeTypes = new StringBuilder("(");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Phone.CONTENT_ITEM_TYPE)
				+ ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Email.CONTENT_ITEM_TYPE)
				+ ",");
		mimeTypes.append(DatabaseUtils
				.sqlEscapeString(StructuredPostal.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Event.CONTENT_ITEM_TYPE)
				+ ")");

		String selection = Data.CONTACT_ID + " =? AND " + Data.MIMETYPE
				+ " IN " + mimeTypes.toString();

		String[] projection = new String[] { Data.DATA1, Data.DATA2,
				Data.DATA3, Data.MIMETYPE, Data.DISPLAY_NAME };

		Cursor c = getContentResolver().query(Data.CONTENT_URI, projection,
				selection, new String[] { id }, null);

		int data1Idx = c.getColumnIndex(Data.DATA1);
		int data2Idx = c.getColumnIndex(Data.DATA2);
		int data3Idx = c.getColumnIndex(Data.DATA3);
		int mimeTypeIdx = c.getColumnIndex(Data.MIMETYPE);
		int nameIdx = c.getColumnIndex(Data.DISPLAY_NAME);

		JSONObject contactJson = new JSONObject();

		JSONArray phoneNumbersJson = null;
		JSONArray emailsJson = null;
		JSONArray addressesJson = null;
		JSONArray eventsJson = null;

		List<ContactInfoData> items = new ArrayList<ContactInfoData>();
		String name = null;
		try {
			while (c.moveToNext()) {
				String mimeType = c.getString(mimeTypeIdx);

				if (!contactJson.has(HikeConstants.NAME)) {
					String dispName = c.getString(nameIdx);
					contactJson.put(HikeConstants.NAME, dispName);
					name = dispName;
				}

				if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {

					if (phoneNumbersJson == null) {
						phoneNumbersJson = new JSONArray();
						contactJson.put(HikeConstants.PHONE_NUMBERS,
								phoneNumbersJson);
					}

					String type = Phone.getTypeLabel(getResources(),
							c.getInt(data2Idx), c.getString(data3Idx))
							.toString();
					String msisdn = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, msisdn);
					phoneNumbersJson.put(data);

					items.add(new ContactInfoData(DataType.PHONE_NUMBER,
							msisdn, type));
				} else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {

					if (emailsJson == null) {
						emailsJson = new JSONArray();
						contactJson.put(HikeConstants.EMAILS, emailsJson);
					}

					String type = Email.getTypeLabel(getResources(),
							c.getInt(data2Idx), c.getString(data3Idx))
							.toString();
					String email = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, email);
					emailsJson.put(data);

					items.add(new ContactInfoData(DataType.EMAIL, email, type));
				} else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)) {

					if (addressesJson == null) {
						addressesJson = new JSONArray();
						contactJson.put(HikeConstants.ADDRESSES, addressesJson);
					}

					String type = StructuredPostal.getTypeLabel(getResources(),
							c.getInt(data2Idx), c.getString(data3Idx))
							.toString();
					String address = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, address);
					addressesJson.put(data);

					items.add(new ContactInfoData(DataType.ADDRESS, address,
							type));
				} else if (Event.CONTENT_ITEM_TYPE.equals(mimeType)) {

					if (eventsJson == null) {
						eventsJson = new JSONArray();
						contactJson.put(HikeConstants.EVENTS, eventsJson);
					}

					String event;
					int eventType = c.getInt(data2Idx);
					if (eventType == Event.TYPE_ANNIVERSARY) {
						event = getString(R.string.anniversary);
					} else if (eventType == Event.TYPE_OTHER) {
						event = getString(R.string.other);
					} else if (eventType == Event.TYPE_BIRTHDAY) {
						event = getString(R.string.birthday);
					} else {
						event = c.getString(data3Idx);
					}
					String type = event.toString();
					String eventDate = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, eventDate);
					eventsJson.put(data);

					items.add(new ContactInfoData(DataType.EVENT, eventDate,
							type));
				}
			}
		} catch (JSONException e) {
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}

		Log.d(getClass().getSimpleName(),
				"Data of contact is : " + contactJson.toString());
		clearTempData();
		showContactDetails(items, name, contactJson, false);
	}

	public void showContactDetails(final List<ContactInfoData> items,
			final String name, final JSONObject contactInfo,
			final boolean saveContact) {
		contactDialog = new ContactDialog(this, R.style.Theme_CustomDialog);
		contactDialog.setContentView(R.layout.contact_share_info);

		ViewGroup parent = (ViewGroup) contactDialog.findViewById(R.id.parent);
		TextView contactName = (TextView) contactDialog
				.findViewById(R.id.contact_name);
		ListView contactDetails = (ListView) contactDialog
				.findViewById(R.id.contact_details);
		Button yesBtn = (Button) contactDialog.findViewById(R.id.btn_ok);
		Button noBtn = (Button) contactDialog.findViewById(R.id.btn_cancel);
		TextView targetAccount = (TextView) contactDialog
				.findViewById(R.id.target_account);
		final Spinner accounts = (Spinner) contactDialog
				.findViewById(R.id.account_spinner);

		int screenHeight = getResources().getDisplayMetrics().heightPixels;
		int dialogWidth = (int) getResources().getDimension(
				R.dimen.contact_info_width);
		int dialogHeight = (int) (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? ((3 * screenHeight) / 4)
				: FrameLayout.LayoutParams.MATCH_PARENT);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dialogWidth,
				dialogHeight);
		lp.topMargin = (int) (5 * Utils.densityMultiplier);
		lp.bottomMargin = (int) (5 * Utils.densityMultiplier);

		parent.setLayoutParams(lp);

		contactDialog.setViewReferences(parent, accounts);

		yesBtn.setText(saveContact ? R.string.save : R.string.send);

		if (saveContact) {
			accounts.setVisibility(View.VISIBLE);
			targetAccount.setVisibility(View.VISIBLE);
			accounts.setAdapter(new AccountAdapter(getApplicationContext(),
					getAccountList()));
		} else {
			accounts.setVisibility(View.GONE);
			targetAccount.setVisibility(View.GONE);
		}

		contactName.setText(name);
		contactDetails.setAdapter(new ArrayAdapter<ContactInfoData>(
				getApplicationContext(), R.layout.contact_share_item,
				R.id.info_value, items) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				ContactInfoData contactInfoData = getItem(position);

				TextView header = (TextView) v.findViewById(R.id.info_head);
				header.setText(contactInfoData.getDataSubType());

				TextView details = (TextView) v.findViewById(R.id.info_value);
				details.setText(contactInfoData.getData());
				return v;
			}

		});
		yesBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (saveContact) {
					saveContact(items, accounts, name);
				} else {
					initialiseContactTransfer(contactInfo);
				}
				contactDialog.dismiss();
			}
		});
		noBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				contactDialog.dismiss();
			}
		});
		contactDialog.show();
	}

	private void initialiseFileTransfer(String filePath,
			HikeFileType hikeFileType, String fileType, boolean isRecording,
			long recordingDuration, boolean isForwardingFile) {
		clearTempData();
		UploadFileTask uploadFileTask = new UploadFileTask(mContactNumber,
				filePath, fileType, hikeFileType, isRecording
						&& !isForwardingFile, recordingDuration,
				getApplicationContext(), mConversation);
		uploadFileTask.execute();
	}

	private void initialiseLocationTransfer(double latitude, double longitude,
			int zoomLevel) {
		clearTempData();
		UploadContactOrLocationTask uploadLocationTask = new UploadContactOrLocationTask(
				mContactNumber, latitude, longitude, zoomLevel,
				getApplicationContext(), mConversation);
		uploadLocationTask.execute();
	}

	private void initialiseContactTransfer(JSONObject contactJson) {
		UploadContactOrLocationTask contactOrLocationTask = new UploadContactOrLocationTask(
				mContactNumber, contactJson, getApplicationContext(),
				mConversation);
		contactOrLocationTask.execute();
	}

	private void saveContact(List<ContactInfoData> items,
			Spinner accountSpinner, String name) {

		AccountData accountData = (AccountData) accountSpinner
				.getSelectedItem();

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		int rawContactInsertIndex = ops.size();

		ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
				.withValue(RawContacts.ACCOUNT_TYPE, accountData.getType())
				.withValue(RawContacts.ACCOUNT_NAME, accountData.getName())
				.build());

		for (ContactInfoData contactInfoData : items) {
			switch (contactInfoData.getDataType()) {
			case ADDRESS:
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE,
								StructuredPostal.CONTENT_ITEM_TYPE)
						.withValue(StructuredPostal.DATA,
								contactInfoData.getData())
						.withValue(StructuredPostal.TYPE,
								StructuredPostal.TYPE_CUSTOM)
						.withValue(StructuredPostal.LABEL,
								contactInfoData.getDataSubType()).build());
				break;
			case EMAIL:
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
						.withValue(Email.DATA, contactInfoData.getData())
						.withValue(Email.TYPE, Email.TYPE_CUSTOM)
						.withValue(Email.LABEL,
								contactInfoData.getDataSubType()).build());
				break;
			case EVENT:
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
						.withValue(Event.DATA, contactInfoData.getData())
						.withValue(Event.TYPE, Event.TYPE_CUSTOM)
						.withValue(Event.LABEL,
								contactInfoData.getDataSubType()).build());
				break;
			case PHONE_NUMBER:
				ops.add(ContentProviderOperation
						.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(
								ContactsContract.Data.RAW_CONTACT_ID,
								rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
						.withValue(Phone.NUMBER, contactInfoData.getData())
						.withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
						.withValue(Phone.LABEL,
								contactInfoData.getDataSubType()).build());
				break;
			}
		}
		ops.add(ContentProviderOperation
				.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID,
						rawContactInsertIndex)
				.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
				.withValue(StructuredName.DISPLAY_NAME, name).build());
		boolean contactSaveSuccessful;
		try {
			getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			contactSaveSuccessful = true;
		} catch (RemoteException e) {
			e.printStackTrace();
			contactSaveSuccessful = false;
		} catch (OperationApplicationException e) {
			e.printStackTrace();
			contactSaveSuccessful = false;
		}
		Toast.makeText(
				getApplicationContext(),
				contactSaveSuccessful ? R.string.contact_saved
						: R.string.contact_not_saved, Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public boolean onKey(View view, int keyCode, KeyEvent event) {
		if ((event.getAction() == KeyEvent.ACTION_UP)
				&& (keyCode == KeyEvent.KEYCODE_ENTER) && event.isAltPressed()) {
			mComposeView.append("\n");
			/*
			 * micromax phones appear to fire this event twice. Doing this seems
			 * to fix the problem.
			 */
			KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN);
			return true;
		}
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// For preventing the tool tip from animating again if its already
		// showing
		outState.putBoolean(HikeConstants.Extras.OVERLAY_SHOWING,
				mOverlayLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.EMOTICON_SHOWING,
				emoticonLayout != null
						&& emoticonLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.FILE_TRANSFER_DIALOG_SHOWING,
				filePickerDialog != null && filePickerDialog.isShowing());
		outState.putBoolean(HikeConstants.Extras.RECORDER_DIALOG_SHOWING,
				recordingDialog != null && recordingDialog.isShowing());
		outState.putLong(
				HikeConstants.Extras.RECORDER_START_TIME,
				updateRecordingDuration != null ? updateRecordingDuration
						.getStartTime() : 0);
		outState.putLong(HikeConstants.Extras.RECORDED_TIME, recordedTime);
		if (emoticonLayout != null
				&& emoticonLayout.getVisibility() == View.VISIBLE) {
			outState.putInt(HikeConstants.Extras.WHICH_EMOTICON_CATEGORY,
					currentEmoticonCategorySelected.getId());
			outState.putInt(HikeConstants.Extras.WHICH_EMOTICON_SUBCATEGORY,
					tabHost.getCurrentTab());
		}
		outState.putInt(HikeConstants.Extras.DIALOG_SHOWING,
				dialogShowing != null ? dialogShowing.ordinal() : -1);
		super.onSaveInstanceState(outState);
	}

	public void onEmoticonBtnClicked(View v) {
		onEmoticonBtnClicked(v, tabHost != null ? tabHost.getCurrentTab() : 0,
				false);
	}

	public void onEmoticonBtnClicked(View v, int whichSubcategory,
			boolean backPressed) {
		// This will be -1 when the tab host was initialized, but not tabs were
		// added to it.
		if (whichSubcategory == -1) {
			whichSubcategory = 0;
		}

		if (tipView != null) {
			TipType viewTipType = (TipType) tipView.getTag();
			if (viewTipType == TipType.EMOTICON) {
				Utils.closeTip(TipType.EMOTICON, tipView, prefs);
				tipView = null;
			}
		}

		emoticonLayout = emoticonLayout == null ? (ViewGroup) findViewById(R.id.emoticon_layout)
				: emoticonLayout;
		emoticonViewPager = emoticonViewPager == null ? (ViewPager) findViewById(R.id.emoticon_pager)
				: emoticonViewPager;

		boolean wasCategoryChanged = !isTabInitialised;

		if (tabHost != null && !isTabInitialised) {
			isTabInitialised = true;
			Log.d(getClass().getSimpleName(),
					"Initialising boolean for emoticon layout setup.: "
							+ isTabInitialised);

			int[] tabDrawables = null;

			int offset = 0;
			int emoticonsListSize = 0;
			switch (currentEmoticonCategorySelected.getId()) {
			case R.id.hike_emoticons_btn:
				tabDrawables = new int[] { R.drawable.ic_recents_emo,
						R.drawable.emo_im_01_bigsmile,
						R.drawable.emo_im_81_exciting,
						R.drawable.emo_im_111_grin };
				emoticonType = EmoticonType.HIKE_EMOTICON;
				emoticonsListSize = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
				break;
			case R.id.emoji_btn:
				tabDrawables = new int[] { R.drawable.ic_recents_emo,
						EmoticonConstants.EMOJI_RES_IDS[0],
						EmoticonConstants.EMOJI_RES_IDS[109],
						EmoticonConstants.EMOJI_RES_IDS[162],
						EmoticonConstants.EMOJI_RES_IDS[294],
						EmoticonConstants.EMOJI_RES_IDS[392] };
				emoticonType = EmoticonType.EMOJI;
				offset = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;
				emoticonsListSize = EmoticonConstants.EMOJI_RES_IDS.length;
				break;
			case R.id.sticker_btn:
				tabDrawables = new int[] { R.drawable.ic_recents_emo };
				emoticonType = EmoticonType.STICKERS;
				offset = 0;
				emoticonsListSize = EmoticonConstants.LOCAL_STICKER_RES_IDS.length;
				if (!prefs.getBoolean(
						HikeMessengerApp.SHOWN_DEFAULT_STICKER_CATEGORY_POPUP,
						false)) {
					showStickerPreviewDialog(0);
				}
				setupBottomTab(0);
				break;
			}

			LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			for (int i = 0; i < tabDrawables.length; i++) {
				View tabHead = layoutInflater.inflate(
						R.layout.emoticon_tab_layout, null);
				TabSpec ts = tabHost.newTabSpec("tab" + (i + 1));

				((ImageView) tabHead.findViewById(R.id.tab_header_img))
						.setImageResource(tabDrawables[i]);
				if (i == 0) {
					tabHead.findViewById(R.id.divider_left).setVisibility(
							View.GONE);
				} else if (i == tabDrawables.length - 1) {
					tabHead.findViewById(R.id.divider_right).setVisibility(
							View.GONE);
				}
				ts.setIndicator(tabHead);
				ts.setContent(new TabFactory());
				tabHost.addTab(ts);
			}
			/*
			 * Checking whether we have a few emoticons in the recents category.
			 * If not we show the next tab emoticons.
			 */
			if (whichSubcategory == 0 && emoticonType != EmoticonType.STICKERS) {
				int startOffset = offset;
				int endOffset = startOffset + emoticonsListSize;
				int recentEmoticonsSizeReq = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? EmoticonAdapter.MAX_EMOTICONS_PER_ROW_PORTRAIT
						: EmoticonAdapter.MAX_EMOTICONS_PER_ROW_LANDSCAPE;
				int[] recentEmoticons = HikeConversationsDatabase.getInstance()
						.fetchEmoticonsOfType(emoticonType, startOffset,
								endOffset, recentEmoticonsSizeReq);
				if (recentEmoticons.length < recentEmoticonsSizeReq) {
					whichSubcategory++;
				}
			}
			setupEmoticonLayout(emoticonType, whichSubcategory);
			tabHost.setCurrentTab(whichSubcategory);
		}

		if (emoticonLayout.getVisibility() == View.VISIBLE
				&& !wasCategoryChanged) {
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					emoticonLayout.setVisibility(View.GONE);
					setEmoticonButton();
				}
			}, 65);
			if (!backPressed) {
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				imm.showSoftInput(mComposeView,
						InputMethodManager.SHOW_IMPLICIT);
			}
		} else {
			if (!wasCategoryChanged) {
				Animation slideUp = AnimationUtils.loadAnimation(
						ChatThread.this, android.R.anim.fade_in);
				slideUp.setDuration(400);
				emoticonLayout.setAnimation(slideUp);
			}
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					emoticonLayout.setVisibility(View.VISIBLE);
					setEmoticonButton();
				}
			}, 45);
			Utils.hideSoftKeyboard(this, mComposeView);
		}

		emoticonViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int pageNum) {
				Log.d("ViewPager", "Page number: " + pageNum);
				if (emoticonType != EmoticonType.STICKERS) {
					tabHost.setCurrentTab(pageNum);
				} else {
					if (currentStickerCategorySelected != null) {
						currentStickerCategorySelected.setSelected(false);
					}
					if (stickerCatgoryContainer != null) {
						currentStickerCategorySelected = stickerCatgoryContainer
								.findViewWithTag(HikeMessengerApp.stickerCategories
										.get(pageNum));
						currentStickerCategorySelected.setSelected(true);
					}

					String categoryId = Utils.getCategoryIdForIndex(pageNum);
					if (pageNum == 0
							&& !prefs
									.getBoolean(
											HikeMessengerApp.SHOWN_DEFAULT_STICKER_CATEGORY_POPUP,
											false)) {
						showStickerPreviewDialog(0);
					} else if (pageNum != 0
							&& (!Utils.checkIfStickerCategoryExists(
									ChatThread.this, categoryId) || !prefs.getBoolean(
									HikeMessengerApp.stickerCategories
											.get(pageNum).downloadDialogPref,
									false))
							&& !ChatThread.stickerTaskMap
									.containsKey(categoryId)) {
						showStickerPreviewDialog(pageNum);
					}
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				emoticonViewPager.setCurrentItem(tabHost.getCurrentTab(), false);
			}
		});

		/*
		 * Here we dispatch a touch event to the compose view so that it regains
		 * focus http://code.google.com/p/android/issues/detail?id=2516
		 */
		if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS) {
			mComposeView.dispatchTouchEvent(null);
		}
	}

	private void showStickerPreviewDialog(final int categoryIndex) {
		final Dialog dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sticker_preview_dialog);

		View parent = dialog.findViewById(R.id.preview_container);

		setupStickerPreviewDialog(parent, categoryIndex);

		final String categoryId = Utils.getCategoryIdForIndex(categoryIndex);

		Button okBtn = (Button) dialog.findViewById(R.id.ok_btn);
		okBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
				Editor editor = prefs.edit();
				try {
					if (categoryIndex == 0) {
						editor.putBoolean(
								HikeMessengerApp.SHOWN_DEFAULT_STICKER_CATEGORY_POPUP,
								true);
						return;
					}
					DownloadStickerTask downloadStickerTask = new DownloadStickerTask(
							ChatThread.this, categoryIndex,
							DownloadType.NEW_CATEGORY);
					downloadStickerTask.execute();

					ChatThread.stickerTaskMap.put(categoryId,
							downloadStickerTask);
					updateStickerCategoryUI(categoryIndex, false, null);

					if (categoryIndex != 0) {
						editor.putBoolean(HikeMessengerApp.stickerCategories
								.get(categoryIndex).downloadDialogPref, true);
					}
				} finally {
					editor.commit();
				}
			}
		});

		dialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				if (categoryIndex != 0
						&& !Utils.checkIfStickerCategoryExists(ChatThread.this,
								categoryId)) {
					emoticonViewPager.setCurrentItem(0, false);
				}
			}
		});

		dialog.show();
	}

	private void setupStickerPreviewDialog(View parent, int categoryIndex) {
		GradientDrawable parentDrawable = (GradientDrawable) parent
				.getBackground();
		Button stickerBtn = (Button) parent.findViewById(R.id.ok_btn);
		TextView category = (TextView) parent.findViewById(R.id.preview_text);
		View divider = parent.findViewById(R.id.divider);
		ImageView sticker = (ImageView) parent.findViewById(R.id.preview_image);

		int resParentBg = 0;
		int stickerBtnBg = 0;
		int stickerBtnText = 0;
		int stickerBtnTextColor = 0;
		int stickerBtnShadowColor = 0;
		int categoryText = 0;
		int categoryTextColor = 0;
		int categoryTextShadowColor = 0;
		int dividerBg = 0;

		switch (HikeMessengerApp.stickerCategories.get(categoryIndex).categoryResId) {
		case R.drawable.doggy:
			resParentBg = getResources().getColor(R.color.doggy_bg);

			stickerBtnBg = R.drawable.doggy_btn;
			stickerBtnText = android.R.string.ok;
			stickerBtnTextColor = getResources().getColor(
					R.color.doggy_btn_text);
			stickerBtnShadowColor = getResources().getColor(
					R.color.doggy_btn_text_shadow);

			categoryText = R.string.dog_category;
			categoryTextColor = getResources().getColor(R.color.doggy_text);
			categoryTextShadowColor = getResources().getColor(
					R.color.doggy_text_shadow);

			dividerBg = getResources().getColor(R.color.doggy_div);
			break;
		case R.drawable.kitty:
			resParentBg = getResources().getColor(R.color.kitty_bg);

			stickerBtnBg = R.drawable.kitty_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(
					R.color.kitty_btn_text);
			stickerBtnShadowColor = getResources().getColor(
					R.color.kitty_btn_text_shadow);

			categoryText = R.string.kitty_category;
			categoryTextColor = getResources().getColor(R.color.kitty_text);
			categoryTextShadowColor = getResources().getColor(
					R.color.kitty_text_shadow);

			dividerBg = getResources().getColor(R.color.kitty_div);
			break;
		case R.drawable.expressions:
			resParentBg = getResources().getColor(R.color.exp_bg);

			stickerBtnBg = R.drawable.exp_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.exp_btn_text);
			stickerBtnShadowColor = getResources().getColor(
					R.color.exp_btn_text_shadow);

			categoryText = R.string.exp_category;
			categoryTextColor = getResources().getColor(R.color.exp_text);
			categoryTextShadowColor = getResources().getColor(
					R.color.exp_text_shadow);

			dividerBg = getResources().getColor(R.color.exp_div);
			break;
		case R.drawable.bollywood:
			resParentBg = getResources().getColor(R.color.bollywood_bg);

			stickerBtnBg = R.drawable.bollywood_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(
					R.color.bollywood_btn_text);
			stickerBtnShadowColor = getResources().getColor(
					R.color.bollywood_btn_text_shadow);

			categoryText = R.string.bollywood_category;
			categoryTextColor = getResources().getColor(R.color.bollywood_text);
			categoryTextShadowColor = getResources().getColor(
					R.color.bollywood_text_shadow);

			dividerBg = getResources().getColor(R.color.bollywood_div);
			break;
		case R.drawable.rageface:
			resParentBg = getResources().getColor(R.color.rf_bg);

			stickerBtnBg = R.drawable.rf_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.rf_btn_text);
			stickerBtnShadowColor = getResources().getColor(
					R.color.rf_btn_text_shadow);

			categoryText = R.string.rf_category;
			categoryTextColor = getResources().getColor(R.color.rf_text);
			categoryTextShadowColor = getResources().getColor(
					R.color.rf_text_shadow);

			dividerBg = getResources().getColor(R.color.rf_div);
			break;
		}

		parentDrawable.setColor(resParentBg);
		sticker.setImageResource(HikeMessengerApp.stickerCategories
				.get(categoryIndex).categoryPreviewResId);

		stickerBtn.setBackgroundResource(stickerBtnBg);
		stickerBtn.setText(stickerBtnText);
		stickerBtn.setTextColor(stickerBtnTextColor);
		stickerBtn.setShadowLayer(0.7f, 0, 0.7f, stickerBtnShadowColor);

		category.setText(categoryText);
		category.setTextColor(categoryTextColor);
		category.setShadowLayer(0.6f, 0, 0.6f, categoryTextShadowColor);

		divider.setBackgroundColor(dividerBg);
	}

	private void updateStickerCategoryUI(int categoryIndex, boolean failed,
			DownloadType downloadTypeBeforeFail) {
		if (emoticonsAdapter == null) {
			return;
		}
		String categoryId = Utils.getCategoryIdForIndex(categoryIndex);

		View emoticonPage = emoticonViewPager.findViewWithTag(categoryId);

		if (emoticonPage == null) {
			return;
		}
		emoticonsAdapter.setupStickerPage(emoticonPage, categoryIndex, failed,
				downloadTypeBeforeFail);
		if (downloadTypeBeforeFail == DownloadType.UPDATE && !failed) {
			setupBottomTab(categoryIndex);
		}
	}

	private void setupBottomTab(int preselectedCategoryIndex) {
		View tabContainer = findViewById(android.R.id.tabs);
		tabContainer.setVisibility(View.GONE);

		stickerCatgoryContainer = (LinearLayout) findViewById(R.id.sticker_categories_container);
		stickerCatgoryContainer.setVisibility(View.VISIBLE);
		stickerCatgoryContainer.removeAllViews();

		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		List<StickerCategory> stickerCatgoryTabList = new ArrayList<StickerCategory>(
				HikeMessengerApp.stickerCategories);

		stickerCatgoryTabList.add(0, new StickerCategory(
				StickerCategory.BACK_CATEGORY_ID,
				StickerCategory.BACK_CATEGORY_RES_ID, null, 0));

		for (int i = 0; i < stickerCatgoryTabList.size(); i++) {
			StickerCategory stickerCategory = stickerCatgoryTabList.get(i);

			View parent = layoutInflater.inflate(R.layout.sticker_btn, null);

			if (i == 0) {
				parent.findViewById(R.id.divider_left).setVisibility(View.GONE);
			}

			boolean updateAvailable = HikeConversationsDatabase.getInstance()
					.isStickerUpdateAvailable(stickerCategory.categoryId);

			LayoutParams layoutParams = new LayoutParams(0,
					LayoutParams.MATCH_PARENT, 1.0f);

			ImageView updateAvailableView = (ImageView) parent
					.findViewById(R.id.update_available);
			updateAvailableView.setVisibility(updateAvailable ? View.VISIBLE
					: View.GONE);

			ImageButton stickerCategoryButton = (ImageButton) parent
					.findViewById(R.id.category_btn);
			stickerCategoryButton
					.setImageResource(stickerCategory.categoryResId);
			stickerCategoryButton.setTag(stickerCategory);

			if (i == preselectedCategoryIndex + 1) {
				stickerCategoryButton.setSelected(true);
				currentStickerCategorySelected = stickerCategoryButton;
			}

			parent.setLayoutParams(layoutParams);
			stickerCatgoryContainer.addView(parent);
		}
	}

	public void onStickerCategoryClick(View v) {
		Log.d(getClass().getSimpleName(), "ID: " + v.getId());

		StickerCategory tag = (StickerCategory) v.getTag();

		StickerCategory backCategory = new StickerCategory(
				StickerCategory.BACK_CATEGORY_ID,
				StickerCategory.BACK_CATEGORY_RES_ID, null, 0);
		if (backCategory.equals(tag)) {
			onEmoticonCategoryClick(findViewById(R.id.emoji_btn));
			hideStickerTabs();
			return;
		}

		for (int i = 0; i < HikeMessengerApp.stickerCategories.size(); i++) {
			StickerCategory category = HikeMessengerApp.stickerCategories
					.get(i);
			if (category.equals(tag)) {
				setStickerCategorySelected(tag);
				emoticonViewPager.setCurrentItem(i, false);
				break;
			}
		}
	}

	public int getCurrentPage() {
		if (emoticonViewPager == null || emoticonType != EmoticonType.STICKERS) {
			return -1;
		}
		return emoticonViewPager.getCurrentItem();
	}

	private void setStickerCategorySelected(StickerCategory tag) {
		if (currentStickerCategorySelected != null) {
			currentStickerCategorySelected.setSelected(false);
		}
		if (stickerCatgoryContainer != null) {
			currentStickerCategorySelected = stickerCatgoryContainer
					.findViewWithTag(tag);
			currentStickerCategorySelected.setSelected(true);
		}
	}

	private void hideStickerTabs() {
		findViewById(android.R.id.tabs).setVisibility(View.VISIBLE);
		findViewById(R.id.sticker_categories_container)
				.setVisibility(View.GONE);
	}

	private void setupEmoticonLayout(EmoticonType emoticonType, int pageNum) {
		emoticonsAdapter = new EmoticonAdapter(
				ChatThread.this,
				mComposeView,
				emoticonType,
				getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
		emoticonViewPager.setAdapter(emoticonsAdapter);
		emoticonViewPager.setCurrentItem(pageNum, false);
		emoticonViewPager.invalidate();

		/*
		 * show the tip if we are not currently on the stickers tab and we have
		 * not shown this tip before and there is no other tip showing.
		 */
		if (tipView == null
				&& emoticonType != EmoticonType.STICKERS
				&& !prefs
						.getBoolean(HikeMessengerApp.SHOWN_STICKERS_TIP, false)) {
			tipView = findViewById(R.id.stickers_tip);

			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tipView
					.getLayoutParams();
			int screenWidth = getResources().getDisplayMetrics().widthPixels;
			int buttonWidth = screenWidth / 3;
			int marginRight = (int) (buttonWidth / 2 - ((int) 22 * Utils.densityMultiplier));
			layoutParams.rightMargin = marginRight;

			tipView.setLayoutParams(layoutParams);
			Utils.showTip(this, TipType.STICKER, tipView);
		} else if (emoticonType == EmoticonType.STICKERS && tipView != null) {
			TipType viewTipType = (TipType) tipView.getTag();
			if (viewTipType == TipType.STICKER) {
				Utils.closeTip(TipType.STICKER, tipView, prefs);
				tipView = null;
			}
		}
	}

	public void onEmoticonCategoryClick(View v) {
		if (v.isSelected()) {
			return;
		}
		v.setSelected(true);
		isTabInitialised = false;
		currentEmoticonCategorySelected.setSelected(false);
		currentEmoticonCategorySelected = v;
		/*
		 * Added this line for older android device issue
		 * http://stackoverflow.com
		 * /questions/6157373/removing-a-tab-and-the-activity
		 * -intent-inside-of-it-from-a-tabhost
		 */
		tabHost.setCurrentTab(0);
		tabHost.clearAllTabs();
		onEmoticonBtnClicked(null, 0, false);
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

	private void toggleGroupLife(boolean alive) {
		((GroupConversation) mConversation).setGroupAlive(alive);
		this.mSendBtn.setEnabled(false);
		this.mComposeView.setVisibility(alive ? View.VISIBLE : View.INVISIBLE);
		this.titleIconView.setEnabled(alive ? true : false);
		findViewById(R.id.emo_btn).setEnabled(alive ? true : false);
		findViewById(R.id.title_image_btn2).setEnabled(alive ? true : false);
	}

	private String getMsisdnMainUser() {
		return mConversation instanceof GroupConversation ? ((GroupConversation) mConversation)
				.getGroupOwner() : mContactNumber;
	}

	@Override
	public void onFinish(boolean success) {
	}

	public void sendPoke() {
		ConvMessage convMessage = makeConvMessage(getString(R.string.poke_msg));

		JSONObject metadata = new JSONObject();
		try {
			metadata.put(HikeConstants.POKE, true);
			convMessage.setMetadata(metadata);
		} catch (JSONException e) {
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		sendMessage(convMessage);

		/*
		 * Added to make sure we scroll to the end when we add the poke message.
		 */
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mConversationsView.smoothScrollBy(-1, 1);
				mConversationsView.smoothScrollToPosition(mAdapter.getCount() - 1);
			}
		}, 10);

		Utils.vibrateNudgeReceived(this);
	}

	public void sendSticker(Sticker sticker) {
		ConvMessage convMessage = makeConvMessage("Sticker");

		JSONObject metadata = new JSONObject();
		try {
			metadata.put(HikeConstants.CATEGORY_ID, sticker.getCategoryId());

			metadata.put(HikeConstants.STICKER_ID, sticker.getStickerId());

			convMessage.setMetadata(metadata);
			Log.d(getClass().getSimpleName(), "metadat: " + metadata.toString());
		} catch (JSONException e) {
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		sendMessage(convMessage);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (mConversation instanceof GroupConversation) {
			return false;
		}
		return gestureDetector.onTouchEvent(event);
	}

	SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			sendPoke();
			return true;
		}

	};

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (!reachedEnd
				&& !loadingMoreMessages
				&& messages != null
				&& !messages.isEmpty()
				&& firstVisibleItem <= HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES) {

			int startIndex = hasSMSToggle() ? 1 : 0;
			/*
			 * This should only happen in the case where the user starts a new
			 * chat and gets a typing notification.
			 */
			if (messages.size() <= startIndex
					|| messages.get(startIndex) == null) {
				return;
			}

			loadingMoreMessages = true;

			List<ConvMessage> olderMessages = mConversationDb
					.getConversationThread(mContactNumber,
							mConversation.getConvId(),
							HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME,
							mConversation, messages.get(startIndex).getMsgID());

			if (!olderMessages.isEmpty()) {
				mAdapter.addMessages(olderMessages, startIndex);
				mAdapter.notifyDataSetChanged();
				mConversationsView.setSelection(firstVisibleItem
						+ olderMessages.size());
			} else {
				/*
				 * This signifies that we've reached the end. No need to query
				 * the db anymore unless we add a new message.
				 */
				reachedEnd = true;
			}

			loadingMoreMessages = false;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub

	}

	private List<AccountData> getAccountList() {
		Account[] a = AccountManager.get(this).getAccounts();
		// Clear out any old data to prevent duplicates
		List<AccountData> accounts = new ArrayList<AccountData>();

		// Get account data from system
		AuthenticatorDescription[] accountTypes = AccountManager.get(this)
				.getAuthenticatorTypes();

		// Populate tables
		for (int i = 0; i < a.length; i++) {
			// The user may have multiple accounts with the same name, so we
			// need to construct a
			// meaningful display name for each.
			String type = a[i].type;
			/*
			 * Only showing the user's google accounts
			 */
			if (!"com.google".equals(type)) {
				continue;
			}
			String systemAccountType = type;
			AuthenticatorDescription ad = getAuthenticatorDescription(
					systemAccountType, accountTypes);
			AccountData data = new AccountData(a[i].name, ad, this);
			accounts.add(data);
		}

		return accounts;
	}

	/**
	 * Obtain the AuthenticatorDescription for a given account type.
	 * 
	 * @param type
	 *            The account type to locate.
	 * @param dictionary
	 *            An array of AuthenticatorDescriptions, as returned by
	 *            AccountManager.
	 * @return The description for the specified account type.
	 */
	private AuthenticatorDescription getAuthenticatorDescription(String type,
			AuthenticatorDescription[] dictionary) {
		for (int i = 0; i < dictionary.length; i++) {
			if (dictionary[i].type.equals(type)) {
				return dictionary[i];
			}
		}
		// No match found
		throw new RuntimeException("Unable to find matching authenticator");
	}

	@Override
	public void startActivity(Intent intent) {
		try {
			/* Workaround for an HTC issue */
			if (intent.getComponent() != null
					&& ".HtcLinkifyDispatcherActivity".equals(intent
							.getComponent().getShortClassName()))
				intent.setComponent(null);
			super.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			super.startActivity(Intent.createChooser(intent, null));
		}
	}

	private class FetchLastSeenTask extends AsyncTask<Void, Void, Long> {

		long currentLastSeenValue;
		boolean retriedOnce;
		int isOffline;
		String msisdn;

		public FetchLastSeenTask(String msisdn, boolean retriedOnce) {
			this.msisdn = msisdn;
			this.currentLastSeenValue = HikeUserDatabase.getInstance()
					.getLastSeenTime(msisdn);
			this.isOffline = HikeUserDatabase.getInstance()
					.getIsOffline(msisdn);
			if (isOffline == 0) {
				/*
				 * We reset this to 1 since the user's online state is stale
				 * here.
				 */
				isOffline = 1;
			}
			this.retriedOnce = retriedOnce;
		}

		@Override
		protected Long doInBackground(Void... params) {
			URL url;
			try {
				url = new URL(AccountUtils.base + "/user/lastseen/"
						+ mContactNumber);

				Log.d(getClass().getSimpleName(), "URL:  " + url);

				URLConnection connection = url.openConnection();
				AccountUtils.addUserAgent(connection);
				connection.addRequestProperty("Cookie", "user="
						+ AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

				if (AccountUtils.ssl) {
					((HttpsURLConnection) connection)
							.setSSLSocketFactory(HikeSSLUtil
									.getSSLSocketFactory());
				}

				JSONObject response = AccountUtils.getResponse(connection
						.getInputStream());
				Log.d(getClass().getSimpleName(), "Response: " + response);
				if (response == null
						|| !HikeConstants.OK.equals(response
								.getString(HikeConstants.STATUS))) {
					return null;
				}
				JSONObject data = response.getJSONObject(HikeConstants.DATA);
				return data.getLong(HikeConstants.LAST_SEEN);

			} catch (MalformedURLException e) {
				Log.w(getClass().getSimpleName(), e);
				return null;
			} catch (IOException e) {
				Log.w(getClass().getSimpleName(), e);
				return null;
			} catch (JSONException e) {
				Log.w(getClass().getSimpleName(), e);
				return null;
			}

		}

		@Override
		protected void onPostExecute(Long result) {
			if (result == null) {
				if (!retriedOnce) {
					new FetchLastSeenTask(msisdn, true).execute();
					return;
				}
			} else {
				/*
				 * Update current last seen value.
				 */
				currentLastSeenValue = result;
				/*
				 * We only apply the offset if the value is greater than 0 since
				 * 0 and -1 are reserved.
				 */
				if (currentLastSeenValue > 0) {
					isOffline = 1;
					currentLastSeenValue = Utils.applyServerTimeOffset(
							ChatThread.this, currentLastSeenValue);
				} else {
					isOffline = (int) currentLastSeenValue;
					currentLastSeenValue = System.currentTimeMillis() / 1000;
				}

				HikeUserDatabase.getInstance().updateLastSeenTime(msisdn,
						currentLastSeenValue);
				HikeUserDatabase.getInstance().updateIsOffline(msisdn,
						isOffline);

			}
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.LAST_SEEN_TIME_UPDATED,
					new Pair<String, Long>(msisdn,
							isOffline == 1 ? currentLastSeenValue : isOffline));
		}
	}
}
