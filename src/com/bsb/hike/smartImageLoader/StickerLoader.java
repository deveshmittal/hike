package com.bsb.hike.smartImageLoader;

import com.bsb.hike.ui.utils.RecyclingBitmapDrawable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

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
		Log.d("StickerLoader",mResources.toString());
		RecyclingBitmapDrawable b = new RecyclingBitmapDrawable(mResources,BitmapFactory.decodeFile(data));
		return b.getBitmap();
	}

}
