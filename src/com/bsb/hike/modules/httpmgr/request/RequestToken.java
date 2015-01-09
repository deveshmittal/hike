package com.bsb.hike.modules.httpmgr.request;

/**
 * Provides a mechanism for executing or canceling a http request without giving access to the request class outside the http manager
 * 
 * @author sidharth
 * 
 */
public class RequestToken
{
	private Request request;

	RequestToken(Request request)
	{
		this.request = request;
	}

	/**
	 * 
	 */
	public void execute()
	{
		// TODO
	}

	/**
	 * 
	 */
	public void cancel()
	{
		// TODO
	}

}
