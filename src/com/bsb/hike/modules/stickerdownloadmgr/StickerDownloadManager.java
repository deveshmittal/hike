package com.bsb.hike.modules.stickerdownloadmgr;

import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;

import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadSource;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.Logger;

public class StickerDownloadManager
{

	public static final String TAG = "StickerDownloadManager";
	
	public static StickerDownloadManager _instance;
	
	private ConcurrentHashMap<String, BaseStickerDownloadTask> stickerTaskMap;

	private StickerDownloadManager()
	{
		stickerTaskMap = new ConcurrentHashMap<String, BaseStickerDownloadTask>();
	}

	public static StickerDownloadManager getInstance()
	{
		if (_instance == null)
		{
			synchronized (StickerDownloadManager.class)
			{
				if (_instance == null)
				{
					_instance = new StickerDownloadManager();
				}
			}
		}
		return _instance;
		
	}

	/*
	 * This function will close down the executor service, and usually be called after unlink or delete account
	 */
	public void shutDownAll()
	{
		stickerTaskMap.clear();
		stickerTaskMap = null;
		_instance = null;
	}

	public void DownloadSingleSticker(String catId, String stkId)
	{
		String taskId = getTaskId(StickerRequestType.SINGLE, stkId, catId);
		if (isTaskAlreadyExists(taskId))
		{
			Logger.d(TAG, "Download SingleStickerTask for catId : " + catId + "  stkId : " + stkId + " alreadyExists");
			return;
		}
		BaseStickerDownloadTask singleStickerDownloadTask = new SingleStickerDownloadTask(taskId, stkId, catId);
		addTask(taskId, singleStickerDownloadTask);
	}

	public void DownloadMultipleStickers(StickerCategory cat, StickerConstants.DownloadType downloadType, DownloadSource source)
	{
		String taskId = getTaskId(StickerRequestType.MULTIPLE, null, cat.getCategoryId());
		if (isTaskAlreadyExists(taskId))
		{
			Logger.d(TAG, "DownloadMultipleStickers task for catId : " + cat.getCategoryId() +  " already exists");
			return;
		}
		BaseStickerDownloadTask multiStickerDownloadTask = new MultiStickerDownloadTask(taskId, cat, downloadType, source);
		addTask(taskId, multiStickerDownloadTask);
	}
	
	public void DownloadStickerPreviewImage(String categoryId)
	{
		String taskId = getTaskId(StickerRequestType.PREVIEW, null, categoryId);
		if (isTaskAlreadyExists(taskId))
		{
			Logger.d(TAG, "DownloadStickersPrevieImage task for catId : " + categoryId +  " already exists");
			return;
		}
		BaseStickerDownloadTask stickerPreviewImageDownloadTask = new StickerPreviewImageDownloadTask(taskId, categoryId);
		addTask(taskId, stickerPreviewImageDownloadTask);
	}
	
	public void DownloadStickerPalleteImage(String categoryId)
	{
		String taskId = getTaskId(StickerRequestType.ENABLE_DISABLE, null, categoryId);
		if (isTaskAlreadyExists(taskId))
		{
			Logger.d(TAG, "DownloadStickersEnableDisable task for catId : " + categoryId +  " already exists");
			return;
		}
		BaseStickerDownloadTask stickerPalleteImageDownloadTask = new StickerPalleteImageDownloadTask(taskId, categoryId);
		addTask(taskId, stickerPalleteImageDownloadTask);
	}
	
	public void DownloadStickerShopTask(int offset)
	{
		String taskId = getTaskId(StickerRequestType.SHOP, null, null);
		if (isTaskAlreadyExists(taskId))
		{
			Logger.d(TAG, "DownloadStickersShop task " + " already exists");
			return;
		}
		BaseStickerDownloadTask stickerShopDownloadTask = new StickerShopDownloadTask(taskId, offset);
		addTask(taskId, stickerShopDownloadTask);
	}
	
	public void DownloadStickerSignupUpgradeTask(JSONArray categoryList)
	{
		String taskId = getTaskId(StickerRequestType.SHOP, null, null);
		if (isTaskAlreadyExists(taskId))
		{
			Logger.d(TAG, "DownloadStickersSignupUpdrade task " + " already exists");
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new StickerSignupUpgradeDownloadTask(taskId, categoryList);
		addTask(taskId, stickerCategoryTask);
	}

	public boolean isTaskAlreadyExists(String taskId)
	{
		return stickerTaskMap.contains(taskId);
	}

	protected void addTask(String taskId, BaseStickerDownloadTask task)
	{
		stickerTaskMap.put(taskId, task);
	}
	
	protected void removeTask(String taskId)
	{
		stickerTaskMap.remove(taskId);
	}
	
	String getTaskId(StickerRequestType reqType, String stkId, String catId)
	{
		StringBuilder builder = new StringBuilder();

		if (reqType.getType() == StickerRequestType.SINGLE.getType())
		{
			builder.append(StickerRequestType.SINGLE.getLabel());
			builder.append("\\");
			builder.append(catId);
			builder.append("\\");
			builder.append(stkId);

		}
		else if (reqType.getType() == StickerRequestType.MULTIPLE.getType())
		{
			builder.append(StickerRequestType.MULTIPLE.getLabel());
			builder.append("\\");
			builder.append(catId);

		}
		else if (reqType.getType() == StickerRequestType.PREVIEW.getType())
		{
			builder.append(StickerRequestType.PREVIEW.getLabel());
			builder.append("\\");
			builder.append(catId);
			builder.append("\\");

		}
		else if (reqType.getType() == StickerRequestType.ENABLE_DISABLE.getType())
		{
			builder.append(StickerRequestType.ENABLE_DISABLE.getLabel());
			builder.append("\\");
			builder.append(catId);
			builder.append("\\");
		}
		else if (reqType.getType() == StickerRequestType.SIZE.getType())
		{
			builder.append(StickerRequestType.SIZE.getLabel());
			builder.append("\\");
			builder.append(catId);
			builder.append("\\");
		}
		else if (reqType.getType() == StickerRequestType.SIGNUP_UPGRADE.getType())
		{
			builder.append(StickerRequestType.SIGNUP_UPGRADE.getLabel());
			builder.append("\\");
		}
		else if (reqType.getType() == StickerRequestType.SHOP.getType())
		{
			builder.append(StickerRequestType.SHOP.getLabel());
			builder.append("\\");
		}

		return builder.toString();
	}
	
}
