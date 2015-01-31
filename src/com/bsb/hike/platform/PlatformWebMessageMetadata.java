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

	private JSONObject cardobj ;

	public int getCardHeight()
	{
		return cardHeight;
	}

	public void setCardHeight(int cardHeight)
	{
		try
		{
			cardobj.put(HikePlatformConstants.HEIGHT, String.valueOf(cardHeight));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	public String getNotifText()
	{
		return notifText;
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
			cardobj = metadata.optJSONObject(HikePlatformConstants.CARD_OBJECT);

			if (cardobj.has(HikePlatformConstants.HELPER_DATA))
			{
				setHelperData(cardobj.optJSONObject(HikePlatformConstants.HELPER_DATA));
			}

			if (cardobj.has(HikePlatformConstants.HEIGHT))
			{
				this.cardHeight = Integer.parseInt(cardobj.optString(HikePlatformConstants.HEIGHT));
			}

			// Extract notif text
			if (cardobj.has(HikePlatformConstants.NOTIF_TEXT_WC))
			{
				setNotifText(cardobj.optString(HikePlatformConstants.NOTIF_TEXT_WC));
			}
		}else
		{
			cardobj = new JSONObject();
		}
	}

	public JSONObject getJSON()
	{
		return json;
	}

	public String JSONtoString()
	{
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

	public void setNotifText(String notifText)
	{
		this.notifText = notifText;
	}
}
