package com.bsb.hike.platform;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.model.card.CardConstants;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;

public class HikeSDKMessageFilter implements HikePlatformConstants{
	public static List<ConvMessage> filterMessage(JSONObject json){
		try{
		List<ConvMessage> listOfConvMessages = new ArrayList<ConvMessage>();
		String[] toArray = json.has(RECEPIENT) ? json.getString(RECEPIENT).split(",") : new String[]{};
		ArrayList<String> msisdns = ContactManager.getInstance().getMsisdnFromId(toArray);
		for(String msisdn : msisdns){
		ConvMessage convMessage = new ConvMessage();
		convMessage.setMessageType(CardConstants.CARD_MSG_TYPE.equals(json.optString(MESSAGE_TYPE)) ? HikeConstants.MESSAGE_TYPE.CONTENT
				: HikeConstants.MESSAGE_TYPE.PLAIN_TEXT);
		convMessage.setMsisdn(msisdn);
		convMessage.platformMessageMetadata = new PlatformMessageMetadata(json);
		// fetch love here is present
		listOfConvMessages.add(convMessage);
		}
		return listOfConvMessages;
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return null;
	}
}
