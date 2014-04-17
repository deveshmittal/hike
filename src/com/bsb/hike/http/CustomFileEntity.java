package com.bsb.hike.http;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.entity.FileEntity;

import com.bsb.hike.utils.ProgressListener;

public class CustomFileEntity extends FileEntity
{

	private final ProgressListener listener;

	private CountingOutputStream countingOutputStream;

	private boolean cancel = false;

	public CustomFileEntity(File file, String contentType)
	{
		super(file, contentType);
		this.listener = null;
		// TODO Auto-generated constructor stub
	}

	public CustomFileEntity(File file, String contentType, final ProgressListener listener)
	{
		super(file, contentType);
		this.listener = listener;
	}

	@Override
	public void writeTo(final OutputStream outstream) throws IOException
	{
		if (cancel)
		{
			return;
		}
		countingOutputStream = new CountingOutputStream(outstream, this.listener);
		super.writeTo(countingOutputStream);
	}

	public void cancelDownload()
	{
		cancel = true;
		if (countingOutputStream != null)
		{
			countingOutputStream.cancel();
		}
	}
}