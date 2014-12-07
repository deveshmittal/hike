package com.bsb.hike.modules.animationModule;

import com.bsb.hike.utils.Utils;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

public class HikeAnimationFactory
{	
	public static Animation getPulsatingDotAnimation(int initialOffset)
	{
		AnimationSet animSet = new AnimationSet(true);
		float a = 1.75f;
		float pivotX = 0.5f;
		float pivotY = 0.5f;
		
		Animation anim0 = new ScaleAnimation(1, a, 1, a, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim0.setStartOffset(initialOffset);
		anim0.setDuration(2500);
		anim0.setRepeatCount(Animation.INFINITE);
		animSet.addAnimation(anim0);

		Animation fade = new AlphaAnimation(1, 0);
		fade.setInterpolator(new AccelerateInterpolator(2f));
		fade.setStartOffset(1500);
		fade.setDuration(1000);
		fade.setRepeatCount(Animation.INFINITE);
		animSet.addAnimation(fade);
		return animSet;
	}

	public static Animation getScaleOutBackgroundAnimation()
	{
		Animation anim = new ScaleAnimation(1, Utils.densityMultiplier * 65, 1, Utils.densityMultiplier * 65, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		anim.setInterpolator(new AccelerateInterpolator());
		anim.setStartOffset(500);
		anim.setDuration(1200);
		anim.setFillAfter(true);
		return anim;
	}

	public static AnimationSet getStickerShopIconAnimation()
	{
		final AnimationSet animSet = new AnimationSet(true);
		float a = 0.8f;
		float b = 1.05f;
		float c = 0.9f;
		float d = 1.01f;
		float e = 1f;
		int initialOffset = 2000;
		float pivotX = 0.5f;
		float pivotY = 0.5f;
		Animation anim0 = new ScaleAnimation(1, a, 1, a, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim0.setInterpolator(new AccelerateInterpolator(2f));
		anim0.setStartOffset(initialOffset);
		anim0.setDuration(100);
		animSet.addAnimation(anim0);

		Animation anim1 = new ScaleAnimation(1, b / a, 1, b / a, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim1.setInterpolator(new AccelerateInterpolator(2f));
		anim1.setDuration(100);
		anim1.setStartOffset(initialOffset + anim0.getDuration());
		animSet.addAnimation(anim1);

		Animation anim2 = new ScaleAnimation(1f, c / b, 1f, c / b, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim2.setInterpolator(new AccelerateInterpolator(-1f));
		anim2.setDuration(150);
		anim2.setStartOffset(initialOffset + anim0.getDuration() + anim1.getDuration());
		animSet.addAnimation(anim2);

		Animation anim3 = new ScaleAnimation(1f, d / c, 1f, d / c, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim2.setInterpolator(new AccelerateInterpolator(1f));
		anim3.setDuration(150);
		anim3.setStartOffset(initialOffset + anim0.getDuration() + anim1.getDuration() + anim2.getDuration());
		animSet.addAnimation(anim3);

		Animation anim4 = new ScaleAnimation(1f, e / d, 1f, e / d, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
		anim4.setInterpolator(new AccelerateInterpolator(1f));
		anim4.setDuration(150);
		anim4.setStartOffset(initialOffset + anim0.getDuration() + anim1.getDuration() + anim2.getDuration() + anim3.getDuration());
		animSet.addAnimation(anim4);

		animSet.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				animation.reset();
				animation.start();
			}
		});
		return animSet;
	}
}
