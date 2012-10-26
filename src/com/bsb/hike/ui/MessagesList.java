package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
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
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadAndInstallUpdateAsyncTask;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.DrawerLayout;

public class MessagesList extends DrawerBaseActivity implements OnClickListener, OnItemClickListener, HikePubSub.Listener, android.content.DialogInterface.OnClickListener, Runnable
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

	private boolean introMessageAdded = false;

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
			HikePubSub.GROUP_NAME_CHANGED, 
			HikePubSub.UPDATE_AVAILABLE, 
			HikePubSub.CONTACT_ADDED,
			HikePubSub.MESSAGE_DELETED,
			HikePubSub.TYPING_CONVERSATION,
			HikePubSub.END_TYPING_CONVERSATION
	};

	private Dialog updateAlert;

	private Button updateAlertOkBtn;
	
	private Handler clearTypingNotificationHandler;

	private Map<String, ClearTypingNotification> pendingClearTypingNotifications;

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
		HikeMessengerApp.getFacebook().extendAccessTokenIfNeeded(this, null);
	}
	
	private class DeleteConversationsAsyncTask extends AsyncTask<Conversation, Void, Conversation[]>
	{

		@Override
		protected Conversation[] doInBackground(Conversation... convs)
		{
			HikeConversationsDatabase db = null;
			ArrayList<Long> ids = new ArrayList<Long>(convs.length);
			ArrayList<String> msisdns =new ArrayList<String>(convs.length);
			Editor editor = getSharedPreferences(HikeConstants.DRAFT_SETTING, MODE_PRIVATE).edit();
			for (Conversation conv : convs)
			{
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
	protected void onNewIntent(Intent intent) 
	{
		// Required here since onCreate is not called when we come to this activity after deleting/unlinking account.
		if (Utils.requireAuth(this))
		{
			return;
		}
		// Doing this to close the drawer when the user selects the home option on the drawer
		if(intent.getBooleanExtra(HikeConstants.Extras.GOING_BACK_TO_HOME, false))
		{
			((DrawerLayout) findViewById(R.id.drawer_layout)).closeLeftSidebar(true);
		}

		if(intent.hasExtra(HikeConstants.Extras.GROUP_LEFT))
		{
			Log.d(getClass().getSimpleName(), "LEAVING GROUP FROM ONNEWINTENT");
			leaveGroup(mConversationsByMSISDN.get(intent.getStringExtra(HikeConstants.Extras.GROUP_LEFT)));
			intent.removeExtra(HikeConstants.Extras.GROUP_LEFT);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Utils.setDensityMultiplier(MessagesList.this);
		if (Utils.requireAuth(this))
		{
			return;
		}

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
		afterSetContentView(savedInstanceState);

		clearTypingNotificationHandler = new Handler();
		pendingClearTypingNotifications = new HashMap<String, MessagesList.ClearTypingNotification>();

		isToolTipShowing = savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.TOOLTIP_SHOWING);
		wasAlertCancelled = savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.ALERT_CANCELLED);
		deviceDetailsSent = savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);
		introMessageAdded = savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.INTRO_MESSAGE_ADDED);

		int updateTypeAvailable = accountPrefs.getInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
		showUpdatePopup(updateTypeAvailable);

		mConversationsView = (ListView) findViewById(R.id.conversations);

		if(getIntent().getBooleanExtra(HikeConstants.Extras.FIRST_TIME_USER, false))
		{
			if(!deviceDetailsSent)
			{
				sendDeviceDetails();
			}
			if(!introMessageAdded)
			{
				// Delaying the making of threads
				(new Handler()).postDelayed(new Runnable() 
				{
					@Override
					public void run() 
					{
						createNewConversationsForFirstTimeUser();
					}
				}, 500);
				introMessageAdded = true;
			}
		}

		View view = findViewById(R.id.title_hikeicon);
		view.setVisibility(View.VISIBLE);

		/*
		 * mSearchIconView = findViewById(R.id.search); mSearchIconView.setOnClickListener(this);
		 */

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

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		/* register for long-press's */
		registerForContextMenu(mConversationsView);

		/*
		 *  Calling this manually since this method is not called when the activity is created.
		 *  Need to call this to check if the user left the group.
		 */
		onNewIntent(getIntent());
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

	private void createNewConversationsForFirstTimeUser()
	{
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(400);
		
		MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.v1);
		mediaPlayer.start();

		String sortOrder = Phone.LAST_TIME_CONTACTED + " DESC LIMIT " + 10;
		Cursor c = getContentResolver().query(Phone.CONTENT_URI, new String[] { Phone.NUMBER }, null, null, sortOrder);
		int numberColIdx = c.getColumnIndex(Phone.NUMBER);
		List<String> recentlyContactedNumbers = new ArrayList<String>();
		while(c.moveToNext())
		{
			recentlyContactedNumbers.add(c.getString(numberColIdx));
		}

		List<ContactInfo> recentNonHikeContacts = HikeUserDatabase.getInstance().getNonHikeContactsFromListOfNumbers(recentlyContactedNumbers);
		List<ContactInfo> hikeContacts = HikeUserDatabase.getInstance().getContactsOrderedByLastMessaged(3, -1, true, true);
		Log.d(getClass().getSimpleName(), "Number of recent contacts: " + recentNonHikeContacts.size() + " HIKE CONTACT: " + hikeContacts.size());

		int numRecentContactsToShow = HikeConstants.MAX_CONVERSATIONS - hikeContacts.size();
		numRecentContactsToShow = recentNonHikeContacts.size() < numRecentContactsToShow ? recentNonHikeContacts.size() : numRecentContactsToShow;

		for(int i = 0; i<numRecentContactsToShow; i++)
		{
			addIntroMessage(false, recentNonHikeContacts.get(i));
		}
		for(ContactInfo contactInfo : hikeContacts)
		{
			addIntroMessage(true, contactInfo);
		}
	}

	private void addIntroMessage(boolean onHike, ContactInfo contactInfo)
	{
		/*
		 * Making a json to  be used as the metadata for the intro message
		 */
		JSONObject jsonObject = new JSONObject();
		try 
		{
			jsonObject.put(HikeConstants.TYPE, HikeConstants.INTRO_MESSAGE);
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}

		String message = String.format(getString(onHike ? R.string.intro_hike_thread : R.string.intro_sms_thread), contactInfo.getFirstName());
		ConvMessage convMessage = new  ConvMessage(message, contactInfo.getMsisdn(), System.currentTimeMillis()/1000, State.RECEIVED_UNREAD);
		convMessage.setSMS(!onHike);
		try 
		{
			convMessage.setMetadata(jsonObject);
		} 
		catch (JSONException e) 
		{
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}

		HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
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
		outState.putBoolean(HikeConstants.Extras.INTRO_MESSAGE_ADDED, introMessageAdded);
		super.onSaveInstanceState(outState);
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
		if(accountPrefs != null)
		{
			Utils.incrementNumTimesScreenOpen(accountPrefs, HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
		}
		super.onDestroy();
		Log.d(getClass().getSimpleName(), "onDestroy " + this);
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
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
		super.onEventReceived(type, object);
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
		else if (HikePubSub.MESSAGE_DELETED.equals(type))
		{
			Log.d(getClass().getSimpleName(), "Message Deleted");
			final ConvMessage message = (ConvMessage) object;
			String msisdn = message.getMsisdn();
			final Conversation conversation = mConversationsByMSISDN.get(msisdn);

			if(conversation == null)
			{
				return;
			}

			List<ConvMessage> existingList = conversation.getMessages();
			/*
			 *  Checking if the message deleted was the last message in the conversation.
			 *  If it wasn't, no need to do anything here. 
			 */
			if(existingList.get(existingList.size() - 1).getMsgID() != message.getMsgID())
			{
				Log.d(getClass().getSimpleName(), "The last message was not deleted. No need to do anything here");
				return;
			}

			final List<ConvMessage> messageList = HikeConversationsDatabase.getInstance().getConversationThread
					(msisdn, conversation.getContactId(), conversation.getConvId(), 1, conversation);
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					if(messageList.isEmpty())
					{
						mConversationsByMSISDN.remove(conversation.getMsisdn());
						mConversationsAdded.remove(conversation.getMsisdn());
						mAdapter.remove(conversation);
					}
					else
					{
						conversation.setMessages(messageList);
					}
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

			mAdapter.setNotifyOnChange(false);
			runOnUiThread(this);
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))
		{
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_CONFIRMED.ordinal()))
			{
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				runOnUiThread(this);
			}
		}
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))
		{
			Pair<String, long[]> pair = (Pair<String, long[]>) object;

			long[] ids = (long[]) pair.second;
			// TODO we could keep a map of msgId -> conversation objects somewhere to make this faster
			for (int i = 0; i < ids.length; i++)
			{
				ConvMessage msg = findMessageById(ids[i]);
				if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED_READ.ordinal()))
				{
					// If the msisdn don't match we simply return
					if(!msg.getMsisdn().equals(pair.first))
					{
						return;
					}
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				}
			}
			runOnUiThread(this);
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))
		{
			Pair<String, Long> pair = (Pair<String, Long>) object;

			long msgId = pair.second;
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg, ConvMessage.State.SENT_DELIVERED.ordinal()))
			{
				// If the msisdn don't match we simply return
				if(!msg.getMsisdn().equals(pair.first))
				{
					return;
				}
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
					showUpdatePopup(updateType);
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
		else if (HikePubSub.TYPING_CONVERSATION.equals(type))
		{
			String msisdn = (String) object;
			toggleTypingNotification(true, msisdn);

			ClearTypingNotification clearTypingNotification;
			if(!pendingClearTypingNotifications.containsKey(msisdn))
			{
				clearTypingNotification = new ClearTypingNotification(msisdn);
				pendingClearTypingNotifications.put(msisdn, clearTypingNotification);
			}
			else
			{
				clearTypingNotification = pendingClearTypingNotifications.get(msisdn);
				clearTypingNotificationHandler.removeCallbacks(clearTypingNotification);
			}
			clearTypingNotificationHandler.postDelayed(clearTypingNotification, HikeConstants.LOCAL_CLEAR_TYPING_TIME);
		}
		else if (HikePubSub.END_TYPING_CONVERSATION.equals(type))
		{
			toggleTypingNotification(false, (String) object);
		}
	}

	private class ClearTypingNotification implements Runnable 
	{
		String msisdn;

		public ClearTypingNotification(String msisdn) 
		{
			this.msisdn = msisdn;
		}

		@Override
		public void run() 
		{
			toggleTypingNotification(false, msisdn);
		}
	};

	private void toggleTypingNotification(boolean isTyping, String msisdn)
	{
		Conversation conversation = mConversationsByMSISDN.get(msisdn);
		if(conversation == null)
		{
			Log.d(getClass().getSimpleName(), "Conversation Does not exist");
			return;
		}
		List<ConvMessage> messageList = conversation.getMessages();
		if(messageList.isEmpty())
		{
			Log.d(getClass().getSimpleName(), "Conversation is empty");
			return;
		}
		if(isTyping)
		{
			ConvMessage message = messageList.get(messageList.size() - 1);
			if(!HikeConstants.IS_TYPING.equals(message.getMessage()) && message.getMsgID() != -1 && message.getMappedMsgID() != -1)
			{
				// Setting the msg id and mapped msg id as -1 to identify that this is an "is typing..." message.
				ConvMessage convMessage = new ConvMessage(HikeConstants.IS_TYPING, msisdn, message.getTimestamp(), ConvMessage.State.RECEIVED_UNREAD, -1, -1);
				messageList.add(convMessage);
			}
		}
		else
		{
			ClearTypingNotification clearTypingNotification = pendingClearTypingNotifications.remove(msisdn);
			if(clearTypingNotification != null)
			{
				clearTypingNotificationHandler.removeCallbacks(clearTypingNotification);
			}

			ConvMessage message = messageList.get(messageList.size() - 1);
			if(HikeConstants.IS_TYPING.equals(message.getMessage()) && message.getMsgID() == -1 && message.getMappedMsgID() == -1)
			{
				messageList.remove(message);
			}
		}
		runOnUiThread(this);
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

	private void updateApp(int updateType)
	{
		if(TextUtils.isEmpty(this.accountPrefs.getString(HikeConstants.Extras.UPDATE_URL, "")))
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
		else
		{
			if(updateType == HikeConstants.NORMAL_UPDATE)
			{
				updateAlert.dismiss();
			}
			else
			{
				updateAlertOkBtn.setText("Downloading...");
				updateAlertOkBtn.setEnabled(false);
			}
			// In app update!
			DownloadAndInstallUpdateAsyncTask downloadAndInstallUpdateAsyncTask = new DownloadAndInstallUpdateAsyncTask(this, accountPrefs.getString(HikeConstants.Extras.UPDATE_URL, ""));
			downloadAndInstallUpdateAsyncTask.execute();
		}
	}
	public void onToolTipClicked(View v)
	{
		if(groupChatToolTipParent != null && groupChatToolTipParent.getVisibility() == View.VISIBLE)
		{
			setToolTipDismissed();
			openOptionsMenu();
			return;
		}
	}

	private void leaveGroup(Conversation conv)
	{
		if(conv == null)
		{
			Log.d(getClass().getSimpleName(), "Invalid conversation");
			return;
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, conv.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
		task.execute(conv);
	}

	private void showUpdatePopup(final int updateType)
	{
		if(updateType == HikeConstants.NO_UPDATE)
		{
			return;
		}

		if(updateType == HikeConstants.NORMAL_UPDATE)
		{
			// Here we check if the user cancelled the update popup for this version earlier
			String updateToIgnore = accountPrefs.getString(HikeConstants.Extras.UPDATE_TO_IGNORE, ""); 
			if(!TextUtils.isEmpty(updateToIgnore) && updateToIgnore.equals(accountPrefs.getString(HikeConstants.Extras.LATEST_VERSION, "")))
			{
				return;
			}
		}

		// If we are already showing an update we don't need to do anything else
		if(updateAlert != null && updateAlert.isShowing())
		{
			return;
		}

		updateAlert = new Dialog(MessagesList.this, R.style.Theme_CustomDialog);
		updateAlert.setContentView(R.layout.alert_box);

		((ImageView)updateAlert.findViewById(R.id.alert_image)).setVisibility(View.GONE);

		int padding = (int) (10 * Utils.densityMultiplier);

		TextView updateText = ((TextView)updateAlert.findViewById(R.id.alert_text));
		TextView updateTitle = (TextView)updateAlert.findViewById(R.id.alert_title);

		updateText.setPadding(padding, 0, padding, padding);
		updateText.setGravity(Gravity.CENTER);
		updateText.setText(updateType == HikeConstants.CRITICAL_UPDATE ? R.string.critical_update : R.string.normal_update);

		updateTitle.setPadding(padding, padding, padding, padding);
		updateTitle.setGravity(Gravity.CENTER);
		updateTitle.setText(updateType == HikeConstants.CRITICAL_UPDATE ? R.string.critical_update_head : R.string.normal_update_head);


		Button cancelBtn = null;
		if(updateType == HikeConstants.CRITICAL_UPDATE)
		{
			((Button)updateAlert.findViewById(R.id.alert_ok_btn)).setVisibility(View.GONE);
			((Button)updateAlert.findViewById(R.id.alert_cancel_btn)).setVisibility(View.GONE);
			(updateAlert.findViewById(R.id.btn_divider)).setVisibility(View.GONE);

			updateAlertOkBtn = (Button) updateAlert.findViewById(R.id.alert_center_btn);
			updateAlertOkBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			updateAlertOkBtn = (Button) updateAlert.findViewById(R.id.alert_ok_btn);
			cancelBtn = (Button) updateAlert.findViewById(R.id.alert_cancel_btn);
			cancelBtn.setText(R.string.cancel);
		}
		updateAlertOkBtn.setText(R.string.update_app);

		updateAlert.setCancelable(true);

		updateAlertOkBtn.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				updateApp(updateType);
			}
		});

		if(cancelBtn != null)
		{
			cancelBtn.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View v) 
				{
					updateAlert.cancel();
				}
			});
		}

		updateAlert.setOnCancelListener(new OnCancelListener() 
		{
			@Override
			public void onCancel(DialogInterface dialog) 
			{
				if(updateType == HikeConstants.CRITICAL_UPDATE)
				{
					finish();
				}
				else
				{
					Editor editor = accountPrefs.edit();
					editor.putString(HikeConstants.Extras.UPDATE_TO_IGNORE, accountPrefs.getString(HikeConstants.Extras.LATEST_VERSION, ""));
					editor.commit();
				}
			}
		});

		updateAlert.show();
	}

	public void updateFailed()
	{
		if(updateAlertOkBtn != null)
		{
			updateAlertOkBtn.setText(R.string.update_app);
			updateAlertOkBtn.setEnabled(true);
		}
	}

	public void onOverlayButtonClick(View v)
	{
		if (v.getId() != R.id.overlay_layout) 
		{
			Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.HOME_UDPATE_OVERLAY_BUTTON_CLICKED);
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
}
