package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.view.Menu;
import android.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.AnalyticsConstants.MessageType;
import com.bsb.hike.analytics.AnalyticsConstants.MsgRelEventType;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.analytics.MsgRelLogManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.dialog.H20Dialog;
import com.bsb.hike.dialog.HikeDialog;
import com.bsb.hike.dialog.HikeDialogFactory;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.MessagePrivateData;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToOneConversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.lastseenmgr.FetchLastSeenTask;
import com.bsb.hike.service.HikeMqttManagerNew;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.LastSeenScheduler;
import com.bsb.hike.utils.LastSeenScheduler.LastSeenFetchedCallback;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.OneToNConversationUtils;
import com.bsb.hike.utils.SoundUtils;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.voip.VoIPUtils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class OneToOneChatThread extends ChatThread implements LastSeenFetchedCallback, ViewStub.OnInflateListener
{
	private static final String TAG = "oneonechatthread";

	private ContactInfo mContactInfo;

	private LastSeenScheduler lastSeenScheduler;

	private RequestToken lastSeenRequestToken;
	
	private Dialog smsDialog;

	private int mCredits;

	private boolean mBlockOverlay;

	private short modeOfChat = H2H_MODE;

	private static final int CONTACT_ADDED_OR_DELETED = 101;

	private static final int SHOW_SMS_SYNC_DIALOG = 102;

	private static final int SMS_SYNC_COMPLETE_OR_FAIL = 103;

	private static final int UPDATE_LAST_SEEN = 104;

	private static final int SEND_SMS_PREF_TOGGLED = 105;

	private static final int SMS_CREDIT_CHANGED = 106;

	private static final int REMOVE_UNDELIVERED_MESSAGES = 107;

	private static final int BULK_MESSAGE_RECEIVED = 108;

	private static final int USER_JOINED_OR_LEFT = 109;

	private static final int SCHEDULE_LAST_SEEN = 110;

	private static final int SCHEDULE_H20_TIP = 111;

	private static final int SCROLL_LIST_VIEW = 112;

	private static final int ADD_UNDELIVERED_MESSAGE = 114;

	private static final int SHOW_CALL_ICON = 115;
	
	protected static final int BLOCK_UNBLOCK_USER = 116;
	
	private static short H2S_MODE = 0; // Hike to SMS Mode

	private static short H2H_MODE = 1; // Hike to Hike Mode

	/* The waiting time in seconds before scheduling a H20 Tip */
	private static final int DEFAULT_UNDELIVERED_WAIT_TIME = 30;

	private static final int DEFAULT_SMS_LENGTH = 140;
	
	String prevLastSeen = null;

	private View hikeToOfflineTipView;

	/**
	 * this is set of all the currently visible messages which are stuck in tick and are not sms
	 */
	private LinkedHashMap<Long, ConvMessage> undeliveredMessages = new LinkedHashMap<Long, ConvMessage>();

	/**
	 * Since {@link #undeliveredMessages} is a LinkedList, this variable is used to keep track of the head of the list
	 */
	private ConvMessage firstPendingConvMessage = null;

	/**
	 * We keep a flag which indicates whether we can schedule H20Tip or not
	 */
	private boolean shouldScheduleH20Tip = true;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public OneToOneChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		Logger.i(TAG, "on create options menu " + menu.hashCode());

		if (mConversation != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, getOverFlowItems(), this, this);
			if(shouldShowCallIcon())
			{
				menu.findItem(R.id.voip_call).setVisible(true);
			}
			return super.onCreateOptionsMenu(menu);
		}

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Logger.i(TAG, "menu item click" + item.getItemId());
		
//		Not allowing user to access actionbar items on a blocked user's chatThread
		if (mConversation.isBlocked())
		{
			Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.block_overlay_message, mConversation.getMsisdn()), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		switch (item.getItemId())
		{
			case R.id.voip_call:
				onCallClicked();
				return true;
		}
		return mActionBar.onOptionsItemSelected(item) ? true : super.onOptionsItemSelected(item);
	}

	/**
	 * Returns a list of over flow menu items to be displayed
	 * 
	 * @return
	 */
	private List<OverFlowMenuItem> getOverFlowItems()
	{
		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.view_profile), 0, 0, R.string.view_profile));
		list.add(new OverFlowMenuItem(getString(R.string.chat_theme), 0, 0, R.string.chat_theme));
		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}

		list.add(new OverFlowMenuItem(mConversation.isBlocked() ? getString(R.string.unblock_title) : getString(R.string.block_title), 0, 0, R.string.block_title));
		if (mContactInfo.isNotOrRejectedFavourite())
		{
			list.add(new OverFlowMenuItem(getString(R.string.add_as_favorite_menu), 0, 0, R.string.add_as_favorite_menu));
		}
		return list;
	}

	@Override
	protected Conversation fetchConversation()
	{
		mConversation = mConversationDb.getConversation(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, OneToNConversationUtils.isGroupConversation(msisdn));

		mContactInfo = HikeMessengerApp.getContactManager().getContact(msisdn, true, true);

		if (mConversation == null)
		{
			mConversation = new OneToOneConversation.ConversationBuilder(msisdn).setConvName((mContactInfo != null) ? mContactInfo.getName() : null).setIsOnHike(mContactInfo.isOnhike()).build();
			mConversation.setMessages(HikeConversationsDatabase.getInstance().getConversationThread(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, mConversation, -1));
		}

		ChatTheme chatTheme = mConversationDb.getChatThemeForMsisdn(msisdn);
		Logger.d(TAG, "Calling setchattheme from createConversation");
		mConversation.setChatTheme(chatTheme);

		mConversation.setBlocked(ContactManager.getInstance().isBlocked(msisdn));
		mCredits = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getInt(HikeMessengerApp.SMS_SETTING, 0);

		return mConversation;
	}

	@Override
	protected int getContentView()
	{
		return R.layout.chatthread;
	}

	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		showTips();

		super.fetchConversationFinished(conversation);

		if (mConversation.isOnHike())
		{
			addUnkownContactBlockHeader();
		}

		else
		{
			FetchHikeUser.fetchHikeUser(activity.getApplicationContext(), msisdn);
		}

		if (ChatThreadUtils.shouldShowLastSeen(activity.getApplicationContext(), mContactInfo.getFavoriteType(), mConversation.isOnHike()))
		{
			checkAndStartLastSeenTask();
		}

		/**
		 * If user is blocked
		 */

		if (mConversation.isBlocked())
		{
			showBlockOverlay(getConvLabel());
		}

		// TODO : ShowStickerFTUE Tip and H20 Tip. H20 Tip is a part of one to one chatThread. Sticker Tip is a part of super class

		if (mConversation.isOnHike())
		{
			addAllUndeliverdMessages(messages);
		}

	}

	private void showTips()
	{
		mTips = new ChatThreadTips(activity.getBaseContext(), activity.findViewById(R.id.chatThreadParentLayout), new int[] { ChatThreadTips.ATOMIC_ATTACHMENT_TIP,
				ChatThreadTips.ATOMIC_STICKER_TIP, ChatThreadTips.ATOMIC_CHAT_THEME_TIP, ChatThreadTips.STICKER_TIP }, sharedPreference);
		mTips.showTip();
	}

	private void resetLastSeenScheduler()
	{
		if (lastSeenScheduler != null)
		{
			lastSeenScheduler.stop(false);
			lastSeenScheduler = null;
		}
	}

	private void cancelFetchLastseenTask()
	{
		if (lastSeenRequestToken != null)
		{
			lastSeenRequestToken.cancel();
		}
	}

	protected void addUnkownContactBlockHeader()
	{
		if (mContactInfo != null && mContactInfo.isUnknownContact() && messages != null && messages.size() > 0 && !Utils.isBot(msisdn))
		{
			ConvMessage cm = messages.get(0);
			/**
			 * Check if the conv message was previously a block header or not
			 */
			if (!cm.isBlockAddHeader())
			{
				/**
				 * Creating a new conv message to be appended at the 0th position.
				 */
				cm = new ConvMessage(0, 0l, 0l);
				cm.setBlockAddHeader(true);
				messages.add(0, cm);
				Logger.d(TAG, "Adding unknownContact Header to the chatThread");

				if (mAdapter != null)
				{
					mAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	@Override
	public void lastSeenFetched(String contMsisdn, int offline, long lastSeenTime)
	{
		Logger.d(TAG, " Got lastSeen Time for msisdn : " + contMsisdn + " LastSeenTime : " + lastSeenTime);
		HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "lastSeenFetched", "going to update UI", contMsisdn);
		updateLastSeen(contMsisdn, offline, lastSeenTime);
	}

	@Override
	protected String[] getPubSubListeners()
	{
		String[] oneToOneListeners = new String[] { HikePubSub.SMS_CREDIT_CHANGED, HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.CONTACT_ADDED, HikePubSub.CONTACT_DELETED,
				HikePubSub.CHANGED_MESSAGE_TYPE, HikePubSub.SHOW_SMS_SYNC_DIALOG, HikePubSub.SMS_SYNC_COMPLETE, HikePubSub.SMS_SYNC_FAIL, HikePubSub.SMS_SYNC_START,
				HikePubSub.LAST_SEEN_TIME_UPDATED, HikePubSub.SEND_SMS_PREF_TOGGLED, HikePubSub.BULK_MESSAGE_RECEIVED, HikePubSub.USER_JOINED, HikePubSub.USER_LEFT,
				HikePubSub.APP_FOREGROUNDED, HikePubSub.BLOCK_USER, HikePubSub.UNBLOCK_USER };
		return oneToOneListeners;
	}

	@Override
	protected void addMessage(ConvMessage convMessage)
	{
		/*
		 * If we were showing the typing bubble, we remove it from the add the new message and add the typing bubble back again
		 */

		TypingNotification typingNotification = removeTypingNotification();

		/**
		 * Adding message to the adapter
		 */

		mAdapter.addMessage(convMessage);

		if (convMessage.getTypingNotification() == null && typingNotification != null && convMessage.isSent())
		{
			mAdapter.addMessage(new ConvMessage(typingNotification));
		}

		super.addMessage(convMessage);
	}

	/**
	 * This overrides {@link ChatThread}'s {@link #onTypingConversationNotificationReceived(Object)}
	 */
	@Override
	protected void onTypingConversationNotificationReceived(Object object)
	{
		TypingNotification typingNotification = (TypingNotification) object;

		if (typingNotification == null)
		{
			return;
		}

		if (msisdn.equals(typingNotification.getId()))
		{
			sendUIMessage(TYPING_CONVERSATION, typingNotification);
		}

		if (ChatThreadUtils.shouldShowLastSeen(activity.getApplicationContext(), mContactInfo.getFavoriteType(), mConversation.isOnHike()) && mContactInfo.getOffline() != -1)
		{
			/*
			 * Publishing an online event for this number.
			 */
			mContactInfo.setOffline(0);
			HikeMessengerApp.getPubSub().publish(HikePubSub.LAST_SEEN_TIME_UPDATED, mContactInfo);
		}
	}

	/**
	 * This overrides : {@link ChatThread}'s {@link #setTypingText(boolean, TypingNotification)}
	 */
	@Override
	protected void setTypingText(boolean direction, TypingNotification typingNotification)
	{
		if (direction)
		{
			super.setTypingText(direction, typingNotification);
		}

		else
		{
			if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
			{
				/*
				 * We only remove the typing notification if the conversation in a one to one conversation or it no one is typing in the group.
				 */
				messages.remove(messages.size() - 1);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	protected void onMessageRead(Object object)
	{
		Pair<String, long[]> pair = (Pair<String, long[]>) object;
		// If the msisdn don't match we simply return
		if (!mConversation.getMsisdn().equals(pair.first))
		{
			return;
		}
		long[] ids = pair.second;
		// TODO we could keep a map of msgId -> conversation objects
		// somewhere to make this faster
		for (int i = 0; i < ids.length; i++)
		{
			ConvMessage msg = findMessageById(ids[i]);
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
			{
				msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				removeFromMessageMap(msg);
				
				//Log Events For Message Reliability
				MessagePrivateData pd = msg.getPrivateData();
				if(pd != null && pd.getTrackID() != null)
				{
					Logger.d(AnalyticsConstants.MSG_REL_TAG, "===========================================");
					Logger.d(AnalyticsConstants.MSG_REL_TAG, "Read Shown to Sender:track_id "+ msg.getPrivateData().getTrackID());
					MsgRelLogManager.logMsgRelEvent(msg, MsgRelEventType.MR_SHOWN_AT_SENEDER_SCREEN);
				}
			}
		}
		if (mConversation.isOnHike())
		{
			uiHandler.sendEmptyMessage(REMOVE_UNDELIVERED_MESSAGES);
		}

		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}

	@Override
	protected boolean onMessageDelivered(Object object)
	{
		if (super.onMessageDelivered(object))
		{
			if (mConversation.isOnHike())
			{
				Pair<String, Long> pair = (Pair<String, Long>) object;
				long msgID = pair.second;
				// TODO we could keep a map of msgId -> conversation objects
				// somewhere to make this faster
				ConvMessage msg = findMessageById(msgID);

				sendUIMessage(REMOVE_UNDELIVERED_MESSAGES, msg);
			}
			return true;
		}
		return false;
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.MESSAGE_DELIVERED_READ:
			onMessageRead(object);
			break;
		case HikePubSub.CONTACT_ADDED:
			onContactAddedOrDeleted(object, true);
			break;
		case HikePubSub.CONTACT_DELETED:
			onContactAddedOrDeleted(object, false);
			break;
		case HikePubSub.CHANGED_MESSAGE_TYPE:
			uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			break;
		case HikePubSub.SHOW_SMS_SYNC_DIALOG:
			uiHandler.sendEmptyMessage(SHOW_SMS_SYNC_DIALOG);
			break;
		case HikePubSub.SMS_SYNC_COMPLETE:
			uiHandler.sendEmptyMessage(SMS_SYNC_COMPLETE_OR_FAIL);
			break;
		case HikePubSub.SMS_SYNC_FAIL:
			uiHandler.sendEmptyMessage(SMS_SYNC_COMPLETE_OR_FAIL);
			break;
		case HikePubSub.SMS_SYNC_START:
			onSMSSyncStart();
			break;
		case HikePubSub.LAST_SEEN_TIME_UPDATED:
			ContactInfo contactInfo = (ContactInfo) object;
			HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "recv pubsub LAST_SEEN_TIME_UPDATED", "going update UI", contactInfo.getMsisdn());
			updateLastSeen(contactInfo.getMsisdn(), contactInfo.getOffline(), contactInfo.getLastSeenTime());
			break;
		case HikePubSub.SEND_SMS_PREF_TOGGLED:
			uiHandler.sendEmptyMessage(SEND_SMS_PREF_TOGGLED);
			break;
		case HikePubSub.SMS_CREDIT_CHANGED:
			uiHandler.sendEmptyMessage(SMS_CREDIT_CHANGED);
			break;
		case HikePubSub.BULK_MESSAGE_RECEIVED:
			onBulkMessageReceived(object);
			break;
		case HikePubSub.USER_JOINED:
			onUserJoinedOrLeft(object, true);
			uiHandler.sendEmptyMessage(SHOW_CALL_ICON);
			break;
		case HikePubSub.USER_LEFT:
			onUserJoinedOrLeft(object, false);
			break;
		case HikePubSub.APP_FOREGROUNDED:
			onAppForegrounded();
			break;
		case HikePubSub.BLOCK_USER:
			blockUser(object, true);
			break;
		case HikePubSub.UNBLOCK_USER:
			blockUser(object, false);
			break;
		default:
			Logger.d(TAG, "Did not find any matching PubSub event in OneToOne ChatThread. Calling super class' onEventReceived");
			super.onEventReceived(type, object);
			break;
		}
	}

	@Override
	protected boolean setStateAndUpdateView(long msgId, boolean updateView)
	{
		// TODO Auto-generated method stub
		if (super.setStateAndUpdateView(msgId, updateView))
		{
			if (mConversation.isOnHike())
			{
				ConvMessage msg = findMessageById(msgId);
				if (!msg.isSMS() && (!msg.isBroadcastMessage())) // Since ConvMessage is not sent as SMS, hence add it to undeliveredMap
				{
					sendUIMessage(ADD_UNDELIVERED_MESSAGE, msg);
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * This method is used to update UI, when an unsaved contact is saved to phonebook while the chatThread is active
	 */
	private void onContactAddedOrDeleted(Object object, boolean isAdded)
	{
		ContactInfo contactInfo = (ContactInfo) object;

		/**
		 * Returning here if contactInfo is null or we received this event in a different chatThread
		 */

		if (contactInfo == null || (!msisdn.equals(contactInfo.getMsisdn())))
		{
			return;
		}

		String mContactName = isAdded ? contactInfo.getName() : contactInfo.getMsisdn();
		mConversation.setConversationName(mContactName);
		mContactName = Utils.getFirstName(mContactName);
		sendUIMessage(CONTACT_ADDED_OR_DELETED, new Pair<Boolean, String>(isAdded, mContactName));
	}

	/**
	 * Called on the UI Thread from the UI Handler, which is called from {@link OneToOneChatThread #onContactAddedOrDeleted(Object, boolean)}
	 */

	private void contactAddedOrDeleted(Pair<Boolean, String> pair)
	{
		if (!pair.first)
		{
			setAvatar();
		}

		setLabel(pair.second);

		if (messages != null && messages.size() > 0)
		{
			ConvMessage convMessage = messages.get(0);

			if (convMessage.isBlockAddHeader())
			{
				messages.remove(0);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	/**
	 * Performs tasks on the UI thread.
	 */
	@Override
	protected void handleUIMessage(Message msg)
	{
		switch (msg.what)
		{
		case CONTACT_ADDED_OR_DELETED:
			contactAddedOrDeleted((Pair<Boolean, String>) msg.obj);
			break;
		case SHOW_SMS_SYNC_DIALOG:
			onShowSMSSyncDialog();
			break;
		case SMS_SYNC_COMPLETE_OR_FAIL:
			dismissSMSSyncDialog();
			break;
		case UPDATE_LAST_SEEN:
			setLastSeen((String) msg.obj);
			break;
		case SEND_SMS_PREF_TOGGLED:
			updateUIForHikeStatus();
			break;
		case SMS_CREDIT_CHANGED:
			setSMSCredits();
			break;
		case REMOVE_UNDELIVERED_MESSAGES:
			removeUndeliveredMessages(msg.obj);
			break;
		case BULK_MESSAGE_RECEIVED:
			addBulkMessages((LinkedList<ConvMessage>) msg.obj);
			break;
		case USER_JOINED_OR_LEFT:
			userJoinedOrLeft();
			break;
		case SCHEDULE_LAST_SEEN:
			scheduleLastSeen();
			break;
		case SCHEDULE_H20_TIP:
			showH20TipFromHandler();
			break;
		case SCROLL_LIST_VIEW:
			mConversationsView.smoothScrollToPosition(messages.size() - 1);
			break;
		case ADD_UNDELIVERED_MESSAGE:
			addToUndeliveredMessages((ConvMessage) msg.obj);
			break;
		case SHOW_CALL_ICON:
			if(shouldShowCallIcon())
			{
				mActionBar.getMenuItem(R.id.voip_call).setVisible(true);
			}
			break;
		case BLOCK_UNBLOCK_USER:
			blockUnBlockUser((boolean) msg.obj);
			break;
		default:
			Logger.d(TAG, "Did not find any matching event in OneToOne ChatThread. Calling super class' handleUIMessage");
			super.handleUIMessage(msg);
			break;
		}

	}

	/**
	 * Method is called from the UI Thread to show the SMS Sync Dialog
	 */
	private void onShowSMSSyncDialog()
	{
		smsDialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.SMS_SYNC_DIALOG, true);
	}

	/**
	 * Called on the UI Thread to dismiss SMS Sync Dialog
	 */
	private void dismissSMSSyncDialog()
	{
		if (smsDialog != null)
		{
			smsDialog.dismiss();
		}
		// TODO :
		// dialogShowing = null;
	}

	private void onSMSSyncStart()
	{
		// TODO :
		// dialogShowing = DialogShowing.SMS_SYNCING_DIALOG;
	}

	/**
	 * Used to update last seen. This is called from the PubSub thread
	 * 
	 * @param object
	 */
	private void updateLastSeen(String contMsisdn, int offline, long lastSeenTime)
	{
		/**
		 * Proceeding only if the current chat thread is open and we should show the last seen
		 */
		if (msisdn.equals(contMsisdn) && ChatThreadUtils.shouldShowLastSeen(activity.getApplicationContext(), mContactInfo.getFavoriteType(), mConversation.isOnHike()))
		{
			/**
			 * Fix for case where server and client values are out of sync
			 */

			if (offline == 1 && lastSeenTime <= 0)
			{
				return;
			}

			/**
			 * Updating mContactInfo object
			 */
			mContactInfo.setOffline(offline);
			mContactInfo.setLastSeenTime(lastSeenTime);

			String lastSeenString = Utils.getLastSeenTimeAsString(activity.getApplicationContext(), lastSeenTime, offline, false, true);
			/**
			 * mContactInfo.getOffline == 0 indicates user is online
			 */
			if (isH20TipShowing() && (mContactInfo.getOffline() == 0))
			{
				/**
				 * If hike to offline tip is showing and server sends that the user is online, we do not update the last seen field until all pending messages are delivered
				 */
				return;
			}

			sendUIMessage(UPDATE_LAST_SEEN, lastSeenString);
		}
	}

	/**
	 * Called from the UI Thread
	 * 
	 * @param lastSeenString
	 */
	private void setLastSeen(String lastSeenString)
	{
		if (mContactInfo.getOffline() == 0) //User online ?
		{
			/**
			 * If the user is online, we set this flag to true
			 */
			shouldScheduleH20Tip = true;
		}

		if (lastSeenString == null)
		{
			HAManager.getInstance().recordLastSeenEvent(OneToOneChatThread.class.getName(), "updateLastSeen", "lastSeen null so setLastSeenTextBasedOnHikeValue", msisdn);
			setLastSeenTextBasedOnHikeValue(mConversation.isOnHike());
		}
		else
		{
			setLastSeenText(lastSeenString);
		}
	}

	private void setSMSCredits()
	{
		updateUIForHikeStatus();

		if ((mCredits % HikeConstants.SHOW_CREDITS_AFTER_NUM == 0) && !mConversation.isOnHike())
		{
			showSMSCounter();
		}
	}

	private void updateUIForHikeStatus()
	{
		if (mConversation.isOnHike())
		{
			/**
			 * since this is a view stub, so can return null
			 */
			if (activity.findViewById(R.id.sms_toggle_button) != null)
			{
				hideView(R.id.sms_toggle_button);
			}
			nonZeroCredits();
		}

		else
		{
			setupSMSToggleLayout();
			updateChatMetadata();
		}

	}

	private void nonZeroCredits()
	{
		Logger.d(TAG, "Non Zero credits");
		if (!mComposeView.isEnabled())
		{
			if (!TextUtils.isEmpty(mComposeView.getText()))
			{
				mComposeView.setText("");
			}
			mComposeView.setEnabled(true);
		}

		hideView(R.id.info_layout);
		showView(R.id.emoticon_btn);

		activity.findViewById(R.id.emoticon_btn).setEnabled(true);
		activity.findViewById(R.id.sticker_btn).setEnabled(true);

		if (!mBlockOverlay)
		{
			hideOverlay();
		}

		if (mTips.isAnyTipOpen()) // Could be that we might have hidden a tip in Zero Credits case. To offset that, we show the hidden tip here
		{
			mTips.showHiddenTip();
		}
	}

	private void zeroCredits()
	{
		Logger.d(TAG, "Zero Credits");

		ImageButton mSendButton = (ImageButton) activity.findViewById(R.id.send_message);
		mSendButton.setEnabled(false);

		if (!TextUtils.isEmpty(mComposeView.getText()))
		{
			mComposeView.setText("");
		}

		mComposeView.setHint(activity.getString(R.string.zero_sms_hint));
		mComposeView.setEnabled(false);

		showView(R.id.info_layout);
		hideView(R.id.emoticon_btn);

		activity.findViewById(R.id.emoticon_btn).setEnabled(false);
		activity.findViewById(R.id.sticker_btn).setEnabled(false);

		if (!mConversationDb.wasOverlayDismissed(mConversation.getMsisdn()))
		{
			mConversationDb.setOverlay(false, mConversation.getMsisdn());
			String formatString = activity.getString(R.string.no_credits);
			String label = getConvLabel();

			String formatted = String.format(formatString, label);
			SpannableString str = new SpannableString(formatted);

			showOverlay(label, formatString, activity.getString(R.string.invite_now), str, R.drawable.ic_no_credits, R.string.invite_now);
		}

		/**
		 * If any tip is open, we hide it
		 */
		mTips.hideTip();
	}

	private void updateChatMetadata()
	{
		TextView mMetadataNumChars = (TextView) activity.findViewById(R.id.sms_chat_metadata_num_chars);

		boolean mNativeSMSPref = Utils.getSendSmsPref(activity.getApplicationContext());

		if (mCredits <= 0 && !mNativeSMSPref)
		{
			zeroCredits();
		}

		else
		{
			nonZeroCredits();

			if (mComposeView.getLineCount() > 2)
			{
				mMetadataNumChars.setVisibility(View.VISIBLE);
				int length = mComposeView.getText().length();
				/**
				 * Set the max sms length to a length appropriate to the number of characters we have
				 */

				int charNum = length % 140;
				int numSMS = ((int) (length / 140)) + 1;

				String charNumString = Integer.toString(charNum);
				SpannableString ss = new SpannableString(charNumString + "/#" + Integer.toString(numSMS));
				ss.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.send_green)), 0, charNumString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				mMetadataNumChars.setText(ss);
			}

			else
			{
				mMetadataNumChars.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void showSMSCounter()
	{
		Animation slideUp = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_up_noalpha);
		slideUp.setDuration(2000);

		final Animation slideDown = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_down_noalpha);
		slideDown.setDuration(2000);
		slideDown.setStartOffset(2000);

		final TextView smsCounterView = (TextView) activity.findViewById(R.id.sms_counter);
		smsCounterView.setBackgroundColor(activity.getResources().getColor(mAdapter.isDefaultTheme() ? R.color.updates_text : R.color.chat_thread_indicator_bg_custom_theme));
		smsCounterView.setAnimation(slideUp);
		smsCounterView.setVisibility(View.VISIBLE);
		smsCounterView.setText(mCredits + " " + activity.getResources().getString(R.string.sms_left));

		slideUp.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{

			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{

			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				smsCounterView.setAnimation(slideDown);
				smsCounterView.setVisibility(View.INVISIBLE);
			}
		});
	}

	/**
	 * This method is called to remove undelivered messages from the message adapter
	 * 
	 * @param convMessage
	 */
	private void removeUndeliveredMessages(Object obj)
	{
		if (obj != null)
		{
			removeFromUndeliveredMessage((ConvMessage) obj, true);
		}

		else
		{
			removeAllFromUndeliveredMessages();
		}
	}

	/**
	 * This overrides sendPoke from ChatThread
	 */
	@Override
	protected void sendPoke()
	{
		/** Disabling super as we have to do logging specific to OneToOneChat
			and we need convmessage object for logging
		**/
		//super.sendPoke();
		
		
		//When MsgRelLogManager is removed / or when to for GC as well, we can go with super
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, getString(R.string.poke_msg), mConversation.isOnHike());
		ChatThreadUtils.setPokeMetadata(convMessage);

		// 1) user double clicked on Chat Screen i.e Sending nudge
		MsgRelLogManager.startMessageRelLogging(convMessage, MessageType.TEXT);
				
		sendMessage(convMessage);

		Utils.vibrateNudgeReceived(activity.getApplicationContext());
	}

	/** Overrriding as we have to do logging specific to OneToOneChat
		and we need convmessage object for logging
	 **/
	@Override
	protected void sendSticker(Sticker sticker, String source)
	{
		ConvMessage convMessage = Utils.makeConvMessage(msisdn, StickerManager.STICKER_MESSAGE_TAG, mConversation.isOnHike());
		ChatThreadUtils.setStickerMetadata(convMessage, sticker.getCategoryId(), sticker.getStickerId(), source);

		// 1) user clicked sticker in Sticker Pallete i.e Sending Sticker
		MsgRelLogManager.startMessageRelLogging(convMessage, MessageType.STICKER);
				
		sendMessage(convMessage);
	}
	
	/**
	 * Overrides {@link ChatThread}'s {@link #setupActionBar()}, to set the last seen time
	 */
	@Override
	protected void setupActionBar(boolean firstInflation)
	{
		super.setupActionBar(firstInflation);

		setLabel(getConvLabel());

		/**
		 * If unsaved contact : do not show last seen first. Wait for the query to return the result
		 */
		
		if (firstInflation)
		{
			if (mContactInfo.isUnknownContact())
			{
				hideLastSeenText();
			}
			else
			{
				setLastSeenTextBasedOnHikeValue(mConversation.isOnHike());
			}
		}
		
		else
		{
			setPrevLastSeenTextFromActionBar();
		}
	}

	private void setPrevLastSeenTextFromActionBar()
	{
		Logger.d(TAG, " Previous lastSeen value : " + prevLastSeen);
		setLastSeen(prevLastSeen);
	}

	/**
	 * If the conv is on Hike, then we hide the last seen text, else we show it as "On SMS"
	 * 
	 * @param isConvOnHike
	 */
	private void setLastSeenTextBasedOnHikeValue(boolean isConvOnHike)
	{
		if (isConvOnHike || Utils.isBot(msisdn))
		{
			hideLastSeenText();
		}

		else
		{
			setLastSeenText(activity.getResources().getString(R.string.on_sms));
		}
	}

	/**
	 * Utility method to set the last seen text
	 */
	private void setLastSeenText(String text)
	{
		final TextView mLastSeenView = (TextView) mActionBarView.findViewById(R.id.contact_status);

		TextView mLabelView = (TextView) mActionBarView.findViewById(R.id.contact_name);

		/**
		 * If the last seen string is empty or null
		 */
		if (TextUtils.isEmpty(text))
		{
			mLastSeenView.setVisibility(View.GONE);
			return;
		}

		/**
		 * Setting text on lastSeenView
		 */
		mLastSeenView.setText(text);
		
		prevLastSeen = text;
		
		if (mLastSeenView.getVisibility() == View.GONE)
		{
			/**
			 * If the view was initially gone, we animate the label view in order to make lastSeenView visible
			 */
			{
				mLastSeenView.setVisibility(View.INVISIBLE);

				Animation animation = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_up_last_seen);
				mLabelView.startAnimation(animation);

				animation.setAnimationListener(new AnimationListener()
				{
					@Override
					public void onAnimationStart(Animation animation)
					{
					}

					@Override
					public void onAnimationRepeat(Animation animation)
					{
					}

					@Override
					public void onAnimationEnd(Animation animation)
					{
						mLastSeenView.setVisibility(View.VISIBLE);
					}
				});
			}
		}
		
		HAManager.getInstance().recordLastSeenEvent(OneToOneChatThread.class.getName(), "setLastSeenText", "Updated UI for LastSeen", msisdn);
	}

	/**
	 * Utility method used for hiding the lastSeenView from the Action Bar
	 */
	private void hideLastSeenText()
	{
		mActionBarView.findViewById(R.id.contact_status).setVisibility(View.GONE);
	}

	@Override
	protected boolean updateUIAsPerTheme(ChatTheme theme)
	{
		if (super.updateUIAsPerTheme(theme))
		{
			if (!mContactInfo.isUnknownContact())
			{
				setupSMSToggleLayout();
			}
		}
		return false;
	}

	/**
	 * Used to setup FreeSMS - Hike SMS Toggle button for Versions below KitKat
	 */
	private void setupSMSToggleLayout()
	{
		/**
		 * Proceeding only if the conv is not on hike, neither is the number an international one and the device OS is < v 4.4 Kitkat
		 */
		if (!mConversation.isOnHike() && !Utils.isContactInternational(msisdn) && !Utils.isKitkatOrHigher())
		{

			ViewStub viewStub = (ViewStub) activity.findViewById(R.id.sms_toggle_view_stub);

			/**
			 * Inflating it only once when needed on demand.
			 */
			if (viewStub != null)
			{
				viewStub.setOnInflateListener(this);
				viewStub.inflate();
			}

			/**
			 * ViewStub has been inflated
			 */
			else
			{
				setUpSMSViews();
			}
		}
	}

	private void setUpSMSViews()
	{
		animateSMSToggleLayout();
		
		TextView smsToggleSubtext = (TextView) activity.findViewById(R.id.sms_toggle_subtext);
		CheckBox smsToggle = (CheckBox) activity.findViewById(R.id.checkbox);
		TextView hikeSmsText = (TextView) activity.findViewById(R.id.hike_text);
		TextView regularSmsText = (TextView) activity.findViewById(R.id.sms_text);

		ChatTheme theme = getCurrentlTheme();

		if (theme == ChatTheme.DEFAULT)
		{
			hikeSmsText.setTextColor(this.getResources().getColor(R.color.sms_choice_unselected));
			regularSmsText.setTextColor(this.getResources().getColor(R.color.sms_choice_unselected));
			smsToggleSubtext.setTextColor(this.getResources().getColor(R.color.sms_choice_unselected));
			smsToggle.setButtonDrawable(R.drawable.sms_checkbox);
			activity.findViewById(R.id.sms_toggle_button).setBackgroundResource(R.drawable.bg_sms_toggle);
		}
		else
		{
			hikeSmsText.setTextColor(this.getResources().getColor(R.color.white));
			regularSmsText.setTextColor(this.getResources().getColor(R.color.white));
			smsToggleSubtext.setTextColor(this.getResources().getColor(R.color.white));
			smsToggle.setButtonDrawable(R.drawable.sms_checkbox_custom_theme);
			activity.findViewById(R.id.sms_toggle_button).setBackgroundResource(theme.smsToggleBgRes());
		}

		boolean smsToggleOn = Utils.getSendSmsPref(activity.getApplicationContext());
		smsToggle.setChecked(smsToggleOn);
		mAdapter.initializeSmsToggleTexts(hikeSmsText, regularSmsText, smsToggleSubtext);
		mAdapter.setSmsToggleSubtext(smsToggleOn);

		smsToggleSubtext.setVisibility(View.VISIBLE);
		smsToggle.setVisibility(View.VISIBLE);
		hikeSmsText.setVisibility(View.VISIBLE);
		regularSmsText.setVisibility(View.VISIBLE);
		smsToggle.setOnCheckedChangeListener(mAdapter);
	}

	/**
	 * This method sets alpha animation on the SMS toggle layout
	 */
	private void animateSMSToggleLayout()
	{
		View SMSToggleButton = (View) activity.findViewById(R.id.sms_toggle_button);
		AlphaAnimation showSMSToggleButton = new AlphaAnimation(0.0f, 1.0f);
		showSMSToggleButton.setDuration(1000);
		SMSToggleButton.startAnimation(showSMSToggleButton);		
	}

	/**
	 * Returns the label for the current conversation
	 * 
	 * @return
	 */
	protected String getConvLabel()
	{
		String tempLabel = mConversation.getLabel();
		tempLabel = Utils.getFirstNameAndSurname(tempLabel);

		return tempLabel;
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case R.string.block_title:
			onBlockUserclicked();
			break;
		case R.string.view_profile:
			openProfileScreen();
			break;
		case R.string.chat_theme:
			showThemePicker();
			break;
		case R.string.add_as_favorite_menu:
			addFavorite();
			break;
		default:
			Logger.d(TAG, "Calling super Class' itemClicked");
			super.itemClicked(item);
		}
	}

	@Override
	protected String getMsisdnMainUser()
	{
		return msisdn;
	}

	private String getBlockedUserLabel()
	{
		return getConvLabel();
	}

	/**
	 * Used to launch Profile Activity from one to one chat thread
	 */
	@Override
	protected void openProfileScreen()
	{
		Intent profileIntent = IntentFactory.getSingleProfileIntent(activity.getApplicationContext(), mConversation.isOnHike(), msisdn);

		activity.startActivity(profileIntent);
	}

	/**
	 * On Call button clicked
	 */
	private void onCallClicked()
	{
		Utils.onCallClicked(activity.getApplicationContext(), msisdn, VoIPUtils.CallSource.CHAT_THREAD);
	}

	/**
	 * Performs actions relevant to One to One Chat Thread for clearing a conversation
	 */
	@Override
	protected void clearConversation()
	{
		removeAllFromUndeliveredMessages();
		hideH20Tip();
		super.clearConversation();
	}

	/**
	 * Spawns a new thread to mark SMS messages as read.
	 */
	@Override
	protected void setSMSReadInNative()
	{
		String threadName = "setSMSRead";
		Thread t = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				Logger.d(TAG, "Marking SMS as read for : " + msisdn);

				ContentValues contentValues = new ContentValues();
				contentValues.put(HikeConstants.SMSNative.READ, 1);

				try
				{
					activity.getContentResolver().update(HikeConstants.SMSNative.INBOX_CONTENT_URI, contentValues, HikeConstants.SMSNative.NUMBER + "=?", new String[] { msisdn });
				}

				catch (Exception e)
				{
					Logger.e(TAG, e.toString());
				}

			}
		}, threadName);

		t.start();
	}

	/**
	 * Overrides {@link ChatThread#onDestroy()}
	 */
	@Override
	public void onDestroy()
	{
		super.onDestroy();

		HikeSharedPreferenceUtil prefsUtil = HikeSharedPreferenceUtil.getInstance();

		if (mAdapter != null && mAdapter.shownSdrToolTip() && (!prefsUtil.getData(HikeMessengerApp.SHOWN_SDR_INTRO_TIP, false)))
		{
			prefsUtil.saveData(HikeMessengerApp.SHOWN_SDR_INTRO_TIP, true);
		}

		if (Utils.isOkHttp())
		{
			cancelFetchLastseenTask();
		}
		else
		{
			resetLastSeenScheduler();
		}
	}

	private void onBulkMessageReceived(Object object)
	{
		HashMap<String, LinkedList<ConvMessage>> messageListMap = (HashMap<String, LinkedList<ConvMessage>>) object;

		LinkedList<ConvMessage> messagesList = messageListMap.get(msisdn);

		String bulkLabel = null;

		/**
		 * Proceeding only if messages list is not null
		 */
		if (messagesList != null)
		{
			JSONArray ids = new JSONArray();

			for (ConvMessage convMessage : messagesList)
			{
				if (activity.hasWindowFocus())
				{
					convMessage.setState(ConvMessage.State.RECEIVED_READ);

					if (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
					{
						ids.put(String.valueOf(convMessage.getMappedMsgID()));
					}
				}

				bulkLabel = convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO ? mConversation.getLabel() : null;

				if (isActivityVisible && SoundUtils.isTickSoundEnabled(activity.getApplicationContext()))
				{
					SoundUtils.playSoundFromRaw(activity.getApplicationContext(), R.raw.received_message);
				}
			}

			sendUIMessage(SET_LABEL, bulkLabel);

			sendUIMessage(BULK_MESSAGE_RECEIVED, messagesList);

			if (ids != null && ids.length() > 0)
			{
				ChatThreadUtils.doBulkMqttPublish(ids, msisdn);
			}
		}
	}

	/**
	 * Adds a complete list of messages at the end of the messages list and updates the UI at once
	 * 
	 * @param messagesList
	 *            The list of messages to be added
	 */

	private void addBulkMessages(LinkedList<ConvMessage> messagesList)
	{
		/**
		 * Proceeding only if messagesList is not null
		 */

		if (messagesList != null)
		{
			/**
			 * If we were showing the typing bubble, we remove it, add the new messages and add the typing bubble again
			 */

			TypingNotification typingNotification = removeTypingNotification();

			mAdapter.addMessages(messagesList, messages.size());

			reachedEnd = false;

			ConvMessage convMessage = messagesList.get(messagesList.size() - 1);

			/**
			 * We add back the typing notification if the message was sent by the user.
			 */

			if (typingNotification != null && convMessage.isSent())
			{
				mAdapter.addMessage(new ConvMessage(typingNotification));
			}

			mAdapter.notifyDataSetChanged();

			/**
			 * Don't scroll to bottom if the user is at older messages. It's possible user might be reading them.
			 */

			tryScrollingToBottom(convMessage, messagesList.size());

		}
	}

	/**
	 * pubSub Thread
	 * 
	 * @param object
	 * @param isJoined
	 */
	private void onUserJoinedOrLeft(Object object, boolean isJoined)
	{
		String pubSubMsisdn = (String) object;

		/**
		 * Proceeding only if we recived a pubSub for the given chat thread
		 */
		if (msisdn.equals(pubSubMsisdn))
		{
			mConversation.setOnHike(isJoined);

			uiHandler.sendEmptyMessage(USER_JOINED_OR_LEFT);
		}
	}

	/**
	 * Runs on the UI Thread
	 */
	private void userJoinedOrLeft()
	{
		setLastSeenTextBasedOnHikeValue(mConversation.isOnHike());

		updateUIForHikeStatus();

		mAdapter.notifyDataSetChanged();
	}

	/**
	 * PubSub thread
	 */
	private void onAppForegrounded()
	{
		if (mContactInfo != null)
		{
			return;
		}

		if (!ChatThreadUtils.shouldShowLastSeen(activity.getApplicationContext(), mContactInfo.getFavoriteType(), mConversation.isOnHike()))
		{
			return;
		}

		uiHandler.sendEmptyMessage(SCHEDULE_LAST_SEEN);
		HAManager.getInstance().recordLastSeenEvent(ChatThread.class.getName(), "onEventRecv", "recv pubsub APP_FOREGROUNDED", msisdn);

	}

	private void startFetchLastSeenTask()
	{
		cancelFetchLastseenTask();
		FetchLastSeenTask fetchLastseenTask = new FetchLastSeenTask(mContactInfo.getMsisdn());
		lastSeenRequestToken = fetchLastseenTask.start();
	}
	
	/**
	 * UI Thread
	 */
	private void scheduleLastSeen()
	{
		if (Utils.isOkHttp())
		{
			startFetchLastSeenTask();
		}
		else
		{
			if (lastSeenScheduler == null)
			{
				lastSeenScheduler = LastSeenScheduler.getInstance(activity.getApplicationContext());
			}
			else
			{
				lastSeenScheduler.stop(false);
			}

			lastSeenScheduler.start(mContactInfo.getMsisdn(), this);
		}
	}

	private void checkAndStartLastSeenTask()
	{
		/*
		 * Making sure nothing is already scheduled wrt last seen.
		 */
		if (Utils.isOkHttp())
		{
			startFetchLastSeenTask();
		}
		else
		{
			resetLastSeenScheduler();
			lastSeenScheduler = LastSeenScheduler.getInstance(activity.getApplicationContext());
			lastSeenScheduler.start(mContactInfo.getMsisdn(), this);
		}
		HAManager.getInstance().recordLastSeenEvent(OneToOneChatThread.class.getName(), "createConversation", null, msisdn);
	}
	
	@Override
	protected void takeActionBasedOnIntent()
	{
		super.takeActionBasedOnIntent();
	}

	@Override
	public void onInflate(ViewStub stub, View inflated)
	{
		switch (stub.getId())
		{
		case R.id.sms_toggle_view_stub:
			setUpSMSViews();
			break;
		default:
			break;
		}
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		if (!mConversation.isOnHike())
		{
			updateChatMetadata();
		}
	}

	@Override
	protected void sendButtonClicked()
	{
		if (!mConversation.isOnHike() && mCredits <= 0)
		{
			if (!Utils.getSendSmsPref(activity.getApplicationContext()))
			{
				Toast.makeText(activity, R.string.zero_sms_hint, Toast.LENGTH_LONG).show();
				return;
			}
		}

		super.sendButtonClicked();
		
	}

	/**
	 * Overrriding API as we have to log this event specific to OneToOne
	 * When to go logging for GC we can go with super
	 */
	@Override
	protected void sendMessage()
	{
		ConvMessage convMessage = createConvMessageFromCompose();

		// 1) user pressed send button i.e sending Text Message
		MsgRelLogManager.startMessageRelLogging(convMessage, MessageType.TEXT);
				
		sendMessage(convMessage);
	}
	

	/**
	 * H20 TIP FUNCTIONS START HERE. Unless explicitly stated, these functions are called on the UI Thread
	 */

	/**
	 * Utility function to insert a message to the undelivered map
	 * 
	 * @param msg
	 */
	private void addToUndeliveredMessages(ConvMessage msg)
	{
		Logger.d(TAG, "Adding to undelivered messages map : " + undeliveredMessages.size());
		undeliveredMessages.put(msg.getMsgID(), msg);
		updateFirstPendingConvMessage();
		scheduleH20Tip();
	}

	/**
	 * Run on UI Thread only
	 * 
	 * @param convMessages
	 */
	private void removeFromUndeliveredMessage(List<ConvMessage> convMessages)
	{
		for (ConvMessage convMessage : convMessages)
		{
			removeFromUndeliveredMessage(convMessage, false);
		}
	}

	/**
	 * Run on UI Thread only
	 * 
	 * @param convMessage
	 */
	private void removeFromUndeliveredMessage(ConvMessage convMessage, boolean isMsgDelivered)
	{
		ConvMessage msg = undeliveredMessages.remove(convMessage.getMsgID());

		if (msg != null)
		{

			if (undeliveredMessages.isEmpty())
			{
				shouldScheduleH20Tip = true;
				hideH20Tip(false, false, false, isMsgDelivered);
			}

			if (firstPendingConvMessage.equals(convMessage))
			{
				updateFirstPendingConvMessage();
			}
		}
	}

	private void removeAllFromUndeliveredMessages()
	{
		hideH20Tip();
		undeliveredMessages.clear();
		updateFirstPendingConvMessage();
		shouldScheduleH20Tip = true;
	}

	private void addAllUndeliverdMessages(List<ConvMessage> messages)
	{
		int i = messages.size() - 1;
		while (i >= 0)
		{
			ConvMessage convMessage = messages.get(i);
			/**
			 * If the convMessage is received, we continue since it could be possible that sent messages above it could be also in Single Tick mode
			 */
			if (!convMessage.isSent())
			{
				i--;
				continue;
			}
			
			else if (convMessage.getState() == State.SENT_CONFIRMED && !convMessage.isSMS() && !convMessage.isBroadcastMessage())
			{
				undeliveredMessages.put(convMessage.getMsgID(), convMessage);
			}
			
			else
			{
				break;
			}

			i--;
		}
		if (firstPendingConvMessage == null)
		{
			updateFirstPendingConvMessage();
		}
		if (!isH20TipShowing())
		{
			scheduleH20Tip();
		}
	}

	/**
	 * Utility method for updating {@link #firstPendingConvMessage}
	 */
	private void updateFirstPendingConvMessage()
	{
		if (undeliveredMessages.isEmpty())
		{
			firstPendingConvMessage = null;
		}
		else
		{
			firstPendingConvMessage = undeliveredMessages.get(undeliveredMessages.keySet().iterator().next());
		}
	}

	/**
	 * Used for scheduling the H20 Tip after sending a message
	 * 
	 */
	private void scheduleH20Tip()
	{
		/**
		 * If the msisdn is international, then don't show tip
		 */
		if (!msisdn.startsWith(HikeConstants.INDIA_COUNTRY_CODE))
		{
			return;
		}

		/**
		 * If the OS is Kitkat or higher, we can't send a regular SMS, so if the user has 0 free SMS left or user ain't online, no point in showing the tip
		 */

		if (Utils.isKitkatOrHigher() && (mCredits == 0 || (!Utils.isUserOnline(activity.getApplicationContext()))))
		{
			return;
		}

		/**
		 * Removing any previously scheduled tips
		 */
		if (uiHandler.hasMessages(SCHEDULE_H20_TIP))
		{
			uiHandler.removeMessages(SCHEDULE_H20_TIP);
		}

		if (firstPendingConvMessage != null)
		{
			long diff = (((long) System.currentTimeMillis() / 1000) - firstPendingConvMessage.getTimestamp());

			if (Utils.isUserOnline(activity.getApplicationContext()) && (diff < DEFAULT_UNDELIVERED_WAIT_TIME) && shouldScheduleH20Tip)
			{
				uiHandler.sendEmptyMessageDelayed(SCHEDULE_H20_TIP, (DEFAULT_UNDELIVERED_WAIT_TIME - diff) * 1000);
			}

			else if (!undeliveredMessages.isEmpty())
			{
				showH20Tip();
			}
		}
	}

	/**
	 * Called from {@link #handleUIMessage(Message)}
	 */
	private void showH20TipFromHandler()
	{
		/**
		 * If the message is null or has been delivered, we return here
		 */
		if (firstPendingConvMessage == null || !isMessageUndelivered(firstPendingConvMessage))
		{
			return;
		}

		long diff = (((long) System.currentTimeMillis() / 1000) - firstPendingConvMessage.getTimestamp());

		if (Utils.isUserOnline(activity.getApplicationContext()) && (diff >= DEFAULT_UNDELIVERED_WAIT_TIME))
		{
			showH20Tip();
		}
	}

	/**
	 * Returns whether the given message is delivered or not.
	 * 
	 * @param convMessage
	 * @return
	 */
	private boolean isMessageUndelivered(ConvMessage convMessage)
	{
		boolean fileUploaded = true;

		if (convMessage.isFileTransferMessage())
		{
			HikeFile hikeFile = convMessage.getMetadata().getHikeFiles().get(0);
			fileUploaded = !TextUtils.isEmpty(hikeFile.getFileKey());
		}

		return ((!convMessage.isSMS() && convMessage.getState().ordinal() < State.SENT_DELIVERED.ordinal()) || (convMessage.isSMS() && convMessage.getState().ordinal() < State.SENT_CONFIRMED
				.ordinal())) && fileUploaded;
	}

	private boolean isH20TipShowing()
	{
		if (hikeToOfflineTipView != null)
		{
			return hikeToOfflineTipView.getVisibility() == View.VISIBLE;
		}
		return false;
	}

	/**
	 * Get the msgId of {@link #firstPendingConvMessage}
	 * 
	 * @return
	 */
	private long getFirstPendingConvMessageId()
	{
		if (firstPendingConvMessage != null)
		{
			return firstPendingConvMessage.getMsgID();
		}

		else
		{
			return -1;
		}
	}

	/**
	 * Method for displaying H20Tip
	 */
	private void showH20Tip()
	{
		if (!mConversation.isOnHike() || isH20TipShowing())
		{
			return;
		}

		if (!HikeMqttPersistence.getInstance().isMessageSent(getFirstPendingConvMessageId()))
		{
			return;
		}
		
		//User online and actionBarView is not null
		
		if ((mContactInfo.getOffline() == 0) && mActionBarView != null)
		{
			mActionBarView.findViewById(R.id.contact_status).setVisibility(View.GONE);
		}

		if (hikeToOfflineTipView == null)
		{
			hikeToOfflineTipView = LayoutInflater.from(activity.getApplicationContext()).inflate(R.layout.hike_to_offline_tip, null);
		}

		hikeToOfflineTipView.clearAnimation();

		setupH20TipViews();

		LinearLayout tipContainer = (LinearLayout) activity.findViewById(R.id.tipContainerBottom);

		if (tipContainer.getChildCount() > 0)
		{
			tipContainer.removeAllViews();
		}

		/**
		 * Hide any other open FTUE Tips
		 */
		mTips.hideTip();

		tipContainer.addView(hikeToOfflineTipView);
		hikeToOfflineTipView.setVisibility(View.VISIBLE);

		scrollListViewOnShowingH20Tip();
		mAdapter.setH20TipShowing(true);
		shouldScheduleH20Tip = false;
	}

	/**
	 * Utility methods for setting up H20Tip Views
	 */

	private void setupH20TipViews()
	{
		if (modeOfChat == H2S_MODE) // Are we in SMS Mode now ?
		{
			((TextView) hikeToOfflineTipView.findViewById(R.id.tip_header)).setText(getResources().getString(R.string.selected_count, mAdapter.getSelectedCount()));
			((TextView) hikeToOfflineTipView.findViewById(R.id.tip_msg)).setText(getResources().getString(R.string.hike_offline_mode_msg, getSelectedFreeSmsCount()));
			((TextView) hikeToOfflineTipView.findViewById(R.id.send_button_text)).setText(R.string.send_uppercase);
			hikeToOfflineTipView.findViewById(R.id.send_button).setVisibility(View.VISIBLE);
		}

		else
		{

			/**
			 * Only when the user has selected native sms as Always we show "Send Paid sms" in all other cases we show heading as "send free sms"
			 */

			if (PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, false)
					&& PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false))
			{
				((TextView) hikeToOfflineTipView.findViewById(R.id.tip_header)).setText(R.string.send_paid_sms);
			}

			else
			{
				((TextView) hikeToOfflineTipView.findViewById(R.id.tip_header)).setText(R.string.send_free_sms);
			}

			((TextView) hikeToOfflineTipView.findViewById(R.id.tip_msg)).setText(getResources().getString(R.string.reciever_is_offline, getConvLabel()));
			((TextView) hikeToOfflineTipView.findViewById(R.id.send_button_text)).setText(R.string.next_uppercase);
			hikeToOfflineTipView.findViewById(R.id.send_button).setVisibility(View.VISIBLE);

			/**
			 * If action mode is on and H20 Tip comes, so we are disabling NextButton to avoid interference
			 */
			if (mActionMode.isActionModeOn())
			{
				setEnabledH20NextButton(false);
			}
		}

		hikeToOfflineTipView.findViewById(R.id.send_button).setOnClickListener(this);
	}

	/**
	 * Utility method to hide H20 Tip
	 */
	private void hideH20Tip()
	{
		hideH20Tip(false, false, false, false);
	}

	private void hideH20Tip(boolean messagesSent, boolean isNativeSMS)
	{
		hideH20Tip(messagesSent, isNativeSMS, false, false);
	}

	private void hideH20Tip(final boolean messagesSent, final boolean isNativeSms, boolean hideWithoutAnimation, boolean calledFromMsgDelivered)
	{
		/**
		 * Proceeding only if the h20tipView is not null
		 */
		if (hikeToOfflineTipView == null)
		{
			return;
		}

		AnimationListener animationListener = new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				if (hikeToOfflineTipView != null)
				{
					hikeToOfflineTipView.setVisibility(View.GONE);
					mAdapter.setH20TipShowing(false);
					if (modeOfChat == H2S_MODE)
					{
						destroyH20Mode();
					}

					((LinearLayout) activity.findViewById(R.id.tipContainerBottom)).removeView(hikeToOfflineTipView);

					/**
					 * When messages are sent we need to show a toast
					 */

					if (messagesSent)
					{
						String toastMsg = isNativeSms ? activity.getString(R.string.regular_sms_sent_confirmation) : activity.getString(R.string.hike_offline_messages_sent_msg,
								mCredits - getSelectedFreeSmsCount());
						Toast.makeText(activity.getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
					}
				}
			}
		};

		if (hikeToOfflineTipView.getAnimation() == null)
		{
			setupH20TipHideAnimation(animationListener, hideWithoutAnimation);

			if (calledFromMsgDelivered)
			{
				/**
				 * We need to update the last seen value because the contact's ls time might have updated while the H20 tip was visible
				 */
				updateLastSeen(mContactInfo.getMsisdn(), mContactInfo.getOffline(), mContactInfo.getLastSeenTime());
			}
		}

	}

	private void setupH20TipHideAnimation(AnimationListener animationListener, boolean hideWithoutAnimation)
	{
		Animation slideDown = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide_down_noalpha);
		slideDown.setDuration(hideWithoutAnimation ? 0 : 400);

		slideDown.setAnimationListener(animationListener);

		hikeToOfflineTipView.startAnimation(slideDown);
	}

	private void scrollListViewOnShowingH20Tip()
	{
		if (mConversationsView.getLastVisiblePosition() > messages.size() - 3)
		{
			uiHandler.sendEmptyMessage(SCROLL_LIST_VIEW);
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.send_button:
			if(modeOfChat == H2S_MODE)
			{
				h20SendClick();
			}
			else if(modeOfChat == H2H_MODE)
			{
				h20NextClick();
			}
			break;
			
		case R.id.block_unknown_contact:
			HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, msisdn);
			break;

		case R.id.add_unknown_contact:
			Utils.addToContacts(activity, msisdn);
			break;
			
		case R.id.overlay_layout:
			/**
			 * Do nothing. We simply eat this event to avoid chat thread window from catching this
			 */
			break;
			
		case R.id.overlay_button:
			onOverlayLayoutClicked((int) v.getTag());
			break;

		default:
			super.onClick(v);
		}
	}
	
	private void h20NextClick()
	{
		HAManager.getInstance().record(HikeConstants.LogEvent.FIRST_OFFLINE_TIP_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
		modeOfChat = H2S_MODE;
		mAdapter.setH20Mode(true);
		initializeH20Mode();
		setupH20TipViews();
	}
	
	/**
	 * Overriding this here since we need to intercept clicks for H20 overlay
	 */
	@Override
	public void onBlueOverLayClick(ConvMessage convMessage, View view)
	{
		if (modeOfChat == H2S_MODE)
		{
			clickedH20Message(convMessage);
		}

		else
		{
			super.onBlueOverLayClick(convMessage, view);
		}
	}

	/**
	 * Cater to a message click in H2S Mode
	 * 
	 * @param message
	 */
	public void clickedH20Message(ConvMessage message)
	{
		if (shouldProcessH20MessageTap(message))
		{
			mAdapter.toggleSelection(message);

			if (!(mAdapter.getSelectedCount() > 0))
			{
				destroyH20Mode();
			}

			setupH20TipViews();
		}
	}

	/**
	 * Indicates whether we should honour the click on a message when in H2S Mode, i.e, when we clicked the H20 tip to select messages
	 * 
	 * @param message
	 * @return
	 */
	private boolean shouldProcessH20MessageTap(ConvMessage message)
	{
		if (message == null || message.getParticipantInfoState() != ParticipantInfoState.NO_INFO || message.getTypingNotification() != null || message.isBlockAddHeader())
		{
			return false;
		}

		if (message.getState() != State.SENT_CONFIRMED || message.isSMS())
		{
			return false;
		}

		return true;
	}

	/**
	 * Resets the {@link #modeOfChat to H2H Mode and clears selection in messages Adapter}
	 */
	private void destroyH20Mode()
	{
		modeOfChat = H2H_MODE;
		mAdapter.setH20Mode(false);
		mAdapter.removeSelection();
		setupH20TipViews();
	}

	/**
	 * Clicked the send button on H20Tip
	 */
	private void h20SendClick()
	{
		HAManager.getInstance().record(HikeConstants.LogEvent.SECOND_OFFLINE_TIP_CLICKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
		
		HashMap<Long, ConvMessage> selectedMessagesMap = mAdapter.getSelectedMessagesMap();

		if (firstPendingConvMessage != null && !selectedMessagesMap.isEmpty())
		{
			/**
			 * Only show H2S option if the user himself is onLine Else show the send via Native SMS dialog
			 */

			if (!Utils.isUserOnline(activity.getApplicationContext()))
			{
				if (!Utils.isKitkatOrHigher())
				{
					showH20SMSDialog(true);
				}
			}
			else
			{
				/**
				 * Only show H2S option as a fallback if messaging Indian numbers
				 */
				if (!Utils.isKitkatOrHigher())
				{
					showH20SMSDialog(!mConversation.getMsisdn().startsWith(HikeConstants.INDIA_COUNTRY_CODE));
				}

				else
				{
					if (mCredits < getSelectedFreeSmsCount())
					{
						Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.kitkat_not_enough_sms, mCredits), Toast.LENGTH_LONG).show();
					}

					/**
					 * Send the messages
					 */
					else
					{
						sendAllMessagesAsSMS(false, getAllUnsentSelectedMessages());
					}
				}
			}

		}
	}

	private void initializeH20Mode()
	{
		hikeToOfflineTipView.findViewById(R.id.send_button).setVisibility(View.VISIBLE);

		for (Long msgid : undeliveredMessages.keySet())
		{
			ConvMessage convMsg = undeliveredMessages.get(msgid);
			if (convMsg.getState() == State.SENT_CONFIRMED)
			{
				mAdapter.selectView(convMsg, true);
			}
		}
	}

	/**
	 * We try to send messages are either Native SMS or Hike SMS based on which preferences are set. If the preferences are not set, we show the SMS Dialog.
	 * 
	 * @param nativeOnly
	 */

	private void showH20SMSDialog(boolean nativeOnly)
	{
		// Trying to send normal Hike SMS
		if (!nativeOnly && mCredits >= getSelectedFreeSmsCount())
		{
			sendAllMessagesAsSMS(false, getAllUnsentSelectedMessages());
			return;
		}

		// Send Undelivered messages as SMS
		if (PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF, false))
		{ // Send Undelivered messages as Native SMS
			if (PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.SEND_UNDELIVERED_AS_NATIVE_PREF, false))
			{ // Is it the default SMS client on devices < Kitkat
				if (!PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
				{
					showSMSClientDialog(false);
					return;
				}

				else
				// Send Messages as Native SMS
				{
					sendAllMessagesAsSMS(true, getAllUnsentSelectedMessages());
					return;
				}
			}
		}

		/**
		 * Show the H20 dialog with checkBoxes to select between Hike SMS or Native SMS
		 */
		else
		{
			this.dialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.SHOW_H20_SMS_DIALOG, this, nativeOnly, getSelectedFreeSmsCount(), mCredits);
		}
	}

	@Override
	public void positiveClicked(HikeDialog dialog)
	{
		switch (dialog.getId())
		{
		case HikeDialogFactory.SHOW_H20_SMS_DIALOG:
			smsDialogSendClick((H20Dialog) dialog);
			break;

		case HikeDialogFactory.SMS_CLIENT_DIALOG:
			dialog.dismiss();
			onSMSClientDialogPositiveClick();
			break;

		default:
			super.positiveClicked(dialog);
		}
	}

	@Override
	public void negativeClicked(HikeDialog dialog)
	{
		switch (dialog.getId())
		{
		case HikeDialogFactory.SMS_CLIENT_DIALOG:
			dialog.dismiss();
			Utils.setReceiveSmsSetting(activity.getApplicationContext(), false);
			this.dialog = null;
			break;

		default:
			super.negativeClicked(dialog);
		}
	}

	/**
	 * To be shown when always/just once is pressed from H20 Dialog from {@link #showSMSDialog(boolean)}
	 * 
	 * @param dialog
	 */
	private void smsDialogSendClick(H20Dialog dialog)
	{
		boolean isHikeSMSChecked = dialog.isHikeSMSChecked();
		
		if (!isHikeSMSChecked)
		{
			HAManager.getInstance().record(HikeConstants.LogEvent.SMS_POPUP_REGULAR_CHECKED, AnalyticsConstants.UI_EVENT, AnalyticsConstants.CLICK_EVENT);
		}

		if (isHikeSMSChecked)
		{
			sendAllMessagesAsSMS(false, getAllUnsentSelectedMessages());
		}

		else
		{
			if (!PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.RECEIVE_SMS_PREF, false))
			{
				showSMSClientDialog(false);
			}

			else
			{
				sendAllMessagesAsSMS(true, getAllUnsentSelectedMessages());
			}
		}

	}

	private void showSMSClientDialog(boolean showingNativeInfoDialog)
	{
		this.dialog = HikeDialogFactory.showDialog(activity, HikeDialogFactory.SMS_CLIENT_DIALOG, this, false, null, showingNativeInfoDialog);
	}

	private void onSMSClientDialogPositiveClick()
	{
		Utils.setReceiveSmsSetting(activity.getApplicationContext(), true);
		sendAllMessagesAsSMS(true, getAllUnsentSelectedMessages());

		if (!activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
		}
	}

	/**
	 * Methods to send Undelivered messages as SMS
	 * 
	 * @param nativeSMS
	 * @param unsentMessages
	 */
	private void sendAllMessagesAsSMS(boolean nativeSMS, List<ConvMessage> unsentMessages)
	{
		Logger.d(TAG, "Unsent messages: " + unsentMessages.size());

		if (nativeSMS)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.SEND_NATIVE_SMS_FALLBACK, unsentMessages);
			messagesSentCloseHikeToOfflineMode(nativeSMS);
			removeFromUndeliveredMessage(unsentMessages);
		}
		else
		{
			if (mConversation.isOnHike())
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.SEND_HIKE_SMS_FALLBACK, unsentMessages);
				messagesSentCloseHikeToOfflineMode(nativeSMS);
				removeFromUndeliveredMessage(unsentMessages);
			}
			else
			{
				for (ConvMessage convMessage : unsentMessages)
				{
					HikeMqttManagerNew.getInstance().sendMessage(convMessage.serialize(), HikeMqttManagerNew.MQTT_QOS_ONE);
					convMessage.setTimestamp(System.currentTimeMillis() / 1000);
				}
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	/**
	 * Returns a list of all unsent selected messages. Also alters their timestamps, since these messages might be potentially sent as SMS.
	 * 
	 * @return
	 */
	private List<ConvMessage> getAllUnsentSelectedMessages()
	{
		List<ConvMessage> unsentMessages = new ArrayList<ConvMessage>();
		final HashMap<Long, ConvMessage> selectedMessagesMap = mAdapter.getSelectedMessagesMap();
		ArrayList<Long> selectedMsgIds = new ArrayList<Long>(mAdapter.getSelectedMessageIds());
		Collections.sort(selectedMsgIds);

		for (int i = 0; i < selectedMsgIds.size(); i++)
		{
			ConvMessage convMessage = selectedMessagesMap.get(selectedMsgIds.get(i));
			if (convMessage == null)
			{
				continue;
			}

			if (!convMessage.isSent())
			{
				break;
			}

			if (!isMessageUndelivered(convMessage))
			{
				break;
			}

			if (convMessage.getState().ordinal() < State.SENT_CONFIRMED.ordinal())
			{
				convMessage.setTimestamp(System.currentTimeMillis() / 1000);
			}

			unsentMessages.add(convMessage);
		}
		return unsentMessages;
	}

	/**
	 * After sending messages as SMS, we close the tip and destroy H20 Mode
	 * 
	 * @param isNativeSMS
	 */
	private void messagesSentCloseHikeToOfflineMode(boolean isNativeSMS)
	{
		destroyH20Mode();
		hideH20Tip(true, isNativeSMS);
	}

	/**
	 * Overridding {@link ChatThread.#deleteMessage(ConvMessage, boolean)}
	 */
	@Override
	protected void deleteMessage(ConvMessage convMessage, boolean deleteMediaFromPhone)
	{
		removeFromUndeliveredMessage(convMessage, false);
		super.deleteMessage(convMessage, deleteMediaFromPhone);
	}

	/**
	 * Calculates the no. of SMSes based on message size
	 * 
	 * @return
	 */
	private int getSelectedFreeSmsCount()
	{
		Collection<ConvMessage> selectedMessages = mAdapter.getSelectedMessagesMap().values();
		int totalMsgLength = Utils.combineInOneSmsString(activity.getApplicationContext(), false, selectedMessages, true).length();

		return (totalMsgLength / DEFAULT_SMS_LENGTH) + 1;
	}

	/**
	 * Used to enable/disable next button upon entering actionMode
	 * 
	 * @param enabled
	 */
	private void setEnabledH20NextButton(boolean enabled)
	{
		if (isH20TipShowing())
		{
			hikeToOfflineTipView.findViewById(R.id.send_button).setEnabled(enabled);
			hikeToOfflineTipView.findViewById(R.id.send_button_text).setEnabled(enabled);
			hikeToOfflineTipView.findViewById(R.id.send_button_tick).setEnabled(enabled);
		}
	}

	/**
	 * Overriding this method to disable the next button if h20 tip is showing
	 */
	@Override
	public void initActionbarActionModeView(int id, View view)
	{
		switch (id)
		{
		case MULTI_SELECT_ACTION_MODE:
			setEnabledH20NextButton(false);
			break;
		}

		super.initActionbarActionModeView(id, view);
	}

	@Override
	public boolean onBackPressed()
	{
		if (modeOfChat == H2S_MODE)
		{
			destroyH20Mode();
			return true;
		}

		else if (isH20TipShowing())
		{
			hideH20Tip();
			return true;
		}
		return super.onBackPressed();
	}
	
	@Override
	protected void destroyActionMode()
	{
		super.destroyActionMode();
		
		if (isH20TipShowing())
		{
			setEnabledH20NextButton(true);
		}
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		if (modeOfChat == H2S_MODE)  // Are we in H2S mode ? 
		{
			return false;
		}

		if (!mConversation.isOnHike() && mCredits <= 0)
		{
			if (!Utils.getSendSmsPref(activity.getApplicationContext()))
			{
				return false;
			}
		}
		
		return super.onDoubleTap(e);
	}

	/*
	 *  Show call icon in chat thread only if:
	 *  1. When voip is activated for self.
	 *  2. Partner is on hike.
	 *  3. Partner not a bot.
	 */
	private boolean shouldShowCallIcon()
	{
		return Utils.isVoipActivated(activity.getApplicationContext()) && mConversation.isOnHike() && !HikeMessengerApp.hikeBotNamesMap.containsKey(msisdn);
	}
	
	protected void showThemePicker()
	{
		setUpThemePicker();
		themePicker.showThemePicker(activity.findViewById(R.id.cb_anchor), currentTheme, R.string.chat_theme_tip, activity.getResources().getConfiguration().orientation);
	}

	/*
	 * Adding user as favorite
	 */
	private void addFavorite()
	{
		FavoriteType favoriteType = FavoriteType.REQUEST_SENT;
		mContactInfo.setFavoriteType(favoriteType);
		Pair<ContactInfo, FavoriteType> favoriteToggle = new Pair<ContactInfo, FavoriteType>(mContactInfo, favoriteType);
		HikeMessengerApp.getPubSub().publish(HikePubSub.FAVORITE_TOGGLED, favoriteToggle);
	}
	
	@Override
	public void onPrepareOverflowOptionsMenu(List<OverFlowMenuItem> overflowItems)
	{
		if (overflowItems == null)
		{
			return;
		}
		
		super.onPrepareOverflowOptionsMenu(overflowItems);
		
		for (OverFlowMenuItem overFlowMenuItem : overflowItems)
		{

			switch (overFlowMenuItem.id)
			{
			case R.string.add_as_favorite_menu:
				/**
				 * Removing favorites option if the user might have pressed on favorites or if we are accidentally showing it
				 */
				if(!mContactInfo.isNotOrRejectedFavourite())
				{
					overflowItems.remove(overFlowMenuItem);
				}
				else
				{
					overFlowMenuItem.enabled = !mConversation.isBlocked();
				}
				break;
				
			case R.string.search:
			case R.string.clear_chat:
				overFlowMenuItem.enabled = !messages.isEmpty() && !mConversation.isBlocked();
				break;
				
			case R.string.email_chat:
				overFlowMenuItem.enabled = !messages.isEmpty();
				break;
				
			case R.string.chat_theme:
				overFlowMenuItem.enabled = !mConversation.isBlocked();
				break;
			}
		}
	}
	
	/**
	 * This runs only on the UI Thread
	 * 
	 * @param isBlocked
	 */
	protected void blockUnBlockUser(boolean isBlocked)
	{
		mConversation.setBlocked(isBlocked);

		if (isBlocked)
		{
			Utils.logEvent(activity.getApplicationContext(), HikeConstants.LogEvent.MENU_BLOCK);
			showBlockOverlay(getBlockedUserLabel());
			mActionBar.updateOverflowMenuItemString(R.string.block_title, activity.getString(R.string.unblock_title));
		}

		else
		{
			mComposeView.setEnabled(true);
			hideOverlay();
			mActionBar.updateOverflowMenuItemString(R.string.block_title, activity.getString(R.string.block_title));
		}
	}
	
	private void blockUser(Object object, boolean isBlocked)
	{
		String mMsisdn = (String) object;

		/**
		 * Proceeding only if the blocked user's msisdn is that of the current chat thread
		 */
		if (mMsisdn.equals(getMsisdnMainUser()))
		{
			sendUIMessage(BLOCK_UNBLOCK_USER, isBlocked);
		}
	}

	private void hideOverlay()
	{
		View mOverlayLayout = activity.findViewById(R.id.overlay_layout);

		if (mOverlayLayout.getVisibility() == View.VISIBLE && activity.hasWindowFocus())
		{
			Animation fadeOut = AnimationUtils.loadAnimation(activity.getApplicationContext(), android.R.anim.fade_out);
			mOverlayLayout.setAnimation(fadeOut);
		}

		mOverlayLayout.setVisibility(View.INVISIBLE);
	}
	
	private void onOverlayLayoutClicked(int tag)
	{
		switch (tag)
		{

		/**
		 * Block Case :
		 */
		case R.string.unblock_title:
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, getMsisdnMainUser());
			break;

		/**
		 * Zero SMS Credits :
		 */
		case R.string.invite_now:
			Utils.logEvent(activity.getApplicationContext(), HikeConstants.LogEvent.INVITE_OVERLAY_BUTTON);
			inviteUser();
			hideOverlay();
			break;
		}
	}

	/**
	 * Invite user
	 */
	private void inviteUser()
	{
		if (mConversation.isOnHike())
		{
			Toast.makeText(activity, R.string.already_hike_user, Toast.LENGTH_LONG).show();
		}

		else
		{
			Utils.sendInviteUtil(new ContactInfo(msisdn, msisdn, mConversation.getConversationName(), msisdn), activity.getApplicationContext(),
					HikeConstants.SINGLE_INVITE_SMS_ALERT_CHECKED, getString(R.string.native_header), getString(R.string.native_info));

		}
	}

	/**
	 * blockOverLay flag indicates whether this is used to block a user or not. This function can also be called from in zero SMS Credits case.
	 * 
	 * @param label
	 * @param formatString
	 * @param overlayBtnText
	 * @param str
	 * @param drawableResId
	 */

	private void showOverlay(String label, String formatString, String overlayBtnText, SpannableString str, int drawableResId, int viewTag)
	{
		Utils.hideSoftKeyboard(activity.getApplicationContext(), mComposeView);

		View mOverlayLayout = activity.findViewById(R.id.overlay_layout);

		if (mOverlayLayout.getVisibility() != View.VISIBLE && activity.hasWindowFocus())
		{
			Animation fadeIn = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
			mOverlayLayout.setAnimation(fadeIn);
		}

		mComposeView.setEnabled(false);

		mOverlayLayout.setVisibility(View.VISIBLE);
		mOverlayLayout.setOnClickListener(this);

		TextView message = (TextView) mOverlayLayout.findViewById(R.id.overlay_message);
		Button overlayBtn = (Button) mOverlayLayout.findViewById(R.id.overlay_button);
		ImageView overlayImg = (ImageView) mOverlayLayout.findViewById(R.id.overlay_image);

		overlayBtn.setOnClickListener(this);
		overlayBtn.setTag(viewTag);

		mComposeView.setEnabled(false);

		overlayImg.setImageResource(R.drawable.ic_no);
		overlayBtn.setText(overlayBtnText);

		message.setText(str);
	}


	/**
	 * Used to call {@link #showOverlay(boolean, String, String, String)} from {@link OneToOneChatThread} 
	 * 
	 * @param label
	 */
	private void showBlockOverlay(String label)
	{
		/**
		 * Making the blocked user's name as bold
		 */
		String formatString = activity.getString(R.string.block_overlay_message);
		String formatted = String.format(formatString, label);
		SpannableString str = new SpannableString(formatted);
		int start = formatString.indexOf("%1$s");
		str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, start + label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		showOverlay(label, formatString, activity.getString(R.string.unblock_title), str, R.drawable.ic_no, R.string.unblock_title);
	}

	/**
	 * Used for giving block and unblock user pubSubs
	 */
	private void onBlockUserclicked()
	{
		if (mConversation.isBlocked())
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.UNBLOCK_USER, msisdn);
		}

		else
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.BLOCK_USER, msisdn);
		}
	}

}