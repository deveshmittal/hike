package com.bsb.hike.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.Utils;

public class GroupConversation extends Conversation {

	private String groupOwner;
	private boolean isGroupAlive;
	private Map<String, GroupParticipant> groupParticipantList;

	public GroupConversation(String msisdn, long convId, String contactId, String contactName, String groupOwner, boolean isGroupAlive) 
	{
		super(msisdn, convId, contactId, contactName, true);
		this.groupOwner = groupOwner;
		this.isGroupAlive = isGroupAlive;
	}

	public GroupConversation(JSONObject jsonObject, Context context) throws JSONException
	{
		super(jsonObject.getString(HikeConstants.TO), 0, null, null, true);
		this.groupOwner = jsonObject.getString(HikeConstants.FROM);
		this.groupParticipantList = new HashMap<String, GroupParticipant>();
		JSONArray array = jsonObject.getJSONArray(HikeConstants.DATA);
		for (int i = 0; i < array.length(); i++) 
		{
			JSONObject nameMsisdn = array.getJSONObject(i);
			String contactNum = nameMsisdn.getString(HikeConstants.MSISDN);
			String contactName = nameMsisdn.getString(HikeConstants.NAME);
			GroupParticipant groupParticipant = new GroupParticipant(new ContactInfo(contactNum, contactNum, contactName, contactNum));
			Log.d(getClass().getSimpleName(), "Parsing JSON and adding contact to conversation: " + contactNum);
			this.groupParticipantList.put(contactNum, groupParticipant);
		}

		HikeConversationsDatabase db = new HikeConversationsDatabase(context);
		setContactName(db.getGroupName(getMsisdn()));
		db.close();
	}

	public String getGroupOwner()
	{
		return groupOwner;
	}

	public boolean getIsGroupAlive()
	{
		return isGroupAlive;
	}

	public void setGroupParticipantList(Map<String, GroupParticipant> groupParticipantList)
	{
		this.groupParticipantList = groupParticipantList;
	}

	public Map<String, GroupParticipant> getGroupParticipantList()
	{
		return groupParticipantList;
	}

	public GroupParticipant getGroupParticipant(String msisdn)
	{
		return groupParticipantList.get(msisdn);
	}

	public String getLabel()
	{
		return !TextUtils.isEmpty(getContactName()) ? getContactName() : Utils.defaultGroupName(groupParticipantList);
	}

	public JSONObject serialize(String type)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, type);
			object.put(HikeConstants.TO, getMsisdn());
			if(type.equals(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN))
			{
				JSONArray array = new JSONArray();
				for(Entry<String, GroupParticipant> participant : groupParticipantList.entrySet())
				{
					JSONObject nameMsisdn = new JSONObject();
					nameMsisdn.put(HikeConstants.NAME, participant.getValue().getContactInfo().getName());
					nameMsisdn.put(HikeConstants.MSISDN, participant.getKey());
					array.put(nameMsisdn);
				}
				object.put(HikeConstants.DATA, array);
			}
		}
		catch (JSONException e)
		{
			Log.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}
}
