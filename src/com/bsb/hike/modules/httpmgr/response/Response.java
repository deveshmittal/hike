package com.bsb.hike.modules.httpmgr.response;

import java.util.List;

import com.bsb.hike.modules.httpmgr.utils.Header;

public class Response
{
	private String url;

	private int status;

	private String reason;

	private List<Header> headers;

	private byte[] body;

	private Response(Builder builder)
	{
		this.url = builder.url;
		this.status = builder.status;
		this.reason = builder.reason;
		this.headers = builder.headers;
		this.body = builder.body;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public int getStatus()
	{
		return status;
	}

	public void setStatus(int status)
	{
		this.status = status;
	}

	public String getReason()
	{
		return reason;
	}

	public void setReason(String reason)
	{
		this.reason = reason;
	}

	public List<Header> getHeaders()
	{
		return headers;
	}

	public void setHeaders(List<Header> headers)
	{
		this.headers = headers;
	}

	public byte[] getBody()
	{
		return body;
	}

	public void setBody(byte[] body)
	{
		this.body = body;
	}

	public static class Builder
	{
		private String url;

		private int status;

		private String reason;

		private List<Header> headers;

		private byte[] body;

		public Builder(String url)
		{
			this.url = url;
		}

		public String getUrl()
		{
			return url;
		}

		public void setUrl(String url)
		{
			this.url = url;
		}

		public int getStatus()
		{
			return status;
		}

		public void setStatus(int status)
		{
			this.status = status;
		}

		public String getReason()
		{
			return reason;
		}

		public void setReason(String reason)
		{
			this.reason = reason;
		}

		public List<Header> getHeaders()
		{
			return headers;
		}

		public void setHeaders(List<Header> headers)
		{
			this.headers = headers;
		}

		public byte[] getBody()
		{
			return body;
		}

		public void setBody(byte[] body)
		{
			this.body = body;
		}

		public Response build()
		{
			return new Response(this);
		}
	}
}
