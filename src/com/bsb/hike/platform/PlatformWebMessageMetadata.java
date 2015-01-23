package com.bsb.hike.platform;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shobhitmandloi on 15/01/15.
 */
public class PlatformWebMessageMetadata
{
	private String notifText = "";

	private JSONObject helperData;

	private int cardHeight;

	private JSONObject cardData = new JSONObject();

	public int getCardHeight()
	{
		return cardHeight;
	}

	public void setCardHeight(int cardHeight)
	{
		this.cardHeight = cardHeight;
	}

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
		if (metadata.has(HikePlatformConstants.CARD_OBJECT))
		{
			JSONObject cardobj = metadata.optJSONObject(HikePlatformConstants.CARD_OBJECT);
			if (cardobj.has(HikePlatformConstants.HELPER_DATA))
			{
				setHelperData(cardobj.optJSONObject(HikePlatformConstants.HELPER_DATA));
			}
		}

		if (metadata.has(HikePlatformConstants.HEIGHT)){
			setCardHeight(Integer.parseInt(metadata.optString(HikePlatformConstants.HEIGHT)));
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
