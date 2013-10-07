package com.bsb.hike.service;

import java.util.Calendar;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
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

import com.bsb.hike.GCMIntentService;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.service.HikeMqttManager.MQTTConnectionStatus;
import com.bsb.hike.tasks.CheckForUpdateTask;
import com.bsb.hike.tasks.HikeHTTPTask;
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

	public static final String MQTT_CONTACT_SYNC_ACTION = "com.bsb.hike.CONTACT_SYNC";

	// used to register to GCM
	public static final String REGISTER_TO_GCM_ACTION = "com.bsb.hike.REGISTER_GCM";

	// used to send GCM registeration id to server
	public static final String SEND_TO_SERVER_ACTION = "com.bsb.hike.SEND_TO_SERVER";

	// used to send GCM registeration id to server
	public static final String SEND_DEV_DETAILS_TO_SERVER_ACTION = "com.bsb.hike.SEND_DEV_DETAILS_TO_SERVER";

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

	// receiver that triggers a contact sync
	private ManualContactSyncTrigger manualContactSyncTrigger;

	private RegisterToGCMTrigger registerToGCMTrigger;

	private SendGCMIdToServerTrigger sendGCMIdToServerTrigger;

	private PostDeviceDetails postDeviceDetails;

	private ScreenOnReceiver screenOnReceiver;

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
		if (sendGCMIdToServerTrigger == null) {
			sendGCMIdToServerTrigger = new SendGCMIdToServerTrigger();
			registerReceiver(sendGCMIdToServerTrigger, new IntentFilter(
					SEND_TO_SERVER_ACTION));
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

		if (postDeviceDetails == null) {
			postDeviceDetails = new PostDeviceDetails();
			registerReceiver(postDeviceDetails, new IntentFilter(
					SEND_DEV_DETAILS_TO_SERVER_ACTION));
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
		/*
		 * listen for changes in sim contacts
		 */
		getContentResolver().registerContentObserver(
				Uri.parse("content://icc/adn"), true, contactsReceived);
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
			Utils.executeAsyncTask(syncContactExtraInfo);
		}

		if (screenOnReceiver == null) {
			screenOnReceiver = new ScreenOnReceiver();
			registerReceiver(screenOnReceiver, new IntentFilter(
					Intent.ACTION_SCREEN_ON));
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

		if (manualContactSyncTrigger != null) {
			unregisterReceiver(manualContactSyncTrigger);
			manualContactSyncTrigger = null;
		}

		if (registerToGCMTrigger != null) {
			unregisterReceiver(registerToGCMTrigger);
			registerToGCMTrigger = null;
		}

		if (sendGCMIdToServerTrigger != null) {
			unregisterReceiver(sendGCMIdToServerTrigger);
			sendGCMIdToServerTrigger = null;
		}

		if (postDeviceDetails != null) {
			unregisterReceiver(postDeviceDetails);
			postDeviceDetails = null;
		}

		if (screenOnReceiver != null) {
			unregisterReceiver(screenOnReceiver);
			screenOnReceiver = null;
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
			Log.d(getClass().getSimpleName(), "Contact content observer called");

			HikeService.this.mContactsChangedHandler
					.removeCallbacks(mContactsChanged);
			HikeService.this.mContactsChangedHandler.postDelayed(
					mContactsChanged, HikeConstants.CONTACT_UPDATE_TIMEOUT);
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
			boolean isWifiOn = Utils.switchSSLOn(getApplicationContext());
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

	private void postRunnableWithDelay(Runnable runnable, long delay) {
		mHandler.removeCallbacks(runnable);
		mHandler.postDelayed(runnable, delay);
	}

	private long getScheduleTime(long wakeUpTime) {
		return (wakeUpTime - System.currentTimeMillis());
	}

	/*
	 * Schedule the next time that you want the phone to wake up and ping the
	 * message broker server
	 */
	public void scheduleNextPing(int timeout) {
		Log.d(getClass().getSimpleName(), "Scheduling ping in " + timeout
				+ " seconds");

		postRunnableWithDelay(pingSenderRunnable, timeout * 1000);
	}

	private Runnable pingSenderRunnable = new Runnable() {

		@Override
		public void run() {
			sendBroadcast(new Intent(MQTT_PING_ACTION));
		}
	};

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
	};

	private class ManualContactSyncTrigger extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			getContentResolver().notifyChange(
					ContactsContract.Contacts.CONTENT_URI, null);
		}
	}

	/**
	 * Added this receiver to as a temporary fix for the issue where app
	 * disconnects and never connects again. Here everytime the user turn on the
	 * screen, we will check whether we are connected or not.
	 */
	private class ScreenOnReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!mMqttManager.isConnected()) {
				Log.d(getClass().getSimpleName(),
						"App was not connected, trying to reconnect");
				mMqttManager.connect();
			} else {
				Log.d(getClass().getSimpleName(), "App is connected!");
			}
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
				/*
				 * Since we are registering again, we should clear this
				 * preference
				 */
				Editor editor = getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
				editor.remove(HikeMessengerApp.GCM_ID_SENT);
				editor.commit();

				GCMRegistrar.register(HikeService.this,
						HikeConstants.APP_PUSH_ID);
			} else {
				sendBroadcast(new Intent(SEND_TO_SERVER_ACTION));
			}
		}
	}

	private class SendGCMIdToServerTrigger extends BroadcastReceiver {
		@Override
		public void onReceive(final Context context, Intent intent) {
			Log.d(getClass().getSimpleName(), "Sending GCM ID");
			final String regId = GCMRegistrar.getRegistrationId(context);
			if ("".equals(regId)) {
				sendBroadcast(new Intent(REGISTER_TO_GCM_ACTION));
				Log.d(getClass().getSimpleName(), "GCM id not found");
				return;
			}

			final SharedPreferences prefs = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
			if (prefs.getBoolean(HikeMessengerApp.GCM_ID_SENT, false)) {
				Log.d(getClass().getSimpleName(), "GCM id sent");
				return;
			}

			Log.d(getClass().getSimpleName(),
					"GCM id was not sent. Sending now");
			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
					"/account/device", RequestType.OTHER,
					new HikeHttpCallback() {
						public void onSuccess(JSONObject response) {
							Log.d(SendGCMIdToServerTrigger.this.getClass()
									.getSimpleName(), "Send successful");
							Editor editor = prefs.edit();
							editor.putBoolean(HikeMessengerApp.GCM_ID_SENT,
									true);
							editor.commit();
						}

						public void onFailure() {
							Log.d(SendGCMIdToServerTrigger.this.getClass()
									.getSimpleName(), "Send unsuccessful");
							scheduleNextSendToServerAction(true);
						}
					});
			JSONObject request = new JSONObject();
			try {
				request.put(GCMIntentService.DEV_TYPE, HikeConstants.ANDROID);
				request.put(GCMIntentService.DEV_TOKEN, regId);
			} catch (JSONException e) {
				Log.d(getClass().getSimpleName(), "Invalid JSON", e);
			}
			hikeHttpRequest.setJSONData(request);

			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hikeHTTPTask, hikeHttpRequest);
		}
	}

	private class PostDeviceDetails extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
					MODE_PRIVATE).getBoolean(
					HikeMessengerApp.DEVICE_DETAILS_SENT, false)) {
				Log.d(getClass().getSimpleName(), "Device details sent");
				return;
			}
			Log.d(getClass().getSimpleName(),
					"Sending device details to server");

			String osVersion = Build.VERSION.RELEASE;
			String devType = HikeConstants.ANDROID;
			String os = HikeConstants.ANDROID;
			String deviceVersion = Build.MANUFACTURER + " " + Build.MODEL;
			String appVersion = "";
			try {
				appVersion = context.getPackageManager().getPackageInfo(
						context.getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				Log.e("AccountUtils", "Unable to get app version");
			}

			TelephonyManager manager = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			String deviceKey = manager.getDeviceId();

			JSONObject data = new JSONObject();
			try {
				data.put(HikeConstants.DEV_TYPE, devType);
				data.put(HikeConstants.APP_VERSION, appVersion);
				data.put(HikeConstants.LogEvent.OS, os);
				data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
				data.put(HikeConstants.DEVICE_VERSION, deviceVersion);
				data.put(HikeConstants.DEVICE_KEY, deviceKey);
			} catch (JSONException e) {
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			}

			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
					"/account/update", RequestType.OTHER,
					new HikeHttpCallback() {
						public void onSuccess(JSONObject response) {
							Log.d(getClass().getSimpleName(), "Send successful");
							Editor editor = getSharedPreferences(
									HikeMessengerApp.ACCOUNT_SETTINGS,
									MODE_PRIVATE).edit();
							editor.putBoolean(
									HikeMessengerApp.DEVICE_DETAILS_SENT, true);
							editor.commit();
						}

						public void onFailure() {
							Log.d(getClass().getSimpleName(),
									"Send unsuccessful");
							scheduleNextSendToServerAction(false);
						}
					});
			hikeHttpRequest.setJSONData(data);

			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hikeHTTPTask, hikeHttpRequest);
		}
	}

	private void scheduleNextSendToServerAction(boolean gcmReg) {
		Log.d(getClass().getSimpleName(), "Scheduling next GCM registration");

		SharedPreferences preferences = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		int lastBackOffTime = preferences.getInt(
				gcmReg ? HikeMessengerApp.LAST_BACK_OFF_TIME
						: HikeMessengerApp.LAST_BACK_OFF_TIME_DEV_DETAILS, 0);

		lastBackOffTime = lastBackOffTime == 0 ? HikeConstants.RECONNECT_TIME
				: (lastBackOffTime * 2);
		lastBackOffTime = Math.min(HikeConstants.MAX_RECONNECT_TIME,
				lastBackOffTime);

		Log.d(getClass().getSimpleName(), "Scheduling the next disconnect");

		if (gcmReg) {
			postRunnableWithDelay(sendGCMIdToServer, lastBackOffTime * 1000);
		} else {
			postRunnableWithDelay(sendDevDetailsToServer,
					lastBackOffTime * 1000);
		}

		Editor editor = preferences.edit();
		editor.putInt(gcmReg ? HikeMessengerApp.LAST_BACK_OFF_TIME
				: HikeMessengerApp.LAST_BACK_OFF_TIME_DEV_DETAILS,
				lastBackOffTime);
		editor.commit();
	}

	private Runnable sendGCMIdToServer = new Runnable() {
		@Override
		public void run() {
			sendBroadcast(new Intent(SEND_TO_SERVER_ACTION));
		}
	};

	private Runnable sendDevDetailsToServer = new Runnable() {
		@Override
		public void run() {
			sendBroadcast(new Intent(SEND_DEV_DETAILS_TO_SERVER_ACTION));
		}
	};

	private void scheduleNextManualContactSync() {
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.HOUR, 24);

		long scheduleTime = getScheduleTime(wakeUpTime.getTimeInMillis());

		postRunnableWithDelay(contactSyncRunnable, scheduleTime);
	}

	private Runnable contactSyncRunnable = new Runnable() {
		@Override
		public void run() {
			sendBroadcast(new Intent(MQTT_CONTACT_SYNC_ACTION));
		}
	};

	private void scheduleNextUserStatsSending() {
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.HOUR, 12);

		long scheduleTime = getScheduleTime(wakeUpTime.getTimeInMillis());

		postRunnableWithDelay(sendUserStats, scheduleTime);
	}

	private Runnable sendUserStats = new Runnable() {

		@Override
		public void run() {
			JSONObject obj = Utils.getDeviceStats(getApplicationContext());
			if (obj != null) {
				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
						obj);
			}
			scheduleNextUserStatsSending();
		}
	};

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
