package com.bsb.hike.models.Conversation;

import android.text.TextUtils;

import com.bsb.hike.models.ConvMessage;

/**
 * This class contains the core fields which are required for a conversation entity to be displayed on the ConversationFragment screen.
 * 
 * @author Anu/Piyush
 */
public class ConvInfo implements Comparable<ConvInfo>
{
	private String msisdn;

	private String mConversationName;

	private int unreadCount;

	private boolean isBlocked;

	private boolean isMute;

	private boolean isStealth;

	private long sortingTimeStamp;

	/**
	 * Keeps track of the last message for a given conversation
	 */
	private ConvMessage lastConversationMsg;

	private ConvInfo(ConvInfoBuilder builder)
	{
		if (!validateConvInfo(builder))
		{
			throw new IllegalArgumentException("No msisdn set.! ConvInfo object cannot be created.");
		}

		this.msisdn = builder.msisdn;
		this.mConversationName = builder.convName;
		this.sortingTimeStamp = builder.sortingTimeStamp;
		this.isStealth = builder.isStealth;
	}

	/**
	 * Validates params for convInfo to ensure msisdn is set
	 * 
	 * @param builder
	 * @return
	 */
	private boolean validateConvInfo(ConvInfoBuilder builder)
	{
		return !(TextUtils.isEmpty(builder.msisdn));
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
	public String getConversationName()
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

	/**
	 * @return the isStealth
	 */
	public boolean isStealth()
	{
		return isStealth;
	}

	/**
	 * @param isStealth
	 *            the isStealth to set
	 */
	protected void setStealth(boolean isStealth)
	{
		this.isStealth = isStealth;
	}

	@Override
	public String toString()
	{
		return "Conversation { msisdn = " + msisdn + ", conversation name = " + mConversationName + " }";
	}

	@Override
	public int compareTo(ConvInfo other)
	{
		if (other == null)
		{
			return 1;
		}

		if (this.equals(other))
		{
			return 0;
		}

		long this_sorting_ts = this.sortingTimeStamp;
		long other_sorting_ts = other.sortingTimeStamp;

		if (other_sorting_ts != this_sorting_ts)
		{
			return (this_sorting_ts < other_sorting_ts) ? -1 : 1;
		}

		return (this.msisdn.compareTo(other.msisdn));
	}

	/**
	 * Custom equals method
	 */

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

		ConvInfo other = (ConvInfo) obj;

		if ((mConversationName == null) && (other.mConversationName != null))
		{
			return false;
		}

		else if (!mConversationName.equals(other.mConversationName))
		{
			return false;
		}

		if (this.msisdn == null && other.msisdn != null)
		{
			return false;
		}

		else if (!this.msisdn.equals(other.msisdn))
		{
			return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mConversationName == null) ? 0 : mConversationName.hashCode());
		result = prime * result + ((msisdn == null) ? 0 : msisdn.hashCode());
		result = prime * result + (isStealth ? 1231 : 1237);

		return result;
	}

	public class ConvInfoBuilder
	{
		private String msisdn;

		private String convName;

		private boolean isStealth;

		private long sortingTimeStamp;

		public ConvInfoBuilder(String msisdn)
		{
			this.msisdn = msisdn;
		}

		public ConvInfoBuilder setConvName(String convName)
		{
			this.convName = convName;
			return this;
		}

		public ConvInfoBuilder setIsStealth(boolean isStealth)
		{
			this.isStealth = isStealth;
			return this;
		}

		public ConvInfoBuilder setSortingTimeStamp(long timeStamp)
		{
			this.sortingTimeStamp = timeStamp;
			return this;
		}

		public ConvInfo build()
		{
			return new ConvInfo(this);
		}
	}
}
