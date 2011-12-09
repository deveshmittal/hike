package com.bsb.hike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HikePubSub {
	public interface Listener {
		public void onEventReceived(String type, Object object);
	}

	public static final String MESSAGE = "message";
	private Map<String, List<Listener> > listeners;

	public HikePubSub() {
		listeners = new HashMap<String, List<Listener>>();
	}

	synchronized public void addListener(String type, Listener listener) {
		List<Listener> list = listeners.get(type);
		if (list == null) {
			list = new ArrayList<Listener>();
			listeners.put(type, list);
		}
		list.add(listener);
	}

	synchronized public boolean publish(String type, Object o) {
		List<Listener> list = listeners.get(type);
		if (list == null) {
			return false;
		}

		for (Listener l : list) {
			l.onEventReceived(type, o);
		}
		return true;
	}
}
