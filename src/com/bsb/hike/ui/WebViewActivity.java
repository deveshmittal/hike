package com.bsb.hike.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;

public class WebViewActivity extends HikeAppStateBaseFragmentActivity
{

	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_activity);

		String urlToLoad = getIntent().getStringExtra(HikeConstants.Extras.URL_TO_LOAD);
		String title = getIntent().getStringExtra(HikeConstants.Extras.TITLE);

		webView = (WebView) findViewById(R.id.t_and_c_page);
		final ProgressBar bar = (ProgressBar) findViewById(R.id.progress);

		WebViewClient client = new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url)
			{
				super.onPageFinished(view, url);
				bar.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				bar.setProgress(0);
				bar.setVisibility(View.VISIBLE);
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				if (url == null)
				{
					return false;
				}
				if (url.startsWith("mailto:"))
				{
					MailTo mt = MailTo.parse(url);
					Intent i = newEmailIntent(WebViewActivity.this, mt.getTo(), mt.getSubject(), mt.getBody(), mt.getCc());
					startActivity(i);
					view.reload();
				}
				else if (url.toLowerCase().endsWith("hike.in/rewards/invite"))
				{
					Intent i = new Intent(WebViewActivity.this, HikeListActivity.class);
					startActivity(i);
				}
				else if (url.startsWith("market://") || url.contains("play.google.com/store/apps/details?id"))
				{
					try
					{
						view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					}
					catch (ActivityNotFoundException e)
					{
						Logger.w(getClass().getSimpleName(), e);
						view.loadUrl(url);
					}
				}
				else
				{
					view.loadUrl(url);
				}
				return true;
			}
		};

		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl(urlToLoad);
		webView.setWebViewClient(client);
		webView.setWebChromeClient(new WebChromeClient()
		{
			@Override
			public void onProgressChanged(WebView view, int newProgress)
			{
				super.onProgressChanged(view, newProgress);
				bar.setProgress(newProgress);
			}
		});
		setupActionBar(title);
	}

	public Intent newEmailIntent(Context context, String address, String subject, String body, String cc)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
		intent.putExtra(Intent.EXTRA_TEXT, body);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_CC, cc);
		intent.setType("message/rfc822");
		return intent;
	}

	private void setupActionBar(String titleString)
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(titleString);
		backContainer.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	public void onBackPressed()
	{
		if (webView.canGoBack())
		{
			webView.goBack();
		}
		else
		{
			super.onBackPressed();
		}
	}
}