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
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadStickerTask extends StickerTaskBase {

	public static enum DownloadType {
		NEW_CATEGORY, UPDATE, MORE_STICKERS
	}

	private Context context;
	private String catId;
	private DownloadType downloadType;

	public DownloadStickerTask(Context context,
			DownloadType downloadType) {
		this.context = context;
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

		String directoryPath = Utils.getExternalStickerDirectoryForCatgoryId(
				context, catId);
		File stickerDir = new File(directoryPath);
		int totalNumber = 0;
		boolean reachedEnd = false;

		JSONArray existingStickerIds = new JSONArray();

		if (stickerDir.exists()) {
			String[] stickerIds = stickerDir.list();
			for (String stickerId : stickerIds) {
				existingStickerIds.put(stickerId);
				Log.d(getClass().getSimpleName(), "Exising id: " + stickerId);
			}
		} else {
			stickerDir.mkdirs();
			Log.d(getClass().getSimpleName(), "No existing sticker");
		}

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
			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);
			for (Iterator<String> keys = data.keys(); keys.hasNext();) {
				String stickerId = keys.next();
				String stickerData = data.getString(stickerId);

				try {
					File f = new File(stickerDir, stickerId);
					Utils.saveBase64StringToFile(f, stickerData);
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
}
