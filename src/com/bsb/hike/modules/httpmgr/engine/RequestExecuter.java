package com.bsb.hike.modules.httpmgr.engine;

import java.io.IOException;

import com.bsb.hike.modules.httpmgr.client.IClient;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.network.NetworkChecker;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.RequestCall;
import com.bsb.hike.modules.httpmgr.request.facade.RequestFacade;
import com.bsb.hike.modules.httpmgr.request.listener.IPreProcessListener;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.modules.httpmgr.retry.IRetryPolicy;

/**
 * This class is responsible for submitting the {@link Request} to the {@link HttpEngine} for engine and decides whether to execute the request asynchronously or synchronously
 * based on request parameters. Also handle exceptions and retries based on {@link IRetryPolicy} set in the request object
 * 
 * @author sidharth
 * 
 */
public class RequestExecuter
{
	private IClient client;

	private Request request;

	private HttpEngine engine;

	private IResponseListener listener;

	public RequestExecuter(IClient client, HttpEngine engine, Request request, IResponseListener listener)
	{
		this.client = client;
		this.engine = engine;
		this.request = request;
		this.listener = listener;
	}

	/**
	 * Simply calls the {@link IPreProcessListener#doInBackground()} for executing tasks other than http task
	 */
	private void preProcess()
	{
		request.getPreProcessListener().doInBackground(new RequestFacade(request));
	}

	/**
	 * Processes request synchronously or asynchronously based on request parameters
	 * 
	 * @see #execute(boolean, long)
	 */
	public void execute()
	{
		execute(true, 0);
	}

	/**
	 * Processes request synchronously or asynchronously based on request parameters
	 * 
	 * @param firstTry
	 *            true if it's an first attempt to execute a request otherwise false
	 * @param delay
	 *            delay between the retries
	 */
	private void execute(final boolean firstTry, long delay)
	{
		if (request.isAsynchronous())
		{
			processAsync(firstTry, delay);
		}
		else
		{
			processSync(firstTry, delay);
		}
	}

	/**
	 * Processes the request synchronously i.e. same thread on which it is submitted
	 * 
	 * @param firstTry
	 * @param delay
	 */
	private void processSync(boolean firstTry, long delay)
	{
		if (firstTry)
		{
			preProcess();
		}
		try
		{
			Thread.sleep(delay);
			processRequest();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Processes the request asynchronously , a {@link RequestCall} is created which is passed to engine for async execution
	 * 
	 * @param firstTry
	 * @param delay
	 */
	private void processAsync(final boolean firstTry, long delay)
	{
		RequestCall call = new RequestCall(request)
		{
			@Override
			public void execute()
			{
				try
				{
					if (firstTry)
					{
						preProcess();
					}
					processRequest();
				}
				finally
				{
					request.setRequestCancellationListener(null);
				}
			}
		};

		engine.submit(call, delay);
	}

	/**
	 * Checks if network is available or not and request is cancelled or not. If network is available and request is not cancelled yet then executes the request using
	 * {@link IClient}
	 * 
	 */
	public void processRequest()
	{
		if (!NetworkChecker.isNetworkAvailable())
		{
			if (!request.isCancelled())
			{
				listener.onResponse(null, new HttpException("No network available"));
				return;
			}
		}

		try
		{
			if (request.isCancelled())
			{
				return;
			}
			Response response = client.execute(request);
			listener.onResponse(response, null);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			handleException(ex);
		}
	}

	/**
	 * Handles the exception that occurs while executing the request, and in case of {@link IOException} handle retries based on {@link IRetryPolicy}
	 * 
	 * @param ex
	 */
	private void handleException(Exception ex)
	{
		if (ex instanceof IOException)
		{
			handleRetry((IOException) ex);
		}
		else
		{
			listener.onResponse(null, new HttpException(ex));
		}
	}

	/**
	 * Handles the retries of the request based on {@link IRetryPolicy}
	 * 
	 * @param ex
	 */
	private void handleRetry(IOException ex)
	{
		HttpException httpException = new HttpException(ex);
		if (null != request.getRetryPolicy())
		{
			IRetryPolicy retryPolicy = request.getRetryPolicy();
			retryPolicy.retry(httpException);
			if (retryPolicy.getRetryCount() >= 0)
			{
				execute(false, retryPolicy.getRetryDelay());
			}
			else
			{
				listener.onResponse(null, httpException);
			}
		}
		else
		{
			listener.onResponse(null, httpException);
		}
	}
}
