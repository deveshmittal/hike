package com.bsb.hike.modules.httpmgr.request;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;

import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.Utils;

public class ByteArrayRequest extends Request<byte[]>
{
	public ByteArrayRequest(Init<?> init)
	{
		super(init);
	}

	protected static abstract class Init<S extends Init<S>> extends Request.Init<S>
	{
		public RequestToken build()
		{
			ByteArrayRequest request = new ByteArrayRequest(this);
			RequestToken token = new RequestToken(request);
			return token;
		}
	}

	public static class Builder extends Init<Builder>
	{
		@Override
		protected Builder self()
		{
			return this;
		}
	}

	@Override
	public byte[] parseResponse(InputStream in) throws IOException
	{
		return Utils.streamToBytes(in);
	}
}
