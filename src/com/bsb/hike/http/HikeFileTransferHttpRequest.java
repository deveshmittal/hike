package com.bsb.hike.http;


public class HikeFileTransferHttpRequest extends HikeHttpRequest {

	private String fileName;
	private String filePath;
	private String fileType;

	public HikeFileTransferHttpRequest(String path, HikeHttpCallback completionRunnable, String fileName, String filePath, String fileType) 
	{
		super(path, completionRunnable);
		this.fileName = fileName;
		this.filePath = filePath;
		this.fileType = fileType;
	}

	public String getFileName()
	{
		return fileName;
	}

	public String getFilePath()
	{
		return filePath;
	}

	public String getFileType()
	{
		return fileType;
	}
}
