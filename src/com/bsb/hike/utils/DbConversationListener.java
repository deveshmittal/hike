package com.bsb.hike.utils;

import org.json.JSONArray;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.models.ConvMessage;

public class DbConversationListener implements Listener
{

	HikeConversationsDatabase mConversationDb;

	HikeUserDatabase mUserDb;
	
	private HikePubSub mPubSub;

	private Editor mEditor;

	public DbConversationListener(Context context)
	{
		mPubSub = HikeMessengerApp.getPubSub();
		mConversationDb = new HikeConversationsDatabase(context);
		mUserDb = new HikeUserDatabase(context);
		mEditor = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_SENT, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_RECEIVED_FROM_SENDER, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SERVER_RECEIVED_MSG, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED_READ, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELIVERED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.MESSAGE_DELETED, this);
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.MESSAGE_SENT.equals(type))
		{
			ConvMessage convMessage = (ConvMessage) object;
			mConversationDb.addConversationMessages(convMessage);
			Log.d("DBCONVERSATION LISTENER","Sending Message : "+convMessage.getMessage()+"	;	to : "+convMessage.getConversation().getContactName());
			mPubSub.publish(HikePubSub.WS_SEND, convMessage.serialize("send")); // this is used to be sent by the web socket.
		}
		else if (HikePubSub.MESSAGE_RECEIVED_FROM_SENDER.equals(type))  // represents event when a client receive msg from other client through server.
		{
			ConvMessage message = (ConvMessage) object;
			mConversationDb.addConversationMessages(message);
			Log.d("DBCONVERSATION LISTENER","Receiver received Message : "+message.getMessage() + "		;	Receiver Msg ID : "+message.getMsgID()+"	; Mapped msgID : "+message.getMappedMsgID());
			mPubSub.publish(HikePubSub.WS_SEND, message.serializeDeliveryReport("msgDelivered")); // handle return to sender
			mPubSub.publish(HikePubSub.MESSAGE_RECEIVED, message);		
		}
		else if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			Integer credits = (Integer) object;
			mEditor.putInt(HikeMessengerApp.SMS_SETTING, credits.intValue());
			mEditor.commit();
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
			mConversationDb.deleteMessage((Long) object);
		}
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
