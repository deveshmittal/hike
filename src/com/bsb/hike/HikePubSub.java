package com.bsb.hike;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

public class HikePubSub implements Runnable
{
	public class Operation
	{
		public Operation(String type, Object o)
		{
			this.type = type;
			this.payload = o;
		}

		public final String type;

		public final Object payload;
	}

	public interface Listener
	{
		public void onEventReceived(String type, Object object);
	}

	private static final Operation DONE_OPERATION = null; /*
														 * TODO this can't be null
														 */

	/*
	 * broadcast when the sender sends the message (click the send button in chat thread view)
	 */
	public static final String MESSAGE_SENT = "messagesent";

	public static final String MESSAGE_DELIVERED = "messageDelivered"; // represents
																		// that
																		// msg
																		// is
																		// delivered
																		// to
																		// receiver
																		// but
																		// is
																		// not
																		// read.

	public static final String MESSAGE_DELIVERED_READ = "messageDeliveredRead"; // represents
																				// that
																				// msg
																				// is
																				// delivered
																				// to
																				// receiver
																				// and
																				// is
																				// read
																				// by
																				// the
																				// same.
	
	public static final String GROUP_MESSAGE_DELIVERED_READ = "groupMessageDeliveredRead";

	public static final String WS_CLOSE = "ws_close";

	public static final String WS_OPEN = "ws_open";

	// public static final String WS_SEND = "ws_send";

	public static final String NEW_CONVERSATION = "newconv";

	/*
	 * Broadcast after we've received a message and written it to our DB. Status is RECEIVED_UNREAD
	 */
	public static final String MESSAGE_RECEIVED = "messagereceived";

	public static final String NEW_ACTIVITY = "new_activity";

	public static final String END_TYPING_CONVERSATION = "endtypingconv";

	public static final String TYPING_CONVERSATION = "typingconv";

	public static final String TOKEN_CREATED = "tokencreated";

	/* sms credits have been modified */
	public static final String SMS_CREDIT_CHANGED = "smscredits";

	/*
	 * broadcast when the server receives the message and replies with a confirmation
	 */
	public static final String SERVER_RECEIVED_MSG = "serverReceivedMsg";

	/*
	 * broadcast when a message is received from the sender but before it's been written our DB
	 */
	public static final String MESSAGE_RECEIVED_FROM_SENDER = "messageReceivedFromSender";

	public static final String MSG_READ = "msgRead";

	/* publishes a message via mqtt to the server */
	public static final String MQTT_PUBLISH = "serviceSend";

	/* publishes a message via mqtt to the server with QoS 0 */
	public static final String MQTT_PUBLISH_LOW = "serviceSendLow";

	/* published when the app is asked to delete a message */
	public static final String DELETE_MESSAGE = "deleteMessage";

	/* published when the clients has deleted the message */
	public static final String LAST_MESSAGE_DELETED = "lastMessageDeleted";

	public static final String MESSAGE_FAILED = "messageFailed";

	public static final String CONNECTION_STATUS = "connStatus";

	public static final String BLOCK_USER = "blockUser";

	public static final String UNBLOCK_USER = "unblockUser";

	public static final String ICON_CHANGED = "iconChanged";

	public static final String USER_JOINED = "userJoined";

	public static final String USER_LEFT = "userLeft";

	public static final String GROUP_LEFT = "groupLeft";

	public static final String GROUP_END = "groupEnd";

	public static final String GROUP_NAME_CHANGED = "groupNameChanged";

	public static final String PARTICIPANT_JOINED_GROUP = "participantJoinedGroup";

	public static final String PARTICIPANT_LEFT_GROUP = "participantLeftGroup";

	public static final String INVITEE_NUM_CHANGED = "inviteeNoChanged";

	public static final String INVITE_TOKEN_ADDED = "inviteTokenAdded";

	public static final String CONTACT_ADDED = "contactAdded";

	public static final String UPLOAD_FINISHED = "uploadFinished";

	public static final String FILE_TRANSFER_PROGRESS_UPDATED = "fileTransferProgressUpdated";

	// public static final String RESUME_BUTTON_UPDATED = "resumeButtonUpdated";

	public static final String SHOW_PARTICIPANT_STATUS_MESSAGE = "showParticipantStatusMessage";

	public static final String PROFILE_PIC_CHANGED = "profilePicChanged";

	public static final String PROFILE_NAME_CHANGED = "profileNameChanged";

	public static final String RECENT_CONTACTS_UPDATED = "recentContactsUpdated";

	public static final String FILE_MESSAGE_CREATED = "fileMessageCreated";

	public static final String MUTE_CONVERSATION_TOGGLED = "mutedConversationToggled";

	public static final String FAVORITE_TOGGLED = "favoriteToggled";

	public static final String AUTO_RECOMMENDED_FAVORITES_ADDED = "autoRecommendedFavoritesAdded";

	public static final String REMOVE_MESSAGE_FROM_CHAT_THREAD = "removeMessageFromChatThread";

	public static final String SOCIAL_AUTH_COMPLETED = "socialAuthCompleted";

	public static final String SOCIAL_AUTH_FAILED = "socialAuthFailed";

	public static final String FREE_SMS_TOGGLED = "freeSMSToggled";

	public static final String REFRESH_FAVORITES = "refreshFavorites";

	public static final String REFRESH_RECENTS = "refreshRecents";

	public static final String SSL_PREFERENCE_CHANGED = "sslPrefChanged";

	public static final String GROUP_REVIVED = "groupRevived";

	public static final String PROFILE_IMAGE_DOWNLOADED = "profileImageDownloaded";

	public static final String PROFILE_IMAGE_NOT_DOWNLOADED = "profileImageNotDownloaded";

	public static final String UPDATE_OF_MENU_NOTIFICATION = "updateOfMenuNotification";

	public static final String TALK_TIME_CHANGED = "talkTimeChanged";

	public static final String SHOW_STATUS_DIALOG = "showStatusDialog";

	public static final String STATUS_MESSAGE_RECEIVED = "statusMessageReceived";

	public static final String MY_STATUS_CHANGED = "myStatusChanged";

	public static final String RESET_NOTIFICATION_COUNTER = "resetNotificationCounter";

	public static final String FACEBOOK_IMAGE_DOWNLOADED = "facebookImageDownloaded";

	public static final String DECREMENT_NOTIFICATION_COUNTER = "decrementNotificationCounter";

	public static final String FRIEND_REQUEST_ACCEPTED = "friendRequestAccepted";

	public static final String REJECT_FRIEND_REQUEST = "rejectFriendRequest";

	public static final String DELETE_STATUS = "deleteStatus";

	public static final String HIKE_JOIN_TIME_OBTAINED = "hikeJoinTimeObtained";

	public static final String TIMELINE_UPDATE_RECIEVED = "timelineUpdateRecieved";

	public static final String USER_JOIN_TIME_OBTAINED = "userJoinTimeObtained";

	public static final String STATUS_POST_REQUEST_DONE = "statusPostRequestDone";

	public static final String BATCH_STATUS_UPDATE_PUSH_RECEIVED = "batchSUReceived";

	public static final String CANCEL_ALL_STATUS_NOTIFICATIONS = "cancelAllStatusNotifications";

	public static final String DRAWER_ANIMATION_COMPLETE = "drawerAnimationComplete";

	public static final String SMS_SYNC_START = "smsSyncStart";

	public static final String SMS_SYNC_COMPLETE = "smsSyncComplete";

	public static final String SMS_SYNC_FAIL = "smsSyncFail";

	public static final String SEND_HIKE_SMS_FALLBACK = "sendHikeSMSFallback";

	public static final String SEND_NATIVE_SMS_FALLBACK = "sendNativeSMSFallback";

	public static final String CHANGED_MESSAGE_TYPE = "changedMessageType";

	public static final String SHOW_SMS_SYNC_DIALOG = "showSMSSyncDialog";

	public static final String STICKER_DOWNLOADED = "stickerDownloaded";

	public static final String STICKER_CATEGORY_DOWNLOADED = "stickerCategoryDownloaded";

	public static final String STICKER_CATEGORY_DOWNLOAD_FAILED = "stickerCategoryDownloadFailed";

	public static final String LAST_SEEN_TIME_UPDATED = "lastSeenTimeUpdated";

	public static final String LAST_SEEN_TIME_BULK_UPDATED = "lastSeenTimeBulkUpdated";

	public static final String REMOVE_PROTIP = "removeProtip";

	public static final String PROTIP_ADDED = "protipAdded";

	public static final String GAMING_PROTIP_DOWNLOADED = "gamingProtipDownloaded";

	public static final String DISMISS_POSTING_DIALOG = "dismissPostingDialog";

	public static final String SHOW_IMAGE = "showImage";

	public static final String RESET_UNREAD_COUNT = "resetUnreadCount";

	public static final String PUSH_FILE_DOWNLOADED = "pushFileDownloaded";

	public static final String PUSH_AVATAR_DOWNLOADED = "pushAvtarDownloaded";

	public static final String PUSH_STICKER_DOWNLOADED = "pushStickerDownloaded";

	public static final String CANCEL_ALL_NOTIFICATIONS = "cancelAllNotifications";

	public static final String LARGER_IMAGE_DOWNLOADED = "largerImageDownloaded";

	public static final String LARGER_UPDATE_IMAGE_DOWNLOADED = "largerUpdateImageDownloaded";

	public static final String FRIENDS_TAB_QUERY = "friendsTabQuery";

	public static final String INCREMENTED_UNSEEN_STATUS_COUNT = "incrementedUnseenStatusCount";

	public static final String SEND_SMS_PREF_TOGGLED = "sendSmsPrefToggled";

	public static final String FINISHED_AVTAR_UPGRADE = "finshedAvtarDBUpgrade";

	public static final String FTUE_LIST_FETCHED_OR_UPDATED = "ftueListFetchedOrUpdated";

	public static final String UPDATE_PUSH = "updatePush";

	public static final String APPLICATIONS_PUSH = "applicationsPush";

	public static final String UPDATE_AVAILABLE = "updateAvailable"; // TODO: get rid of this.

	public static final String SERVICE_STARTED = "serviceStarted";

	public static final String INVITE_SENT = "inviteSent";

	public static final String SHOW_FREE_INVITE_SMS = "showFreeInviteSMS";

	public static final String CHAT_BACKGROUND_CHANGED = "chatBackgroundChanged";

	public static final String CLEAR_CONVERSATION = "clearConversation";
	
	public static final String UPDATE_NETWORK_STATE = "updateNetworkState";

	public static final String MULTI_FILE_TASK_FINISHED = "multiFileTaskFinished";

	public static final String CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE = "conversationClearedByDeletingLastMessage";

	public static final String CONTACT_SYNCED = "contactSynced";

	public static final String DISMISS_GROUP_CHAT_TIP = "dismissGroupChatTip";
	
	public static final String IPS_CHANGED = "ipsChanged";

	public static final String DISMISS_STEALTH_FTUE_CONV_TIP = "dismissStealthFtueConvTip";

	public static final String SHOW_STEALTH_FTUE_CONV_TIP = "showStealthFtueConvTip";

	public static final String SHOW_STEALTH_FTUE_SET_PASS_TIP = "showStealthFtueSetPassTip";

	public static final String SHOW_STEALTH_FTUE_ENTER_PASS_TIP = "showStealthFtueEnterPassTip";

	public static final String STEALTH_MODE_TOGGLED = "stealthModeToggled";

	public static final String CLEAR_FTUE_STEALTH_CONV = "clearFtueStealthConv";

	public static final String STEALTH_CONVERSATION_MARKED = "stealthConverstaionMarked";

	public static final String STEALTH_CONVERSATION_UNMARKED = "stealthConversationUnmarked";

	public static final String RESET_STEALTH_INITIATED = "resetStealthInitiated";

	public static final String RESET_STEALTH_CANCELLED = "resetStealthCancelled";

	public static final String STEALTH_MODE_RESET_COMPLETE = "stealthModeResetComplete";

	public static final String CONNECTED_TO_MQTT = "connectedToMqtt";

	public static final String CLOSE_CURRENT_STEALTH_CHAT = "closeCurrentStealthChat";

	public static final String APP_FOREGROUNDED = "appForegrounded";

	public static final String REMOVE_WELCOME_HIKE_TIP = "removeWelcomeHikeTip";

	public static final String STEALTH_POPUP_WITH_PUSH = "stealthPopupShowPush";

	public static final String ATOMIC_POPUP_WITH_PUSH = "atomicPopupShowPush";

	public static final String REMOVE_STEALTH_INFO_TIP = "removeStealthInfoTip";

	public static final String REMOVE_STEALTH_UNREAD_TIP = "removeStealthUnreadTip";

	public static final String STEALTH_UNREAD_TIP_CLICKED = "stealthUnreadTipClicked";
	
	public static final String BULK_MESSAGE_RECEIVED = "bulkMessagesReceived";

	public static final String BULK_MESSAGE_DELIVERED_READ = "bulkMessageDeliveredRead";
	
	public static final String BULK_MESSAGE_NOTIFICATION = "bulkMessageNotification";
	
	public static final String UPDATE_PIN_METADATA = "pinUpdated";

	public static final String HIKE_SHARED_FILE_DELETED = "hikeSharedFileDeleted";

	public static final String ClOSE_PHOTO_VIEWER_FRAGMENT = "closePhotoViewerFragment";

	public static String FRIEND_REQ_COUNT_RESET = "resetFriendRequest";
	
	public static final String CONTACT_SYNC_STARTED = "contactSyncStarted";

	public static final String FAVORITE_COUNT_CHANGED = "favoriteCountChanged";

	public static String HIKE_TO_OFFLINE_PUSH = "hikeToOfflinePush";

	public static String PROFILE_UPDATE_FINISH = "profileUpdateFinish";
	
	public static final String CONV_META_DATA_UPDATED = "convMetaDataUpdated";
	
	public static final String LATEST_PIN_DELETED = "lastPinDeleted";

	private final Thread mThread;

	private final BlockingQueue<Operation> mQueue;

	private Map<String, Set<Listener>> listeners;

	public HikePubSub()
	{
		listeners = new ConcurrentHashMap<String, Set<Listener>>();
		mQueue = new LinkedBlockingQueue<Operation>();
		mThread = new Thread(this);
		mThread.start();
	}

	public void addListener(String type, Listener listener)
	{
		add(type, listener);
	}

	public void addListeners(Listener listener, String... types)
	{
		for (String type : types)
		{
			add(type, listener);
		}
	}

	private void add(String type, Listener listener)
	{
		Set<Listener> list;
		list = listeners.get(type);
		if (list == null)
		{
			synchronized (this) // take a smaller lock
			{
				if ((list = listeners.get(type)) == null)
				{
					list = new CopyOnWriteArraySet<Listener>();
					listeners.put(type, list);
				}
			}
		}
		list.add(listener);
	}

	/*
	 * We also need to make removeListener a synchronized method. if we don't do that it would lead to memory inconsistency issue. in our case some activities won't get destroyed
	 * unless we unregister all listeners and in that slot if activity receives a pubsub event it would try to handle this event which may lead to anything unusual.
	 */
	public void removeListener(String type, Listener listener)
	{
		remove(type, listener);
	}

	public void removeListeners(Listener listener, String... types)
	{
		for (String type : types)
		{
			remove(type, listener);
		}
	}

	private void remove(String type, Listener listener)
	{
		Set<Listener> l = null;
		l = listeners.get(type);
		if (l != null)
		{
			l.remove(listener);
		}
	}

	public boolean publish(String type, Object o)
	{
		Set<Listener> l = listeners.get(type);
		if (l != null && l.size() >= 0)
		{
			mQueue.add(new Operation(type, o));
			return true;
		}
		return false;
	}

	@Override
	public void run()
	{
		Operation op;
		while (true)
		{
			try
			{
				op = mQueue.take();
			}
			catch (InterruptedException e)
			{
				continue;
			}
			if (op == DONE_OPERATION)
			{
				break;
			}
			String type = op.type;
			Object o = op.payload;

			Set<Listener> list = listeners.get(type);

			if (list == null || list.isEmpty())
			{
				continue;
			}

			for (Listener l : list)
			{
				l.onEventReceived(type, o);
			}
		}
	}
}
