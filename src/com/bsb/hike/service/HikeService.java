package com.bsb.hike.service;

import java.util.Calendar;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.service.HikeMqttManager.MQTTConnectionStatus;
import com.bsb.hike.tasks.CheckForUpdateTask;
import com.bsb.hike.tasks.SyncContactExtraInfo;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.Utils;
import com.google.android.gcm.GCMRegistrar;

public class HikeService extends Service {

	public class ContactsChanged implements Runnable {

		private Context context;

		public ContactsChanged(Context ctx) {
			this.context = ctx;
		}

		@Override
		public void run() {
			Log.d("ContactsChanged", "calling syncUpdates");
			ContactUtils.syncUpdates(this.context);
		}
	}

	private class PingRunnable implements Runnable {
		@Override
		public void run() {
			HikeService.this.mMqttManager.ping();
		}
	}

	public static final int MSG_APP_CONNECTED = 1;

	public static final int MSG_APP_DISCONNECTED = 2;

	public static final int MSG_APP_TOKEN_CREATED = 3;

	public static final int MSG_APP_PUBLISH = 4;

	public static final int MSG_APP_MESSAGE_STATUS = 5;

	public static final int MSG_APP_CONN_STATUS = 6;

	public static final int MSG_APP_INVALID_TOKEN = 7;

	protected Messenger mApp;

	class IncomingHandler extends Handler {
		public IncomingHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case MSG_APP_CONNECTED:
					if (mMqttManager.isConnected()) {
						mMqttManager.subscribeToUIEvents();
					}
					handleStart();

					mApp = msg.replyTo;
					broadcastServiceStatus(mMqttManager.getConnectionStatus());
					break;
				case MSG_APP_DISCONNECTED:
					mMqttManager.unsubscribeFromUIEvents();
					mApp = null;
					break;
				case MSG_APP_TOKEN_CREATED:
					Log.d("HikeService", "received MSG_APP_TOKEN_CREATED");
					handleStart();
					break;
				case MSG_APP_PUBLISH:
					Bundle bundle = msg.getData();
					String message = bundle.getString(HikeConstants.MESSAGE);
					long msgId = bundle.getLong(HikeConstants.MESSAGE_ID, -1);
					mMqttManager.send(new HikePacket(message.getBytes(), msgId,
							System.currentTimeMillis()), msg.arg1);
				}
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "Exception", e);
			}
		}
	}

	private Messenger mMessenger;

	/************************************************************************/
	/* CONSTANTS */
	/************************************************************************/

	// constant used internally to schedule the next ping event
	public static final String MQTT_PING_ACTION = "com.bsb.hike.PING";

	// constant used internally to send user stats
	public static final String MQTT_USER_STATS_SEND_ACTION = "com.bsb.hike.USER_STATS";

	public static final String MQTT_CONTACT_SYNC_ACTION = "com.bsb.hike.CONTACT_SYNC";

	// constant used internally to check for updates
	public static final String UPDATE_CHECK_ACTION = "com.bsb.hike.UPDATE_CHECK";

	// used to register to GCM
	public static final String REGISTER_TO_GCM_ACTION = "com.bsb.hike.REGISTER_GCM";

	// constants used by status bar notifications
	public static final int MQTT_NOTIFICATION_ONGOING = 1;

	public static final int MQTT_NOTIFICATION_UPDATE = 2;

	/************************************************************************/
	/* VARIABLES - other local variables */
	/************************************************************************/

	// receiver that notifies change in network type.
	private NetworkTypeChangeIntentReceiver networkTypeChangeIntentReceiver;

	// receiver that notifies the Service when the phone gets data connection
	private NetworkConnectionIntentReceiver netConnReceiver;

	// receiver that notifies the Service when the user changes data use
	// preferences
	private BackgroundDataChangeIntentReceiver dataEnabledReceiver;

	// receiver that wakes the Service up when it's time to ping the server
	private PingSender pingSender;

	// receiver that sends the user stats once every 24 hours
	private UserStatsSender userStatsSender;

	// receiver that triggers a contact sync
	private ManualContactSyncTrigger manualContactSyncTrigger;

	// receiver that triggers a check for updates
	private UpdateCheckTrigger updateCheckTrigger;

	private RegisterToGCMTrigger registerToGCMTrigger;

	private HikeMqttManager mMqttManager;
	private ContactListChangeIntentReceiver contactsReceived;
	private Handler mHandler;

	private Handler mContactsChangedHandler;
	private Runnable mContactsChanged;

	private PingRunnable pingRunnable;

	private Looper mContactHandlerLooper;

	private Looper mMqttHandlerLooper;

	/************************************************************************/
	/* METHODS - core Service lifecycle methods */
	/************************************************************************/

	// see http://developer.android.com/guide/topics/fundamentals.html#lcycles

	@Override
	public void onCreate() {
		super.onCreate();

		if (registerToGCMTrigger == null) {
			registerToGCMTrigger = new RegisterToGCMTrigger();
			registerReceiver(registerToGCMTrigger, new IntentFilter(
					REGISTER_TO_GCM_ACTION));
			sendBroadcast(new Intent(REGISTER_TO_GCM_ACTION));
		}

		Log.d("HikeService", "onCreate called");
		HandlerThread mqttHandlerThread = new HandlerThread("MQTTThread");
		mqttHandlerThread.start();
		mMqttHandlerLooper = mqttHandlerThread.getLooper();
		this.mHandler = new Handler(mMqttHandlerLooper);
		mMessenger = new Messenger(new IncomingHandler(mMqttHandlerLooper));

		// reset status variable to initial state
		mMqttManager = new HikeMqttManager(this, this.mHandler);

		// register to be notified whenever the user changes their preferences
		// relating to background data use - so that we can respect the current
		// preference
		dataEnabledReceiver = new BackgroundDataChangeIntentReceiver();
		registerReceiver(dataEnabledReceiver, new IntentFilter(
				ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));

		networkTypeChangeIntentReceiver = new NetworkTypeChangeIntentReceiver();
		registerReceiver(networkTypeChangeIntentReceiver, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));
		/*
		 * notify android that our service represents a user visible action, so
		 * it should not be killable. In order to do so, we need to show a
		 * notification so the user understands what's going on
		 */

		/*
		 * Remove this for now because a) it's irritating and b) it may be
		 * causing increased battery usage. Notification notification = new
		 * Notification(R.drawable.ic_contact_logo,
		 * getResources().getString(R.string.service_running_message),
		 * System.currentTimeMillis()); notification.flags |=
		 * Notification.FLAG_AUTO_CANCEL;
		 * 
		 * Intent notificationIntent = new Intent(this, MessagesList.class);
		 * PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		 * notificationIntent, 0); notification.setLatestEventInfo(this, "Hike",
		 * "Hike", contentIntent);
		 * startForeground(HikeNotification.HIKE_NOTIFICATION, notification);
		 */

		HandlerThread contactHandlerThread = new HandlerThread("");
		contactHandlerThread.start();
		mContactHandlerLooper = contactHandlerThread.getLooper();
		mContactsChangedHandler = new Handler(mContactHandlerLooper);
		mContactsChanged = new ContactsChanged(this);

		if (userStatsSender == null) {
			userStatsSender = new UserStatsSender();
			registerReceiver(userStatsSender, new IntentFilter(
					MQTT_USER_STATS_SEND_ACTION));
			scheduleNextUserStatsSending();
		}

		if (updateCheckTrigger == null) {
			updateCheckTrigger = new UpdateCheckTrigger();
			registerReceiver(updateCheckTrigger, new IntentFilter(
					UPDATE_CHECK_ACTION));
			scheduleNextUpdateCheck();
		}

		/*
		 * register with the Contact list to get an update whenever the phone
		 * book changes. Use the application thread for the intent receiver, the
		 * IntentReceiver will take care of running the event on a different
		 * thread
		 */
		contactsReceived = new ContactListChangeIntentReceiver(new Handler());
		/* listen for changes in the addressbook */
		getContentResolver().registerContentObserver(
				ContactsContract.Contacts.CONTENT_URI, true, contactsReceived);
		if (manualContactSyncTrigger == null) {
			manualContactSyncTrigger = new ManualContactSyncTrigger();
			registerReceiver(manualContactSyncTrigger, new IntentFilter(
					MQTT_CONTACT_SYNC_ACTION));
			/*
			 * Forcing a sync first time service is created to fix bug where if
			 * the app is force stopped no contacts are synced if they are added
			 * when the app is in force stopped state
			 */
			getContentResolver().notifyChange(
					ContactsContract.Contacts.CONTENT_URI, null);
		}

		if (!getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE).getBoolean(
				HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, false)) {
			Log.d(getClass().getSimpleName(), "SYNCING");
			SyncContactExtraInfo syncContactExtraInfo = new SyncContactExtraInfo();
			syncContactExtraInfo.execute();
		}
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId) {
		asyncStart();
		Log.d("HikeService", "Intent is " + intent);
		if (intent != null && intent.hasExtra(HikeConstants.Extras.SMS_MESSAGE)) {
			String s = intent.getExtras().getString(
					HikeConstants.Extras.SMS_MESSAGE);
			try {
				JSONObject msg = new JSONObject(s);
				Log.d("HikeService",
						"Intent contained SMS message "
								+ msg.getString(HikeConstants.TYPE));
				MqttMessagesManager mgr = MqttMessagesManager.getInstance(this);
				mgr.saveMqttMessage(msg);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		// return START_NOT_STICKY - we want this Service to be left running
		// unless explicitly stopped, and it's process is killed, we want it to
		// be restarted
		return START_STICKY;
	}

	private void asyncStart() {
		/* ensure that all mqtt activity is done on the mqtt thread */
		this.mHandler.postAtFrontOfQueue(new Runnable() {
			@Override
			public void run() {
				handleStart();
			}
		});
	}

	synchronized void handleStart() {
		// before we start - check for a couple of reasons why we should stop

		Log.d("HikeService", "handlestart called");
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getBackgroundDataSetting() == false) // respect the user's
		// request not to use data!
		{
			// user has disabled background data
			mMqttManager
					.setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_DATADISABLED);

			// we have a listener running that will notify us when this
			// preference changes, and will call handleStart again when it
			// is - letting us pick up where we leave off now
			return;
		}

		// if the Service was already running and we're already connected - we
		// don't need to do anything
		if (this.haveCredentials() && !this.mMqttManager.isConnected()) {
			this.mMqttManager.connect();

		}

		// changes to the phone's network - such as bouncing between WiFi
		// and mobile data networks - can break the MQTT connection
		// the MQTT connectionLost can be a bit slow to notice, so we use
		// Android's inbuilt notification system to be informed of
		// network changes - so we can reconnect immediately, without
		// haing to wait for the MQTT timeout
		if (netConnReceiver == null) {
			netConnReceiver = new NetworkConnectionIntentReceiver();
			registerReceiver(netConnReceiver, new IntentFilter(
					ConnectivityManager.CONNECTIVITY_ACTION));

		}

		// creates the intents that are used to wake up the phone when it is
		// time to ping the server
		if (pingSender == null) {
			pingSender = new PingSender();
			pingRunnable = new PingRunnable();
			registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
		}
	}

	private boolean haveCredentials() {
		SharedPreferences settings = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		return !TextUtils.isEmpty(settings.getString(
				HikeMessengerApp.TOKEN_SETTING, null));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// disconnect immediately
		this.mMqttManager.disconnectFromBroker(false);
		this.mMqttManager.finish();
		this.mMqttManager = null;

		// inform the app that the app has successfully disconnected
		Log.i("HikeService", "onDestroy.  Shutting down service");

		unregisterDataChangeReceivers();

		if (pingSender != null) {
			unregisterReceiver(pingSender);
			pingSender = null;
		}

		if (contactsReceived != null) {
			getContentResolver().unregisterContentObserver(contactsReceived);
			contactsReceived = null;
		}

		if (mMqttHandlerLooper != null) {
			mMqttHandlerLooper.quit();
		}

		if (mContactHandlerLooper != null) {
			mContactHandlerLooper.quit();
		}

		if (userStatsSender != null) {
			unregisterReceiver(userStatsSender);
			userStatsSender = null;
		}

		if (manualContactSyncTrigger != null) {
			unregisterReceiver(manualContactSyncTrigger);
			manualContactSyncTrigger = null;
		}

		if (updateCheckTrigger != null) {
			unregisterReceiver(updateCheckTrigger);
			updateCheckTrigger = null;
		}

		if (registerToGCMTrigger != null) {
			unregisterReceiver(registerToGCMTrigger);
			registerToGCMTrigger = null;
		}
	}

	public void unregisterDataChangeReceivers() {
		if (netConnReceiver != null) {
			unregisterReceiver(netConnReceiver);
			netConnReceiver = null;
		}
		if (dataEnabledReceiver != null) {
			unregisterReceiver(dataEnabledReceiver);
			dataEnabledReceiver = null;
		}
		if (networkTypeChangeIntentReceiver != null) {
			unregisterReceiver(networkTypeChangeIntentReceiver);
			networkTypeChangeIntentReceiver = null;
		}
	}

	/************************************************************************/
	/* METHODS - broadcasts and notifications */
	/************************************************************************/

	// methods used to notify the Activity UI of something that has happened
	// so that it can be updated to reflect status and the data received
	// from the server

	public void broadcastServiceStatus(
			HikeMqttManager.MQTTConnectionStatus status) {
		Log.d("HikeService", "broadcastServiceStatus " + status);

		if (status == HikeMqttManager.MQTTConnectionStatus.CONNECTED) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(HikeConstants.HIKE_SYSTEM_NOTIFICATION);
		}

		if (mApp == null) {
			return;
		}

		try {
			Message msg = Message.obtain();
			msg.what = MSG_APP_CONN_STATUS;
			msg.arg1 = status.ordinal();
			mApp.send(msg);
		} catch (RemoteException e) {
			// client is dead :(
			mApp = null;
			mMqttManager.unsubscribeFromUIEvents();
			Log.e("HikeService",
					"Can't send connection status to the application");
		}

	}

	// methods used to notify the user of what has happened for times when
	// the app Activity UI isn't running

	public void notifyUser(String alert, String title, String body) {
		Log.w("HikeService", alert + ":" + "body");
	}

	/************************************************************************/
	/* METHODS - binding that allows access from the Activity */
	/************************************************************************/

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	/************************************************************************/
	/* METHODS - wrappers for some of the MQTT methods that we use */
	/************************************************************************/

	private class ContactListChangeIntentReceiver extends ContentObserver {

		public ContactListChangeIntentReceiver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			HikeService.this.mContactsChangedHandler
					.removeCallbacks(mContactsChanged);
			HikeService.this.mContactsChangedHandler.postDelayed(
					mContactsChanged, HikeConstants.CONTACT_UPDATE_TIMEOUT);
			Log.d("ContactListChangeIntentReceiver", "onChange called");
			// Schedule the next manual sync to happed 24 hours from now.
			scheduleNextManualContactSync();
		}
	}

	private class BackgroundDataChangeIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			// we protect against the phone switching off while we're doing this
			// by requesting a wake lock - we request the minimum possible wake
			// lock - just enough to keep the CPU running until we've finished
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			WakeLock wl = pm
					.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
			try {
				wl.acquire();

				ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				if (cm.getBackgroundDataSetting()) {
					Log.d("HikeService", "User has enabled data");
					// user has allowed background data - we start again -
					// picking
					// up where we left off in handleStart before
					asyncStart();
				} else {
					Log.w("HikeService", "User has disabled data");
					// user has disabled background data
					mMqttManager
							.setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_DATADISABLED);

					// disconnect from the broker
					mMqttManager.disconnectFromBroker(false);
				}

				// we're finished - if the phone is switched off, it's okay for
				// the CPU
				// to sleep now
			} finally {
				wl.release();
			}
		}
	}

	/*
	 * Called in response to a change in network connection - after losing a
	 * connection to the server, this allows us to wait until we have a usable
	 * data connection again
	 */
	private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			// we protect against the phone switching off while we're doing this
			// by requesting a wake lock - we request the minimum possible wake
			// lock - just enough to keep the CPU running until we've finished
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			WakeLock wl = pm
					.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
			wl.acquire();

			if (Utils.isUserOnline(HikeService.this)) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						HikeService.this.mMqttManager.ping();
					}
				});
			}

			// we're finished - if the phone is switched off, it's okay for the
			// CPU
			// to sleep now
			wl.release();
		}
	}

	private class NetworkTypeChangeIntentReceiver extends BroadcastReceiver {

		boolean wasWifiOnLastTime = false;

		@Override
		public void onReceive(Context context, Intent intent) {
			boolean isWifiOn = Utils.isWifiOn(getApplicationContext());
			if (wasWifiOnLastTime == isWifiOn) {
				Log.d("SSL", "Same connection type as before. Wifi? "
						+ isWifiOn);
				return;
			}
			Log.d("SSL", "Different connection type. Wifi? " + isWifiOn);
			wasWifiOnLastTime = isWifiOn;
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.SWITCHED_DATA_CONNECTION, isWifiOn);
		}
	}

	public void scheduleNextPing() {
		scheduleNextPing((int) (HikeConstants.KEEP_ALIVE * 0.9));
	}

	/*
	 * Schedule the next time that you want the phone to wake up and ping the
	 * message broker server
	 */
	public void scheduleNextPing(int timeout) {
		// When the phone is off, the CPU may be stopped. This means that our
		// code may stop running.
		// When connecting to the message broker, we specify a 'keep alive'
		// period - a period after which, if the client has not contacted
		// the server, even if just with a ping, the connection is considered
		// broken.
		// To make sure the CPU is woken at least once during each keep alive
		// period, we schedule a wake up to manually ping the server
		// thereby keeping the long-running connection open
		// Normally when using this Java MQTT client library, this ping would be
		// handled for us.
		// Note that this may be called multiple times before the next scheduled
		// ping has fired. This is good - the previously scheduled one will be
		// cancelled in favour of this one.
		// This means if something else happens during the keep alive period,
		// (e.g. we receive an MQTT message), then we start a new keep alive
		// period, postponing the next ping.

		PendingIntent pendingIntent = PendingIntent
				.getBroadcast(this, 0, new Intent(MQTT_PING_ACTION),
						PendingIntent.FLAG_UPDATE_CURRENT);

		// in case it takes us a little while to do this, we try and do it
		// shortly before the keep alive period expires
		// it means we're pinging slightly more frequently than necessary
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.SECOND, timeout); // comes from
		// PushMqttManager.KEEPALIVE

		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
				pendingIntent);
	}

	/*
	 * Used to implement a keep-alive protocol at this Service level - it sends
	 * a PING message to the server, then schedules another ping after an
	 * interval defined by keepAliveSeconds
	 */
	public class PingSender extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			mHandler.post(pingRunnable);

			// start the next keep alive period
			scheduleNextPing();

		}
	}

	/*
	 * Used for sending the user stats to the server once every 24 hours.
	 */
	private class UserStatsSender extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			sendUserStats();
		}
	}

	private class ManualContactSyncTrigger extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			getContentResolver().notifyChange(
					ContactsContract.Contacts.CONTENT_URI, null);
		}
	}

	private class RegisterToGCMTrigger extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(getClass().getSimpleName(), "Registering for GCM");
			GCMRegistrar.checkDevice(HikeService.this);
			GCMRegistrar.checkManifest(HikeService.this);
			final String regId = GCMRegistrar
					.getRegistrationId(HikeService.this);
			if ("".equals(regId)) {
				GCMRegistrar.register(HikeService.this,
						HikeConstants.APP_PUSH_ID);
			} else {
				Log.d(getClass().getSimpleName(), "Already registered");
			}
		}
	}

	private void scheduleNextManualContactSync() {
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				new Intent(MQTT_CONTACT_SYNC_ACTION),
				PendingIntent.FLAG_UPDATE_CURRENT);

		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.HOUR, 24);

		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		// Cancel any pending alarms with this pending intent
		aMgr.cancel(pendingIntent);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
				pendingIntent);
	}

	private void sendUserStats() {
		JSONObject obj = Utils.getDeviceStats(getApplicationContext());
		if (obj != null) {
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, obj);
		}
		scheduleNextUserStatsSending();
	}

	private void scheduleNextUserStatsSending() {
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				new Intent(MQTT_USER_STATS_SEND_ACTION),
				PendingIntent.FLAG_UPDATE_CURRENT);

		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.HOUR, 12);

		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
				pendingIntent);
	}

	private class UpdateCheckTrigger extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			CheckForUpdateTask checkForUpdateTask = new CheckForUpdateTask(
					HikeService.this);
			checkForUpdateTask.execute();
		}

	}

	public void scheduleNextUpdateCheck() {
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				new Intent(UPDATE_CHECK_ACTION),
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Randomizing the time when we will poll the server for an update
		Random random = new Random();

		Calendar wakeUpTime = Calendar.getInstance();
		if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE).getBoolean(HikeMessengerApp.PRODUCTION, true)) {
			int hour = random.nextInt(48) + 1;
			wakeUpTime.add(Calendar.HOUR, hour);
		} else {
			int min = random.nextInt(2) + 1;
			wakeUpTime.add(Calendar.MINUTE, min);
		}

		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		// Cancel any pending alarms with this pending intent
		aMgr.cancel(pendingIntent);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(),
				pendingIntent);
	}

	public boolean appIsConnected() {
		return mApp != null;
	}

	public boolean sendInvalidToken() {
		if (mApp == null) {
			Log.d("HikeService", "no app");
			return false;
		}

		try {
			Message msg = Message.obtain(null, MSG_APP_INVALID_TOKEN);
			mApp.send(msg);
		} catch (RemoteException e) {
			// client is dead :(
			mApp = null;
			mMqttManager.unsubscribeFromUIEvents();
			Log.e("HikeService", "Can't send message to the application");
			return false;
		}
		return true;
	}

	public boolean sendMessageStatus(Long msgId, boolean sent) {
		if (mApp == null) {
			Log.d("HikeService", "no app");
			return false;
		}

		try {
			Message msg = Message.obtain(null, MSG_APP_MESSAGE_STATUS);
			msg.obj = msgId;
			msg.arg1 = sent ? 1 : 0;
			mApp.send(msg);
		} catch (RemoteException e) {
			// client is dead :(
			mApp = null;
			mMqttManager.unsubscribeFromUIEvents();
			Log.e("HikeService", "Can't send message to the application");
			return false;
		}
		return true;
	}
}
