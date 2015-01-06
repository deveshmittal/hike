package com.bsb.hike.analytics;

import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;

import com.bsb.hike.models.HikeAlarmManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * @author rajesh
 * This is the class exposed for analytics api instrumentation in clients
 */
public class HAManager 
{
	/** enum specifying the priority type of the analytics event */
	public enum EventPriority
	{
		NORMAL,
		HIGH
	}

	private static HAManager instance;
	
	private Context context;
	
	private ArrayList<JSONObject> eventsList;
		
	public static String ANALYTICS_SETTINGS = "analyticssettings";

	public static String FILE_SIZE_LIMIT = "fileSizeLimit";

	public static String ANALYTICS_SIZE_LIMIT = "analyticsSizeLimit";

	public static String HOUR_TO_SEND = "hourToSend";
	
	public static String ANALYTICS_SERVICE_STATUS = "analyticsServiceStatus";
		
	private boolean isAnalyticsEnabled = true;
	
	private long fileMaxSize = AnalyticsConstants.MAX_FILE_SIZE;
	
	private long analyticsMaxSize = AnalyticsConstants.MAX_ANALYTICS_SIZE;
	
	private int analyticsUploadFrequency = 0;
	
//	private NetworkListener listner;
	
	/**
	 * Constructor
	 */
	private HAManager(Context context) 
	{
		this.context = context.getApplicationContext();
		
		eventsList = new ArrayList<JSONObject>();
						
		isAnalyticsEnabled = getPrefs().getBoolean(HAManager.ANALYTICS_SERVICE_STATUS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
		
		fileMaxSize = getPrefs().getLong(HAManager.FILE_SIZE_LIMIT, AnalyticsConstants.MAX_FILE_SIZE);
		
		analyticsMaxSize = getPrefs().getLong(ANALYTICS_SIZE_LIMIT, AnalyticsConstants.MAX_ANALYTICS_SIZE);
		
		long whenToSendAnalytics = Utils.getTimeInMillis(Calendar.getInstance(), getWhenToSend(), 0, 0);  
	
		// set first alarm to upload analytics data to server
		HikeAlarmManager.setAlarm(this.context, whenToSendAnalytics, HikeAlarmManager.REQUESTCODE_HIKE_ANALYTICS, false);
		
		// set wifi listener
//		listner = new NetworkListener(this.context);
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
	 * records the analytics event to the file
	 * @param type event type
	 * @param eventContext context of the event
	 */
	public void record(String type, String eventContext)
	{
		if(!isAnalyticsEnabled)
			return;
		recordEvent(type, eventContext, EventPriority.NORMAL, null, AnalyticsConstants.EVENT_TAG_VALUE);
	}

	/**
	 * records the analytics event to the file
	 * @param type event type
	 * @param eventContext context of the event
	 * @param priority priority of the event
	 * @param metadata metadata of the event
	 */
	public void record(String type, String eventContext, JSONObject metadata)
	{
		if(!isAnalyticsEnabled)
			return;
		recordEvent(type, eventContext, EventPriority.NORMAL, metadata, AnalyticsConstants.EVENT_TAG_VALUE);
	}

	/**
	 * records the analytics event to the file
	 * @param type type of the event
	 * @param eventContext context of the event
	 * @param priority event priority
	 * @param tag tag for the event
	 */
	public void record(String type, String eventContext, EventPriority priority, String tag)
	{
		if(!isAnalyticsEnabled)
			return;
		recordEvent(type, eventContext, priority, null, tag);
	}

	/**
	 * records the analytics event to the file
	 * @param type type of the event
	 * @param eventContext context of the event
	 * @param priority priority of the event
	 * @param metadata metadata of the event
	 * @param tag tag of the event
	 */
	public void record(String type, String eventContext, EventPriority priority, JSONObject metadata, String tag)
	{
		if(!isAnalyticsEnabled)
			return;
		recordEvent(type, eventContext, priority, metadata, tag);
	}

	/**
	 * Used to write the event onto the text file
	 * @param type type of the event
	 * @param eventContext context for the event
	 * @param priority priority of the event
	 * @param metadata event metadata
	 * @param tag tag for the event
	 * @throws NullPointerException
	 */
	// TODO need to look for a better way to do this operation and avoid synchronization
	private synchronized void recordEvent(String type, String eventContext, EventPriority priority, JSONObject metadata, String tag) throws NullPointerException 
	{
		if(type == null || eventContext == null)
		{
			throw new NullPointerException("Type and Context of event cannot be null.");
		}
		eventsList.add(generateAnalticsJson(type, eventContext, priority, metadata, tag));

		if (AnalyticsConstants.MAX_EVENTS_IN_MEMORY == eventsList.size()) 
		{			
			// clone a local copy and send for writing
			ArrayList<JSONObject> jsons = (ArrayList<JSONObject>) eventsList.clone();
			
			eventsList.clear();
			
			AnalyticsStore.getInstance(this.context).dumpEvents(jsons);

			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "writer thread started!");
		}
	}

	public synchronized void dumpMostRecentEvents()
	{
		if(eventsList.size() > 0)
		{
			ArrayList<JSONObject> jsons = (ArrayList<JSONObject>) eventsList.clone();
			
			eventsList.clear();
			
			AnalyticsStore.getInstance(this.context).dumpEvents(jsons);
		}
	}
	
	/**
	 * sets the analytics service settings for the client
	 * @param context application context
	 * @param maxFileSize maximum event file size in bytes
	 * @param maxAnalyticsSize maximum size of the total analytics data on client
	 * @param whenToSend time(long) of the day when event file should be sent to the server
	 * @param isServiceEnabled true if analytics service should be stopped, false otherwise
	 */
	public void configureAnalyticsService(long maxFileSize, long maxAnalyticsSize, int whenToSend, boolean isServiceEnabled)	
	{
		Editor editor = getPrefs().edit();
		editor.putLong(HAManager.FILE_SIZE_LIMIT, AnalyticsConstants.MAX_FILE_SIZE);
		editor.putLong(HAManager.ANALYTICS_SIZE_LIMIT, AnalyticsConstants.MAX_ANALYTICS_SIZE);
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
	 * Used to set the maximum analytics size on the client
	 * @param size
	 */
	public void setAnalyticsMaxSizeOnClient(long size)
	{
		analyticsMaxSize = size;
	}
	
	/**
	 * Used to get the maximum analytics size on the client
	 * @return size of analytics in Kbs
	 */
	public long getMaxAnalyticsSizeOnClient()	
	{
		return getPrefs().getLong(HAManager.ANALYTICS_SIZE_LIMIT, AnalyticsConstants.MAX_ANALYTICS_SIZE );
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

	/**
	 * generates the analytics json object to be written to the file
	 * @param type type of the event
	 * @param eventContext context of the event
	 * @param priority priority of the event
	 * @param metadata metadata of the event
	 * @param tag tag for the event
	 * @return
	 */
	private JSONObject generateAnalticsJson(String type, String eventContext, EventPriority priority, JSONObject metadata, String tagValue)
	{		
		JSONObject json = new JSONObject();
		JSONObject data = new JSONObject();
		
		try 
		{
			data.put(AnalyticsConstants.EVENT_TYPE, type);				
			data.put(AnalyticsConstants.EVENT_SUB_TYPE, eventContext);
			data.put(AnalyticsConstants.EVENT_PRIORITY, priority);
			data.put(AnalyticsConstants.CURRENT_TIME_STAMP, System.currentTimeMillis());
			data.put(AnalyticsConstants.EVENT_TAG, tagValue);

			if(metadata != null)
			{
				data.put(AnalyticsConstants.METADATA, metadata);
			}
			json.put(AnalyticsConstants.TYPE, AnalyticsConstants.ANALYTICS_EVENT);
			json.put(AnalyticsConstants.DATA, data);
		}
		catch (JSONException e) 
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}		
		return json;
	}		
	
	/**
	 * Used to send the analytics data to the server
	 */
	public void sendAnalyticsData()
	{
		// if total logged data is less than threshold value or wifi is available, try sending all the data else delete normal priority data
		if(!((AnalyticsStore.getInstance(context).getTotalAnalyticsSize() <= HAManager.getInstance(context).getMaxAnalyticsSizeOnClient()) || 
				(Utils.getNetworkType(context) == ConnectivityManager.TYPE_WIFI)))
		{
			AnalyticsStore.getInstance(context).deleteNormalPriorityData();
		}
		new Thread(AnalyticsSender.getInstance(context)).start();
	}	
}
