package com.bsb.hike.utils;

import android.app.Activity;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikeMessengerApp.CurrentState;

public class HikeAppStateUtils
{

	private static final String TAG = "HikeAppState";

	public static void onCreate(Activity activity)
	{
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED || HikeMessengerApp.currentState == CurrentState.CLOSED)
		{
			Logger.d(TAG, "App was opened");
			HikeMessengerApp.currentState = CurrentState.OPENED;
			Utils.appStateChanged(activity.getApplicationContext());
		}

	}

	public static void onResume(Activity activity)
	{
		com.facebook.Settings.publishInstallAsync(activity, HikeConstants.APP_FACEBOOK_ID);
	}

	public static void onStart(Activity activity)
	{
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED || HikeMessengerApp.currentState == CurrentState.CLOSED)
		{
			Logger.d(TAG, "App was resumed");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.appStateChanged(activity.getApplicationContext());
		}
	}

	public static void onBackPressed()
	{
		HikeMessengerApp.currentState = CurrentState.BACK_PRESSED;
	}

	public static void onStop(Activity activity)
	{
		Logger.d(TAG, "OnStop");
		if (HikeMessengerApp.currentState == CurrentState.NEW_ACTIVITY)
		{
			Logger.d(TAG, "App was going to another activity");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
		}
		else if (HikeMessengerApp.currentState == CurrentState.BACK_PRESSED)
		{
			HikeMessengerApp.currentState = CurrentState.RESUMED;
		}
		else
		{
			if(Utils.isHoneycombOrHigher() && activity.isChangingConfigurations())
			{
				Logger.d(TAG, "App was going to another activity");
				HikeMessengerApp.currentState = CurrentState.RESUMED;
				return;
			}
			Logger.d(TAG, "App was backgrounded");
			HikeMessengerApp.currentState = CurrentState.BACKGROUNDED;
			Utils.appStateChanged(activity.getApplicationContext(), true, true);
		}
	}

	public static void finish()
	{
		HikeMessengerApp.currentState = CurrentState.BACK_PRESSED;
	}

	public static void startActivityForResult()
	{
		HikeMessengerApp.currentState = CurrentState.NEW_ACTIVITY;
	}

	public static void onActivityResult(Activity activity)
	{
		if (HikeMessengerApp.currentState == CurrentState.BACKGROUNDED)
		{
			Logger.d(TAG, "App returning from activity with result");
			HikeMessengerApp.currentState = CurrentState.RESUMED;
			Utils.appStateChanged(activity.getApplicationContext());
		}
	}

}
