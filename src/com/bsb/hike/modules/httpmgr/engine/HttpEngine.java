package com.bsb.hike.modules.httpmgr.engine;

import static com.bsb.hike.modules.httpmgr.engine.HttpEngineConstants.CORE_POOL_SIZE;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Pair;

import com.bsb.hike.modules.httpmgr.Utils;
import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.RequestCall;
import com.bsb.hike.modules.httpmgr.response.ResponseCall;
import com.bsb.hike.utils.Logger;
import com.squareup.okhttp.internal.Util;

/**
 * 
 * @author anubhavgupta
 * 
 */
public class HttpEngine
{
	private static String TAG = "HTTP_ENGINE";

	static short LONG_EXECUTER = 0x0;

	static short SHORT_EXECUTER = 0x1;

	private HttpExecuter shortRequestExecuter;

	private HttpExecuter longRequestExecuter;

	private ThreadPoolExecutor responseExecuter;

	private HttpQueue queue;

	/**
	 * Creates a new HttpEngine
	 */
	public HttpEngine()
	{
		// executer used for serving short requests
		shortRequestExecuter = new HttpExecuter(this, SHORT_EXECUTER, CORE_POOL_SIZE, Utils.threadFactory("short-thread", false), Utils.rejectedExecutionHandler());

		// executer used for serving long requests
		longRequestExecuter = new HttpExecuter(this, LONG_EXECUTER, CORE_POOL_SIZE, Utils.threadFactory("long-thread", false), Utils.rejectedExecutionHandler());

		// executer used for serving response
		responseExecuter = new ThreadPoolExecutor(0, 2 * CORE_POOL_SIZE + 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Util.threadFactory("http-response", false));

		// underlying queue used for storing submitted and running request
		queue = new HttpQueue();

	}

	public void submit(RequestCall request, long delay)
	{
		if (delay <= 0)
		{
			submit(request);
		}
		else
		{
			submitWithDelay(request, delay);
		}
	}

	/**
	 * Adds the queue to respective queue and submits a new request to respective executer based on request type, if number of requests is less than that can be served by executer
	 * 
	 * @param request
	 */
	private synchronized void submit(RequestCall request)
	{
		queue.add(request);
		if (queue.spaceAvailable(request.getRequestType()))
		{
			submitNext(request.getRequestType());
		}
	}

	/**
	 * Submits a new request to executer which will execute after a given delay
	 * 
	 * @param request
	 * @param delay
	 *            Delay after which request should execute
	 * @return
	 */
	private void submitWithDelay(RequestCall request, long delay)
	{
		if (request.isCancelled())
		{
			return;
		}

		Future<?> future;

		if (request.getRequestType() == Request.REQUEST_TYPE_LONG)
		{
			future = shortRequestExecuter.schedule(request, delay, TimeUnit.MILLISECONDS);
		}
		else
		{
			future = longRequestExecuter.schedule(request, delay, TimeUnit.MILLISECONDS);
		}
		request.setFuture(future);
	}

	/**
	 * Submits a response call to response executer
	 * 
	 * @param response
	 * @return
	 */
	public void submit(ResponseCall response)
	{
		responseExecuter.submit(response);
	}

	/**
	 * Called from request executer in afterExecute method . Fetches and submits next task to the executer
	 * 
	 * @param executerType
	 */
	synchronized void fetchNextTask(short executerType)
	{
		submitNext(executerType);
	}

	/**
	 * adds new task to long or short running queue Called from beforeExecute in executer
	 * 
	 * @param call
	 */
	synchronized void addRunningTask(Runnable call, short executer)
	{
		queue.addToRunningQueue(call, executer);
	}

	/**
	 * removes task from long or short running queue Called from beforeExecute in executer
	 * 
	 * @param call
	 */
	synchronized void removeRunningTask(Runnable call, short executer)
	{
		queue.removeFromRunningQueue(call, executer);
	}

	/**
	 * polls the long or short queue for new tasks and submits it to the executer
	 * 
	 * @param executerType
	 */
	private void submitNext(int executerType)
	{
		Pair<RequestCall, Short> result = queue.getNextCall(executerType);

		if (result != null && result.first != null)
		{
			Future<?> future;
			RequestCall call = result.first;
			short executer = result.second;
			if (executer == LONG_EXECUTER)
			{
				future = longRequestExecuter.submit(call);
			}
			else
			{
				future = shortRequestExecuter.submit(call);
			}
			call.setFuture(future);
		}
		else
		{
			Logger.i(TAG, "nothing to submit to executers");
		}
	}

	public void solveStarvation(RequestCall call)
	{
		queue.solveStarvation(call);
	}

	/**
	 * shutdown the executer properly clearing all the references so that all the containing objects be available for garbage collection
	 */
	public void shutDown()
	{
		shortRequestExecuter.shutdown();
		longRequestExecuter.shutdown();
		responseExecuter.shutdown();

		shortRequestExecuter = null;
		longRequestExecuter = null;
		responseExecuter = null;

		queue.shutdown();

	}
}
