package com.bsb.hike.ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

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
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.EmoticonType;
import com.bsb.hike.HikeConstants.TipType;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.AccountAdapter;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.adapters.StickerAdapter;
import com.bsb.hike.adapters.UpdateAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.filetransfer.FileSavedState;
import com.bsb.hike.filetransfer.FileTransferBase.FTState;
import com.bsb.hike.filetransfer.FileTransferManager;
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
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.OverFlowMenuItem;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.tasks.DownloadStickerTask;
import com.bsb.hike.tasks.DownloadStickerTask.DownloadType;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.ContactDialog;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.RoundedRepeatingDrawable;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;

public class ChatThread extends HikeAppStateBaseFragmentActivity implements HikePubSub.Listener, TextWatcher, OnEditorActionListener, OnSoftKeyboardListener, View.OnKeyListener,
		FinishableEvent, OnTouchListener, OnScrollListener, OnItemLongClickListener
{

	private enum DialogShowing
	{
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

	private TextView mLastSeenView;

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

	private GroupParticipant myInfo;

	private static File selectedFile;

	private Dialog filePickerDialog;

	private static MediaRecorder recorder;

	private static MediaPlayer player;

	private Handler recordingHandler;

	private UpdateRecordingDuration updateRecordingDuration;

	private Dialog recordingDialog;

	private RecorderState recorderState;

	private boolean showKeyboard = false;

	private boolean isOnline = false;

	private ContactInfo contactInfo;

	private StickerEmoticonIconPageIndicator iconPageIndicator;

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.TYPING_CONVERSATION, HikePubSub.END_TYPING_CONVERSATION, HikePubSub.SMS_CREDIT_CHANGED,
			HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.MESSAGE_DELIVERED, HikePubSub.SERVER_RECEIVED_MSG, HikePubSub.MESSAGE_FAILED, HikePubSub.ICON_CHANGED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT, HikePubSub.GROUP_NAME_CHANGED, HikePubSub.GROUP_END, HikePubSub.CONTACT_ADDED, HikePubSub.UPLOAD_FINISHED,
			HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, HikePubSub.FILE_MESSAGE_CREATED, HikePubSub.MUTE_CONVERSATION_TOGGLED, HikePubSub.BLOCK_USER, HikePubSub.UNBLOCK_USER,
			HikePubSub.REMOVE_MESSAGE_FROM_CHAT_THREAD, HikePubSub.GROUP_REVIVED, HikePubSub.CHANGED_MESSAGE_TYPE, HikePubSub.SHOW_SMS_SYNC_DIALOG, HikePubSub.SMS_SYNC_COMPLETE,
			HikePubSub.SMS_SYNC_FAIL, HikePubSub.SMS_SYNC_START, HikePubSub.SHOWN_UNDELIVERED_MESSAGE, HikePubSub.STICKER_DOWNLOADED, HikePubSub.LAST_SEEN_TIME_UPDATED,
			HikePubSub.SEND_SMS_PREF_TOGGLED, HikePubSub.PARTICIPANT_JOINED_GROUP, HikePubSub.PARTICIPANT_LEFT_GROUP, HikePubSub.STICKER_CATEGORY_DOWNLOADED,
			HikePubSub.STICKER_CATEGORY_DOWNLOAD_FAILED, HikePubSub.LAST_SEEN_TIME_UPDATED, HikePubSub.SEND_SMS_PREF_TOGGLED, HikePubSub.PARTICIPANT_JOINED_GROUP,
			HikePubSub.PARTICIPANT_LEFT_GROUP, HikePubSub.CHAT_BACKGROUND_CHANGED };

	private EmoticonType emoticonType;

	private PagerAdapter emoticonsAdapter;

	private boolean wasOrientationChanged = false;

	private GestureDetector gestureDetector;

	private boolean loadingMoreMessages;

	private boolean reachedEnd;

	private ContactDialog contactDialog;

	private DialogShowing dialogShowing;

	private Dialog smsDialog;

	private Dialog nativeSmsDialog;

	private long recordStartTime;

	private long recordedTime = -1;

	private static boolean recording = false;

	/*
	 * Made this public so that its accessible to this activity's adapter
	 */
	public View tipView;

	private TextView mLabelView;

	private ImageView avatar;

	private PopupWindow attachmentWindow;

	private Menu mMenu;

	private ChatTheme selectedTheme;

	private ChatTheme temporaryTheme;

	private boolean showingChatThemePicker;

	private ImageView backgroundImage;

	@Override
	protected void onPause()
	{
		super.onPause();
		if (mAdapter != null)
		{
			// mAdapter.getStickerLoader().setPauseWork(false);
			// mAdapter.getStickerLoader().setExitTasksEarly(true);
			mAdapter.getIconImageLoader().setPauseWork(false);
			mAdapter.getIconImageLoader().setExitTasksEarly(true);
		}
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
		if (mAdapter != null)
		{
			// mAdapter.getStickerLoader().setExitTasksEarly(false);
			mAdapter.getIconImageLoader().setExitTasksEarly(false);
			mAdapter.notifyDataSetChanged();
		}
		/* mark any messages unread as read */
		setMessagesRead();

		/* clear any pending notifications */
		/* clear any toast notifications */
		if (mConversation != null)
		{
			NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mgr.cancel((int) mConversation.getMsisdn().hashCode());
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.init();

			/* check if the send button should be enabled */
			mComposeViewWatcher.setBtnEnabled();
			mComposeView.requestFocus();
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(chatThreadReceiver);
		if (emoticonsAdapter != null && (emoticonsAdapter instanceof StickerAdapter))
		{
			((StickerAdapter) emoticonsAdapter).unregisterListeners();
		}

		if (mComposeViewWatcher != null)
		{
			// If we didn't send an end typing. We should send one before
			// exiting
			if (!mComposeViewWatcher.wasEndTypingSent())
			{
				mComposeViewWatcher.sendEndTyping();
			}
			mComposeViewWatcher.uninit();
			mComposeViewWatcher = null;
		}
		if (contactDialog != null)
		{
			contactDialog.dismiss();
			contactDialog = null;
		}
		if (smsDialog != null)
		{
			smsDialog.cancel();
			smsDialog = null;
		}
		if (nativeSmsDialog != null)
		{
			nativeSmsDialog.cancel();
			nativeSmsDialog = null;
		}
		if (mAdapter != null)
		{
			mAdapter.resetPlayerIfRunning();
		}
		if (attachmentWindow != null && attachmentWindow.isShowing())
		{
			attachmentWindow.dismiss();
			attachmentWindow = null;
		}
		StickerManager.getInstance().saveSortedListForCategory(StickerCategoryId.recent, StickerManager.getInstance().getRecentStickerList());
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return getIntent();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		/*
		 * Making the action bar transparent for custom theming.
		 */
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		super.onCreate(savedInstanceState);

		/* force the user into the reg-flow process if the token isn't set */
		if (Utils.requireAuth(this))
		{
			return;
		}

		// TODO this is being called everytime this activity is created. Way too
		// often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		setContentView(R.layout.chatthread);

		// we are getting the intent which has started our activity here.
		// we fetch the boolean extra to check if the keyboard has to be
		// expanded.

		Intent fromIntent = getIntent();
		if (fromIntent.getBooleanExtra(HikeConstants.Extras.SHOW_KEYBOARD, false))
			showKeyboard = true;

		mHandler = new Handler();

		prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		wasOrientationChanged = savedInstanceState != null;
		isOverlayShowing = savedInstanceState == null ? false : savedInstanceState.getBoolean(HikeConstants.Extras.OVERLAY_SHOWING);

		config = getResources().getConfiguration();

		/* bind views to variables */
		chatLayout = (CustomLinearLayout) findViewById(R.id.chat_layout);
		backgroundImage = (ImageView) findViewById(R.id.background);
		mBottomView = findViewById(R.id.bottom_panel);
		mConversationsView = (ListView) findViewById(R.id.conversations_list);
		mComposeView = (EditText) findViewById(R.id.msg_compose);
		mSendBtn = (ImageButton) findViewById(R.id.send_message);
		mMetadataNumChars = (TextView) findViewById(R.id.sms_chat_metadata_num_chars);
		mOverlayLayout = findViewById(R.id.overlay_layout);

		/*
		 * ensure that when the softkeyboard Done button is pressed (different than the sen button we have), we send the message.
		 */
		mComposeView.setOnEditorActionListener(this);

		/*
		 * Fix for android bug, where the focus is removed from the edittext when you have a layout with tabs (Emoticon layout) for hard keyboard devices
		 * http://code.google.com/p/android/issues/detail?id=2516
		 */
		if (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS)
		{
			mComposeView.setOnTouchListener(new OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
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
		/* register listeners */
		mPubSub.addListeners(this, pubSubListeners);
		if (prefs.contains(HikeMessengerApp.TEMP_NUM))
		{
			mContactName = prefs.getString(HikeMessengerApp.TEMP_NAME, null);
			mContactNumber = prefs.getString(HikeMessengerApp.TEMP_NUM, null);
			clearTempData();
			setIntentFromField();
			onNewIntent(getIntent());
		}
		else
		{
			Object o = getLastCustomNonConfigurationInstance();
			Intent intent = (o instanceof Intent) ? (Intent) o : getIntent();
			onNewIntent(intent);
		}

		if (savedInstanceState != null)
		{
			if (savedInstanceState.getBoolean(HikeConstants.Extras.FILE_TRANSFER_DIALOG_SHOWING))
			{
				showFilePicker(Utils.getExternalStorageState());
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.EMOTICON_SHOWING))
			{

				int emoticonTypeOrdinal = savedInstanceState.getInt(HikeConstants.Extras.EMOTICON_TYPE, -1);
				if (emoticonTypeOrdinal != -1)
				{
					EmoticonType type = EmoticonType.values()[emoticonTypeOrdinal];

					View emoticonLayout = findViewById(type == EmoticonType.STICKERS ? R.id.sticker_btn : R.id.emo_btn);
					onEmoticonBtnClicked(emoticonLayout, savedInstanceState.getInt(HikeConstants.Extras.WHICH_EMOTICON_SUBCATEGORY, 0), false);
				}
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.RECORDER_DIALOG_SHOWING))
			{
				recordStartTime = savedInstanceState.getLong(HikeConstants.Extras.RECORDER_START_TIME);
				recordedTime = savedInstanceState.getLong(HikeConstants.Extras.RECORDED_TIME);

				showRecordingDialog();
			}

			int dialogShowingOrdinal = savedInstanceState.getInt(HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1)
			{
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
				smsDialog = Utils.showSMSSyncDialog(this, dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG);
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.SHOW_STICKER_TIP_FOR_EMMA, false))
			{
				showStickerFtueTip();
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.CHAT_THEME_WINDOW_OPEN, false))
			{
				final ChatTheme chatTheme = ChatTheme.values()[savedInstanceState.getInt(HikeConstants.Extras.SELECTED_THEME, 0)];
				/*
				 * We need to queue this piece of code after the lifecycle methods are done. Else the popup window throws an exception.
				 */
				chatLayout.post(new Runnable()
				{

					@Override
					public void run()
					{
						setupThemePicker(chatTheme);
						setChatTheme(chatTheme);
					}
				});
			}
		}

		/* registering localbroadcast manager */
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED));
		// LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter(HikePubSub.RESUME_BUTTON_UPDATED));
		LocalBroadcastManager.getInstance(this).registerReceiver(chatThreadReceiver, new IntentFilter(StickerManager.STICKERS_UPDATED));

	}

	private void clearTempData()
	{
		Editor editor = prefs.edit();
		editor.remove(HikeMessengerApp.TEMP_NAME);
		editor.remove(HikeMessengerApp.TEMP_NUM);
		editor.commit();
	}

	@Override
	public void onBackPressed()
	{
		if (attachmentWindow != null && attachmentWindow.isShowing())
		{
			dismissPopupWindow();
			attachmentWindow = null;
			return;
		}

		if (!getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT) && this.mConversation != null)
		{
			if ((mConversation instanceof GroupConversation))
			{
				Utils.incrementNumTimesScreenOpen(prefs, HikeMessengerApp.NUM_TIMES_CHAT_THREAD_GROUP);
			}
			else if (!this.mConversation.isOnhike())
			{
				Utils.incrementNumTimesScreenOpen(prefs, HikeMessengerApp.NUM_TIMES_CHAT_THREAD_INVITE);
			}
		}
		if (emoticonLayout == null || emoticonLayout.getVisibility() != View.VISIBLE)
		{

			selectedFile = null;

			Intent intent = null;
			if (!getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT) && !getIntent().hasExtra(HikeConstants.Extras.FORWARD_MESSAGE)
					&& !getIntent().getBooleanExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, false)
					&& !getIntent().getBooleanExtra(HikeConstants.Extras.FROM_CENTRAL_TIMELINE, false))
			{
				intent = new Intent(this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
			else if (getIntent().hasExtra(HikeConstants.Extras.FORWARD_MESSAGE))
			{
				super.onBackPressed();
				intent = new Intent(this, ChatThread.class);
				intent.putExtra(HikeConstants.Extras.NAME, getIntent().getStringExtra(HikeConstants.Extras.PREV_NAME));
				intent.putExtra(HikeConstants.Extras.MSISDN, getIntent().getStringExtra(HikeConstants.Extras.PREV_MSISDN));
				startActivity(intent);
			}

			saveDraft();
			super.onBackPressed();
		}
		else
		{
			onEmoticonBtnClicked(null, 0, true);
		}
	}

	private void saveDraft()
	{
		/*
		 * If the user had typed something, we save it as a draft and will show it in the text box when he comes back to this conversation.
		 */
		if (mComposeView != null && mComposeView.getVisibility() == View.VISIBLE)
		{
			Editor editor = getSharedPreferences(HikeConstants.DRAFT_SETTING, MODE_PRIVATE).edit();
			if (mComposeView.length() != 0)
			{
				editor.putString(mContactNumber, mComposeView.getText().toString());
			}
			else
			{
				editor.remove(mContactNumber);
			}
			editor.commit();
		}
	}

	public boolean performContextBasedOperationOnMessage(ConvMessage message, int id)
	{
		switch (id)
		{
		case R.id.copy:
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			if (message.isFileTransferMessage())
			{
				HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
				clipboard.setText(AccountUtils.fileTransferBaseViewUrl + hikeFile.getFileKey());
			}
			else
			{
				clipboard.setText(message.getMessage());
			}
			return true;
		case R.id.forward:
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.FORWARD_MSG);
			Intent intent = new Intent(this, ComposeActivity.class);
			String msg;
			intent.putExtra(HikeConstants.Extras.FORWARD_MESSAGE, true);
			if (message.isFileTransferMessage())
			{
				HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
				intent.putExtra(HikeConstants.Extras.FILE_KEY, hikeFile.getFileKey());
				if (hikeFile.getHikeFileType() == HikeFileType.LOCATION)
				{
					intent.putExtra(HikeConstants.Extras.ZOOM_LEVEL, hikeFile.getZoomLevel());
					intent.putExtra(HikeConstants.Extras.LATITUDE, hikeFile.getLatitude());
					intent.putExtra(HikeConstants.Extras.LONGITUDE, hikeFile.getLongitude());
				}
				else if (hikeFile.getHikeFileType() == HikeFileType.CONTACT)
				{
					intent.putExtra(HikeConstants.Extras.CONTACT_METADATA, hikeFile.serialize().toString());
				}
				else
				{
					intent.putExtra(HikeConstants.Extras.FILE_PATH, hikeFile.getFilePath());
					intent.putExtra(HikeConstants.Extras.FILE_TYPE, hikeFile.getFileTypeString());
					if (hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
					{
						intent.putExtra(HikeConstants.Extras.RECORDING_TIME, hikeFile.getRecordingDuration());
					}
				}
			}
			else if (message.isStickerMessage())
			{
				Sticker sticker = message.getMetadata().getSticker();
				/*
				 * If the category is an unknown one, we have the id saved in the json.
				 */
				String categoryId = sticker.getCategory().categoryId == StickerCategoryId.unknown ? message.getMetadata().getUnknownStickerCategory()
						: sticker.getCategory().categoryId.name();
				intent.putExtra(StickerManager.FWD_CATEGORY_ID, categoryId);
				intent.putExtra(StickerManager.FWD_STICKER_ID, sticker.getStickerId());
				intent.putExtra(StickerManager.FWD_STICKER_INDEX, sticker.getStickerIndex());
			}
			else
			{
				msg = message.getMessage();
				intent.putExtra(HikeConstants.Extras.MSG, msg);
			}
			intent.putExtra(HikeConstants.Extras.PREV_MSISDN, mContactNumber);
			intent.putExtra(HikeConstants.Extras.PREV_NAME, mContactName);
			startActivity(intent);
			return true;
		case R.id.delete:
			removeMessage(message);
			if (message.isFileTransferMessage())
			{
				// @GM cancelTask has been changed
				HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
				File file = hikeFile.getFile();
				FileTransferManager.getInstance(getApplicationContext()).cancelTask(message.getMsgID(), file, message.isSent());
				mAdapter.notifyDataSetChanged();
			}
			return true;
		case R.id.cancel_file_transfer:
		{
			// @GM cancelTask has been changed
			HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
			File file = hikeFile.getFile();
			FileTransferManager.getInstance(getApplicationContext()).cancelTask(message.getMsgID(), file, message.isSent());
			mAdapter.notifyDataSetChanged();
			return true;
		}
		case R.id.share:
			HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
			Utils.startShareIntent(ChatThread.this, getString(R.string.share_file_message, AccountUtils.fileTransferBaseViewUrl + hikeFile.getFileKey()));
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (showingChatThemePicker)
		{
			return false;
		}
		getSupportMenuInflater().inflate(R.menu.chat_thread_menu, menu);
		mMenu = menu;
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (mConversation == null || showingChatThemePicker)
		{
			return super.onPrepareOptionsMenu(menu);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mConversation == null)
		{
			Log.w("ChatThread", "OptionItem menu selected when conversation was null");
			return false;
		}

		if (mConversation instanceof GroupConversation)
		{
			if (!((GroupConversation) mConversation).getIsGroupAlive())
			{
				return false;
			}
		}
		if (mUserIsBlocked)
		{
			return false;
		}
		if (!mConversation.isOnhike() && mCredits <= 0)
		{
			boolean nativeSmsPref = Utils.getSendSmsPref(this);
			if (!nativeSmsPref)
			{
				return false;
			}
		}

		switch (item.getItemId())
		{
		case R.id.chat_bg:
			setupThemePicker(null);
			if (!prefs.getBoolean(HikeMessengerApp.SHOWN_VALENTINE_CHAT_BG_TOOL_TIP, false))
			{
				closeChatBgFtueTip();
			}
			break;
		case R.id.attachment:
			showFilePicker(Utils.getExternalStorageState());
			break;
		case R.id.overflow_menu:
			showOverFlowMenu();
			break;
		}

		return true;
	}

	private void setupThemePicker(ChatTheme preSelectedTheme)
	{
		showThemePicker(preSelectedTheme);
		setupChatThemeActionBar();
		showingChatThemePicker = true;
		invalidateOptionsMenu();
	}

	private void dismissPopupWindow()
	{
		if (attachmentWindow != null)
		{
			attachmentWindow.dismiss();
		}
	}

	private void showOverFlowMenu()
	{

		ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		optionsList.add(new OverFlowMenuItem(getString((mConversation instanceof GroupConversation) ? R.string.group_profile : R.string.view_profile), 0));

		if (!(mConversation instanceof GroupConversation))
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.call), 1));
		}

		if (mConversation instanceof GroupConversation)
		{
			boolean isMuted = ((GroupConversation) mConversation).isMuted();

			optionsList.add(new OverFlowMenuItem(getString(isMuted ? R.string.unmute_group : R.string.mute_group), 2));

			optionsList.add(new OverFlowMenuItem(getString(R.string.clear_conversation), 5));
		}

		optionsList.add(new OverFlowMenuItem(getString(R.string.email_conversation), 3));

		optionsList.add(new OverFlowMenuItem(getString(R.string.shortcut), 4));

		dismissPopupWindow();

		attachmentWindow = new PopupWindow(this);

		View parentView = getLayoutInflater().inflate(R.layout.overflow_menu, chatLayout, false);

		attachmentWindow.setContentView(parentView);

		ListView overFlowListView = (ListView) parentView.findViewById(R.id.overflow_menu_list);
		overFlowListView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(this, R.layout.over_flow_menu_item, R.id.item_title, optionsList)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = getLayoutInflater().inflate(R.layout.over_flow_menu_item, parent, false);
				}

				OverFlowMenuItem item = getItem(position);

				TextView itemTextView = (TextView) convertView.findViewById(R.id.item_title);
				itemTextView.setText(item.getName());

				convertView.findViewById(R.id.profile_image_view).setVisibility(View.GONE);

				TextView freeSmsCount = (TextView) convertView.findViewById(R.id.free_sms_count);
				freeSmsCount.setVisibility(View.GONE);

				TextView newGamesIndicator = (TextView) convertView.findViewById(R.id.new_games_indicator);
				newGamesIndicator.setVisibility(View.GONE);

				return convertView;
			}
		});

		overFlowListView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				Log.d(getClass().getSimpleName(), "Onclick: " + position);

				dismissPopupWindow();
				OverFlowMenuItem item = (OverFlowMenuItem) adapterView.getItemAtPosition(position);

				switch (item.getKey())
				{
				case 0:
					openProfileScreen();
					break;
				case 1:
					Utils.onCallClicked(ChatThread.this, mContactNumber);
					break;
				case 2:
					GroupConversation groupConversation = (GroupConversation) mConversation;

					groupConversation.setIsMuted(!groupConversation.isMuted());

					HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED,
							new Pair<String, Boolean>(groupConversation.getMsisdn(), groupConversation.isMuted()));
					break;
				case 3:
					EmailConversationsAsyncTask emailTask = new EmailConversationsAsyncTask(ChatThread.this, null);
					Utils.executeConvAsyncTask(emailTask, mConversation);
					break;
				case 4:
					Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.ADD_SHORTCUT);
					Utils.createShortcut(ChatThread.this, mConversation);
					break;
				case 5:
					clearConversation();
				}

			}
		});

		attachmentWindow.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss()
			{
				attachmentWindow = null;
			}
		});

		attachmentWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		attachmentWindow.setOutsideTouchable(true);
		attachmentWindow.setFocusable(true);
		attachmentWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.overflow_menu_width));
		attachmentWindow.setHeight(LayoutParams.WRAP_CONTENT);
		/*
		 * In some devices Activity crashes and a BadTokenException is thrown by showAsDropDown method. Still need to find out exact repro of the bug.
		 */
		try
		{
			attachmentWindow.showAsDropDown(findViewById(R.id.attachment_anchor));
		}
		catch (BadTokenException e)
		{
			Log.e(getClass().getSimpleName(), "Excepetion in HomeActivity Overflow popup", e);
		}
		attachmentWindow.getContentView().setFocusableInTouchMode(true);
		attachmentWindow.getContentView().setOnKeyListener(new View.OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				return onKeyUp(keyCode, event);
			}
		});
	}

	private void clearConversation()
	{
		mPubSub.publish(HikePubSub.CLEAR_CONVERSATION, new Pair<String, Long>(mContactNumber, mConversation.getConvId()));
		messages.clear();
		mAdapter.notifyDataSetChanged();
	}

	private void blockUser()
	{
		Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.MENU_BLOCK);
		mUserIsBlocked = true;
		showOverlay(true);
	}

	private void unblockUser()
	{
		mUserIsBlocked = false;
		mComposeView.setEnabled(true);
		hideOverlay();
	}

	public void onOverlayButtonClick(View v)
	{
		/* user clicked the unblock button in the chat-screen */
		if (v.getId() != R.id.overlay_layout && blockOverlay)
		{
			mPubSub.publish(HikePubSub.UNBLOCK_USER, getMsisdnMainUser());
			unblockUser();
		}
		else if (v.getId() != R.id.overlay_layout)
		{
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.INVITE_OVERLAY_BUTTON);
			inviteUser();
			hideOverlay();
		}

		if (!blockOverlay)
		{
			hideOverlay();
			mConversationDb.setOverlay(true, mConversation.getMsisdn());
		}
	}

	private void hideOverlay()
	{
		if (mOverlayLayout.getVisibility() == View.VISIBLE && hasWindowFocus())
		{
			Animation fadeOut = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_out);
			mOverlayLayout.setAnimation(fadeOut);
		}
		mOverlayLayout.setVisibility(View.INVISIBLE);
		if (mConversation instanceof GroupConversation)
		{
			mComposeView.setEnabled(true);
		}
		isOverlayShowing = false;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		ConvMessage message = mAdapter.getItem(position);
		return showMessageContextMenu(message);
	}

	public boolean showMessageContextMenu(final ConvMessage message)
	{
		if (message == null || message.getParticipantInfoState() != ParticipantInfoState.NO_INFO || message.getTypingNotification() != null)
		{
			return false;
		}

		ArrayList<String> optionsList = new ArrayList<String>();

		if (message.isFileTransferMessage())
		{
			HikeFile hikeFile = message.getMetadata().getHikeFiles().get(0);
			if (!TextUtils.isEmpty(hikeFile.getFileKey()) && hikeFile.wasFileDownloaded())
			{
				optionsList.add(getString(R.string.forward));
			}
			// TODO : This should also be handled according to state
			// @GM Completed the above mentioned TODO
			File file = hikeFile.getFile();
			FileSavedState fss;
			if (message.isSent())
			{
				fss = FileTransferManager.getInstance(getApplicationContext()).getUploadFileState(message.getMsgID(), file);
			}
			else
			{
				fss = FileTransferManager.getInstance(getApplicationContext()).getDownloadFileState(message.getMsgID(), file);
			}
			if (fss.getFTState() == FTState.IN_PROGRESS || fss.getFTState() == FTState.PAUSED || fss.getFTState() == FTState.PAUSING)
			{
				optionsList.add(message.isSent() ? getString(R.string.cancel_upload) : getString(R.string.cancel_download));
			}
		}
		else if (message.getMetadata() == null || !message.getMetadata().isPokeMessage())
		{
			optionsList.add(getString(R.string.forward));
			if (!message.isStickerMessage())
			{
				optionsList.add(getString(R.string.copy));
			}
		}

		optionsList.add(getString(R.string.delete));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this, R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String option = options[which];
				if (getString(R.string.forward).equals(option))
				{
					performContextBasedOperationOnMessage(message, R.id.forward);
				}
				else if (getString(R.string.cancel_download).equals(option) || getString(R.string.cancel_upload).equals(option))
				{
					performContextBasedOperationOnMessage(message, R.id.cancel_file_transfer);
				}
				else if (getString(R.string.copy).equals(option))
				{
					performContextBasedOperationOnMessage(message, R.id.copy);
				}
				else if (getString(R.string.delete).equals(option))
				{
					performContextBasedOperationOnMessage(message, R.id.delete);
				}
			}
		});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(getResources().getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	private void sendMessage(ConvMessage convMessage)
	{
		addMessage(convMessage);

		mPubSub.publish(HikePubSub.MESSAGE_SENT, convMessage);
	}

	public void onSendClick(View v)
	{
		if (!mConversation.isOnhike() && mCredits <= 0)
		{
			boolean nativeSmsPref = Utils.getSendSmsPref(this);
			if (!nativeSmsPref)
			{
				return;
			}
		}
		if (TextUtils.isEmpty(mComposeView.getText()))
		{
			if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
			{
				Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			if (tipView != null)
			{
				TipType viewTipType = (TipType) tipView.getTag();
				if (viewTipType == TipType.WALKIE_TALKIE)
				{
					Utils.closeTip(TipType.WALKIE_TALKIE, tipView, prefs);
					tipView = null;
				}
			}
			if (!prefs.getBoolean(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP, false))
			{
				/*
				 * The user has already tapped on the walkie talkie button without seeing the tip no need to show it now.
				 */
				Editor editor = prefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP, true);
				editor.commit();
			}
			showRecordingDialog();
			return;
		}

		String message = mComposeView.getText().toString();

		mComposeView.setText("");

		ConvMessage convMessage = Utils.makeConvMessage(mConversation, mContactNumber, message, isConversationOnHike());

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
		Log.d(getClass().getSimpleName(), "Intent: " + intent.toString());

		String prevContactNumber = null;

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
			// We were getting msisdns with spaces in them. Replacing all spaces
			// so that lookup is correct
			phoneNumber = phoneNumber.replaceAll(" ", "");
			/*
			 * Replacing all '-' that we get in the number
			 */
			phoneNumber = phoneNumber.replaceAll("-", "");
			Log.d(getClass().getSimpleName(), "SMS To: " + phoneNumber);
			ContactInfo contactInfo = HikeUserDatabase.getInstance().getContactInfoFromPhoneNo(phoneNumber);
			/*
			 * phone lookup fails for a *lot* of people. If that happens, fall back to using their msisdn
			 */
			if (contactInfo != null)
			{
				mContactName = contactInfo.getName();
				prevContactNumber = mContactNumber;
				mContactNumber = contactInfo.getMsisdn();
				setIntentFromField();
			}
			else
			{
				prevContactNumber = mContactNumber;
				mContactName = mContactNumber = Utils.normalizeNumber(phoneNumber, prefs.getString(HikeMessengerApp.COUNTRY_CODE, HikeConstants.INDIA_COUNTRY_CODE));
			}

			createConversation();
		}
		else if (intent.hasExtra(HikeConstants.Extras.MSISDN) && !intent.hasExtra(HikeConstants.Extras.GROUP_CHAT))
		{

			prevContactNumber = mContactNumber;
			// selected chat from conversation list
			mContactNumber = intent.getStringExtra(HikeConstants.Extras.MSISDN);
			mContactName = intent.getStringExtra(HikeConstants.Extras.NAME);

			createConversation();
			if (intent.getBooleanExtra(HikeConstants.Extras.INVITE, false))
			{
				intent.removeExtra(HikeConstants.Extras.INVITE);
				inviteUser();
			}

			if (intent.hasExtra(HikeConstants.Extras.MSG))
			{
				String msg = intent.getStringExtra(HikeConstants.Extras.MSG);
				mComposeView.setText(msg);
				mComposeView.setSelection(mComposeView.length());
				SmileyParser.getInstance().addSmileyToEditable(mComposeView.getText(), false);
			}
			else if (intent.hasExtra(HikeConstants.Extras.FILE_PATH))
			{

				String fileKey = null;
				String filePath = intent.getStringExtra(HikeConstants.Extras.FILE_PATH);
				String fileType = intent.getStringExtra(HikeConstants.Extras.FILE_TYPE);

				boolean isRecording = false;
				long recordingDuration = -1;
				if (intent.hasExtra(HikeConstants.Extras.RECORDING_TIME))
				{
					recordingDuration = intent.getLongExtra(HikeConstants.Extras.RECORDING_TIME, -1);
					isRecording = true;
					fileType = HikeConstants.VOICE_MESSAGE_CONTENT_TYPE;
				}

				initiateFileTransferFromIntentData(fileType, filePath, isRecording, recordingDuration);

				// Making sure the file does not get forwarded again on
				// orientation change.
				intent.removeExtra(HikeConstants.Extras.FILE_PATH);
			}
			else if (intent.hasExtra(HikeConstants.Extras.LATITUDE))
			{
				String fileKey = null;
				double latitude = intent.getDoubleExtra(HikeConstants.Extras.LATITUDE, 0);
				double longitude = intent.getDoubleExtra(HikeConstants.Extras.LONGITUDE, 0);
				int zoomLevel = intent.getIntExtra(HikeConstants.Extras.ZOOM_LEVEL, 0);

				initialiseLocationTransfer(latitude, longitude, zoomLevel);
				// Making sure the file does not get forwarded again on
				// orientation change.
				intent.removeExtra(HikeConstants.Extras.LATITUDE);
			}
			else if (intent.hasExtra(HikeConstants.Extras.CONTACT_METADATA))
			{
				try
				{
					JSONObject contactJson = new JSONObject(intent.getStringExtra(HikeConstants.Extras.CONTACT_METADATA));
					HikeFile hikeFile = new HikeFile(contactJson);
					showContactDetails(Utils.getContactDataFromHikeFile(hikeFile), hikeFile.getDisplayName(), contactJson, false);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
			else if (intent.hasExtra(StickerManager.FWD_CATEGORY_ID))
			{
				String categoryId = intent.getStringExtra(StickerManager.FWD_CATEGORY_ID);
				String stickerId = intent.getStringExtra(StickerManager.FWD_STICKER_ID);
				int stickerIdx = intent.getIntExtra(StickerManager.FWD_STICKER_INDEX, -1);
				Sticker sticker = new Sticker(categoryId, stickerId, stickerIdx);
				sendSticker(sticker);
				// add this sticker to recents
				StickerManager.getInstance().addRecentSticker(sticker);
				/*
				 * Making sure the sticker is not forwarded again on orientation change
				 */
				intent.removeExtra(StickerManager.FWD_CATEGORY_ID);
			}
			/*
			 * Since the message was not forwarded, we check if we have any drafts saved for this conversation, if we do we enter it in the compose box.
			 */
			else
			{
				String message = getSharedPreferences(HikeConstants.DRAFT_SETTING, MODE_PRIVATE).getString(mContactNumber, "");
				mComposeView.setText(message);
				mComposeView.setSelection(mComposeView.length());
				SmileyParser.getInstance().addSmileyToEditable(mComposeView.getText(), false);
			}
			intent.removeExtra(HikeConstants.Extras.FORWARD_MESSAGE);
		}
		/*
		 * close context menu(if open) if the previous MSISDN is different from the current one)
		 */
		if (prevContactNumber != null && !prevContactNumber.equalsIgnoreCase(mContactNumber))
		{
			Log.w("ChatThread", "DIFFERENT MSISDN CLOSING CONTEXT MENU!!");
			closeContextMenu();
		}
	}

	private void initiateFileTransferFromIntentData(String fileType, String filePath)
	{
		initiateFileTransferFromIntentData(fileType, filePath, false, -1);
	}

	private void initiateFileTransferFromIntentData(String fileType, String filePath, boolean isRecording, long recordingDuration)
	{
		HikeFileType hikeFileType = HikeFileType.fromString(fileType, isRecording);

		Log.d(getClass().getSimpleName(), "Forwarding file- Type:" + fileType + " Path: " + filePath);

		if (Utils.isPicasaUri(filePath))
		{
			FileTransferManager.getInstance(getApplicationContext()).uploadFile(Uri.parse(filePath), hikeFileType, mContactNumber, mConversation.isOnhike());
		}
		else
		{
			initialiseFileTransfer(filePath, hikeFileType, fileType, isRecording, recordingDuration, true);
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
			Utils.sendInviteUtil(new ContactInfo(mContactNumber, mContactNumber, mContactName, mContactNumber), this, HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED,
					getString(R.string.native_header), getString(R.string.native_info));
		}
		else if (mConversation instanceof GroupConversation)
		{
			startActivity(new Intent(ChatThread.this, HikeListActivity.class));
		}
		else
		{
			Toast toast = Toast.makeText(this, R.string.already_hike_user, Toast.LENGTH_LONG);
			toast.show();
		}
	}

	/*
	 * sets the intent for this screen based on the fields we've assigned. useful if the user has entered information or we've determined information that indicates the type of
	 * data on this screen.
	 */
	private void setIntentFromField()
	{
		Intent intent = new Intent();
		if (mContactName != null)
		{
			intent.putExtra(HikeConstants.Extras.NAME, mContactName);
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
		// This prevent the activity from simply finishing and opens up the last
		// screen.
		getIntent().removeExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);

		mComposeView.setFocusable(true);
		mComposeView.setVisibility(View.VISIBLE);
		mComposeView.requestFocus();

		/*
		 * strictly speaking we shouldn't be reading from the db in the UI Thread
		 */
		mConversation = mConversationDb.getConversation(mContactNumber, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY);
		if (mConversation == null)
		{
			if (Utils.isGroupConversation(mContactNumber))
			{
				/* the user must have deleted the chat. */
				Toast toast = Toast.makeText(this, R.string.invalid_group_chat, Toast.LENGTH_LONG);
				toast.show();
				onBackPressed();
				return;
			}

			mConversation = mConversationDb.addConversation(mContactNumber, false, "", null);
		}

		/*
		 * Setting a flag which tells us whether the group contains sms users or not.
		 */
		if (mConversation instanceof GroupConversation)
		{
			boolean hasSmsUser = false;
			for (Entry<String, GroupParticipant> entry : ((GroupConversation) mConversation).getGroupParticipantList().entrySet())
			{
				GroupParticipant groupParticipant = entry.getValue();
				if (!groupParticipant.getContactInfo().isOnhike())
				{
					hasSmsUser = true;
					break;
				}
			}
			((GroupConversation) mConversation).setHasSmsUser(hasSmsUser);
		}

		mLabel = mConversation.getLabel();

		if (showKeyboard && !wasOrientationChanged)
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

		setupActionBar(true);

		gestureDetector = new GestureDetector(this, simpleOnGestureListener);

		if (!(mConversation instanceof GroupConversation))
		{
			contactInfo = HikeUserDatabase.getInstance().getContactInfoFromMSISDN(mContactNumber, false);

			favoriteType = contactInfo.getFavoriteType();

			if (!mConversation.isOnhike())
			{
				HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/profile/" + mContactNumber, RequestType.HIKE_JOIN_TIME, new HikeHttpCallback()
				{
					@Override
					public void onSuccess(JSONObject response)
					{
						Log.d(getClass().getSimpleName(), "Response: " + response.toString());
						try
						{
							JSONObject profile = response.getJSONObject(HikeConstants.PROFILE);
							long hikeJoinTime = profile.optLong(HikeConstants.JOIN_TIME, 0);
							if (hikeJoinTime > 0)
							{
								hikeJoinTime = Utils.applyServerTimeOffset(ChatThread.this, hikeJoinTime);

								HikeMessengerApp.getPubSub().publish(HikePubSub.HIKE_JOIN_TIME_OBTAINED, new Pair<String, Long>(mContactNumber, hikeJoinTime));
								ContactUtils.updateHikeStatus(ChatThread.this, mContactNumber, true);
								mConversationDb.updateOnHikeStatus(mContactNumber, true);
								HikeMessengerApp.getPubSub().publish(HikePubSub.USER_JOINED, mContactNumber);
							}
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}
					}
				});
				HikeHTTPTask getHikeJoinTimeTask = new HikeHTTPTask(null, -1);
				Utils.executeHttpTask(getHikeJoinTimeTask, hikeHttpRequest);
			}

			if (shouldShowLastSeen())
			{
				mLastSeenView.setText("");
				mLastSeenView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				/*
				 * Fetching last seen value.
				 */
				Utils.executeLongResultTask(new FetchLastSeenTask(mContactNumber, false));
			}
		}

		HikeUserDatabase db = HikeUserDatabase.getInstance();
		mUserIsBlocked = db.isBlocked(getMsisdnMainUser());
		if (mUserIsBlocked)
		{
			showOverlay(true);
		}

		/*
		 * make a copy of the message list since it's used internally by the adapter
		 */
		messages = new ArrayList<ConvMessage>(mConversation.getMessages());

		/*
		 * Add another item which translates to the SMS toggle option.
		 */
		if (!mConversation.isOnhike() && !Utils.isContactInternational(mContactNumber))
		{
			messages.add(0, new ConvMessage(null, null, -1, State.RECEIVED_READ, ConvMessage.SMS_TOGGLE_ID, -1));
		}

		mAdapter = new MessagesAdapter(this, messages, mConversation, this);
		mConversationsView.setAdapter(mAdapter);
		mConversationsView.setOnItemLongClickListener(this);
		mConversationsView.setOnTouchListener(this);
		mConversationsView.setOnScrollListener(this);

		if (getIntent().getBooleanExtra(HikeConstants.Extras.FROM_CHAT_THEME_FTUE, false))
		{
			setupChatThemeFTUE();
		}
		else
		{
			selectedTheme = mConversationDb.getChatThemeForMsisdn(mContactNumber);
			setChatTheme(selectedTheme);

			if (selectedTheme == ChatTheme.VALENTINES_2)
			{
				showValentineNudgeTip();
			}
		}

		if (mContactNumber.equals(HikeConstants.FTUE_HIKEBOT_MSISDN))
		{
			// In case of Emma HikeBot we show sticker Ftue tip only on
			// scrolling to
			// the bottom of the chat thread
			mConversationsView.setOnScrollListener(getOnScrollListenerForEmmaThread());
		}

		if (messages.isEmpty() && mBottomView.getVisibility() != View.VISIBLE)
		{
			Animation alphaIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up_noalpha);
			alphaIn.setDuration(400);
			mBottomView.setAnimation(alphaIn);
			mBottomView.setVisibility(View.VISIBLE);
		}
		else
		{
			mBottomView.setVisibility(View.VISIBLE);
		}

		// Scroll to the bottom if we just opened a new conversation
		if (!wasOrientationChanged)
		{
			mConversationsView.setSelection(messages.size() - 1);
		}

		/* add a text changed listener */
		mComposeView.addTextChangedListener(this);

		/* get the number of credits and also listen for changes */
		mCredits = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
		}

		updateUIForHikeStatus();

		mComposeViewWatcher = new ComposeViewWatcher(mConversation, mComposeView, mSendBtn, mCredits, this);

		/*
		 * create an object that we can notify when the contents of the thread are updated
		 */
		mUpdateAdapter = new UpdateAdapter(mAdapter);

		/* clear any toast notifications */
		NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mgr.cancel((int) mConversation.getConvId());

		if (mConversation instanceof GroupConversation)
		{
			myInfo = new GroupParticipant(Utils.getUserContactInfo(prefs));
			toggleConversationMuteViewVisibility(((GroupConversation) mConversation).isMuted());
		}
		else
		{
			toggleConversationMuteViewVisibility(false);
		}

		if ((mConversation instanceof GroupConversation) && !((GroupConversation) mConversation).getIsGroupAlive())
		{
			toggleGroupLife(false);
		}
		/*
		 * Check whether we have an existing typing notification for this conversation
		 */
		if (HikeMessengerApp.getTypingNotificationSet().containsKey(mContactNumber))
		{
			runOnUiThread(new SetTypingText(true, HikeMessengerApp.getTypingNotificationSet().get(mContactNumber)));
		}

		/*
		 * Only show these tips in a live group conversation or other conversations and is the conversation is not a hike bot conversation.
		 */
		if (!HikeMessengerApp.hikeBotNamesMap.containsKey(mContactNumber))
		{
			if (!(mConversation instanceof GroupConversation) || ((GroupConversation) mConversation).getIsGroupAlive())
			{
				if (!prefs.getBoolean(HikeMessengerApp.SHOWN_VALENTINE_CHAT_BG_TOOL_TIP, false))
				{
					showChatBgFtueTip();
				}
				else if (!prefs.getBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, false))
				{
					showStickerFtueTip();
				}
				else if (!prefs.getBoolean(HikeMessengerApp.SHOWN_WALKIE_TALKIE_TIP, false))
				{
					/*
					 * Only show the tip if we currently do not have any drafts
					 */
					if (TextUtils.isEmpty(getSharedPreferences(HikeConstants.DRAFT_SETTING, MODE_PRIVATE).getString(mContactNumber, "")))
					{
						tipView = findViewById(R.id.walkie_talkie_tip);
						Utils.showTip(this, TipType.WALKIE_TALKIE, tipView);
					}
				}
				if (tipView == null && !(mConversation instanceof GroupConversation) && !prefs.getBoolean(HikeMessengerApp.NUDGE_INTRO_SHOWN, false))
				{
					showNudgeDialog();
				}
			}
		}
	}

	private void setupChatThemeFTUE()
	{
		Random random = new Random();
		selectedTheme = ChatTheme.FTUE_THEMES[random.nextInt(ChatTheme.FTUE_THEMES.length)];

		final View whiteOverlay = findViewById(R.id.white_overlay);
		AnimationSet animationSet = new AnimationSet(false);

		AlphaAnimation alphaIn = new AlphaAnimation(0, 1);
		alphaIn.setStartOffset(500);
		alphaIn.setDuration(500);

		AlphaAnimation alphaOut = new AlphaAnimation(1, 0);
		alphaOut.setStartOffset(1000);
		alphaOut.setDuration(500);

		animationSet.addAnimation(alphaIn);
		animationSet.addAnimation(alphaOut);

		whiteOverlay.setVisibility(View.VISIBLE);
		whiteOverlay.startAnimation(animationSet);

		animationSet.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				whiteOverlay.setVisibility(View.GONE);
			}
		});

		mHandler.postDelayed(new Runnable()
		{

			@Override
			public void run()
			{
				setChatTheme(selectedTheme);
				sendChatThemeMessage();
				/*
				 * Only remove the extra once we've actually set the theme
				 */
				getIntent().removeExtra(HikeConstants.Extras.FROM_CHAT_THEME_FTUE);
			}
		}, 1200);
	}

	/*
	 * In case of Emma hikebot we show sticker ftue tip only on scrolling to the bottom of the emma chatthread
	 */
	private OnScrollListener getOnScrollListenerForEmmaThread()
	{
		return new OnScrollListener()
		{
			@Override
			public void onScrollStateChanged(AbsListView arg0, int scrollState)
			{
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
				ChatThread.this.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
				if (view.getChildAt(view.getChildCount() - 1) != null && view.getLastVisiblePosition() == view.getAdapter().getCount() - 1
						&& view.getChildAt(view.getChildCount() - 1).getBottom() <= view.getHeight())
				{
					if (!prefs.getBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, false))
					{
						// variable hideTip is for hiding the sticker tip
						// for the first auto scroll from bottom to top of emma
						// thread.
						// after that if user manually scroll the emma thread
						// from top
						// to bottom than we show the tip and keep it showing
						// than on
						boolean hideTip = false;
						if (tipView == null)
						{
							hideTip = true;
						}

						showStickerFtueTip();

						if (hideTip)
						{
							tipView.setVisibility(View.GONE);
						}
					}
				}
			}
		};
	}

	private void showChatBgFtueTip()
	{
		tipView = findViewById(R.id.chat_bg_ftue_tip);
		tipView.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
			{
				// disabling on touch gesture for sticker ftue tip
				// so that we do not send an unnecessary nudge on a
				// double tap on tipview.
				return true;
			}
		});
		Utils.showTip(this, TipType.CHAT_BG_FTUE, tipView);
		Animation chatBgFtueAnimation = AnimationUtils.loadAnimation(this, R.anim.chat_bg_ftue_anim);
		tipView.startAnimation(chatBgFtueAnimation);
	}

	private void closeChatBgFtueTip()
	{
		if (tipView != null)
		{
			TipType viewTipType = (TipType) tipView.getTag();
			if (viewTipType == TipType.CHAT_BG_FTUE)
			{
				tipView.clearAnimation();
				Utils.closeTip(TipType.CHAT_BG_FTUE, tipView, prefs);
				tipView = null;
			}
		}
	}

	private void showStickerFtueTip()
	{
		tipView = findViewById(R.id.emoticon_tip);
		tipView.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
			{
				// disabling on touch gesture for sticker ftue tip
				// so that we do not send an unnecessary nudge on a
				// double tap on tipview.
				return true;
			}
		});
		Utils.showTip(this, TipType.EMOTICON, tipView);
		Animation stickerFtueAnimation = AnimationUtils.loadAnimation(this, R.anim.sticker_ftue_anim);
		findViewById(R.id.sticker_image_icon).startAnimation(stickerFtueAnimation);
	}

	private void setupActionBar(boolean initialising)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_thread_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);
		View contactInfoContainer = actionBarView.findViewById(R.id.contact_info);

		CharSequence lastSeenString = null;
		if (!initialising)
		{
			lastSeenString = mLastSeenView.getText();
		}

		avatar = (ImageView) actionBarView.findViewById(R.id.avatar);
		mLabelView = (TextView) actionBarView.findViewById(R.id.contact_name);
		mLastSeenView = (TextView) actionBarView.findViewById(R.id.contact_status);

		mLastSeenView.setVisibility(View.VISIBLE);
		mLastSeenView.setSelected(true);

		if (initialising)
		{
			if (mConversation instanceof GroupConversation)
			{
				updateActivePeopleNumberView(0);
			}
			else
			{
				mLastSeenView.setText(mConversation.isOnhike() ? R.string.on_hike : R.string.on_sms);
				mLastSeenView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		}
		else
		{
			mLastSeenView.setText(lastSeenString);
			mLastSeenView.setCompoundDrawablesWithIntrinsicBounds(shouldShowLastSeen() ? R.drawable.ic_last_seen_clock : 0, 0, 0, 0);
		}

		avatar.setImageDrawable(HikeMessengerApp.getLruCache().getIconFromCache(mContactNumber, true));
		// avatar.setImageDrawable(IconCacheManager.getInstance()
		// .getIconForMSISDN(mContactNumber, true));
		mLabelView.setText(mLabel);

		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				saveDraft();

				Intent intent = new Intent(ChatThread.this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);

				finish();
			}
		});

		contactInfoContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				openProfileScreen();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	private void updateActivePeopleNumberView(int addPeopleCount)
	{
		int numActivePeople = ((GroupConversation) mConversation).getGroupMemberAliveCount() + addPeopleCount;
		((GroupConversation) mConversation).setGroupMemberAliveCount(numActivePeople);

		if (numActivePeople > 0)
		{
			/*
			 * Adding 1 to count the user.
			 */
			mLastSeenView.setText(getString(R.string.num_people, (numActivePeople + 1)));
			mLastSeenView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
	}

	private void openProfileScreen()
	{
		if (mUserIsBlocked)
		{
			return;
		}
		if (!(mConversation instanceof GroupConversation))
		{
			String userMsisdn = prefs.getString(HikeMessengerApp.MSISDN_SETTING, "");

			Intent intent = new Intent();
			intent.setClass(ChatThread.this, ProfileActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			if (!userMsisdn.equals(mContactNumber))
			{
				intent.putExtra(HikeConstants.Extras.CONTACT_INFO, mContactNumber);
				intent.putExtra(HikeConstants.Extras.ON_HIKE, mConversation.isOnhike());
			}
			startActivity(intent);
		}
		else
		{
			if (!((GroupConversation) mConversation).getIsGroupAlive())
			{
				return;
			}

			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.GROUP_INFO_TOP_BUTTON);
			Intent intent = new Intent();
			intent.setClass(ChatThread.this, ProfileActivity.class);
			intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
			intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mConversation.getMsisdn());
			startActivity(intent);
		}
		saveDraft();
	}

	private boolean shouldShowLastSeen()
	{
		if ((favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_RECEIVED || favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED)
				&& mConversation.isOnhike())
		{
			return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		}
		return false;
	}

	private void showNudgeDialog()
	{

		final Dialog nudgeAlert = new Dialog(this, R.style.Theme_CustomDialog);
		nudgeAlert.setCancelable(true);
		nudgeAlert.setContentView(R.layout.nudge_dialog);

		nudgeAlert.setCancelable(true);

		Button okBtn = (Button) nudgeAlert.findViewById(R.id.ok_btn);
		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				nudgeAlert.cancel();
			}
		});
		nudgeAlert.setOnCancelListener(new OnCancelListener()
		{

			@Override
			public void onCancel(DialogInterface dialog)
			{
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
	private void updateUIForHikeStatus()
	{
		if (mConversation.isOnhike() || (mConversation instanceof GroupConversation))
		{

			removeSMSToggle();

			mSendBtn.setBackgroundResource(R.drawable.bg_red_btn_selector);
			mComposeView.setHint(mConversation instanceof GroupConversation ? R.string.group_msg : R.string.hike_msg);
			if ((mConversation instanceof GroupConversation) && ((GroupConversation) mConversation).hasSmsUser())
			{
				if (mCredits == 0)
				{
					zeroCredits();
				}
				else
				{
					nonZeroCredits();
				}
			}
			else
			{
				nonZeroCredits();
			}
		}
		else
		{
			updateChatMetadata();
			mSendBtn.setBackgroundResource(R.drawable.bg_red_btn);
			mComposeView.setHint(R.string.sms_msg);
		}
	}

	private void removeSMSToggle()
	{
		if (!messages.isEmpty() && hasSMSToggle())
		{
			mAdapter.removeMessage(0);
		}
	}

	private boolean hasSMSToggle()
	{
		ConvMessage convMessage = messages.get(0);
		/*
		 * Typing notification
		 */
		if (convMessage == null)
		{
			return false;
		}
		if (convMessage.getMsgID() == ConvMessage.SMS_TOGGLE_ID)
		{
			return true;
		}
		return false;
	}

	/* returns TRUE iff the last message was received and unread */
	private boolean isLastMsgReceivedAndUnread()
	{
		if (mAdapter == null || mConversation == null)
		{
			return false;
		}

		ConvMessage lastMsg = null;
		for (int i = messages.size() - 1; i >= 0; i--)
		{
			ConvMessage msg = messages.get(i);
			if (msg.getTypingNotification() != null)
			{
				continue;
			}
			lastMsg = msg;
			break;
		}
		if (lastMsg == null)
		{
			return false;
		}

		return lastMsg.getState() == ConvMessage.State.RECEIVED_UNREAD || lastMsg.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE;
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
			if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
			{
				setSMSReadInNative();
			}

			long convID = mConversation.getConvId();
			JSONArray ids = mConversationDb.updateStatusAndSendDeliveryReport(convID);
			mPubSub.publish(HikePubSub.RESET_UNREAD_COUNT, mConversation.getMsisdn());
			/*
			 * If there are msgs which are RECEIVED UNREAD then only broadcast a msg that these are read avoid sending read notifications for group chats
			 */
			if (ids != null)
			{
				int lastReadIndex = messages.size() - ids.length();
				// Scroll to the last unread message
				if (lastReadIndex == 0)
				{
					mConversationsView.setSelection(lastReadIndex);
				}
				else
				{
					mConversationsView.setSelection(lastReadIndex - 1);
				}

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

	private void setSMSReadInNative()
	{
		new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				Log.d(getClass().getSimpleName(), "Marking message as read: " + mContactNumber);

				ContentValues contentValues = new ContentValues();
				contentValues.put(HikeConstants.SMSNative.READ, 1);

				getContentResolver().update(HikeConstants.SMSNative.INBOX_CONTENT_URI, contentValues, HikeConstants.SMSNative.NUMBER + "=?", new String[] { mContactNumber });
			}
		}).start();
	}

	private class SetTypingText implements Runnable
	{
		TypingNotification typingNotification;

		public SetTypingText(boolean direction, TypingNotification typingNotification)
		{
			this.direction = direction;
			this.typingNotification = typingNotification;
		}

		boolean direction;

		@Override
		public void run()
		{
			if (direction)
			{
				if (messages.isEmpty() || messages.get(messages.size() - 1).getTypingNotification() == null)
				{
					addMessage(new ConvMessage(typingNotification));
					Log.d(getClass().getSimpleName(), "calling chatThread.addMessage() Line no. : 2129");
				}
				else if (messages.get(messages.size() - 1).getTypingNotification() != null)
				{
					ConvMessage convMessage = messages.get(messages.size() - 1);
					convMessage.setTypingNotification(typingNotification);

					mAdapter.notifyDataSetChanged();
				}
			}
			else
			{
				if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
				{
					/*
					 * We only remove the typing notification if the conversation in a one to one conversation or it no one is typing in the group.
					 */
					if (!(mConversation instanceof GroupConversation))
					{
						messages.remove(messages.size() - 1);
					}
					else
					{
						GroupTypingNotification groupTypingNotification = (GroupTypingNotification) messages.get(messages.size() - 1).getTypingNotification();
						if (groupTypingNotification.getGroupParticipantList().isEmpty())
						{
							messages.remove(messages.size() - 1);
						}
					}
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	@Override
	public void onEventReceived(final String type, final Object object)
	{
		if (mContactNumber == null || mConversation == null)
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

				if (hasWindowFocus())
				{
					message.setState(ConvMessage.State.RECEIVED_READ);
					mConversationDb.updateMsgStatus(message.getMsgID(), ConvMessage.State.RECEIVED_READ.ordinal(), mConversation.getMsisdn());
					mPubSub.publish(HikePubSub.MQTT_PUBLISH, message.serializeDeliveryReportRead()); // handle
					// return to
					// sender

					mPubSub.publish(HikePubSub.RESET_UNREAD_COUNT, mConversation.getMsisdn());
					mPubSub.publish(HikePubSub.MSG_READ, mConversation.getMsisdn());
				}

				if (message.getParticipantInfoState() != ParticipantInfoState.NO_INFO && mConversation instanceof GroupConversation)
				{
					HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
					((GroupConversation) mConversation).setGroupParticipantList(hCDB.getGroupParticipants(mConversation.getMsisdn(), false, false));
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
						Log.d(getClass().getSimpleName(), "calling chatThread.addMessage() Line no. : 2219");
					}
				});

			}
		}
		else if (HikePubSub.END_TYPING_CONVERSATION.equals(type))
		{
			TypingNotification typingNotification = (TypingNotification) object;
			if (typingNotification == null)
			{
				return;
			}
			if (mContactNumber.equals(typingNotification.getId()))
			{
				runOnUiThread(new SetTypingText(false, typingNotification));
			}
		}
		else if (HikePubSub.TYPING_CONVERSATION.equals(type))
		{
			TypingNotification typingNotification = (TypingNotification) object;
			if (typingNotification == null)
			{
				return;
			}
			if (mContactNumber.equals(typingNotification.getId()))
			{

				runOnUiThread(new SetTypingText(true, typingNotification));

				if (shouldShowLastSeen() && contactInfo.getOffline() != -1)
				{
					/*
					 * Publishing an online event for this number.
					 */
					contactInfo.setOffline(0);
					HikeMessengerApp.getPubSub().publish(HikePubSub.LAST_SEEN_TIME_UPDATED, contactInfo);
				}
			}
		}
		// We only consider this case if there is a valid conversation in the
		// Chat Thread
		else if (mConversation != null && HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			mCredits = ((Integer) object).intValue();
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					updateUIForHikeStatus();
					if (!animatedOnce)
					{
						animatedOnce = prefs.getBoolean(HikeConstants.Extras.ANIMATED_ONCE, false);
						if (!animatedOnce)
						{
							Editor editor = prefs.edit();
							editor.putBoolean(HikeConstants.Extras.ANIMATED_ONCE, true);
							editor.commit();
						}
					}

					if ((mCredits % HikeConstants.SHOW_CREDITS_AFTER_NUM == 0 || !animatedOnce) && !mConversation.isOnhike())
					{
						animatedOnce = true;
						showSMSCounter();
					}
				}
			});
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))
		{
			Pair<String, Long> pair = (Pair<String, Long>) object;
			// If the msisdn don't match we simply return
			if (!mConversation.getMsisdn().equals(pair.first))
			{
				return;
			}
			long msgID = pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			ConvMessage msg = findMessageById(msgID);
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
			{
				msg.setState(ConvMessage.State.SENT_DELIVERED);
				runOnUiThread(mUpdateAdapter);
			}
		}
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))
		{
			Pair<String, long[]> pair = (Pair<String, long[]>) object;
			// If the msisdn don't match we simply return
			if (!mConversation.getMsisdn().equals(pair.first))
			{
				return;
			}
			long[] ids = pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			for (int i = 0; i < ids.length; i++)
			{
				ConvMessage msg = findMessageById(ids[i]);
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
				{
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
					msg.setReadByArray(HikeConversationsDatabase.getInstance().getReadByValueForMessageID(msg.getMsgID()));
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
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_CONFIRMED.ordinal()))
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
				runOnUiThread(new Runnable()
				{

					@Override
					public void run()
					{
						avatar.setImageDrawable(HikeMessengerApp.getLruCache().getIconFromCache(mContactNumber, true));
						// avatar.setImageDrawable(IconCacheManager.getInstance()
						// .getIconForMSISDN(mContactNumber, true));
					}
				});
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
					mLastSeenView.setText(mConversation.isOnhike() ? R.string.on_hike : R.string.on_sms);
					mLastSeenView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

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

				runOnUiThread(new Runnable()
				{
					public void run()
					{
						mLabelView.setText(groupName);
					}
				});
			}
		}
		else if (HikePubSub.GROUP_END.equals(type))
		{
			String groupId = ((JSONObject) object).optString(HikeConstants.TO);
			if (mContactNumber.equals(groupId))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						toggleGroupLife(false);
					}
				});
			}
		}
		else if (HikePubSub.CONTACT_ADDED.equals(type))
		{
			ContactInfo contactInfo = (ContactInfo) object;
			if (contactInfo == null)
			{
				return;
			}

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
		else if (HikePubSub.UPLOAD_FINISHED.equals(type))
		{
			ConvMessage convMessage = (ConvMessage) object;
			if (!convMessage.getMsisdn().equals(this.mContactNumber))
			{
				return;
			}
			ConvMessage adapterMessage = findMessageById(convMessage.getMsgID());
			if (adapterMessage != null)
			{
				try
				{
					adapterMessage.setMetadata(convMessage.getMetadata().getJSON());
				}
				catch (JSONException e)
				{
					Log.e(getClass().getSimpleName(), "Invalid JSON", e);
				}
			}
			runOnUiThread(mUpdateAdapter);
		}
		else if (HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED.equals(type))
		{
			runOnUiThread(mUpdateAdapter);
		}
		else if (HikePubSub.FILE_MESSAGE_CREATED.equals(type))
		{
			final ConvMessage convMessage = (ConvMessage) object;
			selectedFile = null;

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					addMessage(convMessage);
					Log.d(getClass().getSimpleName(), "calling chatThread.addMessage() Line no. : 2429");
				}
			});
		}
		else if (HikePubSub.MUTE_CONVERSATION_TOGGLED.equals(type))
		{
			Pair<String, Boolean> groupMute = (Pair<String, Boolean>) object;
			if (!groupMute.first.equals(this.mContactNumber))
			{
				return;
			}
			final Boolean isMuted = groupMute.second;
			((GroupConversation) mConversation).setIsMuted(isMuted);
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					toggleConversationMuteViewVisibility(isMuted);
					invalidateOptionsMenu();
				}
			});
		}
		else if (HikePubSub.BLOCK_USER.equals(type) || HikePubSub.UNBLOCK_USER.equalsIgnoreCase(type))
		{
			String msisdn = (String) object;
			final boolean blocked = HikePubSub.BLOCK_USER.equals(type);

			if (!msisdn.equals(getMsisdnMainUser()))
			{
				return;
			}

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (blocked)
					{
						blockUser();
					}
					else
					{
						unblockUser();
					}
				}
			});
		}
		else if (HikePubSub.REMOVE_MESSAGE_FROM_CHAT_THREAD.equals(type))
		{
			final ConvMessage convMessage = (ConvMessage) object;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					removeMessage(convMessage);
				}
			});
		}
		else if (HikePubSub.GROUP_REVIVED.equals(type))
		{
			String groupId = (String) object;
			if (mContactNumber.equals(groupId))
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						toggleGroupLife(true);
					}
				});
			}
		}
		else if (HikePubSub.CHANGED_MESSAGE_TYPE.equals(type))
		{
			updateAdapter();
		}
		else if (HikePubSub.SHOW_SMS_SYNC_DIALOG.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					smsDialog = Utils.showSMSSyncDialog(ChatThread.this, true);
					dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG;
				}
			});
		}
		else if (HikePubSub.SMS_SYNC_COMPLETE.equals(type) || HikePubSub.SMS_SYNC_FAIL.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (smsDialog != null)
					{
						smsDialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		}
		else if (HikePubSub.SMS_SYNC_START.equals(type))
		{
			dialogShowing = DialogShowing.SMS_SYNCING_DIALOG;
		}
		else if (HikePubSub.SHOWN_UNDELIVERED_MESSAGE.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					mConversationsView.setSelection(messages.size() - 1);
				}
			});
		}
		else if (HikePubSub.STICKER_DOWNLOADED.equals(type))
		{
			updateAdapter();
		}
		else if (HikePubSub.LAST_SEEN_TIME_UPDATED.equals(type))
		{
			ContactInfo newContactInfo = (ContactInfo) object;

			if (!mContactNumber.equals(newContactInfo.getMsisdn()) || (mConversation instanceof GroupConversation) || !shouldShowLastSeen())
			{
				return;
			}

			/*
			 * Updating the class's contactinfo object
			 */
			contactInfo.setOffline(newContactInfo.getOffline());
			contactInfo.setLastSeenTime(newContactInfo.getLastSeenTime());

			final String lastSeenString = Utils.getLastSeenTimeAsString(this, contactInfo.getLastSeenTime(), contactInfo.getOffline(), false, true);

			isOnline = contactInfo.getOffline() == 0;

			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (lastSeenString == null)
					{
						mLastSeenView.setText(mConversation.isOnhike() ? R.string.on_hike : R.string.on_sms);
						mLastSeenView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					}
					else
					{
						mLastSeenView.setText(lastSeenString);
						mLastSeenView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_last_seen_clock, 0, 0, 0);

						if (tipView == null && !prefs.getBoolean(HikeMessengerApp.SHOWN_LAST_SEEN_TIP, false))
						{
							tipView = findViewById(R.id.last_seen_tip);
							Utils.showTip(ChatThread.this, TipType.LAST_SEEN, tipView);
						}
					}
				}
			});
		}
		else if (HikePubSub.SEND_SMS_PREF_TOGGLED.equals(type))
		{
			if (mConversation == null)
			{
				return;
			}
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateUIForHikeStatus();
				}
			});
		}
		else if (HikePubSub.PARTICIPANT_LEFT_GROUP.equals(type))
		{
			if (mConversation == null)
			{
				return;
			}
			if (mConversation instanceof GroupConversation)
			{
				if (mConversation.getMsisdn().equals(((JSONObject) object).optString(HikeConstants.TO)))
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							// decrementing one user
							updateActivePeopleNumberView(-1);
						}
					});
				}
			}
		}
		else if (HikePubSub.PARTICIPANT_JOINED_GROUP.equals(type))
		{
			if (mConversation == null)
			{
				return;
			}
			if (mConversation.getMsisdn().equals(((JSONObject) object).optString(HikeConstants.TO)))
			{
				JSONObject obj = (JSONObject) object;
				JSONArray participants = obj.optJSONArray(HikeConstants.DATA);
				final int addPeopleCount = participants.length();
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						// increment by number of newly added participants
						updateActivePeopleNumberView(addPeopleCount);
					}
				});
			}
		}
		else if (HikePubSub.CHAT_BACKGROUND_CHANGED.equals(type))
		{
			if (mConversation == null)
			{
				return;
			}
			Pair<String, ChatTheme> pair = (Pair<String, ChatTheme>) object;
			if (!mConversation.getMsisdn().equals(pair.first))
			{
				return;
			}

			selectedTheme = pair.second;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					setChatTheme(selectedTheme);
				}
			});
		}
	}

	public boolean isContactOnline()
	{
		return isOnline;
	}

	private void updateAdapter()
	{
		runOnUiThread(new Runnable()
		{

			@Override
			public void run()
			{
				mUpdateAdapter.run();
			}
		});
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

		boolean nativeSmsPref = Utils.getSendSmsPref(this);

		if (mCredits <= 0 && !nativeSmsPref)
		{
			zeroCredits();
		}
		else
		{
			nonZeroCredits();

			if (mComposeView.getLineCount() > 2)
			{
				mMetadataNumChars.setVisibility(View.VISIBLE);
				int length = mComposeView.getText().length();
				// set the max sms length to a length appropriate to the number
				// of characters we have
				int charNum = length % 140;
				int numSms = ((int) (length / 140)) + 1;
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

	public void onTitleIconClick(View v)
	{
		if (v.getId() == R.id.info_layout || v.getId() == R.id.group_info_layout)
		{
			Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.I_BUTTON);
			showOverlay(false);
		}
	}

	private void zeroCredits()
	{
		Log.d(getClass().getSimpleName(), "Zero credits");
		mSendBtn.setEnabled(false);

		if (!TextUtils.isEmpty(mComposeView.getText()))
		{
			mComposeView.setText("");
		}
		if (!(mConversation instanceof GroupConversation))
		{
			mComposeView.setHint("0 Free SMS left...");
			mComposeView.setEnabled(false);
			findViewById(R.id.info_layout).setVisibility(View.VISIBLE);
		}
		else
		{
			findViewById(R.id.group_info_layout).setVisibility(View.VISIBLE);
		}

		boolean show = mConversationDb.wasOverlayDismissed(mConversation.getMsisdn());
		if (!show)
		{
			showOverlay(false);
		}
		if (!(mConversation instanceof GroupConversation))
		{
			findViewById(R.id.emo_btn).setEnabled(false);
			findViewById(R.id.sticker_btn).setEnabled(false);
		}
	}

	private void nonZeroCredits()
	{
		Log.d(getClass().getSimpleName(), "Non Zero credits");
		if (!mComposeView.isEnabled())
		{
			if (!TextUtils.isEmpty(mComposeView.getText()))
			{
				mComposeView.setText("");
			}
			if (mConversation instanceof GroupConversation)
			{
				mComposeView.setHint(R.string.group_msg);
			}
			else if (mConversation.isOnhike())
			{
				mComposeView.setHint(R.string.hike_msg);
			}
			else
			{
				mComposeView.setHint(R.string.sms_msg);
			}
			mComposeView.setEnabled(true);
		}
		findViewById((mConversation instanceof GroupConversation) ? R.id.group_info_layout : R.id.info_layout).setVisibility(View.GONE);

		findViewById(R.id.emo_btn).setEnabled(true);
		findViewById(R.id.sticker_btn).setEnabled(true);

		if (!blockOverlay)
		{
			hideOverlay();
		}
	}

	private void showSMSCounter()
	{
		slideUp = AnimationUtils.loadAnimation(ChatThread.this, R.anim.slide_up_noalpha);
		slideUp.setDuration(2000);

		slideDown = AnimationUtils.loadAnimation(ChatThread.this, R.anim.slide_down_noalpha);
		slideDown.setDuration(2000);
		slideDown.setStartOffset(2000);

		if (smsCount == null)
		{
			smsCount = (TextView) findViewById(R.id.sms_counter);
		}
		smsCount.setBackgroundColor(getResources().getColor(mAdapter.isDefaultTheme() ? R.color.updates_text : R.color.chat_thread_indicator_bg_custom_theme));
		smsCount.setAnimation(slideUp);
		smsCount.setVisibility(View.VISIBLE);
		smsCount.setText(mCredits + " " + getString(R.string.sms_left));

		slideUp.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

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

		if ((view == mComposeView)
				&& ((actionId == EditorInfo.IME_ACTION_SEND) || ((keyEvent != null) && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
						&& (keyEvent.getAction() != KeyEvent.ACTION_UP) && (config.keyboard != Configuration.KEYBOARD_NOKEYS))))
		{
			boolean ret = mSendBtn.performClick();
			Utils.hideSoftKeyboard(this, mComposeView);
			return ret;
		}
		return false;
	}

	private void addMessage(ConvMessage convMessage)
	{
		if (messages != null && mAdapter != null)
		{
			TypingNotification typingNotification = null;
			/*
			 * If we were showing the typing bubble, we remove it from the add the new message and add the typing bubble back again
			 */
			if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
			{
				typingNotification = messages.get(messages.size() - 1).getTypingNotification();
				messages.remove(messages.size() - 1);
			}
			mAdapter.addMessage(convMessage);

			// Reset this boolean to load more messages when the user scrolls to
			// the top
			reachedEnd = false;

			/*
			 * We add the typing notification back if the message was sent by the user or someone in the group is still typing.
			 */
			if (convMessage.getTypingNotification() == null && typingNotification != null)
			{
				if (convMessage.isSent())
				{
					mAdapter.addMessage(new ConvMessage(typingNotification));
				}
				else if (mConversation instanceof GroupConversation)
				{
					if (!((GroupTypingNotification) typingNotification).getGroupParticipantList().isEmpty())
					{
						Log.d("TypingNotification", "Size in chat thread: " + ((GroupTypingNotification) typingNotification).getGroupParticipantList().size());
						mAdapter.addMessage(new ConvMessage(typingNotification));
					}
				}
			}
			mAdapter.notifyDataSetChanged();

			/*
			 * Don't scroll to bottom if the user is at older messages. It's possible that the user might be reading them.
			 */
			if (((convMessage != null && !convMessage.isSent()) || convMessage == null) && mConversationsView.getLastVisiblePosition() < messages.size() - 4)
			{
				return;
			}

			mConversationsView.setSelection(messages.size() - 1);
		}
	}

	private void removeMessage(ConvMessage convMessage)
	{
		boolean lastMessage = convMessage.equals(messages.get(messages.size() - 1));
		mPubSub.publish(HikePubSub.DELETE_MESSAGE, new Pair<ConvMessage, Boolean>(convMessage, lastMessage));
		mAdapter.removeMessage(convMessage);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onShown()
	{
		if (messages != null)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mConversationsView.setSelection(messages.size() - 1);
				}
			});
		}
		Log.d(getClass().getSimpleName(), "Keyboard shown");
		if (emoticonLayout != null && emoticonLayout.getVisibility() == View.VISIBLE)
		{
			onEmoticonBtnClicked(null);
		}
	}

	@Override
	public void onHidden()
	{
	}

	public void onInviteButtonClick(View v)
	{
		inviteUser();
	}

	private void showOverlay(boolean blockOverlay)
	{
		this.blockOverlay = blockOverlay;

		Utils.hideSoftKeyboard(this, mComposeView);

		if (mOverlayLayout.getVisibility() != View.VISIBLE && !isOverlayShowing && hasWindowFocus())
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
			{
			}
		});

		TextView message = (TextView) mOverlayLayout.findViewById(R.id.overlay_message);
		Button overlayBtn = (Button) mOverlayLayout.findViewById(R.id.overlay_button);
		ImageView overlayImg = (ImageView) mOverlayLayout.findViewById(R.id.overlay_image);

		mComposeView.setEnabled(false);
		String label = mConversation instanceof GroupConversation ? ((GroupConversation) mConversation).getGroupParticipantFirstName(getMsisdnMainUser()) : mLabel;
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
			formatString = getResources().getString(mConversation instanceof GroupConversation ? R.string.no_credits_gc : R.string.no_credits);
			overlayImg.setImageResource(R.drawable.ic_no_credits);
			overlayBtn.setText(mConversation instanceof GroupConversation ? R.string.invite_to_hike : R.string.invite_now);
			mOverlayLayout.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					Utils.logEvent(ChatThread.this, HikeConstants.LogEvent.INVITE_OVERLAY_DISMISS);
					onOverlayButtonClick(mOverlayLayout);
				}
			});
		}
		/* bold the blocked users name */
		String formatted = String.format(formatString, label);
		SpannableString str = new SpannableString(formatted);
		if (!(mConversation instanceof GroupConversation) || blockOverlay)
		{
			int start = formatString.indexOf("%1$s");
			str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, start + label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		message.setText(str);
	}

	private void toggleConversationMuteViewVisibility(boolean isMuted)
	{
		findViewById(R.id.conversation_mute).setVisibility(isMuted ? View.VISIBLE : View.GONE);
	}

	private void setMuteViewBackground()
	{
		findViewById(R.id.conversation_mute).setBackgroundColor(
				getResources().getColor(mAdapter.isDefaultTheme() ? R.color.updates_text : R.color.chat_thread_indicator_bg_custom_theme));
	}

	private void showThemePicker(ChatTheme preSelectedTheme)
	{

		dismissPopupWindow();

		if (emoticonLayout != null && emoticonLayout.getVisibility() == View.VISIBLE)
		{
			onEmoticonBtnClicked(null, 0, true);
		}
		Utils.hideSoftKeyboard(this, mComposeView);

		attachmentWindow = new PopupWindow(this);

		View parentView = getLayoutInflater().inflate(R.layout.chat_backgrounds, chatLayout, false);

		attachmentWindow.setContentView(parentView);

		GridView attachmentsGridView = (GridView) parentView.findViewById(R.id.attachment_grid);

		TextView chatThemeTip = (TextView) parentView.findViewById(R.id.chat_theme_tip);

		chatThemeTip.setText(mConversation instanceof GroupConversation ? R.string.chat_theme_tip_group : R.string.chat_theme_tip);
		chatThemeTip.setVisibility(mConversation.isOnhike() ? View.VISIBLE : View.GONE);

		temporaryTheme = preSelectedTheme == null ? selectedTheme : preSelectedTheme;

		attachmentsGridView.setNumColumns(getNumColumnsChatThemes());

		final ArrayAdapter<ChatTheme> gridAdapter = new ArrayAdapter<ChatTheme>(this, -1, ChatTheme.values())
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = LayoutInflater.from(ChatThread.this).inflate(R.layout.chat_bg_item, parent, false);
				}
				ChatTheme chatTheme = getItem(position);

				ImageView theme = (ImageView) convertView.findViewById(R.id.theme);
				ImageView animatedThemeIndicator = (ImageView) convertView.findViewById(R.id.animated_theme_indicator);

				animatedThemeIndicator.setVisibility(chatTheme.isAnimated() ? View.VISIBLE : View.GONE);
				theme.setBackgroundResource(chatTheme.previewResId());
				theme.setEnabled(temporaryTheme == chatTheme);

				return convertView;
			}
		};

		attachmentsGridView.setAdapter(gridAdapter);

		int selection = temporaryTheme.ordinal();
		if (tipView != null)
		{
			TipType viewTipType = (TipType) tipView.getTag();
			selection = viewTipType == TipType.CHAT_BG_FTUE ? 0 : temporaryTheme.ordinal();
		}
		attachmentsGridView.setSelection(selection);

		attachmentsGridView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				temporaryTheme = ChatTheme.values()[position];
				gridAdapter.notifyDataSetChanged();
				setChatTheme(temporaryTheme);
			}
		});

		attachmentWindow.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss()
			{
				temporaryTheme = null;

				setChatTheme(selectedTheme);

				setupActionBar(false);
				showingChatThemePicker = false;
				invalidateOptionsMenu();

				attachmentWindow = null;
			}
		});

		attachmentsGridView.requestFocus();
		attachmentWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		attachmentWindow.setOutsideTouchable(true);
		attachmentWindow.setFocusable(true);
		attachmentWindow.setWidth(LayoutParams.MATCH_PARENT);
		attachmentWindow.setHeight(LayoutParams.WRAP_CONTENT);

		try
		{
			attachmentWindow.showAsDropDown(findViewById(R.id.cb_anchor));
		}
		catch (BadTokenException e)
		{
			Log.e(getClass().getSimpleName(), "Excepetion in ChatThread ChatTheme picker", e);
		}

		FrameLayout viewParent = (FrameLayout) parentView.getParent();
		WindowManager.LayoutParams lp = (WindowManager.LayoutParams) viewParent.getLayoutParams();
		lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

		WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		windowManager.updateViewLayout(viewParent, lp);

		attachmentWindow.setTouchInterceptor(new OnTouchListener()
		{

			@Override
			public boolean onTouch(View view, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE)
				{
					return true;
				}
				return false;
			}
		});
	}

	private int getNumColumnsChatThemes()
	{
		int width = getResources().getDisplayMetrics().widthPixels;

		int chatThemePaletteMargin = 2 * getResources().getDimensionPixelSize(R.dimen.chat_theme_palette_margin);

		int chatThemeGridMargin = 2 * getResources().getDimensionPixelSize(R.dimen.chat_theme_grid_margin);

		int chatThemeGridWidth = width - chatThemeGridMargin - chatThemePaletteMargin;

		int chatThemeItemWidth = getResources().getDimensionPixelSize(R.dimen.chat_bg_item_width);

		return (int) (chatThemeGridWidth / chatThemeItemWidth);
	}

	private void setChatTheme(ChatTheme chatTheme)
	{

		if (chatTheme != ChatTheme.DEFAULT)
		{
			backgroundImage.setScaleType(chatTheme.isTiled() ? ScaleType.FIT_XY : ScaleType.CENTER_CROP);
			backgroundImage.setImageDrawable(HikeMessengerApp.getLruCache().getChatTheme(chatTheme));
		}
		else
		{
			backgroundImage.setImageResource(chatTheme.bgResId());
		}

		mAdapter.setChatTheme(chatTheme);

		setMuteViewBackground();

		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(chatTheme.headerBgResId()));
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
	}

	private void sendChatThemeMessage()
	{
		long timestamp = System.currentTimeMillis() / 1000;
		mConversationDb.setChatBackground(mContactNumber, selectedTheme.bgId(), timestamp);

		JSONObject jsonObject = new JSONObject();
		JSONObject data = new JSONObject();

		try
		{
			data.put(HikeConstants.MESSAGE_ID, Long.toString(timestamp));
			data.put(HikeConstants.BG_ID, selectedTheme.bgId());

			jsonObject.put(HikeConstants.DATA, data);
			jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.CHAT_BACKGROUD);
			jsonObject.put(HikeConstants.TO, mConversation.getMsisdn());
			jsonObject.put(HikeConstants.FROM, prefs.getString(HikeMessengerApp.MSISDN_SETTING, ""));

			ConvMessage convMessage = new ConvMessage(jsonObject, mConversation, this, true);
			sendMessage(convMessage);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void setupChatThemeActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.chat_theme_action_bar, null);

		Button saveThemeBtn = (Button) actionBarView.findViewById(R.id.save);
		View closeBtn = actionBarView.findViewById(R.id.close_action_mode);
		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		ViewGroup closeContainer = (ViewGroup) actionBarView.findViewById(R.id.close_container);

		title.setText(R.string.chat_theme);

		closeContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dismissPopupWindow();
			}
		});

		saveThemeBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{

				/*
				 * If the user has selected the same theme, we shouldn't do anything
				 */
				if (temporaryTheme != selectedTheme)
				{
					selectedTheme = temporaryTheme;
					sendChatThemeMessage();
				}
				dismissPopupWindow();

				/*
				 * If we select the new valentines theme, we need to show the nudge tut.
				 */
				if (selectedTheme == ChatTheme.VALENTINES_2)
				{
					showValentineNudgeTip();
				}
			}
		});

		actionBar.setCustomView(actionBarView);

		Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_noalpha);
		slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
		slideIn.setDuration(200);
		closeBtn.startAnimation(slideIn);
		saveThemeBtn.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_in));
	}

	private void showValentineNudgeTip()
	{
		if (prefs.getBoolean(HikeMessengerApp.SHOWN_VALENTINE_NUDGE_TIP, false))
		{
			return;
		}

		final Dialog dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.valentine_nudge_dialog);
		dialog.setCancelable(false);

		View container = dialog.findViewById(R.id.container);

		Drawable bg = new RoundedRepeatingDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.bg_valentine_dialog), getResources().getDimension(
				R.dimen.preview_corner_radius));
		container.setBackgroundDrawable(bg);

		Button done = (Button) dialog.findViewById(R.id.ok_btn);
		done.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View view)
			{
				Editor editor = prefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_VALENTINE_NUDGE_TIP, true);
				editor.commit();

				dialog.dismiss();
			}
		});

		dialog.show();
	}

	private void showFilePicker(final ExternalStorageState externalStorageState)
	{
		final boolean canShareLocation = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION);

		final boolean canShareContacts = mConversation.isOnhike();

		ArrayList<String> optionsList = new ArrayList<String>();

		optionsList.add(getString(R.string.camera));
		optionsList.add(getString(R.string.photo));
		optionsList.add(getString(R.string.video));
		optionsList.add(getString(R.string.audio));
		if (canShareLocation)
		{
			optionsList.add(getString(R.string.location));
		}
		if (canShareContacts)
		{
			optionsList.add(getString(R.string.contact));
		}

		final ArrayList<Integer> optionImagesList = new ArrayList<Integer>();
		optionImagesList.add(R.drawable.ic_attach_camera);
		optionImagesList.add(R.drawable.ic_attach_pic);
		optionImagesList.add(R.drawable.ic_attach_video);
		optionImagesList.add(R.drawable.ic_attach_music);
		if (canShareLocation)
		{
			optionImagesList.add(R.drawable.ic_attach_location);
		}
		if (canShareContacts)
		{
			optionImagesList.add(R.drawable.ic_attach_contact);
		}

		dismissPopupWindow();

		attachmentWindow = new PopupWindow(this);

		View parentView = getLayoutInflater().inflate(R.layout.attachments, chatLayout, false);

		attachmentWindow.setContentView(parentView);

		GridView attachmentsGridView = (GridView) parentView.findViewById(R.id.attachment_grid);
		attachmentsGridView.setAdapter(new ArrayAdapter<String>(this, R.layout.attachment_item, R.id.text, optionsList)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = getLayoutInflater().inflate(R.layout.attachment_item, parent, false);
				}

				ImageView attachmentImageView = (ImageView) convertView.findViewById(R.id.attachment_icon);
				TextView attachmentTextView = (TextView) convertView.findViewById(R.id.text);

				attachmentImageView.setImageResource(optionImagesList.get(position));
				attachmentTextView.setText(getItem(position));

				return convertView;
			}
		});

		attachmentsGridView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				Log.d(getClass().getSimpleName(), "Onclick: " + position);

				dismissPopupWindow();

				int requestCode;
				Intent pickIntent = new Intent();
				Intent newMediaFileIntent = null;
				/*
				 * If we're not doing a location/contact transfer, we need an external storage
				 */
				if (position != 4 && position != 5)
				{
					if (externalStorageState == ExternalStorageState.NONE)
					{
						Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
						return;
					}
				}

				switch (position)
				{
				case 0:
					requestCode = HikeConstants.IMAGE_CAPTURE_CODE;
					pickIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					selectedFile = Utils.getOutputMediaFile(HikeFileType.IMAGE, null);

					pickIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFile));
					/*
					 * For images, save the file path as a preferences since in some devices the reference to the file becomes null.
					 */
					Editor editor = prefs.edit();
					editor.putString(HikeMessengerApp.FILE_PATH, selectedFile.getAbsolutePath());
					editor.commit();

					break;
				case 2:
					requestCode = HikeConstants.VIDEO_TRANSFER_CODE;
					pickIntent.setType("video/*");
					newMediaFileIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
					newMediaFileIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, (long) (0.9 * HikeConstants.MAX_FILE_SIZE));
					break;

				case 3:
					requestCode = HikeConstants.AUDIO_TRANSFER_CODE;
					break;

				case 4:
					if (canShareLocation)
					{
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
				if (requestCode == HikeConstants.SHARE_LOCATION_CODE)
				{
					startActivityForResult(new Intent(ChatThread.this, ShareLocation.class), requestCode);
					return;
				}
				else if (requestCode == HikeConstants.AUDIO_TRANSFER_CODE)
				{
					showAudioDialog();
					return;
				}
				else if (requestCode == HikeConstants.SHARE_CONTACT_CODE)
				{
					pickIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
					startActivityForResult(pickIntent, requestCode);
					return;
				}
				Intent chooserIntent;
				if (requestCode != HikeConstants.IMAGE_CAPTURE_CODE)
				{
					pickIntent.setAction(Intent.ACTION_PICK);

					chooserIntent = Intent.createChooser(pickIntent, "");
				}
				else
				{
					chooserIntent = pickIntent;
				}

				if (externalStorageState == ExternalStorageState.WRITEABLE)
				{
					/*
					 * Cannot send a file for new videos because of an android issue http://stackoverflow.com/questions/10494839 /verifiyandsetparameter
					 * -error-when-trying-to-record-video
					 */
					if (requestCode == HikeConstants.IMAGE_CAPTURE_CODE)
					{
						if (selectedFile == null)
						{
							Log.w(getClass().getSimpleName(), "Unable to create file to store media.");
							Toast.makeText(ChatThread.this, ChatThread.this.getResources().getString(R.string.no_external_storage), Toast.LENGTH_LONG).show();
							return;
						}
					}
					if (newMediaFileIntent != null)
					{
						chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { newMediaFileIntent });
					}
				}
				Editor editor = prefs.edit();
				editor.putString(HikeMessengerApp.TEMP_NUM, mContactNumber);
				editor.putString(HikeMessengerApp.TEMP_NAME, mContactName);
				editor.commit();
				startActivityForResult(chooserIntent, requestCode);
			}
		});

		attachmentWindow.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss()
			{
				dismissPopupWindow();
			}
		});

		attachmentWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		attachmentWindow.setOutsideTouchable(true);
		attachmentWindow.setFocusable(true);
		attachmentWindow.setWidth((int) (Utils.densityMultiplier * 184));
		attachmentWindow.setHeight(LayoutParams.WRAP_CONTENT);
		attachmentWindow.showAsDropDown(findViewById(R.id.attachment_anchor), 0, -(int) (1 * Utils.densityMultiplier));
	}

	private class AudioActivityInfo
	{
		CharSequence label;

		Drawable icon;

		String packageName;

		String activityName;

		public AudioActivityInfo(CharSequence label, Drawable icon, String packageName, String activityName)
		{
			this.label = label;
			this.icon = icon;
			this.packageName = packageName;
			this.activityName = activityName;
		}
	}

	private void showAudioDialog()
	{
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/*");
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
		final List<AudioActivityInfo> audioActivityList = new ArrayList<AudioActivityInfo>();
		int maxSize = Math.min(list.size(), 2);
		for (int i = 0; i < maxSize; i++)
		{
			ActivityInfo activityInfo = list.get(i).activityInfo;
			audioActivityList.add(new AudioActivityInfo(getPackageManager().getApplicationLabel(activityInfo.applicationInfo), getPackageManager().getApplicationIcon(
					activityInfo.applicationInfo), activityInfo.packageName, activityInfo.name));
		}
		Builder builder = new Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<AudioActivityInfo>(this, android.R.layout.select_dialog_item, android.R.id.text1, audioActivityList)
		{

			public View getView(int position, View convertView, ViewGroup parent)
			{
				AudioActivityInfo audioActivityInfo = getItem(position);
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);
				tv.setText(audioActivityInfo.label);
				tv.setCompoundDrawablesWithIntrinsicBounds(audioActivityInfo.icon, null, null, null);
				tv.setCompoundDrawablePadding((int) (15 * Utils.densityMultiplier));
				return v;
			}
		};
		builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
		{

			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				AudioActivityInfo audioActivityInfo = audioActivityList.get(which);
				intent.setClassName(audioActivityInfo.packageName, audioActivityInfo.activityName);
				startActivityForResult(intent, HikeConstants.AUDIO_TRANSFER_CODE);
			}
		});

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	private enum RecorderState
	{
		IDLE, RECORDING, RECORDED, PLAYING
	}

	/**
	 * Method for displaying the record audio dialog.
	 * 
	 * @param startTime
	 *            If the recording was already ongoing when this method is called, this parameter denotes the time the recording was started
	 */
	private void showRecordingDialog()
	{
		recordingDialog = new Dialog(ChatThread.this, R.style.Theme_CustomDialog);

		recordingDialog.setContentView(R.layout.record_audio_dialog);

		final TextView recordInfo = (TextView) recordingDialog.findViewById(R.id.record_info);
		final ImageView recordImage = (ImageView) recordingDialog.findViewById(R.id.record_img);
		final Button cancelBtn = (Button) recordingDialog.findViewById(R.id.cancel_btn);
		final Button sendBtn = (Button) recordingDialog.findViewById(R.id.send_btn);
		final ImageButton recordBtn = (ImageButton) recordingDialog.findViewById(R.id.btn_record);

		recordBtn.setEnabled(true);

		recordBtn.setImageResource(R.drawable.ic_record_selector);
		sendBtn.setEnabled(false);

		recordingHandler = new Handler();

		recorderState = RecorderState.IDLE;
		// Recording already onGoing
		if (recorder != null)
		{
			initialiseRecorder(recordBtn, recordInfo, recordImage, cancelBtn, sendBtn);
			setupRecordingView(recordInfo, recordImage, recordStartTime);
		}
		// Player is playing the recording
		else if (player != null && selectedFile != null)
		{
			try
			{
				initialisePlayer(recordBtn, recordInfo, recordImage, sendBtn);
			}
			catch (IOException e)
			{
				Log.e(getClass().getSimpleName(), "Error while playing the recording", e);
				Toast.makeText(getApplicationContext(), R.string.error_play_recording, Toast.LENGTH_SHORT).show();
				setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, 0);
				stopPlayer();
			}
			setUpPlayingRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordStartTime);
		}
		// Recording has been stopped and we have a valid file to be sent
		else if (recorder == null && selectedFile != null)
		{
			setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
		}

		recordBtn.setOnTouchListener(new OnTouchListener()
		{

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				int action = event.getAction();
				if (recorderState == RecorderState.RECORDED || recorderState == RecorderState.PLAYING)
				{
					return false;
				}
				switch (action)
				{
				case MotionEvent.ACTION_DOWN:
					if (recording)
					{
						return false;
					}
					recordBtn.setPressed(true);
					// New recording
					if (recorder == null)
					{
						initialiseRecorder(recordBtn, recordInfo, recordImage, cancelBtn, sendBtn);
					}
					try
					{
						recorder.prepare();
						recorder.start();
						recordStartTime = System.currentTimeMillis();
						setupRecordingView(recordInfo, recordImage, recordStartTime);
					}
					catch (IOException e)
					{
						stopRecorder();
						recordingError(true);
						Log.e(getClass().getSimpleName(), "Failed to start recording", e);
					}
					recording = true;

					Utils.blockOrientationChange(ChatThread.this);
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					return true;
				case MotionEvent.ACTION_UP:
					if (!recording)
					{
						return false;
					}
					mHandler.postDelayed(new Runnable()
					{

						@Override
						public void run()
						{
							recordBtn.setPressed(false);
							recording = false;
							stopRecorder();
							recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
							setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
							getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

		cancelBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				recordingDialog.cancel();
			}
		});

		sendBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				recordingDialog.dismiss();
				if (selectedFile == null)
				{
					return;
				}
				initialiseFileTransfer(selectedFile.getPath(), HikeFileType.AUDIO_RECORDING, HikeConstants.VOICE_MESSAGE_CONTENT_TYPE, true, recordedTime, false);
			}
		});

		recordingDialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				stopRecorder();
				stopPlayer();
				recordingError(false);
			}
		});

		recordingDialog.show();
	}

	private void initialiseRecorder(final ImageButton recordBtn, final TextView recordInfo, final ImageView recordImage, final Button cancelBtn, final Button sendBtn)
	{
		if (recorder == null)
		{
			recorder = new MediaRecorder();
			recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			recorder.setMaxDuration(HikeConstants.MAX_DURATION_RECORDING_SEC * 1000);
			recorder.setMaxFileSize(HikeConstants.MAX_FILE_SIZE);
			selectedFile = Utils.getOutputMediaFile(HikeFileType.AUDIO_RECORDING, null);
			recorder.setOutputFile(selectedFile.getPath());
		}
		recorder.setOnErrorListener(new OnErrorListener()
		{
			@Override
			public void onError(MediaRecorder mr, int what, int extra)
			{
				stopRecorder();
				recordingError(true);
			}
		});
		recorder.setOnInfoListener(new OnInfoListener()
		{
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra)
			{
				stopRecorder();
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
				{
					recordedTime = (System.currentTimeMillis() - recordStartTime) / 1000;
					setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
				}
				else
				{
					recordingError(true);
				}
			}
		});
	}

	private void initialisePlayer(final ImageButton recordBtn, final TextView recordInfo, final ImageView recordImage, final Button sendBtn) throws IOException
	{
		if (player == null)
		{
			player = new MediaPlayer();
			player.setDataSource(selectedFile.getPath());
		}

		player.setOnCompletionListener(new OnCompletionListener()
		{
			@Override
			public void onCompletion(MediaPlayer mp)
			{
				setUpPreviewRecordingLayout(recordBtn, recordInfo, recordImage, sendBtn, recordedTime);
				stopPlayer();
			}
		});
	}

	private void stopPlayer()
	{
		if (updateRecordingDuration != null)
		{
			updateRecordingDuration.stopUpdating();
			updateRecordingDuration = null;
		}
		if (player != null)
		{
			player.stop();
			player.reset();
			player.release();
			player = null;
		}
	}

	private void setupRecordingView(TextView recordInfo, ImageView recordImage, long startTime)
	{
		recorderState = RecorderState.RECORDING;

		updateRecordingDuration = new UpdateRecordingDuration(recordInfo, recordImage, startTime, R.drawable.ic_recording);
		recordingHandler.post(updateRecordingDuration);
	}

	private void setUpPreviewRecordingLayout(ImageButton recordBtn, TextView recordText, ImageView recordImage, Button sendBtn, long duration)
	{
		recorderState = RecorderState.RECORDED;

		recordBtn.setEnabled(false);
		recordBtn.setImageResource(R.drawable.ic_big_tick);
		recordImage.setImageResource(R.drawable.ic_recorded);
		Utils.setupFormattedTime(recordText, duration);

		sendBtn.setEnabled(true);
	}

	private void setUpPlayingRecordingLayout(ImageButton recordBtn, TextView recordInfo, ImageView recordImage, Button sendBtn, long startTime)
	{
		recorderState = RecorderState.PLAYING;

		sendBtn.setEnabled(true);

		updateRecordingDuration = new UpdateRecordingDuration(recordInfo, recordImage, startTime, 0);
		recordingHandler.post(updateRecordingDuration);
	}

	private class UpdateRecordingDuration implements Runnable
	{
		private long startTime;

		private TextView durationText;

		private ImageView recordImage;

		private boolean keepUpdating = true;

		private boolean imageSet = false;

		private int imageRes;

		public UpdateRecordingDuration(TextView durationText, ImageView iv, long startTime, int imageRes)
		{
			this.durationText = durationText;
			this.recordImage = iv;
			this.startTime = startTime;
			this.imageRes = imageRes;
		}

		public void stopUpdating()
		{
			keepUpdating = false;
		}

		public long getStartTime()
		{
			return startTime;
		}

		@Override
		public void run()
		{
			long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;
			Utils.setupFormattedTime(durationText, timeElapsed);
			if (!imageSet)
			{
				recordImage.setImageResource(imageRes);
				imageSet = true;
			}
			if (keepUpdating)
			{
				recordingHandler.postDelayed(updateRecordingDuration, 500);
			}
		}
	};

	private void stopRecorder()
	{
		if (updateRecordingDuration != null)
		{
			updateRecordingDuration.stopUpdating();
			updateRecordingDuration = null;
		}
		if (recorder != null)
		{
			/*
			 * Catching RuntimeException here to prevent the app from crashing when the the media recorder is immediately stopped after starting.
			 */
			try
			{
				recorder.stop();
			}
			catch (RuntimeException e)
			{
			}
			recorder.reset();
			recorder.release();
			recorder = null;
		}
		recording = false;
	}

	private void recordingError(boolean showError)
	{
		recorderState = RecorderState.IDLE;

		if (showError)
		{
			Toast.makeText(getApplicationContext(), R.string.error_recording, Toast.LENGTH_SHORT).show();
		}
		if (selectedFile == null)
		{
			return;
		}
		if (selectedFile.exists())
		{
			selectedFile.delete();
			selectedFile = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == HikeConstants.IMAGE_CAPTURE_CODE || requestCode == HikeConstants.IMAGE_TRANSFER_CODE || requestCode == HikeConstants.VIDEO_TRANSFER_CODE || requestCode == HikeConstants.AUDIO_TRANSFER_CODE)
				&& resultCode == RESULT_OK)
		{
			if (requestCode == HikeConstants.IMAGE_CAPTURE_CODE)
			{
				selectedFile = new File(prefs.getString(HikeMessengerApp.FILE_PATH, ""));

				Editor editor = prefs.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
			}
			if (data == null && (selectedFile == null || !selectedFile.exists()))
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				clearTempData();
				return;
			}

			HikeFileType hikeFileType = (requestCode == HikeConstants.IMAGE_TRANSFER_CODE || requestCode == HikeConstants.IMAGE_CAPTURE_CODE) ? HikeFileType.IMAGE
					: requestCode == HikeConstants.VIDEO_TRANSFER_CODE ? HikeFileType.VIDEO : HikeFileType.AUDIO;

			String filePath = null;
			if (data == null || data.getData() == null)
			{
				if (selectedFile != null)
				{
					filePath = selectedFile.getAbsolutePath();
				}
				else
				{
					/*
					 * This else condition was added because of a bug in android 4.3 with recording videos. https://code.google.com/p/android/issues/detail?id=57996
					 */
					Toast.makeText(this, R.string.error_capture_video, Toast.LENGTH_SHORT).show();
					clearTempData();
					return;
				}
			}
			else
			{
				Uri selectedFileUri = Utils.makePicasaUri(data.getData());

				if (Utils.isPicasaUri(selectedFileUri.toString()))
				{
					// Picasa image
					FileTransferManager.getInstance(getApplicationContext()).uploadFile(selectedFileUri, hikeFileType, mContactNumber, mConversation.isOnhike());
					clearTempData();
					return;
				}
				else
				{
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart))
					{
						selectedFile = new File(URI.create(fileUriString));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						filePath = selectedFile.getAbsolutePath();
					}
					else
					{
						filePath = Utils.getRealPathFromUri(selectedFileUri, this);
					}
					Log.d(getClass().getSimpleName(), "File path: " + filePath);
				}
			}

			initialiseFileTransfer(filePath, hikeFileType, null, false, -1, false);
		}
		else if (requestCode == HikeConstants.SHARE_LOCATION_CODE && resultCode == RESULT_OK)
		{
			double latitude = data.getDoubleExtra(HikeConstants.Extras.LATITUDE, 0);
			double longitude = data.getDoubleExtra(HikeConstants.Extras.LONGITUDE, 0);
			int zoomLevel = data.getIntExtra(HikeConstants.Extras.ZOOM_LEVEL, 0);

			Log.d(getClass().getSimpleName(), "Share Location Lat: " + latitude + " long:" + longitude + " zoom: " + zoomLevel);
			initialiseLocationTransfer(latitude, longitude, zoomLevel);
		}
		else if (requestCode == HikeConstants.SHARE_CONTACT_CODE && resultCode == RESULT_OK)
		{
			String id = data.getData().getLastPathSegment();
			getContactData(id);
		}
		else if (resultCode == RESULT_CANCELED)
		{
			clearTempData();
			Log.d(getClass().getSimpleName(), "File transfer Cancelled");
			selectedFile = null;
		}
	}

	private void getContactData(String id)
	{
		StringBuilder mimeTypes = new StringBuilder("(");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Phone.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Email.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(StructuredPostal.CONTENT_ITEM_TYPE) + ",");
		mimeTypes.append(DatabaseUtils.sqlEscapeString(Event.CONTENT_ITEM_TYPE) + ")");

		String selection = Data.CONTACT_ID + " =? AND " + Data.MIMETYPE + " IN " + mimeTypes.toString();

		String[] projection = new String[] { Data.DATA1, Data.DATA2, Data.DATA3, Data.MIMETYPE, Data.DISPLAY_NAME };

		Cursor c = getContentResolver().query(Data.CONTENT_URI, projection, selection, new String[] { id }, null);

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
		try
		{
			while (c.moveToNext())
			{
				String mimeType = c.getString(mimeTypeIdx);

				if (!contactJson.has(HikeConstants.NAME))
				{
					String dispName = c.getString(nameIdx);
					contactJson.put(HikeConstants.NAME, dispName);
					name = dispName;
				}

				if (Phone.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (phoneNumbersJson == null)
					{
						phoneNumbersJson = new JSONArray();
						contactJson.put(HikeConstants.PHONE_NUMBERS, phoneNumbersJson);
					}

					String type = Phone.getTypeLabel(getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String msisdn = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, msisdn);
					phoneNumbersJson.put(data);

					items.add(new ContactInfoData(DataType.PHONE_NUMBER, msisdn, type));
				}
				else if (Email.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (emailsJson == null)
					{
						emailsJson = new JSONArray();
						contactJson.put(HikeConstants.EMAILS, emailsJson);
					}

					String type = Email.getTypeLabel(getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String email = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, email);
					emailsJson.put(data);

					items.add(new ContactInfoData(DataType.EMAIL, email, type));
				}
				else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (addressesJson == null)
					{
						addressesJson = new JSONArray();
						contactJson.put(HikeConstants.ADDRESSES, addressesJson);
					}

					String type = StructuredPostal.getTypeLabel(getResources(), c.getInt(data2Idx), c.getString(data3Idx)).toString();
					String address = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, address);
					addressesJson.put(data);

					items.add(new ContactInfoData(DataType.ADDRESS, address, type));
				}
				else if (Event.CONTENT_ITEM_TYPE.equals(mimeType))
				{

					if (eventsJson == null)
					{
						eventsJson = new JSONArray();
						contactJson.put(HikeConstants.EVENTS, eventsJson);
					}

					String event;
					int eventType = c.getInt(data2Idx);
					if (eventType == Event.TYPE_ANNIVERSARY)
					{
						event = getString(R.string.anniversary);
					}
					else if (eventType == Event.TYPE_OTHER)
					{
						event = getString(R.string.other);
					}
					else if (eventType == Event.TYPE_BIRTHDAY)
					{
						event = getString(R.string.birthday);
					}
					else
					{
						event = c.getString(data3Idx);
					}
					String type = event.toString();
					String eventDate = c.getString(data1Idx);

					JSONObject data = new JSONObject();
					data.put(type, eventDate);
					eventsJson.put(data);

					items.add(new ContactInfoData(DataType.EVENT, eventDate, type));
				}
			}
		}
		catch (JSONException e)
		{
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}

		Log.d(getClass().getSimpleName(), "Data of contact is : " + contactJson.toString());
		clearTempData();
		showContactDetails(items, name, contactJson, false);
	}

	public void showContactDetails(final List<ContactInfoData> items, final String name, final JSONObject contactInfo, final boolean saveContact)
	{
		contactDialog = new ContactDialog(this, R.style.Theme_CustomDialog);
		contactDialog.setContentView(R.layout.contact_share_info);

		ViewGroup parent = (ViewGroup) contactDialog.findViewById(R.id.parent);
		TextView contactName = (TextView) contactDialog.findViewById(R.id.contact_name);
		ListView contactDetails = (ListView) contactDialog.findViewById(R.id.contact_details);
		Button yesBtn = (Button) contactDialog.findViewById(R.id.btn_ok);
		Button noBtn = (Button) contactDialog.findViewById(R.id.btn_cancel);
		View accountContainer = contactDialog.findViewById(R.id.account_container);
		final Spinner accounts = (Spinner) contactDialog.findViewById(R.id.account_spinner);
		final TextView accountInfo = (TextView) contactDialog.findViewById(R.id.account_info);

		int screenHeight = getResources().getDisplayMetrics().heightPixels;
		int dialogWidth = (int) getResources().getDimension(R.dimen.contact_info_width);
		int dialogHeight = (int) (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? ((3 * screenHeight) / 4)
				: FrameLayout.LayoutParams.MATCH_PARENT);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dialogWidth, dialogHeight);
		lp.topMargin = (int) (5 * Utils.densityMultiplier);
		lp.bottomMargin = (int) (5 * Utils.densityMultiplier);

		parent.setLayoutParams(lp);

		contactDialog.setViewReferences(parent, accounts);

		yesBtn.setText(saveContact ? R.string.save : R.string.send);

		if (saveContact)
		{
			accountContainer.setVisibility(View.VISIBLE);
			accounts.setAdapter(new AccountAdapter(getApplicationContext(), getAccountList()));
			if (accounts.getSelectedItem() != null)
			{
				accountInfo.setText(((AccountData) accounts.getSelectedItem()).getName());
			}
			else
			{
				accountInfo.setText(R.string.device);
			}
		}
		else
		{
			accountContainer.setVisibility(View.GONE);
		}

		accountContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				accounts.performClick();
			}
		});

		accounts.setOnItemSelectedListener(new OnItemSelectedListener()
		{

			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
			{
				accountInfo.setText(((AccountData) accounts.getSelectedItem()).getName());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}

		});

		contactName.setText(name);
		contactDetails.setAdapter(new ArrayAdapter<ContactInfoData>(getApplicationContext(), R.layout.contact_share_item, R.id.info_value, items)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				ContactInfoData contactInfoData = getItem(position);

				TextView header = (TextView) v.findViewById(R.id.info_head);
				header.setText(contactInfoData.getDataSubType());

				TextView details = (TextView) v.findViewById(R.id.info_value);
				details.setText(contactInfoData.getData());
				return v;
			}

		});
		yesBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (saveContact)
				{
					if (accounts.getSelectedItem() != null)
					{
						saveContact(items, accounts, name);
					}
					else
					{
						Utils.addToContacts(items, name, ChatThread.this);
					}
				}
				else
				{
					initialiseContactTransfer(contactInfo);
				}
				contactDialog.dismiss();
			}
		});
		noBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				contactDialog.dismiss();
			}
		});
		contactDialog.show();
	}

	private void initialiseFileTransfer(String filePath, HikeFileType hikeFileType, String fileType, boolean isRecording, long recordingDuration, boolean isForwardingFile)
	{
		clearTempData();
		if (filePath == null)
		{
			Toast.makeText(getApplicationContext(), R.string.upload_failed, Toast.LENGTH_SHORT).show();
			return;
		}
		File file = new File(filePath);
		Log.d(getClass().getSimpleName(), "File size: " + file.length() + " File name: " + file.getName());

		if (HikeConstants.MAX_FILE_SIZE != -1 && HikeConstants.MAX_FILE_SIZE < file.length())
		{
			Toast.makeText(getApplicationContext(), R.string.max_file_size, Toast.LENGTH_SHORT).show();
			return;
		}
		FileTransferManager.getInstance(getApplicationContext()).uploadFile(mContactNumber, file, fileType, hikeFileType, isRecording, isForwardingFile, mConversation.isOnhike(),
				recordingDuration);
	}

	private void initialiseLocationTransfer(double latitude, double longitude, int zoomLevel)
	{
		clearTempData();
		FileTransferManager.getInstance(getApplicationContext()).uploadLocation(mContactNumber, latitude, longitude, zoomLevel, mConversation.isOnhike());
	}

	private void initialiseContactTransfer(JSONObject contactJson)
	{
		FileTransferManager.getInstance(getApplicationContext()).uploadContact(mContactNumber, contactJson, mConversation.isOnhike());
	}

	private void saveContact(List<ContactInfoData> items, Spinner accountSpinner, String name)
	{

		AccountData accountData = (AccountData) accountSpinner.getSelectedItem();

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		int rawContactInsertIndex = ops.size();

		ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue(RawContacts.ACCOUNT_TYPE, accountData.getType())
				.withValue(RawContacts.ACCOUNT_NAME, accountData.getName()).build());

		for (ContactInfoData contactInfoData : items)
		{
			switch (contactInfoData.getDataType())
			{
			case ADDRESS:
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
						.withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE).withValue(StructuredPostal.DATA, contactInfoData.getData())
						.withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_CUSTOM).withValue(StructuredPostal.LABEL, contactInfoData.getDataSubType()).build());
				break;
			case EMAIL:
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE).withValue(Email.DATA, contactInfoData.getData()).withValue(Email.TYPE, Email.TYPE_CUSTOM)
						.withValue(Email.LABEL, contactInfoData.getDataSubType()).build());
				break;
			case EVENT:
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE).withValue(Event.DATA, contactInfoData.getData()).withValue(Event.TYPE, Event.TYPE_CUSTOM)
						.withValue(Event.LABEL, contactInfoData.getDataSubType()).build());
				break;
			case PHONE_NUMBER:
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
						.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE).withValue(Phone.NUMBER, contactInfoData.getData()).withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
						.withValue(Phone.LABEL, contactInfoData.getDataSubType()).build());
				break;
			}
		}
		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI).withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
				.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE).withValue(StructuredName.DISPLAY_NAME, name).build());
		boolean contactSaveSuccessful;
		try
		{
			getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			contactSaveSuccessful = true;
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			contactSaveSuccessful = false;
		}
		catch (OperationApplicationException e)
		{
			e.printStackTrace();
			contactSaveSuccessful = false;
		}
		Toast.makeText(getApplicationContext(), contactSaveSuccessful ? R.string.contact_saved : R.string.contact_not_saved, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onKey(View view, int keyCode, KeyEvent event)
	{
		if ((event.getAction() == KeyEvent.ACTION_UP) && (keyCode == KeyEvent.KEYCODE_ENTER) && event.isAltPressed())
		{
			mComposeView.append("\n");
			/*
			 * micromax phones appear to fire this event twice. Doing this seems to fix the problem.
			 */
			KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN);
			return true;
		}
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// For preventing the tool tip from animating again if its already
		// showing
		outState.putBoolean(HikeConstants.Extras.OVERLAY_SHOWING, mOverlayLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.EMOTICON_SHOWING, emoticonLayout != null && emoticonLayout.getVisibility() == View.VISIBLE);
		outState.putInt(HikeConstants.Extras.EMOTICON_TYPE, emoticonType != null ? emoticonType.ordinal() : -1);
		outState.putInt(HikeConstants.Extras.WHICH_EMOTICON_SUBCATEGORY, emoticonViewPager != null ? emoticonViewPager.getCurrentItem() : -1);
		outState.putBoolean(HikeConstants.Extras.FILE_TRANSFER_DIALOG_SHOWING, filePickerDialog != null && filePickerDialog.isShowing());
		outState.putBoolean(HikeConstants.Extras.RECORDER_DIALOG_SHOWING, recordingDialog != null && recordingDialog.isShowing());
		outState.putLong(HikeConstants.Extras.RECORDER_START_TIME, updateRecordingDuration != null ? updateRecordingDuration.getStartTime() : 0);
		outState.putLong(HikeConstants.Extras.RECORDED_TIME, recordedTime);
		outState.putInt(HikeConstants.Extras.DIALOG_SHOWING, dialogShowing != null ? dialogShowing.ordinal() : -1);
		if (mContactNumber.equals(HikeConstants.FTUE_HIKEBOT_MSISDN) && findViewById(R.id.emoticon_tip).getVisibility() == View.VISIBLE)
		{
			outState.putBoolean(HikeConstants.Extras.SHOW_STICKER_TIP_FOR_EMMA, true);
		}
		if (attachmentWindow != null && attachmentWindow.isShowing() && temporaryTheme != null)
		{
			outState.putBoolean(HikeConstants.Extras.CHAT_THEME_WINDOW_OPEN, true);
			outState.putInt(HikeConstants.Extras.SELECTED_THEME, temporaryTheme.ordinal());
		}
		super.onSaveInstanceState(outState);
	}

	public void onStickerBtnClicked(View v)
	{
		onEmoticonBtnClicked(v, 0, false);
		if (!prefs.getBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, false))
		{
			/*
			 * Added this code to prevent the sticker ftue tip from showing up if the user has already used stickers.
			 */
			Editor editor = prefs.edit();
			editor.putBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, true);
			editor.commit();
		}
	}

	public void onEmoticonBtnClicked(View v)
	{
		onEmoticonBtnClicked(v, 0, false);
	}

	public void onEmoticonBtnClicked(View v, int whichSubcategory, boolean backPressed)
	{
		dismissPopupWindow();

		emoticonLayout = emoticonLayout == null ? (ViewGroup) findViewById(R.id.emoticon_layout) : emoticonLayout;
		emoticonViewPager = emoticonViewPager == null ? (ViewPager) findViewById(R.id.emoticon_pager) : emoticonViewPager;

		View eraseKey = findViewById(R.id.erase_key);

		boolean sameType = true;
		if (v != null)
		{

			int[] tabDrawables = null;

			if (v.getId() == R.id.sticker_btn)
			{
				if (tipView != null)
				{
					TipType viewTipType = (TipType) tipView.getTag();
					if (viewTipType == TipType.EMOTICON)
					{
						Utils.closeTip(TipType.EMOTICON, tipView, prefs);
						Utils.sendUILogEvent(HikeConstants.LogEvent.STICKER_FTUE_BTN_CLICK);
						tipView = null;
					}
				}
				if (emoticonType != EmoticonType.STICKERS)
				{
					sameType = false;
					emoticonType = EmoticonType.STICKERS;
				}
				eraseKey.setVisibility(View.GONE);
			}
			else
			{
				int offset = 0;
				int emoticonsListSize = 0;
				tabDrawables = new int[] { R.drawable.ic_recents_emo, R.drawable.emo_1_tab, R.drawable.emo_2_tab, R.drawable.emo_3_tab, R.drawable.emo_4_tab,
						EmoticonConstants.EMOJI_RES_IDS[0], EmoticonConstants.EMOJI_RES_IDS[109], EmoticonConstants.EMOJI_RES_IDS[162], EmoticonConstants.EMOJI_RES_IDS[294],
						EmoticonConstants.EMOJI_RES_IDS[392] };
				if (emoticonType != EmoticonType.EMOTICON)
				{
					sameType = false;
					emoticonType = EmoticonType.EMOTICON;
				}
				emoticonsListSize = EmoticonConstants.DEFAULT_SMILEY_RES_IDS.length;

				/*
				 * Checking whether we have a few emoticons in the recents category. If not we show the next tab emoticons.
				 */
				if (whichSubcategory == 0)
				{
					int startOffset = offset;
					int endOffset = startOffset + emoticonsListSize;
					int recentEmoticonsSizeReq = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? EmoticonAdapter.MAX_EMOTICONS_PER_ROW_PORTRAIT
							: EmoticonAdapter.MAX_EMOTICONS_PER_ROW_LANDSCAPE;
					int[] recentEmoticons = HikeConversationsDatabase.getInstance().fetchEmoticonsOfType(startOffset, endOffset, recentEmoticonsSizeReq);
					if (recentEmoticons.length < recentEmoticonsSizeReq)
					{
						whichSubcategory++;
					}
				}
				eraseKey.setVisibility(View.VISIBLE);
				eraseKey.setOnClickListener(new OnClickListener()
				{

					@Override
					public void onClick(View view)
					{
						mComposeView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
					}
				});
			}
			setupEmoticonLayout(emoticonType, whichSubcategory, tabDrawables);
		}

		if (emoticonLayout.getVisibility() == View.VISIBLE)
		{
			if (!sameType && v != null)
			{
				return;
			}
			mHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					emoticonLayout.setVisibility(View.GONE);
				}
			}, 65);
			if (!backPressed)
			{
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				imm.showSoftInput(mComposeView, InputMethodManager.SHOW_IMPLICIT);
			}
		}
		else
		{
			Animation slideUp = AnimationUtils.loadAnimation(ChatThread.this, android.R.anim.fade_in);
			slideUp.setDuration(400);
			emoticonLayout.setAnimation(slideUp);
			mHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					final int selection = mConversationsView.getLastVisiblePosition();

					emoticonLayout.setVisibility(View.VISIBLE);
					/*
					 * Making sure we keep the same selection as we did before showing the sticker/emoticon layout.
					 */
					mHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							mConversationsView.setSelection(selection);
						}
					});
				}
			}, 45);
			Utils.hideSoftKeyboard(this, mComposeView);
		}

	}

	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{

		@Override
		public void onPageSelected(int pageNum)
		{
			Log.d("ViewPager", "Page number: " + pageNum);
			if (emoticonType == EmoticonType.STICKERS)
			{
				StickerCategory category = StickerManager.getInstance().getCategoryForIndex(pageNum);
				StickerCategoryId categoryId = category.categoryId;
				if (StickerCategoryId.recent.equals(categoryId))
					return;
				if (!categoryId.equals(StickerManager.StickerCategoryId.humanoid) && !categoryId.equals(StickerManager.StickerCategoryId.doggy))
				{
					if ((!StickerManager.getInstance().checkIfStickerCategoryExists(categoryId.name()) || !prefs.getBoolean(categoryId.downloadPref(), false))
							&& !StickerManager.getInstance().isStickerDownloading(categoryId.name()))
					{
						showStickerPreviewDialog(category);
					}
				}
				else if (!prefs.getBoolean(categoryId.downloadPref(), false))
				{
					showStickerPreviewDialog(category);
				}
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
		}

		@Override
		public void onPageScrollStateChanged(int arg0)
		{
		}
	};

	private void showStickerPreviewDialog(final StickerCategory category)
	{
		final Dialog dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sticker_preview_dialog);

		View parent = dialog.findViewById(R.id.preview_container);

		setupStickerPreviewDialog(parent, category.categoryId);

		Button okBtn = (Button) dialog.findViewById(R.id.ok_btn);
		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
				Editor editor = prefs.edit();
				try
				{
					editor.putBoolean(category.categoryId.downloadPref(), true);
					if (category.categoryId.equals(StickerCategoryId.recent) || category.categoryId.equals(StickerCategoryId.humanoid)
							|| category.categoryId.equals(StickerCategoryId.doggy))
					{
						return;
					}
					DownloadStickerTask downloadStickerTask = new DownloadStickerTask(ChatThread.this, category, DownloadType.NEW_CATEGORY, null);
					Utils.executeFtResultAsyncTask(downloadStickerTask);

					StickerManager.getInstance().insertTask(category.categoryId.name(), downloadStickerTask);
					updateStickerCategoryUI(category, false, null);

				}
				finally
				{
					editor.commit();
				}
			}
		});

		dialog.setOnCancelListener(new OnCancelListener()
		{

			/*
			 * If user cancels non fixed category , he should be taken to recents if not empty else to humanoid whose index is 1
			 */
			@Override
			public void onCancel(DialogInterface dialog)
			{
				if (!category.categoryId.equals(StickerCategoryId.recent) && !category.categoryId.equals(StickerCategoryId.humanoid)
						&& !category.categoryId.equals(StickerCategoryId.doggy) && !StickerManager.getInstance().checkIfStickerCategoryExists(category.categoryId.name()))
				{
					int idx = 0;
					if (StickerManager.getInstance().getRecentStickerList().size() == 0)
						idx = 1;
					emoticonViewPager.setCurrentItem(idx, false);
				}
			}
		});

		dialog.show();
	}

	private void setupStickerPreviewDialog(View parent, StickerCategoryId categoryId)
	{
		GradientDrawable parentDrawable = (GradientDrawable) parent.getBackground();
		Button stickerBtn = (Button) parent.findViewById(R.id.ok_btn);
		TextView category = (TextView) parent.findViewById(R.id.preview_text);
		View divider = parent.findViewById(R.id.divider);
		ImageView sticker = (ImageView) parent.findViewById(R.id.preview_image);

		int resParentBg = 0;
		int stickerBtnBg = 0;
		int stickerBtnText = 0;
		int stickerBtnTextColor = 0;
		int stickerBtnShadowColor = 0;
		String categoryText = null;
		int categoryTextColor = 0;
		int categoryTextShadowColor = 0;
		int dividerBg = 0;

		switch (categoryId)
		{
		case humanoid:
			resParentBg = getResources().getColor(R.color.humanoid_bg);

			stickerBtnBg = R.drawable.humanoid_btn;
			stickerBtnText = android.R.string.ok;
			stickerBtnTextColor = getResources().getColor(R.color.humanoid_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.humanoid_btn_text_shadow);

			categoryText = "Hikins";
			categoryTextColor = getResources().getColor(R.color.humanoid_text);
			categoryTextShadowColor = getResources().getColor(R.color.humanoid_text_shadow);

			dividerBg = getResources().getColor(R.color.humanoid_div);
			break;
		case doggy:
			resParentBg = getResources().getColor(R.color.doggy_bg);

			stickerBtnBg = R.drawable.doggy_btn;
			stickerBtnText = android.R.string.ok;
			stickerBtnTextColor = getResources().getColor(R.color.doggy_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.doggy_btn_text_shadow);

			categoryText = "Snuggles";
			categoryTextColor = getResources().getColor(R.color.doggy_text);
			categoryTextShadowColor = getResources().getColor(R.color.doggy_text_shadow);

			dividerBg = getResources().getColor(R.color.doggy_div);
			break;
		case kitty:
			resParentBg = getResources().getColor(R.color.kitty_bg);

			stickerBtnBg = R.drawable.kitty_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.kitty_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.kitty_btn_text_shadow);

			categoryText = "Miley";
			categoryTextColor = getResources().getColor(R.color.kitty_text);
			categoryTextShadowColor = getResources().getColor(R.color.kitty_text_shadow);

			dividerBg = getResources().getColor(R.color.kitty_div);
			break;
		case expressions:
			resParentBg = getResources().getColor(R.color.exp_bg);

			stickerBtnBg = R.drawable.exp_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.exp_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.exp_btn_text_shadow);

			categoryText = "Expressions";
			categoryTextColor = getResources().getColor(R.color.exp_text);
			categoryTextShadowColor = getResources().getColor(R.color.exp_text_shadow);

			dividerBg = getResources().getColor(R.color.exp_div);
			break;
		case bollywood:
			resParentBg = getResources().getColor(R.color.bollywood_bg);

			stickerBtnBg = R.drawable.bollywood_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.bollywood_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.bollywood_btn_text_shadow);

			categoryText = "Bollywood";
			categoryTextColor = getResources().getColor(R.color.bollywood_text);
			categoryTextShadowColor = getResources().getColor(R.color.bollywood_text_shadow);

			dividerBg = getResources().getColor(R.color.bollywood_div);
			break;
		case rageface:
			resParentBg = getResources().getColor(R.color.rf_bg);

			stickerBtnBg = R.drawable.rf_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.rf_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.rf_btn_text_shadow);

			categoryText = "Rage Face";
			categoryTextColor = getResources().getColor(R.color.rf_text);
			categoryTextShadowColor = getResources().getColor(R.color.rf_text_shadow);

			dividerBg = getResources().getColor(R.color.rf_div);
			break;
		case humanoid2:
			resParentBg = getResources().getColor(R.color.humanoid2_bg);

			stickerBtnBg = R.drawable.humanoid2_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.humanoid2_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.humanoid2_btn_text_shadow);

			categoryText = "You and I";
			categoryTextColor = getResources().getColor(R.color.humanoid2_text);
			categoryTextShadowColor = getResources().getColor(R.color.humanoid2_text_shadow);

			dividerBg = getResources().getColor(R.color.humanoid2_div);
			break;
		case smileyexpressions:
			resParentBg = getResources().getColor(R.color.se_bg);

			stickerBtnBg = R.drawable.se_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.se_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.se_btn_text_shadow);

			categoryText = "Goofy Smileys";
			categoryTextColor = getResources().getColor(R.color.se_text);
			categoryTextShadowColor = getResources().getColor(R.color.se_text_shadow);

			dividerBg = getResources().getColor(R.color.se_div);
			break;
		case avatars:
			resParentBg = getResources().getColor(R.color.avtars_bg);

			stickerBtnBg = R.drawable.avtars_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.avtars_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.avtars_btn_text_shadow);

			categoryText = "Hikin Avatars";
			categoryTextColor = getResources().getColor(R.color.avtars_text);
			categoryTextShadowColor = getResources().getColor(R.color.avtars_text_shadow);

			dividerBg = getResources().getColor(R.color.avtars_div);
			break;
		case indian:
			resParentBg = getResources().getColor(R.color.indian_bg);

			stickerBtnBg = R.drawable.indian_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.indian_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.indian_btn_text_shadow);

			categoryText = "Things Indians Say";
			categoryTextColor = getResources().getColor(R.color.indian_text);
			categoryTextShadowColor = getResources().getColor(R.color.indian_text_shadow);

			dividerBg = getResources().getColor(R.color.indian_div);
			break;
		case love:
			resParentBg = getResources().getColor(R.color.love_bg);

			stickerBtnBg = R.drawable.love_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.love_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.love_btn_text_shadow);

			categoryText = "I Love You";
			categoryTextColor = getResources().getColor(R.color.love_text);
			categoryTextShadowColor = getResources().getColor(R.color.love_text_shadow);

			dividerBg = getResources().getColor(R.color.love_div);
			break;
		case angry:
			resParentBg = getResources().getColor(R.color.angry_bg);

			stickerBtnBg = R.drawable.angry_btn;
			stickerBtnText = R.string.download;
			stickerBtnTextColor = getResources().getColor(R.color.angry_btn_text);
			stickerBtnShadowColor = getResources().getColor(R.color.angry_btn_text_shadow);

			categoryText = "Hot Heads";
			categoryTextColor = getResources().getColor(R.color.angry_text);
			categoryTextShadowColor = getResources().getColor(R.color.angry_text_shadow);

			dividerBg = getResources().getColor(R.color.angry_div);
			break;
		}

		parentDrawable.setColor(resParentBg);
		sticker.setImageResource(categoryId.previewResId());

		stickerBtn.setBackgroundResource(stickerBtnBg);
		stickerBtn.setText(stickerBtnText);
		stickerBtn.setTextColor(stickerBtnTextColor);
		stickerBtn.setShadowLayer(0.7f, 0, 0.7f, stickerBtnShadowColor);

		category.setText(categoryText);
		category.setTextColor(categoryTextColor);
		category.setShadowLayer(0.6f, 0, 0.6f, categoryTextShadowColor);

		divider.setBackgroundColor(dividerBg);
	}

	private void updateStickerCategoryUI(StickerCategory category, boolean failed, DownloadType downloadTypeBeforeFail)
	{
		if (emoticonsAdapter == null && (emoticonsAdapter instanceof StickerAdapter))
		{
			return;
		}

		View emoticonPage = emoticonViewPager.findViewWithTag(category.categoryId);

		if (emoticonPage == null)
		{
			return;
		}

		((StickerAdapter) emoticonsAdapter).setupStickerPage(emoticonPage, category, failed, downloadTypeBeforeFail);

	}

	public int getCurrentPage()
	{
		if (emoticonViewPager == null || emoticonType != EmoticonType.STICKERS)
		{
			return -1;
		}
		return emoticonViewPager.getCurrentItem();
	}

	private void setupEmoticonLayout(EmoticonType emoticonType, int pageNum, int[] categoryResIds)
	{
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		if (emoticonType != EmoticonType.STICKERS)
		{
			emoticonsAdapter = new EmoticonAdapter(this, mComposeView, isPortrait, categoryResIds);
		}
		else
		{
			emoticonsAdapter = new StickerAdapter(this, isPortrait);
		}

		emoticonViewPager.setAdapter(emoticonsAdapter);
		int actualPageNum = pageNum;
		if (emoticonType == EmoticonType.STICKERS && pageNum == 0)
		{
			// if recent list is empty, then skip to first category
			if (StickerManager.getInstance().getRecentStickerList().size() == 0)
				actualPageNum = 1;
		}
		emoticonViewPager.setCurrentItem(actualPageNum, false);
		emoticonViewPager.invalidate();

		iconPageIndicator = (StickerEmoticonIconPageIndicator) findViewById(R.id.icon_indicator);
		iconPageIndicator.setViewPager(emoticonViewPager);
		iconPageIndicator.setOnPageChangeListener(onPageChangeListener);
		iconPageIndicator.notifyDataSetChanged();
		iconPageIndicator.setCurrentItem(actualPageNum);

		onPageChangeListener.onPageSelected(pageNum);
	}

	private void toggleGroupLife(boolean alive)
	{
		((GroupConversation) mConversation).setGroupAlive(alive);
		this.mSendBtn.setEnabled(false);
		this.mComposeView.setVisibility(alive ? View.VISIBLE : View.INVISIBLE);
		findViewById(R.id.emo_btn).setEnabled(alive ? true : false);
		findViewById(R.id.sticker_btn).setEnabled(alive ? true : false);
		if (emoticonLayout != null && emoticonLayout.getVisibility() == View.VISIBLE)
		{
			onEmoticonBtnClicked(null);
		}
	}

	private String getMsisdnMainUser()
	{
		return mConversation instanceof GroupConversation ? ((GroupConversation) mConversation).getGroupOwner() : mContactNumber;
	}

	@Override
	public void onFinish(boolean success)
	{
	}

	public void sendPoke()
	{
		ConvMessage convMessage = Utils.makeConvMessage(mConversation, mContactNumber, getString(R.string.poke_msg), isConversationOnHike());

		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(HikeConstants.POKE, true);
			convMessage.setMetadata(metadata);
		}
		catch (JSONException e)
		{
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		sendMessage(convMessage);

		boolean vibrate = false;
		if (mConversation != null)
		{
			if (mConversation instanceof GroupConversation)
			{
				if (!((GroupConversation) mConversation).isMuted())
				{
					vibrate = true;
				}
			}
			else
			{
				vibrate = true;
			}
		}

		if (vibrate)
		{
			Utils.vibrateNudgeReceived(this);
		}
	}

	public void sendSticker(Sticker sticker)
	{
		ConvMessage convMessage = Utils.makeConvMessage(mConversation, mContactNumber, "Sticker", isConversationOnHike());

		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(StickerManager.CATEGORY_ID, sticker.getCategory().categoryId.name());

			metadata.put(StickerManager.STICKER_ID, sticker.getStickerId());

			metadata.put(StickerManager.STICKER_INDEX, sticker.getStickerIndex());

			convMessage.setMetadata(metadata);
			Log.d(getClass().getSimpleName(), "metadata: " + metadata.toString());
		}
		catch (JSONException e)
		{
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		sendMessage(convMessage);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		return gestureDetector.onTouchEvent(event);
	}

	SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener()
	{

		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			if (mConversation instanceof GroupConversation)
			{
				if (!((GroupConversation) mConversation).getIsGroupAlive())
				{
					return false;
				}
			}
			if (!mConversation.isOnhike() && mCredits <= 0)
			{
				boolean nativeSmsPref = Utils.getSendSmsPref(ChatThread.this);
				if (!nativeSmsPref)
				{
					return false;
				}
			}
			sendPoke();
			return true;
		}

	};

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (!reachedEnd && !loadingMoreMessages && messages != null && !messages.isEmpty() && firstVisibleItem <= HikeConstants.MIN_INDEX_TO_LOAD_MORE_MESSAGES)
		{

			int startIndex = hasSMSToggle() ? 1 : 0;
			/*
			 * This should only happen in the case where the user starts a new chat and gets a typing notification.
			 */
			if (messages.size() <= startIndex || messages.get(startIndex) == null)
			{
				return;
			}

			loadingMoreMessages = true;

			List<ConvMessage> olderMessages = mConversationDb.getConversationThread(mContactNumber, mConversation.getConvId(), HikeConstants.MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME,
					mConversation, messages.get(startIndex).getMsgID());

			if (!olderMessages.isEmpty())
			{
				mAdapter.addMessages(olderMessages, startIndex);
				mAdapter.notifyDataSetChanged();
				mConversationsView.setSelection(firstVisibleItem + olderMessages.size());
			}
			else
			{
				/*
				 * This signifies that we've reached the end. No need to query the db anymore unless we add a new message.
				 */
				reachedEnd = true;
			}

			loadingMoreMessages = false;
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		Log.d("ChatThread", "Message Adapter Scrolled State: " + scrollState);
		mAdapter.setIsListFlinging(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING);
	}

	private List<AccountData> getAccountList()
	{
		Account[] a = AccountManager.get(this).getAccounts();
		// Clear out any old data to prevent duplicates
		List<AccountData> accounts = new ArrayList<AccountData>();

		// Get account data from system
		AuthenticatorDescription[] accountTypes = AccountManager.get(this).getAuthenticatorTypes();

		// Populate tables
		for (int i = 0; i < a.length; i++)
		{
			// The user may have multiple accounts with the same name, so we
			// need to construct a
			// meaningful display name for each.
			String type = a[i].type;
			/*
			 * Only showing the user's google accounts
			 */
			if (!"com.google".equals(type))
			{
				continue;
			}
			String systemAccountType = type;
			AuthenticatorDescription ad = getAuthenticatorDescription(systemAccountType, accountTypes);
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
	 *            An array of AuthenticatorDescriptions, as returned by AccountManager.
	 * @return The description for the specified account type.
	 */
	private AuthenticatorDescription getAuthenticatorDescription(String type, AuthenticatorDescription[] dictionary)
	{
		for (int i = 0; i < dictionary.length; i++)
		{
			if (dictionary[i].type.equals(type))
			{
				return dictionary[i];
			}
		}
		// No match found
		throw new RuntimeException("Unable to find matching authenticator");
	}

	@Override
	public void startActivity(Intent intent)
	{
		try
		{
			/* Workaround for an HTC issue */
			if (intent.getComponent() != null && ".HtcLinkifyDispatcherActivity".equals(intent.getComponent().getShortClassName()))
				intent.setComponent(null);
			super.startActivity(intent);
		}
		catch (ActivityNotFoundException e)
		{
			super.startActivity(Intent.createChooser(intent, null));
		}
	}

	private class FetchLastSeenTask extends AsyncTask<Void, Void, Long>
	{

		boolean retriedOnce;

		String msisdn;

		public FetchLastSeenTask(String msisdn, boolean retriedOnce)
		{
			this.msisdn = msisdn;
			if (contactInfo.getOffline() == 0)
			{
				isOnline = true;
				/*
				 * We reset this to 1 since the user's online state is stale here.
				 */
				contactInfo.setOffline(1);
			}
			this.retriedOnce = retriedOnce;
		}

		@Override
		protected Long doInBackground(Void... params)
		{
			URL url;
			try
			{
				url = new URL(AccountUtils.base + "/user/lastseen/" + mContactNumber);

				Log.d(getClass().getSimpleName(), "URL:  " + url);

				URLConnection connection = url.openConnection();
				AccountUtils.addUserAgent(connection);
				connection.addRequestProperty("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

				if (AccountUtils.ssl)
				{
					((HttpsURLConnection) connection).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
				}

				JSONObject response = AccountUtils.getResponse(connection.getInputStream());
				Log.d(getClass().getSimpleName(), "Response: " + response);
				if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
				{
					return null;
				}
				JSONObject data = response.getJSONObject(HikeConstants.DATA);
				return data.getLong(HikeConstants.LAST_SEEN);

			}
			catch (MalformedURLException e)
			{
				Log.w(getClass().getSimpleName(), e);
				return null;
			}
			catch (IOException e)
			{
				Log.w(getClass().getSimpleName(), e);
				return null;
			}
			catch (JSONException e)
			{
				Log.w(getClass().getSimpleName(), e);
				return null;
			}

		}

		@Override
		protected void onPostExecute(Long result)
		{
			if (result == null)
			{
				if (!retriedOnce)
				{
					Utils.executeLongResultTask(new FetchLastSeenTask(msisdn, true));
					return;
				}
			}
			else
			{
				/*
				 * Update current last seen value.
				 */
				long currentLastSeenValue = result;
				/*
				 * We only apply the offset if the value is greater than 0 since 0 and -1 are reserved.
				 */
				if (currentLastSeenValue > 0)
				{
					contactInfo.setOffline(1);
					contactInfo.setLastSeenTime(Utils.applyServerTimeOffset(ChatThread.this, currentLastSeenValue));
				}
				else
				{
					contactInfo.setOffline((int) currentLastSeenValue);
					contactInfo.setLastSeenTime(System.currentTimeMillis() / 1000);
				}

				HikeUserDatabase.getInstance().updateLastSeenTime(msisdn, contactInfo.getLastSeenTime());
				HikeUserDatabase.getInstance().updateIsOffline(msisdn, contactInfo.getOffline());

			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.LAST_SEEN_TIME_UPDATED, contactInfo);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(this).hasPermanentMenuKey()))
		{
			if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU)
			{
				/*
				 * For some reason the activity randomly catches this event in the background and we get an NPE when that happens with mMenu. Adding an NPE guard for that.
				 */
				if (mMenu == null)
				{
					return super.onKeyUp(keyCode, event);
				}
				mMenu.performIdentifierAction(R.id.overflow_menu, 0);
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private boolean isConversationOnHike()
	{
		return mConversation != null && mConversation.isOnhike();
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED))
			{
				// runOnUiThread(mUpdateAdapter);
				// @GM
				mAdapter.notifyDataSetChanged();
			}
			// else if(intent.getAction().equals(HikePubSub.RESUME_BUTTON_UPDATED))
			// {
			// //Log.d(getClass().getSimpleName(),"making button visible...1");
			// long msgId = intent.getLongExtra("msgId", -1);
			// if(msgId<0)
			// return;
			// //Log.d(getClass().getSimpleName(),"making button visible...2");
			// ConvMessage message = null;
			// for(int i=(messages.size()-1); i>=0; i--)
			// {
			// ConvMessage m = messages.get(i);
			// //Log.d(getClass().getSimpleName(), "comparing  : " +m.getMsgID() +" == " + msgId);
			// if(m != null && m.getMsgID() == msgId)
			// {
			// message = m;
			// break;
			// }
			// }
			// if (message == null)
			// return;
			// //Log.d(getClass().getSimpleName(),"making button visible...3");
			//
			// if(!message.getResumeButtonVisibility())
			// {
			// Log.d(getClass().getSimpleName(),"making button visible...DONE");
			// message.setResumeButtonVisibility(true);
			// mAdapter.notifyDataSetChanged();
			// }
			// }
		}
	};

	private BroadcastReceiver chatThreadReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(StickerManager.STICKERS_UPDATED))
			{
				if (iconPageIndicator != null)
				{
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							iconPageIndicator.notifyDataSetChanged();
						}
					});
				}
			}
		}
	};
}
