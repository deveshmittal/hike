package com.bsb.hike.models;

import java.util.List;

public class EmptyConversationContactItem extends EmptyConversationItem
{
	private List<ContactInfo> contactList;

	private String header;

	public EmptyConversationContactItem(List<ContactInfo> contactList, String header, int type)
	{
		super(type);

		this.setContactList(contactList);

		this.setHeader(header);
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

}
