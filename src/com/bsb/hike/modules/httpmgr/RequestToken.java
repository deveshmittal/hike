package com.bsb.hike.modules.httpmgr;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.request.Request;

/**
 * Provides a mechanism for executing or canceling a http request without giving access to the request class outside the http manager
 * 
 * @author sidharth
 * 
 */
public class RequestToken
{
	private Request<?> request;

	public RequestToken(Request<?> request)
	{
		this.request = request;
	}

	/**
	 * 
	 */
	public void execute()
	{
		HttpManager.getInstance().addRequest(request);
	}
	
	public void execute(ClientOptions options)
	{
		HttpManager.getInstance().addRequest(request, options);
	}

	/**
	 * 
	 */
	public void cancel()
	{
		HttpManager.getInstance().cancel(request);
	}
	
	public boolean isRequestRunning()
	{
		return HttpManager.getInstance().isRequestRunning(request);
	}

	public Pipeline<IRequestInterceptor> getRequestInterceptors()
	{
		return request.getRequestInterceptors();
	}
}