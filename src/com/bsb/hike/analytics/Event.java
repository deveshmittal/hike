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
		
	private String subType;
	
	private EventPriority priority;
	
	private JSONObject metadata;
					
	public Event(JSONObject metadata)
	{
		this.metadata = metadata;
	}

	public void setType(String type)
	{
		this.type = type;
	}
	
	public void setContext(String subType)
	{
		this.subType = subType;
	}
	
	public void setPriority(EventPriority priority)
	{
		this.priority = priority;
	}
	
	public static JSONObject toJson(Event event)
	{		
		JSONObject json = new JSONObject();
		JSONObject data = new JSONObject();
		
		try 
		{
			data.put(AnalyticsConstants.EVENT_TYPE, event.type);
			data.put(AnalyticsConstants.EVENT_SUB_TYPE, event.subType);
			data.put(AnalyticsConstants.EVENT_PRIORITY, event.priority);
			data.put(AnalyticsConstants.EVENT_TAG, AnalyticsConstants.EVENT_TAG_VALUE);
			data.put(AnalyticsConstants.CURRENT_TIME_STAMP, System.currentTimeMillis());			
			data.put(AnalyticsConstants.METADATA, event.metadata);
			
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
