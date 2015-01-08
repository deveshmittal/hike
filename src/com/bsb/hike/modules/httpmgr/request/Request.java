package com.bsb.hike.modules.httpmgr.request;

import java.util.List;
import java.util.concurrent.Future;

import com.bsb.hike.modules.httpmgr.request.listener.IRequestCancellationListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;
import com.bsb.hike.modules.httpmgr.utils.Header;

public class Request
{
	private String id;

	private String method;

	private String url;

	private List<Header> headers;

	private byte[] body;

	private int priority;

	private int requestType;

	private boolean isCancelled;

	private IRetryPolicy retryPolicy;

	private IRequestListener requestListener;

	private IRequestCancellationListener requestCancellationListener;

	private boolean shouldCache;

	private boolean runOnUIThread;

	private Future<?> future;

	private Request(Builder builder)
	{
		this.id = builder.id;
		this.method = builder.method;
		this.url = builder.url;
		this.headers = builder.headers;
		this.body = builder.body;
		this.priority = builder.priority;
		this.requestType = builder.requestType;
		this.isCancelled = builder.isCancelled;
		this.retryPolicy = builder.retryPolicy;
		this.requestListener = builder.requestListener;
		this.requestCancellationListener = builder.requestCancellationListener;
		this.shouldCache = builder.shouldCache;
		this.runOnUIThread = builder.runOnUIThread;
		this.future = builder.future;
	}

	public String getId()
	{
		return id;
	}

	public String getMethod()
	{
		return method;
	}

	public String getUrl()
	{
		return url;
	}

	public List<Header> getHeaders()
	{
		return headers;
	}

	public byte[] getBody()
	{
		return body;
	}

	public int getPriority()
	{
		return priority;
	}

	public int getRequestType()
	{
		return requestType;
	}

	public boolean isCancelled()
	{
		return isCancelled;
	}

	public IRetryPolicy getRetryPolicy()
	{
		return retryPolicy;
	}

	public IRequestListener getRequestListener()
	{
		return requestListener;
	}

	public IRequestCancellationListener getRequestCancellationListener()
	{
		return requestCancellationListener;
	}

	public boolean isShouldCache()
	{
		return shouldCache;
	}

	public boolean isRunOnUIThread()
	{
		return runOnUIThread;
	}

	public Future<?> getFuture()
	{
		return future;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setMethod(String method)
	{
		this.method = method;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public void setHeaders(List<Header> headers)
	{
		this.headers = headers;
	}

	public void setBody(byte[] body)
	{
		this.body = body;
	}

	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	public void setRequestType(int requestType)
	{
		this.requestType = requestType;
	}

	public void setCancelled(boolean isCancelled)
	{
		this.isCancelled = isCancelled;
	}

	public void setRetryPolicy(IRetryPolicy retryPolicy)
	{
		this.retryPolicy = retryPolicy;
	}

	public void setRequestListener(IRequestListener requestListener)
	{
		this.requestListener = requestListener;
	}

	public void setRequestCancellationListener(IRequestCancellationListener requestCancellationListener)
	{
		this.requestCancellationListener = requestCancellationListener;
	}

	public void setShouldCache(boolean shouldCache)
	{
		this.shouldCache = shouldCache;
	}

	public void setRunOnUIThread(boolean runOnUIThread)
	{
		this.runOnUIThread = runOnUIThread;
	}

	public void setFuture(Future<?> future)
	{
		this.future = future;
	}

	public static class Builder
	{
		private String id;

		private String method;

		private String url;

		private List<Header> headers;

		private byte[] body;

		private int priority;

		private int requestType;

		private boolean isCancelled;

		private IRetryPolicy retryPolicy;

		private IRequestListener requestListener;

		private IRequestCancellationListener requestCancellationListener;

		private boolean shouldCache;

		private boolean runOnUIThread;

		private Future<?> future;

		public Builder(String url, String method)
		{
			this.url = url;
			this.method = method;
		}

		public String getId()
		{
			return id;
		}

		public Builder setId(String id)
		{
			this.id = id;
			return this;
		}

		public String getMethod()
		{
			return method;
		}

		public Builder setMethod(String method)
		{
			this.method = method;
			return this;
		}

		public String getUrl()
		{
			return url;
		}

		public Builder setUrl(String url)
		{
			this.url = url;
			return this;
		}

		public List<Header> getHeaders()
		{
			return headers;
		}

		public Builder setHeaders(List<Header> headers)
		{
			this.headers = headers;
			return this;
		}

		public byte[] getBody()
		{
			return body;
		}

		public Builder setBody(byte[] body)
		{
			this.body = body;
			return this;
		}

		public int getPriority()
		{
			return priority;
		}

		public Builder setPriority(int priority)
		{
			this.priority = priority;
			return this;
		}

		public int getRequestType()
		{
			return requestType;
		}

		public Builder setRequestType(int requestType)
		{
			this.requestType = requestType;
			return this;
		}

		public boolean isCancelled()
		{
			return isCancelled;
		}

		public Builder setCancelled(boolean isCancelled)
		{
			this.isCancelled = isCancelled;
			return this;
		}

		public IRetryPolicy getRetryPolicy()
		{
			return retryPolicy;
		}

		public Builder setRetryPolicy(IRetryPolicy retryPolicy)
		{
			this.retryPolicy = retryPolicy;
			return this;
		}

		public IRequestListener getRequestListener()
		{
			return requestListener;
		}

		public Builder setRequestListener(IRequestListener requestListener)
		{
			this.requestListener = requestListener;
			return this;
		}

		public IRequestCancellationListener getRequestCancellationListener()
		{
			return requestCancellationListener;
		}

		public Builder setRequestCancellationListener(IRequestCancellationListener requestCancellationListener)
		{
			this.requestCancellationListener = requestCancellationListener;
			return this;
		}

		public boolean isShouldCache()
		{
			return shouldCache;
		}

		public Builder setShouldCache(boolean shouldCache)
		{
			this.shouldCache = shouldCache;
			return this;
		}

		public boolean isRunOnUIThread()
		{
			return runOnUIThread;
		}

		public Builder setRunOnUIThread(boolean runOnUIThread)
		{
			this.runOnUIThread = runOnUIThread;
			return this;
		}

		public Future<?> getFuture()
		{
			return future;
		}

		public Builder setFuture(Future<?> future)
		{
			this.future = future;
			return this;
		}

		public RequestToken build()
		{
			Request request = new Request(this);
			RequestToken requestToken = new RequestToken(request);
			return requestToken;
		}
	}
}
