package com.bsb.hike.models;

import org.json.JSONException;
import org.json.JSONObject;

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
}
