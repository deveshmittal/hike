package com.bsb.hike.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadStickerTask extends StickerTaskBase {

	private Context context;
	private String catId;

	public DownloadStickerTask(Context context, String catId) {
		this.context = context;
		this.catId = catId;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected FTResult doInBackground(Void... params) {
		Log.d(getClass().getSimpleName(), "CategoryId: " + catId);

		String directoryPath = Utils.getExternalStickerDirectoryForCatgoryId(
				context, catId);
		File stickerDir = new File(directoryPath);

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

			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);
			for (Iterator<String> keys = data.keys(); keys.hasNext();) {
				String stickerId = keys.next();
				String stickerData = data.getString(stickerId);
				stickerId = stickerId.replace("._", "");
				Log.d(getClass().getSimpleName(), "StickerId: " + stickerId);

				try {
					File f = new File(stickerDir, stickerId);
					f.createNewFile();
					FileOutputStream fos = new FileOutputStream(f);

					byte[] b = Base64.decode(stickerData, Base64.DEFAULT);
					if (b == null) {
						Log.d(getClass().getSimpleName(), "Null!!");
						continue;
					}
					fos.write(b);
					fos.flush();
					fos.close();

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

		return FTResult.SUCCESS;
	}
}
