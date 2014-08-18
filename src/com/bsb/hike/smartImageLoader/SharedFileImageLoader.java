package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;

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
		BitmapDrawable bd = HikeMessengerApp.getLruCache().getFileIconFromCache(data);
		if (bd != null)
			return bd.getBitmap();

		Bitmap b = HikeBitmapFactory.scaleDownBitmap(data, size_image, size_image, Bitmap.Config.RGB_565, true, false);
		return b;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}

}
