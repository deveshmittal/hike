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
import com.bsb.hike.NetworkManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.utils.ContactUtils;

public class MqttMessageSaver {

	HikeConversationsDatabase convDb;

	HikeUserDatabase userDb;

	SharedPreferences settings;

	Context context;

	public MqttMessageSaver(Context context) 
	{
		this.convDb = new HikeConversationsDatabase(context);
		this.userDb = new HikeUserDatabase(context);
		this.settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		this.context = context;
	}

	public void close()
	{
		convDb.close();
		userDb.close();
	}

	public void saveMqttMessage(JSONObject jsonObj)
	{
		String type = jsonObj.optString(HikeConstants.TYPE);
		try 
		{
			if (NetworkManager.ICON.equals(type)) //Icon changed
			{
				String msisdn = jsonObj.getString(HikeConstants.FROM);
				String iconBase64 = jsonObj.getString(HikeConstants.DATA);
				this.userDb.setIcon(msisdn, Base64.decode(iconBase64, Base64.DEFAULT));
			}
			else if (NetworkManager.SMS_CREDITS.equals(type)) //Credits changed
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
			}
			else if ((NetworkManager.USER_JOINED.equals(type)) || (NetworkManager.USER_LEFT.equals(type))) //User joined/left
			{
				String msisdn = jsonObj.optString(HikeConstants.DATA);
				boolean joined = NetworkManager.USER_JOINED.equals(type);
				ContactUtils.updateHikeStatus(this.context, msisdn, joined);
				this.convDb.updateOnHikeStatus(msisdn, joined);
			}
			else if (NetworkManager.INVITE_INFO.equals(type)) //Invite info
			{
				JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
				int invited = data.optInt(HikeConstants.ALL_INVITEE);
				int invited_joined = data.optInt(HikeConstants.ALL_INVITEE_JOINED);
				Editor editor = settings.edit();
				editor.putInt(HikeMessengerApp.INVITED, invited);
				editor.putInt(HikeMessengerApp.INVITED_JOINED, invited_joined);
				editor.commit();
			}
			else if (NetworkManager.GROUP_CHAT_JOIN.equals(type)) //Group chat join
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
			else if (NetworkManager.GROUP_CHAT_LEAVE.equals(type)) //Group chat leave
			{
				String groupId = jsonObj.optString(HikeConstants.TO);
				String msisdn = jsonObj.optString(HikeConstants.FROM);
				this.convDb.setParticipantLeft(groupId, msisdn);
				saveGroupStatusMsg(jsonObj);
			}
			else if (NetworkManager.GROUP_CHAT_NAME.equals(type)) //Group chat name change
			{
				String groupname = jsonObj.optString(HikeConstants.DATA);
				String groupId = jsonObj.optString(HikeConstants.TO);
				this.convDb.setGroupName(groupId, groupname);
			}
			else if (NetworkManager.GROUP_CHAT_END.equals(type)) //Group chat end
			{
				String groupId = jsonObj.optString(HikeConstants.TO);
				this.convDb.setGroupDead(groupId);
				saveGroupStatusMsg(jsonObj);
			}
			else if (NetworkManager.MESSAGE.equals(type)) //Message received from server
			{
				try
				{
					ConvMessage convMessage = new ConvMessage(jsonObj);
					convDb.addConversationMessages(convMessage);
					Log.d(getClass().getSimpleName(),"Receiver received Message : "
							+ convMessage.getMessage() + "		;	Receiver Msg ID : "
							+ convMessage.getMsgID()+"	; Mapped msgID : " + convMessage.getMappedMsgID());
					// We have to do publish this here since we are adding the message to the db here, and the id is set after inserting into the db.
					HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
				}
				catch (JSONException e)
				{
					Log.d(getClass().getSimpleName(), "Invalid JSON", e);
				}
			}
			else if (NetworkManager.DELIVERY_REPORT.equals(type)) //Message delivered to receiver
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
			}
			else if (NetworkManager.MESSAGE_READ.equals(type)) //Message has been read
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
			}
		} 
		catch (JSONException e) 
		{
			Log.e(getClass().getSimpleName(), "Invalid json", e);
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

	private void saveGroupStatusMsg(JSONObject jsonObj)
	{
		try {
			HikeConversationsDatabase hCDB = new HikeConversationsDatabase(context);
			Conversation conversation = hCDB.getConversation(jsonObj.getString(HikeConstants.TO), 0);
			hCDB.close();
			
			ConvMessage convMessage = new ConvMessage(jsonObj, conversation, context, false);
			convDb.addConversationMessages(convMessage);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}