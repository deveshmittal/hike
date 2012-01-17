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

	private long mTimestamp;

	private boolean mIsSent;
	
	private State mState;

	public static enum State
	{
		SENT_UNCONFIRMED, SENT_CONFIRMED , SENT_DELIVERED, SENT_DELIVERED_READ , RECEIVED_UNREAD, RECEIVED_READ, UNKNOWN
	};

	public ConvMessage(String message, String msisdn, long timestamp, State msgState)
	{
		this.mMsisdn = msisdn;
		this.mMessage = message;
		this.mTimestamp = timestamp;
		setState(msgState);
	}

	public ConvMessage(JSONObject obj) throws JSONException
	{
		this.mMsisdn = obj.getString("from");
		JSONObject data = obj.getJSONObject("data");
		this.mMessage = data.getString("msg");

		this.mTimestamp = data.getLong("ts");

		/* prevent us from receiving a message from the future */
		long now = System.currentTimeMillis()/1000;
		this.mTimestamp = (this.mTimestamp > now) ? now : this.mTimestamp;
		/* if we're deserialized an object from json, it's always unread */
		setState(State.RECEIVED_UNREAD);
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

	public String getMsisdn()
	{
		return mMsisdn;
	}

	@Override
	public String toString()
	{
		String convId = mConversation == null ? "null" : Long.toString(mConversation.getConvId());
		return "ConvMessage [mConversation=" + convId + ", mMessage=" + mMessage + ", mMsisdn=" + mMsisdn + ", mTimestamp=" + mTimestamp
				+ ", mIsSent=" + mIsSent + ", mState=" + mState + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
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
			case 2: return State.SENT_DELIVERED;
			case 3: return State.SENT_DELIVERED_READ;
			case 4: return State.RECEIVED_UNREAD;
			case 5: return State.RECEIVED_READ;
			default: return State.UNKNOWN;
		}
	}

	public void setState(State sentConfirmed)
	{
		mState = sentConfirmed;
		mIsSent = (mState == State.SENT_UNCONFIRMED || mState == State.SENT_CONFIRMED || mState == State.SENT_DELIVERED || mState == State.SENT_DELIVERED_READ);
	}

	public JSONObject serializeDeliveryReport(String type)
	{
				JSONObject object = new JSONObject();
				try
				{
					object.put("ts", mTimestamp);
					object.put("msgID", msgID); // added msgID to the JSON Object
					object.put("type", type);
					object.put("to", mMsisdn);
				}
				catch (JSONException e)
				{
					Log.e("ConvMessage", "invalid json message", e);
				}
				return object;
	}
}
