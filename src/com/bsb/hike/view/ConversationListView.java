package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import com.bsb.hike.utils.Logger;

/*
 * Added to add a workaround for this bug:
 * http://stackoverflow.com/questions/12140665/android-monkey-causes-adapter-notification-exception-in-android-widget-headervie
 */
public class ConversationListView extends ListView
{

	public ConversationListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public ConversationListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public ConversationListView(Context context)
	{
		super(context);
	}

	@Override
	protected void layoutChildren()
	{
		try
		{
			super.layoutChildren();
		}
		catch (IllegalStateException e)
		{
			Logger.w(getClass().getSimpleName(), "Conversation footer exception");
		}
	}
}
