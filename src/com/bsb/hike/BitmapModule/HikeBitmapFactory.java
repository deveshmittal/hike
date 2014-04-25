package com.bsb.hike.BitmapModule;

import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HikeBitmapFactory
{
	private static final String TAG = "HikeBitmapFactory";

	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap)
	{
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = 4;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	public static Bitmap getCircularBitmap(Bitmap bitmap)
	{
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}

	public static BitmapDrawable stringToDrawable(String encodedString)
	{
		if (TextUtils.isEmpty(encodedString))
		{
			return null;
		}
		byte[] thumbnailBytes = Base64.decode(encodedString, Base64.DEFAULT);
		return new BitmapDrawable(BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length));
	}

	public static Bitmap scaleDownImage(String filePath, int dimensionLimit, boolean makeSquareThumbnail)
	{
		Bitmap thumbnail = null;

		int currentWidth = 0;
		int currentHeight = 0;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(filePath, options);
		currentHeight = options.outHeight;
		currentWidth = options.outWidth;

		if (dimensionLimit == -1)
		{
			dimensionLimit = (int) (0.75 * (currentHeight > currentWidth ? currentHeight : currentWidth));
		}

		options.inSampleSize = Math.round((currentHeight > currentWidth ? currentHeight : currentWidth) / (dimensionLimit));
		options.inJustDecodeBounds = false;

		thumbnail = BitmapFactory.decodeFile(filePath, options);
		/*
		 * Should only happen when the external storage does not have enough free space
		 */
		if (thumbnail == null)
		{
			return null;
		}
		if (makeSquareThumbnail)
		{
			return makeSquareThumbnail(thumbnail);
		}

		return thumbnail;
	}

	public static Bitmap scaleDownImage(String filePath, int dimensionLimit, boolean makeSquareThumbnail, boolean applyBitmapConfig)
	{
		Bitmap thumbnail = null;

		int currentWidth = 0;
		int currentHeight = 0;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeFile(filePath, options);
		currentHeight = options.outHeight;
		currentWidth = options.outWidth;

		if (dimensionLimit == -1)
		{
			dimensionLimit = (int) (0.75 * (currentHeight > currentWidth ? currentHeight : currentWidth));
		}

		options.inSampleSize = Math.round((currentHeight > currentWidth ? currentHeight : currentWidth) / (dimensionLimit));
		options.inJustDecodeBounds = false;
		if (applyBitmapConfig)
		{
			options.inPreferredConfig = Config.RGB_565;
		}

		thumbnail = BitmapFactory.decodeFile(filePath, options);
		/*
		 * Should only happen when the external storage does not have enough free space
		 */
		if (thumbnail == null)
		{
			return null;
		}
		if (makeSquareThumbnail)
		{
			return makeSquareThumbnail(thumbnail);
		}

		return thumbnail;
	}

	public static Bitmap makeSquareThumbnail(Bitmap thumbnail)
	{
		int dimensionLimit = thumbnail.getWidth() < thumbnail.getHeight() ? thumbnail.getWidth() : thumbnail.getHeight();

		int startX = thumbnail.getWidth() > dimensionLimit ? (int) ((thumbnail.getWidth() - dimensionLimit) / 2) : 0;
		int startY = thumbnail.getHeight() > dimensionLimit ? (int) ((thumbnail.getHeight() - dimensionLimit) / 2) : 0;

		Logger.d("Utils", "StartX: " + startX + " StartY: " + startY + " WIDTH: " + thumbnail.getWidth() + " Height: " + thumbnail.getHeight());
		Logger.d("Utils", "dimensionLimit : " + dimensionLimit);
		Bitmap squareThumbnail = Bitmap.createBitmap(thumbnail, startX, startY, startX + dimensionLimit, startY + dimensionLimit);

		if (squareThumbnail != thumbnail)
		{
			thumbnail.recycle();
		}
		thumbnail = null;
		return squareThumbnail;
	}

	public static Bitmap stringToBitmap(String thumbnailString)
	{
		byte[] encodeByte = Base64.decode(thumbnailString, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
	}

	public static Bitmap drawableToBitmap(Drawable drawable)
	{
		if (drawable instanceof BitmapDrawable)
		{
			return ((BitmapDrawable) drawable).getBitmap();
		}
		/*
		 * http://developer.android.com/reference/android/graphics/Bitmap.Config. html
		 */
		Bitmap bitmap = Bitmap.createBitmap((int) (48 * Utils.densityMultiplier), (int) (48 * Utils.densityMultiplier), Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static Bitmap rotateBitmap(Bitmap b, int degrees)
	{
		if (degrees != 0 && b != null)
		{
			Matrix m = new Matrix();
			m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
			try
			{
				Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
				if (b != b2)
				{
					b.recycle();
					b = b2;
				}
			}
			catch (OutOfMemoryError e)
			{
				Logger.e("Utils", "Out of memory", e);
			}
		}
		return b;
	}

	public static Bitmap returnScaledBitmap(Bitmap src, Context context)
	{
		Resources res = context.getResources();
		if (Utils.isHoneycombOrHigher())
		{
			int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
			int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
			return src = Bitmap.createScaledBitmap(src, width, height, false);
		}
		else
			return src;

	}

	public static Drawable getDefaultIconForUser(Context context, String msisdn)
	{
		return getDefaultIconForUser(context, msisdn, false);
	}

	public static Drawable getDefaultIconForUser(Context context, String msisdn, boolean rounded)
	{
		return context.getResources().getDrawable(BitmapUtils.getDefaultAvatarResourceId(msisdn, rounded));
	}

	public static BitmapDrawable getDefaultIconForUserFromDecodingRes(Context context, String msisdn)
	{
		return getDefaultIconForUserFromDecodingRes(context, msisdn, false);
	}

	public static BitmapDrawable getDefaultIconForUserFromDecodingRes(Context context, String msisdn, boolean rounded)
	{
		return getBitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), BitmapUtils.getDefaultAvatarResourceId(msisdn, rounded)));
	}

	public static BitmapDrawable getBitmapDrawable(Resources mResources, final Bitmap bitmap)
	{
		if (bitmap == null)
			return null;

		if (Utils.isHoneycombOrHigher())
		{
			// Running on Honeycomb or newer, so wrap in a standard BitmapDrawable
			return new BitmapDrawable(mResources, bitmap);
		}
		else
		{
			// Running on Gingerbread or older, so wrap in a RecyclingBitmapDrawable
			// which will recycle automagically
			return new RecyclingBitmapDrawable(mResources, bitmap);
		}
	}

	/**
	 * Decode and sample down a bitmap from resources to the requested width and height.
	 * 
	 * @param res
	 *            The resources object containing the image data
	 * @param resId
	 *            The resource id of the image data
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight)
	{
		return decodeSampledBitmapFromResource(res, resId, reqWidth, reqHeight, Bitmap.Config.ARGB_8888);
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight, Bitmap.Config con)
	{

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		options.inPreferredConfig = con;
		if (con == Bitmap.Config.RGB_565)
			options.inDither = true;

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		// if (Utils.hasHoneycomb())
		// {
		// addInBitmapOptions(options, cache);
		// }

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = BitmapFactory.decodeResource(res, resId, options);
		}
		catch (IllegalArgumentException e)
		{
			result = BitmapFactory.decodeResource(res, resId);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from resources: ", e);
		}
		return result;
	}

	/**
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 * 
	 * @param filename
	 *            The full path of the file to decode
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight)
	{
		return decodeSampledBitmapFromFile(filename, reqWidth, reqHeight, Bitmap.Config.ARGB_8888);
	}

	public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight, Bitmap.Config con)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		options.inPreferredConfig = con;
		if (con == Bitmap.Config.RGB_565)
			options.inDither = true;

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		// if (Utils.hasHoneycomb())
		// {
		// addInBitmapOptions(options, cache);
		// }

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = BitmapFactory.decodeFile(filename, options);
		}
		catch (IllegalArgumentException e)
		{
			result = BitmapFactory.decodeFile(filename, options);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from file: ", e);
		}
		return result;
	}

	/**
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 * 
	 * @param filename
	 *            The full path of the file to decode
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @param cache
	 *            The ImageCache used to find candidate bitmaps for use with inBitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromByteArray(String msisdn, boolean rounded, int reqWidth, int reqHeight)
	{
		return decodeSampledBitmapFromByteArray(msisdn, rounded, reqWidth, reqHeight, Bitmap.Config.ARGB_8888);
	}

	public static Bitmap decodeSampledBitmapFromByteArray(String msisdn, boolean rounded, int reqWidth, int reqHeight, Bitmap.Config con)
	{
		byte[] icondata = HikeUserDatabase.getInstance().getIconByteArray(msisdn, rounded);
		if (icondata == null)
			return null;
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		BitmapFactory.decodeByteArray(icondata, 0, icondata.length, options);

		options.inPreferredConfig = con;
		if (con == Bitmap.Config.RGB_565)
			options.inDither = true;

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// If we're running on Honeycomb or newer, try to use inBitmap
		// if (Utils.hasHoneycomb())
		// {
		// addInBitmapOptions(options, cache);
		// }

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = BitmapFactory.decodeByteArray(icondata, 0, icondata.length, options);
		}
		catch (IllegalArgumentException e)
		{
			result = BitmapFactory.decodeByteArray(icondata, 0, icondata.length);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from ByteArray: ", e);
		}
		return result;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected static void addInBitmapOptions(BitmapFactory.Options options)
	{
		// inBitmap only works with mutable bitmaps so force the decoder to
		// return mutable bitmaps.
		options.inMutable = true;
		HikeLruCache cache = HikeMessengerApp.getLruCache();
		if (cache != null)
		{
			// Try and find a bitmap to use for inBitmap
			Bitmap inBitmap = cache.getBitmapFromReusableSet(options);

			if (inBitmap != null)
			{
				Logger.d(TAG, "Found a bitmap in reusable set.");
				options.inBitmap = inBitmap;
			}
		}
	}

	/**
	 * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding bitmaps using the decode* methods from {@link BitmapFactory}. This implementation
	 * calculates the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap having a width and height equal to or larger than the requested width
	 * and height.
	 * 
	 * @param options
	 *            An options object with out* params already populated (run through a decode* method with inJustDecodeBounds==true
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @return The value to be used for inSampleSize
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth)
			{
				inSampleSize *= 2;
			}
			
		}
		return inSampleSize;
	}

	/**
	 * Decode and sample down a bitmap from resources to the requested inSampleSize.
	 * 
	 * @param res
	 *            The resources object containing the image data
	 * @param resId
	 *            The resource id of the image data
	 * @param inSampleSize
	 *            The value to be used for inSampleSize
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int inSampleSize)
	{

		final BitmapFactory.Options options = new BitmapFactory.Options();

		options.inSampleSize = inSampleSize;

		options.inJustDecodeBounds = false;
		Bitmap result = null;
		try
		{
			result = BitmapFactory.decodeResource(res, resId, options);
		}
		catch (IllegalArgumentException e)
		{
			result = BitmapFactory.decodeResource(res, resId);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in decoding Bitmap from resources: ", e);
		}
		return result;
	}

	public static Bitmap decodeFile(String path)
	{
		return BitmapFactory.decodeFile(path);
	}

	public static Bitmap decodeStream(InputStream is)
	{
		return BitmapFactory.decodeStream(is);
	}

	public static Bitmap decodeResource(Resources res, int id)
	{
		return BitmapFactory.decodeResource(res, id);
	}

	public static Bitmap decodeByteArray(byte[] data, int offset, int length)
	{
		return BitmapFactory.decodeByteArray(data, offset, length);
	}

	public static Bitmap scaleDownBitmap(String filename, int reqWidth, int reqHeight)
	{
		return scaleDownBitmap(filename, reqWidth, reqHeight, Bitmap.Config.ARGB_8888);
	}

	public static Bitmap scaleDownBitmap(String filename, int reqWidth, int reqHeight, Bitmap.Config config)
	{
		Bitmap unscaledBitmap = HikeBitmapFactory.decodeSampledBitmapFromFile(filename, reqWidth, reqHeight, config);

		Bitmap small = HikeBitmapFactory.createScaledBitmap(unscaledBitmap, reqWidth, reqHeight, config);

		return small;
	}

	public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int reqWidth, int reqHeight, Bitmap.Config config)
	{
		if (reqWidth < unscaledBitmap.getWidth() && reqHeight < unscaledBitmap.getHeight())
		{
			Rect srcRect = new Rect(0, 0, unscaledBitmap.getWidth(), unscaledBitmap.getHeight());

			Rect reqRect = calculateReqRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(), reqWidth, reqHeight);

			Bitmap scaledBitmap = Bitmap.createBitmap(reqRect.width(), reqRect.height(), config);

			Canvas canvas = new Canvas(scaledBitmap);
			canvas.drawBitmap(unscaledBitmap, srcRect, reqRect, new Paint(Paint.FILTER_BITMAP_FLAG));

			return scaledBitmap;
		}
		else
		{
			return unscaledBitmap;
		}
	}

	protected static Rect calculateReqRect(int srcWidth, int srcHeight, int reqWidth, int reqHeight)
	{
		final float srcAspect = (float) srcWidth / (float) srcHeight;
		final float dstAspect = (float) reqWidth / (float) reqHeight;

		if (srcAspect > dstAspect)
		{
			return new Rect(0, 0, (int) (reqHeight * srcAspect), reqHeight);
		}
		else
		{
			return new Rect(0, 0, reqWidth, (int) (reqWidth / srcAspect));
		}
	}

}
