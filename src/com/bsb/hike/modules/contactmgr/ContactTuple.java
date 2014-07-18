package com.bsb.hike.modules.contactmgr;

import com.bsb.hike.models.ContactInfo;

public class ContactTuple
{

	private int referenceCount;

	private ContactInfo contact;

	public ContactTuple(int refCount, ContactInfo con)
	{
		this.referenceCount = refCount;
		this.contact = con;
	}

	public int getReferenceCount()
	{
		return referenceCount;
	}

	public ContactInfo getContact()
	{
		return contact;
	}

	public void setReferenceCount(int refCount)
	{
		referenceCount = refCount;
	}

	public void setContact(ContactInfo con)
	{
		contact = con;
	}
}
