package com.bsb.im.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.bsb.im.R;
import com.bsb.im.service.aidl.IXmppFacade;

import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.bsb.im.BeemApplication;
import com.bsb.im.BeemService;
import com.bsb.im.providers.AvatarProvider;
import com.bsb.im.service.UserInfo;
import com.bsb.im.ui.wizard.AccountConfigure;
import com.bsb.im.utils.BeemBroadcastReceiver;
import com.bsb.im.utils.BeemConnectivity;
import com.bsb.im.utils.Status;

/**
 * This Activity is used to change the status.
 * 
 */
public class ChangeStatus extends Activity {

	private static final Intent SERVICE_INTENT = new Intent();
	static {
		SERVICE_INTENT.setComponent(new ComponentName("com.bsb.im",
				"com.bsb.im.BeemService"));
	}

	private static final String TAG = ChangeStatus.class.getSimpleName();
	private static final int AVAILABLE_FOR_CHAT_IDX = 0;
	private static final int AVAILABLE_IDX = 1;
	private static final int BUSY_IDX = 2;
	private static final int AWAY_IDX = 3;
	private static final int UNAVAILABLE_IDX = 4;
	private static final int DISCONNECTED_IDX = 5;

	private static final int ICON_SIZE = 80;

	private static final int SELECT_PHOTO_DLG = 0;

	private static final int CAMERA_WITH_DATA = 0;
	private static final int PHOTO_PICKED_WITH_DATA = 1;

	private static final File PHOTO_DIR = new File(
			Environment.getExternalStorageDirectory() + "/DCIM/Camera");

	private static final String KEY_CURRENT_PHOTO_FILE = "currentphotofile";

	private static final Uri MY_AVATAR_URI = Uri
			.parse(AvatarProvider.CONTENT_URI + "/my_avatar");

	private EditText mStatusMessageEditText;
	private Toast mToast;
	private Button mOk;
	private Button mClear;
	private Button mContact;
	private Spinner mSpinner;
	private ImageButton mAvatar;
	private Uri mAvatarUri;

	private SharedPreferences mSettings;
	private ArrayAdapter<CharSequence> mAdapter;
	private IXmppFacade mXmppFacade;
	private final ServiceConnection mServConn = new BeemServiceConnection();
	private final OnTouchListener mOnClickOk = new MyOnTouchListener();
	private final BeemBroadcastReceiver mReceiver = new BeemBroadcastReceiver();
	private boolean mShowCurrentAvatar = true;
	private boolean mDisableAvatar;
	private File mCurrentPhotoFile;

	/**
	 * Constructor.
	 */
	public ChangeStatus() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "oncreate");
		setContentView(R.layout.changestatus);

		mOk = (Button) findViewById(R.id.ChangeStatusOk);
		mOk.setOnTouchListener(mOnClickOk);

		mClear = (Button) findViewById(R.id.ChangeStatusClear);
		mClear.setOnTouchListener(mOnClickOk);

		mContact = (Button) findViewById(R.id.OpenContactList);
		mContact.setOnTouchListener(mOnClickOk);

		BeemApplication app = (BeemApplication) getApplication();
		mAvatar = (ImageButton) findViewById(R.id.avatarButton);
		mAvatar.setOnTouchListener(mOnClickOk);
		if (!app.isPepEnabled()) {
			View avatarPanel = findViewById(R.id.avatar_panel);
			avatarPanel.setVisibility(View.GONE);
		}

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mStatusMessageEditText = (EditText) findViewById(R.id.ChangeStatusMessage);
		mStatusMessageEditText.setText(mSettings.getString(
				BeemApplication.STATUS_TEXT_KEY, ""));

		mSpinner = (Spinner) findViewById(R.id.ChangeStatusSpinner);
		mAdapter = ArrayAdapter.createFromResource(this, R.array.status_types,
				android.R.layout.simple_spinner_item);
		mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(mAdapter);

		mToast = Toast.makeText(this, R.string.ChangeStatusOk,
				Toast.LENGTH_LONG);
		mSpinner.setSelection(getPreferenceStatusIndex());

		this.registerReceiver(mReceiver, new IntentFilter(
				BeemBroadcastReceiver.BEEM_CONNECTION_CLOSED));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (!BeemConnectivity.isConnected(getApplicationContext())) {
			Intent i = new Intent(this, Login.class);
			startActivity(i);
			finish();
		}
		bindService(new Intent(this, BeemService.class), mServConn,
				BIND_AUTO_CREATE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(mServConn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mReceiver);
	}

	/*
	 * The activity is often reclaimed by the system memory.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mCurrentPhotoFile != null) {
			outState.putString(KEY_CURRENT_PHOTO_FILE,
					mCurrentPhotoFile.toString());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		String fileName = savedInstanceState.getString(KEY_CURRENT_PHOTO_FILE);
		if (fileName != null) {
			mCurrentPhotoFile = new File(fileName);
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == SELECT_PHOTO_DLG)
			return createPickPhotoDialog();
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Ignore failed requests
		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
		case PHOTO_PICKED_WITH_DATA:
			mAvatarUri = Uri.parse(data.getAction());
			Log.d(TAG, "selected avatar uri " + mAvatarUri);
			if (mAvatarUri != null) {
				mAvatar.setImageURI(mAvatarUri);
				mDisableAvatar = false;
				mShowCurrentAvatar = false;
			}
			break;

		case CAMERA_WITH_DATA:
			doCropPhoto(mCurrentPhotoFile);
			break;
		default:
			Log.w(TAG, "onActivityResult : invalid request code");

		}
	}

	/**
	 * Return the status index from status the settings.
	 * 
	 * @return the status index from status the settings.
	 */
	private int getPreferenceStatusIndex() {
		return mSettings.getInt(BeemApplication.STATUS_KEY, AVAILABLE_IDX);
	}

	/**
	 * Return the status text from status the settings.
	 * 
	 * @param id
	 *            status text id.
	 * @return the status text from status the settings.
	 */
	private String getPreferenceString(int id) {
		return mSettings.getString(getString(id), "");
	}

	/**
	 * convert status text to.
	 * 
	 * @param item
	 *            selected item text.
	 * @return item position in the array.
	 */
	private int getStatusForService(String item) {
		int result;
		switch (mAdapter.getPosition(item)) {
		case ChangeStatus.DISCONNECTED_IDX:
			result = Status.CONTACT_STATUS_DISCONNECT;
			break;
		case ChangeStatus.AVAILABLE_FOR_CHAT_IDX:
			result = Status.CONTACT_STATUS_AVAILABLE_FOR_CHAT;
			break;
		case ChangeStatus.AVAILABLE_IDX:
			result = Status.CONTACT_STATUS_AVAILABLE;
			break;
		case ChangeStatus.AWAY_IDX:
			result = Status.CONTACT_STATUS_AWAY;
			break;
		case ChangeStatus.BUSY_IDX:
			result = Status.CONTACT_STATUS_BUSY;
			break;
		case ChangeStatus.UNAVAILABLE_IDX:
			result = Status.CONTACT_STATUS_UNAVAILABLE;
			break;
		default:
			result = Status.CONTACT_STATUS_AVAILABLE;
			break;
		}
		return result;
	}

	/**
	 * ClickListener for the avatarButton.
	 * 
	 * @param button
	 *            the avatar button
	 */
	private void onAvatarButton(View button) {
		showDialog(SELECT_PHOTO_DLG);
	}

	/**
	 * Publish the selected avatar.
	 */
	private void publishAvatar() {
		try {
			if (mDisableAvatar)
				mXmppFacade.disableAvatarPublishing();
			else if (mAvatarUri != null)
				mXmppFacade.publishAvatar(mAvatarUri);
		} catch (RemoteException e) {
			Log.e(TAG, "Error while publishing avatar", e);
		}
	}

	/**
	 * Display the current avatar in the button.
	 */
	private void displayCurrentAvatar() {
		try {
			UserInfo ui = mXmppFacade.getUserInfo();
			String avatarId = ui.getAvatarId();
			Log.d(TAG, "User info ; avatar id " + avatarId);
			if (avatarId != null) {
				Uri uri = AvatarProvider.CONTENT_URI.buildUpon()
						.appendPath(avatarId).build();
				mAvatar.setImageURI(uri);
			}
		} catch (RemoteException e) {
			Log.e(TAG, "Error while displaying current avatar", e);
		}
		mShowCurrentAvatar = false;
	}

	/*
	 * Some codes from AOSP (platform/packages/apps/Contacts) to select and crop
	 * an image.
	 */

	/**
	 * Creates a dialog offering two options: take a photo or pick a photo from
	 * the gallery.
	 * 
	 * @return the dialog
	 */
	private Dialog createPickPhotoDialog() {
		// Wrap our context to inflate list items using correct theme
		final Context dialogContext = new ContextThemeWrapper(this,
				android.R.style.Theme_Light);

		final ListAdapter adapter = ArrayAdapter.createFromResource(
				dialogContext, R.array.pick_photo_items,
				android.R.layout.simple_list_item_1);

		final AlertDialog.Builder builder = new AlertDialog.Builder(
				dialogContext);
		builder.setTitle(R.string.select_avatar);
		builder.setSingleChoiceItems(adapter, -1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						switch (which) {
						case 0:
							doTakePhoto();
							break;
						case 1:
							doPickPhotoFromGallery();
							break;
						case 2:
							mDisableAvatar = true;
							mAvatar.setImageURI(null);
							break;
						default:
							Log.w(TAG,
									"DialogInterface onClick : invalid which code");
						}
					}
				});
		return builder.create();
	}

	/**
	 * Create a file name for the icon photo using current time.
	 * 
	 * @return the filename
	 */
	private String getPhotoFileName() {
		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"'IMG'_yyyyMMdd_HHmmss");
		return dateFormat.format(date) + ".jpg";
	}

	/**
	 * Launches Camera to take a picture and store it in a file.
	 */
	protected void doTakePhoto() {
		try {
			// Launch camera to take photo for selected contact
			PHOTO_DIR.mkdirs();
			mCurrentPhotoFile = new File(PHOTO_DIR, getPhotoFileName());
			final Intent intent = getTakePickIntent(mCurrentPhotoFile);
			startActivityForResult(intent, CAMERA_WITH_DATA);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.photoPickerNotFoundText,
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Constructs an intent for capturing a photo and storing it in a temporary
	 * file.
	 * 
	 * @param f
	 *            the temporary file to use to store the picture
	 * @return the intent
	 */
	public static Intent getTakePickIntent(File f) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
		return intent;
	}

	/**
	 * Sends a newly acquired photo to Gallery for cropping.
	 * 
	 * @param f
	 *            the image file to crop
	 */
	protected void doCropPhoto(final File f) {
		try {

			// Add the image to the media store
			// level 8
			/*
			 * MediaScannerConnection.scanFile( this, new String[] {
			 * f.getAbsolutePath() }, new String[] { null }, null);
			 */

			// Launch gallery to crop the photo
			final Intent intent = getCropImageIntent(Uri.fromFile(f));
			startActivityForResult(intent, PHOTO_PICKED_WITH_DATA);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "Cannot crop image", e);
			Toast.makeText(this, R.string.photoPickerNotFoundText,
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Constructs an intent for image cropping.
	 * 
	 * @param photoUri
	 *            the uri of the photo to crop
	 * @return the intent
	 */
	public static Intent getCropImageIntent(Uri photoUri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(photoUri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", ICON_SIZE);
		intent.putExtra("outputY", ICON_SIZE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, MY_AVATAR_URI);
		return intent;
	}

	/**
	 * Launches Gallery to pick a photo.
	 */
	protected void doPickPhotoFromGallery() {
		try {
			// Launch picker to choose photo for selected contact
			final Intent intent = getPhotoPickIntent();
			startActivityForResult(intent, PHOTO_PICKED_WITH_DATA);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.photoPickerNotFoundText,
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Constructs an intent for picking a photo from Gallery, cropping it and
	 * returning the bitmap.
	 * 
	 * @return the intent
	 */
	public static Intent getPhotoPickIntent() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
		intent.setType("image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", ICON_SIZE);
		intent.putExtra("outputY", ICON_SIZE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, MY_AVATAR_URI);
		// use this to get the bitmap in the intent
		// intent.putExtra("return-data", true);
		return intent;
	}

	/**
	 * connection to service.
	 * 
	 */
	private class BeemServiceConnection implements ServiceConnection {

		/**
		 * constructor.
		 */
		public BeemServiceConnection() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXmppFacade = IXmppFacade.Stub.asInterface(service);
			if (mShowCurrentAvatar)
				displayCurrentAvatar();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXmppFacade = null;
		}
	}

	/**
	 * User have clicked on ok.
	 * 
	 */
	private class MyOnTouchListener implements OnTouchListener {

		/**
		 * constructor.
		 */
		public MyOnTouchListener() {
		}

		@Override
		public boolean onTouch(View v, MotionEvent arg1) {
			// TODO Auto-generated method stub
			if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
				//if (v != mAvatar)
					//v.setBackgroundResource(R.drawable.button1_pressed);
			} else if (arg1.getAction() == MotionEvent.ACTION_UP) {
				//if (v != mAvatar)
					//v.setBackgroundResource(R.drawable.button1);
				if (v == mOk) {
					String msg = mStatusMessageEditText.getText().toString();
					int status = getStatusForService((String) mSpinner
							.getSelectedItem());
					Editor edit = mSettings.edit();
					edit.putString(BeemApplication.STATUS_TEXT_KEY, msg);
					if (status == Status.CONTACT_STATUS_DISCONNECT) {
						stopService(new Intent(ChangeStatus.this,
								BeemService.class));
					} else {
						try {
							mXmppFacade.changeStatus(status, msg.toString());
							edit.putInt(BeemApplication.STATUS_KEY,
									mSpinner.getSelectedItemPosition());
							publishAvatar();
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						mToast.show();
					}
					edit.commit();
					ChangeStatus.this.finish();
				} else if (v == mClear) {
					mStatusMessageEditText.setText(null);
				} else if (v == mContact) {
					startActivity(new Intent(ChangeStatus.this,
							ContactList.class));
					ChangeStatus.this.finish();
				} else if (v == mAvatar)
					onAvatarButton(v);
			}
			return false;
		}
	}
}
