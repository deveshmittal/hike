package com.bsb.hike.modules.httpmgr.response;

import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;

/**
 * This object is passed in interceptor methods so that users can work on response objtained form the network
 * 
 * @author sidharth
 * 
 */
public class ResponseFacade implements IResponseFacade
{
	private Response response;

	public ResponseFacade(Response response)
	{
		this.response = response;
	}

	public Response getResponse()
	{
		return response;
	}

	@Override
	public Pipeline<IResponseInterceptor> getResponseInterceptors()
	{
		return response.getResponseInterceptors();
	}

}
