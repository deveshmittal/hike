package com.bsb.hike.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;

public class Protip
{

	private long id;

	private String mappedId;

	private String header;

	private String text;

	private String imageURL;

	private long waitTime;

	private long timeStamp;

	private String gameDownlodURL;

	private boolean showPush;

	public Protip(JSONObject jsonObject) throws JSONException
	{

		this.timeStamp = jsonObject.optLong(HikeConstants.TIMESTAMP, System.currentTimeMillis() / 1000);
		/* prevent us from receiving a message from the future */
		long now = System.currentTimeMillis() / 1000;
		this.timeStamp = (this.timeStamp > now) ? now : this.timeStamp;

		JSONObject data = jsonObject.getJSONObject(HikeConstants.DATA);
		this.mappedId = data.getString(HikeConstants.MESSAGE_ID);
		this.header = data.optString(HikeConstants.PROTIP_HEADER, null);
		this.text = data.optString(HikeConstants.PROTIP_TEXT, null);
		this.imageURL = data.optString(HikeConstants.PROTIP_IMAGE_URL, null);
		this.waitTime = data.optLong(HikeConstants.PROTIP_WAIT_TIME, -1);
		this.gameDownlodURL = data.optString(HikeConstants.PROTIP_GAME_DOWNLOAD_URL, null);
		this.showPush = data.optBoolean(HikeConstants.PROTIP_SHOW_PUSH);
	}

	public Protip(long id, String mappedId, String header, String text, String imageURL, long waitTime, long timeStamp, String gameDownlodURL)
	{
		this.id = id;
		this.mappedId = mappedId;
		this.header = header;
		this.text = text;
		this.imageURL = imageURL;
		this.waitTime = waitTime;
		this.timeStamp = timeStamp;
		this.gameDownlodURL = gameDownlodURL;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getMappedId()
	{
		return mappedId;
	}

	public String getHeader()
	{
		return header;
	}

	public String getText()
	{
		return text;
	}

	public String getImageURL()
	{
		return imageURL;
	}

	public long getWaitTime()
	{
		return waitTime;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}

	public String getGameDownlodURL()
	{
		return gameDownlodURL;
	}

	public void setGameDownlodURL(String gameDownlodURL)
	{
		this.gameDownlodURL = gameDownlodURL;
	}

	public boolean isShowPush()
	{
		return showPush;
	}

	public void setShowPush(boolean showPush)
	{
		this.showPush = showPush;
	}

}
