package com.bsb.hike.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.utils.DrawerBaseActivity;
import com.bsb.hike.utils.Utils;
import com.facebook.Session;
import com.facebook.SessionState;

public class TellAFriend extends DrawerBaseActivity implements OnClickListener {

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

		View smsContainer = findViewById(R.id.sms);
		TextView smsMainText = (TextView) smsContainer
				.findViewById(R.id.item_txt);
		TextView smsSubText = (TextView) smsContainer
				.findViewById(R.id.item_subtxt);

		smsMainText
				.setText(HikeMessengerApp.isIndianUser() ? R.string.free_sms_txt
						: R.string.sms);
		smsSubText
				.setText(HikeMessengerApp.isIndianUser() ? R.string.invite_free_sms
						: R.string.invite_sms);

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
				Intent intent = new Intent(this, SocialNetInviteActivity.class);
				intent.putExtra(HikeConstants.Extras.IS_FACEBOOK, false);
				startActivity(intent);
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
			Log.d("tell a friend","calling socialNetInviteActivity");
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

}
