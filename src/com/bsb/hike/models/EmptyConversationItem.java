package com.bsb.hike.models;

public class EmptyConversationItem
{
	public static final int HIKE_CONTACTS = 1;

	public static final int SMS_CONTACTS = 2;

	public static final int LAST_SEEN = 3;

	public static final int HIKE_OFFLINE = 5;

	public static final int STICKERS = 6;
	
	public static final int SEPERATOR = 7;

	public static final int OTHER_FTUE_CARD = 8;

	public static final int GROUP = 9;

	public static final int INVITE = 10;

	private int type;

	public EmptyConversationItem(int type)
	{
		this.setType(type);
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