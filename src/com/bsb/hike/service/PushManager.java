package com.bsb.hike.service;

import java.net.URISyntaxException;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ContactUtils;
import com.bsb.hike.utils.HikeConversationsDatabase;
import com.bsb.hike.utils.HikeToast;
import com.bsb.hike.utils.HikeUserDatabase;

public class PushManager extends Thread
{

	private static PushManager mPushManager;

	private MQTT mqtt;

	private volatile boolean finished;

	private String password;

	private String topic;

	private HikeToast toaster;

	private HikeUserDatabase db;

	private HikeConversationsDatabase convDb;

	private HikeService service;

	public PushManager(HikeService service, String uid, String password)
	{
		mqtt = new MQTT();
		this.toaster = new HikeToast(service);
		try
		{
			mqtt.setHost(AccountUtils.HOST, 1883);
		} catch(URISyntaxException e)
		{
			Log.w("PushManager", "Invalid Host:" + AccountUtils.HOST);
			System.exit(1);
		}
		mqtt.setKeepAlive((short) (60 * 5));
		mqtt.setWillTopic("connection_terminated");
		mqtt.setPassword(password);
		this.password = password;
		this.topic = uid;
		this.db = new HikeUserDatabase(service);
		this.convDb = new HikeConversationsDatabase(service);
		this.service = service;
	}

	private BlockingConnection connect()
	{
		BlockingConnection connection;
		while (true)
		{
			try
			{
				Log.d("PushManager", "creating blockingConnection");
				connection = mqtt.blockingConnection();
				Log.d("PushManager", "connecting");
				connection.connect();
				Log.d("PushManager", "subscribing to topic: " + this.topic);
				Topic[] topics = { new Topic(this.topic, QoS.AT_LEAST_ONCE) };
				byte[] qos = connection.subscribe(topics);
				Log.d("PushManager", "subscribed");
				return connection;
			}
			catch (Exception e)
			{
				Log.e("PushManager", "trying to connect", e);
				try
				{
					Thread.sleep(2000);
				}
				catch (InterruptedException e1)
				{
					e1.printStackTrace();
				}
			}
		}
	}

	@Override
	public void run()
	{
/*		Log.d("PushManager", "sending test message");
		JSONObject tst = new JSONObject();
		try
		{
			tst.put("type", "message");
			tst.put("from", "+917146804208");
			JSONObject data = new JSONObject();
			data.put("message", "hello world!");
			tst.put("data", data);
		}
		catch (JSONException e1)
		{
			e1.printStackTrace();
		}

		this.service.sendToApp(tst.toString());*/

		Log.d("PushManager", "Making connection");
		BlockingConnection connection = connect();
		Log.d("PushManager", "Connection made");
		while (!finished)
		{
			Message message;
			try
			{
				Log.d("PushManager", "receiving message");
				message = connection.receive();
				Log.d("PushManager", "message received " + message);
			}
			catch (Exception e)
			{
				connection = connect();
				continue;
			}

			Buffer buffer = message.getPayloadBuffer();
			if (buffer == null)
			{
				//empty message, ignore it
				message.ack();
				continue;
			}

			byte[] payload = buffer.getData();

			String str = new String(payload);
			if (this.service.sendToApp(str))
			{
				message.ack();
				continue;
			}

			/* couldn't send a message to the app
			 if it's a message -- toast and write it now
			 otherwise, just save it in memory until the app connects
			 */

			JSONObject obj = null;
			try
			{
				obj = new JSONObject(str);
			} catch (JSONException e)
			{
				Log.e("PushManager", "Invalid JSON Object", e);
				/* we ack the message so we can continue processing later valid messages */
				message.ack();
				continue;
			}

			if ("message".equals(obj.optString("type")))
			{
				/* toast and save it */
				try
				{
					ConvMessage convMessage = new ConvMessage(obj);
					this.convDb.addConversationMessages(convMessage);
					ContactInfo contactInfo = ContactUtils.getContactInfo(convMessage.getMsisdn(), service);
					toaster.toast(contactInfo, convMessage.getMsisdn(), convMessage.getMessage(), convMessage.getTimestamp());
				} catch (JSONException e)
				{
					Log.e("JSON", "Invalid JSON", e);
				}
			} else
			{
				/* just save it */
				this.service.storeMessage(str);
			}
			message.ack();
		}
	}

	public synchronized static PushManager createInstance(HikeService service)
	{
		if (mPushManager == null)
		{
			SharedPreferences settings = service.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
			String uid = settings.getString(HikeMessengerApp.UID_SETTING, null);
			mPushManager = new PushManager(service, uid, token);
			mPushManager.start();
		}
		return mPushManager;
	}

}
