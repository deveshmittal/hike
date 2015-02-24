package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.StickerSignupUpgradeRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerSignupUpgradeDownloadTask
{
	
	private static final String TAG = "StickerSignupUpgradeDownloadTask";
	
	private JSONArray categoryList;
	
	public StickerSignupUpgradeDownloadTask(JSONArray categoryList)
	{
		this.categoryList = categoryList;
	}
	
	public void execute()
	{
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

		long requestId = getRequestId();
		RequestToken requestToken = StickerSignupUpgradeRequest(requestId, postObject, getRequestListener());
		
		if(requestToken.isRequestRunning())
		{
			return ;
		}
		requestToken.execute();
	}
	
	private long getRequestId()
	{
		return StickerRequestType.SIGNUP_UPGRADE.getLabel().hashCode();
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
					if(!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null response");
						onFailure(null);
						return ;
					}
					
					Logger.d(TAG,  "Got response for download : " + response.toString());
					
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
	
	void onSuccess(Object result)
	{
		JSONArray resultData = (JSONArray) result;
		StickerManager.getInstance().updateStickerCategoriesMetadata(resultData);
		HikeSharedPreferenceUtil.getInstance(HikeMessengerApp.getInstance()).saveData(StickerManager.STICKERS_SIZE_DOWNLOADED, true);
	}

	void onFailure(Exception e)
	{
		Logger.e(TAG, "on failure, exception ", e);
	}
}
