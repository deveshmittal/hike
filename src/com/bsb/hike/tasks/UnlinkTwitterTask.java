package com.bsb.hike.tasks;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.modules.httpmgr.HttpRequests;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class UnlinkTwitterTask implements ActivityCallableTask
{

	private HikePreferences activity;

	private boolean finished;

	private Context ctx;

	public UnlinkTwitterTask(HikePreferences activity, Context context)
	{
		this.activity = activity;
		this.ctx = context;
	}

	public void execute()
	{
		IRequestListener requestListener = new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject json = (JSONObject) result.getBody().getContent();
				if (!Utils.isResponseValid(json))
				{
					doOnFailure();
					return;
				}
				doOnSuccess();
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.e("UnlinkTwitterTask", "error unlinking account", httpException);
				doOnFailure();
			}
		};

		RequestToken requestToken = HttpRequests.deleteSocialCreadentialsRequest("twitter", requestListener);
		requestToken.execute();
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

	private void doOnSuccess()
	{
		Editor editor = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false);
		editor.putString(HikeMessengerApp.TWITTER_TOKEN, "");
		editor.putString(HikeMessengerApp.TWITTER_TOKEN_SECRET, "");
		editor.commit();

		finished = true;
		activity.dismissProgressDialog();

		Toast.makeText(activity, R.string.social_unlink_success, Toast.LENGTH_SHORT).show();
		activity.getPreferenceScreen().removePreference(activity.getPreferenceScreen().findPreference(HikeConstants.UNLINK_TWITTER));
	}

	private void doOnFailure()
	{
		finished = true;
		activity.dismissProgressDialog();
		Toast.makeText(activity, R.string.unlink_account_failed, Toast.LENGTH_SHORT).show();
	}
}
