package com.bsb.hike.utils;

import java.io.File;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class ChangeProfileImageBaseActivity extends HikeAppStateBaseFragmentActivity implements OnClickListener
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onClick(DialogInterface dialog, int item)
	{
		SharedPreferences accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		Intent intent = null;
		switch (item)
		{
		case HikeConstants.PROFILE_PICTURE_FROM_CAMERA:
			if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
			{
				Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!Utils.hasEnoughFreeSpaceForProfilePic())
			{
				Toast.makeText(getApplicationContext(), R.string.not_enough_space_profile_pic, Toast.LENGTH_SHORT).show();
				return;
			}
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File selectedFileIcon = Utils.getOutputMediaFile(HikeFileType.PROFILE, null, false); // create a file to save
			// the image
			if (selectedFileIcon != null)
			{
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFileIcon));

				/*
				 * Saving the file path. Will use this to get the file once the image has been captured.
				 */
				Editor editor = accountPrefs.edit();
				editor.putString(HikeMessengerApp.FILE_PATH, selectedFileIcon.getAbsolutePath());
				editor.commit();

				startActivityForResult(intent, HikeConstants.CAMERA_RESULT);
			}
			else
			{
				Toast.makeText(this, getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
			}
			break;
		case HikeConstants.PROFILE_PICTURE_FROM_GALLERY:
			if (Utils.getExternalStorageState() == ExternalStorageState.NONE)
			{
				Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!Utils.hasEnoughFreeSpaceForProfilePic())
			{
				Toast.makeText(getApplicationContext(), R.string.not_enough_space_profile_pic, Toast.LENGTH_SHORT).show();
				return;
			}
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, HikeConstants.GALLERY_RESULT);
			break;
		}
	}	
}
