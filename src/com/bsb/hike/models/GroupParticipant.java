package com.bsb.hike.models;

public class GroupParticipant implements Comparable<GroupParticipant> {
	private boolean hasLeft;

	private boolean onDnd;

	private ContactInfo contactInfo;

	public GroupParticipant(ContactInfo contactInfo) {
		this(contactInfo, false, false);
	}

	public GroupParticipant(ContactInfo contactInfo, boolean hasLeft,
			boolean onDnd) {
		this.contactInfo = contactInfo;
		this.hasLeft = hasLeft;
		this.onDnd = onDnd;
	}

	public void setOnDnd(boolean onDnd) {
		this.onDnd = onDnd;
	}

	public boolean onDnd() {
		return onDnd;
	}

	public boolean hasLeft() {
		return hasLeft;
	}

	public void setHasLeft(boolean hasLeft) {
		this.hasLeft = hasLeft;
	}

	public ContactInfo getContactInfo() {
		return contactInfo;
	}

	@Override
	public int compareTo(GroupParticipant another) {
		return this.contactInfo.compareTo(another.contactInfo);
	}
}
