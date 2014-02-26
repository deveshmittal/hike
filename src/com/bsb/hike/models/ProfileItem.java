package com.bsb.hike.models;

public abstract class ProfileItem
{

	public static final int HEADER_ID = -1;

	public static final int EMPTY_ID = -2;

	public static final int REQUEST_ID = -3;

	private int itemId;

	public ProfileItem(int itemId)
	{
		this.itemId = itemId;
	}

	public int getItemId()
	{
		return itemId;
	}

	public static class ProfileStatusItem extends ProfileItem
	{

		private StatusMessage statusMessage;

		public ProfileStatusItem(int itemId)
		{
			super(itemId);
		}

		public ProfileStatusItem(StatusMessage statusMessage)
		{
			super(0);
			this.statusMessage = statusMessage;
		}

		public StatusMessage getStatusMessage()
		{
			return statusMessage;
		}
	}

	public static class ProfileGroupItem extends ProfileItem
	{

		private GroupParticipant[] groupParticipants;

		public ProfileGroupItem(int itemId)
		{
			super(itemId);
		}

		public ProfileGroupItem(GroupParticipant[] groupParticipants)
		{
			super(0);
			this.groupParticipants = groupParticipants;
		}

		public GroupParticipant[] getGroupParticipants()
		{
			return groupParticipants;
		}
	}

}