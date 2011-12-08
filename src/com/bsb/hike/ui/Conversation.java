package com.bsb.hike.ui;

public class Conversation {
	public Conversation(String message, String to, int timeestamp, boolean isSent, boolean isIP) {
		this.mTo = to;
		this.mMessage = message;
		this.mTimestamp = timeestamp;
		this.mIsSent = isSent;
		this.mIsIP = isIP;
	}

	public String getMessage() {
		return mMessage;
	}

	public boolean isSent() {
		return mIsSent;
	}

	public boolean isIP() {
		return mIsIP;
	}

	public int getTimestamp() {
		return this.mTimestamp;
	}

	public State getState() {
		return mState;
	}

	public String getTo() {
		return mTo;
	}

	private String mMessage;
	private String mTo;
	private int mTimestamp;
	private boolean mIsSent;
	public enum State {SENT, DELIVERED, RECEIVED };
	private State mState;
	private boolean mIsIP;
}
