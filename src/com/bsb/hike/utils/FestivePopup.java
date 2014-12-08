package com.bsb.hike.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.snowfall.SnowFallView;
import com.bsb.hike.ui.HomeActivity;

public class FestivePopup
{	
	public static SnowFallView snowFallView; 
	
	public static final String XMAS_POPUP = "xmas";

	public static final String NEW_YEAR_POPUP = "newyear";

	private static String stickerId;

	private static String categoryId;

	private static String popupType;

	public static void setPopupType(String type)
	{
		popupType = type;
	}

	public  static void setStickerIdAndCatId()
	{
		if(popupType.equals(NEW_YEAR_POPUP))
		{
			stickerId = "008_hikinnewyear";
			categoryId = "festive";
		}
		else
		{
			stickerId = "002_merryxmas.png";
			categoryId = "festive";
		}
	}

	public static SnowFallView startAndSetSnowFallView(final Activity activity)
	{
		if (activity == null)
		{
			return null;
		}
		activity.findViewById(R.id.chat_bg_ftue_fade).setVisibility(View.VISIBLE);
		Handler animHandler = new Handler();
		AlphaAnimation alphaAnim = new AlphaAnimation(0.2f, 1f);
		alphaAnim.setFillAfter(true);

		if (((int) Utils.densityMultiplier * 100) >= 100)
		{
			alphaAnim.setDuration(1400);
			activity.findViewById(R.id.chat_bg_ftue_fade).startAnimation(alphaAnim); // dim
			RelativeLayout layout = (RelativeLayout) activity.findViewById(R.id.parent_layout);
			snowFallView = new SnowFallView(activity);
			snowFallView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			snowFallView.setVisibility(View.GONE);
			layout.addView(snowFallView, 4);

			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					setupFestivePopup(activity);
				}
			}, 2800);

			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					activity.findViewById(R.id.snowman_footer).setVisibility(View.VISIBLE);
					addMoveUpAnimation(activity.findViewById(R.id.snowman_footer));

					snowFallView.setVisibility(View.VISIBLE);
					AlphaAnimation alphaAnim = new AlphaAnimation(0.1f, 1f);
					AccelerateInterpolator accInterpolator = new AccelerateInterpolator(1f);
					alphaAnim.setInterpolator(accInterpolator);
					alphaAnim.setDuration(600);
					alphaAnim.setFillAfter(true);
					snowFallView.startAnimation(alphaAnim);
				}
			}, 1200);
			return snowFallView;
		}
		else
		{
			alphaAnim.setDuration(1800);
			activity.findViewById(R.id.chat_bg_ftue_fade).startAnimation(alphaAnim); // dim
			animHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					activity.findViewById(R.id.snowman_footer).setVisibility(View.VISIBLE);
					addMoveUpAnimation(activity.findViewById(R.id.snowman_footer));

					setupFestivePopup(activity);
				}
			}, 2000);
			return null;
		}

	}

	public static void setupFestivePopup(final Activity activity)
	{
		setStickerIdAndCatId();

		activity.findViewById(R.id.festive_popup_parent).setVisibility(View.VISIBLE);

		addFallAnimation(activity.findViewById(R.id.festive_popup_parent));

		activity.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopFestiveAnimAndPopup(activity);
				Intent intent = IntentManager.getForwardStickerIntent(activity, stickerId, categoryId, false);
				intent.putExtra(HikeConstants.Extras.SELECT_ALL_INITIALLY, true);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(intent);
			}
		});
		activity.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopFestiveAnimAndPopup(activity);
			}
		});
	}
	
	private static void addMoveUpAnimation(View view)
	{
		Animation anim = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0, Animation.RELATIVE_TO_PARENT, 0,
		                                        Animation.RELATIVE_TO_PARENT, 1, Animation.RELATIVE_TO_PARENT, 0);
		anim.setDuration(1600);
		view.startAnimation(anim);
	}

	private static void addFallAnimation(View view)
	{
		AnimationSet boxFallAnimSet = new AnimationSet(true);
		boxFallAnimSet.setInterpolator(new AccelerateInterpolator(1.5f));

		AlphaAnimation aa1 = new AlphaAnimation(0f, 1f);
		aa1.setDuration(350);
		boxFallAnimSet.addAnimation(aa1);

		Animation boxFallAnim = new TranslateAnimation(0, 0, -400 * Utils.densityMultiplier, -13 * Utils.densityMultiplier);
		boxFallAnim.setDuration(600);
		boxFallAnimSet.addAnimation(boxFallAnim);

		view.startAnimation(boxFallAnimSet);
	}
	
	public static void stopFestiveAnimAndPopup(final Activity activity)
	{
		// Removing overlay
		activity.findViewById(R.id.chat_bg_ftue_fade).clearAnimation();	
		activity.findViewById(R.id.chat_bg_ftue_fade).setVisibility(View.GONE);

		// Removing popup
		activity.findViewById(R.id.festive_view).setVisibility(View.GONE);

		// Removing snowfall anim
		if(snowFallView!=null)
		{
			snowFallView.clearAnimation();
			snowFallView.setVisibility(View.GONE);
		}

		((HomeActivity)activity).showActionBarAfterFestivePopup();

		HikeSharedPreferenceUtil.getInstance(activity).removeData(HikeConstants.SHOW_FESTIVE_POPUP);
	}
}
