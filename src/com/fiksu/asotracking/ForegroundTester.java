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

import java.util.List;

import android.app.ActivityManager;
import android.app.Application;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Process;
import android.util.Log;

class ForegroundTester implements Runnable {
	private final Application mApplication;
	private final LaunchEventTracker mLaunchEventTracker;

	private boolean mWasInForeground = false;
	private boolean mPostedLaunch = false;

	private static boolean sStarted = false;

	ForegroundTester(Application application, LaunchEventTracker launchEventTracker) {
		mApplication = application;
		mLaunchEventTracker = launchEventTracker;
		synchronized(ForegroundTester.class) {
			if (sStarted) {
				Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Already initialized!. Only call FiksuTrackingManager.initialize() once.");
				return;
			}
			sStarted = true;
		}

		new Thread(this).start();
	}

	private boolean inForeground() {
		ActivityManager activityManager = (ActivityManager) mApplication.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		if (appProcesses == null) {
			return false;
		}
			
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess == null) { 
				continue;
			}
			if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				if (mApplication.getPackageName().equals(appProcess.processName)) {
					return true;
				}
			}
		}

		return false;
	}
	
	protected void postEvent() {
		if (!mPostedLaunch) {
			mPostedLaunch = true;
			mLaunchEventTracker.uploadEvent();
		} else {
			new ResumeEventTracker(mApplication).uploadEvent();
		}
	}

	public void run() {
		try {
			Log.d(FiksuTrackingManager.FIKSU_LOG_TAG, "ForegroundTester thread started, process: " + Process.myPid());
			// Allow the INSTALL_REFERRER BroadcastReceiver to finish.
			Thread.sleep(6000);
			
			while (true) {
				Thread.sleep(5000);
				
				if (!mWasInForeground && inForeground()) {
					postEvent();
					mWasInForeground = true;
				} else if (mWasInForeground && !inForeground()) {
					mWasInForeground = false;
				}
			}
		} catch (InterruptedException ie) {
			Log.i(FiksuTrackingManager.FIKSU_LOG_TAG, "Sleep interrupted");
		}
	}
}
	
