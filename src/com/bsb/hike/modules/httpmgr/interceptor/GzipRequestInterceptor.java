package com.bsb.hike.modules.httpmgr.interceptor;

import java.util.List;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.request.requestbody.GzipRequestBody;

/** This interceptor compresses the HTTP request body */
public final class GzipRequestInterceptor implements IRequestInterceptor
{
	@Override
	public void intercept(Chain chain)
	{
		RequestFacade requestFacade = chain.getRequestFacade();
		if (requestFacade.getBody() == null || containsHeader(requestFacade.getHeaders(), "Content-Encoding"))
		{
			chain.proceed();
		}
		requestFacade.getHeaders().add(new Header("Content-Encoding", "gzip"));
		requestFacade.setBody(new GzipRequestBody(requestFacade.getBody()));
		chain.proceed();
	}

	private boolean containsHeader(List<Header> headers, String headerString)
	{
		for (Header header : headers)
		{
			if (header.getName().equalsIgnoreCase(headerString))
			{
				return true;
			}
		}
		return false;
	}
}