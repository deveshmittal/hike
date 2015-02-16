package com.bsb.hike.modules.httpmgr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.engine.HttpEngine;
import com.bsb.hike.modules.httpmgr.engine.RequestListenerNotifier;
import com.bsb.hike.modules.httpmgr.engine.RequestProcessor;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;

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
	 * Initializes the http manager with default {@link ClientOptions}
	 */
	public static void init()
	{
		init(null);
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
	public <T> void addRequest(Request<T> request)
	{
		addRequest(request, null);
	}

	/**
	 * Submits the request to {@link RequestProcessor} with client options
	 * 
	 * @param request
	 * @param options
	 */
	public <T> void addRequest(Request<T> request, ClientOptions options)
	{
		requestProcessor.addRequest(request, options);
	}

	/**
	 * Cancels the request
	 * 
	 * @param request
	 */
	public <T> void cancel(Request<T> request)
	{
		request.cancel();
	}
	
	/**
	 * Removes particular listener from list of listeners for a request
	 * 
	 * @param request
	 * @param listener
	 */
	public <T> void removeListener(Request<T> request, IRequestListener listener)
	{
		List<IRequestListener> listeners = new ArrayList<IRequestListener>(1);
		listeners.add(listener);
		
		request.removeRequestListeners(listeners);
	}
	
	/**
	 * Removes list of listeners from list of request listeners for a request
	 * 
	 * @param request
	 * @param listeners
	 */
	public <T> void removeListeners(Request<T> request, List<IRequestListener> listeners)
	{
		request.removeRequestListeners(listeners);
	}
	
	/**
	 * Determines whether a request is running or not
	 * @param request
	 * @return
	 */
	public <T> boolean isRequestRunning(Request<T> request)
	{
		return requestProcessor.isRequestRunning(request);
	}
}
