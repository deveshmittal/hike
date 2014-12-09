package com.bsb.hike.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * @author rajesh
 *
 */
class AnalyticsSettings 
{
	private Context context = null;
	
	public AnalyticsSettings(Context context)
	{
		this.context = context;
	}
	
	public static void configureAnalyticsService(Context context, int maxFileSize, long whenToSend, boolean isServiceEnabled)	
	{
		SharedPreferences prefs = context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putLong(HAManager.FILE_SIZE_LIMIT, AnalyticsConstants.MAX_FILE_SIZE);
		editor.putBoolean(HAManager.ANALYTICS_SERVICE_STATUS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
		editor.putInt(HAManager.HOUR_TO_SEND, AnalyticsConstants.HOUR_OF_DAY_TO_SEND);
		editor.commit();
	}
	
	public long getMaxFileSize()
	{
		SharedPreferences prefs = context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);		
		return prefs.getLong(HAManager.FILE_SIZE_LIMIT, AnalyticsConstants.MAX_FILE_SIZE );
	}
	
	public int getWhenToSend()
	{
		SharedPreferences prefs = context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
		return prefs.getInt(HAManager.HOUR_TO_SEND, AnalyticsConstants.HOUR_OF_DAY_TO_SEND);
	}
	
	public boolean isServiceEnabled()
	{
		SharedPreferences prefs = context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
		return prefs.getBoolean(HAManager.ANALYTICS_SERVICE_STATUS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
	}
}
