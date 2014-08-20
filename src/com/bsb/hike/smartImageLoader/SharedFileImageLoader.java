package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.HikeFile.HikeFileType;

public class SharedFileImageLoader extends ImageWorker
{
	private int size_image;

	private Context context;

	public SharedFileImageLoader(Context context, int size_image)
	{
		this.context = context;
		this.size_image = size_image;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		String[] dataArray = data.split("::");
		String filePath = dataArray[0];
		HikeFileType hikeFileType = HikeFileType.values()[Integer.valueOf(dataArray[1])];

		BitmapDrawable bd = HikeMessengerApp.getLruCache().getSharedMediaThumbnailFromCache(data, filePath, size_image, (hikeFileType == HikeFileType.IMAGE));
		if (bd != null)
			return bd.getBitmap();

		return null;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}

}
