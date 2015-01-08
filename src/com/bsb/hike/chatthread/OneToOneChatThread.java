package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.ui.HikeDialog.HikeDialogListener;
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
	
	private int mCredits;
	
	private boolean mBlockOverlay;
	
	private static final int CONTACT_ADDED_OR_DELETED = 101;
	
	private static final int SHOW_SMS_SYNC_DIALOG = 102;
	
	private static final int SMS_SYNC_COMPLETE_OR_FAIL = 103;
	
	private static final int UPDATE_LAST_SEEN = 104;
	
	private static final int SEND_SMS_PREF_TOGGLED = 105;
	
	private static final int SMS_CREDIT_CHANGED = 106;
	
	private static final int REMOVE_UNDELIVERED_MESSAGES = 107;
	
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
		mActionBar.onCreateOptionsMenu(menu, R.menu.one_one_chat_thread_menu, getOverFlowItems(), this);
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
		return mActionBar.onOptionsItemSelected(item) ? true : super.onOptionsItemSelected(item);
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
		return mUserIsBlocked;
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
		return super.onTouch(v, event);
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
			mUserIsBlocked = true;
			showBlockOverlay(getConvLabel());
		}
		
		
		// TODO : ShowStickerFTUE Tip and H20 Tip. H20 Tip is a part of one to one chatThread. Sticker Tip is a part of super class
		
		if(mConversation.isOnhike())
		{
			//GETTING AN NPE HERE
			// TODO : mAdapter.addAllUndeliverdMessages(messages);
		}
		
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
				HikePubSub.LAST_SEEN_TIME_UPDATED, HikePubSub.SEND_SMS_PREF_TOGGLED };
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
			if (isUserOnHike())
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
			removeSMSToggle();
			nonZeroCredits();
		}

		else
		{
			updateChatMetadata();
		}

	}
	
	/**
	 * Used for making SMS Toggle view invisible
	 */
	private void removeSMSToggle()
	{
		activity.findViewById(R.id.sms_toggle_button).setVisibility(View.GONE);
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

		activity.findViewById(R.id.info_layout).setVisibility(View.GONE);
		activity.findViewById(R.id.emoticon_btn).setVisibility(View.VISIBLE);

		activity.findViewById(R.id.emoticon_btn).setEnabled(true);
		activity.findViewById(R.id.sticker_btn).setEnabled(true);

		if (!mBlockOverlay)
		{
			hideOverlay();
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
		activity.findViewById(R.id.info_layout).setVisibility(View.VISIBLE);
		activity.findViewById(R.id.emoticon_btn).setVisibility(View.GONE);

		activity.findViewById(R.id.emoticon_btn).setEnabled(false);
		activity.findViewById(R.id.sticker_btn).setEnabled(false);

		if (!mConversationDb.wasOverlayDismissed(mConversation.getMsisdn()))
		{
			showZeroCreditsOverlay(getConvLabel(), activity.getApplicationContext().getString(R.string.no_credits), activity.getApplicationContext().getString(R.string.invite_now));
		}

		// TODO : Make tipView a member of superclass ?
		/**
		 * if (tipView != null && tipView.getVisibility() == View.VISIBLE) { Object tag = tipView.getTag();
		 * 
		 * if (tag instanceof TipType && ((TipType)tag == TipType.EMOTICON)) { HikeTip.closeTip(TipType.EMOTICON, tipView, prefs); tipView = null; } }
		 */

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
				setupSMSToggleLayout();
			}
		}
		return false;
	};
	
	/**
	 * Used to setup FreeSMS - Hike SMS Toggle button for Versions below KitKat
	 */
	private void setupSMSToggleLayout()
	{
		activity.findViewById(R.id.sms_toggle_button).setVisibility(View.VISIBLE);
		TextView smsToggleSubtext = (TextView) activity.findViewById(R.id.sms_toggle_subtext);
		CheckBox smsToggle = (CheckBox) activity.findViewById(R.id.checkbox);
		TextView hikeSmsText = (TextView) activity.findViewById(R.id.hike_text);
		TextView regularSmsText = (TextView) activity.findViewById(R.id.sms_text);

		if (currentTheme == ChatTheme.DEFAULT)
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
			activity.findViewById(R.id.sms_toggle_button).setBackgroundResource(currentTheme.smsToggleBgRes());
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
	 * Returns the label for the cuurent conversation
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
		switch(item.id)
		{
		case R.string.block_title:
			onBlockUserclicked();
			break;
		default :
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
	
}
