package com.bsb.hike.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.utils.AccountUtils;

public class HikeHTTPTask extends AsyncTask<HikeHttpRequest, Void, Boolean> implements ActivityCallableTask
{
	boolean finished;
	private FinishableEvent finishableEvent;
	private HikeHttpRequest[] requests;
	private int errorStringId;

	public HikeHTTPTask(FinishableEvent activity, int errorStringId)
	{
		this.finishableEvent = activity;
		this.errorStringId = errorStringId;
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		finished = true;
		finishableEvent.onFinish(result.booleanValue());
		if (result.booleanValue())
		{
			for (HikeHttpRequest request : requests)
			{
				request.onSuccess();
			}
		}
		else
		{

			for (HikeHttpRequest request : requests)
			{
				request.onFailure();
			}

			int duration = Toast.LENGTH_LONG;
			Toast toast = Toast.makeText((Activity) finishableEvent, ((Activity) finishableEvent).getResources().getString(errorStringId), duration);
			toast.show();
		}
	}

	@Override
	protected Boolean doInBackground(HikeHttpRequest... requests)
	{
		this.requests = requests;
		try
		{
			for (HikeHttpRequest hikeHttpRequest : requests)
			{
				Log.d("HikeHTTPTask", "About to perform request:" + hikeHttpRequest.getPath());
				AccountUtils.performRequest(hikeHttpRequest);
				Log.d("HikeHTTPTask", "Finished performing request:" + hikeHttpRequest.getPath());
			}
		}
		catch(Exception e)
		{
			Log.e("HikeHTTPTask", "Exception performing http task", e);
			return Boolean.FALSE;
		}

		return Boolean.TRUE;
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.finishableEvent = (FinishableEvent) activity;
	}

	@Override
	public boolean isFinished()
	{
		return finished;
	}

}
