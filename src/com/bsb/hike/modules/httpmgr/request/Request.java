package com.bsb.hike.modules.httpmgr.request;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestCancellationListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;

/**
 * Encapsulates all of the information necessary to make an HTTP request.
 */
public class Request
{
	public static final int REQUEST_TYPE_LONG = 0;

	public static final int REQUEST_TYPE_SHORT = 1;

	private String id;

	private String method;

	private String url;

	private List<Header> headers;

	private IRequestBody body;

	private int priority;

	private int requestType;

	private IRetryPolicy retryPolicy;

	private boolean isCancelled;

	private IRequestListener requestListener;

	private IRequestCancellationListener requestCancellationListener;

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
		this.runOnUIThread = builder.runOnUIThread;
		this.future = builder.future;
	}

	/**
	 * Returns the unique id of the request
	 * 
	 * @return
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Returns the method (GET / POST etc) of the request
	 * 
	 * @return
	 */
	public String getMethod()
	{
		return method;
	}

	/**
	 * Returns the target url of the request
	 * 
	 * @return
	 */
	public String getUrl()
	{
		return url;
	}

	/**
	 * Returns a list of headers of the request
	 * 
	 * @return
	 */
	public List<Header> getHeaders()
	{
		return headers;
	}

	/**
	 * Returns the request body
	 * 
	 * @return
	 */
	public IRequestBody getBody()
	{
		return body;
	}

	/**
	 * Returns the priority of the request
	 * 
	 * @return
	 */
	public int getPriority()
	{
		return priority;
	}

	/**
	 * Returns the request type of the request
	 * 
	 * @return
	 */
	public int getRequestType()
	{
		return requestType;
	}

	public boolean isCancelled()
	{
		return isCancelled;
	}

	/**
	 * Returns the object of class implementing {@link IRetryPolicy} which is used to schedule retries of the request in case of failure
	 * 
	 * @return
	 */
	public IRetryPolicy getRetryPolicy()
	{
		return retryPolicy;
	}

	/**
	 * Returns the {@link IRequestListener} object , request listener
	 * 
	 * @return
	 */
	public IRequestListener getRequestListener()
	{
		return requestListener;
	}

	/**
	 * Returns the {@link IRequestCancellationListener} object used when request is cancelled
	 * 
	 * @return
	 */
	public IRequestCancellationListener getRequestCancellationListener()
	{
		return requestCancellationListener;
	}

	/**
	 * Returns a boolean representing whether this request should run on ui thread or not
	 * 
	 * @return
	 */
	public boolean isRunOnUIThread()
	{
		return runOnUIThread;
	}

	/**
	 * Returns the future of the request that is submitted to the executor
	 * 
	 * @return
	 */
	public Future<?> getFuture()
	{
		return future;
	}

	/**
	 * Sets the headers of the request
	 * 
	 * @param headers
	 */
	public void setHeaders(List<Header> headers)
	{
		if (null == headers)
		{
			headers = new ArrayList<Header>();
		}
		this.headers = headers;
	}

	/**
	 * Adds more headers to the list of headers of the request
	 * 
	 * @param headers
	 */
	public void addHeaders(List<Header> headers)
	{
		if (null == headers)
		{
			return;
		}

		if (null == this.headers)
		{
			this.headers = headers;
		}
		else
		{
			this.headers.addAll(headers);
		}
	}

	/**
	 * Sets the body of the request
	 * 
	 * @param body
	 */
	public void setBody(IRequestBody body)
	{
		this.body = body;
	}

	/**
	 * Sets the priority of the request. Use priority constants or a positive integer. Will have no effect on a request after it starts being executed.
	 * 
	 * @param priority
	 *            the priority of request. Defaults to {@link #PRIORITY_NORMAL}.
	 * @throws IllegalArgumentException
	 *             if priority is not between 1 to 100 inclusive
	 * @see PriorityConstants#PRIORITY_LOW
	 * @see PriorityConstants#PRIORITY_NORMAL
	 * @see PriorityConstants#PRIORITY_HIGH
	 * 
	 */
	public void setPriority(int priority)
	{
		if (priority > PriorityConstants.PRIORITY_LOW || priority < PriorityConstants.PRIORITY_HIGH)
		{
			throw new IllegalArgumentException("Priority can be between " + PriorityConstants.PRIORITY_LOW + " to " + PriorityConstants.PRIORITY_HIGH);
		}
		this.priority = priority;
	}

	/**
	 * Sets the request type. Use request types constants
	 * 
	 * @param requestType
	 *            the request type of the request. Defaults to {@link #REQUEST_TYPE_LONG}
	 * @see #REQUEST_TYPE_LONG
	 * @see #REQUEST_TYPE_SHORT
	 */
	public void setRequestType(int requestType)
	{
		this.requestType = requestType;
	}

	/**
	 * Sets the cancelled boolean to true when request is cancelled
	 * 
	 * @param isCancelled
	 */
	public void setCancelled(boolean isCancelled)
	{
		this.isCancelled = isCancelled;
	}

	/**
	 * Sets the request cancellation listener {@link IRequestCancellationListener}
	 * 
	 * @param requestCancellationListener
	 */
	public void setRequestCancellationListener(IRequestCancellationListener requestCancellationListener)
	{
		this.requestCancellationListener = requestCancellationListener;
	}

	/**
	 * Sets the future of the runnable submitted to the executor
	 * 
	 * @param future
	 */
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

		private IRequestBody body;

		private int priority;

		private int requestType = REQUEST_TYPE_LONG;

		private boolean isCancelled;

		private IRetryPolicy retryPolicy;

		private IRequestListener requestListener;

		private IRequestCancellationListener requestCancellationListener;

		private boolean runOnUIThread;

		private Future<?> future;

		/**
		 * Sets the unique id of the request
		 * 
		 * @param id
		 */
		public void setId(String id)
		{
			this.id = id;
		}

		/**
		 * Sets the method of the request
		 * 
		 * @param method
		 */
		public void setMethod(String method)
		{
			this.method = method;
		}

		/**
		 * Sets the url of the request
		 * 
		 * @param url
		 */
		public void setUrl(String url)
		{
			this.url = url;
		}

		/**
		 * Sets the headers of the request
		 * 
		 * @param headers
		 */
		public Builder setHeaders(List<Header> headers)
		{
			this.headers = headers;
			return this;
		}

		/**
		 * Sets the body of the request
		 * 
		 * @param body
		 */
		public Builder setBody(IRequestBody body)
		{
			this.body = body;
			return this;
		}

		/**
		 * Sets the priority of the request. Use priority constants or a positive integer. Will have no effect on a request after it starts being executed.
		 * 
		 * @param priority
		 *            the priority of request. Defaults to {@link #PRIORITY_NORMAL}.
		 * @see #PRIORITY_LOW
		 * @see #PRIORITY_NORMAL
		 * @see #PRIORITY_HIGH
		 */
		public Builder setPriority(int priority)
		{
			this.priority = priority;
			return this;
		}

		/**
		 * Sets the request type. Use request types constants
		 * 
		 * @param requestType
		 *            the request type of the request. Defaults to {@link #REQUEST_TYPE_LONG}
		 * @see #REQUEST_TYPE_LONG
		 * @see #REQUEST_TYPE_SHORT
		 */
		public Builder setRequestType(int requestType)
		{
			this.requestType = requestType;
			return this;
		}

		/**
		 * Sets the cancelled boolean to true when request is cancelled
		 * 
		 * @param isCancelled
		 */
		public Builder setCancelled(boolean isCancelled)
		{
			this.isCancelled = isCancelled;
			return this;
		}

		/**
		 * Set a {@link IRetryPolicy} that will be responsible to coordinate retry attempts by the RequestRunner. Can be null (no retry).
		 * 
		 * @param retryPolicy
		 *            the new retry policy
		 * @see
		 */
		public void setRetryPolicy(IRetryPolicy retryPolicy)
		{
			this.retryPolicy = retryPolicy;
		}

		/**
		 * Sets the request listener {@link IRequestListener}
		 * 
		 * @param requestListener
		 */
		public void setRequestListener(IRequestListener requestListener)
		{
			this.requestListener = requestListener;
		}

		/**
		 * Sets the boolean whether request should be eun on ui thread or not
		 * 
		 * @param runOnUIThread
		 */
		public void setRunOnUIThread(boolean runOnUIThread)
		{
			this.runOnUIThread = runOnUIThread;
		}

		/**
		 * Returns an object of {@link RequestToken} which allows outside world to only have limited access to request class so that users can not update request after being
		 * submitted to the executor
		 * 
		 * @return
		 */
		public RequestToken build()
		{
			ensureSaneDefaults();
			Request request = new Request(this);
			RequestToken requestToken = new RequestToken(request);
			return requestToken;
		}

		private void ensureSaneDefaults()
		{
			if (TextUtils.isEmpty(url))
			{
				throw new IllegalStateException("Url must not be null and its length must be greater than 0");
			}

			if (priority > PriorityConstants.PRIORITY_LOW || priority < PriorityConstants.PRIORITY_HIGH)
			{
				throw new IllegalArgumentException("Priority can be between " + PriorityConstants.PRIORITY_LOW + " to " + PriorityConstants.PRIORITY_HIGH);
			}

			if (TextUtils.isEmpty(method))
			{
				method = RequestConstants.GET;
			}

			if (null == headers)
			{
				headers = new ArrayList<Header>();
			}
		}
	}
}
