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
		
		setGiftFallAnim(activity);
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

	public static void setGiftFallAnim(final Activity activity)
	{
		Handler animHandler = new Handler();
		FrameLayout layout = (FrameLayout) activity.findViewById(R.id.parent_layout);
		View.inflate(activity, R.layout.chat_bg_ftue_box, layout);
		activity.findViewById(R.id.gift_box_layout).setVisibility(View.GONE);
		
		animHandler.postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				AnimationSet boxFallAnimSet = new AnimationSet(true);
				boxFallAnimSet.setFillAfter(true);
				int rtype = Animation.RELATIVE_TO_SELF;
				float rotationAngle = 15;
				RotateAnimation ra1 = new RotateAnimation(0, rotationAngle,rtype,0.5f , rtype,0.5f );
				ra1.setDuration(500);
				
				boxFallAnimSet.addAnimation(ra1);
				
				activity.findViewById(R.id.gift_box_layout).setVisibility(View.VISIBLE);
				Animation boxFallAnim = AnimationUtils.loadAnimation(activity,
					R.anim.boxfall);
				boxFallAnimSet.addAnimation(boxFallAnim);
				
				RotateAnimation ra2 = new RotateAnimation(0, -rotationAngle,rtype, 0.5f , rtype, 0.5f );
				ra2.setStartOffset(500);
				ra2.setDuration(200);
				boxFallAnimSet.addAnimation(ra2);
				
				RotateAnimation ra3 = new RotateAnimation(0, rotationAngle/3,rtype, 0.5f , rtype, 0.5f );
				ra3.setStartOffset(700);
				ra3.setDuration(100);
				boxFallAnimSet.addAnimation(ra3);
				
				RotateAnimation ra4 = new RotateAnimation(0, -rotationAngle/3,rtype, 0.5f , rtype, 0.5f );
				ra4.setStartOffset(800);
				ra4.setDuration(100);
				boxFallAnimSet.addAnimation(ra4);
				
				activity.findViewById(R.id.gift_box_layout).startAnimation(boxFallAnimSet);
			}
		}, 3000);
		
		animHandler.postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				AnimationSet giftCardApearAnimSet = new AnimationSet(true);
				giftCardApearAnimSet.setFillAfter(true);
				int rtype = Animation.RELATIVE_TO_SELF;
				float rotationAngle = 15;
				RotateAnimation ra1 = new RotateAnimation(rotationAngle, 0,rtype,0.5f , rtype,0.5f );
				ra1.setDuration(600);
				
				giftCardApearAnimSet.addAnimation(ra1);
				
				activity.findViewById(R.id.gift_card).setVisibility(View.VISIBLE);
				AlphaAnimation cardFadeInAnim = new AlphaAnimation(0.1f, 1);
				cardFadeInAnim.setDuration(600);
				giftCardApearAnimSet.addAnimation(cardFadeInAnim);
				
				ScaleAnimation cardScaleInAnim = new ScaleAnimation(1.2f, 1, 1.2f, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				cardScaleInAnim.setDuration(600);
				giftCardApearAnimSet.addAnimation(cardScaleInAnim);
				
				activity.findViewById(R.id.gift_card).startAnimation(giftCardApearAnimSet);
				
			}
		}, 4800);
	}
	

}
