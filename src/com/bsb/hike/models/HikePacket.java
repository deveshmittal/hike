package com.bsb.hike.models;

import com.bsb.hike.HikeConstants;

public class HikePacket
{
	private byte[] message;

	private long msgId;

	private long timeStamp;

	private long packetId = -1;
	
	private int packetType = HikeConstants.NORMAL_MESSAGE_TYPE;

	public int getPacketType()
	{
		return packetType;
	}

	public void setPacketType(int packetType)
	{
		this.packetType = packetType;
	}

	public long getPacketId()
	{
		return packetId;
	}

	public void setPacketId(long packetId)
	{
		this.packetId = packetId;
	}

	public long getTimeStamp()
	{
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

	public HikePacket(byte[] message, long msgId, long timeStamp, int packetType)
	{
		this(message, msgId, timeStamp, -1, packetType);
	}

	public HikePacket(byte[] message, long msgId, long timeStamp, long packetId, int packetType)
	{
		this.message = message;
		this.msgId = msgId;
		this.timeStamp = timeStamp;
		this.packetId = packetId;
		this.packetType = packetType;
	}
}
