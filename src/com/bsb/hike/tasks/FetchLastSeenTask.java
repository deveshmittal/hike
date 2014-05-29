package com.bsb.hike.tasks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FetchLastSeenTask extends AsyncTask<Void, Void, Long>
{
	public static interface FetchLastSeenCallback
	{
		public void lastSeenFetched(String msisdn, int offline, long lastSeenTime);

		public void lastSeenNotFetched();
	}

	String msisdn;

	Context context;

	FetchLastSeenCallback fetchLastSeenCallback;

	public FetchLastSeenTask(Context context, String msisdn, FetchLastSeenCallback fetchLastSeenCallback)
	{
		/*
		 * We only need the application context in this case
		 */
		this.context = context.getApplicationContext();

		this.msisdn = msisdn;

		this.fetchLastSeenCallback = fetchLastSeenCallback;
	}

	@Override
	protected Long doInBackground(Void... params)
	{
		URL url;
		try
		{
			url = new URL(AccountUtils.base + "/user/lastseen/" + msisdn);

			Logger.d(getClass().getSimpleName(), "URL:  " + url);

			URLConnection connection = url.openConnection();
			AccountUtils.addUserAgent(connection);
			connection.addRequestProperty("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

			if (AccountUtils.ssl)
			{
				((HttpsURLConnection) connection).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
			}

			JSONObject response = AccountUtils.getResponse(connection.getInputStream());
			Logger.d(getClass().getSimpleName(), "Response: " + response);
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
			{
				return null;
			}
			JSONObject data = response.getJSONObject(HikeConstants.DATA);
			return data.getLong(HikeConstants.LAST_SEEN);

		}
		catch (MalformedURLException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return null;
		}
		catch (IOException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return null;
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return null;
		}

	}

	@Override
	protected void onPostExecute(Long result)
	{
		if (result == null)
		{
			fetchLastSeenCallback.lastSeenNotFetched();
		}
		else
		{
			int offline;
			long lastSeenValue;

			/*
			 * Update current last seen value.
			 */
			long currentLastSeenValue = result;
			/*
			 * We only apply the offset if the value is greater than 0 since 0 and -1 are reserved.
			 */
			if (currentLastSeenValue > 0)
			{
				offline = 1;
				lastSeenValue = Utils.applyServerTimeOffset(context, currentLastSeenValue);
			}
			else
			{
				offline = (int) currentLastSeenValue;
				lastSeenValue = System.currentTimeMillis() / 1000;
			}

			HikeUserDatabase.getInstance().updateLastSeenTime(msisdn, lastSeenValue);
			HikeUserDatabase.getInstance().updateIsOffline(msisdn, offline);

			fetchLastSeenCallback.lastSeenFetched(msisdn, offline, lastSeenValue);
		}
	}
}