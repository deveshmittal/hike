package com.bsb.hike.utils;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;

public abstract class HikeAppStateBasePreferenceActivity extends SherlockPreferenceActivity
{

	private static final String TAG = "HikeAppState";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED || HikeMessengerApp.currentState == CurrentState.CLOSED)
		{
			Logger.d(TAG + getClass().getSimpleName(), "App was opened");
			HikeMessengerApp.currentState = CurrentState.OPENED;
			Utils.sendAppState(this);
		}

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		com.facebook.Settings.publishInstallAsync(this, HikeConstants.APP_FACEBOOK_ID);
	}

	@Override
	protected void onStart()
	{
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED || HikeMessengerApp.currentState == CurrentState.CLOSED)
		{
			Logger.d(TAG + getClass().getSimpleName(), "App was resumed");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.sendAppState(this);

		}
		super.onStart();
	}

	@Override
	public void onBackPressed()
	{
		HikeMessengerApp.currentState = CurrentState.BACK_PRESSED;
		super.onBackPressed();
	}

	@Override
	protected void onStop()
	{
		Logger.d(TAG + getClass().getSimpleName(), "OnStop");
		if (HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY)
		{
			Logger.d(TAG + getClass().getSimpleName(), "App was going to another activity");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
		}
		else if (HikeMessengerApp.currentState == CurrentState.BACK_PRESSED)
		{
			HikeMessengerApp.currentState = CurrentState.RESUMED;
		}
		else
		{
			Logger.d(TAG + getClass().getSimpleName(), "App was backgrounded");
			HikeMessengerApp.currentState = CurrentState.BACKGROUNDED;
			Utils.sendAppState(this);
		}
		super.onStop();
	}

	@Override
	public void finish()
	{
		HikeMessengerApp.currentState = CurrentState.BACK_PRESSED;
		super.finish();
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode)
	{
		HikeMessengerApp.currentState = requestCode == -1 || requestCode == HikeConstants.SHARE_LOCATION_CODE || requestCode == HikeConstants.CROP_RESULT ? CurrentState.NEW_ACTIVITY
				: CurrentState.BACKGROUNDED;
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED)
		{
			Logger.d(TAG + getClass().getSimpleName(), "App returning from activity with result");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.sendAppState(this);
		}
	}
}
