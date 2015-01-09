package com.bsb.hike.chatthread;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.AudioRecordView;
import com.bsb.hike.media.AudioRecordView.AudioRecordListener;
import com.bsb.hike.media.CaptureImageParser;
import com.bsb.hike.media.CaptureImageParser.CaptureImageListener;
import com.bsb.hike.media.EmoticonPicker;
import com.bsb.hike.media.EmoticonPicker.EmoticonPickerListener;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.media.PickContactParser;
import com.bsb.hike.media.PickFileParser;
import com.bsb.hike.media.PickFileParser.PickFileListener;
import com.bsb.hike.media.ShareablePopup;
import com.bsb.hike.media.ShareablePopupLayout;
import com.bsb.hike.media.StickerPicker;
import com.bsb.hike.media.StickerPicker.StickerPickerListener;
import com.bsb.hike.media.ThemePicker;
import com.bsb.hike.media.ThemePicker.ThemePickerListener;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.ComposeViewWatcher;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.ui.HikeDialog.HDialog;
import com.bsb.hike.ui.HikeDialog.HHikeDialogListener;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @generated
 */

public abstract class ChatThread extends SimpleOnGestureListener implements OverflowItemClickListener, View.OnClickListener, ThemePickerListener, BackPressListener, CaptureImageListener, PickFileListener,
		HHikeDialogListener, StickerPickerListener, EmoticonPickerListener, AudioRecordListener, LoaderCallbacks<Object>, OnItemLongClickListener, OnTouchListener,
		OnScrollListener, Listener
{
	private static final String TAG = "chatthread";

	protected static final int FETCH_CONV = 1;

	protected static final int LOAD_MORE_MESSAGES = 2;

	protected static final int SHOW_TOAST = 3;

	protected static final int MESSAGE_RECEIVED = 4;
	
	protected static final int END_TYPING_CONVERSATION = 5;
	
	protected static final int TYPING_CONVERSATION = 6;

	protected static final int NOTIFY_DATASET_CHANGED = 7;
	
	protected static final int UPDATE_AVATAR = 8;
	
	protected static final int FILE_MESSAGE_CREATED = 9;
	
	protected static final int DELETE_MESSAGE = 10;
	
	protected static final int CHAT_THEME = 11;
	
	protected static final int CLOSE_CURRENT_STEALTH_CHAT = 12;
	/**
	 * Skipping the number '13' intentionally. 
	 * #triskaidekaphobia
	 */
	protected static final int CLOSE_PHOTO_VIEWER_FRAGMENT = 14;
	
	protected static final int BLOCK_UNBLOCK_USER = 15;
	
	protected ChatThreadActivity activity;

	protected ThemePicker themePicker;

	protected AttachmentPicker attachmentPicker;

	protected HikeSharedPreferenceUtil sharedPreference;

	protected ChatTheme currentTheme;

	protected String msisdn;

	protected static StickerPicker mStickerPicker;

	protected static EmoticonPicker mEmoticonPicker;

	protected static ShareablePopupLayout mShareablePopupLayout;

	protected AudioRecordView audioRecordView;

	protected Conversation mConversation;

	protected HikeConversationsDatabase mConversationDb;

	protected MessagesAdapter mAdapter;

	protected List<ConvMessage> messages;

	protected static HashMap<Long, ConvMessage> mMessageMap;

	protected boolean isActivityVisible = true;

	protected boolean reachedEnd = false;

	private String[] mPubSubListeners;

	protected ListView mConversationsView;

	protected ComposeViewWatcher mComposeViewWatcher;
	
	private int unreadMessageCount = 0;

	protected EditText mComposeView;
	
	private GestureDetector mGestureDetector;
	
	protected View tipView;
	
	protected View mActionBarView;
	
	protected boolean blockOverlay;
	
	protected boolean mUserIsBlocked;
	
	private boolean wasThemeClicked;

	protected Handler uiHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			handleUIMessage(msg);
		}

	};
	
	
	/**
	 * This method is called from the UI Handler's handleMessage. All the tasks performed by this are supposed to run on the UI Thread only.
	 * 
	 * This is also overriden by {@link OneToOneChatThread} and {@link GroupChatThread}
	 * @param msg
	 */
	protected void handleUIMessage(android.os.Message msg)
	{
		switch (msg.what)
		{
		case SHOW_TOAST:
			showToast(msg.arg1);
			break;
		case MESSAGE_RECEIVED:
			addMessage((ConvMessage) msg.obj);
			break;
		case NOTIFY_DATASET_CHANGED:
			Logger.i(TAG, "notifying data set changed on UI Handler");
			mAdapter.notifyDataSetChanged();
			break;
		case END_TYPING_CONVERSATION:
			setTypingText(false, (TypingNotification) msg.obj);
			break;
		case TYPING_CONVERSATION:
			setTypingText(true, (TypingNotification) msg.obj);
			break;
		case FILE_MESSAGE_CREATED:
			addMessage((ConvMessage) msg.obj);
			break;
		case DELETE_MESSAGE:
			deleteMessages((Pair<Boolean, ArrayList<Long>>) msg.obj);
			break;
		case CHAT_THEME:
			changeChatTheme((ChatTheme) msg.obj);
			break;
		case CLOSE_CURRENT_STEALTH_CHAT:
			closeStealthChat();
			break;
		case CLOSE_PHOTO_VIEWER_FRAGMENT:
			removeFragment(HikeConstants.IMAGE_FRAGMENT_TAG, true);
			break;
		case BLOCK_UNBLOCK_USER:
			blockUnBlockUser((boolean) msg.obj);
			break;
		default:
			Logger.d(TAG, "Did not find any matching event for msg.what : " + msg.what);
			break;
		}
	}

	protected void addMessage(ConvMessage convMessage, boolean scrollToLast)
	{

		mAdapter.addMessage(convMessage);
		
		addtoMessageMap(messages.size() - 1, messages.size());

		mAdapter.notifyDataSetChanged();

		// Reset this boolean to load more messages when the user scrolls to
		// the top
		reachedEnd = false;
		if (scrollToLast)
		{
			mConversationsView.setSelection(mAdapter.getCount());
		}

		// TODO : THIS IS TO BE BASED ON PRODUCT CALL
		/*
		 * Don't scroll to bottom if the user is at older messages. It's possible that the user might be reading them.
		 */
		
		if (((convMessage != null && !convMessage.isSent()) || convMessage == null) && mConversationsView.getLastVisiblePosition() < messages.size() - 4)
		{
			if (convMessage.getTypingNotification() == null
					&& (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO || convMessage.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE))
			{
				showUnreadCountIndicator();
			}
			return;
		}
		else
		{
			mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		}
		
		/*
		 * Resetting the transcript mode once the list has scrolled to the bottom.
		 */
		/*
		 * mHandler.post(new Runnable() {
		 * 
		 * @Override public void run() { mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED); } });
		 */

	}

	protected void addMessage(ConvMessage convMessage)
	{
		addMessage(convMessage, false);
	}

	public ChatThread(ChatThreadActivity activity, String msisdn)
	{
		this.activity = activity;
		this.msisdn = msisdn;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public MessageSenderLayout messageSenderLayout;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public HikeActionBar mActionBar;

	public void onCreate(Bundle arg0)
	{
		init();
		setContentView();
		fetchConversation(true);
	}

	protected void init()
	{
		mActionBar = new HikeActionBar(activity);
		mConversationDb = HikeConversationsDatabase.getInstance();
		mUserIsBlocked = ContactManager.getInstance().isBlocked(msisdn);
	}

	/**
	 * This function must be called after setting content view
	 */
	protected void initView()
	{
		initShareablePopup();

		addOnClickListeners();

		audioRecordView = new AudioRecordView(activity, this);
		mComposeView = (EditText) activity.findViewById(R.id.msg_compose);
	}

	/**
	 * Instantiate the mShareable popupLayout
	 */
	private void initShareablePopup()
	{
		if (mShareablePopupLayout == null)
		{
			int[] mEatOuterTouchIds = new int[] { R.id.sticker_btn, R.id.emoticon_btn, R.id.send_message };

			List<ShareablePopup> sharedPopups = new ArrayList<ShareablePopup>();

			initStickerPicker();
			initEmoticonPicker();

			sharedPopups.add(mEmoticonPicker);
			sharedPopups.add(mStickerPicker);
			mShareablePopupLayout = new ShareablePopupLayout(activity.getApplicationContext(), activity.findViewById(R.id.chatThreadParentLayout),
					(int) (activity.getResources().getDimension(R.dimen.emoticon_pallete)), mEatOuterTouchIds);
		}

		else
		{
			updateSharedPopups();
		}

	}

	/**
	 * Updates the mainView for KeyBoard popup as well as updates the Picker Listeners for Emoticon and Stickers
	 */
	private void updateSharedPopups()
	{
		mShareablePopupLayout.updateMainView(activity.findViewById(R.id.chatThreadParentLayout));
		mStickerPicker.updateListener(this);
		mEmoticonPicker.updateListener(this);
	}

	private void addOnClickListeners()
	{
		activity.findViewById(R.id.sticker_btn).setOnClickListener(this);
		activity.findViewById(R.id.emoticon_btn).setOnClickListener(this);
		activity.findViewById(R.id.send_message).setOnClickListener(this);
		activity.findViewById(R.id.new_message_indicator).setOnClickListener(this);
		activity.findViewById(R.id.scroll_bottom_indicator).setOnClickListener(this);
	}

	private void initStickerPicker()
	{
		mStickerPicker = new StickerPicker(activity.getApplicationContext(), this);
	}

	private void initEmoticonPicker()
	{
		mEmoticonPicker = new EmoticonPicker(activity.getApplicationContext(), this);
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		// overflow is common between all, one to one and group
		menu.findItem(R.id.overflow_menu).getActionView().setOnClickListener(this);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return false;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.attachment:
			showAttchmentPicker();
			return true;
		}
		return false;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Logger.i(TAG, "on activity result " + requestCode + " result " + resultCode);
		switch (requestCode)
		{
		case AttachmentPicker.CAMERA:
			CaptureImageParser.parseOnActivityResult(activity, resultCode, data, this);
			break;
		case AttachmentPicker.AUDIO:
		case AttachmentPicker.VIDEO:
			PickFileParser.OnActivityResult(requestCode, resultCode, data, this, activity);
			break;
		case AttachmentPicker.LOCATOIN:
			break;
		case AttachmentPicker.CONTACT:
			contactOnActivityResult(resultCode, data);
			break;
		}
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case R.string.clear_chat:
			showClearConversationDialog();
			break;
		case R.string.email_chat:
			break;
		case AttachmentPicker.GALLERY:
			startHikeGallary(true);
			break;
		}
	}

	protected String getString(int stringId)
	{
		return activity.getString(stringId);
	}

	protected Resources getResources()
	{
		return activity.getResources();
	}

	public void setContentView()
	{
		activity.setContentView(getContentView());
		initView();
	}

	protected OverFlowMenuItem[] getOverFlowMenuItems()
	{
		return new OverFlowMenuItem[] { new OverFlowMenuItem(getString(R.string.clear_chat), 0, 0, R.string.clear_chat),
				new OverFlowMenuItem(getString(R.string.email_chat), 0, 0, R.string.email_chat) };
	}

	protected void showOverflowMenu()
	{
		int width = getResources().getDimensionPixelSize(R.dimen.overflow_menu_width);
		int rightMargin = width + getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);
		mActionBar.showOverflowMenu(width, LayoutParams.WRAP_CONTENT, -rightMargin, -(int) (0.5 * Utils.densityMultiplier), activity.findViewById(R.id.attachment_anchor));
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.overflowmenu:
			showOverflowMenu();
			break;
		case R.id.sticker_btn:
			stickerClicked();
			break;
		case R.id.emoticon_btn:
			emoticonClicked();
			break;
		case R.id.send_message:
			sendButtonClicked();
			break;
		case R.id.new_message_indicator:
			unreadCounterClicked();
			break;
		case R.id.scroll_bottom_indicator:
			bottomScrollIndicatorClicked();
			break;
		case R.id.back:
			//onActionBarBackPressed();
			break;
		case R.id.contact_info:
			openProfileScreen();
			break;
		case R.id.overlay_layout:
			/**
			 * Do nothing. We simply eat this event to avoid chat thread window from catching this
			 */
			break;
		case R.id.overlay_button:
			onOverlayLayoutClicked();
			break;
		default:
			Logger.e(TAG, "onClick Registered but not added in onClick : " + v.toString());
			break;
		}

	}

	protected void sendButtonClicked()
	{
		if (TextUtils.isEmpty(mComposeView.getText()))
		{
			audioRecordClicked();
		}
		else
		{
			sendMessage();
		}
	}

	/**
	 * This function gets text from compose edit text and makes a message and uses {@link ChatThread#sendMessage(ConvMessage)} to send message
	 * 
	 * In addition, it calls {@link ComposeViewWatcher#onMessageSent()}
	 */
	protected void sendMessage()
	{
		String message = mComposeView.getText().toString();
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, message, isOnHike());
		// TODO : PinShowing related code -gaurav
		mComposeView.setText("");
		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.onMessageSent();
		}
		sendMessage(convMessage);
	}

	/**
	 * This function adds convmessage to list using
	 * 
	 * It publishes a pubsub with {@link HikePubSub#MESSAGE_SENT}
	 */
	protected void sendMessage(ConvMessage convMessage)
	{
		addMessage(convMessage, true);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);

	}

	protected void audioRecordClicked()
	{
		audioRecordView.show();
	}

	protected void stickerClicked()
	{
		mShareablePopupLayout.showPopup(mStickerPicker);
	}

	protected void emoticonClicked()
	{
		mShareablePopupLayout.showPopup(mEmoticonPicker);
	}

	protected void showThemePicker()
	{
		if (themePicker == null)
		{
			themePicker = new ThemePicker(activity, this, currentTheme);
		}
		themePicker.showThemePicker(activity.findViewById(R.id.cb_anchor), null);
	}

	protected void showAttchmentPicker()
	{
		initAttachmentPicker(true);
		int width = (int) (Utils.densityMultiplier * 270);
		int xOffset = -(int) (276 * Utils.densityMultiplier);
		int yOffset = -(int) (0.5 * Utils.densityMultiplier);
		attachmentPicker.show(width, LayoutParams.WRAP_CONTENT, xOffset, yOffset, activity.findViewById(R.id.attachment_anchor));
	}

	/**
	 * Subclasses can override and create as per their use
	 */
	protected void initAttachmentPicker(boolean addContact)
	{
		if (attachmentPicker == null)
		{
			attachmentPicker = new AttachmentPicker(this, activity, true);
			if (addContact)
			{
				attachmentPicker.appendItem(new OverFlowMenuItem(getString(R.string.contact), 0, R.drawable.ic_attach_contact, AttachmentPicker.CONTACT));
			}
		}
	}

	@Override
	public void themeClicked(ChatTheme theme)
	{
		Logger.i(TAG, "theme clicked " + theme);
		if(theme != currentTheme)
		{
			wasThemeClicked = true;
		}
		
		updateUIAsPerTheme(theme);
	}

	@Override
	public void themeSelected(ChatTheme chatTheme)
	{
		Logger.i(TAG, "theme selected " + chatTheme);
		
		/**
		 *  Save current theme and send chat theme message
		 */
		if(currentTheme != chatTheme)
		{
			currentTheme = chatTheme;
			sendChatThemeMessage();
		}
	}

	protected boolean updateUIAsPerTheme(ChatTheme theme)
	{
		if (theme != null && currentTheme != theme)
		{
			Logger.i(TAG, "update ui for theme " + theme);
			
			setConversationTheme(theme);
			return true;
		}
		return false;
	}

	protected void setBackground(ChatTheme theme)
	{
		ImageView backgroundImage = (ImageView) activity.findViewById(R.id.background);
		if (theme == ChatTheme.DEFAULT)
		{
			backgroundImage.setImageResource(theme.bgResId());
		}
		else
		{
			backgroundImage.setScaleType(theme.isTiled() ? ScaleType.FIT_XY : ScaleType.CENTER_CROP);
			backgroundImage.setImageDrawable(Utils.getChatTheme(theme, activity));
		}
	}

	@Override
	public void themeCancelled()
	{
		Logger.i(TAG, "theme cancelled, resetting the default theme if needed.");
		if (wasThemeClicked)
		{
			wasThemeClicked = false;
			setConversationTheme(currentTheme);
		}
	}

	@Override
	public boolean onBackPressed()
	{
		if (mShareablePopupLayout != null && mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
			return true;
		}

		if (themePicker != null && themePicker.isShowing())
		{
			return themePicker.onBackPressed();
		}

		return false;
	}

	private void removePubSubListeners()
	{
		Logger.d(TAG, "removing pubSub listeners");
		HikeMessengerApp.getPubSub().removeListeners(this, mPubSubListeners);
	}

	protected void startHikeGallary(boolean onHike)
	{
		Intent imageIntent = IntentManager.getHileGallaryShare(activity.getApplicationContext(), null, onHike);
		activity.startActivityForResult(imageIntent, 1);
	}

	protected void shareCapturedImage(int resultCode, Intent data)
	{

	}

	protected void uploadFile(Uri uri, HikeFileType fileType)
	{
		Logger.i(TAG, "upload file , uri " + uri + " filetype " + fileType);
	}

	protected void UploadFile(String filePath, HikeFileType fileType)
	{
		Logger.i(TAG, "upload file , filepath " + filePath + " filetype " + fileType);
	}

	protected void showToast(int messageId)
	{
		Toast.makeText(activity, getString(messageId), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void imageCaptured(Uri uri)
	{
		uploadFile(uri, HikeFileType.IMAGE);
	}

	@Override
	public void imageCaptured(String imagePath)
	{
		UploadFile(imagePath, HikeFileType.IMAGE);
	}

	@Override
	public void imageCaptureFailed()
	{
		showToast(R.string.error_capture);
	}

	@Override
	public void pickFileSuccess(int requestCode, String filePath)
	{
		switch (requestCode)
		{
		case AttachmentPicker.AUDIO:
			UploadFile(filePath, HikeFileType.AUDIO);
			break;
		case AttachmentPicker.VIDEO:
			UploadFile(filePath, HikeFileType.VIDEO);
			break;
		}

	}

	@Override
	public void pickFileFailed(int requestCode)
	{
		switch (requestCode)
		{
		case AttachmentPicker.AUDIO:
			showToast(R.string.error_recording);
			break;
		case AttachmentPicker.VIDEO:
			showToast(R.string.error_capture_video);
			break;
		}

	}

	protected void locationOnActivityResult(Intent data)
	{
		if (data == null)
		{
			showToast(R.string.error_pick_location);
		}
		else
		{
			double latitude = data.getDoubleExtra(HikeConstants.Extras.LATITUDE, 0);
			double longitude = data.getDoubleExtra(HikeConstants.Extras.LONGITUDE, 0);
			int zoomLevel = data.getIntExtra(HikeConstants.Extras.ZOOM_LEVEL, 0);
			initialiseLocationTransfer(latitude, longitude, zoomLevel);
		}
	}

	protected void initialiseLocationTransfer(double latitude, double longitude, int zoomLevel)
	{
		FileTransferManager.getInstance(activity.getApplicationContext()).uploadLocation(null, latitude, longitude, zoomLevel, isOnHike());
	}

	protected boolean isOnHike()
	{
		return true;
	}

	protected void contactOnActivityResult(int resultCode, Intent data)
	{
		PhonebookContact contact = PickContactParser.onActivityResult(resultCode, data, activity.getApplicationContext());
		HikeDialog.showDialog(activity, HikeDialog.CONTACT_SEND_DIALOG, this, contact, getString(R.string.send), false);
	}

	protected void initialiseContactTransfer(JSONObject contactJson)
	{
		Logger.v(TAG, "initiate contact transfer " + contactJson.toString());
		// FileTransferManager.getInstance(activity.getApplicationContext()).uploadContact(null, contactJson, isOnHike());
	}

	@Override
	public void negativeClicked(int id, HDialog dialog)
	{

	}

	@Override
	public void positiveClicked(int id, HDialog dialog)
	{
		switch (id)
		{
		case HikeDialog.CONTACT_SEND_DIALOG:
			initialiseContactTransfer(((PhonebookContact) dialog.data).jsonData);
			dialog.dismiss();
			break;
		case HikeDialog.CONTACT_SAVE_DIALOG:
			break;
		}

	}

	@Override
	public void neutralClicked(int id, HDialog dialog)
	{

	}

	protected void setConversationTheme(ChatTheme theme)
	{
		System.gc();
		// messages theme changed, call adapter
		mAdapter.setChatTheme(theme);
		// action bar
		activity.updateActionBarColor(theme.headerBgResId());
		// background image
		setBackground(theme);
	}

	@Override
	public void stickerSelected(Sticker sticker, String sourceOfSticker)
	{
		Logger.i(TAG, "sticker clicked " + sticker.getStickerId() + sticker.getCategoryId() + sourceOfSticker);
		sendSticker(sticker, sourceOfSticker);
	}

	@Override
	public void emoticonSelected(int emoticonIndex)
	{
		// TODO : Implement this
		// Utils.emoticonClicked(getApplicationContext(), emoticonIndex, mComposeView);
		Logger.i(TAG, " This emoticon was selected : " + emoticonIndex);
		mShareablePopupLayout.dismiss();
	}

	@Override
	public void audioRecordSuccess(String filePath, long duration)
	{
		Logger.i(TAG, "Audio Recorded " + filePath + "--" + duration);
		// initialiseFileTransfer(filePath, null, HikeFileType.AUDIO_RECORDING, HikeConstants.VOICE_MESSAGE_CONTENT_TYPE, true, duration, false);

	}

	@Override
	public void audioRecordCancelled()
	{
		Logger.i(TAG, "Audio Recorded failed");
	}

	/**
	 * This method calls {@link #fetchConversation(String)} in UI or non UI thread, depending upon async variable For non UI, it starts asyncloader, see {@link ConversationLoader}
	 * 
	 * @param async
	 * @param convId
	 */
	protected final void fetchConversation(boolean async)
	{
		Logger.i(TAG, "fetch conversation called , isAsync " + async);
		if (async)
		{
			activity.getSupportLoaderManager().initLoader(FETCH_CONV, null, this);
		}
		else
		{
			fetchConversation();
		}
	}

	/**
	 * This method is either called in either UI thread or non UI, check {@link #fetchConversation(boolean, String)}
	 * 
	 */
	protected abstract Conversation fetchConversation();

	/**
	 * This method is called in NON UI thread when list view scrolls
	 * 
	 * @return
	 */
	protected abstract List<ConvMessage> loadMessages();

	protected abstract int getContentView();
	
	/**
	 * This method returns the main msisdn in the present chatThread.
	 * It can have different implementations in OneToOne, Group and a Bot Chat
	 * @return
	 */
	protected abstract String getMsisdnMainUser();

	/**
	 * This function is called in UI thread when conversation is fetched from DB
	 */
	protected void fetchConversationFinished(Conversation conversation)
	{
		// this function should be called only once per conversation
		Logger.i(TAG, "conversation fetch success");
		mConversation = conversation;
		/*
		 * make a copy of the message list since it's used internally by the adapter
		 * 
		 * Adapter has to show UI elements like tips, day/date of messages, unknown contact headers etc.
		 */
		messages = new ArrayList<ConvMessage>(mConversation.getMessages());
		messages.addAll(mConversation.getMessages());

		mMessageMap = new HashMap<Long, ConvMessage>();
		addtoMessageMap(0, messages.size());

		mAdapter = new MessagesAdapter(activity, messages, mConversation, null);

		initListView(); // set adapter and add clicks etc
		setupActionBar(); //Setup the action bar
		currentTheme = mConversation.getTheme();
		updateUIAsPerTheme(currentTheme);// it has to be done after setting adapter
		initMessageSenderLayout();
	}

	/*
	 * This Function initializes all components which are required to send message, in case you do not want to send message OR want to provide your own functionality, override this
	 */
	protected void initMessageSenderLayout()
	{
		initComposeViewWatcher();
		initGestureDetector();
	}

	protected void initComposeViewWatcher()
	{
		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
		}
		/* get the number of credits and also listen for changes */
		int mCredits = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);

		mComposeViewWatcher = new ComposeViewWatcher(mConversation, mComposeView, (ImageButton) activity.findViewById(R.id.send_message), mCredits,
				activity.getApplicationContext());

		mComposeViewWatcher.init();

		/* check if the send button should be enabled */
		mComposeViewWatcher.setBtnEnabled();
		mComposeView.requestFocus();

	}
	
	/**
	 * This function is used to initialize the double tap to nudge
	 */
	private void initGestureDetector()
	{
		mGestureDetector = new GestureDetector(activity.getApplicationContext(), this);
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		Logger.d(TAG, "Double Tap motion");
		sendPoke();
		return true;
	}
	
	protected void sendPoke()
	{
		ConvMessage convMessage =  Utils.makeConvMessage(msisdn, getString(R.string.poke_msg), mConversation.isOnhike());
		
		JSONObject metadata = new JSONObject();
		
		try
		{
			metadata.put(HikeConstants.POKE, true);
			convMessage.setMetadata(metadata);
		}
		
		catch (JSONException e)
		{
			Logger.e(TAG, "Invalid JSON in sendPoke() : " + e.toString());
		}
		
		sendMessage(convMessage);
		
	}

	private void initListView()
	{
		mConversationsView = (ListView) activity.findViewById(R.id.conversations_list);
		mConversationsView.setAdapter(mAdapter);
		if (mConversation.getUnreadCount() > 0)
		{
			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getTypingNotification() != null)
			{
				message = messages.get(messages.size() - 2);
			}
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD)
			{
				mConversationsView.setSelection(messages.size() - mConversation.getUnreadCount() - 1);
			}
			else
			{
				mConversationsView.setSelection(mAdapter.getCount());
			}
		}
		else
		{
			mConversationsView.setSelection(mAdapter.getCount());
		}
		mConversationsView.setOnItemLongClickListener(this);
		mConversationsView.setOnTouchListener(this);
		mConversationsView.setOnScrollListener(this);

		updateUIAsPerTheme(mConversation.getTheme());// it has to be done after setting adapter

		/**
		 * Adding PubSub, here since all the heavy work related to fetching of messages and setting up UI has been done already.
		 */
		addToPubSub();

	}

	/*
	 * This function is called in UI thread when conversation fetch is failed from DB, By default we finish activity, override in case you want to do something else
	 */
	protected void fetchConversationFailed()
	{
		Logger.e(TAG, "conversation fetch failed");
		activity.finish();
	}

	/**
	 * This function is called in UI thread when message loading is finished
	 */
	protected void loadMessagesFinished(List<ConvMessage> list)
	{
		if (list == null)
		{
			Logger.e(TAG, "load message failed");
		}
		else
		{
			mAdapter.addMessages(list, mAdapter.getCount());
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public Loader<Object> onCreateLoader(int arg0, Bundle arg1)
	{
		Logger.d(TAG, "on create loader is called " + arg0);
		if (arg0 == FETCH_CONV)
		{
			return new ConversationLoader(activity.getApplicationContext(), FETCH_CONV, this);
		}
		else if (arg0 == LOAD_MORE_MESSAGES)
		{
			return new ConversationLoader(activity.getApplicationContext(), LOAD_MORE_MESSAGES, this);
		}
		else
		{
			throw new IllegalArgumentException("On create loader is called with wrong loader id");
		}
	}

	@Override
	public void onLoadFinished(Loader<Object> arg0, Object arg1)
	{
		Logger.d(TAG, "onLoadFinished");
		ConversationLoader loader = (ConversationLoader) arg0;
		if (loader.loaderId == FETCH_CONV)
		{
			if (arg1 == null)
			{
				fetchConversationFailed();
			}
			else
			{
				fetchConversationFinished((Conversation) arg1);
			}
		}
		else if (loader.loaderId == LOAD_MORE_MESSAGES)
		{
			if (arg1 == null)
			{
				loadMessagesFinished(null);
			}
			else
			{
				loadMessagesFinished((List<ConvMessage>) arg1);
			}

		}
		else
		{
			throw new IllegalStateException("Expected data is either Conversation OR List<ConvMessages> , please check " + arg0.getClass().getCanonicalName());
		}
	}

	@Override
	public void onLoaderReset(Loader<Object> arg0)
	{

	}

	private static class ConversationLoader extends AsyncTaskLoader<Object>
	{
		int loaderId;

		private Conversation conversation;

		private List<ConvMessage> list;

		WeakReference<ChatThread> chatThread;

		public ConversationLoader(Context context, int loaderId, ChatThread chatThread)
		{
			super(context);
			Logger.i(TAG, "conversation loader object " + loaderId);
			this.loaderId = loaderId;
			this.chatThread = new WeakReference<ChatThread>(chatThread);
		}

		@Override
		public Object loadInBackground()
		{
			Logger.i(TAG, "load in background of conversation loader");
			if (chatThread.get() != null)
			{
				return loaderId == FETCH_CONV ? chatThread.get().fetchConversation() : loaderId == LOAD_MORE_MESSAGES ? chatThread.get().loadMessages() : null;
			}
			return null;
		}

		/**
		 * This has to be done due to some bug in compat library -- http://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground
		 */
		protected void onStartLoading()
		{
			Logger.i(TAG, "conversation loader onStartLoading");
			if (loaderId == FETCH_CONV && conversation != null)
			{
				deliverResult(conversation);
			}
			else if (loaderId == LOAD_MORE_MESSAGES && list != null)
			{
				deliverResult(list);
			}
			else
			{
				forceLoad();
			}
		}

	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		return mGestureDetector.onTouchEvent(event);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * This method is called when a one to one or group chat thread is instantiated
	 */
	public void loadData()
	{
		fetchConversation(true);
	}

	private void addtoMessageMap(int from, int to)
	{
		for (int i = to - 1; i >= from; i--)
		{
			ConvMessage message = messages.get(i);
			ConvMessage msg = checkNUpdateFTMsg(message);
			if (msg != null)
			{
				message = msg;
				messages.set(i, message);
			}
			addtoMessageMap(message);
		}
	}

	private ConvMessage checkNUpdateFTMsg(ConvMessage message)
	{
		if (message.isSent() && message.isFileTransferMessage())
		{
			ConvMessage msg = FileTransferManager.getInstance(activity.getApplicationContext()).getMessage(message.getMsgID());
			return msg;
		}
		return null;
	}

	public static void addtoMessageMap(ConvMessage msg)
	{
		State msgState = msg.getState();

		if (msg.getMsgID() <= 0)
		{
			return;
		}
		if (msg.isSent())
		{
			if (mMessageMap == null)
			{
				mMessageMap = new HashMap<Long, ConvMessage>();
			}

			if (msg.isFileTransferMessage())
			{
				if (TextUtils.isEmpty(msg.getMetadata().getHikeFiles().get(0).getFileKey()))
				{
					mMessageMap.put(msg.getMsgID(), msg);
					return;
				}
			}
			if (msg.isSMS())
			{
				if (msgState == State.SENT_UNCONFIRMED || msgState == State.SENT_FAILED)
				{
					mMessageMap.put(msg.getMsgID(), msg);
				}
			}
			else
			{
				if (msgState != State.SENT_DELIVERED_READ)
				{
					mMessageMap.put(msg.getMsgID(), msg);
				}
			}
		}
	}

	protected void removeFromMessageMap(ConvMessage msg)
	{
		if (mMessageMap == null)
			return;

		if (msg.isFileTransferMessage())
		{
			if (!TextUtils.isEmpty(msg.getMetadata().getHikeFiles().get(0).getFileKey()))
			{
				mMessageMap.remove(msg.getMsgID());
			}
		}
		else
		{
			mMessageMap.remove(msg.getMsgID());
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		Logger.d(TAG, "Inside onEventReceived of pubSub : " + type);

		/**
		 * Using switch on String, as it is present in JDK 7 onwards. Switch makes for a cleaner and easier to read code as well.
		 * http://stackoverflow.com/questions/338206/why-cant-i-switch-on-a-string
		 */
		switch (type)
		{
		case HikePubSub.MESSAGE_RECEIVED:
			onMessageReceived(object);
			break;
		case HikePubSub.END_TYPING_CONVERSATION:
			onEndTypingNotificationReceived(object);
			break;
		case HikePubSub.TYPING_CONVERSATION:
			onTypingConversationNotificationReceived(object);
			break;
		case HikePubSub.MESSAGE_DELIVERED:
			onMessageDelivered(object);
			break;
		case HikePubSub.SERVER_RECEIVED_MSG:
			long msgId = ((Long) object).longValue();
			setStateAndUpdateView(msgId, true);
			break;
		case HikePubSub.SERVER_RECEIVED_MULTI_MSG:
			onServerReceivedMultiMessage(object);
			break;
		case HikePubSub.ICON_CHANGED:
			onIconChanged(object);
			break;
		case HikePubSub.UPLOAD_FINISHED:
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			break;
		case HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED:
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			break;
		case HikePubSub.FILE_MESSAGE_CREATED:
			onFileMessageCreated(object);
			break;
		case HikePubSub.DELETE_MESSAGE:
			onDeleteMessage(object);
			break;
		case HikePubSub.STICKER_DOWNLOADED:
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			break;
		case HikePubSub.MESSAGE_FAILED:
			onMessageFailed(object);
			break;
		case HikePubSub.CHAT_BACKGROUND_CHANGED:
			onChatBackgroundChanged(object);
			break;
		case HikePubSub.CLOSE_CURRENT_STEALTH_CHAT:
			uiHandler.sendEmptyMessage(CLOSE_CURRENT_STEALTH_CHAT);
			break;
		case HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT:
			uiHandler.sendEmptyMessage(CLOSE_PHOTO_VIEWER_FRAGMENT);
			break;
		case HikePubSub.BLOCK_USER:
			blockUser(object, true);
			break;
		case HikePubSub.UNBLOCK_USER:
			blockUser(object, false);
			break;
		default:
			Logger.e(TAG, "PubSub Registered But Not used : " + type);
			break;
		}
	}

	/**
	 * Handles message received events in chatThread
	 * 
	 * @param object
	 */
	protected void onMessageReceived(Object object)
	{
		ConvMessage message = (ConvMessage) object;
		String senderMsisdn = message.getMsisdn();
		if (senderMsisdn == null)
		{
			Logger.wtf("ChatThread", "Message with missing msisdn:" + message.toString());
		}
		if (msisdn.equals(senderMsisdn))
		{
			if (activity.hasWindowFocus())
			{
				message.setState(ConvMessage.State.RECEIVED_READ);
				mConversationDb.updateMsgStatus(message.getMsgID(), ConvMessage.State.RECEIVED_READ.ordinal(), mConversation.getMsisdn());
				if (message.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, message.serializeDeliveryReportRead()); // handle MR
				}

				HikeMessengerApp.getPubSub().publish(HikePubSub.MSG_READ, mConversation.getMsisdn());
			}

			if (message.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
			{
				/**
				 * ParticipantInfoState == NO_INFO indicates a normal message.
				 */
				handleAbnormalMessages();
			}

			if (isActivityVisible && Utils.isPlayTickSound(activity.getApplicationContext()))
			{
				Utils.playSoundFromRaw(activity.getApplicationContext(), R.raw.received_message);
			}

			sendUIMessage(MESSAGE_RECEIVED, message);

		}
	}

	protected boolean onMessageDelivered(Object object)
	{

		Pair<String, Long> pair = (Pair<String, Long>) object;
		// If the msisdn don't match we simply return
		if (!mConversation.getMsisdn().equals(pair.first))
		{
			return false;
		}
		long msgID = pair.second;
		// TODO we could keep a map of msgId -> conversation objects
		// somewhere to make this faster
		ConvMessage msg = findMessageById(msgID);
		if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
		{
			msg.setState(ConvMessage.State.SENT_DELIVERED);

			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			return true;
		}
		return false;
	}

	protected ConvMessage findMessageById(long msgID)
	{
		if (mMessageMap == null)
			return null;

		return mMessageMap.get(msgID);
	}

	protected void handleAbnormalMessages()
	{
		// TODO DO NOTHING. Only classes which need to handle such type of messages need to override this method
		return;
	}

	protected void sendUIMessage(int what, Object data)
	{
		Message message = Message.obtain();
		message.what = what;
		message.obj = data;
		uiHandler.sendMessage(message);
	}

	/**
	 * Utility method for adding listeners for pubSub
	 * 
	 * @param listeners
	 */
	protected void addToPubSub()
	{
		mPubSubListeners = getPubSubEvents();
		Logger.d(TAG, "adding pubsub, length = " + Integer.toString(mPubSubListeners.length));
		HikeMessengerApp.getPubSub().addListeners(this, mPubSubListeners);
	}

	/**
	 * Returns pubSubListeners for ChatThread
	 * 
	 */

	private String[] getPubSubEvents()
	{
		String[] retVal;
		/**
		 * Array of pubSub listeners common to both {@link OneToOneChatThread} and {@link GroupChatThread}
		 */
		String[] commonEvents = new String[] { HikePubSub.MESSAGE_RECEIVED, HikePubSub.END_TYPING_CONVERSATION, HikePubSub.TYPING_CONVERSATION, HikePubSub.MESSAGE_DELIVERED,
				HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.SERVER_RECEIVED_MSG, HikePubSub.SERVER_RECEIVED_MULTI_MSG, HikePubSub.ICON_CHANGED, HikePubSub.UPLOAD_FINISHED,
				HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, HikePubSub.FILE_MESSAGE_CREATED, HikePubSub.DELETE_MESSAGE, HikePubSub.STICKER_DOWNLOADED, HikePubSub.MESSAGE_FAILED,
				HikePubSub.CHAT_BACKGROUND_CHANGED, HikePubSub.CLOSE_CURRENT_STEALTH_CHAT, HikePubSub.ClOSE_PHOTO_VIEWER_FRAGMENT, HikePubSub.STICKER_CATEGORY_MAP_UPDATED,
				HikePubSub.BLOCK_USER, HikePubSub.UNBLOCK_USER };

		/**
		 * Array of pubSub listeners we get from {@link OneToOneChatThread} or {@link GroupChatThread}
		 * 
		 */
		String[] moreEvents = getPubSubListeners();

		if (moreEvents == null)
		{
			retVal = new String[commonEvents.length];

			System.arraycopy(commonEvents, 0, retVal, 0, commonEvents.length);
		}

		else
		{
			retVal = new String[commonEvents.length + moreEvents.length];

			System.arraycopy(commonEvents, 0, retVal, 0, commonEvents.length);

			System.arraycopy(moreEvents, 0, retVal, commonEvents.length, moreEvents.length);

		}

		return retVal;
	}

	protected abstract String[] getPubSubListeners();

	/**
	 * Mimics the onDestroy method of an Activity. It is used to release resources help by the ChatThread instance.
	 */

	public void onDestroy()
	{
		removePubSubListeners();
	}

	/**
	 * Mimics the onPause method of an Activity.
	 */

	public void onPause()
	{
		isActivityVisible = false;
	}

	/**
	 * Mimics the onResume method of an Activity.
	 */

	public void onResume()
	{
		isActivityVisible = true;
	}
	
	private void unreadCounterClicked()
	{
		mConversationsView.setSelection(mAdapter.getCount() - unreadMessageCount - 1);
		hideUnreadCountIndicator();
	}
	
	private void hideUnreadCountIndicator()
	{
		unreadMessageCount = 0;
		activity.findViewById(R.id.new_message_indicator).setVisibility(View.GONE);
	}

	private void bottomScrollIndicatorClicked()
	{
		mConversationsView.setSelection(messages.size() - 1);
		activity.findViewById(R.id.scroll_bottom_indicator);
	}
	
	private void incrementUnreadMessageCount(int count)
	{
		unreadMessageCount += count;
	}
	
	/**
	 * Used to show the unreadCount indicator
	 */
	private void showUnreadCountIndicator()
	{
		incrementUnreadMessageCount(1);
		handleUnreadUI();
	}
	
	private void showUnreadCountIndicator(int unreadCount)
	{
		incrementUnreadMessageCount(unreadCount);
		handleUnreadUI();
	}
	
	private void handleUnreadUI()
	{
		/**
		 * fast scroll indicator and unread message should not show simultaneously
		 */

		activity.findViewById(R.id.scroll_bottom_indicator).setVisibility(View.GONE);
		activity.findViewById(R.id.new_message_indicator).setVisibility(View.VISIBLE);

		TextView indicatorText = (TextView) activity.findViewById(R.id.indicator_text);
		indicatorText.setVisibility(View.VISIBLE);
		if (unreadMessageCount == 1)
		{
			indicatorText.setText(getResources().getString(R.string.one_new_message));
		}
		else
		{
			indicatorText.setText(getResources().getString(R.string.num_new_messages, unreadMessageCount));
		}
	}
	
	private void onEndTypingNotificationReceived(Object object)
	{
		TypingNotification typingNotification = (TypingNotification) object;
		if (typingNotification == null)
		{
			return;
		}
		
		if (msisdn.equals(typingNotification.getId()))
		{
			sendUIMessage(END_TYPING_CONVERSATION, typingNotification);
		}
	}
	
	/**
	 * This is used to add Typing Conversation on the UI
	 * @param object
	 */
	protected void onTypingConversationNotificationReceived(Object object)
	{
		TypingNotification typingNotification = (TypingNotification) object;
		if (typingNotification == null)
		{
			return;
		}
		
		if (msisdn.equals(typingNotification.getId()))
		{
			sendUIMessage(TYPING_CONVERSATION, typingNotification);
		}
	}
	
	/**
	 * Adds typing notification on the UI
	 * 
	 * @param direction
	 * @param typingNotification
	 */
	protected void setTypingText(boolean direction, TypingNotification typingNotification)
	{
		if (messages.isEmpty() || messages.get(messages.size() - 1).getTypingNotification() == null)
		{
			addMessage(new ConvMessage(typingNotification));
		}
		else if (messages.get(messages.size() - 1).getTypingNotification() != null)
		{
			ConvMessage convMessage = messages.get(messages.size() - 1);
			convMessage.setTypingNotification(typingNotification);

			mAdapter.notifyDataSetChanged();
		}
	}

	protected boolean setStateAndUpdateView(long msgId, boolean updateView)
	{
		/*
		 * This would happen in the case if the events calling this method are called before the conversation is setup.
		 */
		if (mConversation == null || mAdapter == null)
		{
			return false;
		}
		ConvMessage msg = findMessageById(msgId);

		/*
		 * This is a hackish check. For some cases we were getting convMsg in another user's messageMap. which should not happen ideally. that was leading to showing hikeOfflineTip
		 * in wrong ChatThread.
		 */
		if (msg == null || TextUtils.isEmpty(msg.getMsisdn()) || !msg.getMsisdn().equals(msgId))
		{
			Logger.i("ChatThread", "We are getting a wrong msisdn convMessage object in " + msisdn + " ChatThread");
			return false;
		}

		if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_CONFIRMED.ordinal()))
		{
			if (isActivityVisible && (!msg.isTickSoundPlayed()) && Utils.isPlayTickSound(activity.getApplicationContext()))
			{
				Utils.playSoundFromRaw(activity.getApplicationContext(), R.raw.message_sent);
			}
			msg.setTickSoundPlayed(true);
			msg.setState(ConvMessage.State.SENT_CONFIRMED);

			if (updateView)
			{
				uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			}
		}
		return true;
	}
	
	/**
	 * This indicates the clock to tick for a multi forward message
	 * @param object
	 */
	private void onServerReceivedMultiMessage(Object object)
	{
		Pair<Long, Integer> p = (Pair<Long, Integer>) object;
		long baseId = p.first;
		int count = p.second;

		for (long msgId = baseId; msgId < (baseId + count); msgId++)
		{
			setStateAndUpdateView(msgId, false);
		}

		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}
	
	/**
	 * This is called when a group icon changes or a contact's dp is changed
	 * 
	 * @param object
	 */
	private void onIconChanged(Object object)
	{
		String mContactNumber = (String) object;
		if (mContactNumber.equals(msisdn))
		{
			uiHandler.sendEmptyMessage(UPDATE_AVATAR);
		}
	}

	/**
	 * This method is used to setAvatar for a contact.
	 */
	protected void setAvatar(int defaultResId)
	{
		ImageView avatar = (ImageView) mActionBarView.findViewById(R.id.avatar);
		Drawable avatarDrawable = HikeMessengerApp.getLruCache().getIconFromCache(msisdn, true);

		if (avatarDrawable != null)
		{
			avatar.setScaleType(ScaleType.FIT_CENTER);
			avatar.setImageDrawable(avatarDrawable);
			avatar.setBackgroundDrawable(null);
		}

		else
		{
			avatar.setScaleType(ScaleType.CENTER_INSIDE);
			avatar.setImageDrawable(activity.getResources().getDrawable(defaultResId));
			avatar.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(msisdn, true));
		}

	}
	
	
	/**
	 * Called from PubSub thread, when a file upload is initiated, to add the convMessage to ChatThread
	 * 
	 * @param object
	 */
	private void onFileMessageCreated(Object object)
	{
		ConvMessage convMessage = (ConvMessage) object;

		/**
		 * Ensuring that the convMessage object belongs to the conversation
		 */

		if (!(convMessage.getMsisdn().equals(msisdn)))
		{
			return;
		}

		sendUIMessage(FILE_MESSAGE_CREATED, convMessage);
	}
	
	/**
	 * Called form PubSub thread, when a message is deleted.
	 * 
	 * @param object
	 */
	private void onDeleteMessage(Object object)
	{
		Pair<ArrayList<Long>, Bundle> deleteMessage = (Pair<ArrayList<Long>, Bundle>) object;
		ArrayList<Long> msgIds = deleteMessage.first;
		Bundle bundle = deleteMessage.second;
		String msgMsisdn = bundle.getString(HikeConstants.Extras.MSISDN);

		/**
		 * Received a delete message pubsub for a different thread, we received a false event with no msgIds
		 */
		if (!(msgMsisdn.equals(msisdn)) || msgIds.isEmpty())
		{
			return;
		}

		boolean deleteMediaFromPhone = bundle.getBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE);

		sendUIMessage(DELETE_MESSAGE, new Pair<Boolean, ArrayList<Long>>(deleteMediaFromPhone, msgIds));
	}

	/**
	 * Deletes the messages based on the message Ids present in the {@link ArrayList<Long>} in {@link Pair.second}
	 * 
	 * @param pair
	 */
	private void deleteMessages(Pair<Boolean, ArrayList<Long>> pair)
	{
		for (long msgId : pair.second)
		{
			for (ConvMessage convMessage : messages)
			{
				if (convMessage.getMsgID() == msgId)
				{
					deleteMessage(convMessage, pair.first);
					break;
				}
			}
		}

		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}
	
	/**
	 * This function accomplishes the following : 1. Removes message from ChatThread and Db. 2. Removes message from {@code MessagesAdapter.undeliveredMessages} set of
	 * {@link MessagesAdapter} 3. If there is an ongoing FileTransfer, we cancel it.
	 * 
	 * @param convMessage
	 * @param deleteMediaFromPhone
	 */
	private void deleteMessage(ConvMessage convMessage, boolean deleteMediaFromPhone)
	{
		mAdapter.removeMessage(convMessage);
		if (!convMessage.isSMS() && convMessage.getState() == State.SENT_CONFIRMED)
		{
			mAdapter.removeFromUndeliverdMessage(convMessage);
			if (mAdapter.isSelected(convMessage))
			{
				mAdapter.toggleSelection(convMessage);
			}
		}

		if (convMessage.isFileTransferMessage())
		{
			// @GM cancelTask has been changed
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			File file = hikeFile.getFile();
			if (deleteMediaFromPhone && hikeFile != null)
			{
				hikeFile.delete(activity.getApplicationContext());
			}
			FileTransferManager.getInstance(activity.getApplicationContext()).cancelTask(convMessage.getMsgID(), file, convMessage.isSent());
			mAdapter.notifyDataSetChanged();
		}
	}
	
	private void onMessageFailed(Object object)
	{
		long msgId = ((Long) object).longValue();
		ConvMessage convMessage = findMessageById(msgId);
		if (convMessage != null)
		{
			convMessage.setState(ConvMessage.State.SENT_FAILED);
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
		}
	}
	
	/**
	 * Used to change the chat theme
	 * @param object
	 */
	private void onChatBackgroundChanged(Object object)
	{
		Pair<String, ChatTheme> pair = (Pair<String, ChatTheme>) object;
		
		/**
		 * Proceeding only if the chat theme is changed for the current msisdn
		 */
		if(mConversation.getMsisdn().equals(pair.first))
		{
			sendUIMessage(CHAT_THEME, pair.second);
		}
	}
	
	/**
	 * Used to close the stealth chat
	 */
	private void closeStealthChat()
	{
		saveDraft();
		activity.finish();
	}
	
	/**
	 * If the user had typed something, we save it as a draft and will show it in the edit text box when he/she comes back to this conversation.
	 */
	private void saveDraft()
	{
		if (mComposeView != null && mComposeView.getVisibility() == View.VISIBLE)
		{
			Editor editor = activity.getSharedPreferences(HikeConstants.DRAFT_SETTING, android.content.Context.MODE_PRIVATE).edit();
			if (mComposeView.length() != 0)
			{
				editor.putString(msisdn, mComposeView.getText().toString());
			}
			else
			{
				editor.remove(msisdn);
			}
			editor.commit();
		}
	}
	
	private boolean removeFragment(String tag, boolean updateActionBar)
	{
		boolean isRemoved = activity.removeFragment(tag);
		if (isRemoved && updateActionBar)
		{
			// TODO :
			// setupActionBar(false);
		}
		return isRemoved;
	}
	
	/**
	 * Utility method used for setting up the ActionBar in the ChatThread.
	 *  
	 */
	protected void setupActionBar()
	{
		mActionBarView = mActionBar.setCustomActionBarView(R.layout.chat_thread_action_bar);
		
		View backContainer = mActionBarView.findViewById(R.id.back);
		
		View contactInfoContainer = mActionBarView.findViewById(R.id.contact_info);
		
		/**
		 * Adding click listeners
		 */
		
		contactInfoContainer.setOnClickListener(this);
		backContainer.setOnClickListener(this);
	}
	
	/**
	 * Sets the label for the action bar
	 */
	protected void setLabel(String label)
	{
		TextView mLabelTextView = (TextView) mActionBarView.findViewById(R.id.contact_name);
		
		mLabelTextView.setText(label);
	}
	
	protected void playUpDownAnimation(final View view)
	{
		if (view == null)
		{
			return;
		}
		Animation an = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.down_up_up_part);
		an.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				view.setVisibility(View.GONE);
				if (view == tipView)
				{
					tipView = null;
				}
			}
		});
		view.startAnimation(an);
	}
	
	/**
	 * blockOverLay flag indicates whether this is used to block a user or not. 
	 * This function can also be called from in zero SMS Credits case.
	 * 
	 * @param blockOverlay
	 * @param label
	 * @param formatString
	 * @param overlayBtnText
	 */
	
	private void showOverlay(boolean blockOverlay, String label, String formatString, String overlayBtnText)
	{
		this.blockOverlay = blockOverlay;
		
		Utils.hideSoftKeyboard(activity.getApplicationContext(), mComposeView);

		View mOverlayLayout = activity.findViewById(R.id.overlay_layout);

		if (mOverlayLayout.getVisibility() != View.VISIBLE && activity.hasWindowFocus())
		{
			Animation fadeIn = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
			mOverlayLayout.setAnimation(fadeIn);
		}

		mComposeView.setEnabled(false);

		mOverlayLayout.setVisibility(View.VISIBLE);
		mOverlayLayout.setOnClickListener(this);

		TextView message = (TextView) mOverlayLayout.findViewById(R.id.overlay_message);
		Button overlayBtn = (Button) mOverlayLayout.findViewById(R.id.overlay_button);
		ImageView overlayImg = (ImageView) mOverlayLayout.findViewById(R.id.overlay_image);
		
		overlayBtn.setOnClickListener(this);

		mComposeView.setEnabled(false);

		if (blockOverlay)
		{
			overlayImg.setImageResource(R.drawable.ic_no);
			overlayBtn.setText(overlayBtnText);
		}
		else
		{
			mConversationDb.setOverlay(false, mConversation.getMsisdn());
			overlayImg.setImageResource(R.drawable.ic_no_credits);
			overlayBtn.setText(overlayBtnText);

		}

		/**
		 * Making the blocked user's name as bold
		 */
		String formatted = String.format(formatString, label);
		SpannableString str = new SpannableString(formatted);
		if (blockOverlay)
		{
			int start = formatString.indexOf("%1$s");
			str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, start + label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		message.setText(str);
	}

	/**
	 * Used to call {@link #showOverlay(boolean, String, String, String)} from {@link OneToOneChatThread} or {@link GroupChatThread}
	 * 
	 * @param label
	 */
	protected void showBlockOverlay(String label)
	{
		showOverlay(true, label, activity.getString(R.string.block_overlay_message), activity.getString(R.string.unblock_title));
	}
	
	/**
	 * Used to call {@link #showOverlay(boolean, String, String, String)} from {@link OneToOneChatThread} or {@link GroupChatThread}
	 * 
	 * @param label
	 * @param formatString
	 * @param overlayBtnText
	 */
	protected void showZeroCreditsOverlay(String label, String formatString, String overlayBtnText)
	{
		showOverlay(false, label, formatString, overlayBtnText);
	}
	
	private void onOverlayLayoutClicked()
	{
		if(activity.findViewById(R.id.overlay_layout).getVisibility() == View.VISIBLE && mUserIsBlocked)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, getMsisdnMainUser());
		}
		
		//TODO : Add SMS credits 0 case 
	}
	
	/**
	 * Can be used to update the title of an overflow menu item on the fly
	 * 
	 * @param itemId
	 * @param newTitle
	 */
	protected void updateOverflowMenuItemString(int itemId, String newTitle)
	{
		List<OverFlowMenuItem> mItems = mActionBar.getOverFlowMenuItems();

		for (OverFlowMenuItem overFlowMenuItem : mItems)
		{
			if (overFlowMenuItem.id == itemId)
			{	
				overFlowMenuItem.text = newTitle;
				mActionBar.overFlowMenuLayout.notifyDateSetChanged();
				break;
			}
		}

	}
	
	private void blockUser(Object object, boolean isBlocked)
	{
		String mMsisdn = (String) object;
		
		/**
		 * Proceeding only if the blocked user's msisdn is that of the current chat thread
		 */
		if(mMsisdn.equals(getMsisdnMainUser()))
		{
			sendUIMessage(BLOCK_UNBLOCK_USER, isBlocked);
		}
	}
	
	protected void hideOverlay()
	{
		View mOverlayLayout = activity.findViewById(R.id.overlay_layout);

		if (mOverlayLayout.getVisibility() == View.VISIBLE && activity.hasWindowFocus())
		{
			Animation fadeOut = AnimationUtils.loadAnimation(activity.getApplicationContext(), android.R.anim.fade_out);
			mOverlayLayout.setAnimation(fadeOut);
		}

		mOverlayLayout.setVisibility(View.INVISIBLE);
	}
	
	/**
	 * This method is overriden by {@link OneToOneChatThread} and {@link GroupChatThread}
	 * 
	 * @return
	 */
	protected String getBlockedUserLabel()
	{
		return null;
	}
	
	/**
	 * This runs only on the UI Thread
	 * @param isBlocked
	 */
	protected void blockUnBlockUser(boolean isBlocked)
	{
		mUserIsBlocked = isBlocked;

		if (isBlocked)
		{
			Utils.logEvent(activity.getApplicationContext(), HikeConstants.LogEvent.MENU_BLOCK);
			showBlockOverlay(getBlockedUserLabel());
			updateOverflowMenuItemString(R.string.block_title, activity.getString(R.string.unblock_title));
		}

		else
		{
			mComposeView.setEnabled(true);
			hideOverlay();
			updateOverflowMenuItemString(R.string.block_title, activity.getString(R.string.block_title));
		}
	}
	
	/**
	 * Used for giving block and unblock user pubSubs
	 */
	protected void onBlockUserclicked()
	{
		if(mUserIsBlocked)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, msisdn);
		}
		
		else
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, msisdn);
		}
	}
	
	/**
	 * This method is used to construct {@link ConvMessage} with a given sticker and send it.
	 * 
	 * @param sticker
	 * @param categoryIdIfUnkown
	 * @param source
	 */
	private void sendSticker(Sticker sticker, String source)
	{
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, StickerManager.STICKER_MESSAGE_TAG, mConversation.isOnhike());

		JSONObject metadata = new JSONObject();

		try
		{
			String categoryId;
			categoryId = sticker.getCategoryId();

			metadata.put(StickerManager.CATEGORY_ID, categoryId);

			metadata.put(StickerManager.STICKER_ID, sticker.getStickerId());

			/**
			 * Implies, sticker is sent from Recent stickers
			 */
			if (!(source.equalsIgnoreCase(StickerManager.FROM_OTHER)))
			{
				metadata.put(StickerManager.SEND_SOURCE, source);
			}

			convMessage.setMetadata(metadata);
		}

		catch (JSONException e)
		{
			Logger.e(TAG, "Invalid JSON for Sending sticker : " + e.toString());
		}

		sendMessage(convMessage);
	}
	
	private void sendChatThemeMessage()
	{
		long timestamp = System.currentTimeMillis() / 1000;
		mConversationDb.setChatBackground(msisdn, currentTheme.bgId(), timestamp);

		JSONObject jsonObject = new JSONObject();
		JSONObject data = new JSONObject();

		try
		{
			data.put(HikeConstants.MESSAGE_ID, Long.toString(timestamp));
			data.put(HikeConstants.BG_ID, currentTheme.bgId());

			jsonObject.put(HikeConstants.DATA, data);
			jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.CHAT_BACKGROUD);
			jsonObject.put(HikeConstants.TO, mConversation.getMsisdn());
			jsonObject.put(HikeConstants.FROM, HikeSharedPreferenceUtil.getInstance(activity.getApplicationContext()).getData(HikeMessengerApp.MSISDN_SETTING, ""));

			ConvMessage convMessage = new ConvMessage(jsonObject, mConversation, activity.getApplicationContext(), true);
			
			sendMessage(convMessage);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Called from the UI Handler to change the chat theme
	 * 
	 * @param chatTheme
	 */
	private void changeChatTheme(ChatTheme chatTheme)
	{
		updateUIAsPerTheme(chatTheme);
		
		currentTheme = chatTheme;
	}
	
	/**
	 * open Profile from action bar. Calling the child class' respective functions here
	 */
	protected void openProfileScreen()
	{
		return;
	}
	
	/**
	 * Used to show clear conversation confirmation dialog
	 */
	private void showClearConversationDialog()
	{
		final CustomAlertDialog dialog = new CustomAlertDialog(activity);
		dialog.setHeader(R.string.clear_conversation);
		dialog.setBody(R.string.confirm_clear_conversation);
		dialog.setOkButton(R.string.ok, new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				clearConversation();
				dialog.dismiss();
			}
		});
		dialog.setCancelButton(R.string.cancel);
		
		

		dialog.show();
	}
	
	/**
	 * Used to clear a user's conversation
	 */
	protected void clearConversation()
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.CLEAR_CONVERSATION, msisdn);
		messages.clear();
		
		if(mMessageMap != null)
		{
			mMessageMap.clear();
		}
		
		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
		Logger.d(TAG, "Clearing conversation");
	}
}
