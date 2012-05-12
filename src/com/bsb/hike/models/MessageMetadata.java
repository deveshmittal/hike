package com.bsb.hike.models;

import org.json.JSONObject;

import com.bsb.hike.HikeConstants;

public class MessageMetadata
{
	private String dndMissedCallNumber;
	private JSONObject json;

	public MessageMetadata(JSONObject metadata)
	{
		this.dndMissedCallNumber = metadata.optString(HikeConstants.METADATA_DND);
		this.json = metadata;
	}

	public String getDNDMissedCallNumber()
	{
		return dndMissedCallNumber;
	}

	public String serialize()
	{
		return this.json.toString();
	}
}
