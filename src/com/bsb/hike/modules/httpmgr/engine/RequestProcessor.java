package com.bsb.hike.modules.httpmgr.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.log.LogFull;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IProgressListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestCancellationListener;

/**
 * This class handles the duplicate check of the request and then submits the request to {@link RequestRunner}
 * 
 * @author sidharth & anubhav
 * 
 */
public class RequestProcessor
{
	private Map<Long, Request<?>> requestMap;

	private RequestRunner requestRunner;

	private RequestListenerNotifier requestListenerNotifier;

	public RequestProcessor(ClientOptions options, HttpEngine engine, RequestListenerNotifier notifier)
	{
		this.requestMap = new ConcurrentHashMap<Long, Request<?>>();
		this.requestListenerNotifier = notifier;
		requestRunner = new RequestRunner(options, requestMap, engine, requestListenerNotifier);
	}

	/**
	 * Checks request is duplicate or not if yes then updates the list of request listeners with listeners in request object.Then submits the request to {@link RequestRunner} for
	 * further processing
	 * 
	 * @param request
	 * @param options
	 *            {@link ClientOptions} options to be used for executing this request
	 */
	public void addRequest(final Request<?> request, ClientOptions options)
	{
		long requestId = request.getId();
		if (requestMap.containsKey(requestId))
		{
			LogFull.i(request.toString() + " already exists");
			Request<?> req = requestMap.get(requestId);
			req.addRequestListeners(request.getRequestListeners());
		}
		else
		{
			LogFull.d("adding " + request.toString() + " to request map");
			requestMap.put(requestId, request);
			IRequestCancellationListener listener = new IRequestCancellationListener()
			{
				@Override
				public void onCancel()
				{
					LogFull.i("on cancel called for " + request.toString() + "  removing from request map");
					requestListenerNotifier.notifyListenersOfRequestCancellation(request);
					requestMap.remove(request.getId());
				}
			};
			request.setRequestCancellationListener(listener);

			IProgressListener progressListener = new IProgressListener()
			{
				@Override
				public void onProgressUpdate(float progress)
				{
					requestListenerNotifier.notifyListenersOfRequestProgress(request, progress);
				}
			};
			request.setProgressListener(progressListener);

			requestRunner.submit(request, options);
		}
	}

	/**
	 * Returns true if the request passed as parameter is running currently otherwise false
	 * 
	 * @param request
	 * @return
	 */
	public boolean isRequestRunning(Request<?> request)
	{
		long requestId = request.getId();
		if (requestMap.containsKey(requestId))
		{
			LogFull.d(request.toString() + " is already running ");
			return true;
		}
		LogFull.d(request.toString() + " is not already running ");
		return false;
	}

	/**
	 * Shutdown method to close everything (setting all variables to null for easy garbage collection)
	 */
	public void shutdown()
	{
		requestMap.clear();
		requestMap = null;
		requestRunner.shutdown();
		requestRunner = null;
		requestListenerNotifier.shutdown();
		requestListenerNotifier = null;
	}
}
