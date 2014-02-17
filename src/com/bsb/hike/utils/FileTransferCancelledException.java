package com.bsb.hike.utils;

public class FileTransferCancelledException extends Exception
{

	public FileTransferCancelledException(String message)
	{
		super(message);
	}

	public FileTransferCancelledException(String message, Throwable e)
	{
		super(message, e);
	}
}
