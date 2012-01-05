package com.bsb.hike.models;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ConvMessage {
	public ConvMessage(String message, String msisdn, String contactId, int timestamp, boolean isSent) {
		this.contactId = contactId;
		this.mMsisdn = msisdn;
		this.mMessage = message;
		this.mTimestamp = timestamp;
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

	public String getId() {
		return contactId;
	}

	public String getMsisdn() {
		return mMsisdn;
	}

	@Override
	public String toString() {
	    String convId = mConversation == null ? "null" : Long.toString(mConversation.getConvId());
		return "ConvMessage [mConversation=" + convId + ", mMessage="
				+ mMessage + ", mMsisdn=" + mMsisdn + ", contactId="
				+ contactId + ", mTimestamp=" + mTimestamp + ", mIsSent="
				+ mIsSent + ", mState=" + mState + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contactId == null) ? 0 : contactId.hashCode());
		result = prime * result + (mIsSent ? 1231 : 1237);
		result = prime * result
				+ ((mMessage == null) ? 0 : mMessage.hashCode());
		result = prime * result + ((mMsisdn == null) ? 0 : mMsisdn.hashCode());
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
		ConvMessage other = (ConvMessage) obj;
		if (contactId == null) {
			if (other.contactId != null)
				return false;
		} else if (!contactId.equals(other.contactId))
			return false;
		if (mConversation == null) {
			if (other.mConversation != null)
				return false;
		} else if (!mConversation.equals(other.mConversation))
			return false;
		if (mIsSent != other.mIsSent)
			return false;
		if (mMessage == null) {
			if (other.mMessage != null)
				return false;
		} else if (!mMessage.equals(other.mMessage))
			return false;
		if (mMsisdn == null) {
			if (other.mMsisdn != null)
				return false;
		} else if (!mMsisdn.equals(other.mMsisdn))
			return false;
		if (mState != other.mState)
			return false;
		if (mTimestamp != other.mTimestamp)
			return false;
		return true;
	}

	private Conversation mConversation;
	private String mMessage;
	private String mMsisdn;
	private String contactId;
	private int mTimestamp;
	private boolean mIsSent;
	public enum State {SENT, DELIVERED, RECEIVED };
	private State mState;

	public JSONObject serialize(String type) {
		JSONObject object = new JSONObject();
		try {
			object.put("type", type);
			object.put("to", mMsisdn);
			object.put("body", mMessage);
		} catch (JSONException e) {
			Log.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}

	public void setConversation(Conversation conversation) {
		this.mConversation = conversation;
	}

	public Conversation getConversation() {
		return mConversation;
	}

	public String getTimestampFormatted() {
		SimpleDateFormat dfm = new SimpleDateFormat("HH:mm");
		Date date = new Date(mTimestamp * 1000);
		String dateFormatted = dfm.format(date);
		return dateFormatted;
	}
}
