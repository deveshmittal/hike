package com.bsb.hike.utils;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.auth.AccessToken;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.view.TwitterOAuthView;
import com.bsb.hike.view.TwitterOAuthView.Result;
import com.bsb.hike.view.TwitterOAuthView.TwitterAuthListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public abstract class AuthSocialAccountBaseActivity extends HikeAppStateBaseActivity
		implements DialogListener, TwitterAuthListener {

	public static final int FB_AUTH_REQUEST_CODE = 64206;
	private static final String CALLBACK_URL = "http://get.hike.in/";

	private HikeHTTPTask hikeHTTPTask;
	private ProgressDialog dialog;
	private boolean shouldPost;
	protected TwitterOAuthView twitterOAuthView;
	protected boolean facebookAuthPopupShowing;

	public void startFBAuth(boolean post) {
		shouldPost = post;
		facebookAuthPopupShowing = true;
		HikeMessengerApp.getFacebook().authorize(this,
				new String[] { "publish_stream" }, Facebook.FORCE_DIALOG_AUTH,
				this);
	}

	public void startTwitterAuth(boolean post) {
		shouldPost = post;
		twitterOAuthView = new TwitterOAuthView(this);
		twitterOAuthView.start(HikeConstants.APP_TWITTER_ID,
				HikeConstants.APP_TWITTER_SECRET, CALLBACK_URL, true, this);

		/*
		 * Workaround for an android bug where the keyboard does not popup in
		 * the web view. http://code.google.com/p/android/issues/detail?id=7189
		 */
		twitterOAuthView.requestFocus(View.FOCUS_DOWN);
		twitterOAuthView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
					if (!v.hasFocus()) {
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
	protected void onResume() {
		super.onResume();
		Object o = getLastNonConfigurationInstance();
		if (o instanceof HikeHTTPTask) {
			dialog = ProgressDialog.show(this, null,
					getString(R.string.saving_social));
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
		hikeHTTPTask = null;
		super.onDestroy();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (hikeHTTPTask != null) {
			return hikeHTTPTask;
		}
		return super.onRetainNonConfigurationInstance();
	}

	@Override
	public void onSuccess(TwitterOAuthView view, AccessToken accessToken) {
		Log.d(getClass().getSimpleName(), "TOKEN:  " + accessToken.getToken()
				+ " SECRET: " + accessToken.getTokenSecret());

		SharedPreferences settings = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Editor editor = settings.edit();
		editor.putString(HikeMessengerApp.TWITTER_TOKEN, accessToken.getToken());
		editor.putString(HikeMessengerApp.TWITTER_TOKEN_SECRET,
				accessToken.getTokenSecret());
		editor.commit();

		HikeMessengerApp.makeTwitterInstance(accessToken.getToken(),
				accessToken.getTokenSecret());

		sendCredentialsToServer(accessToken.getToken(),
				accessToken.getTokenSecret(), -1, false);
	}

	@Override
	public void onFailure(TwitterOAuthView view, Result result) {
		HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED,
				false);
	}

	@Override
	public void onComplete(Bundle values) {
		facebookAuthPopupShowing = false;
		Facebook facebook = HikeMessengerApp.getFacebook();

		final Editor editor = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putString(HikeMessengerApp.FACEBOOK_TOKEN,
				facebook.getAccessToken());
		editor.putLong(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES,
				facebook.getAccessExpires());

		String userId = null;
		try {
			JSONObject me = new JSONObject(facebook.request("me"));
			userId = me.optString("id");
			editor.putString(HikeMessengerApp.FACEBOOK_USER_ID, userId);
			editor.commit();
			sendCredentialsToServer(userId, facebook.getAccessToken(),
					facebook.getAccessExpires(), true);
			return;
		} catch (MalformedURLException e1) {
			Log.e(getClass().getSimpleName(), "Malformed URL", e1);
			HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED,
					true);
		} catch (JSONException e2) {
			Log.e(getClass().getSimpleName(), "Invalid JSON", e2);
			HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED,
					true);
		} catch (IOException e3) {
			Log.e(getClass().getSimpleName(), "IOException", e3);
			HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED,
					true);
		}
	}

	@Override
	public void onFacebookError(FacebookError error) {
		facebookAuthPopupShowing = false;
		Toast.makeText(this, R.string.social_failed, Toast.LENGTH_SHORT).show();
		HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED,
				true);
	}

	@Override
	public void onError(DialogError e) {
		facebookAuthPopupShowing = false;
		Toast.makeText(this, R.string.social_failed, Toast.LENGTH_SHORT).show();
		HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED,
				true);
	}

	@Override
	public void onCancel() {
		facebookAuthPopupShowing = false;
		HikeMessengerApp.getPubSub().publish(HikePubSub.SOCIAL_AUTH_FAILED,
				true);
	}

	private void sendCredentialsToServer(String id, String token, long expires,
			final boolean facebook) {
		JSONObject request = new JSONObject();
		try {
			request.put(HikeConstants.ID, id);
			request.put(HikeConstants.TOKEN, token);
			if (expires != -1) {
				request.put(HikeConstants.EXPIRES, expires);
			}
			request.put(HikeConstants.POST, shouldPost);
		} catch (JSONException e) {
			Log.e(getClass().getSimpleName(), "Invalid JSON", e);
		}
		Log.d(getClass().getSimpleName(), "Request: " + request.toString());
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
				facebook ? "/account/connect/fb" : "/account/connect/twitter",
				RequestType.OTHER, new HikeHttpRequest.HikeHttpCallback() {
					public void onSuccess(JSONObject response) {
						if (dialog != null) {
							dialog.dismiss();
							dialog = null;
						}
						Editor editor = getSharedPreferences(
								HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
								.edit();
						if (facebook) {
							editor.putBoolean(
									HikeMessengerApp.FACEBOOK_AUTH_COMPLETE,
									true);
						} else {
							editor.putBoolean(
									HikeMessengerApp.TWITTER_AUTH_COMPLETE,
									true);
						}
						editor.commit();
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.SOCIAL_AUTH_COMPLETED, facebook);
						hikeHTTPTask = null;
					}

					public void onFailure() {
						if (dialog != null) {
							dialog.dismiss();
							dialog = null;
						}
						Toast.makeText(AuthSocialAccountBaseActivity.this,
								R.string.social_failed, Toast.LENGTH_SHORT)
								.show();
						Editor editor = getSharedPreferences(
								HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
								.edit();
						// Fail the whole process if the request to our server
						// fails.
						if (facebook) {
							HikeMessengerApp.getFacebook().setAccessExpires(0);
							HikeMessengerApp.getFacebook().setAccessToken("");

							editor.remove(HikeMessengerApp.FACEBOOK_TOKEN);
							editor.remove(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES);
							editor.remove(HikeMessengerApp.FACEBOOK_USER_ID);
							editor.remove(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE);
						} else {
							editor.remove(HikeMessengerApp.TWITTER_TOKEN);
							editor.remove(HikeMessengerApp.TWITTER_TOKEN_SECRET);
							editor.remove(HikeMessengerApp.TWITTER_AUTH_COMPLETE);
						}
						editor.commit();
						hikeHTTPTask = null;
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.SOCIAL_AUTH_FAILED, facebook);
					}
				});
		hikeHttpRequest.setJSONData(request);
		hikeHTTPTask = new HikeHTTPTask(null, 0);
		hikeHTTPTask.execute(hikeHttpRequest);

		dialog = ProgressDialog.show(this, null,
				getString(R.string.saving_social));
	}
}
