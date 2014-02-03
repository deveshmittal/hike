package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class StickerLoader extends ImageWorker
{
	private Context ctx;

	public StickerLoader(Context ctx)
	{
		super();
		this.ctx = ctx;
		mResources = ctx.getResources();
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		if (data.contains("res:"))
		{
			int id = Integer.parseInt(data.substring(data.indexOf(":") + 1));
			return BitmapFactory.decodeResource(mResources, id);
		}
		else
		{
			return BitmapFactory.decodeFile(data);
		}
	}

}
