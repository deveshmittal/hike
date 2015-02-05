package com.bsb.hike.platform.content;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.bsb.hike.models.HikeHandlerUtil;
import com.bsb.hike.platform.content.PlatformContent.ErrorCode;
import com.samskivert.mustache.Template;

/**
 * This class is responsible for handling content requests. Directly communicates with cache, template engine and download task.
 */
class PlatformContentLoader extends Handler
{
	private static String TAG = "PlatformContentLoader";

	private static PlatformContentLoader mLoader = new PlatformContentLoader();

	/**
	 * Instantiates a new platform content loader.
	 */
	private PlatformContentLoader()
	{
		super(HikeHandlerUtil.getInstance().getLooper());
	}

	/**
	 * Gets the loader.
	 * 
	 * @return the loader
	 */
	public static PlatformContentLoader getLoader()
	{
		return mLoader;
	}

	/**
	 * Handle request.
	 * 
	 * @param platformContentModel
	 *            the platform content model
	 * @param listener
	 *            the listener
	 */
	public void handleRequest(final PlatformContentRequest argContentRequest)
	{
		Log.d(TAG, "handling request");

		PlatformContentModel formedContent = PlatformContentCache.getFormedContent(argContentRequest);

		if (formedContent != null)
		{
			Log.d(TAG, "found formed content");
			argContentRequest.getListener().onComplete(formedContent);
			return;
		}
		else
		{
			Log.d(TAG, "formed content not found");
			PlatformRequestManager.addRequest(argContentRequest);
		}
	}

	public void loadData(PlatformContentRequest argContentRequest)
	{
		// Get from template
		Template template = PlatformContentCache.getTemplate(argContentRequest);
		if (template != null)
		{
			Log.d(TAG, "found cached template");
			// Compile template
			if (PlatformTemplateEngine.execute(template, argContentRequest))
			{
				Log.d(TAG, "data binded");
				// Add to cache
				PlatformContentCache.putFormedContent(argContentRequest.getContentData());
				
				argContentRequest.getListener().onFailure(ErrorCode.LOADED);

				PlatformRequestManager.completeRequest(argContentRequest);
			}
			else
			{
				// Incorrect data. Could not execute. Remove request from queue.
				PlatformRequestManager.reportFailure(argContentRequest, PlatformContent.ErrorCode.INVALID_DATA);
				PlatformRequestManager.remove(argContentRequest);
			}
		}
		else
		{
			if (argContentRequest.getState() != PlatformContentRequest.STATE_CANCELLED)
			{
				// Fetch from remote
				getTemplateFromRemote(argContentRequest);
			}
		}
	}

	@SuppressLint("NewApi")
	private void getTemplateFromRemote(PlatformContentRequest argContentRequest)
	{
		PlatformRequestManager.setWaitState(argContentRequest);

		// Check if this is already being downloaded
		ArrayList<Integer> currentDownloadingTemplates = PlatformRequestManager.getCurrentDownloadingTemplates();

		for (Integer downloadingTemplateCode : currentDownloadingTemplates)
		{
			if (downloadingTemplateCode.compareTo(argContentRequest.getContentData().appHashCode()) == 0)
			{
				return;
			}
		}

		Log.d(TAG, "fetching template from remote");

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1)
		{
			new PlatformTemplateDownloadTask(argContentRequest).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[]) null);
		}
		else
		{
			new PlatformTemplateDownloadTask(argContentRequest).execute();
		}
		
		PlatformRequestManager.getCurrentDownloadingTemplates().add(argContentRequest.getContentData().appHashCode());
	}
}
