package com.bsb.hike.models;

public class HikePacket
{
	private byte[] message;
	private long msgId;

	public byte[] getMessage()
	{
		return message;
	}

	public long getMsgId()
	{
		return msgId;
	}

	public HikePacket(byte[] message, long msgId)
	{
		this.message = message;
		this.msgId = msgId;
		
	}
}
