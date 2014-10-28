package com.bsb.hike.modules.stickerdownloadmgr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.adapters.StickerPageAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.NetworkHandler.NetworkType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.modules.stickerdownloadmgr.retry.DefaultRetryPolicy;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class MultiStickerDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private StickerCategory category;
	private StickerConstants.DownloadType downloadType;
	private StickerPageAdapter stickerPageAdapter;
	private int stickerDownloadSize;

	protected MultiStickerDownloadTask(Handler handler, Context ctx, String taskId, StickerCategory category, StickerConstants.DownloadType downloadType, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.category  = category;
		this.downloadType = downloadType;
		this.handler = handler;
		this.context = ctx;
	}

	@Override
	public STResult call() throws Exception
	{
		Logger.d(getClass().getSimpleName(), "CategoryId: " + category.getCategoryId());

		String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, category.getCategoryId());
		if (directoryPath == null)
		{
			setException(new StickerException(StickerException.DIRECTORY_NOT_EXISTS));
			return STResult.DOWNLOAD_FAILED;
		}

		File largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
		File smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);
		int totalNumber = 0;
		boolean reachedEnd = false;
		boolean retry = true;

		JSONArray existingStickerIds = new JSONArray();

		if (smallStickerDir.exists())
		{
			String[] stickerIds = smallStickerDir.list(StickerManager.getInstance().stickerFileFilter);
			for (String stickerId : stickerIds)
			{
				existingStickerIds.put(stickerId);
				Logger.d(getClass().getSimpleName(), "Existing id: " + stickerId);
			}
		}
		else
		{
			smallStickerDir.mkdirs();
			Logger.d(getClass().getSimpleName(), "No existing sticker");
		}
		if (!largeStickerDir.exists())
			largeStickerDir.mkdirs();

		Utils.makeNoMediaFile(largeStickerDir);
		Utils.makeNoMediaFile(smallStickerDir);
		while (!reachedEnd)
		{

			try
			{

				JSONObject request = new JSONObject();
				request.put(StickerManager.CATEGORY_ID, category.getCategoryId());
				request.put(HikeConstants.STICKER_IDS, existingStickerIds);
				request.put(HikeConstants.RESOLUTION_ID, Utils.getResolutionId());
				request.put(HikeConstants.NUMBER_OF_STICKERS, getStickerDownloadSize());

				String urlString = AccountUtils.base + "/stickers";
				if (AccountUtils.ssl)
				{
					urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers";
				}
				setDownloadUrl(urlString);
				JSONObject response = (JSONObject) download(request, StickerRequestType.MULTIPLE);

				if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
				{
					setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
					return STResult.DOWNLOAD_FAILED;
				}

				int length = response.toString().getBytes().length;

				if (length > Utils.getFreeSpace())
				{
					setException(new StickerException(StickerException.OUT_OF_SPACE));
					return STResult.FILE_TOO_LARGE;
				}

				totalNumber = response.optInt(HikeConstants.TOTAL_STICKERS, -1);
				reachedEnd = response.optBoolean(HikeConstants.REACHED_STICKER_END);
				Logger.d(getClass().getSimpleName(), "Reached end? " + reachedEnd);
				Logger.d(getClass().getSimpleName(), "Sticker count: " + totalNumber);
				JSONObject data = response.getJSONObject(HikeConstants.DATA_2);
				for (Iterator<String> keys = data.keys(); keys.hasNext();)
				{
					String stickerId = keys.next();
					String stickerData = data.getString(stickerId);
					existingStickerIds.put(stickerId);

					try
					{
						Sticker s = new Sticker(category, stickerId);
						if (downloadType.equals(DownloadType.MORE_STICKERS) || downloadType.equals(DownloadType.UPDATE) && stickerPageAdapter != null)
						{
							stickerPageAdapter.addSticker(s);
						}
						// some hack : seems server was sending stickers which already exist so it was leading to duplicate issue
						// so we save small sticker , if not present already

						File f = StickerManager.getInstance().saveLargeStickers(largeStickerDir, stickerId, stickerData);
						StickerManager.getInstance().saveSmallStickers(smallStickerDir, stickerId, f);
					}
					catch (FileNotFoundException e)
					{
						Logger.w(getClass().getSimpleName(), e);
					}
					catch (IOException e)
					{
						Logger.w(getClass().getSimpleName(), e);
					}
				}

			}
			catch (Exception e)
			{
				setException(new StickerException(e));
				return STResult.DOWNLOAD_FAILED;
			}

			if (getRetryPolicy() != null)
			{
				((DefaultRetryPolicy) getRetryPolicy()).reset();
			}
			if (getCallback() != null)
			{
				getCallback().onProgressUpdated(totalNumber);
			}
		}

		HikeConversationsDatabase.getInstance().updateStickerCountForStickerCategory(category.getCategoryId(), totalNumber);
		return STResult.SUCCESS;
	}

	@Override
	protected void postExecute(STResult result)
	{
		// TODO Auto-generated method stub
		Bundle b = new Bundle();
		b.putSerializable(StickerManager.STICKER_CATEGORY, category);
		b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
		if (result != STResult.SUCCESS)
		{
			if(result == STResult.FILE_TOO_LARGE)
			{
				b.putBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE,true);
			}
		}
		setResult(b);
		super.postExecute(result);
	}

	public int getStickerDownloadSize()
	{
		if(stickerDownloadSize !=0 )
		{
			return stickerDownloadSize;
		}
		else
		{
			NetworkType networkType = StickerDownloadManager.getInstance(context).getNetworkType();
			return networkType.getStickerDownloadSize();
			
		}
	}

	public void setStickerDownloadSize(int stickerDownloadSize)
	{
		this.stickerDownloadSize = stickerDownloadSize;
	}
}