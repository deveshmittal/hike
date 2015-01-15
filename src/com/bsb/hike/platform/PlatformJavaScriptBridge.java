package com.bsb.hike.platform;

import android.content.Context;
import android.os.Build;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import com.bsb.hike.db.HikeContentDatabase;
import com.bsb.hike.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add
 * it as the JavaScript interface of the Card WebView.
 *
 */

public class PlatformJavaScriptBridge
{

	private static final String tag = "platformbridge";
	Context mContext;
	WebView mWebView;

	public PlatformJavaScriptBridge(Context c, WebView webView) {
		this.mContext = c;
		this.mWebView = webView;
	}


	public void animationComplete(int height, int id, boolean isExpanded){

		mWebView.loadUrl("javascript:getInnerHTML(\"" + id + "\")");


	}

	@JavascriptInterface
	public void showToast(String toast){
		Toast.makeText(mContext, toast , Toast.LENGTH_SHORT).show();

	}
	@JavascriptInterface
	public void receiveInnerHTML(String html, int id) {
		mWebView.loadData(html,"text/html; charset=UTF-8", null);
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.addJavascriptInterface(this, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
		mWebView.getSettings().setJavaScriptEnabled(true);
	}

	@JavascriptInterface
	public void setDebuggableEnabled(boolean setEnabled)
	{

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
		{
			if (setEnabled)
			{

				mWebView.setWebContentsDebuggingEnabled(true);
			}
			else
			{
				mWebView.setWebContentsDebuggingEnabled(false);
			}

		}
	}

	@JavascriptInterface
	public void allowDebugging()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			mWebView.setWebContentsDebuggingEnabled(true);
		}
	}

	@JavascriptInterface
	public void allowUniversalAccess()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
		}
	}

	@JavascriptInterface
	public void setAlarm(String json, long messageId, long timeInMills){
		try
		{
			Logger.i(tag,"set alarm called "+json +" , mId "+ messageId +" , time "+timeInMills);
			PlatformAlarmManager.setAlarm(mContext, new JSONObject(json), messageId, timeInMills);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@JavascriptInterface
	public void deleteAlarm(int id) {
		HikeContentDatabase.getInstance(mContext).deleteAppAlarm(id);
	}

	@JavascriptInterface
	public void logFromJS(String tag, String data){
		Logger.v(tag, data);
	}



}

