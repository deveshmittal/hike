package com.bsb.hike.userlogs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;


public class UserLogInfo {

	private static final String HASH_SCHEME = "MD5";
	private static final String SALT_DEFAULT = "umangjeet";
	private static final String TAG = "UserLogInfo";
	
	public static final int CALL_ANALYTICS_FLAG = 1;
	public static final int APP_ANALYTICS_FLAG = 2;	
	public static final int LOCATION_ANALYTICS_FLAG = 4;
	
	private static final long milliSecInDay = 1000 * 60 * 60 * 24;
	private static final int DAYS_TO_LOG = 30;
	private static final int MAX_CURSOR_LIMIT = 500;

	private static final String MISSED_CALL_COUNT = "m";
	private static final String RECEIVED_CALL_COUNT = "r";
	private static final String SENT_CALL_COUNT = "s";
	private static final String RECEIVED_CALL_DURATION = "rd";
	private static final String SENT_CALL_DURATION = "sd";
	private static final String PHONE_NUMBER = "ph";
	
	private static final String SENT_SMS = "ss";
	private static final String RECEIVED_SMS = "rs";

	private static final String PACKAGE_NAME = "pn";
	private static final String APPLICATION_NAME = "an";
	private static final String INSTALL_TIME = "it";

	public static class AppLogPojo {
		final String packageName;
		final String applicationName;
		final long installTime;

		public AppLogPojo(String packageName, String applicationName, long installTime) {
			this.packageName = packageName;
			this.applicationName = applicationName;
			this.installTime = installTime;
		}
	}
	
	public static class CallLogPojo {
		final String phoneNumber;
		int missedCallCount;
		int receivedCallCount;
		int sentCallCount;
		int sentCallDuration;
		int receivedCallDuration;

		public CallLogPojo(String phoneNumber, int missedCallCount, int receivedCallCount, 
				int sentCallCount, int sentCallDuration, int receivedCallDuration) {
			this.phoneNumber = phoneNumber;
			this.missedCallCount = missedCallCount;
			this.receivedCallCount = receivedCallCount;
			this.sentCallCount = sentCallCount;
			this.sentCallDuration = sentCallDuration;
			this.receivedCallDuration = receivedCallDuration;
		}
	}

	public static List<AppLogPojo> getAppLogs(Context ctx) {
		
		List<AppLogPojo> appLogList = new ArrayList<AppLogPojo>();
		List<PackageInfo> packInfoList = ctx.getPackageManager().getInstalledPackages(0);
		
		for(PackageInfo pi : packInfoList){
			
			if (pi.versionName == null)
				continue;
			AppLogPojo appLog = new AppLogPojo(
					pi.packageName,
					pi.applicationInfo.loadLabel(ctx.getPackageManager()).toString(),
					new File(pi.applicationInfo.sourceDir).lastModified());
			appLogList.add(appLog);
			
		}
		return appLogList;

	}

	private static JSONArray getJSONAppArray(List<AppLogPojo> arrayAL)
			throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (AppLogPojo AL : arrayAL) {
			JSONObject jsonObj = new JSONObject();
			jsonObj.putOpt(APPLICATION_NAME, AL.applicationName);
			jsonObj.putOpt(PACKAGE_NAME, AL.applicationName);
			jsonObj.putOpt(INSTALL_TIME, AL.installTime);
			jsonArray.put(jsonObj);
		}
		return jsonArray;

	}
	
	private static String getLogKey(int flag) {
		String jsonKey = null;
		switch (flag) {
			case (APP_ANALYTICS_FLAG): jsonKey = HikeConstants.APP_LOG_ANALYTICS; break;
			case (CALL_ANALYTICS_FLAG): jsonKey = HikeConstants.CALL_LOG_ANALYTICS; break;
			case (LOCATION_ANALYTICS_FLAG): jsonKey = HikeConstants.LOCATION_LOG_ANALYTICS; break;
		}
		return jsonKey;
	}

	private static JSONObject getEncryptedJSON(Context ctx, int flag) throws JSONException {
		
		JSONArray jsonLogArray = null;
		switch(flag){
			case APP_ANALYTICS_FLAG : jsonLogArray = getJSONAppArray(getAppLogs(ctx)); break;
			//case CALL_ANALYTICS_FLAG : jsonLogArray = getJSONCallArray(getCallLogs(ctx)); break;
			//case LOCATION_ANALYTICS_FLAG : jsonArray = getJSONLocationArray(getAllLocationLogs(ctx)); break;	
		}
		
		JSONObject jsonLogObj = new JSONObject();
		if(jsonLogArray != null)
			jsonLogObj.putOpt(getLogKey(flag), AESEncryption.encrypt(jsonLogArray.toString()));
		Logger.d(TAG, "sending analytics : " + jsonLogObj.toString());
		return jsonLogObj;

	}

	public static void sendLogs(Context ctx, int flags) throws JSONException {
		
		JSONObject jsonLogData = getEncryptedJSON(ctx, flags);
		
		if(jsonLogData != null){
			SharedPreferences settings = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			
			String key = settings.getString(HikeMessengerApp.MSISDN_SETTING, "");
			//for the case when AI packet will not send us the backup Token
			String salt = settings.getString(HikeMessengerApp.BACKUP_TOKEN_SETTING, SALT_DEFAULT);
			AESEncryption.makeKey(key + salt, HASH_SCHEME);
			
			HikeHttpRequest userLogRequest = new HikeHttpRequest("/" + getLogKey(flags), RequestType.OTHER,
					new HikeHttpRequest.HikeHttpCallback() {
						public void onFailure() {
							Logger.d(TAG, "failure");
						}

						public void onSuccess(JSONObject response) {
							Logger.d(TAG, response.toString());
						}

					});
			userLogRequest.setJSONData(jsonLogData);
			HikeHTTPTask hht = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hht, userLogRequest);
			
		}
	}
	
	public static List<CallLogPojo> getCallLogs(Context ctx){
		
		//Map is being used to store and retrieve values multiple times
		Map<String, CallLogPojo> callLogMap = new HashMap<String, CallLogPojo>();
		
		String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
		String[] projection = new String[] { CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION };
		String selection = CallLog.Calls.DATE + " > ?";
		String[] selectors = new String[] { String.valueOf(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 30)) };
		Uri callUri = CallLog.Calls.CONTENT_URI;
		Uri callUriLimited = callUri.buildUpon()
				.appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, String.valueOf(MAX_CURSOR_LIMIT))
				.build();
		
		ContentResolver cr = ctx.getContentResolver();
		Cursor cur = cr.query(callUriLimited, projection, null, null, strOrder);
		
		if (cur != null) { 
			try {
				while (cur.moveToNext()) {
	
					String callNumber = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
					String callDate = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.DATE));				
					int duration = cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.DURATION));
	
					if (Long.parseLong(callDate) > (System.currentTimeMillis() - (milliSecInDay * DAYS_TO_LOG))) {

						CallLogPojo callLog = null;
						
						if(callLogMap.containsKey(callNumber)){
							callLog = callLogMap.get(callNumber);
						} else {
							callLog = new CallLogPojo(callNumber,0,0,0,0,0);
						}
	
						switch (cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.TYPE))) {
						case CallLog.Calls.MISSED_TYPE : 
							callLog.missedCallCount++;
							break;
						case CallLog.Calls.OUTGOING_TYPE :
							callLog.sentCallCount++;
							callLog.sentCallDuration += duration;
							break;
						case CallLog.Calls.INCOMING_TYPE : 
							callLog.receivedCallCount++;
							callLog.receivedCallDuration += duration;
							break;
	
						}
						callLogMap.put(callNumber, callLog);
					}
	
				}
			} catch (Exception e) {
				Logger.d(TAG, e.toString());
			} finally {
				cur.close();
			}
		}
		
		List<CallLogPojo> callLogList = new ArrayList<CallLogPojo>(callLogMap.size());
		for (Entry<String, CallLogPojo> entry : callLogMap.entrySet()) {
	        callLogList.add(entry.getValue());
	    }
		return callLogList;
		
	}
	
	private static JSONArray getJSONCallArray(List<CallLogPojo> callLogList) throws JSONException {
		JSONArray callJsonArray = new JSONArray();
		for(CallLogPojo callLog : callLogList){
			JSONObject jsonObj = new JSONObject();
			jsonObj.putOpt(PHONE_NUMBER, callLog.phoneNumber);
			jsonObj.putOpt(MISSED_CALL_COUNT, callLog.missedCallCount);
			jsonObj.putOpt(SENT_CALL_COUNT, callLog.sentCallCount);
			jsonObj.putOpt(RECEIVED_CALL_COUNT, callLog.receivedCallCount);
			jsonObj.putOpt(SENT_CALL_DURATION, callLog.sentCallDuration);
			jsonObj.putOpt(RECEIVED_CALL_DURATION, callLog.receivedCallDuration);
			callJsonArray.put(jsonObj);
		}
		Logger.d(TAG, callJsonArray.toString());
		return callJsonArray;
	}

}
