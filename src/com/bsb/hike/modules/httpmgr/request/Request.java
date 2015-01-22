package com.bsb.hike.modules.httpmgr.request;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.request.facade.IRequestFacade;
import com.bsb.hike.modules.httpmgr.request.listener.IPreProcessListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestCancellationListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;

/**
 * Encapsulates all of the information necessary to make an HTTP request.
 */
public class Request implements IRequestFacade
{
	public static final short REQUEST_TYPE_LONG = 0x0;

	public static final short REQUEST_TYPE_SHORT = 0x1;

	private long id;

	private String method;

	private String url;

	private List<Header> headers;

	private IRequestBody body;

	private int priority;

	private short requestType;

	private IRetryPolicy retryPolicy;

	private boolean isCancelled;

	private CopyOnWriteArrayList<IRequestListener> requestListeners;

	private IRequestCancellationListener requestCancellationListener;

	private IPreProcessListener preProcessListener;

	private boolean responseOnUIThread;

	private boolean asynchronous;

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
		this.retryPolicy = builder.retryPolicy;
		this.requestListeners = builder.requestListeners;
		this.preProcessListener = builder.preProcessListener;
		this.responseOnUIThread = builder.responseOnUIThread;
		this.asynchronous = builder.asynchronous;
	}

	/**
	 * Returns the unique id of the request
	 * 
	 * @return
	 */
	public long getId()
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
	public short getRequestType()
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
	public CopyOnWriteArrayList<IRequestListener> getRequestListeners()
	{
		return requestListeners;
	}

	/**
	 * Returns the {@link IPreProcessListener} object
	 * 
	 * @return
	 */
	public IPreProcessListener getPreProcessListener()
	{
		return preProcessListener;
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
	public boolean isResponseOnUIThread()
	{
		return responseOnUIThread;
	}

	/**
	 * Returns true if the request to be executed asynchronously (Non blocking call) otherwise false
	 * 
	 * @return
	 */
	public boolean isAsynchronous()
	{
		return asynchronous;
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
	public void setRequestType(short requestType)
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

	public void addRequestListeners(List<IRequestListener> requestListeners)
	{
		if (this.requestListeners == null)
		{
			this.requestListeners = new CopyOnWriteArrayList<IRequestListener>();
		}
		this.requestListeners.addAll(requestListeners);
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
	
	/**
	 * Sets isCancelled field to true and cancels the future of this request that has been submitted to the executor
	 */
	public void cancel()
	{
		this.isCancelled = true;

		if (future != null)
		{
			future.cancel(true);
		}

		if (this.requestCancellationListener != null)
		{
			this.requestCancellationListener.onCancel();
		}
	}

	public static class Builder
	{
		private long id = -1;

		private String method;

		private String url;

		private List<Header> headers;

		private IRequestBody body;

		private int priority;

		private short requestType = REQUEST_TYPE_LONG;

		private IRetryPolicy retryPolicy;

		private CopyOnWriteArrayList<IRequestListener> requestListeners;

		private IPreProcessListener preProcessListener;

		private boolean responseOnUIThread;

		private boolean asynchronous;

		/**
		 * Sets the unique id of the request
		 * 
		 * @param id
		 */
		public Builder setId(long id)
		{
			this.id = id;
			return this;
		}

		/**
		 * Sets the method of the request
		 * 
		 * @param method
		 */
		public Builder setMethod(String method)
		{
			this.method = method;
			return this;
		}

		/**
		 * Sets the url of the request
		 * 
		 * @param url
		 */
		public Builder setUrl(String url)
		{
			this.url = url;
			return this;
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
		public Builder setRequestType(short requestType)
		{
			this.requestType = requestType;
			return this;
		}

		/**
		 * Set a {@link IRetryPolicy} that will be responsible to coordinate retry attempts by the RequestRunner. Can be null (no retry).
		 * 
		 * @param retryPolicy
		 *            the new retry policy
		 * @see
		 */
		public Builder setRetryPolicy(IRetryPolicy retryPolicy)
		{
			this.retryPolicy = retryPolicy;
			return this;
		}

		/**
		 * Sets the request listener {@link IRequestListener}
		 * 
		 * @param requestListener
		 */
		public Builder setRequestListener(IRequestListener requestListener)
		{
			if (this.requestListeners == null)
			{
				this.requestListeners = new CopyOnWriteArrayList<IRequestListener>();
			}
			this.requestListeners.add(requestListener);
			return this;
		}

		/**
		 * Sets the {@link IPreProcessListener} object to the request which will be used to perfoem background task other than http call
		 * 
		 * @param preProcessListener
		 * @return
		 */
		public Builder setPreProcessListener(IPreProcessListener preProcessListener)
		{
			this.preProcessListener = preProcessListener;
			return this;
		}

		/**
		 * Sets the boolean whether request should be eun on ui thread or not
		 * 
		 * @param runOnUIThread
		 */
		public Builder setResponseOnUIThread(boolean responseOnUIThread)
		{
			this.responseOnUIThread = responseOnUIThread;
			return this;
		}

		/**
		 * Sets the asynchronous field , pass true if this request should be executed asynchronously (non blocking)
		 * 
		 * @param async
		 * @return
		 */
		public Builder setAsynchronous(boolean async)
		{
			this.asynchronous = async;
			return this;
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

			if (id < 0)
			{
				id = hashCode();
			}
		}
	}

	@Override
	public int hashCode()
	{
		int urlHashCode = url.hashCode();
		int headersHashCode = 0;
		for (Header header : headers)
		{
			headersHashCode += header.hashCode();
		}
		int requestBodyHashCode = (null != body) ? body.hashCode() : 0;
		return (31 * urlHashCode) + headersHashCode + requestBodyHashCode;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}

		if (!(other instanceof Request))
		{
			return false;
		}

		Request req = (Request) other;
		if (this.getId() != req.getId())
		{
			return false;
		}
		return true;
	}
}
