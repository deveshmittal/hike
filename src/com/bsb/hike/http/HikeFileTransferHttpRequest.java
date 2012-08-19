package com.bsb.hike.http;


public class HikeFileTransferHttpRequest extends HikeHttpRequest {

	private String fileName;

	public HikeFileTransferHttpRequest(String path, HikeHttpCallback completionRunnable, String fileName) 
	{
		super(path, completionRunnable);
		this.fileName = fileName;
	}

	public String getFileName()
	{
		return fileName;
	}
}
