package com.bsb.hike.modules.httpmgr;

import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.bulkLastSeenUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.deleteAccountBase;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.editProfileAvatarBase;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.getAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.getGroupBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.getHikeJoinTimeBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.getStaticAvatarBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.getStatusBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.lastSeenUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.multiStickerDownloadUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.postAddressbookBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.postDeviceDetailsBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.postGreenBlueDetailsBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.postToSocialNetworkBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.preActivationBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.registerAccountBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.sendDeviceDetailBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.sendTwitterInviteBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.sendUserLogsInfoBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.setProfileUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.singleStickerDownloadBase;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.socialCredentialsBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.stickerPalleteImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.stickerPreviewImageDownloadUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.stickerShopDownloadUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.stickerSignupUpgradeUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.unlinkAccountBase;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.updateAddressbookBaseUrl;
import static com.bsb.hike.modules.httpmgr.HttpRequestConstants.validateNumberBaseUrl;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_HIGH;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_LONG;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;

import java.io.File;

import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.ByteArrayRequest;
import com.bsb.hike.modules.httpmgr.request.FileRequest;
import com.bsb.hike.modules.httpmgr.request.JSONObjectRequest;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.FileBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;
import com.bsb.hike.utils.Utils;

public class HttpRequests
{
	public static RequestToken SingleStickerDownloadRequest(String stickerId, String categoryId, IRequestInterceptor interceptor, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(singleStickerDownloadBase() + "?catId=" + categoryId + "&stId=" + stickerId + "&resId=" + Utils.getResolutionId())
				.setRequestListener(requestListener)
				.build();
		
		requestToken.getRequestInterceptors().addLast("sticker", interceptor);
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
	
	public static RequestToken StickerPalleteImageDownloadRequest(String categoryId, IRequestInterceptor interceptor, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerPalleteImageDownloadUrl() + "?catId=" + categoryId + "&resId=" + Utils.getResolutionId())
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setPriority(10) // Setting priority between sticker shop task and enable_disable icon task
				.build();
		requestToken.getRequestInterceptors().addLast("sticker", interceptor);
		return requestToken;
	}
	
	public static RequestToken StickerPreviewImageDownloadRequest(String categoryId, IRequestInterceptor interceptor, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(stickerPreviewImageDownloadUrl() + "?catId=" + categoryId + "&resId=" + Utils.getResolutionId())
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_SHORT)
				.build();
		requestToken.getRequestInterceptors().addLast("sticker", interceptor);
		return requestToken;
	}
	
	public static RequestToken MultiStickerDownloadRequest(IRequestInterceptor interceptor, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(multiStickerDownloadUrl())
				.post(null)  // will set it in interceptor method using request facade
				.setRequestListener(requestListener)
				.setRequestType(REQUEST_TYPE_LONG)
				.setPriority(PRIORITY_HIGH)
				.build();
		requestToken.getRequestInterceptors().addLast("sticker", interceptor);
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

	public static RequestToken sendSocialCredentialsRequest(String social, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(socialCredentialsBaseUrl() + social)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}

	public static RequestToken registerAccountRequest(IRequestBody body, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(registerAccountBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.setAsynchronous(false)
				.build();
		return requestToken;

	}

	public static RequestToken sendUserLogInfoRequest(String logKey, JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(sendUserLogsInfoBaseUrl() + logKey)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		return requestToken;
	}

	public static RequestToken downloadStatusImageRequest(String id, String filePath, IRequestListener requestListener)
	{
		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(getStatusBaseUrl() + "/" + id + "?only_image=true")
				.setFile(filePath)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.get()
				.build();
		return requestToken;
	}

	public static RequestToken downloadProfileImageRequest(String id, String filePath, boolean hasCustomIcon, boolean isGroupConvs, IRequestListener requestListener)
	{
		String url;
		if (hasCustomIcon)
		{
			if (isGroupConvs)
			{
				url = getGroupBaseUrl() + "/" + id + "/avatar?fullsize=1";
			}
			else
			{
				url = getAvatarBaseUrl() + "/" + id + "?fullsize=1";
			}
		}
		else
		{
			url = getStaticAvatarBaseUrl() + "/" + filePath;
		}
		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(url)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.get()
				.build();
		return requestToken;
	}

	public static RequestToken validateNumberRequest(IRequestBody body, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(validateNumberBaseUrl() + "?digits=4")
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.setAsynchronous(false)
				.build();
		return requestToken;
	}

	public static RequestToken setProfileRequest(IRequestBody body, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(setProfileUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.setAsynchronous(false)
				.build();
		return requestToken;
	}

	public static RequestToken downloadProtipRequest(String url, String filePath, IRequestListener requestListener)
	{
		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(url)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.get()
				.build();
		return requestToken;
	}

	public static RequestToken postToSocialNetworkRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(postToSocialNetworkBaseUrl())
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}

	public static RequestToken sendTwitterInviteRequest(JSONObject json, IRequestListener requestListener)
	{
		JsonBody body = new JsonBody(json);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(sendTwitterInviteBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		return requestToken;
	}

	public static RequestToken postAddressBookRequest(IRequestBody body, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(postAddressbookBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_LONG)
				.setRequestListener(requestListener)
				.post(body)
				.setAsynchronous(false)
				.build();
		return requestToken;
	}

	public static RequestToken updateAddressBookRequest(IRequestBody body, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(updateAddressbookBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.patch(body)
				.setAsynchronous(false)
				.build();
		return requestToken;
	}

	public static RequestToken sendDeviceDetailsRequest(IRequestBody body, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(sendDeviceDetailBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		return requestToken;
	}

	public static RequestToken sendPreActivationRequest(IRequestBody body, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(preActivationBaseUrl())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(body)
				.build();
		return requestToken;
	}

	public static RequestToken profileImageLoaderRequest(String id, String fileName, String filePath, boolean hasCustomIcon, boolean statusImage, String url, IRequestListener requestListener)
	{
		String urlString;
		
		if (TextUtils.isEmpty(url))
		{
			if (statusImage)
			{
				urlString = getStatusBaseUrl() + "/" + id + "?only_image=true";
			}
			else
			{
				boolean isGroupConversation = Utils.isGroupConversation(id);

				if (hasCustomIcon)
				{
					urlString = (isGroupConversation ? getGroupBaseUrl() + "/" + id + "/avatar" : getAvatarBaseUrl() + "/" + id)  + "?fullsize=1";
				}
				else
				{
					urlString = getStaticAvatarBaseUrl() + "/" + fileName;
				}
			}
		}
		else
		{
			urlString = url;
		}
		RequestToken requestToken = new FileRequest.Builder()
				.setUrl(urlString)
				.setFile(filePath)
				.setRequestListener(requestListener)
				.setAsynchronous(false)
				.get()
				.build();
		return requestToken;
	}
	
	public static RequestToken deleteSocialCreadentialsRequest(String social, IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(socialCredentialsBaseUrl() + social)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.delete()
				.build();
		return requestToken;
	}
	
	public static RequestToken deleteAccountRequest(IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(deleteAccountBase())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.delete()
				.build();
		return requestToken;
	}

	public static RequestToken unlinkAccountRequest(IRequestListener requestListener)
	{
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(unlinkAccountBase())
				.setRequestType(Request.REQUEST_TYPE_SHORT)
				.setRequestListener(requestListener)
				.post(null)
				.build();
		return requestToken;
	}
	
	public static RequestToken editProfileAvatarRequest(String filePath, IRequestListener requestListener)
	{
		File file = new File(filePath);
		FileBody body = new FileBody("application/json", file);
		RequestToken requestToken = new JSONObjectRequest.Builder()
				.setUrl(editProfileAvatarBase())
				.setRequestType(Request.REQUEST_TYPE_LONG)
				.setRequestListener(requestListener)
				.setResponseOnUIThread(true)
				.post(body)
				.build();
		return requestToken;
	}
}
