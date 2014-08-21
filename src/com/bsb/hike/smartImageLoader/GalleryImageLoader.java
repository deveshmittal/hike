package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bsb.hike.BitmapModule.HikeBitmapFactory;

public class GalleryImageLoader extends ImageWorker
{
	public static final String GALLERY_KEY_PREFIX = "gal:";

	private int size_image;

	private Context context;

	public GalleryImageLoader(Context context , int size_image)
	{
		this.context = context;
		this.size_image = size_image;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		if (!data.startsWith(GALLERY_KEY_PREFIX))
		{
			return null;
		}
		BitmapDrawable bd = this.getImageCache().get(data);
		if (bd != null)
			return bd.getBitmap();

		String id = data.substring(GALLERY_KEY_PREFIX.length());

		Bitmap b = HikeBitmapFactory.scaleDownBitmap(id, size_image, size_image, Bitmap.Config.RGB_565, true, false);
		return b;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}

}
