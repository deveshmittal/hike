package com.bsb.hike.service;

import java.net.URISyntaxException;
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
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.db.MqttPersistenceException;
import com.bsb.hike.models.HikePacket;
import com.bsb.hike.mqtt.client.Buffer;
import com.bsb.hike.mqtt.client.Callback;
import com.bsb.hike.mqtt.client.CallbackConnection;
import com.bsb.hike.mqtt.client.ConnectionException;
import com.bsb.hike.mqtt.client.Listener;
import com.bsb.hike.mqtt.client.MQTT;
import com.bsb.hike.mqtt.client.UTF8Buffer;
import com.bsb.hike.mqtt.msg.ConnAckMessage.ConnectionStatus;
import com.bsb.hike.mqtt.msg.QoS;
import com.bsb.hike.pubsub.Topic;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.Utils;

/**
 * @author vr
 * 
 */
public class HikeMqttManager implements Listener
{
	private final class RetryFailedMessages implements Runnable {
        public void run(){
        	final List<HikePacket> packets = persistence.getAllSentMessages();
            HikeMqttManager.this.haveUnsentMessages = false;
        	for (HikePacket hikePacket : packets)
        	{
        		Log.d("HikeMqttManager", "resending message " + new String(hikePacket.getMessage()));
        		send(hikePacket, 1);
        	}
        }
    }

    public class DisconnectCB implements Callback<Void>
	{

		private boolean reconnect;

		public DisconnectCB(boolean reconnect)
		{
			this.reconnect = reconnect;
		}

		@Override
		public void onSuccess(Void value)
		{
			Log.d("HikeMqttManager", "Sucessfully disconnected : "+reconnect);
			if (mqttConnection != null)
			{
				mqttConnection.listener(CallbackConnection.DEFAULT_LISTENER);
			}

			setConnectionStatus(HikeMqttManager.MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
			if (reconnect)
			{
				connectToBroker();
			}
		}

		@Override
		public void onFailure(Throwable value)
		{
			Log.d("HikeMqttManager", "Error disconnecting from server : "+reconnect);
			setConnectionStatus(HikeMqttManager.MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
			if (mqttConnection != null)
			{
				mqttConnection.listener(CallbackConnection.DEFAULT_LISTENER);
			}

			if (reconnect)
			{
				connectToBroker();
			}
		}

	}

	public class ConnectTimeoutHandler implements Runnable
	{
		public void run()
		{
			disconnectFromBroker(true);
		}
	}

	public class PublishCB implements Callback<Void>, Runnable
	{
		private HikePacket packet;
		boolean called;
		public PublishCB(HikePacket packet)
		{
			this.packet = packet;
			this.called = false;
		}

		@Override
		public void onSuccess(Void value)
		{
			if (called)
			{
				Log.w("HikeMqttManager", "Received 'success' for message that's already been triggered");
			}

			called = true;
			handler.removeCallbacks(this);
			if (packet.getMsgId() > 0)
			{
				mHikeService.sendMessageStatus(packet.getMsgId(), true);
			}

			if (HikeMqttManager.this.haveUnsentMessages)
			{
			    handler.post(new RetryFailedMessages());
			}
		}

		@Override
		public void onFailure(Throwable value)
		{
			handler.post(this);
		}

		@Override
		public void run()
		{
			called = true;
			handler.removeCallbacks(this);
			Log.d("HikeMqttManager", "unable to send packet");
			ping();
			try
			{
			    HikeMqttManager.this.haveUnsentMessages = true;
				persistence.addSentMessage(packet);
			}
			catch (MqttPersistenceException e)
			{
				Log.e("HikeMqttManager", "Unable to persist message" + packet.toString(), e);
			}
		}
	}


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

	/*
	 * how often should the app ping the server to keep the connection alive?
	 * 
	 * too frequently - and you waste battery life too infrequently - and you wont notice if you lose your connection until the next unsuccessfull attempt to ping // // it's a
	 * trade-off between how time-sensitive the data is that your // app is handling, vs the acceptable impact on battery life // // it is perhaps also worth bearing in mind the
	 * network's support for // long running, idle connections. Ideally, to keep a connection open // you want to use a keep alive value that is less than the period of // time
	 * after which a network operator will kill an idle connection
	 */
	private short keepAliveSeconds = HikeConstants.KEEP_ALIVE;

	private String clientId;

	private String topic;

	private String password;

	private HikeConversationsDatabase convDb;

	private Map<Integer, HikePacket> mqttIdToPacket;

	private MQTT mqtt;

	private CallbackConnection mqttConnection;

	private HikeService mHikeService;

	private Handler handler;

	private String uid;

	private HikeUserDatabase userDb;

	private Runnable mConnectTimeoutHandler;

	private SharedPreferences settings;

	private MqttMessagesManager mqttMessageManager;

    private boolean haveUnsentMessages;

	public HikeMqttManager(HikeService hikeService, Handler handler)
	{
		this.mHikeService = hikeService;
		this.convDb = HikeConversationsDatabase.getInstance();
		this.userDb = HikeUserDatabase.getInstance();
		mqttIdToPacket = Collections.synchronizedMap(new HashMap<Integer, HikePacket>());
		this.handler = handler;
		persistence = HikeMqttPersistence.getInstance();
		mConnectTimeoutHandler = new ConnectTimeoutHandler();
		setConnectionStatus(MQTTConnectionStatus.INITIAL);
		this.mqttMessageManager = MqttMessagesManager.getInstance(mHikeService);
	}

	public HikePacket getPacketIfUnsent(int mqttId)
	{
		HikePacket packet = mqttIdToPacket.remove(mqttId);
		return packet;
	}

	private boolean init()
	{
		settings = this.mHikeService.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		password = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		topic = uid = settings.getString(HikeMessengerApp.UID_SETTING, null);
		clientId = settings.getString(HikeMessengerApp.MSISDN_SETTING, null) + ":" + HikeConstants.APP_API_VERSION;
		Log.d("HikeMqttManager", "clientId is " + clientId);
		return !TextUtils.isEmpty(topic) && !TextUtils.isEmpty(clientId) && !TextUtils.isEmpty(password);
	}

	/*
	 * Create a client connection object that defines our connection to a message broker server
	 */
	private void create()
	{
		init();
		try
		{
			mqtt = new MQTT();
			mqtt.setHost(brokerHostName, brokerPortNumber);
			mqtt.setClientId(clientId);
			mqtt.setKeepAlive((short) keepAliveSeconds);
			mqtt.setCleanSession(false);
			mqtt.setUserName(uid);
			mqtt.setPassword(password);
		}
		catch (URISyntaxException e)
		{
			// something went wrong!
			mqtt = null;
			setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);

			//
			// inform the user (for times when the Activity UI isn't running)
			// that we failed to connect
			this.mHikeService.notifyUser("Unable to connect", "MQTT", "Unable to connect");
		}
	}

	public void finish()
	{
		this.mqttMessageManager.close();
	}

	/*
	 * (Re-)connect to the message broker
	 */
	private synchronized void connectToBroker()
	{
		Log.d("HikeMqttManager", "calling connectToBroker "+connectionStatus);
		if (connectionStatus == MQTTConnectionStatus.CONNECTING)
		{
			Log.d("HikeMqttManager", "called connectToBroker but already in CONNECTING state");
			return;
		}

		if (mqtt == null)
		{
			create();
		}

		if (mqttConnection == null)
		{
			mqttConnection = mqtt.callbackConnection();
			mqttConnection.listener(this);
		}

		try {
			// try to connect
			Log.e("HikeMqttManager", "Trying to connect");
			setConnectionStatus(MQTTConnectionStatus.CONNECTING);
			handler.postDelayed(mConnectTimeoutHandler, 60*1000);
			mqttConnection.connect(new Callback<Void>() {
				public void onFailure(Throwable value)
				{
					Log.e("HikeMqttManager", "Hike Unable to connect", value);
					if (value instanceof ConnectionException && ((ConnectionException) value).getCode().equals(ConnectionStatus.BAD_USERNAME_OR_PASSWORD))
					{
						Log.e("HikeMqttManager", "Invalid account credentials");
						/* delete the token and send a message to the app to send the user back to the main screen */
						SharedPreferences.Editor editor = mHikeService.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
						editor.clear();
						editor.commit();
					}

					setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);

					mHikeService.notifyUser("Unable to connect", "MQTT", "Unable to connect - will retry later");

					mqttConnection = null; /* set the connection to null since it's no longer valid */
					/* if something has failed, we wait for one keep-alive period before
					 * trying again
					 * in a real implementation, you would probably want to keep count
					 * of how many times you attempt this, and stop trying after a
					 * certain number, or length of time - rather than keep trying
					 * forever.
					 * a failure is often an intermittent network issue, however, so
					 * some limited retry is a good idea */
					mHikeService.scheduleNextPing(HikeConstants.RECONNECT_TIME);
				}

				@Override
				public void onSuccess(Void value)
				{
					Log.d("HikeMqttManager", "Hike Connected");
					// inform the app that the app has successfully connected
					setConnectionStatus(MQTTConnectionStatus.CONNECTED);
					// we need to wake up the phone's CPU frequently enough so that the
					// keep alive messages can be sent
					// we schedule the first one of these now
					mHikeService.scheduleNextPing();
				}
			});
		}
		catch (Exception e)
		{
			/* couldn't connect, schedule a ping even earlier? */
			mHikeService.scheduleNextPing(HikeConstants.RECONNECT_TIME);
			Log.e("HikeMqttManager", "Exception Unable to connect", e);
		}
	}

	private void unsubscribeFromTopics(UTF8Buffer[] topics)
	{
		if (!isConnected())
		{
			Log.e("HikeMqttManager", "Unable to unsubscribe since we're not connected");
			return;
		}

		try
		{
			mqttConnection.unsubscribe(topics, null);
		}
		catch (IllegalArgumentException e)
		{
			Log.e("HikeMqttManager", "IllegalArgument trying to unsubscribe", e);
		}
	}

	/*
	 * Send a request to the message broker to be sent messages published with the specified topic name. Wildcards are allowed.
	 */
	private void subscribeToTopics(Topic[] topics)
	{

		if (isConnected() == false)
		{
			// quick sanity check - don't try and subscribe if we
			// don't have a connection
			Log.e("mqtt", "Unable to subscribe as we are not connected");
			return;
		}

		Log.d("HikeMqttManager", "connection is " + mqttConnection);
		mqttConnection.subscribe(topics, new Callback<byte[]>() {
			public void onSuccess(byte[] qoses)
			{
				Log.d("HikeMqttManager", "subscribe succeeded");
			}
			public void onFailure(Throwable value)
			{
				Log.e("HikeMqttManager", "subscribe failed.", value);
				disconnectFromBroker(false);
				mHikeService.scheduleNextPing(HikeConstants.RECONNECT_TIME);
			}
		});
	}

	/*
	 * Terminates a connection to the message broker.
	 */
	public synchronized void disconnectFromBroker(boolean reconnect)
	{
		try
		{
			if (mqttConnection != null)
			{
				mqttConnection.disconnect(new DisconnectCB(reconnect));
				mqttConnection = null;
			}

			setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
		}
		catch(Exception e)
		{
			Log.e("HikeMqttManager", "Caught exception while disconnecting", e);
		}
	}

	/*
	 * Checks if the MQTT client thinks it has an active connection
	 */
	public boolean isConnected()
	{
		Log.d("HikeMqttManager", "in isConnected status " + connectionStatus);
		return (mqttConnection != null) && (MQTTConnectionStatus.CONNECTED == connectionStatus);
	}

	public MQTTConnectionStatus getConnectionStatus()
	{
		return connectionStatus;
	}

	public void setConnectionStatus(MQTTConnectionStatus connectionStatus)
	{
		mHikeService.broadcastServiceStatus(connectionStatus);
		handler.removeCallbacks(mConnectTimeoutHandler);
		this.connectionStatus = connectionStatus;
	}

	/************************************************************************/
	/* METHODS - MQTT methods inherited from MQTT classes */
	/************************************************************************/

	/*
	 * callback - method called when we no longer have a connection to the message broker server
	 */
	@Override
	public void onDisconnected()
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
			if (!Utils.isUserOnline(mHikeService))
			{
				setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);

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
			}
		}
		finally
		{
			// we're finished - if the phone is switched off, it's okay for the CPU
			// to sleep now
			wl.release();

		}
	}

	private Topic[] getTopics()
	{
		boolean appConnected = mHikeService.appIsConnected();
		ArrayList<Topic> topics = new ArrayList<Topic>(2 + (appConnected ? 0 : 1));
		topics.add(new Topic(this.topic + HikeConstants.APP_TOPIC, QoS.AT_LEAST_ONCE));
		topics.add(new Topic(this.topic + HikeConstants.SERVICE_TOPIC, QoS.AT_LEAST_ONCE));

		/* only subscribe to UI events if the app is currently connected */
		if (appConnected)
		{
			topics.add(new Topic(this.topic + HikeConstants.UI_TOPIC, QoS.AT_LEAST_ONCE));
		}

		return (Topic[]) topics.toArray(new Topic[0]);
	}

	public void ping()
	{
		Log.d("HikeMqttManager", "calling ping");
		if (!isConnected() || 
				!mqttConnection.ping())
		{
			Log.d("HikeMqttManager", "App isn't connected, reconnecting");
			if (connectionStatus == MQTTConnectionStatus.CONNECTED)
			{
				setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
			}
			connect();
		}
	}
	
	public void reconnect(){
		if(this.connectionStatus == MQTTConnectionStatus.CONNECTING)
			return;
		
		if (mqttConnection != null)
		{
			mqttConnection.disconnect(new DisconnectCB(true));
			mqttConnection = null;
		}
		setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON);
		connect();
	}

	public void connect()
	{
		if (isConnected())
		{
			Log.d("HikeMqttManager", "already connected");
			return;
		}

		if (Utils.isUserOnline(mHikeService))
		{
			Log.d("HikeMqttManager", "netconnection valid, try to connect");
			// set the status to show we're trying to connect
			connectToBroker();
		}
		else
		{
			// we can't do anything now because we don't have a working
			// data connection
			setConnectionStatus(MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET);
		}
	}

	public void send(HikePacket packet, int qos)
	{
		if (!isConnected())
		{
			Log.d("HikeMqttManager", "trying to send " + new String(packet.getMessage()) + " but not connected. Try to connect but fail this message");

			/* only care about failures for messages we care about. */
			if (qos > 0)
			{
				try
				{
					persistence.addSentMessage(packet);
				}
				catch (MqttPersistenceException e)
				{
					Log.e("HikeMqttManager", "Unable to persist message");
				}
			}

			this.connect();
			return;
		}

		Log.d("HikeMqttManager", "About to send message " + new String(packet.getMessage()));
		PublishCB pbCB = new PublishCB(packet);

		mqttConnection.publish(new UTF8Buffer(this.topic + HikeConstants.PUBLISH_TOPIC),
				new Buffer(packet.getMessage()), qos == 0 ? QoS.AT_MOST_ONCE : QoS.AT_LEAST_ONCE,
				false, pbCB);
	}

	public void unsubscribeFromUIEvents()
	{
		unsubscribeFromTopics(new UTF8Buffer[] { new UTF8Buffer(this.topic + HikeConstants.UI_TOPIC) });
	}

	public void subscribeToUIEvents()
	{
		subscribeToTopics(new Topic[]{ new Topic(this.topic + HikeConstants.UI_TOPIC, QoS.AT_MOST_ONCE)});
	}

	@Override
	public void onConnected()
	{
		Log.d("HikeMqttManager", "mqtt connected");
		setConnectionStatus(MQTTConnectionStatus.CONNECTED);

		subscribeToTopics(getTopics());

		/* Accesses the persistence object from the main handler thread */
		handler.post(new RetryFailedMessages());
	}

	@Override
	public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack)
	{
		// we protect against the phone switching off while we're doing this
		// by requesting a wake lock - we request the minimum possible wake
		// lock - just enough to keep the CPU running until we've finished

		PowerManager pm = (PowerManager) this.mHikeService.getSystemService(Context.POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
		try
		{
			wl.acquire();

			String messageBody = new String(body.toByteArray());

			Log.d("HikeMqttManager", "onPublish called " + messageBody);
			JSONObject jsonObj = new JSONObject(messageBody);

			/* handle saving of messages here so we don't risk losing them when the app is not open.
			 */
			mqttMessageManager.saveMqttMessage(jsonObj);

			/* don't bother saving messages for the UI topic */
			if ((topic != null) &&
				(topic.getString().endsWith(("/u"))))
			{
				return;
			}			

			// receiving this message will have kept the connection alive for us, so
			// we take advantage of this to postpone the next scheduled ping
			this.mHikeService.scheduleNextPing();

			// we're finished - if the phone is switched off, it's okay for the CPU
			// to sleep now
		}
		catch (JSONException e)
		{
			Log.e("HikeMqttManager", "invalid JSON message", e);
		}
		finally
		{
			Log.d("HikeMqttManager", "About to call ack");
			ack.run();
			wl.release();
		}

	}

	@Override
	public void onFailure(Throwable value)
	{
		Log.e("HikeMqttManager", "onFailure called.", value);
		disconnectFromBroker(false);
		this.mHikeService.scheduleNextPing(HikeConstants.RECONNECT_TIME);
	}
}
