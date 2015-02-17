package com.bsb.hike.chatthread;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;

public class ChatThreadUtils
{
	protected static void playUpDownAnimation(Context context, final View view)
	{
		if (view == null)
		{
			return;
		}

		Animation an = AnimationUtils.loadAnimation(context, R.anim.down_up_up_part);
		an.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationStart(Animation animation)
			{
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				view.setVisibility(View.GONE);
			}
		});
		view.startAnimation(an);
	}

	/**
	 * This method is used to add pin related parameters in the convMessage
	 * 
	 * @param convMessage
	 */
	protected static void modifyMessageToPin(Context context, ConvMessage convMessage)
	{
		convMessage.setMessageType(HikeConstants.MESSAGE_TYPE.TEXT_PIN);
		JSONObject jsonObject = new JSONObject();
		try
		{
			jsonObject.put(HikeConstants.PIN_MESSAGE, 1);
			convMessage.setMetadata(jsonObject);
			convMessage.setHashMessage(HikeConstants.HASH_MESSAGE_TYPE.HASH_PIN_MESSAGE);
		}
		catch (JSONException je)
		{
			Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show();
			je.printStackTrace();
		}
	}

}
