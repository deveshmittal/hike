package com.bsb.hike.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Log;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.mqtt.client.HikeSSLUtil;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadSingleStickerTask extends StickerTaskBase {

	private String urlString;
	private String dirPath;
	private String filePath;
	private ImageView stickerImageView;
	private String key;

	public DownloadSingleStickerTask(Context context, String catId,
			String stId, ImageView stickerImageView) {
		this.key = catId + stId;
		this.dirPath = Utils.getExternalStickerDirectoryForCatgoryId(context,
				catId);
		this.filePath = this.dirPath + "/" + stId;

		this.urlString = AccountUtils.base + "/stickers?catId=" + catId
				+ "&stId=" + stId + "&resId=" + Utils.getResolutionId();

		this.stickerImageView = stickerImageView;
	}

	@Override
	protected FTResult doInBackground(Void... arg0) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			File dir = new File(dirPath);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					return FTResult.DOWNLOAD_FAILED;
				}
			}

			Log.d(getClass().getSimpleName(), "Downloading sticker: "
					+ urlString);
			URL url = new URL(urlString);

			URLConnection connection = url.openConnection();
			connection.addRequestProperty("Cookie", "user="
					+ AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

			Log.d(getClass().getSimpleName(),
					"File size: " + connection.getContentLength());
			if (AccountUtils.ssl) {
				((HttpsURLConnection) connection)
						.setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
			}

			is = new BufferedInputStream(connection.getInputStream());

			fos = new FileOutputStream(filePath);

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len = 0;

			while ((len = is.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}

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
	protected void onPostExecute(FTResult result) {
		ChatThread.stickerTaskMap.remove(key);
		if (result != FTResult.SUCCESS) {
			(new File(filePath)).delete();
			return;
		}
		if (stickerImageView != null) {
			stickerImageView.setImageBitmap(BitmapFactory.decodeFile(filePath));
		}
	}
}
