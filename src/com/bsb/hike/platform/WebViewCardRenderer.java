package com.bsb.hike.platform;

import android.content.Context;
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
import com.bsb.hike.platform.content.PlatformContentRequest;
import com.bsb.hike.platform.content.PlatformWebClient;

import java.util.ArrayList;

/**
 * Created by shobhitmandloi on 14/01/15.
 */
public class WebViewCardRenderer extends BaseAdapter
{
	Context mContext;

	ArrayList<ConvMessage> convMessages;


	public WebViewCardRenderer(Context context, ArrayList<ConvMessage> convMessages)
	{
		this.mContext = context;
		this.convMessages = convMessages;
	}

	private static class WebViewHolder
	{

		WebView myBrowser;
	}

	private WebViewHolder initializaHolder(WebViewHolder holder, View view)
	{
		holder.myBrowser = (WebView) view.findViewById(R.id.webcontent);
		return holder;
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
	public View getView(int position, View convertView, ViewGroup parent)
	{
		WebViewHolder viewHolder = new WebViewHolder();
		View view = convertView;
		final ConvMessage convMessage = (ConvMessage) getItem(position);
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (view == null)
		{
			view = inflater.inflate(R.layout.html_item, parent, false);
			initializaHolder(viewHolder, view);
			view.setTag(viewHolder);
		}
		else
		{
			viewHolder = (WebViewHolder) view.getTag();
		}

		final WebView web = viewHolder.myBrowser;
		final PlatformJavaScriptBridge myJavaScriptInterface = new PlatformJavaScriptBridge(mContext, web, convMessage);
		String req2JSON = "{ \"appID\": \"cricket-fever\",\"layout_url\": \"https://s3-ap-southeast-1.amazonaws.com/platform-qa-segments/micro-apps/cricket-fevers/v1/cricket-fever.zip\",\"version\": \"0.1\", \"layout_id\": \"liveScore.html\", \"card_data\": \"{ \\\"title\\\": \\\"Final, ICC WC 2015\\\",\\\"liveScoreURL\\\": \\\"Final, ICC WC 2015\\\",\\\"statsURL\\\": \\\"Final, ICC WC 2015\\\",\\\"matchId\\\": \\\"m1\\\", \\\"place\\\": \\\"Sydney Cricket Ground\\\", \\\"team1\\\": { \\\"name\\\": \\\"IND\\\", \\\"score\\\": \\\"265/6\\\", \\\"overs\\\": \\\"43.3\\\" }, \\\"team2\\\": { \\\"name\\\": \\\"RSA\\\", \\\"score\\\": \\\"273/8\\\", \\\"overs\\\": \\\"50\\\" }, \\\"battingScore\\\": [{ \\\"name\\\": \\\"S Dhawan\\\", \\\"runs\\\": \\\"150\\\", \\\"balls\\\": \\\"140\\\", \\\"fours\\\": \\\"11\\\", \\\"sixes\\\": \\\"5\\\", \\\"SR\\\": \\\"120.23\\\", \\\"current\\\": false },{ \\\"name\\\": \\\"CA Pujara\\\", \\\"runs\\\": \\\"50\\\", \\\"balls\\\": \\\"40\\\", \\\"fours\\\": \\\"8\\\", \\\"sixes\\\": \\\"1\\\", \\\"SR\\\": \\\"120.47\\\", \\\"current\\\": false }], \\\"bowlingScore\\\": [{ \\\"name\\\": \\\"MG Johnson\\\", \\\"overs\\\": \\\"8\\\", \\\"maidens\\\": \\\"3\\\", \\\"runs\\\": \\\"29\\\", \\\"wickets\\\": \\\"0\\\", \\\"econ\\\": \\\"3.62\\\", \\\"current\\\": true },{ \\\"name\\\": \\\"JR Hazlewood\\\", \\\"overs\\\": \\\"8\\\", \\\"maidens\\\": \\\"3\\\", \\\"runs\\\": \\\"29\\\", \\\"wickets\\\": \\\"0\\\", \\\"econ\\\": \\\"3.62\\\", \\\"current\\\": false  }] }\"}";

		PlatformContentRequest req2 = PlatformContent.getContent(convMessage.platformWebMessageMetadata.JSONtoString(), new PlatformContentListener< PlatformContentModel >()
		{
			public void onComplete(PlatformContentModel content)
			{
				System.out.println("+++++++++++ " + content.toString());
				web.setVerticalScrollBarEnabled(false);
				web.setHorizontalScrollBarEnabled(false);
				web.addJavascriptInterface(myJavaScriptInterface, HikePlatformConstants.PLATFORM_BRIDGE_NAME);
				myJavaScriptInterface.allowUniversalAccess();
				myJavaScriptInterface.allowDebugging();
				web.getSettings().setJavaScriptEnabled(true);
				web.setWebViewClient(new CustomWebViewClient(convMessage));
				web.setWebViewClient(new CustomWebViewClient(convMessage));
				web.getSettings().setJavaScriptEnabled(true);
				web.getSettings().setAllowUniversalAccessFromFileURLs(true);
				web.loadDataWithBaseURL("", content.getFormedData(), "text/html", "UTF-8", "");
			}});

		return view;

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
			Log.d("Height data", "Height of webView after loading is " + String.valueOf(view.getMeasuredHeight()) + "px");
			view.loadUrl("javascript:setData(\"" + convMessage.getMsgID() + "','" + convMessage.getMsisdn() + "','" + convMessage.platformWebMessageMetadata.getHelperData() + "\")");
			super.onPageFinished(view, url);
		}
	}

}
