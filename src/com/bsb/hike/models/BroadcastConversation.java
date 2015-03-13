package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

import android.content.Context;

public class BroadcastConversation extends GroupConversation {

	public BroadcastConversation(JSONObject jsonObject, Context context)
			throws JSONException {
		super(jsonObject, context);
	}

	public BroadcastConversation(String msisdn, String contactName, String groupOwner, boolean isGroupAlive)
	{
		super(msisdn, contactName, groupOwner, isGroupAlive);
	}
	
	public BroadcastConversation(String msisdn, String contactName, String groupOwner, boolean isGroupAlive, boolean isGroupMute)
	{
		super(msisdn, contactName, groupOwner, isGroupAlive, false);
	}
	
	public static String defaultBroadcastName(ArrayList<String> participantList)
	{
		List<ContactInfo> broadcastParticipants = new ArrayList<ContactInfo>(participantList.size());
		for (String msisdn : participantList)
		{
			ContactInfo contactInfo = ContactManager.getInstance().getContact(msisdn, true, false);
			broadcastParticipants.add(contactInfo);
		}
		Collections.sort(broadcastParticipants);

		switch (broadcastParticipants.size())
		{
		case 0:
			return "";
		case 1:
			return broadcastParticipants.get(0).getFirstName();
		case 2:
			return broadcastParticipants.get(0).getFirstName() + " and " + broadcastParticipants.get(1).getFirstName();
		default:
			return broadcastParticipants.get(0).getFirstName() + ", " + broadcastParticipants.get(1).getFirstName() + " and " + (broadcastParticipants.size() - 2) + " others";
		}
	}
	
	public String getDefaultBroadcastName()
	{
		{
			setGroupParticipantList(ContactManager.getInstance().getGroupParticipants(getMsisdn(), false, false));
			return Utils.defaultGroupName(new ArrayList<PairModified<GroupParticipant, String>>(groupParticipantList.values()));
		}
	}
}
