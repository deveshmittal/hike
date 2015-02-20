package com.bsb.hike.tasks;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.hikehttp.IHikeHTTPTask;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.utils.Logger;

public class SendTwitterInviteTask implements IHikeHTTPTask
{
	private Set<String> selectedFriends;

	private RequestToken token;

	private Context applicationCtx;

	public SendTwitterInviteTask(Set<String> selectedFriends)
	{
		this.selectedFriends = selectedFriends;
		this.applicationCtx = HikeMessengerApp.getInstance().getApplicationContext();
	}

	@Override
	public void execute()
	{
		JSONArray inviteesArray = new JSONArray();
		try
		{
			for (String id : selectedFriends)
			{
				inviteesArray.put(id);
			}
			sendTwitterInvite(new JSONObject().put("invitees", inviteesArray));
		}
		catch (JSONException e)
		{
			Logger.e("SocialNetInviteActivity", "Creating a JSONObject payload for http Twitter Invite request", e);
		}

	}

	public void sendTwitterInvite(JSONObject data)
	{
		token = HttpRequests.sendTwitterInviteRequest(data, getRequestListener());
		token.execute();
		return;
	}

	@Override
	public void cancel()
	{
		token.cancel();
	}

	public IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{
			@Override
			public void onRequestSuccess(com.bsb.hike.modules.httpmgr.response.Response result)
			{
				Toast.makeText(applicationCtx, R.string.posted_update, Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Toast.makeText(applicationCtx, R.string.posting_update_fail, Toast.LENGTH_SHORT).show();
			}
		};
	}

}
