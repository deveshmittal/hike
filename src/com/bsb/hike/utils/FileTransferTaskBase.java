package com.bsb.hike.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import android.os.AsyncTask;

import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

public abstract class FileTransferTaskBase extends AsyncTask<Void, Integer, FTResult>
{
	private int progress;
	public AtomicBoolean cancelTask;

	public void updateProgress(int progress)
	{
		this.progress = progress;
		HikeMessengerApp.getPubSub().publish(HikePubSub.FILE_TRANSFER_PROGRESS_UPDATED, null);
	}

	public int getProgress()
	{
		return progress;
	}

	public void cancelTask()
	{
		this.cancelTask.set(true);
	}
}