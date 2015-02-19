package com.bsb.hike.tasks;

import static com.bsb.hike.modules.httpmgr.HttpRequests.profileImageLoaderRequest;

import java.io.File;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.ui.ProfileActivity;
import com.bsb.hike.utils.Utils;

public class ProfileImageLoader extends AsyncTaskLoader<Boolean>
{
	private String dirPath;

	private String filePath;

	private String key;

	private boolean isStatusImage;

	private RequestToken requestToken;
	
	private boolean resultValue;

	public ProfileImageLoader(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage)
	{
		this(context, id, fileName, hasCustomIcon, statusImage, null);
	}

	public ProfileImageLoader(Context context, String id, String fileName, boolean hasCustomIcon, boolean statusImage, String url)
	{
		super(context);

		this.isStatusImage = statusImage;
		this.key = id;

		this.dirPath = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		this.filePath = dirPath + "/" + Utils.getTempProfileImageFileName(key);

		requestToken = profileImageLoaderRequest(key, fileName, filePath, hasCustomIcon, statusImage, url, getRequestListener());
	}

	@Override
	protected void onStartLoading()
	{
		forceLoad();
	}

	@Override
	public Boolean loadInBackground()
	{

		File dir = new File(dirPath);
		if (!dir.exists())
		{
			if (!dir.mkdirs())
			{
				return Boolean.FALSE;
			}
		}
		requestToken.execute();
		return Boolean.valueOf(resultValue);
	}

	@Override
	public void onCanceled(Boolean data)
	{
		super.onCanceled(data);
		Utils.removeTempProfileImage(key);
	}

	@Override
	protected void onStopLoading()
	{
		cancelLoad();
		Utils.removeTempProfileImage(key);
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{

			@Override
			public void onRequestSuccess(Response result)
			{
				Utils.renameTempProfileImage(key);

				String keypp = key;

				if (!isStatusImage)
					keypp = key + ProfileActivity.PROFILE_PIC_SUFFIX;

				HikeMessengerApp.getLruCache().remove(keypp);
				HikeMessengerApp.getPubSub().publish(HikePubSub.PROFILE_IMAGE_DOWNLOADED, key);
				resultValue = true;
			}

			@Override
			public void onRequestProgressUpdate(float progress)
			{
				
			}

			@Override
			public void onRequestFailure(HttpException httpException)
			{
				Utils.removeTempProfileImage(key);
				resultValue = false;
			}
		};
	}

}