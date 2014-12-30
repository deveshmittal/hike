package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

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
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.HikeHTTPTask;
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
		
		FavoriteType favoriteType = mContactInfo.getFavoriteType();

		if (mConversation.isOnhike())
		{
			addUnkownContactBlockHeader();
		}

		else
		{
			FetchHikeUser.fetchHikeUser(activity.getApplicationContext(), msisdn);
		}
		
		if(shouldShowLastSeen(favoriteType))
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

	private boolean shouldShowLastSeen(FavoriteType favoriteType)
	{
		if ((favoriteType == FavoriteType.FRIEND || favoriteType == FavoriteType.REQUEST_RECEIVED || favoriteType == FavoriteType.REQUEST_RECEIVED_REJECTED)
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
		String[] oneToOneListeners = new String[] { HikePubSub.SMS_CREDIT_CHANGED };
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

	@Override
	protected void onMessageRead(Object object)
	{
		super.onMessageRead(object);
		/*
		 * Right now our logic is to force MR for all the unread messages that is why we need to remove all message from undelivered set
		 * 
		 * if in future we move to MR less than msgId we should modify this logic also
		 */
		if (isUserOnHike())
		{
			mAdapter.removeAllFromUndeliverdMessage();
		}
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
}
