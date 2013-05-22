package com.bsb.hike.models;

public class HikePacket {
	private byte[] message;
	private long msgId;
	private long timeStamp;

	public long getTimeStamp() {
		return timeStamp;
	}

	public byte[] getMessage() {
		return message;
	}

	public long getMsgId() {
		return msgId;
	}

	}

	public HikePacket(byte[] message, long msgId, long timeStamp) {
		this.message = message;
		this.msgId = msgId;
		this.timeStamp = timeStamp;
	}
}
