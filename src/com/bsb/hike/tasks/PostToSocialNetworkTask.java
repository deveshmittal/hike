package com.bsb.hike.tasks;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.modules.httpmgr.HttpRequests;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;

public class PostToSocialNetworkTask
{
	private boolean facebook;

	private Context applicationCtx;

	public PostToSocialNetworkTask(boolean facebook)
	{
		this.facebook = facebook;
		this.applicationCtx = HikeMessengerApp.getInstance().getApplicationContext();
	}

	public void execute()
	{
		JSONObject data = new JSONObject();
		try
		{
			data.put(facebook ? HikeConstants.FACEBOOK_STATUS : HikeConstants.TWITTER_STATUS, true);
			Logger.d(getClass().getSimpleName(), "JSON: " + data);
			RequestToken token = HttpRequests.postToSocialNetworkRequest(data, getRequestListener());
			token.execute();
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
	}

	public IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();
				HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_POSTING_DIALOG, null);
				parseResponse(response, facebook);
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.DISMISS_POSTING_DIALOG, null);
				Toast.makeText(applicationCtx, R.string.posting_update_fail, Toast.LENGTH_SHORT).show();
			}
		};
	}

	private void parseResponse(JSONObject response, boolean facebook)
	{
		String responseString = response.optString(facebook ? HikeConstants.FACEBOOK_STATUS : HikeConstants.TWITTER_STATUS);

		if (TextUtils.isEmpty(responseString))
		{
			return;
		}

		if (HikeConstants.SocialPostResponse.SUCCESS.equals(responseString))
		{
			Toast.makeText(applicationCtx, R.string.posted_update, Toast.LENGTH_SHORT).show();
		}
		else if (HikeConstants.SocialPostResponse.FAILURE.equals(responseString))
		{
			Toast.makeText(applicationCtx, R.string.posting_update_fail, Toast.LENGTH_SHORT).show();
		}
		else
		{
			Editor editor = applicationCtx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE).edit();
			if (facebook)
			{
				editor.remove(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE);
				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN);
				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES);
				editor.remove(HikeMessengerApp.FACEBOOK_USER_ID);
			}
			else
			{
				editor.remove(HikeMessengerApp.TWITTER_AUTH_COMPLETE);
				editor.remove(HikeMessengerApp.TWITTER_TOKEN);
				editor.remove(HikeMessengerApp.TWITTER_TOKEN_SECRET);
			}
			editor.commit();
		}
	}
}
