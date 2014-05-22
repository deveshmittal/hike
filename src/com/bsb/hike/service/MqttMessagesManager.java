package com.bsb.hike.service;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.filetransfer.FileTransferManager.NetworkType;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.tasks.DownloadProfileImageTask;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.ClearGroupTypingNotification;
import com.bsb.hike.utils.ClearTypingNotification;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author Rishabh This class is used for saving all the mqtt messages in the db based on their types. Its also used to publish these events for the UI to make the changes,
 *         wherever applicable. This class should be a singleton, since only one instance should be used managing these messages
 */
public class MqttMessagesManager
{

	private HikeConversationsDatabase convDb;

	private HikeUserDatabase userDb;

	private SharedPreferences settings;

	private SharedPreferences appPrefs;

	private Context context;

	private HikePubSub pubSub;

	private Map<String, TypingNotification> typingNotificationMap;

	private Handler clearTypingNotificationHandler;

	private static MqttMessagesManager instance;

	private String userMsisdn;

	private MqttMessagesManager(Context context)
	{
		this.convDb = HikeConversationsDatabase.getInstance();
		this.userDb = HikeUserDatabase.getInstance();
		this.settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		this.context = context;
		this.pubSub = HikeMessengerApp.getPubSub();
		this.typingNotificationMap = HikeMessengerApp.getTypingNotificationSet();
		this.clearTypingNotificationHandler = new Handler();
		this.appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.userMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, "");
	}

	public static MqttMessagesManager getInstance(Context context)
	{
		if (instance == null)
		{
			synchronized (MqttMessagesManager.class)
			{
				if (instance == null)
				{
					instance = new MqttMessagesManager(context);
				}
			}
		}
		return instance;
	}

	public void close()
	{
		instance = null;
	}

	public void saveMqttMessage(JSONObject jsonObj) throws JSONException
	{
		String type = jsonObj.optString(HikeConstants.TYPE);
		if (HikeConstants.MqttMessageTypes.ICON.equals(type)) // Icon changed
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			/*
			 * We don't consider this packet if the msisdn is the user's msisdn or a group conversation.
			 */
			if (Utils.isGroupConversation(msisdn) || userMsisdn.equals(msisdn))
			{
				return;
			}
			String iconBase64 = jsonObj.getString(HikeConstants.DATA);
			this.userDb.setIcon(msisdn, Base64.decode(iconBase64, Base64.DEFAULT), false);

			HikeMessengerApp.getLruCache().clearIconForMSISDN(msisdn);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
			// IconCacheManager.getInstance().clearIconForMSISDN(msisdn);

			/*
			 * Only auto download if the ic packet is not generated due to signup.
			 */
			if (!HikeConstants.SIGNUP_IC.equals(jsonObj.optString(HikeConstants.SUB_TYPE)))
			{
				autoDownloadGroupImage(msisdn);
			}
		}
		else if (HikeConstants.MqttMessageTypes.DISPLAY_PIC.equals(type))
		{
			String groupId = jsonObj.getString(HikeConstants.TO);
			String iconBase64 = jsonObj.getString(HikeConstants.DATA);
			String newIconIdentifier = null;

			if (iconBase64.length() < 6)
			{
				newIconIdentifier = iconBase64;
			}
			else
			{
				newIconIdentifier = iconBase64.substring(0, 5) + iconBase64.substring(iconBase64.length() - 6);
			}

			String oldIconIdentifier = this.userDb.getIconIdentifierString(groupId);

			/*
			 * Same Icon
			 */
			if (newIconIdentifier.equals(oldIconIdentifier))
			{
				return;
			}

			this.userDb.setIcon(groupId, Base64.decode(iconBase64, Base64.DEFAULT), false);

			HikeMessengerApp.getLruCache().clearIconForMSISDN(groupId);
			HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, groupId);

			// IconCacheManager.getInstance().clearIconForMSISDN(groupId);
			autoDownloadGroupImage(groupId);
			saveStatusMsg(jsonObj, groupId);
		}
		else if (HikeConstants.MqttMessageTypes.SMS_CREDITS.equals(type)) // Credits
		// changed
		{
			Integer credits = jsonObj.optInt(HikeConstants.DATA);
			if (settings.getInt(HikeMessengerApp.SMS_SETTING, 0) == 0)
			{
				if (credits > 0)
				{
					convDb.setOverlay(false, null);
				}
			}
			Editor mEditor = settings.edit();
			mEditor.putInt(HikeMessengerApp.SMS_SETTING, credits.intValue());
			mEditor.commit();
			this.pubSub.publish(HikePubSub.SMS_CREDIT_CHANGED, credits);
		}
		else if ((HikeConstants.MqttMessageTypes.USER_JOINED.equals(type)) || (HikeConstants.MqttMessageTypes.USER_LEFT.equals(type))) // User
		// joined/left
		{
			String msisdn = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);
			boolean joined = HikeConstants.MqttMessageTypes.USER_JOINED.equals(type);

			boolean stateChanged = false;

			int rowsChanged = ContactUtils.updateHikeStatus(this.context, msisdn, joined);
			rowsChanged += this.convDb.updateOnHikeStatus(msisdn, joined);

			/*
			 * If at least one row has been updated, that means that the user has changed his/her hike state
			 */
			if (rowsChanged > 0)
			{
				stateChanged = true;
			}

			if (!stateChanged)
			{
				return;
			}

			if (joined)
			{
				long joinTime = jsonObj.optLong(HikeConstants.TIMESTAMP);
				if (joinTime > 0)
				{
					joinTime = Utils.applyServerTimeOffset(context, joinTime);
					userDb.setHikeJoinTime(msisdn, joinTime);
				}

				saveStatusMsg(jsonObj, msisdn);

				List<String> groupConversations = convDb.listOfGroupConversationsWithMsisdn(msisdn);

				// For group chats
				for (String groupId : groupConversations)
				{
					saveStatusMsg(jsonObj, groupId);
				}
			}
			else
			{
				HikeMessengerApp.getLruCache().deleteIconForMSISDN(msisdn);
				HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
				// IconCacheManager.getInstance().deleteIconForMSISDN(msisdn);
			}

			/*
			 * Change the friend type since the user has now left hike and delete this contact's status messages.
			 */
			if (!joined)
			{
				convDb.deleteStatusMessagesForMsisdn(msisdn);
				removeOrPostponeFriendType(msisdn);
			}

			this.pubSub.publish(joined ? HikePubSub.USER_JOINED : HikePubSub.USER_LEFT, msisdn);
		}
		else if (HikeConstants.MqttMessageTypes.INVITE_INFO.equals(type)) // Invite
		// info
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			int invited = data.optInt(HikeConstants.ALL_INVITEE);
			int invited_joined = data.optInt(HikeConstants.ALL_INVITEE_JOINED);
			String totalCreditsPerMonth = data.optString(HikeConstants.TOTAL_CREDITS_PER_MONTH);
			Editor editor = settings.edit();
			editor.putInt(HikeMessengerApp.INVITED, invited);
			editor.putInt(HikeMessengerApp.INVITED_JOINED, invited_joined);
			if (!TextUtils.isEmpty(totalCreditsPerMonth) && Integer.parseInt(totalCreditsPerMonth) > 0)
			{
				editor.putString(HikeMessengerApp.TOTAL_CREDITS_PER_MONTH, totalCreditsPerMonth);
			}
			editor.commit();
			this.pubSub.publish(HikePubSub.INVITEE_NUM_CHANGED, null);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN.equals(type)) // Group
		// chat
		// join
		{
			if (jsonObj.getJSONArray(HikeConstants.DATA).length() == 0)
			{
				return;
			}
			GroupConversation groupConversation = new GroupConversation(jsonObj, this.context);

			boolean groupRevived = false;

			if (!this.convDb.isGroupAlive(groupConversation.getMsisdn()))
			{

				Logger.d(getClass().getSimpleName(), "Group is not alive");
				int updated = this.convDb.toggleGroupDeadOrAlive(groupConversation.getMsisdn(), true);
				Logger.d(getClass().getSimpleName(), "Group revived? " + updated);
				groupRevived = updated > 0;

				if (groupRevived)
				{
					jsonObj.put(HikeConstants.NEW_GROUP, true);
					pubSub.publish(HikePubSub.GROUP_REVIVED, groupConversation.getMsisdn());
				}

			}
			if (!groupRevived && this.convDb.addGroupParticipants(groupConversation.getMsisdn(), groupConversation.getGroupParticipantList()) != HikeConstants.NEW_PARTICIPANT)
			{

				Logger.d(getClass().getSimpleName(), "GCJ Message was already received");
				return;
			}
			Logger.d(getClass().getSimpleName(), "GCJ Message is new");

			JSONObject metadata = jsonObj.optJSONObject(HikeConstants.METADATA);

			if (!groupRevived && !this.convDb.doesConversationExist(groupConversation.getMsisdn()))
			{
				Logger.d(getClass().getSimpleName(), "The group conversation does not exists");
				groupConversation = (GroupConversation) this.convDb.addConversation(groupConversation.getMsisdn(), false, "", groupConversation.getGroupOwner());

				if (metadata != null)
				{
					String groupName = metadata.optString(HikeConstants.NAME);
					if (!TextUtils.isEmpty(groupName))
					{
						convDb.setGroupName(groupConversation.getMsisdn(), groupName);
						groupConversation.setContactName(groupName);
					}
				}
				// Adding a key to the json signify that this was the GCJ
				// received for group creation
				jsonObj.put(HikeConstants.NEW_GROUP, true);
			}

			if (metadata != null)
			{
				JSONObject chatBgJson = metadata.optJSONObject(HikeConstants.CHAT_BACKGROUND);
				if (chatBgJson != null)
				{
					String bgId = chatBgJson.optString(HikeConstants.BG_ID);
					String groupId = groupConversation.getMsisdn();
					try
					{
						/*
						 * We don't support custom themes yet.
						 */
						if (chatBgJson.optBoolean(HikeConstants.CUSTOM))
						{
							throw new IllegalArgumentException();
						}

						ChatTheme chatTheme = ChatTheme.getThemeFromId(bgId);
						convDb.setChatBackground(groupId, chatTheme.bgId(), 0);
					}
					catch (IllegalArgumentException e)
					{
						/*
						 * This exception is thrown for unknown themes. Do nothing
						 */
					}
				}
			}

			saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE.equals(type)) // Group
		// chat
		// leave
		{
			String groupId = jsonObj.optString(HikeConstants.TO);
			String msisdn = jsonObj.optString(HikeConstants.DATA);
			if (this.convDb.setParticipantLeft(groupId, msisdn) > 0)
			{
				saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
			}
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_NAME.equals(type)) // Group
		// chat
		// name
		// change
		{
			String groupname = jsonObj.optString(HikeConstants.DATA);
			String groupId = jsonObj.optString(HikeConstants.TO);

			if (this.convDb.setGroupName(groupId, groupname) > 0)
			{
				this.pubSub.publish(HikePubSub.GROUP_NAME_CHANGED, groupId);

				boolean showPush = true;
				JSONObject metadata = jsonObj.optJSONObject(HikeConstants.METADATA);
				if (metadata != null)
				{
					showPush = metadata.optBoolean(HikeConstants.PUSH, true);
				}
				if (showPush)
				{
					saveStatusMsg(jsonObj, groupId);
				}
			}
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_END.equals(type)) // Group
		// chat
		// end
		{
			String groupId = jsonObj.optString(HikeConstants.TO);
			if (this.convDb.toggleGroupDeadOrAlive(groupId, false) > 0)
			{
				saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
			}
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE.equals(type)) // Message
		// received
		// from
		// server
		{
			Logger.d(getClass().getSimpleName(), "Checking if message exists");
			ConvMessage convMessage = new ConvMessage(jsonObj);
			if (convMessage.isStickerMessage())
			{
				convMessage.setMessage(context.getString(R.string.sent_sticker));
			}
			if (this.convDb.wasMessageReceived(convMessage)) // Check if message
			// was already
			// received by
			// the receiver
			{
				Logger.d(getClass().getSimpleName(), "Message already exists");
				return;
			}
			/*
			 * Need to rename every audio recording to a unique name since the ios client is sending every file with the same name.
			 */
			if (convMessage.isFileTransferMessage())
			{
				MessageMetadata messageMetadata = convMessage.getMetadata();
				HikeFile hikeFile = messageMetadata.getHikeFiles().get(0);

				JSONObject metadataJson = messageMetadata.getJSON();
				// this value indicates that file is not downloaded yet
				JSONArray fileArray = metadataJson.optJSONArray(HikeConstants.FILES);
				for (int i = 0; i < fileArray.length(); i++)
				{
					JSONObject fileJson = fileArray.getJSONObject(i);
					Logger.d(getClass().getSimpleName(), "Previous json: " + fileJson);
					if (hikeFile.getHikeFileType() != HikeFileType.CONTACT && hikeFile.getHikeFileType() != HikeFileType.LOCATION) // dont change name for contact or location
						fileJson.put(HikeConstants.FILE_NAME, Utils.getFinalFileName(hikeFile.getHikeFileType(), hikeFile.getFileName()));
					Logger.d(getClass().getSimpleName(), "New json: " + fileJson);
				}
				/*
				 * Resetting the metadata
				 */
				convMessage.setMetadata(metadataJson);
			}
			/*
			 * Applying the offset.
			 */
			convMessage.setTimestamp(Utils.applyServerTimeOffset(context, convMessage.getTimestamp()));

			convDb.addConversationMessages(convMessage);

			/*
			 * Return if there is no conversation mapped to this message
			 */
			if (convMessage.getConversation() == null)
			{
				return;
			}

			/*
			 * We need to add the name here in order to fix the bug where if the client receives two files of the same name, it shows the same file under both files.
			 */
			if (convMessage.isFileTransferMessage())
			{
				HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
				Logger.d(getClass().getSimpleName(), "FT MESSAGE: " + " NAME: " + hikeFile.getFileName() + " KEY: " + hikeFile.getFileKey());
				Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());
			}

			if (convMessage.getMetadata() != null)
			{
				if (convMessage.getMetadata().isPokeMessage())
				{
					Conversation conversation = convMessage.getConversation();
					boolean vibrate = false;
					if (conversation != null)
					{
						if (conversation instanceof GroupConversation)
						{
							if (!((GroupConversation) conversation).isMuted())
							{
								vibrate = true;
							}
						}
						else
						{
							vibrate = true;
						}
					}
					if (vibrate)
					{
						Utils.vibrateNudgeReceived(context);
					}
				}
			}
			Logger.d(getClass().getSimpleName(), "Receiver received Message : " + convMessage.getMessage() + "		;	Receiver Msg ID : " + convMessage.getMsgID() + "	; Mapped msgID : "
					+ convMessage.getMappedMsgID());
			// We have to do publish this here since we are adding the message
			// to the db here, and the id is set after inserting into the db.
			this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);

			if (convMessage.isGroupChat() && convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
			{
				this.pubSub.publish(HikePubSub.SHOW_PARTICIPANT_STATUS_MESSAGE, convMessage.getMsisdn());
			}

			/*
			 * Start auto download for media files
			 */
			if (convMessage.isFileTransferMessage() && (!TextUtils.isEmpty(convMessage.getConversation().getContactName())))
			{
				HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
				NetworkType networkType = FileTransferManager.getInstance(context).getNetworkType();
				if (hikeFile.getHikeFileType() == HikeFileType.IMAGE)
				{
					if ((networkType == NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, true))
							|| (networkType != NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, true)))
					{
						FileTransferManager.getInstance(context).downloadFile(hikeFile.getFile(), hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(),
								convMessage, false);
					}
				}
				else if (hikeFile.getHikeFileType() == HikeFileType.AUDIO || hikeFile.getHikeFileType() == HikeFileType.AUDIO_RECORDING)
				{
					if ((networkType == NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_AUDIO_PREF, true))
							|| (networkType != NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_AUDIO_PREF, false)))
					{
						FileTransferManager.getInstance(context).downloadFile(hikeFile.getFile(), hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(),
								convMessage, false);
					}
				}
				else if (hikeFile.getHikeFileType() == HikeFileType.VIDEO)
				{
					if ((networkType == NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_VIDEO_PREF, true))
							|| (networkType != NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_VIDEO_PREF, false)))
					{
						FileTransferManager.getInstance(context).downloadFile(hikeFile.getFile(), hikeFile.getFileKey(), convMessage.getMsgID(), hikeFile.getHikeFileType(),
								convMessage, false);
					}
				}

			}
			removeTypingNotification(convMessage.getMsisdn(), convMessage.getGroupParticipantMsisdn());
		}
		else if (HikeConstants.MqttMessageTypes.DELIVERY_REPORT.equals(type)) // Message
		// delivered
		// to
		// receiver
		{
			String id = jsonObj.optString(HikeConstants.DATA);
			String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
			long msgID;
			try
			{
				msgID = Long.parseLong(id);
			}
			catch (NumberFormatException e)
			{
				Logger.e(getClass().getSimpleName(), "Exception occured while parsing msgId. Exception : " + e);
				msgID = -1;
			}
			Logger.d(getClass().getSimpleName(), "Delivery report received for msgid : " + msgID + "	;	REPORT : DELIVERED");
			int rowsUpdated = updateDB(msgID, ConvMessage.State.SENT_DELIVERED, msisdn);

			if (rowsUpdated == 0)
			{
				Logger.d(getClass().getSimpleName(), "No rows updated");
				return;
			}

			Pair<String, Long> pair = new Pair<String, Long>(msisdn, msgID);

			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED, pair);
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE_READ.equals(type)) // Message
		// has
		// been
		// read
		{
			JSONArray msgIds = jsonObj.optJSONArray(HikeConstants.DATA);
			String id = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);

			String participantMsisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.FROM) : "";

			if (msgIds == null)
			{
				Logger.e(getClass().getSimpleName(), "Update Error : Message id Array is empty or null . Check problem");
				return;
			}

			long[] ids;
			if (!Utils.isGroupConversation(id))
			{
				ids = convDb.setAllDeliveredMessagesReadForMsisdn(id, msgIds);
				if (ids == null)
				{
					return;
				}
			}
			else
			{
				ids = new long[msgIds.length()];
				for (int i = 0; i < msgIds.length(); i++)
				{
					ids[i] = msgIds.optLong(i);
				}
				convDb.setReadByForGroup(id, ids, participantMsisdn);
			}

			Pair<String, long[]> pair = new Pair<String, long[]>(id, ids);

			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED_READ, pair);
		}
		else if (HikeConstants.MqttMessageTypes.START_TYPING.equals(type) || HikeConstants.MqttMessageTypes.END_TYPING.equals(type))
		{
			String id = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
			String participantMsisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.FROM) : null;

			if (HikeConstants.MqttMessageTypes.START_TYPING.equals(type))
			{
				addTypingNotification(id, participantMsisdn);
			}
			else
			{
				removeTypingNotification(id, participantMsisdn);
			}

		}
		else if (HikeConstants.MqttMessageTypes.UPDATE_AVAILABLE.equals(type))
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			String version = data.optString(HikeConstants.VERSION);
			Editor editor = settings.edit();
			int update = Utils.isUpdateRequired(version, context) ? (data.optBoolean(HikeConstants.CRITICAL) ? HikeConstants.CRITICAL_UPDATE : HikeConstants.NORMAL_UPDATE)
					: HikeConstants.NO_UPDATE;
			editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, update);
			editor.putString(HikeConstants.Extras.UPDATE_MESSAGE, data.optString(HikeConstants.MqttMessageTypes.MESSAGE));
			editor.commit();
			if (update != HikeConstants.NO_UPDATE)
			{
				this.pubSub.publish(HikePubSub.UPDATE_AVAILABLE, update);
			}
		}
		else if (HikeConstants.MqttMessageTypes.ACCOUNT_INFO.equals(type))
		{
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

			boolean inviteTokenAdded = false;
			boolean inviteeNumChanged = false;
			boolean showNewRewards = false;
			boolean showNewGames = false;
			boolean talkTimeChanged = false;
			int newTalkTime = 0;

			Editor editor = settings.edit();
			if (data.has(HikeConstants.INVITE_TOKEN))
			{
				editor.putString(HikeConstants.INVITE_TOKEN, data.getString(HikeConstants.INVITE_TOKEN));
				inviteTokenAdded = true;
			}
			if (data.has(HikeConstants.TOTAL_CREDITS_PER_MONTH))
			{
				editor.putString(HikeConstants.TOTAL_CREDITS_PER_MONTH, data.getString(HikeConstants.TOTAL_CREDITS_PER_MONTH));
				inviteeNumChanged = true;
			}
			if (data.optBoolean(HikeConstants.DEFAULT_SMS_CLIENT_TUTORIAL))
			{
				setDefaultSMSClientTutorialSetting();
			}
			if (data.has(HikeConstants.ENABLE_FREE_INVITES))
			{
				boolean sendNativeInvite = !data.optBoolean(HikeConstants.ENABLE_FREE_INVITES, true);
				boolean showFreeInvitePopup = data.optBoolean(HikeConstants.SHOW_FREE_INVITES) && !settings.getBoolean(HikeMessengerApp.SET_FREE_INVITE_POPUP_PREF_FROM_AI, false);
				if (showFreeInvitePopup)
				{
					editor.putBoolean(HikeMessengerApp.SET_FREE_INVITE_POPUP_PREF_FROM_AI, true);
					editor.putBoolean(HikeMessengerApp.FREE_INVITE_POPUP_DEFAULT_IMAGE, true);
				}

				handleSendNativeInviteKey(sendNativeInvite, showFreeInvitePopup, null, null, editor);
			}
			if (data.has(HikeConstants.ACCOUNT))
			{
				JSONObject account = data.getJSONObject(HikeConstants.ACCOUNT);
				if (account.has(HikeConstants.ACCOUNTS))
				{
					JSONObject accounts = account.getJSONObject(HikeConstants.ACCOUNTS);
					if (accounts.has(HikeConstants.TWITTER))
					{
						try
						{
							JSONObject twitter = accounts.getJSONObject(HikeConstants.TWITTER);
							String token = twitter.getString(HikeConstants.ID);
							String tokenSecret = twitter.getString(HikeConstants.TOKEN);
							HikeMessengerApp.makeTwitterInstance(token, tokenSecret);

							editor.putString(HikeMessengerApp.TWITTER_TOKEN, token);
							editor.putString(HikeMessengerApp.TWITTER_TOKEN_SECRET, tokenSecret);
							editor.putBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, true);
						}
						catch (JSONException e)
						{
							Logger.w(getClass().getSimpleName(), "Unknown format for twitter", e);
						}
					}
				}
				if (account.has(HikeConstants.MUTED))
				{
					JSONObject mutedGroups = account.getJSONObject(HikeConstants.MUTED);
					JSONArray groupIds = mutedGroups.names();
					if (groupIds != null && groupIds.length() > 0)
					{
						for (int i = 0; i < groupIds.length(); i++)
						{
							convDb.toggleGroupMute(groupIds.optString(i), true);
						}
					}
				}
				if (account.has(HikeConstants.FAVORITES))
				{
					JSONObject favorites = account.getJSONObject(HikeConstants.FAVORITES);

					if (favorites.length() > 0)
					{
						userDb.setMultipleContactsToFavorites(favorites);
					}
				}
				editor.putString(HikeMessengerApp.REWARDS_TOKEN, account.optString(HikeConstants.REWARDS_TOKEN));
				editor.putBoolean(HikeMessengerApp.SHOW_REWARDS, account.optBoolean(HikeConstants.SHOW_REWARDS));
				editor.putBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, !account.optBoolean(HikeConstants.SHOW_REWARDS));

				editor.putString(HikeMessengerApp.GAMES_TOKEN, account.optString(HikeConstants.REWARDS_TOKEN));
				editor.putBoolean(HikeMessengerApp.SHOW_GAMES, account.optBoolean(HikeConstants.SHOW_GAMES));
				editor.putBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, !account.optBoolean(HikeConstants.SHOW_GAMES));

				if (account.optBoolean(HikeConstants.SHOW_REWARDS))
				{
					showNewRewards = true;
				}

				if (account.optBoolean(HikeConstants.SHOW_GAMES))
				{
					showNewGames = true;
				}

				if (account.has(HikeConstants.REWARDS))
				{
					JSONObject rewards = account.getJSONObject(HikeConstants.REWARDS);

					int talkTime = rewards.optInt(HikeConstants.TALK_TIME, -1);
					if (talkTime > -1)
					{
						editor.putInt(HikeMessengerApp.TALK_TIME, talkTime);
						talkTimeChanged = true;
						newTalkTime = talkTime;
					}
				}
				if (account.has(HikeConstants.LAST_SEEN_SETTING))
				{
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
					Editor settingEditor = settings.edit();
					settingEditor.putBoolean(HikeConstants.LAST_SEEN_PREF, account.optBoolean(HikeConstants.LAST_SEEN_SETTING, true));
					settingEditor.commit();
				}
				if (account.has(HikeConstants.CHAT_BACKGROUNDS))
				{
					JSONArray chatBackgroundArray = account.getJSONArray(HikeConstants.CHAT_BACKGROUNDS);
					convDb.setChatThemesFromArray(chatBackgroundArray);
				}
				if (account.has(HikeConstants.CHAT_BACKGROUD_NOTIFICATION))
				{
					boolean showNotification = account.optInt(HikeConstants.CHAT_BG_NOTIFICATION_PREF, 0) != -1;
					Editor settingEditor = settings.edit();
					settingEditor.putBoolean(HikeConstants.CHAT_BG_NOTIFICATION_PREF, showNotification);
					settingEditor.commit();
				}
			}
			editor.commit();
			if (inviteTokenAdded)
			{
				pubSub.publish(HikePubSub.INVITE_TOKEN_ADDED, null);
			}
			if (inviteeNumChanged)
			{
				pubSub.publish(HikePubSub.INVITEE_NUM_CHANGED, null);
			}
			if (talkTimeChanged)
			{
				pubSub.publish(HikePubSub.TALK_TIME_CHANGED, newTalkTime);
			}
			if (showNewGames || showNewRewards)
			{
				this.pubSub.publish(HikePubSub.UPDATE_OF_MENU_NOTIFICATION, null);
			}
		}
		else if (HikeConstants.MqttMessageTypes.USER_OPT_IN.equals(type))
		{
			String msisdn = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);

			// For one-to-one chat
			saveStatusMsg(jsonObj, msisdn);

			List<String> groupConversations = convDb.listOfGroupConversationsWithMsisdn(msisdn);

			// Set the dnd status for the participant for all group chats
			convDb.updateDndStatus(msisdn);
			// For group chats
			for (String groupId : groupConversations)
			{
				saveStatusMsg(jsonObj, groupId);
			}
		}
		else if (HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS.equals(type))
		{
			String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
			saveStatusMsg(jsonObj, msisdn);
		}
		else if (HikeConstants.MqttMessageTypes.ADD_FAVORITE.equals(type))
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);

			/*
			 * Ignore if contact is blocked.
			 */
			if (userDb.isBlocked(msisdn))
			{
				return;
			}

			ContactInfo contactInfo = userDb.getContactInfoFromMSISDN(msisdn, false);
			if (contactInfo.getFavoriteType() == FavoriteType.FRIEND)
			{
				return;
			}
			FavoriteType currentType = contactInfo.getFavoriteType();
			FavoriteType favoriteType = (currentType == FavoriteType.NOT_FRIEND || currentType == FavoriteType.REQUEST_RECEIVED_REJECTED || currentType == FavoriteType.REQUEST_RECEIVED) ? FavoriteType.REQUEST_RECEIVED
					: FavoriteType.FRIEND;
			Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
			this.pubSub.publish(favoriteType == FavoriteType.REQUEST_RECEIVED ? HikePubSub.FAVORITE_TOGGLED : HikePubSub.FRIEND_REQUEST_ACCEPTED, favoriteToggle);
			if (favoriteType == FavoriteType.FRIEND)
			{
				StatusMessage statusMessage = new StatusMessage(0, null, msisdn, contactInfo.getName(), context.getString(R.string.confirmed_friend),
						StatusMessageType.FRIEND_REQUEST_ACCEPTED, System.currentTimeMillis() / 1000);

				convDb.addStatusMessage(statusMessage, true);

				pubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
				pubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
			}
		}
		else if (HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG.equals(type))
		{
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

			Editor editor = settings.edit();

			if (data.has(HikeConstants.REWARDS_TOKEN))
			{
				String rewardToken = data.getString(HikeConstants.REWARDS_TOKEN);
				editor.putString(HikeMessengerApp.REWARDS_TOKEN, rewardToken);
			}
			if (data.has(HikeConstants.GAMES_TOKEN))
			{
				String gamesToken = data.getString(HikeConstants.GAMES_TOKEN);
				editor.putString(HikeMessengerApp.GAMES_TOKEN, gamesToken);
			}
			if (data.has(HikeConstants.SHOW_REWARDS))
			{
				boolean showRewards = data.getBoolean(HikeConstants.SHOW_REWARDS);
				editor.putBoolean(HikeMessengerApp.SHOW_REWARDS, showRewards);
				editor.putBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, !showRewards);
			}
			if (data.has(HikeConstants.SHOW_GAMES))
			{
				boolean showGames = data.getBoolean(HikeConstants.SHOW_GAMES);
				editor.putBoolean(HikeMessengerApp.SHOW_GAMES, showGames);
				editor.putBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, !showGames);
			}
			if (data.has(HikeConstants.ENABLE_PUSH_BATCHING_STATUS_NOTIFICATIONS))
			{
				JSONArray array = data.getJSONArray(HikeConstants.ENABLE_PUSH_BATCHING_STATUS_NOTIFICATIONS);
				editor.putString(HikeMessengerApp.BATCH_STATUS_NOTIFICATION_VALUES, array.toString());
			}
			if (data.has(HikeConstants.ENABLE_FREE_INVITES))
			{
				String newId = data.optString(HikeConstants.MESSAGE_ID);
				String currentId = settings.getString(HikeMessengerApp.FREE_INVITE_PREVIOUS_ID, "");
				/*
				 * Duplicate check
				 */
				if (currentId.equals(newId))
				{
					Logger.d(getClass().getSimpleName(), "Duplicate enable free invite packet");
					return;
				}

				editor.putString(HikeMessengerApp.FREE_INVITE_PREVIOUS_ID, newId);
				editor.putBoolean(HikeMessengerApp.FREE_INVITE_POPUP_DEFAULT_IMAGE, false);

				boolean sendNativeInvite = !data.optBoolean(HikeConstants.ENABLE_FREE_INVITES, true);
				boolean showFreeInvitePopup = data.optBoolean(HikeConstants.SHOW_FREE_INVITES);
				String header = data.optString(HikeConstants.FREE_INVITE_POPUP_TITLE);
				String body = data.optString(HikeConstants.FREE_INVITE_POPUP_TEXT);

				handleSendNativeInviteKey(sendNativeInvite, showFreeInvitePopup, header, body, editor);

				/*
				 * Show notification if free SMS is turned on.
				 */
				if (!sendNativeInvite && HikeMessengerApp.isIndianUser())
				{
					Bundle bundle = new Bundle();
					bundle.putString(HikeConstants.Extras.FREE_SMS_POPUP_BODY, body);
					bundle.putString(HikeConstants.Extras.FREE_SMS_POPUP_HEADER, header);

					this.pubSub.publish(HikePubSub.SHOW_FREE_INVITE_SMS, bundle);
				}
			}
			if(data.has(HikeConstants.MQTT_IP_ADDRESSES)){
				JSONArray ipArray = data.getJSONArray(HikeConstants.MQTT_IP_ADDRESSES);
				if (null != ipArray && ipArray.length() > 0)
				{
					editor.putString(HikeMessengerApp.MQTT_IPS, ipArray.toString());
					HikeMqttManagerNew.setIpsChanged(true);
				}
			}

			editor.commit();
			this.pubSub.publish(HikePubSub.UPDATE_OF_MENU_NOTIFICATION, null);

		}
		else if (HikeConstants.MqttMessageTypes.REWARDS.equals(type))
		{
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

			int talkTime = data.getInt(HikeConstants.TALK_TIME);

			Editor editor = settings.edit();
			editor.putInt(HikeMessengerApp.TALK_TIME, talkTime);
			editor.commit();

			this.pubSub.publish(HikePubSub.TALK_TIME_CHANGED, talkTime);
		}
		else if (HikeConstants.MqttMessageTypes.ACTION.equals(type))
		{
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
			if (data.optBoolean(HikeConstants.POST_AB))
			{
				String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
				List<ContactInfo> contactinfos = ContactUtils.getContacts(this.context);
				Map<String, List<ContactInfo>> contacts = ContactUtils.convertToMap(contactinfos);
				try
				{
					AccountUtils.postAddressBook(token, contacts);
				}
				catch (IllegalStateException e)
				{
					Logger.w(getClass().getSimpleName(), "Exception while posting ab", e);
				}
				catch (IOException e)
				{
					Logger.w(getClass().getSimpleName(), "Exception while posting ab", e);
				}
			}
			if (data.optBoolean(HikeConstants.PUSH))
			{
				Editor editor = settings.edit();
				editor.putBoolean(HikeMessengerApp.GCM_ID_SENT, false);
				editor.commit();
				context.sendBroadcast(new Intent(HikeService.SEND_TO_SERVER_ACTION));
			}
			if (data.optBoolean(HikeConstants.DEFAULT_SMS_CLIENT_TUTORIAL))
			{
				setDefaultSMSClientTutorialSetting();
			}
			if (data.optBoolean(HikeConstants.POST_INFO))
			{
				Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
				editor.putBoolean(HikeMessengerApp.GREENBLUE_DETAILS_SENT, false);
				editor.commit();
				context.sendBroadcast(new Intent(HikeService.SEND_GB_DETAILS_TO_SERVER_ACTION));
			}
		}
		else if (HikeConstants.MqttMessageTypes.STATUS_UPDATE.equals(type))
		{
			StatusMessage statusMessage = new StatusMessage(jsonObj);
			/*
			 * This would be true for unsupported status message types. We should not be doing anything if we get one.
			 * 
			 * Also if the user is blocked, we ignore the message.
			 */
			if (statusMessage.getStatusMessageType() == null || userDb.isBlocked(statusMessage.getMsisdn()))
			{
				return;
			}
			/*
			 * Applying the offset.
			 */
			statusMessage.setTimeStamp(Utils.applyServerTimeOffset(context, statusMessage.getTimeStamp()));

			ContactInfo contactInfo = userDb.getContactInfoFromMSISDN(statusMessage.getMsisdn(), false);
			FavoriteType favoriteType = contactInfo.getFavoriteType();
			/*
			 * Only add updates to timeline for contacts that have a 2-way relationship with the user.
			 */
			long id = convDb.addStatusMessage(statusMessage, favoriteType == FavoriteType.FRIEND);

			if (id == -1)
			{
				Logger.d(getClass().getSimpleName(), "This status message was already added");
				return;
			}

			if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
			{
				String iconBase64 = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.THUMBNAIL);
				this.userDb.setIcon(statusMessage.getMappedId(), Base64.decode(iconBase64, Base64.DEFAULT), false);
				/*
				 * Removing the thumbnail string from the JSON, since we've already saved it.
				 */
				jsonObj.getJSONObject(HikeConstants.DATA).remove(HikeConstants.THUMBNAIL);

			}

			statusMessage.setName(TextUtils.isEmpty(contactInfo.getName()) ? contactInfo.getMsisdn() : contactInfo.getName());

			if (favoriteType == FavoriteType.FRIEND)
			{
				incrementUnseenStatusCount();
				pubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
				if (statusMessage.getStatusMessageType() == StatusMessageType.PROFILE_PIC)
				{
					/*
					 * Start auto download of the profile image.
					 */
					autoDownloadProfileImage(statusMessage, true);
				}
			}
			pubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			ConvMessage convMessage = saveStatusMsg(jsonObj, msisdn);

			if (convMessage == null)
			{
				return;
			}

			convDb.setMessageIdForStatus(statusMessage.getMappedId(), convMessage.getMsgID());
		}
		else if (HikeConstants.MqttMessageTypes.DELETE_STATUS.equals(type))
		{
			String statusId = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.STATUS_ID);
			pubSub.publish(HikePubSub.DELETE_STATUS, statusId);
		}
		else if (HikeConstants.MqttMessageTypes.POSTPONE_FAVORITE.equals(type) || HikeConstants.MqttMessageTypes.REMOVE_FAVORITE.equals(type))
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			removeOrPostponeFriendType(msisdn);
		}
		else if (HikeConstants.MqttMessageTypes.BATCH_STATUS_UPDATE.equals(type))
		{
			/*
			 * Only proceed if the user has selected a batch update preference
			 */
			if (PreferenceManager.getDefaultSharedPreferences(context).getInt(HikeConstants.STATUS_PREF, 0) <= 0)
			{
				return;
			}

			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

			String header = data.getString(HikeConstants.BATCH_HEADER);
			String message = data.getString(HikeConstants.BATCH_MESSAGE);

			pubSub.publish(HikePubSub.BATCH_STATUS_UPDATE_PUSH_RECEIVED, new Pair<String, String>(header, message));
		}
		else if (HikeConstants.MqttMessageTypes.STICKER.equals(type))
		{
			String subType = jsonObj.getString(HikeConstants.SUB_TYPE);
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
			String categoryId = data.getString(StickerManager.CATEGORY_ID);
			if (HikeConstants.ADD_STICKER.equals(subType))
			{
				convDb.stickerUpdateAvailable(categoryId);
				StickerManager.getInstance().setStickerUpdateAvailable(categoryId, true);
			}
			else if (HikeConstants.REMOVE_STICKER.equals(subType) || HikeConstants.REMOVE_CATEGORY.equals(subType))
			{

				String categoryDirPath = StickerManager.getInstance().getStickerDirectoryForCategoryId(context, categoryId);

				if (categoryDirPath == null)
				{
					return;
				}

				File categoryDir = new File(categoryDirPath);

				/*
				 * If the category itself does not exist, then we have nothing to delete
				 */
				if (!categoryDir.exists())
				{
					return;
				}

				if (HikeConstants.REMOVE_CATEGORY.equals(subType))
				{

					String removedIds = settings.getString(StickerManager.REMOVED_CATGORY_IDS, "[]");

					JSONArray removedIdArray = new JSONArray(removedIds);
					removedIdArray.put(categoryId);

					Editor editor = settings.edit();
					editor.putString(StickerManager.REMOVED_CATGORY_IDS, removedIdArray.toString());
					editor.commit();

					StickerManager.getInstance().setupStickerCategoryList(settings);

				}
				else
				{
					JSONArray stickerIds = data.getJSONArray(HikeConstants.STICKER_IDS);

					for (int i = 0; i < stickerIds.length(); i++)
					{
						String stickerId = stickerIds.getString(i);
						File stickerSmall = new File(categoryDir + HikeConstants.SMALL_STICKER_ROOT, stickerId);
						stickerSmall.delete();
						StickerManager.getInstance().removeStickerFromRecents(new Sticker(categoryId, stickerId));
					}
				}
			}
		}
		else if (HikeConstants.MqttMessageTypes.BULK_LAST_SEEN.equals(type))
		{
			/*
			 * {"t": "bls", "ts":<server timestamp>, "d": {"lastseens":{"+919818149394":<last_seen_time_in_epoch> ,"+919810335374":<last_seen_time_in_epoch>}}}
			 */
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
			JSONObject lastSeens = null;
			if (data != null)
				lastSeens = data.getJSONObject(HikeConstants.BULK_LAST_SEEN_KEY);
			// Iterator<String> iterator = lastSeens.keys();

			if (lastSeens != null)
			{
				for (Iterator<String> iterator = lastSeens.keys(); iterator.hasNext();)
				{
					String msisdn = iterator.next();
					int isOffline;
					long lastSeenTime = lastSeens.getLong(msisdn);
					if (lastSeenTime > 0)
					{
						isOffline = 1;
						lastSeenTime = Utils.applyServerTimeOffset(context, lastSeenTime);
					}
					else
					{
						/*
						 * Otherwise the last seen time notifies that the user is either online or has turned the setting off.
						 */
						isOffline = (int) lastSeenTime;
						lastSeenTime = System.currentTimeMillis() / 1000;
					}
					userDb.updateLastSeenTime(msisdn, lastSeenTime);
					userDb.updateIsOffline(msisdn, (int) isOffline);

					HikeMessengerApp.lastSeenFriendsMap.put(msisdn, new Pair<Integer, Long>(isOffline, lastSeenTime));

				}
				pubSub.publish(HikePubSub.LAST_SEEN_TIME_BULK_UPDATED, null);
			}

		}
		else if (HikeConstants.MqttMessageTypes.LAST_SEEN.equals(type))
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
			long lastSeenTime = data.getLong(HikeConstants.LAST_SEEN);
			int isOffline;
			/*
			 * Apply offset only if value is greater than 0
			 */
			if (lastSeenTime > 0)
			{
				isOffline = 1;
				lastSeenTime = Utils.applyServerTimeOffset(context, lastSeenTime);
			}
			else
			{
				/*
				 * Otherwise the last seen time notifies that the user is either online or has turned the setting off.
				 */
				isOffline = (int) lastSeenTime;
				lastSeenTime = System.currentTimeMillis() / 1000;
			}
			userDb.updateLastSeenTime(msisdn, lastSeenTime);
			userDb.updateIsOffline(msisdn, (int) isOffline);

			ContactInfo contactInfo = userDb.getContactInfoFromMSISDN(msisdn, false);
			contactInfo.setLastSeenTime(lastSeenTime);
			contactInfo.setOffline(isOffline);

			pubSub.publish(HikePubSub.LAST_SEEN_TIME_UPDATED, contactInfo);
		}
		else if (HikeConstants.MqttMessageTypes.SERVER_TIMESTAMP.equals(type))
		{
			long serverTimestamp = jsonObj.getLong(HikeConstants.TIMESTAMP);
			long diff = (System.currentTimeMillis() / 1000) - serverTimestamp;

			Logger.d(getClass().getSimpleName(), "Diff b/w server and client: " + diff);

			Editor editor = settings.edit();
			editor.putLong(HikeMessengerApp.SERVER_TIME_OFFSET, diff);
			editor.commit();
		}
		else if (HikeConstants.MqttMessageTypes.PROTIP.equals(type))
		{
			// We should delete the last showing pro tip from the DB, we don't
			// need it anymore.
			// As per the last request from growth team, we don't need to show
			// the older pro tips once the latest pro tips come in.
			long currentProtipId = settings.getLong(HikeMessengerApp.CURRENT_PROTIP, -1);
			boolean isValidProtip = false;

			Protip protip = new Protip(jsonObj);
			// check upfront if this protip is a valid protip
			if (protip != null && currentProtipId != protip.getId())
			{
				isValidProtip = true;
			}
			// only if its a valid protip, proceed with the display
			if (isValidProtip)
			{

				/*
				 * Applying the offset.
				 */
				protip.setTimeStamp(Utils.applyServerTimeOffset(context, protip.getTimeStamp()));
				long id = convDb.addProtip(protip);
				if (id == -1)
				{
					Logger.d(getClass().getSimpleName(), "Error adding this protip");
					return; // for some reason the insertion failed,
				}
				// delete all pro tips before these.
				// we dont need them anymore.

				convDb.deleteAllProtipsBeforeThisId(id);
				protip.setId(id);
				Editor editor = settings.edit();
				editor.putLong(HikeMessengerApp.CURRENT_PROTIP, protip.getId());
				editor.commit();
				String iconBase64 = jsonObj.getJSONObject(HikeConstants.DATA).optString(HikeConstants.THUMBNAIL);
				if (!TextUtils.isEmpty(iconBase64))
				{
					this.userDb.setIcon(protip.getMappedId(), Base64.decode(iconBase64, Base64.DEFAULT), false);
				}
				// increment the unseen status count straight away.
				// we've got a new pro tip.
				incrementUnseenStatusCount();

				StatusMessage statusMessage = new StatusMessage(protip);
				// download the protip only if the URL is non empty
				// also respect the user's auto photo download setting.
				if (!TextUtils.isEmpty(protip.getImageURL())
						&& ((FileTransferManager.getInstance(context).getNetworkType() == NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, true)) || (FileTransferManager
								.getInstance(context).getNetworkType() != NetworkType.WIFI && appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, true))))
				{
					autoDownloadProtipImage(statusMessage, true);
				}
				pubSub.publish(HikePubSub.PROTIP_ADDED, protip);
			}

		}
		else if (HikeConstants.MqttMessageTypes.UPDATE_PUSH.equals(type))
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			String devType = data.optString(HikeConstants.DEV_TYPE);
			String id = data.optString(HikeConstants.MESSAGE_ID);
			String lastPushPacketId = settings.getString(HikeConstants.Extras.LAST_UPDATE_PACKET_ID, "");
			if (!TextUtils.isEmpty(devType) && devType.equals(HikeConstants.NOKIA) && !TextUtils.isEmpty(id) && !lastPushPacketId.equals(id))
			{
				String version = data.optString(HikeConstants.UPDATE_VERSION);
				String updateURL = data.optString(HikeConstants.Extras.URL);
				int update = Utils.isUpdateRequired(version, context) ? (data.optBoolean(HikeConstants.CRITICAL_UPDATE_KEY) ? HikeConstants.CRITICAL_UPDATE
						: HikeConstants.NORMAL_UPDATE) : HikeConstants.NO_UPDATE;
				if ((update == HikeConstants.CRITICAL_UPDATE || update == HikeConstants.NORMAL_UPDATE))
				{
					if (Utils.isUpdateRequired(version, context))
					{
						Editor editor = settings.edit();
						editor.putInt(HikeConstants.Extras.UPDATE_AVAILABLE, update);
						editor.putString(HikeConstants.Extras.UPDATE_MESSAGE, data.optString(HikeConstants.MESSAGE));
						editor.putString(HikeConstants.Extras.LATEST_VERSION, version);
						editor.putString(HikeConstants.Extras.LAST_UPDATE_PACKET_ID, id);
						if (!TextUtils.isEmpty(updateURL))
							editor.putString(HikeConstants.Extras.URL, updateURL);
						editor.commit();
						this.pubSub.publish(HikePubSub.UPDATE_PUSH, update);
					}
				}
			}
		}
		else if (HikeConstants.MqttMessageTypes.APPLICATIONS_PUSH.equals(type))
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			String id = data.optString(HikeConstants.MESSAGE_ID);
			String lastPushPacketId = settings.getString(HikeConstants.Extras.LAST_APPLICATION_PUSH_PACKET_ID, "");
			String devType = data.optString(HikeConstants.DEV_TYPE);
			String message = data.optString(HikeConstants.MESSAGE);
			String packageName = data.optString(HikeConstants.PACKAGE);
			if (!TextUtils.isEmpty(devType) && devType.equals(HikeConstants.ANDROID) && !TextUtils.isEmpty(message) && !TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(id)
					&& !lastPushPacketId.equals(id))
			{
				Editor editor = settings.edit();
				editor.putString(HikeConstants.Extras.APPLICATIONSPUSH_MESSAGE, data.optString(HikeConstants.MESSAGE));
				editor.putString(HikeConstants.Extras.LAST_APPLICATION_PUSH_PACKET_ID, id);
				editor.commit();
				this.pubSub.publish(HikePubSub.APPLICATIONS_PUSH, packageName);
			}
		}
		else if (HikeConstants.MqttMessageTypes.CHAT_BACKGROUD.equals(type))
		{
			String from = jsonObj.optString(HikeConstants.FROM);
			String to = jsonObj.optString(HikeConstants.TO);

			long timestamp = jsonObj.optLong(HikeConstants.TIMESTAMP);
			timestamp = Utils.applyServerTimeOffset(context, timestamp);

			boolean isGroupConversation = false;
			if (!TextUtils.isEmpty(to))
			{
				isGroupConversation = Utils.isGroupConversation(to);
			}
			String id = isGroupConversation ? to : from;

			Pair<ChatTheme, Long> chatThemedata = convDb.getChatThemeAndTimestamp(id);

			if (chatThemedata != null)
			{
				long oldTimestamp = chatThemedata.second;
				if (oldTimestamp > timestamp)
				{
					/*
					 * We should ignore this packet since its either old or duplicate.
					 */
					return;
				}
				else if (oldTimestamp == timestamp)
				{
					JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
					String bgId = data.optString(HikeConstants.BG_ID);

					if (bgId.equals(chatThemedata.first.bgId()))
					{
						/*
						 * Duplicate theme.
						 */
						return;
					}
				}
			}

			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
			String bgId = data.optString(HikeConstants.BG_ID);

			try
			{
				/*
				 * If this is a custom theme, we should show it as not supported.
				 */
				if (data.optBoolean(HikeConstants.CUSTOM))
				{
					throw new IllegalArgumentException();
				}

				ChatTheme chatTheme = ChatTheme.getThemeFromId(bgId);
				convDb.setChatBackground(id, bgId, timestamp);

				this.pubSub.publish(HikePubSub.CHAT_BACKGROUND_CHANGED, new Pair<String, ChatTheme>(id, chatTheme));

				saveStatusMsg(jsonObj, id);
			}
			catch (IllegalArgumentException e)
			{
				/*
				 * This exception is thrown for unknown themes. Show an unsupported message
				 * Now in this case, we don't do anything. if user doesn't have certain theme
				 * that chatthread will keep on current applied theme.
				 */
			}
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_OWNER_CHANGE.equals(type))
		{
			String groupId = jsonObj.getString(HikeConstants.TO);

			JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
			String msisdn = data.getString(HikeConstants.MSISDN);

			convDb.changeGroupOwner(groupId, msisdn);
		}
		else if (HikeConstants.MqttMessageTypes.REQUEST_DP.equals(type))
		{
			final String groupId = jsonObj.getString(HikeConstants.TO);
			uploadGroupProfileImage(groupId, true);
		}
	}

	private void uploadGroupProfileImage(final String groupId, final boolean retryOnce)
	{
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		String fileName = Utils.getTempProfileImageFileName(groupId);

		File groupImageFile = new File(directory, fileName);
		if (!groupImageFile.exists())
		{
			return;
		}

		String path = "/group/" + groupId + "/avatar";

		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(path, RequestType.PROFILE_PIC, new HikeHttpCallback()
		{
			public void onFailure()
			{
				if (retryOnce)
				{
					uploadGroupProfileImage(groupId, false);
				}
				else
				{
					Utils.removeTempProfileImage(groupId);
					HikeMessengerApp.getLruCache().deleteIconForMSISDN(groupId);
					HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, groupId);
				}
			}

			public void onSuccess(JSONObject response)
			{
				Utils.renameTempProfileImage(groupId);
			}
		});
		hikeHttpRequest.setFilePath(groupImageFile.getPath());

		HikeHTTPTask task = new HikeHTTPTask(null, 0);
		Utils.executeHttpTask(task, hikeHttpRequest);
	}

	private void handleSendNativeInviteKey(boolean sendNativeInvite, boolean showFreeSmsPopup, String header, String body, Editor editor)
	{
		if (!HikeMessengerApp.isIndianUser())
		{
			return;
		}
		editor.putBoolean(HikeMessengerApp.SEND_NATIVE_INVITE, sendNativeInvite);
		if (sendNativeInvite)
		{
			/*
			 * If native is being turned on, we remove all preferences saved for not showing the native SMS invite dialog so that the user is shown these dialogs again.
			 */
			editor.remove(HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED);
			editor.remove(HikeConstants.FTUE_ADD_SMS_ALERT_CHECKED);
			editor.remove(HikeConstants.OPERATOR_SMS_ALERT_CHECKED);

			editor.putBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP, false);
		}
		else
		{
			/*
			 * Else we set a preference to show a dialog in the home screen that the free Invites are turned on.
			 */
			editor.putBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP, showFreeSmsPopup);
			if (showFreeSmsPopup)
			{
				editor.putString(HikeMessengerApp.FREE_INVITE_POPUP_BODY, body);
				editor.putString(HikeMessengerApp.FREE_INVITE_POPUP_HEADER, header);
			}
		}

	}

	private void autoDownloadProfileImage(StatusMessage statusMessage, boolean statusUpdate)
	{
		if ((FileTransferManager.getInstance(context).getNetworkType() == NetworkType.WIFI && !appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, true))
				|| (FileTransferManager.getInstance(context).getNetworkType() != NetworkType.WIFI && !appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, true)))
		{
			return;
		}

		String fileName = Utils.getProfileImageFileName(statusMessage.getMappedId());
		DownloadProfileImageTask downloadProfileImageTask = new DownloadProfileImageTask(context, statusMessage.getMappedId(), fileName, true, statusUpdate,
				statusMessage.getMsisdn(), statusMessage.getNotNullName(), false);
		Utils.executeBoolResultAsyncTask(downloadProfileImageTask);
	}

	private void autoDownloadGroupImage(String id)
	{
		if ((FileTransferManager.getInstance(context).getNetworkType() == NetworkType.WIFI && !appPrefs.getBoolean(HikeConstants.WF_AUTO_DOWNLOAD_IMAGE_PREF, true))
				|| (FileTransferManager.getInstance(context).getNetworkType() != NetworkType.WIFI && !appPrefs.getBoolean(HikeConstants.MD_AUTO_DOWNLOAD_IMAGE_PREF, true)))
		{
			return;
		}
		String fileName = Utils.getProfileImageFileName(id);
		DownloadProfileImageTask downloadProfileImageTask = new DownloadProfileImageTask(context, id, fileName, true, false, null, null, false);
		Utils.executeBoolResultAsyncTask(downloadProfileImageTask);
	}

	private void autoDownloadProtipImage(StatusMessage statusMessage, boolean statusUpdate)
	{
		String fileName = Utils.getProfileImageFileName(statusMessage.getMappedId());
		DownloadProfileImageTask downloadProfileImageTask = new DownloadProfileImageTask(context, statusMessage.getMappedId(), fileName, true, statusUpdate,
				statusMessage.getMsisdn(), statusMessage.getNotNullName(), false, statusMessage.getProtip().getImageURL());
		Utils.executeBoolResultAsyncTask(downloadProfileImageTask);
	}

	private void setDefaultSMSClientTutorialSetting()
	{
		/*
		 * If settings already contains this key, no need to do anything since this has already been handled.
		 */
		if (settings.contains(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP))
		{
			return;
		}

		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, false);
		editor.commit();
	}

	private void removeOrPostponeFriendType(String msisdn)
	{
		ContactInfo contactInfo = userDb.getContactInfoFromMSISDN(msisdn, false);
		if (contactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND)
		{
			return;
		}
		FavoriteType currentFavoriteType = contactInfo.getFavoriteType();
		FavoriteType favoriteType = (currentFavoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED || currentFavoriteType == FavoriteType.REQUEST_RECEIVED) ? FavoriteType.NOT_FRIEND
				: FavoriteType.REQUEST_SENT_REJECTED;
		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, ContactInfo.FavoriteType>(contactInfo, favoriteType);
		this.pubSub.publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
	}

	private void incrementUnseenStatusCount()
	{
		int count = settings.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
		count++;
		Editor editor = settings.edit();
		editor.putInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, count);
		editor.commit();

		pubSub.publish(HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT, null);
	}

	private void updateDbBatch(long[] ids, ConvMessage.State status, String msisdn)
	{
		convDb.updateBatch(ids, status.ordinal(), msisdn);
	}

	private int updateDB(Object object, ConvMessage.State status, String msisdn)
	{
		long msgID = (Long) object;
		/*
		 * TODO we should lookup the convid for this user, since otherwise one could set mess with the state for other conversations
		 */
		return convDb.updateMsgStatus(msgID, status.ordinal(), msisdn);
	}

	private ConvMessage saveStatusMsg(JSONObject jsonObj, String msisdn) throws JSONException
	{
		Conversation conversation = convDb.getConversationWithLastMessage(msisdn);

		boolean isChatBgMsg = HikeConstants.MqttMessageTypes.CHAT_BACKGROUD.equals(jsonObj.getString(HikeConstants.TYPE));
		boolean isUJMsg = HikeConstants.MqttMessageTypes.USER_JOINED.equals(jsonObj.getString(HikeConstants.TYPE));
		boolean isGettingCredits = false;
		if (isUJMsg)
		{
			isGettingCredits = jsonObj.getJSONObject(HikeConstants.DATA).optInt(HikeConstants.CREDITS, -1) > 0;
		}
		/*
		 * If the message is of type 'uj' we want to show it for all known contacts regardless of if the user currently has an existing conversation. We also want to show the 'uj'
		 * message in all the group chats with that participant. Otherwise for other types, we only show the message if the user already has an existing conversation.
		 */
		if (!isChatBgMsg)
		{
			if ((conversation == null && (!isUJMsg || !userDb.doesContactExist(msisdn)))
					|| (conversation != null && TextUtils.isEmpty(conversation.getContactName()) && isUJMsg && !isGettingCredits && !(conversation instanceof GroupConversation)))
			{
				return null;
			}
		}
		ConvMessage convMessage = new ConvMessage(jsonObj, conversation, context, false);
		convDb.addConversationMessages(convMessage);

		this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED || convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT
				|| convMessage.getParticipantInfoState() == ParticipantInfoState.GROUP_END)
		{
			this.pubSub.publish(
					convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED ? HikePubSub.PARTICIPANT_JOINED_GROUP
							: convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT ? HikePubSub.PARTICIPANT_LEFT_GROUP : HikePubSub.GROUP_END, jsonObj);
		}
		return convMessage;
	}

	private void addTypingNotification(String id, String participant)
	{
		TypingNotification typingNotification;
		ClearTypingNotification clearTypingNotification;
		boolean isGroupConversation = !TextUtils.isEmpty(participant);

		if (!typingNotificationMap.containsKey(id))
		{
			if (isGroupConversation)
			{
				clearTypingNotification = new ClearGroupTypingNotification(id, participant);
				typingNotification = new GroupTypingNotification(id, participant, (ClearGroupTypingNotification) clearTypingNotification);
			}
			else
			{
				clearTypingNotification = new ClearTypingNotification(id);

				typingNotification = new TypingNotification(id, clearTypingNotification);
			}

			typingNotificationMap.put(id, typingNotification);
		}
		else
		{
			typingNotification = typingNotificationMap.get(id);

			if (isGroupConversation)
			{
				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) typingNotification;
				if (!groupTypingNotification.hasParticipant(participant))
				{
					clearTypingNotification = new ClearGroupTypingNotification(id, participant);

					groupTypingNotification.addParticipant(participant);
					groupTypingNotification.addClearTypingNotification((ClearGroupTypingNotification) clearTypingNotification);
				}
				else
				{
					clearTypingNotification = groupTypingNotification.getClearTypingNotification(participant);
				}
			}
			else
			{
				clearTypingNotification = typingNotification.getClearTypingNotification();
			}
			clearTypingNotificationHandler.removeCallbacks(clearTypingNotification);
		}
		clearTypingNotificationHandler.postDelayed(clearTypingNotification, HikeConstants.LOCAL_CLEAR_TYPING_TIME);

		this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, typingNotification);
	}

	private void removeTypingNotification(String id, String participant)
	{

		boolean isGroupConversation = !TextUtils.isEmpty(participant);

		TypingNotification typingNotification = typingNotificationMap.get(id);

		ClearTypingNotification clearTypingNotification;

		if (typingNotification != null)
		{
			if (isGroupConversation)
			{
				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) typingNotification;
				groupTypingNotification.removeParticipant(participant);
				Logger.d("TypingNotification", "Particpant size: " + groupTypingNotification.getGroupParticipantList().size());
				if (groupTypingNotification.getGroupParticipantList().isEmpty())
				{
					typingNotificationMap.remove(id);
				}
				clearTypingNotification = groupTypingNotification.getClearTypingNotification(participant);
			}
			else
			{
				typingNotificationMap.remove(id);
				clearTypingNotification = typingNotification.getClearTypingNotification();
			}

			clearTypingNotificationHandler.removeCallbacks(clearTypingNotification);
		}

		this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, typingNotification);
	}
		
}