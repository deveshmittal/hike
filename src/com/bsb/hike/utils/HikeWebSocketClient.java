package com.bsb.hike.utils;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;

import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

import net.tootallnate.websocket.Draft;
import net.tootallnate.websocket.WebSocketClient;

public class HikeWebSocketClient extends WebSocketClient {

	protected static final String FINISH = "finish";
	private HikePubSub pubSub;
	private Thread mThread;
	private LinkedBlockingQueue<String> mQueue;

	public HikeWebSocketClient(URI uri, Draft draft) {
		super(uri, draft);
		pubSub = HikeMessengerApp.getPubSub();
		mQueue = new LinkedBlockingQueue<String>();
		mThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					String message;
					try {
						message = mQueue.take();
					} catch (InterruptedException e) {
						Log.e("WebSocket", "sending thread", e);
						continue;
					}
					if (message == FINISH) {
						break;
					}
					try {
						send(message);
					} catch (IOException e) {
						Log.e("WebSocket", "Unable to send message", e);
						//TODO maybe requeue?
					}
				}
			}});
		mThread.start();
		
	}

	public void nb_send(JSONObject obj) {
		String message = obj.toString();
		mQueue.add(message);
	}

	@Override
	public void onClose() {
		Log.w("HikeWebSocketClient", "close");
		pubSub.publish(HikePubSub.WS_CLOSE, null);
	}

	@Override
	public void onIOError(IOException e) {
		Log.e("WebSocket", "IOError", e);
		pubSub.publish(HikePubSub.WS_CLOSE, e);
	}

	@Override
	public void onMessage(String message) {
		System.out.println("onMessage -- " + message);
		pubSub.publish(HikePubSub.WS_MESSAGE, message);
	}

	@Override
	public void onOpen() {
		Log.w("HikeWebSocketClient", "open");
		pubSub.publish(HikePubSub.WS_OPEN, null);
	}

}
