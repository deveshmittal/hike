package com.bsb.hike.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
	private TextView mMadeWithLoveView;

	private ViewGroup credits;
	private ViewGroup notifications;
	private ViewGroup privacy;
	private ViewGroup help;
	private ViewGroup myInfo;

	private Dialog mDialog;
	public String mLocalMSISDN = null;

	private ActivityState mActivityState; /* config state of this activity */
	private TextView mEmailView;
	private class ActivityState
	{
		public HikeHTTPTask task; /* the task to update the global profile */

		public Bitmap newBitmap = null; /* the bitmap before the user saves it */
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
		mNameView = (TextView) findViewById(R.id.name);
		mTitleView = (TextView) findViewById(R.id.title);
		mMadeWithLoveView = (TextView) findViewById(R.id.made_with_love);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String name = settings.getString(HikeMessengerApp.NAME, "Set a name!");
		mLocalMSISDN = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);

		Drawable drawable = IconCacheManager.getInstance().getIconForMSISDN(getLargerIconId());
		mIconView.setImageDrawable(drawable);

		mNameView.setText(name);

		mIconView.setOnClickListener(this);
		myInfo.setOnClickListener(this);

		mTitleView.setText(getResources().getString(R.string.profile_title));
		/* add the heart in code because unicode isn't supported via xml*/
		//mMadeWithLoveView.setText(String.format(getString(R.string.made_with_love), "\u2665"));
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
//		updateEditableUI();
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
	
	public void changeProfilePic()
	{
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
					/* reset the image */
					mIconView.setImageDrawable(IconCacheManager.getInstance().getIconForMSISDN(getLargerIconId()));
				}

				public void onSuccess()
				{
					HikeUserDatabase db = new HikeUserDatabase(ProfileActivity.this);
					db.setIcon(mLocalMSISDN, bytes);
					db.setIcon(getLargerIconId(), larger_bytes);
					db.close();
				}
			});

			request.setPostData(bytes);
			mDialog = ProgressDialog.show(this, null, getResources().getString(R.string.updating_profile));
			mActivityState.task = new HikeHTTPTask(this, R.string.update_profile_failed);
			mActivityState.task.execute(request);
		}
	}

	protected String getLargerIconId()
	{
		return mLocalMSISDN + "::large";
	}

//	private void updateEditableUI()
//	{
//		/* update the UI to let the user know that what's changeable */
//		mProfilePictureChangeOverlay.setVisibility(mActivityState.editable ? View.VISIBLE : View.GONE);
//		mNameViewEdittable.setVisibility(mActivityState.editable ? View.VISIBLE : View.GONE);
//		mNameView.setVisibility(!mActivityState.editable ? View.VISIBLE : View.GONE);
//		mTitleIcon.setText(!mActivityState.editable ? R.string.edit : R.string.save);
//		if (mActivityState.newBitmap != null)
//		{
//			mIconView.setImageBitmap(mActivityState.newBitmap);
//		}
//	}

	@Override
	public void onClick(View view)
	{
		Log.d("ProfileActivity", "View is " + view);
		if ((view == mIconView))
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
			mIconView.setImageBitmap(mActivityState.newBitmap);
			changeProfilePic();
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
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFileIcon));
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
