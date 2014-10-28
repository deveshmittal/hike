package com.bsb.hike.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.adapters.StickerPageAdapter;
import com.bsb.hike.adapters.StickerPageAdapter.ViewType;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.StickerPageAdapterItem;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.DownloadType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadStickerTask extends StickerTaskBase
{

	private Context context;

	private StickerCategory category;

	private DownloadType downloadType;

	private StickerPageAdapter stickerPageAdapter;

	public DownloadStickerTask(Context context, StickerCategory cat, DownloadType downloadType, StickerPageAdapter st)
	{
		this.context = context;
		this.category = cat;
		this.downloadType = downloadType;
		this.stickerPageAdapter = st;
	}

	public DownloadType getDownloadType()
	{
		return downloadType;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected FTResult doInBackground(Void... params)
	{
		Logger.d(getClass().getSimpleName(), "CategoryId: " + category.getCategoryId());

		String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(category.getCategoryId());
		if (directoryPath == null)
		{
			return FTResult.DOWNLOAD_FAILED;
		}

		File largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
		File smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);
		int totalNumber = 0;
		boolean reachedEnd = false;

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

		try
		{

			JSONObject response = AccountUtils.downloadSticker(category.getCategoryId(), existingStickerIds);

			if (response == null)
			{
				return FTResult.DOWNLOAD_FAILED;
			}

			int length = response.toString().getBytes().length;

			if (length > Utils.getFreeSpace())
			{
				return FTResult.FILE_TOO_LARGE;
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
		catch (NetworkErrorException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return FTResult.DOWNLOAD_FAILED;
		}
		catch (IllegalStateException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return FTResult.DOWNLOAD_FAILED;
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), e);
			return FTResult.DOWNLOAD_FAILED;
		}

		HikeConversationsDatabase.getInstance().updateStickerCountForStickerCategory(category.getCategoryId(), totalNumber);
		return FTResult.SUCCESS;
	}

	@Override
	protected void onPostExecute(FTResult result)
	{
		StickerManager.getInstance().removeTask(category.getCategoryId());
		if (result != FTResult.SUCCESS)
		{
			Intent i = new Intent(StickerManager.STICKERS_FAILED);
			Bundle b = new Bundle();
			b.putSerializable(StickerManager.STICKER_CATEGORY, category);
			b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
			if(result == FTResult.FILE_TOO_LARGE)
			{
				b.putBoolean(StickerManager.STICKER_DOWNLOAD_FAILED_FILE_TOO_LARGE,true);
			}
			i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
			LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		}
		else
		{
			if (DownloadType.UPDATE.equals(downloadType) && stickerPageAdapter != null)
			{
				StickerManager.getInstance().setStickerUpdateAvailable(category.getCategoryId(), false);
				category.setState(StickerCategory.DONE);
				List<StickerPageAdapterItem> l = stickerPageAdapter.getStickerPageAdapterItemList();
				l.remove(0);
				stickerPageAdapter.notifyDataSetChanged();
				Intent i = new Intent(StickerManager.STICKERS_UPDATED);
				LocalBroadcastManager.getInstance(context).sendBroadcast(i);
			}

			else if (DownloadType.MORE_STICKERS.equals(downloadType) && stickerPageAdapter != null)
			{
				List<StickerPageAdapterItem> l = stickerPageAdapter.getStickerPageAdapterItemList();
				category.setState(StickerCategory.DONE);
				l.remove(0);
				stickerPageAdapter.notifyDataSetChanged();
			}
			else if (DownloadType.NEW_CATEGORY.equals(downloadType))
			{
				Intent i = new Intent(StickerManager.STICKERS_DOWNLOADED);
				Bundle b = new Bundle();
				b.putSerializable(StickerManager.STICKER_CATEGORY, category);
				b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
				i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
				LocalBroadcastManager.getInstance(context).sendBroadcast(i);
			}
		}
	}
}
