package com.bsb.hike.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.facebook.FacebookRequestError.Category;

public class StickerManager
{
	public static final String SHOWN_DEFAULT_STICKER_DOGGY_CATEGORY_POPUP = "shownDefaultStickerCategoryPopup";

	public static final String SHOWN_DEFAULT_STICKER_HUMANOID_CATEGORY_POPUP = "shownDefaultStickerHumanoidCategoryPopup";

	public static final String DOGGY_CATEGORY_INSERT_TO_DB = "firstCategoryInsertedToDB";

	public static final String HUMANOID_CATEGORY_INSERT_TO_DB = "secondCategoryInsertedToDB";

	public static final String REMOVED_CATGORY_IDS = "removedCategoryIds";

	public static final String SHOW_BOLLYWOOD_STICKERS = "showBollywoodStickers";

	public static final String RESET_REACHED_END_FOR_DEFAULT_STICKERS = "resetReachedEndForDefaultStickers";

	public static final String CORRECT_DEFAULT_STICKER_DIALOG_PREFERENCES = "correctDefaultStickerDialogPreferences";

	public static final String REMOVE_HUMANOID_STICKERS = "removeHumanoiStickers";

	public static final String SHOWN_STICKERS_TUTORIAL = "shownStickersTutorial";

	public static final String STICKERS_DOWNLOADED = "st_downloaded";

	public static final String STICKERS_FAILED = "st_failed";

	public static final String STICKER_DOWNLOAD_TYPE = "stDownloadType";

	public static final String STICKER_DATA_BUNDLE = "stickerDataBundle";

	public static final String STICKER_CATEGORY = "stickerCategory";

	public static final String RECENT_STICKER_SENT = "recentStickerSent";

	public static final String RECENTS_UPDATED = "recentsUpdated";

	public static final String STICKER_ID = "stId";

	public static final String CATEGORY_ID = "catId";

	public static final String STICKER_INDEX = "stIdx";

	public static final String FWD_STICKER_ID = "fwdStickerId";

	public static final String FWD_CATEGORY_ID = "fwdCategoryId";

	public static final String FWD_STICKER_INDEX = "fwdStickerIdx";

	public static final String STICKERS_UPDATED = "stickersUpdated";

	public static int RECENT_STICKERS_COUNT = 30;

	public final int[] LOCAL_STICKER_RES_IDS_HUMANOID = { R.drawable.sticker_9_love1, R.drawable.sticker_10_love2, R.drawable.sticker_11_teasing, R.drawable.sticker_12_rofl,
			R.drawable.sticker_13_bored, R.drawable.sticker_14_angry, R.drawable.sticker_15_strangle, R.drawable.sticker_16_shocked, R.drawable.sticker_17_hurray,
			R.drawable.sticker_18_yawning };

	public final int[] LOCAL_STICKER_SMALL_RES_IDS_HUMANOID = { R.drawable.sticker_9_love1_small, R.drawable.sticker_10_love2_small, R.drawable.sticker_11_teasing_small,
			R.drawable.sticker_12_rofl_small, R.drawable.sticker_13_bored_small, R.drawable.sticker_14_angry_small, R.drawable.sticker_15_strangle_small,
			R.drawable.sticker_16_shocked_small, R.drawable.sticker_17_hurray_small, R.drawable.sticker_18_yawning_small };

	public final String[] LOCAL_STICKER_IDS_HUMANOID = { "001_love1.png", "002_love2.png", "003_teasing.png", "004_rofl.png", "005_bored.png", "006_angry.png", "007_strangle.png",
			"008_shocked.png", "009_hurray.png", "010_yawning.png" };

	public final int[] LOCAL_STICKER_RES_IDS_DOGGY = { R.drawable.sticker_1_hi, R.drawable.sticker_2_thumbsup, R.drawable.sticker_3_drooling, R.drawable.sticker_4_devilsmile,
			R.drawable.sticker_5_sorry, R.drawable.sticker_6_urgh, R.drawable.sticker_7_confused, R.drawable.sticker_8_dreaming, };

	public final int[] LOCAL_STICKER_SMALL_RES_IDS_DOGGY = { R.drawable.sticker_small_1_hi, R.drawable.sticker_small_2_thumbsup, R.drawable.sticker_small_3_drooling,
			R.drawable.sticker_small_4_devilsmile, R.drawable.sticker_small_5_sorry, R.drawable.sticker_small_6_urgh, R.drawable.sticker_small_7_confused,
			R.drawable.sticker_small_8_dreaming };

	public final String[] LOCAL_STICKER_IDS_DOGGY = { "001_hi.png", "002_thumbsup.png", "003_drooling.png", "004_devilsmile.png", "005_sorry.png", "006_urgh.png",
			"007_confused.png", "008_dreaming.png", };

	public enum StickerCategoryId
	{
		recent
		{
			@Override
			public int resId()
			{
				return R.drawable.recents;
			}

			/*
			 * This is not required for recent category as we dont wanna show preview for recents. Returning random negative integer
			 */
			@Override
			public int previewResId()
			{
				return -10;
			}

			/* This is again not for recent category */
			@Override
			public String downloadPref()
			{
				return "rs";
			}
		},
		humanoid
		{
			@Override
			public int resId()
			{
				return R.drawable.humanoid;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_humanoid;
			}

			@Override
			public String downloadPref()
			{
				return "humanoidDownloadShown";
			}
		},
		doggy
		{
			@Override
			public int resId()
			{
				return R.drawable.doggy;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_doggy;
			}

			@Override
			public String downloadPref()
			{
				return "doggyDownloadShown";
			}
		},
		expressions
		{
			@Override
			public int resId()
			{
				return R.drawable.expressions;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_expressions;
			}

			@Override
			public String downloadPref()
			{
				return "expDownloadShown";
			}
		},
		love
		{
			@Override
			public int resId()
			{
				return R.drawable.love;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_love;
			}

			@Override
			public String downloadPref()
			{
				return "loveDownloadShown";
			}
		},
		angry
		{
			@Override
			public int resId()
			{
				return R.drawable.angry;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_angry;
			}

			@Override
			public String downloadPref()
			{
				return "angryDownloadShown";
			}
		},
		bollywood
		{
			@Override
			public int resId()
			{
				return R.drawable.bollywood;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_bollywood;
			}

			@Override
			public String downloadPref()
			{
				return "bollywoodDownloadShown";
			}
		},
		rageface
		{
			@Override
			public int resId()
			{
				return R.drawable.rageface;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_rageface;
			}

			@Override
			public String downloadPref()
			{
				return "rfDownloadShown";
			}
		},
		indian
		{
			@Override
			public int resId()
			{
				return R.drawable.indian;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_indian;
			}

			@Override
			public String downloadPref()
			{
				return "indianDownloadShown";
			}
		},
		humanoid2
		{
			@Override
			public int resId()
			{
				return R.drawable.humanoid2;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_humanoid2;
			}

			@Override
			public String downloadPref()
			{
				return "humanoid2DownloadShown";
			}
		},
		avatars
		{
			@Override
			public int resId()
			{
				return R.drawable.avtars;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_avtars;
			}

			@Override
			public String downloadPref()
			{
				return "avtarsDownloadShown";
			}
		},
		smileyexpressions
		{
			@Override
			public int resId()
			{
				return R.drawable.smileyexpressions;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_smilyexpressions;
			}

			@Override
			public String downloadPref()
			{
				return "smileyexpressionDownloadShown";
			}
		},
		kitty
		{
			@Override
			public int resId()
			{
				return R.drawable.kitty;
			}

			@Override
			public int previewResId()
			{
				return R.drawable.preview_kitty;
			}

			@Override
			public String downloadPref()
			{
				return "kittyDownloadShown";
			}
		},
		unknown
		{
			@Override
			public int resId()
			{
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int previewResId()
			{
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String downloadPref()
			{
				// TODO Auto-generated method stub
				return null;
			}

		};

		public static StickerCategoryId getCategoryIdFromName(String value)
		{
			if (value == null)
				throw new IllegalArgumentException();
			for (StickerCategoryId v : values())
				if (value.equalsIgnoreCase(v.name()))
					return v;
			throw new IllegalArgumentException();
		}

		public abstract int resId();

		public abstract int previewResId();

		public abstract String downloadPref();
	};

	public Map<String, StickerTaskBase> stickerTaskMap;

	private Set<Sticker> recentStickers;

	private List<StickerCategory> stickerCategories;

	private Context context;

	private static SharedPreferences preferenceManager;

	private static StickerManager instance;

	public static StickerManager getInstance()
	{
		if (instance == null)
		{
			synchronized (StickerManager.class)
			{
				if (instance == null)
					instance = new StickerManager();
			}
		}
		return instance;
	}

	private StickerManager()
	{
		stickerCategories = new ArrayList<StickerCategory>();
		if (stickerTaskMap == null)
		{
			stickerTaskMap = new HashMap<String, StickerTaskBase>();
		}
	}

	public void init(Context ctx)
	{
		context = ctx;
		preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
		loadRecentStickers();
	}

	public void loadRecentStickers()
	{
		recentStickers = getSortedListForCategory(StickerCategoryId.recent);
	}

	public List<StickerCategory> getStickerCategoryList()
	{
		return stickerCategories;
	}

	public void setupStickerCategoryList(SharedPreferences preferences)
	{
		/*
		 * TODO : This will throw an exception in case of remove category as, this function will be called from mqtt thread and stickerCategories will be called from UI thread
		 * also.
		 */
		stickerCategories = new ArrayList<StickerCategory>();
		EnumMap<StickerCategoryId, StickerCategory> stickerDataMap = HikeConversationsDatabase.getInstance().stickerDataForCategories();
		for (StickerCategoryId s : StickerCategoryId.values())
		{
			if (s.equals(StickerCategoryId.recent))
			{
				stickerCategories.add(new StickerCategory(StickerCategoryId.recent));
				continue;
			}
			else if (StickerCategoryId.unknown.equals(s))
			{
				/*
				 * We don't want to add the unknown category to this list.
				 */
				continue;
			}
			StickerCategory cat = stickerDataMap.get(s);
			if (cat != null)
				stickerCategories.add(cat);
			else
				stickerCategories.add(new StickerCategory(s, false, false));
		}
		String removedIds = preferences.getString(REMOVED_CATGORY_IDS, "[]");

		try
		{
			JSONArray removedIdArray = new JSONArray(removedIds);
			for (int i = 0; i < removedIdArray.length(); i++)
			{
				String removedCategoryId = removedIdArray.getString(i);
				removeCategoryFromList(removedCategoryId);
			}
		}
		catch (JSONException e)
		{
			Log.w("HikeMessengerApp", "Invalid JSON", e);
		}
	}

	private void removeCategoryFromList(String removedCategoryId)
	{
		Iterator<StickerCategory> it = stickerCategories.iterator();
		while (it.hasNext())
		{
			StickerCategory cat = it.next();
			if (cat.categoryId.name().equals(removedCategoryId))
			{
				removeCategoryFromRecents(cat);
				it.remove();
			}
		}
	}

	private void removeCategoryFromRecents(StickerCategory category)
	{
		if (category.categoryId.equals(StickerCategoryId.doggy))
		{
			for (int i = 0; i < LOCAL_STICKER_IDS_DOGGY.length; i++)
			{
				removeStickerFromRecents(new Sticker(category, LOCAL_STICKER_IDS_DOGGY[i], i));
			}
		}
		else if (category.categoryId.equals(StickerCategoryId.humanoid))
		{
			for (int i = 0; i < LOCAL_STICKER_IDS_HUMANOID.length; i++)
			{
				removeStickerFromRecents(new Sticker(category, LOCAL_STICKER_IDS_HUMANOID[i], i));
			}
		}
		String categoryDirPath = getStickerDirectoryForCategoryId(context, category.categoryId.name());
		if (categoryDirPath != null)
		{
			File categoryDir = new File(categoryDirPath + HikeConstants.SMALL_STICKER_ROOT);
			File bigCatDir = new File(categoryDirPath);
			if (categoryDir.exists())
			{
				String[] stickerIds = categoryDir.list();
				for (String stickerId : stickerIds)
				{
					recentStickers.remove(new Sticker(category, stickerId));
				}
			}
			Utils.deleteFile(bigCatDir);
		}
	}

	public void insertDoggyCategory()
	{
		HikeConversationsDatabase.getInstance().insertDoggyStickerCategory();
		Editor editor = preferenceManager.edit();
		editor.putBoolean(DOGGY_CATEGORY_INSERT_TO_DB, true);
		editor.commit();
	}

	public void insertHumanoidCategory()
	{
		HikeConversationsDatabase.getInstance().insertHumanoidStickerCategory();
		Editor editor = preferenceManager.edit();
		editor.putBoolean(HUMANOID_CATEGORY_INSERT_TO_DB, true);
		editor.commit();
	}

	public void resetReachedEndForDefaultStickers()
	{
		HikeConversationsDatabase.getInstance().updateReachedEndForCategory(StickerCategoryId.doggy.name(), false);
		HikeConversationsDatabase.getInstance().updateReachedEndForCategory(StickerCategoryId.humanoid.name(), false);
		Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putBoolean(RESET_REACHED_END_FOR_DEFAULT_STICKERS, true);
		editor.commit();
	}

	public void setDialoguePref()
	{
		SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Editor editor = settings.edit();
		editor.putBoolean(StickerCategoryId.humanoid.downloadPref(), settings.getBoolean(SHOWN_DEFAULT_STICKER_HUMANOID_CATEGORY_POPUP, false));
		editor.putBoolean(StickerCategoryId.doggy.downloadPref(), settings.getBoolean(SHOWN_DEFAULT_STICKER_DOGGY_CATEGORY_POPUP, false));
		editor.putBoolean(StickerManager.CORRECT_DEFAULT_STICKER_DIALOG_PREFERENCES, true);
		editor.commit();
	}

	public void removeHumanoidSticker()
	{
		String categoryDirPath = getStickerDirectoryForCategoryId(context, StickerCategoryId.humanoid.name());
		if (categoryDirPath != null)
		{
			File categoryDir = new File(categoryDirPath);
			Utils.deleteFile(categoryDir);
			Editor editor = preferenceManager.edit();
			editor.putBoolean(REMOVE_HUMANOID_STICKERS, true);
			editor.commit();
		}
	}

	public static void setStickersForIndianUsers(boolean isIndianUser, SharedPreferences prefs)
	{
		HikeMessengerApp.isIndianUser = isIndianUser;
		if (!prefs.contains(StickerManager.SHOW_BOLLYWOOD_STICKERS))
		{
			setupBollywoodCategoryVisibility(prefs);
		}
	}

	public static void setupBollywoodCategoryVisibility(SharedPreferences prefs)
	{
		/*
		 * We now show the bollywood category for all users.
		 */
		Editor editor = prefs.edit();
		editor.remove(SHOW_BOLLYWOOD_STICKERS);
		editor.remove(REMOVED_CATGORY_IDS);
		editor.commit();
	}

	public Set<Sticker> getRecentStickerList()
	{
		return recentStickers;
	}

	public void addRecentSticker(Sticker st)
	{
		boolean isRemoved = recentStickers.remove(st);
		if (isRemoved) // this means list size is less than 30
			recentStickers.add(st);
		else if (recentStickers.size() == RECENT_STICKERS_COUNT) // if size is already RECENT_STICKERS_COUNT remove first element and then add
		{
			synchronized (recentStickers)
			{
				Sticker firstSt = recentStickers.iterator().next();
				if (firstSt != null)
					recentStickers.remove(firstSt);
				recentStickers.add(st);
			}
		}
		else
		{
			recentStickers.add(st);
		}
	}

	public void removeStickerFromRecents(Sticker st)
	{
		recentStickers.remove(st);
	}

	public void setStickerUpdateAvailable(String categoryId, boolean updateAvailable)
	{
		for (StickerCategory sc : stickerCategories)
		{
			if (sc.categoryId.name().equals(categoryId))
				sc.updateAvailable = updateAvailable;
		}
	}

	public StickerCategory getCategoryForIndex(int index)
	{
		if (index == -1 || index >= stickerCategories.size())
		{
			throw new IllegalArgumentException();
		}
		return stickerCategories.get(index);
	}

	private String getExternalStickerDirectoryForCategoryId(Context context, String catId)
	{
		File dir = context.getExternalFilesDir(null);
		if (dir == null)
		{
			return null;
		}
		return dir.getPath() + HikeConstants.STICKERS_ROOT + "/" + catId;
	}

	private String getInternalStickerDirectoryForCategoryId(Context context, String catId)
	{
		return context.getFilesDir().getPath() + HikeConstants.STICKERS_ROOT + "/" + catId;
	}

	/**
	 * Returns the directory for a sticker category.
	 * 
	 * @param context
	 * @param catId
	 * @return
	 */
	public String getStickerDirectoryForCategoryId(Context context, String catId)
	{
		/*
		 * We give a higher priority to external storage. If we find an exisiting directory in the external storage, we will return its path. Otherwise if there is an exisiting
		 * directory in internal storage, we return its path.
		 * 
		 * If the directory is not available in both cases, we return the external storage's path if external storage is available. Else we return the internal storage's path.
		 */
		boolean externalAvailable = false;
		if (Utils.getExternalStorageState() == ExternalStorageState.WRITEABLE)
		{
			externalAvailable = true;
			String stickerDirPath = getExternalStickerDirectoryForCategoryId(context, catId);

			if (stickerDirPath == null)
			{
				return null;
			}

			File stickerDir = new File(stickerDirPath);

			if (stickerDir.exists())
			{
				return stickerDir.getPath();
			}
		}
		File stickerDir = new File(getInternalStickerDirectoryForCategoryId(context, catId));
		if (stickerDir.exists())
		{
			return stickerDir.getPath();
		}
		if (externalAvailable)
		{
			return getExternalStickerDirectoryForCategoryId(context, catId);
		}
		return getInternalStickerDirectoryForCategoryId(context, catId);
	}

	public boolean checkIfStickerCategoryExists(String categoryId)
	{
		String path = getStickerDirectoryForCategoryId(context, categoryId);
		if (path == null)
		{
			return false;
		}
		File category = new File(path + HikeConstants.LARGE_STICKER_ROOT);
		if (category.exists() && category.list().length > 0)
		{
			return true;
		}
		return false;
	}

	public boolean isStickerDownloading(String key)
	{
		if (key != null)
			return stickerTaskMap.containsKey(key);
		return false;
	}

	public StickerTaskBase getTask(String key)
	{
		if (key == null)
			return null;
		return stickerTaskMap.get(key);
	}

	public void insertTask(String categoryId, StickerTaskBase downloadStickerTask)
	{
		stickerTaskMap.put(categoryId, downloadStickerTask);
	}

	public void removeTask(String key)
	{
		stickerTaskMap.remove(key);
	}

	public StickerCategory getCategoryForName(String categoryName)
	{
		for (int i = 0; i < stickerCategories.size(); i++)
		{
			if (stickerCategories.get(i).categoryId.name().equals(categoryName))
				return stickerCategories.get(i);
		}
		return new StickerCategory(StickerCategoryId.unknown);
	}

	/***
	 * 
	 * @param catId
	 * @return
	 * 
	 *         This function can return null if file doesnot exist.
	 */
	public Set<Sticker> getSortedListForCategory(StickerCategoryId catId)
	{
		Set<Sticker> list = null;
		try
		{
			long t1 = System.currentTimeMillis();
			String extDir = getStickerDirectoryForCategoryId(context, catId.name());
			File dir = new File(extDir);
			if (!dir.exists())
			{
				dir.mkdirs();
				return Collections.synchronizedSet(new LinkedHashSet<Sticker>(RECENT_STICKERS_COUNT));
			}
			File catFile = new File(extDir, catId.name() + ".bin");
			if (!catFile.exists())
				return Collections.synchronizedSet(new LinkedHashSet<Sticker>(RECENT_STICKERS_COUNT));
			FileInputStream fileIn = new FileInputStream(catFile);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			int size = in.readInt();
			list = Collections.synchronizedSet(new LinkedHashSet<Sticker>(size));
			for (int i = 0; i < size; i++)
			{
				Sticker s = new Sticker();
				s.deSerializeObj(in);
				list.add(s);
			}
			in.close();
			fileIn.close();
			long t2 = System.currentTimeMillis();
			Log.d(getClass().getSimpleName(), "Time in ms to get sticker list of category : " + catId + " from file :" + (t2 - t1));
		}
		catch (Exception e)
		{
			Log.e(getClass().getSimpleName(), "Exception while reading category file.", e);
		}
		return list;
	}

	public void saveSortedListForCategory(StickerCategoryId catId, Set<Sticker> list)
	{
		try
		{
			if (list.size() == 0)
				return;

			long t1 = System.currentTimeMillis();
			String extDir = getStickerDirectoryForCategoryId(context, catId.name());
			File dir = new File(extDir);
			if (!dir.exists())
				dir.mkdirs();
			File catFile = new File(extDir, catId.name() + ".bin");
			FileOutputStream fileOut = new FileOutputStream(catFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeInt(list.size());
			synchronized (recentStickers)
			{
				Iterator<Sticker> it = list.iterator();
				while (it.hasNext())
				{
					it.next().serializeObj(out);
				}
			}
			out.flush();
			out.close();
			fileOut.close();
			long t2 = System.currentTimeMillis();
			Log.d(getClass().getSimpleName(), "Time in ms to save sticker list of category : " + catId + " to file :" + (t2 - t1));
		}
		catch (Exception e)
		{
			Log.e(getClass().getSimpleName(), "Exception while saving category file.", e);
		}
	}

	public void deleteStickers()
	{
		/*
		 * First delete all stickers, if any, in the internal memory
		 */
		String dirPath = context.getFilesDir().getPath() + HikeConstants.STICKERS_ROOT;
		File dir = new File(dirPath);
		if (dir.exists())
		{
			Utils.deleteFile(dir);
		}

		/*
		 * Next is the external memory. We first check if its available or not.
		 */
		if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
		{
			return;
		}
		String extDirPath = context.getExternalFilesDir(null).getPath() + HikeConstants.STICKERS_ROOT;
		File extDir = new File(extDirPath);
		if (extDir.exists())
		{
			Utils.deleteFile(extDir);
		}

		/* Delete recent stickers */
		String recentsDir = getStickerDirectoryForCategoryId(context, StickerCategoryId.recent.name());
		File rDir = new File(recentsDir);
		if (rDir.exists())
			Utils.deleteFile(rDir);
	}
}
