package com.bsb.hike;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.bsb.hike.HikePubSub.Operation;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeUserDatabase;
import com.bsb.hike.utils.HikeWebSocketClient;

public class NetworkManager implements HikePubSub.Listener, Runnable {

	private static final String FINISH = null;
    private SharedPreferences settings;
	private Context context;
	private HikeUserDatabase mDb;
	private HikePubSub pubSub;
	private HikeWebSocketClient mWebSocket;
	private Thread mThread;
    private BlockingQueue<String> mQueue;

	public NetworkManager(Context context) {
		this.mDb = new HikeUserDatabase(context);
		this.context = context;
		this.settings = context.getSharedPreferences(HikeMessengerApp.MESSAGES_SETTING, 0);
		pubSub = HikeMessengerApp.getPubSub();
		pubSub.addListener(HikePubSub.WS_MESSAGE, this);
		pubSub.addListener(HikePubSub.WS_SEND, this);
		pubSub.addListener(HikePubSub.WS_CLOSE, this);
		pubSub.addListener(HikePubSub.WS_OPEN, this);
		pubSub.addListener(HikePubSub.TOKEN_CREATED, this);
		mQueue = new LinkedBlockingQueue<String>();
		mThread = new Thread(this);
		mThread.start();
	}

	public void onMessage(String msg) {
		JSONObject data;
		String type;
		try {
			data = new JSONObject(msg);
			type = data.getString("type");
		} catch (JSONException e) {
			Log.e("WebSocketPublisher", "Invalid JSON message: " + msg);
			return;
		}

		if ("message".equals(type)) {
			String id = data.optString("id");
			String msisdn = data.optString("from");
			ContactInfo contactInfo = this.mDb.getContactInfoFromMSISDN(msisdn);
			String contactId = (contactInfo != null) ? contactInfo.id : null;
			String message = data.optString("data");
			int ts = (int) System.currentTimeMillis()/1000;
			ConvMessage convMessage = new ConvMessage(message, msisdn, contactId, ts, false);
			this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		} else if ("typing".equals(type)) {
			String msisdn = data.optString("from");
			this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, msisdn);
		} else if ("stop_typing".equals(type)) {
			String msisdn = data.optString("from");
			this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, msisdn);
		} else {
			Log.d("WebSocketPublisher", "Unknown Type:" + type);
		}
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (type.equals(HikePubSub.WS_MESSAGE)) {
			String message = (String) object;
			onMessage(message);
		} else if (HikePubSub.TOKEN_CREATED.equals(type)) {
		    Log.i("NetworkManager", "Creating Websocket");
		    startWebSocket();
		} else if (HikePubSub.WS_CLOSE.equals(type)) {
            Log.i("NetworkManager", "Websocket closed");
            mWebSocket = null;
            startWebSocket();
		} else if (HikePubSub.WS_SEND.equals(type)) {
		    JSONObject o = (JSONObject) object;
		    String str = o.toString();
		    mQueue.add(str);
		}
	}

    private synchronized void startWebSocket() {
        if (mWebSocket == null) {
            mWebSocket = AccountUtils.startWebSocketConnection();
        }
    }

    @Override
    public void run() {
        while (true) {
            String message;
            try {
                message = mQueue.take();
                System.out.println("trying to send message");
            } catch (InterruptedException e) {
                Log.e("WebSocket", "sending thread", e);
                continue;
            }
            if (message == FINISH) {
                break;
            }

            while (mWebSocket == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    continue;
                }
            }

            try {
                mWebSocket.send(message);
            } catch (IOException e) {
                Log.e("WebSocket", "Unable to send message", e);
                mQueue.add(message);
            } catch (java.nio.channels.NotYetConnectedException e) {
                Log.e("WebSocket", "Not yet connected");
                mQueue.add(message);
            }
        }

    }
}
