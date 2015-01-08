package com.bsb.hike.modules.httpmgr.request.listener;

import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.response.Response;

public interface IRequestListener
{
	void onRequestFailure(HttpException spiceException);

	void onRequestSuccess(Response result);
}
