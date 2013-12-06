package com.bsb.hike.tasks;

import java.io.File;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.facebook.Session;
import com.google.android.gcm.GCMRegistrar;

public class DeleteAccountTask extends AsyncTask<Void, Void, Boolean> implements
		ActivityCallableTask {

	private HikePreferences activity;
	private boolean finished;
	private boolean delete;
	private Context ctx;

	public DeleteAccountTask(HikePreferences activity, boolean delete,Context context) {
		this.activity = activity;
		this.delete = delete;
		this.ctx = context;
	}

	@Override
	protected Boolean doInBackground(Void... unused) {
		FileTransferManager.getInstance(ctx).shutDownAll();
		HikeUserDatabase db = HikeUserDatabase.getInstance();
		HikeConversationsDatabase convDb = HikeConversationsDatabase
				.getInstance();
		Editor editor = activity.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();
		Editor appPrefEditor = PreferenceManager.getDefaultSharedPreferences(
				activity).edit();

		try {
			AccountUtils.deleteOrUnlinkAccount(this.delete);

			// Unregister from GCM service
			GCMRegistrar.unregister(activity.getApplicationContext());

			HikeMessengerApp app = (HikeMessengerApp) activity
					.getApplicationContext();
			app.disconnectFromService();
			activity.stopService(new Intent(activity, HikeService.class));

			db.deleteAll();
			convDb.deleteAll();
			IconCacheManager.getInstance().clearIconCache();
			editor.clear();
			appPrefEditor.clear();
			Log.d("DeleteAccountTask", "account deleted");

			Session session = Session.getActiveSession();
			if (session != null) {
				session.closeAndClearTokenInformation();
			}
			deleteStickers();

			return true;
		} catch (Exception e) {
			Log.e("DeleteAccountTask", "error deleting account", e);
			return false;
		} finally {
			editor.commit();
			appPrefEditor.commit();
		}
	}

	private void deleteStickers() {
		/*
		 * First delete all stickers, if any, in the internal memory
		 */
		String dirPath = activity.getFilesDir().getPath()
				+ HikeConstants.STICKERS_ROOT;
		File dir = new File(dirPath);
		if (dir.exists()) {
			Utils.deleteFile(dir);
		}

		/*
		 * Next is the external memory. We first check if its available or not.
		 */
		if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE) {
			return;
		}
		String extDirPath = activity.getExternalFilesDir(null).getPath()
				+ HikeConstants.STICKERS_ROOT;
		File extDir = new File(extDirPath);
		if (extDir.exists()) {
			Utils.deleteFile(extDir);
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		finished = true;
		if (result.booleanValue()) {
			/* clear any toast notifications */
			NotificationManager mgr = (NotificationManager) activity
					.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
			mgr.cancelAll();

			// redirect user to the welcome screen
			activity.accountDeleted();
		} else {
			activity.dismissProgressDialog();
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(
					activity,
					this.delete ? activity.getResources().getString(
							R.string.delete_account_failed) : activity
							.getResources().getString(
									R.string.unlink_account_failed), duration);
			toast.show();
		}
	}

	@Override
	public void setActivity(Activity activity) {
		this.activity = (HikePreferences) activity;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

}
