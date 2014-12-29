package com.bsb.hike.view;

import com.bsb.hike.HikeConstants;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

/**
 * 
 * This is a small circle shown
 * on Notification Screen painted with 
 * color chosen for Led Notification
 *
 */
public class LedColorView extends View
{
	private Paint circlePaint;

	private int circleCol;

	public LedColorView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		
		// paint object for drawing in onDraw
		circlePaint = new Paint();
		circleCol = HikeConstants.LED_DEFAULT_BLUE_COLOR;
	}

	public LedColorView(Context context)
	{
		super(context);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		// get half of the width and height as we are working with a circle
		int viewWidthHalf = this.getMeasuredWidth() / 2;
		int viewHeightHalf = this.getMeasuredHeight() / 2;
		
		// get the radius as half of the width or height, whichever is smaller
		// subtract ten so that it has some space around it
		int radius = 0;
		if (viewWidthHalf > viewHeightHalf)
			radius = viewHeightHalf - 10;
		else
			radius = viewWidthHalf - 10;
		circlePaint.setStyle(Style.FILL);
		circlePaint.setAntiAlias(true);
		
		// set the paint color using the circle color specified
		circlePaint.setColor(circleCol);
		canvas.drawCircle(viewWidthHalf, viewHeightHalf, radius, circlePaint);
	}

	public void setCircleColor(int newColor)
	{
		// update the instance variable
		circleCol = newColor;
		
		// redraw the view
		invalidate();
		requestLayout();
	}
}