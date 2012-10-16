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

import android.app.Application;

class LaunchEventTracker extends EventTracker {
	
	LaunchEventTracker(Application application) {
		this(application, false);
	}
	
	LaunchEventTracker(Application application, boolean notification) {
		super(application, notification ? "NotificationLaunch" : "Launch");
	}
}
