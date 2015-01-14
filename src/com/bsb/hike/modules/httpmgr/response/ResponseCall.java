package com.bsb.hike.modules.httpmgr.response;

public abstract class ResponseCall implements Runnable
{
	@Override
	public void run()
	{
		execute();
	}

	public abstract void execute();
}
