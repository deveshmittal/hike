package com.bsb.hike.models.Conversation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.Utils;

/**
 * 1-n conversation primitives will be derived from this class.<br>
 * Examples of 1-n conversations : private group chats, broadcast, public chats
 * 
 * @author Anu/Piyush
 * 
 */
public abstract class OneToNConversation extends Conversation
{

	private String conversationOwner;

	private Map<String, PairModified<GroupParticipant, String>> groupParticipantList;

	private ArrayList<String> readByParticipantsList;

	private ConvMessage pinnedConvMessage;

	private boolean isConversationAlive;

	/**
	 * Default value of long is 0, hence setting this as -1 here
	 */
	private long lastSentMsgId = -1;

	private int unreadPinnedMessageCount;

	/**
	 * @param builder
	 */
	protected OneToNConversation(InitBuilder<?> builder)
	{
		super(builder);
	}

	/**
	 * @return the conversationOwner
	 */
	public String getConversationOwner()
	{
		return conversationOwner;
	}

	/**
	 * @param conversationOwner
	 *            the conversationOwner to set
	 */
	public void setConversationOwner(String conversationOwner)
	{
		this.conversationOwner = conversationOwner;
	}

	/**
	 * @return the groupParticipantList
	 */
	public Map<String, PairModified<GroupParticipant, String>> getGroupParticipantList()
	{
		return groupParticipantList;
	}

	/**
	 * @param groupParticipantList
	 *            the groupParticipantList to set
	 */
	public void setGroupParticipantList(Map<String, PairModified<GroupParticipant, String>> groupParticipantList)
	{
		this.groupParticipantList = groupParticipantList;
	}

	public void setGroupParticipantList(List<PairModified<GroupParticipant, String>> groupParticipantList)
	{
		this.groupParticipantList = new HashMap<String, PairModified<GroupParticipant, String>>();
		for (PairModified<GroupParticipant, String> grpParticipant : groupParticipantList)
		{
			String msisdn = grpParticipant.getFirst().getContactInfo().getMsisdn();
			this.groupParticipantList.put(msisdn, grpParticipant);
		}
	}

	public PairModified<GroupParticipant, String> getGroupParticipant(String msisdn)
	{
		return groupParticipantList.containsKey(msisdn) ? groupParticipantList.get(msisdn) : new PairModified<GroupParticipant, String>(new GroupParticipant(new ContactInfo(
				msisdn, msisdn, msisdn, msisdn)), msisdn);
	}

	/**
	 * Used to get the name of the contact either from the groupParticipantList or ContactManager
	 * 
	 * @param msisdn
	 *            of the contact
	 * @return name of the contact
	 */
	public String getGroupParticipantName(String msisdn)
	{
		String name = null;

		if (null != groupParticipantList)
		{
			PairModified<GroupParticipant, String> grpPair = groupParticipantList.get(msisdn);

			if (null != grpPair)
			{
				name = grpPair.getSecond();
			}
		}

		/*
		 * If groupParticipantsList is not loaded(in case of conversation screen as we load group members when we enter into GC) then we get name from contact manager
		 */
		if (null == name)
		{
			HikeMessengerApp.getContactManager().getContact(msisdn, true, false);
			name = HikeMessengerApp.getContactManager().getName(getMsisdn(), msisdn);
		}
		return name;
	}

	/**
	 * Used to get the first full name of the contact whose msisdn is known
	 * 
	 * @param msisdn
	 *            of the contact
	 * @return first full name of the contact
	 */
	public String getGroupParticipantFullFirstName(String msisdn)
	{
		String fullName = getGroupParticipantName(msisdn);

		return Utils.extractFullFirstName(fullName);
	}

	/**
	 * Used to get the first name and the last name of the contact whose msisdn is known
	 * 
	 * @param msisdn
	 *            of the contact
	 * @return first name + last name of the contact
	 */
	public String getGroupParticipantFirstNameAndSurname(String msisdn)
	{
		return getGroupParticipantName(msisdn);
	}

	/**
	 * @return the pinnedConvMessage
	 */
	public ConvMessage getPinnedConvMessage()
	{
		return pinnedConvMessage;
	}

	/**
	 * @param pinnedConvMessage
	 *            the pinnedConvMessage to set
	 */
	public void setPinnedConvMessage(ConvMessage pinnedConvMessage)
	{
		this.pinnedConvMessage = pinnedConvMessage;
	}

	/**
	 * @return the isConversationAlive
	 */
	public boolean isConversationAlive()
	{
		return isConversationAlive;
	}

	/**
	 * @param isConversationAlive
	 *            the isConversationAlive to set
	 */
	public void setConversationAlive(boolean isConversationAlive)
	{
		this.isConversationAlive = isConversationAlive;
	}

	public void setIsMute(boolean isMute)
	{
		convInfo.setMute(isMute);
	}

	public boolean isMuted()
	{
		return convInfo.isMute();
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
				for (Entry<String, PairModified<GroupParticipant, String>> participant : groupParticipantList.entrySet())
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
			Logger.e("Conversation", "invalid json message", e);
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
			readByParticipantsList = new ArrayList<String>();
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
			readByParticipantsList = new ArrayList<String>();
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

	/**
	 * @return the readByParticipantsList
	 */
	public ArrayList<String> getReadByParticipantsList()
	{
		return readByParticipantsList;
	}

	/**
	 * @return the unreadPinnedMessageCount
	 */
	public int getUnreadPinnedMessageCount()
	{
		return unreadPinnedMessageCount;
	}

	/**
	 * @param unreadPinnedMessageCount
	 *            the unreadPinnedMessageCount to set
	 */
	public void setUnreadPinnedMessageCount(int unreadPinnedMessageCount)
	{
		this.unreadPinnedMessageCount = unreadPinnedMessageCount;
	}

	@Override
	public void setMetadata(ConversationMetadata metadata)
	{
		if (!(metadata instanceof OneToNConversationMetadata))
		{
			throw new IllegalStateException("Pass metadata as OneToNConversationMetadata object for such type of conversations!");
		}

		this.metadata = (OneToNConversationMetadata) metadata;
	}

	@Override
	public OneToNConversationMetadata getMetadata()
	{
		return (OneToNConversationMetadata) this.metadata;
	}

}
