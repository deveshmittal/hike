package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Message;
import android.text.Editable;
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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.analytics.AnalyticsConstants;
import com.bsb.hike.analytics.HAManager;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.Conversation.MetaData;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.GroupParticipant;
import com.bsb.hike.models.GroupTypingNotification;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.ui.utils.HashSpanWatcher;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.EmoticonTextWatcher;
import com.bsb.hike.utils.IntentFactory;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.SoundUtils;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.CustomFontEditText;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class GroupChatThread extends ChatThread implements HashTagModeListener
{
	private static final int PIN_CREATE_ACTION_MODE = 201;

	private static final int MUTE_CONVERSATION_TOGGLED = 202;

	private static final int LATEST_PIN_DELETED = 203;

	private static final int BULK_MESSAGE_RECEIVED = 204;

	private static final int SHOW_IMP_MESSAGE = 205;

	private static final int GROUP_REVIVED = 206;

	private static final int PARTICIPANT_JOINED_OR_LEFT_GROUP = 207;

	private static final String TAG = "groupchatthread";

	private HashSpanWatcher mHashSpanWatcher;

	protected GroupConversation groupConversation;

	private static final String HASH_PIN = "#pin";

	private static final String PIN_MESSAGE_SEPARATOR = ": ";
	
	private View pinView;

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
		if (groupConversation != null)
		{
			mActionBar.onCreateOptionsMenu(menu, R.menu.group_chat_thread_menu, getOverFlowItems(), this, this);
			updateUnreadPinCount();
			return super.onCreateOptionsMenu(menu);
		}

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.pin_imp:
			showPinCreateView();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private List<OverFlowMenuItem> getOverFlowItems()
	{

		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		int unreadPinCount = 0;
		if (groupConversation != null)
		{
			/**
			 * It could be possible that conversation, (which loads on a background thread) is created before the UI thread calls for overflow menu creation.
			 */
			unreadPinCount = groupConversation.getUnreadPinCount();
		}
		list.add(new OverFlowMenuItem(getString(R.string.group_profile), unreadPinCount, 0, R.string.group_profile));

		for (OverFlowMenuItem item : super.getOverFlowMenuItems())
		{
			list.add(item);
		}
		list.add(new OverFlowMenuItem(isMuted() ? getString(R.string.unmute_group) : getString(R.string.mute_group), 0, 0, R.string.mute_group));
		list.add(new OverFlowMenuItem(getString(R.string.chat_theme_small), 0, 0, R.string.chat_theme));
		return list;
	}

	/**
	 * Returns whether the group is mute or not
	 * 
	 * @return
	 */
	private boolean isMuted()
	{
		/**
		 * Defensive check
		 */

		if (groupConversation == null)
		{
			return false;
		}
		return groupConversation.isMuted();
	}

	@Override
	public void itemClicked(OverFlowMenuItem item)
	{
		switch (item.id)
		{
		case R.string.chat_theme:
			showThemePicker();
			break;
		case R.string.group_profile:
			openProfileScreen();
			break;
		case R.string.mute_group:
			toggleMuteGroup();
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
			groupConversation.setupReadByList(readBy, msgId);
		}

		// fetch theme
		ChatTheme currentTheme = mConversationDb.getChatThemeForMsisdn(msisdn);
		Logger.d("ChatThread", "Calling setchattheme from createConversation");
		groupConversation.setTheme(currentTheme);

		groupConversation.setConvBlocked(ContactManager.getInstance().isBlocked(groupConversation.getGroupOwner()));

		return groupConversation;
	}

	private void fetchImpMessage()
	{
		if (mConversation.getMetaData() != null && mConversation.getMetaData().isShowLastPin(HikeConstants.MESSAGE_TYPE.TEXT_PIN))
		{
			groupConversation.setImpMessage(mConversationDb.getLastPinForConversation(mConversation));
		}
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
		mHashSpanWatcher = new HashSpanWatcher(mComposeView, HASH_PIN, getResources().getColor(R.color.sticky_yellow));
		showTips();
		groupConversation = (GroupConversation) conversation;
		super.fetchConversationFinished(conversation);

		/**
		 * Is the group owner blocked ? If true then show the block overlay with appropriate strings
		 */

		if (groupConversation.isConvBlocked())
		{
			String label = groupConversation.getGroupParticipantFirstName(groupConversation.getGroupOwner());

			showBlockOverlay(label);

		}

		showTips();

		toggleConversationMuteViewVisibility(groupConversation.isMuted());
		toggleGroupLife(groupConversation.getIsGroupAlive());
		addUnreadCountMessage();
		if (groupConversation.getImpMessage() != null)
		{
			showStickyMessageAtTop(groupConversation.getImpMessage(), false);
		}

		updateUnreadPinCount();

	}

	private void showTips()
	{
		mTips = new ChatThreadTips(activity.getBaseContext(), activity.findViewById(R.id.chatThreadParentLayout), new int[] { ChatThreadTips.ATOMIC_ATTACHMENT_TIP,
				ChatThreadTips.ATOMIC_STICKER_TIP, ChatThreadTips.PIN_TIP, ChatThreadTips.STICKER_TIP }, sharedPreference);

		mTips.showTip();
	}

	/**
	 * @param impMessage
	 *            -- ConvMessage to stick to top
	 */
	private void showStickyMessageAtTop(ConvMessage impMessage, boolean playPinAnim)
	{
		// Hiding pin tip if open
		if (impMessage == null)
		{
			Logger.wtf(TAG, "Trying to showStickyPinMessage on a null ConvMessage object");
			return;
		}
		
		mTips.hideTip(ChatThreadTips.PIN_TIP);
		boolean wasPinViewInflated = false;
		if (impMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			if (pinView == null)   //Inflate only if pinView is null
			{
				pinView = LayoutInflater.from(activity).inflate(R.layout.imp_message_text_pin, null);
				wasPinViewInflated = true;
			}
		}

		else
		{
			Logger.wtf(TAG, "got imp message but type is unnknown , type " + impMessage.getMessageType());
			return;
		}
		
		TextView text = (TextView) pinView.findViewById(R.id.text);
		if (impMessage.getMetadata() != null && impMessage.getMetadata().isGhostMessage())
		{
			pinView.findViewById(R.id.main_content).setBackgroundResource(R.drawable.pin_bg_black);
			text.setTextColor(getResources().getColor(R.color.gray));
		}
		String name = "";
		if (impMessage.isSent())
		{
			name = getString(R.string.pin_self) + PIN_MESSAGE_SEPARATOR;
		}
		else
		{
			name = groupConversation.getGroupParticipantFirstName(impMessage.getGroupParticipantMsisdn()) + PIN_MESSAGE_SEPARATOR;
		}

		ForegroundColorSpan fSpan = new ForegroundColorSpan(getResources().getColor(R.color.pin_name_color));
		String str = name + impMessage.getMessage();
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
				boolean result = super.onTouchEvent(widget, buffer, event);
				if (!result)
				{
					showPinHistory(false);
				}
				return result;
			}
		});
		// text.setText(spanStr);

		View cross = pinView.findViewById(R.id.cross);
		cross.setOnClickListener(this);

		pinView.setOnClickListener(new OnClickListener()
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
		ll.addView(pinView, 0);

		/**
		 * If we were composing a new pin and a pin arrives from somewhere else
		 * Then we hide the received pin.
		 * The pin view will be made visible when pinCreateView is Destroyed.
		 */

		if (activity.findViewById(R.id.impMessageCreateView).getVisibility() == View.VISIBLE)
		{
			pinView.setVisibility(View.GONE);
			wasPinViewInflated = false; // To avoid animation, since the message create view will be destroyed soon.
		}
		
		if (playPinAnim && wasPinViewInflated) // If we're inflating pin for the first time, then we animate it.
		{
			ChatThreadUtils.playPinUpAnimation(activity.getApplicationContext(), pinView, R.anim.up_down_fade_in);
		}
		
		// decrement the unread count if message pinned
		ChatThreadUtils.decrementUnreadPInCount(mConversation, isActivityVisible);

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
			e.printStackTrace();
		}

		HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_PIN_METADATA, mConversation);
	}

	private boolean isShowingPin()
	{
		return pinView != null && pinView.getVisibility() == View.VISIBLE;
	}
	
	private boolean wasPinHidden()
	{
		return pinView != null && pinView.getVisibility() == View.GONE;
	}

	private void hidePinFromUI(boolean playAnim)
	{
		if (playAnim)
		{
			ChatThreadUtils.playUpDownAnimation(activity.getApplicationContext(), pinView);
		}
		else
		{
			pinView.setVisibility(View.GONE);
		}
		
		pinView = null;
		// If the pin tip was previously being seen, and it wasn't closed, we need to show it again.
		mTips.showHiddenTip(ChatThreadTips.PIN_TIP);
	}

	private void showPinHistory(boolean viaMenu)
	{
		Intent intent = IntentFactory.getPinHistoryIntent(activity.getApplicationContext(), msisdn);
		activity.startActivity(intent);
		Utils.resetPinUnreadCount(mConversation);

		HAManager.getInstance().record(viaMenu ? HikeConstants.LogEvent.PIN_HISTORY_VIA_MENU : HikeConstants.LogEvent.PIN_HISTORY_VIA_PIN_CLICK, AnalyticsConstants.UI_EVENT,
				AnalyticsConstants.CLICK_EVENT);
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
		groupConversation.setGroupAlive(alive);
		activity.findViewById(R.id.send_message).setEnabled(alive);
		activity.findViewById(R.id.msg_compose).setVisibility(alive ? View.VISIBLE : View.INVISIBLE);
		activity.findViewById(R.id.emo_btn).setEnabled(alive);
		activity.findViewById(R.id.sticker_btn).setEnabled(alive);
		// TODO : Hide popup OR dialog if visible
	}

	@Override
	protected String[] getPubSubListeners()
	{
		return new String[] { HikePubSub.GROUP_MESSAGE_DELIVERED_READ, HikePubSub.MUTE_CONVERSATION_TOGGLED, HikePubSub.LATEST_PIN_DELETED, HikePubSub.CONV_META_DATA_UPDATED,
				HikePubSub.BULK_MESSAGE_RECEIVED, HikePubSub.GROUP_REVIVED, HikePubSub.PARTICIPANT_JOINED_GROUP, HikePubSub.PARTICIPANT_LEFT_GROUP };
	}

	/**
	 * Called from {@link ChatThread}'s {@link #onMessageReceived(Object)}, to handle System messages like User joined group, user left group etc.}
	 * 
	 */

	@Override
	protected void handleSystemMessages()
	{
		ContactManager conMgr = ContactManager.getInstance();
		groupConversation.setGroupParticipantList(conMgr.getGroupParticipants(mConversation.getMsisdn(), false, false));
	}
	
	@Override
	protected void addMessage(ConvMessage convMessage)
	{
		/*
		 * If we were showing the typing bubble, we remove it from the add the new message and add the typing bubble back again
		 */

		TypingNotification typingNotification = removeTypingNotification();

		/**
		 * Pin Message
		 */
		if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
		{
			showStickyMessageAtTop(convMessage, true);
		}

		/**
		 * Adding message to the adapter
		 */
		mAdapter.addMessage(convMessage);

		if (convMessage.isSent())
		{
			groupConversation.setupReadByList(null, convMessage.getMsgID());
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
		Pair<String, Pair<Long, String>> pair = (Pair<String, Pair<Long, String>>) object;
		// If the msisdn don't match we simply return
		if (!mConversation.getMsisdn().equals(pair.first) || messages == null || messages.isEmpty())
		{
			return;
		}
		Long mrMsgId = pair.second.first;
		for (int i = messages.size() - 1; i >= 0; i--)
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
		groupConversation.updateReadByList(participant, mrMsgId);
		uiHandler.sendEmptyMessage(NOTIFY_DATASET_CHANGED);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		switch (type)
		{
		case HikePubSub.GROUP_MESSAGE_DELIVERED_READ:
			onMessageRead(object);
			break;
		case HikePubSub.MUTE_CONVERSATION_TOGGLED:
			onMuteConversationToggled(object);
			break;
		case HikePubSub.LATEST_PIN_DELETED:
			onLatestPinDeleted(object);
			break;
		case HikePubSub.CONV_META_DATA_UPDATED:
			onConvMetadataUpdated(object);
			break;
		case HikePubSub.BULK_MESSAGE_RECEIVED:
			onBulkMessageReceived(object);
			break;
		case HikePubSub.GROUP_REVIVED:
			onGroupRevived(object);
			break;
		case HikePubSub.PARTICIPANT_JOINED_GROUP:
			onParticipantJoinedOrLeftGroup(object, true);
			break;
		case HikePubSub.PARTICIPANT_LEFT_GROUP:
			onParticipantJoinedOrLeftGroup(object, false);
			break;
		default:
			Logger.d(TAG, "Did not find any matching PubSub event in Group ChatThread. Calling super class' onEventReceived");
			super.onEventReceived(type, object);
			break;
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
		case UPDATE_AVATAR:
			setAvatar(R.drawable.ic_default_avatar_group);
			break;
		case MUTE_CONVERSATION_TOGGLED:
			muteConvToggledUIChange((boolean) msg.obj);
			break;
		case LATEST_PIN_DELETED:
			hidePinFromUI((boolean) msg.obj);
			break;
		case BULK_MESSAGE_RECEIVED:
			addBulkMessages((LinkedList<ConvMessage>) msg.obj);
			break;
		case SHOW_IMP_MESSAGE:
			showStickyMessageAtTop((ConvMessage) msg.obj, true);
			break;
		case GROUP_REVIVED:
			handleGroupRevived();
			break;
		case PARTICIPANT_JOINED_OR_LEFT_GROUP:
			incrementGroupParticipants((int) msg.obj);
			break;
		case MESSAGE_RECEIVED:
			addMessage((ConvMessage) msg.obj);
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
		if (!groupConversation.isMuted())
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

	private void showPinCreateView()
	{
		mActionMode.showActionMode(PIN_CREATE_ACTION_MODE, getString(R.string.create_pin), getString(R.string.pin));
		// TODO : dismissPopupWindow was here : gaurav
		
		View content = activity.findViewById(R.id.impMessageCreateView);
		content.setVisibility(View.VISIBLE);
		mComposeView = (CustomFontEditText) content.findViewById(R.id.messageedittext);
		mComposeView.requestFocus();
		mEmoticonPicker.updateET(mComposeView);
		
		View mBottomView = activity.findViewById(R.id.bottom_panel);
		if (mShareablePopupLayout.isKeyboardOpen())
		{ 
			// ifkeyboard is not open, then keyboard will come which will make so much animation on screen
			mBottomView.startAnimation(AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.up_down_lower_part));
		}
		
		mBottomView.setVisibility(View.GONE);
		
		playPinCreateViewAnim();
		
		Utils.showSoftKeyboard(activity.getApplicationContext(), mComposeView);
		
		mComposeView.addTextChangedListener(new EmoticonTextWatcher());
		mComposeView.requestFocus();
		
		content.findViewById(R.id.emo_btn).setOnClickListener(this);
		
		if (mTips.isGivenTipShowing(ChatThreadTips.PIN_TIP))
		{
			mTips.setTipSeen(ChatThreadTips.PIN_TIP);
		}

	}

	private void destroyPinCreateView()
	{
		// AFTER PIN MODE, we make sure mComposeView is reinitialized to message composer compose

		mComposeView = (CustomFontEditText) activity.findViewById(R.id.msg_compose);
		mEmoticonPicker.updateET(mComposeView);
		mComposeView.requestFocus();
		View mBottomView = activity.findViewById(R.id.bottom_panel);
		mBottomView.startAnimation(AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.down_up_lower_part));
		mBottomView.setVisibility(View.VISIBLE);
		playPinCreateDestroyViewAnim();
		
		if (mShareablePopupLayout != null && mShareablePopupLayout.isShowing())
		{
			mShareablePopupLayout.dismiss();
		}
	}
	
	
	private void playPinCreateViewAnim()
	{
		final View view = activity.findViewById(R.id.impMessageCreateView);
		
		if(view == null)
		{
			return;
		}
		
		Animation am = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.up_down_fade_in);
		am.setAnimationListener(new AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
				if (isShowingPin())
				{
					pinView.setVisibility(View.GONE);
				}
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
			}
		});
		view.startAnimation(am);
	}
	
	private void playPinCreateDestroyViewAnim()
	{
		final View view = activity.findViewById(R.id.impMessageCreateView);
		
		if (view == null)
		{
			return;
		}

		Animation an = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.down_up_up_part);
		an.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
				/**
				 * If the pin had been hidden while pinCreate view was shown, now is the best time to make it visible again.
				 */
				if (wasPinHidden())
				{
					pinView.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				view.setVisibility(View.GONE);
			}
		});
		
		view.startAnimation(an);
	}

	/**
	 * Utility method used for sending Pin
	 */
	
	private void sendPin()
	{
		ConvMessage message = createConvMessageFromCompose();
		if (message != null)
		{
			ChatThreadUtils.modifyMessageToPin(activity.getApplicationContext(), message);
			sendMessage(message);
			mActionMode.finish();
		}
	}
	
	/**
	 * Overriding it here because the message oculd be of type Pin, which needs to animated if the pin view is not yet shown.
	 */
	@Override
	protected void sendMessage(ConvMessage convMessage)
	{
		if (convMessage != null)
		{
			addMessage(convMessage);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_SENT, convMessage);

			if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
			{
				recordPinMessage(convMessage.getHashMessage() == HikeConstants.HASH_MESSAGE_TYPE.DEFAULT_MESSAGE);
			}
		}
	}

	@Override
	public void doneClicked(int id)
	{
		if (id == PIN_CREATE_ACTION_MODE)
		{
			sendPin();
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
		if (groupConversation.getIsGroupAlive())
		{
			Utils.logEvent(activity.getApplicationContext(), HikeConstants.LogEvent.GROUP_INFO_TOP_BUTTON);

			Intent intent = IntentFactory.getGroupProfileIntent(activity.getApplicationContext(), msisdn);

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

		if (isShowingPin())
		{
			hidePinFromUI(true);
			Utils.resetPinUnreadCount(groupConversation);
			updateUnreadPinCount();
		}
	}

	@Override
	public void actionModeDestroyed(int id)
	{
		switch (id)
		{
		case PIN_CREATE_ACTION_MODE:
			destroyPinCreateView();
			break;

		default:
			super.actionModeDestroyed(id);
		}
	}

	@Override
	public void onClick(View v)
	{
		Logger.i(TAG, "onclick of view " + v.getId());
		switch (v.getId())
		{
		case R.id.emo_btn:
			emoticonClicked();
			break;
		case R.id.cross: // Pin message bar cross
			hidePin();
			break;
		default:
			super.onClick(v);
		}
	}

	@Override
	public boolean onBackPressed()
	{
		if (mActionMode.whichActionModeIsOn() == PIN_CREATE_ACTION_MODE)
		{
			destroyPinCreateView();
			return true;
		}
		return super.onBackPressed();
	}

	/**
	 * Used to toggle mute and unmute for group
	 */
	private void toggleMuteGroup()
	{
		groupConversation.setIsMuted(!(groupConversation.isMuted()));

		HikeMessengerApp.getPubSub().publish(HikePubSub.MUTE_CONVERSATION_TOGGLED, new Pair<String, Boolean>(groupConversation.getMsisdn(), groupConversation.isMuted()));
	}

	private void onMuteConversationToggled(Object object)
	{
		Pair<String, Boolean> groupMute = (Pair<String, Boolean>) object;

		/**
		 * Proceeding only if we caught an event for this groupchat thread
		 */

		if (groupMute.first.equals(msisdn))
		{
			groupConversation.setIsMuted(groupMute.second);
		}

		sendUIMessage(MUTE_CONVERSATION_TOGGLED, groupMute.second);
	}

	/**
	 * This method handles the UI part of Mute group conversation It is to be strictly called from the UI Thread
	 * 
	 * @param isMuted
	 */
	private void muteConvToggledUIChange(boolean isMuted)
	{
		if (!ChatThreadUtils.checkNetworkError())
		{
			toggleConversationMuteViewVisibility(isMuted);
		}

		/**
		 * Updating the overflow menu item
		 */

		updateOverflowMenuItemString(R.string.mute_group, isMuted ? activity.getString(R.string.unmute_group) : activity.getString(R.string.mute_group));
	}

	private void toggleConversationMuteViewVisibility(boolean isMuted)
	{
		activity.findViewById(R.id.conversation_mute).setVisibility(isMuted ? View.VISIBLE : View.GONE);
	}

	/**
	 * This overrides {@link ChatThread#updateNetworkState()} inorder to toggleGroupMute visibility appropriately
	 */
	@Override
	protected void updateNetworkState()
	{
		super.updateNetworkState();

		if (ChatThreadUtils.checkNetworkError())
		{
			toggleConversationMuteViewVisibility(false);
		}

		else
		{
			toggleConversationMuteViewVisibility(groupConversation.isMuted());
		}
	}

	/**
	 * Used to set unread pin count
	 */
	private void updateUnreadPinCount()
	{
		if (groupConversation != null)
		{
			int unreadPinCount = groupConversation.getUnreadPinCount();
			updateOverflowMenuIndicatorCount(unreadPinCount);
			updateOverflowMenuItemCount(R.string.group_profile, unreadPinCount);
		}
	}

	/**
	 * Called from the pubSub thread
	 * 
	 * @param object
	 */
	private void onLatestPinDeleted(Object object)
	{
		long msgId = (Long) object;

		try
		{
			long pinIdFromMetadata = mConversation.getMetaData().getLastPinId(HikeConstants.MESSAGE_TYPE.TEXT_PIN);

			if (msgId == pinIdFromMetadata)
			{
				sendUIMessage(LATEST_PIN_DELETED, true);
			}
		}

		catch (JSONException e)
		{
			Logger.wtf(TAG, "Got an exception during the pubSub : onLatestPinDeleted " + e.toString());
		}

	}

	/**
	 * Called from the pubSub thread
	 * 
	 * @param object
	 */
	private void onConvMetadataUpdated(Object object)
	{
		if (msisdn.equals(((MetaData) object).getGroupId()))
		{
			groupConversation.setMetaData(((MetaData) object));
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if (isShowingPin())
		{
			ChatThreadUtils.decrementUnreadPInCount(mConversation, isActivityVisible);
		}

		updateUnreadPinCount();
	}

	private void onBulkMessageReceived(Object object)
	{
		HashMap<String, LinkedList<ConvMessage>> messageListMap = (HashMap<String, LinkedList<ConvMessage>>) object;

		LinkedList<ConvMessage> messagesList = messageListMap.get(msisdn);

		String bulkLabel = null;

		/**
		 * Proceeding only if messages list is not null
		 */

		if (messagesList != null)
		{
			ConvMessage pinConvMessage = null;

			JSONArray ids = new JSONArray();

			for (ConvMessage convMessage : messagesList)
			{
				if (convMessage.getMessageType() == HikeConstants.MESSAGE_TYPE.TEXT_PIN)
				{
					pinConvMessage = convMessage;
				}

				if (activity.hasWindowFocus())
				{

					convMessage.setState(ConvMessage.State.RECEIVED_READ);

					if (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO)
					{
						ids.put(String.valueOf(convMessage.getMappedMsgID()));
					}
				}

				if (convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
				{
					ContactManager contactManager = ContactManager.getInstance();
					groupConversation.setGroupParticipantList(contactManager.getGroupParticipants(groupConversation.getMsisdn(), false, false));
				}

				bulkLabel = convMessage.getParticipantInfoState() != ParticipantInfoState.NO_INFO ? groupConversation.getLabel() : null;

				if (isActivityVisible && SoundUtils.isTickSoundEnabled(activity.getApplicationContext()))
				{

					SoundUtils.playSoundFromRaw(activity.getApplicationContext(), R.raw.received_message);
				}

			}

			sendUIMessage(SET_LABEL, bulkLabel);

			sendUIMessage(BULK_MESSAGE_RECEIVED, messagesList);

			sendUIMessage(SHOW_IMP_MESSAGE, pinConvMessage);

			if (ids != null && ids.length() > 0)
			{
				ChatThreadUtils.doBulkMqttPublish(ids, msisdn);
			}
		}
	}

	/**
	 * Adds a complete list of messages at the end of the messages list and updates the UI at once
	 * 
	 * @param messagesList
	 *            The list of messages to be added
	 */

	private void addBulkMessages(LinkedList<ConvMessage> messagesList)
	{
		/**
		 * Proceeding only if the messages are not null
		 */

		if (messagesList != null)
		{

			/**
			 * If we were showing the typing bubble, we remove it, add the new messages and add the typing bubble again
			 */

			TypingNotification typingNotification = removeTypingNotification();

			mAdapter.addMessages(messagesList, messages.size());

			reachedEnd = false;

			ConvMessage convMessage = messagesList.get(messagesList.size() - 1);

			/**
			 * We add back the typing notification if the message was sent by the user.
			 */

			if (typingNotification != null && (!((GroupTypingNotification) typingNotification).getGroupParticipantList().isEmpty()))
			{
				Logger.d(TAG, "Size in chat thread: " + ((GroupTypingNotification) typingNotification).getGroupParticipantList().size());
				mAdapter.addMessage(new ConvMessage(typingNotification));
			}

			mAdapter.notifyDataSetChanged();

			/**
			 * Don't scroll to bottom if the user is at older messages. It's possible user might be reading them.
			 */
			tryScrollingToBottom(convMessage, messagesList.size());

		}
	}

	/**
	 * This method is used to update readByList
	 * 
	 */

	@Override
	protected void updateReadByInLoop(long mrMsgId, Set<String> second)
	{
		for (String msgMsisdn : second)
		{
			groupConversation.updateReadByList(msgMsisdn, mrMsgId);
		}
	}

	private void onGroupRevived(Object object)
	{
		String groupId = (String) object;

		if (msisdn.equals(groupId))
		{
			uiHandler.sendEmptyMessage(GROUP_REVIVED);
		}
	}

	/**
	 * This method is called on the UI thread
	 * 
	 */
	private void handleGroupRevived()
	{
		toggleGroupLife(true);
		groupConversation.setGroupMemberAliveCount(0);
	}

	/**
	 * Called from PubSub Thread
	 * 
	 * @param object
	 */
	private void onParticipantJoinedOrLeftGroup(Object object, boolean joined)
	{
		/**
		 * Received message for current open chatThread
		 */
		if (shouldProcessGCJOrGCK(object)) // Defensive check
		{
			int addPeopleCount = 0;
			if (joined) // Participants added
			{
				JSONObject jObj = (JSONObject) object;

				JSONArray participants = jObj.optJSONArray(HikeConstants.DATA);

				if (participants == null) // If we don't get participants, we simply return here.
				{
					Logger.wtf(TAG, "onParticipantJoinedOrLeftGroup : Getting null participants array in : " + object.toString());
					return;
				}

				addPeopleCount = participants.length();
			}

			else
			// A participant has been kicked out
			{
				addPeopleCount = -1;
			}

			sendUIMessage(PARTICIPANT_JOINED_OR_LEFT_GROUP, addPeopleCount);
		}
	}

	/**
	 * Indicates whether we should process a GCJ/GCK or not.
	 * 
	 * @param object
	 * @return
	 */
	private boolean shouldProcessGCJOrGCK(Object object)
	{
		if (object instanceof JSONObject)
		{
			String msgMsisdn = ((JSONObject) object).optString(HikeConstants.TO);
			if (msgMsisdn != null && groupConversation.getMsisdn().equals(msgMsisdn))
			{
				return true;
			}
		}

		// Default case :

		return false;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
		if (mHashSpanWatcher != null)
		{
			mHashSpanWatcher.onTextChanged(s, start, count, after);
		}
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		if (mHashSpanWatcher != null)
		{
			mHashSpanWatcher.afterTextChanged(s);
		}
	}

	@Override
	protected ConvMessage createConvMessageFromCompose()
	{
		ConvMessage convMessage = super.createConvMessageFromCompose();
		if (convMessage != null)
		{
			if (ChatThreadUtils.checkMessageTypeFromHash(activity.getApplicationContext(), convMessage, HASH_PIN))
			{
				Logger.d(TAG, "Found a pin message type");
				ChatThreadUtils.modifyMessageToPin(activity.getApplicationContext(), convMessage);
			}
		}
		return convMessage;
	}
	
	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		if (!groupConversation.getIsGroupAlive())
		{
			return false;
		}
		else
		{
			return super.onDoubleTap(e);
		}
	}
	
	private void recordPinMessage(boolean viaPinIcon)
	{
		HAManager.getInstance().record(viaPinIcon ? HikeConstants.LogEvent.PIN_POSTED_VIA_ICON : HikeConstants.LogEvent.PIN_POSTED_VIA_HASH_PIN, AnalyticsConstants.UI_EVENT,
				AnalyticsConstants.CLICK_EVENT);
	}
	
	@Override
	protected void showThemePicker()
	{
		super.showThemePicker();
		themePicker.showThemePicker(activity.findViewById(R.id.cb_anchor), currentTheme, R.string.chat_theme_tip_group);
	}
	
	@Override
	protected void handleActionModeOrientationChange(int whichActionMode)
	{
		switch (whichActionMode)
		{
		case PIN_CREATE_ACTION_MODE:
			mActionMode.reInflateActionMode();
			break;
		default:
			super.handleActionModeOrientationChange(whichActionMode);
		}
	}
}