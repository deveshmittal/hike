package com.bsb.hike.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
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
	private Map<String, GroupParticipant> participantList;

	private ProfileType profileType;
	private String httpRequestURL;
	private String groupOwner;

	private boolean shouldShowInviteAllButton = false;
	private TextView mNameDisplay;
	private ViewGroup participantNameContainer;
	private ProfileItem[] items;
	private ViewGroup credits;
	private int lastSavedGender;

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
			HikeMessengerApp.getPubSub().removeListener(HikePubSub.PARTICIPANT_JOINED_GROUP, this);
			HikeMessengerApp.getPubSub().removeListener(HikePubSub.PARTICIPANT_LEFT_GROUP, this);
			HikeMessengerApp.getPubSub().removeListener(HikePubSub.GROUP_END, this);
		}
		else if(profileType == ProfileType.USER_PROFILE)
		{
			HikeMessengerApp.getPubSub().removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
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
			HikeMessengerApp.getPubSub().addListener(HikePubSub.PARTICIPANT_JOINED_GROUP, this);
			HikeMessengerApp.getPubSub().addListener(HikePubSub.PARTICIPANT_LEFT_GROUP, this);
			HikeMessengerApp.getPubSub().addListener(HikePubSub.GROUP_END, this);
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
				HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
				setupProfileScreen();
			}
		}
	}

	private void setupGroupProfileScreen()
	{
		setContentView(R.layout.group_info);

		ViewGroup addParticipantsLayout = (ViewGroup) findViewById(R.id.add_participants_layout);
		TextView mTitleView = (TextView) findViewById(R.id.title);
		TextView groupOwnerTextView = (TextView) findViewById(R.id.group_owner);
		mNameEdit = (EditText) findViewById(R.id.name_input);
		mNameDisplay = (TextView) findViewById(R.id.name_display);
		mIconView = (ImageView) findViewById(R.id.profile);

		addParticipantsLayout.setFocusable(true);
		addParticipantsLayout.setBackgroundResource(R.drawable.profile_bottom_item_selector);

		this.mLocalMSISDN = getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);

		HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
		GroupConversation groupConversation = (GroupConversation) hCDB.getConversation(mLocalMSISDN, 0);

		participantList = groupConversation.getGroupParticipantList();
		httpRequestURL = "/group/" + groupConversation.getMsisdn();

		participantNameContainer = (ViewGroup) findViewById(R.id.group_participant_container);

		int left = (int) (0 * Utils.densityMultiplier);
		int top = (int) (0 * Utils.densityMultiplier);
		int right = (int) (0 * Utils.densityMultiplier);
		int bottom = (int) (6 * Utils.densityMultiplier);

		GroupParticipant userInfo = new GroupParticipant(Utils.getUserContactInfo(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0)));
		participantList.put(userInfo.getContactInfo().getMsisdn(), userInfo);

		groupOwner = groupConversation.getGroupOwner();

		for(Entry<String, GroupParticipant> participant : participantList.entrySet())
		{
			ContactInfo contactInfo = participant.getValue().getContactInfo();
			if(participant.getKey().equals(groupOwner))
			{
				groupOwnerTextView.setText(participant.getValue().getContactInfo().getFirstName());
				continue;
			}
			if(!contactInfo.isOnhike())
			{
				shouldShowInviteAllButton = true;
			}
			// Dont show participant that has left group
			if(participant.getValue().hasLeft())
			{
				continue;
			}
			TextView participantNameItem = (TextView) ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.participant_name_item, null);
			participantNameItem.setText(Utils.ellipsizeName(contactInfo.getName()));
			participantNameItem.setTextColor(getResources().getColor(contactInfo.isOnhike() ? R.color.contact_blue : R.color.contact_green));
			participantNameItem.setBackgroundResource(contactInfo.isOnhike() ? R.drawable.hike_contact_bg : R.drawable.sms_contact_bg);

			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.setMargins(left, top, right, bottom);
			participantNameItem.setLayoutParams(lp);
			participantNameItem.setId(participant.getKey().hashCode());

			participantNameContainer.addView(participantNameItem);
		}
		participantList.remove(userInfo.getContactInfo().getMsisdn());

		findViewById(R.id.invite_all_btn).setVisibility(shouldShowInviteAllButton ? View.VISIBLE : View.INVISIBLE);

		nameTxt = groupConversation.getLabel();
		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(groupConversation.getMsisdn());

		mIconView.setImageDrawable(drawable);
		mNameEdit.setText(nameTxt);
		mNameDisplay.setText(nameTxt);
		mTitleView.setText(R.string.group_info);

		mNameEdit.setVisibility(View.GONE);
		mNameDisplay.setVisibility(View.VISIBLE);
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

		credits = (ViewGroup) findViewById(R.id.free_sms);
		ViewGroup myInfo = (ViewGroup) findViewById(R.id.my_info); 
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

		items = new ProfileItem[] 
				{
				new ProfileItem.ProfileSettingsItem("Free hike SMS left", R.drawable.ic_credits, HikeMessengerApp.SMS_SETTING),
				new ProfileItem.ProfilePreferenceItem("Notifications", R.drawable.ic_notifications, R.xml.notification_preferences),
				new ProfileItem.ProfilePreferenceItem("Privacy", R.drawable.ic_privacy, R.xml.privacy_preferences),
				new ProfileItem.ProfileLinkItem("Help", R.drawable.ic_help, HikeConstants.HELP_URL)
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
		lastSavedGender = settings.getInt(HikeConstants.Extras.GENDER, 0);
		mActivityState.genderType = mActivityState.genderType == 0 ? lastSavedGender : mActivityState.genderType;
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
						HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
						hCDB.setGroupName(ProfileActivity.this.mLocalMSISDN, mNameEdit.getText().toString());
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
					mActivityState.newBitmap, 120, 120, false);
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
					HikeUserDatabase db = HikeUserDatabase.getInstance();
					db.setIcon(mLocalMSISDN, bytes);
					if (ProfileActivity.this.profileType != ProfileType.GROUP_INFO)
					{
						db.setIcon(getLargerIconId(), larger_bytes);
					}
					if (isBackPressed) {
						finishEditing();
					}
				}
			});

			request.setPostData(bytes);
			requests.add(request);
		}

		if (this.profileType == ProfileType.USER_PROFILE_EDIT && 
				((!emailTxt.equals(mEmailEdit.getText().toString())) || 
						((mActivityState.genderType != lastSavedGender))))
		{
			HikeHttpRequest request = new HikeHttpRequest(httpRequestURL + "/profile", new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					if (isBackPressed) {
						finishEditing();
					}
				}

				public void onSuccess()
				{
					SharedPreferences prefs = getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
					Editor editor = prefs.edit();
					if (Utils.isValidEmail(mEmailEdit.getText()))
					{
						editor.putString(HikeConstants.Extras.EMAIL, mEmailEdit
								.getText().toString());
					}
					editor.putInt(HikeConstants.Extras.GENDER, currentSelection !=null ? (currentSelection.getId() == R.id.guy ? 1 : 2) : 0);
					editor.commit();
					if (isBackPressed) {
						finishEditing();
					}
				}
			});
			JSONObject obj = new JSONObject();
			try
			{
				Log.d(getClass().getSimpleName(), "Profile details Email: " + mEmailEdit.getText() + " Gender: " + mActivityState.genderType);
				if(!emailTxt.equals(mEmailEdit.getText().toString()))
				{
					obj.put(HikeConstants.EMAIL, mEmailEdit.getText());
				}
				if(mActivityState.genderType != lastSavedGender)
				{
					obj.put(HikeConstants.GENDER, mActivityState.genderType == 1 ? "m" : mActivityState.genderType == 2 ? "f" : "");
				}
				Log.d(getClass().getSimpleName(), "JSON to be sent is: " + obj.toString());
				request.setJSONData(obj);
			}
			catch(JSONException e)
			{
				Log.e("ProfileActivity", "Could not set email or gender", e);
			}
			requests.add(request);
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
				return;
			}
			mActivityState.genderType = 0;
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
		for(Entry<String, GroupParticipant> participant : participantList.entrySet())
		{
			if (!participant.getValue().getContactInfo().isOnhike()) 
			{
				long time = (long) System.currentTimeMillis() / 1000;
				ConvMessage convMessage = new ConvMessage(getResources()
						.getString(R.string.invite_message), participant.getKey(), time,
						ConvMessage.State.SENT_UNCONFIRMED);
				convMessage.setInvite(true);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
						convMessage.serialize());
			}
		}
		Toast toast = Toast.makeText(ProfileActivity.this, R.string.invite_sent, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.BOTTOM, 0, 0);
		toast.show();
	}

	public void onAddNewParticipantsClicked(View v)
	{
		Utils.logEvent(ProfileActivity.this, HikeConstants.LogEvent.ADD_PARTICIPANT);

		Intent intent = new Intent(ProfileActivity.this, ChatThread.class);
		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		intent.putExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT, mLocalMSISDN);
		startActivity(intent);

		overridePendingTransition(R.anim.slide_in_right_noalpha,
				R.anim.slide_out_left_noalpha);
	}

	public void onBlockGroupOwnerClicked(View v)
	{
		HikeMessengerApp.getPubSub().publish(v.isSelected() ? HikePubSub.UNBLOCK_USER : HikePubSub.BLOCK_USER, this.groupOwner);
		v.setSelected(!v.isSelected());
	}

	public void onEditGroupNameClicked(View v)
	{
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
		Log.d(getClass().getSimpleName(), "ONSHOWN");
		mNameDisplay.setVisibility(View.GONE);
		mNameEdit.setVisibility(View.VISIBLE);
		mNameEdit.setSelection(mNameEdit.length());
		mNameEdit.requestFocus();
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if(mLocalMSISDN == null)
		{
			Log.w(getClass().getSimpleName(), "The msisdn is null, we are doing something wrong.." + object);
			return;
		}
		if (HikePubSub.ICON_CHANGED.equals(type)) 
		{
			if (mLocalMSISDN.equals((String)object)) 
			{
				HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
				nameTxt = db.getGroupName(mLocalMSISDN);

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mNameEdit.setText(nameTxt);
					}
				});
			}
		} 
		else if (HikePubSub.GROUP_NAME_CHANGED.equals(type)) 
		{
			if (mLocalMSISDN.equals((String)object)) 
			{
				final Drawable drawable = IconCacheManager.getInstance()
						.getIconForMSISDN(mLocalMSISDN);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mIconView.setImageDrawable(drawable);
					}
				});
			}
		}

		if (HikePubSub.PARTICIPANT_LEFT_GROUP.equals(type))
		{
			if(mLocalMSISDN.equals(((JSONObject)object).optString(HikeConstants.TO)))
			{
				final JSONObject obj = (JSONObject) object;
				this.participantList.remove(object);
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run() 
					{
						participantNameContainer.removeView(participantNameContainer.findViewById(obj.optString(HikeConstants.DATA).hashCode()));
						participantNameContainer.requestLayout();
					}
				});
			}
		}
		else if (HikePubSub.PARTICIPANT_JOINED_GROUP.equals(type))
		{
			if(mLocalMSISDN.equals(((JSONObject)object).optString(HikeConstants.TO)))
			{
				final JSONObject obj = (JSONObject) object;
				final JSONArray participants = obj.optJSONArray(HikeConstants.DATA);
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run() 
					{
						for (int i = 0; i < participants.length(); i++) 
						{
							String msisdn = participants.optJSONObject(i).optString(HikeConstants.MSISDN);

							HikeUserDatabase hUDB = HikeUserDatabase.getInstance();
							ContactInfo participant = hUDB.getContactInfoFromMSISDN(msisdn);

							if (TextUtils.isEmpty(participant.getName())) 
							{
								HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
								participant.setName(hCDB.getParticipantName(mLocalMSISDN, msisdn));
							}

							if (!participant.isOnhike()) 
							{
								shouldShowInviteAllButton = true;
								findViewById(R.id.invite_all_btn).setVisibility(View.VISIBLE);
							}

							TextView participantNameItem = (TextView) ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE))
									.inflate(R.layout.participant_name_item, null);
							participantNameItem.setText(participant.getFirstName());
							participantNameItem.setBackgroundResource(participant.isOnhike() ?
									R.drawable.hike_contact_bg: R.drawable.sms_contact_bg);

							LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
							lp.setMargins(0, 0, 0, (int) (6 * Utils.densityMultiplier));
							participantNameItem.setLayoutParams(lp);

							participantNameItem.setId(msisdn.hashCode());
							participantNameContainer.addView(participantNameItem);

							participantList.put(msisdn, new GroupParticipant(participant));
						}

					}
				});
			}
		}
		else if(HikePubSub.GROUP_END.equals(type))
		{
			mLocalMSISDN.equals(((JSONObject)object).optString(HikeConstants.TO));
			{
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run() 
					{
						ProfileActivity.this.finish();
					}
				});
			}
		}
		else if(HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					items[0].bindView(ProfileActivity.this, credits);
				}
			});
		}
	}
}
