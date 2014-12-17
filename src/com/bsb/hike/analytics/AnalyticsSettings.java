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
		this.context = context.getApplicationContext();
	}
	
	/**
	 * Constructor
	 * @param context application context
	 * @param maxFileSize Maximum size of each log file will be controlled by the server
	 * @param whenToSend Server can also configure what hour of the day analytics data should be sent to the server
	 * @param isServiceEnabled This enables or disables the analytics logging service on the client as required by the server 
	 * true means logging service is currently active, false means logging service is disabled
	 */
	public static void configureAnalyticsService(Context context, long maxFileSize, int whenToSend, boolean isServiceEnabled)	
	{
		SharedPreferences prefs = context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putLong(HAManager.FILE_SIZE_LIMIT, AnalyticsConstants.MAX_FILE_SIZE);
		editor.putBoolean(HAManager.ANALYTICS_SERVICE_STATUS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
		editor.putInt(HAManager.HOUR_TO_SEND, AnalyticsConstants.HOUR_OF_DAY_TO_SEND);
		editor.commit();
	}
	
	/**
	 * Returns current max log file size 
	 * @return log file size in bytes
	 */
	public long getMaxFileSize()	
	{
		SharedPreferences prefs = context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);		
		return prefs.getLong(HAManager.FILE_SIZE_LIMIT, AnalyticsConstants.MAX_FILE_SIZE );
	}
	
	/**
	 * Returns the hour of the day when log file should be sent to the server
	 * @return hour of the day(0-23)
	 */
	public int getWhenToSend()
	{
		SharedPreferences prefs = context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
		return prefs.getInt(HAManager.HOUR_TO_SEND, AnalyticsConstants.HOUR_OF_DAY_TO_SEND);
	}
	
	/**
	 * Returns whether analytics logging service is currently enabled or disabled 
	 * @return true if logging service is enabled, false otherwise
	 */
	public boolean isAnalyticsEnabled()
	{
		SharedPreferences prefs = context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);
		return prefs.getBoolean(HAManager.ANALYTICS_SERVICE_STATUS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
	}
}
