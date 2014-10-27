package com.bsb.hike.modules.stickerdownloadmgr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerPreviewImageDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String taskId;
	private String catId;

	protected StickerPreviewImageDownloadTask(Handler handler, Context ctx, String taskId, StickerCategory category, IStickerResultListener callback)
	{
		super(handler, ctx, taskId , callback);
		this.handler = handler;
		this.taskId = taskId;
		context = ctx;
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
		
		String previewImagePath = dirPath + HikeConstants.OTHER_ROOT + HikeConstants.PREVIEW_IMAGE;
		
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
			
			String urlString = AccountUtils.base + "/preview?catId=" + catId + "&resId=" + Utils.getResolutionId();
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/preview?catId=" + catId + "&resId=" + Utils.getResolutionId();
			}
			setDownloadUrl(urlString);
			
			JSONObject response = (JSONObject) download(null, StickerRequestType.PREVIEW);
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)) || !catId.equals(response.getString(HikeConstants.CATEGORY_ID)))
			{
				return STResult.DOWNLOAD_FAILED;
			}

			JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

			String stickerData = data.getString(HikeConstants.PREVIEW_IMAGE);

			Utils.saveBase64StringToFile(new File(previewImagePath), stickerData);
			
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
			return STResult.DOWNLOAD_FAILED;
		}
		catch (MalformedURLException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid URL", e);
			return STResult.DOWNLOAD_FAILED;
		}
		catch (IOException e)
		{
			Logger.e(getClass().getSimpleName(), "Error while downloding file", e);
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
