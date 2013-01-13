package com.bsb.hike.http;

import org.json.JSONObject;

public class HikeHttpRequest {

	public static abstract class HikeHttpCallback {
		public void onSuccess(JSONObject response) {
		}

		public void onFailure() {
		}
	}

	public enum Method {
		GET, POST;

	};

	private String mPath;
	private JSONObject mJSONData;
	private HikeHttpCallback mCompletionRunnable;
	private byte[] mPostData;
	private JSONObject response;
	private String filePath;

	public HikeHttpRequest(String path, HikeHttpCallback completionRunnable) {
		this.mPath = path;
		this.mCompletionRunnable = completionRunnable;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFilePath() {
		return filePath;
	}

	public JSONObject getJSONData() {
		return mJSONData;
	}

	public void setJSONData(JSONObject json) {
		this.mJSONData = json;
	}

	public String getPath() {
		return mPath;
	}

	public void onSuccess() {
		if (mCompletionRunnable != null) {
			mCompletionRunnable.onSuccess(response);
		}
	}

	public void setPostData(byte[] bytes) {
		this.mPostData = bytes;
	}

	public byte[] getPostData() {
		if (mPostData != null) {
			return mPostData;
		}

		return mJSONData.toString().getBytes();
	}

	public void onFailure() {
		if (mCompletionRunnable != null) {
			mCompletionRunnable.onFailure();
		}
	}

	public String getContentType() {
		if (mJSONData != null) {
			return "application/json";
		} else {
			return "";
		}
	}

	public void setResponse(JSONObject response) {
		this.response = response;
	}

	public JSONObject getResponse() {
		return response;
	}
}
