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
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.facebook.Session;
import com.google.android.gcm.GCMRegistrar;

public class DeleteAccountTask extends AsyncTask<Void, Void, Boolean> implements ActivityCallableTask
{

	public static interface DeleteAccountListener
	{
		public void accountDeleted(boolean isSuccess);
	}

	private DeleteAccountListener listener;

	private boolean finished;

	private boolean delete;

	private Context ctx;

	public DeleteAccountTask(DeleteAccountListener activity, boolean delete, Context context)
	{
		this.listener = activity;
		this.delete = delete;
		this.ctx = context;
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		FileTransferManager.getInstance(ctx).shutDownAll();
		HikeConversationsDatabase convDb = HikeConversationsDatabase.getInstance();
		Editor editor = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();
		Editor appPrefEditor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();

		HikeMessengerApp.clearStealthMsisdn();

		try
		{
			AccountUtils.deleteOrUnlinkAccount(this.delete);

			// Unregister from GCM service
			GCMRegistrar.unregister(ctx.getApplicationContext());

			HikeMessengerApp app = (HikeMessengerApp) ctx.getApplicationContext();
			app.disconnectFromService();
			ctx.stopService(new Intent(ctx, HikeService.class));

			ContactManager.getInstance().deleteAll();
			convDb.deleteAll();
			HikeMessengerApp.getLruCache().clearIconCache();
			HikeMessengerApp.getContactManager().clearCache();
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

			/*
			 * We need to do this where on reset/delete account. We need to we need to run initial setup for stickers. for normal cases it runs from onCreate method of
			 * HikeMessangerApp but in this case onCreate won't be called and user can complete signup.
			 */
			app.startUpdgradeIntent();
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
			NotificationManager mgr = (NotificationManager) ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
			mgr.cancelAll();

			// redirect user to the welcome screen
			if (listener != null)
			{
				listener.accountDeleted(true);
			}
		}
		else
		{
			if (listener != null)
			{
				listener.accountDeleted(false);
			}
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(ctx,
					this.delete ? ctx.getResources().getString(R.string.delete_account_failed) : ctx.getResources().getString(R.string.unlink_account_failed), duration);
			toast.show();
		}
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.listener = (DeleteAccountListener) activity;
	}

	@Override
	public boolean isFinished()
	{
		return finished;
	}

}
