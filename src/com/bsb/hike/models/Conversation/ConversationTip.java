package com.bsb.hike.models.Conversation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bsb.hike.R;

public class ConversationTip
{
	public static final int GROUP_CHAT_TIP = 1;

	public static final int STEALTH_FTUE_TIP = 2;

	public static final int RESET_STEALTH_TIP = 3;

	public static final int WELCOME_HIKE_TIP = 4;

	public static final int STEALTH_INFO_TIP = 5;

	public static final int STEALTH_UNREAD_TIP = 6;

	public static final int ATOMIC_PROFILE_PIC_TIP = 7;

	public static final int ATOMIC_FAVOURTITES_TIP = 8;

	public static final int ATOMIC_INVITE_TIP = 9;

	public static final int ATOMIC_STATUS_TIP = 10;

	public static final int ATOMIC_INFO_TIP = 11;

	public static final int ATOMIC_HTTP_TIP = 12;

	public static final int ATOMIC_APP_GENERIC_TIP = 13;

	private int tipType;

	private LayoutInflater inflater;

	public ConversationTip(int tipType, Context context)
	{
		this.tipType = tipType;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public View getView(int tipType, ViewGroup parent)
	{
		switch (tipType)
		{
		case STEALTH_FTUE_TIP:
		case RESET_STEALTH_TIP:
			return inflater.inflate(R.layout.stealth_ftue_conversation_tip, parent, false);

		case WELCOME_HIKE_TIP:
			return inflater.inflate(R.layout.welcome_hike_tip, parent, false);

		case STEALTH_INFO_TIP:
		case STEALTH_UNREAD_TIP:
			return inflater.inflate(R.layout.stealth_unread_tip, parent, false);

		case ATOMIC_PROFILE_PIC_TIP:
		case ATOMIC_FAVOURTITES_TIP:
		case ATOMIC_INVITE_TIP:
		case ATOMIC_STATUS_TIP:
		case ATOMIC_INFO_TIP:
		case ATOMIC_HTTP_TIP:
		case ATOMIC_APP_GENERIC_TIP:
			return inflater.inflate(R.layout.tip_left_arrow, parent, false);

		default:
			return null;
		}
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

	public boolean isWelcomeHikeTip()
	{
		return tipType == WELCOME_HIKE_TIP;
	}

	public boolean isStealthInfoTip()
	{
		return tipType == STEALTH_INFO_TIP;
	}

	public boolean isStealthUnreadTip()
	{
		return tipType == STEALTH_UNREAD_TIP;
	}
}
