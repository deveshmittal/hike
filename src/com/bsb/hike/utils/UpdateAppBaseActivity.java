package com.bsb.hike.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.RemoteViews;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.ui.MessagesList;

/**
 * Base Activity which is to be extended by other activities. Used to notify if update is available and for downloading and installing the update.
 * @author rs
 *
 */
public class UpdateAppBaseActivity extends Activity {

	// URL for fetching the newest APK
	public static final String UPDATE_URL = "http://192.168.11.7/hike.apk";

	@Override
	protected void onResume() {
		super.onResume();

		updateApp();
	}

	private void updateApp()
	{
		SharedPreferences prefs = getSharedPreferences(HikeMessengerApp.UPDATE_SETTING, 0);
		Editor editor = prefs.edit();
		int update = prefs.getInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
		if(update == HikeConstants.CRITICAL_UPDATE)
		{
			updateAppDialog(true);
		}
		else if(update == HikeConstants.UPDATE_AVAILABLE)
		{
			updateAppDialog(false);
		}
		editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
		editor.commit();
	}

	private void updateAppDialog(final boolean exitIfNotUpdated)
	{
		AlertDialog alertDialog;
		Builder builder = new Builder(this);
		if(exitIfNotUpdated)
		{
			builder.setMessage("You have not updated for quite some time. Update the app for proper functioning.");
		}
		else
		{
			builder.setMessage("There is an update available. Would you like to update the app?");
		}
		builder.setPositiveButton("Sure!", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Update the app
				DownloadAndInstallUpdateAsyncTask updateAsyncTask = new DownloadAndInstallUpdateAsyncTask();
				updateAsyncTask.execute();

			}
		});
		builder.setNegativeButton("Nope", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(exitIfNotUpdated)
				{
					// Exit the app
					finish();
				}
			}
		});
		alertDialog = builder.create();
		alertDialog.show();
	}

	private class DownloadAndInstallUpdateAsyncTask extends AsyncTask<Void, Integer, Void>
	{
		private String filePath;
		private Notification notification;
		private NotificationManager notificationManager;
		private int notificationId = 119;

		@Override
		protected void onPreExecute() {
			// Show progress notification
			Intent intent = new Intent(getApplicationContext(), MessagesList.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			notification = new Notification(R.drawable.ic_launcher, "Downloading file...", System
					.currentTimeMillis());
			notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
			notification.contentIntent = pendingIntent;
			notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_notification);
			notification.contentView.setProgressBar(R.id.download_progress, 100, 0, false);
			notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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
			Log.d("MessagesList", "Progress: "+ values[0]);
			notification.contentView.setProgressBar(R.id.download_progress, 100, values[0], false);
			notificationManager.notify(notificationId, notification);
		}

		@Override
		protected void onPostExecute(Void result) {
			notificationManager.cancel(notificationId);
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new File(filePath+"hike.apk")), "application/vnd.android.package-archive");
			startActivity(intent);
		}
	}
}
