package com.bsb.hike.modules.httpmgr;

import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.singleStickerDownloadBase;

import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
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
}
