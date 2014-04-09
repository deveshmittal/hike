package com.bsb.hike.utils;

import java.util.Comparator;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;

public class LastSeenComparator implements Comparator<ContactInfo>
{

	public boolean lastSeenPref = true;

	@Override
	public int compare(ContactInfo lhs, ContactInfo rhs)
	{
		FavoriteType lhsFavoriteType = lhs.getFavoriteType();
		FavoriteType rhsFavoriteType = rhs.getFavoriteType();

		// if both the values are FRIEND REQUEST then compare on the basis of contacts
		if (lhsFavoriteType == FavoriteType.REQUEST_RECEIVED && rhsFavoriteType == FavoriteType.REQUEST_RECEIVED)
			return lhs.compareTo(rhs);

		else if (lhsFavoriteType == FavoriteType.REQUEST_RECEIVED)
			return -1;
		
		else if (rhsFavoriteType == FavoriteType.REQUEST_RECEIVED)
			return 1;

		else if (lastSeenPref)
		{
			boolean l = hasLastSeenValue(lhs);
			boolean r = hasLastSeenValue(rhs);
			if(l && r) // both have last seen values and are online
				return lhs.compareTo(rhs);
			
			else if(l) // only left has last seen value
				return -1;
			
			else if(r) // only right has last seen value
				return 1;
			
			else // neither left nor right has last seen value
				lhs.compareTo(rhs);
		}
		return lhs.compareTo(rhs);
	}

	private boolean hasLastSeenValue(ContactInfo lhs)
	{
		return (lhs.getFavoriteType() == FavoriteType.FRIEND || lhs.getFavoriteType() == FavoriteType.REQUEST_RECEIVED_REJECTED) && (lhs.getOffline() == 0);
	}
}
