package com.bsb.hike.modules.httpmgr.interceptor;

import com.bsb.hike.modules.httpmgr.response.ResponseFacade;

public interface IResponseInterceptor
{
	void intercept(Chain chain);

	interface Chain
	{
		ResponseFacade getResponseFacade();

		void proceed();
	}
}
