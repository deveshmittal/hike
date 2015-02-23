package com.bsb.hike.utils;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.auth.AccessToken;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.modules.httpmgr.RequestToken;
import com.bsb.hike.modules.httpmgr.exception.HttpException;
import com.bsb.hike.modules.httpmgr.hikehttp.HttpRequests;
import com.bsb.hike.modules.httpmgr.request.listener.IRequestListener;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.view.TwitterOAuthView;
import com.bsb.hike.view.TwitterOAuthView.Result;
import com.bsb.hike.view.TwitterOAuthView.TwitterAuthListener;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;

public abstract class AuthSocialAccountBaseActivity extends HikeAppStateBaseFragmentActivity implements TwitterAuthListener
{

	public static final int FB_AUTH_REQUEST_CODE = 64206;

	private static final String CALLBACK_URL = "http://get.hike.in/";

	private ProgressDialog dialog;

	private boolean shouldPost;

	private RequestToken requestToken;
	
	protected TwitterOAuthView twitterOAuthView;

	protected boolean facebookAuthPopupShowing;

	public void startTwitterAuth(boolean post)
	{
		shouldPost = post;
		twitterOAuthView = new TwitterOAuthView(this);
		twitterOAuthView.start(HikeConstants.APP_TWITTER_ID, HikeConstants.APP_TWITTER_SECRET, CALLBACK_URL, true, this);

		/*
		 * Workaround for an android bug where the keyboard does not popup in the web view. http://code.google.com/p/android/issues/detail?id=7189
		 */
		twitterOAuthView.requestFocus(View.FOCUS_DOWN);
		twitterOAuthView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				switch (event.getAction())
				{
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
					if (!v.hasFocus())
					{
						v.requestFocus();
					}
					break;
				}
				return false;
			}
		});

		setContentView(twitterOAuthView);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Object o = getLastCustomNonConfigurationInstance();
		if (o instanceof RequestToken)
		{
			dialog = ProgressDialog.show(this, null, getString(R.string.saving_social));
		}
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy()
	{
		if (dialog != null)
		{
			dialog.dismiss();
			dialog = null;
		}
		requestToken = null;
		super.onDestroy();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if(requestToken != null)
		{
			return requestToken;
		}
		return super.onRetainNonConfigurationInstance();
	}

	@Override
	public void onSuccess(TwitterOAuthView view, AccessToken accessToken)
	{
		Logger.d(getClass().getSimpleName(), "TOKEN:  " + accessToken.getToken() + " SECRET: " + accessToken.getTokenSecret());

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Editor editor = settings.edit();
		editor.putString(HikeMessengerApp.TWITTER_TOKEN, accessToken.getToken());
		editor.putString(HikeMessengerApp.TWITTER_TOKEN_SECRET, accessToken.getTokenSecret());
		editor.commit();

		HikeMessengerApp.makeTwitterInstance(accessToken.getToken(), accessToken.getTokenSecret());

		sendCredentialsToServer(accessToken.getToken(), accessToken.getTokenSecret(), -1, false);
	}

	@Override
	public void onFailure(TwitterOAuthView view, Result result)
	{
		HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED, false);
	}

	public void onCompleteFacebookAuth(String aToken, long expirationDate, String userId)
	{
		facebookAuthPopupShowing = false;

		final Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putString(HikeMessengerApp.FACEBOOK_TOKEN, aToken);
		editor.putLong(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES, expirationDate);

		editor.putString(HikeMessengerApp.FACEBOOK_USER_ID, userId);
		editor.commit();
		sendCredentialsToServer(userId, aToken, expirationDate, true);
		return;
	}

	public void makeMeRequest(final Session session, final String aToken, final long expirationDate)
	{
		// Make an API call to get user data and define a
		// new callback to handle the response.
		Request request = Request.newMeRequest(session, new Request.GraphUserCallback()
		{
			@Override
			public void onCompleted(GraphUser user, Response response)
			{
				// If the response is successful
				if (session == Session.getActiveSession())
				{
					if (user != null)
					{
						onCompleteFacebookAuth(aToken, expirationDate, user.getId());
					}
				}
				if (response.getError() != null)
				{
					Logger.e(getClass().getSimpleName(), "Facebook Get newMeRequest Failled", response.getError().getException());
					facebookError();
				}
			}
		});
		request.executeAsync();
	}

	public void facebookError()
	{
		Toast.makeText(this, R.string.social_failed, Toast.LENGTH_SHORT).show();
		HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED, true);
	}

	private IRequestListener getSendCredentialsToServerRequestListener(final boolean facebook)
	{
		return new IRequestListener()
		{	
			@Override
			public void onRequestSuccess(com.bsb.hike.modules.httpmgr.response.Response result)
			{
				if (dialog != null)
				{
					dialog.dismiss();
					dialog = null;
				}
				Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
				if (facebook)
				{
					editor.putBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, true);
				}
				else
				{
					editor.putBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, true);
				}
				editor.commit();
				requestToken = null;
				HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_COMPLETED, facebook);
			}
			
			@Override
			public void onRequestProgressUpdate(float progress)
			{				
			}
			
			@Override
			public void onRequestFailure(HttpException httpException)
			{
				if (dialog != null)
				{
					dialog.dismiss();
					dialog = null;
				}
				Toast.makeText(AuthSocialAccountBaseActivity.this, R.string.social_failed, Toast.LENGTH_SHORT).show();
				Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
				// Fail the whole process if the request to our server
				// fails.
				if (facebook)
				{
					editor.remove(HikeMessengerApp.FACEBOOK_TOKEN);
					editor.remove(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES);
					editor.remove(HikeMessengerApp.FACEBOOK_USER_ID);
					editor.remove(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE);
				}
				else
				{
					editor.remove(HikeMessengerApp.TWITTER_TOKEN);
					editor.remove(HikeMessengerApp.TWITTER_TOKEN_SECRET);
					editor.remove(HikeMessengerApp.TWITTER_AUTH_COMPLETE);
				}
				editor.commit();
				requestToken = null;
				HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED, facebook);
			}
		};
	}
	
	private void sendCredentialsToServer(String id, String token, long expires, final boolean facebook)
	{
		JSONObject request = new JSONObject();
		try
		{
			request.put(HikeConstants.ID, id);
			request.put(HikeConstants.TOKEN, token);
			if (expires != -1)
			{
				request.put(HikeConstants.EXPIRES, expires);
			}
			request.put(HikeConstants.POST, shouldPost);
		}
		catch (JSONException e)
		{
			Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		Logger.d(getClass().getSimpleName(), "Request: " + request.toString());
		requestToken = HttpRequests.sendSocialCredentialsRequest(facebook ? "fb" : "twitter", request, getSendCredentialsToServerRequestListener(facebook));
		requestToken.execute();
		
		if (!this.isFinishing())
			dialog = ProgressDialog.show(this, null, getString(R.string.saving_social));
	}
}
