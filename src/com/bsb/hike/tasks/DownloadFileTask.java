package com.bsb.hike.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

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
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.FileTransferCancelledException;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadFileTask extends FileTransferTaskBase {
	private File destinationFile;
	private String fileKey;
	private Context context;
	private long msgId;
	private ConvMessage convMessage;
	private HikeFileType hikeFileType;
	private boolean showToast;

	public DownloadFileTask(Context context, File destinationFile,
			String fileKey, ConvMessage convMessage, HikeFileType hikeFileType,
			long msgId) {
		this(context, destinationFile, fileKey, convMessage, hikeFileType,
				msgId, true);
	}

	public DownloadFileTask(Context context, File destinationFile,
			String fileKey, ConvMessage convMessage, HikeFileType hikeFileType,
			long msgId, boolean showToast) {
		this.destinationFile = destinationFile;
		this.fileKey = fileKey;
		this.context = context;
		this.hikeFileType = hikeFileType;
		this.msgId = msgId;
		this.convMessage = convMessage;
		this.showToast = showToast;
	}

	@Override
	protected void onPreExecute() {
		HikeMessengerApp.fileTransferTaskMap.put(msgId, this);
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	@Override
	protected FTResult doInBackground(Void... params) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			URL url = new URL(AccountUtils.fileTransferBaseDownloadUrl
					+ fileKey);
			URLConnection urlConnection = url.openConnection();
			if (AccountUtils.ssl) {
				((HttpsURLConnection) urlConnection)
						.setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
			}
			AccountUtils.addUserAgent(urlConnection);

			int length = urlConnection.getContentLength();

			if (length > Utils.getFreeSpace()) {
				return FTResult.FILE_TOO_LARGE;
			}

			is = new BufferedInputStream(urlConnection.getInputStream());

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
					throw new FileTransferCancelledException(
							"Download cancelled by the user");
				}
			}
			Log.d(getClass().getSimpleName(), "Done downloading file");
		} catch (MalformedURLException e) {
			Log.e(getClass().getSimpleName(), "Invalid URL", e);
			return FTResult.DOWNLOAD_FAILED;
		} catch (FileNotFoundException e) {
			Log.e(getClass().getSimpleName(), "File Expired", e);
			return FTResult.FILE_EXPIRED;
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), "Error while downloding file", e);
			return FTResult.DOWNLOAD_FAILED;
		} catch (FileTransferCancelledException e) {
			return FTResult.CANCELLED;
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
							: result == FTResult.FILE_EXPIRED ? R.string.file_expire
									: R.string.download_failed;
			if (showToast) {
				Toast.makeText(context, errorStringId, Toast.LENGTH_SHORT)
						.show();
			}
			if (destinationFile != null) {
				destinationFile.delete();
			}
		}

		if (destinationFile != null) {
			if (destinationFile.exists()
					&& hikeFileType != HikeFileType.AUDIO_RECORDING) {
				context.sendBroadcast(new Intent(
						Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
								.fromFile(destinationFile)));
			}
		}

		HikeMessengerApp.fileTransferTaskMap.remove(msgId);
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
		if (HikeFileType.IMAGE == hikeFileType)
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.PUSH_FILE_DOWNLOADED, convMessage);

	}
}