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

		if (lhsFavoriteType != rhsFavoriteType)
		{
			if (lhsFavoriteType == FavoriteType.REQUEST_RECEIVED)
			{
				return -1;
			}
			else if (rhsFavoriteType == FavoriteType.REQUEST_RECEIVED)
			{
				return 1;
			}
		}

		if (lastSeenPref)
		{
			if (hasLastSeenValue(lhsFavoriteType))
			{
				if (!hasLastSeenValue(rhsFavoriteType))
				{
					if (lhs.getOffline() == 0)
					{
						return -1;
					}
				}
				int value = compareOfflineValues(lhs.getOffline(), rhs.getOffline());
				if (value != 0)
				{
					return value;
				}
			}
			else if (hasLastSeenValue(rhsFavoriteType) && !hasLastSeenValue(lhsFavoriteType))
			{
				if (rhs.getOffline() == 0)
				{
					return 1;
				}
			}
		}

		return lhs.compareTo(rhs);
	}

	private boolean hasLastSeenValue(FavoriteType favoriteType)
	{
		return favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED;
	}

	private int compareOfflineValues(int lhs, int rhs)
	{
		if (lhs != rhs)
		{
			if (lhs == 0)
			{
				return -1;
			}
			else if (rhs == 0)
			{
				return 1;
			}
		}
		return 0;
	}

}
