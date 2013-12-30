package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.tasks.ActivityCallableTask;
import com.bsb.hike.tasks.DeleteAccountTask;
import com.bsb.hike.utils.CustomAlertDialog;
import com.bsb.hike.utils.HikeAppStateBasePreferenceActivity;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.view.IconCheckBoxPreference;
import com.facebook.Session;

public class HikePreferences extends HikeAppStateBasePreferenceActivity
		implements OnPreferenceClickListener, OnPreferenceChangeListener,
		Listener {

	private enum DialogShowing {
		SMS_SYNC_CONFIRMATION_DIALOG, SMS_SYNCING_DIALOG
	}

	private ActivityCallableTask mTask;
	ProgressDialog mDialog;
	private boolean isDeleting;

	private DialogShowing dialogShowing;

	private Dialog smsDialog;

	private String[] pubSubListeners = { HikePubSub.SHOW_SMS_SYNC_DIALOG,
			HikePubSub.SMS_SYNC_COMPLETE, HikePubSub.SMS_SYNC_FAIL,
			HikePubSub.SMS_SYNC_START };

	@Override
	public Object onRetainNonConfigurationInstance() {
		return ((mTask != null) && (!mTask.isFinished())) ? mTask : null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hikepreferences);

		Intent intent = getIntent();
		int preferences = intent.getIntExtra(HikeConstants.Extras.PREF, -1);
		int titleRes = intent.getIntExtra(HikeConstants.Extras.TITLE, 0);

		
		addPreferencesFromResource(preferences);

		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof ActivityCallableTask) {
			isDeleting = savedInstanceState != null ? savedInstanceState
					.getBoolean(HikeConstants.Extras.IS_DELETING_ACCOUNT)
					: isDeleting;
			setBlockingTask((ActivityCallableTask) retained);
			mTask.setActivity(this);
		}

		Preference deletePreference = getPreferenceScreen().findPreference(
				HikeConstants.DELETE_PREF);
		if (deletePreference != null) {
			Utils.logEvent(HikePreferences.this,
					HikeConstants.LogEvent.PRIVACY_SCREEN);
			deletePreference.setOnPreferenceClickListener(this);
		} else {
			Utils.logEvent(HikePreferences.this,
					HikeConstants.LogEvent.NOTIFICATION_SCREEN);
		}
		Preference unlinkPreference = getPreferenceScreen().findPreference(
				HikeConstants.UNLINK_PREF);
		if (unlinkPreference != null) {
			unlinkPreference.setOnPreferenceClickListener(this);
		}

		Preference unlinkFacebookPreference = getPreferenceScreen()
				.findPreference(HikeConstants.UNLINK_FB);
		if (unlinkFacebookPreference != null) {
			Session session = Session.getActiveSession();
			if (Session.getActiveSession() != null) {
				unlinkFacebookPreference.setOnPreferenceClickListener(this);
			} else {
				getPreferenceScreen()
						.removePreference(unlinkFacebookPreference);
			}
		}

		Preference unlinkTwitterPreference = getPreferenceScreen()
				.findPreference(HikeConstants.UNLINK_TWITTER);
		if (unlinkTwitterPreference != null) {
			if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
					MODE_PRIVATE).getBoolean(
					HikeMessengerApp.TWITTER_AUTH_COMPLETE, false)) {
				unlinkTwitterPreference.setOnPreferenceClickListener(this);
			} else {
				getPreferenceScreen().removePreference(unlinkTwitterPreference);
			}
		}

		final IconCheckBoxPreference smsClientPreference = (IconCheckBoxPreference) getPreferenceScreen()
				.findPreference(HikeConstants.RECEIVE_SMS_PREF);
		if (smsClientPreference != null) {
			HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
			smsClientPreference.setOnPreferenceChangeListener(this);
		}

		final IconCheckBoxPreference lastSeenPreference = (IconCheckBoxPreference) getPreferenceScreen()
				.findPreference(HikeConstants.LAST_SEEN_PREF);
		if (lastSeenPreference != null) {
			lastSeenPreference.setOnPreferenceChangeListener(this);
		}

		final IconCheckBoxPreference freeSmsPreference = (IconCheckBoxPreference) getPreferenceScreen()
				.findPreference(HikeConstants.FREE_SMS_PREF);
		if (freeSmsPreference != null) {
			freeSmsPreference.setOnPreferenceChangeListener(this);
		}

		final IconCheckBoxPreference sslPreference = (IconCheckBoxPreference) getPreferenceScreen()
				.findPreference(HikeConstants.SSL_PREF);
		if (sslPreference != null) {
			sslPreference.setOnPreferenceChangeListener(this);
		}

		Preference blockedListPreference = getPreferenceScreen()
				.findPreference(HikeConstants.BLOKED_LIST_PREF);
		if (blockedListPreference != null) {
			Log.d(getClass().getSimpleName(),
					"blockedListPreference preference not null"
							+ blockedListPreference.getKey());
			blockedListPreference.setOnPreferenceClickListener(this);
		} else {
			Log.d(getClass().getSimpleName(),
					"blockedListPreference preference is null");
		}

		Preference systemHealthPreference = getPreferenceScreen()
				.findPreference(HikeConstants.SYSTEM_HEALTH_PREF);
		if (systemHealthPreference != null) {
			Log.d(getClass().getSimpleName(),
					"systemHealthPreference preference is not null");
			systemHealthPreference.setOnPreferenceClickListener(this);
		} else {
			Log.d(getClass().getSimpleName(),
					"systemHealthPreference preference is null");
		}

		Preference helpFaqsPreference = getPreferenceScreen().findPreference(
				HikeConstants.HELP_FAQS_PREF);
		if (helpFaqsPreference != null) {
			Log.d(getClass().getSimpleName(),
					"helpFaqsPreference preference is not null"
							+ helpFaqsPreference.getKey());
			helpFaqsPreference.setOnPreferenceClickListener(this);
		} else {
			Log.d(getClass().getSimpleName(),
					"helpFaqsPreference preference is null");
		}

		Preference helpContactPreference = getPreferenceScreen()
				.findPreference(HikeConstants.HELP_FEEDBACK_PREF);
		if (helpContactPreference != null) {
			Log.d(getClass().getSimpleName(),
					"helpContactPreference preference is not null");
			helpContactPreference.setOnPreferenceClickListener(this);
		} else {
			Log.d(getClass().getSimpleName(),
					"helpContactPreference preference is null");
		}

		Preference mutePreference = getPreferenceScreen().findPreference(
				HikeConstants.STATUS_BOOLEAN_PREF);
		if (mutePreference != null) {
			mutePreference.setOnPreferenceClickListener(this);
		}

		if (savedInstanceState != null) {
			int dialogShowingOrdinal = savedInstanceState.getInt(
					HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1) {
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
				smsDialog = Utils
						.showSMSSyncDialog(
								this,
								dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG);
			}
		}
		setupActionBar(titleRes);

	}

	private void setupActionBar(int titleRes) {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

		View actionBarView = LayoutInflater.from(this).inflate(
				R.layout.compose_action_bar, null);

		View backContainer = actionBarView.findViewById(R.id.back);

		TextView title = (TextView) actionBarView.findViewById(R.id.title);
		title.setText(titleRes);
		backContainer.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(HikePreferences.this,
						SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		actionBar.setCustomView(actionBarView);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.IS_DELETING_ACCOUNT,
				isDeleting);
		if (mDialog != null && mDialog.isShowing()) {
			outState.putInt(HikeConstants.Extras.DIALOG_SHOWING,
					dialogShowing != null ? dialogShowing.ordinal() : -1);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
		if (smsDialog != null) {
			smsDialog.cancel();
			smsDialog = null;
		}
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		mTask = null;
	}

	public void setBlockingTask(ActivityCallableTask task) {
		Log.d("HikePreferences", "setting task:" + task.isFinished());
		if (!task.isFinished()) {
			mTask = task;
			mDialog = ProgressDialog.show(this, getString(R.string.account),
					isDeleting ? getString(R.string.deleting_account)
							: getString(R.string.unlinking_account));
		}
	}

	public void dismissProgressDialog() {
		if (mDialog != null) {
			mDialog.dismiss();
			mDialog = null;
		}
	}

	@Override
	public boolean onPreferenceClick(final Preference preference) {
		Log.d("HikePreferences", "Preference clicked: " + preference.getKey());
		if (preference.getKey().equals(HikeConstants.DELETE_PREF)) {
			final CustomAlertDialog confirmDialog = new CustomAlertDialog(HikePreferences.this);
			confirmDialog.setHeader(R.string.delete_account);
			confirmDialog.setBody(R.string.delete_confirmation);
			View.OnClickListener dialogOkClickListener = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					DeleteAccountTask task = new DeleteAccountTask(
							HikePreferences.this, true,getApplicationContext());
					isDeleting = true;
					setBlockingTask(task);
					Utils.executeBoolResultAsyncTask(task);
					confirmDialog.dismiss();
				}
			}; 
			
			confirmDialog.setOkButton(R.string.delete, dialogOkClickListener);
			confirmDialog.setCancelButton(R.string.cancel);
			confirmDialog.show();

		} else if (preference.getKey().equals(HikeConstants.UNLINK_PREF)) {
			final CustomAlertDialog confirmDialog = new CustomAlertDialog(HikePreferences.this);
			confirmDialog.setHeader(R.string.unlink_account);
			confirmDialog.setBody(R.string.unlink_confirmation);
			View.OnClickListener dialogOkClickListener = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					DeleteAccountTask task = new DeleteAccountTask(
							HikePreferences.this, false,getApplicationContext());
					isDeleting = false;
					setBlockingTask(task);
					Utils.executeBoolResultAsyncTask(task);
					confirmDialog.dismiss();
				}
			}; 
			
			confirmDialog.setOkButton(R.string.unlink_account, dialogOkClickListener);
			confirmDialog.setCancelButton(R.string.cancel);
			confirmDialog.show();
			
		} else if (preference.getKey().equals(HikeConstants.UNLINK_FB)) {
			final CustomAlertDialog confirmDialog = new CustomAlertDialog(HikePreferences.this);
			confirmDialog.setHeader(R.string.unlink_facebook);
			confirmDialog.setBody(R.string.unlink_facebook_confirmation);
			View.OnClickListener dialogOkClickListener = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Editor editor = getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
							.edit();
					editor.putBoolean(HikeMessengerApp.FACEBOOK_AUTH_COMPLETE,
							false);
					editor.commit();
					Session session = Session.getActiveSession();
					if (session != null) {
						session.closeAndClearTokenInformation();
						session.setActiveSession(null);
					}
					Toast.makeText(getApplicationContext(),
							R.string.social_unlink_success, Toast.LENGTH_SHORT)
							.show();
					getPreferenceScreen().removePreference(
							getPreferenceScreen().findPreference(
									HikeConstants.UNLINK_FB));
					confirmDialog.dismiss();
				}
			}; 
			
			confirmDialog.setOkButton(R.string.unlink, dialogOkClickListener);
			confirmDialog.setCancelButton(R.string.cancel);
			confirmDialog.show();
			
		} else if (preference.getKey().equals(HikeConstants.UNLINK_TWITTER)) {
			final CustomAlertDialog confirmDialog = new CustomAlertDialog(HikePreferences.this);
			confirmDialog.setHeader(R.string.unlink_twitter);
			confirmDialog.setBody(R.string.unlink_twitter_confirmation);
			View.OnClickListener dialogOkClickListener = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Editor editor = getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
							.edit();
					editor.putBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE,
							false);
					editor.putString(HikeMessengerApp.TWITTER_TOKEN, "");
					editor.putString(HikeMessengerApp.TWITTER_TOKEN_SECRET, "");
					editor.commit();

					Toast.makeText(getApplicationContext(),
							R.string.social_unlink_success, Toast.LENGTH_SHORT)
							.show();
					getPreferenceScreen().removePreference(
							getPreferenceScreen().findPreference(
									HikeConstants.UNLINK_TWITTER));
					confirmDialog.dismiss();
				}
			}; 
			
			confirmDialog.setOkButton(R.string.unlink, dialogOkClickListener);
			confirmDialog.setCancelButton(R.string.cancel);
			confirmDialog.show();
			
		} else if (HikeConstants.BLOKED_LIST_PREF.equals(preference.getKey())) {
			Intent intent = new Intent(HikePreferences.this,
					HikeListActivity.class);
			intent.putExtra(HikeConstants.Extras.BLOCKED_LIST, true);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		} else if (HikeConstants.SYSTEM_HEALTH_PREF.equals(preference.getKey())) {
			Log.d(getClass().getSimpleName(),
					"system health preference selected");
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(HikeConstants.SYSTEM_HEALTH_URL));
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(getApplicationContext(),
						R.string.system_health_error, Toast.LENGTH_SHORT)
						.show();
			}
		} else if (HikeConstants.HELP_FAQS_PREF.equals(preference.getKey())) {
			Log.d(getClass().getSimpleName(), "FAQ preference selected");
			Intent intent = new Intent(HikePreferences.this,
					WebViewActivity.class);
			intent.putExtra(HikeConstants.Extras.URL_TO_LOAD,
					HikeConstants.HELP_URL);
			intent.putExtra(HikeConstants.Extras.TITLE, getString(R.string.faq));
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		} else if (HikeConstants.HELP_FEEDBACK_PREF.equals(preference.getKey())) {
			Log.d(getClass().getSimpleName(), "contact preference selected");
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(Uri.parse("mailto:" + HikeConstants.MAIL));

			StringBuilder message = new StringBuilder("\n\n");

			try {
				message.append(getString(R.string.hike_version)
						+ " "
						+ getPackageManager().getPackageInfo(getPackageName(),
								0).versionName + "\n");
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			message.append(getString(R.string.device_name) + " "
					+ Build.MANUFACTURER + " " + Build.MODEL + "\n");

			message.append(getString(R.string.android_version) + " "
					+ Build.VERSION.RELEASE + "\n");

			String msisdn = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(
					HikeMessengerApp.MSISDN_SETTING, "");
			message.append(getString(R.string.msisdn) + " " + msisdn);

			intent.putExtra(Intent.EXTRA_TEXT, message.toString());
			intent.putExtra(Intent.EXTRA_SUBJECT,
					getString(R.string.feedback_on_hike));
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(getApplicationContext(), R.string.email_error,
						Toast.LENGTH_SHORT).show();
			}
		} else if (HikeConstants.STATUS_BOOLEAN_PREF
				.equals(preference.getKey())) {
			SharedPreferences settingPref = PreferenceManager
					.getDefaultSharedPreferences(this);
			int statusIntPreference = settingPref.getInt(HikeConstants.STATUS_PREF, 0);

			int newValue;

			Editor editor = settingPref.edit();
			if (statusIntPreference == 0) {
				newValue = -1;
				editor.putInt(HikeConstants.STATUS_PREF, newValue);
			} else {
				newValue = 0;
				editor.putInt(HikeConstants.STATUS_PREF, newValue);
			}
			editor.commit();

			try {
				JSONObject jsonObject = new JSONObject();
				JSONObject data = new JSONObject();
				data.put(HikeConstants.PUSH_SU, newValue);
				jsonObject.put(HikeConstants.DATA, data);
				jsonObject.put(HikeConstants.TYPE,
						HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
						jsonObject);

			} catch (JSONException e) {
				Log.w(getClass().getSimpleName(), e);
			}
		}

		return true;
	}

	/**
	 * For redirecting back to the Welcome Screen.
	 */
	public void accountDeleted() {
		dismissProgressDialog();
		/*
		 * First we send the user to the Main Activity(MessagesList) from there
		 * we redirect him to the welcome screen.
		 */
		Intent dltIntent = new Intent(this, HomeActivity.class);
		dltIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(dltIntent);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.SHOW_SMS_SYNC_DIALOG.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					smsDialog = Utils.showSMSSyncDialog(HikePreferences.this,
							true);
					dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION_DIALOG;
				}
			});
		} else if (HikePubSub.SMS_SYNC_COMPLETE.equals(type)
				|| HikePubSub.SMS_SYNC_FAIL.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (smsDialog != null) {
						smsDialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		} else if (HikePubSub.SMS_SYNC_START.equals(type)) {
			dialogShowing = DialogShowing.SMS_SYNCING_DIALOG;
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean isChecked = (Boolean) newValue;
		((IconCheckBoxPreference) preference).setChecked(isChecked);

		if (HikeConstants.RECEIVE_SMS_PREF.equals(preference.getKey())) {
			Utils.sendDefaultSMSClientLogEvent(isChecked);

			if (!isChecked) {
				Editor editor = PreferenceManager.getDefaultSharedPreferences(
						HikePreferences.this).edit();
				editor.putBoolean(HikeConstants.SEND_SMS_PREF, false);
				editor.commit();
			} else {
				if (!HikePreferences.this.getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(
						HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false)) {
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.SHOW_SMS_SYNC_DIALOG, null);
				}
			}
		} else if (HikeConstants.LAST_SEEN_PREF.equals(preference.getKey())) {
			JSONObject object = new JSONObject();
			try {
				object.put(HikeConstants.TYPE,
						HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);

				JSONObject data = new JSONObject();
				data.put(HikeConstants.LAST_SEEN_SETTING, isChecked);

				object.put(HikeConstants.DATA, data);

				HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
						object);
			} catch (JSONException e) {
				Log.w(getClass().getSimpleName(), "Invalid json", e);
			}
		} else if (HikeConstants.FREE_SMS_PREF.equals(preference.getKey())) {
			Log.d(getClass().getSimpleName(), "Free SMS toggled");
			HikeMessengerApp.getPubSub().publish(HikePubSub.FREE_SMS_TOGGLED,
					isChecked);

			Utils.sendFreeSmsLogEvent(isChecked);
		} else if (HikeConstants.SSL_PREF.equals(preference.getKey())) {
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.SWITCHED_DATA_CONNECTION, null);
		}
		return false;
	}

}
