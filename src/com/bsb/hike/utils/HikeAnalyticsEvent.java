package com.bsb.hike.utils;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;

public class HikeAnalyticsEvent
{
	/*
	 * We send this event every time user mark some chats as stealth
	 */
	public static void sendStealthMsisdns(List<String> enabledMsisdn, List<String> disabledMsisdn)
	{
		// TODO use array instead of sets here.
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.STEALTH);

			JSONObject dataJson = new JSONObject();
			if (enabledMsisdn != null)
			{
				dataJson.put(HikeConstants.ENABLED_STEALTH, new JSONArray(enabledMsisdn));
			}
			if (disabledMsisdn != null)
			{
				dataJson.put(HikeConstants.DISABLED_STEALTH, new JSONArray(disabledMsisdn));
			}
			object.put(HikeConstants.DATA, dataJson);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, object);
		}
		catch (JSONException e)
		{
			Logger.e("HikeAnalyticsEvent", "Exception in sending analytics event", e);
		}

	}

	/*
	 * We send this event every time when user resets stealth mode
	 */
	public static void sendStealthReset()
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.STEALTH);

			JSONObject dataJson = new JSONObject();
			dataJson.put(HikeConstants.RESET, true);
			object.put(HikeConstants.DATA, dataJson);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, object);
		}
		catch (JSONException e)
		{
			Logger.e("HikeAnalyticsEvent", "Exception in sending analytics event", e);
		}
	}

	/*
	 * We send this event every time when user enter stealth mode
	 */
	public static void sendStealthEnabled(boolean enabled)
	{
		JSONObject object = new JSONObject();
		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.TOGGLE_STEALTH);

			JSONObject dataJson = new JSONObject();
			dataJson.put(HikeConstants.ENABLED, enabled);
			object.put(HikeConstants.DATA, dataJson);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH_LOW, object);
		}
		catch (JSONException e)
		{
			Logger.e("HikeAnalyticsEvent", "Exception in sending analytics event", e);
		}
	}
}
