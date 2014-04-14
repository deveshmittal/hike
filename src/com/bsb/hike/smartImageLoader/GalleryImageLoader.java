package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore.Images.Thumbnails;

public class GalleryImageLoader extends ImageWorker
{
	public static final String GALLERY_KEY_PREFIX = "gal:";

	private Context context;

	public GalleryImageLoader(Context context)
	{
		this.context = context;
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
		
		long id = Long.valueOf(data.substring(GALLERY_KEY_PREFIX.length()));

		return Thumbnails.getThumbnail(context.getContentResolver(), id, Thumbnails.MINI_KIND, null);
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		return processBitmap(data);
	}

}
