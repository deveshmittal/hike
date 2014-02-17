package com.bsb.hike.view;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bsb.hike.utils.Utils;

public class TwitterOAuthView extends WebView
{
	/**
	 * Result code of Twitter OAuth process.
	 */
	public enum Result
	{
		/**
		 * The application has been authorized by the user and got an access token successfully.
		 */
		SUCCESS,

		/**
		 * Twitter OAuth process was cancelled. This result code is generated when the internal {@link AsyncTask} subclass was cancelled for some reasons.
		 */
		CANCELLATION,

		/**
		 * Twitter OAuth process was not even started due to failure of getting a request token. The pair of consumer key and consumer secret was wrong or some kind of network
		 * error occurred.
		 */
		REQUEST_TOKEN_ERROR,

		/**
		 * The application has not been authorized by the user, or a network error occurred during the OAuth handshake.
		 */
		AUTHORIZATION_ERROR,

		/**
		 * The application has been authorized by the user but failed to get an access token.
		 */
		ACCESS_TOKEN_ERROR
	}

	/**
	 * Listener to be notified of Twitter OAuth process result.
	 * 
	 * <p>
	 * The methods of this listener are called on the UI thread.
	 * </p>
	 * 
	 */
	public interface TwitterAuthListener
	{
		/**
		 * Called when the application has been authorized by the user and got an access token successfully.
		 * 
		 * @param view
		 * @param accessToken
		 */
		void onSuccess(TwitterOAuthView view, AccessToken accessToken);

		/**
		 * Called when the OAuth process was not completed successfully.
		 * 
		 * @param view
		 * @param result
		 */
		void onFailure(TwitterOAuthView view, Result result);
	}

	/**
	 * A constructor that calls {@link WebView#WebView(Context, AttributeSet, int) super}(context, attrs, defStyle).
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public TwitterOAuthView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		// Additional initialization.
		init();
	}

	/**
	 * A constructor that calls {@link WebView#WebView(Context, AttributeSet) super}(context, attrs).
	 * 
	 * @param context
	 * @param attrs
	 */
	public TwitterOAuthView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		// Additional initialization.
		init();
	}

	/**
	 * A constructor that calls {@link WebView#WebView(Context) super}(context).
	 * 
	 * @param context
	 */
	public TwitterOAuthView(Context context)
	{
		super(context);

		// Additional initialization.
		init();
	}

	private void init()
	{
		WebSettings settings = getSettings();

		// Not use cache.
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

		// Enable JavaScript.
		settings.setJavaScriptEnabled(true);

		// Enable zoom control.
		settings.setBuiltInZoomControls(true);

		// Scroll bar
		setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
	}

	/**
	 * Start Twitter OAuth process.
	 * 
	 * <p>
	 * This method does the following in the background.
	 * </p>
	 * 
	 * <ol>
	 * <li>Get a request token using the given pair of consumer key and consumer secret.
	 * <li>Load the authorization URL that the obtained request token points to into this TwitterOAuthView instance.
	 * <li>Wait for the user to finish the authorization process at Twitter's authorization site. This TwitterOAuthView instance is redirected to the callback URL as a result.
	 * <li>Detect the redirection to the callback URL and retrieve the value of the oauth_verifier parameter from the URL. If and only if dummyCallbackUrl is false, the callback
	 * URL is actually accessed.
	 * <li>Get an access token using the oauth_verifier.
	 * <li>Call {@link TwitterAuthListener#onSuccess(TwitterOAuthView, AccessToken) onSuccess()} method of the {@link TwitterAuthListener listener} on the UI thread.
	 * </ol>
	 * 
	 * <p>
	 * If an error occurred during the above steps, {@link TwitterAuthListener#onFailure(TwitterOAuthView, TwitterOAuthView.Result) onFailure()} of the {@link TwitterAuthListener
	 * listener} is called.
	 * </p>
	 * 
	 * @param consumerKey
	 * @param consumerSecret
	 * @param callbackUrl
	 * @param dummyCallbackUrl
	 * @param listener
	 * 
	 * @throws IllegalArgumentException
	 *             At least one of 'consumerKey', 'consumerSecret' or 'callbackUrl' is null.
	 */
	public void start(String consumerKey, String consumerSecret, String callbackUrl, boolean dummyCallbackUrl, TwitterAuthListener listener)
	{
		if (consumerKey == null || consumerSecret == null || callbackUrl == null || listener == null)
		{
			throw new IllegalArgumentException();
		}
		Boolean dummy = Boolean.valueOf(dummyCallbackUrl);
		TwitterOAuthTask authTask = new TwitterOAuthTask();
		if (Utils.isHoneycombOrHigher())
		{
			authTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, consumerKey, consumerSecret, callbackUrl, dummy, listener);
		}
		else
		{
			authTask.execute(consumerKey, consumerSecret, callbackUrl, dummy, listener);
		}
	}

	private class TwitterOAuthTask extends AsyncTask<Object, Void, Result>
	{
		private String callbackUrl;

		private boolean dummyCallbackUrl;

		private TwitterAuthListener listener;

		private Twitter twitter;

		private RequestToken requestToken;

		private volatile boolean authorizationDone;

		private volatile String verifier;

		private AccessToken accessToken;

		@Override
		protected void onPreExecute()
		{
			// Set up a WebViewClient on the UI thread.
			TwitterOAuthView.this.setWebViewClient(new LocalWebViewClient());
		}

		@Override
		protected Result doInBackground(Object... args)
		{
			String consumerKey = (String) args[0];
			String consumerSecret = (String) args[1];

			// Callback URL.
			callbackUrl = (String) args[2];
			dummyCallbackUrl = (Boolean) args[3];

			// Listener
			listener = (TwitterAuthListener) args[4];

			{
				Log.d(getClass().getSimpleName(), "CONSUMER KEY = " + consumerKey);
				Log.d(getClass().getSimpleName(), "CONSUMER SECRET = " + consumerSecret);
				Log.d(getClass().getSimpleName(), "CALLBACK URL = " + callbackUrl);
				Log.d(getClass().getSimpleName(), "DUMMY CALLBACK URL = " + dummyCallbackUrl);
			}

			System.setProperty("twitter4j.debug", "true");

			// Create a Twitter instance with the given pair of
			// consumer key and consumer secret.
			twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(consumerKey, consumerSecret);

			// Get a request token. This triggers network access.
			requestToken = getRequestToken();
			if (requestToken == null)
			{
				// Failed to get a request token.
				return Result.REQUEST_TOKEN_ERROR;
			}

			// Access Twitter's authorization page. After the user's
			// operation, this web view is redirected to the callback
			// URL, which is caught by shouldOverrideUrlLoading() of
			// LocalWebViewClient.
			authorize();

			// Wait until the authorization step is done.
			waitForAuthorization();

			// If the authorization has succeeded, 'verifier' is not null.
			if (verifier == null)
			{
				// The authorization failed.
				return Result.AUTHORIZATION_ERROR;
			}

			// The authorization succeeded. The last step is to get
			// an access token using the verifier.
			accessToken = getAccessToken();
			if (accessToken == null)
			{
				// Failed to get an access token.
				return Result.ACCESS_TOKEN_ERROR;
			}

			// All the steps were done successfully.
			return Result.SUCCESS;
		}

		@Override
		protected void onProgressUpdate(Void... values)
		{
			// In this implementation, onProgressUpdate() is called
			// only from authorize().

			// The authorization URL.
			String url = requestToken.getAuthorizationURL();

			Log.d(getClass().getSimpleName(), "Loading the authorization URL: " + url);

			// Load the authorization URL on the UI thread.
			TwitterOAuthView.this.loadUrl(url);
		}

		@Override
		protected void onPostExecute(Result result)
		{

			Log.d(getClass().getSimpleName(), "onPostExecute: result = " + result);

			if (result == null)
			{
				// Probably cancelled.
				result = Result.CANCELLATION;
			}

			if (result == Result.SUCCESS)
			{
				// Call onSuccess() method of the listener.
				listener.onSuccess(TwitterOAuthView.this, accessToken);
			}
			else
			{
				// Call onFailure() method of the listener.
				listener.onFailure(TwitterOAuthView.this, result);
			}
		}

		private RequestToken getRequestToken()
		{
			try
			{
				// Get a request token. This triggers network access.
				RequestToken token = twitter.getOAuthRequestToken();

				Log.d(getClass().getSimpleName(), "Got a request token.");

				return token;
			}
			catch (TwitterException e)
			{
				// Failed to get a request token.
				e.printStackTrace();
				Log.e(getClass().getSimpleName(), "Failed to get a request token.", e);

				// No request token.
				return null;
			}
		}

		private void authorize()
		{
			// WebView.loadUrl() needs to be called on the UI thread,
			// so trigger onProgressUpdate().
			publishProgress();
		}

		private void waitForAuthorization()
		{
			while (authorizationDone == false)
			{
				synchronized (this)
				{
					try
					{
						Log.d(getClass().getSimpleName(), "Waiting for the authorization step to be done.");
						this.wait();
					}
					catch (InterruptedException e)
					{
					}
				}
			}

			Log.d(getClass().getSimpleName(), "Finished waiting for the authorization step to be done.");
		}

		private void notifyAuthorization()
		{
			// The authorization step was done.
			authorizationDone = true;

			synchronized (this)
			{
				Log.d(getClass().getSimpleName(), "Notifying that the authorization step was done.");
				this.notify();
			}
		}

		private class LocalWebViewClient extends WebViewClient
		{

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
			{
				// Something wrong happened during the authorization step.
				Log.e(getClass().getSimpleName(), "onReceivedError: [" + errorCode + "] " + description);

				// Stop the authorization step.
				notifyAuthorization();
			}

			@Override
			public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
			{
				handler.proceed();
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				super.onPageStarted(view, url, favicon);

				// 11 = Build.VERSION_CODES.HONEYCOMB (Android 3.0)
				if (Build.VERSION.SDK_INT < 11)
				{
					// According to this page:
					//
					// http://www.catchingtales.com/android-webview-shouldoverrideurlloading-and-redirect/416/
					//
					// shouldOverrideUrlLoading() is not called for redirects on
					// Android earlier than 3.0, so call the method manually.
					//
					// The implementation of shouldOverrideUrlLoading() returns
					// true only when the URL starts with the callback URL and
					// dummyCallbackUrl is true.
					boolean stop = shouldOverrideUrlLoading(view, url);

					if (stop)
					{
						// Stop loading the current page.
						stopLoading();
					}
				}
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				Log.d(getClass().getSimpleName(), "URL:  " + url + " callback: " + callbackUrl);
				// Check if the given URL is the callback URL.
				if (url.startsWith(callbackUrl) == false)
				{
					// The URL is not the callback URL.
					return false;
				}

				// This web view is about to be redirected to the callback URL.

				Log.d(getClass().getSimpleName(), "Detected the callback URL: " + url);

				// Convert String to Uri.
				Uri uri = Uri.parse(url);

				// Get the value of the query parameter "oauth_verifier".
				// A successful response should contain the parameter.
				verifier = uri.getQueryParameter("oauth_verifier");

				Log.d(getClass().getSimpleName(), "oauth_verifier = " + verifier);

				// Notify that the the authorization step was done.
				notifyAuthorization();

				// Whether the callback URL is actually accessed or not
				// depends on the value of dummyCallbackUrl. If the
				// value of dummyCallbackUrl is true, the callback URL
				// is not accessed.
				return dummyCallbackUrl;
			}
		}

		private AccessToken getAccessToken()
		{
			try
			{
				// Get an access token. This triggers network access.
				AccessToken token = twitter.getOAuthAccessToken(requestToken, verifier);
				Log.d(getClass().getSimpleName(), "Got an access token for " + token.getScreenName());

				return token;
			}
			catch (TwitterException e)
			{
				// Failed to get an access token.
				Log.e(getClass().getSimpleName(), "Failed to get an access token.", e);
				// No access token.
				return null;
			}
		}
	}
}