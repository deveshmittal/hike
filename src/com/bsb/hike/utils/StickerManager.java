package com.bsb.hike.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
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

	public static final String STICKERS_FAILED = "st_failed";

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

	public static final String DOWNLOAD_PREF = "downloadPref";

	public static final String RECENT = "recent";

	public static final String DOGGY_CATEGORY = "doggy";

	private static final String EXPRESSIONS = "expressions";

	public static final String HUMANOID = "humanoid";
	
	public static final String OTHER_STICKER_ASSET_ROOT = "/other";

	public static final String PALLATE_ICON = "pallate_icon";

	public static final String PALLATE_ICON_SELECTED = "pallate_icon_selected";

	public static final String PREVIEW_IMAGE = "preview";
	
	private Map<String, StickerCategory> stickerCategoriesMap;
	
	public FilenameFilter stickerFileFilter = new FilenameFilter()
	{
		@Override
		public boolean accept(File file, String fileName)
		{
			return !".nomedia".equalsIgnoreCase(fileName);
		}
	};

	public Map<String, StickerTaskBase> stickerTaskMap;

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
		stickerCategoriesMap = Collections.synchronizedMap(new LinkedHashMap<String, StickerCategory>());
		if (stickerTaskMap == null)
		{
			stickerTaskMap = new HashMap<String, StickerTaskBase>();
		}
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
			else
			{
				HikeConversationsDatabase.getInstance().removeStickerCategory(DOGGY_CATEGORY);
			}
		}
	}

	public List<StickerCategory> getStickerCategoryList()
	{
		List<StickerCategory> stickerCategoryList = new ArrayList<StickerCategory>(stickerCategoriesMap.values());
		Collections.sort(stickerCategoryList);
		return stickerCategoryList;
	}

	public void setupStickerCategoryList(SharedPreferences preferences)
	{
		/*
		 * TODO : This will throw an exception in case of remove category as, this function will be called from mqtt thread and stickerCategories will be called from UI thread
		 * also.
		 */
		stickerCategoriesMap.putAll(HikeConversationsDatabase.getInstance().getAllStickerCategoriesWithVisibility(true));
	}

	public void removeCategory(String removedCategoryId)
	{
		HikeConversationsDatabase.getInstance().removeStickerCategory(removedCategoryId);
		StickerCategory cat = stickerCategoriesMap.remove(removedCategoryId);
		if(!cat.isCustom())
		{
			String categoryDirPath = getStickerDirectoryForCategoryId(context, removedCategoryId);
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
		String categoryDirPath = getStickerDirectoryForCategoryId(context, categoryId);
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
		stickerCategoriesMap.get(categoryId).setUpdateAvailable(updateAvailable);
		HikeConversationsDatabase.getInstance().stickerUpdateAvailable(categoryId, updateAvailable);
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
		File stickerDir = new File(getInternalStickerDirectoryForCategoryId(catId));
		Logger.d(TAG, "Checking Internal Storage dir : " + stickerDir.getAbsolutePath());
		if (stickerDir.exists())
		{
			Logger.d(TAG, "Internal Storage dir exist so returning it.");
			return stickerDir.getPath();
		}
		if (externalAvailable)
		{
			Logger.d(TAG, "Returning external storage dir.");
			return getExternalStickerDirectoryForCategoryId(context, catId);
		}
		Logger.d(TAG, "Returning internal storage dir.");
		return getInternalStickerDirectoryForCategoryId(catId);
	}

	public boolean checkIfStickerCategoryExists(String categoryId)
	{
		String path = getStickerDirectoryForCategoryId(context, categoryId);
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

	public StickerCategory getCategoryForId(String categoryId)
	{
		return stickerCategoriesMap.get(categoryId);
	}

	public void saveCustomCategories()
	{
		saveSortedListForCategory(RECENT);
	}
	
	public void saveSortedListForCategory(String catId)
	{
		Set<Sticker> list = ((CustomStickerCategory) stickerCategoriesMap.get(catId)).getStickerSet();
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
		String recentsDir = getStickerDirectoryForCategoryId(context, StickerManager.RECENT);
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
		
		try
		{
			JSONObject jsonObj = new JSONObject(Utils.loadJSONFromAsset(context, STICKERS_JSON_FILE_NAME));
			JSONArray harcodedStickers = jsonObj.optJSONArray(HARCODED_STICKERS);
			for (int i=0; i<harcodedStickers.length(); i++)
			{
				JSONObject obj = harcodedStickers.optJSONObject(i);
				String categoryId = obj.getString(CATEGORY_ID);
				
				String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, categoryId);
				if (directoryPath == null)
				{
					return false;
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
						return false;
					}
				}	
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean moveStickerPreviewAssetsToSdcard()
	{
		if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
		{
			return false;
		}

		try
		{
			JSONObject jsonObj = new JSONObject(Utils.loadJSONFromAsset(context, StickerManager.STICKERS_JSON_FILE_NAME));
			JSONArray stickerCategories = jsonObj.optJSONArray(StickerManager.STICKER_CATEGORIES);
			for (int i = 0; i < stickerCategories.length(); i++)
			{
				JSONObject obj = stickerCategories.optJSONObject(i);
				String categoryId = obj.optString(StickerManager.CATEGORY_ID);

				String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, categoryId);
				if (directoryPath == null)
				{
					return false;
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
			return false;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;
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
		stickerCategories.addAll(HikeConversationsDatabase.getInstance().getAllStickerCategoriesWithVisibility(false).values());
		Iterator<StickerCategory> it = stickerCategories.iterator();
		while(it.hasNext())
		{
			StickerCategory sc = it.next();
			if(sc.isCustom())
			{
				it.remove();
			}
		}
		Collections.sort(stickerCategories);
		return stickerCategories;
	}
}
