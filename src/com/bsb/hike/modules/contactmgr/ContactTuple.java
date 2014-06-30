package com.bsb.hike.modules.contactmgr;

import com.bsb.hike.models.ContactInfo;

public class ContactTuple
{

	private int referenceCount;

	private String name;

	private ContactInfo contact;

	public ContactTuple(int refCount, String n, ContactInfo con)
	{
		this.referenceCount = refCount;
		this.name = n;
		this.contact = con;
	}

	public int getReferenceCount()
	{
		return referenceCount;
	}

	public String getName()
	{
		return name;
	}

	public ContactInfo getContact()
	{
		return contact;
	}

	public void setReferenceCount(int refCount)
	{
		referenceCount = refCount;
	}

	public void setName(String n)
	{
		name = n;
	}

	public void setContact(ContactInfo con)
	{
		contact = con;
	}
}
