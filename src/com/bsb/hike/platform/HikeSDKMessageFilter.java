package com.bsb.hike.platform;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.modules.contactmgr.ContactManager;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HikeSDKMessageFilter implements HikePlatformConstants{
	public static List<ConvMessage> filterMessage(JSONObject json){
		try{
		List<ConvMessage> listOfConvMessages = new ArrayList<ConvMessage>();
		String[] toArray = json.has(RECEPIENT) ? json.getString(RECEPIENT).split(",") : new String[]{};
		ArrayList<String> msisdns = ContactManager.getInstance().getMsisdnFromId(toArray);
		for(String msisdn : msisdns){
		ConvMessage convMessage = new ConvMessage();

		convMessage.setMessageType(com.bsb.hike.model.card.CardConstants.CARD_MSG_TYPE.equals(json.optString(MESSAGE_TYPE)) ? HikeConstants.MESSAGE_TYPE.CONTENT
				: HikeConstants.MESSAGE_TYPE.PLAIN_TEXT);
		convMessage.setMsisdn(msisdn);
		convMessage.platformMessageMetadata = new PlatformMessageMetadata(json);
        convMessage.setMessage(convMessage.platformMessageMetadata.notifText);
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
