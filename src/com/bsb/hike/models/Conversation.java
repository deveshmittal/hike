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

			return rhs.compareTo(lhs);
		}
	}

	public String getMsisdn()
	{
		return msisdn;
	}

	public long getConvId()
	{
		return convId;
	}

	@Override
	public String toString()
	{
		return "Conversation [msisdn=" + msisdn + ", convId=" + convId + ", messages=" + messages.size() + ", contactName=" + contactName + ", onhike=" + onhike + "]";
	}

	private String msisdn;

	private long convId;

	private List<ConvMessage> messages;

	private String contactName;

	private boolean onhike;

	private int unreadCount;

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

	public Conversation(String msisdn, long convId)
	{
		this(msisdn, convId, null, false);
	}

	public Conversation(String msisdn, long convId, String contactName, boolean onhike)
	{
		this(msisdn, convId, contactName, onhike, false);
	}

	public Conversation(String msisdn, long convId, String contactName, boolean onhike, boolean isStealth)
	{
		this.msisdn = msisdn;
		this.convId = convId;
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

		if (convId != rhs.convId)
		{
			return (convId < rhs.convId) ? -1 : 1;
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
		result = prime * result + (int) (convId ^ (convId >>> 32));
		result = prime * result + ((messages == null) ? 0 : messages.hashCode());
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
		if (convId != other.convId)
			return false;
		if (messages == null)
		{
			if (other.messages != null)
				return false;
		}
		else if (!messages.equals(other.messages))
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
}
