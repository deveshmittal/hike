package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class CustomLinearLayout extends LinearLayout
{

	public CustomLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CustomLinearLayout(Context context)
	{
		super(context);
	}

	private OnSoftKeyboardListener onSoftKeyboardListener;

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
	{
		if (onSoftKeyboardListener != null)
		{
			final int newSpec = MeasureSpec.getSize(heightMeasureSpec);
			final int oldSpec = getMeasuredHeight();
			if ((int) (0.66 * oldSpec) > newSpec)
			{
				onSoftKeyboardListener.onShown();
			}
			else
			{
				onSoftKeyboardListener.onHidden();
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}

	public final void setOnSoftKeyboardListener(final OnSoftKeyboardListener listener)
	{
		this.onSoftKeyboardListener = listener;
	}

	public interface OnSoftKeyboardListener
	{
		public void onShown();

		public void onHidden();
	}

}
