package com.bsb.hike.analytics;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.bsb.hike.utils.Logger;

/**
 * @author rajesh
 *
 */
public class Event
{
	public enum EventPriority
	{
		NORMAL,
		REALTIME
	}

	private String type;
		
	private String eventContext;
	
	private String key;
	
	private String msisdn;
	
	private EventPriority priority;
					
	public Event()
	{
		
	}

	public void setType(String type)
	{
		this.type = type;
	}
	
	public void setContext(String eventContext)
	{
		this.eventContext = eventContext;
	}
	
	public void setPriority(EventPriority priority)
	{
		this.priority = priority;
	}
	
	public void setEventKey(String key)
	{
		this.key = key;		
	}
	
	public void setMsisdn(String msisdn)
	{
		this.msisdn = msisdn;
	}
	
	public static JSONObject toJson(Event event)
	{		
		JSONObject json = new JSONObject();
		JSONObject data = new JSONObject();
		
		try 
		{
			data.put(AnalyticsConstants.EVENT_TYPE, event.type);
			
			JSONObject metadata = new JSONObject();
			metadata.put(AnalyticsConstants.SUB_TYPE, event.eventContext);
			metadata.put(AnalyticsConstants.EVENT_PRIORITY, event.priority);
			metadata.put(AnalyticsConstants.EVENT_KEY, event.key);
						
			if(TextUtils.isEmpty(event.msisdn))
			{
				metadata.put(AnalyticsConstants.TO, event.msisdn);				
			}
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
}
