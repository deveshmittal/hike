package com.bsb.hike.modules.httpmgr;

import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.bulkLastSeenUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.getHikeJoinTimeBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.getStatusBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.lastSeenUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.multiStickerDownloadUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.postDeviceDetailsBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.postGreenBlueDetailsBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.singleStickerDownloadBase;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.stickerPalleteImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.stickerPreviewImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.stickerShopDownloadUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.stickerSignupUpgradeUrl;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_HIGH;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_LONG;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;

import org.json.JSONObject;

import com.bsb.hike.modules.httpmgr.request.ByteArrayRequest;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IPreProcessListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;
import com.bsb.hike.utils.Utils;

public class HttpRequests
{
	public static RequestToken SingleStickerDownloadRequest(String stickerId, String categoryId, IPreProcessListener preProcessListener, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(singleStickerDownloadBase() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId())
				.setPreProcessListener(preProcessListener)
				.setRequestListener(requestListener)
				.build();
		return requestToken;
	}

	public static RequestToken StickerSignupUpgradeRequest(IRequestBody body, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerSignupUpgradeUrl())
				.post(body)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.build();
		return requestToken;
	}

	public static RequestToken StickerShopDownloadRequest(int offset, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerShopDownloadUrl() + "?offset=" + offset)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.setPriority(PRIORITY_HIGH)
				.build();
		return requestToken;
	}
	
	public static RequestToken StickerPalleteImageDownloadRequest(String categoryId, IPreProcessListener preProcessListener, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerPalleteImageDownloadUrl() + "?catId=" + categoryId + "&resId=" + Utils.getResolutionId())
				.setPreProcessListener(preProcessListener)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setPriority(10) // Setting priority between sticker shop task and enable_disable icon task
				.build();
		return requestToken;
	}
	
	public static RequestToken StickerPreviewImageDownloadRequest(String categoryId, IPreProcessListener preProcessListener, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerPreviewImageDownloadUrl() + "?catId=" + categoryId + "&resId=" + Utils.getResolutionId())
				.setPreProcessListener(preProcessListener)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.build();
		return requestToken;
	}
	
	public static RequestToken MultiStickerDownloadRequest(IPreProcessListener preProcessListener, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(multiStickerDownloadUrl())
				.post(null)  // will set it in preprocess listener method using request facade
				.setPreProcessListener(preProcessListener)
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setPriority(PRIORITY_HIGH)
				.build();
		return requestToken;
	}
	
	public static RequestToken LastSeenRequest(String msisdn, IRequestListener requestListener, IRetryPolicy retryPolicy)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
											   .setUrl(lastSeenUrl() + "/" + msisdn)
											   .setRetryPolicy(retryPolicy)
											   .setRequestListener(requestListener)
											   .setRequestType(REQUEST_TYPE_SHORT)
											   .setPriority(PRIORITY_HIGH)
											   .build();
		return requestToken;
	}
	
	public static RequestToken BulkLastSeenRequest(IRequestListener requestListener, IRetryPolicy retryPolicy)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
											   .setUrl(bulkLastSeenUrl())
											   .setRetryPolicy(retryPolicy)
											   .setRequestListener(requestListener)
											   .setRequestType(REQUEST_TYPE_SHORT)
											   .build();
		return requestToken;
	}
	
	public static RequestToken getHikeJoinTimeRequest(String msisdn, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getHikeJoinTimeBaseUrl() + msisdn)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.build();
		return requestToken;
	}
	
	public static RequestToken postStatusRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(getStatusBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}
	
	public static RequestToken deleteStatusRequest(String statusId, IRequestListener requestListener)
	{
		RequestToken requestToken = new ByteArrayRequest.Builder()
				.setUrl(getStatusBaseUrl() + "/" + statusId)
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.delete()
				.build();
		return requestToken;
	}
	
	public static RequestToken postDeviceDetailsRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(postDeviceDetailsBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		return requestToken;
	}
	
	public static RequestToken postGreenBlueDetailsRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(postGreenBlueDetailsBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		return requestToken;
	}
}
