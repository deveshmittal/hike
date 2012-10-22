/*
 * Android ASO Tracking Code
 *
 * Version: 1.2
 *
 * Copyright (C) 2011 Fiksu Incorporated
 * All Rights Reserved. 
 *
 */

package com.fiksu.asotracking;

import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;

public class EventTracker {
	private static Context mCachedContext = null;
	protected Context mContext = null;
	private final HashMap<String, String> mParameters;
	static final String SHARED_PREFERENCES_LOCK = "shared preferences lock";
	
	public EventTracker(Context context, String event) {
		mParameters = new HashMap<String, String>();

		mParameters.put("event", event);
		if (context != null) {
			mCachedContext = context;
			this.mContext = context;
		} else {
			this.mContext = mCachedContext;
		}
	}	
	
	protected void addParameter(String name, String value) {
		mParameters.put(name, value);
	}
	
	private HashMap<String, String>copyOfParams() {
		HashMap<String, String> newParams = new HashMap<String, String>();
		for (String key : mParameters.keySet()) {
			newParams.put(key, mParameters.get(key));
		}
		return newParams;
	}
	
	protected void uploadEvent() {
		new Thread(new EventUploader(mContext, copyOfParams())).start();
	}

	protected void uploadEventSynchronously(long timeoutMs) {
		final EventUploader uploader = new EventUploader(mContext, copyOfParams());

		synchronized (uploader) {
			new Thread(uploader).start();
			try {
				uploader.wait(timeoutMs);
			} catch (InterruptedException ie) {
			}
		}
	}

	static SharedPreferences getOurSharedPreferences(Context context) {
		if (context == null) {
			return null;
		}
		return context.getSharedPreferences("FiksuSharedPreferences", 0);
	}
	
	private static final class C2DMessageTimeSaver implements Runnable {
		private final Context mContext;
		
		C2DMessageTimeSaver(Context context) {
			mContext = context;
		}
		
		public void run() {
			synchronized(EventTracker.SHARED_PREFERENCES_LOCK) {
				SharedPreferences preferences = getOurSharedPreferences(mContext);
				Date now = new Date();
				SharedPreferences.Editor editor = preferences.edit();
				editor.putLong("Fiksu.cd2MessageTime", now.getTime());
				editor.commit();
			}
		}
	}

	static void c2dMessageReceived(Context context) {
		new Thread(new C2DMessageTimeSaver(context)).start();
	}
}

