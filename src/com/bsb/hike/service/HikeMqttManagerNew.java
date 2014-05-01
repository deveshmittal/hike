package com.bsb.hike.service;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.db.MqttPersistenceException;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeSSLUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * Author : GK
 * 
 * This class handles all the MQTT related stuff. Every operation is non blocking and run on dedicated MQTT Thread. Function 'scheduleConnCheck' checks for connection after certain
 * interval of time. All pings are handled by mqtt paho internally. As soon as you get connected simply reschdule next conn check. In case of no netowrk and SERVER unavailable , we
 * should try and connect on exponential basis.
 * */
public class HikeMqttManagerNew extends BroadcastReceiver
{
	// this variable when true, does not allow mqtt operation such as publish or connect
	// this will become true when you force close or force disconnect mqtt (ex : ssl toggle)
	private boolean forceDisconnect = false;

	private MqttAsyncClient mqtt = null; // main class for handling mqtt connections

	private MqttConnectOptions op = null; // options to set when you connect to mqtt broker

	private Context context;

	private SharedPreferences settings;

	private String brokerHostName;

	private String clientId;

	private String uid;

	private String topic;

	private String password;

	private int brokerPortNumber;

	private volatile AtomicBoolean haveUnsentMessages = new AtomicBoolean(false);

	private int reconnectTime = 0;

	private Looper mMqttHandlerLooper;

	private Handler mqttThreadHandler;

	private MqttCallback mqttCallBack;

	private IMqttActionListener listernerConnect;

	private MqttMessagesManager mqttMessageManager;

	private IsMqttConnectedCheckRunnable isConnRunnable;

	private ConnectionCheckRunnable connChkRunnable;

	private DisconnectRunnable disConnectRunnable;

	private HikeMqttPersistence persistence = null;

	private WakeLock wakelock = null;

	private ConnectivityManager cm;

	private volatile MQTTConnectionStatus mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;

	private Messenger mMessenger; // this is used to interact with the mqtt thread

	private String wakeLockTag = "MQTTWLock"; // Name of the MQTT Wake lock

	// constant used internally to schedule the next ping event
	public static final String MQTT_CONNECTION_CHECK_ACTION = "com.bsb.hike.PING";

	private static final String PRODUCTION_BROKER_HOST_NAME = "mqtt.im.hike.in";

	private static final String STAGING_BROKER_HOST_NAME = AccountUtils.STAGING_HOST;

	private static final int PRODUCTION_BROKER_PORT_NUMBER = 8080;

	private static final int PRODUCTION_BROKER_PORT_NUMBER_SSL = 443;

	private static final int STAGING_BROKER_PORT_NUMBER = 1883;

	private static final int STAGING_BROKER_PORT_NUMBER_SSL = 8883;

	// this represents number of msgs published whose callback is not yet arrived
	private short MAX_INFLIGHT_MESSAGES_ALLOWED = 100;

	private short keepAliveSeconds = HikeConstants.KEEP_ALIVE; // this is the time for which conn will remain open w/o messages

	private static short connectionTimeoutSec = 60;
	
	private static volatile AtomicBoolean ipsChanged = new AtomicBoolean(false);

	/*
	 * When disconnecting (forcibly) it might happen that some messages are waiting for acks or delivery. So before disconnecting,wait for this time to let mqtt finish the work and
	 * then disconnect w/o letting more msgs to come in.
	 */
	private short quiesceTime = 500;

	private static final String TAG = "HikeMqttManagerNew";

	// constants used to define MQTT connection status, this is used by external classes and hardly of any use internally
	public enum MQTTConnectionStatus
	{
		NOT_CONNECTED, // initial status
		CONNECTING, // attempting to connect
		CONNECTED, // connected
		NOT_CONNECTED_UNKNOWN_REASON // failed to connect for some reason
	}

	public enum ServerConnectionStatus
	{
		ACCEPTED, UNACCEPTABLE_PROTOCOL_VERSION, IDENTIFIER_REJECTED, SERVER_UNAVAILABLE, BAD_USERNAME_OR_PASSWORD, NOT_AUTHORIZED, UNKNOWN
	}

	private class IsMqttConnectedCheckRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if (!isConnected())
			{
				HikeMessengerApp.networkError = true;
				HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_NETWORK_STATE, null);
			}
		}
	}

	// this is used to check and connect mqtt and will be run on MQTT thread
	private class ConnectionCheckRunnable implements Runnable
	{
		private long sleepTime = 0;

		public void setSleepTime(long t)
		{
			sleepTime = t;
		}

		@Override
		public void run()
		{
			if (sleepTime > 0)
			{
				try
				{
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				sleepTime = 0;
			}
			connect();
		}
	}

	// this is used to disconnect from mqtt broker and will be run on MQTT thread
	private class DisconnectRunnable implements Runnable
	{
		private boolean reconnect = true;

		public void setReconnect(boolean isReconnect)
		{
			reconnect = isReconnect;
		}

		@Override
		public void run()
		{
			disconnect(reconnect);
			reconnect = true; // resetting value after run
		}
	}

	private final class RetryFailedMessages implements Runnable
	{
		public void run()
		{
			final List<HikePacket> packets = persistence.getAllSentMessages();
			Logger.w(TAG, "Retrying to send " + packets.size() + " messages");
			for (HikePacket hikePacket : packets)
			{
				Logger.d(TAG, "Resending message " + new String(hikePacket.getMessage()));
				send(hikePacket, 1);
				try
				{
					// always give some time for network call to complete
					// lopping this way could exceed the memory too as GC will block
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			haveUnsentMessages.set(false);
		}
	}

	class IncomingHandler extends Handler
	{
		public IncomingHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg)
		{
			try
			{
				switch (msg.what)
				{
				case HikeService.MSG_APP_PUBLISH:
					Bundle bundle = msg.getData();
					String message = bundle.getString(HikeConstants.MESSAGE);
					long msgId = bundle.getLong(HikeConstants.MESSAGE_ID, -1);
					send(new HikePacket(message.getBytes(), msgId, System.currentTimeMillis()), msg.arg1);
					break;
				case 12341: // just for testing
					Bundle b = msg.getData();
					String m = b.getString(HikeConstants.MESSAGE);
					long mId = b.getLong(HikeConstants.MESSAGE_ID, -1);
					send(new HikePacket(m.getBytes(), mId, System.currentTimeMillis()), msg.arg1);
					break;
				}
			}
			catch (Exception e)
			{
				Logger.e(TAG, "Exception", e);
			}
		}
	}

	public HikeMqttManagerNew(Context ctx)
	{
		context = ctx;
		cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		password = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		topic = uid = settings.getString(HikeMessengerApp.UID_SETTING, null);
		clientId = settings.getString(HikeMessengerApp.MSISDN_SETTING, null) + ":" + HikeConstants.APP_API_VERSION + ":" + true;

		persistence = HikeMqttPersistence.getInstance();
		mqttMessageManager = MqttMessagesManager.getInstance(context);
		setBrokerHostPort(Utils.switchSSLOn(context));
		isConnRunnable = new IsMqttConnectedCheckRunnable();
		connChkRunnable = new ConnectionCheckRunnable();
		disConnectRunnable = new DisconnectRunnable();
	}

	/*
	 * This method should be used after creating this object. Note : Functions involving 'this' reference and Threads should not be used or started in constructor as it might
	 * happen that incomplete 'this' object creation took place till that time.
	 */
	public void init()
	{
		HandlerThread mqttHandlerThread = new HandlerThread("MQTT_Thread");
		mqttHandlerThread.start();
		mMqttHandlerLooper = mqttHandlerThread.getLooper();
		mqttThreadHandler = new Handler(mMqttHandlerLooper);
		mMessenger = new Messenger(new IncomingHandler(mMqttHandlerLooper));
		// register for Screen ON, Network Connection Change
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(MQTT_CONNECTION_CHECK_ACTION);
		filter.addAction(HikePubSub.SSL_PREFERENCE_CHANGED);
		context.registerReceiver(this, filter);
		LocalBroadcastManager.getInstance(context).registerReceiver(this, filter);
		// mqttThreadHandler.postDelayed(new TestOutmsgs(), 15 * 1000); // this is just for testing
	}

	private boolean isNetworkAvailable()
	{
		if (context == null)
		{
			Logger.e(TAG, "Hike service is null!!");
			return false;
		}
		/*
		 * We've seen NPEs in this method on the dev console but have not been able to figure out the reason so putting this in a try catch block.
		 */
		try
		{
			return (cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected());
		}
		catch (NullPointerException e)
		{
			return false;
		}
	}

	private void setBrokerHostPort(boolean ssl)
	{
		Logger.d("SSL", "Switching broker port/host. SSL? " + ssl);
		String brokerHost = settings.getString(HikeMessengerApp.BROKER_HOST, "");

		/*
		 * If we set a custom broker host we set those values.
		 */
		if (!TextUtils.isEmpty(brokerHost))
		{
			brokerHostName = brokerHost;
			brokerPortNumber = settings.getInt(HikeMessengerApp.BROKER_PORT, 8080);
			return;
		}

		boolean production = settings.getBoolean(HikeMessengerApp.PRODUCTION, true);

		brokerHostName = production ? PRODUCTION_BROKER_HOST_NAME : STAGING_BROKER_HOST_NAME;

		brokerPortNumber = production ? (ssl ? PRODUCTION_BROKER_PORT_NUMBER_SSL : PRODUCTION_BROKER_PORT_NUMBER) : (ssl ? STAGING_BROKER_PORT_NUMBER_SSL
				: STAGING_BROKER_PORT_NUMBER);

		Logger.d(TAG, "Broker host name: " + brokerHostName);
		Logger.d(TAG, "Broker port: " + brokerPortNumber);
	}

	public void finish()
	{
		context.unregisterReceiver(this);
		this.mqttMessageManager.close();
	}

	// this function works on exponential retrying
	private int getConnRetryTime()
	{
		if (reconnectTime == 0)
		{
			Random random = new Random();
			reconnectTime = random.nextInt(HikeConstants.RECONNECT_TIME) + 1;
		}
		else
		{
			reconnectTime *= 2;
		}
		reconnectTime = reconnectTime > HikeConstants.MAX_RECONNECT_TIME ? HikeConstants.MAX_RECONNECT_TIME : reconnectTime;
		return reconnectTime;
	}

	private void printThreadInfo(String obj)
	{
		Long id = Thread.currentThread().getId();
		String thName = Thread.currentThread().getName();
		Logger.d(TAG, obj + " is running on thread : " + thName + " id : " + id);
	}

	// delete the token and send a message to the app to send the user back to the main screen
	private void clearSettings()
	{
		Logger.e(TAG, "Invalid account credentials, so clear settings and move to welcome screen.");
		SharedPreferences.Editor editor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.clear();
		editor.commit();
	}

	public Messenger getMessenger()
	{
		return mMessenger;
	}

	private ServerConnectionStatus getServerStatusCode(String msg)
	{
		if (msg.contains(ServerConnectionStatus.ACCEPTED.toString()))
			return ServerConnectionStatus.ACCEPTED;
		else if (msg.contains(ServerConnectionStatus.BAD_USERNAME_OR_PASSWORD.toString()))
			return ServerConnectionStatus.BAD_USERNAME_OR_PASSWORD;
		else if (msg.contains(ServerConnectionStatus.IDENTIFIER_REJECTED.toString()))
			return ServerConnectionStatus.IDENTIFIER_REJECTED;
		else if (msg.contains(ServerConnectionStatus.NOT_AUTHORIZED.toString()))
			return ServerConnectionStatus.NOT_AUTHORIZED;
		else if (msg.contains(ServerConnectionStatus.SERVER_UNAVAILABLE.toString()))
			return ServerConnectionStatus.SERVER_UNAVAILABLE;
		else if (msg.contains(ServerConnectionStatus.UNACCEPTABLE_PROTOCOL_VERSION.toString()))
			return ServerConnectionStatus.UNACCEPTABLE_PROTOCOL_VERSION;
		else
			return ServerConnectionStatus.UNKNOWN;
	}

	private void acquireWakeLock()
	{
		if (wakelock == null)
		{
			PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
			wakelock.setReferenceCounted(false);
		}
		wakelock.acquire();
	}

	/**
	 * This function will be used when wakeLock is taken during connect as
	 * 
	 * timeout : seconds
	 */
	private void acquireWakeLock(int timeout)
	{
		if (timeout > 0)
		{
			if (wakelock == null)
			{
				PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
				wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
				wakelock.setReferenceCounted(false);
			}
			wakelock.acquire(timeout * 1000);
		}
		else
			acquireWakeLock();
		Logger.d(TAG, "Wakelock Acquired");
	}

	private void releaseWakeLock()
	{
		if (wakelock != null && wakelock.isHeld())
		{
			wakelock.release();
			Logger.d(TAG, "Wakelock Released");
		}
	}

	private void scheduleNextConnectionCheck()
	{
		scheduleNextConnectionCheck(HikeConstants.MAX_RECONNECT_TIME);
	}

	private void scheduleNextConnectionCheck(int reconnectTime)
	{
		try
		{
			mqttThreadHandler.removeCallbacks(connChkRunnable);
			mqttThreadHandler.postDelayed(connChkRunnable, reconnectTime * 1000);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private boolean isConnected()
	{
		if (mqtt == null)
			return false;
		if (!mqtt.isConnected())
		{
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED_UNKNOWN_REASON;
			return false;
		}
		return mqtt.isConnected();
	}

	// This function should be called always from external classes inorder to run connect on MQTT thread
	public void connectOnMqttThread()
	{
		try
		{
			connectOnMqttThread(10);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in MQTT connect handler: " + e.getMessage());
		}
	}

	private void connectOnMqttThread(long t)
	{
		try
		{
			// make MQTT thread wait for t ms to attempt reconnect
			connChkRunnable.setSleepTime(t);
			mqttThreadHandler.postAtFrontOfQueue(connChkRunnable);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in MQTT connect handler: " + e.getMessage());
		}
	}

	private void scheduleNetworkErrorTimer()
	{
		if (HikeMessengerApp.networkError == true)
			return;

		if (isAirplaneModeOn(context))
		{
			HikeMessengerApp.networkError = true;
			HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_NETWORK_STATE, null);
			return;
		}
		mqttThreadHandler.postDelayed(isConnRunnable, HikeConstants.NETWORK_ERROR_POP_UP_TIME);
	}

	private void cancelNetworkErrorTimer()
	{
		mqttThreadHandler.removeCallbacks(isConnRunnable);
		if (HikeMessengerApp.networkError == false)
			return;
		HikeMessengerApp.networkError = false;
		HikeMessengerApp.getPubSub().publish(HikePubSub.UPDATE_NETWORK_STATE, null);
	}

	private static boolean isAirplaneModeOn(Context context)
	{
		return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
	}

	// this should and will run only on MQTT thread so no need to synchronize it explicitly
	private void connect()
	{
		try
		{
			if (!isNetworkAvailable())
			{
				Logger.d(TAG, "No Network Connection so should not connect");
				return;
			}

			// if force disconnect is in progress don't connect
			if (forceDisconnect)
				return;

			boolean connectUsingSSL = Utils.switchSSLOn(context);
			
			if (op == null || ipsChanged.get())
			{
				op = new MqttConnectOptions();
				op.setUserName(uid);
				op.setPassword(password.toCharArray());
				op.setCleanSession(false);
				op.setKeepAliveInterval((short) keepAliveSeconds);
				op.setConnectionTimeout(connectionTimeoutSec);
				setServerUris(op);
				if (connectUsingSSL)
					op.setSocketFactory(HikeSSLUtil.getSSLSocketFactory());
				
				if(ipsChanged.get())
				{
					ipsChanged.set(false);
				}
			}

			if (mqtt == null)
			{
				String protocol = connectUsingSSL ? "ssl://" : "tcp://";

				// Here I am using my modified MQTT PAHO library
				mqtt = new MqttAsyncClient(protocol + brokerHostName + ":" + brokerPortNumber, clientId, null, MAX_INFLIGHT_MESSAGES_ALLOWED);
				mqtt.setCallback(getMqttCallback());
				Logger.d(TAG, "Number of max inflight msgs allowed : " + mqtt.getMaxflightMessages());
			}
			if (isConnected())
				return;

			mqttConnStatus = MQTTConnectionStatus.CONNECTING;
			// if any network is available, then only connect, else connect at next check or when network gets available
			if (isNetworkAvailable())
			{
				acquireWakeLock(connectionTimeoutSec);
				Logger.d(TAG, "MQTT connecting on : " + mqtt.getServerURI());
				mqtt.connect(op, null, getConnectListener());
			}
			else
			{
				mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
				scheduleNextConnectionCheck(getConnRetryTime()); // exponential retry incase of no network
			}
		}
		catch (MqttSecurityException e)
		{
			e.printStackTrace();
			handleMqttException(e, false);
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED_UNKNOWN_REASON;
			releaseWakeLock();
		}
		catch (MqttException e)
		{
			Logger.e(TAG, "Connect exception : " + e.getReasonCode());
			handleMqttException(e, true);
			releaseWakeLock();
		}
		catch (Exception e) // this exception cannot be thrown on connect
		{
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED_UNKNOWN_REASON;
			Logger.e(TAG, "Connect exception : " + e.getMessage());
			e.printStackTrace();
			scheduleNextConnectionCheck();
			releaseWakeLock();
		}
	}
	
	private void setServerUris(MqttConnectOptions op)
	{
		String protocol = Utils.switchSSLOn(context) ? "ssl://" : "tcp://";

		SharedPreferences pref = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String ipString = pref.getString(HikeMessengerApp.MQTT_IPS, "");
		JSONArray ipArray = null;
		
		try
		{
			ipArray = new JSONArray(ipString);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		if (null != ipArray && ipArray.length() > 0)
		{
			String serverURIs[] = new String[ipArray.length() + 1];
			int len  = ipArray.length();
			
			serverURIs[0] = protocol + brokerHostName + ":" + brokerPortNumber;
			for (int i = 0; i < len; i++)
			{
				serverURIs[i + 1] = protocol + ipArray.optString(i) + ":" + brokerPortNumber;
			}
			
			op.setServerURIs(serverURIs);
		} 
		else
		{
			
			String serverURIs[] = 
				{ 
					protocol + brokerHostName + ":" + brokerPortNumber,
					protocol + "54.251.180.0" + ":" + brokerPortNumber, 
					protocol + "54.251.180.1" + ":" + brokerPortNumber, 
					protocol + "54.251.180.2" + ":" + brokerPortNumber,
					protocol + "54.251.180.3" + ":" + brokerPortNumber, 
					protocol + "54.251.180.4" + ":" + brokerPortNumber, 
					protocol + "54.251.180.5" + ":" + brokerPortNumber,
					protocol + "54.251.180.6" + ":" + brokerPortNumber
				};
			
			op.setServerURIs(serverURIs);
		}

	}
	

	// This function should be called always from external classes inorder to run connect on MQTT thread
	public void disconnectOnMqttThread(final boolean reconnect)
	{
		try
		{
			disConnectRunnable.setReconnect(reconnect);
			mqttThreadHandler.removeCallbacks(disConnectRunnable); // remove any pending disconnects queued
			mqttThreadHandler.removeCallbacks(connChkRunnable); // remove any pending connects queued
			mqttThreadHandler.postAtFrontOfQueue(disConnectRunnable);
		}
		catch (Exception e)
		{
			Logger.e(TAG, "Exception in MQTT connect : " + e.getMessage());
		}
	}

	private void disconnect(final boolean reconnect)
	{
		try
		{
			if (mqtt != null)
			{
				forceDisconnect = true;
				IMqttToken t = mqtt.disconnect(quiesceTime, null, new IMqttActionListener()
				{
					@Override
					public void onSuccess(IMqttToken arg0)
					{
						Logger.d(TAG, "Explicit Disconnection success");
						handleDisconnect(reconnect);
					}

					@Override
					public void onFailure(IMqttToken arg0, Throwable arg1)
					{
						Logger.e(TAG, "Explicit Disconnection failed", arg1);
						// dont care about failure and move on as you have to connect anyways
						handleDisconnect(reconnect);
					}
				});
				/*
				 * blocking the mqtt thread, so that no other operation takes place till disconnects completes or timeout This will wait for max 5 secs
				 */
				t.waitForCompletion(2 * quiesceTime);
			}
		}
		catch (MqttException e)
		{
			// we dont need to handle MQTT exception here as we reconnect depends on reconnect var
			e.printStackTrace();
			handleDisconnect(reconnect);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			handleDisconnect(reconnect);
		}
	}

	private void handleDisconnect(boolean reconnect)
	{
		forceDisconnect = false;
		try
		{
			mqtt.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		mqtt = null;
		op = null;
		mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
		if (reconnect)
			connectOnMqttThread(10); // try reconnection after 10 ms
		else
		{
			try
			{
				// if you dont want to reconnect simply remove all connection check runnables
				mqttThreadHandler.removeCallbacks(connChkRunnable);
			}
			catch (Exception e)
			{

			}
		}
	}

	/* Listeners for conection */
	private IMqttActionListener getConnectListener()
	{
		if (listernerConnect == null)
		{
			listernerConnect = new IMqttActionListener()
			{
				@Override
				public void onSuccess(IMqttToken arg0)
				{
					reconnectTime = 0; // resetting the reconnect timer to 0 as it would have been changed in failure
					mqttConnStatus = MQTTConnectionStatus.CONNECTED;
					Logger.d(TAG, "Client Connected ....");
					cancelNetworkErrorTimer();

					HikeMessengerApp.getPubSub().publish(HikePubSub.CONNECTED_TO_MQTT, null);

					mqttThreadHandler.postAtFrontOfQueue(new RetryFailedMessages());
					try
					{
						/*
						 * String[] topics = new String[3]; topics[0] = uid + "/s"; topics[1] = uid + "/a"; topics[2] = uid + "/u"; int[] qos = new int[] { 1, 1, 1 };
						 * mqtt.subscribe(topics, qos).setActionCallback(new IMqttActionListener() {
						 * 
						 * @Override public void onSuccess(IMqttToken arg0) { Logger.d(TAG, "Successfully subscribed to topics."); }
						 * 
						 * @Override public void onFailure(IMqttToken arg0, Throwable arg1) { Logger.e(TAG, "Error subscribing to topics : " + arg1.getMessage()); } });
						 */
						scheduleNextConnectionCheck(); // after successfull connect, reschedule for next conn check
					}

					/*
					 * catch (MqttException e) { handleMqttException(e, true); }
					 */
					catch (Exception e) // although this might not happen , but still catching it
					{
						e.printStackTrace();
						scheduleNextConnectionCheck();
					}
					releaseWakeLock();
				}

				@Override
				public void onFailure(IMqttToken arg0, Throwable value)
				{
					MqttException ex = arg0.getException();
					if (ex != null)
						Logger.e(TAG, "Exception : " + ex.getReasonCode());
					ServerConnectionStatus connectionStatus = ServerConnectionStatus.UNKNOWN;
					scheduleNetworkErrorTimer();
					if (value != null)
					{
						Logger.e(TAG, "Connection failed : " + value.getMessage());
						String msg = value.getMessage();
						Logger.e("(TAG", "Hike Unable to connect", value);
						connectionStatus = getServerStatusCode(msg);

						if (connectionStatus == ServerConnectionStatus.BAD_USERNAME_OR_PASSWORD || connectionStatus == ServerConnectionStatus.IDENTIFIER_REJECTED
								|| connectionStatus == ServerConnectionStatus.NOT_AUTHORIZED)
						{
							clearSettings();
						}

					}
					mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED_UNKNOWN_REASON;

					/*
					 * if something has failed, we wait for one keep-alive period before trying again in a real implementation, you would probably want to keep count of how many
					 * times you attempt this, and stop trying after a certain number, or length of time - rather than keep trying forever. a failure is often an intermittent
					 * network issue, however, so some limited retry is a good idea
					 */
					if (connectionStatus != ServerConnectionStatus.SERVER_UNAVAILABLE)
					{
						int reConnTime = getConnRetryTime();
						Logger.d(TAG, "Reconnect time (sec): " + reConnTime);
						scheduleNextConnectionCheck(reConnTime);
					}
					else
					{
						Random random = new Random();
						int reconnectIn = random.nextInt(HikeConstants.SERVER_UNAVAILABLE_MAX_CONNECT_TIME) + 1;
						scheduleNextConnectionCheck(reconnectIn * 60); // Converting minutes to seconds
					}
					releaseWakeLock();
				}
			};
		}
		return listernerConnect;
	}

	/* This call back will be called when message is arrived */
	private MqttCallback getMqttCallback()
	{
		if (mqttCallBack == null)
		{
			mqttCallBack = new MqttCallback()
			{
				@Override
				public void messageArrived(String arg0, MqttMessage arg1) throws Exception
				{
					try
					{
						cancelNetworkErrorTimer();
						String messageBody = new String(arg1.getPayload(), "UTF-8");
						Logger.i(TAG, "messageArrived called " + messageBody);
						JSONObject jsonObj = new JSONObject(messageBody);
						mqttMessageManager.saveMqttMessage(jsonObj);
					}
					catch (JSONException e)
					{
						Logger.e(TAG, "invalid JSON message", e);
					}
					catch (Exception e)
					{
						Logger.e(TAG, "Exception when msg arrived : ", e);
					}
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken arg0)
				{
					// nothing needs to be done here as success will get called eventually
				}

				@Override
				public void connectionLost(Throwable arg0)
				{
					Logger.w(TAG, "Connection Lost : " + arg0.getMessage());
					scheduleNetworkErrorTimer();
					connectOnMqttThread();
				}
			};
		}
		return mqttCallBack;
	}

	// this should always run on MQTT Thread
	public void send(HikePacket packet, int qos)
	{
		/* only care about failures for messages we care about. */
		if (qos > 0 && packet.getPacketId() == -1)
		{
			try
			{
				persistence.addSentMessage(packet);
			}
			catch (MqttPersistenceException e)
			{
				Logger.e(TAG, "Unable to persist message", e);
			}
			catch (Exception e)
			{
				Logger.e(TAG, "Unable to persist message", e);
			}
		}

		// if force disconnect is in progress dont allow mqtt operations to take place
		if (forceDisconnect)
			return;

		if (!isConnected())
		{
			connect();
			return;
		}

		Logger.d(TAG, "About to send message " + new String(packet.getMessage()));
		try
		{
			Logger.d(TAG, "Current inflight msg count : " + mqtt.getInflightMessages());
			while (mqtt.getInflightMessages() + 1 >= mqtt.getMaxflightMessages())
			{
				try
				{
					Logger.w(TAG, String.format("Inflight msgs : %d , MaxInflight count : %d .... Waiting for sometime", mqtt.getInflightMessages(), mqtt.getMaxflightMessages()));
					Thread.sleep(30);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			mqtt.publish(this.topic + HikeConstants.PUBLISH_TOPIC, packet.getMessage(), qos, false, packet, new IMqttActionListener()
			{
				@Override
				public void onSuccess(IMqttToken arg0)
				{
					try
					{
						cancelNetworkErrorTimer();
						HikePacket packet = (HikePacket) arg0.getUserContext();
						if (packet != null)
						{
							persistence.removeMessageForPacketId(packet.getPacketId());
							if (packet.getMsgId() > 0)
							{
								Long msgId = packet.getMsgId();
								Logger.d(TAG, "Recieved S status for msg with id : " + msgId);
								HikeMessengerApp.getPubSub().publish(HikePubSub.SERVER_RECEIVED_MSG, msgId);
							}
						}
						if (haveUnsentMessages.get())
						{
							mqttThreadHandler.postAtFrontOfQueue(new RetryFailedMessages());
						}
					}
					catch (Exception e)
					{
						Logger.e(TAG, "Exception in publish success : " + e.getMessage());
						e.printStackTrace();
					}
				}

				@Override
				public void onFailure(IMqttToken arg0, Throwable arg1)
				{
					Logger.e(TAG, "Message delivery failed for : " + arg0.getMessageId() + ", exception : " + arg1.getMessage());
					haveUnsentMessages.set(true);
					connectOnMqttThread();
				}
			});
		}
		catch (org.eclipse.paho.client.mqttv3.MqttPersistenceException e)
		{
			e.printStackTrace();
			haveUnsentMessages.set(true);
		}
		catch (MqttException e)
		{
			haveUnsentMessages.set(true);
			handleMqttException(e, true);
		}
		catch (Exception e)
		{
			// this might happen if mqtt object becomes null while disconnect, so just ignore
		}
	}

	private void handleMqttException(MqttException e, boolean reConnect)
	{
		switch (e.getReasonCode())
		{
		case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
			Logger.e(TAG, "Server Unavailable, try reconnecting later");
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			scheduleNextConnectionCheck(getConnRetryTime());// exponential retry for connection
			break;
		case MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED:
			Logger.e(TAG, "Client already disconnected.");
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			if (reConnect)
				connectOnMqttThread();
			break;
		case MqttException.REASON_CODE_CLIENT_CLOSED:
			// this will happen only when you close the conn, so dont do any thing
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			break;
		case MqttException.REASON_CODE_CLIENT_CONNECTED:
			mqttConnStatus = MQTTConnectionStatus.CONNECTED;
			break;
		case MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED:
			// Thrown when an attempt to call MqttClient.disconnect() has been made from within a method on MqttCallback.
			break;
		case MqttException.REASON_CODE_CLIENT_DISCONNECTING:
			if (reConnect)
				scheduleNextConnectionCheck(1); // try reconnect after 1 sec, so that disconnect happens properly
			break;
		case MqttException.REASON_CODE_CLIENT_EXCEPTION:
			Logger.e(TAG, "Exception : " + e.getCause().getMessage());
			// Till this point disconnect has already happened due to exception (This is as per lib)
			if (reConnect)
				connectOnMqttThread(20);
			break;
		case MqttException.REASON_CODE_CLIENT_NOT_CONNECTED:
			Logger.e(TAG, "Client not connected retry connection");
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			if (reConnect)
				connectOnMqttThread();
			break;
		case MqttException.REASON_CODE_CLIENT_TIMEOUT:
			// Till this point disconnect has already happened. This could happen in PING or other TIMEOUT happen such as CONNECT, DISCONNECT
			if (reConnect)
				connectOnMqttThread();
			break;
		case MqttException.REASON_CODE_CONNECT_IN_PROGRESS:
			Logger.e(TAG, "Client already in connecting state");
			break;
		case MqttException.REASON_CODE_CONNECTION_LOST:
			Logger.e(TAG, "Client not connected retry connection");
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			if (reConnect)
				connectOnMqttThread();
			break;
		case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
			clearSettings();
			break;
		case MqttException.REASON_CODE_INVALID_CLIENT_ID:
			clearSettings();
			break;
		case MqttException.REASON_CODE_INVALID_MESSAGE:
			// simply ignore as message is invalid
			// Remove the corrupt packet from DB(ALL)
			break;
		case MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION:
			clearSettings();
			break;
		case MqttException.REASON_CODE_MAX_INFLIGHT:
			Logger.e(TAG, "There are already to many messages in publish. Exception : " + e.getMessage());
			break;
		case MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE:
			// simply ignore as message is invalid due to no msgIds
			break;
		case MqttException.REASON_CODE_NOT_AUTHORIZED:
			clearSettings();
			break;
		case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
			scheduleNextConnectionCheck(getConnRetryTime());// exponential handling
			break;
		case MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH:
			clearSettings();
			break;
		case MqttException.REASON_CODE_SSL_CONFIG_ERROR:
			clearSettings();
			break;
		case MqttException.REASON_CODE_TOKEN_INUSE:
			clearSettings();
			break;
		case MqttException.REASON_CODE_UNEXPECTED_ERROR:
			// This could happen while reading or writing error on a socket, hence disconnection happens
			connectOnMqttThread(20);
			break;
		default:
			mqttConnStatus = MQTTConnectionStatus.NOT_CONNECTED;
			connectOnMqttThread();
			break;
		}
		Logger.e(TAG, "Exception : " + e.getMessage());
		e.printStackTrace();
	}

	public void destroyMqtt()
	{
		try
		{
			short retryAttempts = 0;
			Logger.w(TAG, "Destroying mqtt connection.");
			context.unregisterReceiver(this);
			LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
			disconnectOnMqttThread(false);
			// here we are blocking service main thread for 1 second or less so that disconnection takes place cleanly
			while (mqttConnStatus != MQTTConnectionStatus.NOT_CONNECTED || mqttConnStatus != MQTTConnectionStatus.NOT_CONNECTED_UNKNOWN_REASON && retryAttempts <= 100)
			{
				Thread.sleep(10);
				retryAttempts++;
			}
			if (mMqttHandlerLooper != null)
			{
				if (Utils.hasKitKat())
					mMqttHandlerLooper.quitSafely();
				else
					mMqttHandlerLooper.quit();
			}
			mqttMessageManager.close();
			Logger.w(TAG, "Mqtt connection destroyed.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// This will be called on for Broadcast events fired by system
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
		{
			if (isNetworkAvailable())
				connectOnMqttThread();
		}
		else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			boolean isNetwork = isNetworkAvailable();
			Logger.d(TAG, "Network change event happened. Network connected : " + isNetwork);
			if (isNetwork)
			{
				boolean shouldConnectUsingSSL = Utils.switchSSLOn(context);
				setBrokerHostPort(shouldConnectUsingSSL);
				boolean isSSLConnected = isSSLAlreadyOn();
				// reconnect using SSL as currently not connected using SSL
				if (shouldConnectUsingSSL && !isSSLConnected)
					disconnectOnMqttThread(true);
				// reconnect using nonSSL but currently connected using SSL
				else if (!shouldConnectUsingSSL && isSSLConnected)
					disconnectOnMqttThread(true);
				else
					connectOnMqttThread();
				Utils.setupUri(context); // TODO : this should be moved out from here to some other place
			}
		}
		else if (intent.getAction().equals(MQTT_CONNECTION_CHECK_ACTION))
		{
			Logger.d(TAG, "Connection check happened from GCM");
			connectOnMqttThread();
		}
		else if (intent.getAction().equals(HikePubSub.SSL_PREFERENCE_CHANGED))
		{
			/*
			 * ssl settings toggled so disconnect and reconnect mqtt
			 */
			boolean shouldConnectUsingSSL = Utils.switchSSLOn(context);
			setBrokerHostPort(shouldConnectUsingSSL);
			boolean isSSLConnected = isSSLAlreadyOn();
			Logger.d(TAG, "SSL Preference has changed. OnSSL : " + shouldConnectUsingSSL + " ,isSSLAlreadyOn : " + isSSLConnected);
			// reconnect using SSL as currently not connected using SSL
			if (shouldConnectUsingSSL && !isSSLConnected)
				disconnectOnMqttThread(true);
			// reconnect using nonSSL but currently connected using SSL
			else if (!shouldConnectUsingSSL && isSSLConnected)
				disconnectOnMqttThread(true);
		}
	}

	private boolean isSSLAlreadyOn()
	{
		if (mqtt != null)
		{
			String uri = mqtt.getServerURI();
			if (uri != null && uri.startsWith("ssl"))
				return true;
		}
		return false;
	}
	
	public static void setIpsChanged(boolean isIpsChnaged){
		ipsChanged.set(isIpsChnaged);
	}

	// This class is just for testing .....
	public class TestOutmsgs implements Runnable
	{
		@Override
		public void run()
		{
			Logger.w(TAG, "Starting testing thread .....");
			Thread t = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					int count = 0;
					for (int i = 0; i < 1000; i++)
					{
						count++;
						String data = String.format("{\"t\": \"m\",\"to\": \"+918826670738\",\"d\":{\"hm\":\"%d\",\"i\":%d, \"ts\":%d}}", count + 10, count,
								System.currentTimeMillis());
						Logger.d(TAG, "Sending msg : " + data);
						Message msg = Message.obtain();
						msg.what = 12341;
						Bundle bundle = new Bundle();
						bundle.putString(HikeConstants.MESSAGE, data);
						msg.setData(bundle);
						msg.replyTo = mMessenger;
						try
						{
							mMessenger.send(msg);
							Thread.sleep(20);
						}
						catch (RemoteException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						catch (InterruptedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						catch (Exception e)
						{

						}
					}

				}
			});
			t.setName("Test Thread");
			t.start();
		}
	}

}