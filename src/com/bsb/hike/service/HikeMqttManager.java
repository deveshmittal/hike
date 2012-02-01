package com.bsb.hike.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeToast;
import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttAdvancedCallback;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttNotConnectedException;
import com.ibm.mqtt.MqttPersistenceException;

/**
 * @author vr
 * 
 */
public class HikeMqttManager implements MqttAdvancedCallback
{
	public class BroadcastFailure implements Runnable
	{
		int mqttId;

		public BroadcastFailure(int mqttId)
		{
			this.mqttId = mqttId;
		}

		@Override
		public void run()
		{
			HikeMqttManager.this.broadcastFailureIfUnsent(mqttId);
		}
	}

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

	private HikeMqttPersistence persistence = null;

	private boolean cleanStart = false;

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
	private HikeMqttClient mqttClient = null;

	private String clientId;

	private String topic;

	/*
	 * TODO figure out how to send the password
	 */
	private String password;

	private HikeToast toaster;

	private HikeConversationsDatabase convDb;

	private Handler handler;

	private Map<Integer, HikePacket> mqttIdToPacket;

	public HikeMqttManager(HikeService hikeService, Handler handler)
	{
		this.mHikeService = hikeService;
		this.toaster = new HikeToast(hikeService);
		this.convDb = new HikeConversationsDatabase(hikeService);
		setConnectionStatus(MQTTConnectionStatus.INITIAL);
		this.handler = handler;
		this.create();
		mqttIdToPacket = Collections.synchronizedMap(new HashMap<Integer, HikePacket>());

		persistence = new HikeMqttPersistence(hikeService);
	}

	public void broadcastFailureIfUnsent(int mqttId)
	{
		HikePacket packet = mqttIdToPacket.remove(mqttId);
		if (packet != null)
		{
			long msgId = packet.getMsgId();
			if (msgId > 0)
			{
				Log.e("HikeMqttManager", "Broadcasting message failure " + msgId);
				this.mHikeService.sendMessageStatus(msgId, false);
			}

			try
			{
				persistence.addSentMessage(mqttId, packet);
			}
			catch (MqttPersistenceException e)
			{
				Log.e("HikeMqttManager", "Unable to persist message");
			}
		}

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
	private void create()
	{
		String mqttConnSpec = "tcp://" + brokerHostName + "@" + brokerPortNumber;

		try
		{
			// define the connection to the broker
			mqttClient = HikeMqttClient.createHikeMqttClient(mqttConnSpec, null, this.handler, this);

			// register this client app has being able to receive messages
			mqttClient.registerAdvancedHandler(this);
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

	private boolean unsubscribeFromTopics(String[] topics)
	{
		if (!isConnected())
		{
			Log.e("HikeMqttManager", "Unable to unsubscribe since we're not connected");
			return false;
		}

		try
		{
			mqttClient.unsubscribe(topics);
		}
		catch (IllegalArgumentException e)
		{
			Log.e("HikeMqttManager", "IllegalArgument trying to unsubscribe", e);
		}
		catch (MqttException e)
		{
			Log.e("HikeMqttManager", "Exception trying to unsubscribe", e);
		}
		return false;
	}

	/*
	 * Send a request to the message broker to be sent messages published with the specified topic name. Wildcards are allowed.
	 */
	private void subscribeToTopics(String[] topics)
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
				int[] qos = new int[topics.length];
				for (int i = 0; i < topics.length; ++i)
				{
					qos[i] = 1;
				}

				mqttClient.subscribe(topics, qos);

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
					subscribeToTopics(getTopics());
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

	private String[] getTopics()
	{
		boolean appConnected = mHikeService.appIsConnected();
		ArrayList<String> topics = new ArrayList<String>(2 + (appConnected ? 0 : 1));
		topics.add(this.topic + HikeConstants.APP_TOPIC);
		topics.add(this.topic + HikeConstants.SERVICE_TOPIC);

		/* only subscribe to UI events if the app is currently connected */
		if (appConnected)
		{
			topics.add(this.topic + HikeConstants.UI_TOPIC);
		}

		return (String[]) topics.toArray(new String[0]);
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
				subscribeToTopics(getTopics());
			}
		}
	}

	public void connect()
	{
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
				subscribeToTopics(getTopics());
			}
		}
		else
		{
			// we can't do anything now because we don't have a working
			// data connection
			setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);
		}
	}

	public void send(byte[] message, long msgId, int qos)
	{
		HikePacket packet = null;
		if (qos > 0)
		{
			packet = new HikePacket(message, msgId);
		}

		try
		{
			int mqttId = mqttClient.publish(this.topic + HikeConstants.PUBLISH_TOPIC, message, qos, false);
			if (packet != null)
			{
				/* store the message ... if we don't get confirmation of sent within a few seconds,
				 * persist it for later.
				 */
				mqttIdToPacket.put(mqttId, packet);
				handler.postDelayed(new BroadcastFailure(mqttId), HikeConstants.MESSAGE_DELIVERY_TIMEOUT);
			}
		}
		catch (MqttNotConnectedException e)
		{
			Log.d("HikeMqttManager", "trying to 'send' but not connected");
			if (packet != null)
			{
				try
				{
					persistence.addSentMessage(mqttClient.getNextMqttId(), packet);
				}
				catch (MqttException e1)
				{
					//only thrown when we have >65k messages in flight.  should never happen
					Log.e("HikeMqttManager", "Unable to allocate messageId", e1);
				}

			}

			if (msgId >= 0)
			{
				this.mHikeService.sendMessageStatus(msgId, false);
			}

			this.connect();
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

	public void unsubscribeFromUIEvents()
	{
		unsubscribeFromTopics(new String[] { this.topic + HikeConstants.UI_TOPIC });
	}

	public void subscribeToUIEvents()
	{
		subscribeToTopics(new String[]{ this.topic + HikeConstants.UI_TOPIC});
	}

	@Override
	public void published(int mqttId)
	{
		HikePacket packet = this.mqttIdToPacket.remove(mqttId);
		if ((packet != null) && (packet.getMsgId() > 0))
		{
			mHikeService.sendMessageStatus(packet.getMsgId(), true);
		}
		else
		{
			/* the failure callback fired, but we actually did send the message.
			 * we should still fire the sendMessageStatus but also clear the message
			 * from the db
			 */
			Log.d("HikeMqttManager", "Published received but no such packet " + mqttId);
			packet = persistence.popMessage(mqttId);
			if ((packet != null) && (packet.getMsgId() > 0))
			{
				mHikeService.sendMessageStatus(packet.getMsgId(), true);
			}
		}
	}

	@Override
	public void subscribed(int arg0, byte[] arg1)
	{
		/* this is a convenient place to determine that we're
		 * connected to the server.  Use a different thread since this is
		 * mqtt's reader thread
		 */
		final List<HikePacket> packets = persistence.getAllSentMessages();
		if (packets.isEmpty())
		{
			return;
		}

		this.handler.post(new Runnable()
		{
			public void run()
			{
				for (HikePacket hikePacket : packets)
				{
					Log.d("HikeMqttManager", "resending message " + new String(hikePacket.getMessage()));
					send(hikePacket.getMessage(), hikePacket.getMsgId(), 1);
				}
			}
		});
	}

	@Override
	public void unsubscribed(int arg0)
	{
		//ignore
	}
}
