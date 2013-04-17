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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.adapters.ConversationsAdapter;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;
import com.bsb.hike.models.ConvMessage.State;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.StatusMessage;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadAndInstallUpdateAsyncTask;
import com.bsb.hike.utils.AppRater;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.DrawerLayout;
import com.fiksu.asotracking.FiksuTrackingManager;

public class MessagesList extends DrawerBaseActivity implements
		OnClickListener, OnItemClickListener, HikePubSub.Listener,
		android.content.DialogInterface.OnClickListener, Runnable,
		OnItemLongClickListener {
	public static final Object COMPOSE = "compose";

	private ConversationsAdapter mAdapter;

	private Map<String, Conversation> mConversationsByMSISDN;

	private Set<String> mConversationsAdded;

	private ListView mConversationsView;

	private View mEmptyView;

	private Comparator<? super Conversation> mConversationsComparator;

	private View mToolTip;

	private SharedPreferences accountPrefs;

	private View updateToolTipParent;

	private View groupChatToolTipParent;

	private boolean wasAlertCancelled = false;

	private boolean deviceDetailsSent = false;

	private boolean introMessageAdded = false;

	private boolean nuxNumbersInvited = false;

	private String[] pubSubListeners = { HikePubSub.MESSAGE_RECEIVED,
			HikePubSub.SERVER_RECEIVED_MSG, HikePubSub.MESSAGE_DELIVERED_READ,
			HikePubSub.MESSAGE_DELIVERED, HikePubSub.MESSAGE_FAILED,
			HikePubSub.NEW_CONVERSATION, HikePubSub.MESSAGE_SENT,
			HikePubSub.MSG_READ, HikePubSub.ICON_CHANGED,
			HikePubSub.GROUP_NAME_CHANGED, HikePubSub.UPDATE_AVAILABLE,
			HikePubSub.CONTACT_ADDED, HikePubSub.LAST_MESSAGE_DELETED,
			HikePubSub.TYPING_CONVERSATION, HikePubSub.END_TYPING_CONVERSATION,
			HikePubSub.FAVORITE_TOGGLED, HikePubSub.TIMELINE_UPDATE_RECIEVED,
			HikePubSub.RESET_NOTIFICATION_COUNTER,
			HikePubSub.DECREMENT_NOTIFICATION_COUNTER,
			HikePubSub.DRAWER_ANIMATION_COMPLETE };

	private Dialog updateAlert;

	private Button updateAlertOkBtn;

	private Handler messageRefreshHandler;

	private Button notificationCounter;

	private int notificationCount;

	private String userMsisdn;

	@Override
	protected void onPause() {
		super.onPause();
		Log.d("MESSAGE LIST", "Currently in pause state. .......");
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, null);
	}

	@Override
	protected void onResume() {
		super.onResume();
		HikeMessengerApp.getPubSub().publish(HikePubSub.NEW_ACTIVITY, this);
		HikeMessengerApp.getFacebook().extendAccessTokenIfNeeded(this, null);

		int unseenStatus = Utils.getNotificationCount(accountPrefs, false);
		setNotificationCounter(unseenStatus);
	}

	private class DeleteConversationsAsyncTask extends
			AsyncTask<Conversation, Void, Conversation[]> {

		@Override
		protected Conversation[] doInBackground(Conversation... convs) {
			HikeConversationsDatabase db = null;
			ArrayList<Long> ids = new ArrayList<Long>(convs.length);
			ArrayList<String> msisdns = new ArrayList<String>(convs.length);
			Editor editor = getSharedPreferences(HikeConstants.DRAFT_SETTING,
					MODE_PRIVATE).edit();
			for (Conversation conv : convs) {
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
		protected void onPostExecute(Conversation[] deleted) {
			NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			for (Conversation conversation : deleted) {
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
		// Required here since onCreate is not called when we come to this
		// activity after deleting/unlinking account.
		if (Utils.requireAuth(this)) {
			return;
		}
		// Doing this to close the drawer when the user selects the home option
		// on the drawer
		if (intent.getBooleanExtra(HikeConstants.Extras.GOING_BACK_TO_HOME,
				false)) {
			((DrawerLayout) findViewById(R.id.drawer_layout))
					.closeSidebar(true);
		}

		if (intent.hasExtra(HikeConstants.Extras.GROUP_LEFT)) {
			Log.d(getClass().getSimpleName(), "LEAVING GROUP FROM ONNEWINTENT");
			leaveGroup(mConversationsByMSISDN.get(intent
					.getStringExtra(HikeConstants.Extras.GROUP_LEFT)));
			intent.removeExtra(HikeConstants.Extras.GROUP_LEFT);
		}

		if (intent.getBooleanExtra(HikeConstants.Extras.OPEN_FAVORITES, false)) {
			parentLayout.toggleSidebar(true, false);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setDensityMultiplier(MessagesList.this);
		if (Utils.requireAuth(this)) {
			return;
		}

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				0);

		Intent i = null;
		boolean justSignedUp = accountPrefs.getBoolean(
				HikeMessengerApp.JUST_SIGNED_UP, false);
		if (justSignedUp
				&& !accountPrefs.getBoolean(HikeMessengerApp.INTRO_DONE, false)) {
			i = new Intent(MessagesList.this, Tutorial.class);
		} else if (!accountPrefs.getBoolean(HikeMessengerApp.NUX1_DONE, false)) {
			i = new Intent(MessagesList.this, HikeListActivity.class);
			i.putExtra(HikeConstants.Extras.SHOW_MOST_CONTACTED, true);
		} else if (!accountPrefs.getBoolean(HikeMessengerApp.NUX2_DONE, false)) {
			i = new Intent(MessagesList.this, HikeListActivity.class);
			i.putExtra(HikeConstants.Extras.SHOW_FAMILY, true);
		}
		if (i != null) {
			boolean startNux = true;
			if (!justSignedUp) {
				startNux = HikeUserDatabase.getInstance().getHikeContactCount() < 10;
			}
			if (startNux) {
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				finish();
				return;
			} else {
				Editor editor = accountPrefs.edit();
				editor.putBoolean(HikeMessengerApp.NUX1_DONE, true);
				editor.putBoolean(HikeMessengerApp.NUX2_DONE, true);
				editor.commit();
			}
		}

		// TODO this is being called everytime this activity is created. Way too
		// often
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		setContentView(R.layout.main);
		afterSetContentView(savedInstanceState);

		wasAlertCancelled = savedInstanceState != null
				&& savedInstanceState
						.getBoolean(HikeConstants.Extras.ALERT_CANCELLED);
		deviceDetailsSent = savedInstanceState != null
				&& savedInstanceState
						.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);
		introMessageAdded = savedInstanceState != null
				&& savedInstanceState
						.getBoolean(HikeConstants.Extras.INTRO_MESSAGE_ADDED);
		nuxNumbersInvited = savedInstanceState != null
				&& savedInstanceState
						.getBoolean(HikeConstants.Extras.NUX_NUMBERS_INVITED);

		int updateTypeAvailable = accountPrefs.getInt(
				HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
		showUpdatePopup(updateTypeAvailable);

		mConversationsView = (ListView) findViewById(R.id.conversations);

		if (justSignedUp) {

			Editor editor = accountPrefs.edit();
			editor.remove(HikeMessengerApp.JUST_SIGNED_UP);
			editor.commit();

			if (!deviceDetailsSent) {
				sendDeviceDetails();
			}
			if (!introMessageAdded) {
				// Delaying the making of threads
				(new Handler()).postDelayed(new Runnable() {
					@Override
					public void run() {
						createNewConversationsForFirstTimeUser();
					}
				}, 500);
				introMessageAdded = true;
			}
		}
		if (!nuxNumbersInvited) {
			if (accountPrefs.contains(HikeMessengerApp.INVITED_NUMBERS)) {
				inviteNuxNumbers();
			}
		}

		if (getIntent().getBooleanExtra(HikeConstants.Extras.FROM_NUX_SCREEN,
				false)) {
			if (accountPrefs.contains(HikeConstants.LogEvent.NUX_SKIP2)
					|| accountPrefs.contains(HikeConstants.LogEvent.NUX_SKIP1)) {
				sendNuxEvents();
			}
		}

		userMsisdn = accountPrefs
				.getString(HikeMessengerApp.MSISDN_SETTING, "");

		notificationCounter = (Button) findViewById(R.id.title_hikeicon);
		notificationCounter.setVisibility(View.VISIBLE);
		findViewById(R.id.counter_container).setVisibility(View.VISIBLE);

		/*
		 * mSearchIconView = findViewById(R.id.search);
		 * mSearchIconView.setOnClickListener(this);
		 */

		/* set the empty view layout for the list */
		mEmptyView = findViewById(R.id.empty_view);
		mEmptyView.setOnClickListener(this);

		mConversationsView.setEmptyView(mEmptyView);
		mConversationsView.setOnItemClickListener(this);

		if (mAdapter == null || mConversationsByMSISDN == null
				|| mConversationsAdded == null) {
			HikeConversationsDatabase db = HikeConversationsDatabase
					.getInstance();
			List<Conversation> conversations = db.getConversations();

			mConversationsByMSISDN = new HashMap<String, Conversation>(
					conversations.size());
			mConversationsAdded = new HashSet<String>();

			/*
			 * Use an iterator so we can remove conversations w/ no messages
			 * from our list
			 */
			for (Iterator<Conversation> iter = conversations.iterator(); iter
					.hasNext();) {
				Conversation conv = (Conversation) iter.next();
				mConversationsByMSISDN.put(conv.getMsisdn(), conv);
				if (conv.getMessages().isEmpty()
						&& !(conv instanceof GroupConversation)) {
					iter.remove();
				} else {
					mConversationsAdded.add(conv.getMsisdn());
				}
			}

			mAdapter = new ConversationsAdapter(MessagesList.this,
					R.layout.conversation_item, conversations, parentLayout);

			HikeMessengerApp.getPubSub().addListeners(MessagesList.this,
					pubSubListeners);
		}
		/*
		 * we need this object every time a message comes in, seems simplest to
		 * just create it once
		 */
		mConversationsComparator = new Conversation.ConversationComparator();

		/*
		 * because notifyOnChange gets re-enabled whenever we call
		 * notifyDataSetChanged it's simpler to assume it's set to false and
		 * always notifyOnChange by hand
		 */
		mAdapter.setNotifyOnChange(false);

		View footerView = getLayoutInflater().inflate(
				R.layout.conversation_list_footer, null);
		AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(
				AbsListView.LayoutParams.MATCH_PARENT, (int) getResources()
						.getDimension(R.dimen.conversation_footer_height));
		footerView.setLayoutParams(layoutParams);
		mConversationsView.addFooterView(footerView);

		mConversationsView.setAdapter(mAdapter);

		mConversationsView.setOnItemLongClickListener(this);

		/*
		 * Calling this manually since this method is not called when the
		 * activity is created. Need to call this to check if the user left the
		 * group.
		 */
		onNewIntent(getIntent());

		// if (!accountPrefs.getBoolean(HikeMessengerApp.FAVORITES_INTRO_SHOWN,
		// false)) {
		// showFavoritesIntroOverlay();
		// }

		/*
		 * Check whether we have an existing typing notification for any
		 * conversations
		 */
		Iterator<String> iterator = HikeMessengerApp.getTypingNotificationSet()
				.keySet().iterator();
		while (iterator.hasNext()) {
			String msisdn = iterator.next();
			toggleTypingNotification(true, msisdn);
		}

		if (!accountPrefs.getBoolean(HikeMessengerApp.FRIEND_INTRO_SHOWN, false)) {
			findViewById(R.id.friend_intro).setVisibility(View.VISIBLE);
		} else if (savedInstanceState == null) {
			/*
			 * Only show app rater if the tutorial is not being shown an the app
			 * was just launched i.e not an orientation change
			 */
			AppRater.appLaunched(this);
		}
	}

	private void sendNuxEvents() {
		(new Handler()).postDelayed(new Runnable() {

			@Override
			public void run() {

				JSONObject data = new JSONObject();
				JSONObject obj = new JSONObject();

				try {
					if (accountPrefs.contains(HikeConstants.LogEvent.NUX_SKIP1)) {
						data.put(HikeConstants.LogEvent.NUX_SKIP1, accountPrefs
								.getBoolean(HikeConstants.LogEvent.NUX_SKIP1,
										false) ? 1 : 0);
					}
					if (accountPrefs.contains(HikeConstants.LogEvent.NUX_SKIP2)) {
						data.put(HikeConstants.LogEvent.NUX_SKIP2, accountPrefs
								.getBoolean(HikeConstants.LogEvent.NUX_SKIP2,
										false) ? 1 : 0);
					}
					data.put(HikeConstants.LogEvent.TAG, "mob");

					obj.put(HikeConstants.TYPE,
							HikeConstants.MqttMessageTypes.ANALYTICS_EVENT);
					obj.put(HikeConstants.DATA, data);
				} catch (JSONException e) {
					Log.e(getClass().getSimpleName(), "Invalid JSON", e);
				}

				Editor editor = accountPrefs.edit();
				editor.remove(HikeConstants.LogEvent.NUX_SKIP1);
				editor.remove(HikeConstants.LogEvent.NUX_SKIP2);
				editor.commit();

				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
						obj);
			}
		}, 5 * 1000);
	}

	public void onFavoriteIntroClick(View v) {
		findViewById(R.id.favorite_intro).setVisibility(View.GONE);

		Editor editor = accountPrefs.edit();
		editor.putBoolean(HikeMessengerApp.FAVORITES_INTRO_SHOWN, true);
		editor.commit();
	}

	public void onFriendIntroClick(View v) {
		findViewById(R.id.friend_intro).setVisibility(View.GONE);

		Editor editor = accountPrefs.edit();
		editor.putBoolean(HikeMessengerApp.FRIEND_INTRO_SHOWN, true);
		editor.commit();
	}

	private void sendDeviceDetails() {
		// We're adding this delay to ensure that the service is alive before
		// sending the message
		(new Handler()).postDelayed(new Runnable() {
			@Override
			public void run() {
				JSONObject obj = Utils.getDeviceDetails(MessagesList.this);
				if (obj != null) {
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.MQTT_PUBLISH, obj);
				}
				Utils.requestAccountInfo(false);
			}
		}, 5 * 1000);
		deviceDetailsSent = true;
	}

	private void inviteNuxNumbers() {
		(new Handler()).postDelayed(new Runnable() {
			@Override
			public void run() {
				String invitedNumbers = accountPrefs.getString(
						HikeMessengerApp.INVITED_NUMBERS, "");
				if (TextUtils.isEmpty(invitedNumbers)) {
					return;
				}
				String[] invitedNumbersArray = invitedNumbers.split(",");
				for (String msisdn : invitedNumbersArray) {
					FiksuTrackingManager.uploadPurchaseEvent(MessagesList.this,
							HikeConstants.INVITE, HikeConstants.INVITE_SENT,
							HikeConstants.CURRENCY);
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.MQTT_PUBLISH,
							Utils.makeHike2SMSInviteMessage(msisdn,
									MessagesList.this).serialize());
				}
				Editor editor = accountPrefs.edit();
				editor.remove(HikeMessengerApp.INVITED_NUMBERS);
				editor.commit();
			}
		}, 5 * 1000);
		nuxNumbersInvited = true;
	}

	private void createNewConversationsForFirstTimeUser() {
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		int ringerMode = audioManager.getRingerMode();
		int vibrateMode = audioManager
				.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);

		if (vibrateMode != AudioManager.VIBRATE_SETTING_OFF) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(400);
		}

		if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
			MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.v1);
			mediaPlayer.start();
		}

		List<ContactInfo> recentNonHikeContacts = new ArrayList<ContactInfo>(0);
		if (HikeMessengerApp.isIndianUser()) {
			recentNonHikeContacts = HikeUserDatabase.getInstance()
					.getNonHikeRecentContacts(10, true, null);
		}
		List<ContactInfo> hikeContacts = HikeUserDatabase.getInstance()
				.getContactsOrderedByLastMessaged(3,
						HikeConstants.ON_HIKE_VALUE);
		Log.d(getClass().getSimpleName(), "Number of recent contacts: "
				+ recentNonHikeContacts.size() + " HIKE CONTACT: "
				+ hikeContacts.size());

		int numRecentContactsToShow = HikeConstants.MAX_CONVERSATIONS
				- hikeContacts.size();
		numRecentContactsToShow = recentNonHikeContacts.size() < numRecentContactsToShow ? recentNonHikeContacts
				.size() : numRecentContactsToShow;

		for (int i = 0; i < numRecentContactsToShow; i++) {
			addIntroMessage(false, recentNonHikeContacts.get(i));
		}
		for (ContactInfo contactInfo : hikeContacts) {
			addIntroMessage(true, contactInfo);
		}
	}

	private void addIntroMessage(boolean onHike, ContactInfo contactInfo) {
		/*
		 * Making a json to be used as the metadata for the intro message
		 */
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(HikeConstants.TYPE, HikeConstants.INTRO_MESSAGE);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		String message;
		if (onHike) {
			boolean firstIntro = contactInfo.getMsisdn().hashCode() % 2 == 0;
			message = String.format(
					getString(firstIntro ? R.string.start_thread1
							: R.string.start_thread1), contactInfo
							.getFirstName());
		} else {
			message = String.format(getString(R.string.intro_sms_thread),
					contactInfo.getFirstName());
		}
		ConvMessage convMessage = new ConvMessage(message,
				contactInfo.getMsisdn(), System.currentTimeMillis() / 1000,
				State.RECEIVED_UNREAD);
		convMessage.setSMS(!onHike);
		try {
			convMessage.setMetadata(jsonObject);
		} catch (JSONException e) {
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}

		HikeConversationsDatabase.getInstance().addConversationMessages(
				convMessage);

		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED,
				convMessage);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.TOOLTIP_SHOWING,
				mToolTip != null && mToolTip.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT,
				deviceDetailsSent);
		outState.putBoolean(HikeConstants.Extras.ALERT_CANCELLED,
				wasAlertCancelled);
		outState.putBoolean(HikeConstants.Extras.INTRO_MESSAGE_ADDED,
				introMessageAdded);
		outState.putBoolean(HikeConstants.Extras.NUX_NUMBERS_INVITED,
				nuxNumbersInvited);
		super.onSaveInstanceState(outState);
	}

	private Intent createIntentForConversation(Conversation conversation) {
		Intent intent = new Intent(MessagesList.this, ChatThread.class);
		if (conversation.getContactName() != null) {
			intent.putExtra(HikeConstants.Extras.NAME,
					conversation.getContactName());
		}
		intent.putExtra(HikeConstants.Extras.MSISDN, conversation.getMsisdn());
		return intent;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		if (adapterView.getId() == R.id.favorite_list) {
			return super.onItemLongClick(adapterView, view, position, id);
		}
		if (position >= mAdapter.getCount()) {
			return false;
		}
		ArrayList<String> optionsList = new ArrayList<String>();

		final Conversation conv = mAdapter.getItem(position);

		optionsList.add(getString(R.string.shortcut));
		if (conv instanceof GroupConversation) {
			optionsList.add(getString(R.string.delete_leave));
		} else {
			optionsList.add(getString(R.string.delete));
		}

		final String[] options = new String[optionsList.size()];
		optionsList.toArray(options);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this,
				R.layout.alert_item, R.id.item, options);

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String option = options[which];
						if (getString(R.string.shortcut).equals(option)) {
							Utils.logEvent(MessagesList.this,
									HikeConstants.LogEvent.ADD_SHORTCUT);
							Intent shortcutIntent = createIntentForConversation(conv);
							Intent intent = new Intent();
							intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
									shortcutIntent);
							intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
									conv.getLabel());
							Drawable d = IconCacheManager.getInstance()
									.getIconForMSISDN(conv.getMsisdn());
							Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
							Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
									60, 60, false);
							bitmap = null;
							intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, scaled);
							intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
							sendBroadcast(intent);
						} else if (getString(R.string.delete).equals(option)) {
							Utils.logEvent(MessagesList.this,
									HikeConstants.LogEvent.DELETE_CONVERSATION);
							DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
							task.execute(conv);
						} else if (getString(R.string.delete_leave).equals(
								option)) {
							Utils.logEvent(MessagesList.this,
									HikeConstants.LogEvent.DELETE_CONVERSATION);
							leaveGroup(conv);
						}
					}
				});

		AlertDialog alertDialog = builder.show();
		alertDialog.getListView().setDivider(
				getResources()
						.getDrawable(R.drawable.ic_thread_divider_profile));
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.MENU);
		if (groupChatToolTipParent != null
				&& groupChatToolTipParent.getVisibility() == View.VISIBLE) {
			onToolTipClosed(null);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.deleteconversations:
			if (!mAdapter.isEmpty()) {
				Utils.logEvent(MessagesList.this,
						HikeConstants.LogEvent.DELETE_ALL_CONVERSATIONS_MENU);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.delete_all_question)
						.setPositiveButton(R.string.delete, this)
						.setNegativeButton(R.string.cancel, this).show();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onDestroy() {
		if (accountPrefs != null) {
			Utils.incrementNumTimesScreenOpen(accountPrefs,
					HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
		}
		HikeMessengerApp.getPubSub().removeListeners(MessagesList.this,
				pubSubListeners);
		super.onDestroy();
		Log.d(getClass().getSimpleName(), "onDestroy " + this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.title_hikeicon) {
			changeMqttBroker();
		}
	}

	private void changeMqttBroker() {
		final Dialog mqttDialog = new Dialog(this);
		mqttDialog.setContentView(R.layout.mqtt_broker_dialog);

		final EditText mqttHost = (EditText) mqttDialog.findViewById(R.id.host);
		final EditText mqttPort = (EditText) mqttDialog.findViewById(R.id.port);
		Button done = (Button) mqttDialog.findViewById(R.id.done);

		done.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String mqttHostString = mqttHost.getText().toString();
				int mqttPortInt = 0;
				try {
					mqttPortInt = Integer.parseInt(mqttPort.getText()
							.toString());
				} catch (NumberFormatException e) {
					Toast.makeText(getApplicationContext(),
							"Port can only be an integer", Toast.LENGTH_SHORT)
							.show();
					return;
				}

				if (TextUtils.isEmpty(mqttHostString)) {
					Toast.makeText(getApplicationContext(),
							"Enter all details", Toast.LENGTH_SHORT).show();
					return;
				}
				Editor editor = accountPrefs.edit();
				editor.putString(HikeMessengerApp.BROKER_HOST, mqttHostString);
				editor.putInt(HikeMessengerApp.BROKER_PORT, mqttPortInt);
				editor.commit();

				mqttDialog.dismiss();
				Toast.makeText(
						getApplicationContext(),
						"Force stop and start the app again for the changes to take effect",
						Toast.LENGTH_SHORT).show();
			}
		});

		mqttDialog.show();
	}

	private ArrayList<Pair<Conversation, ConvMessage>> receivedMsgWhileAnimating = new ArrayList<Pair<Conversation, ConvMessage>>();

	private boolean refreshAfterAnimation;

	@Override
	public void onEventReceived(String type, Object object) {
		super.onEventReceived(type, object);
		Log.d(getClass().getSimpleName(), "Event received: " + type);
		if ((HikePubSub.MESSAGE_RECEIVED.equals(type))
				|| (HikePubSub.MESSAGE_SENT.equals(type))) {
			Log.d("MESSAGE LIST", "New msg event sent or received.");
			ConvMessage message = (ConvMessage) object;
			/* find the conversation corresponding to this message */
			String msisdn = message.getMsisdn();
			final Conversation conv = mConversationsByMSISDN.get(msisdn);

			if (conv == null) {
				// When a message gets sent from a user we don't have a
				// conversation for, the message gets
				// broadcasted first then the conversation gets created. It's
				// okay that we don't add it now, because
				// when the conversation is broadcasted it will contain the
				// messages
				return;
			}

			if (message.getParticipantInfoState() == ParticipantInfoState.STATUS_MESSAGE) {
				if (!conv.getMessages().isEmpty()) {
					ConvMessage prevMessage = conv.getMessages().get(
							conv.getMessages().size() - 1);
					String metadata = message.getMetadata().serialize();
					message = new ConvMessage(message.getMessage(),
							message.getMsisdn(), prevMessage.getTimestamp(),
							prevMessage.getState(), prevMessage.getMsgID(),
							prevMessage.getMappedMsgID(),
							message.getGroupParticipantMsisdn());
					try {
						message.setMetadata(metadata);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			// For updating the group name if some participant has joined or
			// left the group
			else if ((conv instanceof GroupConversation)
					&& message.getParticipantInfoState() != ParticipantInfoState.NO_INFO) {
				HikeConversationsDatabase hCDB = HikeConversationsDatabase
						.getInstance();
				((GroupConversation) conv).setGroupParticipantList(hCDB
						.getGroupParticipants(conv.getMsisdn(), false, false));
			}
			if (parentLayout.isAnimating()) {
				refreshAfterAnimation = true;
				receivedMsgWhileAnimating
						.add(new Pair<Conversation, ConvMessage>(conv, message));
				return;
			}

			final ConvMessage finalMessage = message;

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					addMessage(conv, finalMessage);

					messageRefreshHandler.removeCallbacks(MessagesList.this);
					messageRefreshHandler.postDelayed(MessagesList.this, 100);
				}
			});

		} else if (HikePubSub.LAST_MESSAGE_DELETED.equals(type)) {
			Pair<ConvMessage, String> messageMsisdnPair = (Pair<ConvMessage, String>) object;

			final ConvMessage message = messageMsisdnPair.first;
			final String msisdn = messageMsisdnPair.second;

			final boolean conversationEmpty = message == null;

			final Conversation conversation = mConversationsByMSISDN
					.get(msisdn);

			final List<ConvMessage> messageList = new ArrayList<ConvMessage>(1);

			if (!conversationEmpty) {
				if (conversation == null) {
					return;
				}
				messageList.add(message);
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (conversationEmpty) {
						mConversationsByMSISDN.remove(msisdn);
						mConversationsAdded.remove(msisdn);
						mAdapter.remove(conversation);
					} else {
						conversation.setMessages(messageList);
					}
					mAdapter.sort(mConversationsComparator);
					mAdapter.notifyDataSetChanged();
					// notifyDataSetChanged sets notifyonChange to true but we
					// want it to always be false
					mAdapter.setNotifyOnChange(false);
				}
			});
		} else if (HikePubSub.NEW_CONVERSATION.equals(type)) {
			final Conversation conversation = (Conversation) object;
			Log.d(getClass().getSimpleName(),
					"New Conversation. Group Conversation? "
							+ (conversation instanceof GroupConversation));
			mConversationsByMSISDN.put(conversation.getMsisdn(), conversation);
			if (conversation.getMessages().isEmpty()
					&& !(conversation instanceof GroupConversation)) {
				return;
			}

			mConversationsAdded.add(conversation.getMsisdn());
			runOnUiThread(new Runnable() {
				public void run() {
					mAdapter.add(conversation);
					if (conversation instanceof GroupConversation) {
						mAdapter.notifyDataSetChanged();
					}
					mAdapter.setNotifyOnChange(false);
				}
			});
		} else if (HikePubSub.MSG_READ.equals(type)) {
			String msisdn = (String) object;
			Conversation conv = mConversationsByMSISDN.get(msisdn);
			if (conv == null) {
				/*
				 * We don't really need to do anything if the conversation does
				 * not exist.
				 */
				return;
			}
			/*
			 * look for the latest received messages and set them to read. Exit
			 * when we've found some read messages
			 */
			List<ConvMessage> messages = conv.getMessages();
			for (int i = messages.size() - 1; i >= 0; --i) {
				ConvMessage msg = messages.get(i);
				if (Utils.shouldChangeMessageState(msg,
						ConvMessage.State.RECEIVED_READ.ordinal())) {
					ConvMessage.State currentState = msg.getState();
					msg.setState(ConvMessage.State.RECEIVED_READ);
					if (currentState == ConvMessage.State.RECEIVED_READ) {
						break;
					}
				}
			}

			runOnUiThread(this);
		} else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type)) {
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg,
					ConvMessage.State.SENT_CONFIRMED.ordinal())) {
				msg.setState(ConvMessage.State.SENT_CONFIRMED);
				runOnUiThread(this);
			}
		} else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type)) {
			Pair<String, long[]> pair = (Pair<String, long[]>) object;

			long[] ids = (long[]) pair.second;
			// TODO we could keep a map of msgId -> conversation objects
			// somewhere to make this faster
			for (int i = 0; i < ids.length; i++) {
				ConvMessage msg = findMessageById(ids[i]);
				if (Utils.shouldChangeMessageState(msg,
						ConvMessage.State.SENT_DELIVERED_READ.ordinal())) {
					// If the msisdn don't match we simply return
					if (!msg.getMsisdn().equals(pair.first)) {
						return;
					}
					msg.setState(ConvMessage.State.SENT_DELIVERED_READ);
				}
			}
			runOnUiThread(this);
		} else if (HikePubSub.MESSAGE_DELIVERED.equals(type)) {
			Pair<String, Long> pair = (Pair<String, Long>) object;

			long msgId = pair.second;
			ConvMessage msg = findMessageById(msgId);
			if (Utils.shouldChangeMessageState(msg,
					ConvMessage.State.SENT_DELIVERED.ordinal())) {
				// If the msisdn don't match we simply return
				if (!msg.getMsisdn().equals(pair.first)) {
					return;
				}
				msg.setState(ConvMessage.State.SENT_DELIVERED);
				runOnUiThread(this);
			}
		} else if (HikePubSub.MESSAGE_FAILED.equals(type)) {
			long msgId = ((Long) object).longValue();
			ConvMessage msg = findMessageById(msgId);
			if (msg != null) {
				msg.setState(ConvMessage.State.SENT_FAILED);
				runOnUiThread(this);
			}
		} else if (HikePubSub.ICON_CHANGED.equals(type)) {
			/* an icon changed, so update the view */
			runOnUiThread(this);
		} else if (HikePubSub.GROUP_NAME_CHANGED.equals(type)) {
			String groupId = (String) object;
			HikeConversationsDatabase db = HikeConversationsDatabase
					.getInstance();
			final String groupName = db.getGroupName(groupId);

			Conversation conv = mConversationsByMSISDN.get(groupId);
			if (conv == null) {
				return;
			}
			conv.setContactName(groupName);

			runOnUiThread(this);
		} else if (HikePubSub.UPDATE_AVAILABLE.equals(type)) {
			final int updateType = (Integer) object;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showUpdatePopup(updateType);
				}
			});
		} else if (HikePubSub.CONTACT_ADDED.equals(type)) {
			ContactInfo contactInfo = (ContactInfo) object;

			if (contactInfo == null) {
				return;
			}

			Conversation conversation = this.mConversationsByMSISDN
					.get(contactInfo.getMsisdn());
			if (conversation != null) {
				conversation.setContactName(contactInfo.getName());
				runOnUiThread(this);
			}
		} else if (HikePubSub.TYPING_CONVERSATION.equals(type)) {
			String msisdn = (String) object;
			toggleTypingNotification(true, msisdn);
		} else if (HikePubSub.END_TYPING_CONVERSATION.equals(type)) {
			toggleTypingNotification(false, (String) object);
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)
				|| HikePubSub.TIMELINE_UPDATE_RECIEVED.equals(type)) {
			if (HikePubSub.FAVORITE_TOGGLED.equals(type)) {
				final Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
				if (favoriteToggle.second != FavoriteType.REQUEST_RECEIVED) {
					return;
				}
			} else {
				StatusMessage statusMessage = (StatusMessage) object;
				/*
				 * We don't show a notification for the user's own statuses
				 */
				if (userMsisdn.equals(statusMessage.getMsisdn())) {
					return;
				}
			}
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					setNotificationCounter(++notificationCount);
				}
			});
		} else if (HikePubSub.RESET_NOTIFICATION_COUNTER.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					setNotificationCounter(0);
				}
			});
		} else if (HikePubSub.DECREMENT_NOTIFICATION_COUNTER.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					setNotificationCounter(--notificationCount);
				}
			});
		} else if (HikePubSub.DRAWER_ANIMATION_COMPLETE.equals(type)) {
			if (!refreshAfterAnimation) {
				return;
			}
			refreshAfterAnimation = false;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					showReceivedMessages(receivedMsgWhileAnimating);
					mAdapter.drawerAnimationComplete();
				}
			});
		}
	}

	private void addMessage(Conversation conv, ConvMessage convMessage) {
		if (!mConversationsAdded.contains(conv.getMsisdn())) {
			mConversationsAdded.add(conv.getMsisdn());
			mAdapter.add(conv);
		}

		conv.addMessage(convMessage);
		Log.d("MessagesList", "new message is " + convMessage);
		mAdapter.sort(mConversationsComparator);

		if (messageRefreshHandler == null) {
			messageRefreshHandler = new Handler();
		}
	}

	private void showReceivedMessages(
			final List<Pair<Conversation, ConvMessage>> convMessageList) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (Pair<Conversation, ConvMessage> conversationConvMessagePair : convMessageList) {
					addMessage(conversationConvMessagePair.first,
							conversationConvMessagePair.second);
				}
				messageRefreshHandler.removeCallbacks(MessagesList.this);
				messageRefreshHandler.postDelayed(MessagesList.this, 100);
				receivedMsgWhileAnimating.clear();
			}
		});
	}

	private void setNotificationCounter(int notificationCount) {
		if (notificationCount > 0) {
			notificationCounter.setBackgroundResource(R.drawable.notification);
			notificationCounter.setText(Integer.toString(notificationCount));
		} else {
			notificationCounter
					.setBackgroundResource(R.drawable.no_notification);
			notificationCounter.setText("");
		}
		this.notificationCount = notificationCount;
	}

	private void toggleTypingNotification(boolean isTyping, String msisdn) {
		if (mConversationsByMSISDN == null) {
			return;
		}
		Conversation conversation = mConversationsByMSISDN.get(msisdn);
		if (conversation == null) {
			Log.d(getClass().getSimpleName(), "Conversation Does not exist");
			return;
		}
		List<ConvMessage> messageList = conversation.getMessages();
		if (messageList.isEmpty()) {
			Log.d(getClass().getSimpleName(), "Conversation is empty");
			return;
		}
		if (isTyping) {
			ConvMessage message = messageList.get(messageList.size() - 1);
			if (!HikeConstants.IS_TYPING.equals(message.getMessage())
					&& message.getMsgID() != -1
					&& message.getMappedMsgID() != -1) {
				// Setting the msg id and mapped msg id as -1 to identify that
				// this is an "is typing..." message.
				ConvMessage convMessage = new ConvMessage(
						HikeConstants.IS_TYPING, msisdn,
						message.getTimestamp(),
						ConvMessage.State.RECEIVED_UNREAD, -1, -1);
				messageList.add(convMessage);
			}
		} else {
			ConvMessage message = messageList.get(messageList.size() - 1);
			if (HikeConstants.IS_TYPING.equals(message.getMessage())
					&& message.getMsgID() == -1
					&& message.getMappedMsgID() == -1) {
				messageList.remove(message);
			}
		}
		runOnUiThread(this);
	}

	ConvMessage findMessageById(long msgId) {
		int count = mAdapter.getCount();
		for (int i = 0; i < count; ++i) {
			Conversation conversation = mAdapter.getItem(i);
			if (conversation == null) {
				continue;
			}
			List<ConvMessage> messages = conversation.getMessages();
			if (messages.isEmpty()) {
				continue;
			}

			ConvMessage message = messages.get(messages.size() - 1);
			if (message.getMsgID() == msgId) {
				return message;
			}
		}

		return null;
	}

	public void run() {
		if (mAdapter == null) {
			return;
		}
		mAdapter.notifyDataSetChanged();
		// notifyDataSetChanged sets notifyonChange to true but we want it to
		// always be false
		mAdapter.setNotifyOnChange(false);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			Conversation[] convs = new Conversation[mAdapter.getCount()];
			for (int i = 0; i < convs.length; i++) {
				convs[i] = mAdapter.getItem(i);
				if ((convs[i] instanceof GroupConversation)) {
					HikeMessengerApp
							.getPubSub()
							.publish(
									HikePubSub.MQTT_PUBLISH,
									convs[i].serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
				}
			}
			DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
			task.execute(convs);
			break;
		default:
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		Conversation conv = (Conversation) adapterView
				.getItemAtPosition(position);
		if (conv == null) {
			return;
		}
		Intent intent = createIntentForConversation(conv);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_in_right_noalpha,
				R.anim.slide_out_left_noalpha);
	}

	private void hideToolTip() {
		if (mToolTip.getVisibility() == View.VISIBLE) {
			Animation alphaOut = AnimationUtils.loadAnimation(
					MessagesList.this, android.R.anim.fade_out);
			alphaOut.setDuration(200);
			mToolTip.setAnimation(alphaOut);
			mToolTip.setVisibility(View.INVISIBLE);
		}
	}

	private void setToolTipDismissed() {
		hideToolTip();

		Editor editor = accountPrefs.edit();
		editor.putBoolean(HikeMessengerApp.MESSAGES_LIST_TOOLTIP_DISMISSED,
				true);
		editor.commit();
	}

	public void onToolTipClosed(View v) {
		if (updateToolTipParent != null
				&& updateToolTipParent.getVisibility() == View.VISIBLE) {
			Utils.logEvent(MessagesList.this,
					HikeConstants.LogEvent.HOME_UPDATE_TOOL_TIP_CLOSED);
			Editor editor = accountPrefs.edit();
			editor.putBoolean(HikeConstants.Extras.SHOW_UPDATE_TOOL_TIP, false);
			// Doing this so that we show this tip after the user has opened the
			// home screen a few times.
			editor.remove(HikeMessengerApp.NUM_TIMES_HOME_SCREEN);
			editor.commit();
			hideToolTip();
			return;
		} else if (groupChatToolTipParent != null
				&& groupChatToolTipParent.getVisibility() == View.VISIBLE) {
			Editor editor = accountPrefs.edit();
			editor.putBoolean(HikeMessengerApp.SHOW_GROUP_CHAT_TOOL_TIP, false);
			editor.commit();
			hideToolTip();
			return;
		}
		Utils.logEvent(MessagesList.this,
				HikeConstants.LogEvent.HOME_TOOL_TIP_CLOSED);
		setToolTipDismissed();
	}

	private void updateApp(int updateType) {
		if (TextUtils.isEmpty(this.accountPrefs.getString(
				HikeConstants.Extras.UPDATE_URL, ""))) {
			Intent marketIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id=" + getPackageName()));
			marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
					| Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			try {
				startActivity(marketIntent);
			} catch (ActivityNotFoundException e) {
				Log.e(MessagesList.class.getSimpleName(),
						"Unable to open market");
			}
		} else {
			if (updateType == HikeConstants.NORMAL_UPDATE) {
				updateAlert.dismiss();
			} else {
				updateAlertOkBtn.setText("Downloading...");
				updateAlertOkBtn.setEnabled(false);
			}
			// In app update!
			DownloadAndInstallUpdateAsyncTask downloadAndInstallUpdateAsyncTask = new DownloadAndInstallUpdateAsyncTask(
					this, accountPrefs.getString(
							HikeConstants.Extras.UPDATE_URL, ""));
			downloadAndInstallUpdateAsyncTask.execute();
		}
	}

	public void onToolTipClicked(View v) {
		if (groupChatToolTipParent != null
				&& groupChatToolTipParent.getVisibility() == View.VISIBLE) {
			setToolTipDismissed();
			openOptionsMenu();
			return;
		}
	}

	private void leaveGroup(Conversation conv) {
		if (conv == null) {
			Log.d(getClass().getSimpleName(), "Invalid conversation");
			return;
		}
		HikeMessengerApp
				.getPubSub()
				.publish(
						HikePubSub.MQTT_PUBLISH,
						conv.serialize(HikeConstants.MqttMessageTypes.GROUP_CHAT_LEAVE));
		DeleteConversationsAsyncTask task = new DeleteConversationsAsyncTask();
		task.execute(conv);
	}

	private void showUpdatePopup(final int updateType) {
		if (updateType == HikeConstants.NO_UPDATE) {
			return;
		}

		if (updateType == HikeConstants.NORMAL_UPDATE) {
			// Here we check if the user cancelled the update popup for this
			// version earlier
			String updateToIgnore = accountPrefs.getString(
					HikeConstants.Extras.UPDATE_TO_IGNORE, "");
			if (!TextUtils.isEmpty(updateToIgnore)
					&& updateToIgnore.equals(accountPrefs.getString(
							HikeConstants.Extras.LATEST_VERSION, ""))) {
				return;
			}
		}

		// If we are already showing an update we don't need to do anything else
		if (updateAlert != null && updateAlert.isShowing()) {
			return;
		}

		updateAlert = new Dialog(MessagesList.this, R.style.Theme_CustomDialog);
		updateAlert.setContentView(R.layout.alert_box);

		((ImageView) updateAlert.findViewById(R.id.alert_image))
				.setVisibility(View.GONE);

		int padding = (int) (10 * Utils.densityMultiplier);

		TextView updateText = ((TextView) updateAlert
				.findViewById(R.id.alert_text));
		TextView updateTitle = (TextView) updateAlert
				.findViewById(R.id.alert_title);

		updateText.setPadding(padding, 0, padding, padding);
		updateText.setGravity(Gravity.CENTER);
		updateText
				.setText(updateType == HikeConstants.CRITICAL_UPDATE ? R.string.critical_update
						: R.string.normal_update);

		updateTitle.setPadding(padding, padding, padding, padding);
		updateTitle.setGravity(Gravity.CENTER);
		updateTitle
				.setText(updateType == HikeConstants.CRITICAL_UPDATE ? R.string.critical_update_head
						: R.string.normal_update_head);

		Button cancelBtn = null;
		if (updateType == HikeConstants.CRITICAL_UPDATE) {
			((Button) updateAlert.findViewById(R.id.alert_ok_btn))
					.setVisibility(View.GONE);
			((Button) updateAlert.findViewById(R.id.alert_cancel_btn))
					.setVisibility(View.GONE);
			(updateAlert.findViewById(R.id.btn_divider))
					.setVisibility(View.GONE);

			updateAlertOkBtn = (Button) updateAlert
					.findViewById(R.id.alert_center_btn);
			updateAlertOkBtn.setVisibility(View.VISIBLE);
		} else {
			updateAlertOkBtn = (Button) updateAlert
					.findViewById(R.id.alert_ok_btn);
			cancelBtn = (Button) updateAlert
					.findViewById(R.id.alert_cancel_btn);
			cancelBtn.setText(R.string.cancel);
		}
		updateAlertOkBtn.setText(R.string.update_app);

		updateAlert.setCancelable(true);

		updateAlertOkBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateApp(updateType);
			}
		});

		if (cancelBtn != null) {
			cancelBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					updateAlert.cancel();
				}
			});
		}

		updateAlert.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (updateType == HikeConstants.CRITICAL_UPDATE) {
					finish();
				} else {
					Editor editor = accountPrefs.edit();
					editor.putString(HikeConstants.Extras.UPDATE_TO_IGNORE,
							accountPrefs.getString(
									HikeConstants.Extras.LATEST_VERSION, ""));
					editor.commit();
				}
			}
		});

		updateAlert.show();
	}

	public void updateFailed() {
		if (updateAlertOkBtn != null) {
			updateAlertOkBtn.setText(R.string.update_app);
			updateAlertOkBtn.setEnabled(true);
		}
	}

	public void onOpenTimelineClick(View v) {
		Intent intent = new Intent(this, CentralTimeline.class);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_noalpha, R.anim.no_animation);
	}

	public void onGroupChatClick(View v) {
		Intent intent = new Intent(this, ChatThread.class);
		intent.putExtra(HikeConstants.Extras.GROUP_CHAT, true);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_noalpha, R.anim.no_animation);
	}

	public void onOneToOneClick(View v) {
		Utils.logEvent(MessagesList.this, HikeConstants.LogEvent.COMPOSE_BUTTON);
		Intent intent = new Intent(this, ChatThread.class);
		intent.putExtra(HikeConstants.Extras.EDIT, true);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_noalpha, R.anim.no_animation);
	}

	public void onStatusClick(View v) {
		Intent intent = new Intent(this, StatusUpdate.class);
		intent.putExtra(HikeConstants.Extras.FROM_CONVERSATIONS_SCREEN, true);
		startActivity(intent);
		overridePendingTransition(R.anim.slide_up_noalpha, R.anim.no_animation);
	}
}
