package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.List;

import com.bsb.hike.utils.ClearGroupTypingNotification;

public class GroupTypingNotification extends TypingNotification
{

	private List<String> groupParticipantList;

	private List<ClearGroupTypingNotification> clearGroupTypingNotificationList;

	public GroupTypingNotification(String id, String participant, ClearGroupTypingNotification clearGroupTypingNotification)
	{
		super(id);

		groupParticipantList = new ArrayList<String>();
		clearGroupTypingNotificationList = new ArrayList<ClearGroupTypingNotification>();

		addParticipant(participant);
		addClearTypingNotification(clearGroupTypingNotification);
	}

	public void addParticipant(String participant)
	{
		groupParticipantList.add(participant);
	}

	public void removeParticipant(String participant)
	{
		groupParticipantList.remove(participant);
	}

	public boolean hasParticipant(String participant)
	{
		return groupParticipantList.contains(participant);
	}

	public List<String> getGroupParticipantList()
	{
		return groupParticipantList;
	}

	public void addClearTypingNotification(ClearGroupTypingNotification clearGroupTypingNotification)
	{
		clearGroupTypingNotificationList.add(clearGroupTypingNotification);
	}

	public ClearGroupTypingNotification getClearTypingNotification(String participant)
	{
		for (ClearGroupTypingNotification clearGroupTypingNotification : clearGroupTypingNotificationList)
		{
			if (clearGroupTypingNotification.getParticipantMsisdn().equals(participant))
			{
				return clearGroupTypingNotification;
			}
		}
		return null;
	}
}
