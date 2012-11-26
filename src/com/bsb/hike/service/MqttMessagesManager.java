package com.bsb.hike.service;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author Rishabh This class is used for saving all the mqtt messages in the db
 *         based on their types. Its also used to publish these events for the
 *         UI to make the changes, wherever applicable. This class should be a
 *         singleton, since only one instance should be used managing these
 *         messages
 */
public class MqttMessagesManager {

	private HikeConversationsDatabase convDb;

	private HikeUserDatabase userDb;

	private SharedPreferences settings;

	private Context context;

	private HikePubSub pubSub;

	private static MqttMessagesManager instance;

	private MqttMessagesManager(Context context) {
		this.convDb = HikeConversationsDatabase.getInstance();
		this.userDb = HikeUserDatabase.getInstance();
		this.settings = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		this.context = context;
		this.pubSub = HikeMessengerApp.getPubSub();
	}

	public static MqttMessagesManager getInstance(Context context) {
		if (instance == null) {
			synchronized (MqttMessagesManager.class) {
				if (instance == null) {
					instance = new MqttMessagesManager(context);
				}
			}
		}
		return instance;
	}

	public void close() {
		instance = null;
	}

	public void saveMqttMessage(JSONObject jsonObj) throws JSONException {
		String type = jsonObj.optString(HikeConstants.TYPE);
		if (HikeConstants.MqttMessageTypes.ICON.equals(type)) // Icon changed
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			String iconBase64 = jsonObj.getString(HikeConstants.DATA);
			this.userDb.setIcon(msisdn,
					Base64.decode(iconBase64, Base64.DEFAULT), false);

			IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		} else if (HikeConstants.MqttMessageTypes.SMS_CREDITS.equals(type)) // Credits
																			// changed
		{
			Integer credits = jsonObj.optInt(HikeConstants.DATA);
			if (settings.getInt(HikeMessengerApp.SMS_SETTING, 0) == 0) {
				if (credits > 0) {
					convDb.setOverlay(false, null);
				}
			}
			Editor mEditor = settings.edit();
			mEditor.putInt(HikeMessengerApp.SMS_SETTING, credits.intValue());
			mEditor.commit();
			this.pubSub.publish(HikePubSub.SMS_CREDIT_CHANGED, credits);
		} else if ((HikeConstants.MqttMessageTypes.USER_JOINED.equals(type))
				|| (HikeConstants.MqttMessageTypes.USER_LEFT.equals(type))) // User
																			// joined/left
		{
			String msisdn = jsonObj.getJSONObject(HikeConstants.DATA)
					.getString(HikeConstants.MSISDN);
			boolean joined = HikeConstants.MqttMessageTypes.USER_JOINED
					.equals(type);
			if (joined) {
				saveStatusMsg(jsonObj, msisdn);

				List<String> groupConversations = convDb
						.listOfGroupConversationsWithMsisdn(msisdn);

				// For group chats
				for (String groupId : groupConversations) {
					saveStatusMsg(jsonObj, groupId);
				}
			}
			ContactUtils.updateHikeStatus(this.context, msisdn, joined);
			this.convDb.updateOnHikeStatus(msisdn, joined);

			this.pubSub.publish(joined ? HikePubSub.USER_JOINED
					: HikePubSub.USER_LEFT, msisdn);
		} else if (HikeConstants.MqttMessageTypes.INVITE_INFO.equals(type)) // Invite
																			// info
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			int invited = data.optInt(HikeConstants.ALL_INVITEE);
			int invited_joined = data.optInt(HikeConstants.ALL_INVITEE_JOINED);
			String totalCreditsPerMonth = data
					.optString(HikeConstants.TOTAL_CREDITS_PER_MONTH);
			Editor editor = settings.edit();
			editor.putInt(HikeMessengerApp.INVITED, invited);
			editor.putInt(HikeMessengerApp.INVITED_JOINED, invited_joined);
			if (!TextUtils.isEmpty(totalCreditsPerMonth)
					&& Integer.parseInt(totalCreditsPerMonth) > 0) {
				editor.putString(HikeMessengerApp.TOTAL_CREDITS_PER_MONTH,
						totalCreditsPerMonth);
			}
			editor.commit();
			this.pubSub.publish(HikePubSub.INVITEE_NUM_CHANGED, null);
		} else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN.equals(type)) // Group
																				// chat
																				// join
		{
			if (jsonObj.getJSONArray(HikeConstants.DATA).length() == 0) {
				return;
			}
			GroupConversation groupConversation = new GroupConversation(
					jsonObj, this.context);

			if (this.convDb.addGroupParticipants(groupConversation.getMsisdn(),
					groupConversation.getGroupParticipantList()) != HikeConstants.NEW_PARTICIPANT) {
				Log.d(getClass().getSimpleName(),
						"GCJ Message was already received");
				return;
			}
			Log.d(getClass().getSimpleName(), "GCJ Message is new");

			if (!this.convDb.doesConversationExist(groupConversation
					.getMsisdn())) {
				Log.d(getClass().getSimpleName(),
						"The group conversation does not exists");
				groupConversation = (GroupConversation) this.convDb
						.addConversation(groupConversation.getMsisdn(), false,
								"", groupConversation.getGroupOwner());

				// Adding a key to the json signify that this was the GCJ
				// received for group creation
				jsonObj.put(HikeConstants.NEW_GROUP, true);
			}
			saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
		} else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE.equals(type)) // Group
																					// chat
																					// leave
		{
			String groupId = jsonObj.optString(HikeConstants.TO);
			String msisdn = jsonObj.optString(HikeConstants.DATA);
			if (this.convDb.setParticipantLeft(groupId, msisdn) > 0) {
				saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
			}
		} else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_NAME.equals(type)) // Group
																				// chat
																				// name
																				// change
		{
			String groupname = jsonObj.optString(HikeConstants.DATA);
			String groupId = jsonObj.optString(HikeConstants.TO);

			if (this.convDb.setGroupName(groupId, groupname) > 0) {
				this.pubSub.publish(HikePubSub.GROUP_NAME_CHANGED, groupId);
			}
		} else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_END.equals(type)) // Group
																				// chat
																				// end
		{
			String groupId = jsonObj.optString(HikeConstants.TO);
			if (this.convDb.setGroupDead(groupId) > 0) {
				saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
			}
		} else if (HikeConstants.MqttMessageTypes.MESSAGE.equals(type)) // Message
																		// received
																		// from
																		// server
		{
			Log.d(getClass().getSimpleName(), "Checking if message exists");
			ConvMessage convMessage = new ConvMessage(jsonObj);
			if (this.convDb.wasMessageReceived(convMessage)) // Check if message
																// was already
																// received by
																// the receiver
			{
				Log.d(getClass().getSimpleName(), "Message already exists");
				return;
			}

			convDb.addConversationMessages(convMessage);

			/*
			 * Return if there is no conversation mapped to this message
			 */
			if (convMessage.getConversation() == null) {
				return;
			}
			/*
			 * We are forcing the app to set all the messages sent by the user
			 * in a group chat before this as read.
			 */
			long[] ids = convDb.getUnreadMessageIds(convMessage
					.getConversation().getConvId());
			if (ids != null && convMessage.isGroupChat()
					&& convMessage.getMetadata() == null) {
				updateDbBatch(ids, ConvMessage.State.SENT_DELIVERED_READ,
						convMessage.getMsisdn());

				Pair<String, long[]> pair = new Pair<String, long[]>(
						convMessage.getMsisdn(), ids);
				this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED_READ, pair);
			}

			/*
			 * We need to add the name here in order to fix the bug where if the
			 * client receives two files of the same name, it shows the same
			 * file under both files.
			 */
			if (convMessage.isFileTransferMessage()) {
				HikeFile hikeFile = convMessage.getMetadata().getHikeFiles()
						.get(0);
				Log.d(getClass().getSimpleName(),
						"FT MESSAGE: " + " NAME: " + hikeFile.getFileName()
								+ " KEY: " + hikeFile.getFileKey());
				Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());
			}

			Log.d(getClass().getSimpleName(), "Receiver received Message : "
					+ convMessage.getMessage() + "		;	Receiver Msg ID : "
					+ convMessage.getMsgID() + "	; Mapped msgID : "
					+ convMessage.getMappedMsgID());
			// We have to do publish this here since we are adding the message
			// to the db here, and the id is set after inserting into the db.
			this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);

			if (convMessage.isGroupChat()
					&& convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO) {
				this.pubSub.publish(HikePubSub.SHOW_PARTICIPANT_STATUS_MESSAGE,
						convMessage.getMsisdn());
			}
		} else if (HikeConstants.MqttMessageTypes.DELIVERY_REPORT.equals(type)) // Message
																				// delivered
																				// to
																				// receiver
		{
			String id = jsonObj.optString(HikeConstants.DATA);
			String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj
					.getString(HikeConstants.TO) : jsonObj
					.getString(HikeConstants.FROM);
			long msgID;
			try {
				msgID = Long.parseLong(id);
			} catch (NumberFormatException e) {
				Log.e(getClass().getSimpleName(),
						"Exception occured while parsing msgId. Exception : "
								+ e);
				msgID = -1;
			}
			Log.d(getClass().getSimpleName(),
					"Delivery report received for msgid : " + msgID
							+ "	;	REPORT : DELIVERED");
			updateDB(msgID, ConvMessage.State.SENT_DELIVERED, msisdn);

			Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgID);

			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED, pair);
		} else if (HikeConstants.MqttMessageTypes.MESSAGE_READ.equals(type)) // Message
																				// has
																				// been
																				// read
		{
			JSONArray msgIds = jsonObj.optJSONArray(HikeConstants.DATA);
			String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj
					.getString(HikeConstants.TO) : jsonObj
					.getString(HikeConstants.FROM);
			if (msgIds == null) {
				Log.e(getClass().getSimpleName(),
						"Update Error : Message id Array is empty or null . Check problem");
				return;
			}

			long[] ids = new long[msgIds.length()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = msgIds.optLong(i);
			}
			Log.d(getClass().getSimpleName(), "Delivery report received : "
					+ "	;	REPORT : DELIVERED READ");
			updateDbBatch(ids, ConvMessage.State.SENT_DELIVERED_READ, msisdn);

			Pair<String, long[]> pair = new Pair<String, long[]>(msisdn, ids);

			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED_READ, pair);
		} else if (HikeConstants.MqttMessageTypes.START_TYPING.equals(type)) // Start
																				// Typing
																				// event
																				// received
		{
			String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj
					.getString(HikeConstants.TO) : jsonObj
					.getString(HikeConstants.FROM);
			this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, msisdn);
		} else if (HikeConstants.MqttMessageTypes.END_TYPING.equals(type)) // End
																			// Typing
																			// event
																			// received
		{
			String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj
					.getString(HikeConstants.TO) : jsonObj
					.getString(HikeConstants.FROM);
			this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, msisdn);
		} else if (HikeConstants.MqttMessageTypes.UPDATE_AVAILABLE.equals(type)) {
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			String version = data.optString(HikeConstants.VERSION);
			Editor editor = settings.edit();
			int update = Utils.isUpdateRequired(version, context) ? (data
					.optBoolean(HikeConstants.CRITICAL) ? HikeConstants.CRITICAL_UPDATE
					: HikeConstants.NORMAL_UPDATE)
					: HikeConstants.NO_UPDATE;
			editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, update);
			editor.putString(HikeConstants.Extras.UPDATE_MESSAGE,
					data.optString(HikeConstants.MqttMessageTypes.MESSAGE));
			editor.commit();
			if (update != HikeConstants.NO_UPDATE) {
				this.pubSub.publish(HikePubSub.UPDATE_AVAILABLE, update);
			}
		} else if (HikeConstants.MqttMessageTypes.ACCOUNT_INFO.equals(type)) {
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
			JSONArray keys = data.names();
			Editor editor = settings.edit();
			for (int i = 0; i < keys.length(); i++) {
				String key = keys.getString(i);
				String value = data.optString(key);
				editor.putString(key, value);
			}
			editor.commit();
			if (data.has(HikeConstants.INVITE_TOKEN)) {
				this.pubSub.publish(HikePubSub.INVITE_TOKEN_ADDED, null);
			}
			if (data.has(HikeConstants.TOTAL_CREDITS_PER_MONTH)) {
				this.pubSub.publish(HikePubSub.INVITEE_NUM_CHANGED, null);
			}
		} else if (HikeConstants.MqttMessageTypes.USER_OPT_IN.equals(type)) {
			String msisdn = jsonObj.getJSONObject(HikeConstants.DATA)
					.getString(HikeConstants.MSISDN);

			// For one-to-one chat
			saveStatusMsg(jsonObj, msisdn);

			List<String> groupConversations = convDb
					.listOfGroupConversationsWithMsisdn(msisdn);

			// Set the dnd status for the participant for all group chats
			convDb.updateDndStatus(msisdn);
			// For group chats
			for (String groupId : groupConversations) {
				saveStatusMsg(jsonObj, groupId);
			}
		} else if (HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS
				.equals(type)) {
			String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj
					.getString(HikeConstants.TO) : jsonObj
					.getString(HikeConstants.FROM);
			saveStatusMsg(jsonObj, msisdn);
		} else if (HikeConstants.MqttMessageTypes.ADD_FAVORITE.equals(type)) {
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			ContactInfo contactInfo = userDb.getContactInfoFromMSISDN(msisdn,
					true);
			if (contactInfo == null) {
				return;
			}
			Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(
					contactInfo, FavoriteType.RECOMMENDED_FAVORITE);
			this.pubSub.publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
		}
	}

	private void updateDbBatch(long[] ids, ConvMessage.State status,
			String msisdn) {
		convDb.updateBatch(ids, status.ordinal(), msisdn);
	}

	private void updateDB(Object object, ConvMessage.State status, String msisdn) {
		long msgID = (Long) object;
		/*
		 * TODO we should lookup the convid for this user, since otherwise one
		 * could set mess with the state for other conversations
		 */
		convDb.updateMsgStatus(msgID, status.ordinal(), msisdn);
	}

	private void saveStatusMsg(JSONObject jsonObj, String msisdn)
			throws JSONException {
		Conversation conversation = convDb.getConversation(msisdn, 0);

		/*
		 * If the message is of type 'uj' we want to show it for all known
		 * contacts regardless of if the user currently has an existing
		 * conversation. We also want to show the 'uj' message in all the group
		 * chats with that participant. Otherwise for other types, we only show
		 * the message if the user already has an existing conversation.
		 */
		if ((conversation == null && !HikeConstants.MqttMessageTypes.USER_JOINED
				.equals(jsonObj.getString(HikeConstants.TYPE)))
				|| (conversation != null
						&& TextUtils.isEmpty(conversation.getContactName())
						&& HikeConstants.MqttMessageTypes.USER_JOINED
								.equals(jsonObj.getString(HikeConstants.TYPE)) && !(conversation instanceof GroupConversation))) {
			return;
		}
		ConvMessage convMessage = new ConvMessage(jsonObj, conversation,
				context, false);
		convDb.addConversationMessages(convMessage);

		this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED
				|| convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT
				|| convMessage.getParticipantInfoState() == ParticipantInfoState.GROUP_END) {
			this.pubSub
					.publish(
							convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED ? HikePubSub.PARTICIPANT_JOINED_GROUP
									: convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT ? HikePubSub.PARTICIPANT_LEFT_GROUP
											: HikePubSub.GROUP_END, jsonObj);
		}
	}
}