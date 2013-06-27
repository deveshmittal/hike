package com.bsb.hike.service;

import com.mobileapptracker.Tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppTrackerReceiver extends BroadcastReceiver {
	@Override
    public void onReceive(Context context, Intent intent) {
       
		// Initialize the Mobile App tracker Intent
		Tracker tracker = new Tracker();
		tracker.onReceive(context, intent);

		// Initialize and call onReceive for other receivers
		ReferralReceiver refRec = new ReferralReceiver();
		refRec.onReceive(context, intent);
    }
}
