package com.bsb.hike.platform.content;

import android.util.Log;

import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

import java.io.File;

public class PlatformContent
{

	private static boolean isInitialized;

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
		if (!isInitialized)
		{
			if (Utils.getExternalStorageState() == ExternalStorageState.WRITEABLE)
			{
				PlatformContentConstants.PLATFORM_CONTENT_DIR = HikeMessengerApp.getInstance().getApplicationContext().getExternalFilesDir(null) + File.separator
						+ PlatformContentConstants.CONTENT_DIR_NAME + File.separator;
			}
			else
			{
				PlatformContentConstants.PLATFORM_CONTENT_DIR = HikeMessengerApp.getInstance().getApplicationContext().getFilesDir() + File.separator
						+ PlatformContentConstants.CONTENT_DIR_NAME + File.separator;
			}
			isInitialized = true;

			if (AppConfig.ALLOW_STAGING_TOGGLE)
			{
				// For testing purposes. We delete Content folder from saved location when hike messenger is re-started
				File contentDir = new File(PlatformContentConstants.PLATFORM_CONTENT_DIR);
				
//				if (contentDir != null)
//				{
//					try
//					{
//						Logger.d("PlatformContent", "Deleting old content");
//						Utils.deleteFile(contentDir); 
//					}
//					catch (NullPointerException npe)
//					{
//						npe.printStackTrace();
//					}
//				}
			}
		}

		Logger.d("PlatformContent", "Content Dir : " + PlatformContentConstants.PLATFORM_CONTENT_DIR);

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

	public static void cancelRequest(PlatformContentRequest argRequest)
	{
		PlatformRequestManager.remove(argRequest);
	}

	public static void cancelAllRequests()
	{
		PlatformRequestManager.removeAll();
	}

}
