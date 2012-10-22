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

import android.content.Context;

class ResumeEventTracker extends EventTracker {
	ResumeEventTracker(Context context) {
		this(context, false);
	}
	
	ResumeEventTracker(Context context, boolean notification) {
		super(context, notification ? "NotificationResume" : "Resume");
	}
}
