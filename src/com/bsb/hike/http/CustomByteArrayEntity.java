package com.bsb.hike.http;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.entity.ByteArrayEntity;

import com.bsb.hike.utils.ProgressListener;

public class CustomByteArrayEntity extends ByteArrayEntity
{

	private final ProgressListener listener;

	private CountingOutputStream countingOutputStream;

	private boolean cancel = false;

	public CustomByteArrayEntity(byte[] b)
	{
		super(b);
		this.listener = null;
	}

	public CustomByteArrayEntity(byte[] b, ProgressListener listener)
	{
		super(b);
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
