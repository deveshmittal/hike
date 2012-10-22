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

class RegistrationEventTracker extends EventTracker {
	RegistrationEventTracker(Context context, String username) {
		super(context, "Registration");
		addParameter("username", username);
	}
}
