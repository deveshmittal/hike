package com.bsb.hike.models.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.utils.Utils;

public class IconCacheManager {
	private class LRUCache<A, B> extends LinkedHashMap<A, B> {

		private static final long serialVersionUID = 1L;
		private final int maxEntries;

		public LRUCache(final int maxEntries) {
			super(maxEntries + 1, 1.0f, true);
			this.maxEntries = maxEntries;
		}

		/**
		 * Returns <tt>true</tt> if this <code>LruCache</code> has more entries
		 * than the maximum specified when it was created.
		 * 
		 * <p>
		 * This method <em>does not</em> modify the underlying <code>Map</code>;
		 * it relies on the implementation of <code>LinkedHashMap</code> to do
		 * that, but that behavior is documented in the JavaDoc for
		 * <code>LinkedHashMap</code>.
		 * </p>
		 * 
		 * @param eldest
		 *            the <code>Entry</code> in question; this implementation
		 *            doesn't care what it is, since the implementation is only
		 *            dependent on the size of the cache
		 * @return <tt>true</tt> if the oldest
		 * @see java.util.LinkedHashMap#removeEldestEntry(Map.Entry)
		 */
		@Override
		protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
			return super.size() > maxEntries;
		}
	}

	private static final String ROUND_SUFFIX = "round";
	private Map<String, Drawable> mIcons; /*
										 * maps strings to bitmaps. Should be an
										 * LRU cache
										 */

	private HikeUserDatabase mDb;
	private HikeConversationsDatabase hCDb;
	private static IconCacheManager mCacheManager;

	private IconCacheManager() {
		mIcons = Collections.synchronizedMap(new LRUCache<String, Drawable>(getCacheSize()));
		mDb = HikeUserDatabase.getInstance();
		hCDb = HikeConversationsDatabase.getInstance();
	}

	private int getCacheSize() {
		int densityMultiplier = Utils.getResolutionId();
		switch (densityMultiplier) {
		case HikeConstants.XXHDPI_ID:
			return HikeConstants.XXHDPI_CACHE;
		case HikeConstants.XHDPI_ID:
			return HikeConstants.XHDPI_CACHE;
		case HikeConstants.HDPI_ID:
			return HikeConstants.HDPI_CACHE;
		case HikeConstants.MDPI_ID:
			return HikeConstants.MDPI_CACHE;
		case HikeConstants.LDPI_ID:
			return HikeConstants.LDPI_CACHE;
		default:
			return HikeConstants.MDPI_CACHE;
		}
	}

	private static void init() {
		if (mCacheManager == null) {
			synchronized (IconCacheManager.class) {
				if (mCacheManager == null) {
					mCacheManager = new IconCacheManager();
				}
			}
		}
	}

	private static IconCacheManager getInstance() {
		if (mCacheManager == null) {
			synchronized (IconCacheManager.class) {
				if (mCacheManager == null) {
					mCacheManager = new IconCacheManager();
				}
			}
		}
		return mCacheManager;
	}

	public synchronized Drawable getIconForMSISDN(String msisdn) {
		return getIconForMSISDN(msisdn, false);
	}

	public synchronized Drawable getIconForMSISDN(String msisdn, boolean rounded) {
		String key = msisdn + (rounded ? ROUND_SUFFIX : "");

		Drawable b = mIcons.get(key);
		if (b == null) {
			b = mDb.getIcon(msisdn, rounded);
			mIcons.put(key, b);
		}

		return b;
	}

	public synchronized Drawable getFileThumbnail(String fileKey) {
		Drawable b = mIcons.get(fileKey);
		if (b == null) {
			b = hCDb.getFileThumbnail(fileKey);

			if (b != null) {
				mIcons.put(fileKey, b);
			}
		}

		return b;
	}

	public Drawable getStickerThumbnail(String stickerPath) {
		Drawable b = mIcons.get(stickerPath);
		if (b == null) {
			long t1 = System.currentTimeMillis();
			b = new BitmapDrawable(BitmapFactory.decodeFile(stickerPath));
			long t2 = System.currentTimeMillis();
			//Log.w("Sticker loading", "Time to load sticker bitmap : "+(t2-t1)+" ms");
			if (b != null) {
				mIcons.put(stickerPath, b);
			}
		}
		return b;
	}

	public Drawable getSticker(Context context, String stickerPath) {
		Drawable b = mIcons.get(stickerPath);
		if (b == null) {
			b = new BitmapDrawable(context.getResources(),
					BitmapFactory.decodeFile(stickerPath));

			if (b != null) {
				mIcons.put(stickerPath, b);
			}
		}
		return b;
	}

	public synchronized void clearIconForMSISDN(String msisdn) {
		mIcons.remove(msisdn);
		mIcons.remove(msisdn + ROUND_SUFFIX);
		HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
	}

	public synchronized void deleteIconForMSISDN(String msisdn) {
		mDb.removeIcon(msisdn);
		clearIconForMSISDN(msisdn);
	}

	public synchronized void clearIconCache() {
		mIcons.clear();
	}
}
