package com.bsb.hike.db;

import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.GroupParticipant;
import com.fiksu.asotracking.FiksuTrackingManager;

public class DbConversationListener implements Listener {
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
				mPubSub.publish(HikePubSub.MQTT_PUBLISH,
						convMessage.serialize());
				if (convMessage.isGroupChat()) {
					mPubSub.publish(HikePubSub.SHOW_PARTICIPANT_STATUS_MESSAGE,
							convMessage.getMsisdn());
				}
			}
		} else if (HikePubSub.DELETE_MESSAGE.equals(type)) {
			ConvMessage message = ((ConvMessage) object);
			mConversationDb.deleteMessage(message);
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
			JSONObject blockObj = blockUnblockSerialize("b", msisdn);
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
								context.getString(R.string.user_added_contact_as_friend),
								StatusMessageType.FRIEND_REQUEST_ACCEPTED,
								System.currentTimeMillis() / 1000);
						mConversationDb.addStatusMessage(statusMessage, true);
						mPubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED,
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
		} else if (HikePubSub.HIKE_JOIN_TIME_OBTAINED.equals(type)) {
			Pair<String, Long> msisdnHikeJoinTimePair = (Pair<String, Long>) object;

			String msisdn = msisdnHikeJoinTimePair.first;
			long hikeJoinTime = msisdnHikeJoinTimePair.second;

			mUserDb.setHikeJoinTime(msisdn, hikeJoinTime);
		}
	}

	/*
	 * Recording the event on fiksu if this was the first message of the day.
	 */
	private void uploadFiksuPerDayMessageEvent() {
		int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		if (today != dayRecorded) {
			FiksuTrackingManager.uploadPurchaseEvent(context,
					HikeConstants.FIRST_MESSAGE,
					HikeConstants.FIRST_MSG_IN_DAY, HikeConstants.CURRENCY);
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
}
