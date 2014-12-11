package com.bsb.hike.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.entity.AbstractHttpEntity;

import android.os.AsyncTask;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.http.CustomByteArrayEntity;
import com.bsb.hike.http.CustomFileEntity;

public abstract class FileTransferTaskBase extends AsyncTask<Void, Integer, FTResult>
{
	private int progress;

	public AtomicBoolean cancelTask = new AtomicBoolean(false);

	public AbstractHttpEntity entity;

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
		cancelTask.set(true);
		if (entity != null)
		{
			if (entity instanceof CustomFileEntity)
			{
				((CustomFileEntity) entity).cancelDownload();
			}
			else if (entity instanceof CustomByteArrayEntity)
			{
				((CustomByteArrayEntity) entity).cancelDownload();
			}
		}
	}

	public void setEntity(AbstractHttpEntity entity)
	{
		this.entity = entity;
	}
}