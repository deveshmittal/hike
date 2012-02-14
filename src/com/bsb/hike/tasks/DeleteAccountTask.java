package com.bsb.hike.tasks;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils;

public class DeleteAccountTask extends AsyncTask<Void, Void, Boolean> implements ActivityCallableTask
{

	private Activity activity;
	private boolean finished;

	public DeleteAccountTask(Activity activity)
	{
		this.activity = activity;
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		HikeUserDatabase db = new HikeUserDatabase(activity);
		HikeConversationsDatabase convDb = new HikeConversationsDatabase(activity);
		Editor editor = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();

		try
		{
			AccountUtils.deleteAccount();
			HikeMessengerApp app = (HikeMessengerApp) activity.getApplicationContext();
			app.disconnectFromService();
			activity.stopService(new Intent(activity, HikeService.class));
			db.deleteAll();
			convDb.deleteAll();
			editor.clear();
			return true;
		}
		catch (Exception e) {
			Log.e("DeleteAccountTask", "error deleting account", e);
			return false;
		}
		finally
		{
			db.close();
			convDb.close();
			editor.commit();
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

			Intent intent = new Intent(activity, WelcomeActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.startActivity(intent);
			activity.finish();
		}
		else
		{
			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText(activity, activity.getResources().getString(R.string.delete_account_failed), duration);
			toast.show();
		}
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.activity = activity;
	}

	@Override
	public boolean isFinished()
	{
		return finished;
	}


}
