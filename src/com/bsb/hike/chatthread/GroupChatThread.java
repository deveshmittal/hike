package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.Conversation.MetaData;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.PinHistoryActivity;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class GroupChatThread extends ChatThread implements HashTagModeListener
{

	private static final String TAG = "groupchatthread";

	protected GroupConversation groupConversation;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public GroupChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void hashTagModeEnded(String parameter)
	{
		// TODO implement me
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void hashTagModeStarted(String parameter)
	{
		// TODO implement me
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		mActionBar.onCreateOptionsMenu(menu, R.menu.group_chat_thread_menu, getOverFlowItems(), this);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// TODO Auto-generated method stub
		return super.onOptionsItemSelected(item);
	}

	private List<OverFlowMenuItem> getOverFlowItems()
	{

		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.group_profile), 0, 0, R.string.group_profile));

		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		list.add(new OverFlowMenuItem(isMuted() ? getString(R.string.unmute_group) : getString(R.string.mute_group), 0, 0, R.string.mute_group));
		list.add(new OverFlowMenuItem(getString(R.string.chat_theme_small), 0, 0, R.string.chat_theme));
		return list;
	}

	private boolean isMuted()
	{
		return false;
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case R.string.chat_theme:
			showThemePicker();
			break;
		case R.string.block_title:
			onBlockUserclicked();
			break;
		case R.string.group_profile:
			openProfileScreen();
			break;
		default:
			Logger.d(TAG, "Calling super Class' itemClicked");
			super.itemClicked(item);
		}
	}

	/**
	 * NON UI
	 */
	@Override
	protected Conversation fetchConversation()
	{
		Logger.i(TAG, "fetch group conversation " + Thread.currentThread().getName());
		mConversation = groupConversation = (GroupConversation) mConversationDb.getConversation(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, true);
		if (mConversation == null)
		{
			/* the user must have deleted the chat. */
			Message message = Message.obtain();
			message.what = SHOW_TOAST;
			message.arg1 = R.string.invalid_group_chat;
			uiHandler.sendMessage(message);
			return null;
		}

		// Setting a flag which tells us whether the group contains sms users or not.
		boolean hasSmsUser = false;
		for (Entry<String, PairModified<GroupParticipant, String>> entry : groupConversation.getGroupParticipantList().entrySet())
		{
			GroupParticipant groupParticipant = entry.getValue().getFirst();
			if (!groupParticipant.getContactInfo().isOnhike())
			{
				hasSmsUser = true;
				break;
			}
		}
		groupConversation.setHasSmsUser(hasSmsUser);
		// imp message from DB like pin
		fetchImpMessage();
		// Set participant read by list
		Pair<String, Long> pair = HikeConversationsDatabase.getInstance().getReadByValueForGroup(mConversation.getMsisdn());
		if (pair != null)
		{
			String readBy = pair.first;
			long msgId = pair.second;
			(groupConversation).setupReadByList(readBy, msgId);
		}
		
		// fetch theme
		ChatTheme currentTheme = mConversationDb.getChatThemeForMsisdn(msisdn);
		Logger.d("ChatThread", "Calling setchattheme from createConversation");
		groupConversation.setTheme(currentTheme);
		return groupConversation;
	}

	private void fetchImpMessage()
	{
		if (mConversation.getMetaData() != null && mConversation.getMetaData().isShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN)){
			groupConversation.setImpMessage(mConversationDb.getLastPinForConversation(mConversation));
		}
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

	/*
	 * Called in UI Thread
	 * 
	 * @see com.bsb.hike.chatthread.ChatThread#fetchConversationFinished(com.bsb.hike.models.Conversation)
	 */
	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		super.fetchConversationFinished(conversation);
		
		/**
		 * Is the group owner blocked ?  
		 * If true then show the block overlay with appropriate strings
		 */
		
		if(ContactManager.getInstance().isBlocked(groupConversation.getGroupOwner()))
		{
			mUserIsBlocked = true;
			
			String label = groupConversation.getGroupParticipantFirstName(groupConversation.getGroupOwner());
					
			showBlockOverlay(label);
			
		}
		
		toggleGroupLife(groupConversation.getIsGroupAlive());
		addUnreadCountMessage();
		if(groupConversation.getImpMessage()!=null){
			showImpMessage(groupConversation.getImpMessage(), -1);
		}
	}

	
	/**
	 * 
	 * @param impMessage
	 *            -- ConvMessage to stick to top
	 * @param animationId
	 *            -- play animation on message , id must be anim resource id, -1 of no
	 */
	private void showImpMessage(ConvMessage impMessage, int animationId){
		// TODO  : Use HikeActionbarUtil
		SharedPreferences prefs = activity.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, activity.MODE_PRIVATE);
		if (!prefs.getBoolean(HikeMessengerApp.SHOWN_PIN_TIP, false))
		{
		
		Editor editor = prefs.edit();
		editor.putBoolean(HikeMessengerApp.SHOWN_PIN_TIP, true);
		editor.commit();
		}
		
		if (tipView != null)
		{
			tipView.setVisibility(View.GONE);
		}
		if (impMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			tipView = LayoutInflater.from(activity).inflate(R.layout.imp_message_text_pin, null);
		}

		if (tipView == null)
		{
			Logger.e("chatthread", "got imp message but type is unnknown , type " + impMessage.getMessageType());
			return;
		}
		TextView text = (TextView) tipView.findViewById(R.id.text);
		if (impMessage.getMetadata() != null && impMessage.getMetadata().isGhostMessage())
		{
			tipView.findViewById(R.id.main_content).setBackgroundResource(R.drawable.pin_bg_black);
			text.setTextColor(getResources().getColor(R.color.gray));
		}
		String name="";
			if(impMessage.isSent()){
				name="You: ";
			}else{
				if(mConversation instanceof GroupConversation){
				name = ((GroupConversation) mConversation).getGroupParticipantFirstName(impMessage.getGroupParticipantMsisdn()) + ": ";
				}
			}
		
		ForegroundColorSpan fSpan = new ForegroundColorSpan(getResources().getColor(R.color.pin_name_color));
		String str = name+impMessage.getMessage();
		SpannableString spanStr = new SpannableString(str);
		spanStr.setSpan(fSpan, 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		spanStr.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.pin_text_color)), name.length(), str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		CharSequence markedUp = spanStr;
		SmileyParser smileyParser = SmileyParser.getInstance();
		markedUp = smileyParser.addSmileySpans(markedUp, false);
		text.setText(markedUp);
		Linkify.addLinks(text, Linkify.ALL);
		Linkify.addLinks(text, Utils.shortCodeRegex, "tel:");
		text.setMovementMethod(new LinkMovementMethod()
		{
			@Override
			public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event)
			{
				// TODO pin history
				boolean result = super.onTouchEvent(widget, buffer, event);
				if (!result)
				{
					showPinHistory(false);
				}
				return result;
			}
		});
		// text.setText(spanStr);

		View cross = tipView.findViewById(R.id.cross);
		cross.setTag(impMessage);
		cross.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				hidePin();
				
			}
		});

		tipView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showPinHistory(false);
			}
		});
		LinearLayout ll = ((LinearLayout) activity.findViewById(R.id.impMessageContainer));
		if (ll.getChildCount() > 0)
		{
			ll.removeAllViews();
		}
		ll.addView(tipView, 0);
		// to hide pin , if pin create view is visible
		if (activity.findViewById(R.id.impMessageCreateView).getVisibility() == View.VISIBLE)
		{
			tipView.setVisibility(View.GONE);
		}
		if (animationId != -1 && !isShowingPin())
		{
			tipView.startAnimation(AnimationUtils.loadAnimation(activity.getApplicationContext(), animationId));
		}
		tipView.setTag(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
		//decrement the unread count if message pinned
		   
		     decrementUnreadPInCount();
	
	}
	
	public void decrementUnreadPInCount()
	{
		MetaData metadata = mConversation.getMetaData();
		if (!metadata.isPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN) && isActivityVisible)
		{
			try
			{
				metadata.setPinDisplayed(HikeConstants.MESSAGE_TYPE.TEXT_PIN, true);
				metadata.decrementUnreadCount(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, mConversation);
		}
	}
	
	private void hidePin()
	{
		hidePinFromUI(true);
		MetaData metadata = mConversation.getMetaData();
		try
		{
			metadata.setShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN, false);
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, mConversation);
	}
	
	private boolean isShowingPin(){
		return tipView!=null && tipView.getTag() instanceof Integer && ((Integer)tipView.getTag() == HikeConstants.MESSAGE_TYPE.TEXT_PIN);
	}
	
	private void hidePinFromUI(boolean playAnim)
	{
		if (!isShowingPin())
		{
			return;
		}
		if (playAnim)
		{
			playUpDownAnimation(tipView);
		}
		else
		{
			tipView.setVisibility(View.GONE);
			tipView = null;
		}
	}
	
	
	private void showPinHistory(boolean viaMenu)
	{
		Intent intent = new Intent();
		intent.setClass(activity.getApplicationContext(), PinHistoryActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(HikeConstants.TEXT_PINS, msisdn);
		activity.startActivity(intent);
		Utils.resetPinUnreadCount(mConversation);

		if (viaMenu)
		{
			Utils.sendUILogEvent(HikeConstants.LogEvent.PIN_HISTORY_VIA_MENU);
		}
		else
		{
			Utils.sendUILogEvent(HikeConstants.LogEvent.PIN_HISTORY_VIA_PIN_CLICK);
		}
	}
	
	private void addUnreadCountMessage()
	{
		if (groupConversation.getUnreadCount() > 0 && groupConversation.getMessages().size() > 0)
		{
			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getState() == ConvMessage.State.RECEIVED_UNREAD && (message.getTypingNotification() == null))
			{
				long timeStamp = messages.get(messages.size() - mConversation.getUnreadCount()).getTimestamp();
				long msgId = messages.get(messages.size() - mConversation.getUnreadCount()).getMsgID();
				if ((messages.size() - mConversation.getUnreadCount()) > 0)
				{
					messages.add((messages.size() - mConversation.getUnreadCount()), new ConvMessage(mConversation.getUnreadCount(), timeStamp, msgId));
				}
				else
				{
					messages.add(0, new ConvMessage(mConversation.getUnreadCount(), timeStamp, msgId));
				}
			}
		}
	}

	private void toggleGroupLife(boolean alive)
	{
		((GroupConversation) mConversation).setGroupAlive(alive);
		activity.findViewById(R.id.send_message).setEnabled(alive);
		activity.findViewById(R.id.msg_compose).setVisibility(alive ? View.VISIBLE : View.INVISIBLE);
		activity.findViewById(R.id.emo_btn).setEnabled(alive);
		activity.findViewById(R.id.sticker_btn).setEnabled(alive);
		// TODO : Hide popup OR dialog if visible
	}

	@Override
	protected String[] getPubSubListeners()
	{
		return new String[]{HikePubSub.GROUP_MESSAGE_DELIVERED_READ};
	}
	
	/**
	 * Called from {@link ChatThread}'s {@link #onMessageReceived(Object)}, to handle abnormal messages like User joined group, user left group etc.}
	 * 
	 */
	
	@Override
	protected void handleAbnormalMessages()
	{
		ContactManager conMgr = ContactManager.getInstance();
		((GroupConversation) mConversation).setGroupParticipantList(conMgr.getGroupParticipants(mConversation.getMsisdn(), false, false));
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

		// Something related to Pins
		// if(convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		// showImpMessage(convMessage, playPinAnim ? R.anim.up_down_fade_in : -1);

		if (convMessage.isSent())
		{
			((GroupConversation) mConversation).setupReadByList(null, convMessage.getMsgID());
		}

		if (convMessage.getTypingNotification() == null && typingNotification != null)
		{
			if (!((GroupTypingNotification) typingNotification).getGroupParticipantList().isEmpty())
			{
				Logger.d(TAG, "Typing notification in group chat thread: " + ((GroupTypingNotification) typingNotification).getGroupParticipantList().size());
				mAdapter.addMessage(new ConvMessage(typingNotification));
			}
		}
		
		super.addMessage(convMessage);
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
				GroupTypingNotification groupTypingNotification = (GroupTypingNotification) messages.get(messages.size() - 1).getTypingNotification();
				if (groupTypingNotification.getGroupParticipantList().isEmpty())
				{
					messages.remove(messages.size() - 1);
				}

				mAdapter.notifyDataSetChanged();
			}
		}
	}
	
	protected void onMessageRead(Object object)
	{
		Pair<String, Pair<Long,String>> pair = (Pair<String, Pair<Long, String>>) object;
		// If the msisdn don't match we simply return
		if (!mConversation.getMsisdn().equals(pair.first) || messages == null || messages.isEmpty())
		{
			return;
		}
		Long mrMsgId = pair.second.first;
		for (int i = messages.size() - 1 ; i>=0; i--)
		{
			ConvMessage msg = messages.get(i);
			if (msg != null && msg.isSent())
			{
				long id = msg.getMsgID();
				if (id > mrMsgId)
				{
					continue;
				}
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
				{
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
					removeFromMessageMap(msg);
				}
				else
				{
					break;
				}
			}
		}
		String participant = pair.second.second;
		// TODO we could keep a map of msgId -> conversation objects
		// somewhere to make this faster
		groupConversation.updateReadByList(participant,mrMsgId);
		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}
	
	@Override
	public void onEventReceived(String type, Object object)
	{
		switch(type){
		case HikePubSub.GROUP_MESSAGE_DELIVERED_READ:
			onMessageRead(object);
			break;
		}
		super.onEventReceived(type, object);
	}
	
	/**
	 * Performs tasks on the UI thread.
	 */
	@Override
	protected void handleUIMessage(Message msg)
	{
		switch (msg.what)
		{
		case UPDATE_AVATAR:
			setAvatar(R.drawable.ic_default_avatar_group);
			break;
		default:
			Logger.d(TAG, "Did not find any matching event in Group ChatThread. Calling super class' handleUIMessage");
			super.handleUIMessage(msg);
			break;
		}
	}
	
	/**
	 * This overrides sendPoke from ChatThread
	 */
	@Override
	protected void sendPoke()
	{
		super.sendPoke();
		if(!groupConversation.isMuted())
		{
			Utils.vibrateNudgeReceived(activity.getApplicationContext());
		}
	}
	
	@Override
	protected void setupActionBar()
	{
		super.setupActionBar();
		
		setAvatar(R.drawable.ic_default_avatar_group);
		
		setLabel(mConversation.getLabel());
		
		incrementGroupParticipants(0);
	}
	
	/**
	 * Setting the group participant count
	 * 
	 * @param morePeopleCount
	 */
	private void incrementGroupParticipants(int morePeopleCount)
	{
		int numActivePeople = groupConversation.getGroupMemberAliveCount() + morePeopleCount;
		groupConversation.setGroupMemberAliveCount(numActivePeople);

		TextView groupCountTextView = (TextView) mActionBarView.findViewById(R.id.contact_status);

		if (numActivePeople > 0)
		{
			/**
			 * Incrementing numActivePeople by + 1 to add self
			 */
			groupCountTextView.setText(activity.getResources().getString(R.string.num_people, (numActivePeople + 1)));
		}
	}

	@Override
	protected String getMsisdnMainUser()
	{
		return groupConversation.getGroupOwner();
	}
	
	@Override
	protected String getBlockedUserLabel()
	{
		return groupConversation.getGroupParticipantFirstName(groupConversation.getGroupOwner());
	}
	/**
	 * Used to launch Profile Activity from GroupChatThread
	 */
	@Override
	protected void openProfileScreen()
	{
		/**
		 * Proceeding only if the group is alive
		 */
		if(groupConversation.getIsGroupAlive())
		{
			Utils.logEvent(activity.getApplicationContext(), HikeConstants.LogEvent.GROUP_INFO_TOP_BUTTON);
			
			Intent intent = IntentManager.getGroupProfileIntent(activity.getApplicationContext(), msisdn);
			
			activity.startActivity(intent);
		}
	}

	/**
	 * Perform's actions relevant to clear conversation for a GroupChat
	 */
	@Override
	protected void clearConversation()
	{
		super.clearConversation();
		
		// TODO : hidePinFromUI();
		// Utils.resetPinUnreadCount(mConversation);
		// updateOverflowMenuUnreadCount();
		
	}
}
