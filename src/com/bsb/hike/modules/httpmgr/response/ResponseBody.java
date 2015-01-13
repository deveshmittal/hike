package com.bsb.hike.modules.httpmgr.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Contains the mime type , content and content length of the response body
 * 
 */
public class ResponseBody
{
	private static final int BUFFER_SIZE = 4096;

	private String mimeType;

	private int contentLength;

	private byte[] content;

	private ResponseBody(String mimeType, int contentLength, byte[] content)
	{
		this.mimeType = mimeType;
		this.contentLength = contentLength;
		this.content = content;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	public int getContentLength()
	{
		return contentLength;
	}

	public void setContentLength(int contentLength)
	{
		this.contentLength = contentLength;
	}

	public byte[] getContent()
	{
		return content;
	}

	public void setContent(byte[] content)
	{
		this.content = content;
	}

	/**
	 * Returns the {@link ResponseBody} object using mime type and content byte array
	 * 
	 * @param mimeType
	 * @param content
	 * @return
	 */
	public static ResponseBody create(String mimeType, byte[] content)
	{
		return new ResponseBody(mimeType, content.length, content);
	}

	/**
	 * Returns the {@link ResponseBody} object using mimetype and input stream
	 * 
	 * @param mimeType
	 * @param contentLength
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static ResponseBody create(String mimeType, int contentLength, InputStream in) throws IOException
	{
		if (contentLength > Integer.MAX_VALUE)
		{
			throw new IOException("Cannot buffer entire body for content length: " + contentLength);
		}

		byte[] content = streamToBytes(in);
		return new ResponseBody(mimeType, contentLength, content);
	}

	private static byte[] streamToBytes(InputStream stream) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		long time = System.currentTimeMillis();
		if (stream != null)
		{
			byte[] buf = new byte[BUFFER_SIZE];
			int r, count = 0;

			while ((r = stream.read(buf)) != -1)
			{
				count++;
				baos.write(buf, 0, r);
			}
			System.out.println(" stream to bytes while loop count : " + count + "   time : " + (System.currentTimeMillis() - time));
		}
		System.out.println(" stream to bytes method time : " + (System.currentTimeMillis() - time));
		return baos.toByteArray();
	}

}
