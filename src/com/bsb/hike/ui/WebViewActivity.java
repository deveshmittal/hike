package com.bsb.hike.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;

public class WebViewActivity extends DrawerBaseActivity {

	private boolean rewardsPage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview_activity);

		String urlToLoad = getIntent().getStringExtra(
				HikeConstants.Extras.URL_TO_LOAD);
		String title = getIntent().getStringExtra(HikeConstants.Extras.TITLE);

		rewardsPage = getIntent().getBooleanExtra(
				HikeConstants.Extras.REWARDS_PAGE, false);

		TextView titleTV = (TextView) findViewById(rewardsPage ? R.id.title_centered
				: R.id.title);
		titleTV.setText(title);

		if (rewardsPage) {
			afterSetContentView(savedInstanceState);
		}

		WebView webView = (WebView) findViewById(R.id.t_and_c_page);

		WebViewClient client = new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				if (!rewardsPage) {
					findViewById(R.id.loading_layout).setVisibility(
							View.INVISIBLE);
				}
				super.onPageFinished(view, url);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if (!rewardsPage) {
					findViewById(R.id.loading_layout).setVisibility(
							View.VISIBLE);
				}
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url == null) {
					return false;
				}
				if (url.startsWith("mailto:")) {
					MailTo mt = MailTo.parse(url);
					Intent i = newEmailIntent(WebViewActivity.this, mt.getTo(),
							mt.getSubject(), mt.getBody(), mt.getCc());
					startActivity(i);
					view.reload();
				} else if (url.toLowerCase().endsWith("hike.in/rewards/invite")) {
					Intent i = new Intent(WebViewActivity.this,
							HikeListActivity.class);
					startActivity(i);
				} else {
					view.loadUrl(url);
				}
				return true;
			}
		};

		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl(urlToLoad);
		webView.setWebViewClient(client);
	}

	@Override
	public void onBackPressed() {
		if (rewardsPage) {
			super.onBackPressed();
		} else {
			finish();
		}
	}

	public Intent newEmailIntent(Context context, String address,
			String subject, String body, String cc) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
		intent.putExtra(Intent.EXTRA_TEXT, body);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_CC, cc);
		intent.setType("message/rfc822");
		return intent;
	}
}