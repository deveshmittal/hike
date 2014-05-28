package com.bsb.hike.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class FetchBulkLastSeenTask extends AsyncTask<Void, Void, Boolean>
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
		/*
		 * Not working in prod
		 */
		if (HikeSharedPreferenceUtil.getInstance(context).getData(HikeMessengerApp.PRODUCTION, true))
		{
			return true;
		}

		URL url;
		try
		{
			url = new URL("http://54.251.147.222:8181/v2/user/bls");

			Logger.d(getClass().getSimpleName(), "URL:  " + url);

			URLConnection connection = url.openConnection();

			Logger.d(getClass().getSimpleName(), "opened connection " + url);
			AccountUtils.addUserAgent(connection);
			connection.addRequestProperty("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

			if (AccountUtils.ssl)
			{
				((HttpsURLConnection) connection).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
			}

			Logger.d(getClass().getSimpleName(), "gettting is");
			InputStream is = connection.getInputStream();
			Logger.d(getClass().getSimpleName(), "got is");
			JSONObject response = AccountUtils.getResponse(is);
			Logger.d(getClass().getSimpleName(), "Response: " + response);
			if (response == null)
			{
				return false;
			}
			Utils.handleBulkLastSeenPacket(context, response);

			return true;

		}
		catch (MalformedURLException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return false;
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
