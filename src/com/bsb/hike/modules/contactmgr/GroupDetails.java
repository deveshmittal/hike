package com.bsb.hike.modules.contactmgr;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.bsb.hike.utils.PairModified;

public class GroupDetails
{

	private String groupName;

	private ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdns;

	GroupDetails(String grpName, ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdns)
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

	ConcurrentLinkedQueue<PairModified<String, String>> getLastMsisdns()
	{
		return lastMsisdns;
	}

	void setLastMsisdns(ConcurrentLinkedQueue<PairModified<String, String>> lastMsisdns)
	{
		this.lastMsisdns = lastMsisdns;
	}

	void setName(String msisdn, String name)
	{
		if (null != lastMsisdns)
		{
			for (PairModified<String, String> msPair : lastMsisdns)
			{
				if (msisdn.equals(msPair.getFirst()))
				{
					msPair.setSecond(name);
				}
			}
		}
	}

	String getName(String msisdn)
	{
		if (null != lastMsisdns)
		{
			for (PairModified<String, String> msPair : lastMsisdns)
			{
				if (msisdn.equals(msPair.getFirst()))
				{
					return msPair.getSecond();
				}
			}
		}

		return null;
	}

}
