package com.bsb.hike.modules.httpmgr.response;

import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;

public interface IResponseFacade
{
	public Pipeline<IResponseInterceptor> getResponseInterceptors();
}
