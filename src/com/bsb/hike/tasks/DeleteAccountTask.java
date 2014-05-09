package com.bsb.hike.tasks;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.facebook.Session;
import com.google.android.gcm.GCMRegistrar;

public class DeleteAccountTask extends AsyncTask<Void, Void, Boolean> implements ActivityCallableTask
{

	private HikePreferences activity;

	private boolean finished;

	private boolean delete;

	private Context ctx;

	public DeleteAccountTask(HikePreferences activity, boolean delete, Context context)
	{
		this.activity = activity;
		this.delete = delete;
		this.ctx = context;
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		FileTransferManager.getInstance(ctx).shutDownAll();
		HikeUserDatabase db = HikeUserDatabase.getInstance();
		HikeConversationsDatabase convDb = HikeConversationsDatabase.getInstance();
		Editor editor = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();
		Editor appPrefEditor = PreferenceManager.getDefaultSharedPreferences(activity).edit();

		HikeMessengerApp.clearStealthMsisdn();

		try
		{
			AccountUtils.deleteOrUnlinkAccount(this.delete);

			// Unregister from GCM service
			GCMRegistrar.unregister(activity.getApplicationContext());

			HikeMessengerApp app = (HikeMessengerApp) activity.getApplicationContext();
			app.disconnectFromService();
			activity.stopService(new Intent(activity, HikeService.class));

			db.deleteAll();
			convDb.deleteAll();
			HikeMessengerApp.getLruCache().clearIconCache();
			// IconCacheManager.getInstance().clearIconCache();
			editor.clear();
			appPrefEditor.clear();
			Logger.d("DeleteAccountTask", "account deleted");

			Session session = Session.getActiveSession();
			if (session != null)
			{
				session.closeAndClearTokenInformation();
				Session.setActiveSession(null);
			}
			StickerManager.getInstance().deleteStickers();

			return true;
		}
		catch (Exception e)
		{
			Logger.e("DeleteAccountTask", "error deleting account", e);
			return false;
		}
		finally
		{
			editor.commit();
			appPrefEditor.commit();
		}
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		finished = true;
		if (result.booleanValue())
		{
			/* clear any toast notifications */
			NotificationManager mgr = (NotificationManager) activity.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
			mgr.cancelAll();

			// redirect user to the welcome screen
			activity.accountDeleted();
		}
		else
		{
			activity.dismissProgressDialog();
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(activity,
					this.delete ? activity.getResources().getString(R.string.delete_account_failed) : activity.getResources().getString(R.string.unlink_account_failed), duration);
			toast.show();
		}
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.activity = (HikePreferences) activity;
	}

	@Override
	public boolean isFinished()
	{
		return finished;
	}

}
