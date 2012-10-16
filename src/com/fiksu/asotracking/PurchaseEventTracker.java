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

class PurchaseEventTracker extends EventTracker {
	PurchaseEventTracker(Context context, String username, Double price, String currency) {
		super(context, "Purchase");
		addParameter("username", username);
		addParameter("fvalue", price.toString());
		addParameter("tvalue", currency);
	}
}
