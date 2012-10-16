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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

class EventUploader implements Runnable {
	private static final String FIKSU_SEPARATOR = "<FIKSU>";
	private static final int MAX_FAILED_URLS = 10;
	private final Map<String, String> mParameters;
	private final Context mContext;
	
	EventUploader(Context context, Map<String, String> parameters) {
		this.mParameters = parameters;
		this.mContext = context;
	}

	private String encodeParameter(String parameter) throws UnsupportedEncodingException {
		return URLEncoder.encode(parameter, "UTF-8");
	}
	
	private boolean launchedFromNotification() {
		synchronized(EventTracker.SHARED_PREFERENCES_LOCK) {
			SharedPreferences preferences = EventTracker.getOurSharedPreferences(mContext);
			long now = new Date().getTime();
			long then = preferences.getLong("Fiksu.cd2MessageTime", 0);
			if ((now - then) < 3 * 60 * 1000) {
				return true;
			}
		}
		return false;
	}

	private String buildURL() {
		if (mContext == null) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Could not find context to use.  Please set it in your main Activity class using EventTracking.setContext().");
			return null;
		}
		
		String event = mParameters.get("event");
		Log.d(FiksuTrackingManager.FIKSU_LOG_TAG, "Event: " + event);

		String httpPrefix = "https://";

		String hostname = httpPrefix + "asotrack1.fluentmobile.com/";

		if ((event.equals("Launch") || event.equals("Resume")) && launchedFromNotification()) {
			hostname = httpPrefix + "asotrack2.fluentmobile.com/";
			event = "Notification" + event;
		}

		String ourVersion = "$Rev: 28663 $";
		String packageName = mContext.getPackageName();

		String url = hostname + ourVersion.split(" ")[1] + "/android/" + packageName + "/event?";

		try {
			url += "appid=" + mContext.getPackageName();

			String aid = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
			if (aid == null) {
				Log.e(FiksuTrackingManager.FIKSU_LOG_TAG,
					  "Could not retrieve android_id.  The android_id is not available on emulators running Android 2.1 or below.  " + 
					  "Run the code on emulator 2.2 or better or an a device.");
				aid = "";
			}
			
			String deviceId = "";
			try {
				TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService("phone");
				if (telephonyManager == null) {
					Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Could not access telephonyManager.");
					deviceId = "";
				} else {
					deviceId = telephonyManager.getDeviceId();
					if ((deviceId == null)  || (deviceId.length() == 0)) {
						Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Could not retrieve deviceId. ");
						deviceId = "";
					}
				}
			} catch (SecurityException se) {
				Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "READ_PHONE_STATE permission not granted. " +
					  "Could not retrieve deviceId. ");
				deviceId = "";
			}

			url += "&deviceid=" + deviceId;
			url += "&udid=" + aid;
			url += "&device=" + encodeParameter(android.os.Build.MODEL);
			
			try {
				PackageManager packageManager = mContext.getPackageManager();
				url += "&app_version=" + encodeParameter(packageManager.getPackageInfo(packageName, 0).versionName);
				
				String appName = packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString();
				if (appName != null) {
					url += "&app_name=" + encodeParameter(appName);
				}
			} catch (PackageManager.NameNotFoundException pnfe) {
				Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Could not access package: " + packageName);
			}
			url += "&system_version=" + android.os.Build.VERSION.RELEASE;
			url += "&system_name=" + encodeParameter(android.os.Build.PRODUCT);
			Locale locale = mContext.getResources().getConfiguration().locale;
			url += "&country=" + encodeParameter(locale.getCountry());
			url += "&lang=" + encodeParameter(locale.getLanguage());
			url += "&timezone=" + encodeParameter(TimeZone.getDefault().getDisplayName());

			url += "&gmtoffset=" + TimeZone.getDefault().getRawOffset() / 1000;

			if (event != null) {
				url += "&event=" +  event;
			}
			
			if (mParameters.get("username") != null) {
				url += "&username=" + encodeParameter(mParameters.get("username"));
			}
			
			if (mParameters.get("tvalue") != null) {
				url += "&tvalue=" +  encodeParameter(mParameters.get("tvalue"));
			}

			if (mParameters.get("fvalue") != null) {
				url += "&fvalue=" +  encodeParameter(mParameters.get("fvalue"));
			}

			if (mParameters.get("ivalue") != null) {
				url += "&ivalue=" +  mParameters.get("ivalue");
			}
		}
		catch (UnsupportedEncodingException e) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Problem creating URL", e);
			return null;
		}
		  		
		return url;
	}
	
	private List<String> getSavedUrls() {
		List<String> urls = new ArrayList<String>();
		SharedPreferences preferences = EventTracker.getOurSharedPreferences(mContext);

		if (preferences != null) {
			String savedUrls = preferences.getString("Fiksu.savedUrls", "");
			if (savedUrls != null && !savedUrls.equals("")) {
				String [] urlArray = savedUrls.split(FIKSU_SEPARATOR);
				for (String tempUrl : urlArray) {
					urls.add(tempUrl);
				}
			}
		}
		return urls;
	}
	
	private void saveFailedUrls(List<String> failedUrls) {
		if (failedUrls.size() > MAX_FAILED_URLS) {
			// Discard oldest.
			failedUrls = new ArrayList<String>(failedUrls.subList(failedUrls.size() - MAX_FAILED_URLS, failedUrls.size()));
		}

		String urlsToSave = "";
		if (failedUrls.size() > 0) {
			urlsToSave += failedUrls.get(0);
			for (int i = 1; i < failedUrls.size(); ++i) {
				urlsToSave += FIKSU_SEPARATOR + failedUrls.get(i);
			}
		}

		SharedPreferences preferences = EventTracker.getOurSharedPreferences(mContext);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("Fiksu.savedUrls", urlsToSave);
		editor.commit();
	}
	
	private void uploadToTracking() {
		if (mContext == null) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Could not find context to use.  Please set it in your main Activity class using EventTracking.setContext().");
			return;
		}

		String url = buildURL();
		
		synchronized(EventTracker.SHARED_PREFERENCES_LOCK) {
			List<String> urls = getSavedUrls();

			if (url != null) {
				urls.add(url);
				if (mParameters.get("event").equals("Conversion")) {
					// Save the Conversion in case the process drops out from under us.
					saveFailedUrls(urls);
				}
			}

			List<String> failedUrls = new ArrayList<String>();
			for (String tempUrl : urls) {
				try {
					if (!doUpload(tempUrl)) {
						Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Upload failed for url.  Saving it for retry later: " + tempUrl);
						failedUrls.add(tempUrl);
					}
				} catch (java.net.MalformedURLException e) {
					android.util.Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, tempUrl);

					// If the URL was malformed, saving it and retrying will still fail, so don't save it.
					android.util.Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, e.toString());
				}
			}

			saveFailedUrls(failedUrls);
		}
	}
	
	private boolean doUpload(String url) throws java.net.MalformedURLException {
		java.net.URL fiksuUrl = null;
		fiksuUrl = new java.net.URL(url);
		
		int responseCode = 0;

		try {
			java.net.HttpURLConnection fiksuConnection = (java.net.HttpURLConnection)fiksuUrl.openConnection();
			responseCode = fiksuConnection.getResponseCode();
		} catch (java.io.IOException e) {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Failed to upload tracking information.");
			return false;
		}

		if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
			Log.d(FiksuTrackingManager.FIKSU_LOG_TAG, "Successfully uploaded tracking information.");
		} else {
			Log.e(FiksuTrackingManager.FIKSU_LOG_TAG, "Failed to upload tracking information, bad response: " + responseCode);
			return responseCode < 500 || responseCode > 599; // Only retry server errors. All others considered fatal.
		}
		
		return true;
	}
	
	public void run() {
		try {
			uploadToTracking();
		} finally {
			synchronized(this) {
				this.notifyAll();
			}
		}
	}
}
