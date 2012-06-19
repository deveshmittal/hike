package com.bsb.hike.db;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.ConvMessage.ParticipantInfoState;

public class DbConversationListener implements Listener
{
	HikeConversationsDatabase mConversationDb;

	HikeUserDatabase mUserDb;

	HikeMqttPersistence persistence;

	private HikePubSub mPubSub;

	public DbConversationListener(Context context)
	{
		mPubSub = HikeMessengerApp.getPubSub();
		mConversationDb = new HikeConversationsDatabase(context);
		mUserDb = new HikeUserDatabase(context);
		persistence = new HikeMqttPersistence(context);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_SENT, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELETED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_FAILED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.BLOCK_USER, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.UNBLOCK_USER, this);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.MESSAGE_SENT.equals(type))
		{
			ConvMessage convMessage = (ConvMessage) object;
			mConversationDb.addConversationMessages(convMessage);

			if (convMessage.getParticipantInfoState() == ParticipantInfoState.NO_INFO) 
			{
				Log.d("DBCONVERSATION LISTENER","Sending Message : "+convMessage.getMessage()+"	;	to : "+convMessage.getMsisdn());
				mPubSub.publish(HikePubSub.MQTT_PUBLISH, convMessage.serialize());
			}
		}
		else if (HikePubSub.MESSAGE_DELETED.equals(type))
		{
			Long msgId = (Long) object;
			mConversationDb.deleteMessage(msgId);
			persistence.removeMessage(msgId);
		}
		else if (HikePubSub.MESSAGE_FAILED.equals(type))  // server got msg from client 1 and sent back received msg receipt
		{
			updateDB(object,ConvMessage.State.SENT_FAILED.ordinal());
		}
		else if (HikePubSub.BLOCK_USER.equals(type))
		{
			String msisdn = (String) object;
			mUserDb.block(msisdn);
			JSONObject blockObj = blockUnblockSerialize("b",msisdn);
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, blockObj);
		}
		else if (HikePubSub.UNBLOCK_USER.equals(type))
		{
			String msisdn = (String) object;
			mUserDb.unblock(msisdn);
			JSONObject unblockObj = blockUnblockSerialize("ub",msisdn);
			mPubSub.publish(HikePubSub.MQTT_PUBLISH, unblockObj);
		}
	}

	private JSONObject blockUnblockSerialize(String type, String msisdn) 
	{
		JSONObject obj = new JSONObject();
		try 
		{
			obj.put(HikeConstants.TYPE, type);
			obj.put(HikeConstants.DATA, msisdn);
		} 
		catch (JSONException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}

	private void updateDB(Object object, int status)
	{
		long msgID = (Long)object;
		/* TODO we should lookup the convid for this user, since otherwise one could set mess with the state for other conversations */
		mConversationDb.updateMsgStatus(msgID,status);
	}
}
