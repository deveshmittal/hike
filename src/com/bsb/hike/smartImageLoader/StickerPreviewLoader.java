package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.StickerManager;

public class StickerPreviewLoader extends ImageWorker
{
	private Context ctx;
	private boolean downloadIfNotFound;
	
	/**
	 * 
	 * @param ctx
	 * @param downloadIfNotFound -- true if preview image should be downloaded if not found
	 */
	public StickerPreviewLoader(Context ctx, boolean downloadIfNotFound)
	{
		super();
		this.ctx = ctx;
		this.downloadIfNotFound = downloadIfNotFound;
	}

	@Override
	protected Bitmap processBitmap(String data)
	{	
		String categoryId = data.split(HikeConstants.DELIMETER)[0];
		Bitmap bmp = StickerManager.getInstance().getCategoryPreviewAsset(ctx, categoryId, downloadIfNotFound);
		return bmp;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
