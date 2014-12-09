package com.bsb.hike.analytics;

import java.util.ArrayList;

import android.content.Context;

import com.bsb.hike.utils.Logger;

/**
 * @author rajesh
 * This is the class exposed for analytics api instrumentation in clients
 */
public class HAManager 
{
	private static HAManager instance = null;
	
	private Context context = null;
	
	private ArrayList<Event> eventsList;
	
	private AnalyticsStore store = null;
	
	public static String ANALYTICS_SETTINGS = "analyticssettings";

	public static String FILE_SIZE_LIMIT = "fileSizeLimit";
	
	public static String HOUR_TO_SEND = "hourToSend";
	
	public static String ANALYTICS_SERVICE_STATUS = "analyticsServiceStatus";
	
	public Thread writerThread;
	
	/**
	 * Constructor
	 */
	public HAManager(Context context) 
	{
		this.context = context.getApplicationContext();
		
		eventsList = new ArrayList<Event>();
		
		store = new AnalyticsStore(context);					
	}
	
	/**
	 * Singleton instance of HAManager
	 * @return HAManager instance
	 */
	public static HAManager getInstance(Context context)
	{
		if(instance == null)
		{
			instance = new HAManager(context);
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
			store.setEventsToDump(eventsList);
			
			eventsList.clear();
			
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "writer thread started!");
			
			writerThread = new Thread(store);
			
			writerThread.start();
		}					
	}

	/**
	 * sets the analytics service settings for the client
	 * @param context application context
	 * @param maxFileSize maximum event file size in bytes
	 * @param whenToSend time(long) of the day when event file should be sent to the server
	 * @param isServiceEnabled true if analytics service should be stopped, false otherwise
	 */
	public static void configureAnalyticsService(Context context, int maxFileSize, long whenToSend, boolean isServiceEnabled)	
	{
		AnalyticsSettings.configureAnalyticsService(context, maxFileSize, whenToSend, isServiceEnabled);
	}
}
