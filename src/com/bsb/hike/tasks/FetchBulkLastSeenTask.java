package com.bsb.hike.tasks;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FetchBulkLastSeenTask extends FetchLastSeenBase
{
	public static interface FetchBulkLastSeenCallback
	{
		public void bulkLastSeenFetched();

		public void bulkLastSeenNotFetched();
	}

	String msisdn;

	Context context;

	FetchBulkLastSeenCallback fetchBulkLastSeenCallback;

	public FetchBulkLastSeenTask(Context context, FetchBulkLastSeenCallback fetchBulkLastSeenCallback)
	{
		/*
		 * We only need the application context in this case
		 */
		this.context = context.getApplicationContext();

		this.fetchBulkLastSeenCallback = fetchBulkLastSeenCallback;
	}

	@Override
	protected Boolean doInBackground(Void... params)
	{
		try
		{
			JSONObject response = sendRequest(AccountUtils.baseV2 + "/user/bls");
			return saveResult(response);
		}
		catch (IOException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return false;
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return false;
		}
	}

	@Override
	public boolean saveResult(JSONObject response) throws JSONException
	{
		if (response == null)
		{
			return false;
		}
		Utils.handleBulkLastSeenPacket(context, response);
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		if (result == false)
		{
			fetchBulkLastSeenCallback.bulkLastSeenNotFetched();
		}
		else
		{
			fetchBulkLastSeenCallback.bulkLastSeenFetched();
		}
	}
}
