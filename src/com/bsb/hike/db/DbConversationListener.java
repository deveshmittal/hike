package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.FtueContactInfo;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.service.SmsMessageStatusReceiver;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class DbConversationListener implements Listener
{
	private static final String SMS_SENT_ACTION = "com.bsb.hike.SMS_SENT";

	HikeConversationsDatabase mConversationDb;

	HikeMqttPersistence persistence;

	private HikePubSub mPubSub;

	private Context context;

	private int dayRecorded = 0;

	public DbConversationListener(Context context)
	{
		this.context = context;
		mPubSub = HikeMessengerApp.getPubSub();
		mConversationDb = HikeConversationsDatabase.getInstance();
		persistence = HikeMqttPersistence.getInstance();

		dayRecorded = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.DAY_RECORDED, 0);

		mPubSub.addListener(HikePubSub.MESSAGE_SENT, this);
		mPubSub.addListener(HikePubSub.DELETE_MESSAGE, this);
		mPubSub.addListener(HikePubSub.MESSAGE_FAILED, this);
		mPubSub.addListener(HikePubSub.BLOCK_USER, this);
		mPubSub.addListener(HikePubSub.UNBLOCK_USER, this);
		mPubSub.addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		mPubSub.addListener(HikePubSub.FAVORITE_TOGGLED, this);
		mPubSub.addListener(HikePubSub.MUTE_CONVERSATION_TOGGLED, this);
		mPubSub.addListener(HikePubSub.FRIEND_REQUEST_ACCEPTED, this);
		mPubSub.addListener(HikePubSub.REJECT_FRIEND_REQUEST, this);
		mPubSub.addListener(HikePubSub.DELETE_STATUS, this);
		mPubSub.addListener(HikePubSub.HIKE_JOIN_TIME_OBTAINED, this);
		mPubSub.addListener(HikePubSub.SEND_HIKE_SMS_FALLBACK, this);
		mPubSub.addListener(HikePubSub.SEND_NATIVE_SMS_FALLBACK, this);
		mPubSub.addListener(HikePubSub.REMOVE_PROTIP, this);
		mPubSub.addListener(HikePubSub.GAMING_PROTIP_DOWNLOADED, this);
		mPubSub.addListener(HikePubSub.CLEAR_CONVERSATION, this);
		mPubSub.addListener(HikePubSub.UPDATE_PIN_METADATA, this);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.MESSAGE_SENT.equals(type))
		{
			ConvMessage convMessage = (ConvMessage) object;
			boolean shouldSendMessage = convMessage.isFileTransferMessage() && !TextUtils.isEmpty(convMessage.getMetadata().getHikeFiles().get(0).getFileKey());
			if (shouldSendMessage)
			{
				mConversationDb.updateMessageMetadata(convMessage.getMsgID(), convMessage.getMetadata());
			}
			else
			{
				if (!convMessage.isFileTransferMessage())
				{
					mConversationDb.addConversationMessages(convMessage);
					if (convMessage.isSent())
					{
						uploadFiksuPerDayMessageEvent();
					}
				}
				// Recency was already updated when the ft message was added.
				ContactManager.getInstance().updateContactRecency(convMessage.getMsisdn(), convMessage.getTimestamp());

				mPubSub.publish(HikePubSub.RECENT_CONTACTS_UPDATED, convMessage.getMsisdn());
			}

			if ((convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO || convMessage.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
					&& (!convMessage.isFileTransferMessage() || shouldSendMessage))
			{
				Logger.d("DBCONVERSATION LISTENER", "Sending Message : " + convMessage.getMessage() + "	;	to : " + convMessage.getMsisdn());
				if (!convMessage.isSMS() || !Utils.getSendSmsPref(context) || convMessage.getParticipantInfoState() == ParticipantInfoState.CHAT_BACKGROUND)
				{
					mPubSub.publish(HikePubSub.MQTT_PUBLISH, convMessage.serialize());
				}
				else
				{
					Logger.d(getClass().getSimpleName(), "Messages Id: " + convMessage.getMsgID());
					sendNativeSMS(convMessage);
				}
				if (convMessage.isGroupChat())
				{
					convMessage = mConversationDb.showParticipantStatusMessage(convMessage.getMsisdn());
					if(convMessage != null)
					{
						mPubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
					}
				}
			}
		}
		else if (HikePubSub.DELETE_MESSAGE.equals(type))
		{
			Pair<ArrayList<Long>, Bundle> deleteMessage = (Pair<ArrayList<Long>, Bundle>) object;
			ArrayList<Long> msgIds = deleteMessage.first;
			Bundle bundle = deleteMessage.second;
			boolean isLastMessage = bundle.getBoolean(HikeConstants.Extras.IS_LAST_MESSAGE);
			String msisdn = bundle.getString(HikeConstants.Extras.MSISDN);
			
			mConversationDb.deleteMessages(msgIds, msisdn, isLastMessage);
			persistence.removeMessages(msgIds);
		}
		else if (HikePubSub.MESSAGE_FAILED.equals(type)) // server got msg
		// from client 1 and
		// sent back
		// received msg
		// receipt
		{
			updateDB(object, ConvMessage.State.SENT_FAILED.ordinal());
		}
		else if (HikePubSub.BLOCK_USER.equals(type))
		{
			String msisdn = (String) object;
			ContactManager.getInstance().block(msisdn);
			/*
			 * When a user blocks someone, we reset the contact's friend type.
			 */
			ContactManager.getInstance().toggleContactFavorite(msisdn, FavoriteType.NOT_FRIEND);

			JSONObject blockObj = blockUnblockSerialize("b", msisdn);
			/*
			 * We remove the icon for a blocked user as well.
			 */
			HikeMessengerApp.getLruCache().deleteIconForMSISDN(msisdn);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
			// IconCacheManager.getInstance().deleteIconForMSISDN(msisdn);
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, blockObj);
		}
		else if (HikePubSub.UNBLOCK_USER.equals(type))
		{
			String msisdn = (String) object;
			ContactManager.getInstance().unblock(msisdn);
			JSONObject unblockObj = blockUnblockSerialize("ub", msisdn);
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, unblockObj);
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type)) // server got
		// msg from
		// client 1 and
		// sent back
		// received msg
		// receipt
		{
			Logger.d("DBCONVERSATION LISTENER", "(Sender) Message sent confirmed for msgID -> " + (Long) object);
			updateDB(object, ConvMessage.State.SENT_CONFIRMED.ordinal());
		}
		else if (HikePubSub.FAVORITE_TOGGLED.equals(type) || HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type) || HikePubSub.REJECT_FRIEND_REQUEST.equals(type))
		{
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			ContactManager.getInstance().toggleContactFavorite(contactInfo.getMsisdn(), favoriteType);

			if (favoriteType != FavoriteType.REQUEST_RECEIVED && favoriteType != FavoriteType.REQUEST_SENT_REJECTED && !HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type))
			{
				String requestType;
				if (favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_SENT)
				{

					/*
					 * Adding a status message for accepting the friend request
					 */
					if (favoriteType == FavoriteType.FRIEND)
					{
						StatusMessage statusMessage = new StatusMessage(0, null, contactInfo.getMsisdn(), contactInfo.getName(),
								context.getString(R.string.accepted_friend_request), StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST, System.currentTimeMillis() / 1000);
						mConversationDb.addStatusMessage(statusMessage, true);
						mPubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
						mPubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
					}

					requestType = HikeConstants.MqttMessageTypes.ADD_FAVORITE;
				}
				else if (HikePubSub.REJECT_FRIEND_REQUEST.equals(type))
				{
					requestType = HikeConstants.MqttMessageTypes.POSTPONE_FAVORITE;
				}
				else
				{
					requestType = HikeConstants.MqttMessageTypes.REMOVE_FAVORITE;
				}
				mPubSub.publish(HikePubSub.MQTT_PUBLISH, serializeMsg(requestType, contactInfo.getMsisdn(), contactInfo instanceof FtueContactInfo));
			}
		}
		else if (HikePubSub.MUTE_CONVERSATION_TOGGLED.equals(type))
		{
			Pair<String, Boolean> groupMute = (Pair<String, Boolean>) object;

			String id = groupMute.first;
			boolean mute = groupMute.second;

			mConversationDb.toggleGroupMute(id, mute);

			mPubSub.publish(HikePubSub.MQTT_PUBLISH, serializeMsg(mute ? HikeConstants.MqttMessageTypes.MUTE : HikeConstants.MqttMessageTypes.UNMUTE, id));
		}
		else if (HikePubSub.DELETE_STATUS.equals(type))
		{
			String statusId = (String) object;
			mConversationDb.deleteStatus(statusId);
			/*
			 * If the status also has an icon, we delete that as well.
			 */
			ContactManager.getInstance().removeIcon(statusId);
		}
		else if (HikePubSub.HIKE_JOIN_TIME_OBTAINED.equals(type))
		{
			Pair<String, Long> msisdnHikeJoinTimePair = (Pair<String, Long>) object;

			String msisdn = msisdnHikeJoinTimePair.first;
			long hikeJoinTime = msisdnHikeJoinTimePair.second;

			ContactManager.getInstance().setHikeJoinTime(msisdn, hikeJoinTime);
			
		}
		else if (HikePubSub.SEND_HIKE_SMS_FALLBACK.equals(type))
		{
			List<ConvMessage> messages = (List<ConvMessage>) object;
			if (messages.isEmpty())
			{
				return;
			}

			try
			{
				JSONObject jsonObject = new JSONObject();

				jsonObject.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.FORCE_SMS);
				jsonObject.put(HikeConstants.TO, messages.get(0).getMsisdn());

				JSONObject data = new JSONObject();

				JSONArray messagesArray = new JSONArray();

				for (ConvMessage convMessage : messages)
				{

					mConversationDb.updateIsHikeMessageState(convMessage.getMsgID(), false);

					convMessage.setSMS(true);
				}
				
				/*
				 * We will send combined string of all the messages
				 * in json of last convMessage object
				 */
				ConvMessage lastMessage = messages.get(messages.size() -1);
				
				ConvMessage convMessage = new ConvMessage(Utils.combineInOneSmsString(context, true, messages, true), lastMessage.getMsisdn(), 
						lastMessage.getTimestamp(), lastMessage.getState(), lastMessage.getMsgID(), lastMessage.getMappedMsgID());
				JSONObject messageJSON = convMessage.serialize().getJSONObject(HikeConstants.DATA);

				messagesArray.put(messageJSON);

				data.put(HikeConstants.BATCH_MESSAGE, messagesArray);
				data.put(HikeConstants.COUNT, 1);
				data.put(HikeConstants.MESSAGE_ID, messages.get(0).getMsgID());

				jsonObject.put(HikeConstants.DATA, data);

				mPubSub.publish(HikePubSub.CHANGED_MESSAGE_TYPE, null);

				mPubSub.publish(HikePubSub.MQTT_PUBLISH, jsonObject);
			}
			catch (JSONException e)
			{
				Logger.w(getClass().getSimpleName(), "Invalid json", e);
			}

		}
		else if (HikePubSub.SEND_NATIVE_SMS_FALLBACK.equals(type))
		{
			List<ConvMessage> messages = (List<ConvMessage>) object;
			if (messages.isEmpty())
			{
				return;
			}

			ContactInfo contactInfo = ContactManager.getInstance().getContact(messages.get(0).getMsisdn(), true, false);

			sendNativeSMSFallbackLogEvent(contactInfo.isOnhike(), Utils.isUserOnline(context), messages.size());

			for (ConvMessage convMessage : messages)
			{
				convMessage.setSMS(true);
				mConversationDb.updateIsHikeMessageState(convMessage.getMsgID(), false);
			}
			ConvMessage lastMessage = messages.get(messages.size() - 1);
			sendNativeSMS(new ConvMessage(Utils.combineInOneSmsString(context, true, messages, false), lastMessage.getMsisdn(), lastMessage.getTimestamp(), State.UNKNOWN, lastMessage.getMsgID(), -1));

			mPubSub.publish(HikePubSub.CHANGED_MESSAGE_TYPE, null);
		}
		else if (HikePubSub.REMOVE_PROTIP.equals(type))
		{
			String mappedId = (String) object;
			HikeMessengerApp.getLruCache().deleteIconForMSISDN(mappedId);
			// IconCacheManager.getInstance().deleteIconForMSISDN(mappedId);
			mConversationDb.deleteProtip(mappedId);

			sendDismissTipLogEvent(mappedId, null);
		}
		else if (HikePubSub.GAMING_PROTIP_DOWNLOADED.equals(type))
		{
			Protip protip = (Protip) object;

			String mappedId = protip.getMappedId();
			String url = protip.getGameDownlodURL();

			HikeMessengerApp.getLruCache().deleteIconForMSISDN(mappedId);
			// IconCacheManager.getInstance().deleteIconForMSISDN(mappedId);
			mConversationDb.deleteProtip(mappedId);
			sendDismissTipLogEvent(mappedId, url);
		}
		else if (HikePubSub.CLEAR_CONVERSATION.equals(type))
		{
			String msisdn = (String) object;
			mConversationDb.clearConversation(msisdn);
		}
		else if (HikePubSub.UPDATE_PIN_METADATA.equals(type))
		{
			
				Conversation conv = (Conversation)object;
				HikeConversationsDatabase.getInstance().updateConversationMetadata(conv.getMsisdn(), conv.getMetaData());
			
		}
	}

	private void sendNativeSMSFallbackLogEvent(boolean onHike, boolean userOnline, int numMessages)
	{
		JSONObject data = new JSONObject();
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(HikeConstants.IS_H2H, onHike);
			metadata.put(HikeConstants.OFFLINE, userOnline ? HikeConstants.RECIPIENT : HikeConstants.SENDER);
			metadata.put(HikeConstants.NUMBER_OF_SMS, numMessages);

			data.put(HikeConstants.METADATA, metadata);
			data.put(HikeConstants.SUB_TYPE, HikeConstants.SMS);

			Utils.sendLogEvent(data);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
	}

	private void sendDismissTipLogEvent(String tipId, String URL)
	{
		JSONObject data = new JSONObject();
		JSONObject metadata = new JSONObject();
		try
		{
			metadata.put(HikeConstants.TIP_ID, tipId);
			if (!TextUtils.isEmpty(URL))
				metadata.put(HikeConstants.TIP_URL, URL);
			data.put(HikeConstants.SUB_TYPE, HikeConstants.UI_EVENT);
			data.put(HikeConstants.METADATA, metadata);

			Utils.sendLogEvent(data);
		}
		catch (JSONException e)
		{
			Logger.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
	}

	private void sendNativeSMS(ConvMessage convMessage)
	{
		SmsManager smsManager = SmsManager.getDefault();

		ArrayList<String> messages = smsManager.divideMessage(Utils.getMessageDisplayText(convMessage, context));

		ArrayList<PendingIntent> pendingIntents = new ArrayList<PendingIntent>();

		for (int i = 0; i < messages.size(); i++)
		{
			Intent intent = new Intent(SMS_SENT_ACTION + convMessage.getMsgID());
			intent.setClass(context, SmsMessageStatusReceiver.class);
			intent.putExtra(HikeConstants.Extras.SMS_ID, convMessage.getMsgID());
			pendingIntents.add(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
		}

		/*
		 * The try-catch block is needed for a bug in certain LG devices where it throws an NPE here.
		 */
		try
		{
			smsManager.sendMultipartTextMessage(convMessage.getMsisdn(), null, messages, pendingIntents, null);
		}
		catch (NullPointerException e)
		{
			Logger.d(getClass().getSimpleName(), "NPE while trying to send SMS", e);
		}

		writeToNativeSMSDb(convMessage);
	}

	/*
	 * Recording the event on fiksu if this was the first message of the day.
	 */
	private void uploadFiksuPerDayMessageEvent()
	{
		int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		if (today != dayRecorded)
		{
			dayRecorded = today;
			Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeMessengerApp.DAY_RECORDED, dayRecorded);
			editor.commit();
		}
	}

	private JSONObject serializeMsg(String type, String id)
	{
		return serializeMsg(type, id, false);
	}
	
	private JSONObject serializeMsg(String type, String id, boolean isFromFtue)
	{
		JSONObject obj = new JSONObject();
		JSONObject data = new JSONObject();
		try
		{
			if (HikeConstants.MqttMessageTypes.ADD_FAVORITE.equals(type))
			{
				obj.put(HikeConstants.TO, id);
			}
			obj.put(HikeConstants.TYPE, type);
			data.put(HikeConstants.ID, id);
			if(isFromFtue)
			{
				JSONObject ftueData = new JSONObject();
				ftueData.put(HikeConstants.SCREEN, HikeConstants.FTUE);
				data.put(HikeConstants.METADATA, ftueData);
			}
			data.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));
			obj.put(HikeConstants.DATA, data);
			Logger.d(getClass().getSimpleName(), "Sending add friends packet, Object: "+obj.toString());
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid json", e);
		}
		return obj;
	}

	private JSONObject blockUnblockSerialize(String type, String msisdn)
	{
		JSONObject obj = new JSONObject();
		try
		{
			obj.put(HikeConstants.TYPE, type);
			obj.put(HikeConstants.DATA, msisdn);
			obj.put(HikeConstants.MESSAGE_ID, Long.toString(System.currentTimeMillis()/1000));
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}

	private void updateDB(Object object, int status)
	{
		long msgID = (Long) object;
		/*
		 * TODO we should lookup the convid for this user, since otherwise one could set mess with the state for other conversations
		 */
		mConversationDb.updateMsgStatus(msgID, status, null);
	}

	private void writeToNativeSMSDb(ConvMessage convMessage)
	{

		ContentValues values = new ContentValues();
		values.put(HikeConstants.SMSNative.NUMBER, convMessage.getMsisdn());
		values.put(HikeConstants.SMSNative.DATE, convMessage.getTimestamp() * 1000);
		values.put(HikeConstants.SMSNative.MESSAGE, Utils.getMessageDisplayText(convMessage, context));

		context.getContentResolver().insert(HikeConstants.SMSNative.SENTBOX_CONTENT_URI, values);
	}

}
