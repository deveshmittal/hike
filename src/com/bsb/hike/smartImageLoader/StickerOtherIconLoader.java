package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.StickerManager;

public class StickerOtherIconLoader extends ImageWorker
{
	private Context ctx;
	private boolean downloadIfNotFound;
	
	/**
	 * 
	 * @param ctx
	 * @param downloadIfNotFound -- true if preview image should be downloaded if not found
	 */
	public StickerOtherIconLoader(Context ctx, boolean downloadIfNotFound)
	{
		super();
		this.ctx = ctx;
		mResources = ctx.getResources();
		this.downloadIfNotFound = downloadIfNotFound;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{	
		String[] args = data.split(HikeConstants.DELIMETER);
		String categoryId = args[0];
		int type = Integer.valueOf(args[1]);
		Bitmap bmp = StickerManager.getInstance().getCategoryOtherAsset(ctx, categoryId, type, downloadIfNotFound);
		return bmp;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return processBitmap(data);
	}

}
