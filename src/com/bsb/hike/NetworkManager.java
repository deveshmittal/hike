package com.bsb.hike;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeUserDatabase;
import com.bsb.hike.utils.HikeWebSocketClient;

/**
 * 
 * @author Vijay , Gautam (Some changes)
 * Class should be singleton as only one instance is required to manage the components.
 *
 */

public class NetworkManager implements HikePubSub.Listener, Runnable
{

	private static final JSONObject FINISH = new JSONObject();

	private SharedPreferences settings;

	private Context context;

	private HikeUserDatabase mDb;

	private HikePubSub pubSub;

	private HikeWebSocketClient mWebSocket;

	private Thread mThread;

	private BlockingQueue<JSONObject> mQueue;

	private JSONObject lastMessageSent;

	private long lastMessageTimestamp;

	private static volatile NetworkManager instance;
	
	private NetworkManager(Context context)
	{
		this.mDb = new HikeUserDatabase(context);
		this.context = context;
		this.settings = context.getSharedPreferences(HikeMessengerApp.MESSAGES_SETTING, 0);
		pubSub = HikeMessengerApp.getPubSub();
		pubSub.addListener(HikePubSub.WS_RECEIVED, this);
		pubSub.addListener(HikePubSub.WS_SEND, this);
		pubSub.addListener(HikePubSub.WS_CLOSE, this);
		pubSub.addListener(HikePubSub.WS_OPEN, this);
		pubSub.addListener(HikePubSub.TOKEN_CREATED, this);
		mQueue = new LinkedBlockingQueue<JSONObject>();
		mThread = new Thread(this);
		mThread.start();
	}
	
	public static NetworkManager getInstance(Context context)
	{
		if (instance == null)
		{
			synchronized (NetworkManager.class)
			{
				if (instance == null)
				{
					instance = new NetworkManager(context);
				}
			}
		}

		return instance;
	}

	private void onMessage(String msg)
	{
		JSONObject data;
		String type;
		try
		{
			data = new JSONObject(msg);
			type = data.getString("type");
		}
		catch (JSONException e)
		{
			Log.e("WebSocketPublisher", "Invalid JSON message: " + msg);
			return;
		}

		if ("message".equals(type))  // this represents msg from another client through tornado server.
		{
			try
			{
				ConvMessage convMessage = new ConvMessage(data);
				this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED_FROM_SENDER, convMessage);
			} catch (JSONException e)
			{
				Log.d("JSON", "Invalid JSON", e);
			}
		}
		else if ("typing".equals(type))
		{
			String msisdn = data.optString("from");
			this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, msisdn);
		}
		else if ("stop_typing".equals(type))
		{
			String msisdn = data.optString("from");
			this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, msisdn);
		}
		else if ("sms_credits".equals(type))
		{
			int sms_credits = data.optInt("data");
			this.pubSub.publish(HikePubSub.SMS_CREDIT_CHANGED, new Integer(sms_credits));
		}
		else if("msgrcpt".equals(type)) // this handles the case when msg with msgId is recieved by the tornado server and it send back a recieved msg
		{
			long msgID = data.optLong("data");
			this.pubSub.publish(HikePubSub.SERVER_RECEIVED_MSG, msgID);	
		}
		else if("msgDelivered".equals(type)) // this handles the case when msg with msgId is recieved by the tornado server and it send back a received msg
		{
			long msgID = data.optLong("msgID");
			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED, msgID);	
		}
		else if("msgDeliveredRead".equals(type)) // this handles the case when msg with msgId is recieved by the tornado server and it send back a recieved msg
		{
			long msgID = data.optLong("msgID");
			this.pubSub.publish(HikePubSub.MESSAGE_DELIVERED_READ, msgID);	
		}

		else
		{
			Log.d("WebSocketPublisher", "Unknown Type:" + type);
		}
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (type.equals(HikePubSub.WS_RECEIVED)) // signifies msg is received through web sockets.
		{
			String message = (String) object;
			onMessage(message);
		}
		else if (HikePubSub.TOKEN_CREATED.equals(type))
		{
			Log.i("NetworkManager", "Token Created -- starting websocket");
			startWebSocket();
		}
		else if (HikePubSub.WS_CLOSE.equals(type))
		{
			Log.i("NetworkManager", "Websocket closed");
			/* try to close any existing connections from our end */
			if (mWebSocket != null)
			{
				try
				{
					mWebSocket.close();
				}
				catch (IOException e)
				{
					Log.e("NetworkManager", "Error closing websocket", e);
				}
			}

			mWebSocket = null;
			Runnable r = new Runnable()
			{

				@Override
				public void run()
				{
					try
					{
						Thread.sleep(2000);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					startWebSocket();
				}
			};
			Thread t = new Thread(r);
			t.start();
		}
		else if (HikePubSub.WS_SEND.equals(type))
		{
			JSONObject o = (JSONObject) object;
			mQueue.add(o);
		}
		else if (HikePubSub.WS_OPEN.equals(type))
		{
			long now = System.currentTimeMillis();
			Log.d("NetworkManager", "Websocket opened " + now + " " + this.lastMessageTimestamp);
			if ((now - this.lastMessageTimestamp) <= 5) //resend any message sent in the last 5 seconds
			{
				Log.e("NetworkManager", "open after recent message.  Resending");
				//assume the last message didn't get sent through.  Total hack
				this.lastMessageTimestamp = 0;
				mQueue.add(this.lastMessageSent);
			}
		}
	}

	public synchronized void startWebSocket()
	{
		if (mWebSocket == null)
		{
			synchronized(NetworkManager.class)
			{
				if (mWebSocket == null)
				{
					Log.d("NetworkManager", "restarting websocket");
					mWebSocket = AccountUtils.startWebSocketConnection();
				}
			}
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			JSONObject message;
			try
			{
				message = mQueue.take();
				Log.d("NetworkManager", "trying to send message: " + message);
			}
			catch (InterruptedException e)
			{
				Log.e("WebSocket", "sending thread", e);
				continue;
			}

			if (message == FINISH)
			{
				break;
			}

			try
			{
				if ("ASDFsend".equals(message.optString("type")))
				{
					Log.d("NetworkManager", "received a 'send' message.  saving it");
					this.lastMessageSent = message;
					this.lastMessageTimestamp = System.currentTimeMillis();
				}

				while (mWebSocket == null)
				{
					startWebSocket();
					Log.i("NetworkManager", "no websocket, sleeping");
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				/* total race condition here, but whatever */
				mWebSocket.send(message.toString());
			}
			catch (IOException e)
			{
				Log.e("WebSocket", "Unable to send message", e);
				mQueue.add(message);
			}
			catch (java.nio.channels.NotYetConnectedException e)
			{
				Log.e("WebSocket", "Not yet connected");
				try
				{
					Thread.sleep(1000);
				}
				catch(InterruptedException e3)
				{
					//pass
				}

				mQueue.add(message);
			}
		}

	}
}
