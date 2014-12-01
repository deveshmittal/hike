package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Pair;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;

public class MultipleConvMessage
{
	
	private ArrayList<ConvMessage> messageList;
	private ArrayList<ContactInfo> contactList;
	private boolean createChatThread;
	private String source;

	public ArrayList<ConvMessage> getMessageList()
	{
		return messageList;
	}

	public void setMessageList(ArrayList<ConvMessage> messageList)
	{
		this.messageList = messageList;
	}

	public ArrayList<ContactInfo> getContactList()
	{
		return contactList;
	}

	public void setContactList(ArrayList<ContactInfo> list)
	{
		this.contactList = list;
	}

	private long timeStamp;

	public long getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	
	public void setCreateChatThread(boolean val)
	{
		this.createChatThread = val;
	}
	
	public boolean getCreateChatThread()
	{
		return createChatThread;
	}
	
	public MultipleConvMessage(ArrayList<ConvMessage> messageList, ArrayList<ContactInfo> contactList)
	{
		this.messageList = messageList;
		this.timeStamp = System.currentTimeMillis()/1000;
		this.contactList = contactList;
	}

	public MultipleConvMessage(ArrayList<ConvMessage> messageList, ArrayList<ContactInfo> contactList, long timeStamp)
	{
		this.messageList = messageList;
		this.timeStamp = timeStamp;
		this.contactList = contactList;
	}
	
	public MultipleConvMessage(ArrayList<ConvMessage> messageList, ArrayList<ContactInfo> contactList, long timeStamp,boolean createChatThread, String source)
	{
		this.messageList = messageList;
		this.timeStamp = timeStamp;
		this.contactList = contactList;
		this.createChatThread = createChatThread;
		this.source = source;
	}
	
	public JSONObject serialize()
	{
		JSONObject object = new JSONObject();
		JSONObject data = new JSONObject();
		JSONObject md = null;
		try
		{
			object.put(HikeConstants.TYPE, HikeConstants.MqttMessageTypes.MESSAGE);
			object.put(HikeConstants.SUB_TYPE, HikeConstants.MqttMessageTypes.MULTIPLE_FORWARD);

			if(source!=null)
			{
				JSONObject metadata = new JSONObject();
				metadata.put(HikeConstants.SOURCE, source);
				data.put(HikeConstants.METADATA, metadata);
			}

			data.put(HikeConstants.TIMESTAMP, timeStamp);
			data.put(HikeConstants.MESSAGE_ID, messageList.get(0).getMsgID());
			
			JSONArray msgArray = new JSONArray();
			for (int i=0; i<messageList.size();i++)
			{
				JSONObject msg = new JSONObject();
				msg.put(HikeConstants.HIKE_MESSAGE, ((ConvMessage)messageList.get(i)).getMessage());
				if(((ConvMessage)messageList.get(i)).getMetadata()!=null){
					msg.put(HikeConstants.METADATA,((ConvMessage)messageList.get(i)).getMetadata().getJSON());
				}
				
				msgArray.put(msg);
			}
			data.put(HikeConstants.MESSAGES, msgArray);
			
			JSONArray msisdnArray = new JSONArray();
			for (int i=0; i<contactList.size();i++)
			{
				msisdnArray.put((String)contactList.get(i).getMsisdn());
			}
			
			data.put(HikeConstants.LIST, msisdnArray);
			object.put(HikeConstants.DATA, data);
			
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logger.d("dcdc",object.toString());
		return object;
	}
	
	public void sendPubSubForConvScreenMultiMessage()
	{
		ArrayList<ConvMessage> convMessages = getMessageList();
		long baseId = ((ConvMessage)convMessages.get(0)).getMsgID();
		int totalMessages = convMessages.size();
		ConvMessage lastMessage = convMessages.get(totalMessages-1);
		long lastMessageId = baseId + totalMessages-1;
		List<ContactInfo> recipient = getContactList();
		int totalRecipient = recipient.size();
		List<Pair<ContactInfo, ConvMessage>> allPairs = new ArrayList<Pair<ContactInfo,ConvMessage>>(totalRecipient);
		long timestamp = getTimeStamp();
		for(int i=0;i<totalRecipient;i++)
		{
			ConvMessage message = new ConvMessage(lastMessage);
			message.setTimestamp(timestamp++);
			message.setMsgID(lastMessageId+(i*totalMessages));
			ContactInfo contactInfo = recipient.get(i);
			message.setMsisdn(contactInfo.getMsisdn());
			Pair<ContactInfo, ConvMessage> pair = new Pair<ContactInfo, ConvMessage>(contactInfo, message);
			allPairs.add(pair);
		}
		HikeMessengerApp.getPubSub().publish(HikePubSub.MULTI_MESSAGE_DB_INSERTED, allPairs);
	}
	
}
