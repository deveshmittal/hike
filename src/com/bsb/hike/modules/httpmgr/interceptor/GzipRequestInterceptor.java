package com.bsb.hike.modules.httpmgr.interceptor;

import java.io.IOException;
import java.util.List;

import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.request.requestbody.GzipRequestBody;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

/** This interceptor compresses the HTTP request body */

public final class GzipRequestInterceptor implements IRequestInterceptor
{
	@Override
	public void intercept(Chain chain)
	{
		RequestFacade requestFacade = chain.getRequestFacade();
		
		if (requestFacade.getBody() == null || containsHeader(requestFacade.getHeaders(),"Content-Encoding"))
		{
			chain.proceed();
		}
		
		requestFacade.getHeaders().add(new Header("Content-Encoding", "gzip"));
		requestFacade.setBody(new GzipRequestBody(requestFacade.getBody()));
		chain.proceed();
	}
	
	private boolean containsHeader(List<Header> headers, String headerString)
	{
		for(Header header : headers)
		{
			if(header.getName().equalsIgnoreCase(headerString))
			{
				return true;
			}
		}
		return false;
	}
}