package com.bsb.hike.utils;

import android.view.animation.Interpolator;

public class CustomInterpolator implements Interpolator
{

	@Override
	public float getInterpolation(float progress)
	{
		return 1 - (progress * progress * progress * progress);
	}

}
