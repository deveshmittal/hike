package com.bsb.hike.models.Conversation;

import com.bsb.hike.models.ConvMessage;

/**
 * This class contains the core fields which are required for a conversation entity to be displayed on the ConversationFragment screen.
 * 
 * @author Anu/Piyush
 */
public class ConvInfo
{
	private String msisdn;

	private String mConversationName;

	private int unreadCount;

	private boolean isBlocked;

	private boolean isMute;

	private long sortingTimeStamp;

	/**
	 * Keeps track of the last message for a given conversation
	 */
	private ConvMessage lastConversationMsg;

	public ConvInfo(String msisdn, String convName)
	{
		this.msisdn = msisdn;
		this.mConversationName = convName;
	}

	/**
	 * @return the msisdn
	 */
	public String getMsisdn()
	{
		return msisdn;
	}

	/**
	 * @return the mConversationName
	 */
	public String getmConversationName()
	{
		return mConversationName;
	}

	/**
	 * @param mConversationName
	 *            the mConversationName to set
	 */
	protected void setmConversationName(String mConversationName)
	{
		this.mConversationName = mConversationName;
	}

	/**
	 * @return the unreadCount
	 */
	public int getUnreadCount()
	{
		return unreadCount;
	}

	/**
	 * @param unreadCount
	 *            the unreadCount to set
	 */
	protected void setUnreadCount(int unreadCount)
	{
		this.unreadCount = unreadCount;
	}

	/**
	 * @return the isBlocked
	 */
	public boolean isBlocked()
	{
		return isBlocked;
	}

	/**
	 * @param isBlocked
	 *            the isBlocked to set
	 */
	protected void setBlocked(boolean isBlocked)
	{
		this.isBlocked = isBlocked;
	}

	/**
	 * @return the isMute
	 */
	public boolean isMute()
	{
		return isMute;
	}

	/**
	 * @param isMute
	 *            the isMute to set
	 */
	protected void setMute(boolean isMute)
	{
		this.isMute = isMute;
	}

	/**
	 * @return the sortingTimeStamp
	 */
	public long getSortingTimeStamp()
	{
		return sortingTimeStamp;
	}

	/**
	 * @param sortingTimeStamp
	 *            the sortingTimeStamp to set
	 */
	protected void setSortingTimeStamp(long sortingTimeStamp)
	{
		this.sortingTimeStamp = sortingTimeStamp;
	}

	/**
	 * @return the lastConversationMsg
	 */
	public ConvMessage getLastConversationMsg()
	{
		return lastConversationMsg;
	}

	/**
	 * @param lastConversationMsg
	 *            the lastConversationMsg to set
	 */
	protected void setLastConversationMsg(ConvMessage lastConversationMsg)
	{
		this.lastConversationMsg = lastConversationMsg;
	}

	@Override
	public String toString()
	{
		return "Conversation { msisdn = " + msisdn + ", conversation name = " + mConversationName + " }";
	}

}
