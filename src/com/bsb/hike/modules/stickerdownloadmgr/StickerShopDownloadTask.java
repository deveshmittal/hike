package com.bsb.hike.modules.stickerdownloadmgr;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.STResult;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.HttpRequestType;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

class StickerShopDownloadTask extends BaseStickerDownloadTask
{
	
	private Handler handler;
	private Context context;
	private String taskId;
	private Bundle bundle;
	private long timeStamp;
	private Object resultObject;
	
	protected StickerShopDownloadTask(Handler handler, Context ctx, String taskId, long timeStamp, IStickerResultListener callback)
	{
		super(handler, ctx, taskId, callback);
		this.handler = handler;
		this.taskId = taskId;
		context = ctx;
		this.timeStamp = timeStamp;
	}

	@Override
	public STResult call() throws Exception
	{
		
		try
		{
			timeStamp = 0;
			String urlString = AccountUtils.base + "/stickers/shop?ts=" + timeStamp;
			if(AccountUtils.ssl)
			{
				urlString = AccountUtils.HTTPS_STRING + AccountUtils.host + "/v1" + "/stickers/shop?ts=" + timeStamp ;
			}
			
			setDownloadUrl(urlString);
			
			JSONObject request = new JSONObject();
			request.put(HikeConstants.TIMESTAMP, timeStamp);
			
			JSONObject response = (JSONObject) download(request, HttpRequestType.POST);
			
			if (response == null || !HikeConstants.OK.equals(response.getString(HikeConstants.STATUS)))
			{
				setException(new StickerException(StickerException.NULL_OR_INVALID_RESPONSE));
				return STResult.DOWNLOAD_FAILED;
			}
			JSONArray data = response.optJSONArray(HikeConstants.DATA_2);
			if(null == data)
			{
				setException(new StickerException(StickerException.NULL_DATA));
				return STResult.DOWNLOAD_FAILED;
			}
			resultObject = data;
		}
		catch (Exception e)
		{
			setException(new StickerException(e));
			return STResult.DOWNLOAD_FAILED;
		}
		return STResult.SUCCESS;
	}

	@Override
	protected void postExecute(STResult result)
	{
		// TODO Auto-generated method stub
		setResult(resultObject);
		super.postExecute(result);
	}
}

