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

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

class RatingPrompter {
	private final RatingClickListener mRatingButtonListener;
	private final RatingClickListener mNoRatingButtonListener;
	private final RatingClickListener mPostponeRatingButtonListener;
	private final String mAppName;
	private static final int NUMBER_OF_LAUNCHES_BEFORE_RATING = 5;
	private static final int NUMBER_OF_DAYS_BEFORE_RATING_IN_MILLIS = 5 * 24 * 60 * 60 * 1000;
	private static final String PREFERENCES_NAME_KEY = "Fiksu.ratingsDictionary";
	private static final String PREFERENCES_NUMBER_OF_LAUNCHES_KEY = "Fiksu.numberOfLaunches";
	private static final String PREFERENCES_FIRST_LAUNCHED_KEY = "Fiksu.firstLaunchedAt";
	private static final String PREFERENCES_ALREADY_RATED_KEY = "Fiksu.alreadyRated";
	private final Activity mActivity;
	
	public RatingPrompter(Activity activity) {
		mRatingButtonListener = new RatingClickListener(PromptResult.USER_RATED, activity);
		mNoRatingButtonListener = new RatingClickListener(PromptResult.USER_DID_NOT_RATE, activity);
		mPostponeRatingButtonListener = new RatingClickListener(PromptResult.USER_POSTPONED_RATING, activity);
		mActivity = activity;
		PackageManager packageManager = mActivity.getPackageManager();
		String packageName = mActivity.getPackageName();
		String appName = null;
		try {
			appName = packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString();
		} catch (PackageManager.NameNotFoundException pnfe) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Could not access package: " + packageName);
		}
		mAppName = appName;
	}
	
	private enum PromptResult {
		USER_RATED(1),
		USER_DID_NOT_RATE(2),
		USER_POSTPONED_RATING(3);
		
		private PromptResult(int value) {
		}
	}
	
	private void setUserRated() {
		SharedPreferences preferences = mActivity.getSharedPreferences(PREFERENCES_NAME_KEY, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(PREFERENCES_ALREADY_RATED_KEY, true);
		editor.commit();
	}
	
	private class RatingClickListener implements DialogInterface.OnClickListener {
		private PromptResult mPromptResult;

		RatingClickListener(PromptResult userRated, Activity activity) {
			mPromptResult = userRated;
		}
		
		public void onClick(DialogInterface dialog, int which) {
			switch (mPromptResult) {
			case USER_RATED:
				new RatingEventTracker(mActivity, "rated", NUMBER_OF_LAUNCHES_BEFORE_RATING).uploadEvent();
				Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, mActivity.getPackageName());
				setUserRated();
				mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + mActivity.getPackageName())));
				break;
			case USER_DID_NOT_RATE:
				new RatingEventTracker(mActivity, "did_not_rate", NUMBER_OF_LAUNCHES_BEFORE_RATING).uploadEvent();
				setUserRated();
				break;
			case USER_POSTPONED_RATING:
				new RatingEventTracker(mActivity, "deferred_rating", NUMBER_OF_LAUNCHES_BEFORE_RATING).uploadEvent();
				break;
			default:
				break;
			}
		}
	}
	
	private boolean connectedToNetwork() {
		ConnectivityManager connectivityManager = (ConnectivityManager)mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected());
	}
	
	private int getNumberOfLaunches(SharedPreferences preferences, SharedPreferences.Editor editor) {
		int numberOfLaunches = preferences.getInt(PREFERENCES_NUMBER_OF_LAUNCHES_KEY, 0);
		numberOfLaunches += 1;
		
		editor.putInt(PREFERENCES_NUMBER_OF_LAUNCHES_KEY, numberOfLaunches);

		return numberOfLaunches;
	}
	
	private boolean enoughTimeSinceFirstLaunch(SharedPreferences preferences, SharedPreferences.Editor editor) {
		Date now = new Date();
		long firstLaunchedAtMillis = preferences.getLong(PREFERENCES_FIRST_LAUNCHED_KEY, now.getTime());
		boolean enoughTimeHasPassed = (now.getTime() - firstLaunchedAtMillis > NUMBER_OF_DAYS_BEFORE_RATING_IN_MILLIS);
		
		if (now.getTime() == firstLaunchedAtMillis) {
			editor.putLong(PREFERENCES_FIRST_LAUNCHED_KEY, now.getTime());
		}

		return enoughTimeHasPassed;
	}
	
	private boolean shouldPrompt() {
		if (mAppName == null) {
			return false;
		}

		if (!connectedToNetwork()) {
			return false;
		}
		
		SharedPreferences preferences = mActivity.getSharedPreferences(PREFERENCES_NAME_KEY, 0);
		
		if (preferences.getBoolean(PREFERENCES_ALREADY_RATED_KEY, false)) {
			return false;
		}
		
		SharedPreferences.Editor editor = preferences.edit();

		int numberOfLaunches = getNumberOfLaunches(preferences, editor);
		
		boolean enoughTimeHasPassed = enoughTimeSinceFirstLaunch(preferences, editor);
		
		editor.commit();
		
		if (numberOfLaunches < NUMBER_OF_LAUNCHES_BEFORE_RATING || !enoughTimeHasPassed) {
			return false;
		}

		return true;
	}
	
	public void maybeShowPrompt() {
		if (!shouldPrompt()) {
			return;
		}
		
		AlertDialog.Builder ratingDialogBuilder = new AlertDialog.Builder(mActivity);
		ratingDialogBuilder.setTitle("Enjoying " + mAppName + "?");
		ratingDialogBuilder.setMessage("If so, please rate it in the Android Marketplace.  It takes less than a minute and we appreciate your support!");
		ratingDialogBuilder.setPositiveButton("Rate " + mAppName, mRatingButtonListener);
		ratingDialogBuilder.setNegativeButton("No thanks", mNoRatingButtonListener);
		ratingDialogBuilder.setNeutralButton("Remind me later", mPostponeRatingButtonListener);
		ratingDialogBuilder.show();
	}
}
