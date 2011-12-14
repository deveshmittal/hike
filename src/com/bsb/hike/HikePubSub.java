package com.bsb.hike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class HikePubSub implements Runnable {
	public class Operation {
		public Operation(String type, Object o) {
			this.type = type;
			this.payload = o;
		}
		public final String type;
		public final Object payload;
	}

	public interface Listener {
		public void onEventReceived(String type, Object object);
	}

	public static final String MESSAGE_SENT = "messagesent";
	public static final String WS_CLOSE = "ws_close";
	public static final String WS_MESSAGE = "ws_message";
	public static final String WS_OPEN = "ws_open";
	private static final Operation DONE_OPERATION = null; /* TODO this can't be null */
	public static final String NEW_CONVERSATION = "newconv";
	public static final String MESSAGE_RECEIVED = "messagereceived";
	public static final String NEW_ACTIVITY = "new_activity";
	private final Thread mThread;
	private final BlockingQueue<Operation> mQueue;

	private Map<String, List<Listener> > listeners;

	public HikePubSub() {
		listeners = Collections.synchronizedMap(new HashMap<String, List<Listener>>());
		mQueue = new LinkedBlockingQueue<Operation>();
		mThread = new Thread(this);
		mThread.start();
	}

	synchronized public void addListener(String type, Listener listener) {
		List<Listener> list = listeners.get(type);
		if (list == null) {
			list = Collections.synchronizedList((new ArrayList<Listener>()));
			listeners.put(type, list);
		}
		list.add(listener);
	}

	synchronized public boolean publish(String type, Object o) {
		List<Listener> list = listeners.get(type);
		if (list == null) {
			return false;
		}
		mQueue.add(new Operation(type, o));
		return true;
	}

	public void removeListener(String type, Listener listener) {
		List<Listener> l = listeners.get(type);
		if (l != null) {
			l.remove(listener);
		}
	}

	@Override
	public void run() {
		Operation op;
		while (true) {
			try {
				op = mQueue.take();
			} catch(InterruptedException e) {
				Log.e("PubSub", "exception while running", e);
				continue;
			}
			if (op == DONE_OPERATION) {
				break;
			}

			String type = op.type;
			Object o = op.payload;

			List<Listener> list = listeners.get(type);
			if (list == null) {
				continue;
			}

			for (Listener l : list) {
				l.onEventReceived(type, o);
			}
		}
	}
}
