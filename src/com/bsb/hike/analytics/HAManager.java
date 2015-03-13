package com.bsb.hike.analytics;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.analytics.AnalyticsConstants.AppOpenSource;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.platform.HikePlatformConstants;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
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
	
	private int analyticsSendFreq = AnalyticsConstants.DEFAULT_SEND_FREQUENCY;
	
	private int maxInMemorySize = AnalyticsConstants.MAX_EVENTS_IN_MEMORY;

	private long hourToSend;
	
	private boolean shouldSendLogs = false;
	
	private int analyticsUploadFrequency = 0;
		
	private String analyticsDirectory;

	private NetworkListener listner;
	
	private Session fgSessionInstance;
		
	/**
	 * Constructor
	 */
	private HAManager() 
	{				
		this.context = HikeMessengerApp.getInstance().getApplicationContext();
		
		analyticsDirectory = context.getFilesDir().toString() + AnalyticsConstants.EVENT_FILE_DIR;
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Storage dir :" + analyticsDirectory);

		eventsList = new ArrayList<JSONObject>();
						
		isAnalyticsEnabled = getPrefs().getBoolean(AnalyticsConstants.ANALYTICS, AnalyticsConstants.IS_ANALYTICS_ENABLED);
		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Analytics service status :"+ isAnalyticsEnabled);

		long fileMaxSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_FILESIZE, AnalyticsConstants.MAX_FILE_SIZE);
		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "File max size :" + fileMaxSize + " KBs");
		
		long analyticsMaxSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_TOTAL_SIZE, AnalyticsConstants.MAX_ANALYTICS_SIZE);

		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Total analytics size :" + analyticsMaxSize + " KBs");

		maxInMemorySize = getPrefs().getInt(AnalyticsConstants.ANALYTICS_IN_MEMORY_SIZE, AnalyticsConstants.MAX_EVENTS_IN_MEMORY);

		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Max events in memory before they get dumped to file :" + maxInMemorySize);

		shouldSendLogs = getPrefs().getBoolean(AnalyticsConstants.SEND_WHEN_CONNECTED, false);
		
		hourToSend = getPrefs().getLong(AnalyticsConstants.ANALYTICS_ALARM_TIME, -1);

		Calendar cal = Calendar.getInstance();

		if(hourToSend == -1)
		{
			int rndHour = getRandomTime();
			hourToSend = Utils.getTimeInMillis(cal, 0, 0, rndHour, 0);
			
			Editor editor = getPrefs().edit();		
			editor.putLong(AnalyticsConstants.ANALYTICS_ALARM_TIME, hourToSend);		
			editor.commit();
		}		
		
		cal.setTimeInMillis(hourToSend);
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm date(Constructor) :" + cal.get(Calendar.DAY_OF_MONTH));
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Next alarm time(Constructor) :" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));

		analyticsSendFreq = getPrefs().getInt(AnalyticsConstants.ANALYTICS_SEND_FREQUENCY, AnalyticsConstants.DEFAULT_SEND_FREQUENCY);

		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Send frequency :" + analyticsSendFreq + " mins");

		fgSessionInstance = new Session();
		
		// set network listener
		listner = new NetworkListener(this.context);
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
	 * Used to write analytics event to the file
	 * @param type type type of the event
	 * @param eventContext eventContext context of the event
	 * @param priority normal or high
	 * @param metadata metadata of the event as JSONObject
	 */
	public void record(String type, String eventContext, EventPriority priority, JSONObject metadata)
	{
		if(!isAnalyticsEnabled)
			return;
		recordEvent(type, eventContext, priority, metadata, AnalyticsConstants.EVENT_TAG_MOB);		
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

		if (maxInMemorySize == eventsList.size()) 
		{			
			// clone a local copy and send for writing
			ArrayList<JSONObject> jsons = (ArrayList<JSONObject>) eventsList.clone();
			
			eventsList.clear();
			
			AnalyticsStore.getInstance(this.context).dumpEvents(jsons, false, false);

			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "writer thread started!");
		}
	}

	private synchronized void dumpInMemoryEventsAndTryToUpload(boolean sendNow, boolean isOnDemandFromServer)
	{
		ArrayList<JSONObject> jsons = (ArrayList<JSONObject>) eventsList.clone();
		
		eventsList.clear();
		
		Logger.d(AnalyticsConstants.ANALYTICS_TAG, "Dumping in-memory events :" + jsons.size());
		AnalyticsStore.getInstance(this.context).dumpEvents(jsons, sendNow, isOnDemandFromServer);
	}
	
	/**
	 * Returns current max log file size 
	 * @return log file size in bytes
	 */
	public long getMaxFileSize()	
	{
		long maxFileSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_FILESIZE, AnalyticsConstants.MAX_FILE_SIZE);
		maxFileSize *= 1024;		
		return maxFileSize;
	}
	
	/**
	 * Returns the hour of the day when log file should be sent to the server
	 * @return hour of the day(0-23)
	 */
	public long getWhenToSend()
	{		
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
	 * Used to check if analytics data can be sent based on network availability 
	 * @return true if data can be sent, false otherwise
	 */
	public boolean isSendAnalyticsDataWhenConnected()
	{
		return shouldSendLogs;
	}
	
	/**
	 * Used to set whether logs should be sent next time alarm is triggered and user is connected 
	 * @param value to set
	 */
	public void setIsSendAnalyticsDataWhenConnected(boolean value)
	{
		Editor edit = getPrefs().edit(); 
		edit.putBoolean(AnalyticsConstants.SEND_WHEN_CONNECTED, value);
		edit.commit();
		this.shouldSendLogs = value;
	}
	
	/**
	 * Used to enable/disable the analytics service
	 * @param isAnalyticsEnabled true if analytics service is enabled, false otherwise
	 */
	public void setAnalyticsEnabled(boolean isAnalyticsEnabled)
	{
		Editor edit = getPrefs().edit(); 
		edit.putBoolean(AnalyticsConstants.ANALYTICS, isAnalyticsEnabled);
		edit.commit();
		this.isAnalyticsEnabled = isAnalyticsEnabled;
	}
	
	/**
	 * Used to set the next alarm time for sending analytics data
	 * @param alarmTime in milliseconds
	 */
	public void setNextSendTimeToPrefs(long alarmTime)
	{
		Editor editor = getPrefs().edit();		
		editor.putLong(AnalyticsConstants.ANALYTICS_ALARM_TIME, alarmTime);		
		editor.commit();
		hourToSend = alarmTime;
	}
	
	/**
	 * Used to set the maximum event file size
	 * @param size size in Kb
	 */
	public void setFileMaxSize(long size)
	{
		Editor edit = getPrefs().edit(); 
		edit.putLong(AnalyticsConstants.ANALYTICS_FILESIZE, size);
		edit.commit();
	}
	
	/**
	 * Used to set the maximum analytics size on the client
	 * @param size in kilobytes
	 */
	public void setAnalyticsMaxSizeOnClient(long size)
	{
		Editor edit = getPrefs().edit(); 
		edit.putLong(AnalyticsConstants.ANALYTICS_TOTAL_SIZE, size);
		edit.commit();
	}

	/**
	 * Used to set the analytics send frequency
	 * @param frequency on which upload should be retried(0-23)
	 */
	public void setAnalyticsSendFrequency(int freq)
	{
		Editor edit = getPrefs().edit(); 
		edit.putInt(AnalyticsConstants.ANALYTICS_SEND_FREQUENCY, freq);
		edit.commit();
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
	 * @return size of analytics in bytes
	 */
	public long getMaxAnalyticsSizeOnClient()	
	{
		long maxSize = getPrefs().getLong(AnalyticsConstants.ANALYTICS_TOTAL_SIZE, AnalyticsConstants.MAX_ANALYTICS_SIZE);
		maxSize *= 1024;
		return maxSize;
	}
	
	/**
	 * Used to get the maximum value of in memory events before they are written to file
	 * @return count of in memory events
	 */
	public int getMaxInMemoryEventsSize()
	{
		return maxInMemorySize;
	}
	
	/**
	 * Used to set the maximum number of in memory events before we write them to file
	 * @param size number of in memory events
	 */
	public void setMaxInMemoryEventsSize(int size)
	{
		Editor edit = getPrefs().edit(); 
		edit.putInt(AnalyticsConstants.ANALYTICS_IN_MEMORY_SIZE, size);
		edit.commit();
		maxInMemorySize = size;		
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

			if(metadata == null)
			{
				metadata = new JSONObject();
			}
			
			metadata.put(AnalyticsConstants.SESSION_ID, fgSessionInstance.getSessionId());

			data.put(AnalyticsConstants.METADATA, metadata);
			
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
	public void sendAnalyticsData(boolean sendNow, boolean isOnDemandFromServer)
	{
		dumpInMemoryEventsAndTryToUpload(sendNow, isOnDemandFromServer);		
	}	
	
	/**
	 * Used to generate a random time in the range 0-23 at which analytics data will be sent to the server
	 * @return random time in range 0-23
	 */
	private int getRandomTime()
	{
		Random rand = new Random();		
		int time = rand.nextInt(AnalyticsConstants.DAY_IN_SECONDS);				
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
		File dir = new File(HAManager.getInstance().getAnalyticsDirectory() + File.separator);

		String[] fileNames = dir.list();
		
		return fileNames;
	}
	
	public JSONObject recordAndReturnSessionStart()
	{
		fgSessionInstance.startSession();
		
		JSONObject metadata = getMetaDataForSession(fgSessionInstance, true);
		
		/*
			We are not recording SessionEvent in Analytics File, so commenting it
			If In future if We uncomment it
			Check that While Recording Event we add SeesionID, so now it will be added twice
		*/
		//HAManager.getInstance().record(AnalyticsConstants.SESSION_EVENT, AnalyticsConstants.FOREGROUND, EventPriority.HIGH, metadata, AnalyticsConstants.EVENT_TAG_SESSION);
		
		return metadata;
	}

	public JSONObject recordAndReturnSessionEnd()
	{
		fgSessionInstance.endChatSessions();
		recordChatSessions();
		
		JSONObject metadata = getMetaDataForSession(fgSessionInstance, false);
		
		/*
			We are not recording SessionEvent in Analytics File, so commenting it
			If In future if We want to log that as well just uncomment it but
			Check that While Recording Event we also add SeesionID, so now it will be added twice
			so remove it before recording
		 */
		//HAManager.getInstance().record(AnalyticsConstants.SESSION_EVENT, AnalyticsConstants.BACKGROUND, EventPriority.HIGH, metadata, AnalyticsConstants.EVENT_TAG_SESSION);

		dumpInMemoryEventsAndTryToUpload(false, false);
		
		fgSessionInstance.reset();
		
		return metadata; 
	}

	private JSONObject getMetaDataForSession( Session session, boolean sessionStart)
	{
		JSONObject metadata = null;
		try
		{
			metadata = new JSONObject();
			
			//1)Adding Session Id
			metadata.put(AnalyticsConstants.SESSION_ID, fgSessionInstance.getSessionId());
			
			//2)con:- 2g/3g/4g/wifi/off
			metadata.put(AnalyticsConstants.CONNECTION_TYPE, Utils.getNetworkType(context));
			
			if (sessionStart)
			{
				if (fgSessionInstance.getAppOpenSource() == AppOpenSource.FROM_NOTIFICATION)
				{
					// 5)con-type :- normal/stleath 0/1
					//metadata.put(AnalyticsConstants.CONVERSATION_TYPE, session.getConvType());

					// 6)msg_type :- MessageType (Text/Audio/Vedio/Sticker/Image/Contact/Location)
					//metadata.put(AnalyticsConstants.MESSAGE_TYPE, session.getMsgType());
				}

				// Not sending it for now. We will fix this code in later release when required
				//metadata.put(AnalyticsConstants.SOURCE_APP_OPEN, session.getAppOpenSource());

				// 3)srcctx :- uid/gid/null(in case of appOpen via Launcher)
				metadata.put(AnalyticsConstants.SOURCE_CONTEXT, session.getSrcContext());
				
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "--session-id :" + session.getSessionId() + "--network-type :" + Utils.getNetworkTypeAsString(context) + "--source-context :" + session.getSrcContext() + "--conv-type :" + session.getConvType() + "--msg-type :" + session.getMsgType());
			}
			else
			{
				metadata.put(AnalyticsConstants.SESSION_TIME, fgSessionInstance.getSessionTime());

				metadata.put(AnalyticsConstants.DATA_CONSUMED, fgSessionInstance.getDataConsumedInSession());
				
				Logger.d(AnalyticsConstants.ANALYTICS_TAG, "--session-id :" + session.getSessionId() + "--session-time :" + session.getSessionTime() + "--network-type :" + Utils.getNetworkTypeAsString(context) + "--data-consumed :" + session.getDataConsumedInSession() + "bytes");
			}
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
		return metadata;
	}
	
	public void setMetadatFieldsForSessionEvent(String appOpenSource, String srcContext, ConvMessage convMessage, int convType)
	{
		fgSessionInstance.setAppOpenSource(appOpenSource);
		fgSessionInstance.setMsgType(getMsgType(convMessage));
		fgSessionInstance.setSrcContext(srcContext);
		fgSessionInstance.setConvType(convType);
	}
	
	public void setAppOpenSource(String appOpenSource)
	{
		fgSessionInstance.setAppOpenSource(appOpenSource);
	}

	/**
	 * @param convMessage
	 */
	private String getMsgType(ConvMessage convMessage)
	{
		if (convMessage == null)
		{
			return "";
		}

		if (convMessage.isStickerMessage())
		{
			return AnalyticsConstants.MessageType.STICKER;
		}
		/**
		 * If NO Metadata ===> It was a "Text" Msg in 1-1 Conv
		 */
		else if (convMessage.getMetadata() != null)
		{
			if (convMessage.getMetadata().isPokeMessage())
			{
				return AnalyticsConstants.MessageType.NUDGE;
			}

			List<HikeFile> list = convMessage.getMetadata().getHikeFiles();
			/**
			 * If No HikeFile List ====> It was a "Text" Msg in gc
			 */
			if (list != null)
			{
				HikeFileType fileType = convMessage.getMetadata().getHikeFiles().get(0).getHikeFileType();
				switch (fileType)
				{
				case CONTACT:
					return AnalyticsConstants.MessageType.CONTACT;

				case LOCATION:
					return AnalyticsConstants.MessageType.LOCATION;

				case AUDIO:
					return AnalyticsConstants.MessageType.AUDIO;

				case VIDEO:
					return AnalyticsConstants.MessageType.VEDIO;

				case IMAGE:
					return AnalyticsConstants.MessageType.IMAGE;

				}
			}
			else
			{
				return AnalyticsConstants.MessageType.TEXT;
			}
		}

		return AnalyticsConstants.MessageType.TEXT;

	}
	
	/**
	 * It records Events For All Bots For this App session
	 */
	public void recordChatSessions()
	{
		JSONObject metadata = null;
		
		try
		{
			ArrayList<ChatSession> chatSessionList = fgSessionInstance.getChatSesions();
			
			if(chatSessionList != null && !chatSessionList.isEmpty())
			{
				for(ChatSession chatSession : chatSessionList)
				{
					metadata = new JSONObject();
					//1)to_user:- "+hikecricket+" for cricket bot
					metadata.put(AnalyticsConstants.TO_USER, chatSession.getMsisdn());
					
					//2)duration:-Total time of Chat Session in whole session
					metadata.put(AnalyticsConstants.SESSION_TIME, chatSession.getChatSessionTotalTime());
					
					//3)putting event key (ek) as bot_open
					metadata.put(AnalyticsConstants.EVENT_KEY, HikePlatformConstants.BOT_OPEN);
					
					HAManager.getInstance().record(AnalyticsConstants.CHAT_ANALYTICS, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, AnalyticsConstants.EVENT_TAG_BOTS);
						
					Logger.d(AnalyticsConstants.ANALYTICS_TAG, "--session-id :" + fgSessionInstance.getSessionId() + "--to_user :" + chatSession.getMsisdn() + "--session-time :" + chatSession.getChatSessionTotalTime());
				}
			}
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.ANALYTICS_TAG, "invalid json");
		}
		
	}
	
	/**
	 * Sets StartingTime for Bot Chat Session to CurrentTime
	 */
	public void startChatSession(String msisdn)
	{
		fgSessionInstance.startChatSession(msisdn);
	}
	
	/**
	 * Sets StartingTime for Bot Chat Session to CurrentTime
	 */
	public void endChatSession(String msisdn)
	{
		fgSessionInstance.endChatSesion(msisdn);
	}

	public void recordLastSeenEvent(String screen, String api, String msg, String toUser)
	{
		if(!HikeSharedPreferenceUtil.getInstance().getData(HikeMessengerApp.DETAILED_HTTP_LOGGING_ENABLED, false))
		{	
			return;
		}
		JSONObject metadata = null;
		
		try
		{
			metadata = new JSONObject();
			
			metadata.put("screen", screen);
			
			metadata.put("api", api);
			
			if(!TextUtils.isEmpty(msg))
			{
				metadata.put("m", msg);
			}
			
			if(!TextUtils.isEmpty(toUser))
			{
				metadata.put("to_user", toUser);
			}
			
			HAManager.getInstance().record(AnalyticsConstants.LAST_SEEN_ANALYTICS, AnalyticsConstants.NON_UI_EVENT, EventPriority.HIGH, metadata, AnalyticsConstants.LAST_SEEN_ANALYTICS);
				
			Logger.d(AnalyticsConstants.LAST_SEEN_ANALYTICS_TAG, " --screen :"+ screen + " --api :"+ api + " -- msg :" + msg + " --to_user "+ toUser);
		}
		catch(JSONException e)
		{
			Logger.d(AnalyticsConstants.LAST_SEEN_ANALYTICS_TAG, "invalid json");
		}
	}
	
}
