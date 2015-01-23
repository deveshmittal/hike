package com.bsb.hike.platform;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Toast;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add
 * it as the JavaScript interface of the Card WebView.
 */

public class PlatformJavaScriptBridge
{

	private static final String tag = "platformbridge";

	Context mContext;

	WebView mWebView;

	ConvMessage message;

	BaseAdapter adapter;

	public PlatformJavaScriptBridge(Context c, WebView webView, ConvMessage convMessage, BaseAdapter adapter)
	{
		this.mContext = c;
		this.mWebView = webView;
		this.message = convMessage;
		this.adapter = adapter;
	}

	@JavascriptInterface
	public void animationComplete(String html, String id)
	{
		Logger.i(tag, "on animation complete " + mWebView.getTag());
	}

	@JavascriptInterface
	public void showToast(String toast)
	{
		Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();

	}

	@JavascriptInterface
	public void receiveInnerHTML(final String html, String id)
	{
		mWebView.post(new Runnable()
		{
			@Override
			public void run()
			{
				mWebView.loadDataWithBaseURL("", "twtw", "text/html; charset=UTF-8", null, "");
				mWebView.setVerticalScrollBarEnabled(false);
				mWebView.setHorizontalScrollBarEnabled(false);
				mWebView.addJavascriptInterface(this, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
				mWebView.getSettings().setJavaScriptEnabled(true);
			}
		});

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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
		{
			mWebView.setWebContentsDebuggingEnabled(true);
		}
	}

	@JavascriptInterface
	public void allowUniversalAccess()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
			mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
		}
	}

	@JavascriptInterface
	public void setAlarm(String json, String messageId, String timeInMills)
	{
		try
		{
			Logger.i(tag, "set alarm called " + json + " , mId " + messageId + " , time " + timeInMills);
			PlatformAlarmManager.setAlarm(mContext, new JSONObject(json), Integer.parseInt(messageId), Long.valueOf(timeInMills));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@JavascriptInterface
	public void updateHelperData(String messageId, String json)
	{
		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + messageId);
			String updatedJSON = HikeConversationsDatabase.getInstance().updateHelperData(Integer.parseInt(messageId), json);
			if (updatedJSON != null)
			{
				message.platformWebMessageMetadata = new PlatformWebMessageMetadata(updatedJSON);
			}

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@JavascriptInterface
	public void replaceMetadata(String metadata)
	{
		try
		{
			message.platformWebMessageMetadata = new PlatformWebMessageMetadata(new JSONObject(metadata));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@JavascriptInterface
	public void deleteAlarm(String id)
	{
		HikeConversationsDatabase.getInstance().deleteAppAlarm(Integer.parseInt(id));
	}

	@JavascriptInterface
	public void logFromJS(String tag, String data)
	{
		Logger.v(tag, data);
	}

	@JavascriptInterface
	public void updateMetadata(String messageId, String json)
	{

		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + messageId);
			String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata(Integer.valueOf(messageId), json);
			if (updatedJSON != null)
			{
				message.platformWebMessageMetadata = new PlatformWebMessageMetadata(updatedJSON);

				mWebView.post(new Runnable()
				{

					@Override
					public void run()
					{
						adapter.notifyDataSetChanged();

					}
				});
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@JavascriptInterface
	public void forwardToChat(String messageId, String json)
	{
		try
		{
			Logger.i(tag, "forward to chat called " + json + " , message id=" + messageId);

			if (!TextUtils.isEmpty(json))
			{
				String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata(Integer.valueOf(messageId), json);
				if (!TextUtils.isEmpty(updatedJSON))
				{
					message.platformWebMessageMetadata = new PlatformWebMessageMetadata(updatedJSON);
				}
			}

			message.setMessageType(HikeConstants.MESSAGE_TYPE.FORWARD_WEB_CONTENT);
			final Intent intent = IntentManager.getForwardIntentForConvMessage(mContext, message, PlatformContent.getForwardCardData(message.platformWebMessageMetadata.JSONtoString()));
			mWebView.post(new Runnable()
			{
				@Override
				public void run()
				{
					mContext.startActivity(intent);
				}
			}) ;

		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@JavascriptInterface
	public void openFullPage(String title, String url)
	{
		Logger.i(tag, "open full page called with title " + title + " , and url = " + url);
		final Intent intent = IntentManager.getWebViewActivityIntent(mContext, url, title);
		mWebView.post(new Runnable()
		{
			@Override
			public void run()
			{
				mContext.startActivity(intent);
			}
		}) ;

	}



}

