package com.bsb.hike.modules.httpmgr.hikehttp;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequestConstants.singleStickerDownloadBase;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.request.FileRequest;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.retry.DefaultRetryPolicy;
import com.bsb.hike.platform.content.PlatformContentConstants;
import com.bsb.hike.utils.Utils;

public class HttpRequests
{
	public static RequestToken SingleStickerDownloadRequest(String stickerId, String categoryId, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(singleStickerDownloadBase() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId())
				.setRequestListener(requestListener).build();
		return requestToken;
	}

	public static RequestToken platformZipDownloadRequest(String filePath, String url, IRequestListener requestListener)
	{

		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(url)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.setRetryPolicy(new DefaultRetryPolicy(3, 1000, 2.0f))
				.build();
		return requestToken;
	}
}
