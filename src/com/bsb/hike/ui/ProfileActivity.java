package com.bsb.hike.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.cropimage.CropImage;
import com.bsb.hike.cropimage.Util;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.Utils;

public class ProfileActivity extends Activity implements OnClickListener, FinishableEvent, android.content.DialogInterface.OnClickListener
{
	/* dialog IDs */
	private static final int PROFILE_PICTURE_FROM_CAMERA = 0;
	private static final int PROFILE_PICTURE_FROM_GALLERY = 1;

	/* activityForResult IDs */
	private static final int CAMERA_RESULT = 0;
	private static final int GALLERY_RESULT = 1;
	private static final int CROP_RESULT = 2;

	private ImageView mIconView;
	private TextView mNameView;
	private TextView mTitleView;
	private EditText mNameEdit;

	private ViewGroup credits;
	private ViewGroup notifications;
	private ViewGroup privacy;
	private ViewGroup help;
	private ViewGroup myInfo;

	private ViewGroup name;
	private ViewGroup phone;
	private ViewGroup email;
	private ViewGroup gender;
	private ViewGroup picture;

	private View currentSelection;

	private Dialog mDialog;
	public String mLocalMSISDN = null;

	private ActivityState mActivityState; /* config state of this activity */
	private String nameTxt;
	private boolean isEditingProfile = false;
	private boolean isBackPressed = false;
	private EditText mEmailEdit;
	private String emailTxt;
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
		mActivityState = null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);

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
		fetchPersistentData();

		mTitleView = (TextView) findViewById(R.id.title);

		isEditingProfile = getIntent().getBooleanExtra(HikeConstants.Extras.EDIT_PROFILE, false);

		if(isEditingProfile)
		{
			setupEditScreen();
		}
		else
		{
			setupProfileScreen();
		}
	}
	
	private void setupEditScreen()
	{
		findViewById(R.id.me_layout).setVisibility(View.GONE);
		findViewById(R.id.settings_txt).setVisibility(View.GONE);
		findViewById(R.id.prefs).setVisibility(View.GONE);
		findViewById(R.id.with_love_layout).setVisibility(View.GONE);
		ViewGroup editProfile =(ViewGroup) findViewById(R.id.edit_profile);
		editProfile.setVisibility(View.VISIBLE);

		name = (ViewGroup) findViewById(R.id.name);
		phone = (ViewGroup) findViewById(R.id.phone);
		email = (ViewGroup) findViewById(R.id.email);
		gender = (ViewGroup) findViewById(R.id.gender);
		picture = (ViewGroup) findViewById(R.id.photo);

		mNameEdit = (EditText) name.findViewById(R.id.name_input);
		mEmailEdit = (EditText) email.findViewById(R.id.email_input);

		((TextView)name.findViewById(R.id.name_edit_field)).setText("Name");
		((TextView)phone.findViewById(R.id.phone_edit_field)).setText("Phone");
		((TextView)email.findViewById(R.id.email_edit_field)).setText("Email");
		((TextView)gender.findViewById(R.id.gender_edit_field)).setText("Gender");
		((TextView)picture.findViewById(R.id.photo_edit_field)).setText("Edit Picture");

		picture.setOnClickListener(this);
		picture.setBackgroundResource(R.drawable.profile_bottom_item_selector);

		mTitleView.setText(getResources().getString(R.string.edit_profile));
		((EditText)phone.findViewById(R.id.phone_input)).setText(mLocalMSISDN);
		((EditText)phone.findViewById(R.id.phone_input)).setEnabled(false);

		mNameEdit.setText(nameTxt);
		mEmailEdit.setText(emailTxt);

		mNameEdit.setSelection(nameTxt.length());
		mEmailEdit.setSelection(emailTxt.length());

		onEmoticonClick(mActivityState.genderType == 0 ? null : mActivityState.genderType == 1 ? gender.findViewById(R.id.guy) : gender.findViewById(R.id.girl));
	}
	
	private void setupProfileScreen()
	{
		myInfo = (ViewGroup) findViewById(R.id.my_info); 
		credits = (ViewGroup) findViewById(R.id.free_sms);
		notifications = (ViewGroup) findViewById(R.id.notifications);
		privacy = (ViewGroup) findViewById(R.id.privacy);
		help = (ViewGroup) findViewById(R.id.help);

		myInfo.setBackgroundResource(R.drawable.profile_top_item_selector);
		credits.setBackgroundResource(R.drawable.profile_bottom_item_selector);
		notifications.setBackgroundResource(R.drawable.profile_top_item_selector);
		privacy.setBackgroundResource(R.drawable.profile_center_item_selector);
		help.setBackgroundResource(R.drawable.profile_bottom_item_selector);

		mIconView = (ImageView) findViewById(R.id.profile);
		mNameView = (TextView) findViewById(R.id.name_current);

		ViewGroup[] itemLayouts = new ViewGroup[]
				{
					credits, notifications, privacy, help
				};

		ProfileItem[] items = new ProfileItem[] 
			{
				new ProfileItem.ProfileSettingsItem("Free SMS left", R.drawable.ic_credits, HikeMessengerApp.SMS_SETTING),
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
		mIconView.setOnClickListener(this);
		myInfo.setOnClickListener(this);

		mTitleView.setText(getResources().getString(R.string.profile_title));
		mNameView.setText(nameTxt);
		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(getLargerIconId());
		mIconView.setImageDrawable(drawable);
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
		if(isEditingProfile)
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

		if (mNameEdit != null && !TextUtils.isEmpty(mNameEdit.getText()) && !nameTxt.equals(mNameEdit.getText().toString()))
		{
			/* user edited the text, so update the profile */
			HikeHttpRequest request = new HikeHttpRequest("/account/name", new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					if (isBackPressed) {
						finishEditing();
					}
				}

				public void onSuccess()
				{
					/* if the request was successful, update the shared preferences and the UI */
					String name = mNameEdit.getText().toString();
					Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
					editor.putString(HikeMessengerApp.NAME_SETTING, name);
					editor.commit();
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

			bao = new ByteArrayOutputStream();
			mActivityState.newBitmap.compress(Bitmap.CompressFormat.PNG, 90, bao);
			final byte[] larger_bytes = bao.toByteArray();

			HikeHttpRequest request = new HikeHttpRequest("/account/avatar", new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
					Log.d("ProfileActivity", "resetting image");
					mActivityState.newBitmap = null;
					if (mIconView != null) {
						/* reset the image */
						mIconView.setImageDrawable(IconCacheManager
								.getInstance().getIconForMSISDN(
										getLargerIconId()));
					}
					if (isBackPressed) {
						finishEditing();
					}
				}

				public void onSuccess()
				{
					HikeUserDatabase db = new HikeUserDatabase(ProfileActivity.this);
					db.setIcon(mLocalMSISDN, bytes);
					db.setIcon(getLargerIconId(), larger_bytes);
					db.close();
					if (isBackPressed) {
						finishEditing();
					}
				}
			});

			request.setPostData(bytes);
			requests.add(request);
		}

		if (mEmailEdit != null) {
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
			Intent i = new Intent(this, ProfileActivity.class);
			startActivity(i);
			finish();
		}
	}

	private void finishEditing()
	{
		Intent i = new Intent(this, ProfileActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		finish();
	}

	protected String getLargerIconId()
	{
		return mLocalMSISDN + "::large";
	}

	@Override
	public void onClick(View view)
	{
		Log.d("ProfileActivity", "View is " + view);
		if (view == mIconView || view == picture)
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
		else if(view == myInfo)
		{
			Intent i = new Intent(ProfileActivity.this, ProfileActivity.class);
			i.putExtra(HikeConstants.Extras.EDIT_PROFILE, true);
			startActivity(i);
			finish();
		}
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
			if (!isEditingProfile) {
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
			mActivityState.genderType = currentSelection.getId() == R.id.guy ? 1 : 2;
		}
	}
}
