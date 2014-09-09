package com.bsb.hike.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.R.color;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.Utils;

/**
 * This class is responsible for maintaining states of ConvMessages to be used for showing Android notifications.
 * 
 * @author Atul
 * 
 */
public class HikeNotificationMsgStack implements Listener
{

	private static HikeNotificationMsgStack mHikeNotifMsgStack;

	private static Context mContext;

	// Use linked list since the list is going to be small (<8 elements)
	// and there is a lot of removal/sorting is involved.
	private LinkedList<Pair<String, String>> mMessageTitlePairList;

	private Intent mNotificationIntent;

	private HikeConversationsDatabase mConvDb;

	private ArrayList<SpannableString> mBigTextList;

	private ConvMessage mLastInsertedConvMessage;

	private int mNotificationTextLines;

	// Saving ticker text here. A line is inserted for each new message added to the stack.
	// This is cleared once getNotificationTickerText() is called.
	private StringBuilder mTickerText;

	public String lastAddedMsisdn;

	private long latestAddedTimestamp;

	private final int MAX_LINES = 8;

	private boolean sortedTillEnd;

	private int totalNewMessages;

	// Used to store msisdns which is required to display "From X conversations" in notifications.
	private HashSet<String> uniqueMsisdns = new HashSet<String>();

	private boolean forceBlockNotificationSound;

	private static void init(Context context)
	{
		if (mHikeNotifMsgStack == null)
		{
			mContext = context.getApplicationContext();
			mHikeNotifMsgStack = new HikeNotificationMsgStack();

			// We register for NEW_ACTIVITY so that when a chat thread is opened,
			// all unread notifications against the msisdn can be cleared
			HikeMessengerApp.getPubSub().addListener(HikePubSub.NEW_ACTIVITY, mHikeNotifMsgStack);
		}
	}

	/**
	 * This class is responsible for maintaining states of ConvMessages to be used for showing Android notifications.
	 * 
	 * @param argContext
	 * @return
	 */
	public static HikeNotificationMsgStack getInstance(Context argContext)
	{
		init(argContext);
		return mHikeNotifMsgStack;
	}

	private HikeNotificationMsgStack()
	{
		mMessageTitlePairList = new LinkedList<Pair<String, String>>();
		this.mConvDb = HikeConversationsDatabase.getInstance();
	}

	/**
	 * Add a message to existing notification message stack
	 * 
	 * @param argConvMessage
	 */
	public void addConvMessage(ConvMessage argConvMessage)
	{
		if (argConvMessage != null)
		{

			// Add to ticker text string
			Pair<String, String> convMessagePair = HikeNotificationUtils.getNotificationPreview(mContext, argConvMessage);

			addPair(argConvMessage.getMsisdn(), convMessagePair.first);

			mLastInsertedConvMessage = argConvMessage;
		}

		forceBlockNotificationSound = argConvMessage.isSilent();

	}

	/**
	 * Add list of message to existing notification message stack
	 * 
	 * @param argConvMessageList
	 */
	public void addConvMessageList(List<ConvMessage> argConvMessageList)
	{
		if (argConvMessageList != null)
		{
			for (ConvMessage conv : argConvMessageList)
			{
				addConvMessage(conv);
			}
		}
	}

	/**
	 * Add msisdn-message pair to be included in big view notificaitons
	 * 
	 * @param argMsisdn
	 * @param argMessage
	 * @throws IllegalArgumentException
	 */
	public void addMessage(String argMsisdn, String argMessage) throws IllegalArgumentException
	{
		if (TextUtils.isEmpty(argMessage))
		{
			throw new IllegalArgumentException("argMessage cannot be null or empty");
		}
		addPair(argMsisdn, argMessage);
	}

	/**
	 * Adds msisdn-message pair into the notification messages stack. Also performs sorting/grouping/trimming of the existing data
	 * 
	 * @param argMsisdn
	 * @param argMessage
	 */
	private void addPair(String argMsisdn, String argMessage)
	{
		lastAddedMsisdn = argMsisdn;

		if (argMsisdn.equals(HikeNotification.HIKE_STEALTH_MESSAGE_KEY))
		{
			if (uniqueMsisdns.contains(argMsisdn))
			{
				for (Pair<String, String> pair : mMessageTitlePairList)
				{
					if (pair.first.equals(argMsisdn))
					{
						mMessageTitlePairList.remove(pair);
						argMessage = mContext.getString(R.string.stealth_notification_messages);
						break;
					}
				}
			}
		}

		// Sort/Trim/Group message pair list to show messages smartly

		Pair<String, String> newPair = new Pair<String, String>(argMsisdn, argMessage);

		// Check if the latest message has the same msisdn.
		// Else, iterate through the list, move newPair's message group to the bottom
		Iterator<Pair<String, String>> messageTitleListIterator = mMessageTitlePairList.iterator();

		// This is used to store items temporarily while grouping items in the main data list
		LinkedList<Pair<String, String>> tempMessageTitlePairList = new LinkedList<Pair<String, String>>();

		if (!mMessageTitlePairList.isEmpty() && !mMessageTitlePairList.getLast().first.equals(newPair.first))
		{
			while (messageTitleListIterator.hasNext())
			{
				Pair<String, String> messageTitlePair = messageTitleListIterator.next();
				if (messageTitlePair.first.equals(newPair.first))
				{
					tempMessageTitlePairList.add(messageTitlePair);
					messageTitleListIterator.remove();
				}
			}

			if (!tempMessageTitlePairList.isEmpty())
			{
				for (Pair<String, String> pair : tempMessageTitlePairList)
				{
					mMessageTitlePairList.addLast(pair);
				}
			}

			tempMessageTitlePairList.clear();
		}

		mMessageTitlePairList.addLast(newPair);

		messageTitleListIterator = mMessageTitlePairList.iterator();

		sortedTillEnd = false;

		// If list size > MAX_LINES, remove messages starting from top.
		// Skip if there is only 1 message exists from a particular msisdn
		while (mMessageTitlePairList.size() > (MAX_LINES > 7 ? 7 : MAX_LINES) && !sortedTillEnd)
		{
			trimMessagePairList();
		}

		// If this list is still > MAX_LINES, remove oldest message-msisdn pairs
		if (mMessageTitlePairList.size() > (MAX_LINES > 7 ? 7 : MAX_LINES))
		{
			int toBeTrimmed = mMessageTitlePairList.size() - MAX_LINES;

			for (int i = 0; i < toBeTrimmed; i++)
			{
				mMessageTitlePairList.removeFirst();
			}
		}

		latestAddedTimestamp = System.currentTimeMillis();

		totalNewMessages++;

		uniqueMsisdns.add(argMsisdn);

		if (mTickerText != null)
		{
			mTickerText.append("\n" + HikeNotificationUtils.getNameForMsisdn(mContext, argMsisdn) + " - " + argMessage);
		}
		else
		{
			mTickerText = new StringBuilder();
			mTickerText.append(HikeNotificationUtils.getNameForMsisdn(mContext, argMsisdn) + " - " + argMessage);
		}
	}

	/**
	 * Use when number of messages present in messages stack > MAX_LINES
	 */
	private void trimMessagePairList()
	{
		for (int i = 0; i < mMessageTitlePairList.size() - 1; i++)
		{
			Pair<String, String> lhs = mMessageTitlePairList.get(i);

			Pair<String, String> rhs = mMessageTitlePairList.get(i + 1);

			if (lhs == null || rhs == null)
			{
				break;
			}

			if (lhs.first.equals(rhs.first))
			{
				mMessageTitlePairList.remove(lhs);
				break;
			}

			if (i == mMessageTitlePairList.size() - 2)
			{
				sortedTillEnd = true;
			}
		}
	}

	/**
	 * Invalidate object - use if there are changes to notifications messages stack
	 */
	public void invalidateConvMsgList()
	{
		updateNotificationIntent();

		mNotificationTextLines = mMessageTitlePairList.size();

		mNotificationTextLines = mNotificationTextLines > MAX_LINES ? MAX_LINES : mNotificationTextLines;
	}

	/**
	 * Determine whether the messages in stack are from single/multiple msisdns
	 * 
	 * @return null if the messages in stack are from multiple msisdns, else returns msisdn of the only participant
	 */
	public boolean isFromSingleMsisdn()
	{
		return uniqueMsisdns.size() == 1 ? true : false;
	}

	/**
	 * Update notification intent based on msisdns present in the message stack. If multiple msisdns are present, take user to HomeActivity else to the particular chat thread.
	 */
	private void updateNotificationIntent()
	{

		// If new messages belong to different users/groups, redirect the user
		// to conversations list
		if (!isFromSingleMsisdn() || containsStealthMessage())
		{
			mNotificationIntent = new Intent(mContext, HomeActivity.class);
			mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		// if all the new messages belong to a single user/group
		// we've got to invoke the chat thread from here with the respective
		// users
		else
		{
			if (lastAddedMsisdn.contains(mContext.getString(R.string.app_name)))
			{
				mNotificationIntent = new Intent(mContext, HomeActivity.class);
				mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			}
			else
			{

				mNotificationIntent = new Intent(mContext, ChatThread.class);
				mNotificationIntent.putExtra(HikeConstants.Extras.MSISDN, lastAddedMsisdn);
				mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

				/*
				 * notifications appear to be cached, and their .equals doesn't check 'Extra's. In order to prevent the wrong intent being fired, set a data field that's unique to
				 * the conversation we want to open. http://groups .google.com/group/android-developers/browse_thread/thread /e61ec1e8d88ea94d/1fe953564bd11609?#1fe953564bd11609
				 */
				mNotificationIntent.setData((Uri.parse("custom://" + getNotificationId())));
			}
		}
	}

	/**
	 * Returns notification intent based on messages present in stack
	 * 
	 * @return
	 */
	public Intent getNotificationIntent()
	{
		if (mNotificationIntent == null)
		{
			updateNotificationIntent();
		}
		return mNotificationIntent;
	}

	/**
	 * TODO Improve this.
	 * 
	 * @return
	 */
	public int getNotificationIcon()
	{

		if (isFromSingleMsisdn())
		{
			return R.drawable.ic_stat_notify;
		}
		else
		{
			return R.drawable.ic_contact_logo;
		}
	}

	/**
	 * If there are multiple msisdns associated with the messages present in the stack, use a defined constant as notification id, else use the msisdn hashcode.
	 * 
	 * @return
	 */
	public int getNotificationId()
	{
		return HikeNotification.HIKE_SUMMARY_NOTIFICATION_ID;
	}

	/**
	 * Creates big text string based on notification messages stack
	 * 
	 * @return
	 */
	public String getNotificationBigText()
	{
		int addedNotifications = 0;
		setBigTextList(new ArrayList<SpannableString>());
		StringBuilder bigText = new StringBuilder();

		for (Pair<String, String> convMsgPair : mMessageTitlePairList)
		{
			bigText = new StringBuilder();

			String notificationMsgTitle = mContext.getString(R.string.app_name);

			notificationMsgTitle = HikeNotificationUtils.getNameForMsisdn(mContext, convMsgPair.first);

			if (!isFromSingleMsisdn())
			{
				bigText.append("<strong>" + notificationMsgTitle + "</strong>  " + convMsgPair.second);
				getBigTextList().add(HikeNotificationUtils.makeNotificationLine(notificationMsgTitle, convMsgPair.second));
			} 
			else
			{
				bigText.append(convMsgPair.second);
				getBigTextList().add(HikeNotificationUtils.makeNotificationLine(null, convMsgPair.second));
			}

			if (!(addedNotifications == 0))
			{
				bigText.append("\n");
			}

			addedNotifications++;

			if (addedNotifications == mNotificationTextLines)
			{
				break;
			}
		}

		Collections.reverse(getBigTextList());
		bigText = new StringBuilder();
		for (SpannableString text : getBigTextList())
		{
			bigText.append(text);
		}
		return bigText.toString();
	}

	/**
	 * If the notification messages are from different msisdns, returns string/app_name, else returns the name/msisdn of the only participant involved
	 * 
	 * @return
	 */
	public String getSendersCSV()
	{
		StringBuilder notificationMsgTitle = new StringBuilder();

		for (Pair<String, String> entry : mMessageTitlePairList)
		{
			// Take any message from message list since the contact info will
			// remain the same
			String msisdn = entry.first;
			if (Utils.isGroupConversation(msisdn))
			{
				notificationMsgTitle.append(mConvDb.getConversation(msisdn, 1).getLabel() + "\n");
			}
			else
			{
				String name = ContactManager.getInstance().getName(msisdn);
				notificationMsgTitle.append(name + "\n");
			}
		}

		return notificationMsgTitle.toString();
	}

	/**
	 * Returns text of the latest received messages
	 * 
	 * @return
	 */
	public String getNotificationTickerText()
	{
		if (mTickerText != null)
		{
			String tickerTextString = mTickerText.toString();
			mTickerText = null;
			return tickerTextString;
		}
		else
		{
			return "";
		}
	}

	/**
	 * Returns the summary view of messages present in the stack
	 * 
	 * @return
	 */
	public String getNotificationSubText()
	{
		if (isFromSingleMsisdn())
		{
			if (getNewMessages() <= 1)
			{
				return mContext.getString(R.string.one_new_message);
			}
			else
			{
				return String.format(mContext.getString(R.string.num_new_messages), getNewMessages());
			}
		}
		else
		{
			return String.format(mContext.getString(R.string.num_new_conversations), getNewConversations());
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		/**
		 * Here we will determine if the user has seen the messages present in the notification stack. We remove the messages viewed from the stack so that it does not get
		 * displayed on next bulk notification update.
		 */
		if (HikePubSub.NEW_ACTIVITY.equals(type))
		{
			if (object instanceof Activity)
			{
				Activity activity = (Activity) object;
				if ((activity instanceof ChatThread))
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
				}
			}
		}
	}

	/**
	 * Returns lines (List<String>) which can be used for big view summary notification
	 * 
	 * @return
	 */
	public ArrayList<SpannableString> getBigTextList()
	{
		return mBigTextList;
	}

	private void setBigTextList(ArrayList<SpannableString> bigTextList)
	{
		this.mBigTextList = bigTextList;
	}

	/**
	 * Returns number of unread messages in the conversations database
	 * 
	 * @return
	 */
	public int getUnreadMessages()
	{
		return mConvDb.getTotalUnreadMessages();
	}

	/**
	 * Returns ConvMessage object of the last message inserted into the messages stack
	 * 
	 * @return
	 */
	public ConvMessage getLastInsertedConvMessage()
	{
		return mLastInsertedConvMessage;
	}

	public int getNotificationTextLines()
	{
		return mNotificationTextLines;
	}

	/**
	 * Clear all messages in the notifications stack
	 */
	public void resetMsgStack()
	{
		mMessageTitlePairList.clear();
		lastAddedMsisdn = null;
		totalNewMessages = 0;
		uniqueMsisdns.clear();
	}

	/**
	 * Get number of total messages present in stack
	 * 
	 * @return
	 */
	public int getSize()
	{
		if (mMessageTitlePairList != null)
		{
			return mMessageTitlePairList.size();
		}
		else
		{
			return 0;
		}
	}

	/**
	 * Check whether the notification messages stack is empty
	 * 
	 * @return
	 */
	public boolean isEmpty()
	{
		if (getSize() == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Return time-stamp of latest notification received. Used for summary views.
	 * 
	 * @return Latest notification added time-stamp
	 */
	public long getLatestAddedTimestamp()
	{
		return latestAddedTimestamp;
	}

	/**
	 * Provides the number of new messages present in the cached messages stack
	 * 
	 * @return Number of new messages
	 */
	public int getNewMessages()
	{
		return totalNewMessages;
	}

	/**
	 * Provides the number of new conversations present in the cached messages stack
	 * 
	 * @return Number of new conversations
	 */
	public int getNewConversations()
	{
		return uniqueMsisdns.size();
	}

	/**
	 * Returns notification title based on unique msisdns present in messages stack
	 * 
	 * @return Notification title string
	 */
	public String getNotificationTitle()
	{
		if (isFromSingleMsisdn())
		{
			return HikeNotificationUtils.getNameForMsisdn(mContext, lastAddedMsisdn);
		}

		if (getNewMessages() <= 1)
		{
			return mContext.getString(R.string.one_new_message);
		}
		else
		{
			return String.format(mContext.getString(R.string.num_new_messages), getNewMessages());
		}
	}

	public boolean forceBlockNotificationSound()
	{
		return forceBlockNotificationSound;
	}

	public void setTickerText(StringBuilder mTickerText)
	{
		this.mTickerText = mTickerText;
	}

	private boolean containsStealthMessage()
	{
		if (uniqueMsisdns.contains(HikeNotification.HIKE_STEALTH_MESSAGE_KEY))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
