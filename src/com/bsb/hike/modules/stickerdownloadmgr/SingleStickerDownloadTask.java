package com.bsb.hike.modules.stickerdownloadmgr;

import static com.bsb.hike.modules.httpmgr.HttpRequests.SingleStickerDownloadRequest;
import static com.bsb.hike.modules.httpmgr.exception.HttpException.REASON_CODE_OUT_OF_SPACE;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.request.listener.IPreProcessListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

public class SingleStickerDownloadTask extends BaseStickerDownloadTask
{

	private static final String TAG = "SingleStickerDownloadTask";

	private String stickerId;

	private String categoryId;

	private String largeStickerPath;

	private String smallStickerPath;

	protected SingleStickerDownloadTask(String taskId, String stickerId, String categoryId)
	{
		super(taskId);
		this.stickerId = stickerId;
		this.categoryId = categoryId;
		
		if (!StickerManager.getInstance().isMinimumMemoryAvailable())
		{
			onFailure(new HttpException(REASON_CODE_OUT_OF_SPACE));
			return;
		}
		
		RequestToken token = SingleStickerDownloadRequest(stickerId, categoryId, getPreProcessListener(), getRequestListener());
		token.execute();
	}

	private IPreProcessListener getPreProcessListener()
	{
		return new IPreProcessListener()
		{

			@Override
			public void doInBackground(RequestFacade facade)
			{
				String dirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(categoryId);

				if (dirPath == null)
				{
					Logger.e(TAG, "Sticker download failed directory does not exist");
					onFailure(null);
					return;
				}

				largeStickerPath = dirPath + HikeConstants.LARGE_STICKER_ROOT + "/" + stickerId;
				smallStickerPath = dirPath + HikeConstants.SMALL_STICKER_ROOT + "/" + stickerId;

				File largeDir = new File(dirPath + HikeConstants.LARGE_STICKER_ROOT);
				if (!largeDir.exists())
				{
					if (!largeDir.mkdirs())
					{
						Logger.e(TAG, "Sticker download failed directory not created");
						onFailure(null);
						return;
					}
				}
				File smallDir = new File(dirPath + HikeConstants.SMALL_STICKER_ROOT);
				if (!smallDir.exists())
				{
					if (!smallDir.mkdirs())
					{
						Logger.e(TAG, "Sticker download failed directory not created");
						onFailure(null);
						return;
					}
				}
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
					if(!Utils.isResponseValid(response))
					{
						Logger.e(TAG, "Sticker download failed null or invalid response");
						onFailure(null);
						return ;
					}
					
					JSONObject data = response.getJSONObject(HikeConstants.DATA_2);

					if (null == data)
					{
						Logger.e(TAG, "Sticker download failed null data");
						onFailure(null);
						return;
					}

					String stickerData = data.getString(stickerId);

					Utils.saveBase64StringToFile(new File(largeStickerPath), stickerData);

					boolean isDisabled = data.optBoolean(HikeConstants.DISABLED_ST);
					if (!isDisabled)
					{
						Bitmap thumbnail = HikeBitmapFactory.scaleDownBitmap(largeStickerPath, StickerManager.SIZE_IMAGE, StickerManager.SIZE_IMAGE, true, false);

						if (thumbnail != null)
						{
							File smallImage = new File(smallStickerPath);
							BitmapUtils.saveBitmapToFile(smallImage, thumbnail);
							thumbnail.recycle();
						}
					}
					StickerManager.getInstance().checkAndRemoveUpdateFlag(categoryId);
					onSuccess(null);
				}
				catch (JSONException ex)
				{
					Logger.e(TAG, "Sticker download Json Exception", ex);
					onFailure(ex);
					return;
				}
				catch (IOException ex)
				{
					Logger.e(TAG, "Sticker download Io Exception", ex);
					onFailure(ex);
					return;
				}
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Logger.e(TAG, "Sticker download failed :", httpException);
			}
		};
	}

	@Override
	void onSuccess(Object result)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_DOWNLOADED, null);
		super.onSuccess(result);
	}
	
	@Override
	void onFailure(Exception e)
	{
		(new File(largeStickerPath)).delete();
		super.onFailure(e);
	}
}
