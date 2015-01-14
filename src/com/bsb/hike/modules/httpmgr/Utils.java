package com.bsb.hike.modules.httpmgr;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class Utils
{
	public static ThreadFactory threadFactory(final String name, final boolean daemon)
	{
		return new ThreadFactory()
		{
			private AtomicInteger i = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable runnable)
			{
				int threadCount = i.getAndIncrement();
				Thread result = new Thread(runnable);
				result.setName(name + "-" + threadCount);
				result.setDaemon(daemon);
				return result;
			}
		};
	}

	public static RejectedExecutionHandler rejectedExecutionHandler()
	{
		return new RejectedExecutionHandler()
		{
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
			{
				
			}
		};
	}
}
