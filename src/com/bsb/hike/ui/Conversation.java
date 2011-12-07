package com.bsb.hike.ui;

public class Conversation {
	public Conversation(String message, int timeestamp, boolean isSent, boolean isIP) {
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

	private String mMessage;
	private int mTimestamp;
	private boolean mIsSent;
	public enum State {SENT, DELIVERED, RECEIVED };
	private State mState;
	private boolean mIsIP;
}
