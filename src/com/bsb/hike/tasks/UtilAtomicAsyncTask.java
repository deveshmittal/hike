package com.bsb.hike.tasks;

import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONObject;

import com.bsb.hike.utils.AccountUtils;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

public class UtilAtomicAsyncTask extends AsyncTask<HttpRequestBase, Void, String>
{

	private ProgressBar mProgressBar;

	private UtilAsyncTaskListener mListener;

	public UtilAtomicAsyncTask(ProgressBar argProgressBar, UtilAsyncTaskListener listener)
	{
		mProgressBar = argProgressBar;
		mListener = listener;
	}

	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		if (mProgressBar != null)
		{
			mProgressBar.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected String doInBackground(HttpRequestBase... params)
	{
		// for now, assuming only one request is present in the request array

		HttpRequestBase httpRequest;

		if (params != null)
		{
			httpRequest = params[0];
		}
		else
		{
			return null;
		}

		JSONObject jsonResponse = null;
		try
		{
			jsonResponse = AccountUtils.executeRequest(httpRequest);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			onPostExecute(null);
			// We need to be absolutely sure that we are dismissing progress bar at some point
		}

		return jsonResponse == null ? null : jsonResponse.toString();
	}

	@Override
	protected void onPostExecute(String result)
	{
		super.onPostExecute(result);

		if (mProgressBar != null)
		{
			mProgressBar.setVisibility(View.GONE);
		}

		if (mListener != null)
		{
			if (result == null)
			{
				mListener.onFailed();
			}
			else
			{
				mListener.onComplete(result);
			}
		}
	}

	public interface UtilAsyncTaskListener
	{
		void onComplete(String argResponse);

		void onFailed();
	}
}
