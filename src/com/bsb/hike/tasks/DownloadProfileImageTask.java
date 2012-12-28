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
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.mqtt.client.HikeSSLUtil;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Utils;

public class DownloadProfileImageTask extends AsyncTask<Void, Void, Boolean> {

	private Context context;
	private String msisdn;
	private String urlString;
	private String fileName;
	private String filePath;

	public DownloadProfileImageTask(Context context, String msisdn,
			String fileName, boolean hasCustomIcon) {
		this.context = context;
		this.msisdn = msisdn;

		boolean isGroupConversation = Utils.isGroupConversation(msisdn);

		if (hasCustomIcon) {
			this.urlString = AccountUtils.base
					+ (isGroupConversation ? "/group/" + msisdn + "/avatar"
							: "/account/avatar/" + msisdn) + "?fullsize=1";
		} else {
			this.urlString = (AccountUtils.ssl ? AccountUtils.HTTPS_STRING
					: AccountUtils.HTTP_STRING)
					+ AccountUtils.host
					+ ":"
					+ AccountUtils.port + "/static/avatars/" + fileName;
		}

		this.filePath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;
		this.fileName = filePath + "/" + fileName;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			File dir = new File(filePath);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					return Boolean.FALSE;
				}
			}

			Log.d(getClass().getSimpleName(), "Downloading profile image: "
					+ urlString);
			URL url = new URL(urlString);

			URLConnection connection = url.openConnection();
			connection.addRequestProperty("Cookie", "user="
					+ AccountUtils.mToken);

			if (AccountUtils.ssl) {
				((HttpsURLConnection) connection)
						.setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
			}

			is = new BufferedInputStream(connection.getInputStream());

			fos = new FileOutputStream(fileName);

			byte[] buffer = new byte[HikeConstants.MAX_BUFFER_SIZE_KB * 1024];
			int len = 0;

			while ((len = is.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}

		} catch (MalformedURLException e) {
			Log.e(getClass().getSimpleName(), "Invalid URL", e);
			return Boolean.FALSE;
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), "Error while downloding file", e);
			return Boolean.FALSE;
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
				return Boolean.FALSE;
			}
		}
		return Boolean.TRUE;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result == false) {
			Toast.makeText(context, R.string.error_download,
					Toast.LENGTH_SHORT).show();
			File file = new File(fileName);
			file.delete();
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.PROFILE_IMAGE_NOT_DOWNLOADED, msisdn);
		} else {
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.PROFILE_IMAGE_DOWNLOADED, msisdn);
		}
	}

}
