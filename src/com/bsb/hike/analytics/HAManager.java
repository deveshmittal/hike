package com.bsb.hike.analytics;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author rajesh
 * This is the class exposed for analytics api instrumentation in clients
 */
public class HAManager 
{
	private static HAManager instance;
	
	private Context context;
	
	private ArrayList<Event> eventsList;
		
	public static String ANALYTICS_SETTINGS = "analyticssettings";

	public static String FILE_SIZE_LIMIT = "fileSizeLimit";
	
	public static String HOUR_TO_SEND = "hourToSend";
	
	public static String ANALYTICS_SERVICE_STATUS = "analyticsServiceStatus";
		
	private boolean isAnalyticsEnabled = true;
	
	private long fileMaxSize = AnalyticsConstants.MAX_FILE_SIZE;
	
	private int analyticsUploadFrequency = 0;
	
	private NetworkListener listner;
	
	/**
	 * Constructor
	 */
	private HAManager(Context context) 
	{
		this.context = context.getApplicationContext();
		
		eventsList = new ArrayList<Event>();
						
		isAnalyticsEnabled = getPrefs().getBoolean(HAManager.ANALYTICS_SERVICE_STATUS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
		
		fileMaxSize = getPrefs().getLong(HAManager.FILE_SIZE_LIMIT, AnalyticsConstants.MAX_FILE_SIZE);
		
		long whenToSendAnalytics = Utils.getTimeInMillis(Calendar.getInstance(), getWhenToSend(), 0, 0);  
	
		// set first alarm to upload analytics data to server
		HikeAlarmManager.setAlarm(this.context, whenToSendAnalytics, HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS, false);
		
		// set wifi listener
		listner = new NetworkListener(this.context);
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
	 * passes analytics events for recording by first checking if analytics service is enabled or disabled 
	 * @param e event to be recorded
	 */
	public void record(Event e)
	{
		if(!isAnalyticsEnabled)
			return;
		recordEvent(e);
	}
	
	/**
	 * Records one event
	 * @param event event to be logged
	 * @param context application context
	 */
	// TODO need to look for a better way to do this operation and avoid synchronization
	private synchronized void recordEvent(Event event) 
	{
		eventsList.add(event);

		if (AnalyticsConstants.MAX_EVENTS_IN_MEMORY == eventsList.size()) 
		{			
			// clone a local copy and send for writing
			ArrayList<Event> events = (ArrayList<Event>) eventsList.clone();
			
			eventsList.clear();
			
			AnalyticsStore.getInstance(this.context).dumpEvents(events);

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
	 * Used to set the maximum event file size
	 * @param size size in Kb
	 */
	public void setFileMaxSize(long size)
	{
		fileMaxSize = size;
	}
	
	/**
	 * Used to get the application's SharedPreferences
	 * @return SharedPreference of the application
	 */
	private SharedPreferences getPrefs()
	{
		return context.getSharedPreferences(HAManager.ANALYTICS_SETTINGS, Context.MODE_PRIVATE);		
	}
	
	/**
	 * Returns how many times in the day analytics data has been tried to upload
	 * @return frequency in int
	 */
	protected int getAnalyticsUploadFrequency()
	{
		return analyticsUploadFrequency;
	}
	
	/**
	 * Resets the upload frequency to 0
	 */
	protected void resetAnalyticsUploadFrequency()
	{
		analyticsUploadFrequency = 0;
	}
	
	/**
	 * Increments the analytics upload frequency
	 */
	protected void incrementAnalyticsUploadFrequency()
	{
		analyticsUploadFrequency++;
	}
}
