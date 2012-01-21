package com.bsb.hike.http;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpPatch extends HttpEntityEnclosingRequestBase
{
	public HttpPatch(URI uri)
	{
		super();
		setURI(uri);
	}

	public HttpPatch(String uri)
	{
		super();
		setURI(URI.create(uri));
	}

	@Override
	public String getMethod()
	{
		return "PATCH";
	}

}
