package com.bsb.hike.modules.httpmgr.engine;

import static com.bsb.hike.modules.httpmgr.engine.HttpEngineConstants.CORE_POOL_SIZE;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_LONG;
import static com.bsb.hike.modules.httpmgr.request.Request.REQUEST_TYPE_SHORT;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Pair;

import com.bsb.hike.modules.httpmgr.request.Request;
import com.bsb.hike.modules.httpmgr.request.RequestCall;
import com.bsb.hike.utils.Logger;

public class HttpQueue
{

	private static final long serialVersionUID = 1L;

	private static String TAG = "HTTP_QUEUE";

	private PriorityQueue<RequestCall> shortQueue;

	private PriorityQueue<RequestCall> longQueue;

	private volatile AtomicInteger numShortRunningCalls = new AtomicInteger(0);

	private volatile AtomicInteger numLongRunningCalls = new AtomicInteger(0);

	private short MAX_QUEUE_SIZE = CORE_POOL_SIZE;

	HttpQueue()
	{
		// Priority Queue used for storing short requests submitted to the engine
		shortQueue = new PriorityQueue<RequestCall>();

		// Priority Queue used for storing long requests submitted to engine
		longQueue = new PriorityQueue<RequestCall>();
	}

	/**
	 * add the request to short or long queues
	 * 
	 * @param request
	 */
	void add(RequestCall request)
	{
		if (request.getRequestType() == REQUEST_TYPE_LONG)
		{
			longQueue.add(request);
		}
		else
		{
			shortQueue.add(request);
		}
	}

	/**
	 * increments the number of running calls
	 * 
	 * @param executer
	 *            executerType
	 */
	void incrementRunningTasksSize(short executer)
	{
		if (executer == HttpEngine.LONG_EXECUTER)
		{
			numLongRunningCalls.incrementAndGet();
		}
		else
		{
			numShortRunningCalls.incrementAndGet();
		}
	}

	/**
	 * decrements the number of running calls
	 * 
	 * @param executer
	 *            executerType
	 */
	void decrementRunningTasksSize(short executer)
	{
		if (executer == HttpEngine.LONG_EXECUTER)
		{
			numLongRunningCalls.decrementAndGet();
		}
		else
		{
			numShortRunningCalls.decrementAndGet();
		}
	}

	/**
	 * <p>
	 * Gets the next call for httpEngine
	 * </p>
	 * 
	 * <p>
	 * It works on principle that short tasks can be executed on both short and long executers if there is space Long tasks can be executed only on long executer
	 * </p>
	 * 
	 * @param executerType
	 * @return pair of next call and type of executer on which request will be executed
	 */
	synchronized Pair<RequestCall, Short> getNextCall(int executerType)
	{
		RequestCall call = null;
		RequestCall nextCall = null;
		short executer = HttpEngine.LONG_EXECUTER;

		while (null == nextCall)
		{

			if (executerType == HttpEngine.SHORT_EXECUTER)
			{
				// short queue has some tasks, fetch and return it to engine to be executed on short executer
				if (shortQueue.size() > 0)
				{
					call = shortQueue.poll();
					if (!call.isCancelled())
					{
						nextCall = call;
						executer = HttpEngine.SHORT_EXECUTER;
					}

				}
				else
				{
					// no tasks to execute
					Logger.i(TAG, "no tasks to execute");
					break;
				}
			}
			else
			{
				// long queue has some tasks, fetch and return it to engine to be executed on long executer
				if (longQueue.size() > 0)
				{
					call = longQueue.poll();
					if (!call.isCancelled())
					{
						nextCall = call;
						executer = HttpEngine.LONG_EXECUTER;
					}
				}
				else
				{
					// long queue is empty. poll short queue
					// short queue has some tasks, fetch and return it to engine to be executed on long executer
					if (shortQueue.size() > 0)
					{
						call = shortQueue.poll();
						if (!call.isCancelled())
						{
							nextCall = call;
							executer = HttpEngine.LONG_EXECUTER;
						}
					}
					else
					{
						// no tasks to execute
						Logger.i(TAG, "no tasks to execute");
						break;
					}
				}
			}
		}
		return new Pair<RequestCall, Short>(nextCall, executer);
	}

	/**
	 * Checks if there is space available in long or short executer by checking size of long or short running queues
	 * 
	 * @param requestType
	 *            type of request (long or short)
	 * @return
	 */
	boolean spaceAvailable(int requestType)
	{
		int longSize = numLongRunningCalls.get();
		int shortSize = numShortRunningCalls.get();

		if (requestType == Request.REQUEST_TYPE_LONG)
		{
			if (numLongRunningCalls.get() < MAX_QUEUE_SIZE)
			{
				return true;
			}
		}
		else
		{
			if (numShortRunningCalls.get() < MAX_QUEUE_SIZE)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * clear queue and remove references
	 */
	void shutdown()
	{
		longQueue.clear();
		shortQueue.clear();
		longQueue = null;
		shortQueue = null;
		numLongRunningCalls = null;
		numShortRunningCalls = null;
	}

	public void solveStarvation(RequestCall call)
	{
		if (call.getRequestType() == REQUEST_TYPE_SHORT)
		{
			changePriority(shortQueue, call);
		}
		else
		{
			changePriority(longQueue, call);
		}
	}

	private void changePriority(PriorityQueue<RequestCall> queue, RequestCall call)
	{
		List<RequestCall> requestCallsList = new ArrayList<RequestCall>();

		int size = queue.size();
		for (int index = size - 1; index >= 0; --index)
		{
			RequestCall requestCall = queue.peek();

			if (requestCall == null)
			{
				break;
			}

			if (requestCall.getSubmissionTime() < call.getSubmissionTime() && requestCall.getPriority() > call.getPriority())
			{
				queue.remove();
				requestCall.setPriority(requestCall.getPriority() - 1);
				requestCallsList.add(requestCall);
			}
		}
		queue.addAll(requestCallsList);
	}

}
