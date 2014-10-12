package com.bsb.hike.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.Utils;

public class DownloadSingleStickerTask extends StickerTaskBase
{

	private String urlString;

	private String dirPath;

	private String largeStickerPath;

	private String smallStickerPath;

	private String key;

	private String stId;

	private String catId;

	public DownloadSingleStickerTask(Context context, String catId, String stId)
	{
		this.catId = catId;
		this.key = catId + stId;
		this.stId = stId;
		this.dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, catId);

		this.largeStickerPath = this.dirPath + HikeConstants.LARGE_STICKER_ROOT + "/" + stId;
		this.smallStickerPath = this.dirPath + HikeConstants.SMALL_STICKER_ROOT + "/" + stId;

		this.urlString = AccountUtils.base + "/stickers?catId=" + catId + "&stId=" + stId + "&resId=" + Utils.getResolutionId();
		if(AccountUtils.ssl){
			this.urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers?catId=" + catId + "&stId=" + stId + "&resId=" + Utils.getResolutionId();
		}
	}

	@Override
	protected FTResult doInBackground(Void... arg0)
	{
		if (dirPath == null)
		{
			return FTResult.DOWNLOAD_FAILED;
		}
		StickerManager stickerManager = StickerManager.getInstance();
		FileOutputStream fos = null;
		try
		{
			File largeDir = new File(dirPath + HikeConstants.LARGE_STICKER_ROOT);
			if (!largeDir.exists())
			{
				if (!largeDir.mkdirs())
				{
					return FTResult.DOWNLOAD_FAILED;
				}
			}
			File smallDir = new File(dirPath + HikeConstants.SMALL_STICKER_ROOT);
			if (!smallDir.exists())
			{
				if (!smallDir.mkdirs())
				{
					return FTResult.DOWNLOAD_FAILED;
				}
			}

			Logger.d(getClass().getSimpleName(), "Downloading sticker: " + urlString);
			URL url = new URL(urlString);

			URLConnection connection = url.openConnection();
			AccountUtils.addUserAgent(connection);
			connection.addRequestProperty("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid);

			Logger.d(getClass().getSimpleName(), "File size: " + connection.getContentLength());
			if (urlString.startsWith(AccountUtils.HTTPS_STRING) && AccountUtils.ssl)
			{
				((HttpsURLConnection) connection).setSSLSocketFactory(HikeSSLUtil.getSSLSocketFactory());
			}

			JSONObject response = AccountUtils.getResponse(connection.getInputStream());

			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
			{
				return FTResult.DOWNLOAD_FAILED;
			}

			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

			String stickerData = data.getString(stId);

			Utils.saveBase64StringToFile(new File(largeStickerPath), stickerData);

			boolean isDisabled = data.optBoolean(HikeConstants.DISABLED_ST);
			if (!isDisabled)
			{
				Bitmap thumbnail = HikeBitmapFactory.scaleDownBitmap(largeStickerPath, DownloadStickerTask.SIZE_IMAGE, DownloadStickerTask.SIZE_IMAGE, true,false);

				if (thumbnail != null)
				{
					File smallImage = new File(smallStickerPath);
					BitmapUtils.saveBitmapToFile(smallImage, thumbnail);
					thumbnail.recycle();
				}
			}
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
			return FTResult.DOWNLOAD_FAILED;
		}
		catch (MalformedURLException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid URL", e);
			return FTResult.DOWNLOAD_FAILED;
		}
		catch (IOException e)
		{
			Logger.e(getClass().getSimpleName(), "Error while downloding file", e);
			return FTResult.DOWNLOAD_FAILED;
		}
		finally
		{
			try
			{
				if (fos != null)
				{
					fos.close();
				}
			}
			catch (IOException e)
			{
				Logger.e(getClass().getSimpleName(), "Error while closing file", e);
				return FTResult.DOWNLOAD_FAILED;
			}
		}
		return FTResult.SUCCESS;
	}

	@Override
	protected void onPostExecute(FTResult result)
	{
		StickerManager.getInstance().removeTask(key);
		if (result != FTResult.SUCCESS)
		{
			(new File(largeStickerPath)).delete();
			return;
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_DOWNLOADED, null);
	}
}
