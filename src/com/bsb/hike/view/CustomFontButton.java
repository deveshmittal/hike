package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

public class CustomFontButton extends Button
{
	private int style;

	private void setFont(AttributeSet attrs)
	{
		setTypeface(getTypeface(), style);
	}

	public CustomFontButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setFont(attrs);
	}

	public CustomFontButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setFont(attrs);
	}

	public CustomFontButton(Context context)
	{
		super(context);
	}

	@Override
	public void setTypeface(Typeface tf, int style)
	{
		if (!isInEditMode())
		{
			if (style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC)
			{
				style = Typeface.NORMAL;
			}
			super.setTypeface(tf, style);
		}
	}
}
