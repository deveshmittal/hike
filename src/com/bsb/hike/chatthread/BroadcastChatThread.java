/**
 * 
 */
package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;

import com.actionbarsherlock.view.Menu;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.Conversation.BroadcastConversation;
import com.bsb.hike.models.Conversation.Conversation;
import com.bsb.hike.models.Conversation.OneToNConversation;

/**
 * @author piyush
 * 
 */
public class BroadcastChatThread extends OneToNChatThread
{

	/**
	 * @param activity
	 * @param msisdn
	 */
	public BroadcastChatThread(ChatThreadActivity activity, String msisdn)
	{
		super(activity, msisdn);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.bsb.hike.chatthread.ChatThread#getPubSubListeners()
	 */
	@Override
    protected String[] getPubSubListeners()
    {
        return new String[] { HikePubSub.GROUP_MESSAGE_DELIVERED_READ, HikePubSub.GROUP_REVIVED, HikePubSub.PARTICIPANT_JOINED_GROUP, HikePubSub.PARTICIPANT_LEFT_GROUP };
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (oneToNConversation != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.broadcast_chat_thread_menu, getOverFlowItems(), this, this);
			return super.onCreateOptionsMenu(menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		return false;
	}
	
	private List<OverFlowMenuItem> getOverFlowItems()
	{

		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.broadcast_profile), 0, 0, R.string.broadcast_profile));

		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		list.add(new OverFlowMenuItem(getString(R.string.add_shortcut), 0, 0, R.string.add_shortcut));
		
		return list;
	}
	
	@Override
	protected void setupActionBar()
	{
		super.setupActionBar();
		
		setAvatar(R.drawable.ic_default_avatar_broadcast);
	}
	
	@Override
	protected Conversation fetchConversation()
	{
		mConversation = oneToNConversation = (BroadcastConversation) mConversationDb.getConversation(msisdn, HikeConstants.MAX_MESSAGES_TO_LOAD_INITIALLY, true);
		return super.fetchConversation();
	}
	
	@Override
	protected void fetchConversationFinished(Conversation conversation)
	{
		oneToNConversation = (BroadcastConversation)conversation;
		super.fetchConversationFinished(conversation);
	}
}
