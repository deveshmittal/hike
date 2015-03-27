package com.bsb.hike.models.Conversation;

import java.util.Comparator;

import android.text.TextUtils;

import com.bsb.hike.models.ConvMessage;

/**
 * This class contains the core fields which are required for a conversation entity to be displayed on the ConversationFragment screen. This is the atomic unit for entities to be
 * displayed on the home screen.
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

	protected ConvInfo(InitBuilder<?> builder)
	{
		if (!validateConvInfo(builder))
		{
			throw new IllegalArgumentException("No msisdn set.! ConvInfo object cannot be created.");
		}

		this.msisdn = builder.msisdn;
		this.mConversationName = builder.convName;
		this.sortingTimeStamp = builder.sortingTimeStamp;
		this.isStealth = builder.isStealth;
		this.isMute = builder.isMute;
	}

	/**
	 * Validates params for convInfo to ensure msisdn is set
	 * 
	 * @param builder
	 * @return
	 */
	private boolean validateConvInfo(InitBuilder<?> builder)
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
	public void setmConversationName(String mConversationName)
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
	public void setUnreadCount(int unreadCount)
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
	public void setMute(boolean isMute)
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
	public void setSortingTimeStamp(long sortingTimeStamp)
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
	public void setLastConversationMsg(ConvMessage lastConversationMsg)
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
	public void setStealth(boolean isStealth)
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

		if ((this.mConversationName == null) && (other.mConversationName != null))
		{
			return false;
		}

		else if (this.mConversationName != null && (!mConversationName.equals(other.mConversationName)))
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

	protected static abstract class InitBuilder<P extends InitBuilder<P>>
	{
		private String msisdn;

		private String convName;

		private boolean isStealth;

		private long sortingTimeStamp;

		private boolean isMute;

		protected InitBuilder(String msisdn)
		{
			this.msisdn = msisdn;
		}

		protected abstract P getSelfObject();

		public P setConvName(String convName)
		{
			this.convName = convName;
			return getSelfObject();
		}

		public P setIsStealth(boolean isStealth)
		{
			this.isStealth = isStealth;
			return getSelfObject();
		}

		public P setSortingTimeStamp(long timeStamp)
		{
			this.sortingTimeStamp = timeStamp;
			return getSelfObject();
		}

		public P setIsMute(boolean mute)
		{
			this.isMute = mute;
			return getSelfObject();
		}

		public ConvInfo build()
		{
			return new ConvInfo(this);
		}

	}

	public static class ConvInfoBuilder extends InitBuilder<ConvInfoBuilder>
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

	public static class ConvInfoComparator implements Comparator<ConvInfo>
	{
		/**
		 * This comparator reverses the order of the normal comparable
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */

		@Override
		public int compare(ConvInfo lhs, ConvInfo rhs)
		{
			if (rhs == null)
			{
				return 1;
			}

			return rhs.compareTo(lhs);
		}

	}
}