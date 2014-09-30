package com.bsb.hike.models;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.PairModified;

public class MultipleConvMessage
{
	
	private ArrayList<ConvMessage> messageList;
	private ArrayList<ContactInfo> contactList;
	
	public long getMsgID()
	{
		return msgID;
	}

	public void setMsgID(long msgID)
	{
		this.msgID = msgID;
	}

	public long getMappedMsgId()
	{
		return mappedMsgId;
	}

	public void setMappedMsgId(long mappedMsgId)
	{
		this.mappedMsgId = mappedMsgId;
	}

	private long msgID; // this corresponds to msgID stored in sender's DB

	private long mappedMsgId; // this corresponds to msgID stored in receiver's
								// DB
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

	public MultipleConvMessage(ArrayList<ConvMessage> messageList, ArrayList<ContactInfo> contactList, long timeStamp)
	{
		this.messageList = messageList;
		this.timeStamp = timeStamp;
		this.contactList = contactList;
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
		
			data.put(HikeConstants.TIMESTAMP, timeStamp);
			data.put(HikeConstants.MESSAGE_ID, msgID);
			
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
	
}
