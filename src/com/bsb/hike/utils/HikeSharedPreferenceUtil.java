package com.bsb.hike.utils;

import java.util.HashMap;

import com.bsb.hike.HikeMessengerApp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class HikeSharedPreferenceUtil
{
	private static final String DEFAULT_PREF_NAME = HikeMessengerApp.ACCOUNT_SETTINGS;

	public static final String CONV_UNREAD_COUNT = "ConvUnreadCount";
	
	private SharedPreferences hikeSharedPreferences;

	private Editor editor;

	private static HashMap<String, HikeSharedPreferenceUtil> hikePrefsMap = new HashMap<String, HikeSharedPreferenceUtil>();

	private static HikeSharedPreferenceUtil initializeHikeSharedPref(Context context, String argSharedPrefName)
	{
		HikeSharedPreferenceUtil hikeSharedPreferenceUtil = null;
		if (context != null)
		{
			hikeSharedPreferenceUtil = new HikeSharedPreferenceUtil();
			hikeSharedPreferenceUtil.hikeSharedPreferences = context.getSharedPreferences(argSharedPrefName, Activity.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
			hikeSharedPreferenceUtil.editor = hikeSharedPreferenceUtil.hikeSharedPreferences.edit();
			hikePrefsMap.put(argSharedPrefName, hikeSharedPreferenceUtil);
		}
		return hikeSharedPreferenceUtil;
	}

	public static HikeSharedPreferenceUtil getInstance(Context context)
	{
		if (hikePrefsMap.containsKey(DEFAULT_PREF_NAME))
		{
			return hikePrefsMap.get(DEFAULT_PREF_NAME);
		}
		else
		{
			return initializeHikeSharedPref(context.getApplicationContext(), DEFAULT_PREF_NAME);
		}
	}

	public static HikeSharedPreferenceUtil getInstance(Context context, String argSharedPrefName)
	{
		if (hikePrefsMap.containsKey(argSharedPrefName))
		{
			return hikePrefsMap.get(argSharedPrefName);
		}
		else
		{
			return initializeHikeSharedPref(context.getApplicationContext(), argSharedPrefName);
		}
	}

	private HikeSharedPreferenceUtil()
	{
		// TODO Auto-generated constructor stub
	}

	public synchronized boolean saveData(String key, String value)
	{
		editor.putString(key, value);
		return editor.commit();
	}

	public synchronized boolean saveData(String key, boolean value)
	{
		editor.putBoolean(key, value);
		return editor.commit();
	}

	public synchronized boolean saveData(String key, long value)
	{
		editor.putLong(key, value);
		return editor.commit();
	}

	public synchronized boolean saveData(String key, float value)
	{
		editor.putFloat(key, value);
		return editor.commit();
	}

	public synchronized boolean saveData(String key, int value)
	{
		editor.putInt(key, value);
		return editor.commit();
	}

	/*
	 * public synchronized boolean saveData(String key, Set<String> value) { //editor.putStringSet(key, value); return editor.commit(); }
	 */

	public synchronized boolean removeData(String key)
	{
		editor.remove(key);
		return editor.commit();
	}

	public synchronized Boolean getData(String key, boolean defaultValue)
	{
		return hikeSharedPreferences.getBoolean(key, defaultValue);
	}

	public synchronized String getData(String key, String defaultValue)
	{
		return hikeSharedPreferences.getString(key, defaultValue);
	}

	public synchronized float getData(String key, float defaultValue)
	{

		return hikeSharedPreferences.getFloat(key, defaultValue);
	}

	public synchronized int getData(String key, int defaultValue)
	{
		return hikeSharedPreferences.getInt(key, defaultValue);
	}

	public synchronized long getData(String key, long defaultValue)
	{
		return hikeSharedPreferences.getLong(key, defaultValue);
	}

	public synchronized void deleteAllData()
	{
		editor.clear();
		editor.commit();
	}

	public SharedPreferences getPref()
	{
		return hikeSharedPreferences;
	}
}
