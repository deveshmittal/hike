package com.bsb.hike.analytics;

import java.util.Random;

import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;

public class AnalyticsUtils
{

	public static Random randomGenerator = new Random();
	
	public static boolean isMessageToBeTracked(String msgType)
	{
		if(!TextUtils.isEmpty(msgType))
		{
			if(AnalyticsConstants.MessageType.TEXT.equals(msgType))
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_TEXT_MSG);
				int probSample = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_TEXT_MSG, AnalyticsConstants.text_msg_track_decider);
				Logger.d(AnalyticsConstants.MSG_REL_TAG, " --random number : " + randomInt + ", prob sampling: "+ probSample);
				if(randomInt <= probSample)
				{
					return true;
				}
			}
			else if(AnalyticsConstants.MessageType.STICKER.equals(msgType))
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_STK_MSG);
				int probSample = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_STICKER_MSG, AnalyticsConstants.stk_msg_track_decider);
				Logger.d(AnalyticsConstants.MSG_REL_TAG, " --random number : " + randomInt + ", prob sampling: "+ probSample);
				if(randomInt <= probSample)
				{
					return true;
				}
			}
			else if(AnalyticsConstants.MessageType.MULTIMEDIA.equals(msgType))
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_MULTIMEDIA_MSG);
				int probSample = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_MULTIMEDIA_MSG, AnalyticsConstants.multimedia_msg_track_decider);
				Logger.d(AnalyticsConstants.MSG_REL_TAG, " --random number : " + randomInt + ", prob sampling: "+ probSample);
				if(randomInt <= probSample)
				{
					return true;
				}
			}
		}
		
		return false;
	}
}
