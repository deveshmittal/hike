package com.bsb.hike.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.ContactUtils;

/**
 * 
 * @author Rishabh
 *	This class is used for saving all the mqtt messages in the db based on their types.
 *	Its also used to publish these events for the UI to make the changes, wherever 
 *	applicable.
 *	This class should be a singleton, since only one instance should be used managing
 *	these messages
 */
public class MqttMessageSaver {

	private HikeConversationsDatabase convDb;

	private HikeUserDatabase userDb;

	private SharedPreferences settings;

	private Context context;

	private HikePubSub pubSub;

	private static MqttMessageSaver instance;

	private MqttMessageSaver(Context context) 
	{
		this.convDb = new HikeConversationsDatabase(context);
		this.userDb = new HikeUserDatabase(context);
		this.settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		this.context = context;
		this.pubSub = HikeMessengerApp.getPubSub();
	}

	public static MqttMessageSaver getInstance(Context context)
	{
		if(instance == null)
		{
			synchronized (MqttMessageSaver.class) 
			{
				if(instance == null)
				{
					instance = new MqttMessageSaver(context);
				}
			}
		}
		return instance;
	}

	public void close()
	{
		convDb.close();
		userDb.close();
		instance = null;
	}

	public void saveMqttMessage(JSONObject jsonObj) throws JSONException
	{
		String type = jsonObj.optString(HikeConstants.TYPE);
		if (HikeConstants.MqttMessageTypes.ICON.equals(type)) //Icon changed
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			String iconBase64 = jsonObj.getString(HikeConstants.DATA);
			this.userDb.setIcon(msisdn, Base64.decode(iconBase64, Base64.DEFAULT));

			IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		}
		else if (HikeConstants.MqttMessageTypes.SMS_CREDITS.equals(type)) //Credits changed
		{
			Integer credits =  jsonObj.optInt(HikeConstants.DATA);
			if(settings.getInt(HikeMessengerApp.SMS_SETTING, 0) == 0)
			{
				if(credits > 0)
				{
					convDb.setOverlay(false, null);
				}
			}
			Editor mEditor = settings.edit();
			mEditor.putInt(HikeMessengerApp.SMS_SETTING, credits.intValue());
			mEditor.commit();
			this.pubSub.publish(HikePubSub.SMS_CREDIT_CHANGED, credits);
		}
		else if ((HikeConstants.MqttMessageTypes.USER_JOINED.equals(type)) || (HikeConstants.MqttMessageTypes.USER_LEFT.equals(type))) //User joined/left
		{
			String msisdn = jsonObj.optString(HikeConstants.DATA);
			boolean joined = HikeConstants.MqttMessageTypes.USER_JOINED.equals(type);
			ContactUtils.updateHikeStatus(this.context, msisdn, joined);
			this.convDb.updateOnHikeStatus(msisdn, joined);

			this.pubSub.publish(joined ? HikePubSub.USER_JOINED : HikePubSub.USER_LEFT, msisdn);
		}
		else if (HikeConstants.MqttMessageTypes.INVITE_INFO.equals(type)) //Invite info
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			int invited = data.optInt(HikeConstants.ALL_INVITEE);
			int invited_joined = data.optInt(HikeConstants.ALL_INVITEE_JOINED);
			Editor editor = settings.edit();
			editor.putInt(HikeMessengerApp.INVITED, invited);
			editor.putInt(HikeMessengerApp.INVITED_JOINED, invited_joined);
			editor.commit();
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN.equals(type)) //Group chat join
		{
			GroupConversation groupConversation = new GroupConversation(jsonObj, this.context);

			this.convDb.addGroupParticipants(groupConversation.getMsisdn(), groupConversation.getGroupParticipantList());

			if (!this.convDb.doesConversationExist(groupConversation)) 
			{
				Log.d(getClass().getSimpleName(), "The group conversation does not exists");
				groupConversation =(GroupConversation) this.convDb.addConversation(groupConversation.getMsisdn(), false, "", groupConversation.getGroupOwner());
			}
			saveGroupStatusMsg(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE.equals(type)) //Group chat leave
		{
			String groupId = jsonObj.optString(HikeConstants.TO);
			String msisdn = jsonObj.optString(HikeConstants.FROM);
			this.convDb.setParticipantLeft(groupId, msisdn);
			saveGroupStatusMsg(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_NAME.equals(type)) //Group chat name change
		{
			String groupname = jsonObj.optString(HikeConstants.DATA);
			String groupId = jsonObj.optString(HikeConstants.TO);
			this.convDb.setGroupName(groupId, groupname);

			this.pubSub.publish(HikePubSub.GROUP_NAME_CHANGED, groupId);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_END.equals(type)) //Group chat end
		{
			String groupId = jsonObj.optString(HikeConstants.TO);
			this.convDb.setGroupDead(groupId);
			saveGroupStatusMsg(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE.equals(type)) //Message received from server
		{
			Log.d(getClass().getSimpleName(), "Checking if message exists");
			ConvMessage convMessage = new ConvMessage(jsonObj);
			if (this.convDb.wasMessageReceived(convMessage)) // Check if message was already received by the receiver 
			{
				Log.d(getClass().getSimpleName(), "Message already exists");
				return;
			}

			convDb.addConversationMessages(convMessage);
			Log.d(getClass().getSimpleName(),"Receiver received Message : "
					+ convMessage.getMessage() + "		;	Receiver Msg ID : "
					+ convMessage.getMsgID()+"	; Mapped msgID : " + convMessage.getMappedMsgID());
			// We have to do publish this here since we are adding the message to the db here, and the id is set after inserting into the db.
			this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		}
		else if (HikeConstants.MqttMessageTypes.DELIVERY_REPORT.equals(type)) //Message delivered to receiver
		{
			String id = jsonObj.optString(HikeConstants.DATA);
			long msgID;
			try
			{
				msgID=Long.parseLong(id);
			}
			catch(NumberFormatException e)
			{
				Log.e(getClass().getSimpleName(), "Exception occured while parsing msgId. Exception : "+e);
				msgID = -1;
			}
			Log.d(getClass().getSimpleName(),"Delivery report received for msgid : "+msgID +"	;	REPORT : DELIVERED");
			updateDB(msgID,ConvMessage.State.SENT_DELIVERED.ordinal());

			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED, msgID);	
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE_READ.equals(type)) //Message has been read
		{
			JSONArray msgIds = jsonObj.optJSONArray(HikeConstants.DATA);
			if(msgIds == null)
			{
				Log.e(getClass().getSimpleName(), "Update Error : Message id Array is empty or null . Check problem");
				return;
			}

			long[] ids = new long[msgIds.length()];
			for (int i = 0; i < ids.length; i++)
			{
				ids[i] = msgIds.optLong(i);
			}
			Log.d(getClass().getSimpleName(),"Delivery report received : " +"	;	REPORT : DELIVERED READ");
			updateDbBatch(ids,ConvMessage.State.SENT_DELIVERED_READ.ordinal());

			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED_READ, ids);	
		}
		else if (HikeConstants.MqttMessageTypes.START_TYPING.equals(type)) // Start Typing event received
		{
			String msisdn = jsonObj.optString(HikeConstants.FROM);
			this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, msisdn);
		}
		else if (HikeConstants.MqttMessageTypes.END_TYPING.equals(type)) // End Typing event received
		{
			String msisdn = jsonObj.optString(HikeConstants.FROM);
			this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, msisdn);
		}
	}

	private void updateDbBatch(long[] ids, int status)
	{
		convDb.updateBatch(ids, ConvMessage.State.SENT_DELIVERED_READ.ordinal());
	}

	private void updateDB(Object object, int status)
	{
		long msgID = (Long)object;
		/* TODO we should lookup the convid for this user, since otherwise one could set mess with the state for other conversations */
		convDb.updateMsgStatus(msgID,status);
	}

	private void saveGroupStatusMsg(JSONObject jsonObj) throws JSONException
	{
		HikeConversationsDatabase hCDB = new HikeConversationsDatabase(context);
		Conversation conversation = hCDB.getConversation(jsonObj.getString(HikeConstants.TO), 0);
		hCDB.close();

		ConvMessage convMessage = new ConvMessage(jsonObj, conversation, context, false);
		convDb.addConversationMessages(convMessage);

		this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		this.pubSub.publish(convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED ?
				HikePubSub.PARTICIPANT_JOINED_GROUP : convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED ? 
						HikePubSub.PARTICIPANT_LEFT_GROUP : HikePubSub.GROUP_END, jsonObj);
	}
}