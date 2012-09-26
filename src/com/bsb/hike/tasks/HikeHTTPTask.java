package com.bsb.hike.tasks;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeFileTransferHttpRequest;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.utils.AccountUtils;

public class HikeHTTPTask extends AsyncTask<HikeHttpRequest, Integer, Boolean> implements ActivityCallableTask
{
	boolean finished;
	private FinishableEvent finishableEvent;
	private HikeHttpRequest[] requests;
	private int errorStringId;
	private int progressFileTransfer;
	private AtomicBoolean cancelUpload = new AtomicBoolean();

	public HikeHTTPTask(FinishableEvent activity, int errorStringId)
	{
		this.finishableEvent = activity;
		this.errorStringId = errorStringId;
	}

	public void cancelUpload()
	{
		cancelUpload.set(true);
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		finished = true;
		if(finishableEvent != null)
		{
			finishableEvent.onFinish(result.booleanValue());
		}
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
			errorStringId = cancelUpload.get() ? R.string.upload_cancelled : errorStringId;
			if(finishableEvent != null)
			{
				Toast toast = Toast.makeText((Activity) finishableEvent, errorStringId, duration);
				toast.show();
			}
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
				if(hikeHttpRequest instanceof HikeFileTransferHttpRequest)
				{
					hikeHttpRequest.setResponse(
							AccountUtils.executeFileTransferRequest(
									((HikeFileTransferHttpRequest)hikeHttpRequest), 
									((HikeFileTransferHttpRequest)hikeHttpRequest).getFileName(), 
									this, 
									cancelUpload, 
									((HikeFileTransferHttpRequest)hikeHttpRequest).getFileType()));
				}
				else
				{
					AccountUtils.performRequest(hikeHttpRequest);
				}
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

	public void updateProgress(int progress)
	{
		publishProgress(progress);
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) 
	{
		progressFileTransfer = values[0];
		Log.d(getClass().getSimpleName(), "Progress Percentage: " + progressFileTransfer);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	public int getProgressFileTransfer()
	{
		return progressFileTransfer;
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
