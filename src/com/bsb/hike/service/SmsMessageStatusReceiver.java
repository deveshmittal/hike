package com.bsb.hike.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.db.HikeMqttPersistence;

public class SmsMessageStatusReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		long msgId = intent.getLongExtra(HikeConstants.Extras.SMS_ID, -1);
		switch (getResultCode())
		{
		case Activity.RESULT_OK:
			HikeMessengerApp.getPubSub().publish(HikePubSub.SERVER_RECEIVED_MSG, msgId);

			if (msgId != -1)
			{
				HikeMqttPersistence.getInstance().removeMessage(msgId);
			}
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			break;
		}
	}
};
