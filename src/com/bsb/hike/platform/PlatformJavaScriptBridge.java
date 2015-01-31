package com.bsb.hike.platform;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Toast;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import java.util.ArrayList;
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
	public void showToast(String toast)
	{
		Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
	}

	public void vibrate(String msecs)
	{
		Utils.vibrate(Integer.parseInt(msecs));
	}

	@JavascriptInterface
	public void deleteMessage()
	{
		ArrayList<Long> msgIds = new ArrayList<Long>(1);
		msgIds.add(message.getMsgID());
		Bundle bundle = new Bundle();
		bundle.putBoolean(HikeConstants.Extras.IS_LAST_MESSAGE, false);
		bundle.putString(HikeConstants.Extras.MSISDN, message.getMsisdn());
		bundle.putBoolean(HikeConstants.Extras.DELETE_MEDIA_FROM_PHONE, false);
		HikeMessengerApp.getPubSub().publish(HikePubSub.DELETE_MESSAGE, new Pair<ArrayList<Long>, Bundle>(msgIds, bundle));
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
	public void setAlarm(String json, String timeInMills)
	{
		try
		{
			Logger.i(tag, "set alarm called " + json + " , mId " + message.getMsgID() + " , time " + timeInMills);
			PlatformAlarmManager.setAlarm(mContext, new JSONObject(json), (int)message.getMsgID(), Long.valueOf(timeInMills));
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	@JavascriptInterface
	public void updateHelperData(String json)
	{
		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID());
			String updatedJSON = HikeConversationsDatabase.getInstance().updateHelperData((message.getMsgID()), json);
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
	public void deleteAlarm()
	{
		HikeConversationsDatabase.getInstance().deleteAppAlarm((int)(message.getMsgID()));
	}

	@JavascriptInterface
	public void logFromJS(String tag, String data)
	{
		Logger.v(tag, data);
	}

	@JavascriptInterface
	public void updateMetadata( String json, String notifyScreen)
	{

		try
		{
			Logger.i(tag, "update metadata called " + json + " , message id=" + message.getMsgID() +" notifyScren is "+notifyScreen);
			String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata((int)(message.getMsgID()), json);

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
	public void forwardToChat( String json)
	{
		try
		{
			Logger.i(tag, "forward to chat called " + json + " , message id=" + message.getMsgID());

			if (!TextUtils.isEmpty(json))
			{
				String updatedJSON = HikeConversationsDatabase.getInstance().updateJSONMetadata((int)(message.getMsgID()), json);
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
		try
		{
			int requiredHeightinDP = Integer.parseInt(height);
			int requiredHeightInPX = (int) (requiredHeightinDP * Utils.densityMultiplier);
			if(requiredHeightInPX != mWebView.getHeight())
			{
				Logger.i(tag, "onloadfinished called with height=" + requiredHeightInPX + " current height is " + mWebView.getHeight() +" : updated in DB as well");
				// lets save in DB, so that from next time onwards we will have less flickering
				message.platformWebMessageMetadata.setCardHeight(requiredHeightinDP);
				HikeConversationsDatabase.getInstance().updateMetadataOfMessage(message.getMsgID(), message.platformWebMessageMetadata.JSONtoString());
			}else
			{
				Logger.i(tag, "onloadfinished called with height=" + requiredHeightInPX + " current height is " + mWebView.getHeight());
				resizeWebview(height);
			}
				
		}catch(NumberFormatException ne)
		{
			ne.printStackTrace();
		}
		
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
