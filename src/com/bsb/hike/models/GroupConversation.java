package com.bsb.hike.models;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class GroupConversation extends Conversation
{

	private String groupOwner;

	private boolean isGroupAlive;

	private boolean isMuted;

	private boolean hasSmsUser;

	private Map<String, GroupParticipant> groupParticipantList;
	
	private long lastSentMsgId = -1;
	
	private LinkedList<String> readByParticipantsList;

	private int groupMemberAliveCount;

	public GroupConversation(String msisdn, long convId, String contactName, String groupOwner, boolean isGroupAlive)
	{
		super(msisdn, convId, contactName, true);
		this.groupOwner = groupOwner;
		this.isGroupAlive = isGroupAlive;
	}

	public GroupConversation(JSONObject jsonObject, Context context) throws JSONException
	{
		super(jsonObject.getString(HikeConstants.TO), 0, null, true);
		this.groupOwner = jsonObject.getString(HikeConstants.FROM);
		this.groupParticipantList = new HashMap<String, GroupParticipant>();
		JSONArray array = jsonObject.getJSONArray(HikeConstants.DATA);
		for (int i = 0; i < array.length(); i++)
		{
			JSONObject nameMsisdn = array.getJSONObject(i);
			String contactNum = nameMsisdn.getString(HikeConstants.MSISDN);
			String contactName = nameMsisdn.getString(HikeConstants.NAME);
			boolean onHike = nameMsisdn.optBoolean(HikeConstants.ON_HIKE);
			boolean onDnd = nameMsisdn.optBoolean(HikeConstants.DND);
			GroupParticipant groupParticipant = new GroupParticipant(new ContactInfo(contactNum, contactNum, contactName, contactNum, onHike), false, onDnd);
			Logger.d(getClass().getSimpleName(), "Parsing JSON and adding contact to conversation: " + contactNum);
			this.groupParticipantList.put(contactNum, groupParticipant);
		}

		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		setContactName(db.getGroupName(getMsisdn()));
	}

	public String getGroupOwner()
	{
		return groupOwner;
	}

	public boolean getIsGroupAlive()
	{
		return isGroupAlive;
	}

	public void setGroupAlive(boolean isAlive)
	{
		this.isGroupAlive = isAlive;
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
		return groupParticipantList.containsKey(msisdn) ? groupParticipantList.get(msisdn) : new GroupParticipant(new ContactInfo(msisdn, msisdn, msisdn, msisdn));
	}

	public String getGroupParticipantFirstName(String msisdn)
	{
		return getGroupParticipant(msisdn).getContactInfo().getFirstName();
	}
	
	public String getGroupParticipantFirstNameAndSurname(String msisdn)
	{
		return getGroupParticipant(msisdn).getContactInfo().getFirstNameAndSurname();
	}

	public String getLabel()
	{
		return !TextUtils.isEmpty(getContactName()) ? getContactName() : Utils.defaultGroupName(groupParticipantList);
	}

	public void setIsMuted(boolean isMuted)
	{
		this.isMuted = isMuted;
	}

	public boolean isMuted()
	{
		return isMuted;
	}

	public void setHasSmsUser(boolean hasSmsUser)
	{
		this.hasSmsUser = hasSmsUser;
	}

	public boolean hasSmsUser()
	{
		return hasSmsUser;
	}

	public int getGroupMemberAliveCount()
	{
		return groupMemberAliveCount;
	}

	public void setGroupMemberAliveCount(int groupMemberAliveCount)
	{
		this.groupMemberAliveCount = groupMemberAliveCount;
	}

	public JSONObject serialize(String type)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, type);
			object.put(HikeConstants.TO, getMsisdn());
			if (type.equals(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN))
			{
				JSONArray array = new JSONArray();
				for (Entry<String, GroupParticipant> participant : groupParticipantList.entrySet())
				{
					JSONObject nameMsisdn = new JSONObject();
					nameMsisdn.put(HikeConstants.NAME, participant.getValue().getContactInfo().getName());
					nameMsisdn.put(HikeConstants.MSISDN, participant.getKey());
					array.put(nameMsisdn);
				}
				object.put(HikeConstants.DATA, array);
			}
			object.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));
		}
		catch (JSONException e)
		{
			Logger.e("ConvMessage", "invalid json message", e);
		}
		return object;
	}
	
	public void setupReadByList(String readBy, long msgId)
	{
		if (msgId < 1)
		{
			return;
		}
		
		if (readByParticipantsList == null)
		{
			readByParticipantsList = new LinkedList<String>();
		}
		readByParticipantsList.clear();
		lastSentMsgId = msgId;
		
		if (readBy == null)
		{
			return;
		}
		try
		{
			JSONArray readByArray;
			readByArray = new JSONArray(readBy);
			for (int i = 0; i < readByArray.length(); i++)
			{
				readByParticipantsList.add(readByArray.optString(i));
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void updateReadByList(String msisdn, long msgId)
	{
		if (lastSentMsgId > msgId || TextUtils.isEmpty(msisdn))
		{
			return;
		}
		if (readByParticipantsList == null)
		{
			readByParticipantsList = new LinkedList<String>();
		}
		
		if (lastSentMsgId == msgId)
		{
			if (!readByParticipantsList.contains(msisdn))
			{
				readByParticipantsList.add(msisdn);
			}
		}
		else if (lastSentMsgId < msgId)
		{
			readByParticipantsList.clear();
			readByParticipantsList.add(msisdn);
			lastSentMsgId = msgId;
		}
	}
	
	public LinkedList<String> getReadByList()
	{
		return readByParticipantsList;
	}
}
