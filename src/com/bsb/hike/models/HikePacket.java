package com.bsb.hike.models;

public class HikePacket
{
	private boolean retry;
	private byte[] message;
	private long msgId;
	private long timeStamp;

	public long getTimeStamp() {
		return timeStamp;
	}

	public byte[] getMessage()
	{
		return message;
	}

	public long getMsgId()
	{
		return msgId;
	}

	public boolean shouldRetry()
	{
		return retry;
	}

	public void setRetry(boolean val)
	{
		retry = val;
	}

	public HikePacket(byte[] message, long msgId, long timeStamp)
	{
		this.message = message;
		this.msgId = msgId;
		this.retry = true;
		this.timeStamp = timeStamp;
	}
}
