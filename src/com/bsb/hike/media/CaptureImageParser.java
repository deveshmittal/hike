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

public class CaptureImageParser {
	public interface CaptureImageListener {
		public void imageCaptured(Uri uri);

		public void imageCaptured(String imagePath);

		public void imageCaptureFailed();
	}

	/**
	 * 
	 * @param resultCode
	 *            - returned after camera intent
	 * @param data
	 *            - returned after camera intent
	 * @param listener
	 *            - listener to which give callback
	 */
	public static void parseOnActivityResult(Activity context, int resultCode,
			Intent data, CaptureImageListener listener) {
		Logger.d("parseimage", "onactivity result");
		HikeSharedPreferenceUtil sharedPreference = HikeSharedPreferenceUtil
				.getInstance(context);
		if (resultCode == Activity.RESULT_OK) {
			Logger.d("parseimage", "onactivity result ok");
			// this key was saved when we started camera activity
			String capturedFilepath = sharedPreference.getData(
					HikeMessengerApp.FILE_PATH, null);
			sharedPreference.removeData(HikeMessengerApp.FILE_PATH);
			if (capturedFilepath != null) {
				File imageFile = new File(capturedFilepath);
				// The default Android camera application returns a non-null
				// intent only when passing back a thumbnail in the returned
				// Intent. If you pass EXTRA_OUTPUT with a URL to write to, it
				// will return a null intent and the pictures is in the URL that
				// you passed in. We do pass EXTRA_OUTPUT and save in 
				if (!imageFile.exists()
						&& (data == null || data.getData() == null)) {
					listener.imageCaptureFailed();
				} else {
					// Image Capture Success, Upload File
					Uri selectedFileUri = Utils.makePicasaUri(imageFile
							.exists() ? Uri.fromFile(imageFile) : data
							.getData());
					if (Utils.isPicasaUri(selectedFileUri.toString())) {
						// Picasa image
						listener.imageCaptured(selectedFileUri);
					} else {
						listener.imageCaptured(PickFileParser.parseUri(data, context));
					}

				}

			} else {
				Logger.e("parseimage", " captured image path is null");
				listener.imageCaptureFailed();
			}
		} else {
			// result cancelled
			listener.imageCaptureFailed();
		}
	}
}
