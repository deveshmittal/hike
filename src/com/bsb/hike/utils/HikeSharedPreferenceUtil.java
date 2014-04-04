package com.bsb.hike.utils;

import com.bsb.hike.HikeMessengerApp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class HikeSharedPreferenceUtil
{
	private static final String PREF_NAME = HikeMessengerApp.ACCOUNT_SETTINGS;

	private SharedPreferences hikeSharedPreferences;

	private Editor editor;

	private static HikeSharedPreferenceUtil hikeSharedPreferenceUtil;

	private static void initializeHikeSharedPref(Context context)
	{
		if (context != null)
		{
			hikeSharedPreferenceUtil = new HikeSharedPreferenceUtil();
			hikeSharedPreferenceUtil.hikeSharedPreferences = context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
			hikeSharedPreferenceUtil.editor = hikeSharedPreferenceUtil.hikeSharedPreferences.edit();
		}
	}

	public static HikeSharedPreferenceUtil getInstance(Context context)
	{
		if (hikeSharedPreferenceUtil == null)
		{
			initializeHikeSharedPref(context);
		}
		return hikeSharedPreferenceUtil;
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

		hikeSharedPreferenceUtil = null;
		editor.clear();
		editor.commit();
	}
}
