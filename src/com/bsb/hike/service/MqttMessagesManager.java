package com.bsb.hike.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.filetransfer.FileTransferManager.NetworkType;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.Protip;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.DownloadProfileImageTask;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.ui.HikePreferences;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.ClearGroupTypingNotification;
import com.bsb.hike.utils.ClearTypingNotification;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author Rishabh This class is used for saving all the mqtt messages in the db based on their types. Its also used to publish these events for the UI to make the changes,
 *         wherever applicable. This class should be a singleton, since only one instance should be used managing these messages
 */
public class MqttMessagesManager
{

	private static final String UJFile = "uj_file";

	private HikeConversationsDatabase convDb;

	private SharedPreferences settings;

	private SharedPreferences appPrefs;

	private Context context;

	private HikePubSub pubSub;

	private Map<String, TypingNotification> typingNotificationMap;

	private Handler clearTypingNotificationHandler;

	private static MqttMessagesManager instance;

	private String userMsisdn;

	private boolean isBulkMessage = false;

	private SQLiteDatabase convWriteDb;

	private SQLiteDatabase userWriteDb;

	private LinkedList<ConvMessage> messageList;

	private Map<String, LinkedList<ConvMessage>> messageListMap;

	private Map<String, PairModified<PairModified<Long, Set<String>>, Long>> messageStatusMap;

	private MqttMessagesManager(Context context)
	{
		this.convDb = HikeConversationsDatabase.getInstance();
		this.userWriteDb = ContactManager.getInstance().getWritableDatabase();
		this.convWriteDb = convDb.getWritableDatabase();
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

	private void saveIcon(JSONObject jsonObj) throws JSONException
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
		ContactManager.getInstance().setIcon(msisdn, Base64.decode(iconBase64, Base64.DEFAULT), false);

		HikeMessengerApp.getLruCache().clearIconForMSISDN(msisdn);
		HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
		// IconCacheManager.getInstance().clearIconForMSISDN(msisdn);

		/*
		 * Only auto download if the ic packet is not generated due to signup.
		 */
		if (!HikeConstants.SIGNUP_IC.equals(jsonObj.optString(HikeConstants.SUB_TYPE)))
		{
			FavoriteType favType = ContactManager.getInstance().getFriendshipStatus(msisdn);
			if (favType == FavoriteType.FRIEND || favType == FavoriteType.REQUEST_SENT || favType == FavoriteType.REQUEST_SENT_REJECTED)
			{
				autoDownloadGroupImage(msisdn);
			}
		}
	}

	private void saveDisplayPic(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.getString(HikeConstants.TO);
		String iconBase64 = jsonObj.getString(HikeConstants.DATA);
		String newIconIdentifier = null;
		ContactManager conMgr = ContactManager.getInstance();
		if (iconBase64.length() < 6)
		{
			newIconIdentifier = iconBase64;
		}
		else
		{
			newIconIdentifier = iconBase64.substring(0, 5) + iconBase64.substring(iconBase64.length() - 6);
		}

		String oldIconIdentifier = conMgr.getIconIdentifierString(groupId);

		/*
		 * Same Icon
		 */
		if (newIconIdentifier.equals(oldIconIdentifier))
		{
			return;
		}

		conMgr.setIcon(groupId, Base64.decode(iconBase64, Base64.DEFAULT), false);

		HikeMessengerApp.getLruCache().clearIconForMSISDN(groupId);
		HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, groupId);

		// IconCacheManager.getInstance().clearIconForMSISDN(groupId);
		autoDownloadGroupImage(groupId);
		saveStatusMsg(jsonObj, groupId);
	}

	private void saveSMSCredits(JSONObject jsonObj) throws JSONException
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

	private void saveUserJoinedOrLeft(JSONObject jsonObj) throws JSONException
	{
		String type = jsonObj.optString(HikeConstants.TYPE);
		String msisdn = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.MSISDN);
		boolean joined = HikeConstants.MqttMessageTypes.USER_JOINED.equals(type);
		long joinTime = 0;
		SharedPreferences settings = context.getSharedPreferences(UJFile, Context.MODE_PRIVATE);
		if (joined)
		{
			joinTime = jsonObj.optLong(HikeConstants.TIMESTAMP);
			long ts = settings.getLong(msisdn, -1);
			if (ts == -1) // this shows last uj was for some other msisdn or this user has left or pref file do not exist
			{
				Editor e = settings.edit();
				e.clear(); // remove old values if any as we have to keep just one msisdn at a time
				e.putLong(msisdn, joinTime);
				e.commit();
			}
			else if (ts == joinTime) // this shows UJ is duplicate so ignore
				return;
			else
				// last join time was different from latest time
				settings.edit().putLong(msisdn, joinTime).commit();
		}
		else
		{
			// if user left Hike simply remove the value from pref
			settings.edit().remove(msisdn).commit();
		}

		ContactManager.getInstance().updateHikeStatus(this.context, msisdn, joined);
		this.convDb.updateOnHikeStatus(msisdn, joined);

		if (joined)
		{
			if (joinTime > 0)
			{
				joinTime = Utils.applyServerTimeOffset(context, joinTime);
				ContactManager.getInstance().setHikeJoinTime(msisdn, joinTime);
				
				ContactInfo contact = ContactManager.getInstance().getContact(msisdn, true, false);
				if (contact.getHikeJoinTime() > 0 && !contact.isUnknownContact())
				{
					Editor e = this.settings.edit();
					e.putBoolean(HikeConstants.SHOW_RECENTLY_JOINED_DOT, true);
					e.commit();
				}
			}
			
			if (appPrefs.getBoolean(HikeConstants.NUJ_NOTIF_BOOLEAN_PREF, true))
			{
				ConvMessage convMessage = statusMessagePreProcess(jsonObj, msisdn);
				
				if (convMessage != null && !isBulkMessage)
				{
					this.pubSub.publish(HikePubSub.USER_JOINED_NOTIFICATION, convMessage);
				}
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

	private void saveInviteInfo(JSONObject jsonObj) throws JSONException
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

	private void saveGCJoin(JSONObject jsonObj) throws JSONException
	{
		if (jsonObj.getJSONArray(HikeConstants.DATA).length() == 0)
		{
			return;
		}
		GroupConversation groupConversation = new GroupConversation(jsonObj, this.context);

		boolean groupRevived = false;

		if (!ContactManager.getInstance().isGroupAlive(groupConversation.getMsisdn()))
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
		int gcjAdd = this.convDb.addRemoveGroupParticipants(groupConversation.getMsisdn(), groupConversation.getGroupParticipantList(), groupRevived);
		if (!groupRevived && gcjAdd != HikeConstants.NEW_PARTICIPANT)
		{
			Logger.d(getClass().getSimpleName(), "GCJ Message was already received");
			return;
		}
		Logger.d(getClass().getSimpleName(), "GCJ Message is new");

		JSONObject metadata = jsonObj.optJSONObject(HikeConstants.METADATA);

		/*
		 * 
		 * if (!groupRevived && !ContactManager.getInstance().isGroupExist(groupConversation.getMsisdn())) { Logger.d(getClass().getSimpleName(),
		 * "The group conversation does not exists"); if (metadata != null) {
		 * 
		 * String groupName = metadata.optString(HikeConstants.NAME); groupConversation = (GroupConversation) this.convDb.addConversation(groupConversation.getMsisdn(), false,
		 * groupName, groupConversation.getGroupOwner()); groupConversation.setContactName(groupName);
		 * ContactManager.getInstance().insertGroup(groupConversation.getMsisdn(),groupName); }
		 */
		// Adding a key to the json signify that this was the GCJ
		// received for group creation
		// jsonObj.put(HikeConstants.NEW_GROUP, true);
		if (!groupRevived && !ContactManager.getInstance().isGroupExist(groupConversation.getMsisdn()))
		{
			Logger.d(getClass().getSimpleName(), "The group conversation does not exists");

			String groupName = "";
			if (metadata != null)
			{
				// Earlier there were 2 queries, one to make the group conv and second to set the name. I have combined the both
				groupName = metadata.optString(HikeConstants.NAME);
			}
			groupConversation = (GroupConversation) this.convDb.addConversation(groupConversation.getMsisdn(), false, groupName, groupConversation.getGroupOwner());
			ContactManager.getInstance().insertGroup(groupConversation.getMsisdn(), groupName);

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

	private void saveGCLeave(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.optString(HikeConstants.TO);
		String msisdn = jsonObj.optString(HikeConstants.DATA);
		if (this.convDb.setParticipantLeft(groupId, msisdn) > 0)
		{
			ContactManager.getInstance().removeGroupParticipant(groupId, msisdn);
			saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
		}
	}

	private void saveGCName(JSONObject jsonObj) throws JSONException
	{
		String groupname = jsonObj.optString(HikeConstants.DATA);
		String groupId = jsonObj.optString(HikeConstants.TO);

		if (this.convDb.setGroupName(groupId, groupname) > 0)
		{
			ContactManager.getInstance().setGroupName(groupId, groupname);
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

	private void saveGCEnd(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.optString(HikeConstants.TO);
		if (this.convDb.toggleGroupDeadOrAlive(groupId, false) > 0)
		{
			saveStatusMsg(jsonObj, jsonObj.getString(HikeConstants.TO));
		}
	}

	private void saveMessage(JSONObject jsonObj) throws JSONException
	{
		ConvMessage convMessage = messagePreProcess(jsonObj);

		/*
		 * adding message in db if not duplicate. In case of duplicate message we don't do further processing and return
		 */
		if (!convDb.addConversationMessages(convMessage))
		{
			return;
		}

		/*
		 * Return if there is no conversation for this msisdn.
		 */
		if (!ContactManager.getInstance().isConvExists(convMessage.getMsisdn()))
		{
			Logger.d(getClass().getSimpleName(), "conversation does not exist");
			return;
		}

		// We have to do publish this here since we are adding the message
		// to the db here, and the id is set after inserting into the db.
		this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);

		messageProcessVibrate(convMessage);
		messageProcessFT(convMessage);

		if (convMessage.isGroupChat() && convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{
			ConvMessage convMessageNew = convDb.showParticipantStatusMessage(convMessage.getMsisdn());
			if (convMessageNew != null)
			{
				if (convDb.addConversationMessages(convMessageNew))
				{
					this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessageNew);
				}

			}
		}

		removeTypingNotification(convMessage.getMsisdn(), convMessage.getGroupParticipantMsisdn());

	}

	private void saveMessageBulk(JSONObject jsonObj) throws JSONException
	{
		ConvMessage convMessage = messagePreProcess(jsonObj);
		addToLists(convMessage.getMsisdn(), convMessage);

		if (convMessage.isGroupChat() && convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
		{
			ConvMessage convMessageNew = convDb.showParticipantStatusMessage(convMessage.getMsisdn());
			if (convMessageNew != null)
			{
				addToLists(convMessageNew.getMsisdn(), convMessageNew);
			}
		}
	}

	/**
	 * This function pre-process on message of type "m" like make convMessage object , set metadata and timestamp
	 * 
	 * @param jsonObj
	 *            the JsonObject of type "m"
	 * 
	 * @return ConvMessage object
	 */
	private ConvMessage messagePreProcess(JSONObject jsonObj) throws JSONException
	{
		ConvMessage convMessage = new ConvMessage(jsonObj);
		if (convMessage.isStickerMessage())
		{
			convMessage.setMessage(context.getString(R.string.sent_sticker));
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

		return convMessage;
	}

	/**
	 * This function decides whether to vibrate or not for a given message
	 */
	private void messageProcessVibrate(ConvMessage convMessage)
	{
		if (convMessage.getMetadata() != null)
		{
			if (convMessage.getMetadata().isPokeMessage())
			{
				boolean vibrate = false;
				String msisdn = convMessage.getMsisdn();
				if (ContactManager.getInstance().isConvExists(msisdn))
				{
					if (Utils.isGroupConversation(msisdn))
					{
						if (!HikeConversationsDatabase.getInstance().isGroupMuted(msisdn))
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

	}

	/**
	 * This function does processing on file transfer message
	 * 
	 * @param convMessage
	 *            the ConvMessage object with message id and conversation object initialized
	 */
	private void messageProcessFT(ConvMessage convMessage)
	{

		/*
		 * We need to add the name here in order to fix the bug where if the client receives two files of the same name, it shows the same file under both files.
		 */
		if (convMessage.isFileTransferMessage())
		{
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			Logger.d(getClass().getSimpleName(), "FT MESSAGE: " + " NAME: " + hikeFile.getFileName() + " KEY: " + hikeFile.getFileKey());
			Utils.addFileName(hikeFile.getFileName(), hikeFile.getFileKey());
		}

		ContactManager manager = ContactManager.getInstance();
		String msisdn = convMessage.getMsisdn();
		/*
		 * Start auto download for media files
		 */
		String name = Utils.isGroupConversation(msisdn) ? manager.getName(msisdn) : manager.getContact(msisdn, false, true).getName();
		if (convMessage.isFileTransferMessage() && (!TextUtils.isEmpty(name)) && (manager.isConvExists(msisdn)))
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
	}

	private void saveDeliveryReport(JSONObject jsonObj) throws JSONException
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

	/**
	 * <li>This function does specific "dr" processing for bulk.</li> <li>adds message id to {@link #messageStatusMap} second field if this id is grater than that present in second
	 * field
	 * 
	 * @param jsonObj
	 *            JsonObject of type "dr"
	 * @throws JSONException
	 */
	private void saveDeliveryReportBulk(JSONObject jsonObj) throws JSONException
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

		/*
		 * update message status map with max dr msgId corresponding to its msisdn
		 */

		if (messageStatusMap.get(msisdn) == null)
		{
			messageStatusMap.put(msisdn, new PairModified<PairModified<Long, Set<String>>, Long>(null, (long) -1));
		}
		if (null == messageStatusMap.get(msisdn).getFirst())
		{
			Set<String> msisdnSet = new HashSet<String>();
			PairModified<Long, Set<String>> pair = new PairModified<Long, Set<String>>((long) -1, msisdnSet);
			messageStatusMap.get(msisdn).setFirst(pair);
		}
		if (msgID > messageStatusMap.get(msisdn).getSecond())
		{
			messageStatusMap.get(msisdn).setSecond(msgID);
		}
	}

	private void saveMessageRead(JSONObject jsonObj) throws JSONException
	{
		JSONArray msgIds = jsonObj.optJSONArray(HikeConstants.DATA);
		String id = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);

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
			Pair<String, long[]> pair = new Pair<String, long[]>(id, ids);
			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED_READ, pair);
		}
		else
		{
			String participantMsisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.FROM) : "";
			if(TextUtils.isEmpty(participantMsisdn))
			{
				return ;
			}
			ids = new long[msgIds.length()];
			for (int i = 0; i < msgIds.length(); i++)
			{
				ids[i] = msgIds.optLong(i);
			}
			long maxMsgId = convDb.setReadByForGroup(id, ids, participantMsisdn);

			if (maxMsgId > 0)
			{
				Pair<Long, String> pair = new Pair<Long, String>(maxMsgId, participantMsisdn);
				Pair<String, Pair<Long, String>> groupPair = new Pair<String, Pair<Long, String>>(id, pair);
				this.pubSub.publish(HikePubSub.GROUP_MESSAGE_DELIVERED_READ, groupPair);
			}
		}
	}

	/**
	 * <li>This function does specific "mr" processing for bulk.</li>
	 * <p>
	 * In 1-1 conversation it adds max message id from ids list to {@link #messageStatusMap} first field if this id is greater than that present in first field
	 * </p>
	 * 
	 * <p>
	 * In group conversation since we receive mr for messages sent by others also we have to first check whether the list of ids present in mr belongs to our conversation or not. <br>
	 * We call {@link HikeConversationsDatabase#getMrIdForGroup(String, long[])} passing groupId and ids as arguments.</br> It will return max id from list if it belongs to this
	 * conversation else it will return -1.
	 * <li>if id returned is less than that already present in first field we simply return</li>
	 * <li>if equals we have to participant msisdn to set</li>
	 * <li>if greater than we have clear set and update update both set and msgid fields in pair</li>
	 * 
	 * @param jsonObj
	 *            -- mr json containing list of ids
	 * @throws JSONException
	 */
	private void saveMessageReadBulk(JSONObject jsonObj) throws JSONException
	{

		JSONArray msgIds = jsonObj.optJSONArray(HikeConstants.DATA);
		String id = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);

		String participantMsisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.FROM) : "";

		if (msgIds == null)
		{
			Logger.e(getClass().getSimpleName(), "Update Error : Message id Array is empty or null . Check problem");
			return;
		}

		if (messageStatusMap.get(id) == null)
		{
			messageStatusMap.put(id, new PairModified<PairModified<Long, Set<String>>, Long>(null, (long) -1));
		}

		if (null == messageStatusMap.get(id).getFirst())
		{
			Set<String> msisdnSet = new HashSet<String>();
			PairModified<Long, Set<String>> pair = new PairModified<Long, Set<String>>((long) -1, msisdnSet);
			messageStatusMap.get(id).setFirst(pair);
		}

		PairModified<Long, Set<String>> pair = messageStatusMap.get(id).getFirst();
		long msgID = -1;

		if (Utils.isGroupConversation(id))
		{
			long[] ids = new long[msgIds.length()];
			for (int i = 0; i < msgIds.length(); i++)
			{
				ids[i] = msgIds.optLong(i);
			}

			msgID = convDb.getMrIdForGroup(id, ids);
			if (pair.getFirst() > msgID)
			{
				return;
			}

			if (pair.getFirst() < msgID)
			{
				pair.setFirst(msgID);
				pair.getSecond().clear();
			}
			pair.getSecond().add(participantMsisdn);
		}
		else
		{

			for (int i = 0; i < msgIds.length(); i++)
			{
				long tempId = msgIds.optLong(i);
				if (tempId > msgID)
				{
					msgID = tempId;
				}
				if (msgID > pair.getFirst())
				{
					pair.setFirst(msgID);
				}
			}

		}
	}

	private void saveTyping(JSONObject jsonObj) throws JSONException
	{
		String type = jsonObj.optString(HikeConstants.TYPE);
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

	private void saveUpdateAvailable(JSONObject jsonObj) throws JSONException
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

	private void saveAccountInfo(JSONObject jsonObj) throws JSONException
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
					ContactManager.getInstance().setMultipleContactsToFavorites(favorites);
				}
			}
			
			String rewardsToken = account.optString(HikeConstants.REWARDS_TOKEN);
			if(!TextUtils.isEmpty(rewardsToken))	
			{
				/* Server may disable rewards but not necessary invalidate rewards token.
				 * To help Server not resend rewards token (and thus save server side DB queries) when later enabling rewards,
				 * client will be caching the rewards token.
				 */
				editor.putString(HikeMessengerApp.REWARDS_TOKEN, rewardsToken);
				// TODO. Should this be games_token ?
				editor.putString(HikeMessengerApp.GAMES_TOKEN, rewardsToken); 
			}

			editor.putBoolean(HikeMessengerApp.SHOW_REWARDS, account.optBoolean(HikeConstants.SHOW_REWARDS));
			editor.putBoolean(HikeMessengerApp.SHOW_GAMES, account.optBoolean(HikeConstants.SHOW_GAMES));

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
			if (account.has(HikeConstants.AVATAR))
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				Editor settingEditor = settings.edit();
				int dpSetting = account.optInt(HikeConstants.AVATAR, 1);
				boolean defaultSetting = false;
				if (dpSetting == 2)
				{
					defaultSetting = true;
				}
				settingEditor.putBoolean(HikeConstants.PROFILE_PIC_PREF, defaultSetting);
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

	private void saveUserOptIn(JSONObject jsonObj) throws JSONException
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

	private void saveBlockInternationalSMS(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);
		saveStatusMsg(jsonObj, msisdn);
	}

	private void saveAddFavorite(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.getString(HikeConstants.FROM);

		ContactManager conMgr = ContactManager.getInstance();
		/*
		 * Ignore if contact is blocked.
		 */
		if (conMgr.isBlocked(msisdn))
		{
			return;
		}

		ContactInfo contactInfo = conMgr.getContact(msisdn, true, false);
		if (contactInfo.getFavoriteType() == FavoriteType.FRIEND)
		{
			return;
		}
		FavoriteType currentType = contactInfo.getFavoriteType();
		FavoriteType favoriteType = (currentType == FavoriteType.NOT_FRIEND || currentType == FavoriteType.REQUEST_RECEIVED_REJECTED || currentType == FavoriteType.REQUEST_RECEIVED) ? FavoriteType.REQUEST_RECEIVED
				: FavoriteType.FRIEND;

		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(contactInfo, favoriteType);
		this.pubSub.publish(favoriteType == FavoriteType.REQUEST_RECEIVED ? HikePubSub.FAVORITE_TOGGLED : HikePubSub.FRIEND_REQUEST_ACCEPTED, favoriteToggle);

		if(favoriteType == favoriteType.FRIEND)
		{
			incrementUnseenStatusCount();
		}
		else if(favoriteType == favoriteType.REQUEST_RECEIVED && currentType != favoriteType.REQUEST_RECEIVED)
		{
			int count = settings.getInt(HikeMessengerApp.FRIEND_REQ_COUNT, 0);
			if (count >= 0)
			{
				Utils.incrementOrDecrementFriendRequestCount(settings, 1);
			}
		}

		if (favoriteType == FavoriteType.FRIEND)
		{
			StatusMessage statusMessage = new StatusMessage(0, null, msisdn, contactInfo.getName(), context.getString(R.string.confirmed_friend),
					StatusMessageType.FRIEND_REQUEST_ACCEPTED, System.currentTimeMillis() / 1000);

			convDb.addStatusMessage(statusMessage, true);

			pubSub.publish(HikePubSub.STATUS_MESSAGE_RECEIVED, statusMessage);
			pubSub.publish(HikePubSub.TIMELINE_UPDATE_RECIEVED, statusMessage);
		}
	}

	private void saveAccountConfig(JSONObject jsonObj) throws JSONException
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
		if (data.has(HikeConstants.MQTT_IP_ADDRESSES))
		{
			JSONArray ipArray = data.getJSONArray(HikeConstants.MQTT_IP_ADDRESSES);
			if (null != ipArray && ipArray.length() > 0)
			{
				LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(new Intent(HikePubSub.IPS_CHANGED).putExtra("ips", ipArray.toString()));
			}
		}
		// watsapp invite message
		if (data.has(HikeConstants.WATSAPP_INVITE_ENABLED))
		{
			HikeSharedPreferenceUtil.getInstance(context).saveData(HikeConstants.WATSAPP_INVITE_ENABLED, data.getBoolean(HikeConstants.WATSAPP_INVITE_ENABLED));
		}
		if (data.has(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY))
		{
			String message = data.getString(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY);
			HikeSharedPreferenceUtil.getInstance(context).saveData(HikeConstants.WATSAPP_INVITE_MESSAGE_KEY, message);
		}

		/*
		 * WebView names and their respective urls for Rewards and Hike Extras will be controlled by server  
		 * 		 
		 */
		// Hike Extras
		if (data.has(HikeConstants.HIKE_EXTRAS_NAME))
		{
			String hikeExtrasName = data.getString(HikeConstants.HIKE_EXTRAS_NAME);
			editor.putString(HikeConstants.HIKE_EXTRAS_NAME, hikeExtrasName);
		}
		if (data.has(HikeConstants.HIKE_EXTRAS_URL))
		{
			String hikeExtrasUrl = data.getString(HikeConstants.HIKE_EXTRAS_URL);
			editor.putString(HikeConstants.HIKE_EXTRAS_URL, hikeExtrasUrl);
		}

		// Hike Rewards
		if (data.has(HikeConstants.REWARDS_NAME))
		{
			String rewards_name = data.getString(HikeConstants.REWARDS_NAME);
			editor.putString(HikeConstants.REWARDS_NAME, rewards_name);
		}
		if (data.has(HikeConstants.REWARDS_URL))
		{
			String rewards_url = data.getString(HikeConstants.REWARDS_URL);
			editor.putString(HikeConstants.REWARDS_URL, rewards_url);
		}

		editor.commit();
		this.pubSub.publish(HikePubSub.UPDATE_OF_MENU_NOTIFICATION, null);
	}

	private void saveRewards(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);

		int talkTime = data.getInt(HikeConstants.TALK_TIME);

		Editor editor = settings.edit();
		editor.putInt(HikeMessengerApp.TALK_TIME, talkTime);
		editor.commit();

		this.pubSub.publish(HikePubSub.TALK_TIME_CHANGED, talkTime);
	}

	private void saveAction(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		if (data.optBoolean(HikeConstants.POST_AB))
		{
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			List<ContactInfo> contactinfos = ContactManager.getInstance().getContacts(this.context);
			Map<String, List<ContactInfo>> contacts = ContactManager.getInstance().convertToMap(contactinfos);
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

	private void saveStatusUpdate(JSONObject jsonObj) throws JSONException
	{
		StatusMessage statusMessage = new StatusMessage(jsonObj);
		ContactManager conMgr = ContactManager.getInstance();
		/*
		 * This would be true for unsupported status message types. We should not be doing anything if we get one.
		 * 
		 * Also if the user is blocked, we ignore the message.
		 */
		if (statusMessage.getStatusMessageType() == null || conMgr.isBlocked(statusMessage.getMsisdn()))
		{
			return;
		}
		/*
		 * Applying the offset.
		 */
		statusMessage.setTimeStamp(Utils.applyServerTimeOffset(context, statusMessage.getTimeStamp()));

		ContactInfo contactInfo = conMgr.getContact(statusMessage.getMsisdn(), true, false);
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
			conMgr.setIcon(statusMessage.getMappedId(), Base64.decode(iconBase64, Base64.DEFAULT), false);
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
				if (!isBulkMessage) // do not autodownload in case of bulkmessage
				{
					autoDownloadProfileImage(statusMessage, true);
				}
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

	private void saveDeleteStatus(JSONObject jsonObj) throws JSONException
	{
		String statusId = jsonObj.getJSONObject(HikeConstants.DATA).getString(HikeConstants.STATUS_ID);
		pubSub.publish(HikePubSub.DELETE_STATUS, statusId);
	}

	private void savePostponeOrRemoveFavorite(JSONObject jsonObj) throws JSONException
	{
		String msisdn = jsonObj.getString(HikeConstants.FROM);
		removeOrPostponeFriendType(msisdn);
	}

	private void saveBatchStatusUpdate(JSONObject jsonObj) throws JSONException
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

	private void saveSticker(JSONObject jsonObj) throws JSONException
	{
		String subType = jsonObj.getString(HikeConstants.SUB_TYPE);
		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		if (HikeConstants.ADD_STICKER.equals(subType))
		{
			String categoryId = data.getString(StickerManager.CATEGORY_ID);
			int stickerCount = data.optInt(HikeConstants.COUNT, -1);
			int categorySize = data.optInt(HikeConstants.UPDATED_SIZE, -1);
			StickerManager.getInstance().updateStickerCategoryData(categoryId, true, stickerCount, categorySize);
		}
		else if (HikeConstants.REMOVE_STICKER.equals(subType) || HikeConstants.REMOVE_CATEGORY.equals(subType))
		{
			String categoryId = data.getString(StickerManager.CATEGORY_ID);
			if (HikeConstants.REMOVE_CATEGORY.equals(subType))
			{
				StickerManager.getInstance().removeCategory(categoryId);
			}
			else
			{
				JSONArray stickerIds = data.getJSONArray(HikeConstants.STICKER_IDS);

				for (int i = 0; i < stickerIds.length(); i++)
				{
					StickerManager.getInstance().removeSticker(categoryId, stickerIds.getString(i));
				}
				int stickerCount = data.optInt(HikeConstants.COUNT, -1);
				int categorySize = data.optInt(HikeConstants.UPDATED_SIZE, -1);
				/*
				 * We should not update updateAvailable field in this case
				 */
				StickerManager.getInstance().updateStickerCategoryData(categoryId, null, stickerCount, categorySize);
			}
		}
		else if (HikeConstants.SHOP.equals(subType))
		{
			boolean showBadge = data.optBoolean(HikeConstants.BADGE, false);
			/*
			 * reseting sticker shop update time so that next time we fetch a fresh sicker shop
			 */
			HikeSharedPreferenceUtil.getInstance(context).saveData(StickerManager.LAST_STICKER_SHOP_UPDATE_TIME, 0l);
			if(showBadge)
			{
				HikeSharedPreferenceUtil.getInstance(context).saveData(StickerManager.SHOW_STICKER_SHOP_BADGE, true);
			}
		}
		
		else if (HikeConstants.ADD_CATEGORY.equals(subType))
		{
			if((!data.has(StickerManager.CATEGORY_ID)) || (!data.has(HikeConstants.CAT_NAME)))
			{
				/**
				 * We are returning if we don't find category Id or Category name in the MQTT packet.
				 */
				
				Logger.d("SaveSticker", "Did not receive category Id and category Name. Returning");
				return;
			}
			
			String categoryId = data.getString(StickerManager.CATEGORY_ID);
			String categoryName = data.getString(HikeConstants.CAT_NAME);
			int stickerCount = data.optInt(HikeConstants.COUNT, -1);
			int categorySize = data.optInt(HikeConstants.UPDATED_SIZE, -1);
			int position = data.optInt(HikeConstants.PALLETE_POSITION, -1);
			
			/**
			 * Creating the sticker object here
			 */
			StickerCategory stickerCategory = new StickerCategory(categoryId);
			stickerCategory.setCategoryName(categoryName);
			stickerCategory.setTotalStickers(stickerCount == -1 ? 0 : stickerCount);
			stickerCategory.setCategorySize(categorySize == -1 ? 0 : categorySize);
			int pos = (position == -1 ? (HikeConversationsDatabase.getInstance().getMaxStickerCategoryIndex() + 1) : position);
			pos = (pos == -1 ? StickerManager.DEFAULT_POSITION : pos);
			stickerCategory.setCategoryIndex(pos);  //Choosing it's index based on the above logic
			stickerCategory.setUpdateAvailable(true);  //To show the green badge on category
			stickerCategory.setVisible(true);	//To make it visible in pallete
			stickerCategory.setState(StickerCategory.NONE);
			
			StickerManager.getInstance().addNewCategoryInPallete(stickerCategory);
		}
	}

	private void saveBulkLastSeen(JSONObject jsonObj) throws JSONException
	{
		Utils.handleBulkLastSeenPacket(context, jsonObj);
	}

	private void saveLastSeen(JSONObject jsonObj) throws JSONException
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

		ContactManager.getInstance().updateLastSeenTime(msisdn, lastSeenTime);
		ContactManager.getInstance().updateIsOffline(msisdn, (int) isOffline);
		ContactInfo contact = ContactManager.getInstance().getContact(msisdn, true, true);
		pubSub.publish(HikePubSub.LAST_SEEN_TIME_UPDATED, contact);
	}

	private void saveServerTimestamp(JSONObject jsonObj) throws JSONException
	{
		long serverTimestamp = jsonObj.getLong(HikeConstants.TIMESTAMP);
		long diff = (System.currentTimeMillis() / 1000) - serverTimestamp;

		Logger.d(getClass().getSimpleName(), "Diff b/w server and client: " + diff);

		Editor editor = settings.edit();
		editor.putLong(HikeMessengerApp.SERVER_TIME_OFFSET, diff);
		editor.commit();
	}

	private void saveProtip(JSONObject jsonObj) throws JSONException
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
				ContactManager.getInstance().setIcon(protip.getMappedId(), Base64.decode(iconBase64, Base64.DEFAULT), false);
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

	private void saveUpdatePush(JSONObject jsonObj) throws JSONException
	{
		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		String devType = data.optString(HikeConstants.DEV_TYPE);
		String id = data.optString(HikeConstants.MESSAGE_ID);
		String lastPushPacketId = settings.getString(HikeConstants.Extras.LAST_UPDATE_PACKET_ID, "");
		if (!TextUtils.isEmpty(devType) && devType.equals(HikeConstants.ANDROID) && !TextUtils.isEmpty(id) && !lastPushPacketId.equals(id))
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

	private void saveApplicationsPush(JSONObject jsonObj) throws JSONException
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

	private void saveChatBackground(JSONObject jsonObj) throws JSONException
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
			 * This exception is thrown for unknown themes. Show an unsupported message Now in this case, we don't do anything. if user doesn't have certain theme that chatthread
			 * will keep on current applied theme.
			 */
		}
	}

	private void saveGroupOwnerChange(JSONObject jsonObj) throws JSONException
	{
		String groupId = jsonObj.getString(HikeConstants.TO);

		JSONObject data = jsonObj.getJSONObject(HikeConstants.DATA);
		String msisdn = data.getString(HikeConstants.MSISDN);

		convDb.changeGroupOwner(groupId, msisdn);
	}

	private void saveRequestDP(JSONObject jsonObj) throws JSONException
	{
		final String groupId = jsonObj.getString(HikeConstants.TO);
		uploadGroupProfileImage(groupId, true);
	}

	private void savePopup(JSONObject jsonObj) throws JSONException
	{
		if (jsonObj.getString(HikeConstants.SUB_TYPE).equals(HikeConstants.SHOW_STEALTH_POPUP))
		{
			JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
			String id = data.optString(HikeConstants.MESSAGE_ID);
			String lastPushPacketId = settings.getString(HikeConstants.Extras.LAST_STEALTH_POPUP_ID, "");

			if (!TextUtils.isEmpty(id))
			{
				if (lastPushPacketId.equals(id))
				{
					Logger.d(getClass().getSimpleName(), "Duplicate popup packet ! Gotcha");
					return;
				}
			}
			else
			{
				Logger.d(getClass().getSimpleName(), "Returning with empty packet Id");
				return; // empty packet id : ignore this packet
			}

			String header = data.optString(HikeConstants.HEADER);
			String body = data.optString(HikeConstants.BODY);

			if (!TextUtils.isEmpty(header) && !TextUtils.isEmpty(body))
			{
				Editor editor = settings.edit();
				editor.putString(HikeMessengerApp.STEALTH_UNREAD_TIP_HEADER, data.optString(HikeConstants.HEADER));
				editor.putString(HikeMessengerApp.STEALTH_UNREAD_TIP_MESSAGE, data.optString(HikeConstants.BODY));
				editor.putBoolean(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP, true);
				editor.putString(HikeMessengerApp.LAST_STEALTH_POPUP_ID, id);
				editor.commit();

				if (data.optBoolean(HikeConstants.PUSH, true)) // Toast this only if the push flag is true
				{
					Bundle bundle = new Bundle();
					bundle.putString(HikeConstants.Extras.STEALTH_PUSH_BODY, body);
					bundle.putString(HikeConstants.Extras.STEALTH_PUSH_HEADER, header);
					this.pubSub.publish(HikePubSub.STEALTH_POPUP_WITH_PUSH, bundle);
				}
			}
		}
		else
		{
			// updatePopUpData
			updateAtomicPopUpData(jsonObj);
		}
	}

	private void saveTip(JSONObject jsonObj)
	{
		String subType = jsonObj.optString(HikeConstants.SUB_TYPE);
		if (subType.equals(HikeConstants.STICKER))
		{
			Editor editor = settings.edit();
			if (!settings.contains(HikeMessengerApp.SHOWN_EMOTICON_TIP))
				editor.putBoolean(HikeMessengerApp.SHOWN_EMOTICON_TIP, false);
			editor.commit();
			HikeMessengerApp.getPubSub().publish(HikePubSub.STICKER_FTUE_TIP, null);
		}
	}
	
	/**
	 * <br>
	 * This function handles bulk packet</br>
	 * 
	 * @param bulkObj
	 *            - bulk json object of type "bm"
	 * @throws JSONException
	 */

	public void saveBulkMessage(JSONObject bulkObj) throws JSONException
	{
		boolean shouldFallBackToNormal = false;
		JSONObject bulkMessages = bulkObj.optJSONObject(HikeConstants.DATA);

		if (bulkMessages != null)
		{
			JSONArray msgArray = bulkMessages.optJSONArray(HikeConstants.MESSAGES);
			if (null != msgArray && msgArray.length() > 0)
			{
				isBulkMessage = true;
				int i = 0;
				int length = msgArray.length();
				Logger.d("bulkpacket", "length of json array : " + length);

				Logger.d("BulkProcess", "started");
				long time1 = System.currentTimeMillis();

				/*
				 * Initialize all the datastructures used for bulk packet processing
				 */
				messageList = new LinkedList<ConvMessage>(); // it will store all the convMessage object that can be added to list in one transaction
				messageListMap = new HashMap<String, LinkedList<ConvMessage>>(); // it will store list of conversation objects based on msisdn
				messageStatusMap = new HashMap<String, PairModified<PairModified<Long, Set<String>>, Long>>(); // it will store pair mapping to msisdn. pair first value is a pair
																												// which contains max "mr" msgid and msisdns of participants that
																												// read it. // pair second value is max "dr" message id

				try
				{
					userWriteDb.beginTransaction();
					convWriteDb.beginTransaction();

					while (i < length)
					{
						JSONObject jsonObj = msgArray.optJSONObject(i++);
						if (jsonObj != null)
						{
							saveMqttMessage(jsonObj);
						}
					}
					Logger.d("BulkProcess", "going on");
					finalProcessing();
					convWriteDb.setTransactionSuccessful();
					userWriteDb.setTransactionSuccessful();
				}
				catch (JSONException e)
				{
					throw e;
				}
				catch (Exception e)
				{
					Logger.e("BulkProcessor", "Exception during processing ", e);
					shouldFallBackToNormal = true; // fallback to one message processing
				}
				finally
				{
					convWriteDb.endTransaction();
					userWriteDb.endTransaction();

					Logger.d("BulkProcess", "stopped");
					isBulkMessage = false;
					Logger.d("bulkPacket", "total time : " + (System.currentTimeMillis() - time1));
				}

				/*
				 * If there is some exception processing bulk packet we fallback to normal single message processing of bulk packet messages
				 */
				if (shouldFallBackToNormal)
				{
					i = 0;
					while (i < length)
					{
						JSONObject jsonObj = msgArray.optJSONObject(i++);
						if (jsonObj != null)
						{
							saveMqttMessage(jsonObj);
						}
					}
				}

			}
		}
	}

	private void finalProcessing() throws JSONException
	{

		/*
		 * The list returned by {@link HikeConversationsDatabase#addConversationsBulk(List<ConvMessages>)} contains non duplicate messages This list is used for further processing
		 */

		if (messageList.size() > 0)
		{
			messageList = convDb.addConversationsBulk(messageList);
		}

		/*
		 * lastPinMap is map of msisdn to a pair containing last pin message for a conversation and count of total number of pin messages in bulk packet for that conversation
		 */
		HashMap<String, PairModified<ConvMessage, Integer>> lastPinMap = new HashMap<String, PairModified<ConvMessage, Integer>>();

		for (ConvMessage convMessage : messageList)
		{
			String msisdn = convMessage.getMsisdn();
			if (messageListMap.get(msisdn) == null)
			{
				messageListMap.put(msisdn, new LinkedList<ConvMessage>());
			}
			messageListMap.get(msisdn).add(convMessage); // adds each message into messageListMap according to msisdn

			if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			{
				if (lastPinMap.get(msisdn) == null)
				{
					lastPinMap.put(msisdn, new PairModified<ConvMessage, Integer>(null, 0));
				}
				lastPinMap.get(msisdn).setFirst(convMessage); // update last pin message for a msisdn
				lastPinMap.get(msisdn).setSecond(lastPinMap.get(msisdn).getSecond() + 1); // increment pin unread count for a msisdn
			}
		}

		/*
		 * Increment unread count for each msisdn/groupId
		 */
		if (messageListMap.size() > 0)
		{
			convDb.incrementUnreadCountBulk(messageListMap);
		}

		/*
		 * contains last message for each conversation to be added to conversation table
		 */
		ArrayList<ConvMessage> lastMessageList = new ArrayList<ConvMessage>(messageListMap.keySet().size());
		for (Entry<String, LinkedList<ConvMessage>> entry : messageListMap.entrySet())
		{
			LinkedList<ConvMessage> list = entry.getValue();
			if (list.size() > 0)
			{
				lastMessageList.add(list.get(list.size() - 1));
			}
		}

		/*
		 * add last message and last pin and unread pin count to conversation table
		 */
		if (lastMessageList.size() > 0)
		{
			convDb.addLastConversations(lastMessageList, lastPinMap);
		}

		/*
		 * update status of messages and also update readByString in group info table for each group
		 */
		if (messageStatusMap.size() > 0)
		{
			convDb.updateStatusBulk(messageStatusMap);
			convDb.setReadByForGroupBulk(messageStatusMap);
		}

		/*
		 * Since now messages contains message id and conversation object we can process ft messages
		 */
		for (ConvMessage convMessage : messageList)
		{
			messageProcessFT(convMessage);
		}

		/*
		 * publish the events for updating chat thread and conversation table
		 */
		this.pubSub.publish(HikePubSub.BULK_MESSAGE_RECEIVED, messageListMap);
		this.pubSub.publish(HikePubSub.BULK_MESSAGE_DELIVERED_READ, messageStatusMap);
		this.pubSub.publish(HikePubSub.BULK_MESSAGE_NOTIFICATION,messageListMap);
	}

	private void addToLists(String msisdn, ConvMessage convMessage)
	{
		messageList.add(convMessage);
	}

	public void saveMqttMessage(JSONObject jsonObj) throws JSONException
	{
		String type = jsonObj.optString(HikeConstants.TYPE);
		if (HikeConstants.MqttMessageTypes.ICON.equals(type)) // Icon changed
		{
			saveIcon(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.DISPLAY_PIC.equals(type))
		{
			saveDisplayPic(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.SMS_CREDITS.equals(type)) // Credits
		// changed
		{
			saveSMSCredits(jsonObj);
		}
		else if ((HikeConstants.MqttMessageTypes.USER_JOINED.equals(type)) || (HikeConstants.MqttMessageTypes.USER_LEFT.equals(type))) // User
		// joined/left
		{
			saveUserJoinedOrLeft(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.INVITE_INFO.equals(type)) // Invite
		// info
		{
			saveInviteInfo(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_JOIN.equals(type)) // Group
		// chat
		// join
		{
			saveGCJoin(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE.equals(type)) // Group
		// chat
		// leave
		{
			saveGCLeave(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_NAME.equals(type)) // Group
		// chat
		// name
		// change
		{
			saveGCName(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_CHAT_END.equals(type)) // Group
		// chat
		// end
		{
			saveGCEnd(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE.equals(type)) // Message
		// received
		// from
		// server
		{
			if (isBulkMessage)
			{
				saveMessageBulk(jsonObj);
			}
			else
			{
				saveMessage(jsonObj);
			}
		}
		else if (HikeConstants.MqttMessageTypes.DELIVERY_REPORT.equals(type)) // Message
		// delivered
		// to
		// receiver
		{
			if (isBulkMessage)
			{
				saveDeliveryReportBulk(jsonObj);
			}
			else
			{
				saveDeliveryReport(jsonObj);
			}
		}
		else if (HikeConstants.MqttMessageTypes.MESSAGE_READ.equals(type)) // Message
		// has
		// been
		// read
		{
			if (isBulkMessage)
			{
				saveMessageReadBulk(jsonObj);
			}
			else
			{
				saveMessageRead(jsonObj);
			}
		}
		else if (HikeConstants.MqttMessageTypes.START_TYPING.equals(type) || HikeConstants.MqttMessageTypes.END_TYPING.equals(type))
		{
			saveTyping(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.UPDATE_AVAILABLE.equals(type))
		{
			saveUpdateAvailable(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ACCOUNT_INFO.equals(type))
		{
			saveAccountInfo(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.USER_OPT_IN.equals(type))
		{
			saveUserOptIn(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.BLOCK_INTERNATIONAL_SMS.equals(type))
		{
			saveBlockInternationalSMS(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ADD_FAVORITE.equals(type))
		{
			saveAddFavorite(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG.equals(type))
		{
			saveAccountConfig(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.REWARDS.equals(type))
		{
			saveRewards(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.ACTION.equals(type))
		{
			saveAction(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.STATUS_UPDATE.equals(type))
		{
			saveStatusUpdate(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.DELETE_STATUS.equals(type))
		{
			saveDeleteStatus(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.POSTPONE_FAVORITE.equals(type) || HikeConstants.MqttMessageTypes.REMOVE_FAVORITE.equals(type))
		{
			savePostponeOrRemoveFavorite(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.BATCH_STATUS_UPDATE.equals(type))
		{
			saveBatchStatusUpdate(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.STICKER.equals(type))
		{
			saveSticker(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.BULK_LAST_SEEN.equals(type))
		{
			saveBulkLastSeen(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.LAST_SEEN.equals(type))
		{
			saveLastSeen(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.SERVER_TIMESTAMP.equals(type))
		{
			saveServerTimestamp(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.PROTIP.equals(type))
		{
			saveProtip(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.UPDATE_PUSH.equals(type))
		{
			saveUpdatePush(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.APPLICATIONS_PUSH.equals(type))
		{
			saveApplicationsPush(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.CHAT_BACKGROUD.equals(type))
		{
			saveChatBackground(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.GROUP_OWNER_CHANGE.equals(type))
		{
			saveGroupOwnerChange(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.REQUEST_DP.equals(type))
		{
			saveRequestDP(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.POPUP.equals(type))
		{
			savePopup(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.REMOVE_PIC.equals(type))
		{
			String msisdn = jsonObj.getString(HikeConstants.FROM);
			if (HikeMessengerApp.getLruCache().deleteIconForMSISDN(msisdn))
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.ICON_CHANGED, msisdn);
			}
		}
		else if (HikeConstants.MqttMessageTypes.BULK_MESSAGE.equals(type))
		{
			saveBulkMessage(jsonObj);
		}
		else if (HikeConstants.MqttMessageTypes.TIP.equals(type))
		{
			saveTip(jsonObj);
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
		ContactInfo contactInfo = HikeMessengerApp.getContactManager().getContact(msisdn, true, true);
		if (contactInfo.getFavoriteType() == FavoriteType.NOT_FRIEND)
		{
			return;
		}
		FavoriteType currentFavoriteType = contactInfo.getFavoriteType();
		FavoriteType favoriteType = (currentFavoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED || currentFavoriteType == FavoriteType.REQUEST_RECEIVED) ? FavoriteType.NOT_FRIEND
				: FavoriteType.REQUEST_SENT_REJECTED;

		ContactInfo contact = new ContactInfo(contactInfo);

		ContactInfo updatedContact = new ContactInfo(contactInfo);
		updatedContact.setFavoriteType(favoriteType);
		ContactManager.getInstance().updateContacts(updatedContact);

		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, ContactInfo.FavoriteType>(contact, favoriteType);
		this.pubSub.publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
	}

	private void incrementUnseenStatusCount()
	{
		int count = settings.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0);
		count++;
		Editor editor = settings.edit();
		editor.putInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, count);
		editor.putBoolean(HikeConstants.IS_HOME_OVERFLOW_CLICKED, false);
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
		if (isBulkMessage)
		{
			ConvMessage convMessage = saveStatusMsgBulk(jsonObj, msisdn);
			return convMessage;
		}
		ConvMessage convMessage = statusMessagePreProcess(jsonObj, msisdn);
		
		if (convMessage == null)
		{
			return null;
		}
		
		convDb.addConversationMessages(convMessage);
		
		this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		
		statusMessagePostProcess(convMessage, jsonObj);
		
		return convMessage;
	}
	
	private ConvMessage saveStatusMsgBulk(JSONObject jsonObj, String msisdn) throws JSONException
	{
		ConvMessage convMessage = statusMessagePreProcess(jsonObj, msisdn);

		if (convMessage == null)
		{
			return null;
		}

		addToLists(msisdn, convMessage);

		statusMessagePostProcess(convMessage, jsonObj);

		return convMessage;
	}

	private ConvMessage statusMessagePreProcess(JSONObject jsonObj, String msisdn) throws JSONException
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
			if ((conversation == null && (!isUJMsg || !ContactManager.getInstance().doesContactExist(msisdn)))
					|| (conversation != null && TextUtils.isEmpty(conversation.getContactName()) && isUJMsg && !isGettingCredits && !(conversation instanceof GroupConversation)))
			{
				return null;
			}
		}
		ConvMessage convMessage = new ConvMessage(jsonObj, conversation, context, false);
		if(Utils.isGroupConversation(convMessage.getMsisdn()))
		{
			ContactManager.getInstance().updateGroupRecency(convMessage.getMsisdn(), convMessage.getTimestamp());
		}
		return convMessage;
	}

	private void statusMessagePostProcess(ConvMessage convMessage, JSONObject jsonObj) throws JSONException
	{
		if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED || convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT
				|| convMessage.getParticipantInfoState() == ParticipantInfoState.GROUP_END)
		{
			this.pubSub.publish(
					convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED ? HikePubSub.PARTICIPANT_JOINED_GROUP
							: convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT ? HikePubSub.PARTICIPANT_LEFT_GROUP : HikePubSub.GROUP_END, jsonObj);
		}
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

			/*
			 * We only publish this event if we actually removed a typing notification
			 */
			this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, typingNotification);
		}
	}

	/**
	 * We call it atomic pop up , as we discard old if any when new comes --gauravKhanna
	 * 
	 * @param jsonObject
	 *            - jsonFromServer
	 * @throws JSONException
	 */
	public void updateAtomicPopUpData(JSONObject jsonObj) throws JSONException
	{
		Logger.i("tip", jsonObj.toString());
		String subType = jsonObj.getString(HikeConstants.SUB_TYPE);

		JSONObject data = jsonObj.optJSONObject(HikeConstants.DATA);
		if (isDuplicateOrWrongPacket("last" + subType, data))
		{
			return;
		}
		Logger.i("tip", "id passed");
		String header = data.optString(HikeConstants.HEADER);
		String body = data.optString(HikeConstants.BODY);
		if (!TextUtils.isEmpty(header) && !TextUtils.isEmpty(body))
		{
			HikeSharedPreferenceUtil pref = HikeSharedPreferenceUtil.getInstance(context);
			pref.saveData("last" + subType, data.getString(HikeConstants.MESSAGE_ID));
			String[] keys = getPopUpTypeAndShowNotification(subType, (data.optBoolean(HikeConstants.PUSH, true) ? body : null));
			if (keys != null)
			{
				pref.saveData(keys[0], header);
				pref.saveData(keys[1], body);
				pref.saveData(keys[2], subType);
				String url = data.optString(HikeConstants.URL);
				// for http based generic URL
				if(!TextUtils.isEmpty(url) && HikeMessengerApp.ATOMIC_POP_UP_HTTP.equals(subType)){
				pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_HTTP_URL, url);
				}else if(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC.equals(subType)){
					// for app specific generic tip
					String what = data.optString(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC_WHAT);
					if(!TextUtils.isEmpty(what)){
						try{
						pref.saveData(HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC_WHAT, Integer.parseInt(what));
						}catch(NumberFormatException nf){
							nf.printStackTrace();
							// don know where to go on click, lets remove key so tip id not displayed
							pref.saveData(keys[0], "");
						}
					}
				}
				Logger.i("tip", "writing to pref passed " + header + " -- " + body + " -- subtype " + subType);
			}else{
				Logger.i("tip", "writing to pref failed , could not find keys  " );
			}
		}
		else
		{
			Logger.i("tip", "header message failed " + header + " -- " + body);
		}
	}

	/**
	 * Since we use over write mechanism per screen for tips , say tip 1 and tip2 arrives for main screen , so we over write tip1 and save only tip2
	 * 
	 * 
	 * @param subType
	 *            -- subtype which comes from server
	 * @return String array for header ,message and subtype keys , 0 is header , 1 is message , 2 is subtypekey
	 * 
	 *         in addition it shows notification whereever applicable
	 */
	private String[] getPopUpTypeAndShowNotification(String subType, String notificationTextIfApplicable)
	{
		// for chat screen
		if (HikeMessengerApp.ATOMIC_POP_UP_ATTACHMENT.equals(subType) || HikeMessengerApp.ATOMIC_POP_UP_STICKER.equals(subType)
				|| HikeMessengerApp.ATOMIC_POP_UP_THEME.equals(subType))
		{
			Logger.i("tip", "subtype for chat");
			return new String[] { HikeMessengerApp.ATOMIC_POP_UP_HEADER_CHAT, HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_CHAT, HikeMessengerApp.ATOMIC_POP_UP_TYPE_CHAT };
		}

		// for main screen
		Logger.i("tip", "subtype for main");
		if (HikeMessengerApp.ATOMIC_POP_UP_FAVOURITES.equals(subType) || HikeMessengerApp.ATOMIC_POP_UP_INVITE.equals(subType)
				|| HikeMessengerApp.ATOMIC_POP_UP_PROFILE_PIC.equals(subType) || HikeMessengerApp.ATOMIC_POP_UP_STATUS.equals(subType)
				|| HikeMessengerApp.ATOMIC_POP_UP_INFORMATIONAL.equals(subType) || HikeMessengerApp.ATOMIC_POP_UP_HTTP.equals(subType)
				|| HikeMessengerApp.ATOMIC_POP_UP_APP_GENERIC.equals(subType))
		{
			// show notification
			if (notificationTextIfApplicable != null)
			{
				Bundle bundle = new Bundle();
				bundle.putString(HikeMessengerApp.ATOMIC_POP_UP_NOTIF_MESSAGE, notificationTextIfApplicable);
				bundle.putString(HikeMessengerApp.ATOMIC_POP_UP_NOTIF_SCREEN, HomeActivity.class.getName());
				this.pubSub.publish(HikePubSub.ATOMIC_POPUP_WITH_PUSH, bundle);

			}
			return new String[] { HikeMessengerApp.ATOMIC_POP_UP_HEADER_MAIN, HikeMessengerApp.ATOMIC_POP_UP_MESSAGE_MAIN, HikeMessengerApp.ATOMIC_POP_UP_TYPE_MAIN };

		}
		Logger.i("tip", "subtype for nothing , it shud not reach here");
		// it will not reach here
		return null;
	}

	private boolean isDuplicateOrWrongPacket(String key, JSONObject jsonObject)
	{
		String id = jsonObject.optString(HikeConstants.MESSAGE_ID);
		return TextUtils.isEmpty(id) || HikeSharedPreferenceUtil.getInstance(context).getData(key, "").equals(id);
	}
}