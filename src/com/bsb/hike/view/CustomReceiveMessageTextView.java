package com.bsb.hike.view;

import com.bsb.hike.utils.Utils;

import android.text.Layout;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class CustomReceiveMessageTextView extends CustomFontTextView
{
	String TAG = "CustomReceiveMessageTextView";

	public int maxWidth;

	public int lastLineWidth = 0;

	public int linesMaxWidth = 0;

	public int lines = 0;

	public CustomReceiveMessageTextView(Context context)
	{
		super(context);
	}

	public CustomReceiveMessageTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CustomReceiveMessageTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		try
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			int measuredWidth = getMeasuredWidth();
			Layout layout = getLayout();
			lines = layout.getLineCount();
			float lastLeft = layout.getLineLeft(lines - 1);
			float lastLine = layout.getLineWidth(lines - 1);

			linesMaxWidth = lastLineWidth = (int) Math.ceil(lastLine);

			//Log.d(TAG, "lastLine: " + lastLine + ", density multiplier: " + Utils.densityMultiplier);

			for (int n = 0; n < lines; ++n)
			{
				float lineWidth;
				float lineLeft;
				try
				{
					lineWidth = layout.getLineWidth(n);
					lineLeft = layout.getLineLeft(n);
					//Log.d(TAG, "LINE NO. " + n + ", Width: " + lineWidth + ", Left: " + lineLeft);
				}
				catch (Exception e)
				{
					return;
				}

				linesMaxWidth = Math.max(linesMaxWidth, (int) Math.ceil(lineWidth));
			}

			if (getContext().getResources().getDisplayMetrics().widthPixels - lastLineWidth > (150 * Utils.densityMultiplier))
			{
				int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
				int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
				//Log.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				parentHeight = (layout.getLineTop(1) - layout.getLineTop(0)) * (lines);
				//Log.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				linesMaxWidth = Math.max(linesMaxWidth, (int) ((50 * Utils.densityMultiplier) + lastLineWidth));
				this.setMeasuredDimension(linesMaxWidth, parentHeight);
			}
			else
			{
				int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
				int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
				//Log.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				parentHeight = (layout.getLineTop(1) - layout.getLineTop(0)) * (lines + 1);
				//Log.d(TAG, "Width: " + parentWidth + ", Height: " + parentHeight);
				this.setMeasuredDimension(linesMaxWidth, parentHeight);
			}
		}
		catch (Exception e)
		{
			try
			{
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
			catch (Exception e2)
			{
				setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
			}
		}
	}
}
