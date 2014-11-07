package com.bsb.hike.modules.stickerdownloadmgr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerEDImageDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String taskId;
	private String catId;
	private String stkId;
	
	protected StickerEDImageDownloadTask(Handler handler, Context ctx, String taskId, String categoryId, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.handler = handler;
		context = ctx;
		this.taskId = taskId;
		this.catId = categoryId;
	}

	@Override
	public STResult call() throws Exception
	{
		String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(catId);
		if (dirPath == null)
		{
			setException(new StickerException(StickerException.DIRECTORY_NOT_EXISTS));
			return STResult.DOWNLOAD_FAILED;
		}
		
		String enableImagePath = dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT + "/" + StickerManager.PALLATE_ICON_SELECTED + StickerManager.PALETTE_ICON_TYPE;
		String disableImagePath = dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT + "/" + StickerManager.PALLATE_ICON + StickerManager.PALETTE_ICON_TYPE;
		
		FileOutputStream fos = null;
		try
		{
			File otherDir = new File(dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT);
			if (!otherDir.exists())
			{
				if (!otherDir.mkdirs())
				{
					setException(new StickerException(StickerException.DIRECTORY_NOT_CREATED));
					return STResult.DOWNLOAD_FAILED;
				}
			}
			
			String urlString = AccountUtils.base + "/stickers/enable_disable?catId=" + catId + "&resId=" + Utils.getResolutionId();
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers/enable_disable?catId=" + catId + "&resId=" + Utils.getResolutionId();
			}
			setDownloadUrl(urlString);
			
			Logger.d(StickerDownloadManager.TAG,  "Starting download task : " + taskId + " url : " + urlString );
			JSONObject response = (JSONObject) download(null, HttpRequestType.GET);
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)) || !catId.equals(response.getString(StickerManager.CATEGORY_ID)))
			{
				setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
				return STResult.DOWNLOAD_FAILED;
			}
			
			Logger.d(StickerDownloadManager.TAG,  "Got response for download task : " + taskId + " response : " + response.toString());
			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

			String enableImg = data.getString(HikeConstants.ENABLE_IMAGE);
			String disableImg = data.getString(HikeConstants.DISABLE_IMAGE);

			Utils.saveBase64StringToFile(new File(enableImagePath), enableImg);
			Utils.saveBase64StringToFile(new File(disableImagePath), disableImg);
			
		}
		catch(Exception e)
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
		return STResult.SUCCESS;
	}
	
	@Override
	protected void postExecute(STResult result)
	{
		// TODO Auto-generated method stub
		super.postExecute(result);
	}
}
