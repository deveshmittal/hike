/**
 * 
 */
package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;

import com.bsb.hike.R;
import com.bsb.hike.media.OverFlowMenuItem;

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
		// TODO Auto-generated method stub
		return null;
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
}
