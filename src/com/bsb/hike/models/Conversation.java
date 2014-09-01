package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Logger;

public class Conversation implements Comparable<Conversation>
{

	public static class ConversationComparator implements Comparator<Conversation>
	{
		/*
		 * This comparator reverses the order of the normal comparable
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Conversation lhs, Conversation rhs)
		{
			if (rhs == null)
			{
				return 1;
			}

			if (lhs instanceof ConversationTip)
			{
				return -1;
			}
			else if (rhs instanceof ConversationTip)
			{
				return 1;
			}

			return rhs.compareTo(lhs);
		}
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	@Override
	public String toString()
	{
		return "Conversation [msisdn=" + msisdn + ", messages=" + messages.size() + ", contactName=" + contactName + ", onhike=" + onhike + "]";
	}

	private String msisdn;

	private List<ConvMessage> messages;

	private String contactName;

	private boolean onhike;

	private int unreadCount;

	private int unreadPinCount;

	private String lastPin;

	private MetaData metaData;

	public String getLastPin()
	{
		return lastPin;
	}

	public void setLastPin(String string)
	{
		this.lastPin = string;
	}
	

	public int getUnreadPinCount()
	{
		return unreadPinCount;
	}

	public void setUnreadPinCount(int unreadPinCount)
	{
		this.unreadPinCount = unreadPinCount;
	}

	private boolean isStealth;

	public void setOnhike(boolean onhike)
	{
		this.onhike = onhike;
	}

	public String getContactName()
	{
		return contactName;
	}

	/*
	 * Returns a friendly name for this conversation (name if non-empty, otherwise msisdn)
	 */
	public String getLabel()
	{
		return TextUtils.isEmpty(contactName) ? msisdn : contactName;
	}

	public Conversation(String msisdn)
	{
		this(msisdn, null, false);
	}

	public Conversation(String msisdn, String contactName, boolean onhike)
	{
		this(msisdn, contactName, onhike, false);
	}

	public Conversation(String msisdn, String contactName, boolean onhike, boolean isStealth)
	{
		this.msisdn = msisdn;
		this.contactName = contactName;
		this.onhike = onhike;
		this.isStealth = isStealth;
		this.messages = new ArrayList<ConvMessage>();
	}

	public boolean isOnhike()
	{
		return onhike;
	}

	/* TODO this should be addAll to conform w/ normal java semantics */
	public void setMessages(List<ConvMessage> messages)
	{
		this.messages = messages;
	}

	public void addMessage(ConvMessage message)
	{
		this.messages.add(message);
	}

	public int getUnreadCount()
	{
		return unreadCount;
	}

	public void setUnreadCount(int unreadCount)
	{
		this.unreadCount = unreadCount;
	}

	@Override
	public int compareTo(Conversation rhs)
	{
		if (this.equals(rhs))
		{
			return 0;
		}

		long ts = messages.isEmpty() ? 0 : messages.get(messages.size() - 1).getTimestamp();
		if (rhs == null)
		{
			return 1;
		}

		long rhsTs = rhs.messages.isEmpty() ? 0 : rhs.messages.get(rhs.messages.size() - 1).getTimestamp();

		if (rhsTs != ts)
		{
			return (ts < rhsTs) ? -1 : 1;
		}

		int ret = msisdn.compareTo(rhs.msisdn);
		if (ret != 0)
		{
			return ret;
		}

		return 0;
	}

	public List<ConvMessage> getMessages()
	{
		return messages;
	}

	public void setIsStealth(boolean isStealth)
	{
		this.isStealth = isStealth;
	}

	public boolean isStealth()
	{
		return isStealth;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contactName == null) ? 0 : contactName.hashCode());
		result = prime * result + ((msisdn == null) ? 0 : msisdn.hashCode());
		result = prime * result + (onhike ? 1231 : 1237);
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
		Conversation other = (Conversation) obj;
		if (contactName == null)
		{
			if (other.contactName != null)
				return false;
		}
		else if (!contactName.equals(other.contactName))
			return false;
		if (msisdn == null)
		{
			if (other.msisdn != null)
				return false;
		}
		else if (!msisdn.equals(other.msisdn))
			return false;
		if (onhike != other.onhike)
			return false;
		return true;
	}

	public JSONObject serialize(String type)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, type);
			object.put(HikeConstants.TO, msisdn);
			object.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));
		}
		catch (JSONException e)
		{
			Logger.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}

	public void setContactName(String contactName)
	{
		this.contactName = contactName;
	}

	public MetaData getMetaData()
	{
		return metaData;
	}

	public void setMetaData(MetaData metaData)
	{
		this.metaData = metaData;
	}

	public static class MetaData
	{
		/**
		 * sample json : {'pin':{'id':'1','unreadCount':'1','toShow':'true','timestamp':'XXX','displayed':'false'} }
		 */
		JSONObject jsonObject;

		public MetaData(String jsonString) throws JSONException
		{
			if (jsonString != null)
			{
				jsonObject = new JSONObject(jsonString);
			}
			else
			{
				jsonObject = new JSONObject();
				setLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN, -1);
				setUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN, 0);
				setShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);
			}
			
		}

		public long getLastPinId(int pinType) throws JSONException
		{
			JSONObject pinJSON = getPinJson(pinType);
			return pinJSON.getLong(HikeConstants.ID);
		}
		
		public long getLastPinTimeStamp(int pinType) throws JSONException
		{
			JSONObject pinJSON = getPinJson(pinType);
			if(pinJSON.has(HikeConstants.TIMESTAMP))
			{
				return pinJSON.getLong(HikeConstants.TIMESTAMP);
			}
			return -1;
		}
		public void setLastPinTimeStamp(int pinType, long timeStamp) throws JSONException
		{
			JSONObject pinJSON = getPinJson(pinType);
			pinJSON.put(HikeConstants.TIMESTAMP, timeStamp);
		}

		public int getUnreadCount(int pinType) throws JSONException
		{
			JSONObject pinJSON = getPinJson(pinType);
			return pinJSON.getInt(HikeConstants.UNREAD_COUNT);
		}

		public boolean isShowLastPin(int pinType)
		{
			try
			{
				JSONObject pinJson = getPinJson(pinType);
				return pinJson.getBoolean(HikeConstants.TO_SHOW);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				return false;
			}
		}

		private JSONObject getPinJson(int pinType) throws JSONException
		{
			JSONObject json = jsonObject.optJSONObject(HikeConstants.PIN);
			if (json == null)
			{
				jsonObject.put(HikeConstants.PIN, json = new JSONObject());
			}
			return json;
		}

		public void setLastPinId(int pinType, long id) throws JSONException
		{
			JSONObject pinJSON = getPinJson(pinType);
			pinJSON.put(HikeConstants.ID, id);
		}

		public void setUnreadCount(int pinType, int count) throws JSONException
		{
			JSONObject pinJSON = getPinJson(pinType);
			pinJSON.put(HikeConstants.UNREAD_COUNT, count);
		}

		public void incrementUnreadCount(int pinType) throws JSONException
		{
			JSONObject pinJSON = getPinJson(pinType);
			pinJSON.put(HikeConstants.UNREAD_COUNT, pinJSON.getInt(HikeConstants.UNREAD_COUNT) + 1);
		}
		
		public void decrementUnreadCount(int pinType) throws JSONException
		{
			JSONObject pinJSON = getPinJson(pinType);
			int unreadCount =pinJSON.getInt(HikeConstants.UNREAD_COUNT);
			if(unreadCount>0)
			{
		    	pinJSON.put(HikeConstants.UNREAD_COUNT, pinJSON.getInt(HikeConstants.UNREAD_COUNT) - 1);
			}
		}

		public void setShowLastPin(int pinType, boolean isShow) throws JSONException
		{
			JSONObject pinJson = getPinJson(pinType);
			pinJson.put(HikeConstants.TO_SHOW, isShow);
		}

		

		public void setPinDisplayed(int pinType, boolean isShow) throws JSONException
		{
			JSONObject pinJson = getPinJson(pinType);
			pinJson.put(HikeConstants.PIN_DISPLAYED, isShow);
		}
		public boolean isPinDisplayed(int pinType)
		{
			try
			{
				JSONObject pinJson = getPinJson(pinType);
				return pinJson.getBoolean(HikeConstants.PIN_DISPLAYED);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				return false;
			}
		}
		@Override
		public String toString()
		{
			return jsonObject.toString();
		}

	}
}
