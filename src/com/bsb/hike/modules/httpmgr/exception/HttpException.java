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
	private static final long serialVersionUID = 647946546917050370L;
	
	public static final short REASON_CODE_UNEXPECTED_ERROR = 0x0;
	
	public static final short REASON_CODE_NO_NETWORK = 0x1;
	
	public static final short REASON_CODE_SOCKET_TIMEOUT = 0x2;
	
	public static final short REASON_CODE_CONNECTION_TIMEOUT = 0x3;
	
	public static final short REASON_CODE_MALFORMED_URL = 0x4;
	
	public static final short REASON_CODE_SERVER_ERROR = 0x5;
	
	public static final short REASON_CODE_AUTH_FAILURE = 0x6;
	
	public static final short REASON_CODE_CANCELLATION = 0x7;
	
	private int errorCode;

	public HttpException(short errorCode)
	{
		super();
		this.errorCode = errorCode;
	}

	public HttpException(String message)
	{
		this(0, message);
	}

	public HttpException(Throwable thr)
	{
		this(0, thr);
	}

	public HttpException(String errorMsg, Throwable thr)
	{
		this(0, errorMsg, thr);
	}

	public HttpException(int errorCode, String message)
	{
		super(message);
		this.errorCode = errorCode;
	}

	public HttpException(int errorCode, Throwable thr)
	{
		super(thr);
		this.errorCode = errorCode;
	}

	public HttpException(int errorCode, String errorMsg, Throwable thr)
	{
		super(errorMsg, thr);
		this.errorCode = errorCode;
	}

	public int getErrorCode()
	{
		return errorCode;
	}

}
