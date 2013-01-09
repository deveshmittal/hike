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

import com.bsb.hike.HikeMessengerApp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

public class FiksuTrackingManager {
	static final String FIKSU_LOG_TAG = "FiksuTracking";

	public static void initialize(Application application) {
		if(HikeMessengerApp.isIndianUser()) {
			return;
		}
		new ForegroundTester(application, new LaunchEventTracker(application));
		InstallTracking.checkForFiksuReceiver(application);
	}

	public static void uploadPurchaseEvent(Context context, String username, double price, String currency) {
		if(HikeMessengerApp.isIndianUser()) {
			return;
		}
		new PurchaseEventTracker(context, username, price, currency).uploadEvent();
	}
	
	public static void uploadRegistrationEvent(Context context, String username) {
		if(HikeMessengerApp.isIndianUser()) {
			return;
		}
		new RegistrationEventTracker(context, username).uploadEvent();
	}
	
	public static void c2dMessageReceived(Context context) {
		if(HikeMessengerApp.isIndianUser()) {
			return;
		}
		EventTracker.c2dMessageReceived(context);
	}
	
	public static void promptForRating(Activity activity) {
		if(HikeMessengerApp.isIndianUser()) {
			return;
		}
		new RatingPrompter(activity).maybeShowPrompt();
	}
}
