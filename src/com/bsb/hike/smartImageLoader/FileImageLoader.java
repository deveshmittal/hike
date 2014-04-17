package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;

import com.bsb.hike.HikeMessengerApp;

public class FileImageLoader extends ImageWorker
{
	public static final String FILE_KEY_PREFIX = "fil:";

	private int reqWidth;

	private int reqHeight;

	public FileImageLoader(int reqWidth, int reqHeight)
	{
		this.reqHeight = reqHeight;
		this.reqWidth = reqWidth;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		String filePath = new String(data.substring(FILE_KEY_PREFIX.length()));

		return decodeSampledBitmapFromFile(filePath, reqWidth, reqHeight);
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return null;
	}

}
