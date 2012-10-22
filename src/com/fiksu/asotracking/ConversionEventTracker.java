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

class ConversionEventTracker extends EventTracker {
	ConversionEventTracker(Context context, String referrer) {
		super(context, "Conversion");
		addParameter("tvalue", referrer);
	}
}
