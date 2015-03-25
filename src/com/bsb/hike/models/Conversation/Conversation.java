package com.bsb.hike.models.Conversation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation.ConvInfo.ConvInfoBuilder;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;

/**
 * Conversation objects will be made from this abstract class
 * 
 * @author Anu/Piyush
 * 
 */
public abstract class Conversation implements Comparable<Conversation>
{
	private ConvInfo convInfo;

	private ArrayList<ConvMessage> messagesList;

	private ConversationMetadata metadata;

	/**
	 * Default value of chat theme
	 */
	private ChatTheme chatTheme = ChatTheme.DEFAULT;

	private Conversation(ConversationBuilder conversationBuilder)
	{
		ConvInfoBuilder convInfoBuilder = conversationBuilder.convInfoBuilder;
		this.convInfo = convInfoBuilder.build();
	}

	/**
	 * @param convInfo
	 *            the convInfo to set
	 */
	protected void setConvInfo(ConvInfo convInfo)
	{
		this.convInfo = convInfo;
	}

	/**
	 * @return the messagesList
	 */
	public ArrayList<ConvMessage> getMessagesList()
	{
		return messagesList;
	}

	/**
	 * @param messagesList
	 *            the messagesList to set
	 */
	public void setMessagesList(ArrayList<ConvMessage> messagesList)
	{
		this.messagesList = messagesList;
	}

	/**
	 * @return the metadata
	 */
	public ConversationMetadata getMetadata()
	{
		return metadata;
	}

	/**
	 * @param metadata
	 *            the metadata to set
	 */
	public void setMetadata(ConversationMetadata metadata)
	{
		this.metadata = metadata;
	}

	/**
	 * @return the chatTheme
	 */
	public ChatTheme getChatTheme()
	{
		return chatTheme;
	}

	/**
	 * @param chatTheme
	 *            the chatTheme to set
	 */
	public void setChatTheme(ChatTheme chatTheme)
	{
		this.chatTheme = chatTheme;
	}

	public String getMsisdn()
	{
		return convInfo.getMsisdn();
	}

	public String getConversationName()
	{
		return convInfo.getConversationName();
	}

	public void setConversationName(String convName)
	{
		convInfo.setmConversationName(convName);
	}

	/**
	 * Returns a friendly label for the conversation
	 * 
	 * @return
	 */
	public abstract String getLabel();

	public long getSortingTimeStamp()
	{
		return convInfo.getSortingTimeStamp();
	}

	public void setSortingTimeStamp(long timeStamp)
	{
		convInfo.setSortingTimeStamp(timeStamp);
	}

	public void setMessages(List<ConvMessage> messages)
	{
		this.messagesList = (ArrayList<ConvMessage>) messages;

		if (messagesList != null && !messagesList.isEmpty())
		{
			setSortingTimeStamp(messagesList.get(messagesList.size() - 1).getTimestamp());
		}
	}

	/**
	 * We update the last message because only the last message is shown in the conversation list view at home
	 * 
	 * @param message
	 *            Incoming ConvMessage object
	 */
	public void updateLastConvMessage(ConvMessage message)
	{
		convInfo.setLastConversationMsg(message);
		setSortingTimeStamp(message.getTimestamp());
	}

	public int getUnreadCount()
	{
		return convInfo.getUnreadCount();
	}

	public void setUnreadCount(int unreadCount)
	{
		convInfo.setUnreadCount(unreadCount);
	}

	public boolean isStealth()
	{
		return convInfo.isStealth();
	}

	public void setIsStealth(boolean isStealth)
	{
		convInfo.setStealth(isStealth);
	}

	public JSONObject serialize(String type)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, type);
			object.put(HikeConstants.TO, convInfo.getMsisdn());
			object.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis() / 1000));
		}
		catch (JSONException e)
		{
			Logger.e("Conversation", "invalid json message", e);
		}
		return object;
	}

	@Override
	public int compareTo(Conversation other)
	{
		if (other == null)
		{
			return 1;
		}

		if (this.equals(other))
		{
			return 0;
		}

		return convInfo.compareTo(other.convInfo);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}

		if (obj == null)
		{
			return false;
		}

		if (getClass() != obj.getClass())
		{
			return false;
		}

		Conversation other = (Conversation) obj;

		return convInfo.equals(other.convInfo);
	}

	@Override
	public int hashCode()
	{
		return convInfo.hashCode();
	}

	public static class ConversationComparator implements Comparator<Conversation>
	{
		/**
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

	public static class ConversationBuilder<T extends ConversationBuilder>
	{
		private ConvInfoBuilder convInfoBuilder;

		public T setMsisdn(String msisdn)
		{
			convInfoBuilder = new ConvInfoBuilder(msisdn);
			return (T) this;
		}

		public T setConvName(String convName)
		{
			convInfoBuilder.setConvName(convName);
			return (T) this;
		}

		public T setIsStealth(boolean isStealth)
		{
			convInfoBuilder.setIsStealth(isStealth);
			return (T) this;
		}

		public T setSortingTimeStamp(long timeStamp)
		{
			convInfoBuilder.setSortingTimeStamp(timeStamp);
			return (T) this;
		}
	}
}
