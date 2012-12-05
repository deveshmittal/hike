package com.bsb.hike.ui;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.ConfigurationContext;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.utils.AuthSocialAccountBaseActivity;
import com.bsb.hike.utils.Utils;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class TellAFriend extends AuthSocialAccountBaseActivity implements
		OnClickListener {

	private boolean facebookPostPopupShowing = false;

	private enum ItemTypes {
		FACEBOOK, TWITTER, SMS, EMAIL, OTHER
	}

	private ViewGroup itemContainer;
	private int itemHeight = (int) (48 * Utils.densityMultiplier);
	private SharedPreferences settings;

	private String[] pubSubListeners = { HikePubSub.REMOVE_TWITTER_VIEW,
			HikePubSub.SOCIAL_AUTH_COMPLETED };

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null
				&& savedInstanceState
						.getBoolean(HikeConstants.Extras.TWITTER_VIEW_VISIBLE)) {
			startTwitterAuth(false);
			return;
		}
		setContentView(R.layout.tell_a_friend);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);

		itemContainer = (ViewGroup) findViewById(R.id.items_container);

		afterSetContentView(savedInstanceState);

		TextView viaSms = (TextView) findViewById(R.id.via_sms);

		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				HikeConstants.FREE_SMS_PREF, true)) {
			String text = getString(R.string.earn_sms_friend_join);
			String textToBold = getString(R.string.via_sms);
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			ssb.setSpan(
					new ForegroundColorSpan(getResources().getColor(
							R.color.subtext)), text.indexOf(textToBold),
					text.indexOf(textToBold) + textToBold.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			viaSms.setText(ssb);
			viaSms.setVisibility(View.VISIBLE);
		} else {
			viaSms.setVisibility(View.GONE);
		}

		TextView mTitleView = (TextView) findViewById(R.id.title_centered);
		mTitleView.setText(R.string.invite);

		int ids[] = { R.id.facebook, R.id.twitter, R.id.sms, R.id.email,
				R.id.other };
		for (int i = 0; i < ids.length; i++) {
			findViewById(ids[i]).setOnClickListener(this);
		}

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		if (savedInstanceState != null) {
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.FACEBOOK_AUTH_POPUP_SHOWING)) {
				startFBAuth(false);
			} else if (savedInstanceState
					.getBoolean(HikeConstants.Extras.FACEBOOK_POST_POPUP_SHOWING)) {
				onClick(findViewById(ItemTypes.FACEBOOK.ordinal()));
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.FACEBOOK_POST_POPUP_SHOWING,
				facebookPostPopupShowing);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.facebook:
			if (!settings.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE,
					false)) {
				startFBAuth(false);
			} else {
				facebookPostPopupShowing = true;
				Facebook facebook = HikeMessengerApp.getFacebook();
				Bundle parameters = new Bundle();
				String inviteToken = settings.getString(
						HikeConstants.INVITE_TOKEN, "");
				parameters.putString("link",
						getString(R.string.default_invite_url, inviteToken));
				parameters.putString("description",
						getString(R.string.facebook_description));
				parameters.putString("picture",
						"http://get.hike.in/images/icon%20125x125.jpg");
				facebook.dialog(this, "stream.publish", parameters,
						new DialogListener() {

							@Override
							public void onFacebookError(FacebookError e) {
								facebookPostPopupShowing = false;
								Toast.makeText(TellAFriend.this,
										R.string.fb_post_fail,
										Toast.LENGTH_SHORT).show();
								Log.e(getClass().getSimpleName(),
										"Facebook error while posting", e);
							}

							@Override
							public void onError(DialogError e) {
								facebookPostPopupShowing = false;
								Toast.makeText(TellAFriend.this,
										R.string.fb_post_fail,
										Toast.LENGTH_SHORT).show();
								Log.e(getClass().getSimpleName(),
										"Facebook error while posting", e);
							}

							@Override
							public void onComplete(Bundle values) {
								facebookPostPopupShowing = false;
							}

							@Override
							public void onCancel() {
								facebookPostPopupShowing = false;
							}
						});
			}
			break;

		case R.id.twitter:
			if (!settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE,
					false)) {
				startTwitterAuth(false);
			} else {
				new AsyncTask<Void, Void, Boolean>() {

					@Override
					protected Boolean doInBackground(Void... params) {
						String token = settings.getString(
								HikeMessengerApp.TWITTER_TOKEN, "");
						String tokenSecret = settings.getString(
								HikeMessengerApp.TWITTER_TOKEN_SECRET, "");
						String tweet = Utils.getInviteMessage(TellAFriend.this,
								R.string.twitter_msg);
						AccessToken accessToken = new AccessToken(token,
								tokenSecret);

						OAuthAuthorization authorization = new OAuthAuthorization(
								ConfigurationContext.getInstance());
						authorization.setOAuthAccessToken(accessToken);
						authorization.setOAuthConsumer(
								HikeConstants.APP_TWITTER_ID,
								HikeConstants.APP_TWITTER_SECRET);

						Twitter twitter = new TwitterFactory()
								.getInstance(authorization);
						try {
							twitter.updateStatus(tweet);
						} catch (TwitterException e) {
							Log.e(getClass().getSimpleName(),
									"Twitter Exception while updating status",
									e);
							return Boolean.FALSE;

						}
						return Boolean.TRUE;
					}

					@Override
					protected void onPostExecute(Boolean result) {
						Toast.makeText(
								TellAFriend.this,
								result ? R.string.twitter_post_success
										: R.string.twitter_post_fail,
								Toast.LENGTH_SHORT).show();
					}

				}.execute();

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
		if (HikePubSub.REMOVE_TWITTER_VIEW.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					onCreate(null);
					twitterOAuthView = null;
				}
			});
		} else if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type)) {
			final boolean facebook = (Boolean) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					onClick(findViewById(facebook ? ItemTypes.FACEBOOK
							.ordinal() : ItemTypes.TWITTER.ordinal()));
				}
			});
		}
	}
}
