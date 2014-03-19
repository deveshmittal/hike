package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

public class MyViewFlipper extends ViewFlipper
{
	public MyViewFlipper(Context context)
	{
		super(context);
	}

	public MyViewFlipper(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		try
		{
			super.onDetachedFromWindow();
		}
		catch (IllegalArgumentException e)
		{
			stopFlipping();
			e.printStackTrace();
		}
	}
}