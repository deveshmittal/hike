package com.bsb.hike.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;

public class WebViewActivity extends Activity {

	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_activity);

		String urlToLoad = getIntent().getStringExtra(HikeConstants.Extras.URL_TO_LOAD);
		String title = getIntent().getStringExtra(HikeConstants.Extras.TITLE);

		TextView titleTV = (TextView) findViewById(R.id.title);
		titleTV.setText(title);

		WebView webView = (WebView) findViewById(R.id.t_and_c_page);

		progressDialog = new ProgressDialog(WebViewActivity.this);
		progressDialog.setMessage("Loading...");
		WebViewClient client = new WebViewClient()
		{
			@Override
			public void onPageFinished(WebView view, String url) 
			{
				if (progressDialog != null) 
				{
					progressDialog.dismiss();
				}
				super.onPageFinished(view, url);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) 
			{
				progressDialog.show();
				super.onPageStarted(view, url, favicon);
			}
		};
		client.shouldOverrideUrlLoading(webView, urlToLoad);

		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl(urlToLoad);
		webView.setWebViewClient(client);
	}
}