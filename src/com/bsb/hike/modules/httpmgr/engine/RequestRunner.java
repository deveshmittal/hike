package com.bsb.hike.modules.httpmgr.engine;

import java.util.Map;

import com.bsb.hike.modules.httpmgr.client.ClientOptions;
import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.client.OkUrlClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.response.Response;

/**
 * This class clones the the {@link IClient} object and passes it to {@link RequestExecuter} for the final execution of the request
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
					requestListenerNotifier.notifyListenersOfRequestSuccess(request, response);
				}
				requestMap.remove(request.getId());
			}
		});
		requestExecuter.execute();
	}

}
