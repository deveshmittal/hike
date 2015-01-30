package com.bsb.hike.analytics;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Environment;

import com.bsb.hike.HikeMessengerApp;
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
		
	public static final String ANALYTICS_SETTINGS = "analyticssettings";

	private boolean isAnalyticsEnabled = true;
	
	private long fileMaxSize = AnalyticsConstants.MAX_FILE_SIZE;
	
	private long analyticsMaxSize = AnalyticsConstants.MAX_ANALYTICS_SIZE;

	private int analyticsSendFreq = AnalyticsConstants.DEFAULT_SEND_FREQUENCY;

	private int hourToSend;
	
	private int analyticsUploadFrequency = 0;
		
	private String analyticsDirectory;

//	private NetworkListener listner;
	
	/**
	 * Constructor
	 */
	private HAManager() 
	{		
		this.context = HikeMessengerApp.getInstance().getApplicationContext(); 
		
//		analyticsDirectory = context.getFilesDir().toString() + AnalyticsConstants.EVENT_FILE_DIR;

		// analytics data moved to external storage for QA to manually check the files
		analyticsDirectory = Environment.getExternalStorageDirectory() + AnalyticsConstants.EVENT_FILE_DIR;

		eventsList = new ArrayList<JSONObject>();
						
		isAnalyticsEnabled = getPrefs().getBoolean(AnalyticsConstants.ANALYTICS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
		
		fileMaxSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_FILESIZE, AnalyticsConstants.MAX_FILE_SIZE);
		
		analyticsMaxSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_TOTAL_SIZE, AnalyticsConstants.MAX_ANALYTICS_SIZE);
		
		hourToSend = getRandomTime();

		analyticsSendFreq = getPrefs().getInt(AnalyticsConstants.ANALYTICS_SEND_FREQUENCY, AnalyticsConstants.DEFAULT_SEND_FREQUENCY);

		// set wifi listener
//		listner = new NetworkListener(this.context);
	}
	
	/**
	 * Singleton instance of HAManager
	 * @return HAManager instance
	 */
	public static HAManager getInstance()
	{
		if(instance == null)
		{
			synchronized (HAManager.class)
			{
				if(instance == null)
				{
					instance = new HAManager();
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
		recordEvent(type, eventContext, EventPriority.NORMAL, null, AnalyticsConstants.EVENT_TAG_MOB);
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
		recordEvent(type, eventContext, EventPriority.NORMAL, metadata, AnalyticsConstants.EVENT_TAG_MOB);
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
	 * Used to write analytics event to the file
	 * @param type type of the event
	 * @param eventContext context of the event
	 * @param metadata metadata of the event as JSONObject
	 * @param tag tag value for the event
	 */
	public void record(String type, String eventContext, JSONObject metadata, String tag)
	{
		if(!isAnalyticsEnabled)
			return;
		recordEvent(type, eventContext, EventPriority.NORMAL, metadata, tag);
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
			
			AnalyticsStore.getInstance(this.context).dumpEvents(jsons, false, false);

			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "writer thread started!");
		}
	}

	private synchronized void dumpMostRecentEventsAndSendToServer(boolean isOnDemandFromServer)
	{
		ArrayList<JSONObject> jsons = (ArrayList<JSONObject>) eventsList.clone();
		
		eventsList.clear();
		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Dumping in-memory events :" + jsons.size());
		AnalyticsStore.getInstance(this.context).dumpEvents(jsons, true, isOnDemandFromServer);
	}
	
	/**
	 * Returns current max log file size 
	 * @return log file size in bytes
	 */
	public long getMaxFileSize()	
	{
		return fileMaxSize;
	}
	
	/**
	 * Returns the hour of the day when log file should be sent to the server
	 * @return hour of the day(0-23)
	 */
	public int getWhenToSend()
	{
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Alarm-time :" + hourToSend);
		
		return hourToSend;
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
	 * Used to set the analytics send frequency
	 * @param 
	 */
	public void setAnalyticsSendFrequency(int freq)
	{
		analyticsSendFreq = freq;
	}

	/**
	 * Used to get the current frequency to send analytics data
	 */
	public int getAnalyticsSendFrequency()
	{
		return analyticsSendFreq;
	}
	/**
	 * Used to get the maximum analytics size on the client
	 * @return size of analytics in Kbs
	 */
	public long getMaxAnalyticsSizeOnClient()	
	{
		return analyticsMaxSize;
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
	protected int getAnalyticsUploadRetryCount()
	{
		return analyticsUploadFrequency;
	}
	
	/**
	 * Resets the upload frequency to 0
	 */
	protected void resetAnalyticsUploadRetryCount()
	{
		analyticsUploadFrequency = 0;
	}
	
	/**
	 * Increments the analytics upload frequency
	 */
	protected void incrementAnalyticsUploadRetryCount()
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
	public boolean sendAnalyticsData(boolean isOnDemandFromServer)
	{
		boolean isUserConnected = false;
		
		dumpMostRecentEventsAndSendToServer(isOnDemandFromServer);
		
		if(Utils.isUserOnline(context))
		{
			isUserConnected = true;
		}		
		return isUserConnected;
	}	
	
	/**
	 * Used to generate a random time in the range 0-23 at which analytics data will be sent to the server
	 * @return random time in range 0-23
	 */
	private int getRandomTime()
	{
		Random rand = new Random();
		
		int time = rand.nextInt(24);
		
		return time;
	}
	
	/**
	 * Used to return the directory in which analytics data is saved
	 * @return analytics directory
	 */
	public String getAnalyticsDirectory()
	{
		return analyticsDirectory;
	}
	
	/**
	 * Used to get an array of file names present in the Analytics directory of the application package
	 * @return array of strings with file names 
	 */
	protected static String[] getFileNames(Context context)
	{
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Looking files in directory :" + context.getFilesDir() + "/Analytics/");
		
		File dir = new File(HAManager.getInstance().getAnalyticsDirectory() + File.separator);

		String[] fileNames = dir.list();
		
		return fileNames;
	}
}
