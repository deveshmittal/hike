package com.bsb.hike.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;

public class SMSBroadcastReceiver extends BroadcastReceiver
{
	HikeUserDatabase mDb;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (mDb == null)
		{
			mDb = HikeUserDatabase.getInstance();
		}

		Log.d(getClass().getSimpleName(), "Received SMS message");
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			Log.d(getClass().getSimpleName(), "Received SMS message with extras " + extras.keySet());
			Object[] extra = (Object[]) extras.get("pdus");
			Log.d(getClass().getSimpleName(), "Extras length is " + extra.length);
			for(int i = 0; i < extra.length; ++i)
			{
				SmsMessage sms = SmsMessage.createFromPdu((byte[]) extra[i]);

				String body = sms.getMessageBody();
				long timestamp = sms.getTimestampMillis()/1000;
				String from = sms.getOriginatingAddress();
				ContactInfo contactInfo = mDb.getContactInfoFromPhoneNo(from);
				JSONObject msg = new JSONObject();
				try
				{
					msg.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE);
					msg.put(HikeConstants.FROM, contactInfo.getMsisdn());
					JSONObject data = new JSONObject();
					data.put(HikeConstants.SMS_MESSAGE, body);
					data.put(HikeConstants.TIMESTAMP, timestamp);
					data.put(HikeConstants.MESSAGE_ID, sms.getIndexOnIcc());
					msg.put(HikeConstants.DATA, data);
				}
				catch (JSONException e)
				{
					Log.e(getClass().getSimpleName(), "Invalid data for SMS message", e);
					return;
				}

				Log.d(getClass().getSimpleName(), "Received SMS " + msg.toString());
				Intent smsReceivedIntent = new Intent(context, HikeService.class);
				smsReceivedIntent.putExtra(HikeConstants.Extras.SMS_MESSAGE, msg.toString());
				context.startService(smsReceivedIntent);
				this.abortBroadcast();
			}
		}
	}

	
}
