package com.bsb.hike.models;

public class Conversation {
	public Conversation(String message, long id, int timeestamp, boolean isSent) {
		this.mId = id;
		this.mMessage = message;
		this.mTimestamp = timeestamp;
		this.mIsSent = isSent;
	}

	public String getMessage() {
		return mMessage;
	}

	public boolean isSent() {
		return mIsSent;
	}

	public int getTimestamp() {
		return this.mTimestamp;
	}

	public State getState() {
		return mState;
	}

	public long getId() {
		return mId;
	}


	@Override
	public String toString() {
		return "Conversation [mMessage=" + mMessage + ", mId=" + mId
				+ ", mTimestamp=" + mTimestamp + ", mIsSent=" + mIsSent
				+ ", mState=" + mState + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mId ^ (mId >>> 32));
		result = prime * result + (mIsSent ? 1231 : 1237);
		result = prime * result
				+ ((mMessage == null) ? 0 : mMessage.hashCode());
		result = prime * result + ((mState == null) ? 0 : mState.hashCode());
		result = prime * result + mTimestamp;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Conversation other = (Conversation) obj;
		if (mId != other.mId)
			return false;
		if (mIsSent != other.mIsSent)
			return false;
		if (mMessage == null) {
			if (other.mMessage != null)
				return false;
		} else if (!mMessage.equals(other.mMessage))
			return false;
		if (mState != other.mState)
			return false;
		if (mTimestamp != other.mTimestamp)
			return false;
		return true;
	}


	private String mMessage;
	private long mId;
	private int mTimestamp;
	private boolean mIsSent;
	public enum State {SENT, DELIVERED, RECEIVED };
	private State mState;
}
