package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.facebook.Session;
import com.facebook.SessionState;

public class TellAFriend extends HikeAppStateBaseFragmentActivity implements
		OnClickListener, Listener {

	private boolean facebookPostPopupShowing = false;

	private SharedPreferences settings;

	private String[] pubSubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED,
			HikePubSub.DISMISS_POSTING_DIALOG };

	private ProgressDialog progressDialog;

	boolean pickFriendsWhenSessionOpened;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.tell_a_friend);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);

		int ids[] = { R.id.facebook, R.id.twitter, R.id.sms, R.id.email,
				R.id.other };
		for (int i = 0; i < ids.length; i++) {
			findViewById(ids[i]).setOnClickListener(this);
		}

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		if (savedInstanceState != null) {
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.FACEBOOK_POST_POPUP_SHOWING)) {
				onClick(findViewById(R.id.facebook));
			}
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.DIALOG_SHOWING)) {
				progressDialog = ProgressDialog.show(this, null,
						getString(R.string.posting_update));
			}
		}
		setupActionBar();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.FACEBOOK_POST_POPUP_SHOWING,
				facebookPostPopupShowing);
		outState.putBoolean(HikeConstants.Extras.DIALOG_SHOWING,
				progressDialog != null && progressDialog.isShowing());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		HikeMessengerApp.getFacebook().authorizeCallback(requestCode,
				resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode,
				resultCode, data);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.facebook:
			onClickPickFriends();
			break;

		case R.id.twitter:
			if (!settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE,
					false)) {
				startActivity(new Intent(this, TwitterAuthActivity.class));
			} else {
				postToSocialNetwork(false);
			}
			break;

		case R.id.sms:
			Utils.logEvent(this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
			startActivity(new Intent(this, HikeListActivity.class));
			break;

		case R.id.email:
			Intent mailIntent = new Intent(Intent.ACTION_SENDTO);

			mailIntent.setData(Uri.parse("mailto:"));
			mailIntent.putExtra(Intent.EXTRA_SUBJECT,
					getString(R.string.email_subject));
			mailIntent.putExtra(Intent.EXTRA_TEXT,
					Utils.getInviteMessage(this, R.string.email_body));

			startActivity(mailIntent);
			break;

		case R.id.other:
			Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_INVITE);
			Utils.startShareIntent(this,
					Utils.getInviteMessage(this, R.string.invite_share_message));
			break;
		}
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type)) {
			final boolean facebook = (Boolean) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					onClick(findViewById(facebook ? R.id.facebook
							: R.id.twitter));
				}
			});
		} else if (HikePubSub.DISMISS_POSTING_DIALOG.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (progressDialog != null) {
						progressDialog.dismiss();
						progressDialog = null;
					}
				}
			});
		}
	}

	private void onClickPickFriends() {
		startPickFriendsActivity();
	}

	private void startPickFriendsActivity() {
		if (ensureOpenSession()) {
			Intent intent = new Intent(this, SocialNetInviteActivity.class);
			intent.putExtra(HikeConstants.Extras.IS_FACEBOOK, true);
			Log.d("tell a friend", "calling socialNetInviteActivity");
			startActivity(intent);
		} else {
			pickFriendsWhenSessionOpened = true;
		}
	}

	private boolean ensureOpenSession() {
		Log.d("ensure Open Session", "entered in ensureOpenSession");

		if (Session.getActiveSession() == null
				|| !Session.getActiveSession().isOpened()) {

			Log.d("ensure Open Session",
					"active session is either null or closed");
			Session.openActiveSession(this, true, new Session.StatusCallback() {
				@Override
				public void call(Session session, SessionState state,
						Exception exception) {
					onSessionStateChanged(session, state, exception);
				}
			});
			return false;
		}

		return true;
	}

	private void onSessionStateChanged(Session session, SessionState state,
			Exception exception) {
		Log.d("calling session change ", "inside onSessionStateChanged");
		if (pickFriendsWhenSessionOpened && state.isOpened()) {
			pickFriendsWhenSessionOpened = false;
			startPickFriendsActivity();
		}
	}

	private void postToSocialNetwork(final boolean facebook) {
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
				"/account/spread", RequestType.SOCIAL_POST,
				new HikeHttpCallback() {

					@Override
					public void onSuccess(JSONObject response) {
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.DISMISS_POSTING_DIALOG, null);
						parseResponse(response, facebook);
					}

					@Override
					public void onFailure() {
						HikeMessengerApp.getPubSub().publish(
								HikePubSub.DISMISS_POSTING_DIALOG, null);
						Toast.makeText(getApplicationContext(),
								R.string.posting_update_fail,
								Toast.LENGTH_SHORT).show();
					}

				});
		JSONObject data = new JSONObject();
		try {
			data.put(facebook ? HikeConstants.FACEBOOK_STATUS
					: HikeConstants.TWITTER_STATUS, true);
			hikeHttpRequest.setJSONData(data);
			Log.d(getClass().getSimpleName(), "JSON: " + data);

			progressDialog = ProgressDialog.show(this, null,
					getString(facebook ? R.string.posting_update_facebook
							: R.string.posting_update_twitter));

			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			Utils.executeHttpTask(hikeHTTPTask, hikeHttpRequest);
		} catch (JSONException e) {
			Log.w(getClass().getSimpleName(), "Invalid JSON", e);
		}
	}

	private void parseResponse(JSONObject response, boolean facebook) {
		String responseString = response
				.optString(facebook ? HikeConstants.FACEBOOK_STATUS
						: HikeConstants.TWITTER_STATUS);

		if (TextUtils.isEmpty(responseString)) {
			return;
		}

		if (HikeConstants.SocialPostResponse.SUCCESS.equals(responseString)) {
			Toast.makeText(getApplicationContext(), R.string.posted_update,
					Toast.LENGTH_SHORT).show();
		} else if (HikeConstants.SocialPostResponse.FAILURE
				.equals(responseString)) {
			Toast.makeText(getApplicationContext(),
					R.string.posting_update_fail, Toast.LENGTH_SHORT).show();
		} else {
			Editor editor = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
			if (facebook) {
				editor.remove(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE);
				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN);
				editor.remove(HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES);
				editor.remove(HikeMessengerApp.FACEBOOK_USER_ID);
			} else {
				editor.remove(HikeMessengerApp.TWITTER_AUTH_COMPLETE);
				editor.remove(HikeMessengerApp.TWITTER_TOKEN);
				editor.remove(HikeMessengerApp.TWITTER_TOKEN_SECRET);
			}
			editor.commit();
			onClick(findViewById(facebook ? R.id.facebook : R.id.twitter));
		}
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(
				R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.invite_friends);
		backContainer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TellAFriend.this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		actionBar.setCustomView(actionBarView);
	}

}
