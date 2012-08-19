package com.bsb.hike.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bsb.hike.http.HikeFileTransferHttpRequest;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.AccountUtils;

public class HikeHTTPTask extends AsyncTask<HikeHttpRequest, Integer, Boolean> implements ActivityCallableTask
{
	boolean finished;
	private FinishableEvent finishableEvent;
	private HikeHttpRequest[] requests;
	private int errorStringId;
	private int progressFileTransfer;
	private ChatThread chatThread;

	public HikeHTTPTask(FinishableEvent activity, int errorStringId)
	{
		this.finishableEvent = activity;
		this.errorStringId = errorStringId;
		if(activity instanceof ChatThread)
		{
			this.chatThread = (ChatThread) activity;
		}
	}
	
	public void setChatThread(ChatThread activity)
	{
		this.chatThread = activity;
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
				if(hikeHttpRequest instanceof HikeFileTransferHttpRequest)
				{
					hikeHttpRequest.setResponse(AccountUtils.executeFileTransferRequest(hikeHttpRequest, ((HikeFileTransferHttpRequest)hikeHttpRequest).getFileName(), this));
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
		chatThread.mUpdateAdapter.run();
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
