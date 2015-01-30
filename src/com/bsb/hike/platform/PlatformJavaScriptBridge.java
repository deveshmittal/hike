package com.bsb.hike.platform;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.WebViewCardRenderer.WebViewHolder;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * API bridge that connects the javascript to the Native environment. Make the instance of this class and add it as the JavaScript interface of the Card WebView.
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

	public void vibrate(String msecs)
	{
		Utils.vibrate(mContext, Integer.parseInt(msecs));
	}

	@JavascriptInterface
	public void receiveInnerHTML(final String html, String id)
	{
		mWebView.post(new Runnable()
		{
			@SuppressLint("JavascriptInterface")
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
	public void updateMetadata(String messageId, String json, String notifyScreen)
	{

		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + messageId +" notifyScren is "+notifyScreen);
			String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata(Integer.valueOf(messageId), json);
			if (updatedJSON != null)
			{
				message.platformWebMessageMetadata = new PlatformWebMessageMetadata(updatedJSON); // the new metadata to inflate in webview
				if (notifyScreen != null && Boolean.valueOf(notifyScreen))
				{
					mWebView.post(new Runnable()
					{

						@Override
						public void run()
						{
							Object obj = mWebView.getTag();
							if (obj instanceof WebViewHolder)
							{
								Logger.i(tag, "updated metadata and calling notifydataset of " + adapter.getClass().getName() + " and thread= " + Thread.currentThread().getName());
								WebViewHolder holder = (WebViewHolder) obj;
								holder.id = -1; // will make sure new metadata is inflated in webview
								adapter.notifyDataSetChanged();
							}
							else
							{
								Logger.e(tag, "Expected Tag of Webview was WebViewHolder and received " + obj.getClass().getCanonicalName());
							}

						}
					});
				}
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

			final Intent intent = IntentManager.getForwardIntentForConvMessage(mContext, message,
					PlatformContent.getForwardCardData(message.platformWebMessageMetadata.JSONtoString()));
			mWebView.post(new Runnable()
			{
				@Override
				public void run()
				{
					mContext.startActivity(intent);
				}
			});

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
		});

	}

	@JavascriptInterface
	public void onLoadFinished(String height)
	{
		Logger.i(tag, "onloadfinished called with height=" + (Integer.parseInt(height) * Utils.densityMultiplier) + " current height is " + mWebView.getHeight());
		resizeWebview(height);
	}

	@JavascriptInterface
	public void onResize(String height)
	{
		Logger.i(tag, "onresize called with height=" + (Integer.parseInt(height) * Utils.densityMultiplier));
		resizeWebview(height);
	}

	private void resizeWebview(String heightS)
	{
		if (!TextUtils.isEmpty(heightS))
		{
			heightRunnable.height = Integer.parseInt(heightS);
			mWebView.post(heightRunnable);
		}
	}

	@JavascriptInterface
	public void printHeight(String height)
	{
		Logger.i(tag, "my webview height is px= " + mWebView.getHeight() + " and content height " + mWebView.getContentHeight() * Utils.densityMultiplier
				+ " , height what javascript thinks is " + height);
	}

	HeightRunnable heightRunnable = new HeightRunnable();

	class HeightRunnable implements Runnable
	{
		int height;

		@Override
		public void run()
		{
			if (height != 0)
			{
				height = (int) (Utils.densityMultiplier * height); // javascript returns us in dp

				Logger.i(tag, "HeightRunnable called with height=" + height + " and current height is " + mWebView.getHeight());

				int initHeight = mWebView.getMeasuredHeight();

				Logger.i("HeightAnim", "InitHeight = " + initHeight + " TargetHeight = " + height);

				if (initHeight == height)
				{
					return;
				}
				else if (initHeight > height)
				{
					collapse(mWebView, height);
				}
				else if (initHeight < height)
				{
					expand(mWebView, height);
				}

			}
		}
	};

	public static void expand(final View v, final int targetHeight)
	{
		final int initHeight = v.getMeasuredHeight();

		final int animationHeight = targetHeight - initHeight;

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				v.getLayoutParams().height = initHeight + (int) (animationHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds()
			{
				return true;
			}
		};

		a.setDuration(300);
		v.startAnimation(a);
	}

	public static void collapse(final View v, final int targetHeight)
	{
		final int initialHeight = v.getMeasuredHeight();

		final int animationHeight = initialHeight - targetHeight;

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				v.getLayoutParams().height = initialHeight - (int) (animationHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds()
			{
				return true;
			}
		};

		a.setDuration(300);
		v.startAnimation(a);
	}

	public void updateConvMessage(ConvMessage message)
	{
		this.message = message;
	}
}
