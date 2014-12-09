package com.bsb.hike.userlogs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
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
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;


public class UserLogInfo {

	private static final String HASH_SCHEME = "MD5";
	private static final String SALT_DEFAULT = "umangjeet";
	private static final String TAG = "UserLogInfo";
	
	public static final int CALL_ANALYTICS_FLAG = 1;
	public static final int APP_ANALYTICS_FLAG = 2;	
	public static final int LOCATION_ANALYTICS_FLAG = 4;

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
		final int missedCallCount;
		final int receivedCallCount;
		final int sentCallCount;
		final int sentCallDuration;
		final int receivedCallDuration;

		public CallLogPojo(int missedCallCount, int receivedCallCount, int sentCallCount, 
				int sentCallDuration, int receivedCallDuration) {
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
			case CALL_ANALYTICS_FLAG : jsonLogArray = getJSONCallArray(getCallLogs(ctx)); break;
			case LOCATION_ANALYTICS_FLAG : jsonLogArray = getJSONLocationArray(ctx); break;	
		}
		
		JSONObject jsonLogObj = new JSONObject();
		jsonLogObj.putOpt(getLogKey(flag), AESEncryption.encrypt(jsonLogArray.toString()));
		
		Logger.d(TAG, "sending analytics : " + jsonLogObj.toString());
		return jsonLogObj;

	}
	
	private static JSONArray getJSONLocationArray(Context ctx) throws JSONException {
		LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		Location l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put(HikeConstants.LONGITUDE, l.getLongitude());
		jsonObj.put(HikeConstants.LATITUDE, l.getLatitude());
		jsonObj.put("rd",l.getAccuracy());
		return new JSONArray().put(jsonObj);
	}
	
	public static void sendLogs(Context ctx, int flags) throws JSONException {
		
		SharedPreferences settings = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		
		String key = settings.getString(HikeMessengerApp.MSISDN_SETTING, "");
		//for the case when AI packet will not send us the AI packet
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
		userLogRequest.setJSONData(getEncryptedJSON(ctx, flags));
		HikeHTTPTask hht = new HikeHTTPTask(null, 0);
		Utils.executeHttpTask(hht, userLogRequest);
	}
	
	public static Map<String,Map<String,Integer>> getCallLogs(Context ctx){
		
		Map<String, Map<String, Integer>> callLogsMap = new HashMap<String, Map<String, Integer>>();
		Map<String, Integer> callMap = null;
		
		String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
		String[] projection = new String[] { CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION };
		String selection = CallLog.Calls.DATE + " > ?";
		String[] selectors = new String[] { String.valueOf(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 30)) };
		Uri callUri = CallLog.Calls.CONTENT_URI;
		Uri callUriLimited = callUri.buildUpon().appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, "500").build();
		
		ContentResolver cr = ctx.getContentResolver();
		Cursor cur = cr.query(callUriLimited, projection, null, null, strOrder);
		
		if (cur != null) { 
			try {
				while (cur.moveToNext()) {
	
					String callNumber = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
					String callDate = cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.DATE));				
					int duration = cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.DURATION));
	
					if (Long.parseLong(callDate) > (System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 30))) {
						
						if (!callLogsMap.containsKey(callNumber)) {
							callMap = new HashMap<String, Integer>();
							callMap.put(MISSED_CALL_COUNT, 0);
							callMap.put(SENT_CALL_COUNT, 0);
							callMap.put(RECEIVED_CALL_COUNT, 0);
							callMap.put(SENT_CALL_DURATION, 0);
							callMap.put(RECEIVED_CALL_DURATION, 0);
							callMap.put(SENT_SMS, 0);
							callMap.put(RECEIVED_SMS, 0);
						} else {
							callMap = callLogsMap.get(callNumber);
						}
	
						switch (cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.TYPE))) {
						case CallLog.Calls.MISSED_TYPE : 
							callMap.put(MISSED_CALL_COUNT, callMap.get(MISSED_CALL_COUNT) + 1); 
							break;
						case CallLog.Calls.OUTGOING_TYPE:
							callMap.put(SENT_CALL_COUNT, callMap.get(SENT_CALL_COUNT) + 1);
							callMap.put(SENT_CALL_DURATION,callMap.get(SENT_CALL_DURATION) + duration);
							break;
						case CallLog.Calls.INCOMING_TYPE:
							callMap.put(RECEIVED_CALL_COUNT,callMap.get(RECEIVED_CALL_COUNT) + 1);
							callMap.put(RECEIVED_CALL_DURATION,callMap.get(RECEIVED_CALL_DURATION) + duration);
							break;
	
						}
						callLogsMap.put(callNumber, callMap);
					}
	
				}
			} catch (Exception e) {
				Logger.d(TAG, e.toString());
			} finally {
				cur.close();
			}
		}
		return callLogsMap;
		
	}
	
	private static JSONArray getJSONCallArray(Map<String, Map<String, Integer>> callLogsMap){
		JSONArray callSmsJsonArray = new JSONArray();
		Iterator<Entry<String, Map<String, Integer>>> entries = callLogsMap.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			String key = (String) entry.getKey();
			Map<String, Integer> value = (Map<String, Integer>) entry.getValue();
			JSONObject callSmsJsonObj = new JSONObject(value);
			try {
				callSmsJsonObj.putOpt(PHONE_NUMBER, key);
			} catch (JSONException e) {
				Logger.d(TAG, e.toString());
			}

			callSmsJsonArray.put(callSmsJsonObj);
		}
		Logger.d(TAG, callLogsMap.toString());
		return callSmsJsonArray;
		
	}

}
