package com.bsb.hike.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ChatThread;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.OriginType;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.GroupConversation;
import com.bsb.hike.models.Conversation.OneToNConversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.ui.CreateNewGroupOrBroadcastActivity;

public class OneToNConversationUtils {

	public static String getParticipantAddedMessage(ConvMessage convMessage, Context context, String highlight)
	{
		String participantAddedMessage;
		MessageMetadata metadata = convMessage.getMetadata();
		if (convMessage.isBroadcastConversation())
		{
			if (metadata.isNewBroadcast())
			{
				participantAddedMessage = String.format(context.getString(R.string.new_broadcast_message), highlight);
			}
			else
			{
				participantAddedMessage = String.format(context.getString(R.string.add_to_broadcast_message), highlight);
			}
		}
		else
		{
			if (metadata.isNewGroup())
			{
				participantAddedMessage = String.format(context.getString(R.string.new_group_message), highlight);
			}
			else
			{
				participantAddedMessage = String.format(context.getString(R.string.add_to_group_message), highlight);
			}
		}
		return participantAddedMessage;
	}
	
	public static String getParticipantRemovedMessage(String msisdn, Context context, String participantName)
	{
		String participantRemovedMessage = String.format(context.getString(Utils.isBroadcastConversation(msisdn) ? R.string.removed_from_broadcast : R.string.left_conversation), participantName);
		return participantRemovedMessage;
	}
	
	public static String getConversationNameChangedMessage(String msisdn, Context context, String participantName)
	{
		String nameChangedMessage = String.format(context.getString(Utils.isBroadcastConversation(msisdn) ? R.string.change_broadcast_name : R.string.change_group_name), participantName);
		return nameChangedMessage;
	}
	
	public static String getConversationEndedMessage(String msisdn, Context context)
	{
		String message = context.getString(Utils.isBroadcastConversation(msisdn) ? R.string.broadcast_list_end : R.string.group_chat_end);
		return message;
	}
	
	public static boolean isOneToNConversation(String msisdn)
	{
		return Utils.isGroupConversation(msisdn) || Utils.isBroadcastConversation(msisdn);
	}
	
	public static void createGroup(Activity activity, ArrayList<ContactInfo> selectedContactList)
	{
		String groupId;
		String groupName;
		if (activity.getIntent().hasExtra(HikeConstants.Extras.GROUP_NAME))
		{
			groupName = activity.getIntent().getStringExtra(HikeConstants.Extras.GROUP_NAME);
		}
		else
		{
			groupName = BroadcastConversation.defaultBroadcastName(selectedContactList);
		}
		if (activity.getIntent().hasExtra(HikeConstants.Extras.BROADCAST_LIST))
		{
			groupId = activity.getIntent().getStringExtra(HikeConstants.Extras.EXISTING_BROADCAST_LIST);
		}
		else
		{
			groupId = activity.getIntent().getStringExtra(HikeConstants.Extras.EXISTING_GROUP_CHAT);
		}
		boolean newGroup = false;

		if (TextUtils.isEmpty(groupId))
		{
			// Create new group
			if (activity.getIntent().hasExtra(HikeConstants.IS_BROADCAST))
			{
				groupId = ((CreateNewGroupOrBroadcastActivity) activity).getGroupOrBroadcastId();
			}
			else
			{
				groupId = activity.getIntent().getStringExtra(HikeConstants.Extras.GROUP_BROADCAST_ID);
			}
			newGroup = true;
		}
		else
		{
			// Group alredy exists. Fetch existing participants.
			newGroup = false;
		}
		Map<String, PairModified<GroupParticipant, String>> participantList = new HashMap<String, PairModified<GroupParticipant, String>>();

		for (ContactInfo particpant : selectedContactList)
		{
			GroupParticipant groupParticipant = new GroupParticipant(particpant);
			participantList.put(particpant.getMsisdn(), new PairModified<GroupParticipant, String>(groupParticipant, groupParticipant.getContactInfo().getNameOrMsisdn()));
		}
		ContactInfo userContactInfo = Utils.getUserContactInfo(activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, Context.MODE_PRIVATE));

		OneToNConversation groupConversation;
		if (activity.getIntent().hasExtra(HikeConstants.IS_BROADCAST))
		{
			groupConversation = new BroadcastConversation.ConversationBuilder(groupId).setConversationOwner(userContactInfo.getMsisdn()).setIsAlive(true).build();
		}
		else
		{
			groupConversation = new GroupConversation.ConversationBuilder(groupId).setConversationOwner(userContactInfo.getMsisdn()).setIsAlive(true).build();
		}
		groupConversation.setConversationParticipantList(participantList);

		Logger.d(activity.getClass().getSimpleName(), "Creating group: " + groupId);
		HikeConversationsDatabase mConversationDb = HikeConversationsDatabase.getInstance();
		mConversationDb.addRemoveGroupParticipants(groupId, groupConversation.getConversationParticipantList(), false);
		if (newGroup)
		{
			mConversationDb.addConversation(groupConversation.getMsisdn(), false, groupName, groupConversation.getConversationOwner());
			ContactManager.getInstance().insertGroup(groupConversation.getMsisdn(),groupName);
		}

		try
		{
			// Adding this boolean value to show a different system message
			// if its a new group
			JSONObject gcjPacket = groupConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
			if (groupConversation instanceof BroadcastConversation)
			{
				gcjPacket.put(HikeConstants.NEW_GROUP, newGroup);
			}
			else if (groupConversation instanceof GroupConversation)
			{
				gcjPacket.put(HikeConstants.NEW_BROADCAST, newGroup);
			}
			ConvMessage msg = new ConvMessage(gcjPacket, groupConversation, activity.getBaseContext(), true);
			ContactManager.getInstance().updateGroupRecency(groupId, msg.getTimestamp());
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, msg);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
		JSONObject gcjJson = groupConversation.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN);
		/*
		 * Adding the group name to the packet
		 */
		if (newGroup)
		{
			JSONObject metadata = new JSONObject();
			try
			{
				metadata.put(HikeConstants.NAME, groupName);

				String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
				String fileName = Utils.getTempProfileImageFileName(groupId);
				File groupImageFile = new File(directory, fileName);

				if (groupImageFile.exists())
				{
					metadata.put(HikeConstants.REQUEST_DP, true);
				}

				gcjJson.put(HikeConstants.METADATA, metadata);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		HikeMqttManagerNew.getInstance().sendMessage(gcjJson, HikeMqttManagerNew.MQTT_QOS_ONE);

		ContactInfo conversationContactInfo = new ContactInfo(groupId, groupId, groupId, groupId);
		Intent intent = IntentFactory.createChatThreadIntentFromContactInfo(activity, conversationContactInfo, true);
		intent.setClass(activity, ChatThread.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
		activity.finish();
	}

	public static String defaultGroupName(List<PairModified<GroupParticipant, String>> participantList)
	{
		List<GroupParticipant> groupParticipants = new ArrayList<GroupParticipant>();
		for (PairModified<GroupParticipant, String> participant : participantList)
		{
			if (!participant.getFirst().hasLeft())
			{
				groupParticipants.add(participant.getFirst());
			}
		}
		Collections.sort(groupParticipants);
		String name = null;
		if (groupParticipants.size() > 0)
		{
			name = Utils.extractFullFirstName(groupParticipants.get(0).getContactInfo().getFirstNameAndSurname());
		}
		switch (groupParticipants.size())
		{
		case 0:
			return "";
		case 1:
			return name;
		default:
			for (int i=1; i<groupParticipants.size(); i++)
			{
				name += ", " + Utils.extractFullFirstName(groupParticipants.get(i).getContactInfo().getFirstNameAndSurname());
			}
			return name;
		}
	}

	public static boolean isGroupConversation(String msisdn)
	{
		return msisdn != null && !msisdn.startsWith("+");
	}
	
	public static boolean isBroadcastConversation(String msisdn)
	{
		return msisdn!=null && msisdn.startsWith("b:");
	}

	public static void addBroadcastRecipientConversations(ConvMessage convMessage)
	{
		
		ArrayList<ContactInfo> contacts = HikeConversationsDatabase.getInstance().addBroadcastRecipientConversations(convMessage);
		
		sendPubSubForConvScreenBroadcastMessage(convMessage, contacts);
        // publishing mqtt packet
        HikeMqttManagerNew.getInstance().sendMessage(convMessage.serializeDeliveryReportRead(), HikeMqttManagerNew.MQTT_QOS_ONE);
	}
	

	public static void sendPubSubForConvScreenBroadcastMessage(ConvMessage convMessage, ArrayList<ContactInfo> recipient)
	{
		long firstMsgId = convMessage.getMsgID() + 1;
		int totalRecipient = recipient.size();
		List<Pair<ContactInfo, ConvMessage>> allPairs = new ArrayList<Pair<ContactInfo,ConvMessage>>(totalRecipient);
		long timestamp = System.currentTimeMillis()/1000;
		for(int i=0;i<totalRecipient;i++)
		{
			ConvMessage message = new ConvMessage(convMessage);
			if(convMessage.isBroadcastConversation())
			{
				message.setMessageOriginType(OriginType.BROADCAST);
			}
			else
			{
				//multi-forward case... in braodcast case we donot need to update timestamp
				message.setTimestamp(timestamp++);
			}
			message.setMsgID(firstMsgId+i);
			ContactInfo contactInfo = recipient.get(i);
			message.setMsisdn(contactInfo.getMsisdn());
			Pair<ContactInfo, ConvMessage> pair = new Pair<ContactInfo, ConvMessage>(contactInfo, message);
			allPairs.add(pair);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_MESSAGE_DB_INSERTED, allPairs);
	}
}
