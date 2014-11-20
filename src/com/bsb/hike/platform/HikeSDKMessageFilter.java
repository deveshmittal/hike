package com.bsb.hike.platform;

import android.content.Context;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.model.card.HikeCardConstants;
import com.bsb.hike.models.ConvMessage;
import org.json.JSONObject;

public class HikeSDKMessageFilter implements HikePlatformConstants{
	public static ConvMessage filterMessage(JSONObject json, Context context){
		try{

		ConvMessage convMessage = new ConvMessage();
        convMessage.setIsSent(true);
		convMessage.setMessageType(HikeCardConstants.CARD_MSG_TYPE.equals(json.optString(MESSAGE_TYPE)) ? HikeConstants.MESSAGE_TYPE.CONTENT
				: HikeConstants.MESSAGE_TYPE.PLAIN_TEXT);
		convMessage.platformMessageMetadata = new PlatformMessageMetadata(json, context);
        convMessage.setMessage(convMessage.platformMessageMetadata.notifText);
            convMessage.setMessageType(HikeCardConstants.CARD_MSG_TYPE.equals(json.optString(MESSAGE_TYPE)) ? HikeConstants.MESSAGE_TYPE.CONTENT
                    : HikeConstants.MESSAGE_TYPE.PLAIN_TEXT);
		// fetch love here is present
		return convMessage;

		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return null;
	}
}
