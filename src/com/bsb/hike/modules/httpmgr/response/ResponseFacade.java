package com.bsb.hike.modules.httpmgr.response;

import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;

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
