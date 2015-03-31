package com.bsb.hike.models.Conversation;

import com.bsb.hike.HikeMessengerApp;

/**
 * This class contains the core fields which are required for a 1-n conversation entity to be displayed on the ConversationFragment screen. This is the atomic unit for 1-n
 * conversation to be displayed on the home screen.
 * 
 * @author Anu/Piyush
 */
public class OneToNConvInfo extends ConvInfo
{
	private boolean isConversationAlive;

	protected OneToNConvInfo(InitBuilder<?> builder)
	{
		super(builder);
		this.setConversationAlive(builder.isConversationAlive);
	}

	/**
	 * @return the isConversationAlive
	 */
	public boolean isConversationAlive()
	{
		return isConversationAlive;
	}

	/**
	 * @param isConversationAlive
	 *            the isConversationAlive to set
	 */
	public void setConversationAlive(boolean isConversationAlive)
	{
		this.isConversationAlive = isConversationAlive;
	}

	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends ConvInfo.InitBuilder<P>
	{
		private boolean isConversationAlive;

		protected InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public P setConversationAlive(boolean alive)
		{
			this.isConversationAlive = alive;
			return getSelfObject();
		}
	}

	public static class ConvInfoBuilder extends OneToNConvInfo.InitBuilder<ConvInfoBuilder>
	{

		public ConvInfoBuilder(String msisdn)
		{
			super(msisdn);
		}

		@Override
		protected ConvInfoBuilder getSelfObject()
		{
			return this;
		}

	}

	/**
	 * 
	 * @return conversation participant name
	 */
	public String getConvParticipantName(String groupParticipantMsisdn)
	{
		HikeMessengerApp.getContactManager().getContact(groupParticipantMsisdn, true, false);
		String name = HikeMessengerApp.getContactManager().getName(getMsisdn(), groupParticipantMsisdn);
		return name;
	}
}