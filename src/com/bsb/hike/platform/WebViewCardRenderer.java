package com.bsb.hike.platform;

import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import com.bsb.hike.R;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.platform.content.PlatformContent;
import com.bsb.hike.platform.content.PlatformContentListener;
import com.bsb.hike.platform.content.PlatformContentModel;
import com.bsb.hike.platform.content.PlatformWebClient;
import com.bsb.hike.utils.Logger;

/**
 * Created by shobhitmandloi on 14/01/15.
 */
public class WebViewCardRenderer extends BaseAdapter
{
	
	static final String tag = "webviewcardRenderer";
	
	Context mContext;

	ArrayList<ConvMessage> convMessages;

	BaseAdapter adapter;

	public WebViewCardRenderer(Context context, ArrayList<ConvMessage> convMessages)
	{
		this.mContext = context;
		this.convMessages = convMessages;
	}
	
	public WebViewCardRenderer(Context context, ArrayList<ConvMessage> convMessages,BaseAdapter adapter)
	{
		this.mContext = context;
		this.adapter =  adapter;
		this.convMessages = convMessages;
	}

	public static class WebViewHolder
	{
		long id;
		WebView myBrowser;
		PlatformJavaScriptBridge platformJavaScriptBridge;
		public View selectedStateOverlay;
	}

	private WebViewHolder initializaHolder(WebViewHolder holder, View view, ConvMessage convMessage)
	{
		holder.myBrowser = (WebView) view.findViewById(R.id.webcontent);
		holder.platformJavaScriptBridge = new PlatformJavaScriptBridge(mContext, holder.myBrowser, convMessage, this);
		holder.selectedStateOverlay = view.findViewById(R.id.selected_state_overlay);
		webViewStates(holder, convMessage);

		return holder;
	}

	private void webViewStates(WebViewHolder holder, ConvMessage convMessage)
	{
		holder.myBrowser.setVerticalScrollBarEnabled(false);
		holder.myBrowser.setHorizontalScrollBarEnabled(false);
		holder.myBrowser.addJavascriptInterface(holder.platformJavaScriptBridge, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
		holder.platformJavaScriptBridge.allowUniversalAccess();
		holder.platformJavaScriptBridge.allowDebugging();
		holder.myBrowser.getSettings().setJavaScriptEnabled(true);

	}

	@Override
	public int getCount()
	{
		return 0;
	}

	@Override
	public Object getItem(int position)
	{
		return convMessages.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return convMessages.get(position).getMsgID();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		Logger.i(tag, "get view with called with position " + position);

		View view = convertView;
		final ConvMessage convMessage = (ConvMessage) getItem(position);
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (view == null)
		{
			WebViewHolder viewHolder = new WebViewHolder();
			view = inflater.inflate(R.layout.html_item, parent, false);
			initializaHolder(viewHolder, view, convMessage);
			view.setTag(viewHolder);
		}
		final WebViewHolder viewHolder = (WebViewHolder) view.getTag();

		final WebView web = viewHolder.myBrowser;

		if (viewHolder.id != getItemId(position))
		{
			Logger.i(tag, "either tag is null or reused ");
			PlatformContent.getContent(convMessage.platformWebMessageMetadata.JSONtoString(), new PlatformContentListener<PlatformContentModel>()
			{
				public void onComplete(PlatformContentModel content)
				{
					viewHolder.id = getItemId(position);
					fillContent(web, content, convMessage);
				}
			});
		}
		else
		{
			Logger.i(tag, "either tag is not null ");
		}

		return view;

	}

	private void fillContent(WebView web, PlatformContentModel content, ConvMessage convMessage)
	{
		web.setWebViewClient(new CustomWebViewClient(convMessage));
		web.loadDataWithBaseURL("", content.getFormedData(), "text/html", "UTF-8", "");
	}

	private class CustomWebViewClient extends PlatformWebClient
	{

		ConvMessage convMessage;

		public CustomWebViewClient(ConvMessage convMessage)
		{
			this.convMessage = convMessage;
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
			view.requestLayout();
			view.setVisibility(View.VISIBLE);
			Log.d(tag, "Height of webView after loading is " + String.valueOf(view.getMeasuredHeight()) + "px");
			Logger.d(tag, "conv message passed to webview " + convMessage);
			Logger.d(tag, "Platform message metadata is" + convMessage.platformWebMessageMetadata.JSONtoString());
			view.loadUrl("javascript:setData(" + "'" + convMessage.getMsgID() + "','" + convMessage.getMsisdn() + "','" + convMessage.platformWebMessageMetadata.getHelperData().toString()
					+ "')");
			String alarmData = convMessage.platformWebMessageMetadata.getAlarmData();
			Logger.d(tag, "alarm data to html is " + alarmData);
			if (!TextUtils.isEmpty(alarmData))
			{
				view.loadUrl("javascript:alarmPlayed(" + "'" + alarmData + "')");
			}
			super.onPageFinished(view, url);
		}
	}

}
