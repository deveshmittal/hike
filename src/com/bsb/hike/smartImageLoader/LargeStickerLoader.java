package com.bsb.hike.smartImageLoader;

import com.bsb.hike.ui.utils.RecyclingBitmapDrawable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

public class LargeStickerLoader extends ImageWorker
{
	private Context ctx;
	public LargeStickerLoader(Context context)
	{
		super();
		ctx = context;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{
		RecyclingBitmapDrawable b = new RecyclingBitmapDrawable(ctx.getResources(),BitmapFactory.decodeFile(data));
		return b.getBitmap();
	}

}
