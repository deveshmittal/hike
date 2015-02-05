package com.bsb.hike.modules.httpmgr;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.utils.AccountUtils;

public final class DefaultHeaders
{

	private static List<Header> getDefaultHeaders()
	{
		List<Header> headers = new ArrayList<Header>(2);

		if (AccountUtils.appVersion != null)
		{
			headers.add(new Header("User-Agent", "android-" + AccountUtils.appVersion));
		}
		if (AccountUtils.mToken != null && AccountUtils.mUid != null)
		{
			headers.add(new Header("Cookie", "user=" + AccountUtils.mToken + "; UID=" + AccountUtils.mUid));
		}
		return headers;
	}

	public static <T> void applyDefaultHeaders(Request<T> request)
	{
		request.addHeaders(getDefaultHeaders());
	}

	private DefaultHeaders()
	{

	}
}
