package com.bsb.hike.smartImageLoader;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.utils.Utils;

import android.graphics.Bitmap;

public class HighQualityThumbLoader extends ImageWorker
{

	public HighQualityThumbLoader()
	{
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected Bitmap processBitmap(String data)
	{
		Bitmap thumbnail = null;
		thumbnail = HikeBitmapFactory.scaleDownBitmap(data, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, Bitmap.Config.RGB_565, false,
				false);
		thumbnail = Utils.getRotatedBitmap(data, thumbnail);
		return thumbnail;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}

	
}
