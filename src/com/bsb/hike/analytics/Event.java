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
	 * Used to get the event type
	 * @return event return type
	 */
	public String getType()
	{
		return this.type;
	}

	/** 
	 * Used to return the event context
	 * @return context of the event
	 */
	public String getContext()
	{
		return this.context;
	}
	
	/**
	 * Used to return the priority of the event
	 * @return priority of the event
	 */
	public EventPriority getPriority()
	{
		return this.priority;
	}
	
	/**
	 * Used to convert Map containing key-value pairs to json
	 * @param mdMap Map of key-value pairs
	 * @return JSONObject 
	 */
	public JSONObject getMetadata()
	{
		JSONObject metadata = null;
		  	
		try
		{
			metadata = new JSONObject(this.metadata);
		}
		catch(NullPointerException e)
		{
			Logger.e(AnalyticsConstants.ANALYTICS_TAG, "Metadata has null keys");
		}		
		return metadata;
	}
}
