package com.bsb.hike.models;

import android.os.Handler;
import android.os.HandlerThread;

import com.bsb.hike.utils.Utils;

public class HikeHandlerUtil
{

	private Handler mHandler = null;

	HandlerThread mHandlerThread = null;

	/**
	 * 
	 * @author himanshu
	 * 
	 *         A static object for making the class singleton and synchronized.
	 * 
	 */
	private static HikeHandlerUtil mHikeHandlerUtil = new HikeHandlerUtil();

	private HikeHandlerUtil()
	{
	}

	public static HikeHandlerUtil getInstance()
	{
		return mHikeHandlerUtil;
	}

	/**
	 * Responsible for starting the thread if not already started
	 */
	public void startHandlerThread()
	{
		if (mHandler == null || mHandlerThread == null || !mHandlerThread.isAlive())
		{
			mHandlerThread = new HandlerThread("HikeHandlerUtil");
			mHandlerThread.start();
			mHandler = new Handler(mHandlerThread.getLooper());

		}

	}

	/**
	 * Posting task on the handler.
	 */
	public void postRunnableWithDelay(Runnable runnable, long delay)
	{

		mHandler.postDelayed(runnable, delay);
	}

	/**
	 * Removing runnable from the Handler queue
	 * 
	 * @param runnable
	 */
	public void removeRunnable(Runnable runnable)
	{
		mHandler.removeCallbacks(runnable);

	}

	/**
	 * Closing all the handlers and object
	 * 
	 */
	public void onDestroy()
	{
		if (mHandlerThread != null && mHandlerThread.isAlive())

		{
			if (Utils.isKitkatOrHigher())
			{
				mHandlerThread.quitSafely();
			}
			else
			{
				mHandlerThread.quit();
			}
		}

		mHandler = null;
		mHandlerThread = null;

		mHikeHandlerUtil = null;
	}
}
