package com.bsb.hike.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ClearTypingNotification;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Utils;
import com.facebook.android.Facebook;

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

	private Map<String, ClearTypingNotification> typingNotificationMap;

	private Handler clearTypingNotificationHandler;

	private static MqttMessagesManager instance;

	private MqttMessagesManager(Context context) {
		this.convDb = HikeConversationsDatabase.getInstance();
		this.userDb = HikeUserDatabase.getInstance();
		this.settings = context.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		this.context = context;
		this.pubSub = HikeMessengerApp.getPubSub();
		this.typingNotificationMap = HikeMessengerApp
				.getTypingNotificationSet();
		this.clearTypingNotificationHandler = new Handler();
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
			if (Utils.isGroupConversation(msisdn)) {
				return;
			}
			String iconBase64 = jsonObj.getString(HikeConstants.DATA);
			this.userDb.setIcon(msisdn,
					Base64.decode(iconBase64, Base64.DEFAULT), false);

			IconCacheManager.getInstance().clearIconForMSISDN(msisdn);
		} else if (HikeConstants.MqttMessageTypes.DISPLAY_PIC.equals(type)) {
			String groupId = jsonObj.getString(HikeConstants.TO);
			String iconBase64 = jsonObj.getString(HikeConstants.DATA);
			this.userDb.setIcon(groupId,
					Base64.decode(iconBase64, Base64.DEFAULT), false);

			IconCacheManager.getInstance().clearIconForMSISDN(groupId);
			saveStatusMsg(jsonObj, groupId);
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

			boolean stateChanged = false;
			stateChanged = ContactUtils.updateHikeStatus(this.context, msisdn,
					joined) > 0;

			stateChanged = this.convDb.updateOnHikeStatus(msisdn, joined) > 0;

			if (!stateChanged) {
				return;
			}

			if (joined) {
				long joinTime = jsonObj.optLong(HikeConstants.TIMESTAMP);
				if (joinTime > 0) {
					userDb.setHikeJoinTime(msisdn, joinTime);
				}

				saveStatusMsg(jsonObj, msisdn);

				List<String> groupConversations = convDb
						.listOfGroupConversationsWithMsisdn(msisdn);

				// For group chats
				for (String groupId : groupConversations) {
					saveStatusMsg(jsonObj, groupId);
				}
			} else {
				IconCacheManager.getInstance().deleteIconForMSISDN(msisdn);
			}

			/*
			 * Change the friend type since the user has now left hike
			 */
			if (!joined) {
				removeOrPostponeFriendType(msisdn);
			}

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

			boolean groupRevived = false;

			if (!this.convDb.isGroupAlive(groupConversation.getMsisdn())) {

				Log.d(getClass().getSimpleName(), "Group is not alive");
				int updated = this.convDb.toggleGroupDeadOrAlive(
						groupConversation.getMsisdn(), true);
				Log.d(getClass().getSimpleName(), "Group revived? " + updated);
				groupRevived = updated > 0;

				if (groupRevived) {
					jsonObj.put(HikeConstants.NEW_GROUP, true);
					pubSub.publish(HikePubSub.GROUP_REVIVED,
							groupConversation.getMsisdn());
				}

			}
			if (!groupRevived
					&& this.convDb.addGroupParticipants(
							groupConversation.getMsisdn(),
							groupConversation.getGroupParticipantList()) != HikeConstants.NEW_PARTICIPANT) {

				Log.d(getClass().getSimpleName(),
						"GCJ Message was already received");
				return;
			}
			Log.d(getClass().getSimpleName(), "GCJ Message is new");

			if (!groupRevived
					&& !this.convDb.doesConversationExist(groupConversation
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
				saveStatusMsg(jsonObj, groupId);
			}
		} else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_END.equals(type)) // Group
																				// chat
																				// end
		{
			String groupId = jsonObj.optString(HikeConstants.TO);
			if (this.convDb.toggleGroupDeadOrAlive(groupId, false) > 0) {
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

			if (convMessage.getMetadata() != null) {
				if (convMessage.getMetadata().isPokeMessage()) {
					Utils.vibrateNudgeReceived(context);
				}
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

			removeTypingNotification(convMessage.getMsisdn());
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
			int rowsUpdated = updateDB(msgID, ConvMessage.State.SENT_DELIVERED,
					msisdn);

			if (rowsUpdated == 0) {
				Log.d(getClass().getSimpleName(), "No rows updated");
				return;
			}

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

			long[] ids = convDb.setAllDeliveredMessagesReadForMsisdn(msisdn,
					msgIds);
			if (ids == null) {
				return;
			}

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
			addTypingNotification(msisdn);

		} else if (HikeConstants.MqttMessageTypes.END_TYPING.equals(type)) // End
																			// Typing
																			// event
																			// received
		{
			String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj
					.getString(HikeConstants.TO) : jsonObj
					.getString(HikeConstants.FROM);
			removeTypingNotification(msisdn);

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

			boolean inviteTokenAdded = false;
			boolean inviteeNumChanged = false;
			boolean toggleRewards = false;
			boolean talkTimeChanged = false;
			int newTalkTime = 0;

			Editor editor = settings.edit();
			if (data.has(HikeConstants.INVITE_TOKEN)) {
				editor.putString(HikeConstants.INVITE_TOKEN,
						data.getString(HikeConstants.INVITE_TOKEN));
				inviteTokenAdded = true;
			}
			if (data.has(HikeConstants.TOTAL_CREDITS_PER_MONTH)) {
				editor.putString(HikeConstants.TOTAL_CREDITS_PER_MONTH,
						data.getString(HikeConstants.TOTAL_CREDITS_PER_MONTH));
				inviteeNumChanged = true;
			}
			if (data.has(HikeConstants.ACCOUNT)) {
				JSONObject account = data.getJSONObject(HikeConstants.ACCOUNT);
				if (account.has(HikeConstants.ICON)) {
					String msisdn = settings.getString(
							HikeMessengerApp.MSISDN_SETTING, "");

					String iconBase64 = account.getString(HikeConstants.ICON);
					try {
						byte[] profileImageBytes = Base64.decode(iconBase64,
								Base64.DEFAULT);
						this.userDb.setIcon(msisdn, profileImageBytes, false);

						IconCacheManager.getInstance().clearIconForMSISDN(
								msisdn);
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.PROFILE_PIC_CHANGED, null);
					} catch (Exception e) {
						Log.w(getClass().getSimpleName(), "Invalid image bytes");
					}
				}
				if (account.has(HikeConstants.ACCOUNTS)) {
					JSONObject accounts = account
							.getJSONObject(HikeConstants.ACCOUNTS);
					if (accounts.has(HikeConstants.TWITTER)) {
						try {
							JSONObject twitter = accounts
									.getJSONObject(HikeConstants.TWITTER);
							String token = twitter.getString(HikeConstants.ID);
							String tokenSecret = twitter
									.getString(HikeConstants.TOKEN);
							HikeMessengerApp.makeTwitterInstance(token,
									tokenSecret);

							editor.putString(HikeMessengerApp.TWITTER_TOKEN,
									token);
							editor.putString(
									HikeMessengerApp.TWITTER_TOKEN_SECRET,
									tokenSecret);
							editor.putBoolean(
									HikeMessengerApp.TWITTER_AUTH_COMPLETE,
									true);
						} catch (JSONException e) {
							Log.w(getClass().getSimpleName(),
									"Unknown format for twitter", e);
						}
					}
					if (accounts.has(HikeConstants.FACEBOOK)) {
						try {
							JSONObject facebookJSON = accounts
									.getJSONObject(HikeConstants.FACEBOOK);
							String userId = facebookJSON
									.getString(HikeConstants.ID);
							String userToken = facebookJSON
									.getString(HikeConstants.TOKEN);
							long expires = facebookJSON
									.getLong(HikeConstants.EXPIRES);
							Facebook facebook = HikeMessengerApp.getFacebook();

							facebook.setAccessToken(userToken);
							facebook.setAccessExpires(expires);

							editor.putBoolean(
									HikeMessengerApp.FACEBOOK_AUTH_COMPLETE,
									true);
							editor.putLong(
									HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES,
									facebook.getAccessExpires());
							editor.putString(HikeMessengerApp.FACEBOOK_TOKEN,
									facebook.getAccessToken());
							editor.putString(HikeMessengerApp.FACEBOOK_USER_ID,
									userId);
						} catch (JSONException e) {
							Log.w(getClass().getSimpleName(),
									"Unknown format for facebook", e);
						}
					}
				}
				if (account.has(HikeConstants.MUTED)) {
					JSONObject mutedGroups = account
							.getJSONObject(HikeConstants.MUTED);
					JSONArray groupIds = mutedGroups.names();
					if (groupIds != null && groupIds.length() > 0) {
						for (int i = 0; i < groupIds.length(); i++) {
							HikeConversationsDatabase.getInstance()
									.toggleGroupMute(groupIds.optString(i),
											true);
						}
					}
				}
				if (account.has(HikeConstants.FAVORITES)) {
					JSONObject favorites = account
							.getJSONObject(HikeConstants.FAVORITES);

					if (favorites.length() > 0) {
						userDb.setMultipleContactsToFavorites(favorites);
					}
				}
				editor.putString(HikeMessengerApp.REWARDS_TOKEN,
						account.optString(HikeConstants.REWARDS_TOKEN));
				editor.putBoolean(HikeMessengerApp.SHOW_REWARDS,
						account.optBoolean(HikeConstants.SHOW_REWARDS));
				if (account.optBoolean(HikeConstants.SHOW_REWARDS)) {
					toggleRewards = true;
				}
				if (account.has(HikeConstants.REWARDS)) {
					JSONObject rewards = account
							.getJSONObject(HikeConstants.REWARDS);

					int talkTime = rewards.optInt(HikeConstants.TALK_TIME, -1);
					if (talkTime > -1) {
						editor.putInt(HikeMessengerApp.TALK_TIME, talkTime);
						talkTimeChanged = true;
						newTalkTime = talkTime;
					}
				}
			}
			editor.commit();
			if (inviteTokenAdded) {
				pubSub.publish(HikePubSub.INVITE_TOKEN_ADDED, null);
			}
			if (inviteeNumChanged) {
				pubSub.publish(HikePubSub.INVITEE_NUM_CHANGED, null);
			}
			if (toggleRewards) {
				pubSub.publish(HikePubSub.TOGGLE_REWARDS, null);
			}
			if (talkTimeChanged) {
				pubSub.publish(HikePubSub.TALK_TIME_CHANGED, newTalkTime);
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

			/*
			 * Ignore if contact is blocked.
			 */
			if (userDb.isBlocked(msisdn)) {
				return;
			}

			ContactInfo contactInfo = userDb.getContactInfoFromMSISDN(msisdn,
					false);
			if (contactInfo.getFavoriteType() == FavoriteType.FRIEND) {
				return;
			}
			FavoriteType currentType = contactInfo.getFavoriteType();
			FavoriteType favoriteType = (currentType == FavoriteType.NOT_FRIEND
					|| currentType == FavoriteType.REQUEST_RECEIVED_REJECTED || currentType == FavoriteType.REQUEST_RECEIVED) ? FavoriteType.REQUEST_RECEIVED
					: FavoriteType.FRIEND;
			Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(
					contactInfo, favoriteType);
			this.pubSub
					.publish(
							favoriteType == FavoriteType.REQUEST_RECEIVED ? HikePubSub.FAVORITE_TOGGLED
									: HikePubSub.FRIEND_REQUEST_ACCEPTED,
							favoriteToggle);
			if (favoriteType == FavoriteType.FRIEND) {
				StatusMessage statusMessage = new StatusMessage(0, null,
						msisdn, contactInfo.getName(),
						context.getString(R.string.confirmed_friend),
						StatusMessageType.FRIEND_REQUEST_ACCEPTED,
						System.currentTimeMillis() / 1000);

				convDb.addStatusMessage(statusMessage, true);

				pubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED,
						statusMessage);
				pubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED,
						statusMessage);

				incrementUnseenStatusCount();
			}
		} else if (HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG.equals(type)) {
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

			Editor editor = settings.edit();

			if (data.has(HikeConstants.REWARDS_TOKEN)) {
				String rewardToken = data
						.getString(HikeConstants.REWARDS_TOKEN);
				editor.putString(HikeMessengerApp.REWARDS_TOKEN, rewardToken);
			}

			if (data.has(HikeConstants.SHOW_REWARDS)) {
				boolean showRewards = data
						.getBoolean(HikeConstants.SHOW_REWARDS);
				editor.putBoolean(HikeMessengerApp.SHOW_REWARDS, showRewards);
			}

			if (data.has(HikeConstants.ENABLE_PUSH_BATCHING_STATUS_NOTIFICATIONS)) {
				JSONArray array = data
						.getJSONArray(HikeConstants.ENABLE_PUSH_BATCHING_STATUS_NOTIFICATIONS);
				editor.putString(
						HikeMessengerApp.BATCH_STATUS_NOTIFICATION_VALUES,
						array.toString());
			}

			if (data.has(HikeConstants.CRICKET_MOODS)) {
				editor.putBoolean(HikeMessengerApp.SHOW_CRICKET_MOODS,
						data.optBoolean(HikeConstants.CRICKET_MOODS, true));
			}

			editor.commit();
			if (data.has(HikeConstants.CRICKET_MOODS)) {
				((HikeMessengerApp) context.getApplicationContext())
						.setMoodsResource();
			}

			this.pubSub.publish(HikePubSub.TOGGLE_REWARDS, null);
		} else if (HikeConstants.MqttMessageTypes.REWARDS.equals(type)) {
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

			int talkTime = data.getInt(HikeConstants.TALK_TIME);

			Editor editor = settings.edit();
			editor.putInt(HikeMessengerApp.TALK_TIME, talkTime);
			editor.commit();

			this.pubSub.publish(HikePubSub.TALK_TIME_CHANGED, talkTime);
		} else if (HikeConstants.MqttMessageTypes.ACTION.equals(type)) {
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
			if (data.optBoolean(HikeConstants.POST_AB)) {
				String token = settings.getString(
						HikeMessengerApp.TOKEN_SETTING, null);
				List<ContactInfo> contactinfos = ContactUtils
						.getContacts(this.context);
				Map<String, List<ContactInfo>> contacts = ContactUtils
						.convertToMap(contactinfos);
				try {
					AccountUtils.postAddressBook(token, contacts);
				} catch (IllegalStateException e) {
					Log.w(getClass().getSimpleName(),
							"Exception while posting ab", e);
				} catch (IOException e) {
					Log.w(getClass().getSimpleName(),
							"Exception while posting ab", e);
				}
			} else if (data.optBoolean(HikeConstants.PUSH)) {
				Editor editor = settings.edit();
				editor.putBoolean(HikeMessengerApp.GCM_ID_SENT, false);
				editor.commit();
				context.sendBroadcast(new Intent(
						HikeService.SEND_TO_SERVER_ACTION));
			}
		} else if (HikeConstants.MqttMessageTypes.STATUS_UPDATE.equals(type)) {
			StatusMessage statusMessage = new StatusMessage(jsonObj);
			/*
			 * This would be true for unsupported status message types. We
			 * should not be doing anything if we get one.
			 * 
			 * Also if the user is blocked, we ignore the message.
			 */
			if (statusMessage.getStatusMessageType() == null
					|| userDb.isBlocked(statusMessage.getMsisdn())) {
				return;
			}

			ContactInfo contactInfo = userDb.getContactInfoFromMSISDN(
					statusMessage.getMsisdn(), false);
			FavoriteType favoriteType = contactInfo.getFavoriteType();
			/*
			 * Only add updates to timeline for contacts that have a 2-way
			 * relationship with the user.
			 */
			long id = convDb.addStatusMessage(statusMessage,
					favoriteType == FavoriteType.FRIEND);

			if (id == -1) {
				Log.d(getClass().getSimpleName(),
						"This status message was already added");
				return;
			}

			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC) {
				String iconBase64 = jsonObj.getJSONObject(HikeConstants.DATA)
						.getString(HikeConstants.THUMBNAIL);
				this.userDb.setIcon(statusMessage.getMappedId(),
						Base64.decode(iconBase64, Base64.DEFAULT), false);
				/*
				 * Removing the thumbnail string from the JSON, since we've
				 * already saved it.
				 */
				jsonObj.getJSONObject(HikeConstants.DATA).remove(
						HikeConstants.THUMBNAIL);
			}

			statusMessage
					.setName(TextUtils.isEmpty(contactInfo.getName()) ? contactInfo
							.getMsisdn() : contactInfo.getName());

			if (favoriteType == FavoriteType.FRIEND) {
				incrementUnseenStatusCount();
				pubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED,
						statusMessage);
			}
			pubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);

			String msisdn = jsonObj.getString(HikeConstants.FROM);
			ConvMessage convMessage = saveStatusMsg(jsonObj, msisdn);

			if (convMessage == null) {
				return;
			}

			convDb.setMessageIdForStatus(statusMessage.getMappedId(),
					convMessage.getMsgID());
		} else if (HikeConstants.MqttMessageTypes.DELETE_STATUS.equals(type)) {
			String statusId = jsonObj.getJSONObject(HikeConstants.DATA)
					.getString(HikeConstants.STATUS_ID);
			pubSub.publish(HikePubSub.DELETE_STATUS, statusId);
		} else if (HikeConstants.MqttMessageTypes.POSTPONE_FAVORITE
				.equals(type)
				|| HikeConstants.MqttMessageTypes.REMOVE_FAVORITE.equals(type)) {
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			removeOrPostponeFriendType(msisdn);
		} else if (HikeConstants.MqttMessageTypes.BATCH_STATUS_UPDATE
				.equals(type)) {
			/*
			 * Only proceed if the user has selected a batch update preference
			 */
			if (PreferenceManager.getDefaultSharedPreferences(context).getInt(
					HikeConstants.STATUS_PREF, 0) <= 0) {
				return;
			}

			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

			String header = data.getString(HikeConstants.BATCH_HEADER);
			String message = data.getString(HikeConstants.BATCH_MESSAGE);

			pubSub.publish(HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED,
					new Pair<String, String>(header, message));
		}
	}

	private void removeOrPostponeFriendType(String msisdn) {
		ContactInfo contactInfo = userDb
				.getContactInfoFromMSISDN(msisdn, false);
		if (contactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND) {
			return;
		}
		FavoriteType currentFavoriteType = contactInfo.getFavoriteType();
		FavoriteType favoriteType = (currentFavoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED || currentFavoriteType == FavoriteType.REQUEST_RECEIVED) ? FavoriteType.NOT_FRIEND
				: FavoriteType.REQUEST_SENT_REJECTED;
		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, ContactInfo.FavoriteType>(
				contactInfo, favoriteType);
		this.pubSub.publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
	}

	private void incrementUnseenStatusCount() {
		int count = settings.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
		count++;
		Editor editor = settings.edit();
		editor.putInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, count);
		editor.commit();
	}

	private void updateDbBatch(long[] ids, ConvMessage.State status,
			String msisdn) {
		convDb.updateBatch(ids, status.ordinal(), msisdn);
	}

	private int updateDB(Object object, ConvMessage.State status, String msisdn) {
		long msgID = (Long) object;
		/*
		 * TODO we should lookup the convid for this user, since otherwise one
		 * could set mess with the state for other conversations
		 */
		return convDb.updateMsgStatus(msgID, status.ordinal(), msisdn);
	}

	private ConvMessage saveStatusMsg(JSONObject jsonObj, String msisdn)
			throws JSONException {
		Conversation conversation = convDb
				.getConversationWithLastMessage(msisdn);

		boolean isUJMsg = HikeConstants.MqttMessageTypes.USER_JOINED
				.equals(jsonObj.getString(HikeConstants.TYPE));
		boolean isGettingCredits = false;
		if (isUJMsg) {
			isGettingCredits = jsonObj.getJSONObject(HikeConstants.DATA)
					.optInt(HikeConstants.CREDITS, -1) > 0;
		}
		/*
		 * If the message is of type 'uj' we want to show it for all known
		 * contacts regardless of if the user currently has an existing
		 * conversation. We also want to show the 'uj' message in all the group
		 * chats with that participant. Otherwise for other types, we only show
		 * the message if the user already has an existing conversation.
		 */
		if ((conversation == null && (!isUJMsg || !userDb
				.doesContactExist(msisdn)))
				|| (conversation != null
						&& TextUtils.isEmpty(conversation.getContactName())
						&& isUJMsg && !isGettingCredits && !(conversation instanceof GroupConversation))) {
			return null;
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
		return convMessage;
	}

	private void addTypingNotification(String msisdn) {
		this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, msisdn);

		ClearTypingNotification clearTypingNotification;
		if (!typingNotificationMap.containsKey(msisdn)) {
			clearTypingNotification = new ClearTypingNotification(msisdn);
			typingNotificationMap.put(msisdn, clearTypingNotification);
		} else {
			clearTypingNotification = typingNotificationMap.get(msisdn);
			clearTypingNotificationHandler
					.removeCallbacks(clearTypingNotification);
		}
		clearTypingNotificationHandler.postDelayed(clearTypingNotification,
				HikeConstants.LOCAL_CLEAR_TYPING_TIME);
	}

	private void removeTypingNotification(String msisdn) {
		this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, msisdn);

		ClearTypingNotification clearTypingNotification = typingNotificationMap
				.remove(msisdn);
		if (clearTypingNotification != null) {
			clearTypingNotificationHandler
					.removeCallbacks(clearTypingNotification);
		}
	}
}