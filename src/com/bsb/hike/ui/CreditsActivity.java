package com.bsb.hike.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class CreditsActivity extends HikeAppStateBaseFragmentActivity implements
		Listener {
	private ViewGroup creditsContainer;
	private SharedPreferences settings;
	private TextView creditsMax;
	private TextView creditsCurrent;
	private ProgressBar creditsBar;

	/* Width of the container in which the credit view will slide */
	private int creditProgressBarWidth;
	/* Width of the view that shows the current credit number */
	private int creditNumWidth;

	/*
	 * Offset for the number of credits/(100 total credits) will come in the
	 * curved part of the progress bar
	 */
	private final static int OFFSET_PORTRAIT = 3;
	private final static int OFFSET_LANDSCAPE = 1;

	private int currentOffset;

	private int creditNumContainerWidth;

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

		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		creditsContainer = (ViewGroup) findViewById(R.id.credits_container);
		creditsMax = (TextView) findViewById(R.id.credits_full_txt);
		creditsCurrent = (TextView) findViewById(R.id.credits_num);
		creditsBar = (ProgressBar) findViewById(R.id.credits_progress);

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

		updateCredits();
		setupActionBar();
	}

	public void onInviteClick(View v) {
		Utils.logEvent(CreditsActivity.this,
				HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
		startActivity(new Intent(CreditsActivity.this, HikeListActivity.class));
	}

	@Override
	protected void onDestroy() {
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, Object object) {
		/*
		 * Here we check if we are already showing the twitter webview. If we
		 * are, we dont do any other UI changes.
		 */
		if ((HikePubSub.SMS_CREDIT_CHANGED.equals(type) || HikePubSub.INVITEE_NUM_CHANGED
				.equals(type))) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateCredits();
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

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(
				R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.free_sms_txt);
		backContainer.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(CreditsActivity.this,
						HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}

		});

		actionBar.setCustomView(actionBarView);
	}

}