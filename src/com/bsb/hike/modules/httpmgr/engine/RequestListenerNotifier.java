package com.bsb.hike.modules.httpmgr.engine;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bsb.hike.modules.httpmgr.Utils;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseCall;
import com.bsb.hike.modules.httpmgr.response.ResponseFacade;

/**
 * This class is responsible for notifying the request listeners of the request about the success or failure of the request. The response is either sent synchronously i.e on same
 * thread from where request is executed or asynchronously (on response executor) or on UI thread
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
		this.uiExecuter = new MainThreadExecutor();
	}

	/**
	 * This method notifies the listeners about the request success. Calls {@link IRequestListener#onRequestSuccess(Response)} for each listener of the request
	 * 
	 * @param request
	 * @param response
	 */
	public void notifyListenersOfRequestSuccess(Request<?> request, Response response)
	{
		if (!request.isAsynchronous())
		{
			// send response on same thread
			sendSuccess(request, response);
		}
		else if (request.isResponseOnUIThread())
		{
			// send response on ui thread
			ResponseCall call = getResponseCall(request, response);
			uiExecuter.execute(call);
		}
		else
		{
			// send response on other thread
			ResponseCall call = getResponseCall(request, response);
			engine.submit(call);
		}
	}

	private ResponseCall getResponseCall(final Request<?> request, final Response response)
	{
		ResponseCall call = new ResponseCall()
		{
			@Override
			public void execute()
			{
				sendSuccess(request, response);
			}
		};
		return call;
	}

	private void sendSuccess(Request<?> request, Response response)
	{
		try
		{
			if (request.isCancelled())
			{
				return;
			}

			postProcessResponse(request, response);

			CopyOnWriteArrayList<IRequestListener> listeners = request.getRequestListeners();
			for (IRequestListener listener : listeners)
			{
				listener.onRequestSuccess(response);
			}
		}
		finally
		{
			Utils.finish(request, response);
		}
	}

	private void postProcessResponse(Request<?> request, Response response)
	{
		ResponseFacade responseFacade = new ResponseFacade(response);
		Iterator<IResponseInterceptor> iterator = responseFacade.getResponseInterceptors().iterator();
		ResponseInterceptorChain chain = new ResponseInterceptorChain(iterator, request, responseFacade);
		chain.proceed();
	}
	
	/**
	 * This method notifies the listeners about the request cancellation. Calls {@link IRequestListener#onRequestFailure(HttpException)} for each listener of the request,
	 * cancellation exception is given to the listeners
	 * 
	 * @param request
	 */
	public void notifyListenersOfRequestCancellation(Request<?> request)
	{
		notifyListenersOfRequestFailure(request, new HttpException(HttpException.REASON_CODE_CANCELLATION, "Request Cancellation Exception"));
	}

	/**
	 * This method notifies the listeners about the request cancellation. Calls {@link IRequestListener#onRequestFailure(HttpException)} for each listener of the request
	 * 
	 * @param request
	 * @param ex
	 */
	public void notifyListenersOfRequestFailure(Request<?> request, HttpException ex)
	{
		if (!request.isAsynchronous())
		{
			// send response on same thread
			sendFailure(request, ex);
		}
		else if (request.isResponseOnUIThread())
		{
			// send response on ui thread
			ResponseCall call = getResponseCall(request, ex);
			uiExecuter.execute(call);
		}
		else
		{
			// send response on other thread
			ResponseCall call = getResponseCall(request, ex);
			engine.submit(call);
		}
	}

	private ResponseCall getResponseCall(final Request<?> request, final HttpException ex)
	{
		ResponseCall call = new ResponseCall()
		{
			@Override
			public void execute()
			{
				sendFailure(request, ex);
			}
		};
		return call;
	}

	private void sendFailure(Request<?> request, HttpException ex)
	{
		try
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
		finally
		{
			Utils.finish(request, null);
		}
	}

	/**
	 * This method notifies the listeners about the request progress in percentage. Calls {@link IRequestListener#onRequestProgressUpdate(float)} for each listener of the request
	 * 
	 * @param request
	 * @param progress
	 */
	public void notifyListenersOfRequestProgress(Request<?> request, float progress)
	{
		if (!request.isAsynchronous())
		{
			// send response on same thread
			sendProgress(request, progress);
		}
		else if (request.isResponseOnUIThread())
		{
			// send response on ui thread
			ResponseCall call = getResponseCall(request, progress);
			uiExecuter.execute(call);
		}
		else
		{
			// send response on other thread
			ResponseCall call = getResponseCall(request, progress);
			engine.submit(call);
		}
	}

	private void sendProgress(Request<?> request, float progress)
	{
		if (request.isCancelled())
		{
			return;
		}

		CopyOnWriteArrayList<IRequestListener> listeners = request.getRequestListeners();
		for (IRequestListener listener : listeners)
		{
			listener.onRequestProgressUpdate(progress);
		}
	}

	private ResponseCall getResponseCall(final Request<?> request, final float progress)
	{
		ResponseCall call = new ResponseCall()
		{
			@Override
			public void execute()
			{
				sendProgress(request, progress);
			}
		};
		return call;
	}

	/**
	 * This class implements {@link IResponseInterceptor.Chain} and executes {@link IResponseInterceptor#intercept(IResponseInterceptor.Chain) for each node present in the interceptor chain
	 * @author sidharth
	 *
	 */
	public class ResponseInterceptorChain implements IResponseInterceptor.Chain
	{
		private Iterator<IResponseInterceptor> iterator;

		private Request<?> request;

		private ResponseFacade responseFacade;

		public ResponseInterceptorChain(Iterator<IResponseInterceptor> iterator, Request<?> request, ResponseFacade responseFacade)
		{
			this.iterator = iterator;
			this.request = request;
			this.responseFacade = responseFacade;
		}

		@Override
		public ResponseFacade getResponseFacade()
		{
			return responseFacade;
		}

		@Override
		public void proceed()
		{
			if (iterator.hasNext())
			{
				ResponseInterceptorChain chain = new ResponseInterceptorChain(iterator, request, responseFacade);
				iterator.next().intercept(chain);
			}
		}
	}

	public void shutdown()
	{
		engine = null;
		uiExecuter = null;
	}
}
