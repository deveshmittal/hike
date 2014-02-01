package com.bsb.hike.smartImageLoader;

import com.bsb.hike.ui.utils.RecyclingBitmapDrawable;
import com.bsb.hike.utils.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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
		BitmapDrawable bd = null;
		if (Utils.hasHoneycomb())
		{
			bd = new BitmapDrawable(mResources,BitmapFactory.decodeFile(data));
		}
		else
			bd = new RecyclingBitmapDrawable(mResources,BitmapFactory.decodeFile(data));
		
		return bd.getBitmap();
	}

}
