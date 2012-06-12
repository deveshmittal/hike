package com.bsb.hike.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.cropimage.CropImage;
import com.bsb.hike.cropimage.Util;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.Utils;

public class ProfileActivity extends Activity implements FinishableEvent, android.content.DialogInterface.OnClickListener, Listener
{
	/* dialog IDs */
	private static final int PROFILE_PICTURE_FROM_CAMERA = 0;
	private static final int PROFILE_PICTURE_FROM_GALLERY = 1;

	/* activityForResult IDs */
	private static final int CAMERA_RESULT = 0;
	private static final int GALLERY_RESULT = 1;
	private static final int CROP_RESULT = 2;

	private ImageView mIconView;
	private EditText mNameEdit;

	private View currentSelection;

	private Dialog mDialog;
	private String mLocalMSISDN = null;

	private ActivityState mActivityState; /* config state of this activity */
	private String nameTxt;
	private boolean isBackPressed = false;
	private EditText mEmailEdit;
	private String emailTxt;
	private List<ContactInfo> participantList;

	private ProfileType profileType;
	private String httpRequestURL;

	private static enum ProfileType
	{
		USER_PROFILE, // The user profile screen
		USER_PROFILE_EDIT, // The user profile edit screen
		GROUP_INFO // The group info screen
	};

	private class ActivityState
	{
		public HikeHTTPTask task; /* the task to update the global profile */

		public Bitmap newBitmap = null; /* the bitmap before the user saves it */
		public int genderType;
	}

	/* super hacky, but the Activity can get destroyed between startActivityForResult and the onResult
	 * so store it in a static field.
	 */
	public static File selectedFileIcon; /* the selected file that we'll store the profile camera picture */

	/* store the task so we can keep keep the progress dialog going */
	@Override
	public Object onRetainNonConfigurationInstance()
	{
		Log.d("ProfileActivity", "onRetainNonConfigurationinstance");
		return mActivityState;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
		}
		if ( (mActivityState != null) && (mActivityState.task != null))
		{
			mActivityState.task.setActivity(null);
		}
		if(profileType == ProfileType.GROUP_INFO)
		{
			HikeMessengerApp.getPubSub().removeListener(HikePubSub.ICON_CHANGED, this);
			HikeMessengerApp.getPubSub().removeListener(HikePubSub.GROUP_NAME_CHANGED, this);
		}
		mActivityState = null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (Utils.requireAuth(this))
		{
			return;
		}

		Object o = getLastNonConfigurationInstance();
		if (o instanceof ActivityState)
		{
			mActivityState = (ActivityState) o;
			if (mActivityState.task != null)
			{
				/* we're currently executing a task, so show the progress dialog */
				mActivityState.task.setActivity(this);
				mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));	
			}
		}
		else
		{
			mActivityState = new ActivityState();
		}

		if(getIntent().hasExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT))
		{
			this.profileType = ProfileType.GROUP_INFO;
			HikeMessengerApp.getPubSub().addListener(HikePubSub.ICON_CHANGED, this);
			HikeMessengerApp.getPubSub().addListener(HikePubSub.GROUP_NAME_CHANGED, this);
			setupGroupProfileScreen();
		}
		else
		{
			httpRequestURL = "/account";
			fetchPersistentData();

			if(getIntent().getBooleanExtra(HikeConstants.Extras.EDIT_PROFILE, false))
			{
				this.profileType = ProfileType.USER_PROFILE_EDIT;
				setupEditScreen();
			}
			else
			{
				this.profileType = ProfileType.USER_PROFILE;
				setupProfileScreen();
			}
		}
	}

	private void setupGroupProfileScreen()
	{
		setContentView(R.layout.group_info);

		ViewGroup groupInfoLayout = (ViewGroup) findViewById(R.id.group_info);
		TextView mTitleView = (TextView) findViewById(R.id.title);
		mNameEdit = (EditText) findViewById(R.id.name_input);
		mIconView = (ImageView) findViewById(R.id.profile);

		groupInfoLayout.setFocusable(true);
		groupInfoLayout.setBackgroundResource(R.drawable.profile_bottom_item_selector);

		this.mLocalMSISDN = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);

		HikeConversationsDatabase hCDB = new HikeConversationsDatabase(ProfileActivity.this);
		Conversation conv = hCDB.getConversation(mLocalMSISDN, 0);
		hCDB.close();
		participantList = conv.getGroupParticipants();
		httpRequestURL = "/group/" + conv.getMsisdn();

		ViewGroup participantNameContainer = (ViewGroup) findViewById(R.id.group_participant_container);

		int left = (int) (0 * Utils.densityMultiplier);
		int top = (int) (0 * Utils.densityMultiplier);
		int right = (int) (0 * Utils.densityMultiplier);
		int bottom = (int) (6 * Utils.densityMultiplier);

		ContactInfo userInfo = Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0));
		conv.getGroupParticipants().add(userInfo);

		for(ContactInfo contactInfo : conv.getGroupParticipants())
		{
			TextView participantNameItem = (TextView) ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.participant_name_item, null);
			participantNameItem.setText(!TextUtils.isEmpty(contactInfo.getName()) ? contactInfo.getFirstName() : Utils.getContactName(conv.getMsisdn(), conv.getGroupParticipants(), contactInfo.getMsisdn(), ProfileActivity.this));
			participantNameItem.setBackgroundResource(contactInfo.isOnhike() || userInfo.getMsisdn().equals(contactInfo.getMsisdn()) ? R.drawable.hike_contact_bg : R.drawable.sms_contact_bg);

			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.setMargins(left, top, right, bottom);
			participantNameItem.setLayoutParams(lp);

			participantNameContainer.addView(participantNameItem);
		}
		conv.getGroupParticipants().remove(userInfo);
		
		nameTxt = conv.getLabel();
		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(conv.getMsisdn());

		mIconView.setImageDrawable(drawable);
		mNameEdit.setText(nameTxt);
		mTitleView.setText(R.string.group_info);
		
		// Hide the cursor initially
		Utils.hideCursor(mNameEdit, getResources());
	}

	private void setupEditScreen()
	{
		setContentView(R.layout.profile_edit);

		TextView mTitleView = (TextView) findViewById(R.id.title);

		ViewGroup name = (ViewGroup) findViewById(R.id.name);
		ViewGroup phone = (ViewGroup) findViewById(R.id.phone);
		ViewGroup email = (ViewGroup) findViewById(R.id.email);
		ViewGroup gender = (ViewGroup) findViewById(R.id.gender);
		ViewGroup picture = (ViewGroup) findViewById(R.id.photo);

		mNameEdit = (EditText) name.findViewById(R.id.name_input);
		mEmailEdit = (EditText) email.findViewById(R.id.email_input);

		((TextView)name.findViewById(R.id.name_edit_field)).setText("Name");
		((TextView)phone.findViewById(R.id.phone_edit_field)).setText("Phone");
		((TextView)email.findViewById(R.id.email_edit_field)).setText("Email");
		((TextView)gender.findViewById(R.id.gender_edit_field)).setText("Gender");
		((TextView)picture.findViewById(R.id.photo_edit_field)).setText("Edit Picture");

		picture.setBackgroundResource(R.drawable.profile_bottom_item_selector);
		picture.setFocusable(true);

		mTitleView.setText(getResources().getString(R.string.edit_profile));
		((EditText)phone.findViewById(R.id.phone_input)).setText(mLocalMSISDN);
		((EditText)phone.findViewById(R.id.phone_input)).setEnabled(false);

		mNameEdit.setText(nameTxt);
		mEmailEdit.setText(emailTxt);

		mNameEdit.setSelection(nameTxt.length());
		mEmailEdit.setSelection(emailTxt.length());

		onEmoticonClick(mActivityState.genderType == 0 ? null : mActivityState.genderType == 1 ? gender.findViewById(R.id.guy) : gender.findViewById(R.id.girl));

		//Hide the cursor initially
		Utils.hideCursor(mNameEdit, getResources());
	}
	
	private void setupProfileScreen()
	{
		setContentView(R.layout.profile);

		TextView mTitleView = (TextView) findViewById(R.id.title);
		TextView mNameView = (TextView) findViewById(R.id.name_current);

		ViewGroup myInfo = (ViewGroup) findViewById(R.id.my_info); 
		ViewGroup credits = (ViewGroup) findViewById(R.id.free_sms);
		ViewGroup notifications = (ViewGroup) findViewById(R.id.notifications);
		ViewGroup privacy = (ViewGroup) findViewById(R.id.privacy);
		ViewGroup help = (ViewGroup) findViewById(R.id.help);

		myInfo.setBackgroundResource(R.drawable.profile_top_item_selector);
		credits.setBackgroundResource(R.drawable.profile_bottom_item_selector);
		notifications.setBackgroundResource(R.drawable.profile_top_item_selector);
		privacy.setBackgroundResource(R.drawable.profile_center_item_selector);
		help.setBackgroundResource(R.drawable.profile_bottom_item_selector);

		mIconView = (ImageView) findViewById(R.id.profile);

		ViewGroup[] itemLayouts = new ViewGroup[]
				{
					credits, notifications, privacy, help
				};

		ProfileItem[] items = new ProfileItem[] 
			{
				new ProfileItem.ProfileSettingsItem("Free hike SMS left", R.drawable.ic_credits, HikeMessengerApp.SMS_SETTING),
				new ProfileItem.ProfilePreferenceItem("Notifications", R.drawable.ic_notifications, R.xml.notification_preferences),
				new ProfileItem.ProfilePreferenceItem("Privacy", R.drawable.ic_privacy, R.xml.privacy_preferences),
				new ProfileItem.ProfileLinkItem("Help", R.drawable.ic_help, "http://www.bsb.im/about")
			};

		for(int i = 0; i < items.length; i++)
		{
			items[i].createViewHolder(itemLayouts[i], items[i]);
			items[i].bindView(ProfileActivity.this, itemLayouts[i]);
		}

		notifications.findViewById(R.id.divider).setVisibility(View.GONE);

		mTitleView.setText(getResources().getString(R.string.profile_title));
		mNameView.setText(nameTxt);
		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(getLargerIconId());
		mIconView.setImageDrawable(drawable);
		
		myInfo.setFocusable(true);
		credits.setFocusable(true);
		notifications.setFocusable(true);
		privacy.setFocusable(true);
		help.setFocusable(true);
	}

	private void fetchPersistentData()
	{
		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		nameTxt = settings.getString(HikeMessengerApp.NAME, "Set a name!");
		mLocalMSISDN = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		emailTxt = settings.getString(HikeConstants.Extras.EMAIL, "");
		mActivityState.genderType = mActivityState.genderType == 0 ? settings.getInt(HikeConstants.Extras.GENDER, 0) : mActivityState.genderType;
	}

	public void onBackPressed()
	{
		if(this.profileType == ProfileType.USER_PROFILE_EDIT || this.profileType == ProfileType.GROUP_INFO)
		{
			isBackPressed = true;
			saveChanges();
			overridePendingTransition(R.anim.slide_in_left_noalpha, R.anim.slide_out_right_noalpha);
		}
		else
		{
			super.onBackPressed();
		}
	}
	
	public void onProfileItemClick(View v)
	{
		ProfileItem item = (ProfileItem) v.getTag(R.id.profile);
		Intent intent = item.getIntent(ProfileActivity.this);
		if (intent != null)
		{
			startActivity(intent);
		}
	}
	
	public void saveChanges()
	{
		ArrayList<HikeHttpRequest> requests = new ArrayList<HikeHttpRequest>();

		if (this.profileType == ProfileType.USER_PROFILE_EDIT && !TextUtils.isEmpty(mEmailEdit.getText()))
		{
			if (!Utils.isValidEmail(mEmailEdit.getText()))
			{
				Toast.makeText(this, getResources().getString(R.string.invalid_email), Toast.LENGTH_LONG).show();
				return;
			}
		}

		if (mNameEdit != null && !TextUtils.isEmpty(mNameEdit.getText()) && !nameTxt.equals(mNameEdit.getText().toString()))
		{
			/* user edited the text, so update the profile */
			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL + "/name", new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					if (isBackPressed) {
						finishEditing();
					}
				}

				public void onSuccess()
				{
					if (ProfileActivity.this.profileType != ProfileType.GROUP_INFO) 
					{
						/* if the request was successful, update the shared preferences and the UI */
						String name = mNameEdit.getText().toString();
						Editor editor = getSharedPreferences(
								HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
						editor.putString(HikeMessengerApp.NAME_SETTING, name);
						editor.commit();
					}
					else
					{
						HikeConversationsDatabase hCDB = new HikeConversationsDatabase(ProfileActivity.this);
						hCDB.setGroupName(ProfileActivity.this.mLocalMSISDN, mNameEdit.getText().toString());
						hCDB.close();
					}
					if (isBackPressed) {
						finishEditing();
					}
				}
			});

			JSONObject json = new JSONObject();
			try
			{
				json.put("name", mNameEdit.getText().toString());
				request.setJSONData(json);
			}
			catch (JSONException e)
			{
				Log.e("ProfileActivity", "Could not set name", e);
			}
			requests.add(request);
		}

		if (mActivityState.newBitmap != null)
		{
			/* the server only needs a 40x40 version */
			final Bitmap smallerBitmap = Util.transform(new Matrix(),
					mActivityState.newBitmap, 40, 40, false);
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			smallerBitmap.compress(Bitmap.CompressFormat.JPEG, 95, bao);
			final byte[] bytes = bao.toByteArray();

			final byte[] larger_bytes;
			if (this.profileType != ProfileType.GROUP_INFO) 
			{
				bao = new ByteArrayOutputStream();
				mActivityState.newBitmap.compress(Bitmap.CompressFormat.PNG,
						90, bao);
				larger_bytes = bao.toByteArray();
			}
			else
			{
				larger_bytes = null;
			}

			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL + "/avatar", new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					Log.d("ProfileActivity", "resetting image");
					mActivityState.newBitmap = null;
					if (mIconView != null) {
						/* reset the image */
						mIconView.setImageDrawable(IconCacheManager
								.getInstance().getIconForMSISDN(ProfileActivity.this.profileType != ProfileType.GROUP_INFO ?
										getLargerIconId() : mLocalMSISDN));
					}
					if (isBackPressed) {
						finishEditing();
					}
				}

				public void onSuccess()
				{
					HikeUserDatabase db = new HikeUserDatabase(ProfileActivity.this);
					db.setIcon(mLocalMSISDN, bytes);
					if (ProfileActivity.this.profileType != ProfileType.GROUP_INFO)
 					{
						db.setIcon(getLargerIconId(), larger_bytes);
					}
					db.close();
					if (isBackPressed) {
						finishEditing();
					}
				}
			});

			request.setPostData(bytes);
			requests.add(request);
		}

		if (this.profileType == ProfileType.USER_PROFILE_EDIT) {
			SharedPreferences prefs = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
			Editor editor = prefs.edit();
			if (Utils.isValidEmail(mEmailEdit.getText()))
			{
				editor.putString(HikeConstants.Extras.EMAIL, mEmailEdit
						.getText().toString());
			}

			editor.putInt(HikeConstants.Extras.GENDER,
					currentSelection == null ? 0
							: currentSelection.getId() == R.id.guy ? 1 : 2);
			editor.commit();
		}

		if (!requests.isEmpty())
		{
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
			mActivityState.task = new HikeHTTPTask(this, R.string.update_profile_failed);
			HikeHttpRequest[] r = new HikeHttpRequest[requests.size()];
			requests.toArray(r);
			mActivityState.task.execute(r);
		}
		else if(isBackPressed)
		{
			finishEditing();
		}
	}

	private void finishEditing()
	{
		if (this.profileType != ProfileType.GROUP_INFO) 
		{
			Intent i = new Intent(this, ProfileActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
		}
		finish();
	}

	protected String getLargerIconId()
	{
		return mLocalMSISDN + "::large";
	}

	@Override
	public void onFinish(boolean success)
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}

		mActivityState = new ActivityState();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		String path = null;
		if (resultCode != RESULT_OK)
		{
			return;
		}

		switch(requestCode)
		{
		case CAMERA_RESULT:
			/* fall-through on purpose */
		case GALLERY_RESULT:
			Log.d("ProfileActivity", "The activity is " + this);
			path = (requestCode == CAMERA_RESULT) ? selectedFileIcon.getAbsolutePath() : getGalleryPath(data.getData());
			/* Crop the image */
			Intent intent = new Intent(this, CropImage.class);
			intent.putExtra(HikeConstants.Extras.IMAGE_PATH, path);
			intent.putExtra(HikeConstants.Extras.SCALE, true);
			intent.putExtra(HikeConstants.Extras.OUTPUT_X, 80);
			intent.putExtra(HikeConstants.Extras.OUTPUT_Y, 80);
			intent.putExtra(HikeConstants.Extras.ASPECT_X, 1);
			intent.putExtra(HikeConstants.Extras.ASPECT_Y, 1);
			startActivityForResult(intent, CROP_RESULT);
			break;
		case CROP_RESULT:
			Bitmap bitmap = data.getParcelableExtra(HikeConstants.Extras.BITMAP);
			mActivityState.newBitmap = Utils.getRoundedCornerBitmap(bitmap);
			bitmap.recycle();
			if (mIconView != null) {
				mIconView.setImageBitmap(mActivityState.newBitmap);
			}
			if (this.profileType == ProfileType.USER_PROFILE) {
				saveChanges();
			}
			break;
		}
	}

	private String getGalleryPath(Uri selectedImage)
	{
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor == null)
        {
        	return selectedImage.getPath();
        }

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
	}

	@Override
	public void onClick(DialogInterface dialog, int item)
	{
		Intent intent = null;
		switch(item)
		{
		case PROFILE_PICTURE_FROM_CAMERA:
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			selectedFileIcon = Utils.getOutputMediaFile(Utils.MEDIA_TYPE_IMAGE); // create a file to save the image
			if (selectedFileIcon != null)
			{
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFileIcon));
				startActivityForResult(intent, CAMERA_RESULT);
				overridePendingTransition(R.anim.slide_in_right_noalpha, R.anim.slide_out_left_noalpha);
			}
			else
			{
				Toast.makeText(this, getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
			}
			break;
		case PROFILE_PICTURE_FROM_GALLERY:
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, GALLERY_RESULT);
			overridePendingTransition(R.anim.slide_in_right_noalpha, R.anim.slide_out_left_noalpha);
			break;
		}
	}
	
	public void onEmoticonClick(View v)
	{
		if (v != null) 
		{
			if (currentSelection != null) {
				currentSelection.setSelected(false);
			}
			v.setSelected(currentSelection != v);
			currentSelection = v == currentSelection ? null : v;
			if (currentSelection != null)
			{
				mActivityState.genderType = currentSelection.getId() == R.id.guy ? 1 : 2;
			}

		}
	}

    public void onChangeImageClicked(View v)
    {
    	/* The wants to change their profile picture.
		 * Open a dialog to allow them pick Camera or Gallery 
		 */
		final CharSequence[] items = {"Camera", "Gallery"};/*TODO externalize these */
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose a picture");
		builder.setItems(items, this);
		mDialog = builder.show();
    }

    public void onEditProfileClicked(View v)
    {
    	Utils.logEvent(ProfileActivity.this, HikeConstants.LogEvent.EDIT_PROFILE);
		Intent i = new Intent(ProfileActivity.this, ProfileActivity.class);
		i.putExtra(HikeConstants.Extras.EDIT_PROFILE, true);
		startActivity(i);
		finish();
    }

    public void onInviteAllClicked(View v)
	{
    	for(ContactInfo contactInfo : participantList)
    	{
    		if (!contactInfo.isOnhike()) 
    		{
    			long time = (long) System.currentTimeMillis() / 1000;
				ConvMessage convMessage = new ConvMessage(getResources()
						.getString(R.string.invite_message), contactInfo.getMsisdn(), time,
						ConvMessage.State.SENT_UNCONFIRMED);
				convMessage.setInvite(true);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
						convMessage.serialize());
			}
    	}
	}

	public void onGroupInfoClicked(View v)
	{
		Intent intent = getIntent();
		intent.setClass(ProfileActivity.this, ChatThread.class);
		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mLocalMSISDN);
		startActivity(intent);
		
		overridePendingTransition(R.anim.slide_in_right_noalpha,
				R.anim.slide_out_left_noalpha);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (mLocalMSISDN.equals((String)object)) 
		{
			if (HikePubSub.ICON_CHANGED.equals(type)) 
			{
				HikeConversationsDatabase db = new HikeConversationsDatabase(
						this);
				nameTxt = db.getGroupName(mLocalMSISDN);
				db.close();
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run() 
					{
						mNameEdit.setText(nameTxt);
					}
				});
			} 
			else if (HikePubSub.GROUP_NAME_CHANGED.equals(type)) 
			{
				final Drawable drawable = IconCacheManager.getInstance()
						.getIconForMSISDN(mLocalMSISDN);
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run() 
					{
						mIconView.setImageDrawable(drawable);
					}
				});
			}
		}
	}
}
