package com.bsb.hike.models.Conversation;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;

/**
 * Conversation primitive for GroupChats
 * 
 * @author Anu/Piyush
 * 
 */
public class GroupConversation extends OneToNConversation
{

	private GroupConversation(InitBuilder<?> builder)
	{
		super(builder);
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
			if (type.equals(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN))
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

	/**
	 * Builder base class extending {@link OneToNConversation.InitBuilder}
	 * 
	 * @author piyush
	 * 
	 * @param <P>
	 */
	protected static abstract class InitBuilder<P extends InitBuilder<P>> extends OneToNConversation.InitBuilder<P>
	{

		public InitBuilder(String msisdn)
		{
			super(msisdn);
		}

		public GroupConversation build()
		{
			return new GroupConversation(this);
		}
	}

	/**
	 * Builder class used to generating {@link GroupConversation}
	 * <p>
	 * Bare bone Usage : GroupConversation conv = GroupConversation.ConversationBuilder(msisdn).build();<br>
	 * Other examples : GroupConversation conv = GroupConversation.ConversationBuilder(msisdn).setConvName("ABC").setIsAlive(true).build();
	 * 
	 * @author piyush
	 * 
	 */
	public static class ConversationBuilder extends GroupConversation.InitBuilder<ConversationBuilder>
	{

		public ConversationBuilder(String msisdn)
		{
			super(msisdn);
		}

		@Override
		protected ConversationBuilder getSelfObject()
		{
			return this;
		}

	}

}
