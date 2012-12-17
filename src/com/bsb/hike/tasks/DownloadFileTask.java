package com.bsb.hike.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadFileTask extends FileTransferTaskBase {
	private File destinationFile;
	private String fileKey;
	private Context context;
	private long msgId;
	private HikeFileType hikeFileType;

	public DownloadFileTask(Context context, File destinationFile,
			String fileKey, long msgId, HikeFileType hikeFileType) {
		this.destinationFile = destinationFile;
		this.fileKey = fileKey;
		this.context = context;
		this.msgId = msgId;
		this.cancelTask = new AtomicBoolean(false);
		this.hikeFileType = hikeFileType;
	}

	@Override
	protected FTResult doInBackground(Void... params) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			URL url = new URL(AccountUtils.FILE_TRANSFER_BASE_DOWNLOAD_URL
					+ fileKey);
			URLConnection urlConnection = url.openConnection();

			int length = urlConnection.getContentLength();

			if (length > Utils.getFreeSpace()) {
				return FTResult.FILE_TOO_LARGE;
			}

			is = new BufferedInputStream(url.openConnection().getInputStream());

			fos = new FileOutputStream(destinationFile);

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len = 0;
			int totProg = 0;
			int progress;

			while ((len = is.read(buffer)) != -1) {
				totProg += len;
				fos.write(buffer, 0, len);
				progress = (int) ((totProg * 100 / length));
				publishProgress(progress);
				if (cancelTask.get()) {
					throw new IOException("Download cancelled by the user");
				}
			}
			Log.d(getClass().getSimpleName(), "Done downloading file");
		} catch (MalformedURLException e) {
			Log.e(getClass().getSimpleName(), "Invalid URL", e);
			return FTResult.DOWNLOAD_FAILED;
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), "Error while downloding file", e);
			return FTResult.DOWNLOAD_FAILED;
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), "Error while closing file", e);
				return FTResult.DOWNLOAD_FAILED;
			}
		}
		return FTResult.SUCCESS;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		updateProgress(values[0]);
	}

	@Override
	protected void onPostExecute(FTResult result) {
		if (result != FTResult.SUCCESS) {
			int errorStringId = result == FTResult.FILE_TOO_LARGE ? R.string.not_enough_space
					: result == FTResult.CANCELLED ? R.string.download_cancelled
							: R.string.download_failed;
			Toast.makeText(context, errorStringId, Toast.LENGTH_SHORT).show();
			destinationFile.delete();
		}

		/*
		 * Forcing a media scan for images.
		 */
		if (hikeFileType == HikeFileType.IMAGE) {
			context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
					.fromFile(destinationFile)));
		}

		ChatThread.fileTransferTaskMap.remove(msgId);
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}
}