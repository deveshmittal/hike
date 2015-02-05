package com.bsb.hike.modules.httpmgr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class Utils
{
	private static final int BUFFER_SIZE = 4096;
	
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
	
	public static byte[] streamToBytes(InputStream stream) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		long time = System.currentTimeMillis();
		if (stream != null)
		{
			byte[] buf = new byte[BUFFER_SIZE];
			int r, count = 0;

			while ((r = stream.read(buf)) != -1)
			{
				count++;
				baos.write(buf, 0, r);
			}
			System.out.println(" stream to bytes while loop count : " + count + "   time : " + (System.currentTimeMillis() - time));
		}
		System.out.println(" stream to bytes method time : " + (System.currentTimeMillis() - time));
		return baos.toByteArray();
	}

}
