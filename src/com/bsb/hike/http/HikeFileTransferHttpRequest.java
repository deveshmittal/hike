package com.bsb.hike.http;


public class HikeFileTransferHttpRequest extends HikeHttpRequest {

	private String fileName;
	private String filePath;

	public HikeFileTransferHttpRequest(String path, HikeHttpCallback completionRunnable, String fileName, String filePath) 
	{
		super(path, completionRunnable);
		this.fileName = fileName;
		this.filePath = filePath;
	}

	public String getFileName()
	{
		return fileName;
	}

	public String getFilePath()
	{
		return filePath;
	}
}
