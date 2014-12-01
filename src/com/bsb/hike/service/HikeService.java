package com.bsb.hike.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Messenger;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

import com.bsb.hike.GCMIntentService;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.DBBackupRestore;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.smartcache.HikeLruCache;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.CheckForUpdateTask;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.tasks.SyncContactExtraInfo;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.google.android.gcm.GCMRegistrar;

public class HikeService extends Service
{
	public class ContactsChanged implements Runnable
	{
		private Context context;

		boolean manualSync;

		public ContactsChanged(Context ctx)
		{
			this.context = ctx;
		}

		@Override
		public void run()
		{
			if (!Utils.isUserOnline(context))
			{
				Logger.d("CONTACT UTILS", "Airplane mode is on , skipping sync update tasks.");
			}
			else
			{
				HikeMessengerApp.syncingContacts = true;
				Logger.d("ContactsChanged", "calling syncUpdates, manualSync = " + manualSync);
				HikeMessengerApp.getPubSub().publish(HikePubSub.CONTACT_SYNC_STARTED, null);

				boolean contactsChanged = ContactManager.getInstance().syncUpdates(this.context);

				HikeMessengerApp.syncingContacts = false;
				HikeMessengerApp.getPubSub().publish(HikePubSub.CONTACT_SYNCED, new Boolean[] {manualSync, contactsChanged});
			}

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

	/************************************************************************/
	/* CONSTANTS */
	/************************************************************************/

	public static final String MQTT_CONTACT_SYNC_ACTION = "com.bsb.hike.CONTACT_SYNC";

	// used to register to GCM
	public static final String REGISTER_TO_GCM_ACTION = "com.bsb.hike.REGISTER_GCM";

	// used to send GCM registeration id to server
	public static final String SEND_TO_SERVER_ACTION = "com.bsb.hike.SEND_TO_SERVER";

	// used to send GCM registeration id to server
	public static final String SEND_DEV_DETAILS_TO_SERVER_ACTION = "com.bsb.hike.SEND_DEV_DETAILS_TO_SERVER";

	public static final String SEND_RAI_TO_SERVER_ACTION = "com.bsb.hike.SEND_RAI";

	// used to send GreenBlue details to server
	public static final String SEND_GB_DETAILS_TO_SERVER_ACTION = "com.bsb.hike.SEND_GB_DETAILS_TO_SERVER";

	// constants used by status bar notifications
	public static final int MQTT_NOTIFICATION_ONGOING = 1;

	public static final int MQTT_NOTIFICATION_UPDATE = 2;

	public static final String POST_SIGNUP_PRO_PIC_TO_SERVER_ACTION = "com.bsb.hike.POST_SIGNUP_PRO_PIC_TO_SERVER_ACTION";

	/************************************************************************/
	/* VARIABLES - other local variables */
	/************************************************************************/

	// receiver that triggers a contact sync
	private ManualContactSyncTrigger manualContactSyncTrigger;

	private RegisterToGCMTrigger registerToGCMTrigger;

	private SendGCMIdToServerTrigger sendGCMIdToServerTrigger;

	private PostDeviceDetails postDeviceDetails;

	private PostGreenBlueDetails postGreenBlueDetails;

	private PostSignupProfilePic postSignupProfilePic;

	private HikeMqttManagerNew mMqttManager;

	private SendRai sendRai;

	private ContactListChangeIntentReceiver contactsReceived;

	private Handler mContactsChangedHandler;

	private ContactsChanged mContactsChanged;

	private Looper mContactHandlerLooper;
	
	private static final String TRIM_TAG = "TrimToSize";

	/************************************************************************/
	/* METHODS - core Service lifecycle methods */
	/************************************************************************/

	// see http://developer.android.com/guide/topics/fundamentals.html#lcycles

	@Override
	public void onCreate()
	{
		super.onCreate();

		Logger.d("TestUpdate", "Service started");

		if (sendGCMIdToServerTrigger == null)
		{
			sendGCMIdToServerTrigger = new SendGCMIdToServerTrigger();
			registerReceiver(sendGCMIdToServerTrigger, new IntentFilter(SEND_TO_SERVER_ACTION));
		}

		if (registerToGCMTrigger == null)
		{
			registerToGCMTrigger = new RegisterToGCMTrigger();
			registerReceiver(registerToGCMTrigger, new IntentFilter(REGISTER_TO_GCM_ACTION));
			sendBroadcast(new Intent(REGISTER_TO_GCM_ACTION));
		}

		Logger.d("HikeService", "onCreate called");

		// reset status variable to initial state
		// mMqttManager = HikeMqttManager.getInstance(getApplicationContext());
		mMqttManager = new HikeMqttManagerNew(getApplicationContext());
		mMqttManager.init();

		/*
		 * notify android that our service represents a user visible action, so it should not be killable. In order to do so, we need to show a notification so the user understands
		 * what's going on
		 */

		/*
		 * Remove this for now because a) it's irritating and b) it may be causing increased battery usage. Notification notification = new Notification(R.drawable.ic_contact_logo,
		 * getResources().getString(R.string.service_running_message), System.currentTimeMillis()); notification.flags |= Notification.FLAG_AUTO_CANCEL;
		 * 
		 * Intent notificationIntent = new Intent(this, MessagesList.class); PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		 * notification.setLatestEventInfo(this, "Hike", "Hike", contentIntent); startForeground(HikeNotification.HIKE_NOTIFICATION, notification);
		 */

		HandlerThread contactHandlerThread = new HandlerThread("");
		contactHandlerThread.start();
		mContactHandlerLooper = contactHandlerThread.getLooper();
		mContactsChangedHandler = new Handler(mContactHandlerLooper);
		mContactsChanged = new ContactsChanged(this);
		
		//scheduleNextAccountBackup();

		/*
		 * register with the Contact list to get an update whenever the phone book changes. Use the application thread for the intent receiver, the IntentReceiver will take care of
		 * running the event on a different thread
		 */
		contactsReceived = new ContactListChangeIntentReceiver(new Handler());
		/* listen for changes in the addressbook */
		getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsReceived);
		/*
		 * listen for changes in sim contacts
		 */
		getContentResolver().registerContentObserver(Uri.parse("content://icc/adn"), true, contactsReceived);
		if (manualContactSyncTrigger == null)
		{
			manualContactSyncTrigger = new ManualContactSyncTrigger();
			registerReceiver(manualContactSyncTrigger, new IntentFilter(MQTT_CONTACT_SYNC_ACTION));
			/*
			 * Forcing a sync first time service is created to fix bug where if the app is force stopped no contacts are synced if they are added when the app is in force stopped
			 * state
			 */
			getContentResolver().notifyChange(ContactsContract.Contacts.CONTENT_URI, null);
		}

		if (postDeviceDetails == null)
		{
			postDeviceDetails = new PostDeviceDetails();
			registerReceiver(postDeviceDetails, new IntentFilter(SEND_DEV_DETAILS_TO_SERVER_ACTION));
			sendBroadcast(new Intent(SEND_DEV_DETAILS_TO_SERVER_ACTION));
			Logger.d("TestUpdate", "Update details sender registered");
		}

		if (sendRai == null)
		{
			sendRai = new SendRai();
			registerReceiver(sendRai, new IntentFilter(SEND_RAI_TO_SERVER_ACTION));
			Logger.d("TestUpdate", "Update details sender registered");
		}

		if (postGreenBlueDetails == null)
		{
			postGreenBlueDetails = new PostGreenBlueDetails();
			registerReceiver(postGreenBlueDetails, new IntentFilter(SEND_GB_DETAILS_TO_SERVER_ACTION));
			sendBroadcast(new Intent(SEND_GB_DETAILS_TO_SERVER_ACTION));
		}

		if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH, null) != null && postSignupProfilePic == null)
		{
			postSignupProfilePic = new PostSignupProfilePic();
			registerReceiver(postSignupProfilePic, new IntentFilter(POST_SIGNUP_PRO_PIC_TO_SERVER_ACTION));
			sendBroadcast(new Intent(POST_SIGNUP_PRO_PIC_TO_SERVER_ACTION));
		}

		if (!getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.CONTACT_EXTRA_INFO_SYNCED, false))
		{
			Logger.d(getClass().getSimpleName(), "SYNCING");
			SyncContactExtraInfo syncContactExtraInfo = new SyncContactExtraInfo();
			Utils.executeAsyncTask(syncContactExtraInfo);
		}
	}

	
	
	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId)
	{
		Logger.d("HikeService", "Start MQTT Thread.");
		mMqttManager.connectOnMqttThread();
		Logger.d("HikeService", "Intent is " + intent);
		if (intent != null && intent.hasExtra(HikeConstants.Extras.SMS_MESSAGE))
		{
			String s = intent.getExtras().getString(HikeConstants.Extras.SMS_MESSAGE);
			try
			{
				JSONObject msg = new JSONObject(s);
				Logger.d("HikeService", "Intent contained SMS message " + msg.getString(HikeConstants.TYPE));
				MqttMessagesManager mgr = MqttMessagesManager.getInstance(this);
				mgr.saveMqttMessage(msg);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		// return START_NOT_STICKY - we want this Service to be left running
		// unless explicitly stopped, and it's process is killed, we want it to
		// be restarted
		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Logger.i("HikeService", "onDestroy.  Shutting down service");

		mMqttManager.destroyMqtt();
		this.mMqttManager = null;
		// inform the app that the app has successfully disconnected
		if (contactsReceived != null)
		{
			getContentResolver().unregisterContentObserver(contactsReceived);
			contactsReceived = null;
		}

		if (mContactHandlerLooper != null)
		{
			mContactHandlerLooper.quit();
		}

		if (manualContactSyncTrigger != null)
		{
			unregisterReceiver(manualContactSyncTrigger);
			manualContactSyncTrigger = null;
		}

		if (registerToGCMTrigger != null)
		{
			unregisterReceiver(registerToGCMTrigger);
			registerToGCMTrigger = null;
		}

		if (sendGCMIdToServerTrigger != null)
		{
			unregisterReceiver(sendGCMIdToServerTrigger);
			sendGCMIdToServerTrigger = null;
		}

		if (postDeviceDetails != null)
		{
			unregisterReceiver(postDeviceDetails);
			postDeviceDetails = null;
		}

		if (sendRai != null)
		{
			unregisterReceiver(sendRai);
			sendRai = null;
		}

		if (postGreenBlueDetails != null)
		{
			unregisterReceiver(postGreenBlueDetails);
			postGreenBlueDetails = null;
		}

		if (postSignupProfilePic != null)
		{
			unregisterReceiver(postSignupProfilePic);
			postSignupProfilePic = null;
		}

	}

	/************************************************************************/
	/* METHODS - broadcasts and notifications */
	/************************************************************************/

	// methods used to notify the Activity UI of something that has happened
	// so that it can be updated to reflect status and the data received
	// from the server

	// methods used to notify the user of what has happened for times when
	// the app Activity UI isn't running

	public void notifyUser(String alert, String title, String body)
	{
		Logger.w("HikeService", alert + ":" + "body");
	}

	/************************************************************************/
	/* METHODS - binding that allows access from the Activity */
	/************************************************************************/

	@Override
	public IBinder onBind(Intent intent)
	{
		return mMqttManager.getMessenger().getBinder();
	}

	/************************************************************************/
	/* METHODS - wrappers for some of the MQTT methods that we use */
	/************************************************************************/

	private class ContactListChangeIntentReceiver extends ContentObserver
	{
		boolean manualSync;

		public ContactListChangeIntentReceiver(Handler handler)
		{
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange)
		{
			Logger.d(getClass().getSimpleName(), "Contact content observer called");
			if(HikeMessengerApp.syncingContacts)
				Logger.d(getClass().getSimpleName(),"Contact Syncing already going on");
			else
			{
				mContactsChanged.manualSync = manualSync;
				HikeService.this.mContactsChangedHandler.removeCallbacks(mContactsChanged);
				long delay = manualSync ? 0L: HikeConstants.CONTACT_UPDATE_TIMEOUT;
				HikeService.this.mContactsChangedHandler.postDelayed(mContactsChanged, delay);
				// Schedule the next manual sync to happed 24 hours from now.
				scheduleNextManualContactSync();

				manualSync = false;
			}
		}
	}

	private void postRunnableWithDelay(Runnable runnable, long delay)
	{
		mContactsChangedHandler.removeCallbacks(runnable);
		mContactsChangedHandler.postDelayed(runnable, delay);
	}

	private long getScheduleTime(long wakeUpTime)
	{
		return (wakeUpTime - System.currentTimeMillis());
	}

	private class ManualContactSyncTrigger extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			contactsReceived.manualSync = intent.getBooleanExtra(HikeConstants.Extras.MANUAL_SYNC, false);

			getContentResolver().notifyChange(ContactsContract.Contacts.CONTENT_URI, null);
		}
	}

	private class RegisterToGCMTrigger extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Logger.d(getClass().getSimpleName(), "Registering for GCM");
			try
			{
				GCMRegistrar.checkDevice(HikeService.this);
				GCMRegistrar.checkManifest(HikeService.this);
				final String regId = GCMRegistrar.getRegistrationId(HikeService.this);
				if ("".equals(regId))
				{
					/*
					 * Since we are registering again, we should clear this preference
					 */
					Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
					editor.remove(HikeMessengerApp.GCM_ID_SENT);
					editor.commit();

					GCMRegistrar.register(HikeService.this, HikeConstants.APP_PUSH_ID);
				}
				else
				{
					sendBroadcast(new Intent(SEND_TO_SERVER_ACTION));
				}
			}
			catch (UnsupportedOperationException e)
			{
				/*
				 * User doesnt have google services
				 */
			}
			catch (IllegalStateException e)
			{
				Logger.e("HikeService", "Exception during gcm registration : " , e);
			}
		}
	}

	private class SendGCMIdToServerTrigger extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			Logger.d(getClass().getSimpleName(), "Sending GCM ID");
			final String regId = GCMRegistrar.getRegistrationId(context);
			if ("".equals(regId))
			{
				sendBroadcast(new Intent(REGISTER_TO_GCM_ACTION));
				Logger.d(getClass().getSimpleName(), "GCM id not found");
				return;
			}

			final SharedPreferences prefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
			if (prefs.getBoolean(HikeMessengerApp.GCM_ID_SENT, false))
			{
				Logger.d(getClass().getSimpleName(), "GCM id sent");
				return;
			}

			Logger.d(getClass().getSimpleName(), "GCM id was not sent. Sending now");
			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/device", RequestType.OTHER, new HikeHttpCallback()
			{
				public void onSuccess(JSONObject response)
				{
					Logger.d(SendGCMIdToServerTrigger.this.getClass().getSimpleName(), "Send successful");
					Editor editor = prefs.edit();
					editor.putBoolean(HikeMessengerApp.GCM_ID_SENT, true);
					editor.commit();
				}

				public void onFailure()
				{
					Logger.d(SendGCMIdToServerTrigger.this.getClass().getSimpleName(), "Send unsuccessful");
					scheduleNextSendToServerAction(HikeMessengerApp.LAST_BACK_OFF_TIME, sendGCMIdToServer);
				}
			});
			JSONObject request = new JSONObject();
			try
			{
				request.put(GCMIntentService.DEV_TYPE, HikeConstants.ANDROID);
				request.put(GCMIntentService.DEV_TOKEN, regId);
			}
			catch (JSONException e)
			{
				Logger.d(getClass().getSimpleName(), "Invalid JSON", e);
			}
			hikeHttpRequest.setJSONData(request);

			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hikeHTTPTask, hikeHttpRequest);
		}
	}

	private class PostDeviceDetails extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.DEVICE_DETAILS_SENT, false))
			{
				Logger.d(getClass().getSimpleName(), "Device details sent");
				return;
			}
			Logger.d("TestUpdate", "Sending device details to server");
			Logger.d(getClass().getSimpleName(), "Sending device details to server");

			String osVersion = Build.VERSION.RELEASE;
			String devType = HikeConstants.ANDROID;
			String os = HikeConstants.ANDROID;
			String deviceVersion = Build.MANUFACTURER + " " + Build.MODEL;
			String appVersion = "";
			try
			{
				appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			}
			catch (NameNotFoundException e)
			{
				Logger.e("AccountUtils", "Unable to get app version");
			}

			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String deviceKey = manager.getDeviceId();

			JSONObject data = new JSONObject();
			try
			{
				data.put(HikeConstants.DEV_TYPE, devType);
				data.put(HikeConstants.APP_VERSION, appVersion);
				data.put(HikeConstants.LogEvent.OS, os);
				data.put(HikeConstants.LogEvent.OS_VERSION, osVersion);
				data.put(HikeConstants.DEVICE_VERSION, deviceVersion);
				data.put(HikeConstants.DEVICE_KEY, deviceKey);
				Utils.addCommonDeviceDetails(data, context);
			}
			catch (JSONException e)
			{
				Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
			}

			Logger.d("TestUpdate", "Sending data: " + data.toString());

			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/update", RequestType.OTHER, new HikeHttpCallback()
			{
				public void onSuccess(JSONObject response)
				{
					Logger.d("TestUpdate", "Device details sent successfully");
					Logger.d(getClass().getSimpleName(), "Send successful");
					Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
					editor.putBoolean(HikeMessengerApp.DEVICE_DETAILS_SENT, true);
					if (response != null)
					{
						String backupToken = response.optString("backup_token");
						editor.putString(HikeMessengerApp.BACKUP_TOKEN_SETTING, backupToken);
					}
					editor.commit();
				}

				public void onFailure()
				{
					Logger.d("TestUpdate", "Device details could not be sent");
					Logger.d(getClass().getSimpleName(), "Send unsuccessful");
					scheduleNextSendToServerAction(HikeMessengerApp.LAST_BACK_OFF_TIME_DEV_DETAILS, sendDevDetailsToServer);
				}
			});
			hikeHttpRequest.setJSONData(data);

			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hikeHTTPTask, hikeHttpRequest);
		}
	}

	private class SendRai extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.UPGRADE_RAI_SENT, false))
			{
				Logger.d(getClass().getSimpleName(), "Rai was already sent");
				return;
			}
			Logger.d("TestUpdate", "Sending rai packet to server");

			// Send the device details again which includes the new app
			// version
			JSONObject obj = Utils.getDeviceDetails(context);
			if (obj != null)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, obj);
			}

			Utils.requestAccountInfo(true, false);

			Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
			editor.putBoolean(HikeMessengerApp.UPGRADE_RAI_SENT, true);
			editor.commit();

			Logger.d("TestUpdate", "rai packet sent to server");
		}
	}

	private void scheduleNextSendToServerAction(String lastBackOffTimePref, Runnable postRunnableReference)
	{
		Logger.d(getClass().getSimpleName(), "Scheduling next " + lastBackOffTimePref + " send");

		SharedPreferences preferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		int lastBackOffTime = preferences.getInt(lastBackOffTimePref, 0);

		lastBackOffTime = lastBackOffTime == 0 ? HikeConstants.RECONNECT_TIME : (lastBackOffTime * 2);
		lastBackOffTime = Math.min(HikeConstants.MAX_RECONNECT_TIME, lastBackOffTime);

		Logger.d(getClass().getSimpleName(), "Scheduling the next disconnect");

		postRunnableWithDelay(postRunnableReference, lastBackOffTime * 1000);

		Editor editor = preferences.edit();
		editor.putInt(lastBackOffTimePref, lastBackOffTime);
		editor.commit();
	}

	private Runnable sendGCMIdToServer = new Runnable()
	{
		@Override
		public void run()
		{
			sendBroadcast(new Intent(SEND_TO_SERVER_ACTION));
		}
	};

	private Runnable sendDevDetailsToServer = new Runnable()
	{
		@Override
		public void run()
		{
			sendBroadcast(new Intent(SEND_DEV_DETAILS_TO_SERVER_ACTION));
		}
	};

	private Runnable sendGreenBlueDetailsToServer = new Runnable()
	{
		@Override
		public void run()
		{
			sendBroadcast(new Intent(HikeService.SEND_GB_DETAILS_TO_SERVER_ACTION));
		}
	};

	private Runnable sendSignupProfilePicToServer = new Runnable()
	{
		@Override
		public void run()
		{
			sendBroadcast(new Intent(HikeService.POST_SIGNUP_PRO_PIC_TO_SERVER_ACTION));
		}
	};
	
	private Runnable BackupAccountRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			DBBackupRestore.getInstance(HikeService.this).backupDB();
			scheduleNextAccountBackup();
		}
	};
	
	private void scheduleNextAccountBackup()
	{
		long MaxBackupDelay = 24 * 60 * 60 * 1000; // 24 Hours
		long lastBackup = DBBackupRestore.getInstance(HikeService.this).getLastBackupTime();

		long scheduleTime = 0;
		if ((System.currentTimeMillis() - lastBackup) > MaxBackupDelay)
		{
			scheduleTime = 0;
		}
		else
		{
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DAY_OF_MONTH, 1);
			c.set(Calendar.HOUR_OF_DAY, 3);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			scheduleTime = (c.getTimeInMillis() - System.currentTimeMillis());

		}
		postRunnableWithDelay(BackupAccountRunnable, scheduleTime);
	}

	private void scheduleNextManualContactSync()
	{
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.HOUR, 24);

		long scheduleTime = getScheduleTime(wakeUpTime.getTimeInMillis());

		postRunnableWithDelay(contactSyncRunnable, scheduleTime);
	}

	private Runnable contactSyncRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			sendBroadcast(new Intent(MQTT_CONTACT_SYNC_ACTION));
		}
	};

	private void scheduleNextUserStatsSending()
	{
		Calendar wakeUpTime = Calendar.getInstance();
		wakeUpTime.add(Calendar.HOUR, 12);

		long scheduleTime = getScheduleTime(wakeUpTime.getTimeInMillis());

		postRunnableWithDelay(sendUserStats, scheduleTime);
	}

	private Runnable sendUserStats = new Runnable()
	{

		@Override
		public void run()
		{
			JSONObject obj = Utils.getDeviceStats(getApplicationContext());
			if (obj != null)
			{
				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, obj);
			}
			scheduleNextUserStatsSending();
		}
	};

	private Runnable checkForUpdates = new Runnable()
	{

		@Override
		public void run()
		{
			CheckForUpdateTask checkForUpdateTask = new CheckForUpdateTask(HikeService.this);
			Utils.executeBoolResultAsyncTask(checkForUpdateTask);
		}
	};

	public boolean appIsConnected()
	{
		return mApp != null;
	}

	public class PostGreenBlueDetails extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.GREENBLUE_DETAILS_SENT, false))
			{
				Logger.d("PostInfo", "info details sent");
				return;
			}

			List<ContactInfo> contactinfos = ContactManager.getInstance().getAllContacts();
			ContactManager.getInstance().setGreenBlueStatus(context, contactinfos);
			JSONObject data = AccountUtils.getWAJsonContactList(contactinfos);

			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/account/info", RequestType.OTHER, new HikeHttpCallback()
			{
				public void onSuccess(JSONObject response)
				{
					Logger.d("PostInfo", "info sent successfully");
					Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
					editor.putBoolean(HikeMessengerApp.GREENBLUE_DETAILS_SENT, true);
					editor.putInt(HikeMessengerApp.LAST_BACK_OFF_TIME_GREENBLUE, 0);
					editor.commit();
				}

				public void onFailure()
				{
					Logger.d("PostInfo", "info could not be sent");
					scheduleNextSendToServerAction(HikeMessengerApp.LAST_BACK_OFF_TIME_GREENBLUE, sendGreenBlueDetailsToServer);
				}
			});
			hikeHttpRequest.setJSONData(data);

			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hikeHTTPTask, hikeHttpRequest);
		}
	}

	class PostSignupProfilePic extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			String profilePicPath = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH, null);

			if (profilePicPath == null)
			{
				Logger.d(getClass().getSimpleName(), "Signup profile pic already uploaded");
				HikeSharedPreferenceUtil.getInstance(context).removeData(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH);
				return;
			}

			final File f = new File(profilePicPath);
			if (!(f.exists() && f.length() > 0))
			{
				Logger.d(getClass().getSimpleName(), "Signup profile pic does not exists or it's length is zero");
				HikeSharedPreferenceUtil.getInstance(context).removeData(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH);
				f.delete();
				return;
			}

			Logger.d(getClass().getSimpleName(), "profile pic upload started");

			HikeHttpCallback hikeHttpCallBack = new HikeHttpCallback()
			{
				public void onSuccess(JSONObject response)
				{
					String msisdn = HikeSharedPreferenceUtil.getInstance(context).getData(HikeMessengerApp.MSISDN_SETTING, null);
					HikeSharedPreferenceUtil.getInstance(context).removeData(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH);
					Utils.renameTempProfileImage(msisdn);
					// clearing cache for this msisdn because if user go to profile before rename (above line) executes then icon blurred image will be set in cache
					HikeMessengerApp.getLruCache().clearIconForMSISDN(msisdn);
					Logger.d(getClass().getSimpleName(), "profile pic upload done");
				}

				public void onFailure()
				{
					Logger.d(getClass().getSimpleName(), "profile pic upload failed");
					if (f.exists() && f.length() > 0)
					{
						scheduleNextSendToServerAction(HikeMessengerApp.LAST_BACK_OFF_TIME_SIGNUP_PRO_PIC, sendSignupProfilePicToServer);
					}
					else
					{
						HikeSharedPreferenceUtil.getInstance(context).removeData(HikeMessengerApp.SIGNUP_PROFILE_PIC_PATH);
						f.delete();
					}
				}
			};

			HikeHttpRequest profilePicRequest = new HikeHttpRequest("/account/avatar", RequestType.PROFILE_PIC, hikeHttpCallBack);
			profilePicRequest.setFilePath(profilePicPath);
			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hikeHTTPTask, profilePicRequest);
		}
	}
	
	/**
	 * @see http://developer.android.com/reference/android/content/ComponentCallbacks2.html#onTrimMemory(int)
	 */
	@Override
	public void onTrimMemory(int level)
	{
		// TODO Auto-generated method stub
		super.onTrimMemory(level);
		
		Logger.i(TRIM_TAG, "On Trim Memory callback called");
		if (null == HikeMessengerApp.getLruCache())
		{
			return;
		}

		if (level >= TRIM_MEMORY_MODERATE)
		{
			Logger.i(TRIM_TAG, "On Trim Memory callback called with trim memory moderate, releasing all resources");
			HikeMessengerApp.getLruCache().clearIconCache();
		}
		else if (level >= TRIM_MEMORY_BACKGROUND)
		{
			Logger.i(TRIM_TAG, "On Trim Memory callback called with trim memory background, releasing half of resources");
			HikeMessengerApp.getLruCache().trim(HikeLruCache.HALF_SIZE);
		}
	}
	
	/**
	 * @see http://developer.android.com/reference/android/content/ComponentCallbacks.html#onLowMemory()
	 */
	@Override
	public void onLowMemory()
	{
		// TODO Auto-generated method stub
		super.onLowMemory();
		
		Logger.i(TRIM_TAG, "On Low Memory callback called");
		if (null == HikeMessengerApp.getLruCache())
		{
			return;
		}
		Logger.i(TRIM_TAG, "On Low Memory callback evicting all");
		HikeMessengerApp.getLruCache().clearIconCache();
		
	}

}
