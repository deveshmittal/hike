package com.bsb.hike.platform.content;

import android.util.Log;

public class PlatformContent
{
	private PlatformContent()
	{
		// Classic singleton
	}

	/**
	 * Gets well formed HTML content.
	 * 
	 * @param contentData
	 *            the content data
	 * @param listener
	 *            the listener
	 * @return new request made, use this for cancelling requests
	 * 
	 * @return the content
	 */
	public static PlatformContentRequest getContent(String contentData, PlatformContentListener<PlatformContentModel> listener)
	{
		PlatformContentRequest request = PlatformContentRequest.make(PlatformContentModel.make(contentData), listener);
		if (request != null)
		{
			PlatformContentLoader.getLoader().handleRequest(request);
			return request;
		}
		else
		{
			Log.e("PlatformContent", "Incorrect content data");
			return null;
		}

	}

	public static String getForwardCardData(String contentData)
	{
		return PlatformContentModel.getForwardData(contentData);
	}

	public static boolean cancelRequest(PlatformContentRequest argRequest)
	{
		return PlatformRequestManager.remove(argRequest);
	}

	public static void cancelAllRequests()
	{
		PlatformRequestManager.removeAll();
	}

}
