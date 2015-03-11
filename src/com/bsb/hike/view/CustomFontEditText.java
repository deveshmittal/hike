package com.bsb.hike.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class CustomFontEditText extends EditText
{
	private int style;

	private BackKeyListener listener;

	private void setFont(AttributeSet attrs)
	{
		setTypeface(getTypeface(), style);
	}

	public CustomFontEditText(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setFont(attrs);
	}

	public CustomFontEditText(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setFont(attrs);
	}

	public CustomFontEditText(Context context)
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

	public void setBackKeyListener(BackKeyListener listener)
	{
		this.listener = listener;
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event)
	{
		boolean result = false;
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			result = super.onKeyPreIme(keyCode, event);

			// User has pressed Back key Hide your view as you do it in your activity
			if (listener != null)
			{
				listener.onBackKeyPressedET(this);
			}
		}
		return result;
	}

	public static interface BackKeyListener
	{
		public void onBackKeyPressedET(CustomFontEditText editText);
	}
}
