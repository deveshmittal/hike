package com.bsb.hike.smartImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Utils;

public class IconLoader extends ImageWorker
{

	private static final String TAG = "IconLoader";

	private int mImageWidth;

	private int mImageHeight;
	
	private Context context;

	/**
	 * Initialize providing a target image width and height for the processing images.
	 * 
	 * @param context
	 * @param imageWidth
	 * @param imageHeight
	 */
	public IconLoader(Context ctx, int imageWidth, int imageHeight)
	{
		super();
		this.context = ctx;
		this.mImageWidth = imageWidth;
		this.mImageHeight = imageHeight;
		mResources = this.context.getResources();
	}

	/**
	 * Initialize providing a single target image size (used for both width and height);
	 * 
	 * @param context
	 * @param imageSize
	 */
	public IconLoader(Context ctx, int imageSize)
	{
		this(ctx,imageSize,imageSize);
	}

	public void setImageSize(int width, int height)
	{
		mImageWidth = width;
		mImageHeight = height;
	}

	/**
	 * Set the target image size (width and height will be the same).
	 * 
	 * @param size
	 */
	public void setImageSize(int size)
	{
		setImageSize(size, size);
	}

	/**
	 * The main process method, which will be called by the ImageWorker in the AsyncTask background thread.
	 * 
	 * @param data
	 *            The data to load the bitmap
	 * @return The downloaded and resized bitmap
	 */
	protected Bitmap processBitmap(String id)
	{
		BitmapDrawable bd = this.getImageCache().get(id);
		if(bd != null)
			return bd.getBitmap();
		int idx = id.indexOf(ROUND_SUFFIX);
		boolean rounded = false;
		if(idx > 0)
		{
			id = id.substring(0,idx);
			rounded = true;
		}
		Bitmap bm = decodeSampledBitmapFromByeArray(id,rounded,mImageWidth,mImageHeight,HikeMessengerApp.getLruCache());
		if(bm == null)
		{
			return decodeSampledBitmapFromResource(mResources, Utils.getId(id, rounded), mImageWidth, mImageHeight, HikeMessengerApp.getLruCache());
		}
		else	
			return bm;
	}

	@Override
	protected Bitmap processBitmapOnUiThread(String id)
	{
		return processBitmap(id);
	}
}
