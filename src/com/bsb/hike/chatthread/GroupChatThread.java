package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import android.os.Message;
import android.util.Pair;
import android.view.View;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
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
		chatThreadActionBar.onCreateOptionsMenu(menu, R.menu.group_chat_thread_menu, getOverFlowItems(), this);
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
		if (item.uniqueness == R.string.chat_theme)
		{
			showThemePicker();
		}
		super.itemClicked(item);
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
		toggleGroupLife(groupConversation.getIsGroupAlive());
		addUnreadCountMessage();
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
		default:
			Logger.d(TAG, "Did not find any matching event in Group ChatThread. Calling super class' handleUIMessage");
			super.handleUIMessage(msg);
			break;
		}
	}
}
