package com.bsb.hike.userlogs;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.provider.CallLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressLint("SimpleDateFormat")
public class CallLogs extends Activity {
	
	private static final String MISSED_CALL_COUNT = "m";
	private static final String RECEIVED_CALL_COUNT = "r";
	private static final String SENT_CALL_COUNT = "s";
	private static final String RECEIVED_CALL_DURATION = "rd";
	private static final String SENT_CALL_DURATION = "sd";
	private static final String PHONE_NUMBER = "ph";
	
		
	
	private static SecretKeySpec secretKey;
	private static byte[] key;
	private static String initV = "0011223344556677";
	private static String decryptedString;
	private static String encryptedString;

	public static void setKey(String myKey) {

		MessageDigest sha = null;
		try {
			key = myKey.getBytes("UTF-8");
			System.out.println(key.length);
			sha = MessageDigest.getInstance("MD5");
			key = sha.digest(key);
			// key = Arrays.copyOf(key, 16); // use only first 128 bit
			System.out.println(key.length);

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < key.length; i++)
				hexString.append(Integer.toHexString(0xFF & key[i]));
			System.out.println(hexString.toString());

			System.out.println(Base64.encode(key, 0));
			secretKey = new SecretKeySpec(key, "AES");

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static String getDecryptedString() {
		return decryptedString;
	}

	public static void setDecryptedString(String decryptedString) {
		decryptedString = decryptedString;
	}

	public static String getEncryptedString() {
		return encryptedString;
	}

	public static void setEncryptedString(String encryptedString) {
		encryptedString = encryptedString;
	}

	public static String encrypt(String strToEncrypt) {
		String hello = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(initV.getBytes("UTF-8"));
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			hello = new String(Base64.encode(
					cipher.doFinal(strToEncrypt.getBytes("UTF-8")),
					Base64.NO_WRAP));
			setEncryptedString(hello);
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return hello;
	}

	public static String decrypt(String strToDecrypt) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(initV.getBytes("UTF-8"));
			cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			setDecryptedString(new String(cipher.doFinal(Base64.decode(
					strToDecrypt, 0))));
		} catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getAllCallLogs(getApplicationContext().getContentResolver());
	}

	
	public void getAllCallLogs(ContentResolver cr) {
		// reading all data in descending order according to DATE

		Map<String, Map<String, Integer>> callLogsMap = new HashMap<String, Map<String, Integer>>();

		Map<String, Integer> callMap = null;
		String strOrder = android.provider.CallLog.Calls.DATE + " DESC";

		String[] projection = new String[] { CallLog.Calls.NUMBER,
				CallLog.Calls.DATE, CallLog.Calls.CACHED_NAME,
				CallLog.Calls.TYPE, CallLog.Calls.NEW, CallLog.Calls.DURATION };
		String[] selectors = new String[] { String.valueOf(System
				.currentTimeMillis() - (1000 * 60 * 60 * 24 * 30)) };
		Uri callU = CallLog.Calls.CONTENT_URI;
		Uri callUriLimited = callU.buildUpon()
				.appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, "500")
				.build();
		String selection = CallLog.Calls.DATE + " > ?";
		Log.d("UmangX", String.valueOf(System.currentTimeMillis()));
		Cursor cur = cr.query(callUriLimited, projection, null, null, strOrder);
		// loop through cursor
		Log.d("UmangX", String.valueOf(System.currentTimeMillis()));
		String results;
		try {
			while (cur.moveToNext()) {

				results = new String();

				String callNumber = cur.getString(cur
						.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
				results += (" " + callNumber);

				String callName = cur
						.getString(cur
								.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME));
				results += (" " + callName);

				String callDate = cur.getString(cur
						.getColumnIndex(android.provider.CallLog.Calls.DATE));
				results += (" " + callDate);
				SimpleDateFormat formatter = new SimpleDateFormat(
						"dd-MMM-yyyy HH:mm");
				String dateString = formatter.format(new Date(Long
						.parseLong(callDate)));
				results += (" " + dateString);

				String callType = cur.getString(cur
						.getColumnIndex(android.provider.CallLog.Calls.TYPE));
				results += (" " + callType);

				String isCallNew = cur.getString(cur
						.getColumnIndex(android.provider.CallLog.Calls.NEW));
				results += (" " + isCallNew);

				String duration = cur
						.getString(cur
								.getColumnIndex(android.provider.CallLog.Calls.DURATION));
				results += (" " + duration);

				if (Long.parseLong(callDate) > (System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 30))) {
					if (!callLogsMap.containsKey(callNumber)) {
						callMap = new HashMap<String, Integer>();
						callMap.put(MISSED_CALL_COUNT, 0);
						callMap.put(SENT_CALL_COUNT, 0);
						callMap.put(RECEIVED_CALL_COUNT, 0);
						callMap.put(SENT_CALL_DURATION, 0);
						callMap.put(RECEIVED_CALL_DURATION, 0);
						callMap.put("ss", 0);
						callMap.put("sr", 0);

					} else {
						callMap = callLogsMap.get(callNumber);

					}

					switch ( cur.getInt(cur.getColumnIndex(android.provider.CallLog.Calls.TYPE))) {
					case CallLog.Calls.MISSED_TYPE:
						callMap.put(MISSED_CALL_COUNT, callMap.get(MISSED_CALL_COUNT) + 1);
						break;
					case CallLog.Calls.OUTGOING_TYPE:
						callMap.put(SENT_CALL_COUNT, callMap.get(SENT_CALL_COUNT) + 1);
						callMap.put(SENT_CALL_DURATION,
								callMap.get(SENT_CALL_DURATION) + Integer.parseInt(duration));
						break;
					case CallLog.Calls.INCOMING_TYPE:
						callMap.put(RECEIVED_CALL_COUNT, callMap.get(RECEIVED_CALL_COUNT) + 1);
						callMap.put(RECEIVED_CALL_DURATION,
								callMap.get(RECEIVED_CALL_DURATION) + Integer.parseInt(duration));
						break;

					}
					callLogsMap.put(callNumber, callMap);
				}
				Log.d("UmangX : ", results);

			}

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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				callSmsJsonArray.put(callSmsJsonObj);
			}
			Log.d("Umang",
					" system millis: " + callSmsJsonArray.toString() + " " + callLogsMap.toString());

			final String key = "+919015215290";
			final String salt = "umangjeet";
			final String strPssword = key + salt;
			setKey(strPssword);
			encrypt(callSmsJsonArray.toString());
			JSONObject jo = new JSONObject();
			try {
				jo.putOpt("cl", encrypt(callSmsJsonArray.toString()));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d("Umang", jo.toString());

		} finally {
			cur.close();
		}
	}

}

