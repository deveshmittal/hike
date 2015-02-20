package com.bsb.hike.modules.signupmanager;

import static com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests.setProfileRequest;

import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.NetworkErrorException;

import com.bsb.hike.models.Birthday;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.modules.httpmgr.request.requestbody.IRequestBody;
import com.bsb.hike.modules.httpmgr.request.requestbody.JsonBody;
import com.bsb.hike.modules.httpmgr.response.Response;
import com.bsb.hike.utils.Utils;

public class SetProfileTask
{
	private String name;

	private Birthday birthdate;

	private boolean isFemale;
	
	private JSONObject resultObject;

	public SetProfileTask(String name, Birthday birthdate, boolean isFemale)
	{
		this.name = name;
		this.birthdate = birthdate;
		this.isFemale = isFemale;
	}
	
	public JSONObject execute() throws NetworkErrorException
	{
		JSONObject postObject = getPostObject();
		if(postObject == null)
		{
			return null;
		}
		
		IRequestBody body = new JsonBody(postObject);
		RequestToken requestToken = setProfileRequest(body, getRequestListener());
		requestToken.execute();
		
		if(resultObject == null)
		{
			throw new NetworkErrorException("Unable to set name");
		}
		return resultObject;
	}

	private IRequestListener getRequestListener()
	{
		return new IRequestListener()
		{
			
			@Override
			public void onRequestSuccess(Response result)
			{
				JSONObject response = (JSONObject) result.getBody().getContent();
				if(!Utils.isResponseValid(response))
				{
					resultObject = null;
					return;
				}
				else
				{
					resultObject = response;
				}
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{
				
			}
			
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				resultObject = null;
			}
		};
	}
	private JSONObject getPostObject()
	{
		try
		{
			JSONObject data = new JSONObject();
			data.put("name", name);
			data.put("gender", isFemale ? "f" : "m");
			if (birthdate != null)
			{
				JSONObject bday = new JSONObject();
				if (birthdate.day != 0)
				{
					bday.put("day", birthdate.day);
				}
				if (birthdate.month != 0)
				{
					bday.put("month", birthdate.month);
				}
				bday.put("year", birthdate.year);
				data.put("dob", bday);
			}
			data.put("screen", "signup");
			return data;
		}
		catch (JSONException e)
		{
			return null;
		}
	}
}
