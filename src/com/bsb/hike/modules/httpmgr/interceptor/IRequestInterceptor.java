package com.bsb.hike.modules.httpmgr.interceptor;

import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;

public interface IRequestInterceptor
{
	void intercept(Chain chain);

	interface Chain
	{
		RequestFacade getRequestFacade();

		void proceed();
	}
}
