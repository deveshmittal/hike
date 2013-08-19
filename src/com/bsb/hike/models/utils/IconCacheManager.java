package com.bsb.hike.models.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import android.graphics.drawable.Drawable;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;

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

	private Map<String, Drawable> mIcons; /*
										 * maps strings to bitmaps. Should be an
										 * LRU cache
										 */

	private HikeUserDatabase mDb;
	private HikeConversationsDatabase hCDb;
	private static IconCacheManager mCacheManager;

	public IconCacheManager() {
		mIcons = Collections
				.synchronizedMap(new LRUCache<String, Drawable>(20)); /*
																	 * only keep
																	 * 20
																	 * entries @
																	 * a time
																	 */
		mDb = HikeUserDatabase.getInstance();
		hCDb = HikeConversationsDatabase.getInstance();
	}

	public static void init() {
		if (mCacheManager == null) {
			synchronized (IconCacheManager.class) {
				if (mCacheManager == null) {
					mCacheManager = new IconCacheManager();
				}
			}
		}
	}

	public static IconCacheManager getInstance() {
		return mCacheManager;
	}

	public synchronized Drawable getIconForMSISDN(String msisdn) {
		Drawable b = mIcons.get(msisdn);
		if (b == null) {
			b = mDb.getIcon(msisdn);
			mIcons.put(msisdn, b);
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

	public synchronized void clearIconForMSISDN(String msisdn) {
		mIcons.remove(msisdn);
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
