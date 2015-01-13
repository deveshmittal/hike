package com.bsb.hike.modules.httpmgr.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

/**
 * Represents UrlClient wrapper with underlying pooling and caching done by OkHttp
 * @author anubhavgupta & sidharth
 *
 */
public class OkUrlClient extends UrlConnectionClient
{

	private final OkUrlFactory okUrlFactory;

	public OkUrlClient()
	{
		this(generateClient(getDefaultClientOptions()));
	}

	public OkUrlClient(ClientOptions clientOptions)
	{
		this(generateClient(clientOptions));
	}

	public OkUrlClient(OkHttpClient client)
	{
		this.okUrlFactory = new OkUrlFactory(client);
	}

	@Override
	protected HttpURLConnection openConnection(Request request) throws IOException
	{
		return okUrlFactory.open(new URL(request.getUrl()));
	}
	
	/**
	 * Clones the OkUrlClient with given client option parameters
	 */
	public OkUrlClient clone(ClientOptions clientOptions)
	{
		return new OkUrlClient(setClientParameters(okUrlFactory.client().clone(), clientOptions));
	}
}
