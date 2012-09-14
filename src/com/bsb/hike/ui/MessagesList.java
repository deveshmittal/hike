package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.DrawerLayout;
import com.bsb.hike.view.DrawerLayout.DrawerItems;

public class MessagesList extends Activity implements OnClickListener, OnItemClickListener, HikePubSub.Listener, android.content.DialogInterface.OnClickListener, Runnable, DrawerLayout.Listener
{
	private static final int INVITE_PICKER_RESULT = 1001;

	public static final Object COMPOSE = "compose";

	private ListView mConversationsView;

	private View mSearchIconView;

	private View mEditMessageIconView;

	private ConversationsAdapter mAdapter;

	private View mEmptyView;

	private Comparator<? super Conversation> mConversationsComparator;

	private Map<String, Conversation> mConversationsByMSISDN;

	private Set<String> mConversationsAdded;

	private View mToolTip;

	private SharedPreferences accountPrefs;

	private View updateToolTipParent;

	private View groupChatToolTipParent;

	private boolean isToolTipShowing;

	private boolean wasAlertCancelled = false;

	private boolean deviceDetailsSent = false;

	private DrawerLayout parentLayout;

	private TextView creditsNum;

	private String[] pubSubListeners = {
			HikePubSub.MESSAGE_RECEIVED, 
			HikePubSub.SERVER_RECEIVED_MSG, 
			HikePubSub.MESSAGE_DELIVERED_READ, 
			HikePubSub.MESSAGE_DELIVERED, 
			HikePubSub.MESSAGE_FAILED, 
			HikePubSub.NEW_CONVERSATION, 
			HikePubSub.MESSAGE_SENT, 
			HikePubSub.MSG_READ, 
			HikePubSub.ICON_CHANGED, 
			HikePubSub.GROUP_LEFT, 
			HikePubSub.GROUP_NAME_CHANGED, 
			HikePubSub.UPDATE_AVAILABLE, 
			HikePubSub.CONTACT_ADDED, 
			HikePubSub.SMS_CREDIT_CHANGED
	};
	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d("MESSAGE LIST", "Currently in pause state. .......");
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
	}
	
	private class DeleteConversationsAsyncTask extends AsyncTask<Conversation, Void, Conversation[]>
	{

		@Override
		protected Conversation[] doInBackground(Conversation... convs)
		{
			HikeConversationsDatabase db = null;
			ArrayList<Long> ids = new ArrayList<Long>(convs.length);
			ArrayList<String> msisdns =new ArrayList<String>(convs.length);
			for (Conversation conv : convs)
			{
				ids.add(conv.getConvId());
				msisdns.add(conv.getMsisdn());
			}

			db = HikeConversationsDatabase.getInstance();
			db.deleteConversation(ids.toArray(new Long[] {}), msisdns);
			return convs;
		}

		@Override
		protected void onPostExecute(Conversation[] deleted)
		{
			NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			for (Conversation conversation : deleted)
			{
				mgr.cancel((int) conversation.getConvId());
				mAdapter.remove(conversation);
				mConversationsByMSISDN.remove(conversation.getMsisdn());
				mConversationsAdded.remove(conversation.getMsisdn());
			}

			mAdapter.notifyDataSetChanged();
			mAdapter.setNotifyOnChange(false);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Utils.requireAuth(this);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (Utils.requireAuth(this))
		{
			return;
		}
		Utils.setDensityMultiplier(MessagesList.this);

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = accountPrefs.getString(HikeMessengerApp.TOKEN_SETTING, null);

		if(!accountPrefs.getBoolean(HikeMessengerApp.SHOWN_TUTORIAL, false))
		{
			Intent i = new Intent(MessagesList.this, Tutorial.class);
			startActivity(i);
			finish();
			return;
		}

		// TODO this is being called everytime this activity is created. Way too often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();


		setContentView(R.layout.main);


		isToolTipShowing = savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.TOOLTIP_SHOWING);
		wasAlertCancelled = savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.ALERT_CANCELLED);
		deviceDetailsSent = savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);

		int updateTypeAvailable = accountPrefs.getInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
		updateApp(updateTypeAvailable);

		mConversationsView = (ListView) findViewById(R.id.conversations);

		if(getIntent().getBooleanExtra(HikeConstants.Extras.FIRST_TIME_USER, false))
		{
			if(!deviceDetailsSent)
			{
				sendDeviceDetails();
			}
		}

		View view = findViewById(R.id.title_hikeicon);
		view.setVisibility(View.VISIBLE);

		/*
		 * mSearchIconView = findViewById(R.id.search); mSearchIconView.setOnClickListener(this);
		 */
		parentLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		parentLayout.setListener(this);
		parentLayout.setUpDrawerView();

		findViewById(R.id.topbar_menu).setVisibility(View.VISIBLE);
		findViewById(R.id.menu_bar).setVisibility(View.VISIBLE);
		if(savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.IS_DRAWER_VISIBLE))
		{
			parentLayout.toggleSidebar(true);
		}

		mEditMessageIconView = findViewById(R.id.edit_message);
		mEditMessageIconView.setOnClickListener(this);

		/* set the empty view layout for the list */
		mEmptyView = findViewById(R.id.empty_view);
		mEmptyView.setOnClickListener(this);

		mConversationsView.setEmptyView(mEmptyView);
		mConversationsView.setOnItemClickListener(this);

		HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
		List<Conversation> conversations = db.getConversations();

		mConversationsByMSISDN = new HashMap<String, Conversation>(conversations.size());
		mConversationsAdded = new HashSet<String>();

		/*
		 * Use an iterator so we can remove conversations w/ no messages from our list
		 */
		for (Iterator<Conversation> iter = conversations.iterator(); iter.hasNext();)
		{
			Conversation conv = (Conversation) iter.next();
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

		mAdapter = new ConversationsAdapter(this, R.layout.conversation_item, conversations);
		/* we need this object every time a message comes in, seems simplest to just create it once */
		mConversationsComparator = new Conversation.ConversationComparator();

		/*
		 * because notifyOnChange gets re-enabled whenever we call notifyDataSetChanged it's simpler to assume it's set to false and always notifyOnChange by hand
		 */
		mAdapter.setNotifyOnChange(false);
		mConversationsView.setAdapter(mAdapter);

		if(getIntent().hasExtra(HikeConstants.Extras.GROUP_LEFT))
		{
			Log.d(getClass().getSimpleName(), "LEAVING GROUP FROM ONCREATE");
			leaveGroup(mConversationsByMSISDN.get(getIntent().getStringExtra(HikeConstants.Extras.GROUP_LEFT)));
		}

		for(String pubSubListener : pubSubListeners)
		{
			HikeMessengerApp.getPubSub().addListener(pubSubListener, this);
		}
		/* register for long-press's */
		registerForContextMenu(mConversationsView);
	}

	public void onDrawerItemClicked(View v)
	{
		Log.d(getClass().getSimpleName(), "Drawer item clicked: " + v.getId());
		Intent intent = null;
		switch (DrawerItems.values()[v.getId()]) 
		{
		case HOME:
			parentLayout.closeSidebar();
			break;
		case GROUP_CHAT:
			intent = new Intent(this, ChatThread.class);
			intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
			break;
		case TELL_A_FRIEND:
			Utils.logEvent(this, HikeConstants.LogEvent.INVITE_MENU);
			Utils.startShareIntent(this, Utils.getInviteMessage(MessagesList.this));
			break;
		case REWARDS:
			intent = new Intent(this, Rewards.class);
			break;
		case FREE_SMS:
			Utils.logEvent(this, HikeConstants.LogEvent.CREDITS_SCREEN);
			intent = new Intent(this, CreditsActivity.class);
			break;
		case PROFILE:
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.PROFILE_MENU);
			intent = new Intent(this, ProfileActivity.class);
			break;
		case HELP:
			intent = new Intent(this, WebViewActivity.class);
			intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, HikeConstants.HELP_URL);
			intent.putExtra(HikeConstants.Extras.TITLE, "Help");
			break;
		}
		if (intent != null) 
		{
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
	}

	public void onToggleSideBarClicked(View v)
	{
		parentLayout.toggleSidebar(false);
	}

	private void sendDeviceDetails() 
	{
		// We're adding this delay to ensure that the service is alive before sending the message
		(new Handler()).postDelayed(new Runnable() 
		{
			@Override
			public void run() 
			{
				JSONObject obj = Utils.getDeviceDetails(MessagesList.this);
				if (obj != null) 
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, obj);
				}
				Utils.requestAccountInfo();
			}
		}, 10 * 1000);
		deviceDetailsSent = true;
	}

	private void showSMSNotificationAlert()
	{
		final Dialog smsNotificationAlert = new Dialog(MessagesList.this, R.style.Theme_CustomDialog);
		smsNotificationAlert.setContentView(R.layout.alert_box);

		((TextView)smsNotificationAlert.findViewById(R.id.alert_title)).setText(R.string.sms);
		((TextView)smsNotificationAlert.findViewById(R.id.alert_text)).setText(R.string.sms_alert_text);
		Button okBtn = (Button) smsNotificationAlert.findViewById(R.id.alert_ok_btn);
		Button cancelBtn = (Button) smsNotificationAlert.findViewById(R.id.alert_cancel_btn);
		okBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Editor editor = PreferenceManager.getDefaultSharedPreferences(MessagesList.this).edit();
				editor.putBoolean(HikeConstants.SMS_PREF, true);
				editor.commit();
				Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.DEFAULT_SMS_DIALOG_YES);
				smsNotificationAlert.dismiss();
			}
		});
		cancelBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Editor editor = PreferenceManager.getDefaultSharedPreferences(MessagesList.this).edit();
				editor.putBoolean(HikeConstants.SMS_PREF, false);
				editor.commit();
				Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.DEFAULT_SMS_DIALOG_NO);
				smsNotificationAlert.dismiss();
			}
		});

		smsNotificationAlert.setOnCancelListener(new OnCancelListener() 
		{
			@Override
			public void onCancel(DialogInterface dialog) 
			{
				Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.DEFAULT_SMS_DIALOG_NO);
				wasAlertCancelled = true;
			}
		});
		smsNotificationAlert.setOnDismissListener(new OnDismissListener() 
		{
			@Override
			public void onDismiss(DialogInterface dialog) 
			{
				wasAlertCancelled = true;
			}
		});

		smsNotificationAlert.show();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.TOOLTIP_SHOWING, mToolTip != null && mToolTip.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT, deviceDetailsSent);
		outState.putBoolean(HikeConstants.Extras.ALERT_CANCELLED, wasAlertCancelled);
		outState.putBoolean(HikeConstants.Extras.IS_DRAWER_VISIBLE, this.parentLayout.isOpening());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		if(parentLayout.isOpening())
		{
			parentLayout.closeSidebar();
		}
		else
		{
			Utils.incrementNumTimesScreenOpen(accountPrefs, HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
			// super.onBackPressed() would crash the application sometimes. Android Bug.
			finish();
		}
	}

	private Intent createIntentForConversation(Conversation conversation)
	{
		Intent intent = new Intent(MessagesList.this, ChatThread.class);
		if (conversation.getContactName() != null)
		{
			intent.putExtra(HikeConstants.Extras.NAME, conversation.getContactName());
		}
		if (conversation.getContactId() != null)
		{
			intent.putExtra(HikeConstants.Extras.ID, conversation.getContactId());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, conversation.getMsisdn());
		return intent;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Conversation conv = mAdapter.getItem((int) info.id);
		switch (item.getItemId())
		{
		case R.id.shortcut:
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.ADD_SHORTCUT);
			Intent shortcutIntent = createIntentForConversation(conv);
			Intent intent = new Intent();
			Log.i("CreateShortcut", "Creating intent for broadcasting");
			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, conv.getLabel());
			Drawable d = IconCacheManager.getInstance().getIconForMSISDN(conv.getMsisdn());
			Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
			Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 60, 60, false);
			bitmap = null;
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, scaled);
			intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
			sendBroadcast(intent);
			return true;
		case R.id.delete:
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.DELETE_CONVERSATION);
			if(conv instanceof GroupConversation)
			{
				leaveGroup(conv);
			}
			else
			{
				DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
				task.execute(conv);
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		android.view.MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.conversation_menu, menu);
		
		AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Conversation conversation = mAdapter.getItem(adapterInfo.position);
		if(conversation instanceof GroupConversation)
		{
			MenuItem delete = menu.findItem(R.id.delete);
			delete.setTitle(R.string.delete_leave);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	private void showCreditsScreen()
	{
		Intent intent = new Intent(this, CreditsActivity.class);
		startActivity(intent);		
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.MENU);
		if(groupChatToolTipParent != null && groupChatToolTipParent.getVisibility() == View.VISIBLE)
		{
			onToolTipClosed(null);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.deleteconversations:
			if (!mAdapter.isEmpty()) {
				Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.delete_all_question)
						.setPositiveButton("Delete", this)
						.setNegativeButton(R.string.cancel, this).show();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Log.d(getClass().getSimpleName(), "onDestroy " + this);
		for(String pubSubListener : pubSubListeners)
		{
			HikeMessengerApp.getPubSub().removeListener(pubSubListener, this);
		}
	}

	@Override
	public void onClick(View v)
	{
		if ((v == mEditMessageIconView))
		{
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.COMPOSE_BUTTON);
			Intent intent = new Intent(this, ChatThread.class);
			intent.putExtra(HikeConstants.Extras.EDIT, true);
			startActivity(intent);
			overridePendingTransition(R.anim.slide_up_noalpha, R.anim.no_animation);
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if ((HikePubSub.MESSAGE_RECEIVED.equals(type)) || (HikePubSub.MESSAGE_SENT.equals(type)))
		{
			Log.d("MESSAGE LIST", "New msg event sent or received.");
			final ConvMessage message = (ConvMessage) object;
			/* find the conversation corresponding to this message */
			String msisdn = message.getMsisdn();
			final Conversation conv = mConversationsByMSISDN.get(msisdn);
			
			if (conv == null)
			{
				// When a message gets sent from a user we don't have a conversation for, the message gets
				// broadcasted first then the conversation gets created. It's okay that we don't add it now, because
				// when the conversation is broadcasted it will contain the messages
				return;
			}

			// For updating the group name if some participant has joined or left the group
			else if((conv instanceof GroupConversation) && message.getParticipantInfoState() != ParticipantInfoState.NO_INFO)
			{
				HikeConversationsDatabase hCDB = HikeConversationsDatabase.getInstance();
				((GroupConversation) conv).setGroupParticipantList(hCDB.getGroupParticipants(conv.getMsisdn(), false, false));
			}
			runOnUiThread(new Runnable(){
				@Override
				public void run()
				{
					if (!mConversationsAdded.contains(conv.getMsisdn()))
					{
						mConversationsAdded.add(conv.getMsisdn());
						mAdapter.add(conv);
					}

					conv.addMessage(message);
					Log.d("MessagesList", "new message is " + message);
					mAdapter.sort(mConversationsComparator);
					mAdapter.notifyDataSetChanged();
					// notifyDataSetChanged sets notifyonChange to true but we want it to always be false
					mAdapter.setNotifyOnChange(false);
				}
			});
		}
		else if (HikePubSub.NEW_CONVERSATION.equals(type))
		{
			final Conversation conversation = (Conversation) object;
			Log.d(getClass().getSimpleName(), "New Conversation. Group Conversation? " + (conversation instanceof GroupConversation));
			mConversationsByMSISDN.put(conversation.getMsisdn(), conversation);
			if (conversation.getMessages().isEmpty() && !(conversation instanceof GroupConversation))
			{
				return;
			}

			mConversationsAdded.add(conversation.getMsisdn());
			runOnUiThread(new Runnable()
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
			if(conv == null)
			{
				/*
				 * We don't really need to do anything if the conversation does not exist.
				 */
				return;
			}
			/* look for the latest received messages and set them to read.
			 * Exit when we've found some read messages
			 */
			List<ConvMessage> messages = conv.getMessages();
			for(int i = messages.size() - 1; i >= 0; --i)
			{
				ConvMessage msg = messages.get(i);
				if (!msg.isSent())
				{
					ConvMessage.State currentState = msg.getState();
					msg.setState(ConvMessage.State.RECEIVED_READ);
					if (currentState == ConvMessage.State.RECEIVED_READ)
					{
						break;
					}
				}
			}

			mAdapter.setNotifyOnChange(false);
			runOnUiThread(this);
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				runOnUiThread(this);
			}
		}
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))
		{
			long[] ids = (long[]) object;
			// TODO we could keep a map of msgId -> conversation objects somewhere to make this faster
			for (int i = 0; i < ids.length; i++)
			{
				ConvMessage msg = findMessageById(ids[i]);
				if (msg != null)
				{
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				}
			}
			runOnUiThread(this);
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_DELIVERED);
				runOnUiThread(this);
			}
		}
		else if (HikePubSub.MESSAGE_FAILED.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null)
			{
				msg.setState(ConvMessage.State.SENT_FAILED);
				runOnUiThread(this);
			}
		}
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			/* an icon changed, so update the view */
			runOnUiThread(this);
		}
		else if (HikePubSub.GROUP_LEFT.equals(type))
		{
			final String groupId = (String) object;
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					leaveGroup(MessagesList.this.mConversationsByMSISDN.get(groupId));
				}
			});
		}
		else if (HikePubSub.GROUP_NAME_CHANGED.equals(type))
		{
			String groupId = (String) object;
			HikeConversationsDatabase db = HikeConversationsDatabase.getInstance();
			final String groupName = db.getGroupName(groupId);

			Conversation conv = mConversationsByMSISDN.get(groupId);
			conv.setContactName(groupName);

			runOnUiThread(this);
		}
		else if (HikePubSub.UPDATE_AVAILABLE.equals(type))
		{
			final int updateType = (Integer) object;
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					updateApp(updateType);
				}
			});
		}
		else if (HikePubSub.CONTACT_ADDED.equals(type))
		{
			ContactInfo contactInfo = (ContactInfo) object;
			Conversation conversation = this.mConversationsByMSISDN.get(contactInfo.getMsisdn());
			if (conversation != null) 
			{
				conversation.setContactName(contactInfo.getName());
				runOnUiThread(this);
			}
		}
		else if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			final int credits = (Integer) object;
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					creditsNum.setText(Integer.toString(credits));
				}
			});
		}
	}

	ConvMessage findMessageById(long msgId)
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

	public void run()
	{
		mAdapter.notifyDataSetChanged();
		// notifyDataSetChanged sets notifyonChange to true but we want it to always be false
		mAdapter.setNotifyOnChange(false);
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		switch (which)
		{
		case DialogInterface.BUTTON_POSITIVE:
			Conversation[] convs = new Conversation[mAdapter.getCount()];
			for (int i = 0; i < convs.length; i++)
			{
				convs[i] = mAdapter.getItem(i);
				if ((convs[i] instanceof GroupConversation)) 
				{
					HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, convs[i].serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
				}
			}
			DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
			task.execute(convs);
			break;
		default:
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		Conversation conv = (Conversation) adapterView.getItemAtPosition(position);
		Intent intent = createIntentForConversation(conv);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_right_noalpha, R.anim.slide_out_left_noalpha);
	}

	private void hideToolTip()
	{
		if (mToolTip.getVisibility() == View.VISIBLE) 
		{
			Animation alphaOut = AnimationUtils.loadAnimation(
					MessagesList.this, android.R.anim.fade_out);
			alphaOut.setDuration(200);
			mToolTip.setAnimation(alphaOut);
			mToolTip.setVisibility(View.INVISIBLE);
		}
	}

	private void setToolTipDismissed()
	{
		hideToolTip();

		Editor editor = accountPrefs.edit();
		editor.putBoolean(HikeMessengerApp.MESSAGES_LIST_TOOLTIP_DISMISSED, true);
		editor.commit();
	}

	public void onToolTipClosed(View v)
	{
		if(updateToolTipParent != null && updateToolTipParent.getVisibility() == View.VISIBLE)
		{
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.HOME_UPDATE_TOOL_TIP_CLOSED);
			Editor editor = accountPrefs.edit();
			editor.putBoolean(HikeConstants.Extras.SHOW_UPDATE_TOOL_TIP, false);
			//Doing this so that we show this tip after the user has opened the home screen a few times.
			editor.remove(HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
			editor.commit();
			hideToolTip();
			return;
		}
		else if(groupChatToolTipParent != null && groupChatToolTipParent.getVisibility() == View.VISIBLE)
		{
			Editor editor = accountPrefs.edit();
			editor.putBoolean(HikeMessengerApp.SHOW_GROUP_CHAT_TOOL_TIP, false);
			editor.commit();
			hideToolTip();
			return;
		}
		Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.HOME_TOOL_TIP_CLOSED);
		setToolTipDismissed();
	}

	public void onTitleIconClick(View v)
	{
		setToolTipDismissed();
		openOptionsMenu();
	}

	private void openMarket()
	{
		Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
		marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		try
		{
			startActivity(marketIntent);				
		}
		catch(ActivityNotFoundException e)
		{
			Log.e(MessagesList.class.getSimpleName(), "Unable to open market");
		}
	}
	public void onToolTipClicked(View v)
	{
		if(updateToolTipParent != null && updateToolTipParent.getVisibility() == View.VISIBLE)
		{
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.HOME_UPDATE_TOOL_TIP_CLICKED);
			openMarket();
			hideToolTip();
			return;
		}
		if(groupChatToolTipParent != null && groupChatToolTipParent.getVisibility() == View.VISIBLE)
		{
			setToolTipDismissed();
			openOptionsMenu();
			return;
		}
	}

	private void leaveGroup(Conversation conv)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, conv.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
		task.execute(conv);
	}

	private void updateApp(int updateType)
	{
		if(updateType == HikeConstants.NO_UPDATE)
		{
			return;
		}
		if(updateType == HikeConstants.CRITICAL_UPDATE && 
				accountPrefs.getBoolean(HikeConstants.Extras.SHOW_UPDATE_OVERLAY, true))
		{
			updateAppOverlay();
		}
		else if(updateType == HikeConstants.CRITICAL_UPDATE ||
					accountPrefs.getBoolean(HikeConstants.Extras.SHOW_UPDATE_TOOL_TIP, true) || 
						Utils.wasScreenOpenedNNumberOfTimes(accountPrefs, HikeMessengerApp.NUM_TIMES_HOME_SCREEN))
		{
			showUpdateToolTip(updateType);
		}
	}

	private void updateAppOverlay()
	{
		findViewById(R.id.overlay_layout).setVisibility(View.VISIBLE);
		((TextView)findViewById(R.id.overlay_message)).setText(this.accountPrefs.getString(HikeConstants.Extras.UPDATE_MESSAGE, ""));
		((ImageView)findViewById(R.id.overlay_image)).setImageResource(R.drawable.ic_update);
		((Button)findViewById(R.id.overlay_button)).setText("Update now");
	}

	public void onOverlayButtonClick(View v)
	{
		if (v.getId() != R.id.overlay_layout) 
		{
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.HOME_UDPATE_OVERLAY_BUTTON_CLICKED);
			openMarket();			
		}
		else
		{
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.HOME_UPDATE_OVERLAY_DISMISSED);
			Editor editor = accountPrefs.edit();
			editor.putBoolean(HikeConstants.Extras.SHOW_UPDATE_OVERLAY, false);
			editor.commit();

			findViewById(R.id.overlay_layout).setVisibility(View.GONE);
			showUpdateToolTip(HikeConstants.CRITICAL_UPDATE);
		}
	}

	private void showUpdateToolTip(int updateType)
	{
		updateToolTipParent = findViewById(R.id.tool_tip_on_top);
		updateToolTipParent.setVisibility(View.VISIBLE);
		((LinearLayout)updateToolTipParent.findViewById(R.id.tool_tip_parent_layout)).setGravity(Gravity.CENTER_HORIZONTAL);
		mToolTip = updateToolTipParent.findViewById(R.id.credits_help_layout);
		mToolTip.setBackgroundResource(R.drawable.home_credits_tool_tip_bg);

		// To make the tool tip non closable if a critical update is available
		mToolTip.findViewById(R.id.close).setVisibility(updateType == HikeConstants.NORMAL_UPDATE ? View.VISIBLE : View.GONE);

		((MarginLayoutParams)mToolTip.getLayoutParams()).setMargins(0, 0, 0, 0);

		TextView text = (TextView) mToolTip.findViewById(R.id.tool_tip);
		((MarginLayoutParams)text.getLayoutParams()).setMargins((updateType == HikeConstants.NORMAL_UPDATE ? 0 : (int) (15*Utils.densityMultiplier)), 0, 0, 0);
		text.setText(this.accountPrefs.getString(HikeConstants.Extras.UPDATE_MESSAGE, ""));

		if (!isToolTipShowing) {
			Animation alphaIn = AnimationUtils.loadAnimation(MessagesList.this,
					android.R.anim.fade_in);
			alphaIn.setStartOffset(1000);
			mToolTip.setAnimation(alphaIn);
		}
		mToolTip.setVisibility(View.VISIBLE);

	}

	private void showGroupChatToolTip()
	{
		groupChatToolTipParent = findViewById(R.id.tool_tip_on_top);
		groupChatToolTipParent.setVisibility(View.VISIBLE);
		((LinearLayout)groupChatToolTipParent.findViewById(R.id.tool_tip_parent_layout)).setGravity(Gravity.CENTER_HORIZONTAL);
		mToolTip = groupChatToolTipParent.findViewById(R.id.credits_help_layout);
		mToolTip.setBackgroundResource(R.drawable.home_credits_tool_tip_bg);

		((MarginLayoutParams)mToolTip.getLayoutParams()).setMargins(0, 0, 0, 0);

		TextView text = (TextView) mToolTip.findViewById(R.id.tool_tip);
		text.setText(R.string.we_have_gc);

		if (!isToolTipShowing) {
			Animation alphaIn = AnimationUtils.loadAnimation(MessagesList.this,
					android.R.anim.fade_in);
			alphaIn.setStartOffset(1000);
			mToolTip.setAnimation(alphaIn);
		}
		mToolTip.setVisibility(View.VISIBLE);

	}

	@Override
	public void onSidebarOpened() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSidebarClosed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onContentTouchedWhenOpening() 
	{
		parentLayout.closeSidebar();
		return true;
	}
}
