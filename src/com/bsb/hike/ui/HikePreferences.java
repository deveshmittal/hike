package com.bsb.hike.ui;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.models.GroupConversation;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.ActivityCallableTask;
import com.bsb.hike.tasks.DeleteAccountTask;
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
				getString(R.string.delete_key));
		if (deletePreference != null) {
			Utils.logEvent(HikePreferences.this,
					HikeConstants.LogEvent.PRIVACY_SCREEN);
			deletePreference.setOnPreferenceClickListener(this);
		} else {
			Utils.logEvent(HikePreferences.this,
					HikeConstants.LogEvent.NOTIFICATION_SCREEN);
		}
		Preference unlinkPreference = getPreferenceScreen().findPreference(
				getString(R.string.unlink_key));
		if (unlinkPreference != null) {
			unlinkPreference.setOnPreferenceClickListener(this);
		}

		Preference unlinkFacebookPreference = getPreferenceScreen()
				.findPreference(getString(R.string.unlink_facebook));
		if (unlinkFacebookPreference != null) {
			unlinkFacebookPreference.setOnPreferenceClickListener(this);
		}

		Preference unlinkTwitterPreference = getPreferenceScreen()
				.findPreference(getString(R.string.unlink_twitter));
		if (unlinkTwitterPreference != null) {
			unlinkTwitterPreference.setOnPreferenceClickListener(this);
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
				Intent intent = new Intent(HikePreferences.this, HomeActivity.class);
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
		outState.putInt(HikeConstants.Extras.DIALOG_SHOWING,
				dialogShowing != null ? dialogShowing.ordinal() : -1);
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
		if (preference.getKey().equals(getString(R.string.delete_key))) {
			Builder builder = new Builder(HikePreferences.this);
			builder.setMessage(R.string.delete_confirmation);
			builder.setPositiveButton(R.string.delete, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					DeleteAccountTask task = new DeleteAccountTask(
							HikePreferences.this, true);
					isDeleting = true;
					setBlockingTask(task);
					Utils.executeBoolResultAsyncTask(task);
				}
			});
			builder.setNegativeButton(R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		} else if (preference.getKey().equals(getString(R.string.unlink_key))) {
			Builder builder = new Builder(HikePreferences.this);
			builder.setMessage(R.string.unlink_confirmation);
			builder.setPositiveButton(R.string.unlink, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					DeleteAccountTask task = new DeleteAccountTask(
							HikePreferences.this, false);
					isDeleting = false;
					setBlockingTask(task);
					Utils.executeBoolResultAsyncTask(task);
				}
			});
			builder.setNegativeButton(R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		} else if (preference.getKey().equals(
				getString(R.string.unlink_facebook))) {
			Builder builder = new Builder(HikePreferences.this);
			builder.setMessage(R.string.unlink_facebook_confirmation);
			builder.setPositiveButton(R.string.unlink, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Session session = Session.getActiveSession();
					if (session != null) {
						session.closeAndClearTokenInformation();
					}
				}
			});
			builder.setNegativeButton(R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		} else if (preference.getKey().equals(
				getString(R.string.unlink_twitter))) {
			Builder builder = new Builder(HikePreferences.this);
			builder.setMessage(R.string.unlink_twitter_confirmation);
			builder.setPositiveButton(R.string.unlink, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Editor editor = getSharedPreferences(
							HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE)
							.edit();
					editor.putBoolean(HikeMessengerApp.TWITTER_AUTH_COMPLETE,
							false);
					editor.putString(HikeMessengerApp.TWITTER_TOKEN, "");
					editor.putString(HikeMessengerApp.TWITTER_TOKEN_SECRET, "");
					editor.commit();
				}
			});
			builder.setNegativeButton(R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
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
