package com.bsb.hike.models.Conversation;

/**
 * Conversation primitive for configurable bot chats.
 * 
 * @author Anu/Piyush
 */
public class BotConversation extends OneToOneConversation
{

	/**
	 * This member will be used for defining configurable properties for a bot conversation like enabling stickers, chat theme, VoIP call etc.We will be using bits for setting
	 * properties
	 */
	private short properties = 0;

	/**
	 * 
	 */
	private BotConversation(BotConversationBuilder builder)
	{
		super(builder);
		this.isOnHike = true;
	}

	/**
	 * @return the properties
	 */
	public short getProperties()
	{
		return properties;
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(short properties)
	{
		this.properties = properties;
	}

	private static class BotConversationBuilder extends OneToOneConversationBuilder
	{
		public BotConversation build()
		{
			return new BotConversation(this);
		}
	}
}
