package com.bsb.hike.modules.httpmgr.engine;

import static com.bsb.hike.modules.httpmgr.engine.HttpEngineConstants.KEEP_ALIVE_TIME;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import android.os.Build;

/**
 * 
 * @author anubhavgupta
 * 
 */
public class HttpExecuter extends ScheduledThreadPoolExecutor
{

	private HttpEngine engine;

	short executerType;

	public HttpExecuter(HttpEngine engine, short executerType, int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler)
	{
		super(corePoolSize, threadFactory, handler);
		this.engine = engine;
		this.executerType = executerType;

	}
	
	@Override
	protected void beforeExecute(Thread t, Runnable r)
	{
		engine.incrementRunningTasksSize(executerType); // removes this request from running task queue
		super.beforeExecute(t, r);
	}

	@Override
	public void allowCoreThreadTimeOut(boolean value)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
		{
			super.allowCoreThreadTimeOut(true);
		}
	}

	@Override
	public void setKeepAliveTime(long time, TimeUnit unit)
	{
		super.setKeepAliveTime(KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t)
	{
		super.afterExecute(r, t);
		engine.decrementRunningTasksSize(executerType); // removes this request from running task queue
		engine.fetchNextTask(executerType); // fetch the next task to execute
	}

}
