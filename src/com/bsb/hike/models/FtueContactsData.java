package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.List;

public class FtueContactsData
{

	private List<ContactInfo> hikeContacts;

	private List<ContactInfo> smsContacts;

	private List<ContactInfo> completeList;

	private int totalHikeContactsCount;

	private int totalSmsContactsCount;

	public FtueContactsData()
	{
		this.hikeContacts = new ArrayList<ContactInfo>();

		this.smsContacts = new ArrayList<ContactInfo>();

		this.completeList = new ArrayList<ContactInfo>();
	}

	public FtueContactsData(List<ContactInfo> hikeContacts, List<ContactInfo> smsContacts, int totalHikeContactsCount, int totalSmsContactsCount)
	{
		this.setHikeContacts(hikeContacts);

		this.setSmsContacts(smsContacts);

		this.setTotalHikeContactsCount(totalHikeContactsCount);

		this.setTotalSmsContactsCount(totalSmsContactsCount);
	}

	public List<ContactInfo> getHikeContacts()
	{
		return hikeContacts;
	}

	public void setHikeContacts(List<ContactInfo> hikeContacts)
	{
		this.hikeContacts = hikeContacts;
	}

	public List<ContactInfo> getSmsContacts()
	{
		return smsContacts;
	}

	public void setSmsContacts(List<ContactInfo> smsContacts)
	{
		this.smsContacts = smsContacts;
	}

	public int getTotalHikeContactsCount()
	{
		return totalHikeContactsCount;
	}

	public void setTotalHikeContactsCount(int totalHikeContactsCount)
	{
		this.totalHikeContactsCount = totalHikeContactsCount;
	}

	public int getTotalSmsContactsCount()
	{
		return totalSmsContactsCount;
	}

	public void setTotalSmsContactsCount(int totalSmsContactsCount)
	{
		this.totalSmsContactsCount = totalSmsContactsCount;
	}

	public List<ContactInfo> getCompleteList()
	{
		return completeList;
	}

	public void setCompleteList()
	{
		this.completeList.clear();
		this.completeList.addAll(hikeContacts);
		this.completeList.addAll(smsContacts);
	}

	public boolean isEmpty()
	{
		return hikeContacts.isEmpty() && smsContacts.isEmpty();
	}

	@Override
	public String toString()
	{
		StringBuilder ssb = new StringBuilder();
		ssb.append("Hike Contacts List = \n");

		for (ContactInfo contactInfo : hikeContacts)
		{
			ssb.append(contactInfo.getMsisdn());
			ssb.append(", ");
		}

		ssb.append("\nSMS Contacts List = \n");

		for (ContactInfo contactInfo : smsContacts)
		{
			ssb.append(contactInfo.getMsisdn());
			ssb.append(", ");
		}

		ssb.append("\ntotal hike contacts = " + totalHikeContactsCount);
		ssb.append("\ntotal sms contacts = " + totalSmsContactsCount);

		return ssb.toString();
	}
}
