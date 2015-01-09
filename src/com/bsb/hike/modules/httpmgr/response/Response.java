package com.bsb.hike.modules.httpmgr.response;

import java.util.List;

import android.text.TextUtils;

import com.bsb.hike.modules.httpmgr.Header;

/**
 * Encapsulates all of the information necessary to make an HTTP response.
 * 
 * @author sidharth
 */
public class Response
{
	private String url;

	private int statusCode;

	private String reason;

	private List<Header> headers;

	private byte[] body;

	private Response(Builder builder)
	{
		this.url = builder.url;
		this.statusCode = builder.statusCode;
		this.reason = builder.reason;
		this.headers = builder.headers;
		this.body = builder.body;
	}

	/**
	 * Returns the url of the response
	 * 
	 * @return
	 */
	public String getUrl()
	{
		return url;
	}

	/**
	 * Returns the status code of the response
	 * 
	 * @return
	 */
	public int getStatusCode()
	{
		return statusCode;
	}

	/**
	 * Returns the reason of the response
	 * 
	 * @return
	 */
	public String getReason()
	{
		return reason;
	}

	/**
	 * Returns the list of headers of this response
	 * 
	 * @return
	 */
	public List<Header> getHeaders()
	{
		return headers;
	}

	/**
	 * Returns the body of the response
	 * 
	 * @return
	 */
	public byte[] getBody()
	{
		return body;
	}

	public static class Builder
	{
		private String url;

		private int statusCode = -1;

		private String reason;

		private List<Header> headers;

		private byte[] body;

		/**
		 * Sets the url of the response
		 * 
		 * @param url
		 * @return
		 */
		public Builder setUrl(String url)
		{
			this.url = url;
			return this;
		}

		/**
		 * Sets the status code of the response
		 * 
		 * @param statusCode
		 * @return
		 */
		public Builder setStatusCode(int statusCode)
		{
			this.statusCode = statusCode;
			return this;
		}

		/**
		 * Sets the reason of the response
		 * 
		 * @param reason
		 * @return
		 */
		public Builder setReason(String reason)
		{
			this.reason = reason;
			return this;
		}

		/**
		 * Sets the headers list of the response
		 * 
		 * @param headers
		 * @return
		 */
		public Builder setHeaders(List<Header> headers)
		{
			this.headers = headers;
			return this;
		}

		/**
		 * Sets the body of the response
		 * 
		 * @param body
		 * @return
		 */
		public Builder setBody(byte[] body)
		{
			this.body = body;
			return this;
		}

		/**
		 * Returns the {@link Response} object built using this builder
		 * 
		 * @return
		 */
		public Response build()
		{
			ensureSaneDefaults();
			return new Response(this);
		}

		private void ensureSaneDefaults()
		{
			if (TextUtils.isEmpty(url))
			{
				throw new IllegalStateException("Url must not be null and its length must be greater than 0");
			}
			if (statusCode < 0)
			{
				throw new IllegalStateException("status code < 0 " + statusCode);
			}
		}
	}
}
