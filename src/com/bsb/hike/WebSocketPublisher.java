package com.bsb.hike;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.utils.HikeUserDatabase;

public class WebSocketPublisher implements HikePubSub.Listener {

	private SharedPreferences settings;
	private Context context;
	private HikeUserDatabase mDb;
	private HikePubSub pubSub;

	public WebSocketPublisher(Context context) {
		this.mDb = new HikeUserDatabase(context);
		this.context = context;
		this.settings = context.getSharedPreferences(HikeMessengerApp.MESSAGES_SETTING, 0);
		this.pubSub = HikeMessengerApp.getPubSub();
		this.pubSub.addListener(HikePubSub.WS_MESSAGE, this);
		this.pubSub.addListener(HikePubSub.NEW_CONVERSATION, this);
	}

	public void onMessage(String msg) {
		JSONObject data;
		String type;
		try {
			data = new JSONObject(msg);
			type = data.getString("type");
		} catch (JSONException e) {
			Log.e("WebSocketPublisher", "Invalid JSON message: " + msg);
			return;
		}

		if ("message".equals(type)) {
			String id = data.optString("id");
			String msisdn = data.optString("from");
			ContactInfo contactInfo = this.mDb.getContactInfoFromMSISDN(msisdn);
			String contactId = (contactInfo != null) ? contactInfo.id : null;
			String message = data.optString("data");
			int ts = data.optInt("ts");
			ConvMessage convMessage = new ConvMessage(message, msisdn, contactId, ts, false);
			this.pubSub.publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		} else if ("typing".equals(type)) {
			String msisdn = data.optString("from");
			this.pubSub.publish(HikePubSub.TYPING_CONVERSATION, msisdn);
		} else if ("stop_typing".equals(type)) {
			String msisdn = data.optString("from");
			this.pubSub.publish(HikePubSub.END_TYPING_CONVERSATION, msisdn);
		} else {
			Log.d("WebSocketPublisher", "Unknown Type:" + type);
		}
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (type.equals(HikePubSub.WS_MESSAGE)) {
			String message = (String) object;
			onMessage(message);
		}
	}
}
