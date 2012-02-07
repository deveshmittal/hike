package com.bsb.hike.db;

public class DbException extends Exception
{
	public Exception parentExc;

	public DbException(Exception e)
	{
		this.parentExc = e;
	}

	public String toString()
	{
		return "DbException " + this.parentExc.toString();
	}
}
