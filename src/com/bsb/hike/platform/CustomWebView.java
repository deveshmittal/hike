package com.bsb.hike.platform;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

/**
 * Created by shobhitmandloi on 27/01/15.
 */
public class CustomWebView extends WebView
{
	//Custom WebView to stop background calls when moves out of view.
	public CustomWebView(Context context)
	{
		super(context);
	}

	public CustomWebView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	private boolean is_gone = false;

	// if webView is not visible, call onPause of WebView, else call onResume.
	public void onWindowVisibilityChanged(int visibility)
	{
		super.onWindowVisibilityChanged(visibility);

		if (visibility == View.GONE)
		{

			try
			{
				WebView.class.getMethod("onPause").invoke(this);//stop flash
				this.pauseTimers();
				this.is_gone = true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

		}
		else if (visibility == View.VISIBLE)
		{
			try
			{
				WebView.class.getMethod("onResume").invoke(this);//resume flash
				this.resumeTimers();
				this.is_gone = false;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

		}
	}

	//this will be trigger when back key pressed, not when home key pressed
	public void onDetachedFromWindow()
	{
		if (this.is_gone)
		{
			try
			{
				this.destroy();
			}
			catch (Exception e)
			{
			}
		}
	}
}
