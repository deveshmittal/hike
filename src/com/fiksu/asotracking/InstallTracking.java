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

import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;

import android.os.Bundle;
import android.util.Log;

public class InstallTracking extends BroadcastReceiver {
	private final static long MAX_BLOCK_MS = 3000;
	private final static String INTENT_NAME = "com.android.vending.INSTALL_REFERRER";
	private final static String FIKSU_RECEIVER = "com.fiksu.asotracking.InstallTracking";

	public void onReceive(Context context, Intent intent) {
		uploadConversionEvent(context, intent);
		forwardToOtherReceivers(context, intent);
	}

	protected void uploadConversionEvent(Context context, Intent intent) {
		try {
			String referrer = intent.getStringExtra("referrer");
			if (referrer != null) {
				referrer = URLDecoder.decode(referrer);
			} else {
				referrer = "";
			}
			new ConversionEventTracker(context, referrer).uploadEventSynchronously(MAX_BLOCK_MS);
		} catch (Exception e) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Unhandled exception processing intent.", e);
		}
	}

	protected void forwardToReceiver(Context context, Intent intent, String receiverClassName) {
		try {
			BroadcastReceiver receiver = (BroadcastReceiver)((Class.forName(receiverClassName)).newInstance());
			receiver.onReceive(context, intent);
			Log.d(FiksuTrackingManager.FIKSU_LOG_TAG, "Forwarded to: " + receiverClassName);
		} catch (ClassNotFoundException missing) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Forward failed, couldn't load class: " + receiverClassName);
		} catch (Exception error) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Forwarding to " + receiverClassName + " failed:", error);
		}
	}

	protected void forwardToOtherReceivers(Context context, Intent intent) {
		List<String> classNames = readTargetsFromMetaData(context);

		List<String> badlyBehaved = new ArrayList<String>();
		for (Iterator<String> iter = classNames.iterator(); iter.hasNext();) {
			String value = iter.next();
			if (value.equals(FIKSU_RECEIVER)) {
				iter.remove();
			}
			if (value.startsWith("getjar.")) {
				// Can cause the process to exit, so must run last.
				badlyBehaved.add(0, value);
				iter.remove();
			}
		}

		for (String className : classNames) {
			forwardToReceiver(context, intent, className);
		}

		for (String className : badlyBehaved) {
			forwardToReceiver(context, intent, className);
		}
	}

	protected static List<String> readTargetsFromMetaData(Context context) {
		List<String> receivers = new ArrayList<String>();

		PackageManager packageManager = context.getPackageManager();
		if (packageManager == null) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Couldn't get PackageManager.");
			return receivers;
		}

		try {
			ActivityInfo activityInfo = packageManager.getReceiverInfo(new ComponentName(context, InstallTracking.class),
																	   PackageManager.GET_META_DATA);
			if (activityInfo == null || activityInfo.metaData == null || activityInfo.metaData.keySet() == null) {
				Log.d(FiksuTrackingManager.FIKSU_LOG_TAG, "No forwarding metadata.");
				return receivers;
			}

			Bundle metaData = activityInfo.metaData;
			// Execute in lexical order of keys.
			List<String> keys = new ArrayList<String>(metaData.keySet());
			Collections.sort(keys);

			for (String key : keys) {
				if (!key.startsWith("forward.")) {
					continue;
				}
				if (metaData.getString(key) == null || metaData.getString(key).trim().equals("")) {
					Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Couldn't parse receiver from metadata.");
					continue;
				}
				receivers.add(metaData.getString(key).trim());
			}
			return receivers;
		} catch (NameNotFoundException nnf) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Couldn't get info for receivers.");
			return receivers;
		}
	}

	protected static List<String> readReceiversFromManifest(Context context) {
		List<String> receivers = new ArrayList<String>();

		PackageManager packageManager = context.getPackageManager();
		if (packageManager == null) {
			return receivers;
		}

		Intent installReferrerIntent = new Intent(INTENT_NAME);
		installReferrerIntent.setPackage(context.getPackageName());
		
		List<ResolveInfo> list = packageManager.queryBroadcastReceivers(installReferrerIntent, 0);
		if (list == null || list.size() == 0) {
			return receivers;
		}

		for (ResolveInfo info : list) {
			if (info == null || info.activityInfo == null || info.activityInfo.name == null) {
				continue;
			}
			
			receivers.add(info.activityInfo.name);
		}
		return receivers;
	}

	protected static void checkForFiksuReceiver(Context context) {
		List<String> receivers = readReceiversFromManifest(context);
		if (receivers.size() == 0 || !receivers.get(0).equals(FIKSU_RECEIVER)) {
			String receiver = receivers.size() > 0 ? receivers.get(0) : "NONE";
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "THE FIKSU INSTALL TRACKING CODE ISN'T INSTALLED CORRECTLY!");
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Unexpected receiver: " + receiver);

			try{
				throw new FiksuIntegrationError("The Fiksu BroadcastReceiver must be installed as the only receiver for the " +
						"INSTALL_REFERRER Intent in AndroidManifest.xml.");
			} catch (FiksuIntegrationError e) {
				Log.w("Fiksu", "Fiksu exception", e);
			}
		}

		if (receivers.size() > 1) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "THE FIKSU INSTALL TRACKING CODE ISN'T INSTALLED CORRECTLY!");
			final String msg = "Multiple receivers declared for: " + INTENT_NAME;
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, msg);
			try {
				throw new FiksuIntegrationError(msg);
			} catch (FiksuIntegrationError e) {
				Log.w("Fiksu", "Fiksu exception", e);
			}
		}
	}
}

