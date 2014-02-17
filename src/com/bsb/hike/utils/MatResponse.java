package com.bsb.hike.utils;

import org.json.JSONObject;
import android.util.Log;
import com.mobileapptracker.MATResponse;

public class MatResponse implements MATResponse
{

	@Override
	public void didSucceedWithData(JSONObject data)
	{
		Log.d("hike MAT callback", data.toString());
	}

}
