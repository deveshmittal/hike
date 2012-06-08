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
	private Context context;

	public DbConversationListener(Context context)
	{
		mPubSub = HikeMessengerApp.getPubSub();
		mConversationDb = new HikeConversationsDatabase(context);
		mUserDb = new HikeUserDatabase(context);
		persistence = new HikeMqttPersistence(context);
		this.context = context;
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_SENT, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED_FROM_SENDER, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED_READ, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED, this);
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
		else if (HikePubSub.MESSAGE_RECEIVED_FROM_SENDER.equals(type))  // represents event when a client receive msg from other client through server.
		{
			ConvMessage message = (ConvMessage) object;
			mConversationDb.addConversationMessages(message);
			Log.d("DBCONVERSATION LISTENER","Receiver received Message : "+message.getMessage() + "		;	Receiver Msg ID : "+message.getMsgID()+"	; Mapped msgID : "+message.getMappedMsgID());

			mPubSub.publish(HikePubSub.MESSAGE_RECEIVED, message);		
		}
		else if (HikePubSub.SERVER_RECEIVED_MSG.equals(type))  // server got msg from client 1 and sent back received msg receipt
		{
			Log.d("DBCONVERSATION LISTENER","(Sender) Message sent confirmed for msgID -> "+(Long)object);
			updateDB(object,ConvMessage.State.SENT_CONFIRMED.ordinal());
		}
		else if (HikePubSub.MESSAGE_DELIVERED.equals(type))  // server got msg from client 1 and sent back received msg receipt
		{
			Log.d("DBCONVERSATION LISTENER","Msg delivered to receiver for msgID -> "+(Long)object);
			updateDB(object,ConvMessage.State.SENT_DELIVERED.ordinal());
		}
		else if (HikePubSub.MESSAGE_DELIVERED_READ.equals(type))  // server got msg from client 1 and sent back received msg receipt
		{
			long[] ids = (long[]) object;
			Log.d("DBCONVERSATION LISTENER", "Message delivered read for ids " + ids);
			updateDbBatch(ids,ConvMessage.State.SENT_DELIVERED_READ.ordinal());
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

	private void updateDbBatch(long[] ids, int status)
	{
		mConversationDb.updateBatch(ids, ConvMessage.State.SENT_DELIVERED_READ.ordinal());
	}

	private void updateDB(Object object, int status)
	{
		long msgID = (Long)object;
		/* TODO we should lookup the convid for this user, since otherwise one could set mess with the state for other conversations */
		mConversationDb.updateMsgStatus(msgID,status);
	}
}
