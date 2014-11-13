package com.bsb.hike.platform;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.models.ConvMessage;
import org.json.JSONObject;

public class HikeSDKMessageFilter implements HikePlatformConstants{
	public static ConvMessage filterMessage(JSONObject json){
		try{

		ConvMessage convMessage = new ConvMessage();

		convMessage.setMessageType(com.bsb.hike.model.card.CardConstants.CARD_MSG_TYPE.equals(json.optString(MESSAGE_TYPE)) ? HikeConstants.MESSAGE_TYPE.CONTENT
				: HikeConstants.MESSAGE_TYPE.PLAIN_TEXT);
		convMessage.platformMessageMetadata = new PlatformMessageMetadata(json);
        convMessage.setMessage(convMessage.platformMessageMetadata.notifText);
		// fetch love here is present
		return convMessage;

		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return null;
	}
}
