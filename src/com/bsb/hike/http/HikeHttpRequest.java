package com.bsb.hike.http;

import org.json.JSONObject;

public class HikeHttpRequest
{

	public enum Method
	{
		GET,
		POST;

	};

	private String mPath;
	private JSONObject mJSONData;
	private Runnable mSuccessRunnable;

	public HikeHttpRequest(String path, Runnable successRunnable)
	{
		this.mPath = path;
		this.mSuccessRunnable = successRunnable;
	}

	public JSONObject getJSONData()
	{
		return mJSONData;
	}
	
	public void setJSONData(JSONObject json)
	{
		this.mJSONData = json;
	}

	public String getPath()
	{
		return mPath;
	}

	public void onSuccess()
	{
		if (mSuccessRunnable != null)
		{
			mSuccessRunnable.run();
		}
	}
}
