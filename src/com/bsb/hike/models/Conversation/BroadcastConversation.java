/**
 * 
 */
package com.bsb.hike.models.Conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

/**
 * @author anubansal
 *
 */
public class BroadcastConversation extends OneToNConversation
{

	/**
	 * @param builder
	 */
	public BroadcastConversation(InitBuilder<?> builder)
	{
		super(builder);
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bsb.hike.models.Conversation.Conversation#getLabel()
	 */
	@Override
	public String getLabel()
	{
		if (!TextUtils.isEmpty(getConversationName()))
			return getConversationName();
		else
		{
			setConversationParticipantList(ContactManager.getInstance().getGroupParticipants(getMsisdn(), false, false));
			// Before contact manager we were adding all the group participants to conversation object initially when getConversations of HikeConversationDatabase is called
			// But now we do lazy loading, we don't have group participants when we are on home screen
			// In case of empty group name, group Participants are needed so setting it here.
			return OneToNConversation.defaultConversationName(new ArrayList<PairModified<GroupParticipant, String>>(conversationParticipantList.values()));
		}
	}
	
	@Override
    public JSONObject serialize(String type)
    {
        JSONObject object = new JSONObject();
        try
        {
            object.put(HikeConstants.TYPE, type);
            object.put(HikeConstants.TO, getMsisdn());
            if (type.equals(HikeConstants.MqttMessageTypes.BROADCAST_LIST_JOIN))
            {
                JSONArray array = new JSONArray();
                for (Entry<String, PairModified<GroupParticipant, String>> participant : conversationParticipantList.entrySet())
                {
                    JSONObject nameMsisdn = new JSONObject();
                    nameMsisdn.put(HikeConstants.NAME, participant.getValue().getSecond());
                    nameMsisdn.put(HikeConstants.MSISDN, participant.getKey());
                    array.put(nameMsisdn);
                }
                object.put(HikeConstants.DATA, array);
            }
            object.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis() / 1000));
        }
        catch (JSONException e)
        {
            Logger.e("GroupConversation", "invalid json message", e);
        }
        return object;
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

		String name = Utils.extractFullFirstName(broadcastParticipants.get(0).getFirstNameAndSurname());
		switch (broadcastParticipants.size())
		{
		case 0:
			return "";
		case 1:
			return name;
		default:
			for (int i=1; i<broadcastParticipants.size(); i++)
			{
				name += ", " + Utils.extractFullFirstName(broadcastParticipants.get(i).getFirstNameAndSurname());
			}
			return name;
		}
	}
}
