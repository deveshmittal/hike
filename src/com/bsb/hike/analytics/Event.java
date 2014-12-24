package com.bsb.hike.analytics;

import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import com.bsb.hike.utils.Logger;

/**
 * @author rajesh
 *
 */
public class Event
{
	/** enum specifying the priority type of the analytics event */
	public enum EventPriority
	{
		NORMAL,
		REALTIME
	}

	private String type;
		
	private String context;
	
	private EventPriority priority = EventPriority.NORMAL;
	
	/** HashMap of metadata key-value pairs */
	private Map<String, String> metadata;

	/**
	 * Constructor
	 * @param metadata metadata of the event
	 * @throws NullPointerException if metadata is null
	 */
	public Event(Map<String, String> metadata) throws NullPointerException
	{		
		if(metadata == null)
		{
			throw new NullPointerException("Metadata cannot be null.");
		}
		this.metadata = metadata;
	}

	/**
	 * Sets the type of event. Type may be ui/non-ui
	 * @param type type of the event
	 */
	public void setEventAttributes(String type)
	{
		setEventAttributes(type, null, EventPriority.NORMAL);
	}

	/**
	 * Sets the type and subtype/context of the event	
	 * @param type type of the event
	 * @param context subtype or context of the event
	 */
	public void setEventAttributes(String type, String context)
	{
		setEventAttributes(type, context, EventPriority.NORMAL);
	}

	/**
	 * Sets the type, context and priority of the event
	 * @param type type of the event
	 * @param context context of the event
	 * @param priority priority of the event
	 * @throws NullPointerException if context of the event is null
	 */
	public void setEventAttributes(String type, String context, Event.EventPriority priority) throws NullPointerException
	{		
		this.type = type;		
		this.context = context;
		this.priority = priority;
		
		if(this.type == null)
		{
			throw new NullPointerException("Event type cannot be null");
		}
	}

	/**
	 * Used to create the analytics event in json format
	 * @param event Event for which json is generated
	 * @return JSONObject 
	 */
	public JSONObject toJson(Event event)
	{		
		JSONObject json = new JSONObject();
		JSONObject data = new JSONObject();
		
		try 
		{
			data.put(AnalyticsConstants.EVENT_TYPE, event.type);
			data.put(AnalyticsConstants.EVENT_SUB_TYPE, event.context);
			data.put(AnalyticsConstants.EVENT_PRIORITY, event.priority);
			data.put(AnalyticsConstants.EVENT_TAG, AnalyticsConstants.EVENT_TAG_VALUE);
			data.put(AnalyticsConstants.CURRENT_TIME_STAMP, System.currentTimeMillis());			
			data.put(AnalyticsConstants.METADATA, getMetadata(event.metadata));
			
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
	 * Used to convert Map containing key-value pairs to json
	 * @param mdMap Map of key-value pairs
	 * @return JSONObject 
	 */
	private JSONObject getMetadata(Map<String, String> mdMap)
	{
		JSONObject metadata = null;
		  	
		try
		{
			metadata = new JSONObject(mdMap);
		}
		catch(NullPointerException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Metadata has null keys");
		}		
		return metadata;
	}
}
