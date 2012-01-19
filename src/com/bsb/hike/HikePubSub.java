package com.bsb.hike;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class HikePubSub implements Runnable
{
	public class Operation
	{
		public Operation(String type, Object o)
		{
			this.type = type;
			this.payload = o;
		}

		public final String type;

		public final Object payload;
	}

	public interface Listener
	{
		public void onEventReceived(String type, Object object);
	}

	private static final Operation DONE_OPERATION = null; /* TODO this can't be null */

	/* broadcast when the sender sends the message (click the send button in chat thread view) */
	public static final String MESSAGE_SENT = "messagesent";
	
	public static final String MESSAGE_DELIVERED = "messageDelivered"; // represents that msg is delivered to receiver but is not read.
	
	public static final String MESSAGE_DELIVERED_READ = "messageDeliveredRead"; // represents that msg is delivered to receiver and is read by the same.

	public static final String WS_CLOSE = "ws_close";

	public static final String WS_RECEIVED = "ws_received";

	public static final String WS_OPEN = "ws_open";

	public static final String WS_SEND = "ws_send";

	public static final String NEW_CONVERSATION = "newconv";

	/* Broadcast after we've received a message and written it to our DB.
	 * Status is RECEIVED_UNREAD */
	public static final String MESSAGE_RECEIVED = "messagereceived";

	public static final String NEW_ACTIVITY = "new_activity";

	public static final String END_TYPING_CONVERSATION = "endtypingconv";

	public static final String TYPING_CONVERSATION = "typingconv";

	public static final String TOKEN_CREATED = "tokencreated";

	/* sms credits have been modified */
	public static final String SMS_CREDIT_CHANGED = "smscredits";

	/* broadcast when the server receives the message and replies with a confirmation */
	public static final String SERVER_RECEIVED_MSG = "serverReceivedMsg";

	/* broadcast when a message is received from the sender but before it's been written our DB*/
	public static final String MESSAGE_RECEIVED_FROM_SENDER = "messageReceivedFromSender";

	private final Thread mThread;

	private final BlockingQueue<Operation> mQueue;

	private Map<String, List<Listener>> listeners;

	public HikePubSub()
	{
		listeners = Collections.synchronizedMap(new HashMap<String, List<Listener>>());
		mQueue = new LinkedBlockingQueue<Operation>();
		mThread = new Thread(this);
		mThread.start();
	}

	synchronized public void addListener(String type, Listener listener)
	{
		List<Listener> list = listeners.get(type);
		if (list == null)
		{
			list = new CopyOnWriteArrayList<Listener>();
			listeners.put(type, list);
		}
		list.add(listener);
	}

	synchronized public boolean publish(String type, Object o)
	{
		List<Listener> list = listeners.get(type);
		if (list == null)
		{
			return false;
		}
		mQueue.add(new Operation(type, o));
		return true;
	}

	public void removeListener(String type, Listener listener)
	{
		List<Listener> l = listeners.get(type);
		if (l != null)
		{
			l.remove(listener);
		}
	}

	@Override
	public void run()
	{
		Operation op;
		while (true)
		{
			try
			{
				op = mQueue.take();
			}
			catch (InterruptedException e)
			{
				Log.e("PubSub", "exception while running", e);
				continue;
			}
			if (op == DONE_OPERATION)
			{
				break;
			}

			String type = op.type;
			Object o = op.payload;

			
			List<Listener> list = listeners.get(type);
			
			if (list == null)
			{
				continue;
			}

			for (Listener l : list)
			{
				l.onEventReceived(type, o);
			}
		}
	}
}
