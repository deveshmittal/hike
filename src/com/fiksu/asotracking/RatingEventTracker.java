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

import com.fiksu.asotracking.EventTracker;

class RatingEventTracker extends EventTracker {
	RatingEventTracker(Context context, String outcome, int launches) {
		super(context, "Rating");
		addParameter("tvalue", outcome);
		addParameter("ivalue", new Integer(launches).toString());
	}
	
	public void uploadEvent() {
		super.uploadEvent();
	}
}
