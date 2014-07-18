package com.bsb.hike.modules.contactmgr;

import java.util.concurrent.ConcurrentLinkedQueue;

public class GroupDetails
{

	private String groupName;

	private ConcurrentLinkedQueue<pair> lastMsisdns;

	GroupDetails(String grpName, ConcurrentLinkedQueue<pair> lastMsisdns)
	{
		this.groupName = grpName;
		this.lastMsisdns = lastMsisdns;
	}

	String getGroupName()
	{
		return groupName;
	}

	void setGroupName(String grpName)
	{
		groupName = grpName;
	}

	ConcurrentLinkedQueue<pair> getLastMsisdns()
	{
		return lastMsisdns;
	}

	void setLastMsisdns(ConcurrentLinkedQueue<pair> lastMsisdns)
	{
		this.lastMsisdns = lastMsisdns;
	}

	void setName(String msisdn, String name)
	{
		if (null != lastMsisdns)
		{
			for (pair msPair : lastMsisdns)
			{
				if (msisdn.equals(msPair.first))
				{
					msPair.second = name;
				}
			}
		}
	}

	String getName(String msisdn)
	{
		if (null != lastMsisdns)
		{
			for (pair msPair : lastMsisdns)
			{
				if (msisdn.equals(msPair.first))
				{
					return msPair.second;
				}
			}
		}

		return null;
	}

}
