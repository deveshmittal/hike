package com.bsb.hike.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.snowfall.SnowFallView;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;

public class ChatBgFtue
{
	public static SnowFallView startAndSetSnowFallView(final Activity activity){
		activity.findViewById(R.id.chat_bg_ftue_fade).setVisibility(View.VISIBLE);
		Handler animHandler = new Handler();
		AlphaAnimation alphaAnim = new AlphaAnimation(0.1f, 0.6f);
		AccelerateInterpolator accInterpolator = new AccelerateInterpolator(1f);
		alphaAnim.setInterpolator(accInterpolator);
		alphaAnim.setStartOffset(400);
		alphaAnim.setDuration(800);
		alphaAnim.setFillAfter(true);
		activity.findViewById(R.id.chat_bg_ftue_fade).startAnimation(alphaAnim); // dim

		FrameLayout layout = (FrameLayout) activity.findViewById(R.id.parent_layout);
		final SnowFallView snowFallView = new SnowFallView(activity);
		snowFallView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		snowFallView.setVisibility(View.GONE);
		layout.addView(snowFallView);
		
		animHandler.postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				snowFallView.setVisibility(View.VISIBLE);
			}
		}, 1300);
	
		return snowFallView;
	}

}
