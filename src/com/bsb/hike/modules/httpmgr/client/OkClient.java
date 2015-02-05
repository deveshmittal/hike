package com.bsb.hike.modules.httpmgr.client;

import java.util.concurrent.TimeUnit;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.response.ResponseBody;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;

/**
 * 
 * Represents the OkHttpClient wrapper
 * 
 * @author anubhavgupta & sidharth
 * 
 */
public class OkClient implements IClient
{
	private OkHttpClient client;

	/**
	 * These constants are used internally by okHttp for connection pooling
	 */
	private static final boolean keepAlive = true;

	private static final long keepAliveDuration = 5 * 60 * 1000; // 5 min

	private static final int numOfMaxConnections = 5;

	static
	{
		/**
		 * Don't change these keys as they are used internally by okHttp for connection pooling
		 */
		System.setProperty("http.keepAlive", Boolean.toString(keepAlive));
		System.setProperty("http.keepAliveDuration", Long.toString(keepAliveDuration));
		System.setProperty("http.maxConnections", Integer.toString(numOfMaxConnections));
	}

	/**
	 * Generates a new OkHttpClient with given clientOption parameters
	 * 
	 * @param clientOptions
	 * @return
	 */
	static OkHttpClient generateClient(ClientOptions clientOptions)
	{
		clientOptions = clientOptions != null ? clientOptions : getDefaultClientOptions();
		OkHttpClient client = new OkHttpClient();
		return setClientParameters(client, clientOptions);
	}

	/**
	 * Sets Client option parameters to given OkHttpClient
	 * 
	 * @param client
	 * @param clientOptions
	 * @return
	 */
	static OkHttpClient setClientParameters(OkHttpClient client, ClientOptions clientOptions)
	{
		client.setConnectTimeout(clientOptions.getConnectTimeout(), TimeUnit.MILLISECONDS);
		client.setReadTimeout(clientOptions.getReadTimeout(), TimeUnit.MILLISECONDS);
		client.setWriteTimeout(clientOptions.getWriteTimeout(), TimeUnit.MILLISECONDS);
		client.setWriteTimeout(clientOptions.getWriteTimeout(), TimeUnit.MILLISECONDS);
		client.setSocketFactory(client.getSocketFactory());
		client.setSslSocketFactory(clientOptions.getSslSocketFactory());

		if (clientOptions.getProxy() != null)
		{
			client.setProxy(clientOptions.getProxy());
		}

		if (clientOptions.getProxySelector() != null)
		{
			client.setProxySelector(clientOptions.getProxySelector());
		}

		if (clientOptions.getProxySelector() != null)
		{
			client.setProxySelector(clientOptions.getProxySelector());
		}

		if (clientOptions.getCookieHandler() != null)
		{
			client.setCookieHandler(clientOptions.getCookieHandler());
		}

		if (clientOptions.getCache() != null)
		{
			client.setCache(clientOptions.getCache());
		}

		if (clientOptions.getCache() != null)
		{
			client.setCache(clientOptions.getCache());
		}

		if (clientOptions.getHostnameVerifier() != null)
		{
			client.setHostnameVerifier(clientOptions.getHostnameVerifier());
		}

		if (clientOptions.getCertificatePinner() != null)
		{
			client.setCertificatePinner(clientOptions.getCertificatePinner());
		}

		if (clientOptions.getAuthenticator() != null)
		{
			client.setAuthenticator(clientOptions.getAuthenticator());
		}

		if (clientOptions.getProtocols() != null)
		{
			client.setProtocols(clientOptions.getProtocols());
		}

		if (clientOptions.getConnectionSpecs() != null)
		{
			client.setConnectionSpecs(clientOptions.getConnectionSpecs());
		}

		if (clientOptions.getDispatcher() != null)
		{
			client.setDispatcher(clientOptions.getDispatcher());
		}

		if (clientOptions.getConnectionPool() != null)
		{
			client.setConnectionPool(clientOptions.getConnectionPool());
		}

		client.setFollowSslRedirects(clientOptions.getFollowSslRedirects());
		client.setFollowRedirects(clientOptions.getFollowRedirects());

		return client;
	}

	/**
	 * Returns clientoption with default values set
	 * 
	 * @return
	 */
	static ClientOptions getDefaultClientOptions()
	{
		ClientOptions defaultClientOptions = new ClientOptions.Builder().setConnectTimeout(Defaults.CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
				.setReadTimeout(Defaults.CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).setWriteTimeout(Defaults.WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
				.setSocketFactory(Defaults.SOCKET_FACTORY).setSslSocketFactory(Defaults.SSL_SOCKET_FACTORY).build();
		return defaultClientOptions;
	}

	public OkClient()
	{
		client = generateClient(getDefaultClientOptions());
	}

	public OkClient(ClientOptions clientOptions)
	{
		client = generateClient(clientOptions);
	}

	public OkClient(OkHttpClient client)
	{
		this.client = client;
	}

	@Override
	public Response execute(Request<?> request) throws Throwable
	{
		com.squareup.okhttp.Request httpRequest = makeOkRequest(request);
		Call call = client.newCall(httpRequest);
		com.squareup.okhttp.Response response = call.execute();
		return parseOkResponse(request, response);

	}

	/**
	 * Parse hike http request to OkHttpRequest
	 * 
	 * @param <T>
	 * 
	 * @param request
	 * @return
	 */
	private com.squareup.okhttp.Request makeOkRequest(Request<?> request)
	{
		com.squareup.okhttp.Request.Builder httpRequestBuilder = new com.squareup.okhttp.Request.Builder();

		httpRequestBuilder.url(request.getUrl());
		for (Header header : request.getHeaders())
		{
			httpRequestBuilder.addHeader(header.getName(), header.getValue());
		}
		IRequestBody body = request.getBody();
		httpRequestBuilder.method(request.getMethod(), null == body ? null : body.getRequestBody());
		com.squareup.okhttp.Request httpRequest = httpRequestBuilder.build();
		return httpRequest;
	}

	/**
	 * Parse OkhttpResponse to hike http response
	 * 
	 * @param <T>
	 * 
	 * @param response
	 * @return
	 * @throws Throwable
	 */
	private <T> Response parseOkResponse(Request<T> request, com.squareup.okhttp.Response response) throws Throwable
	{
		Response.Builder responseBuilder = new Response.Builder();
		responseBuilder.setUrl(response.request().urlString());
		responseBuilder.setStatusCode(response.code());
		responseBuilder.setReason(response.message());
		com.squareup.okhttp.ResponseBody responseBody = response.body();
		T bodyContent = request.parseResponse(responseBody.byteStream());
		ResponseBody<T> body = ResponseBody.create(responseBody.toString(), (int) responseBody.contentLength(), bodyContent);
		responseBuilder.setBody(body);
		return responseBuilder.build();
	}

	/**
	 * Clones okClient with given client option parameters
	 * 
	 * @param clientOptions
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public OkClient clone(ClientOptions clientOptions)
	{
		return new OkClient(setClientParameters(client.clone(), clientOptions));
	}

}
