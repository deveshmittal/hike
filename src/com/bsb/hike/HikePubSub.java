package com.bsb.hike;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class HikePubSub implements Runnable {
	public class Operation {
		public Operation(String type, Object o) {
			this.type = type;
			this.payload = o;
		}

		public final String type;

		public final Object payload;
	}

	public interface Listener {
		public void onEventReceived(String type, Object object);
	}

	private static final Operation DONE_OPERATION = null; /*
														 * TODO this can't be
														 * null
														 */

	/*
	 * broadcast when the sender sends the message (click the send button in
	 * chat thread view)
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

	public static final String WS_CLOSE = "ws_close";

	public static final String WS_OPEN = "ws_open";

	// public static final String WS_SEND = "ws_send";

	public static final String NEW_CONVERSATION = "newconv";

	/*
	 * Broadcast after we've received a message and written it to our DB. Status
	 * is RECEIVED_UNREAD
	 */
	public static final String MESSAGE_RECEIVED = "messagereceived";

	public static final String NEW_ACTIVITY = "new_activity";

	public static final String END_TYPING_CONVERSATION = "endtypingconv";

	public static final String TYPING_CONVERSATION = "typingconv";

	public static final String TOKEN_CREATED = "tokencreated";

	/* sms credits have been modified */
	public static final String SMS_CREDIT_CHANGED = "smscredits";

	/*
	 * broadcast when the server receives the message and replies with a
	 * confirmation
	 */
	public static final String SERVER_RECEIVED_MSG = "serverReceivedMsg";

	/*
	 * broadcast when a message is received from the sender but before it's been
	 * written our DB
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
	public static final String MESSAGE_DELETED = "messageDeleted";

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

	public static final String UPDATE_AVAILABLE = "updateAvailable";

	public static final String INVITE_TOKEN_ADDED = "inviteTokenAdded";

	public static final String CONTACT_ADDED = "contactAdded";

	public static final String UPLOAD_FINISHED = "uploadFinished";

	public static final String FILE_TRANSFER_PROGRESS_UPDATED = "fileTransferProgressUpdated";

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
	
	public static final String REMOVE_TWITTER_VIEW = "removeTwitterView";

	public static final String FREE_SMS_TOGGLED = "freeSMSToggled";

	public static final String REFRESH_FAVORITES = "refreshFavorites";

	public static final String REFRESH_RECENTS = "refreshRecents";

	public static final String SWITCHED_DATA_CONNECTION = "switchedDataConnection";

	public static final String GROUP_REVIVED = "groupRevived";

	public static final String PROFILE_IMAGE_DOWNLOADED = "profileImageDownloaded";

	public static final String PROFILE_IMAGE_NOT_DOWNLOADED = "profileImageNotDownloaded";

	public static final String TOGGLE_REWARDS = "toggleRewards";

	public static final String TALK_TIME_CHANGED = "talkTimeChanged";

	private final Thread mThread;

	private final BlockingQueue<Operation> mQueue;

	private Map<String, Set<Listener>> listeners;

	public HikePubSub() {
		listeners = Collections
				.synchronizedMap(new HashMap<String, Set<Listener>>());
		mQueue = new LinkedBlockingQueue<Operation>();
		mThread = new Thread(this);
		mThread.start();
	}

	synchronized public void addListener(String type, Listener listener) {
		addListeners(listener, type);
	}

	synchronized public void addListeners(Listener listener, String... types) {
		for (String type : types) {
			Set<Listener> list = listeners.get(type);
			if (list == null) {
				list = new CopyOnWriteArraySet<Listener>();
				listeners.put(type, list);
			}
			list.add(listener);
		}
	}

	synchronized public boolean publish(String type, Object o) {
		if (!listeners.containsKey(type)) {
			return false;
		}
		mQueue.add(new Operation(type, o));
		return true;
	}

	public void removeListener(String type, Listener listener) {
		removeListeners(listener, type);
	}

	public void removeListeners(Listener listener, String... types) {
		for (String type : types) {
			Set<Listener> l = listeners.get(type);
			if (l != null) {
				l.remove(listener);
			}
		}
	}

	@Override
	public void run() {
		Operation op;
		while (true) {
			try {
				op = mQueue.take();
			} catch (InterruptedException e) {
				Log.e("PubSub", "exception while running", e);
				continue;
			}
			if (op == DONE_OPERATION) {
				break;
			}

			String type = op.type;
			Object o = op.payload;

			Set<Listener> list = listeners.get(type);

			if (list == null) {
				continue;
			}

			for (Listener l : list) {

				l.onEventReceived(type, o);
			}
		}
	}
}
