package com.bsb.hike.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ListView;

public class HikeListView extends ListView
{

	private boolean mScrollable = true;

	public HikeListView(Context context)
	{
		super(context);
	}

	public HikeListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public HikeListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public void setScrollable(boolean isScrollable)
	{
		mScrollable = isScrollable;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev)
	{
		if (!mScrollable)
		{
			if ((ev.getAction() == MotionEvent.ACTION_MOVE) && (Build.VERSION.SDK_INT > 11))
			{
				ev.setAction(MotionEvent.ACTION_CANCEL);
				return true;
			}
		}

		return super.onInterceptTouchEvent(ev);
	}

	
}
