package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class RotatableImageView extends ImageView
{
	private int mAngle = START_ANGLE;

	private static final int START_ANGLE = -71;

	private static final int MAX_CREDITS_TO_SHOW = 600;

	private static final int MAX_ANGLE = 142;

	public RotatableImageView(Context context)
	{
		super(context);
	}

	public RotatableImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public RotatableImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	protected void onDraw(Canvas canvas) // if you want to rotate the entire
											// view (along with its background),
											// you should oveerride draw()
											// instead of onDraw()
	{
		canvas.save();
		canvas.rotate(mAngle % 360, getWidth() / 2, getHeight() - 10);
		super.onDraw(canvas);
		canvas.restore();
	}

	public int getAngle()
	{
		return mAngle;
	}

	public void setCredits(int credits)
	{
		mAngle = creditsToAngle(credits);
		Log.d(getClass().getSimpleName(), "ANGLE: " + mAngle + " CREADITS: " + credits);
		invalidate();
	}

	private int creditsToAngle(int credits)
	{
		if (credits >= MAX_CREDITS_TO_SHOW)
		{
			return MAX_ANGLE + START_ANGLE;
		}
		float ratio = (credits * 100) / MAX_CREDITS_TO_SHOW;
		int angle = (int) (ratio * MAX_ANGLE) / 100;
		return angle + START_ANGLE;
	}
}