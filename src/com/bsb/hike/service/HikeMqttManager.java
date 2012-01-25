package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeToast;
import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttNotConnectedException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

/**
 * @author vr
 * 
 */
public class HikeMqttManager implements MqttSimpleCallback
{
	private HikeService mHikeService;

	// constants used to define MQTT connection status
	public enum MQTTConnectionStatus
	{
		INITIAL, // initial status
		CONNECTING, // attempting to connect
		CONNECTED, // connected
		NOTCONNECTED_WAITINGFORINTERNET, // can't connect because the phone
											// does not have Internet access
		NOTCONNECTED_USERDISCONNECT, // user has explicitly requested
										// disconnection
		NOTCONNECTED_DATADISABLED, // can't connect because the user
									// has disabled data access
		NOTCONNECTED_UNKNOWNREASON // failed to connect for some reason
	}

	// status of MQTT client connection
	private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;

	/************************************************************************/
	/* VARIABLES used to configure MQTT connection */
	/************************************************************************/

	// taken from preferences
	// host name of the server we're receiving push notifications from
	private String brokerHostName = AccountUtils.HOST;

	// defaults - this sample uses very basic defaults for it's interactions
	// with message brokers
	private int brokerPortNumber = 1883;

	private MqttPersistence usePersistence = null;

	private boolean cleanStart = false;

	private int[] qualitiesOfService = { 0 };

	/*
	 * how often should the app ping the server to keep the connection alive?
	 * 
	 * too frequently - and you waste battery life too infrequently - and you wont notice if you lose your connection until the next unsuccessfull attempt to ping // // it's a
	 * trade-off between how time-sensitive the data is that your // app is handling, vs the acceptable impact on battery life // // it is perhaps also worth bearing in mind the
	 * network's support for // long running, idle connections. Ideally, to keep a connection open // you want to use a keep alive value that is less than the period of // time
	 * after which a network operator will kill an idle connection
	 */
	private short keepAliveSeconds = 20 * 60;

	// connection to the message broker
	private IMqttClient mqttClient = null;

	private String clientId;

	private String topic;

	/*
	 * TODO figure out how to send the password
	 */
	private String password;

	private HikeToast toaster;

	private HikeConversationsDatabase convDb;

	public HikeMqttManager(HikeService hikeService)
	{
		this.mHikeService = hikeService;
		this.toaster = new HikeToast(hikeService);
		this.convDb = new HikeConversationsDatabase(hikeService);
		this.createConnectionSpec();
		setConnectionStatus(MQTTConnectionStatus.INITIAL);
	}

	private boolean init()
	{
		SharedPreferences settings = this.mHikeService.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		password = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		topic = settings.getString(HikeMessengerApp.UID_SETTING, null);
		clientId = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);

		return !TextUtils.isEmpty(topic);
	}

	/*
	 * Create a client connection object that defines our connection to a message broker server
	 */
	private void createConnectionSpec()
	{
		String mqttConnSpec = "tcp://" + brokerHostName + "@" + brokerPortNumber;

		try
		{
			// define the connection to the broker
			mqttClient = MqttClient.createMqttClient(mqttConnSpec, usePersistence);

			// register this client app has being able to receive messages
			mqttClient.registerSimpleHandler(this);
		}
		catch (MqttException e)
		{
			// something went wrong!
			mqttClient = null;
			setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);

			//
			// inform the app that we failed to connect so that it can update
			// the UI accordingly
			this.mHikeService.broadcastServiceStatus("Invalid connection parameters");

			//
			// inform the user (for times when the Activity UI isn't running)
			// that we failed to connect
			this.mHikeService.notifyUser("Unable to connect", "MQTT", "Unable to connect");
		}
	}

	/*
	 * (Re-)connect to the message broker
	 */
	private boolean connectToBroker()
	{
		try
		{
			Log.d("HikeMqttManager", "about to call connect " + this.clientId);
			// try to connect

			mqttClient.connect(this.clientId, cleanStart, keepAliveSeconds);

			//
			// inform the app that the app has successfully connected
			this.mHikeService.broadcastServiceStatus("Connected");

			// we are connected
			setConnectionStatus(MQTTConnectionStatus.CONNECTED);

			// we need to wake up the phone's CPU frequently enough so that the
			// keep alive messages can be sent
			// we schedule the first one of these now
			this.mHikeService.scheduleNextPing();

			return true;
		}
		catch (MqttException e)
		{
			Log.e("HikeMqttManager", "Unable to connect", e);
			// something went wrong!

			setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);

			//
			// inform the app that we failed to connect so that it can update
			// the UI accordingly
			this.mHikeService.broadcastServiceStatus("Unable to connect");

			//
			// inform the user (for times when the Activity UI isn't running)
			// that we failed to connect
			this.mHikeService.notifyUser("Unable to connect", "MQTT", "Unable to connect - will retry later");

			// if something has failed, we wait for one keep-alive period before
			// trying again
			// in a real implementation, you would probably want to keep count
			// of how many times you attempt this, and stop trying after a
			// certain number, or length of time - rather than keep trying
			// forever.
			// a failure is often an intermittent network issue, however, so
			// some limited retry is a good idea
			this.mHikeService.scheduleNextPing();

			return false;
		}
	}

	/*
	 * Send a request to the message broker to be sent messages published with the specified topic name. Wildcards are allowed.
	 */
	private void subscribeToTopic(String topicName)
	{
		boolean subscribed = false;

		if (isConnected() == false)
		{
			// quick sanity check - don't try and subscribe if we
			// don't have a connection

			Log.e("mqtt", "Unable to subscribe as we are not connected");
		}
		else
		{
			try
			{
				String[] topics = { topicName };
				mqttClient.subscribe(topics, qualitiesOfService);

				subscribed = true;
			}
			catch (MqttNotConnectedException e)
			{
				Log.e("mqtt", "subscribe failed - MQTT not connected", e);
			}
			catch (IllegalArgumentException e)
			{
				Log.e("mqtt", "subscribe failed - illegal argument", e);
			}
			catch (MqttException e)
			{
				Log.e("mqtt", "subscribe failed - MQTT exception", e);
			}
		}

		if (subscribed == false)
		{
			//
			// inform the app of the failure to subscribe so that the UI can
			// display an error
			this.mHikeService.broadcastServiceStatus("Unable to subscribe");

			//
			// inform the user (for times when the Activity UI isn't running)
			this.mHikeService.notifyUser("Unable to subscribe", "MQTT", "Unable to subscribe");
		}
	}

	/*
	 * Terminates a connection to the message broker.
	 */
	public void disconnectFromBroker()
	{
		try
		{
			if (mqttClient != null)
			{
				mqttClient.disconnect();
			}
		}
		catch (MqttPersistenceException e)
		{
			Log.e("mqtt", "disconnect failed - persistence exception", e);
		}
		finally
		{
			mqttClient = null;
		}
	}

	/*
	 * Checks if the MQTT client thinks it has an active connection
	 */
	public boolean isConnected()
	{
		return ((mqttClient != null) && (mqttClient.isConnected() == true));
	}

	public MQTTConnectionStatus getConnectionStatus()
	{
		return connectionStatus;
	}

	public void setConnectionStatus(MQTTConnectionStatus connectionStatus)
	{
		this.connectionStatus = connectionStatus;
	}

	/************************************************************************/
	/* METHODS - MQTT methods inherited from MQTT classes */
	/************************************************************************/

	/*
	 * callback - method called when we no longer have a connection to the message broker server
	 */
	public void connectionLost() throws Exception
	{
		// we protect against the phone switching off while we're doing this
		// by requesting a wake lock - we request the minimum possible wake
		// lock - just enough to keep the CPU running until we've finished
		PowerManager pm = (PowerManager) this.mHikeService.getSystemService(Context.POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
		try
		{
			wl.acquire();

			//
			// have we lost our data connection?
			//
			if (this.mHikeService.isUserOnline() == false)
			{
				setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);

				// inform the app that we are not connected any more
				this.mHikeService.broadcastServiceStatus("Connection lost - no network connection");

				//
				// inform the user (for times when the Activity UI isn't running)
				// that we are no longer able to receive messages
				this.mHikeService.notifyUser("Connection lost - no network connection", "MQTT", "Connection lost - no network connection");

				//
				// wait until the phone has a network connection again, when we
				// the network connection receiver will fire, and attempt another
				// connection to the broker
			}
			else
			{
				//
				// we are still online
				// the most likely reason for this connectionLost is that we've
				// switched from wifi to cell, or vice versa
				// so we try to reconnect immediately
				//

				setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);

				// inform the app that we are not connected any more, and are
				// attempting to reconnect
				this.mHikeService.broadcastServiceStatus("Connection lost - reconnecting...");

				// try to reconnect
				if (connectToBroker())
				{
					subscribeToTopic(this.topic);
				}
			}
		}
		finally
		{
			// we're finished - if the phone is switched off, it's okay for the CPU
			// to sleep now
			wl.release();

		}
	}

	/*
	 * callback - called when we receive a message from the server
	 */
	public void publishArrived(String topic, byte[] payloadbytes, int qos, boolean retained)
	{
		// we protect against the phone switching off while we're doing this
		// by requesting a wake lock - we request the minimum possible wake
		// lock - just enough to keep the CPU running until we've finished
		PowerManager pm = (PowerManager) this.mHikeService.getSystemService(Context.POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
		try
		{
			wl.acquire();

			//
			// I'm assuming that all messages I receive are being sent as strings
			// this is not an MQTT thing - just me making as assumption about what
			// data I will be receiving - your app doesn't have to send/receive
			// strings - anything that can be sent as bytes is valid
			String messageBody = new String(payloadbytes);

			if (this.mHikeService.sendToApp(messageBody))
			{
				return;
			}

			/*
			 * couldn't send a message to the app if it's a message -- toast and write it now otherwise, just save it in memory until the app connects
			 */

			JSONObject obj = null;
			try
			{
				obj = new JSONObject(messageBody);
			}
			catch (JSONException e)
			{
				Log.e("HikeMqttManager", "Invalid JSON Object", e);
				return;
			}

			if ("message".equals(obj.optString("type")))
			{
				/* toast and save it */
				try
				{
					ConvMessage convMessage = new ConvMessage(obj);
					this.convDb.addConversationMessages(convMessage);
					ContactInfo contactInfo = ContactUtils.getContactInfo(convMessage.getMsisdn(), this.mHikeService);
					toaster.toast(contactInfo, convMessage);
				}
				catch (JSONException e)
				{
					Log.e("JSON", "Invalid JSON", e);
				}
			}
			else
			{
				/* just save it */
				this.mHikeService.storeMessage(messageBody);
			}

			// receiving this message will have kept the connection alive for us, so
			// we take advantage of this to postpone the next scheduled ping
			this.mHikeService.scheduleNextPing();

			// we're finished - if the phone is switched off, it's okay for the CPU
			// to sleep now
		}
		finally
		{
			wl.release();
		}
	}

	public void ping()
	{
		try
		{
			mqttClient.ping();

		}
		catch (MqttException e)
		{
			// if something goes wrong, it should result in connectionLost
			// being called, so we will handle it there
			Log.e("HikeMqttManager", "ping failed - MQTT exception", e);

			// assume the client connection is broken - trash it
			try
			{
				mqttClient.disconnect();
			}
			catch (MqttPersistenceException e1)
			{
				Log.e("HikeMqttManager", "disconnect failed - persistence exception", e1);
			}

			// reconnect
			if (connectToBroker())
			{
				subscribeToTopic(this.topic);
			}
		}
	}

	public void connect()
	{
		Log.d("HikeMqttManager", "connect called");
		if (!init())
		{
			Log.d("HikeMqttManager", "No token yet");
		}

		if (isConnected())
		{
			Log.d("HikeMqttManager", "already connected");
			return;
		}

		if (this.mHikeService.isUserOnline())
		{
			Log.d("HikeMqttManager", "netconnection valid, try to connect");
			// set the status to show we're trying to connect
			setConnectionStatus(MQTTConnectionStatus.CONNECTING);
			if (connectToBroker())
			{
				// we subscribe to a topic - registering to receive push
				// notifications with a particular key
				// in a 'real' app, you might want to subscribe to multiple
				// topics - I'm just subscribing to one as an example
				// note that this topicName could include a wildcard, so
				// even just with one subscription, we could receive
				// messages for multiple topics
				subscribeToTopic(this.topic);
			}
		}
		else
		{
			// we can't do anything now because we don't have a working
			// data connection
			setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);
		}
	}

	public void send(String message)
	{
		try
		{
			mqttClient.publish(topic, message.getBytes(), 1, false);
		}
		catch (MqttNotConnectedException e)
		{
			Log.e("HikeMqttManager", "MQTT Not Connected", e);
		}
		catch (MqttPersistenceException e)
		{
			Log.e("HikeMqttManager", "MQTT Not Connected", e);
		}
		catch (IllegalArgumentException e)
		{
			Log.e("HikeMqttManager", "MQTT Not Connected", e);
		}
		catch (MqttException e)
		{
			Log.e("HikeMqttManager", "MQTT Not Connected", e);
		}
	}
}
