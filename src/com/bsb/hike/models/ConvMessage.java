package com.bsb.hike.models;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ocpsoft.pretty.time.PrettyTime;

public class ConvMessage
{

	private long msgID;

	private Conversation mConversation;

	private String mMessage;

	private String mMsisdn;

	private String contactId;

	private long mTimestamp;

	private boolean mIsSent;
	
	private State mState;
	
	public static enum State
	{
		SENT_UNCONFIRMED, SENT_CONFIRMED , RECEIVED_UNREAD, RECEIVED_READ
	};

	public ConvMessage(String message, String msisdn, String contactId, long timestamp, State msgState)
	{
		this.contactId = contactId;
		this.mMsisdn = msisdn;
		this.mMessage = message;
		this.mTimestamp = timestamp;
		mState = msgState;
		
		mIsSent = (mState == State.SENT_UNCONFIRMED || mState == State.SENT_CONFIRMED);
	}

	public String getMessage()
	{
		return mMessage;
	}

	public boolean isSent()
	{
		return mIsSent;
	}

	public long getTimestamp()
	{
		return this.mTimestamp;
	}

	public State getState()
	{
		return mState;
	}

	public String getId()
	{
		return contactId;
	}

	public String getMsisdn()
	{
		return mMsisdn;
	}

	@Override
	public String toString()
	{
		String convId = mConversation == null ? "null" : Long.toString(mConversation.getConvId());
		return "ConvMessage [mConversation=" + convId + ", mMessage=" + mMessage + ", mMsisdn=" + mMsisdn + ", contactId=" + contactId + ", mTimestamp=" + mTimestamp
				+ ", mIsSent=" + mIsSent + ", mState=" + mState + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contactId == null) ? 0 : contactId.hashCode());
		result = prime * result + (mIsSent ? 1231 : 1237);
		result = prime * result + ((mMessage == null) ? 0 : mMessage.hashCode());
		result = prime * result + ((mMsisdn == null) ? 0 : mMsisdn.hashCode());
		result = prime * result + ((mState == null) ? 0 : mState.hashCode());
		result = prime * result + (int) (mTimestamp ^ (mTimestamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConvMessage other = (ConvMessage) obj;
		if (contactId == null)
		{
			if (other.contactId != null)
				return false;
		}
		else if (!contactId.equals(other.contactId))
			return false;
		if (mIsSent != other.mIsSent)
			return false;
		if (mMessage == null)
		{
			if (other.mMessage != null)
				return false;
		}
		else if (!mMessage.equals(other.mMessage))
			return false;
		if (mMsisdn == null)
		{
			if (other.mMsisdn != null)
				return false;
		}
		else if (!mMsisdn.equals(other.mMsisdn))
			return false;
		if (mState != other.mState)
			return false;
		if (mTimestamp != other.mTimestamp)
			return false;
		return true;
	}

	public JSONObject serialize(String type)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put("ts", mTimestamp);
			object.put("msgID", msgID); // added msgID to the JSON Object
			object.put("type", type);
			object.put("to", mMsisdn);
			object.put("body", mMessage);
		}
		catch (JSONException e)
		{
			Log.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}

	public void setConversation(Conversation conversation)
	{
		this.mConversation = conversation;
	}

	public Conversation getConversation()
	{
		return mConversation;
	}

	public String getTimestampFormatted()
	{
		Date date = new Date(mTimestamp * 1000);
		PrettyTime p = new PrettyTime();
		return p.format(date);
	}

	public void setMsgID(long msgID)
	{
		this.msgID = msgID;
	}
	public long getMsgID()
	{
		return msgID;			
	}
	
	public static State stateValue(int val)
	{
		switch(val)
		{
			case 0: return State.SENT_UNCONFIRMED;
			case 1: return State.SENT_CONFIRMED;
			case 2: return State.RECEIVED_UNREAD;
			case 3: return State.RECEIVED_READ;
			default: return State.SENT_UNCONFIRMED;
		}
	}

	public void setState(State sentConfirmed)
	{
		mState = sentConfirmed;
	}
}
