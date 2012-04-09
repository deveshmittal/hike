package com.bsb.hike.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ProfileArrayAdapter;
import com.bsb.hike.cropimage.CropImage;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.ProfileItem;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.Utils;

public class ProfileActivity extends Activity implements OnItemClickListener, OnClickListener, FinishableEvent, android.content.DialogInterface.OnClickListener
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
	private ListView mListView;
	private TextView mTitleView;
	private TextView mMadeWithLoveView;
	private ImageView mTitleIcon;
	private View mProfilePictureChangeOverlay;
	private EditText mNameViewEdittable;
	private ProgressDialog mDialog;
	public String mLocalMSISDN = null;

	private ActivityState mActivityState; /* config state of this activity */
	private class ActivityState
	{
		public boolean editable = false; /* is this page currently editable */
		public HikeHTTPTask task; /* the task to update the global profile */
		public File selectedFileIcon; /* the selected file that we'll store the profile camera picture */

		public Bitmap newBitmap = null; /* the bitmap before the user saves it */
	}

	/* store the task so we can keep keep the progress dialog going */
	@Override
	public Object onRetainNonConfigurationInstance()
	{
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

		mIconView = (ImageView) findViewById(R.id.profile);
		mNameView = (TextView) findViewById(R.id.name);
		mNameViewEdittable = (EditText) findViewById(R.id.name_editable);
		mListView = (ListView) findViewById(R.id.profile_preferences);
		mTitleView = (TextView) findViewById(R.id.title);
		mTitleIcon = (ImageView) findViewById(R.id.title_icon);
		mMadeWithLoveView = (TextView) findViewById(R.id.made_with_love);
		mProfilePictureChangeOverlay = findViewById(R.id.profile_change_overlay);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String name = settings.getString(HikeMessengerApp.NAME, "Set a name!");
		mLocalMSISDN = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);

		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(mLocalMSISDN);
		mIconView.setImageDrawable(drawable);

		mNameView.setText(name);

		mIconView.setOnClickListener(this);

		mTitleView.setText(getResources().getString(R.string.profile_title));
		mTitleIcon.setVisibility(View.VISIBLE);

		/* add the heart in code because unicode isn't supported via xml*/
		//mMadeWithLoveView.setText(String.format(getString(R.string.made_with_love), "\u2665"));

		ProfileItem[] items = new ProfileItem[] 
			{
				new ProfileItem.ProfileSettingsItem("Credits", R.drawable.ic_credits, HikeMessengerApp.SMS_SETTING),
				new ProfileItem.ProfilePreferenceItem("Notifications", R.drawable.ic_notifications, R.xml.notification_preferences),
				new ProfileItem. ProfilePreferenceItem("Privacy", R.drawable.ic_privacy, R.xml.privacy_preferences),
				new ProfileItem.ProfileLinkItem("Help", R.drawable.ic_help, "http://www.bsb.im/about")
			};
		ProfileArrayAdapter adapter = new ProfileArrayAdapter(this, R.layout.profile_item, items);
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(this);

		updateEditableUI();
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		ProfileItem item = (ProfileItem) adapterView.getItemAtPosition(position);
		Intent intent = item.getIntent(this);
		if (intent != null)
		{
			startActivity(intent);			
		}
	}

	public void onTitleIconClick(View view)
	{
		mActivityState.editable = !mActivityState.editable;
		if (!mActivityState.editable)
		{
			/* hide the softkeyboard */
			InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mNameViewEdittable.getWindowToken(), 0);
	
			ArrayList<HikeHttpRequest> requests = new ArrayList<HikeHttpRequest>();

			/* save the new fields */
			String updatedName = mNameViewEdittable.getText().toString();
			if (!TextUtils.isEmpty(updatedName) && !mNameView.getText().equals(updatedName))
			{
				/* user edited the text, so update the profile */
				HikeHttpRequest request = new HikeHttpRequest("/account/name", new HikeHttpRequest.HikeHttpCallback()
				{
					public void onFailure()
					{
					}

					public void onSuccess()
					{
						/* if the request was successful, update the shared preferences and the UI */
						String name = mNameViewEdittable.getText().toString();
						mNameView.setText(name);
						Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
						editor.putString(HikeMessengerApp.NAME_SETTING, name);
						editor.commit();
					}
				});

				JSONObject json = new JSONObject();
				try
				{
					json.put("name", mNameViewEdittable.getText().toString());
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
				ByteArrayOutputStream bao = new ByteArrayOutputStream();
				mActivityState.newBitmap.compress(Bitmap.CompressFormat.PNG, 90, bao);
				final byte[] bytes = bao.toByteArray();
				HikeHttpRequest request = new HikeHttpRequest("/account/avatar", new HikeHttpRequest.HikeHttpCallback()
				{
					public void onFailure()
					{
						Log.d("ProfileActivity", "resetting image");
						mActivityState.newBitmap = null;
						/* reset the image */
						mIconView.setImageDrawable(IconCacheManager.getInstance().getIconForMSISDN(mLocalMSISDN));
					}

					public void onSuccess()
					{
						IconCacheManager.getInstance().clearIconForMSISDN(mLocalMSISDN);
						HikeUserDatabase db = new HikeUserDatabase(ProfileActivity.this);
						db.setIcon(mLocalMSISDN, bytes);
						db.close();
					}
				});

				request.setPostData(bytes);
				requests.add(request);
			}
			if (!requests.isEmpty())
			{
				mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
				mActivityState.task = new HikeHTTPTask(this);
				HikeHttpRequest[] r = new HikeHttpRequest[requests.size()];
				requests.toArray(r);
				mActivityState.task.execute(r);
			}
		}
		else
		{
			/* make the edittext the same as the currently set name */
			mNameViewEdittable.setText(mNameView.getText());
		}

		updateEditableUI();
	}

	private void updateEditableUI()
	{
		/* update the UI to let the user know that what's changeable */
		mProfilePictureChangeOverlay.setVisibility(mActivityState.editable ? View.VISIBLE : View.GONE);
		mNameViewEdittable.setVisibility(mActivityState.editable ? View.VISIBLE : View.GONE);
		mNameView.setVisibility(!mActivityState.editable ? View.VISIBLE : View.GONE);
		mTitleIcon.setImageResource(!mActivityState.editable ? R.drawable.ic_edit : R.drawable.ic_save);
		if (mActivityState.newBitmap != null)
		{
			mIconView.setImageBitmap(mActivityState.newBitmap);
		}
	}

	@Override
	public void onClick(View view)
	{
		Log.d("ProfileActivity", "View is " + view);
		if (mActivityState.editable && (view == mIconView))
		{
			/* The wants to change their profile picture.
			 * Open a dialog to allow them pick Camera or Gallery 
			 */
			final CharSequence[] items = {"Camera", "Gallery"};/*TODO externalize these */
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Choose a picture");
			builder.setItems(items, this);
			builder.show().setOwnerActivity(this);
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

		if (!success)
		{
			Toast.makeText(this, R.string.delete_account_failed, Toast.LENGTH_LONG).show();
		}
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
			path = (requestCode == CAMERA_RESULT) ? mActivityState.selectedFileIcon.getAbsolutePath() : getGalleryPath(data.getData());
			/* Crop the image */
			Intent intent = new Intent(this, CropImage.class);
			intent.putExtra("image-path", path);
			intent.putExtra("scale", true);
			intent.putExtra("outputX", 40);
			intent.putExtra("outputY", 40);
			intent.putExtra("aspectX", 1);
			intent.putExtra("aspectY", 1);
			startActivityForResult(intent, CROP_RESULT);
			break;
		case CROP_RESULT:
			Bitmap bitmap = data.getParcelableExtra("bitmap");
			mActivityState.newBitmap = Utils.getRoundedCornerBitmap(bitmap);
			bitmap.recycle();
			mIconView.setImageBitmap(mActivityState.newBitmap);
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
			mActivityState.selectedFileIcon = Utils.getOutputMediaFile(Utils.MEDIA_TYPE_IMAGE); // create a file to save the image
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mActivityState.selectedFileIcon));
			startActivityForResult(intent, CAMERA_RESULT);
			break;
		case PROFILE_PICTURE_FROM_GALLERY:
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, GALLERY_RESULT);
			break;
		}
	}
}
