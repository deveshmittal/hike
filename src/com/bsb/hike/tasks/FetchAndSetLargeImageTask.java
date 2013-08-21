package com.bsb.hike.tasks;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.Utils;

public class FetchAndSetLargeImageTask extends AsyncTask<Void, Void, Bitmap> {

	private Context context;
	private File tempFile;
	private File orgFile;
	private ImageView iv;
	private String id;

	public FetchAndSetLargeImageTask(Context context, ImageView iv,
			String id) {
		this.context = context;
		this.iv = iv;
		this.id = id;
	}

	@Override
	protected void onPreExecute() {
		final Drawable avatarDrawable = IconCacheManager.getInstance()
				.getIconForMSISDN(id);
		iv.setImageDrawable(avatarDrawable);
	}

	@Override
	protected Bitmap doInBackground(Void... params) {
		String basePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;
		String fileName = Utils.getProfileImageFileName(id);

		orgFile = new File(basePath, fileName);
		if (!orgFile.exists()) {
			return null;
		}

		File outputDir = context.getCacheDir();
		try {
			tempFile = File.createTempFile(id, ".jpg", outputDir);

			Utils.saveBitmapToFile(tempFile,
					BitmapFactory.decodeFile(orgFile.getPath()),
					CompressFormat.JPEG, 50);
			return BitmapFactory.decodeFile(tempFile.getPath());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (result == null) {
			return;
		}

		StatusMessage statusMessage = (StatusMessage) iv.getTag();
		if (statusMessage.getMappedId() != id) {
			return;
		}

		iv.setImageBitmap(result);
		tempFile.delete();
	}
}