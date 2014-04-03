package com.bsb.hike.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;

public class CreditsActivity extends HikeAppStateBaseFragmentActivity implements Listener
{
	private enum DialogShowing
	{
		SMS_SYNC_CONFIRMATION_DIALOG, SMS_SYNCING_DIALOG
	}

	private SharedPreferences settings;

	private TextView creditsCurrent;

	private String[] pubSubListeners = { HikePubSub.SMS_CREDIT_CHANGED, HikePubSub.INVITEE_NUM_CHANGED, HikePubSub.SHOW_SMS_SYNC_DIALOG, HikePubSub.SMS_SYNC_COMPLETE,
			HikePubSub.SMS_SYNC_FAIL, HikePubSub.SMS_SYNC_START };

	private DialogShowing dialogShowing;

	private Dialog smsDialog;

	private View freeSmsContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		initalizeViews(savedInstanceState);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);

		if (savedInstanceState != null)
		{
			int dialogShowingOrdinal = savedInstanceState.getInt(HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1)
			{
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
				smsDialog = Utils.showSMSSyncDialog(this, dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		if (smsDialog != null && smsDialog.isShowing())
		{
			outState.putInt(HikeConstants.Extras.DIALOG_SHOWING, dialogShowing != null ? dialogShowing.ordinal() : -1);
		}
		super.onSaveInstanceState(outState);
	}

	private void initalizeViews(Bundle savedInstanceState)
	{
		setContentView(R.layout.credits);

		Editor editor = settings.edit();
		editor.putBoolean(HikeMessengerApp.INVITE_TOOLTIP_DISMISSED, true);
		editor.commit();

		creditsCurrent = (TextView) findViewById(R.id.credits_num);

		updateCredits();
		setupActionBar();

		freeSmsContainer = findViewById(R.id.free_sms_details);
		freeSmsContainer.setVisibility(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(HikeConstants.FREE_SMS_PREF, true) ? View.VISIBLE : View.GONE);

		View receiveSmsParent = findViewById(R.id.receive_sms_item);
		View freeSMSParent = findViewById(R.id.free_sms_item);

		setupPreferenceLayout(receiveSmsParent, true);
		setupPreferenceLayout(freeSMSParent, false);

		receiveSmsParent.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				boolean isChecked = togglePreference(HikeConstants.RECEIVE_SMS_PREF, (CheckBox) v.findViewById(R.id.receive_sms_checkbox));

				Utils.sendDefaultSMSClientLogEvent(isChecked);

				if (isChecked)
				{
					if (!CreditsActivity.this.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
					{
						HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
					}
				}
			}
		});

		freeSMSParent.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				boolean isChecked = togglePreference(HikeConstants.FREE_SMS_PREF, (CheckBox) v.findViewById(R.id.free_sms_checkbox));

				freeSmsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);

				HikeMessengerApp.getPubSub().publish(HikePubSub.FREE_SMS_TOGGLED, isChecked);

				Utils.sendFreeSmsLogEvent(isChecked);
			}
		});
	}

	private boolean togglePreference(String key, CheckBox checkBox)
	{
		checkBox.toggle();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		Editor editor = preferences.edit();
		editor.putBoolean(key, checkBox.isChecked());
		editor.commit();

		return checkBox.isChecked();
	}

	private void setupPreferenceLayout(View parent, boolean receiveSmsPref)
	{
		TextView title = (TextView) parent.findViewById(receiveSmsPref ? R.id.receive_sms_title : R.id.free_sms_title);
		TextView summary = (TextView) parent.findViewById(receiveSmsPref ? R.id.receive_sms_summary : R.id.free_sms_summary);
		ImageView icon = (ImageView) parent.findViewById(receiveSmsPref ? R.id.receive_sms_icon : R.id.free_sms_icon);
		CheckBox checkBox = (CheckBox) parent.findViewById(receiveSmsPref ? R.id.receive_sms_checkbox : R.id.free_sms_checkbox);

		title.setText(receiveSmsPref ? R.string.default_client_header : R.string.free_sms);
		summary.setText(receiveSmsPref ? R.string.default_client_info : R.string.free_sms_msg);
		icon.setImageResource(receiveSmsPref ? R.drawable.preference_default_client : R.drawable.preference_free_sms);
		checkBox.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(receiveSmsPref ? HikeConstants.RECEIVE_SMS_PREF : HikeConstants.FREE_SMS_PREF,
				receiveSmsPref ? false : true));
	}

	public void onInviteClick(View v)
	{
		Utils.logEvent(CreditsActivity.this, HikeConstants.LogEvent.INVITE_BUTTON_CLICKED);
		Utils.sendUILogEvent(HikeConstants.LogEvent.INVITE_SMS_SCREEN_FROM_CREDIT);
		Intent intent = new Intent(CreditsActivity.this, HikeListActivity.class);
		intent.putExtra(HikeConstants.Extras.FROM_CREDITS_SCREEN, true);
		startActivity(intent);
	}

	public void onStartHikingClick(View v)
	{
		Utils.sendUILogEvent(HikeConstants.LogEvent.START_HIKING);
		Intent intent = new Intent(this, ComposeChatActivity.class);
		startActivity(intent);
	}

	@Override
	protected void onDestroy()
	{
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);

		if (smsDialog != null)
		{
			smsDialog.cancel();
			smsDialog = null;
		}

		super.onDestroy();
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		/*
		 * Here we check if we are already showing the twitter webview. If we are, we dont do any other UI changes.
		 */
		if ((HikePubSub.SMS_CREDIT_CHANGED.equals(type) || HikePubSub.INVITEE_NUM_CHANGED.equals(type)))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					updateCredits();
				}
			});
		}
		else if (HikePubSub.SHOW_SMS_SYNC_DIALOG.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					smsDialog = Utils.showSMSSyncDialog(CreditsActivity.this, true);
					dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG;
				}
			});
		}
		else if (HikePubSub.SMS_SYNC_COMPLETE.equals(type) || HikePubSub.SMS_SYNC_FAIL.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (smsDialog != null)
					{
						smsDialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		}
		else if (HikePubSub.SMS_SYNC_START.equals(type))
		{
			dialogShowing = DialogShowing.SMS_SYNCING_DIALOG;
		}

	}

	private void updateCredits()
	{
		int currentCredits = settings.getInt(HikeMessengerApp.SMS_SETTING, 0);

		creditsCurrent.setText(Integer.toString(currentCredits));
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(R.string.free_sms_txt);
		backContainer.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(CreditsActivity.this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}

		});

		actionBar.setCustomView(actionBarView);
	}

}