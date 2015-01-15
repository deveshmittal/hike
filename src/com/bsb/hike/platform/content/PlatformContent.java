package com.bsb.hike.platform.content;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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
	public static PlatformContentRequest getContent(final String contentData, PlatformContentListener<PlatformContentModel> listener)
	{
		PlatformContentRequest request = PlatformContentRequest.make(PlatformContentModel.make(contentData), listener);
		PlatformContentLoader.getLoader().handleRequest(request);
		return request;
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
	public static PlatformContentRequest getForwardCardContent(final String contentData, PlatformContentListener<PlatformContentModel> listener)
	{
		PlatformContentRequest request = PlatformContentRequest.make(PlatformContentModel.makeForwardCardModel(contentData), listener);
		PlatformContentLoader.getLoader().handleRequest(request);
		return request;
	}

	public static boolean cancelRequest(PlatformContentRequest argRequest)
	{
		return PlatformRequestManager.remove(argRequest);
	}

	public static void cancelAllRequests()
	{
		PlatformRequestManager.removeAll();
	}

	protected static String readDataFromFile(File file)
	{
		
		Log.d("READING DATA FROM FILE: ", file.getAbsolutePath());
		// Read text from file
		StringBuilder text = new StringBuilder();

		try
		{
			BufferedReader br = new BufferedReader(new FileReader(file));

			String line;

			while ((line = br.readLine()) != null)
			{
				text.append(line);
			}

			br.close();
		}
		catch (FileNotFoundException fnfe)
		{
			// Template not found
			fnfe.printStackTrace();
		}
		catch (IOException e)
		{
			// Template not found
			e.printStackTrace();
		}
		
		return text.toString();
	}

}
