package com.bsb.hike.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.ui.MessagesList;

public class DownloadAndInstallUpdateAsyncTask extends AsyncTask<Void, Integer, Void>
{
	private String filePath;
	private Notification notification;
	private NotificationManager notificationManager;
	private int notificationId = 184;
	private ProgressBar updateProgress;
	private Context context;

	// URL for fetching the newest APK
	public static final String UPDATE_URL = "http://10.1.0.45/hike.apk";	

	public DownloadAndInstallUpdateAsyncTask(Context context, ProgressBar updateProgress) 
	{
		this.context = context;
		this.updateProgress = updateProgress;
	}

	@Override
	protected void onPreExecute() {
		// Show progress notification
		Intent intent = new Intent(context, MessagesList.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		notification = new Notification(R.drawable.ic_contact_logo, "Downloading file...", System
				.currentTimeMillis());
		notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
		notification.contentIntent = pendingIntent;
		notification.contentView = new RemoteViews(context.getPackageName(), R.layout.download_notification);
		notification.contentView.setProgressBar(R.id.download_progress, 100, 0, false);
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationId, notification);
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		try {
			URL url = new URL(UPDATE_URL);
			URLConnection urlConnection = url.openConnection();

			int length = urlConnection.getContentLength();

			InputStream is = new BufferedInputStream(url.openConnection().getInputStream());

			filePath = Environment.getExternalStorageDirectory()+"/hike/";
			File file = new File(filePath);
			file.mkdirs();

			File myApk = new File(file, "hike.apk");
			FileOutputStream fos = new FileOutputStream(myApk);

			byte[] buffer = new byte[4096];
			int len = 0;
			int totProg = 0;
			int progress;

			while((len = is.read(buffer)) != -1)
			{
				totProg += len;
				fos.write(buffer, 0, len);
				progress = (int) ((totProg*100/length));
				if (progress % 10 == 0) {
					publishProgress(progress);
				}
			}
			fos.close();
			is.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		updateProgress.setProgress(values[0]);
		notification.contentView.setProgressBar(R.id.download_progress, 100, values[0], false);
		notificationManager.notify(notificationId, notification);
	}

	@Override
	protected void onPostExecute(Void result) {
		SharedPreferences prefs = context.getSharedPreferences(HikeMessengerApp.UPDATE_SETTING, 0);
		Editor editor = prefs.edit();
		editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
		editor.commit();
		notificationManager.cancel(notificationId);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(filePath+"hike.apk")), "application/vnd.android.package-archive");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
		context.startActivity(intent);
	}
}