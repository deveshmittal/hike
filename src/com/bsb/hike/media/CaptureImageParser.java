package com.bsb.hike.media;

import java.io.File;
import java.net.URI;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class CaptureImageParser
{
	public interface CaptureImageListener
	{
		public void imageCaptured(Uri uri);

		public void imageCaptured(String imagePath);

		public void imageCaptureFailed();
	}

	private static final String TAG = "parseimage";

	/**
	 * 
	 * @param resultCode
	 *            - returned after camera intent
	 * @param data
	 *            - returned after camera intent
	 * @param listener
	 *            - listener to which give callback
	 */
	public static void parseCameraResult(Activity context, int resultCode, Intent data, CaptureImageListener listener)
	{
		Logger.d(TAG, "onactivity result");

		HikeSharedPreferenceUtil sharedPreference = HikeSharedPreferenceUtil.getInstance(context.getApplicationContext());
		if (resultCode == Activity.RESULT_OK)
		{
			Logger.d(TAG, "onactivity result ok");
			// this key was saved when we started camera activity
			String capturedFilepath = sharedPreference.getData(HikeMessengerApp.FILE_PATH, null);
			sharedPreference.removeData(HikeMessengerApp.FILE_PATH);

			if (capturedFilepath != null)
			{
				File imageFile = new File(capturedFilepath);
				String filePath;

				if (imageFile != null && imageFile.exists())
				{
					filePath = imageFile.getAbsolutePath();
					listener.imageCaptured(filePath);
				}

			}
			else
			{
				Logger.e(TAG, "captured image path is null");
				listener.imageCaptureFailed();
			}
		}
		else
		{
			// result cancelled
			listener.imageCaptureFailed();
		}
	}
}
