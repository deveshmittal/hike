package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.HashMap;
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
import android.util.Pair;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.LastSeenScheduler;
import com.bsb.hike.utils.LastSeenScheduler.LastSeenFetchedCallback;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

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
	
	private FavoriteType mFavoriteType;
	
	private Dialog smsDialog;
	
	private boolean isOnline;
	
	private int mCredits;
	
	private boolean mBlockOverlay;
	
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
	
	private static final int ADD_TO_UNDELIVERED_MESSAGE = 111;
	
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
		{	mActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, getOverFlowItems(), this, this);
			return super.onCreateOptionsMenu(menu);
		}
		
		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		Logger.i(TAG, "on prepare options menu");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Logger.i(TAG, "menu item click" + item.getItemId());
		switch (item.getItemId())
		{
		case R.id.chat_bg:
			showThemePicker();
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
		list.add(new OverFlowMenuItem(getString(R.string.call), 0, 0, R.string.call));
		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		
		list.add(new OverFlowMenuItem(mConversation.isConvBlocked() ? getString(R.string.unblock_title) : getString(R.string.block_title), 0, 0, R.string.block_title));
		return list;
	}

	//private boolean isUserOnHike()
	//{
	//	return true;
//	}

	
	@Override
	protected Conversation fetchConversation()
	{
		mConversation = mConversationDb.getConversation(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, Utils.isGroupConversation(msisdn));
		if(mConversation == null)
		{
			ContactInfo contactInfo = HikeMessengerApp.getContactManager().getContact(msisdn, true, true);
			mConversation = new Conversation(msisdn, (contactInfo != null) ? contactInfo.getName() : null, contactInfo.isOnhike());
			mConversation.setMessages(HikeConversationsDatabase.getInstance().getConversationThread(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, mConversation, -1));
		}
		
		ChatTheme chatTheme = mConversationDb.getChatThemeForMsisdn(msisdn);
		Logger.d(TAG, "Calling setchattheme from createConversation");
		mConversation.setTheme(chatTheme);
		
		mConversation.setConvBlocked(ContactManager.getInstance().isBlocked(msisdn));
		
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
		
		//TODO : This is a basic working skeleton. This needs to be segragated into separate functions.
		
		mContactInfo = HikeMessengerApp.getContactManager().getContact(msisdn, true, true);
		
		mFavoriteType = mContactInfo.getFavoriteType();

		if (mConversation.isOnhike())
		{
			addUnkownContactBlockHeader();
		}

		else
		{
			FetchHikeUser.fetchHikeUser(activity.getApplicationContext(), msisdn);
		}
		
		if(shouldShowLastSeen())
		{
			
			/*
			 * Making sure nothing is already scheduled wrt last seen.
			 */
			
			resetLastSeenScheduler();
			
			LastSeenScheduler lastSeenScheduler = LastSeenScheduler.getInstance(activity.getApplicationContext());
			lastSeenScheduler.start(mContactInfo.getMsisdn(), this);
		}
		
		/**
		 * If user is blocked
		 */
		
		if (mConversation.isConvBlocked())
		{
			showBlockOverlay(getConvLabel());
		}
		
		
		// TODO : ShowStickerFTUE Tip and H20 Tip. H20 Tip is a part of one to one chatThread. Sticker Tip is a part of super class
		
		if(mConversation.isOnhike())
		{
			//GETTING AN NPE HERE
			// TODO : mAdapter.addAllUndeliverdMessages(messages);
		}
		
	}
	
	private void showTips()
	{
		mTips = new ChatThreadTips(activity.getBaseContext(), activity.findViewById(R.id.chatThreadParentLayout), new int[] { ChatThreadTips.ATOMIC_ATTACHMENT_TIP, ChatThreadTips.ATOMIC_STICKER_TIP, ChatThreadTips.ATOMIC_CHAT_THEME_TIP, ChatThreadTips.STICKER_TIP}, sharedPreference);
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

	private boolean shouldShowLastSeen()
	{
		if ((mFavoriteType == FavoriteType.FRIEND || mFavoriteType == FavoriteType.REQUEST_RECEIVED || mFavoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED)
				&& mConversation.isOnhike())
		{
			return PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean(HikeConstants.LAST_SEEN_PREF, true);
		}
		return false;
	}

	protected void addUnkownContactBlockHeader()
	{
		if (mContactInfo != null && mContactInfo.isUnknownContact() && messages != null && messages.size() >0 )
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
		Logger.d(TAG, " Got lastSeen Time for msisdn : " + contMsisdn + " LastSeenTime : " + lastSeenTime );
		updateLastSeen(contMsisdn, offline, lastSeenTime);
	}

	@Override
	protected void sendMessage()
	{
		super.sendMessage();
	}

	@Override
	protected void onMessageReceived(Object object)
	{
		super.onMessageReceived(object);
	}

	@Override
	protected String[] getPubSubListeners()
	{
		// TODO Add PubSubListeners
		String[] oneToOneListeners = new String[] { HikePubSub.SMS_CREDIT_CHANGED, HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.CONTACT_ADDED, HikePubSub.CONTACT_DELETED,
				HikePubSub.CHANGED_MESSAGE_TYPE, HikePubSub.SHOW_SMS_SYNC_DIALOG, HikePubSub.SMS_SYNC_COMPLETE, HikePubSub.SMS_SYNC_FAIL, HikePubSub.SMS_SYNC_START,
				HikePubSub.LAST_SEEN_TIME_UPDATED, HikePubSub.SEND_SMS_PREF_TOGGLED, HikePubSub.BULK_MESSAGE_RECEIVED, HikePubSub.USER_JOINED, HikePubSub.USER_LEFT, HikePubSub.APP_FOREGROUNDED };
		return oneToOneListeners;
	}

	@Override
	protected void addMessage(ConvMessage convMessage, boolean scrollToLast)
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
		
		super.addMessage(convMessage, scrollToLast);
	}
	
	/**
	 * This overrides {@link ChatThread}'s {@link #onTypingConversationNotificationReceived(Object)}
	 */
	@Override
	protected void onTypingConversationNotificationReceived(Object object)
	{
		TypingNotification typingNotification = (TypingNotification) object;
		
		if(typingNotification == null)
		{
			return;
		}
		
		if (msisdn.equals(typingNotification.getId()))
		{
			sendUIMessage(TYPING_CONVERSATION, typingNotification);
		}
		
		if (shouldShowLastSeen() && mContactInfo.getOffline() != -1)
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
			}
		}
		if (mConversation.isOnhike())
		{
			uiHandler.sendEmptyMessage(REMOVE_UNDELIVERED_MESSAGES);
		}
		
		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}

	@Override
	protected boolean onMessageDelivered(Object object)
	{
		// TODO Auto-generated method stub
		if (super.onMessageDelivered(object))
		{
			if (mConversation.isOnhike())
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
			break;
		case HikePubSub.USER_LEFT:
			onUserJoinedOrLeft(object, false);
			break;
		case HikePubSub.APP_FOREGROUNDED:
			onAppForegrounded();
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
			if (mConversation.isOnhike())
			{
				ConvMessage msg = findMessageById(msgId);
				if (!msg.isSMS())
				{
					sendUIMessage(ADD_TO_UNDELIVERED_MESSAGE, msg);
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
		
		if(contactInfo == null || (!msisdn.equals(contactInfo.getMsisdn())))
		{
			return;
		}
		
		String mContactName = isAdded ? contactInfo.getName() : contactInfo.getMsisdn();
		mConversation.setContactName(mContactName);
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
			setAvatar(R.drawable.ic_default_avatar);
		}
		// TODO : Add name to actionBar
		// setLabel(pair.second);

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
		case UPDATE_AVATAR:
			setAvatar(R.drawable.ic_default_avatar);
			break;
		case BULK_MESSAGE_RECEIVED:
			addBulkMessages((LinkedList<ConvMessage>) msg.obj);
			break;
		case USER_JOINED_OR_LEFT:
			userJoinedOrLeft();
			break;
		case SCHEDULE_LAST_SEEN :
			scheduleLastSeen();
			break;
		case ADD_TO_UNDELIVERED_MESSAGE:
			mAdapter.addToUndeliverdMessage((ConvMessage) msg.obj);
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
		smsDialog = Utils.showSMSSyncDialog(activity, true);
		// TODO :
		// dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG;

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
	 * @param object
	 */
	private void updateLastSeen(String contMsisdn, int offline, long lastSeenTime)
	{
		/**
		 * Proceeding only if the current chat thread is open and we should show the last seen
		 */
		if (msisdn.equals(contMsisdn) && shouldShowLastSeen())
		{
			/**
			 * Fix for case where server and client values are out of sync
			 */
			
			if(offline == 1 && lastSeenTime <= 0)
			{
				return;
			}
			
			/**
			 * Updating mContactInfo object
			 */
			mContactInfo.setOffline(offline);
			mContactInfo.setLastSeenTime(lastSeenTime);
			
			String lastSeenString = Utils.getLastSeenTimeAsString(activity.getApplicationContext(), lastSeenTime, offline, false, true);
			
			isOnline = mContactInfo.getOffline() == 0;
			
			if(isHikOfflineTipShowing() && isOnline)
			{
				/**
				 * If hike to offline tip is showing and server sends that the user is online, we do not update the last seen field until all pending messages are delivered
				 */
				return;
			}
			
			sendUIMessage(UPDATE_LAST_SEEN, lastSeenString);
		}
	}
	
	private boolean isHikOfflineTipShowing()
	{
		// TODO :
		/**
		 * if (hikeToOfflineTipview != null) { /* if hike offline tip is in last state this means it is going to hide;
		 * 
		 * if (((Integer) hikeToOfflineTipview.getTag()) == HIKE_TO_OFFLINE_TIP_STATE_3) { return false; } return hikeToOfflineTipview.getVisibility() == View.VISIBLE; }
		 */
		return false;
	}
	
	/**
	 * Called from the UI Thread
	 * @param lastSeenString
	 */
	private void setLastSeen(String lastSeenString)
	{
		if(isOnline)
		{
			//shouldRunTimerForHikeOfflineTip = true;
		}
		
		if(lastSeenString == null)
		{
			//setLastSeenTextBasedOnHikeValue(mConversation.isOnhike());
		}
		else
		{
			setLastSeenText(lastSeenString);
		}
	}
	
	private void setSMSCredits()
	{
		updateUIForHikeStatus();
		boolean animatedOnce = sharedPreference.getData(HikeConstants.Extras.ANIMATED_ONCE, false);

		if (!animatedOnce)
		{
			sharedPreference.saveData(HikeConstants.Extras.ANIMATED_ONCE, true);
		}

		if ((mCredits % HikeConstants.SHOW_CREDITS_AFTER_NUM == 0 || !animatedOnce) && !mConversation.isOnhike())
		{
			showSMSCounter();
		}
	}
	
	private void updateUIForHikeStatus()
	{
		if (mConversation.isOnhike())
		{
			/**
			 * since this is a view stub, so can return null 
			 */
			if(activity.findViewById(R.id.sms_toggle_button) != null)
			{
				hideView(R.id.sms_toggle_button);
			}
			nonZeroCredits();
		}

		else
		{
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
			
			showOverlay(label, formatString , activity.getString(R.string.invite_now), str, R.drawable.ic_no_credits);
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
	 * @param convMessage
	 */
	private void removeUndeliveredMessages(Object obj)
	{
		if(obj != null)
		{
			mAdapter.removeFromUndeliverdMessage((ConvMessage) obj, true);
			if(mAdapter.getUndeliveredMessagesCount() == 0)
			{
				/*
				 * if all messages are delivered OR we don't have any undelivered messages than only we should reset this timer not on delivery of some message
				 */
				// TODO :
				// chatThread.shouldRunTimerForHikeOfflineTip = true;

				// chatThread.hideHikeToOfflineTip(false, false, false, msgDelivered);
			}
		}
		
		else
		{
			mAdapter.removeAllFromUndeliverdMessage();
			
			// TODO :
			// chatThread.shouldRunTimerForHikeOfflineTip = true;

			// chatThread.hideHikeToOfflineTip();
			
		}
	}

	
	/**
	 * This overrides sendPoke from ChatThread
	 */
	@Override
	protected void sendPoke()
	{
		super.sendPoke();
		
		Utils.vibrateNudgeReceived(activity.getApplicationContext());
	}

	/**
	 * Overrides {@link ChatThread}'s {@link #setupActionBar()}, to set the last seen time
	 */
	@Override
	protected void setupActionBar()
	{
		super.setupActionBar();

		setAvatar(R.drawable.ic_default_avatar);
		
		setLabel(getConvLabel());
		
		setLastSeenTextBasedOnHikeValue(mConversation.isOnhike());

	}

	/**
	 * If the conv is on Hike, then we hide the last seen text, else we show it as "On SMS"
	 * 
	 * @param isConvOnHike
	 */
	private void setLastSeenTextBasedOnHikeValue(boolean isConvOnHike)
	{
		if (isConvOnHike)
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

		if (mLastSeenView.getVisibility() == View.GONE)
		{
			/**
			 * If the view was initially gone and conversation is on hike, we animate the label view in order to make lastSeenView visible
			 */
			if (mConversation.isOnhike())
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

			else
			{
				mLabelView.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * Utility method used for hiding the lastSeenView from the Action Bar
	 */
	private void hideLastSeenText()
	{
		mActionBarView.findViewById(R.id.contact_status).setVisibility(View.GONE);
	}
	
	/**
	 * This calls the super class method with it's own defaultResId
	 */
	@Override
	protected void setAvatar(int defaultResId)
	{
		super.setAvatar(defaultResId);
	}
	
	@Override
	protected boolean updateUIAsPerTheme(ChatTheme theme)
	{
		if(super.updateUIAsPerTheme(theme))
		{
			/**
			 * If the conv is not on hike, neither is the number an international one and the device OS is < v 4.4 Kitkat
			 */
			if (!mConversation.isOnhike() && !Utils.isContactInternational(msisdn) && !Utils.isKitkatOrHigher())
			{
				setupSMSToggleLayout(theme);
			}
		}
		return false;
	};
	
	/**
	 * Used to setup FreeSMS - Hike SMS Toggle button for Versions below KitKat
	 */
	private void setupSMSToggleLayout(ChatTheme theme)
	{
		ViewStub viewStub = (ViewStub) activity.findViewById(R.id.sms_toggle_view_stub);
		
		/**
		 * Inflating it only once when needed on demand.
		 */
		if(viewStub != null)
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
	
	private void setUpSMSViews()
	{
		showView(R.id.sms_toggle_button);
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
	 * Returns the label for the current conversation
	 * 
	 * @return
	 */
	private String getConvLabel()
	{
		String tempLabel = mConversation.getLabel();
		tempLabel = Utils.getFirstName(tempLabel);
		
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
		case R.string.call:
			onCallClicked();
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
	
	@Override
	protected String getBlockedUserLabel()
	{
		return getConvLabel();
	}
	
	/**
	 * Used to launch Profile Activity from one to one chat thread
	 */
	@Override
	protected void openProfileScreen()
	{
		/**
		 * Do nothing if the user is blocked
		 */
		if(mConversation.isConvBlocked())
		{
			return;
		}
		
		Intent profileIntent = IntentFactory.getSingleProfileIntent(activity.getApplicationContext(), mConversation.isOnhike(), msisdn);
		
		activity.startActivity(profileIntent);
	}
	
	/**
	 * On Call button clicked
	 */
	private void onCallClicked()
	{
		Utils.onCallClicked(activity, msisdn);
	}
	
	/**
	 * Performs actions relevant to One to One Chat Thread for clearing a conversation
	 */
	@Override
	protected void clearConversation()
	{
		super.clearConversation();
		// TODO : hideHikeToOfflineTip();
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
				Logger.d(TAG, "Marking SMS as read for : " + msisdn );
				
				ContentValues contentValues = new ContentValues();
				contentValues.put(HikeConstants.SMSNative.READ, 1);
				
				try
				{
					activity.getContentResolver().update(HikeConstants.SMSNative.INBOX_CONTENT_URI, contentValues, HikeConstants.SMSNative.NUMBER + "=?", new String[] { msisdn});
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

		HikeSharedPreferenceUtil prefsUtil = HikeSharedPreferenceUtil.getInstance(activity.getApplicationContext());
		
		if (mAdapter != null && mAdapter.shownSdrToolTip() && (!prefsUtil.getData(HikeMessengerApp.SHOWN_SDR_INTRO_TIP, false)))
		{
			prefsUtil.saveData(HikeMessengerApp.SHOWN_SDR_INTRO_TIP, true);
		}
		
		resetLastSeenScheduler();
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

				if (isActivityVisible && Utils.isPlayTickSound(activity.getApplicationContext()))
				{
					Utils.playSoundFromRaw(activity.getApplicationContext(), R.raw.received_message);
				}
			}

			sendUIMessage(SET_LABEL, bulkLabel);

			sendUIMessage(BULK_MESSAGE_RECEIVED, messagesList);

			if (ids != null && ids.length() > 0)
			{
				doBulkMqttPublish(ids);
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
			mConversation.setOnhike(isJoined);
			
			uiHandler.sendEmptyMessage(USER_JOINED_OR_LEFT);
		}
 	}
	
	/**
	 * Runs on the UI Thread
	 */
	private void userJoinedOrLeft()
	{
		setLastSeenTextBasedOnHikeValue(mConversation.isOnhike());

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
		
		if (!shouldShowLastSeen())
		{
			return;
		}
		
		uiHandler.sendEmptyMessage(SCHEDULE_LAST_SEEN);
	}
	
	/**
	 * UI Thread
	 */
	private void scheduleLastSeen()
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
		if (!mConversation.isOnhike())
		{
			updateChatMetadata();
		}
	}

	@Override
	protected void sendButtonClicked()
	{
		if (!mConversation.isOnhike() && mCredits <= 0)
		{
			if (!Utils.getSendSmsPref(activity.getApplicationContext()))
			{
				return;
			}
		}

		super.sendButtonClicked();
	}
	
}