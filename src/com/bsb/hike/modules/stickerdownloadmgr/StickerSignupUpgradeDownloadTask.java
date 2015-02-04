package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.HttpRequests.StickerSignupUpgradeRequest;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;

class StickerSignupUpgradeDownloadTask extends BaseStickerDownloadTask
{
	
	private static final String TAG = "StickerSignupUpgradeDownloadTask";
	
	protected StickerSignupUpgradeDownloadTask(String taskId, JSONArray categoryList)
	{

		super(taskId);
		
		if(!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			onFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}
		
		JSONObject postObject = getPostObject(categoryList);

		if (null == postObject)
		{
			onFailure(null);
			return;
		}

		IRequestBody body = new JsonBody(postObject);
		RequestToken requestToken = StickerSignupUpgradeRequest(body, getRequestListener());
		requestToken.execute();
	}
	
	private JSONObject getPostObject(JSONArray categoryList)
	{
		JSONObject postObject = new JSONObject();
		
		try
		{
			if(categoryList != null && categoryList.length() != 0)
			{
				postObject.put(StickerManager.CATEGORY_IDS, categoryList);
				return postObject;
			}
			Logger.e(TAG, "Sticker download failed null or empty category list");
		}
		catch (JSONException e)
		{
			Logger.e(TAG, "Sticker download failed json exception", e);
			return null;
		}
		return null;
	}
	
	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{
			
			@Override
			public void onRequestSuccess(Response result)
			{
				try
				{	
					JSONObject response = (JSONObject) result.getBody().getContent();
					if(response == null)
					{
						Logger.e(TAG, "Sticker download failed null response");
						onFailure(null);
						return ;
					}
					
					Logger.d(StickerDownloadManager.TAG,  "Got response for download : " + response.toString());
					
					JSONArray resultData = response.optJSONArray(HikeConstants.DATA_2);
					if(null == resultData)
					{
						Logger.e(TAG, "Sticker download failed null data");
						onFailure(null);
						return;
					}
					
					onSuccess(resultData);
				}
				catch (Exception e)
				{
					onFailure(e);
				}
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{
				
			}
			
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				onFailure(httpException);
			}
		};
	}
	
	@Override
	void onSuccess(Object result)
	{
		super.onSuccess(result);
		
		JSONArray resultData = (JSONArray) result;
		StickerManager.getInstance().updateStickerCategoriesMetadata(resultData);
		HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.getInstance()).saveData(StickerManager.STICKERS_SIZE_DOWNLOADED, true);
	}

	@Override
	void onFailure(Exception e)
	{
		super.onFailure(e);
	}
}
