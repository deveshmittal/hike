package com.bsb.hike.modules.httpmgr.request;

import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_HIGH;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_LOW;
import static com.bsb.hike.modules.httpmgr.request.PriorityConstants.PRIORITY_NORMAL;
import static com.bsb.hike.modules.httpmgr.request.RequestConstants.GET;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.Header;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.request.facade.IRequestFacade;
import com.bsb.hike.modules.httpmgr.request.listener.IPreProcessListener;
import com.bsb.hike.modules.httpmgr.request.listener.IProgressListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestCancellationListener;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.retry.DefaultRetryPolicy;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;

/**
 * Encapsulates all of the information necessary to make an HTTP request.
 */
public abstract class Request<T> implements IRequestFacade
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

	private IProgressListener progressListener;

	private boolean responseOnUIThread;

	private boolean asynchronous;

	private Future<?> future;

	protected Request(Init<?> builder)
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
		ensureSaneDefaults();
	}

	private void ensureSaneDefaults()
	{
		if (TextUtils.isEmpty(url))
		{
			throw new IllegalStateException("Url must not be null and its length must be greater than 0");
		}

		if (priority > PRIORITY_LOW || priority < PRIORITY_HIGH)
		{
			throw new IllegalArgumentException("Priority can be between " + PRIORITY_LOW + " to " + PRIORITY_HIGH);
		}

		if (TextUtils.isEmpty(method))
		{
			method = GET;
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
	
	public abstract T parseResponse(InputStream in) throws Throwable;

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
	 * Returns the {@link IProgressListener} object , used to update the request progress
	 * 
	 * @return
	 */
	public IProgressListener getProgressListener()
	{
		return progressListener;
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
		if (priority > PRIORITY_LOW || priority < PRIORITY_HIGH)
		{
			throw new IllegalArgumentException("Priority can be between " + PRIORITY_LOW + " to " + PRIORITY_HIGH);
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
	 * Sets the progress listener of the request {@link IProgressListener}
	 * 
	 * @param progressListener
	 */
	public void setProgressListener(IProgressListener progressListener)
	{
		this.progressListener = progressListener;
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

	/**
	 * Used to update the progress of the request
	 * 
	 * @param f
	 */
	public void publishProgress(float f)
	{
		if (this.progressListener != null)
		{
			this.progressListener.onProgressUpdate(f);
		}
	}

	protected static abstract class Init<S extends Init<S>>
	{
		private long id = -1;

		private String method;

		private String url;

		private List<Header> headers;

		private IRequestBody body;

		private int priority = PRIORITY_NORMAL;

		private short requestType = REQUEST_TYPE_LONG;

		private IRetryPolicy retryPolicy  = new DefaultRetryPolicy();

		private CopyOnWriteArrayList<IRequestListener> requestListeners;

		private IPreProcessListener preProcessListener;

		private boolean responseOnUIThread;

		private boolean asynchronous = true;

		protected abstract S self();

		/**
		 * Sets the unique id of the request
		 * 
		 * @param id
		 */
		public S setId(long id)
		{
			this.id = id;
			return self();
		}

		/**
		 * Sets the method type to {@see RequestConstants#GET} and body null
		 * 
		 * @return
		 */
		public S get()
		{
			this.method = RequestConstants.GET;
			this.body = null;
			return self();
		}

		/**
		 * Sets the method type to {@see RequestConstants#HEAD} and body null
		 * 
		 * @return
		 */
		public S head()
		{
			this.method = RequestConstants.HEAD;
			this.body = null;
			return self();
		}

		/**
		 * Sets the method type to {@see RequestConstants#POST} and body passed as a parameter
		 * 
		 * @return
		 */
		public S post(IRequestBody body)
		{
			this.method = RequestConstants.POST;
			this.body = body;
			return self();
		}

		public S put(IRequestBody body)
		{
			this.method = RequestConstants.PUT;
			this.body = body;
			return self();
		}

		public S delete()
		{
			this.method = RequestConstants.DELETE;
			this.body = null;
			return self();
		}

		public S patch(IRequestBody body)
		{
			this.method = RequestConstants.PATCH;
			this.body = body;
			return self();
		}

		/**
		 * Sets the url of the request
		 * 
		 * @param url
		 */
		public S setUrl(String url)
		{
			this.url = url;
			return self();
		}

		/**
		 * Sets the headers of the request
		 * 
		 * @param headers
		 */
		public S setHeaders(List<Header> headers)
		{
			this.headers = headers;
			return self();
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
		public S setPriority(int priority)
		{
			this.priority = priority;
			return self();
		}

		/**
		 * Sets the request type. Use request types constants
		 * 
		 * @param requestType
		 *            the request type of the request. Defaults to {@link #REQUEST_TYPE_LONG}
		 * @see #REQUEST_TYPE_LONG
		 * @see #REQUEST_TYPE_SHORT
		 */
		public S setRequestType(short requestType)
		{
			this.requestType = requestType;
			return self();
		}

		/**
		 * Set a {@link IRetryPolicy} that will be responsible to coordinate retry attempts by the RequestRunner. Can be null (no retry).
		 * 
		 * @param retryPolicy
		 *            the new retry policy
		 * @see
		 */
		public S setRetryPolicy(IRetryPolicy retryPolicy)
		{
			this.retryPolicy = retryPolicy;
			return self();
		}

		/**
		 * Sets the request listener {@link IRequestListener}
		 * 
		 * @param requestListener
		 */
		public S setRequestListener(IRequestListener requestListener)
		{
			if (this.requestListeners == null)
			{
				this.requestListeners = new CopyOnWriteArrayList<IRequestListener>();
			}
			this.requestListeners.add(requestListener);
			return self();
		}

		/**
		 * Sets the {@link IPreProcessListener} object to the request which will be used to perfoem background task other than http call
		 * 
		 * @param preProcessListener
		 * @return
		 */
		public S setPreProcessListener(IPreProcessListener preProcessListener)
		{
			this.preProcessListener = preProcessListener;
			return self();
		}

		/**
		 * Sets the boolean whether request should be eun on ui thread or not
		 * 
		 * @param runOnUIThread
		 */
		public S setResponseOnUIThread(boolean responseOnUIThread)
		{
			this.responseOnUIThread = responseOnUIThread;
			return self();
		}

		/**
		 * Sets the asynchronous field , pass true if this request should be executed asynchronously (non blocking)
		 * 
		 * @param async
		 * @return
		 */
		public S setAsynchronous(boolean async)
		{
			this.asynchronous = async;
			return self();
		}

		/**
		 * Returns an object of {@link RequestToken} which allows outside world to only have limited access to request class so that users can not update request after being
		 * submitted to the executor
		 * 
		 * @return
		 */
		public abstract RequestToken build();
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

	@SuppressWarnings("unchecked")
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

		Request<T> req = (Request<T>) other;
		if (this.getId() != req.getId())
		{
			return false;
		}
		return true;
	}
}
