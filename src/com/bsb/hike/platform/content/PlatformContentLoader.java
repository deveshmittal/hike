package com.bsb.hike.platform.content;

import android.os.Handler;
import android.util.Log;

import com.bsb.hike.models.HikeHandlerUtil;
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

				PlatformRequestManager.completeRequest(argContentRequest);
			}
			else
			{
				// TODO Handle
			}
		}
		else
		{
			// Fetch from remote
			getTemplateFromRemote(argContentRequest);
		}
	}

	private void getTemplateFromRemote(PlatformContentRequest argContentRequest)
	{
		Log.d(TAG, "fetching template from remote");

		PlatformRequestManager.setWaitState(argContentRequest);

		new PlatformTemplateDownloadTask(argContentRequest).execute();
	}
}
