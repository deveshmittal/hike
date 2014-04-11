package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class ConversationFragment extends SherlockListFragment implements OnItemLongClickListener, Listener, Runnable
{

	private class DeleteConversationsAsyncTask extends AsyncTask<Conversation, Void, Conversation[]>
	{

		Context context;

		public DeleteConversationsAsyncTask(Context context)
		{
			/*
			 * Using application context since that will never be null while the task is running.
			 */
			this.context = context.getApplicationContext();
		}

		@Override
		protected Conversation[] doInBackground(Conversation... convs)
		{
			HikeConversationsDatabase db = null;
			ArrayList<Long> ids = new ArrayList<Long>(convs.length);
			ArrayList<String> msisdns = new ArrayList<String>(convs.length);
			Editor editor = context.getSharedPreferences(HikeConstants.DRAFT_SETTING, Context.MODE_PRIVATE).edit();
			for (Conversation conv : convs)
			{
				/*
				 * Added to check for the null conversation item we add for the group chat tip.
				 */
				if (conv == null)
				{
					continue;
				}
				ids.add(conv.getConvId());
				msisdns.add(conv.getMsisdn());
				editor.remove(conv.getMsisdn());
			}
			editor.commit();

			db = HikeConversationsDatabase.getInstance();
			db.deleteConversation(ids.toArray(new Long[] {}), msisdns);

			return convs;
		}

		@Override
		protected void onPostExecute(Conversation[] deleted)
		{
			if (!isAdded())
			{
				return;
			}
			NotificationManager mgr = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
			for (Conversation conversation : deleted)
			{
				/*
				 * Added to check for the null conversation item we add for the group chat tip.
				 */
				if (conversation == null)
				{
					continue;
				}
				mgr.cancel((int) conversation.getConvId());
				mAdapter.remove(conversation);
				mConversationsByMSISDN.remove(conversation.getMsisdn());
				mConversationsAdded.remove(conversation.getMsisdn());
			}

			mAdapter.notifyDataSetChanged();
			mAdapter.setNotifyOnChange(false);
		}
	}

	private class FTUEGridAdapter extends ArrayAdapter<ContactInfo>
	{

		private IconLoader iconLoader;

		public FTUEGridAdapter(Context context, int textViewResourceId, List<ContactInfo> objects)
		{
			super(context, textViewResourceId, objects);
			iconLoader = new IconLoader(context, 180);
			iconLoader.setDefaultAvatarIfNoCustomIcon(true);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ContactInfo contactInfo = getItem(position);

			if (convertView == null)
			{
				convertView = getLayoutInflater(null).inflate(R.layout.ftue_grid_item, parent, false);
			}

			ImageView avatarImage = (ImageView) convertView.findViewById(R.id.avatar);
			ImageView avatarFrame = (ImageView) convertView.findViewById(R.id.avatar_frame);
			TextView contactName = (TextView) convertView.findViewById(R.id.name);

			avatarFrame.setImageResource(contactInfo.isOnhike() ? R.drawable.frame_avatar_highlight : R.drawable.frame_avatar_highlight_green);
			iconLoader.loadImage(contactInfo.getMsisdn(), true, avatarImage, true);
			// avatarImage.setImageDrawable(IconCacheManager.getInstance()
			// .getIconForMSISDN(contactInfo.getMsisdn(), true));
			contactName.setText(contactInfo.getFirstName());

			convertView.setTag(contactInfo);

			return convertView;
		}
	}

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED, HikePubSub.SERVER_RECEIVED_MSG, HikePubSub.MESSAGE_DELIVERED_READ, HikePubSub.MESSAGE_DELIVERED,
			HikePubSub.NEW_CONVERSATION, HikePubSub.MESSAGE_SENT, HikePubSub.MSG_READ, HikePubSub.ICON_CHANGED, HikePubSub.GROUP_NAME_CHANGED, HikePubSub.CONTACT_ADDED,
			HikePubSub.LAST_MESSAGE_DELETED, HikePubSub.TYPING_CONVERSATION, HikePubSub.END_TYPING_CONVERSATION, HikePubSub.RESET_UNREAD_COUNT, HikePubSub.GROUP_LEFT,
			HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, HikePubSub.CLEAR_CONVERSATION, HikePubSub.CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE, HikePubSub.DISMISS_GROUP_CHAT_TIP };

	private ConversationsAdapter mAdapter;

	private HashMap<String, Conversation> mConversationsByMSISDN;

	private HashSet<String> mConversationsAdded;

	private Comparator<? super Conversation> mConversationsComparator;

	private Handler messageRefreshHandler;

	private View emptyView;

	private enum hikeBotConvStat
	{
		NOTVIEWED, VIEWED, DELETED
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View parent = inflater.inflate(R.layout.conversations, null);

		ListView friendsList = (ListView) parent.findViewById(android.R.id.list);

		emptyView = parent.findViewById(android.R.id.empty);
		setupEmptyView();

		friendsList.setEmptyView(emptyView);

		return parent;
	}

	private void setupEmptyView()
	{

		if (emptyView == null || !isAdded())
		{
			return;
		}

		View ftueNotEmptyView = emptyView.findViewById(R.id.ftue_not_empty);

		if (HomeActivity.ftueList.isEmpty())
		{
			ftueNotEmptyView.setVisibility(View.GONE);
		}
		else
		{
			ftueNotEmptyView.setVisibility(View.VISIBLE);

			Button invite = (Button) emptyView.findViewById(R.id.invite);
			Button newChat = (Button) emptyView.findViewById(R.id.new_chat);

			invite.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					startActivity(new Intent(getActivity(), TellAFriend.class));

					Utils.sendUILogEvent(HikeConstants.LogEvent.INVITE_FROM_GRID);
				}
			});

			newChat.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					startActivity(new Intent(getActivity(), ComposeChatActivity.class));

					Utils.sendUILogEvent(HikeConstants.LogEvent.NEW_CHAT_FROM_GRID);
				}
			});

			GridView ftueGrid = (GridView) emptyView.findViewById(R.id.ftue_grid);
			ftueGrid.setAdapter(new FTUEGridAdapter(getActivity(), -1, HomeActivity.ftueList));
			ftueGrid.setOnItemClickListener(new OnItemClickListener()
			{

				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
				{
					ContactInfo contactInfo = (ContactInfo) view.getTag();
					Intent intent = Utils.createIntentFromContactInfo(contactInfo, true);
					intent.setClass(getActivity(), ChatThread.class);
					startActivity(intent);

					Utils.sendUILogEvent(HikeConstants.LogEvent.GRID_6, contactInfo.getMsisdn());
				}
			});
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mConversationsComparator = new Conversation.ConversationComparator();
		fetchConversations();

		for (TypingNotification typingNotification : HikeMessengerApp.getTypingNotificationSet().values())
		{
			toggleTypingNotification(true, typingNotification);
		}
	}

	@Override
	public void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Conversation conv = (Conversation) mAdapter.getItem(position);

		/*
		 * The item will be null only for group chat tip.
		 */
		if (conv == null)
		{
			((HomeActivity) getActivity()).showOverFlowMenu();
			removeGroupChatTip(conv);
			return;
		}

		Intent intent = Utils.createIntentForConversation(getSherlockActivity(), conv);
		startActivity(intent);

		SharedPreferences prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (conv.getMsisdn().equals(HikeConstants.FTUE_HIKEBOT_MSISDN) && prefs.getInt(HikeConstants.HIKEBOT_CONV_STATE, 0) == hikeBotConvStat.NOTVIEWED.ordinal())
		{
			Editor editor = prefs.edit();
			editor.putInt(HikeConstants.HIKEBOT_CONV_STATE, hikeBotConvStat.VIEWED.ordinal());
			editor.commit();
		}

	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		if (position >= mAdapter.getCount())
		{
			return false;
		}
		ArrayList<String> optionsList = new ArrayList<String>();

		final Conversation conv = (Conversation) mAdapter.getItem(position);
		if (conv == null)
		{
			return false;
		}

		optionsList.add(getString(R.string.shortcut));
		optionsList.add(getString(R.string.email_conversation));
		if (conv instanceof GroupConversation)
		{
			optionsList.add(getString(R.string.delete_leave));
		}
		else
		{
			optionsList.add(getString(R.string.delete));
		}
		optionsList.add(getString(R.string.deleteconversations));

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(getActivity(), R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				String option = options[which];
				if (getString(R.string.shortcut).equals(option))
				{
					Utils.logEvent(getActivity(), HikeConstants.LogEvent.ADD_SHORTCUT);
					Utils.createShortcut(getSherlockActivity(), conv);
				}
				else if (getString(R.string.delete).equals(option))
				{
					Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
					DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
					Utils.executeConvAsyncTask(task, conv);
				}
				else if (getString(R.string.delete_leave).equals(option))
				{
					Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
					leaveGroup(conv);
				}
				else if (getString(R.string.email_conversation).equals(option))
				{
					EmailConversationsAsyncTask task = new EmailConversationsAsyncTask(getSherlockActivity(), ConversationFragment.this);
					Utils.executeConvAsyncTask(task, conv);
				}
				else if (getString(R.string.deleteconversations).equals(option))
				{
					Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
					DeleteAllConversations();
				}

			}
		});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(getResources().getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	private void fetchConversations()
	{
		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		List<Conversation> conversations = new ArrayList<Conversation>();
		List<Conversation> conversationList = db.getConversations();

		/*
		 * Add item for group chat tip.
		 */
		if (!conversationList.isEmpty() && !getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SHOWN_GROUP_CHAT_TIP, false))
		{
			conversations.add(null);
		}

		conversations.addAll(conversationList);

		mConversationsByMSISDN = new HashMap<String, Conversation>(conversations.size());
		mConversationsAdded = new HashSet<String>();

		/*
		 * Use an iterator so we can remove conversations w/ no messages from our list
		 */
		for (Iterator<Conversation> iter = conversations.iterator(); iter.hasNext();)
		{
			Object object = iter.next();
			if (object == null)
			{
				continue;
			}
			Conversation conv = (Conversation) object;
			mConversationsByMSISDN.put(conv.getMsisdn(), conv);
			if (conv.getMessages().isEmpty() && !(conv instanceof GroupConversation))
			{
				iter.remove();
			}
			else
			{
				mConversationsAdded.add(conv.getMsisdn());
			}
		}

		if (mAdapter != null)
		{
			mAdapter.clear();
		}

		mAdapter = new ConversationsAdapter(getActivity(), R.layout.conversation_item, conversations);

		/*
		 * because notifyOnChange gets re-enabled whenever we call notifyDataSetChanged it's simpler to assume it's set to false and always notifyOnChange by hand
		 */
		mAdapter.setNotifyOnChange(false);

		setListAdapter(mAdapter);

		getListView().setOnItemLongClickListener(this);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private void leaveGroup(Conversation conv)
	{
		if (conv == null)
		{
			Logger.d(getClass().getSimpleName(), "Invalid conversation");
			return;
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, conv.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
		deleteConversation(conv);
	}

	private void deleteConversation(Conversation conv)
	{
		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
		Utils.executeConvAsyncTask(task, conv);
	}

	private void toggleTypingNotification(boolean isTyping, TypingNotification typingNotification)
	{
		if (mConversationsByMSISDN == null)
		{
			return;
		}
		String msisdn = typingNotification.getId();
		Conversation conversation = mConversationsByMSISDN.get(msisdn);
		if (conversation == null)
		{
			Logger.d(getClass().getSimpleName(), "Conversation Does not exist");
			return;
		}
		List<ConvMessage> messageList = conversation.getMessages();
		if (messageList.isEmpty())
		{
			Logger.d(getClass().getSimpleName(), "Conversation is empty");
			return;
		}
		ConvMessage message;
		if (isTyping)
		{
			message = messageList.get(messageList.size() - 1);
			if (message.getTypingNotification() == null)
			{
				ConvMessage convMessage = new ConvMessage(typingNotification);
				convMessage.setTimestamp(message.getTimestamp());
				convMessage.setMessage(HikeConstants.IS_TYPING);
				convMessage.setState(State.RECEIVED_UNREAD);
				messageList.add(convMessage);
			}
		}
		else
		{
			message = messageList.get(messageList.size() - 1);
			if (message.getTypingNotification() != null)
			{
				messageList.remove(message);
			}
		}
		run();
	}

	@Override
	public void run()
	{
		if (mAdapter == null)
		{
			return;
		}
		mAdapter.notifyDataSetChanged();
		// notifyDataSetChanged sets notifyonChange to true but we want it to
		// always be false
		mAdapter.setNotifyOnChange(false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEventReceived(String type, Object object)
	{

		if (!isAdded())
		{
			return;
		}
		Logger.d(getClass().getSimpleName(), "Event received: " + type);
		if ((HikePubSub.MESSAGE_RECEIVED.equals(type)) || (HikePubSub.MESSAGE_SENT.equals(type)))
		{
			Logger.d(getClass().getSimpleName(), "New msg event sent or received.");
			ConvMessage message = (ConvMessage) object;
			/* find the conversation corresponding to this message */
			String msisdn = message.getMsisdn();
			final Conversation conv = mConversationsByMSISDN.get(msisdn);

			if (conv == null)
			{
				// When a message gets sent from a user we don't have a
				// conversation for, the message gets
				// broadcasted first then the conversation gets created. It's
				// okay that we don't add it now, because
				// when the conversation is broadcasted it will contain the
				// messages
				return;
			}

			if (Utils.shouldIncrementCounter(message))
			{
				conv.setUnreadCount(conv.getUnreadCount() + 1);
			}

			if (message.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE)
			{
				if (!conv.getMessages().isEmpty())
				{
					ConvMessage prevMessage = conv.getMessages().get(conv.getMessages().size() - 1);
					String metadata = message.getMetadata().serialize();
					message = new ConvMessage(message.getMessage(), message.getMsisdn(), prevMessage.getTimestamp(), prevMessage.getState(), prevMessage.getMsgID(),
							prevMessage.getMappedMsgID(), message.getGroupParticipantMsisdn());
					try
					{
						message.setMetadata(metadata);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}
			}
			// For updating the group name if some participant has joined or
			// left the group
			else if ((conv instanceof GroupConversation) && message.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
			{
				HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
				((GroupConversation) conv).setGroupParticipantList(hCDB.getGroupParticipants(conv.getMsisdn(), false, false));
			}

			final ConvMessage finalMessage = message;

			if (conv.getMessages().size() > 0)
			{
				if (finalMessage.getMsgID() < conv.getMessages().get(conv.getMessages().size() - 1).getMsgID())
				{
					return;
				}
			}
			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					addMessage(conv, finalMessage);

					messageRefreshHandler.removeCallbacks(ConversationFragment.this);
					messageRefreshHandler.postDelayed(ConversationFragment.this, 100);
				}
			});

		}
		else if (HikePubSub.LAST_MESSAGE_DELETED.equals(type))
		{
			Pair<ConvMessage, String> messageMsisdnPair = (Pair<ConvMessage, String>) object;

			final ConvMessage message = messageMsisdnPair.first;
			final String msisdn = messageMsisdnPair.second;

			final boolean conversationEmpty = message == null;

			final Conversation conversation = mConversationsByMSISDN.get(msisdn);

			final List<ConvMessage> messageList = new ArrayList<ConvMessage>(1);

			if (!conversationEmpty)
			{
				if (conversation == null)
				{
					return;
				}
				messageList.add(message);
			}
			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (conversationEmpty)
					{
						mConversationsByMSISDN.remove(msisdn);
						mConversationsAdded.remove(msisdn);
						mAdapter.remove(conversation);
					}
					else
					{
						conversation.setMessages(messageList);
					}
					mAdapter.sort(mConversationsComparator);
					mAdapter.notifyDataSetChanged();
					// notifyDataSetChanged sets notifyonChange to true but we
					// want it to always be false
					mAdapter.setNotifyOnChange(false);
				}
			});
		}
		else if (HikePubSub.NEW_CONVERSATION.equals(type))
		{
			final Conversation conversation = (Conversation) object;
			if (HikeMessengerApp.hikeBotNamesMap.containsKey(conversation.getMsisdn()))
			{
				conversation.setContactName(HikeMessengerApp.hikeBotNamesMap.get(conversation.getMsisdn()));
			}
			Logger.d(getClass().getSimpleName(), "New Conversation. Group Conversation? " + (conversation instanceof GroupConversation));
			mConversationsByMSISDN.put(conversation.getMsisdn(), conversation);
			if (conversation.getMessages().isEmpty() && !(conversation instanceof GroupConversation))
			{
				return;
			}

			mConversationsAdded.add(conversation.getMsisdn());

			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				public void run()
				{
					mAdapter.add(conversation);
					if (conversation instanceof GroupConversation)
					{
						mAdapter.notifyDataSetChanged();
					}
					mAdapter.setNotifyOnChange(false);
				}
			});
		}
		else if (HikePubSub.MSG_READ.equals(type))
		{
			String msisdn = (String) object;
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			if (conv == null)
			{
				/*
				 * We don't really need to do anything if the conversation does not exist.
				 */
				return;
			}
			/*
			 * look for the latest received messages and set them to read. Exit when we've found some read messages
			 */
			List<ConvMessage> messages = conv.getMessages();
			for (int i = messages.size() - 1; i >= 0; --i)
			{
				ConvMessage msg = messages.get(i);
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.RECEIVED_READ.ordinal()))
				{
					ConvMessage.State currentState = msg.getState();
					msg.setState(ConvMessage.State.RECEIVED_READ);
					if (currentState == ConvMessage.State.RECEIVED_READ)
					{
						break;
					}
				}
			}

			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(this);
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_CONFIRMED.ordinal()))
			{
				msg.setState(ConvMessage.State.SENT_CONFIRMED);

				if (getActivity() == null)
				{
					return;
				}
				getActivity().runOnUiThread(this);
			}
		}
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))
		{
			Pair<String, long[]> pair = (Pair<String, long[]>) object;

			long[] ids = (long[]) pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			for (int i = 0; i < ids.length; i++)
			{
				ConvMessage msg = findMessageById(ids[i]);
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
				{
					// If the msisdn don't match we simply return
					if (!msg.getMsisdn().equals(pair.first))
					{
						return;
					}
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				}
			}

			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(this);
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))
		{
			Pair<String, Long> pair = (Pair<String, Long>) object;

			long msgId = pair.second;
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
			{
				// If the msisdn don't match we simply return
				if (!msg.getMsisdn().equals(pair.first))
				{
					return;
				}
				msg.setState(ConvMessage.State.SENT_DELIVERED);

				if (getActivity() == null)
				{
					return;
				}
				getActivity().runOnUiThread(this);
			}
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			if (getActivity() == null)
			{
				return;
			}
			/* an icon changed, so update the view */
			getActivity().runOnUiThread(this);
		}
		else if (HikePubSub.GROUP_NAME_CHANGED.equals(type))
		{
			String groupId = (String) object;
			HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
			final String groupName = db.getGroupName(groupId);

			Conversation conv = mConversationsByMSISDN.get(groupId);
			if (conv == null)
			{
				return;
			}
			conv.setContactName(groupName);

			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(this);
		}
		else if (HikePubSub.CONTACT_ADDED.equals(type))
		{
			ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo == null)
			{
				return;
			}

			Conversation conversation = this.mConversationsByMSISDN.get(contactInfo.getMsisdn());
			if (conversation != null)
			{
				conversation.setContactName(contactInfo.getName());

				if (getActivity() == null)
				{
					return;
				}
				getActivity().runOnUiThread(this);
			}
		}
		else if (HikePubSub.TYPING_CONVERSATION.equals(type) || HikePubSub.END_TYPING_CONVERSATION.equals(type))
		{
			if (object == null)
			{
				return;
			}

			final boolean isTyping = HikePubSub.TYPING_CONVERSATION.equals(type);
			final TypingNotification typingNotification = (TypingNotification) object;

			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					toggleTypingNotification(isTyping, typingNotification);
				}
			});
		}
		else if (HikePubSub.RESET_UNREAD_COUNT.equals(type))
		{
			String msisdn = (String) object;
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			if (conv == null)
			{
				return;
			}
			conv.setUnreadCount(0);

			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(this);
		}
		else if (HikePubSub.GROUP_LEFT.equals(type))
		{
			String groupId = (String) object;
			final Conversation conversation = mConversationsByMSISDN.get(groupId);
			if (conversation == null)
			{
				return;
			}

			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					deleteConversation(conversation);
				}
			});
		}
		else if (HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED.equals(type))
		{
			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					setupEmptyView();
				}
			});
		}
		else if (HikePubSub.CLEAR_CONVERSATION.equals(type))
		{
			Pair<String, Long> values = (Pair<String, Long>) object;

			String msisdn = values.first;
			clearConversation(msisdn);
		}
		else if (HikePubSub.CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE.equals(type))
		{
			String msisdn = (String) object;
			clearConversation(msisdn);
		}
		else if (HikePubSub.DISMISS_GROUP_CHAT_TIP.equals(type))
		{
			if (mAdapter == null || mAdapter.isEmpty())
			{
				return;
			}
			final Conversation conversation = mAdapter.getItem(0);
			if (conversation != null)
			{
				return;
			}

			if (getActivity() == null)
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					removeGroupChatTip(conversation);
				}
			});

		}
	}

	private void removeGroupChatTip(Conversation conversation)
	{
		/*
		 * If conversation is null, it is the group chat tip
		 */
		mAdapter.remove(conversation);
		ConversationFragment.this.run();

		Editor editor = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putBoolean(HikeMessengerApp.SHOWN_GROUP_CHAT_TIP, true);
		editor.commit();
	}

	private void clearConversation(String msisdn)
	{
		final Conversation conversation = mConversationsByMSISDN.get(msisdn);

		if (conversation == null)
		{
			return;
		}

		/*
		 * Clearing all current messages.
		 */
		List<ConvMessage> messages = conversation.getMessages();

		ConvMessage convMessage = null;
		if (!messages.isEmpty())
		{
			convMessage = messages.get(0);
		}

		messages.clear();

		/*
		 * Adding a blank message
		 */
		ConvMessage newMessage = new ConvMessage("", msisdn, convMessage != null ? convMessage.getTimestamp() : 0, State.RECEIVED_READ);
		messages.add(newMessage);

		if (getActivity() == null)
		{
			return;
		}

		getActivity().runOnUiThread(this);
	}

	private ConvMessage findMessageById(long msgId)
	{
		int count = mAdapter.getCount();
		for (int i = 0; i < count; ++i)
		{
			Conversation conversation = mAdapter.getItem(i);
			if (conversation == null)
			{
				continue;
			}
			List<ConvMessage> messages = conversation.getMessages();
			if (messages.isEmpty())
			{
				continue;
			}

			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getMsgID() == msgId)
			{
				return message;
			}
		}

		return null;
	}

	private void addMessage(Conversation conv, ConvMessage convMessage)
	{
		if (!mConversationsAdded.contains(conv.getMsisdn()))
		{
			mConversationsAdded.add(conv.getMsisdn());
			mAdapter.add(conv);
		}
		conv.addMessage(convMessage);
		Logger.d(getClass().getSimpleName(), "new message is " + convMessage);
		mAdapter.sort(mConversationsComparator);

		if (messageRefreshHandler == null)
		{
			messageRefreshHandler = new Handler();
		}
	}

	public void DeleteAllConversations()
	{
		if (!mAdapter.isEmpty())
		{
			Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
			final CustomAlertDialog deleteDialog = new CustomAlertDialog(getActivity());
			deleteDialog.setHeader(R.string.deleteconversations);
			deleteDialog.setBody(R.string.delete_all_question);
			OnClickListener deleteAllOkClickListener = new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					Conversation[] convs = new Conversation[mAdapter.getCount()];
					for (int i = 0; i < convs.length; i++)
					{
						convs[i] = mAdapter.getItem(i);
						if ((convs[i] instanceof GroupConversation))
						{
							HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, convs[i].serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
						}
					}
					DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
					task.execute(convs);
					deleteDialog.dismiss();
				}
			};

			deleteDialog.setOkButton(R.string.delete, deleteAllOkClickListener);
			deleteDialog.setCancelButton(R.string.cancel);

			deleteDialog.show();
		}
	}

	@Override
	public void onResume()
	{
		/*
		 * This is a temporary fix for the issue on 4.0 and above devices where the profile picture is wrongly shown. We are simply forcing getview to be called again. TODO think
		 * of a proper fix.
		 */
		run();

		SharedPreferences prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		if (getActivity() == null && prefs.getInt(HikeConstants.HIKEBOT_CONV_STATE, 0) == hikeBotConvStat.VIEWED.ordinal())
		{
			/*
			 * if there is a HikeBotConversation in Conversation list also it is Viewed by user then delete this.
			 */
			Conversation conv = null;
			conv = mConversationsByMSISDN.get(HikeConstants.FTUE_HIKEBOT_MSISDN);
			if (conv != null)
			{
				Editor editor = prefs.edit();
				editor.putInt(HikeConstants.HIKEBOT_CONV_STATE, hikeBotConvStat.DELETED.ordinal());
				editor.commit();
				Utils.logEvent(getActivity(), HikeConstants.LogEvent.DELETE_CONVERSATION);
				DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
				Utils.executeConvAsyncTask(task, conv);
			}
		}
		super.onResume();
	}
}
