package com.bsb.hike.modules.httpmgr;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IPreProcessListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.utils.Utils;

import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.*;

public class HttpRequests
{
	public static RequestToken SingleStickerDownloadRequest(String stickerId, String categoryId, IPreProcessListener preProcessListener, IRequestListener requestListener)
	{
		RequestToken requestToken = new Request.Builder()
										.setUrl(singleStickerDownloadBase() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId())
										.setPreProcessListener(preProcessListener)
										.setRequestListener(requestListener)
										.build();
		return requestToken;
	}
}
