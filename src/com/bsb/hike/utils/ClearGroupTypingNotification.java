package com.bsb.hike.utils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.GroupTypingNotification;

public class ClearGroupTypingNotification extends ClearTypingNotification
{

	private String participantMsisdn;

	public ClearGroupTypingNotification(String id, String participantMsisdn)
	{
		super(id);
		this.participantMsisdn = participantMsisdn;
	}

	public String getParticipantMsisdn()
	{
		return participantMsisdn;
	}

	@Override
	public void run()
	{

		GroupTypingNotification groupTypingNotification = (GroupTypingNotification) HikeMessengerApp.getTypingNotificationSet().get(id);
		if (groupTypingNotification == null)
		{
			return;
		}
		groupTypingNotification.removeParticipant(participantMsisdn);

		if (groupTypingNotification.getGroupParticipantList().isEmpty())
		{
			HikeMessengerApp.getTypingNotificationSet().remove(id);
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.END_TYPING_CONVERSATION, groupTypingNotification);
	}

}
