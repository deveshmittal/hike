package com.bsb.hike.analytics;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.bsb.hike.utils.Logger;

/**
 * @author rajesh
 * This is the class exposed for analytics api instrumentation in clients
 */
public class HAManager 
{
	private static HAManager instance;
	
	private Context context;
	
	private ArrayList<Event> eventsList;
	
	private AnalyticsStore store;
	
	public static String ANALYTICS_SETTINGS = "analyticssettings";

	public static String FILE_SIZE_LIMIT = "fileSizeLimit";
	
	public static String HOUR_TO_SEND = "hourToSend";
	
	public static String ANALYTICS_SERVICE_STATUS = "analyticsServiceStatus";
		
	private boolean isAnalyticsEnabled = true;
	
	/**
	 * Constructor
	 */
	private HAManager(Context context) 
	{
		this.context = context.getApplicationContext();
		
		eventsList = new ArrayList<Event>();
		
		/** synchronize all access to the events list */
		Collections.synchronizedCollection(eventsList);
		
		store = AnalyticsStore.getInstance(this.context);	
		
		isAnalyticsEnabled = getPrefs().getBoolean(HAManager.ANALYTICS_SERVICE_STATUS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
	}
	
	/**
	 * Singleton instance of HAManager
	 * @return HAManager instance
	 */
	public static HAManager getInstance(Context context)
	{
		if(instance == null)
		{
			synchronized (HAManager.class)
			{
				if(instance == null)
				{
					instance = new HAManager(context.getApplicationContext());
				}
			}
		}
		return instance;
	}

	/**
	 * Records one event
	 * @param event event to be logged
	 * @param context application context
	 */
	public void recordEvent(Event event)
	{
		eventsList.add(event);
		
		if(eventsList.size() >= AnalyticsConstants.MAX_EVENTS_IN_MEMORY)
		{	
			// clone a local copy and send for writing
			ArrayList<Event> events = (ArrayList<Event>)eventsList.clone();

			AnalyticsStore.getInstance(this.context).dumpEvents(events);
			
			eventsList.clear();
			
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "writer thread started!");
		}					
	}

	/**
	 * sets the analytics service settings for the client
	 * @param context application context
	 * @param maxFileSize maximum event file size in bytes
	 * @param whenToSend time(long) of the day when event file should be sent to the server
	 * @param isServiceEnabled true if analytics service should be stopped, false otherwise
	 */
	public void configureAnalyticsService(long maxFileSize, int whenToSend, boolean isServiceEnabled)	
	{
		Editor editor = getPrefs().edit();
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
		return getPrefs().getLong(HAManager.FILE_SIZE_LIMIT, AnalyticsConstants.MAX_FILE_SIZE );
	}
	
	/**
	 * Returns the hour of the day when log file should be sent to the server
	 * @return hour of the day(0-23)
	 */
	public int getWhenToSend()
	{
		return getPrefs().getInt(HAManager.HOUR_TO_SEND, AnalyticsConstants.HOUR_OF_DAY_TO_SEND);
	}
	
	/**
	 * Returns whether analytics logging service is currently enabled or disabled 
	 * @return true if logging service is enabled, false otherwise
	 */
	public boolean isAnalyticsEnabled()
	{
		return isAnalyticsEnabled;
	}
	
	/**
	 * Used to enable/disable the analytics service
	 * @param isAnalyticsEnabled true if analytics service is enabled, false otherwise
	 */
	public void setAnalyticsEnabled(boolean isAnalyticsEnabled)
	{
		this.isAnalyticsEnabled = isAnalyticsEnabled;
	}
	
	/**
	 * Used to get the application's SharedPreferences
	 * @return SharedPreference of the application
	 */
	private SharedPreferences getPrefs()
	{
		return context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);		
	}
}
