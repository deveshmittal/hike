package com.bsb.hike.tasks;

import java.io.File;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.bsb.hike.utils.Utils;

public class DownloadPicasaImageTask extends AsyncTask<Void, Void, Boolean> {

	public static interface PicasaDownloadResult {
		public void downloadFinished(boolean result);
	}

	private File destFile;
	private Uri picasaUri;
	private Context context;
	private PicasaDownloadResult picasaDownloadResult;

	public DownloadPicasaImageTask(Context context, File destFile,
			Uri picasaUri, PicasaDownloadResult picasaDownloadResult) {
		this.destFile = destFile;
		this.picasaUri = picasaUri;
		this.context = context;
		this.picasaDownloadResult = picasaDownloadResult;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			Utils.downloadAndSaveFile(context, destFile, picasaUri);
			return Boolean.TRUE;
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "Error while fetching image", e);
			return Boolean.FALSE;
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		picasaDownloadResult.downloadFinished(result);
	}
}