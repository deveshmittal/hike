package com.bsb.hike.modules.httpmgr.engine;

import java.util.concurrent.CopyOnWriteArrayList;

import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseCall;

/**
 * This class is responsible for notifying the request listeners of the request about the success or failure of the request
 * 
 * @author sidharth
 * 
 */
public class RequestListenerNotifier
{
	private HttpEngine engine;

	private MainThreadExecutor uiExecuter;

	public RequestListenerNotifier(HttpEngine engine)
	{
		this.engine = engine;
	}

	/**
	 * This method notifies the listeners about the request success. Calls {@link IRequestListener#onRequestSuccess(Response)} for each listener of the request
	 * 
	 * @param request
	 * @param response
	 */
	public void notifyListenersOfRequestSuccess(final Request request, final Response response)
	{
		if (!request.isAsynchronous())
		{
			// send response on same thread
			sendResponse(request, response);
		}
		else if (request.isResponseOnUIThread())
		{
			// send response on ui thread
			ResponseCall call = new ResponseCall()
			{
				@Override
				public void execute()
				{
					sendResponse(request, response);
				}
			};
			uiExecuter.execute(call);
		}
		else
		{
			// send response on other thread
			ResponseCall call = new ResponseCall()
			{
				@Override
				public void execute()
				{
					sendResponse(request, response);
				}
			};
			engine.submit(call);
		}
	}

	private void sendResponse(Request request, Response response)
	{
		if (request.isCancelled())
		{
			return;
		}

		CopyOnWriteArrayList<IRequestListener> listeners = request.getRequestListeners();
		for (IRequestListener listener : listeners)
		{
			listener.onRequestSuccess(response);
		}
	}

	/**
	 * This method notifies the listeners about the request cancellation. Calls {@link IRequestListener#onRequestFailure(HttpException)} for each listener of the request,
	 * cancellation exception is given to the listeners
	 * 
	 * @param request
	 */
	public void notifyListenersOfRequestCancellation(Request request)
	{
		notifyListenersOfRequestFailure(request, new HttpException("Cancellation Exception"));
	}

	/**
	 * This method notifies the listeners about the request cancellation. Calls {@link IRequestListener#onRequestFailure(HttpException)} for each listener of the request
	 * 
	 * @param request
	 * @param ex
	 */
	public void notifyListenersOfRequestFailure(final Request request, final HttpException ex)
	{
		if (request.isResponseOnUIThread())
		{
			sendException(request, ex);
		}
		else
		{
			ResponseCall call = new ResponseCall()
			{
				@Override
				public void execute()
				{
					sendException(request, ex);
				}
			};
			engine.submit(call);
		}
	}

	private void sendException(Request request, HttpException ex)
	{
		if (request.isCancelled())
		{
			return;
		}

		CopyOnWriteArrayList<IRequestListener> listeners = request.getRequestListeners();
		for (IRequestListener listener : listeners)
		{
			listener.onRequestFailure(ex);
		}
	}

	/**
	 * 
	 * @param request
	 */
	public void notifyListenersOfRequestProgress(Request request)
	{
		// TODO
	}
}
