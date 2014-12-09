package com.bsb.hike.analytics;

import android.content.Context;

/**
 * @author rajesh
 *
 */
class AnalyticsSender implements Runnable  
{
	private Context context = null;
	
	public AnalyticsSender(Context context)
	{
		this.context = context;
	}
	
	public synchronized void sendData()
	{
		
	}
	
	public void retry()
	{
		
	}
	
	@Override
	public void run() 
	{
	}
}
