package com.bsb.hike.modules.httpmgr;

import java.util.List;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.interceptor.Pipeline;
import com.bsb.hike.modules.httpmgr.interceptor.IRequestInterceptor;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;

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
	
	public void addRequestListener(IRequestListener listener)
	{
		HttpManager.getInstance().addRequestListener(request, listener);
	}
	
	/**
	 * Removes particular listener from list of listeners for a request
	 * 
	 * @param listener
	 */
	public void removeListener(IRequestListener listener)
	{
		HttpManager.getInstance().removeListener(request, listener);
	}
	
	/**
	 * Removes list of listeners from list of request listeners for a request
	 * 
	 * @param listeners
	 */
	public void removeListeners(List<IRequestListener> listeners)
	{
		HttpManager.getInstance().removeListeners(request, listeners);
	}
	
	/**
	 * Determines whether a request is running or not
	 * @return true if request is already running
	 */
	public boolean isRequestRunning()
	{
		return HttpManager.getInstance().isRequestRunning(request);
	}

	public Pipeline<IRequestInterceptor> getRequestInterceptors()
	{
		return request.getRequestInterceptors();
	}
}