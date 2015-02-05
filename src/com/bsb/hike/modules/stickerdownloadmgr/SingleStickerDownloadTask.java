package com.bsb.hike.modules.stickerdownloadmgr;

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
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants.FTResult;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class SingleStickerDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String stkId;
	private String catId;
	private String taskId;
	String largeStickerFilePath;
	

	protected SingleStickerDownloadTask(Handler handler, Context ctx, String taskId, String stkId, String catId, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.stkId = stkId;
		this.catId = catId;
		this.handler = handler;
		this.context = ctx;
	}

	@Override
	public STResult call() throws Exception
	{
		String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(catId);
		
		if (dirPath == null)
		{
			setException(new StickerException(StickerException.DIRECTORY_NOT_EXISTS));
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory does not exist for task : " + taskId);
			return STResult.DOWNLOAD_FAILED;
		}
		
		String largeStickerDirPath = dirPath + HikeConstants.LARGE_STICKER_ROOT;
		largeStickerFilePath = largeStickerDirPath + "/" + stkId;
		String smallStickerDirPath = dirPath + HikeConstants.SMALL_STICKER_ROOT;
		String smallStickerFilePath = smallStickerDirPath + "/" + stkId;
		
		FileOutputStream fos = null;
		try
		{
			File largeDir = new File(dirPath + HikeConstants.LARGE_STICKER_ROOT);
			if (!largeDir.exists())
			{
				if (!largeDir.mkdirs())
				{
					setException(new StickerException(StickerException.DIRECTORY_NOT_CREATED));
					Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory not created for task : " + taskId);
					return STResult.DOWNLOAD_FAILED;
				}
			}
			File smallDir = new File(dirPath + HikeConstants.SMALL_STICKER_ROOT);
			if (!smallDir.exists())
			{
				if (!smallDir.mkdirs())
				{
					setException(new StickerException(StickerException.DIRECTORY_NOT_CREATED));
					Logger.e(StickerDownloadManager.TAG, "Sticker download failed directory not created for task : " + taskId);
					return STResult.DOWNLOAD_FAILED;
				}
			}
			
			String urlString = AccountUtils.base + "/stickers?catId=" + catId + "&stId=" + stkId + "&resId=" + Utils.getResolutionId();
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers?catId=" + catId + "&stId=" + stkId + "&resId=" + Utils.getResolutionId();
			}
			setDownloadUrl(urlString);
			
			Logger.d(StickerDownloadManager.TAG,  "Starting download task : " + taskId + " url : " + urlString );
			JSONObject response = (JSONObject) download(null, HttpRequestType.GET);
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
			{
				setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
				Logger.e(StickerDownloadManager.TAG, "Sticker download failed null or invalid response for task : " + taskId);
				return STResult.DOWNLOAD_FAILED;
			}
			Logger.d(StickerDownloadManager.TAG,  "Got response for download task : " + taskId + " response : " + response.toString());
			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

			String stickerData = data.getString(stkId);

			byte[] largeStickerByteArray = StickerManager.getInstance().saveLargeStickers(largeStickerDirPath, stkId, stickerData);

			boolean isDisabled = data.optBoolean(HikeConstants.DISABLED_ST);
			if (!isDisabled)
			{
				StickerManager.getInstance().saveSmallStickers(smallStickerDirPath, stkId, largeStickerByteArray);
			}
		}
		catch (StickerException e)
		{
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed for task : " + taskId, e);
			setException(e);
			return STResult.DOWNLOAD_FAILED;
		}
		catch (Exception e)
		{
			Logger.e(StickerDownloadManager.TAG, "Sticker download failed for task : " + taskId, e);
			setException(new StickerException(e));
			return STResult.DOWNLOAD_FAILED;
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
				setException(new StickerException(StickerException.ERROR_CLOSING_FILE));
				return STResult.DOWNLOAD_FAILED;
			}
		}
		
		StickerManager.getInstance().checkAndRemoveUpdateFlag(catId);
		return STResult.SUCCESS;
	}

	@Override
	protected void postExecute(STResult result)
	{
		// TODO Auto-generated method stub
		setResult(largeStickerFilePath);
		super.postExecute(result);
		
	}
}
