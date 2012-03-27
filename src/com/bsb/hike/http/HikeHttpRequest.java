package com.bsb.hike.http;

import org.json.JSONObject;

public class HikeHttpRequest
{

	public static abstract class HikeHttpCallback
	{
		public void onSuccess() {}
		public void onFailure() {}
	}

	public enum Method
	{
		GET,
		POST;

	};

	private String mPath;
	private JSONObject mJSONData;
	private HikeHttpCallback mCompletionRunnable;
	private byte[] mPostData;

	public HikeHttpRequest(String path, HikeHttpCallback completionRunnable)
	{
		this.mPath = path;
		this.mCompletionRunnable = completionRunnable;
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
		if (mCompletionRunnable != null)
		{
			mCompletionRunnable.onSuccess();
		}
	}

	public void setPostData(byte[] bytes)
	{
		this.mPostData = bytes;
	}

	public byte[] getPostData()
	{
		if (mPostData != null)
		{
			return mPostData;
		}

		return mJSONData.toString().getBytes();
	}

	public void onFailure()
	{
		if (mCompletionRunnable != null)
		{
			mCompletionRunnable.onFailure();
		}
	}

	public String getContentType()
	{
		if (mJSONData != null)
		{
			return "application/json";
		}
		else
		{
			return "";
		}
	}
}
