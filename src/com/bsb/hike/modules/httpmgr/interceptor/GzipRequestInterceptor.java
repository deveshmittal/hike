package com.bsb.hike.modules.httpmgr.interceptor;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.Utils;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.request.requestbody.GzipRequestBody;

/** This interceptor compresses the HTTP request body */
public final class GzipRequestInterceptor implements IRequestInterceptor
{ 
	private static final int THRESHOLD_LENGTH_FOR_GZIP = 250;
	
	@Override
	public void intercept(Chain chain)
	{
		RequestFacade requestFacade = chain.getRequestFacade();

		if (requestFacade.getBody() == null || Utils.containsHeader(requestFacade.getHeaders(), "Content-Encoding"))
		{
			chain.proceed();
		}

		if (requestFacade.getBody().length() >= THRESHOLD_LENGTH_FOR_GZIP)
		{
			requestFacade.getHeaders().add(new Header("Content-Encoding", "gzip"));
			requestFacade.setBody(new GzipRequestBody(requestFacade.getBody()));
		}
		chain.proceed();
	}
}