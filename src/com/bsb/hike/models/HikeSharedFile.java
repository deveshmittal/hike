package com.bsb.hike.models;

import org.json.JSONObject;

public class HikeSharedFile extends HikeFile
{
	private long msgId;

	private String msisdn;

	private long timeStamp;

	public HikeSharedFile(JSONObject fileJSON, boolean isSent, long msgId, String msisdn, long timeStamp)
	{
		super(fileJSON, isSent);

		this.msgId = msgId;

		this.msisdn = msisdn;

		this.timeStamp = timeStamp;
	}

	public long getMsgId()
	{
		return msgId;
	}

	public void setMsgId(long msgId)
	{
		this.msgId = msgId;
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}

}
