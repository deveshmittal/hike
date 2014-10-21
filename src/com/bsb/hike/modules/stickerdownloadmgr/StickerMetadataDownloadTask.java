package com.bsb.hike.modules.stickerdownloadmgr;

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

public class StickerMetadataDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String taskId;
	private String catId;
	private IStickerResultListener callback;
	private JSONObject resultObj;
	
	protected StickerMetadataDownloadTask(Handler handler, Context ctx, String taskId, StickerCategory category, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.handler = handler;
		this.taskId = taskId;
		context = ctx;
		this.catId = category.getCategoryId();
	}

	@Override
	public STResult call() throws Exception
	{
		
		try
		{
			String urlString = AccountUtils.base + "/metadata?catId=" + catId;
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/preview?catId=" + catId;
			}
			setDownloadUrl(urlString);

			JSONObject response = (JSONObject) download(null, StickerRequestType.METADATA);
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)) || !catId.equals(response.getString(HikeConstants.CATEGORY_ID)))
			{
				return STResult.DOWNLOAD_FAILED;
			}

			resultObj = response.getJSONObject(HikeConstants.DATA_2);

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
