package com.bsb.hike.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.snowfall.SnowFallView;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.HomeActivity;

public class ChatBgFtue
{
	public static SnowFallView startAndSetSnowFallView(final Activity activity){
		activity.findViewById(R.id.chat_bg_ftue_fade).setVisibility(View.VISIBLE);
		Handler animHandler = new Handler();
		AlphaAnimation alphaAnim = new AlphaAnimation(0.2f, 1f);
		alphaAnim.setFillAfter(true);
			
		if(((int)Utils.densityMultiplier *100) >= 100){
			alphaAnim.setDuration(1400);
			activity.findViewById(R.id.chat_bg_ftue_fade).startAnimation(alphaAnim); // dim
			FrameLayout layout = (FrameLayout) activity.findViewById(R.id.parent_layout);
			final SnowFallView snowFallView = new SnowFallView(activity);
			snowFallView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			snowFallView.setVisibility(View.GONE);
			layout.addView(snowFallView,3);

			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					setGiftFallAnim(activity);

				}
			}, 2800);

			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					snowFallView.setVisibility(View.VISIBLE);
					AlphaAnimation alphaAnim = new AlphaAnimation(0.1f, 1f);
					AccelerateInterpolator accInterpolator = new AccelerateInterpolator(1f);
					alphaAnim.setInterpolator(accInterpolator);
					//alphaAnim.setStartOffset(600);
					alphaAnim.setDuration(600);
					alphaAnim.setFillAfter(true);
					snowFallView.startAnimation(alphaAnim);
				}
			}, 1200);
			return snowFallView;
		} else{
			alphaAnim.setDuration(1800);
			activity.findViewById(R.id.chat_bg_ftue_fade).startAnimation(alphaAnim); // dim
			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					setGiftFallAnim(activity);

				}
			}, 2000);
			return null;
		}
		
	
	}

	public static void setGiftFallAnim(final Activity activity)
	{
		activity.findViewById(R.id.gift_box_layout_view).setVisibility(View.VISIBLE);
		Handler animHandler = new Handler();
		
		AnimationSet boxFallAnimSet = new AnimationSet(true);
		boxFallAnimSet.setInterpolator(new AccelerateInterpolator(1.5f));
		int animDuration = 600;
		AlphaAnimation aa1 = new AlphaAnimation(0f, 1f);
		aa1.setDuration(350);
		boxFallAnimSet.addAnimation(aa1);
		
		int rtype = Animation.RELATIVE_TO_SELF;
		float rotationAngle = 7.5f;
		RotateAnimation ra1 = new RotateAnimation(20, rotationAngle, rtype, 0.5f , rtype,0.5f );
		ra1.setDuration(animDuration);
		
		boxFallAnimSet.addAnimation(ra1);
		
		Animation boxFallAnim = new TranslateAnimation(0, 6*Utils.densityMultiplier, -400*Utils.densityMultiplier,-13*Utils.densityMultiplier);
		
		boxFallAnim.setDuration(animDuration);
		boxFallAnimSet.addAnimation(boxFallAnim);
		
		TranslateAnimation ta1 = new TranslateAnimation(0, 0, 0, -11*Utils.densityMultiplier);
		ta1.setStartOffset(animDuration-6);
		ta1.setDuration(150);
		boxFallAnimSet.addAnimation(ta1);
		
		RotateAnimation ra2 = new RotateAnimation(0, -(rotationAngle+ 5.8f),rtype, 0.5f , rtype, 0.5f );
		ra2.setStartOffset(animDuration-6);
		ra2.setDuration(182);
		boxFallAnimSet.addAnimation(ra2);
		
		TranslateAnimation ta2 = new TranslateAnimation(0, 0, 0, 24*Utils.densityMultiplier);
		ta2.setInterpolator(new AccelerateInterpolator(3f));
		ta2.setStartOffset(animDuration+95+44);
		ta2.setDuration(150);
		boxFallAnimSet.addAnimation(ta2);
		
		RotateAnimation ra3 = new RotateAnimation(0, 8f, rtype, 0.5f , rtype, 0.5f );
		ra3.setStartOffset(animDuration+127+44);
		ra3.setDuration(216);
		
		boxFallAnimSet.addAnimation(ra3);
		
		RotateAnimation ra4 = new RotateAnimation(0, -2.2f,rtype, 0.5f , rtype, 0.5f );
		ra4.setStartOffset(animDuration+295+44);
		ra4.setDuration(114);
		
		boxFallAnimSet.addAnimation(ra4);
		
		activity.findViewById(R.id.gift_box).startAnimation(boxFallAnimSet);
		
		animHandler.postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				AnimationSet giftCardApearAnimSet = new AnimationSet(true);
				int rtype = Animation.RELATIVE_TO_SELF;
				float rotationAngle = 15;
				RotateAnimation ra1 = new RotateAnimation(rotationAngle, 0,rtype,0.5f , rtype,0.5f );
				ra1.setDuration(400);
				
				giftCardApearAnimSet.addAnimation(ra1);
				
				activity.findViewById(R.id.gift_card).setVisibility(View.VISIBLE);
				AlphaAnimation cardFadeInAnim = new AlphaAnimation(0.1f, 1);
				cardFadeInAnim.setDuration(400);
				giftCardApearAnimSet.addAnimation(cardFadeInAnim);
				
				ScaleAnimation cardScaleInAnim = new ScaleAnimation(1.2f, 1, 1.2f, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				cardScaleInAnim.setDuration(400);
				giftCardApearAnimSet.addAnimation(cardScaleInAnim);
				
				activity.findViewById(R.id.gift_card).startAnimation(giftCardApearAnimSet);
				
			}
		}, 2000);
	}
	
	public static void onChatBgOpenItUpClick(final HomeActivity activity, View v, final SnowFallView snowFallView){
		activity.findViewById(R.id.gift_card).clearAnimation();
		activity.findViewById(R.id.open_it_up_text).setClickable(false);
		
		Handler animHandler = new Handler();
		animHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				activity.findViewById(R.id.gift_card).setVisibility(View.GONE);
				activity.findViewById(R.id.box_flip_side_ribbon).setVisibility(View.VISIBLE);
				
				View giftBoxLayout = activity.findViewById(R.id.gift_box);
				int xCord = giftBoxLayout.getWidth()/2;
				int yCord = giftBoxLayout.getWidth()/2;
				int rtype = Animation.ABSOLUTE;
		        RotateAnimation animation = new RotateAnimation(0, 90, rtype, xCord, rtype, yCord);  
			    animation.setFillAfter(true);  
			    animation.setDuration(0);  
			    activity.findViewById(R.id.box_flip_side_ribbon).startAnimation(animation); 
		        
				activity.findViewById(R.id.fs_middle_dot).setVisibility(View.VISIBLE);
			}
		}, 400);
		AnimationSet flipBoxAnimSet = new AnimationSet(true);
		flipBoxAnimSet.setFillAfter(true);
		int rtype = Animation.RELATIVE_TO_SELF;
		float rotationAngle = 90;
		RotateAnimation ra1 = new RotateAnimation(0, -rotationAngle,rtype,0.5f , rtype,0.5f );
		ra1.setDuration(800);
		flipBoxAnimSet.addAnimation(ra1);

		View parentLayout = activity.findViewById(R.id.parent_layout);
		int xCord = (parentLayout.getWidth()/2);
		int yCord = (parentLayout.getHeight()/2);
		
		Rotate3dAnimation rotateBoxAnimY = new Rotate3dAnimation(0, -180, xCord, yCord, 0, false, Rotate3dAnimation.Y_AXIS);
		rotateBoxAnimY.setDuration(800);
		flipBoxAnimSet.addAnimation(rotateBoxAnimY);
		
		Rotate3dAnimation rotateBoxAnimZ = new Rotate3dAnimation(0, -90, xCord, yCord, 0, false, Rotate3dAnimation.Z_AXIS);
		rotateBoxAnimZ.setDuration(800);
		flipBoxAnimSet.addAnimation(rotateBoxAnimZ);
		
		RotateAnimation ra2 = new RotateAnimation(0, -rotationAngle,rtype,0.5f , rtype,0.5f );
		ra2.setDuration(800);
		flipBoxAnimSet.addAnimation(ra2);

		activity.findViewById(R.id.gift_box_layout).startAnimation(flipBoxAnimSet);
		
		animHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				giftBoxUnpacking(activity, snowFallView);
			}
		}, 1066);
		
	}

	
	private static void giftBoxUnpacking(final HomeActivity activity, final SnowFallView snowFallView)
	{
		activity.findViewById(R.id.box_flip_side_ribbon).clearAnimation();
		activity.findViewById(R.id.gift_box_layout).clearAnimation();
		activity.findViewById(R.id.fs_middle_dot).setVisibility(View.VISIBLE);
		
		int animDuration = 650;
		int animDuration2 = 400;
		ScaleAnimation middleDotAnim = getScaleAnimOnPivot(0.3f,0.5f,activity, 400, 0.5f,0.5f);
		activity.findViewById(R.id.fs_middle_dot).startAnimation(middleDotAnim);

		ScaleAnimation leftKnotAnim = getScaleAnimOnPivot(0,0,activity, 333, 1, 1);
		activity.findViewById(R.id.fs_left_knot).startAnimation(leftKnotAnim);
		
		TranslateAnimation rightKnot2Anim = new TranslateAnimation(0, 33*Utils.densityMultiplier, 0, 33*Utils.densityMultiplier);
		rightKnot2Anim.setFillAfter(true);
		rightKnot2Anim.setDuration(333);
		activity.findViewById(R.id.fs_right_knot2).startAnimation(rightKnot2Anim);
		
		ScaleAnimation rightKnotAnim = getScaleAnimOnPivot(0,0,activity, 400, 0, 1);
		//rightKnotAnim.setInterpolator(new AccelerateInterpolator(2.5f));
		//rightKnotAnim.setStartOffset(animDuration - animDuration2);
		activity.findViewById(R.id.fs_right_knot).startAnimation(rightKnotAnim);
		
		TranslateAnimation leftKnot2Anim = new TranslateAnimation(0, -33*Utils.densityMultiplier, 0, 33*Utils.densityMultiplier);
		//leftKnot2Anim.setInterpolator(new AccelerateInterpolator(2.5f));
		leftKnot2Anim.setFillAfter(true);
		//leftKnot2Anim.setStartOffset(animDuration - animDuration2);
		leftKnot2Anim.setDuration(400);
		activity.findViewById(R.id.fs_left_knot2).startAnimation(leftKnot2Anim);
		
		int lineDisapearStartOff = 66;
		AlphaAnimation lineDisapearAlphaAnim = new AlphaAnimation(1,0);
		lineDisapearAlphaAnim.setFillAfter(true);
		lineDisapearAlphaAnim.setDuration(167);
		lineDisapearAlphaAnim.setStartOffset(lineDisapearStartOff);
		activity.findViewById(R.id.box_front_bottom_line).startAnimation(lineDisapearAlphaAnim);
		activity.findViewById(R.id.box_front_top_line).startAnimation(lineDisapearAlphaAnim);
		activity.findViewById(R.id.box_front_left_line).startAnimation(lineDisapearAlphaAnim);
		activity.findViewById(R.id.box_front_right_line).startAnimation(lineDisapearAlphaAnim);
		
		
		(new Handler()).postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				activity.findViewById(R.id.fs_left_knot2).clearAnimation();
				activity.findViewById(R.id.fs_left_knot22).clearAnimation();
				activity.findViewById(R.id.fs_right_knot2).clearAnimation();
				activity.findViewById(R.id.fs_right_knot22).clearAnimation();
				
				activity.findViewById(R.id.fs_left_knot2).setVisibility(View.GONE);
				activity.findViewById(R.id.fs_left_knot22).setVisibility(View.GONE);
				activity.findViewById(R.id.fs_right_knot2).setVisibility(View.GONE);
				activity.findViewById(R.id.fs_right_knot22).setVisibility(View.GONE);
				activity.findViewById(R.id.box_flip_side_moving_ribbons).setVisibility(View.VISIBLE);
				ribbonMovingAnim(activity, snowFallView);
				activity.findViewById(R.id.fs_middle_dot).clearAnimation();
				activity.findViewById(R.id.fs_middle_dot).setVisibility(View.GONE);
			}
		}, 400);
		
		
		
	}
	

	public static void ribbonMovingAnim(final HomeActivity activity, final SnowFallView snowFallView){
		int animDuration = 1500;
		
		int startOff = 0;
		int duration = 500;
		int rType = Animation.RELATIVE_TO_SELF;
		ScaleAnimation topBarToRibbonAnim = new ScaleAnimation(1, 0, 1, 1, rType, 0.5f, rType, 0.5f); 
		topBarToRibbonAnim.setFillAfter(true);
		topBarToRibbonAnim.setDuration(33);
		//topBarToRibbonAnim.setStartOffset(380);
		activity.findViewById(R.id.box_front_top_bar).startAnimation(topBarToRibbonAnim);
		
		ScaleAnimation bottomBarToRibbonAnim = new ScaleAnimation(1, 1, 1, 0, rType, 0.5f, rType, 0.5f); 
		bottomBarToRibbonAnim.setFillAfter(true);
		bottomBarToRibbonAnim.setDuration(33);
		//bottomBarToRibbonAnim.setStartOffset(380);
		activity.findViewById(R.id.box_front_bottom_bar).startAnimation(bottomBarToRibbonAnim);
		
		AnimationSet translateRibbon3AnimSet = new AnimationSet(true);
		translateRibbon3AnimSet.setFillAfter(true);
		
		TranslateAnimation ta1 = new TranslateAnimation(0,0,0,140*Utils.densityMultiplier); 
		ta1.setDuration(467);
		ta1.setStartOffset(startOff);
		translateRibbon3AnimSet.addAnimation(ta1);
		
		AlphaAnimation aa1 = new AlphaAnimation(1,0); 
		aa1.setDuration(267);
		aa1.setStartOffset(200);
		translateRibbon3AnimSet.addAnimation(aa1);
		
		activity.findViewById(R.id.fs_moving_ribbon3).startAnimation(translateRibbon3AnimSet);
		
		AnimationSet translateRibbon2AnimSet = new AnimationSet(true);
		translateRibbon2AnimSet.setFillAfter(true);
		
		TranslateAnimation ta2 = new TranslateAnimation(0,0,0,140*Utils.densityMultiplier); 
		ta2.setDuration(500);
		ta2.setStartOffset(startOff);
		translateRibbon2AnimSet.addAnimation(ta2);
		
		AlphaAnimation aa2 = new AlphaAnimation(1,0); 
		aa2.setDuration(200);
		aa2.setStartOffset(300);
		translateRibbon2AnimSet.addAnimation(aa2);
		
		activity.findViewById(R.id.fs_moving_ribbon2).startAnimation(translateRibbon2AnimSet);
		
		AnimationSet translateRibbon1AnimSet = new AnimationSet(true);
		translateRibbon1AnimSet.setFillAfter(true);
		
		TranslateAnimation ta3 = new TranslateAnimation(0,0,0,-280*Utils.densityMultiplier); 
		ta3.setDuration(367);
		ta3.setStartOffset(startOff);
		translateRibbon1AnimSet.addAnimation(ta3);
		
		AlphaAnimation aa3 = new AlphaAnimation(1,0); 
		aa3.setDuration(167);
		aa3.setStartOffset(200);
		translateRibbon1AnimSet.addAnimation(aa3);
		
		activity.findViewById(R.id.fs_moving_ribbon1).startAnimation(translateRibbon1AnimSet);
		
		(new Handler()).postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				giftBoxUnfolding(activity, snowFallView);
			}
		}, 400);
	}
	
	private static void giftBoxUnfolding(final HomeActivity activity, SnowFallView snowFallView)
	{
		activity.findViewById(R.id.gift_box_bottom).setVisibility(View.VISIBLE);
		int animDuration = 467;
		
		AlphaAnimation giftBoxPinkBottomAnim = new AlphaAnimation(1,0);
		giftBoxPinkBottomAnim.setFillAfter(true);
		giftBoxPinkBottomAnim.setDuration(animDuration+100);
		activity.findViewById(R.id.gift_box_bottom).startAnimation(giftBoxPinkBottomAnim);
		
		AnimationSet boxLeftFoldAnimSet = new AnimationSet(true);
		boxLeftFoldAnimSet.setFillAfter(true);
		ScaleAnimation ra1 = new ScaleAnimation(1, 0, 1, 1, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
		ra1.setDuration(animDuration);
		boxLeftFoldAnimSet.addAnimation(ra1);
		
		AlphaAnimation aa5 = new AlphaAnimation(1,0);
		aa5.setDuration(animDuration);
		boxLeftFoldAnimSet.addAnimation(aa5);
		activity.findViewById(R.id.box_left_fold).startAnimation(boxLeftFoldAnimSet);
		
		AnimationSet boxRightFoldAnimSet = new AnimationSet(true);
		boxRightFoldAnimSet.setFillAfter(true);
		ScaleAnimation ra2 = new ScaleAnimation(1, 0, 1, 1, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 1f);
		ra2.setDuration(animDuration);
		boxRightFoldAnimSet.addAnimation(ra2);
		
		AlphaAnimation aa2 = new AlphaAnimation(1,0);
		aa2.setDuration(animDuration);
		boxRightFoldAnimSet.addAnimation(aa2);
		activity.findViewById(R.id.box_right_fold).startAnimation(boxRightFoldAnimSet);
		
		
		AnimationSet boxTopFoldAnimSet = new AnimationSet(true);
		boxTopFoldAnimSet.setFillAfter(true);
		ScaleAnimation ra3 = new ScaleAnimation(1, 1, 1, 0, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
		
		ra3.setDuration(animDuration);
		boxTopFoldAnimSet.addAnimation(ra3);
		
		AlphaAnimation aa3 = new AlphaAnimation(1,0);
		aa3.setDuration(animDuration);
		boxTopFoldAnimSet.addAnimation(aa2);
		activity.findViewById(R.id.box_top_fold).startAnimation(boxTopFoldAnimSet);
		
		
		AnimationSet boxBottomFoldAnimSet = new AnimationSet(true);
		boxBottomFoldAnimSet.setFillAfter(true);
		ScaleAnimation ra4 = new ScaleAnimation(1, 1, 1, 0, Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 1f);
		
		ra4.setDuration(animDuration);
		boxBottomFoldAnimSet.addAnimation(ra4);
		
		AlphaAnimation aa4 = new AlphaAnimation(1,0);
		aa4.setDuration(animDuration);
		boxBottomFoldAnimSet.addAnimation(aa4);
		activity.findViewById(R.id.box_bottom_fold).startAnimation(boxBottomFoldAnimSet);
		
		//chat theme popup comes out
		startChatThemesPopupAnimation(activity, animDuration, snowFallView);
	}
	
	
	public static void startChatThemesPopupAnimation(final HomeActivity activity, int animDuration, final SnowFallView snowFallView){
		Handler animHandler = new Handler();
		activity.findViewById(R.id.chat_theme_popup).setVisibility(View.VISIBLE);
		AccelerateInterpolator accInterpolator = new AccelerateInterpolator(1.5f);
		
		AnimationSet chatThemePopupAnimSet = new AnimationSet(true);
		chatThemePopupAnimSet.setInterpolator(accInterpolator);
		chatThemePopupAnimSet.setFillAfter(true);
		ScaleAnimation sa1 = new ScaleAnimation(0.4f, 1f, 0.4f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		
		sa1.setDuration(animDuration);
		chatThemePopupAnimSet.addAnimation(sa1);
		
		ScaleAnimation sa2 = new ScaleAnimation(1, 1.1f, 1f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		sa2.setStartOffset(animDuration);
		sa2.setDuration(100);
		chatThemePopupAnimSet.addAnimation(sa2);
		ScaleAnimation sa3 = new ScaleAnimation(1, 0.91f, 1f, 0.91f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		sa3.setStartOffset(animDuration+100);
		sa3.setDuration(200);
		chatThemePopupAnimSet.addAnimation(sa3);
		
		activity.findViewById(R.id.chat_theme_popup).startAnimation(chatThemePopupAnimSet);
		
		AnimationSet popupFadeAnimSet = new AnimationSet(true);
		popupFadeAnimSet.setFillAfter(true);
		popupFadeAnimSet.setInterpolator(accInterpolator);
		
		AlphaAnimation aa2 = new AlphaAnimation(1f,0);
		aa2.setFillAfter(true);
		aa2.setDuration(animDuration);
		
		popupFadeAnimSet.addAnimation(aa2);
		activity.findViewById(R.id.chat_theme_popup_white_fade).startAnimation(popupFadeAnimSet);
		
		AlphaAnimation aa5 = new AlphaAnimation(1,0);
		aa5.setFillAfter(true);
		aa5.setDuration(300);
		aa5.setStartOffset(animDuration+200);
		activity.findViewById(R.id.chat_theme_popup_glow).startAnimation(aa5);

		boolean newUser = activity.getIntent().getBooleanExtra(HikeConstants.Extras.NEW_USER, false);
		ContactInfo contactInfo = HikeUserDatabase.getInstance().getChatThemeFTUEContact(activity, newUser);
		if(contactInfo == null) {
			TextView textView = (TextView) activity.findViewById(R.id.give_it_a_spin_text);
			textView.setText(R.string.ok);
		} else {
			activity.setChatThemeFTUEContact(contactInfo);
		}
		
		animHandler.postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				AlphaAnimation aa7 = new AlphaAnimation(1,0);
				aa7.setDuration(400);
				if(snowFallView != null){
					snowFallView.startAnimation(aa7);
				}
			}
		}, animDuration);
		
		animHandler.postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				if(snowFallView != null){
					snowFallView.clearAnimation();
					snowFallView.setVisibility(View.GONE);
				}
			}
		}, animDuration+400);
		
		animHandler.postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				activity.findViewById(R.id.give_it_a_spin_text).setVisibility(View.VISIBLE);
				AlphaAnimation aa6 = new AlphaAnimation(0,1);
				aa6.setFillAfter(true);
				aa6.setDuration(500);
				aa6.setStartOffset(1000);
				activity.findViewById(R.id.give_it_a_spin_text).startAnimation(aa6);
				if(snowFallView != null){
					snowFallView.clearAnimation();
					snowFallView.setVisibility(View.GONE);
				}
			}
		}, animDuration);
		
	}


	public static void onChatBgGiveItASpinClick(final HomeActivity activity, View v, SnowFallView snowFallView){
		if(snowFallView!= null){
			snowFallView.clearAnimation();
			snowFallView.setVisibility(View.GONE);
		}
		activity.findViewById(R.id.chat_theme_popup).clearAnimation();
		activity.findViewById(R.id.chat_theme_popup).setVisibility(View.GONE);
		activity.findViewById(R.id.chat_bg_ftue_fade).clearAnimation();
		activity.findViewById(R.id.chat_bg_ftue_fade).setVisibility(View.GONE);
		ContactInfo contactInfo = activity.getChatThemeFTUEContact();
		if(contactInfo != null){
			Intent intent = Utils.createIntentFromContactInfo(contactInfo, false);
			intent.putExtra(HikeConstants.Extras.FROM_CHAT_THEME_FTUE, true);
			intent.setClass(activity, ChatThread.class);
			activity.startActivity(intent);
		} else{
			//Do nothing
		}
		return;
	}

	public static ScaleAnimation getScaleAnimOnPivot(float toX, float toY, final Activity activity, int animDuration, float pivotX, float pivotY){
		int rType = Animation.RELATIVE_TO_SELF;
		ScaleAnimation animation = new ScaleAnimation(1, toX, 1, toY, rType , pivotX, rType, pivotY);
		animation.setFillAfter(true);
		animation.setDuration(animDuration);
		return animation;
	}
}
