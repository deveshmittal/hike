package com.bsb.hike.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadStickerTask extends StickerTaskBase
{

	public enum DownloadType
	{
		NEW_CATEGORY, UPDATE, MORE_STICKERS
	}

	private Context context;

	private StickerCategory category;

	private DownloadType downloadType;

	private StickerPageAdapter stickerPageAdapter;

	public static final int SIZE_IMAGE = (int) (80 * Utils.densityMultiplier);

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
		Logger.d(getClass().getSimpleName(), "CategoryId: " + category.categoryId.name());

		String directoryPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, category.categoryId.name());
		if (directoryPath == null)
		{
			return FTResult.DOWNLOAD_FAILED;
		}

		File largeStickerDir = new File(directoryPath + HikeConstants.LARGE_STICKER_ROOT);
		File smallStickerDir = new File(directoryPath + HikeConstants.SMALL_STICKER_ROOT);
		int totalNumber = 0;
		boolean reachedEnd = false;

		JSONArray existingStickerIds = new JSONArray();

		/*
		 * If the category is the default one, we should add the default stickers as well.
		 */
		if (category.categoryId.equals(StickerCategoryId.humanoid))
		{
			for (String stickerId : StickerManager.getInstance().LOCAL_STICKER_IDS_HUMANOID)
			{
				existingStickerIds.put(stickerId);
			}
		}
		else if (category.categoryId.equals(StickerCategoryId.expressions))
		{
			for (String stickerId : StickerManager.getInstance().LOCAL_STICKER_IDS_EXPRESSIONS)
			{
				existingStickerIds.put(stickerId);
			}
		}

		if (smallStickerDir.exists())
		{
			String[] stickerIds = smallStickerDir.list();
			for (String stickerId : stickerIds)
			{
				existingStickerIds.put(stickerId);
				Logger.d(getClass().getSimpleName(), "Exising id: " + stickerId);
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

			JSONObject response = AccountUtils.downloadSticker(category.categoryId.name(), existingStickerIds);

			if (response == null)
			{
				return FTResult.DOWNLOAD_FAILED;
			}

			int length = response.toString().getBytes().length;

			if (length > Utils.getFreeSpace())
			{
				return FTResult.FILE_TOO_LARGE;
			}

			totalNumber = response.optInt(HikeConstants.NUMBER_OF_STICKERS, -1);
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
					if (downloadType.equals(DownloadType.MORE_STICKERS) || downloadType.equals(DownloadType.UPDATE) && stickerPageAdapter != null)
					{
						stickerPageAdapter.getStickerList().add(new Sticker(category, stickerId));
					}
					saveStickers(largeStickerDir, smallStickerDir, stickerId, stickerData);
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

		category.setReachedEnd(reachedEnd);
		HikeConversationsDatabase.getInstance().addOrUpdateStickerCategory(category.categoryId.name(), totalNumber, reachedEnd);
		return FTResult.SUCCESS;
	}

	private void saveStickers(File largeStickerDir, File smallStickerDir, String stickerId, String stickerData) throws IOException
	{
		File f = new File(largeStickerDir, stickerId);
		Utils.saveBase64StringToFile(f, stickerData);

		Bitmap small = HikeBitmapFactory.scaleDownBitmap(f.getAbsolutePath(), SIZE_IMAGE, SIZE_IMAGE, true, false);

		if (small != null)
		{
			File smallImage = new File(smallStickerDir, stickerId);
			BitmapUtils.saveBitmapToFile(smallImage, small);
			small.recycle();
		}
	}

	@Override
	protected void onPostExecute(FTResult result)
	{
		StickerManager.getInstance().removeTask(category.categoryId.name());
		if (result != FTResult.SUCCESS)
		{
			Intent i = new Intent(StickerManager.STICKERS_FAILED);
			Bundle b = new Bundle();
			b.putSerializable(StickerManager.STICKER_CATEGORY, category);
			b.putSerializable(StickerManager.STICKER_DOWNLOAD_TYPE, downloadType);
			i.putExtra(StickerManager.STICKER_DATA_BUNDLE, b);
			LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		}
		else
		{
			if (DownloadType.UPDATE.equals(downloadType) && stickerPageAdapter != null)
			{
				StickerManager.getInstance().setStickerUpdateAvailable(category.categoryId.name(), false);
				List<ViewType> l = stickerPageAdapter.getViewTypeList();
				l.remove(ViewType.UPDATING_STICKER);
				stickerPageAdapter.calculateNumRowsAndSize(true);
				stickerPageAdapter.notifyDataSetChanged();
				Intent i = new Intent(StickerManager.STICKERS_UPDATED);
				LocalBroadcastManager.getInstance(context).sendBroadcast(i);
			}

			else if (DownloadType.MORE_STICKERS.equals(downloadType) && stickerPageAdapter != null)
			{
				List<ViewType> l = stickerPageAdapter.getViewTypeList();
				l.remove(ViewType.DOWNLOADING_MORE);
				stickerPageAdapter.calculateNumRowsAndSize(true);
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
