package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;
import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.StickerPreviewImageDownloadRequest;

import java.io.File;

import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.stickerdownloadmgr.StickerConstants.StickerRequestType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class StickerPreviewImageDownloadTask
{
	private String TAG = "StickerPreviewImageDownloadTask";

	private String categoryId;

	String previewImagePath;

	public StickerPreviewImageDownloadTask(String categoryId)
	{
		this.categoryId = categoryId;
	}

	public void execute()
	{
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			onFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}

		long requestId = getRequestId();
		RequestToken requestToken = StickerPreviewImageDownloadRequest(requestId, categoryId, getRequestInterceptor(), getRequestListener());

		if (requestToken.isRequestRunning()) // duplicate check
		{
			return;
		}
		requestToken.execute();
	}

	private long getRequestId()
	{
		return (StickerRequestType.PREVIEW.getLabel() + "\\" + categoryId).hashCode();
	}

	private IRequestInterceptor getRequestInterceptor()
	{
		return new IRequestInterceptor()
		{

			@Override
			public void intercept(Chain chain)
			{
				String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);
				if (dirPath == null)
				{
					Logger.e(TAG, "Sticker download failed directory does not exist");
					onFailure(null);
					return;
				}

				previewImagePath = dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT + "/" + StickerManager.PREVIEW_IMAGE + StickerManager.OTHER_ICON_TYPE;

				File otherDir = new File(dirPath + StickerManager.OTHER_STICKER_ASSET_ROOT);
				if (!otherDir.exists())
				{
					if (!otherDir.mkdirs())
					{
						Logger.e(TAG, "Sticker download failed directory not created");
						onFailure(null);
						return;
					}
				}
				chain.proceed();
			}
		};
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
						Logger.e(TAG, "Sticker download failed null or invalid response");
						onFailure(null);
						return;
					}
					Logger.d(TAG, "Got response for download task " + response.toString());
					JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

					if (null == data)
					{
						Logger.e(TAG, "Sticker download failed null data");
						onFailure(null);
						return;
					}

					String stickerData = data.getString(HikeConstants.PREVIEW_IMAGE);
					HikeMessengerApp.getLruCache().remove(StickerManager.getInstance().getCategoryOtherAssetLoaderKey(categoryId, StickerManager.PREVIEW_IMAGE_TYPE));
					Utils.saveBase64StringToFile(new File(previewImagePath), stickerData);
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

	}

	void onFailure(Exception e)
	{
		Logger.e(TAG, "on failure, exception ", e);
	}
}