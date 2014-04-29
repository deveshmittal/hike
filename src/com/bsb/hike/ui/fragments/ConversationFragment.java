package com.bsb.hike.ui.fragments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.Toast;

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
import com.bsb.hike.models.ConversationTip;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.tasks.EmailConversationsAsyncTask;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.ComposeChatActivity;
import com.bsb.hike.ui.HikeDialog;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.ui.TellAFriend;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
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
				 * Added to check for the Conversation tip item we add for the group chat tip and other.
				 */
				if (conv instanceof ConversationTip)
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
				 * Added to check for the Conversation tip item we add for the group chat tip and other.
				 */
				if (conversation instanceof ConversationTip)
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
			HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, HikePubSub.CLEAR_CONVERSATION, HikePubSub.CONVERSATION_CLEARED_BY_DELETING_LAST_MESSAGE, HikePubSub.DISMISS_GROUP_CHAT_TIP,
			HikePubSub.DISMISS_STEALTH_FTUE_CONV_TIP, HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP, HikePubSub.STEALTH_MODE_TOGGLED, HikePubSub.CLEAR_FTUE_STEALTH_CONV,
			HikePubSub.RESET_STEALTH_INITIATED, HikePubSub.RESET_STEALTH_CANCELLED };

	private ConversationsAdapter mAdapter;

	private HashMap<String, Conversation> mConversationsByMSISDN;

	private HashSet<String> mConversationsAdded;

	private Comparator<? super Conversation> mConversationsComparator;

	private Handler messageRefreshHandler;

	private View emptyView;

	private Set<Conversation> stealthConversations;

	private List<Conversation> displayedConversations;
	
	private boolean showingStealthFtueConvTip = false;

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
		
		if(!getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
		{
			// if stealth setup is not done and user has marked some chats as stealth unmark all of them
			for (Conversation conv : stealthConversations)
			{
				conv.setIsStealth(false);
				HikeConversationsDatabase.getInstance().toggleStealth(conv.getMsisdn(), false);
			}

			HikeMessengerApp.clearStealthMsisdn();
		}
		
		super.onDestroy();
	}

	@Override
	public void onStop()
	{
		// TODO Auto-generated method stub
		super.onStop();
		if (showingStealthFtueConvTip)
		{
			HikeSharedPreferenceUtil.getInstance(getActivity()).saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
			Conversation convTip = displayedConversations.get(0);
			removeStealthConvTip(convTip);
		}

	}
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Conversation conv = (Conversation) mAdapter.getItem(position);

		/*
		 * The item will be instance ConversationTip only for tips we show on ConversationFragment.
		 */
		if (conv instanceof ConversationTip)
		{
			switch (((ConversationTip) conv).getTipType())
			{
			case ConversationTip.GROUP_CHAT_TIP:
				((HomeActivity) getActivity()).showOverFlowMenu();
				removeGroupChatTip(conv);
				break;
			case ConversationTip.STEALTH_FTUE_TIP:
				break;
			case ConversationTip.RESET_STEALTH_TIP:
				resetStealthTipClicked();
				break;
			}
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

	private void resetStealthTipClicked()
	{
		long remainingTime = System.currentTimeMillis() - HikeSharedPreferenceUtil.getInstance(getActivity()).getData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l);

		if (remainingTime > HikeConstants.RESET_COMPLETE_STEALTH_TIME_MS)
		{
			Object[] dialogStrings = new Object[4];
			dialogStrings[0] = getString(R.string.reset_complete_stealth_header);
			dialogStrings[1] = getString(R.string.reset_stealth_confirmation);
			dialogStrings[2] = getString(R.string.confirm);
			dialogStrings[3] = getString(R.string.cancel);

			HikeDialog.showDialog(getActivity(), HikeDialog.RESET_STEALTH_DIALOG, new HikeDialog.HikeDialogListener()
			{

				@Override
				public void positiveClicked(Dialog dialog)
				{
					resetStealthMode();
					dialog.dismiss();
				}

				@Override
				public void neutralClicked(Dialog dialog)
				{

				}

				@Override
				public void negativeClicked(Dialog dialog)
				{
					dialog.dismiss();
				}
			}, dialogStrings);
		}
	}

	private void resetStealthMode()
	{
		removeResetStealthTipIfExists();

		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask(getActivity());
		task.execute(stealthConversations.toArray(new Conversation[0]));

		int prevStealthValue = HikeSharedPreferenceUtil.getInstance(getActivity()).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		resetStealthPreferences();
		HikeMessengerApp.clearStealthMsisdn();
		stealthConversations.clear();

		/*
		 * If previously the stealth mode was off, we should publish an event telling the friends fragment to refresh its list.
		 */
		if (prevStealthValue == HikeConstants.STEALTH_OFF)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_RESET_COMPLETE, null);
		}
	}

	private void resetStealthPreferences()
	{
		HikeSharedPreferenceUtil prefUtil = HikeSharedPreferenceUtil.getInstance(getActivity());

		prefUtil.removeData(HikeMessengerApp.STEALTH_ENCRYPTED_PATTERN);
		prefUtil.removeData(HikeMessengerApp.STEALTH_MODE);
		prefUtil.removeData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE);
		prefUtil.removeData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP);
		prefUtil.removeData(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME);
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

		if (conv instanceof ConversationTip)
		{
			return false;
		}

		/*
		 * If stealth ftue conv tap tip is visible than remove it
		 */
		if (!getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
		{
			for (int i = 0; i < mAdapter.getCount(); i++)
			{
				Conversation convTip = mAdapter.getItem(i);
				if (convTip instanceof ConversationTip && ((ConversationTip) convTip).isStealthFtueTip())
				{
					HikeSharedPreferenceUtil.getInstance(getActivity()).saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_ON);
					removeStealthConvTip(convTip);
					break;
				}
			}
		}

		final int stealthType = HikeSharedPreferenceUtil.getInstance(getActivity()).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		if (stealthType == HikeConstants.STEALTH_ON || stealthType == HikeConstants.STEALTH_ON_FAKE)
		{
			optionsList.add(getString(conv.isStealth() ? R.string.unmark_stealth : R.string.mark_stealth));
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
				else if (getString(R.string.mark_stealth).equals(option) || getString(R.string.unmark_stealth).equals(option))
				{
					boolean newStealthValue = !conv.isStealth();
					Toast.makeText(getActivity(), newStealthValue ? R.string.chat_marked_stealth : R.string.chat_unmarked_stealth, Toast.LENGTH_SHORT).show();

					if (stealthType == HikeConstants.STEALTH_ON_FAKE)
					{
						/*
						 * We don't need to do anything here if the device is on fake stealth mode.
						 */
						return;
					}
					if(getString(R.string.mark_stealth).equals(option))
					{
						stealthConversations.add(conv);
						HikeMessengerApp.addStealthMsisdn(conv.getMsisdn());
					}
					else
					{
						stealthConversations.remove(conv);
						HikeMessengerApp.removeStealthMsisdn(conv.getMsisdn());
					}

					conv.setIsStealth(newStealthValue);

					HikeConversationsDatabase.getInstance().toggleStealth(conv.getMsisdn(), newStealthValue);

					mAdapter.notifyDataSetChanged();

					if (!HikeSharedPreferenceUtil.getInstance(getActivity()).getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
					{
						HikeSharedPreferenceUtil.getInstance(getActivity()).saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
						changeConversationsVisibility();
						HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_FTUE_SET_PASS_TIP, null);
					}
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
		displayedConversations = new ArrayList<Conversation>();
		List<Conversation> conversationList = db.getConversations();

		stealthConversations = new HashSet<Conversation>();

		SharedPreferences prefs = getActivity().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		if (prefs.getLong(HikeMessengerApp.RESET_COMPLETE_STEALTH_START_TIME, 0l) > 0)
		{
			displayedConversations.add(new ConversationTip(ConversationTip.RESET_STEALTH_TIP));
		}
		else if (!conversationList.isEmpty() && !prefs.getBoolean(HikeMessengerApp.SHOWN_GROUP_CHAT_TIP, false))
		{
			/*
			 * Add item for group chat tip.
			 */
			displayedConversations.add(new ConversationTip(ConversationTip.GROUP_CHAT_TIP));
		}

		displayedConversations.addAll(conversationList);

		mConversationsByMSISDN = new HashMap<String, Conversation>(displayedConversations.size());
		mConversationsAdded = new HashSet<String>();

		setupConversationLists();

		if (mAdapter != null)
		{
			mAdapter.clear();
		}

		mAdapter = new ConversationsAdapter(getActivity(), R.layout.conversation_item, displayedConversations);

		/*
		 * because notifyOnChange gets re-enabled whenever we call notifyDataSetChanged it's simpler to assume it's set to false and always notifyOnChange by hand
		 */
		mAdapter.setNotifyOnChange(false);

		setListAdapter(mAdapter);

		getListView().setOnItemLongClickListener(this);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private void setupConversationLists()
	{
		int stealthValue = HikeSharedPreferenceUtil.getInstance(getActivity()).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		/*
		 * Use an iterator so we can remove conversations w/ no messages from our list
		 */
		for (Iterator<Conversation> iter = displayedConversations.iterator(); iter.hasNext();)
		{
			Object object = iter.next();
			Conversation conv = (Conversation) object;
			if (conv instanceof ConversationTip)
			{
				continue;
			}

			mConversationsByMSISDN.put(conv.getMsisdn(), conv);
			if (conv.isStealth())
			{
				stealthConversations.add(conv);
				HikeMessengerApp.addStealthMsisdn(conv.getMsisdn());
			}

			if (conv.getMessages().isEmpty() && !(conv instanceof GroupConversation))
			{
				iter.remove();
			}
			else if ((stealthValue == HikeConstants.STEALTH_OFF || stealthValue == HikeConstants.STEALTH_ON_FAKE) && conv.isStealth())
			{
				mConversationsAdded.add(conv.getMsisdn());
				iter.remove();
			}
			else
			{
				mConversationsAdded.add(conv.getMsisdn());
			}
		}

	}

	private void changeConversationsVisibility()
	{
		int stealthValue = HikeSharedPreferenceUtil.getInstance(getActivity()).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);

		if (stealthValue == HikeConstants.STEALTH_OFF || stealthValue == HikeConstants.STEALTH_ON_FAKE)
		{
			for (Iterator<Conversation> iter = displayedConversations.iterator(); iter.hasNext();)
			{
				Object object = iter.next();
				if (object == null)
				{
					continue;
				}
				Conversation conv = (Conversation) object;
				if (conv.isStealth())
				{
					iter.remove();
				}
			}
		}
		else
		{
			displayedConversations.addAll(stealthConversations);
		}

		mAdapter.sort(mConversationsComparator);
		mAdapter.notifyDataSetChanged();
		mAdapter.setNotifyOnChange(false);
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

					mAdapter.notifyDataSetChanged();
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
			if (!(conversation instanceof ConversationTip && ((ConversationTip) conversation).isGroupChatTip()))
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
		else if (HikePubSub.DISMISS_STEALTH_FTUE_CONV_TIP.equals(type))
		{
			if (mAdapter == null || mAdapter.isEmpty())
			{
				return;
			}
			int position = (Integer) object;
			final Conversation conversation = mAdapter.getItem(position);
			if (!(conversation instanceof ConversationTip && ((ConversationTip) conversation).isStealthFtueTip()))
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
					removeStealthConvTip(conversation);
				}
			});
		}
		else if (HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP.equals(type))
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
					showStealthConvTip();
				}
			});

		}
		else if (HikePubSub.STEALTH_MODE_TOGGLED.equals(type))
		{
			boolean changeItemsVisibility = (Boolean) object;

			if (!changeItemsVisibility)
			{
				return;
			}

			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					changeConversationsVisibility();
				}
			});
		}
		else if (HikePubSub.CLEAR_FTUE_STEALTH_CONV.equals(type))
		{
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					// if stealth setup is not done and user has marked some chats as stealth unmark all of them
					for (Conversation conv : stealthConversations)
					{
						conv.setIsStealth(false);
						HikeConversationsDatabase.getInstance().toggleStealth(conv.getMsisdn(), false);
					}
					displayedConversations.addAll(stealthConversations);
					stealthConversations.clear();
					mAdapter.sort(mConversationsComparator);
					mAdapter.notifyDataSetChanged();
					mAdapter.setNotifyOnChange(false);
				}
			});
		}
		else if (HikePubSub.RESET_STEALTH_INITIATED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					removeGroupChatTipIfExists();

					displayedConversations.add(0, new ConversationTip(ConversationTip.RESET_STEALTH_TIP));

					ConversationFragment.this.run();
				}
			});
		}
		else if (HikePubSub.RESET_STEALTH_CANCELLED.equals(type))
		{
			if (!isAdded())
			{
				return;
			}
			getActivity().runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					removeResetStealthTipIfExists();
				}
			});
		}
	}

	private void removeResetStealthTipIfExists()
	{
		if (mAdapter.isEmpty())
		{
			return;
		}

		Conversation conversation = mAdapter.getItem(0);

		if (conversation instanceof ConversationTip && ((ConversationTip) conversation).isResetStealthTip())
		{
			mAdapter.remove(conversation);
			mAdapter.resetCountDownSetter();

			ConversationFragment.this.run();
		}
	}

	private Conversation removeGroupChatTipIfExists()
	{
		Conversation conv = null;
		if (!displayedConversations.isEmpty())
		{
			conv = displayedConversations.get(0);
			if(conv instanceof ConversationTip && ((ConversationTip) conv).isGroupChatTip())
			{
				if(displayedConversations.size()>1)
				{
					mAdapter.remove(conv);
					ConversationFragment.this.run();
				}
				else
				{
					conv = null;
				}
			}
		}
		return conv;
	}

	/*
	 * Add item for stealth ftue conv tap tip.
	 */
	protected void showStealthConvTip()
	{
		/*
		 * if group chat tip is showing we should remove this first and than add stealth ftue conversation tip
		 */
		Conversation conv = removeGroupChatTipIfExists();

		/*
		 * if conv not null this implies, We certainly have some conversations on the screen other than group chat tip
		 */
		if (conv != null)
		{
			displayedConversations.add(0, new ConversationTip(ConversationTip.STEALTH_FTUE_TIP));
			mAdapter.notifyDataSetChanged();
			HikeSharedPreferenceUtil.getInstance(getActivity()).saveData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP, true);
			showingStealthFtueConvTip = true;
		}
		else
		{
			Toast.makeText(getActivity(), R.string.stealth_zero_chat_tip, Toast.LENGTH_SHORT).show();
		}
	}

	protected void removeStealthConvTip(Conversation conversation)
	{
		HikeSharedPreferenceUtil.getInstance(getActivity()).removeData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP);
		showingStealthFtueConvTip = false;
		mAdapter.remove(conversation);
		ConversationFragment.this.run();
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
