package com.bsb.hike.modules.httpmgr.request;

import java.util.concurrent.Future;

public abstract class RequestCall implements Runnable, Comparable<RequestCall>
{

	private Request request;

	public RequestCall(Request request)
	{
		this.request = request;
	}

	@Override
	public void run()
	{
		execute();
	}

	public abstract void execute();

	private int getPriority()
	{
		return request.getPriority();
	}

	public int getRequestType()
	{
		return request.getRequestType();
	}

	public boolean isCancelled()
	{
		return request.isCancelled();
	}

	public void setFuture(Future<?> future)
	{
		request.setFuture(future);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + this.getPriority();
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		RequestCall other = (RequestCall) obj;
		if (this.getPriority() != other.getPriority())
		{
			return false;
		}

		return true;
	}

	@Override
	public int compareTo(RequestCall another)
	{
		if (this == another)
		{
			return 0;
		}
		return this.getPriority() - another.getPriority();
	}

}
