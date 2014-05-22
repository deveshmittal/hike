package com.bsb.hike.models;

public class ConversationTip extends Conversation
{
	public static final int GROUP_CHAT_TIP = 1;
	
	public static final int STEALTH_FTUE_TIP = 2;

	public static final int RESET_STEALTH_TIP = 3;

	private int tipType;
	public ConversationTip(int tipType)
	{
		super(null, -1);
		this.tipType = tipType;
	}
	
	public int getTipType()
	{
		return tipType;
	}
	
	public void setTipType(int tipType)
	{
		this.tipType = tipType;
	}
	
	public boolean isGroupChatTip()
	{
		return tipType == GROUP_CHAT_TIP;
	}
	
	public boolean isStealthFtueTip()
	{
		return tipType == STEALTH_FTUE_TIP;
	}

	public boolean isResetStealthTip()
	{
		return tipType == RESET_STEALTH_TIP;
	}
}
