package com.bsb.hike.tasks;

import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.bsb.hike.utils.AccountUtils;

public class UtilAtomicAsyncTask extends AsyncTask<HttpRequestBase, Void, String>
{

	private ProgressBar mProgressBar;

	private UtilAsyncTaskListener mListener;

	private Activity mActivity;

	private ProgressDialog progressDialog;

	public UtilAtomicAsyncTask(Activity argActivity, ProgressBar argProgressBar, UtilAsyncTaskListener listener)
	{
		mProgressBar = argProgressBar;
		mListener = listener;
		mActivity = argActivity;
	}

	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		if (mProgressBar != null)
		{
			mProgressBar.setVisibility(View.VISIBLE);
		}
		progressDialog = new ProgressDialog(mActivity);
		progressDialog.setCancelable(true);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setMessage("Please wait while we get you connected");
		progressDialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				UtilAtomicAsyncTask.this.onPostExecute(null);
				UtilAtomicAsyncTask.this.cancel(true);
			}
		});
		progressDialog.show();
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
			// We need to be absolutely sure that we are dismissing progress bar at some point
		}

		return jsonResponse == null ? null : jsonResponse.toString();
	}

	@Override
	protected void onPostExecute(String result)
	{
		super.onPostExecute(result);

		if (UtilAtomicAsyncTask.this.isCancelled())
		{
			return;
		}
		
		if (progressDialog != null)
		{
			progressDialog.dismiss();
		}

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

		mActivity = null;
	}

	public interface UtilAsyncTaskListener
	{
		void onComplete(String argResponse);

		void onFailed();
	}
}
