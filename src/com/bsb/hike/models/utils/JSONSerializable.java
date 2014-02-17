package com.bsb.hike.models.utils;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONSerializable
{
	public JSONObject toJSON() throws JSONException;
}
