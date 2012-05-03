package com.bsb.hike.service;

import java.util.ArrayList;
import java.util.Calendar;

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
import android.net.Uri;
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
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.service.HikeMqttManager.MQTTConnectionStatus;
import com.bsb.hike.tasks.CheckForUpdateTask;
import com.bsb.hike.utils.ContactUtils;

public class HikeService extends Service
{

	public class ContactsChanged implements Runnable
	{

		private Context context;

		public ContactsChanged(Context ctx)
		{
			this.context = ctx;
		}

		@Override
		public void run()
		{
			Log.d("ContactsChanged", "calling syncUpdates");
			ContactUtils.syncUpdates(this.context);
		}
	}

	private class PingRunnable implements Runnable
	{
		@Override
		public void run()
		{
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

	protected ArrayList<String> pendingMessages;

	class IncomingHandler extends Handler
	{
		public IncomingHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_APP_CONNECTED:
				if (mMqttManager.isConnected())
				{
					mMqttManager.subscribeToUIEvents();
				}
				handleStart();

				mApp = msg.replyTo;
				/* TODO what if the app crashes while we're sending the message? */
				for (String m : pendingMessages)
				{
					sendToApp(m);
				}
				pendingMessages.clear();
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
				mMqttManager.send(new HikePacket(message.getBytes(), msgId), msg.arg1);
			}
		}
	}

	public void storeMessage(String message)
	{
		pendingMessages.add(message);
	}

	public boolean sendToApp(String message)
	{
		if (mApp == null)
		{
			Log.d("HikeService", "no app connected for message " + message);
			return false;
		}

		try
		{
			Message msg = Message.obtain();
			msg.what = MSG_APP_PUBLISH;
			Bundle bundle = new Bundle();
			bundle.putString("msg", message);
			msg.setData(bundle);
			mApp.send(msg);
		}
		catch (RemoteException e)
		{
			// client is dead :(
			mApp = null;
			mMqttManager.unsubscribeFromUIEvents();
			Log.e("HikeService", "Can't send message to the application");
			return false;
		}
		return true;
	}

	private Messenger mMessenger;

	/************************************************************************/
	/* CONSTANTS */
	/************************************************************************/

	// constant used internally to schedule the next ping event
	public static final String MQTT_PING_ACTION = "com.bsb.hike.PING";
	
	// constant used internally to schedule the next check for update
	public static final String APP_UPDATE_ACTION = "com.bsb.hike.UPDATE";
	
	// constants used by status bar notifications
	public static final int MQTT_NOTIFICATION_ONGOING = 1;

	public static final int MQTT_NOTIFICATION_UPDATE = 2;

	/************************************************************************/
	/* VARIABLES - other local variables */
	/************************************************************************/

	// receiver that notifies the Service when the phone gets data connection
	private NetworkConnectionIntentReceiver netConnReceiver;

	// receiver that notifies the Service when the user changes data use preferences
	private BackgroundDataChangeIntentReceiver dataEnabledReceiver;

	// receiver that wakes the Service up when it's time to ping the server
	private PingSender pingSender;

	private HikeMqttManager mMqttManager;
	private String mToken;
	private ContactListChangeIntentReceiver contactsReceived;
	private Handler mHandler;

	private Handler mContactsChangedHandler;
	private Runnable mContactsChanged;

	private PingRunnable pingRunnable;

	private Looper mContactHandlerLooper;

	private Looper mMqttHandlerLooper;
	
	private UpdateChecker updateChecker;
	/************************************************************************/
	/* METHODS - core Service lifecycle methods */
	/************************************************************************/

	// see http://developer.android.com/guide/topics/fundamentals.html#lcycles

	@Override
	public void onCreate()
	{
		super.onCreate();

		Log.d("HikeService", "onCreate called");
		HandlerThread mqttHandlerThread = new HandlerThread("MQTTThread");
		mqttHandlerThread.start();
		mMqttHandlerLooper = mqttHandlerThread.getLooper();
		this.mHandler = new Handler(mMqttHandlerLooper);
		mMessenger = new Messenger(new IncomingHandler(mMqttHandlerLooper));
		pendingMessages = new ArrayList<String>();

		// reset status variable to initial state
		mMqttManager = new HikeMqttManager(this, this.mHandler);

		// register to be notified whenever the user changes their preferences
		// relating to background data use - so that we can respect the current
		// preference
		dataEnabledReceiver = new BackgroundDataChangeIntentReceiver();
		registerReceiver(dataEnabledReceiver, new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));

		/*
		 * notify android that our service represents a user visible action, so it should not be killable. In order to do so, we need to show a notification so the user understands
		 * what's going on
		 */

		/*
		 * Remove this for now because a) it's irritating and b) it may be causing increased battery usage.
		Notification notification = new Notification(R.drawable.ic_contact_logo, getResources().getString(R.string.service_running_message), System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		Intent notificationIntent = new Intent(this, MessagesList.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, "Hike", "Hike", contentIntent);
		startForeground(HikeNotification.HIKE_NOTIFICATION, notification);
		*/

		HandlerThread contactHandlerThread = new HandlerThread("");
		contactHandlerThread.start();
		mContactHandlerLooper = contactHandlerThread.getLooper();
		mContactsChangedHandler = new Handler(mContactHandlerLooper);
		mContactsChanged = new ContactsChanged(this);

		/* register with the Contact list to get an update whenever the phone book changes.
		 * Use the application thread for the intent receiver, the IntentReceiver will take
		 * care of running the event on a different thread */
		contactsReceived = new ContactListChangeIntentReceiver(new Handler());
		/* listen for changes in the addressbook */
		getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsReceived);
		/* listen for changes on the simcard */
		getContentResolver().registerContentObserver(Uri.parse("content://icc/adn"), true, contactsReceived);
		
		// Check for updates
		if (updateChecker == null)
		{
			Log.d("HikeService", "Registering UPDATE CHECK receiver");
			updateChecker = new UpdateChecker();
			registerReceiver(updateChecker, new IntentFilter(APP_UPDATE_ACTION));
			scheduleNextUpdateCheck();
		}
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId)
	{
		asyncStart();
		// return START_NOT_STICKY - we want this Service to be left running
		// unless explicitly stopped, and it's process is killed, we want it to
		// be restarted
		return START_STICKY;
	}

	private void asyncStart()
	{
		/* ensure that all mqtt activity is done on the mqtt thread */
		this.mHandler.postAtFrontOfQueue(new Runnable()
		{
			@Override
			public void run()
			{
				handleStart();
			}
		});
	}

	synchronized void handleStart()
	{
		// before we start - check for a couple of reasons why we should stop

		Log.d("HikeService", "handlestart called");
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getBackgroundDataSetting() == false) // respect the user's request not to use data!
		{
			// user has disabled background data
			mMqttManager.setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_DATADISABLED);

			// we have a listener running that will notify us when this
			// preference changes, and will call handleStart again when it
			// is - letting us pick up where we leave off now
			return;
		}

		// if the Service was already running and we're already connected - we
		// don't need to do anything
		if (this.haveCredentials() && !this.mMqttManager.isConnected())
		{
			this.mMqttManager.connect();

		}

		// changes to the phone's network - such as bouncing between WiFi
		// and mobile data networks - can break the MQTT connection
		// the MQTT connectionLost can be a bit slow to notice, so we use
		// Android's inbuilt notification system to be informed of
		// network changes - so we can reconnect immediately, without
		// haing to wait for the MQTT timeout
		if (netConnReceiver == null)
		{
			netConnReceiver = new NetworkConnectionIntentReceiver();
			registerReceiver(netConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		}

		// creates the intents that are used to wake up the phone when it is
		// time to ping the server
		if (pingSender == null)
		{
			pingSender = new PingSender();
			pingRunnable = new PingRunnable();
			registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
		}
	}

	private boolean haveCredentials()
	{
		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		return !TextUtils.isEmpty(settings.getString(HikeMessengerApp.TOKEN_SETTING, null));
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		// disconnect immediately
		this.mMqttManager.disconnectFromBroker(false);

		// inform the app that the app has successfully disconnected
		Log.i("HikeService", "onDestroy.  Shutting down service");

		// try not to leak the listener
		if (dataEnabledReceiver != null)
		{
			unregisterReceiver(dataEnabledReceiver);
			dataEnabledReceiver = null;
		}

		if (pingSender != null)
		{
			unregisterReceiver(pingSender);
			pingSender = null;
		}

		if (netConnReceiver != null)
		{
			unregisterReceiver(netConnReceiver);
			netConnReceiver = null;
		}

		if (contactsReceived != null)
		{
			getContentResolver().unregisterContentObserver(contactsReceived);
			contactsReceived = null;
		}

		if (mMqttHandlerLooper != null)
		{
			mMqttHandlerLooper.quit();
		}

		if (mContactHandlerLooper != null)
		{
			mContactHandlerLooper.quit();
		}
		
		if(updateChecker != null)
		{
			unregisterReceiver(updateChecker);
			updateChecker = null;
		}
	}

	/************************************************************************/
	/* METHODS - broadcasts and notifications */
	/************************************************************************/

	// methods used to notify the Activity UI of something that has happened
	// so that it can be updated to reflect status and the data received
	// from the server

	public void broadcastServiceStatus(HikeMqttManager.MQTTConnectionStatus status)
	{
		Log.d("HikeService", "broadcastServiceStatus " + status);

		if (status == HikeMqttManager.MQTTConnectionStatus.CONNECTED)
		{
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(HikeConstants.HIKE_SYSTEM_NOTIFICATION);
		}

		if (mApp == null)
		{
			return;
		}

		try
		{
			Message msg = Message.obtain();
			msg.what = MSG_APP_CONN_STATUS;
			msg.arg1 = status.ordinal();
			mApp.send(msg);
		}
		catch (RemoteException e)
		{
			// client is dead :(
			mApp = null;
			mMqttManager.unsubscribeFromUIEvents();
			Log.e("HikeService", "Can't send connection status to the application");
		}

	}

	// methods used to notify the user of what has happened for times when
	// the app Activity UI isn't running

	public void notifyUser(String alert, String title, String body)
	{
		Log.w("HikeService", alert + ":" + "body");
	}

	/************************************************************************/
	/* METHODS - binding that allows access from the Activity */
	/************************************************************************/

	@Override
	public IBinder onBind(Intent intent)
	{
		return mMessenger.getBinder();
	}

	/************************************************************************/
	/* METHODS - wrappers for some of the MQTT methods that we use */
	/************************************************************************/

	private class ContactListChangeIntentReceiver extends ContentObserver
	{

		public ContactListChangeIntentReceiver(Handler handler)
		{
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange)
		{
			HikeService.this.mContactsChangedHandler.removeCallbacks(mContactsChanged);
			HikeService.this.mContactsChangedHandler.postDelayed(mContactsChanged, HikeConstants.CONTACT_UPDATE_TIMEOUT);
			Log.d("ContactListChangeIntentReceiver", "onChange called");
		}
	}

	private class BackgroundDataChangeIntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent intent)
		{
			// we protect against the phone switching off while we're doing this
			// by requesting a wake lock - we request the minimum possible wake
			// lock - just enough to keep the CPU running until we've finished
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
			try
			{
				wl.acquire();

				ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				if (cm.getBackgroundDataSetting())
				{
					// user has allowed background data - we start again - picking
					// up where we left off in handleStart before
					asyncStart();
				}
				else
				{
					// user has disabled background data
					mMqttManager.setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_DATADISABLED);

					// disconnect from the broker
					mMqttManager.disconnectFromBroker(false);
				}

				// we're finished - if the phone is switched off, it's okay for the CPU
				// to sleep now
			}
			finally
			{
				wl.release();
			}
		}
	}

	/*
	 * Called in response to a change in network connection - after losing a connection to the server, this allows us to wait until we have a usable data connection again
	 */
	private class NetworkConnectionIntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent intent)
		{
			// we protect against the phone switching off while we're doing this
			// by requesting a wake lock - we request the minimum possible wake
			// lock - just enough to keep the CPU running until we've finished
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
			wl.acquire();

			if (isUserOnline())
			{
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						HikeService.this.mMqttManager.reconnect();
					}
				});
			}

			// we're finished - if the phone is switched off, it's okay for the CPU
			// to sleep now
			wl.release();
		}
	}

	public void scheduleNextPing()
	{
		scheduleNextPing( (int) (HikeConstants.KEEP_ALIVE * 0.9));
	}

	/*
	 * Schedule the next time that you want the phone to wake up and ping the message broker server
	 */
	public void scheduleNextPing(int timeout)
	{
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

		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(MQTT_PING_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);

		// in case it takes us a little while to do this, we try and do it
		// shortly before the keep alive period expires
		// it means we're pinging slightly more frequently than necessary
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.SECOND, timeout); //comes from PushMqttManager.KEEPALIVE

		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(), pendingIntent);
	}

	/*
	 * Used to implement a keep-alive protocol at this Service level - it sends a PING message to the server, then schedules another ping after an interval defined by
	 * keepAliveSeconds
	 */
	public class PingSender extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			mHandler.post(pingRunnable);

			// start the next keep alive period
			scheduleNextPing();

		}
	}
	
	/**
	 * Used for checking for an update to the application once a day.
	 * @author rs
	 *
	 */
	public class UpdateChecker extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			checkForUpdate();
		}
	}
	
	/**
	 * Schedule an update check once a day. 
	 */
	private void scheduleNextUpdateCheck()
	{
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(APP_UPDATE_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.HOUR, 24);
		AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(), pendingIntent);
	}
	
	private void checkForUpdate()
	{
		CheckForUpdateTask checkForUpdateTask = new CheckForUpdateTask(getApplicationContext());
		checkForUpdateTask.execute();
		// Schedule next check
		scheduleNextUpdateCheck();
	}
	
	public boolean isUserOnline()
	{
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected())
		{
			return true;
		}

		return false;
	}

	public boolean appIsConnected()
	{
		return mApp != null;
	}

	public boolean sendInvalidToken()
	{
		if (mApp == null)
		{
			Log.d("HikeService", "no app");
			return false;
		}

		try
		{
			Message msg = Message.obtain(null, MSG_APP_INVALID_TOKEN);
			mApp.send(msg);
		}
		catch (RemoteException e)
		{
			// client is dead :(
			mApp = null;
			mMqttManager.unsubscribeFromUIEvents();
			Log.e("HikeService", "Can't send message to the application");
			return false;
		}
		return true;
	}

	public boolean sendMessageStatus(Long msgId, boolean sent)
	{
		if (mApp == null)
		{
			Log.d("HikeService", "no app");
			return false;
		}

		try
		{
			Message msg = Message.obtain(null, MSG_APP_MESSAGE_STATUS);
			msg.obj = msgId;
			msg.arg1 = sent ? 1 : 0;
			mApp.send(msg);
		}
		catch (RemoteException e)
		{
			// client is dead :(
			mApp = null;
			mMqttManager.unsubscribeFromUIEvents();
			Log.e("HikeService", "Can't send message to the application");
			return false;
		}
		return true;
	}
}
