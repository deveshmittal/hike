package com.bsb.hike.models.Conversation;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.ChatTheme;

/**
 * Conversation objects will be made from this abstract class
 * 
 * @author Anu/Piyush
 * 
 */
public abstract class Conversation
{
	private ConvInfo convInfo;

	private ArrayList<ConvMessage> messagesList;

	private ConversationMetadata metadata;

	/**
	 * Default value of chat theme
	 */
	private ChatTheme chatTheme = ChatTheme.DEFAULT;

	public Conversation()
	{
		// TODO Auto-generated constructor stub
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
}
