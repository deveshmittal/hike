package com.bsb.hike.platform;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shobhitmandloi on 15/01/15.
 */
public class PlatformWebMessageMetadata
{
	private String notifText = "";

	private JSONObject helperData = new JSONObject();

	private JSONObject cardData = new JSONObject();

	public String getNotifText()
	{
		return notifText;
	}

	public JSONObject getCardData()
	{
		return cardData;
	}

	public void setCardData(JSONObject cardData)
	{
		this.cardData = cardData;
	}

	public JSONObject getHelperData()
	{
		return helperData;
	}

	public void setHelperData(JSONObject helperData)
	{
		this.helperData = helperData;
	}

	private JSONObject json;

	public PlatformWebMessageMetadata(String jsonString) throws JSONException
	{
		this(new JSONObject(jsonString));
	}

	public PlatformWebMessageMetadata(JSONObject metadata)
	{
		this.json = metadata;
		if (metadata.has(HikePlatformConstants.HELPER_DATA)){
			setHelperData(metadata.optJSONObject(HikePlatformConstants.HELPER_DATA));
		}


	}

	public JSONObject getJSON()
	{
		return json;
	}

	public String JSONtoString() {
		return json.toString();
	}
	
	public String getAlarmData()
	{
		return getString(HikePlatformConstants.ALARM_DATA);
	}
	
	private String getString(String key)
	{
		return json.optString(key);
	}
}
