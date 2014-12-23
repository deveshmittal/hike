package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.MessagesAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.media.AttachmentPicker;
import com.bsb.hike.media.AudioRecordView;
import com.bsb.hike.media.AudioRecordView.AudioRecordListener;
import com.bsb.hike.media.CaptureImageParser;
import com.bsb.hike.media.CaptureImageParser.CaptureImageListener;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.media.PickContactParser;
import com.bsb.hike.media.PickFileParser;
import com.bsb.hike.media.PickFileParser.PickFileListener;
import com.bsb.hike.media.StickerPicker;
import com.bsb.hike.media.StickerPicker.StickerPickerListener;
import com.bsb.hike.media.ThemePicker;
import com.bsb.hike.media.ThemePicker.ThemePickerListener;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.PhonebookContact;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.ui.HikeDialog.HDialog;
import com.bsb.hike.ui.HikeDialog.HHikeDialogListener;
import com.bsb.hike.ui.HomeActivity;
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
		HHikeDialogListener, StickerPickerListener, AudioRecordListener, LoaderCallbacks<Object>
{
	private static final String TAG = "chatthread";

	private static final int FETCH_CONV = 1;

	protected ChatThreadActivity activity;

	protected ThemePicker themePicker;

	protected AttachmentPicker attachmentPicker;

	protected HikeSharedPreferenceUtil sharedPreference;

	protected ChatTheme currentTheme;

	protected String msisdn;

	protected StickerPicker stickerPicker;

	protected AudioRecordView audioRecordView;

	protected Conversation mConversation;

	protected HikeConversationsDatabase mConversationDb;

	protected MessagesAdapter mAdapter;

	protected ArrayList<ConvMessage> messages;

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
	}

	protected boolean filter()
	{
		if (HikeMessengerApp.isStealthMsisdn(msisdn))
		{
			if (HikeSharedPreferenceUtil.getInstance(activity).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF) != HikeConstants.STEALTH_ON)
			{
				Intent intent = new Intent(activity, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(intent);
				activity.finish();
				return false;
			}
		}
		return true;
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
		activity.findViewById(R.id.sticker_btn).setOnClickListener(this);
		activity.findViewById(R.id.send_message).setOnClickListener(this);
		stickerPicker = new StickerPicker(activity, this, activity.findViewById(R.id.chatThreadParentLayout),
				(int) (activity.getResources().getDimension(R.dimen.emoticon_pallete)));
		audioRecordView = new AudioRecordView(activity, this);
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
		stickerPicker.showStickerPicker();
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
		if (currentTheme != theme)
		{
			Logger.i(TAG, "update ui for theme " + theme);
			currentTheme = theme;
			// messages theme changed, call adapter
			// action bar
			activity.updateActionBarColor(theme.headerBgResId());
			// background image
			setBackgroundImage(theme);
			return true;
		}
		return false;
	}

	protected void setBackgroundImage(ChatTheme theme)
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
		if (themePicker != null && themePicker.isShowing())
		{
			return themePicker.onBackPressed();
		}
		return false;
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
		updateUIAsPerTheme(ChatTheme.DEFAULT);
	}

	@Override
	public void stickerSelected(Sticker sticker)
	{
		Logger.i(TAG, "sticker clicked " + sticker);
		stickerPicker.dismiss();
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
		if (async)
		{
			Bundle bundle = new Bundle();
			activity.getSupportLoaderManager().initLoader(FETCH_CONV, bundle, this);
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

	@Override
	public Loader<Object> onCreateLoader(int arg0, Bundle arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Object> arg0, Object arg1)
	{
		
	}

	@Override
	public void onLoaderReset(Loader<Object> arg0)
	{

	}

	private class ConversationLoader extends AsyncTaskLoader<Object>
	{

		public ConversationLoader(Context context)
		{
			super(context);
		}

		@Override
		public Object loadInBackground()
		{

			return fetchConversation();
		}

		/**
		 * This has to be done due to some bug in compat library -- http://stackoverflow.com/questions/10524667/android-asynctaskloader-doesnt-start-loadinbackground
		 */
		protected void onStartLoading()
		{
			if (takeContentChanged())
			{
				forceLoad();
			}
		}

	}

}
