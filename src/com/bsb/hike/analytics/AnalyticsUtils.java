package com.bsb.hike.analytics;

import java.util.Random;

import android.text.TextUtils;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class AnalyticsUtils
{

	public static Random randomGenerator = new Random();
	
	private static int text_msg_track_decider = 2;
	
	private static int stk_msg_track_decider = 2;
	
	private static int multimedia_msg_track_decider = 2;
	
	public static boolean isMessageToBeTracked(String msgType)
	{
		//TODO THIS IS COMMENTED JUST FOR TESTING PURPOSE
		//TODO UNCOMMENT IT BEFORE MERGING
		/*
		if(!TextUtils.isEmpty(msgType))
		{
			if(AnalyticsConstants.MessageType.TEXT.equals(msgType))
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_TEXT_MSG);
				int maxAllowed = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_TEXT_MSG, text_msg_track_decider);
				if(randomInt <= maxAllowed)
				{
					return true;
				}
			}
			else if(AnalyticsConstants.MessageType.STICKER.equals(msgType))
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_STK_MSG);
				int maxAllowed = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_STICKER_MSG, stk_msg_track_decider);
				if(randomInt <= maxAllowed)
				{
					return true;
				}
			}
			else
			{
				int randomInt = randomGenerator.nextInt(AnalyticsConstants.MAX_RANGE_MULTIMEDIA_MSG);
				int maxAllowed = HikeSharedPreferenceUtil.getInstance().
						getData(HikeMessengerApp.PROB_NUM_MULTIMEDIA_MSG, multimedia_msg_track_decider);
				if(randomInt <= maxAllowed)
				{
					return true;
				}
			}
		}
		
		return false;*/
		return true;
	}
}
