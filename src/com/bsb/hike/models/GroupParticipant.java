package com.bsb.hike.models;

public class GroupParticipant implements Comparable<GroupParticipant>
{
	private boolean hasLeft;

	private ContactInfo contactInfo;

	public GroupParticipant(ContactInfo contactInfo)
	{
		this(contactInfo, false);
	}

	public GroupParticipant(ContactInfo contactInfo, boolean hasLeft)
	{
		this.contactInfo = contactInfo;
		this.hasLeft = hasLeft;
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
}
