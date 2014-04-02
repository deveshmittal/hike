package com.bsb.hike.smartcache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.support.v4.util.LruCache;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.smartcache.HikeLruCache.ImageCacheParams;
import com.bsb.hike.ui.utils.RecyclingBitmapDrawable;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.customClasses.MySoftReference;

public class HikeLruCache extends LruCache<String, BitmapDrawable>
{
	private static final String CHAT_THEME_PREFIX = "ct:";

	private static final String ORIENTATION_PORTRAIT_PREFIX = "port:";

	private static final String ORIENTATION_LANDSCAPE_PREFIX = "land:";

	// Default memory cache size in kilobytes
	private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 5; // 5MB

	// Compression settings when writing images to disk cache
	private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;

	private static final int DEFAULT_COMPRESS_QUALITY = 70;

	// Constants to easily toggle various caches
	private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
	
	private final static int MAX_CACHE_SIZE = 1024 * 15;

	private static HikeLruCache instance;

	protected Resources mResources;

	private Context context;

	/**
	 * A holder class that contains cache parameters.
	 */
	public static class ImageCacheParams
	{
		public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;

		public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;

		public int compressQuality = DEFAULT_COMPRESS_QUALITY;

		public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;

		/**
		 * Create a set of image cache parameters that can be provided to {@link ImageCache#getInstance(ImageCacheParams)}
		 */
		public ImageCacheParams(int memCacheSize)
		{
			this.memCacheSize = memCacheSize;
		}

		public ImageCacheParams()
		{

		}

		/**
		 * Sets the memory cache size based on a percentage of the max available VM memory. Eg. setting percent to 0.2 would set the memory cache to one fifth of the available
		 * memory. Throws {@link IllegalArgumentException} if percent is < 0.01 or > .8. memCacheSize is stored in kilobytes instead of bytes as this will eventually be passed to
		 * construct a LruCache which takes an int in its constructor.
		 * 
		 * This value should be chosen carefully based on a number of factors Refer to the corresponding Android Training class for more discussion:
		 * http://developer.android.com/training/displaying-bitmaps/
		 * 
		 * @param percent
		 *            Percent of available app memory to use to size memory cache
		 */
		public void setMemCacheSizePercent(float percent)
		{
			if (percent < 0.01f || percent > 0.8f)
			{
				throw new IllegalArgumentException("setMemCacheSizePercent - percent must be " + "between 0.01 and 0.8 (inclusive)");
			}
			memCacheSize = Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
			if(memCacheSize > MAX_CACHE_SIZE)
				memCacheSize = MAX_CACHE_SIZE;
		}
	}

	private final Set<MySoftReference<Bitmap>> reusableBitmaps;

	public HikeLruCache(ImageCacheParams cacheParams, Context context)
	{
		super(cacheParams.memCacheSize);
		reusableBitmaps = Utils.canInBitmap() ? Collections.synchronizedSet(new HashSet<MySoftReference<Bitmap>>()) : null;
		this.context = context;
		this.mResources = context.getResources();
	}

	public static HikeLruCache getInstance(ImageCacheParams cacheParams, Context context)
	{
		if (instance == null)
		{
			synchronized (HikeLruCache.class)
			{
				if (instance == null)
					instance = new HikeLruCache(cacheParams, context);
			}
		}
		return instance;
	}

	/**
	 * Get the size in bytes of a bitmap in a BitmapDrawable. Note that from Android 4.4 (KitKat) onward this returns the allocated memory size of the bitmap which can be larger
	 * than the actual bitmap data byte count (in the case it was re-used).
	 * 
	 * @param value
	 * @return size in bytes
	 */
	@TargetApi(VERSION_CODES.KITKAT)
	public static int getBitmapSize(BitmapDrawable value)
	{
		Bitmap bitmap = value.getBitmap();
		if (bitmap == null)
		{
			return 0;
		}

		// From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
		// larger than bitmap byte count.
		if (Utils.hasKitKat())
		{
			return bitmap.getAllocationByteCount();
		}

		if (Utils.hasHoneycombMR1())
		{
			return bitmap.getByteCount();
		}

		// Pre HC-MR1
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	/**
	 * Measure item size in kilobytes rather than units which is more practical for a bitmap cache
	 */
	protected int sizeOf(String key, BitmapDrawable value)
	{
		final int bitmapSize = getBitmapSize(value) / 1024;
		return bitmapSize == 0 ? 1 : bitmapSize;
	}

	protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue)
	{
		if (RecyclingBitmapDrawable.class.isInstance(oldValue))
		{
			// The removed entry is a recycling drawable, so notify it
			// that it has been removed from the memory cache
			((RecyclingBitmapDrawable) oldValue).setIsCached(false);
		}
		else
		{
			// The removed entry is a standard BitmapDrawable

			if (Utils.hasHoneycomb())
			{
				// We're running on Honeycomb or later, so add the bitmap
				// to a SoftReference set for possible use with inBitmap later
				reusableBitmaps.add(new MySoftReference<Bitmap>(oldValue.getBitmap()));
			}
		}
	}

	public BitmapDrawable putInCache(String data, BitmapDrawable value)
	{
		if (null != value)
		{
			if (RecyclingBitmapDrawable.class.isInstance(value))
				((RecyclingBitmapDrawable) value).setIsCached(true);
			return put(data, value);
		}
		return null;
	}

	/**
	 * @param options
	 *            - BitmapFactory.Options with out* options populated
	 * @return Bitmap that case be used for inBitmap
	 */
	public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options)
	{
		Bitmap bitmap = null;

		if (reusableBitmaps != null && !reusableBitmaps.isEmpty())
		{
			synchronized (reusableBitmaps)
			{
				final Iterator<MySoftReference<Bitmap>> iterator = reusableBitmaps.iterator();
				Bitmap item;

				while (iterator.hasNext())
				{
					item = iterator.next().get();

					if (null != item && item.isMutable())
					{
						// Check to see it the item can be used for inBitmap
						if (canUseForInBitmap(item, options))
						{
							bitmap = item;

							// Remove from reusable set so it can't be used again
							iterator.remove();
							break;
						}
					}
					else
					{
						// Remove from the set if the reference has been cleared.
						iterator.remove();
					}
				}
			}
		}

		return bitmap;
	}

	/**
	 * @param candidate
	 *            - Bitmap to check
	 * @param targetOptions
	 *            - Options that have the out* value populated
	 * @return true if <code>candidate</code> can be used for inBitmap re-use with <code>targetOptions</code>
	 */
	@TargetApi(VERSION_CODES.KITKAT)
	private static boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions)
	{

		if (!Utils.hasKitKat())
		{
			// On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
			return candidate.getWidth() == targetOptions.outWidth && candidate.getHeight() == targetOptions.outHeight && targetOptions.inSampleSize == 1;
		}

		// From Android 4.4 (KitKat) onward we can re-use if the byte size of the new bitmap
		// is smaller than the reusable bitmap candidate allocation byte count.
		int width = targetOptions.outWidth / targetOptions.inSampleSize;
		int height = targetOptions.outHeight / targetOptions.inSampleSize;
		int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
		return byteCount <= candidate.getAllocationByteCount();
	}

	/**
	 * Return the byte usage per pixel of a bitmap based on its configuration.
	 * 
	 * @param config
	 *            The bitmap configuration.
	 * @return The byte usage per pixel.
	 */
	private static int getBytesPerPixel(Config config)
	{
		if (config == Config.ARGB_8888)
		{
			return 4;
		}
		else if (config == Config.RGB_565)
		{
			return 2;
		}
		else if (config == Config.ARGB_4444)
		{
			return 2;
		}
		else if (config == Config.ALPHA_8)
		{
			return 1;
		}
		return 1;
	}

	/**
	 * 
	 * @param key
	 * @return This method is synchronous method and can call db if image is not found in cache
	 */
	public BitmapDrawable getIconFromCache(String key)
	{
		return getIconFromCache(key, false);
	}

	public BitmapDrawable getIconFromCache(String key, boolean rounded)
	{
		String cacheKey = rounded ? key + IconLoader.ROUND_SUFFIX : key;
		BitmapDrawable b = get(cacheKey);
		if (b == null)
		{
			BitmapDrawable bd = (BitmapDrawable) HikeUserDatabase.getInstance().getIcon(key, rounded);
			if (!Utils.hasHoneycomb())
			{
				if (bd == null)
				{
					return null;
				}
				// Running on Gingerbread or older, so wrap in a RecyclingBitmapDrawable
				// which will recycle automagically
				bd = new RecyclingBitmapDrawable(mResources, bd.getBitmap());
			}
			putInCache(cacheKey, bd);
			return bd;
		}
		else
			return b;
	}

	public BitmapDrawable getFileIconFromCache(String key)
	{
		BitmapDrawable b = get(key);
		if (b == null)
		{
			BitmapDrawable bd = (BitmapDrawable) HikeConversationsDatabase.getInstance().getFileThumbnail(key);
			if (!Utils.hasHoneycomb())
			{
				if (bd == null)
				{
					return null;
				}
				// Running on Gingerbread or older, so wrap in a RecyclingBitmapDrawable
				// which will recycle automagically
				bd = new RecyclingBitmapDrawable(mResources, bd.getBitmap());
			}
			putInCache(key, bd);
			return bd;
		}
		else
			return b;
	}

	public void deleteIconForMSISDN(String msisdn)
	{
		HikeUserDatabase.getInstance().removeIcon(msisdn);
		clearIconForMSISDN(msisdn);
	}

	public void clearIconForMSISDN(String msisdn)
	{
		remove(msisdn);
		remove(msisdn + IconLoader.ROUND_SUFFIX);
	}

	public void clearIconCache()
	{
		evictAll();
	}

	public Drawable getSticker(String path)
	{
		BitmapDrawable bd = get(path);
		if (bd != null)
			return bd;

		Bitmap stickerBitmap = BitmapFactory.decodeFile(path);
		if (stickerBitmap == null)
		{
			return null;
		}

		if (Utils.hasHoneycomb())
			bd = new BitmapDrawable(mResources, stickerBitmap);
		else
			bd = new RecyclingBitmapDrawable(mResources, stickerBitmap);
		putInCache(path, bd);
		return bd;
	}

	public Drawable getChatTheme(ChatTheme chatTheme)
	{
		String key;
		if (chatTheme.isTiled())
		{
			key = CHAT_THEME_PREFIX + chatTheme.name();
		}
		else
		{
			/*
			 * We have different non tiled bgs for portrait and landscape
			 */
			boolean isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
			key = CHAT_THEME_PREFIX + (isPortrait ? ORIENTATION_PORTRAIT_PREFIX : ORIENTATION_LANDSCAPE_PREFIX) + chatTheme.name();
		}

		BitmapDrawable bd = get(key);
		if (bd != null)
			return bd;

		if (Utils.hasHoneycomb())
		{
			bd = new BitmapDrawable(mResources, BitmapFactory.decodeResource(mResources, chatTheme.bgResId()));
		}
		else
		{
			bd = new RecyclingBitmapDrawable(mResources, BitmapFactory.decodeResource(mResources, chatTheme.bgResId()));
		}

		if (chatTheme.isTiled())
		{
			bd.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		}

		putInCache(key, bd);
		return bd;
	}
}
