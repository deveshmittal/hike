package com.bsb.hike.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.AuthSocialAccountBaseActivity;
import com.bsb.hike.utils.Utils;

public class CreditsActivity extends AuthSocialAccountBaseActivity implements
		Listener {
	private ViewGroup creditsContainer;
	private SharedPreferences settings;
	private TextView freeSms50;
	private TextView freeSms100;
	private TextView creditsMax;
	private TextView creditsCurrent;
	private ProgressBar creditsBar;
	private ViewGroup facebookBtn;
	private ViewGroup twitterBtn;
	private TextView facebookTxt;
	private TextView twitterTxt;

	/* Width of the container in which the credit view will slide */
	private int creditProgressBarWidth;
	/* Width of the view that shows the current credit number */
	private int creditNumWidth;

	private ProgressDialog dialog;

	/*
	 * Offset for the number of credits/(100 total credits) will come in the
	 * curved part of the progress bar
	 */
	private final static int OFFSET_PORTRAIT = 3;
	private final static int OFFSET_LANDSCAPE = 1;

	private int currentOffset;

	private int creditNumContainerWidth;

	private AlertDialog alertDialog;

	private DeleteSocialCredentialsTask deleteSocialCredentialsTask;

	private String[] pubSubListeners = { HikePubSub.SMS_CREDIT_CHANGED,
			HikePubSub.INVITEE_NUM_CHANGED, HikePubSub.SOCIAL_AUTH_COMPLETED };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		currentOffset = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? OFFSET_PORTRAIT
				: OFFSET_LANDSCAPE;

		initalizeViews(savedInstanceState);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private void initalizeViews(Bundle savedInstanceState) {
		setContentView(R.layout.credits);

		Object o = getLastCustomNonConfigurationInstance();
		if (o instanceof DeleteSocialCredentialsTask) {
			dialog = ProgressDialog.show(this, null,
					getString(R.string.social_unlinking));
			((DeleteSocialCredentialsTask) o).setDialog(dialog);
		}

		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		creditsContainer = (ViewGroup) findViewById(R.id.credits_container);
		freeSms50 = (TextView) findViewById(R.id.free_sms);
		freeSms100 = (TextView) findViewById(R.id.free_sms_100);
		creditsMax = (TextView) findViewById(R.id.credits_full_txt);
		creditsCurrent = (TextView) findViewById(R.id.credits_num);
		creditsBar = (ProgressBar) findViewById(R.id.credits_progress);
		facebookBtn = (ViewGroup) findViewById(R.id.btn_fb);
		twitterBtn = (ViewGroup) findViewById(R.id.btn_twitter);
		facebookTxt = (TextView) findViewById(R.id.facebook_txt);
		twitterTxt = (TextView) findViewById(R.id.twitter_txt);

		creditNumWidth = getResources().getDimensionPixelSize(
				R.dimen.credits_num_view_width);

		int totalProgressBarMargin = getResources().getDimensionPixelSize(
				R.dimen.credits_main_margin)
				+ getResources().getDimensionPixelSize(
						R.dimen.credits_progress_margin)
				+ getResources().getDimensionPixelSize(
						R.dimen.credits_progress_layout_padding)
				+ getResources().getDimensionPixelSize(
						R.dimen.credits_progress_curve_width);
		creditProgressBarWidth = getResources().getDisplayMetrics().widthPixels
				- (2 * totalProgressBarMargin);

		int totalCreditNumContainerMargin = getResources()
				.getDimensionPixelSize(R.dimen.credits_main_margin)
				+ getResources().getDimensionPixelSize(
						R.dimen.credits_num_container_margin)
				+ getResources().getDimensionPixelSize(
						R.dimen.credits_progress_layout_padding);
		creditNumContainerWidth = getResources().getDisplayMetrics().widthPixels
				- (2 * totalCreditNumContainerMargin);

		String freeSmsString = getString(R.string.invite_friend_free_sms);
		String textToColor = getString(R.string.free_sms_50);
		int startIndex = freeSmsString.indexOf(textToColor);

		SpannableStringBuilder ssb = new SpannableStringBuilder(freeSmsString);
		if (startIndex != -1) {
			ssb.setSpan(
					new ForegroundColorSpan(getResources().getColor(
							R.color.unread_message_blue)), startIndex,
					startIndex + textToColor.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		freeSms50.setText(ssb);

		updateCredits();
		setupSocialButtons();
	}

	private void setupSocialButtons() {
		if (settings.contains(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE)) {
			Editor editor = settings.edit();
			editor.putBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE,
					HikeMessengerApp.getFacebook().isSessionValid());
			editor.commit();
		}

		facebookTxt
				.setText(settings.getBoolean(
						HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false) ? R.string.connected
						: R.string.facebook);
		facebookTxt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fb,
				0, settings.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE,
						false) ? R.drawable.ic_white_tick : 0, 0);
		facebookBtn
				.setBackgroundResource(settings.getBoolean(
						HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false) ? R.drawable.bg_fb_btn_pressed
						: R.drawable.fb_btn);

		twitterTxt
				.setText(settings.getBoolean(
						HikeMessengerApp.TWITTER_AUTH_COMPLETE, false) ? R.string.connected
						: R.string.twitter);
		twitterTxt.setCompoundDrawablesWithIntrinsicBounds(
				R.drawable.ic_twitter, 0,
				settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE,
						false) ? R.drawable.ic_white_tick : 0, 0);
		twitterBtn
				.setBackgroundResource(settings.getBoolean(
						HikeMessengerApp.TWITTER_AUTH_COMPLETE, false) ? R.drawable.bg_twitter_btn_pressed
						: R.drawable.twitter_btn);
	}

	public void onInviteClick(View v) {
		Utils.logEvent(CreditsActivity.this,
				HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
		startActivity(new Intent(CreditsActivity.this, HikeListActivity.class));
	}

	public void onFacebookClick(View v) {
		if (!settings
				.getBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE, false)) {
			startFBAuth(true);
		} else {
			showCredentialUnlinkAlert(true);
		}
	}

	public void onTwitterClick(View v) {
		if (!settings.getBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE, false)) {
			Intent intent = new Intent(this, TwitterAuthActivity.class);
			intent.putExtra(HikeConstants.Extras.POST_TO_TWITTER, true);
			startActivity(intent);
		} else {
			showCredentialUnlinkAlert(false);
		}
	}

	private void showCredentialUnlinkAlert(final boolean facebook) {
		Builder builder = new Builder(CreditsActivity.this);
		builder.setMessage(facebook ? R.string.confirm_unlink_fb
				: R.string.confirm_unlink_twitter);
		builder.setPositiveButton(R.string.unlink, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				deleteSocialCredentialsTask = new DeleteSocialCredentialsTask();
				deleteSocialCredentialsTask.execute(facebook);
			}
		});
		builder.setNegativeButton(R.string.cancel, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		alertDialog = builder.create();
		alertDialog.show();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		if (deleteSocialCredentialsTask != null) {
			return deleteSocialCredentialsTask;
		}
		return super.onRetainNonConfigurationInstance();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		HikeMessengerApp.getFacebook().authorizeCallback(requestCode,
				resultCode, data);
	}

	@Override
	protected void onDestroy() {
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
		deleteSocialCredentialsTask = null;
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, Object object) {
		/*
		 * Here we check if we are already showing the twitter webview. If we
		 * are, we dont do any other UI changes.
		 */
		if ((twitterOAuthView == null)
				&& (HikePubSub.SMS_CREDIT_CHANGED.equals(type) || HikePubSub.INVITEE_NUM_CHANGED
						.equals(type))) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateCredits();
				}
			});
		} else if (HikePubSub.SOCIAL_AUTH_COMPLETED.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					setupSocialButtons();
				}
			});
		}
	}

	private void updateCredits() {
		int currentCredits = settings.getInt(HikeMessengerApp.SMS_SETTING, 0);
		int totalCredits = Integer.parseInt(settings.getString(
				HikeMessengerApp.TOTAL_CREDITS_PER_MONTH, "100"));

		int actualOffset = (int) ((currentOffset * totalCredits) / 100);

		creditsMax.setText(totalCredits + "+");
		creditsCurrent.setText(currentCredits + "");

		creditsBar.setMax(totalCredits);
		creditsBar.setProgress(currentCredits);

		int paddingLeft;
		if (currentCredits <= actualOffset) {
			paddingLeft = 0;
		} else if (currentCredits >= totalCredits - actualOffset) {
			paddingLeft = creditNumContainerWidth - creditNumWidth;
		} else {
			int creditsForContainer = currentCredits - actualOffset;
			paddingLeft = (int) ((creditsForContainer * creditProgressBarWidth) / totalCredits);
		}
		creditsContainer.setPadding(paddingLeft, 0, 0, 0);
	}

	private class DeleteSocialCredentialsTask extends
			AsyncTask<Boolean, Void, Boolean> {
		ProgressDialog taskDialog;

		public void setDialog(ProgressDialog progressDialog) {
			taskDialog = progressDialog;
		}

		@Override
		protected void onPreExecute() {
			taskDialog = dialog = ProgressDialog.show(CreditsActivity.this,
					null, getString(R.string.social_unlinking));
		}

		@Override
		protected Boolean doInBackground(Boolean... params) {
			boolean facebook = params[0];
			Editor editor = settings.edit();
			try {
				AccountUtils.deleteSocialCredentials(facebook);
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
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(),
						"Exception while deleting credentials", e);
				return false;
			} finally {
				editor.commit();
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (taskDialog != null) {
				taskDialog.dismiss();
				taskDialog = null;
			}
			Toast.makeText(
					CreditsActivity.this,
					result ? R.string.social_unlink_success
							: R.string.unlink_account_failed,
					Toast.LENGTH_SHORT).show();
			setupSocialButtons();
			deleteSocialCredentialsTask = null;
		}
	}
}