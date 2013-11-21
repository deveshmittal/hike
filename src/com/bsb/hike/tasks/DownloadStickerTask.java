package com.bsb.hike.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadStickerTask extends StickerTaskBase {

	public static enum DownloadType {
		NEW_CATEGORY, UPDATE, MORE_STICKERS
	}

	private Context context;
	private String catId;
	private int categoryIndex;
	private DownloadType downloadType;

	public DownloadStickerTask(Context context, int categoryIndex,
			DownloadType downloadType) {
		this.context = context;
		this.categoryIndex = categoryIndex;
		this.catId = Utils.getCategoryIdForIndex(categoryIndex);
		this.downloadType = downloadType;
	}

	public DownloadType getDownloadType() {
		return downloadType;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected FTResult doInBackground(Void... params) {
		Log.d(getClass().getSimpleName(), "CategoryId: " + catId);

		String directoryPath = Utils.getStickerDirectoryForCategoryId(context,
				catId);
		if (directoryPath == null) {
			return FTResult.DOWNLOAD_FAILED;
		}

		File largeStickerDir = new File(directoryPath
				+ HikeConstants.LARGE_STICKER_ROOT);
		File smallStickerDir = new File(directoryPath
				+ HikeConstants.SMALL_STICKER_ROOT);
		int totalNumber = 0;
		boolean reachedEnd = false;

		JSONArray existingStickerIds = new JSONArray();

		/*
		 * If the category is the default one, we should add the default
		 * stickers as well.
		 */
		if (categoryIndex == 1) {
			for (String stickerId : EmoticonConstants.LOCAL_STICKER_IDS_1) {
				existingStickerIds.put(stickerId);
			}
		} else if (categoryIndex == 2) {
			for (String stickerId : EmoticonConstants.LOCAL_STICKER_IDS_2) {
				existingStickerIds.put(stickerId);
			}
		}

		if (largeStickerDir.exists()) {
			String[] stickerIds = largeStickerDir.list();
			for (String stickerId : stickerIds) {
				existingStickerIds.put(stickerId);
				Log.d(getClass().getSimpleName(), "Exising id: " + stickerId);
			}
		} else {
			largeStickerDir.mkdirs();
			Log.d(getClass().getSimpleName(), "No existing sticker");
		}
		smallStickerDir.mkdirs();

		try {
			JSONObject response = AccountUtils.downloadSticker(catId,
					existingStickerIds);

			if (response == null) {
				return FTResult.DOWNLOAD_FAILED;
			}

			int length = response.toString().getBytes().length;

			if (length > Utils.getFreeSpace()) {
				return FTResult.FILE_TOO_LARGE;
			}

			totalNumber = response.optInt(HikeConstants.NUMBER_OF_STICKERS, -1);
			reachedEnd = response.optBoolean(HikeConstants.REACHED_STICKER_END);
			Log.d(getClass().getSimpleName(), "REached end? " + reachedEnd);
			Log.d(getClass().getSimpleName(), "Sticker count: " + totalNumber);
			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);
			for (Iterator<String> keys = data.keys(); keys.hasNext();) {
				String stickerId = keys.next();
				String stickerData = data.getString(stickerId);

				try {
					File f = new File(largeStickerDir, stickerId);
					Utils.saveBase64StringToFile(f, stickerData);

					Bitmap thumbnail = Utils.scaleDownImage(f.getPath(), -1,
							false);

					File smallImage = new File(smallStickerDir, stickerId);
					Utils.saveBitmapToFile(smallImage, thumbnail);
				} catch (FileNotFoundException e) {
					Log.w(getClass().getSimpleName(), e);
				} catch (IOException e) {
					Log.w(getClass().getSimpleName(), e);
				}
			}

		} catch (NetworkErrorException e) {
			Log.w(getClass().getSimpleName(), e);
			return FTResult.DOWNLOAD_FAILED;
		} catch (IllegalStateException e) {
			Log.w(getClass().getSimpleName(), e);
			return FTResult.DOWNLOAD_FAILED;
		} catch (JSONException e) {
			Log.w(getClass().getSimpleName(), e);
			return FTResult.DOWNLOAD_FAILED;
		}

		HikeConversationsDatabase.getInstance().addOrUpdateStickerCategory(
				catId, totalNumber, reachedEnd);
		return FTResult.SUCCESS;
	}

	@Override
	protected void onPostExecute(FTResult result) {
		HikeMessengerApp.stickerTaskMap.remove(catId);
		if (result != FTResult.SUCCESS) {
			HikeMessengerApp.getPubSub()
					.publish(
							HikePubSub.STICKER_CATEGORY_DOWNLOAD_FAILED,
							new Pair<Integer, DownloadType>(categoryIndex,
									downloadType));
			return;
		}
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.STICKER_CATEGORY_DOWNLOADED,
				new Pair<Integer, DownloadType>(categoryIndex, downloadType));
	}

}
