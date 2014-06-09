package com.bsb.hike.models;

import java.util.List;

public class EmptyConversationItem
{
	public static final int HIKE_CONTACTS = 1;

	public static final int SMS_CONTACTS = 2;

	private List<ContactInfo> contactList;

	private String header;

	private int type;

	public EmptyConversationItem(List<ContactInfo> contactList, String header, int type)
	{
		this.setContactList(contactList);

		this.setHeader(header);

		this.setType(type);
	}

	public List<ContactInfo> getContactList()
	{
		return contactList;
	}

	public void setContactList(List<ContactInfo> contactList)
	{
		this.contactList = contactList;
	}

	public String getHeader()
	{
		return header;
	}

	public void setHeader(String header)
	{
		this.header = header;
	}

	public int getType()
	{
		return type;
	}

	public void setType(int type)
	{
		this.type = type;
	}
}