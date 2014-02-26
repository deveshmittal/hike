package com.bsb.hike.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

import org.apache.http.entity.ByteArrayEntity;

public class GzipByteArrayEntity extends ByteArrayEntity
{
	public GzipByteArrayEntity(final byte[] data, final String encoding) throws UnsupportedEncodingException
	{
		super(gzip(data, encoding));
		setContentType("application/json");
		setContentEncoding("gzip");
	}

	public static byte[] gzip(byte[] data, String encoding)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		GZIPOutputStream zos;
		try
		{
			zos = new GZIPOutputStream(bos);
			zos.write(data);
			zos.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return bos.toByteArray();
	}

}
