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
		String packageName;
		String applicationName;
		long installTime;

		public AppLogPojo() {
		}
	}

	public static ArrayList<AppLogPojo> getAllAppLogs(Context context) {
		ArrayList<AppLogPojo> res = new ArrayList<AppLogPojo>();
		List<PackageInfo> packs = context.getPackageManager()
				.getInstalledPackages(0);
		for (int i = 0; i < packs.size(); i++) {
			PackageInfo p = packs.get(i);
			if (p.versionName == null) {
				continue;
			}
			AppLogPojo newInfo = new AppLogPojo();
			newInfo.applicationName = p.applicationInfo.loadLabel(
					context.getPackageManager()).toString();
			newInfo.installTime = new File(p.applicationInfo.sourceDir)
					.lastModified();
			newInfo.packageName = p.packageName;
			res.add(newInfo);
		}
		return res;

	}

	private static JSONArray getJSONAppArray(ArrayList<AppLogPojo> arrayAL)
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

	private static JSONObject encryptJSON(JSONArray jsonArray, int flag) {
		
		JSONObject jsonObj = new JSONObject();
		
		try {
			String jsonKey = null;
			switch(flag){
				case(APP_ANALYTICS_FLAG) : jsonKey = HikeConstants.APP_LOG_ANALYTICS; break;
				case(CALL_ANALYTICS_FLAG) : jsonKey = HikeConstants.CALL_LOG_ANALYTICS; break;
				case(LOCATION_ANALYTICS_FLAG) : jsonKey = HikeConstants.LOCATION_LOG_ANALYTICS; break;
			}
			jsonObj.putOpt(jsonKey, AESEncryption.encrypt(jsonArray.toString()));
		} catch (JSONException e) {
			Logger.d(TAG, e.toString());
		}
		Logger.d(TAG, "sending analytics : " + jsonObj.toString());
		return jsonObj;

	}

	public static void sendLogs(Context ctx, int flags) throws JSONException {
		
		SharedPreferences settings = ctx.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		
		String key = settings.getString(HikeMessengerApp.MSISDN_SETTING, "");
		//for the case when AI packet will not send us the AI packet
		String salt = settings.getString(HikeMessengerApp.BACKUP_TOKEN_SETTING, SALT_DEFAULT);
		AESEncryption.makeKey(key + salt, HASH_SCHEME);

		JSONObject jsonObj = encryptJSON(getJSONAppArray(getAllAppLogs(ctx)), APP_ANALYTICS_FLAG);
		// JSONObject jsonObj = encryptJSON(ja);
		//JSONObject jsonObj = encryptJSON(getJSONCallArray(getCallLogs(context.getContentResolver())), CALL_ANALYTICS_FLAG);

		HikeHttpRequest appLogRequest = new HikeHttpRequest("/" + HikeConstants.APP_LOG_ANALYTICS, RequestType.OTHER,
				new HikeHttpRequest.HikeHttpCallback() {
					public void onFailure() {
						Logger.d(TAG, "failure");
					}

					public void onSuccess(JSONObject response) {
						Logger.d(TAG, response.toString());
					}

				});
		appLogRequest.setJSONData(jsonObj);
		HikeHTTPTask hht = new HikeHTTPTask(null, 0);
		Utils.executeHttpTask(hht, appLogRequest);
	}
	
	public static Map<String,Map<String,Integer>> getCallLogs(ContentResolver cr){
		
		Map<String, Map<String, Integer>> callLogsMap = new HashMap<String, Map<String, Integer>>();
		Map<String, Integer> callMap = null;
		
		String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
		String[] projection = new String[] { CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION };
		String selection = CallLog.Calls.DATE + " > ?";
		String[] selectors = new String[] { String.valueOf(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 30)) };
		Uri callUri = CallLog.Calls.CONTENT_URI;
		Uri callUriLimited = callUri.buildUpon().appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, "500").build();
		
		Cursor cur = cr.query(callUriLimited, projection, null, null, strOrder);
		
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
		} finally {
			cur.close();
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
