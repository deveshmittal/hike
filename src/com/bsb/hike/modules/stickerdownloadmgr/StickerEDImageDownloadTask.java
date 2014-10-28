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
	
	protected StickerEDImageDownloadTask(Handler handler, Context ctx, String taskId, StickerCategory category, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.handler = handler;
		context = ctx;
		this.taskId = taskId;
		this.catId = category.getCategoryId();	
	}

	@Override
	public STResult call() throws Exception
	{
		String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, catId);
		if (dirPath == null)
		{
			return STResult.DOWNLOAD_FAILED;
		}
		
		String enableImagePath = dirPath + HikeConstants.OTHER_ROOT + "/" + HikeConstants.ENABLE_IMAGE;
		String disableImagePath = dirPath + HikeConstants.OTHER_ROOT + "/" + HikeConstants.DISABLE_IMAGE;
		
		FileOutputStream fos = null;
		try
		{
			File otherDir = new File(dirPath + HikeConstants.OTHER_ROOT);
			if (!otherDir.exists())
			{
				if (!otherDir.mkdirs())
				{
					return STResult.DOWNLOAD_FAILED;
				}
			}
			
			String urlString = AccountUtils.base + "/enable_disable?catId=" + catId + "&resId=" + Utils.getResolutionId();
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/enable_disable?catId=" + catId + "&resId=" + Utils.getResolutionId();
			}
			setDownloadUrl(urlString);
			
			JSONObject response = (JSONObject) download(null, StickerRequestType.ENABLE_DISABLE);
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)) || !catId.equals(response.getString(HikeConstants.CATEGORY_ID)))
			{
				return STResult.DOWNLOAD_FAILED;
			}

			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

			String enableImg = data.getString(HikeConstants.ENABLE_IMAGE);
			String disableImg = data.getString(HikeConstants.DISABLE_IMAGE);

			Utils.saveBase64StringToFile(new File(enableImagePath), enableImg);
			Utils.saveBase64StringToFile(new File(disableImagePath), disableImg);
			
		}
		catch(Exception e)
		{
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
