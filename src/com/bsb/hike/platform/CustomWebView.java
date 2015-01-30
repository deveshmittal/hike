package com.bsb.hike.platform;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import android.annotation.SuppressLint;
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
	@Override
	public void onWindowVisibilityChanged(int visibility)
	{
		super.onWindowVisibilityChanged(visibility);
		if (visibility == View.GONE)
		{
			onWebViewGone();
		}
		else if (visibility == View.VISIBLE)
		{
			onWebViewVisible();
		}
	}
	
	@SuppressLint("NewApi")
	public void onWebViewGone()
	{
		Logger.i("customWebView", "on webview gone "+this.hashCode());
		try
		{
			if(Utils.isHoneycombOrHigher())
			{
				this.onPause();
			}
			// we giving callback to javascript to stop heavy processing
			this.loadUrl("javascript:onPause()");
			this.is_gone = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@SuppressLint("NewApi")
	public void onWebViewVisible()
	{
		Logger.i("customWebView", "on webview visible "+this.hashCode());
		try
		{
			if(Utils.isHoneycombOrHigher())
			{
				this.onResume();
			}
			this.loadUrl("javascript:onResume()");
			this.is_gone = false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	//this will be trigger when back key pressed, not when home key pressed
	@Override
	public void onDetachedFromWindow()
	{
		Logger.i("customwebview", "on detach called");
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
