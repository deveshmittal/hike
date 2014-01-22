package com.bsb.hike.smartImageLoader;

import com.bsb.hike.db.HikeUserDatabase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class IconLoader extends ImageWorker
{

	private static final String TAG = "IconLoader";

	private int mImageWidth;

	private int mImageHeight;
	
	private HikeUserDatabase mDb;
	
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
		mDb = HikeUserDatabase.getInstance();
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
		int idx = id.indexOf(ROUND_SUFFIX);
		boolean rounded = true;
		if(idx > 0)
		{
			id = id.substring(0,idx);
			rounded = true;
		}
		BitmapDrawable b = (BitmapDrawable)mDb.getIcon(id, rounded);
		return b.getBitmap();
	}
}
