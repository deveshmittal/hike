package com.bsb.hike.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HTTP;

public class GzipUrlEncodedFormEntity extends ByteArrayEntity
{

	public GzipUrlEncodedFormEntity(final List<? extends NameValuePair> parameters, final String encoding) throws UnsupportedEncodingException
	{
		super(gzip(parameters, encoding));
		setContentType(URLEncodedUtils.CONTENT_TYPE + HTTP.CHARSET_PARAM + HTTP.DEFAULT_CONTENT_CHARSET);
		setContentEncoding("gzip");
	}

	private static byte[] gzip(List<? extends NameValuePair> parameters, String encoding)
	{
		String encoded = URLEncodedUtils.format(parameters, encoding);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		GZIPOutputStream zos;
		try
		{
			zos = new GZIPOutputStream(bos);
			zos.write(encoded.getBytes());
			zos.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		byte[] data = bos.toByteArray();
		return data;
	}

}
