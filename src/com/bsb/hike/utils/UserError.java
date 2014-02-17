package com.bsb.hike.utils;

public class UserError extends Exception
{
	public String message;

	public int code;

	public UserError(String message, int code)
	{
		super();
		this.message = message;
		this.code = code;
	}
}
