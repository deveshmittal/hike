package com.bsb.hike.modules.httpmgr.response;

import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;

/**
 * This interface is used so that callers don't have direct access to response object. This interface is implemented by the {@link Response} and {@link ResponseFacade}
 * 
 * @author sidharth
 * 
 */
public interface IResponseFacade
{
	public Pipeline<IResponseInterceptor> getResponseInterceptors();
}
