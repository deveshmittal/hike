package com.bsb.hike.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
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
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.AccountUtils;

public class DbConversationListener implements Listener {
	private static final String SMS_SENT_ACTION = "com.bsb.hike.SMS_SENT";

	HikeConversationsDatabase mConversationDb;

	HikeUserDatabase mUserDb;

	HikeMqttPersistence persistence;

	private HikePubSub mPubSub;

	private Context context;

	private int dayRecorded = 0;

	public DbConversationListener(Context context) {
		this.context = context;
		mPubSub = HikeMessengerApp.getPubSub();
		mConversationDb = HikeConversationsDatabase.getInstance();
		mUserDb = HikeUserDatabase.getInstance();
		persistence = HikeMqttPersistence.getInstance();

		dayRecorded = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(
				HikeMessengerApp.DAY_RECORDED, 0);

		context.registerReceiver(smsMessageStatusReceiver, new IntentFilter(
				SMS_SENT_ACTION));

		mPubSub.addListener(HikePubSub.MESSAGE_SENT, this);
		mPubSub.addListener(HikePubSub.DELETE_MESSAGE, this);
		mPubSub.addListener(HikePubSub.MESSAGE_FAILED, this);
		mPubSub.addListener(HikePubSub.BLOCK_USER, this);
		mPubSub.addListener(HikePubSub.UNBLOCK_USER, this);
		mPubSub.addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		mPubSub.addListener(HikePubSub.SHOW_PARTICIPANT_STATUS_MESSAGE, this);
		mPubSub.addListener(HikePubSub.FAVORITE_TOGGLED, this);
		mPubSub.addListener(HikePubSub.MUTE_CONVERSATION_TOGGLED, this);
		mPubSub.addListener(HikePubSub.FRIEND_REQUEST_ACCEPTED, this);
		mPubSub.addListener(HikePubSub.REJECT_FRIEND_REQUEST, this);
		mPubSub.addListener(HikePubSub.DELETE_STATUS, this);
		mPubSub.addListener(HikePubSub.HIKE_JOIN_TIME_OBTAINED, this);
		mPubSub.addListener(HikePubSub.SEND_HIKE_SMS_FALLBACK, this);
		mPubSub.addListener(HikePubSub.SEND_NATIVE_SMS_FALLBACK, this);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.MESSAGE_SENT.equals(type)) {
			ConvMessage convMessage = (ConvMessage) object;
			boolean shouldSendMessage = convMessage.isFileTransferMessage()
					&& !TextUtils.isEmpty(convMessage.getMetadata()
							.getHikeFiles().get(0).getFileKey());
			if (shouldSendMessage) {
				mConversationDb.updateMessageMetadata(convMessage.getMsgID(),
						convMessage.getMetadata());
			} else {
				if (!convMessage.isFileTransferMessage()) {
					mConversationDb.addConversationMessages(convMessage);
					if (convMessage.isSent()) {
						uploadFiksuPerDayMessageEvent();
					}
				}
				// Recency was already updated when the ft message was added.
				mUserDb.updateContactRecency(convMessage.getMsisdn(),
						convMessage.getTimestamp());
				mPubSub.publish(HikePubSub.RECENT_CONTACTS_UPDATED,
						convMessage.getMsisdn());
			}

			if (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO
					&& (!convMessage.isFileTransferMessage() || shouldSendMessage)) {
				Log.d("DBCONVERSATION LISTENER",
						"Sending Message : " + convMessage.getMessage()
								+ "	;	to : " + convMessage.getMsisdn());
				if (!convMessage.isSMS()
						|| !PreferenceManager.getDefaultSharedPreferences(
								context).getBoolean(
								HikeConstants.SEND_SMS_PREF, false)) {
					mPubSub.publish(HikePubSub.MQTT_PUBLISH,
							convMessage.serialize());
				} else {
					Log.d(getClass().getSimpleName(), "Messages Id: "
							+ convMessage.getMsgID());
					sendNativeSMS(convMessage);
				}
				if (convMessage.isGroupChat()) {
					mPubSub.publish(HikePubSub.SHOW_PARTICIPANT_STATUS_MESSAGE,
							convMessage.getMsisdn());
				}
			}
		} else if (HikePubSub.DELETE_MESSAGE.equals(type)) {
			Pair<ConvMessage, Boolean> deleteMessage = (Pair<ConvMessage, Boolean>) object;
			ConvMessage message = deleteMessage.first;
			mConversationDb.deleteMessage(deleteMessage.first,
					deleteMessage.second);
			persistence.removeMessage(message.getMsgID());
		} else if (HikePubSub.MESSAGE_FAILED.equals(type)) // server got msg
		// from client 1 and
		// sent back
		// received msg
		// receipt
		{
			updateDB(object, ConvMessage.State.SENT_FAILED.ordinal());
		} else if (HikePubSub.BLOCK_USER.equals(type)) {
			String msisdn = (String) object;
			mUserDb.block(msisdn);
			/*
			 * When a user blocks someone, we reset the contact's friend type.
			 */
			mUserDb.toggleContactFavorite(msisdn, FavoriteType.NOT_FRIEND);
			JSONObject blockObj = blockUnblockSerialize("b", msisdn);
			/*
			 * We remove the icon for a blocked user as well.
			 */
			IconCacheManager.getInstance().deleteIconForMSISDN(msisdn);
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, blockObj);
		} else if (HikePubSub.UNBLOCK_USER.equals(type)) {
			String msisdn = (String) object;
			mUserDb.unblock(msisdn);
			JSONObject unblockObj = blockUnblockSerialize("ub", msisdn);
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, unblockObj);
		} else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type)) // server got
		// msg from
		// client 1 and
		// sent back
		// received msg
		// receipt
		{
			Log.d("DBCONVERSATION LISTENER",
					"(Sender) Message sent confirmed for msgID -> "
							+ (Long) object);
			updateDB(object, ConvMessage.State.SENT_CONFIRMED.ordinal());
		} else if (HikePubSub.SHOW_PARTICIPANT_STATUS_MESSAGE.equals(type)) {
			String groupId = (String) object;

			Map<String, GroupParticipant> smsParticipants = mConversationDb
					.getGroupParticipants(groupId, true, true);

			if (smsParticipants.isEmpty()) {
				return;
			}

			JSONObject dndJSON = new JSONObject();
			JSONArray dndParticipants = new JSONArray();

			for (Entry<String, GroupParticipant> smsParticipantEntry : smsParticipants
					.entrySet()) {
				GroupParticipant smsParticipant = smsParticipantEntry
						.getValue();
				String msisdn = smsParticipantEntry.getKey();
				if (smsParticipant.onDnd()) {
					dndParticipants.put(msisdn);
				}
			}

			if (dndParticipants.length() == 0) {
				// No DND participants. Just return
				return;
			}
			try {
				dndJSON.put(HikeConstants.FROM, groupId);
				dndJSON.put(HikeConstants.TYPE, HikeConstants.DND);
				dndJSON.put(HikeConstants.DND_USERS, dndParticipants);

				ConvMessage convMessage = new ConvMessage(dndJSON, null,
						context, false);
				mConversationDb.addConversationMessages(convMessage);
				mConversationDb.updateShownStatus(groupId);

				mPubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
			} catch (JSONException e) {
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)
				|| HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type)
				|| HikePubSub.REJECT_FRIEND_REQUEST.equals(type)) {
			final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			ContactInfo contactInfo = favoriteToggle.first;
			FavoriteType favoriteType = favoriteToggle.second;

			mUserDb.toggleContactFavorite(contactInfo.getMsisdn(), favoriteType);

			if (favoriteType != FavoriteType.REQUEST_RECEIVED
					&& favoriteType != FavoriteType.REQUEST_SENT_REJECTED
					&& !HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type)) {
				String requestType;
				if (favoriteType == FavoriteType.FRIEND
						|| favoriteType == FavoriteType.REQUEST_SENT) {

					/*
					 * Adding a status message for accepting the friend request
					 */
					if (favoriteType == FavoriteType.FRIEND) {
						StatusMessage statusMessage = new StatusMessage(
								0,
								null,
								contactInfo.getMsisdn(),
								contactInfo.getName(),
								context.getString(R.string.accepted_friend_request),
								StatusMessageType.USER_ACCEPTED_FRIEND_REQUEST,
								System.currentTimeMillis() / 1000);
						mConversationDb.addStatusMessage(statusMessage, true);
						mPubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED,
								statusMessage);
						mPubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED,
								statusMessage);
					}

					requestType = HikeConstants.MqttMessageTypes.ADD_FAVORITE;
				} else if (HikePubSub.REJECT_FRIEND_REQUEST.equals(type)) {
					requestType = HikeConstants.MqttMessageTypes.POSTPONE_FAVORITE;
				} else {
					requestType = HikeConstants.MqttMessageTypes.REMOVE_FAVORITE;
				}

				mPubSub.publish(HikePubSub.MQTT_PUBLISH,
						serializeMsg(requestType, contactInfo.getMsisdn()));
			}
		} else if (HikePubSub.MUTE_CONVERSATION_TOGGLED.equals(type)) {
			Pair<String, Boolean> groupMute = (Pair<String, Boolean>) object;

			String id = groupMute.first;
			boolean mute = groupMute.second;

			mConversationDb.toggleGroupMute(id, mute);

			mPubSub.publish(
					HikePubSub.MQTT_PUBLISH,
					serializeMsg(mute ? HikeConstants.MqttMessageTypes.MUTE
							: HikeConstants.MqttMessageTypes.UNMUTE, id));
		} else if (HikePubSub.DELETE_STATUS.equals(type)) {
			String statusId = (String) object;
			mConversationDb.deleteStatus(statusId);
			/*
			 * If the status also has an icon, we delete that as well.
			 */
			mUserDb.removeIcon(statusId);
		} else if (HikePubSub.HIKE_JOIN_TIME_OBTAINED.equals(type)) {
			Pair<String, Long> msisdnHikeJoinTimePair = (Pair<String, Long>) object;

			String msisdn = msisdnHikeJoinTimePair.first;
			long hikeJoinTime = msisdnHikeJoinTimePair.second;

			mUserDb.setHikeJoinTime(msisdn, hikeJoinTime);
		} else if (HikePubSub.SEND_HIKE_SMS_FALLBACK.equals(type)) {
			List<ConvMessage> messages = (List<ConvMessage>) object;
			if (messages.isEmpty()) {
				return;
			}
			ConvMessage convMessage = messages.get(0);

			for (ConvMessage message : messages) {
				message.setSMS(true);
				mConversationDb.updateIsHikeMessageState(message.getMsgID(),
						false);
			}

			mPubSub.publish(HikePubSub.CHANGED_MESSAGE_TYPE, null);

			try {
				JSONObject jsonObject = new JSONObject();

				jsonObject.put(HikeConstants.TYPE,
						HikeConstants.MqttMessageTypes.FORCE_SMS);
				jsonObject.put(HikeConstants.TO, convMessage.getMsisdn());
				jsonObject.put(HikeConstants.COUNT, messages.size());
				jsonObject
						.put(HikeConstants.MESSAGE_ID, convMessage.getMsgID());

				JSONObject data = new JSONObject();

				JSONArray messagesArray = new JSONArray();

				JSONObject messageJSON = new JSONObject();

				messageJSON.put(HikeConstants.HIKE_MESSAGE,
						convMessage.getMessage());
				messageJSON.put(HikeConstants.ID, convMessage.getMsgID());

				messagesArray.put(messageJSON);

				data.put(HikeConstants.MESSAGE, messagesArray);

				jsonObject.put(HikeConstants.DATA, data);

				mPubSub.publish(HikePubSub.MQTT_PUBLISH, jsonObject);
			} catch (JSONException e) {
				Log.w(getClass().getSimpleName(), "Invalid json", e);
			}

		} else if (HikePubSub.SEND_NATIVE_SMS_FALLBACK.equals(type)) {
			List<ConvMessage> messages = (List<ConvMessage>) object;
			if (messages.isEmpty()) {
				return;
			}
			/*
			 * Reversing order since we want to send the oldest message first
			 */
			Collections.reverse(messages);

			for (ConvMessage convMessage : messages) {
				sendNativeSMS(convMessage);
				convMessage.setSMS(true);
				mConversationDb.updateIsHikeMessageState(
						convMessage.getMsgID(), false);
			}

			mPubSub.publish(HikePubSub.CHANGED_MESSAGE_TYPE, null);
		}
	}

	private void sendNativeSMS(ConvMessage convMessage) {
		SmsManager smsManager = SmsManager.getDefault();

		ArrayList<String> messages;
		/*
		 * Sending the file url for file transfer messages
		 */
		if (convMessage.isFileTransferMessage()) {
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			messages = smsManager
					.divideMessage(AccountUtils.fileTransferBaseViewUrl
							+ hikeFile.getFileKey());
		} else {
			messages = smsManager.divideMessage(convMessage.getMessage());
		}

		ArrayList<PendingIntent> pendingIntents = new ArrayList<PendingIntent>();

		for (int i = 0; i < messages.size(); i++) {
			Intent intent = new Intent(SMS_SENT_ACTION);
			intent.putExtra(HikeConstants.Extras.SMS_ID, convMessage.getMsgID());
			pendingIntents.add(PendingIntent
					.getBroadcast(context, 0, intent, 0));
		}

		smsManager.sendMultipartTextMessage(convMessage.getMsisdn(), null,
				messages, pendingIntents, null);

		writeToNativeSMSDb(convMessage);
	}

	/*
	 * Recording the event on fiksu if this was the first message of the day.
	 */
	private void uploadFiksuPerDayMessageEvent() {
		int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		if (today != dayRecorded) {
			dayRecorded = today;
			Editor editor = context.getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putInt(HikeMessengerApp.DAY_RECORDED, dayRecorded);
			editor.commit();
		}
	}

	private JSONObject serializeMsg(String type, String id) {
		JSONObject obj = new JSONObject();
		JSONObject data = new JSONObject();
		try {
			if (HikeConstants.MqttMessageTypes.ADD_FAVORITE.equals(type)) {
				obj.put(HikeConstants.TO, id);
			}
			obj.put(HikeConstants.TYPE, type);
			data.put(HikeConstants.ID, id);
			obj.put(HikeConstants.DATA, data);
		} catch (JSONException e) {
			Log.e(getClass().getSimpleName(), "Invalid json", e);
		}
		return obj;
	}

	private JSONObject blockUnblockSerialize(String type, String msisdn) {
		JSONObject obj = new JSONObject();
		try {
			obj.put(HikeConstants.TYPE, type);
			obj.put(HikeConstants.DATA, msisdn);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}

	private void updateDB(Object object, int status) {
		long msgID = (Long) object;
		/*
		 * TODO we should lookup the convid for this user, since otherwise one
		 * could set mess with the state for other conversations
		 */
		mConversationDb.updateMsgStatus(msgID, status, null);
	}

	private void writeToNativeSMSDb(ConvMessage convMessage) {

		ContentValues values = new ContentValues();
		values.put(HikeConstants.SMSNative.NUMBER, convMessage.getMsisdn());
		values.put(HikeConstants.SMSNative.DATE,
				convMessage.getTimestamp() * 1000);
		values.put(HikeConstants.SMSNative.MESSAGE, convMessage.getMessage());

		context.getContentResolver().insert(
				HikeConstants.SMSNative.SENTBOX_CONTENT_URI, values);
	}

	private BroadcastReceiver smsMessageStatusReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			long msgId = intent.getLongExtra(HikeConstants.Extras.SMS_ID, -1);
			switch (getResultCode()) {
			case Activity.RESULT_OK:
				mPubSub.publish(HikePubSub.SERVER_RECEIVED_MSG, msgId);

				if (msgId != -1) {
					persistence.removeMessage(msgId);
				}
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				break;
			case SmsManager.RESULT_ERROR_NO_SERVICE:
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				break;
			}
		}
	};
}
