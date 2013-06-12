package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.HikeHttpCallback;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;

public class TellAFriend extends DrawerBaseActivity implements OnClickListener {

	private boolean facebookPostPopupShowing = false;

	private SharedPreferences settings;

	private String[] pubSubListeners = { HikePubSub.SOCIAL_AUTH_COMPLETED };

	private ProgressDialog progressDialog;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.tell_a_friend);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);

		afterSetContentView(savedInstanceState);

		TextView viaSms = (TextView) findViewById(R.id.via_sms);

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				HikeConstants.FREE_SMS_PREF, true)) {
			String text = getString(R.string.earn_sms_friend_join);
			String textToBold = getString(R.string.via_sms);
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			if (text.indexOf(textToBold) != -1) {
				ssb.setSpan(
						new ForegroundColorSpan(getResources().getColor(
								R.color.subtext)), text.indexOf(textToBold),
						text.indexOf(textToBold) + textToBold.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}

			viaSms.setText(ssb);
			viaSms.setVisibility(View.VISIBLE);
		} else {
			viaSms.setVisibility(View.GONE);
		}

		TextView mTitleView = (TextView) findViewById(R.id.title_centered);
		mTitleView.setText(R.string.invite_friends);

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
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.facebook:
			if (!settings.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE,
					false)) {
				startFBAuth(false);
			} else {
				postToSocialNetwork(true);
			}
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
		super.onEventReceived(type, object);
		if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type)) {
			final boolean facebook = (Boolean) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					onClick(findViewById(facebook ? R.id.facebook
							: R.id.twitter));
				}
			});
		}
	}

	private void postToSocialNetwork(final boolean facebook) {
		HikeHttpRequest hikeHttpRequest = new HikeHttpRequest(
				"/account/spread", RequestType.SOCIAL_POST,
				new HikeHttpCallback() {

					@Override
					public void onSuccess(JSONObject response) {
						if (progressDialog != null) {
							progressDialog.dismiss();
							progressDialog = null;
						}
						parseResponse(response, facebook);
					}

					@Override
					public void onFailure() {
						if (progressDialog != null) {
							progressDialog.dismiss();
							progressDialog = null;
						}
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
					getString(R.string.posting_update));
			
			HikeHTTPTask hikeHTTPTask = new HikeHTTPTask(null, 0);
			hikeHTTPTask.execute(hikeHttpRequest);
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
}
