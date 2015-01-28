package com.bsb.hike.platform.content;

import android.util.Log;
import com.bsb.hike.HikeMessengerApp;

import java.io.File;

public class PlatformContent
{

	private static byte isInitialized = 1;

	private PlatformContent()
	{
		// Classic singleton
	}

	public static enum ErrorCode
	{
		INVALID_DATA, LOW_CONNECTIVITY, STORAGE_FULL, UNKNOWN, DOWNLOADING
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
		if (isInitialized == 0)
		{
			PlatformContentConstants.PLATFORM_CONTENT_DIR = HikeMessengerApp.getInstance().getApplicationContext().getFilesDir() + File.separator
					+ PlatformContentConstants.CONTENT_DIR_NAME + File.separator;
			isInitialized = 1;
		}

		PlatformContentRequest request = PlatformContentRequest.make(PlatformContentModel.make(contentData), listener);
		if (request != null)
		{
			PlatformContentLoader.getLoader().handleRequest(request);
			return request;
		}
		else
		{
			Log.e("PlatformContent", "Incorrect content data");
			listener.onFailure(ErrorCode.INVALID_DATA);
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
