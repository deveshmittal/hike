package com.bsb.hike.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.MqttMessageTypes;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.filetransfer.FileTransferManager;
import com.bsb.hike.filetransfer.FileTransferManager.NetworkType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.MessageMetadata;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.StatusMessage.StatusMessageType;
import com.bsb.hike.tasks.DownloadProfileImageTask;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class BulkMessageProcessor 
{

	private Context context;

	private HikePubSub pubSub;

	private SharedPreferences appPrefs;

	private MqttMessagesManager mqttMessageManager;

	HashMap<String, HashMap<String, ArrayList<Object>>> mqttMessageMap;

	private HikeConversationsDatabase convDb;

	private HikeUserDatabase userDb;
	
	private SharedPreferences settings;

	private ArrayList<ConvMessage> messageList;

	private Map<String, ArrayList<ConvMessage>> messageListMap;
	
	private Map<String, Pair<Long, Long>> messageStatusMap;
	

	private boolean vibrate = false;

	public BulkMessageProcessor(Context context, JSONObject bulkObject) throws JSONException
	{
		this.context = context.getApplicationContext();
		this.pubSub = HikeMessengerApp.getPubSub();
		this.appPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		mqttMessageManager = MqttMessagesManager.getInstance(context);
		this.convDb = HikeConversationsDatabase.getInstance();
		this.userDb = HikeUserDatabase.getInstance();
		initLists();
		initMaps();
		breakBulkPacket(bulkObject);
	}

	/*
	 * This function will create all the list objects
	 */
	private void initLists()
	{
		messageList = new ArrayList<ConvMessage>();   //  it will store all the convMessage object that can be added to list in one transaction
	}
	
	/*
	 * This function will create all the map objects
	 */
	private void initMaps()
	{
		messageListMap = new HashMap<String, ArrayList<ConvMessage>>(); //  it will store list of conversation objects based on msisdn
		messageStatusMap = new HashMap<String, BulkMessageProcessor.Pair<Long,Long>>();  //  it will store pair max "mr" msdId and max "dr" msgId according to msisdn
	}


	/*
	 * This function breaks the bulk message and gives individiual packets to process() fuction
	 */
	private void breakBulkPacket(JSONObject bulkObject) throws JSONException
	{
		JSONObject bulkMessages = bulkObject.optJSONObject(HikeConstants.DATA);

		if (bulkMessages != null)
		{
			JSONArray msgArray = bulkMessages.optJSONArray("msgs");
			if (null != msgArray && msgArray.length() > 0)
			{
				int i = 0;
				int length = msgArray.length();
				ArrayList<Object> messageList = new ArrayList<Object>();
				while (i < length)
				{
					JSONObject jsonObj = msgArray.optJSONObject(i++);
					if (jsonObj != null)
					{
						process(jsonObj);
					}
				}
				finalProcessing();
			}
		}
	}

	/*
	 * This function is like saveMqttMessage function which does processing on individual packets
	 */
	private void process(JSONObject jsonObj) throws JSONException
	{
		String type = jsonObj.optString(HikeConstants.TYPE);

		if (HikeConstants.MqttMessageTypes.MESSAGE.equals(type))
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

			/*
			 * commented out code because of bulk message
			 */
			// convDb.addConversationMessages(convMessage);

			addToLists(convMessage.getMsisdn(), convMessage);

			/*
			 * Return if there is no conversation mapped to this message
			 */
			
			/*
			 * commented out because of update packet
			 */
//			if (convMessage.getConversation() == null)
//			{
//				return;
//			}

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

					/*
					 * commented because of the bulk packet
					 */
					// if (vibrate)
					// {
					// Utils.vibrateNudgeReceived(context);
					// }
				}
			}
			Logger.d(getClass().getSimpleName(), "Receiver received Message : " + convMessage.getMessage() + "		;	Receiver Msg ID : " + convMessage.getMsgID()
					+ "	; Mapped msgID : " + convMessage.getMappedMsgID());
			// We have to do publish this here since we are adding the message
			// to the db here, and the id is set after inserting into the db.

			// commented out code because of bulk packet
			// this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
			//
			// if (convMessage.isGroupChat() && convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
			// {
			// this.pubSub.publish(HikePubSub.SHOW_PARTICIPANT_STATUS_MESSAGE, convMessage.getMsisdn());
			// }

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
		}
		
		else if (HikeConstants.MqttMessageTypes.DELIVERY_REPORT.equals(type))
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
			 * commented because of bulk packet
			 */
			// int rowsUpdated = updateDB(msgID, ConvMessage.State.SENT_DELIVERED, msisdn);
			//
			// if (rowsUpdated == 0)
			// {
			// Logger.d(getClass().getSimpleName(), "No rows updated");
			// return;
			// }
			
			
			/*
			 * update message status map with max dr msgId corresponding to its msisdn
			 */
			if(messageStatusMap.get(msisdn) == null)
			{
				messageStatusMap.put(msisdn, new Pair<Long, Long>((long) -1, (long) -1));
			}
			if(msgID > messageStatusMap.get(msisdn).getSecond())
			{
				messageStatusMap.get(msisdn).setSecond(msgID);
			}
			
			/*
			 * commented out because of bulk packet
			 */
			//this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED, pair);
		}
		
		else if (HikeConstants.MqttMessageTypes.MESSAGE_READ.equals(type))
		{
			// to do
			

			JSONArray msgIds = jsonObj.optJSONArray(HikeConstants.DATA);
			String id = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.TO) : jsonObj.getString(HikeConstants.FROM);

			String participantMsisdn = jsonObj.has(HikeConstants.TO) ? jsonObj.getString(HikeConstants.FROM) : "";
			
			long msgID = -1;

			if (msgIds == null)
			{
				Logger.e(getClass().getSimpleName(), "Update Error : Message id Array is empty or null . Check problem");
				return;
			}
			
			for (int i = 0; i < msgIds.length(); i++)
			{
				long tempId = msgIds.optLong(i);
				if(msgIds.optLong(i) > msgID)
				{
					msgID = msgIds.optLong(i);
				}
			}
			
			/*
			 * commented out because of bulk packet
			 */
//			long[] ids;
//			if (!Utils.isGroupConversation(id))
//			{
//				ids = convDb.setAllDeliveredMessagesReadForMsisdn(id, msgIds);
//				if (ids == null)
//				{
//					return;
//				}
//			}
//			else
//			{
//				ids = new long[msgIds.length()];
//				for (int i = 0; i < msgIds.length(); i++)
//				{
//					ids[i] = msgIds.optLong(i);
//				}
//				convDb.setReadByForGroup(id, ids, participantMsisdn);
//			}
			
			// to do seperate handling for group and one to one chat
			
			/*
			 * update message status map with max dr msgId corresponding to its msisdn
			 */
			if(messageStatusMap.get(id) == null)
			{
				messageStatusMap.put(id, new Pair<Long, Long>((long) -1, (long) -1));
			}
			if(msgID > messageStatusMap.get(id).getFirst())
			{
				messageStatusMap.get(id).setFirst(msgID);
			}
		}
		
		else if((HikeConstants.MqttMessageTypes.USER_JOINED.equals(type)) || (HikeConstants.MqttMessageTypes.USER_LEFT.equals(type)))
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

			/*
			 * commented out because of bulk packet
			 */
			//this.pubSub.publish(joined ? HikePubSub.USER_JOINED : HikePubSub.USER_LEFT, msisdn);
		}
		
		else if(type.equals(MqttMessageTypes.USER_OPT_IN))
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

			android.util.Pair<ChatTheme, Long> chatThemedata = convDb.getChatThemeAndTimestamp(id);

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

				/*
				 * commented out because of bulk packet
				 */
				//this.pubSub.publish(HikePubSub.CHAT_BACKGROUND_CHANGED, new Pair<String, ChatTheme>(id, chatTheme));

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
		
			else
			{
				mqttMessageManager.saveMqttMessage(jsonObj);
			}
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
		
		addToLists(msisdn, convMessage);
		
		
		if (convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED || convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT
				|| convMessage.getParticipantInfoState() == ParticipantInfoState.GROUP_END)
		{
			this.pubSub.publish(
					convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_JOINED ? HikePubSub.PARTICIPANT_JOINED_GROUP
							: convMessage.getParticipantInfoState() == ParticipantInfoState.PARTICIPANT_LEFT ? HikePubSub.PARTICIPANT_LEFT_GROUP : HikePubSub.GROUP_END, jsonObj);
		}
		return convMessage;
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

	private void addToLists(String msisdn, ConvMessage convMessage)
	{
		messageList.add(convMessage);
		if(messageListMap.get(msisdn) == null)
		{
			messageListMap.put(msisdn, new ArrayList<ConvMessage>());
		}
		messageListMap.get(msisdn).add(convMessage);
	}
	
	
	/*
	 * This function will perform all the final processing : 
	 * 1. Adding message list objects to db in one transaction
	 * 2. update message state of all "dr" and "mr"msgids in db in one transaction
	 * 3. Calling pubsub to update ui
	 * 4. messageListMap and messageStatusMap will be passed to both chat thread and conversation fragment 
	 *
	 */
	
	private void finalProcessing()
	{
		if(messageList.size() > 0)
		{
			convDb.addConversations(messageList);
		}
		if(messageStatusMap.size() > 0)
		{
			convDb.updateStatusBulk(messageStatusMap);
		}
		
		//TODO publish to ui
		if (vibrate)
		{
			Utils.vibrateNudgeReceived(context);
		}
		this.pubSub.publish(HikePubSub.BULK_MESSAGE_RECEIVED, messageListMap);
	}
	
	/*
	 * This is generic pair class. We have created our own because android pair objects have final first and second fields so we can't change them
	 * but since our implementation requires modifiable first and second fields we have created this class
	 */
	public static class Pair<L, R>
	{
		private L l;

		private R r;

		public Pair(L l, R r)
		{
			this.l = l;
			this.r = r;
		}

		public L getFirst()
		{
			return l;
		}

		public R getSecond()
		{
			return r;
		}

		public void setFirst(L l)
		{
			this.l = l;
		}

		public void setSecond(R r)
		{
			this.r = r;
		}
	}


}
