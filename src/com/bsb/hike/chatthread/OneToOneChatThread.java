package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;

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
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.LastSeenScheduler;
import com.bsb.hike.utils.LastSeenScheduler.LastSeenFetchedCallback;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class OneToOneChatThread extends ChatThread implements LastSeenFetchedCallback
{
	private static final String TAG = "oneonechatthread";
	
	private ContactInfo mContactInfo;
	
	private LastSeenScheduler lastSeenScheduler;
	
	private FavoriteType mFavoriteType;
	
	private Dialog smsDialog;
	
	private boolean isOnline;
	
	private static final int CONTACT_ADDED_OR_DELETED = 101;
	
	private static final int SHOW_SMS_SYNC_DIALOG = 102;
	
	private static final int SMS_SYNC_COMPLETE_OR_FAIL = 103;
	
	private static final int UPDATE_LAST_SEEN = 104;
	
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
		chatThreadActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, getOverFlowItems(), this);
		return super.onCreateOptionsMenu(menu);
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
		return chatThreadActionBar.onOptionsItemSelected(item) ? true : super.onOptionsItemSelected(item);
	}

	private List<OverFlowMenuItem> getOverFlowItems()
	{

		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.view_profile), 0, 0, R.string.view_profile));
		list.add(new OverFlowMenuItem(getString(R.string.call), 0, 0, R.string.call));
		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		list.add(new OverFlowMenuItem(isUserBlocked() ? getString(R.string.unblock_title) : getString(R.string.block_title), 0, 0, R.string.block_title));
		return list;
	}

	private boolean isUserBlocked()
	{
		return false;
	}

	private boolean isUserOnHike()
	{
		return true;
	}

	@Override
	protected void initAttachmentPicker(boolean addContact)
	{
		super.initAttachmentPicker(isUserOnHike());
	}

	@Override
	protected void startHikeGallary(boolean onHike)
	{
		super.startHikeGallary(isUserOnHike());
	}
	
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
		
		ChatTheme currentTheme = mConversationDb.getChatThemeForMsisdn(msisdn);
		Logger.d(TAG, "Calling setchattheme from createConversation");
		mConversation.setTheme(currentTheme);
		return mConversation;
	}
	
	@Override
	protected List<ConvMessage> loadMessages()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected int getContentView()
	{
		return R.layout.chatthread;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
	{
		// TODO implement this
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		// TODO implement this
		return false;
	}
	
	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
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
		
		if (ContactManager.getInstance().isBlocked(msisdn))
		{
			showOverlay(true);
		}
		
		
		// TODO : ShowStickerFTUE Tip and H20 Tip. H20 Tip is a part of one to one chatThread. Sticker Tip is a part of super class
		
		if(mConversation.isOnhike())
		{
			//GETTING AN NPE HERE
			// TODO : mAdapter.addAllUndeliverdMessages(messages);
		}
		
	}
	

	private void showOverlay(boolean blockOverLay)
	{
		// TODO Add ShowOverlay
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
	public void lastSeenFetched(String msisdn, int offline, long lastSeenTime)
	{
		// TODO : updateLastSeen(msisdn, offline, lastSeenTime);
	}

	@Override
	protected void sendMessage()
	{
		// TODO : SMS related code -gaurav
		// if (!mConversation.isOnhike() && mCredits <= 0)
		// {
		// boolean nativeSmsPref = Utils.getSendSmsPref(this);
		// if (!nativeSmsPref)
		// {
		// return;
		// }
		// }
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
				HikePubSub.LAST_SEEN_TIME_UPDATED };
		return oneToOneListeners;
	}

	@Override
	protected void addMessage(ConvMessage convMessage)
	{
		TypingNotification typingNotification = null;

		/*
		 * If we were showing the typing bubble, we remove it from the add the new message and add the typing bubble back again
		 */

		if (!messages.isEmpty() && messages.get(messages.size() - 1).getTypingNotification() != null)
		{
			typingNotification = messages.get(messages.size() - 1).getTypingNotification();
			messages.remove(messages.size() - 1);
		}

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
		if (isUserOnHike())
		{
			mAdapter.removeAllFromUndeliverdMessage();
		}
		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}

	@Override
	protected boolean onMessageDelivered(Object object)
	{
		// TODO Auto-generated method stub
		if (super.onMessageDelivered(object))
		{
			if (isUserOnHike())
			{
				Pair<String, Long> pair = (Pair<String, Long>) object;
				long msgID = pair.second;
				// TODO we could keep a map of msgId -> conversation objects
				// somewhere to make this faster
				ConvMessage msg = findMessageById(msgID);
				mAdapter.removeFromUndeliverdMessage(msg, true);
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
			updateLastSeen((ContactInfo) object);
			break;
		default:
			super.onEventReceived(type, object);
		}
	}

	@Override
	protected boolean setStateAndUpdateView(long msgId, boolean updateView)
	{
		// TODO Auto-generated method stub
		if (super.setStateAndUpdateView(msgId, updateView))
		{
			if (isOnHike())
			{
				ConvMessage msg = findMessageById(msgId);
				if (!msg.isSMS())
				{
					mAdapter.addToUndeliverdMessage(msg);
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
			setAvatar();
		}
		// TODO : Add name to actionBar
		// setLabel(pair.second);

		if (messages != null && messages.size() > 0)
		{
			ConvMessage convMessage = messages.get(0);

			if (convMessage.isBlockAddHeader())
			{
				messages.remove(0);
				uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
			}
		}
	}
	
	/**
	 * Performs tasks on the UI thread.
	 */
	@Override
	protected void handleUIMessage(Message msg)
	{
		switch(msg.what)
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
		smsDialog = Utils.showSMSSyncDialog(activity.getApplicationContext(), true);
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
	private void updateLastSeen(ContactInfo contactInfo)
	{
		/**
		 * Proceeding only if the current chat thread is open and we should show the last seen
		 */
		if (msisdn.equals(contactInfo.getMsisdn()) && shouldShowLastSeen())
		{
			/**
			 * Fix for case where server and client values are out of sync
			 */
			
			int offLine = contactInfo.getOffline();
			long lastSeenTime = contactInfo.getLastSeenTime();
			
			if(offLine == 1 && lastSeenTime <= 0)
			{
				return;
			}
			
			/**
			 * Updating mContactInfo object
			 */
			mContactInfo.setOffline(offLine);
			mContactInfo.setLastSeenTime(lastSeenTime);
			
			String lastSeenString = Utils.getLastSeenTimeAsString(activity.getApplicationContext(), lastSeenTime, offLine, false, true);
			
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
			//setLastSeenText(lastSeenString);
		}
	}
}
