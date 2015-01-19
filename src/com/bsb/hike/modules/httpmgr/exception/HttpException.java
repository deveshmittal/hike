package com.bsb.hike.modules.httpmgr.exception;

/**
 * Super class of all exceptions in RoboSpice.
 * 
 * @author sni
 */
public class HttpException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -331202186790369469L;

	public HttpException(String detailMessage)
	{
		super(detailMessage);
	}

	public HttpException(String detailMessage, Throwable throwable)
	{
		super(detailMessage, throwable);
	}

	public HttpException(Throwable throwable)
	{
		super(throwable);
	}

}
