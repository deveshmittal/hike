/**
 * 
 */
package com.bsb.hike.chatthread;

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

}
