package com.bsb.hike.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;
import com.bsb.hike.ui.MessagesList;

/**
 * @author Rishabh Using this to notify the server when the app comes to the
 *         foreground or background. Extending PreferenceActivity to ensure all
 *         our activities can extend this class
 */
public abstract class HikeAppStateBaseActivity extends Activity {

	private static final String TAG = "HikeAppState";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED
				|| HikeMessengerApp.currentState == CurrentState.CLOSED) {
			Log.d(TAG, "App was opened");
			HikeMessengerApp.currentState = CurrentState.OPENED;
			Utils.sendAppState(this);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		com.facebook.Settings.publishInstallAsync(this,
				HikeConstants.APP_FACEBOOK_ID);
	}

	@Override
	protected void onStart() {
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED
				|| HikeMessengerApp.currentState == CurrentState.CLOSED) {
			Log.d(TAG, "App was resumed");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.sendAppState(this);
		}
		super.onStart();
	}

	@Override
	public void onBackPressed() {
		if (this instanceof MessagesList) {
			// Closing the app.
			HikeMessengerApp.currentState = CurrentState.CLOSED;
		} else {
			HikeMessengerApp.currentState = CurrentState.NEW_ACTIVITY;
		}

		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		if (HikeMessengerApp.currentState == CurrentState.CLOSED) {
			Log.d(TAG, "App was closed");
			Utils.sendAppState(this);
		} else if (HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY) {
			Log.d(TAG, "App was going to another activity");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
		} else {
			Log.d(TAG, "App was backgrounded");
			HikeMessengerApp.currentState = CurrentState.BACKGROUNDED;
			Utils.sendAppState(this);
		}
		super.onStop();
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		HikeMessengerApp.currentState = requestCode == -1
				|| requestCode == HikeConstants.SHARE_LOCATION_CODE ? CurrentState.NEW_ACTIVITY
				: CurrentState.BACKGROUNDED;
		super.startActivityForResult(intent, requestCode);
	}
}
