package com.bsb.hike.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
	private SharedPreferences settings;
	private TextView creditsMax;
	private TextView creditsCurrent;
	private ProgressBar creditsBar;

	private String[] pubSubListeners = { HikePubSub.SMS_CREDIT_CHANGED,
			HikePubSub.INVITEE_NUM_CHANGED };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		initalizeViews(savedInstanceState);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	private void initalizeViews(Bundle savedInstanceState) {
		setContentView(R.layout.credits);

		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		creditsMax = (TextView) findViewById(R.id.credits_full_txt);
		creditsCurrent = (TextView) findViewById(R.id.credits_num);
		creditsBar = (ProgressBar) findViewById(R.id.credits_progress);

		updateCredits();
		setupActionBar();
	}

	public void onInviteClick(View v) {
		Utils.logEvent(CreditsActivity.this,
				HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
		Intent intent = new Intent(CreditsActivity.this, HikeListActivity.class);
		intent.putExtra(HikeConstants.Extras.FROM_CREDITS_SCREEN, true);
		startActivity(intent);
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

		creditsMax.setText(Integer.toString(currentCredits) + "/"
				+ Integer.toString(totalCredits));
		creditsCurrent.setText(Integer.toString(currentCredits));

		creditsBar.setMax(totalCredits);
		creditsBar.setProgress(currentCredits);
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