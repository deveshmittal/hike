package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.HttpRequests.StickerShopDownloadRequest;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerShopDownloadTask
{
	private int offset;
	
	private final String TAG = "StickerShopDownloadTask";
	
	public StickerShopDownloadTask(int offset)
	{
		this.offset = offset;
	}
	
	public void execute()
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			onFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}

		long requestId = getRequestId();
		RequestToken requestToken = StickerShopDownloadRequest(requestId, offset, getRequestListener());
		
		if(requestToken.isRequestRunning())
		{
			return ;
		}
		requestToken.execute();
	}
	
	private long getRequestId()
	{
		return StickerRequestType.SHOP.getLabel().hashCode();
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

					if (!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null response");
						onFailure(null);
					}

					JSONArray resultData = response.optJSONArray(HikeConstants.DATA_2);
					if (null == resultData)
					{
						Logger.e(TAG, "Sticker download failed null data");
						onFailure(null);
					}
					
					onSuccess(resultData);
				}
				catch (Exception e)
				{
					onFailure(e);
					return;
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
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_SUCCESS, result);
	}
	
	void onFailure(Exception e)
	{
		Logger.e(TAG, "on failure, exception ", e);
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_SHOP_DOWNLOAD_FAILURE, e);
	}
}