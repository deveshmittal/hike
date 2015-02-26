package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.productpopup.DialogPojo;
import com.bsb.hike.productpopup.HikeDialogFragment;
import com.bsb.hike.productpopup.IActivityPopup;
import com.bsb.hike.productpopup.ProductContentModel;
import com.bsb.hike.productpopup.ProductInfoManager;
import com.bsb.hike.productpopup.ProductPopupsConstants;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class CreateNewGroupActivity extends ChangeProfileImageBaseActivity
{

	private SharedPreferences preferences;

	private String groupId;

	private ImageView groupImage;

	private EditText groupName;

	private View doneBtn;

	private ImageView arrow;

	private TextView postText;

	private Bitmap groupBitmap;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_new_group);

		setupActionBar();

		groupImage = (ImageView) findViewById(R.id.group_profile_image);
		groupName = (EditText) findViewById(R.id.group_name);
		groupName.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{

			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{

			}

			@Override
			public void afterTextChanged(Editable editable)
			{
				Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, !TextUtils.isEmpty(editable.toString().trim()));
			}
		});

		preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		if (savedInstanceState != null)
		{
			groupId = savedInstanceState.getString(HikeConstants.Extras.GROUP_ID);
		}

		if (TextUtils.isEmpty(groupId))
		{
			String uid = preferences.getString(HikeMessengerApp.UID_SETTING, "");
			groupId = uid + ":" + System.currentTimeMillis();
		}

		Object object = getLastCustomNonConfigurationInstance();
		if (object != null && (object instanceof Bitmap))
		{
			groupBitmap = (Bitmap) object;
			groupImage.setImageBitmap(groupBitmap);
		}
		else
		{
			groupImage.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(groupId, true));
		}
		
		int val=ProductPopupsConstants.PopupTriggerPoints.NEWGRP.ordinal();
		ProductInfoManager.getInstance().isThereAnyPopup(val,new IActivityPopup()
		{

			@Override
			public void onSuccess(final ProductContentModel mmModel)
			{
				runOnUiThread(new Runnable()
				{
					
					@Override
					public void run()
					{
						DialogPojo mmDialogPojo=ProductInfoManager.getInstance().getDialogPojo(mmModel);
						HikeDialogFragment mmFragment=HikeDialogFragment.onNewInstance(mmDialogPojo);
						mmFragment.showDialog(getSupportFragmentManager());
					}
				});
			
			}

			@Override
			public void onFailure()
			{
				// No Popup to display
			}
			
		});

	}

	@Override
	public void onBackPressed()
	{
		/**
		 * Deleting the temporary file, if it exists.
		 */
		File file = new File(Utils.getTempProfileImageFileName(groupId));
		file.delete();

		super.onBackPressed();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (groupBitmap != null)
		{
			return groupBitmap;
		}
		return super.onRetainCustomNonConfigurationInstance();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		if (!TextUtils.isEmpty(groupId))
		{
			outState.putString(HikeConstants.Extras.GROUP_ID, groupId);
		}
		super.onSaveInstanceState(outState);
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		doneBtn = actionBarView.findViewById(R.id.done_container);
		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.post_btn);

		doneBtn.setVisibility(View.VISIBLE);
		postText.setText(R.string.next_signup);

		Utils.toggleActionBarElementsEnable(doneBtn, arrow, postText, false);

		title.setText(R.string.new_group);

		backContainer.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onBackPressed();
			}
		});

		doneBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(CreateNewGroupActivity.this, ComposeChatActivity.class);
				intent.putExtra(HikeConstants.Extras.GROUP_NAME, groupName.getText().toString().trim());
				intent.putExtra(HikeConstants.Extras.GROUP_ID, groupId);
				intent.putExtra(HikeConstants.Extras.CREATE_GROUP, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		String path = null;
		if (resultCode != RESULT_OK)
		{
			return;
		}

		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		/*
		 * Making sure the directory exists before setting a profile image
		 */
		File dir = new File(directory);
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		String fileName = Utils.getTempProfileImageFileName(groupId);
		final String destFilePath = directory + "/" + fileName;

		File selectedFileIcon = null;

		switch (requestCode)
		{
		case HikeConstants.CAMERA_RESULT:
			/* fall-through on purpose */
		case HikeConstants.GALLERY_RESULT:
			Logger.d("ProfileActivity", "The activity is " + this);
			if (requestCode == HikeConstants.CAMERA_RESULT)
			{
				String filePath = preferences.getString(HikeMessengerApp.FILE_PATH, "");
				selectedFileIcon = new File(filePath);

				/*
				 * Removing this key. We no longer need this.
				 */
				Editor editor = preferences.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
			}
			if (requestCode == HikeConstants.CAMERA_RESULT && !selectedFileIcon.exists())
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			boolean isPicasaImage = false;
			Uri selectedFileUri = null;
			if (requestCode == HikeConstants.CAMERA_RESULT)
			{
				path = selectedFileIcon.getAbsolutePath();
			}
			else
			{
				if (data == null)
				{
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				}
				selectedFileUri = data.getData();
				if (Utils.isPicasaUri(selectedFileUri.toString()))
				{
					isPicasaImage = true;
					path = Utils.getOutputMediaFile(HikeFileType.PROFILE, null, false).getAbsolutePath();
				}
				else
				{
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart))
					{
						selectedFileIcon = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						path = selectedFileIcon.getAbsolutePath();
					}
					else
					{
						path = Utils.getRealPathFromUri(selectedFileUri, this);
					}
				}
			}
			if (TextUtils.isEmpty(path))
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!isPicasaImage)
			{
				Utils.startCropActivity(this, path, destFilePath);
			}
			else
			{
				/*
				 * TODO handle picasa case.
				 */
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			break;
		case HikeConstants.CROP_RESULT:
			String finalDestFilePath = data.getStringExtra(MediaStore.EXTRA_OUTPUT);
			if (finalDestFilePath == null)
			{
				Toast.makeText(getApplicationContext(), R.string.error_setting_profile, Toast.LENGTH_SHORT).show();
				return;
			}

			Bitmap tempBitmap = HikeBitmapFactory.scaleDownBitmap(finalDestFilePath, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS,
					Bitmap.Config.RGB_565, true, false);

			groupBitmap = HikeBitmapFactory.getCircularBitmap(tempBitmap);
			groupImage.setImageBitmap(HikeBitmapFactory.getCircularBitmap(tempBitmap));

			/*
			 * Saving the icon in the DB.
			 */
			byte[] bytes = BitmapUtils.bitmapToBytes(tempBitmap, CompressFormat.JPEG, 100);

			tempBitmap.recycle();

			ContactManager.getInstance().setIcon(groupId, bytes, false);

			break;
		}
	}
}
