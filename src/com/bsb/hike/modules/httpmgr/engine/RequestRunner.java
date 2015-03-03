package com.bsb.hike.modules.httpmgr.engine;

import java.util.Iterator;
import java.util.Map;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.client.OkUrlClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.interceptor.IResponseInterceptor;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseFacade;

/**
 * This class clones the {@link IClient} object and passes it to {@link RequestExecuter} for the final execution of the request
 * 
 * @author sidharth & anubhav
 * 
 */
public class RequestRunner
{
	private IClient defaultClient;

	private HttpEngine engine;

	private Map<Long, Request<?>> requestMap;

	private RequestListenerNotifier requestListenerNotifier;

	public RequestRunner(ClientOptions options, Map<Long, Request<?>> requestMap, HttpEngine engine, RequestListenerNotifier requestListenerNotifier)
	{
		defaultClient = new OkUrlClient(options);
		this.engine = engine;
		this.requestMap = requestMap;
		this.requestListenerNotifier = requestListenerNotifier;
	}

	/**
	 * Clones the {@link IClient} object if parameter <code>options</code> is not null and then passes this client to the {@link RequestExecuter} for final execution of the request
	 * 
	 * @param request
	 * @param options
	 */
	public void submit(final Request<?> request, ClientOptions options)
	{
		IClient client = (null != options) ? defaultClient.clone(options) : defaultClient;

		RequestExecuter requestExecuter = new RequestExecuter(client, engine, request, new IResponseListener()
		{
			@Override
			public void onResponse(Response response, HttpException ex)
			{
				if (null == response)
				{
					requestListenerNotifier.notifyListenersOfRequestFailure(request, ex);
				}
				else
				{
					ResponseFacade responseFacade = new ResponseFacade(response);
					Iterator<IResponseInterceptor> iterator = responseFacade.getResponseInterceptors().iterator();
					ResponseInterceptorChain chain = new ResponseInterceptorChain(iterator, request, responseFacade);
					chain.proceed();
				}
				requestMap.remove(request.getId());
			}
		});
		requestExecuter.execute();
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
			else
			{
				requestListenerNotifier.notifyListenersOfRequestSuccess(request, responseFacade.getResponse());
			}
		}
	}

	/**
	 * Shutdown method to close everything (setting all variables to null for easy garbage collection)
	 */
	public void shutdown()
	{
		engine.shutDown();
		engine = null;
		defaultClient = null;
		requestMap.clear();
		requestMap = null;
		requestListenerNotifier.shutdown();
	}
}
