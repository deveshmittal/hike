package com.bsb.hike.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.CustomStickerCategory;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.stickerdownloadmgr.IStickerResultListener;
import com.bsb.hike.modules.stickerdownloadmgr.SingleStickerDownloadTask;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerDownloadManager;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerException;
import com.bsb.hike.utils.Utils.ExternalStorageState;

public class StickerManager
{
	public static final String STICKERS_MOVED_EXTERNAL_TO_INTERNAL = "movedStickersExtToInt";
	
	public static final String RECENT_STICKER_SERIALIZATION_LOGIC_CORRECTED = "recentStickerSerializationCorrected";

	public static final String REMOVED_CATGORY_IDS = "removedCategoryIds";

	public static final String RESET_REACHED_END_FOR_DEFAULT_STICKERS = "resetReachedEndForDefaultStickers";

	public static final String CORRECT_DEFAULT_STICKER_DIALOG_PREFERENCES = "correctDefaultStickerDialogPreferences";

	public static final String SHOWN_STICKERS_TUTORIAL = "shownStickersTutorial";
	
	public static final String STICKERS_DOWNLOADED = "st_downloaded";
	
	public static final String MORE_STICKERS_DOWNLOADED = "st_more_downloaded";

	public static final String STICKERS_FAILED = "st_failed";
	
	public static final String STICKERS_PROGRESS = "st_progress";

	public static final String STICKER_DOWNLOAD_TYPE = "stDownloadType";

	public static final String STICKER_DATA_BUNDLE = "stickerDataBundle";
	
	public static final String STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE = "stickerDownloadFailedTooLarge";

	public static final String STICKER_CATEGORY = "stickerCategory";

	public static final String RECENT_STICKER_SENT = "recentStickerSent";

	public static final String RECENTS_UPDATED = "recentsUpdated";

	public static final String STICKER_ID = "stId";

	public static final String CATEGORY_ID = "catId";

	public static final String FWD_STICKER_ID = "fwdStickerId";

	public static final String FWD_CATEGORY_ID = "fwdCategoryId";

	public static final String STICKERS_UPDATED = "stickersUpdated";

	public static final String ADD_NO_MEDIA_FILE_FOR_STICKERS = "addNoMediaFileForStickers";

	public static final String DELETE_DEFAULT_DOWNLOADED_EXPRESSIONS_STICKER = "delDefaultDownloadedExpressionsStickers";
	
	public static final String HARCODED_STICKERS = "harcodedStickers";
	
	public static final String STICKER_IDS = "stickerIds";
	
	public static final String CATEGORY_IDS = "catIds";
	
	public static final String RESOURCE_IDS = "resourceIds";
	
	public static final String MOVED_HARDCODED_STICKERS_TO_SDCARD = "movedHardCodedStickersToSdcard";

	private static final String TAG = "StickerManager";

	public static int RECENT_STICKERS_COUNT = 30;
	
	public static int MAX_CUSTOM_STICKERS_COUNT = 30;
	
	public static final int SIZE_IMAGE = (int) (80 * Utils.densityMultiplier);

	public static final String UPGRADE_FOR_STICKER_SHOP_VERSION_1 = "upgradeForStickerShopVersion1";
	
	public static final String STICKERS_JSON_FILE_NAME = "stickers_data";
	
	public static final String STICKER_CATEGORIES = "stickerCategories";

	public static final String CATEGORY_NAME = "categoryName";

	public static final String IS_VISIBLE = "isVisible";

	public static final String IS_CUSTOM = "isCustom";

	public static final String IS_ADDED = "isAdded";

	public static final String CATEGORY_INDEX = "catIndex";

	public static final String METADATA = "metadata";

	public static final String TIMESTAMP = "timestamp";

	public static final String TOTAL_STICKERS = "totalStickers";
	
	public static final String DOWNLOAD_PREF = "downloadPref";

	public static final String RECENT = "recent";

	public static final String DOGGY_CATEGORY = "doggy";

	public static final String EXPRESSIONS = "expressions";

	public static final String HUMANOID = "humanoid";
	
	public static final String OTHER_STICKER_ASSET_ROOT = "/other";

	public static final String PALLATE_ICON = "pallate_icon";

	public static final String PALLATE_ICON_SELECTED = "pallate_icon_selected";

	public static final String PREVIEW_IMAGE = "preview";
	
	public static final int PALLATE_ICON_TYPE = 0;

	public static final int PALLATE_ICON_SELECTED_TYPE = 1;

	public static final int PREVIEW_IMAGE_TYPE = 2;
	
	public static final String OTHER_ICON_TYPE = ".png";

	public static final String CATEGORY_SIZE = "categorySize";

	public static final String STICKERS_SIZE_DOWNLOADED = "stickersSizeDownloaded";
	
	public static final String PERCENTAGE = "percentage";
	
	public static final String LAST_STICKER_SHOP_UPDATE_TIME = "lastStickerShopUpdateTime";
	
	public static final String STICKER_SHOP_DATA_FULLY_FETCHED = "stickerShopDataFullyFetched";
	
	public static final long STICKER_SHOP_REFRESH_TIME = 24 * 60 * 60 * 1000;

	public static final String SEND_SOURCE = "source";
	
	public static final String FROM_RECENT = "r";
	
	public static final String FROM_FORWARD = "f";
	
	public static final String FROM_OTHER = "o";
	
	public static final long MINIMUM_FREE_SPACE = 10 * 1024 * 1024;

	public static final String SHOW_STICKER_SHOP_BADGE = "showStickerShopBadge";

	public static final String STICKER_RES_ID = "stickerResId";
	
	private static final String REMOVE_LEGACY_GREEN_DOTS = "removeLegacyGreenDots";
	
	private final Map<String, StickerCategory> stickerCategoriesMap;
	
	public static final int DEFAULT_POSITION = 3;

	public static final String STICKER_FOLDER_NAMES_UPGRADE_DONE = "upgradeForStickerFolderNames";
	
	public FilenameFilter stickerFileFilter = new FilenameFilter()
	{
		@Override
		public boolean accept(File file, String fileName)
		{
			return !".nomedia".equalsIgnoreCase(fileName);
		}
	};

	private Context context;

	private static SharedPreferences preferenceManager;

	private static volatile StickerManager instance;
	
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
		stickerCategoriesMap = Collections.synchronizedMap(new LinkedHashMap<String, StickerCategory>());
	}

	public void init(Context ctx)
	{
		context = ctx;
		preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);

	}
	
	public void doInitialSetup()
	{
		// move stickers from external to internal if not done
		SharedPreferences settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if(!settings.getBoolean(StickerManager.RECENT_STICKER_SERIALIZATION_LOGIC_CORRECTED, false)){
			updateRecentStickerFile(settings);
		}
		
		SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
		setupStickerCategoryList(settings);

		if (!settings.getBoolean(StickerManager.ADD_NO_MEDIA_FILE_FOR_STICKERS, false))
		{
			addNoMediaFilesToStickerDirectories();
		}

		/*
		 * this code path will be for users upgrading to the build where we make expressions a default loaded category
		 */
		if (!settings.getBoolean(StickerManager.DELETE_DEFAULT_DOWNLOADED_EXPRESSIONS_STICKER, false))
		{
			settings.edit().putBoolean(StickerManager.DELETE_DEFAULT_DOWNLOADED_EXPRESSIONS_STICKER, true).commit();

			if (checkIfStickerCategoryExists(DOGGY_CATEGORY))
			{
				StickerManager.getInstance().setStickerUpdateAvailable(DOGGY_CATEGORY, true);
			}
		}
		
		/**
		 * This code path is used for removing green dot bug, in which even though there are no stickers to download, the green dot persists.
		 * 
		 * TODO : Remove this code flow after 3-4 release cycles.
		 */
		
		if(!settings.getBoolean(StickerManager.REMOVE_LEGACY_GREEN_DOTS, false))
		{
			removeLegacyGreenDots();
			settings.edit().putBoolean(StickerManager.REMOVE_LEGACY_GREEN_DOTS, true).commit();
		}
		
		if(!settings.getBoolean(StickerManager.STICKER_FOLDER_NAMES_UPGRADE_DONE, false))
		{
			updateStickerFolderNames();
			settings.edit().putBoolean(StickerManager.STICKER_FOLDER_NAMES_UPGRADE_DONE, true).commit();
		}
	}

	public List<StickerCategory> getStickerCategoryList()
	{
		List<StickerCategory> stickerCategoryList = new ArrayList<StickerCategory>(stickerCategoriesMap.values());
		Collections.sort(stickerCategoryList);
		return stickerCategoryList;
	}

	public Map<String, StickerCategory> getStickerCategoryMap()
	{
		// TODO Auto-generated method stub
		return stickerCategoriesMap;
	}

	public void setupStickerCategoryList(SharedPreferences preferences)
	{
		/*
		 * TODO : This will throw an exception in case of remove category as, this function will be called from mqtt thread and stickerCategories will be called from UI thread
		 * also.
		 */
		stickerCategoriesMap.clear();
		stickerCategoriesMap.putAll(HikeConversationsDatabase.getInstance().getAllStickerCategoriesWithVisibility(true));
	}

	public void removeCategory(String removedCategoryId)
	{
		HikeConversationsDatabase.getInstance().removeStickerCategory(removedCategoryId);
		StickerCategory cat = stickerCategoriesMap.remove(removedCategoryId);
		if(!cat.isCustom())
		{
			String categoryDirPath = getStickerDirectoryForCategoryId(removedCategoryId);
			if (categoryDirPath != null)
			{
				File smallCatDir = new File(categoryDirPath + HikeConstants.SMALL_STICKER_ROOT);
				File bigCatDir = new File(categoryDirPath);
				if (smallCatDir.exists())
				{
					String[] stickerIds = smallCatDir.list();
					for (String stickerId : stickerIds)
					{
						removeStickerFromCustomCategory(new Sticker(removedCategoryId, stickerId));
					}
				}
				Utils.deleteFile(bigCatDir);
				Utils.deleteFile(smallCatDir);
			}
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
	}

	public void addNoMediaFilesToStickerDirectories()
	{
		File dir = context.getExternalFilesDir(null);
		if (dir == null)
		{
			return;
		}
		String rootPath = dir.getPath() + HikeConstants.STICKERS_ROOT;
		File root = new File(rootPath);
		if (!root.exists())
		{
			return;
		}
		addNoMedia(root);

		Editor editor = preferenceManager.edit();
		editor.putBoolean(ADD_NO_MEDIA_FILE_FOR_STICKERS, true);
		editor.commit();
	}

	private void addNoMedia(File directory)
	{
		try
		{
			String path = directory.getPath();
			if (path.endsWith(HikeConstants.LARGE_STICKER_ROOT) || path.endsWith(HikeConstants.SMALL_STICKER_ROOT))
			{
				Utils.makeNoMediaFile(directory);
			}
			else if (directory.isDirectory())
			{
				for (File file : directory.listFiles())
				{
					addNoMedia(file);
				}
			}
		}
		catch (Exception e)
		{
		}
	}

	public void addRecentSticker(Sticker st)
	{
		((CustomStickerCategory) stickerCategoriesMap.get(StickerManager.RECENT)).addSticker(st);
	}

	public void removeSticker(String categoryId, String stickerId)
	{
		String categoryDirPath = getStickerDirectoryForCategoryId(categoryId);
		if (categoryDirPath == null)
		{
			return;
		}
		File categoryDir = new File(categoryDirPath);

		/*
		 * If the category itself does not exist, then we have nothing to delete
		 */
		if (!categoryDir.exists())
		{
			return;
		}
		File stickerSmall = new File(categoryDir + HikeConstants.SMALL_STICKER_ROOT, stickerId);
		stickerSmall.delete();
		
		if(stickerCategoriesMap == null)
		{
			return;
		}
		Sticker st = new Sticker(categoryId, stickerId);
		removeStickerFromCustomCategory(st);
		
	}
	
	public void removeStickerFromCustomCategory(Sticker st)
	{
		for(StickerCategory category : stickerCategoriesMap.values())
		{
			if(category.isCustom())
			{
				((CustomStickerCategory) category).removeSticker(st);
				Logger.d(TAG, "Sticker removed from custom category : " + category.getCategoryId());
			}
		}
	}

	public void setStickerUpdateAvailable(String categoryId, boolean updateAvailable)
	{
		updateStickerCategoryData(categoryId, updateAvailable, -1, -1);
	}
	public void updateStickerCategoryData(String categoryId, Boolean updateAvailable, int totalStickerCount, int categorySize)
	{
		StickerCategory category = stickerCategoriesMap.get(categoryId);
		if(category != null)
		{
			if(updateAvailable != null)
			{
				category.setUpdateAvailable(updateAvailable);
			}
			if(totalStickerCount != -1)
			{
				category.setTotalStickers(totalStickerCount);
			}
			if(categorySize != -1)
			{
				category.setCategorySize(categorySize);
			}
		}
		HikeConversationsDatabase.getInstance().updateStickerCategoryData(categoryId, updateAvailable, totalStickerCount, categorySize);
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

	public String getInternalStickerDirectoryForCategoryId(String catId)
	{
		return context.getFilesDir().getPath() + HikeConstants.STICKERS_ROOT + "/" + catId;
	}

	/**
	 * Returns the directory for a sticker category.
	 * @param catId
	 * 
	 * @return
	 */
	public String getStickerDirectoryForCategoryId(String catId)
	{
		/*
		 * We give a higher priority to external storage. If we find an exisiting directory in the external storage, we will return its path. Otherwise if there is an exisiting
		 * directory in internal storage, we return its path.
		 * 
		 * If the directory is not available in both cases, we return the external storage's path if external storage is available. Else we return the internal storage's path.
		 */
		boolean externalAvailable = false;
		ExternalStorageState st = Utils.getExternalStorageState();
		Logger.d(TAG, "External Storage state : " + st.name());
		if (st == ExternalStorageState.WRITEABLE)
		{
			externalAvailable = true;
			String stickerDirPath = getExternalStickerDirectoryForCategoryId(context, catId);
			Logger.d(TAG, "Sticker dir path : " + stickerDirPath);
			if (stickerDirPath == null)
			{
				return null;
			}

			File stickerDir = new File(stickerDirPath);

			if (stickerDir.exists())
			{
				Logger.d(TAG, "Sticker Dir exists .... so returning");
				return stickerDir.getPath();
			}
		}
		if (externalAvailable)
		{
			Logger.d(TAG, "Returning external storage dir.");
			return getExternalStickerDirectoryForCategoryId(context, catId);
		}
		else
		{
			return null;	
		}	
	}

	public boolean checkIfStickerCategoryExists(String categoryId)
	{
		String path = getStickerDirectoryForCategoryId(categoryId);
		if (path == null)
			return false;

		File categoryDir = new File(path + HikeConstants.SMALL_STICKER_ROOT);
		if (categoryDir.exists())
		{
			String[] stickerIds = categoryDir.list(stickerFileFilter);
			if (stickerIds.length > 0)
				return true;
			else
				return false;
		}
		return false;
	}

	public StickerCategory getCategoryForId(String categoryId)
	{
		return stickerCategoriesMap.get(categoryId);
	}

	public void saveCustomCategories()
	{
		saveSortedListForCategory(StickerManager.RECENT);
	}
	
	public void saveSortedListForCategory(String catId)
	{
		StickerCategory customCategory = stickerCategoriesMap.get(catId);

		if (customCategory == null)
		{
			return;
		}

		/**
		 * Putting an instance of check here to avoid ClassCastException.
		 */

		if (!(customCategory instanceof CustomStickerCategory))
		{
			Logger.d("StickerManager", "Inside saveSortedListforCategory : " + customCategory.getCategoryName() + " is not CustomStickerCategory");
			return;
		}

		Set<Sticker> list = ((CustomStickerCategory) customCategory).getStickerSet();
		try
		{
			if (list.size() == 0)
				return;

			long t1 = System.currentTimeMillis();
			String extDir = StickerManager.getInstance().getInternalStickerDirectoryForCategoryId(catId);
			File dir = new File(extDir);
			if (!dir.exists() && !dir.mkdirs())
			{
				return;
			}
			File catFile = new File(extDir, catId + ".bin");
			FileOutputStream fileOut = new FileOutputStream(catFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeInt(list.size());
			synchronized (list)
			{
				Iterator<Sticker> it = list.iterator();
				Sticker st = null;
				while (it.hasNext())
				{
					try
					{
						st = it.next();
						st.serializeObj(out);
					}
					catch (Exception e)
					{
						Logger.e(TAG, "Exception while serializing a sticker : " + st.getStickerId(), e);
					}
				}
			}
			out.flush();
			fileOut.flush();
			fileOut.getFD().sync();
			out.close();
			fileOut.close();
			long t2 = System.currentTimeMillis();
			Logger.d(TAG, "Time in ms to save sticker list of category : " + catId + " to file :" + (t2 - t1));
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception while saving category file.", e);
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
		String recentsDir = getStickerDirectoryForCategoryId(StickerManager.RECENT);
		File rDir = new File(recentsDir);
		if (rDir.exists())
			Utils.deleteFile(rDir);
	}

	public void setContext(Context context)
	{
		this.context = context;
	}

	public void moveRecentStickerFileToInternal(Context context)
	{
		try
		{
			this.context = context;
			Logger.i("stickermanager", "moving recent file from external to internal");
			String recent = StickerManager.RECENT;
			Utils.copyFile(getExternalStickerDirectoryForCategoryId(context, recent) + "/" + recent + ".bin", getInternalStickerDirectoryForCategoryId(recent) + "/"
					+ recent + ".bin", null);
			Logger.i("stickermanager", "moving finished recent file from external to internal");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void deleteDuplicateStickers(String parentDir, String[] bundledFileNames)
	{

		HashSet<String> originalNames = new HashSet<String>(bundledFileNames.length);
		for (String name : bundledFileNames)
		{
			originalNames.add(name);
		}

		deleteDuplicateFiles(originalNames, parentDir + File.separator + HikeConstants.SMALL_STICKER_ROOT);
		deleteDuplicateFiles(originalNames, parentDir + File.separator + HikeConstants.LARGE_STICKER_ROOT);

	}

	public void deleteDuplicateFiles(HashSet<String> originalNames, String fileDir)
	{
		File dir = new File(fileDir);
		String[] fileNames = null;
		if (dir.exists() && dir.isDirectory())
		{
			fileNames = dir.list();
		}
		else
		{
			return;
		}
		for (String fileName : fileNames)
		{
			if (originalNames.contains(fileName))
			{
				File file = new File(fileDir, fileName);
				if (file.exists())
				{
					file.delete();
				}
			}
		}
	}
	
	private String getStickerRootDirectory(Context context) {
		boolean externalAvailable = false;
		ExternalStorageState st = Utils.getExternalStorageState();
		if (st == ExternalStorageState.WRITEABLE) {
			externalAvailable = true;
			String stickerDirPath = getExternalStickerRootDirectory(context);
			if (stickerDirPath == null) {
				return null;
			}

			File stickerDir = new File(stickerDirPath);

			if (stickerDir.exists()) {
				return stickerDir.getPath();
			}
		}
		File stickerDir = new File(getInternalStickerRootDirectory(context));
		if (stickerDir.exists()) {
			return stickerDir.getPath();
		}
		if (externalAvailable) {
			return getExternalStickerRootDirectory(context);
		}
		return getInternalStickerRootDirectory(context);
	}

	private String getExternalStickerRootDirectory(Context context) {
		File dir = context.getExternalFilesDir(null);
		if (dir == null) {
			return null;
		}
		return dir.getPath() + HikeConstants.STICKERS_ROOT;
	}

	private String getInternalStickerRootDirectory(Context context) {
		return context.getFilesDir().getPath() + HikeConstants.STICKERS_ROOT;
	}

	public Map<String, StickerCategory> getStickerToCategoryMapping(
			Context context) {
		String stickerRootDirectoryString = getStickerRootDirectory(context);

		/*
		 * Return null if the the path is null or empty
		 */
		if (TextUtils.isEmpty(stickerRootDirectoryString)) {
			return null;
		}

		File stickerRootDirectory = new File(stickerRootDirectoryString);

		/*
		 * Return null if the directory is null or does not exist
		 */
		if (stickerRootDirectory == null || !stickerRootDirectory.exists()) {
			return null;
		}

		Map<String, StickerCategory> stickerToCategoryMap = new HashMap<String, StickerCategory>();

		for (File stickerCategoryDirectory : stickerRootDirectory.listFiles()) {
			/*
			 * If this is not a directory we have no need for this file.
			 */
			if (!stickerCategoryDirectory.isDirectory()) {
				continue;
			}

			File stickerCategorySmallDirectory = new File(
					stickerCategoryDirectory.getAbsolutePath()
							+ HikeConstants.SMALL_STICKER_ROOT);

			/*
			 * We also don't want to do anything if the category does not have a
			 * small folder.
			 */
			if (stickerCategorySmallDirectory == null
					|| !stickerCategorySmallDirectory.exists()) {
				continue;
			}
			StickerCategory stickerCategory = stickerCategoriesMap.get(stickerCategoryDirectory.getName());
			if(stickerCategory == null)
			{
				stickerCategory = new StickerCategory(stickerCategoryDirectory.getName());
			}
			for (File stickerFile : stickerCategorySmallDirectory.listFiles()) {
				stickerToCategoryMap.put(stickerFile.getName(), stickerCategory);
			}
		}

		return stickerToCategoryMap;
	}
	
	/**
	 * solves recent sticker proguard issue , we serialize stickers , but proguard is changing file name sometime and recent sticker deserialize fails , 
	 * and we loose recent sticker file
	 * 
	 * fix is : we read file , make recent sticker file as per new name and proguard has been changed so it will not obfuscate file name of Sticker
	 */
	public final void updateRecentStickerFile(SharedPreferences settings){
		Logger.i("recent", "Recent Sticker Save Mechanism started");
		// save to preference as we want to try correction logic only once
		Editor edit = settings.edit();
		edit.putBoolean(StickerManager.RECENT_STICKER_SERIALIZATION_LOGIC_CORRECTED, true);
		edit.commit();
		Map<String, StickerCategory> stickerCategoryMapping = getStickerToCategoryMapping(context);
		// we do not want to try more than once, any failure , lets ignore this process there after
		if(stickerCategoryMapping ==null){
			return;
		}
		BufferedReader bufferedReader = null;
		try{
			String filePath = getInternalStickerDirectoryForCategoryId(StickerManager.RECENT);
			File dir = new File(filePath);
			if(!dir.exists()){
				return;
			}
			File file = new File(dir,StickerManager.RECENT + ".bin");
			if(file.exists()){
				bufferedReader = new BufferedReader(new FileReader(file));
				String line = "";
				StringBuilder str = new StringBuilder();
				while((line = bufferedReader.readLine())!=null){
					str.append(line);
				}
				Set<Sticker> recentStickers = Collections.synchronizedSet(new LinkedHashSet<Sticker>());
				
				Pattern p = Pattern.compile("(\\d{3}_.*?\\.png.*?)");
				Matcher m = p.matcher(str);
				
				while(m.find()){
					String stickerId = m.group();
					Logger.i("recent", "Sticker id found is "+stickerId);
					Sticker st = new Sticker();
					st.setStickerData(-1, stickerId, stickerCategoryMapping.get(stickerId));
					recentStickers.add(st);
				}
				saveSortedListForCategory(StickerManager.RECENT);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			Logger.i("recent", "Recent Sticker Save Mechanism finished");
			if(bufferedReader!=null){
				try{
				bufferedReader.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public File saveLargeStickers(File stickerDir, String stickerId, String stickerData) throws IOException
	{
		File f = new File(stickerDir, stickerId);
		Utils.saveBase64StringToFile(f, stickerData);
		return f;
	}

	/*
	 * TODO this logic is temporary we yet need to change it
	 */
	public File saveLargeStickers(File largeStickerDir, String stickerId, Bitmap largeStickerBitmap) throws IOException
	{
		if (largeStickerBitmap != null)
		{
			File largeImage = new File(largeStickerDir, stickerId);
			BitmapUtils.saveBitmapToFile(largeImage, largeStickerBitmap);
			largeStickerBitmap.recycle();
			return largeImage;
		}
		return null;
	}
	
	public void saveSmallStickers(File smallStickerDir, String stickerId, File f) throws IOException
	{
		Bitmap small = HikeBitmapFactory.scaleDownBitmap(f.getAbsolutePath(), SIZE_IMAGE, SIZE_IMAGE, true, false);

		if (small != null)
		{
			File smallImage = new File(smallStickerDir, stickerId);
			BitmapUtils.saveBitmapToFile(smallImage, small);
			small.recycle();
		}
	}

	public static boolean moveHardcodedStickersToSdcard(Context context)
	{
		if(Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
		{
			return false;
		}
		boolean result = true;
		try
		{
			JSONObject jsonObj = new JSONObject(Utils.loadJSONFromAsset(context, STICKERS_JSON_FILE_NAME));
			JSONArray harcodedStickers = jsonObj.optJSONArray(HARCODED_STICKERS);
			for (int i=0; i<harcodedStickers.length(); i++)
			{
				JSONObject obj = harcodedStickers.optJSONObject(i);
				String categoryId = obj.getString(CATEGORY_ID);
				
				String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);
				if (directoryPath == null)
				{
					result = false;
					break;
				}

				Resources mResources = context.getResources();
				File largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
				File smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);

				if (!smallStickerDir.exists())
				{
					smallStickerDir.mkdirs();
				}
				if (!largeStickerDir.exists())
				{
					largeStickerDir.mkdirs();
				}
				
				Utils.makeNoMediaFile(largeStickerDir);
				Utils.makeNoMediaFile(smallStickerDir);
				
				JSONArray stickerIds = obj.getJSONArray(STICKER_IDS);
				JSONArray resourceIds = obj.getJSONArray(RESOURCE_IDS);
				
				for (int j=0; j<stickerIds.length(); j++)
				{
					String stickerId = stickerIds.optString(j);
					String resName = resourceIds.optString(j);
					int resourceId = mResources.getIdentifier(resName, "drawable", 
							   context.getPackageName());
					Bitmap stickerBitmap = HikeBitmapFactory.decodeBitmapFromResource(mResources, resourceId, Bitmap.Config.ARGB_8888);
					File f = StickerManager.getInstance().saveLargeStickers(largeStickerDir, stickerId, stickerBitmap);
					if(f != null)
					{
						StickerManager.getInstance().saveSmallStickers(smallStickerDir, stickerId, f);
					}
					else
					{
						Logger.i("StickerMananger", "moveHardcodedStickersToSdcard failed resName = "+resName+" not found");
						result = false;
					}
				}	
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		}
		
		return result;
	}
	
	public boolean moveStickerPreviewAssetsToSdcard()
	{
		if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
		{
			return false;
		}
		boolean result = true;
		try
		{
			JSONObject jsonObj = new JSONObject(Utils.loadJSONFromAsset(context, StickerManager.STICKERS_JSON_FILE_NAME));
			JSONArray stickerCategories = jsonObj.optJSONArray(StickerManager.STICKER_CATEGORIES);
			for (int i = 0; i < stickerCategories.length(); i++)
			{
				JSONObject obj = stickerCategories.optJSONObject(i);
				String categoryId = obj.optString(StickerManager.CATEGORY_ID);

				String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);
				if (directoryPath == null)
				{
					result = false;
					break;
				}

				File otherAssetsDir = getOtherAssetsStickerDirectory(directoryPath);

				String pallateIcon = obj.optString(StickerManager.PALLATE_ICON);
				String pallateIconSelected = obj.optString(StickerManager.PALLATE_ICON_SELECTED);
				String previewImage = obj.optString(StickerManager.PREVIEW_IMAGE);

				saveAssetToDirectory(otherAssetsDir, pallateIcon, StickerManager.PALLATE_ICON);
				saveAssetToDirectory(otherAssetsDir, pallateIconSelected, StickerManager.PALLATE_ICON_SELECTED);
				if (!TextUtils.isEmpty(previewImage))
				{
					saveAssetToDirectory(otherAssetsDir, previewImage, StickerManager.PREVIEW_IMAGE);
				}
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		}

		return result;
	}
	
	private void saveAssetToDirectory(File dir, String assetName, String fileName) throws IOException
	{
		int assetResId = context.getResources().getIdentifier(assetName, "drawable", context.getPackageName());
		Bitmap assetBitmap = HikeBitmapFactory.decodeBitmapFromResource(context.getResources(), assetResId, Bitmap.Config.ARGB_8888);
		if (assetBitmap != null)
		{
			File file = new File(dir, fileName + ".png");
			BitmapUtils.saveBitmapToFile(file, assetBitmap);
		}
	}

	public File getOtherAssetsStickerDirectory(String directoryPath)
	{
		File otherAssetsDir = new File(directoryPath + OTHER_STICKER_ASSET_ROOT);

		if (!otherAssetsDir.exists())
		{
			otherAssetsDir.mkdirs();
		}

		Utils.makeNoMediaFile(otherAssetsDir);
		return otherAssetsDir;
	}
	
	public List<StickerCategory> getMyStickerCategoryList()
	{
		ArrayList<StickerCategory> stickerCategories = new ArrayList<StickerCategory>(stickerCategoriesMap.values());
		Collections.sort(stickerCategories);
		ArrayList<StickerCategory> invisibleCategories = new ArrayList<StickerCategory>(HikeConversationsDatabase.getInstance().getAllStickerCategoriesWithVisibility(false).values());
		Collections.sort(invisibleCategories);
		stickerCategories.addAll(invisibleCategories);
		Iterator<StickerCategory> it = stickerCategories.iterator();
		while(it.hasNext())
		{
			StickerCategory sc = it.next();
			if(sc.isCustom())
			{
				it.remove();
			}
		}
		
		return stickerCategories;
	}
	
	public void saveVisibilityAndIndex(Set<StickerCategory> stickerCategories)
	{
		/**
		 * Removing invisible/Adding visible categories from the StickerCategory Map
		 */
		for(StickerCategory stickerCategory : stickerCategories)
		{
			if(!stickerCategory.isVisible())
			{
				stickerCategoriesMap.remove(stickerCategory.getCategoryId());
			}
			else
			{
				stickerCategoriesMap.put(stickerCategory.getCategoryId(), stickerCategory);
			}
		}
		
		HikeConversationsDatabase.getInstance().updateVisibilityAndIndex(stickerCategories);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
	}
	
	public int getNumColumnsForStickerGrid(Context context)
	{
		int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
		
		return ((int) (screenWidth/SIZE_IMAGE));
	}
	
	public void sucessFullyDownloadedStickers(Object resultObj)
	{
		Bundle b = (Bundle) resultObj;
		String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
		DownloadType downloadType = (DownloadType) b.getSerializable(StickerManager.STICKER_DOWNLOAD_TYPE);
		DownloadSource downloadSource = (DownloadSource) b.getSerializable(HikeConstants.DOWNLOAD_SOURCE);
		final boolean failedDueToLargeFile =b.getBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE);
		StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
		if(category == null)
		{
			return;
		}
		category.updateDownloadedStickersCount();
		if(downloadSource == DownloadSource.SHOP || downloadSource == DownloadSource.SETTINGS)
		{
			category.setState(StickerCategory.DONE_SHOP_SETTINGS);
		}
		else
		{
			category.setState(StickerCategory.DONE);
		}
		if (DownloadType.UPDATE.equals(downloadType))
		{
			StickerManager.getInstance().setStickerUpdateAvailable(categoryId, false);
			Intent i = new Intent(StickerManager.STICKERS_UPDATED);
			i.putExtra(CATEGORY_ID, categoryId);
			LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		}

		else if (DownloadType.MORE_STICKERS.equals(downloadType))
		{
			Intent i = new Intent(StickerManager.MORE_STICKERS_DOWNLOADED);
			i.putExtra(CATEGORY_ID, categoryId);
			LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		}
		
		else if (DownloadType.NEW_CATEGORY.equals(downloadType))
		{
			Intent i = new Intent(StickerManager.STICKERS_DOWNLOADED);
			i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
			LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		}
	}
	
	public void stickersDownloadFailed(Object resultObj)
	{
		Bundle b = (Bundle) resultObj;
		String categoryId = (String) b.getSerializable(StickerManager.CATEGORY_ID);
		StickerCategory category = StickerManager.getInstance().getCategoryForId(categoryId);
		if(category != null)
		{
			category.setState(StickerCategory.RETRY);  //Doing it here for safety. On orientation change, the stickerAdapter reference can become null, hence the broadcast won't be received there
		}
		Intent i = new Intent(StickerManager.STICKERS_FAILED);
		i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
		LocalBroadcastManager.getInstance(context).sendBroadcast(i);
	}
	
	public void onStickersDownloadProgress(Object resultObj)
	{
		Bundle b = (Bundle) resultObj;
		
		Intent i = new Intent(StickerManager.STICKERS_PROGRESS);
		i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
		LocalBroadcastManager.getInstance(context).sendBroadcast(i);
	}
	
	/**
	 * Returns a category preview {@link Bitmap}
	 * @param ctx
	 * @param categoryId
	 * @param downloadIfNotFound -- true if it should be downloaded if not found.
	 * @return {@link Bitmap}
	 */
	public Bitmap getCategoryOtherAsset(Context ctx, String categoryId, int type, boolean downloadIfNotFound)
	{
		String baseFilePath = getStickerDirectoryForCategoryId(categoryId) + OTHER_STICKER_ASSET_ROOT + "/";
		Bitmap bitmap = null;
		int defaultIconResId = 0;
		switch (type)
		{
		case PALLATE_ICON_TYPE:
			baseFilePath += PALLATE_ICON + OTHER_ICON_TYPE;
			bitmap = HikeBitmapFactory.decodeFile(baseFilePath);
			defaultIconResId = R.drawable.misc_sticker_placeholder;
			break;
		case PALLATE_ICON_SELECTED_TYPE:
			baseFilePath += PALLATE_ICON_SELECTED + OTHER_ICON_TYPE;
			bitmap = HikeBitmapFactory.decodeFile(baseFilePath);
			defaultIconResId = R.drawable.misc_sticker_placeholder_selected;
			break;
		case PREVIEW_IMAGE_TYPE:
			baseFilePath += PREVIEW_IMAGE + OTHER_ICON_TYPE;
			bitmap = HikeBitmapFactory.decodeFile(baseFilePath);
			defaultIconResId = R.drawable.shop_placeholder;
			break;
		default:
			break;
		}
		if (bitmap == null)
		{
			bitmap = HikeBitmapFactory.decodeResource(ctx.getResources(), defaultIconResId);
			if (downloadIfNotFound)
			{
				switch (type)
				{
				case PALLATE_ICON_TYPE:
				case PALLATE_ICON_SELECTED_TYPE:
					StickerDownloadManager.getInstance().DownloadEnableDisableImage(categoryId, null);
					break;
				case PREVIEW_IMAGE_TYPE:
					StickerDownloadManager.getInstance().DownloadStickerPreviewImage(categoryId, null);
					break;
				default:
					break;
				}
			}
		}

		return bitmap;
	}
	
	/**
	 * Generates StickerPageAdapterItemList based on the StickersList provided
	 * @param stickersList
	 * @return
	 */
	public List<StickerPageAdapterItem> generateStickerPageAdapterItemList(List<Sticker> stickersList)
	{
		List<StickerPageAdapterItem> stickerPageList = new ArrayList<StickerPageAdapterItem>();
		if(stickersList != null)
		{
			for (Sticker st : stickersList)
			{
				stickerPageList.add(new StickerPageAdapterItem(StickerPageAdapterItem.STICKER, st));
			}
		}
		return stickerPageList;
	}
	
	public JSONArray getAllInitialyInsertedStickerCategories()
	{
		JSONObject jsonObj;
		JSONArray jsonArray = new JSONArray();
		try
		{
			jsonObj = new JSONObject(Utils.loadJSONFromAsset(context, StickerManager.STICKERS_JSON_FILE_NAME));
			JSONArray stickerCategories = jsonObj.optJSONArray(StickerManager.STICKER_CATEGORIES);
			for (int i = 0; i < stickerCategories.length(); i++)
			{
				JSONObject obj = stickerCategories.optJSONObject(i);
				String categoryId = obj.optString(StickerManager.CATEGORY_ID);
				boolean isCustom = obj.optBoolean(StickerManager.IS_CUSTOM);
				if (!isCustom)
				{
					jsonArray.put(categoryId);
				}
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonArray;
	}

	public void checkAndDownLoadStickerData()
	{
		if (HikeSharedPreferenceUtil.getInstance(context).getData(StickerManager.STICKERS_SIZE_DOWNLOADED, false))
		{
			return;
		}

		StickerDownloadManager.getInstance().DownloadStickerSignupUpgradeTask(getAllInitialyInsertedStickerCategories(), new IStickerResultListener()
		{

			@Override
			public void onSuccess(Object result)
			{
				// TODO Auto-generated method stub
				JSONArray resultData = (JSONArray) result;
				updateStickerCategoriesMetadata(resultData);
				HikeSharedPreferenceUtil.getInstance(context).saveData(StickerManager.STICKERS_SIZE_DOWNLOADED, true);
			}

			@Override
			public void onProgressUpdated(double percentage)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void onFailure(Object result, StickerException exception)
			{
				// TODO Auto-generated method stub

			}
		});
	}
	
	public void updateStickerCategoriesMetadata(JSONArray jsonArray)
	{
		int length = jsonArray.length();
		List<StickerCategory> visibleStickerCategories = new ArrayList<StickerCategory>();
		int humanoidCategoryIndex = stickerCategoriesMap.get(HUMANOID).getCategoryIndex();
		for (int i = 0; i < length; i++)
		{
			JSONObject jsonObj  = jsonArray.optJSONObject(i);
			if(jsonObj != null)
			{
				String catId = jsonObj.optString(StickerManager.CATEGORY_ID);
				
				StickerCategory category = stickerCategoriesMap.get(catId);
				if(category == null)
				{
					category = new StickerCategory(catId);
				}
				
				if(jsonObj.has(HikeConstants.CAT_NAME))
				{
					category.setCategoryName(jsonObj.optString(HikeConstants.CAT_NAME, ""));
				}
				
				if (jsonObj.has(HikeConstants.VISIBLITY))
				{
					boolean isVisible = jsonObj.optInt(HikeConstants.VISIBLITY) == 1;
					category.setVisible(isVisible);
					if (category.isVisible())
					{
						stickerCategoriesMap.put(catId, category);
					}
					visibleStickerCategories.add(category);
					category.setCategoryIndex(humanoidCategoryIndex + visibleStickerCategories.size());
				}
				if (jsonObj.has(HikeConstants.NUMBER_OF_STICKERS))
				{
					category.setTotalStickers(jsonObj.optInt(HikeConstants.NUMBER_OF_STICKERS, 0));
				}
				
				if (jsonObj.has(HikeConstants.SIZE))
				{
					category.setCategorySize(jsonObj.optInt(HikeConstants.SIZE, 0));
				}
			}
		}
		if(!visibleStickerCategories.isEmpty())
		{
			//Updating category index for all other sticker categories as well
			for (StickerCategory stickerCategory : stickerCategoriesMap.values())
			{
				if(visibleStickerCategories.contains(stickerCategory) || stickerCategory.isCustom() || stickerCategory.getCategoryId().equals(HUMANOID))
				{
					continue;
				}
				int currentIndex = stickerCategory.getCategoryIndex();
				stickerCategory.setCategoryIndex(currentIndex + visibleStickerCategories.size());
			}
		}
		
		HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(stickerCategoriesMap.values());
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
	}
	
	public void initialiseDownloadStickerTask(StickerCategory category, DownloadSource source, Context context)
	{
		DownloadType downloadType = category.isUpdateAvailable() ? DownloadType.UPDATE : DownloadType.MORE_STICKERS;
		initialiseDownloadStickerTask(category, source, downloadType, context);
	}
	public void initialiseDownloadStickerTask(StickerCategory category, DownloadSource source, DownloadType downloadType, Context context)
	{
		if(stickerCategoriesMap.containsKey(category.getCategoryId()))
		{
			category = stickerCategoriesMap.get(category.getCategoryId());
		}
		if(category.getTotalStickers() == 0 || category.getDownloadedStickersCount() < category.getTotalStickers())
		{
			category.setState(StickerCategory.DOWNLOADING);
			StickerDownloadManager.getInstance().DownloadMultipleStickers(category, downloadType, source, null);
		}
		saveCategoryAsVisible(category);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);
	}

	private void saveCategoryAsVisible(StickerCategory category)
	{
		if (category.isVisible())
		{
			return;
		}
		category.setVisible(true);
		int catIdx = HikeConversationsDatabase.getInstance().getMaxStickerCategoryIndex();
		category.setCategoryIndex(catIdx == -1 ? stickerCategoriesMap.size() : (catIdx + 1));
		stickerCategoriesMap.put(category.getCategoryId(), category);
		HikeConversationsDatabase.getInstance().insertInToStickerCategoriesTable(category);
	}

	public boolean stickerShopUpdateNeeded()
	{
		long lastUpdateTime = HikeSharedPreferenceUtil.getInstance(context).getData(LAST_STICKER_SHOP_UPDATE_TIME, 0L);
		boolean updateNeeded = 	(lastUpdateTime + STICKER_SHOP_REFRESH_TIME) < System.currentTimeMillis();
		
		if(updateNeeded && HikeSharedPreferenceUtil.getInstance(context).getData(STICKER_SHOP_DATA_FULLY_FETCHED, true))
		{
			HikeSharedPreferenceUtil.getInstance(context).saveData(StickerManager.STICKER_SHOP_DATA_FULLY_FETCHED, false);
		}
		return lastUpdateTime + STICKER_SHOP_REFRESH_TIME < System.currentTimeMillis();
	}
	
	public boolean moreDataAvailableForStickerShop()
	{
		return !HikeSharedPreferenceUtil.getInstance(context).getData(STICKER_SHOP_DATA_FULLY_FETCHED, true);
	}
	
	public boolean isMinimumMemoryAvailable()
	{
		double freeSpace = Utils.getFreeSpace();
		
		Logger.d(TAG, "free space : " + freeSpace);
		if(freeSpace > MINIMUM_FREE_SPACE)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public String getCategoryOtherAssetLoaderKey(String categoryId, int type)
	{
		return categoryId + HikeConstants.DELIMETER + type;
	}
	
	public void checkAndSendAnalytics(boolean visible)
	{
		if(visible)
		{
			if (!HikeSharedPreferenceUtil.getInstance(context).getData(HikeMessengerApp.STICKER_SETTING_CHECK_BOX_CLICKED, false))
			{
				HikeSharedPreferenceUtil.getInstance(context).saveData(HikeMessengerApp.STICKER_SETTING_CHECK_BOX_CLICKED, true);
				Utils.sendUILogEvent(HikeConstants.LogEvent.STICKER_CHECK_BOX_CLICKED);
			}
		}
		else
		{
			if (!HikeSharedPreferenceUtil.getInstance(context).getData(HikeMessengerApp.STICKER_SETTING_UNCHECK_BOX_CLICKED, false))
			{
				HikeSharedPreferenceUtil.getInstance(context).saveData(HikeMessengerApp.STICKER_SETTING_UNCHECK_BOX_CLICKED, true);
				Utils.sendUILogEvent(HikeConstants.LogEvent.STICKER_UNCHECK_BOX_CLICKED);
			}
		}
	}
	
	/**
	 * This method is used for adding a new sticker category in pallete on the fly. The category is placed at a position in the pallete if specified, else at the end
	 * 
	 * @param categoryId
	 * @param categoryName
	 * @param stickerCount
	 * @param categorySize
	 * @param position
	 */
	public void addNewCategoryInPallete(StickerCategory stickerCategory)
	{
		if (stickerCategoriesMap.containsKey(stickerCategory.getCategoryId()))
		{
			/**
			 * Discard the add packet.
			 */
			return;
		}

		boolean isCategoryInserted = HikeConversationsDatabase.getInstance().insertNewCategoryInPallete(stickerCategory);
		/**
		 * If isCategoryInserted is false, we simply return, since it's a duplicate category
		 */
		if (!isCategoryInserted)
		{
			return;
		}

		ArrayList<StickerCategory> updateCategories = new ArrayList<StickerCategory>();
		/**
		 * Incrementing the index of other categories by 1 to accommodate the new category in between
		 */

		for (StickerCategory category : stickerCategoriesMap.values())
		{
			if (category.getCategoryIndex() < stickerCategory.getCategoryIndex())
			{
				continue;
			}

			category.setCategoryIndex(category.getCategoryIndex() + 1);
			updateCategories.add(category);
		}

		stickerCategoriesMap.put(stickerCategory.getCategoryId(), stickerCategory);
		HikeConversationsDatabase.getInstance().updateStickerCategoriesInDb(updateCategories);
		/**
		 * Now download the Enable disable images as well as preview image
		 */
		StickerDownloadManager.getInstance().DownloadEnableDisableImage(stickerCategory.getCategoryId(), null);
		StickerDownloadManager.getInstance().DownloadStickerPreviewImage(stickerCategory.getCategoryId(), null);

		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_CATEGORY_MAP_UPDATED, null);

	}
	
	/**
	 * To cater to a corner case, where server sent an update available packet, and before a user could download the new updates for a pack, the user received the new stickers. In
	 * that case, the updateAvailable flag still remains true for that category. Thus, we are removing it in case the count of stickers in folder == the actual stickers.
	 * 
	 * This method updates the sticker category object in memory as well as database.
	 * 
	 * Called from {@link SingleStickerDownloadTask#call()} if a sticker is downloaded successfully
	 */

	public void checkAndRemoveUpdateFlag(String categoryId)
	{
		StickerCategory category = getCategoryForId(categoryId);

		if (category == null)
		{
			category = HikeConversationsDatabase.getInstance().getStickerCategoryforId(categoryId);
		}

		if (category == null)
		{
			Logger.wtf(TAG, "No category found in db. Which sticker was being downloaded  : ? " + categoryId);
			return;
		}

		/**
		 * Proceeding only if a valid category is found
		 */
		
		if (shouldRemoveGreenDot(category))
		{
			category.setUpdateAvailable(false);
			
			if(category.getState() == StickerCategory.UPDATE)
			{
				category.setState(StickerCategory.NONE);
			}
			
			HikeConversationsDatabase.getInstance().saveUpdateFlagOfStickerCategory(category);
		}
	}
	
	/**
	 * Checks if category has updateAvailable flag as true and the total count of downloaded stickers in folder is same as those present in the category.
	 * 
	 * @param category
	 * @return
	 */
	private boolean shouldRemoveGreenDot(StickerCategory category)
	{
		if (category.isUpdateAvailable())
		{
			int stickerListSize = category.getStickerList().size();

			if (stickerListSize > 0 && stickerListSize == category.getTotalStickers())
			{
				return true;
			}
		}

		return false;
	}
	
	/**
	 * This method is used to remove legacy green dots where needed
	 */
	
	private void removeLegacyGreenDots()
	{
		List<StickerCategory> myStickersList = getMyStickerCategoryList();
		ArrayList<StickerCategory> updatedList = new ArrayList<StickerCategory>();

		if (myStickersList != null)
		{
			for (StickerCategory stickerCategory : myStickersList)
			{
				if (shouldRemoveGreenDot(stickerCategory))
				{
					stickerCategory.setUpdateAvailable(false);
					updatedList.add(stickerCategory);
				}
			}
			
			if (updatedList.size() > 0)
			{
				HikeConversationsDatabase.getInstance().saveUpdateFlagOfStickerCategory(updatedList);
			}
		}
	}
	
	/**
	 * This method is to update our sticker folder names from large/small to stickers_l and stickers_s.
	 * This is being done because some cleanmaster was cleaning large named folder content 
	 */
	public void updateStickerFolderNames()
	{
		File dir = context.getExternalFilesDir(null);
		if (dir == null)
		{
			return;
		}
		String rootPath = dir.getPath() + HikeConstants.STICKERS_ROOT;
		File stickersRoot = new File(rootPath);

		if (!stickersRoot.exists() || !stickersRoot.canRead())
		{
			Logger.d("StickerManager", "sticker root doesn't exit or is not readable");
			return;
		}

		File[] files = stickersRoot.listFiles();

		if (files == null)
		{
			Logger.d("StickerManager", "sticker root is not a directory");
			return;
		}

		// renaming large/small folders for all categories
		for (File categoryRoot : files)
		{
			File[] categoryAssetFiles = categoryRoot.listFiles();
			for (File categoryAssetFile : categoryAssetFiles)
			{
				if (categoryAssetFile.getName().equals(HikeConstants.OLD_LARGE_STICKER_FOLDER_NAME))
				{
					Logger.d("StickerManager", "changing large file name for : " + categoryRoot.getName() + "category");
					categoryAssetFile.renameTo(new File(categoryRoot + HikeConstants.LARGE_STICKER_ROOT));
				}
				else if (categoryAssetFile.getName().equals(HikeConstants.OLD_SMALL_STICKER_FOLDER_NAME))
				{
					Logger.d("StickerManager", "changing small file name for : " + categoryRoot.getName() + "category");
					categoryAssetFile.renameTo(new File(categoryRoot + HikeConstants.SMALL_STICKER_ROOT));
				}
			}

		}
	}
	
}