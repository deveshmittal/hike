package com.bsb.hike.modules.stickerdownloadmgr;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.adapters.StickerPageAdapter;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.NetworkHandler.NetworkType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;

public class StickerDownloadManager
{
	private Context context;

	RequestQueue queue;

	public static StickerDownloadManager _instance = null;

	private Handler handler;
	
	private NetworkHandler networkHandler;

	private StickerDownloadManager(Context ctx)
	{
		queue = new RequestQueue();
		context = ctx;
		handler = new Handler(context.getMainLooper());
		networkHandler = new NetworkHandler(ctx, queue);
	}

	public static StickerDownloadManager getInstance(Context context)
	{
		if (_instance == null)
		{
			synchronized (StickerDownloadManager.class)
			{
				if (_instance == null)
					_instance = new StickerDownloadManager(context.getApplicationContext());
			}
		}
		return _instance;
	}

	/*
	 * This function will close down the executor service, and usually be called after unlink or delete account
	 */
	public void shutDownAll()
	{
		queue.shutdown();
		_instance = null;
	}

	public void DownloadSingleSticker(Context context, String catId, String stkId, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.SINGLE, stkId, catId);
		if (queue.isTaskAlreadyExist(taskId))
		{
			return;
		}
		BaseStickerDownloadTask singleTask = new SingleStickerDownloadTask(handler, context, taskId, stkId, catId, callback);
		Request request = new Request(singleTask);
		queue.addTask(taskId, request);
	}

	public void DownloadMultipleStickers(StickerCategory cat, StickerConstants.DownloadType downloadType, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.MULTIPLE, null, cat.getCategoryId());
		if (queue.isTaskAlreadyExist(taskId))
		{
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new MultiStickerDownloadTask(handler, context, taskId, cat, downloadType, callback);
		Request request = new Request(stickerCategoryTask);
		queue.addTask(taskId, request);
	}
	
	public void DownloadStickerPreviewImage(Context context, StickerCategory cat, DownloadType downloadType, StickerPageAdapter st, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.PREVIEW, null, cat.getCategoryId());
		if (queue.isTaskAlreadyExist(taskId))
		{
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new StickerPreviewImageDownloadTask(handler, context, taskId, cat, callback);
		Request request = new Request(stickerCategoryTask);
		queue.addTask(taskId, request);
	}
	
	public void DownloadEnableDisableImage(Context context, StickerCategory cat, StickerPageAdapter st, IStickerResultListener callback)
	{
		String taskId = getTaskId(StickerRequestType.ENABLE_DISABLE, null, cat.getCategoryId());
		if (queue.isTaskAlreadyExist(taskId))
		{
			return;
		}
		BaseStickerDownloadTask stickerCategoryTask = new StickerEDImageDownloadTask(handler, context, taskId, cat, callback);
		Request request = new Request(stickerCategoryTask);
		queue.addTask(taskId, request);
	}

	public boolean isTaskAlreadyExists(StickerRequestType reqType, String stkId, String catId)
	{
		String taskId = getTaskId(reqType, stkId, catId);
		return queue.isTaskAlreadyExist(taskId);
	}

	protected void removeTask(String taskId)
	{
		queue.removeTask(taskId);
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

		return builder.toString();
	}
	
	NetworkType getNetworkType()
	{
		return networkHandler.getNetworkType();
	}
}
