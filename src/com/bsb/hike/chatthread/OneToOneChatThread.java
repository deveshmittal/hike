package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.R;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class OneToOneChatThread extends ChatThread
{
	private static final String TAG = "oneonechatthread";
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
//		mConversation = mConversationDb.getConversation(msisdn, mes,Utils.isGroupConversation(mContactNumber));
		return null;
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
}
