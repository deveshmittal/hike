package com.bsb.hike.models.Conversation;

import com.bsb.hike.models.ConvMessage;

import android.text.TextUtils;

/**
 * Conversation primitive for 1-1 chats like human chats, bot chats etc.
 * 
 * @author Anu/Piyush
 * 
 */
public class OneToOneConversation extends Conversation
{
	protected boolean isOnHike;

	private OneToOneConversation(OneToOneConversationBuilder builder)
	{
		super(builder);
		this.isOnHike = builder.isOnHike;
	}

	/**
	 * @return the isOnHike
	 */
	public boolean isOnHike()
	{
		return isOnHike;
	}

	/**
	 * @param isOnHike
	 *            the isOnHike to set
	 */
	public void setOnHike(boolean isOnHike)
	{
		this.isOnHike = isOnHike;
	}

	@Override
	public void updateLastConvMessage(ConvMessage message)
	{
		convInfo.setLastConversationMsg(message);

		/**
		 * Updates the Conversation timestamp only if the message does not qualify as a broadcast message in a OneToOneConversation.
		 */
		if (!message.isBroadcastMessage())
		{
			setSortingTimeStamp(message.getTimestamp());
		}
	}

	/*
	 * @see com.bsb.hike.models.Conversation.Conversation#getLabel()
	 */
	@Override
	public String getLabel()
	{
		return (TextUtils.isEmpty(getConversationName()) ? getMsisdn() : getConversationName());
	}

	public static class OneToOneConversationBuilder extends ConversationBuilder<ConversationBuilder>
	{
		private boolean isOnHike;

		public OneToOneConversationBuilder setIsOnHike(boolean isOnHike)
		{
			this.isOnHike = isOnHike;
			return this;
		}

		public OneToOneConversation build()
		{
			return new OneToOneConversation(this);
		}
	}
}
