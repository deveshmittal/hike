package com.bsb.hike.chatthread;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
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
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.ui.ComposeViewWatcher;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.ui.HikeDialog.HDialog;
import com.bsb.hike.ui.HikeDialog.HHikeDialogListener;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @generated
 */

public abstract class ChatThread implements OverflowItemClickListener, View.OnClickListener, ThemePickerListener, BackPressListener, CaptureImageListener, PickFileListener,
		HHikeDialogListener, StickerPickerListener, EmoticonPickerListener, AudioRecordListener, LoaderCallbacks<Object>, OnItemLongClickListener, OnTouchListener, OnScrollListener, Listener
{
	private static final String TAG = "chatthread";

	protected static final int FETCH_CONV = 1;

	protected static final int LOAD_MORE_MESSAGES = 2;

	protected static final int SHOW_TOAST = 3;
	
	protected static final int MESSAGE_RECEIVED = 4;

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

	protected Handler uiHandler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			switch (msg.what)
			{
			case SHOW_TOAST:
				showToast(msg.arg1);
				break;
			case MESSAGE_RECEIVED:
				addMessage((ConvMessage)msg.obj);
				break;
			}
		}

	};
	
	protected void addMessage(ConvMessage convMessage)
	{
		mAdapter.addMessage(convMessage);

		addtoMessageMap(messages.size() - 1 ,messages.size());

		mAdapter.notifyDataSetChanged();
		
		// Reset this boolean to load more messages when the user scrolls to
		// the top
		reachedEnd = false;


		// TODO : THIS IS TO BE BASED ON PRODUCT CALL
		/*
		 * Don't scroll to bottom if the user is at older messages. It's possible that the user might be reading them.
		 */
		
		/*if (((convMessage != null && !convMessage.isSent()) || convMessage == null) && mConversationsView.getLastVisiblePosition() < messages.size() - 4)
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
		*/
		/*
		 * Resetting the transcript mode once the list has scrolled to the bottom.
		 */
		/*mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mConversationsView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
			}
		});*/
	
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

	public ChatThreadActionBar chatThreadActionBar;

	public void onCreate(Bundle arg0)
	{
		init();
		setContentView();
		fetchConversation(true);
	}

	protected void init()
	{
		chatThreadActionBar = new ChatThreadActionBar(activity);
		mConversationDb = HikeConversationsDatabase.getInstance();

	}

	/**
	 * This function must be called after setting content view
	 */
	protected void initView()
	{
		setConversationTheme();

		initShareablePopup();

		addOnClickListeners();

		audioRecordView = new AudioRecordView(activity, this);
		
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
		switch (item.uniqueness)
		{
		case R.string.clear_chat:
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
		chatThreadActionBar.showOverflowMenu(width, LayoutParams.WRAP_CONTENT, -rightMargin, -(int) (0.5 * Utils.densityMultiplier), activity.findViewById(R.id.attachment_anchor));
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
		}

	}

	protected void sendButtonClicked()
	{
		audioRecordClicked();
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
		updateUIAsPerTheme(theme);
	}

	@Override
	public void themeSelected(ChatTheme chatTheme)
	{
		Logger.i(TAG, "theme selected " + chatTheme);
		currentTheme = chatTheme;
		// save and send theme message
	}

	protected boolean updateUIAsPerTheme(ChatTheme theme)
	{
		if (theme != null && currentTheme != theme)
		{
			Logger.i(TAG, "update ui for theme " + theme);
			currentTheme = theme;
			// messages theme changed, call adapter
			mAdapter.setChatTheme(theme);
			// action bar
			activity.updateActionBarColor(theme.headerBgResId());
			// background image
			setBackground(theme);
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
		Logger.i(TAG, "theme cancelled ");
		setConversationTheme();
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

	protected void setConversationTheme()
	{

	}

	@Override
	public void stickerSelected(Sticker sticker, String sourceOfSticker)
	{
		Logger.i(TAG, "sticker clicked " + sticker.getStickerId() + sticker.getCategoryId() + sourceOfSticker);
		mShareablePopupLayout.dismiss();
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
	 * This function is called in UI thread when conversation is fetched from DB
	 */
	protected void fetchConversationFinished(Conversation conversation)
	{
		// this function should be called only once per conversation
		Logger.i(TAG, "conversation fetch success");
		mConversation = conversation;
		/*
		 * make a copy of the message list since it's used internally by the adapter
		 */
		messages = new ArrayList<ConvMessage>(mConversation.getMessages());
		messages.addAll(mConversation.getMessages());

		mMessageMap = new HashMap<Long, ConvMessage>();
		addtoMessageMap(0, messages.size());

		mAdapter = new MessagesAdapter(activity.getApplicationContext(), mConversation.getMessages(), mConversation, null);
		initListView(); // set adapter and add clicks etc

		updateUIAsPerTheme(mConversation.getTheme());// it has to be done after setting adapter

		if (mComposeViewWatcher != null)
		{
			mComposeViewWatcher.uninit();
		}
		/* get the number of credits and also listen for changes */
		int mCredits = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);

		mComposeViewWatcher = new ComposeViewWatcher(mConversation, (EditText) activity.findViewById(R.id.messageedittext), (ImageButton) activity.findViewById(R.id.send_message),
				mCredits, activity.getApplicationContext());
	}

	private void initListView()
	{
		ListView mConversationsView = (ListView) activity.findViewById(R.id.conversations_list);
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
		// TODO Auto-generated method stub
		return false;
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

	private void removeFromMessageMap(ConvMessage msg)
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
		 * Using switch on String, as it is present in JDK 7 onwards. 
		 * Switch makes for a cleaner and easier to read code as well.
		 * http://stackoverflow.com/questions/338206/why-cant-i-switch-on-a-string
		 */
		switch (type)
		{
		case HikePubSub.MESSAGE_RECEIVED:
			onMessageReceived(object);
			break;
       
		default:
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
		final ConvMessage message = (ConvMessage) object;
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
	
	protected void handleAbnormalMessages()
	{
		// TODO DO NOTHING. Only classes which need to handle such type of messages need to override this method
		return;
	}

	protected void sendUIMessage(int what,Object data){
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
		
		Logger.d(TAG, Integer.toString(mPubSubListeners.length));
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
		String[] commonEvents = new String[]{ HikePubSub.MESSAGE_RECEIVED};
		
		/**
		 * Array of pubSub listeners we get from  {@link OneToOneChatThread} or {@link GroupChatThread}
		 * 
		 */
		String [] moreEvents = getPubSubListeners();
		
		if(moreEvents == null)
		{
			retVal = new String[commonEvents.length];
			
			System.arraycopy(commonEvents, 0, retVal, 0, commonEvents.length);
		}
		
		else
		{
			retVal = new String[commonEvents.length + moreEvents.length];
			
			System.arraycopy(commonEvents, 0, retVal, 0, commonEvents.length);
			
			System.arraycopy(moreEvents, 0, retVal, retVal.length - commonEvents.length, moreEvents.length);
			
		}
		
		return retVal;
	}

	protected abstract String[] getPubSubListeners();

	/**
	 * Mimics the onDestroy method of an Activity. 
	 * It is used to release resources help by the ChatThread instance.
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
	
}
