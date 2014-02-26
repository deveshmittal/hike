package com.bsb.hike.models;

import java.util.Comparator;

import com.bsb.hike.models.ContactInfo.FavoriteType;

public class GroupParticipant implements Comparable<GroupParticipant>
{
	private boolean hasLeft;

	private boolean onDnd;

	private ContactInfo contactInfo;

	public GroupParticipant(ContactInfo contactInfo)
	{
		this(contactInfo, false, false);
	}

	public GroupParticipant(ContactInfo contactInfo, boolean hasLeft, boolean onDnd)
	{
		this.contactInfo = contactInfo;
		this.hasLeft = hasLeft;
		this.onDnd = onDnd;
	}

	public void setOnDnd(boolean onDnd)
	{
		this.onDnd = onDnd;
	}

	public boolean onDnd()
	{
		return onDnd;
	}

	public boolean hasLeft()
	{
		return hasLeft;
	}

	public void setHasLeft(boolean hasLeft)
	{
		this.hasLeft = hasLeft;
	}

	public ContactInfo getContactInfo()
	{
		return contactInfo;
	}

	@Override
	public int compareTo(GroupParticipant another)
	{
		return this.contactInfo.compareTo(another.contactInfo);
	}

	public static Comparator<GroupParticipant> lastSeenTimeComparator = new Comparator<GroupParticipant>()
	{

		@Override
		public int compare(GroupParticipant lhs, GroupParticipant rhs)
		{
			ContactInfo lhsContactInfo = lhs.contactInfo;
			ContactInfo rhsContactInfo = rhs.contactInfo;

			if (lhsContactInfo.getFavoriteType() != rhsContactInfo.getFavoriteType())
			{
				if (lhsContactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED)
				{
					return -1;
				}
				else if (rhsContactInfo.getFavoriteType() == FavoriteType.REQUEST_RECEIVED)
				{
					return 1;
				}
			}
			if (lhsContactInfo.getOffline() != rhsContactInfo.getOffline())
			{
				if (lhsContactInfo.getOffline() == 0)
				{
					return -1;
				}
				else if (rhsContactInfo.getOffline() == 0)
				{
					return 1;
				}
			}
			return lhsContactInfo.compareTo(rhsContactInfo);
		}
	};
}
