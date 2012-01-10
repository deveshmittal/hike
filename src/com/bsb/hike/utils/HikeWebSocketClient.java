package com.bsb.hike.utils;

import java.io.IOException;
import java.net.URI;

import net.tootallnate.websocket.Draft;
import net.tootallnate.websocket.WebSocketClient;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

public class HikeWebSocketClient extends WebSocketClient
{

	protected static final String FINISH = "finish";

	private HikePubSub pubSub;

	public HikeWebSocketClient(URI uri, Draft draft)
	{
		super(uri, draft);
		pubSub = HikeMessengerApp.getPubSub();
	}

	@Override
	public void onClose()
	{
		Log.w("HikeWebSocketClient", "close");
		pubSub.publish(HikePubSub.WS_CLOSE, null);
	}

	@Override
	public void onIOError(IOException e)
	{
		Log.e("WebSocket", "IOError", e);
		pubSub.publish(HikePubSub.WS_CLOSE, e);
	}

	@Override
	public void onMessage(String message)
	{
		Log.d("HikeWebSocketClient", message);
		pubSub.publish(HikePubSub.WS_MESSAGE, message);
	}

	@Override
	public void onOpen()
	{
		Log.w("HikeWebSocketClient", "open");
		pubSub.publish(HikePubSub.WS_OPEN, null);
	}

}
