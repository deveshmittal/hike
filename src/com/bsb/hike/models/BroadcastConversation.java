package com.bsb.hike.models;

import java.util.ArrayList;

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
	
	public String getDefaultBroadcastName()
	{
		{
			setGroupParticipantList(ContactManager.getInstance().getGroupParticipants(getMsisdn(), false, false));
			return Utils.defaultGroupName(new ArrayList<PairModified<GroupParticipant, String>>(groupParticipantList.values()));
		}
	}
}
