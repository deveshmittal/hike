package com.bsb.hike.tasks;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;

public class UnlinkTwitterTask extends AsyncTask<Void, Void, Boolean> implements ActivityCallableTask
{

	private HikePreferences activity;

	private boolean finished;

	private Context ctx;

	public UnlinkTwitterTask(HikePreferences activity, Context context)
	{
		this.activity = activity;
		this.ctx = context;
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		try
		{
			AccountUtils.deleteSocialCredentials(false);

			Editor editor = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false);
			editor.putString(HikeMessengerApp.TWITTER_TOKEN, "");
			editor.putString(HikeMessengerApp.TWITTER_TOKEN_SECRET, "");
			editor.commit();

			return true;
		}
		catch (Exception e)
		{
			Logger.e("UnlinkTwitterTask", "error unlinking account", e);
			return false;
		}

	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		finished = true;
		activity.dismissProgressDialog();

		if (result.booleanValue())
		{
			Toast.makeText(activity, R.string.social_unlink_success, Toast.LENGTH_SHORT).show();
			activity.getPreferenceScreen().removePreference(activity.getPreferenceScreen().findPreference(HikeConstants.UNLINK_TWITTER));
		}
		else
		{
			Toast.makeText(activity, R.string.unlink_account_failed, Toast.LENGTH_SHORT).show();
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
