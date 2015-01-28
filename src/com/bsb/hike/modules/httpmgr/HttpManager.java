package com.bsb.hike.modules.httpmgr;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.engine.HttpEngine;
import com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier;
import com.bsb.hike.modules.httpmgr.engine.RequestProcessor;
import com.bsb.hike.modules.httpmgr.request.Request;

/**
 * This class will be used for initialization by and outside world and for adding or canceling a request by {@link RequestToken}
 * 
 * @author anubhav & sidharth
 * 
 */
public class HttpManager
{
	private static volatile HttpManager _instance;

	private RequestProcessor requestProcessor;

	private HttpManager(ClientOptions options)
	{
		HttpEngine engine = new HttpEngine();
		RequestListenerNotifier notifier = new RequestListenerNotifier(engine);
		requestProcessor = new RequestProcessor(options, engine, notifier);
	}

	static HttpManager getInstance()
	{
		if (_instance == null)
		{
			throw new IllegalStateException("Http Manager not initialized");
		}
		return _instance;
	}

	/**
	 * Initializes the http manager with {@link ClientOptions} passed as parameter
	 * 
	 * @param options
	 */
	public static void init(ClientOptions options)
	{
		if (_instance == null)
		{
			synchronized (HttpManager.class)
			{
				if (_instance == null)
				{
					_instance = new HttpManager(options);
				}
			}
		}
	}

	/**
	 * Submits the request to {@link RequestProcessor}
	 * 
	 * @param request
	 */
	public void addRequest(Request request)
	{
		addRequest(request, null);
	}

	/**
	 * Submits the request to {@link RequestProcessor} with client options
	 * 
	 * @param request
	 * @param options
	 */
	public void addRequest(Request request, ClientOptions options)
	{
		requestProcessor.addRequest(request, options);
	}

	/**
	 * Cancels the request
	 * 
	 * @param request
	 */
	public void cancel(Request request)
	{
		request.cancel();
	}
}
