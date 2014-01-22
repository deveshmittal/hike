package com.bsb.hike.smartImageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class StickerLoader extends ImageWorker
{

	public StickerLoader()
	{
		super();
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		return BitmapFactory.decodeFile(data);
	}

}
