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
	private BotConversation(InitBuilder<?> builder)
	{
		super(builder);
		this.properties = builder.properties;
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

	/**
	 * Builder base class extending {@link OneToOneConversation.InitBuilder}
	 * 
	 * @author piyush
	 * 
	 * @param <P>
	 */
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends OneToOneConversation.InitBuilder<P>
	{
		private short properties;

		public InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public P setProperties(short property)
		{
			this.properties = property;
			return getSelfObject();
		}

		public BotConversation build()
		{
			return new BotConversation(this);
		}
	}

	/**
	 * Builder class used to generating {@link BotConversation}
	 * <p>
	 * Bare bone Usage : BotConversation conv = BotConversation.ConversationBuilder(msisdn).build();<br>
	 * Other examples : BotConversation conv = BotConversation.ConversationBuilder(msisdn).setConvName("ABC").setProperties(127).build();
	 * 
	 * @author piyush
	 * 
	 */
	public static class ConversationBuilder extends BotConversation.InitBuilder<ConversationBuilder>
	{
		public ConversationBuilder(String msisdn)
		{
			super(msisdn);
		}

		@Override
		protected ConversationBuilder getSelfObject()
		{
			return this;
		}
	}
}
