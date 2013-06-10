package com.bsb.hike.ui;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.tasks.ActivityCallableTask;
import com.bsb.hike.tasks.DeleteAccountTask;
import com.bsb.hike.utils.HikeAppStateBasePreferenceActivity;
import com.bsb.hike.utils.Utils;

public class HikePreferences extends HikeAppStateBasePreferenceActivity
		implements OnPreferenceClickListener, Listener {

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

		addPreferencesFromResource(preferences);

		TextView titleView = (TextView) findViewById(R.id.title);
		titleView.setText(getTitle());

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

		Preference statusPreference = getPreferenceScreen().findPreference(
				HikeConstants.STATUS_PREF);
		if (statusPreference != null) {
			int currentValue = PreferenceManager.getDefaultSharedPreferences(
					this).getInt(HikeConstants.STATUS_PREF, 0);
			setStatusPreferenceSummary(statusPreference, currentValue);
			statusPreference.setOnPreferenceClickListener(this);
		}

		Preference smsClientPreference = getPreferenceScreen().findPreference(
				HikeConstants.RECEIVE_SMS_PREF);
		if (smsClientPreference != null) {
			HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
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
	}

	private void setStatusPreferenceSummary(Preference statusPreference,
			int currentValue) {
		if (currentValue == -1) {
			statusPreference.setSummary(R.string.off);
		} else if (currentValue == 0) {
			statusPreference.setSummary(R.string.immediate);
		} else if (currentValue == 1) {
			statusPreference.setSummary(R.string.every_hour);
		} else {
			statusPreference.setSummary(getString(R.string.every_x_hours,
					currentValue));
		}
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
					task.execute();
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
					task.execute();
				}
			});
			builder.setNegativeButton(R.string.cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		} else if (preference.getKey().equals(HikeConstants.STATUS_PREF)) {
			Builder builder = new Builder(this);
			final int currentValue = PreferenceManager
					.getDefaultSharedPreferences(this).getInt(
							HikeConstants.STATUS_PREF, 0);
			String batchValues = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getString(
					HikeMessengerApp.BATCH_STATUS_NOTIFICATION_VALUES, null);
			int currentSelection = 0;

			final ArrayList<Integer> valueList = new ArrayList<Integer>();
			final ArrayList<Integer> indexList = new ArrayList<Integer>();
			ArrayList<String> optionList = new ArrayList<String>();

			optionList.add(getString(R.string.immediate));
			valueList.add(0);
			indexList.add(0);
			if (batchValues != null) {
				try {
					JSONArray values = new JSONArray(batchValues);
					for (int i = 0; i < values.length(); i++) {
						int val = values.optInt(i);
						/*
						 * If the value if equal to the current prefernce value,
						 * we need to pre check it. The +1 is to account for the
						 * first value entered.
						 */
						if (val == currentValue) {
							currentSelection = i + 1;
						}
						if (val == 1) {
							optionList.add(getString(R.string.every_hour));
						} else {
							optionList.add(getString(R.string.every_x_hours,
									val));
						}
						valueList.add(val);
						indexList.add(i + 1);
					}
				} catch (JSONException e) {
					Log.w(getClass().getSimpleName(), "Invalid JSON Array", e);
				}
			}
			optionList.add(getString(R.string.off));
			valueList.add(-1);
			indexList.add(-1);

			if (currentValue == -1) {
				currentSelection = optionList.size() - 1;
			}
			final int finalSelection = currentSelection;

			String[] options = new String[optionList.size()];
			optionList.toArray(options);

			ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this,
					R.layout.alert_item, R.id.item, options) {

				@Override
				public View getView(int position, View convertView,
						ViewGroup parent) {
					View v = super.getView(position, convertView, parent);
					CheckBox rb = (CheckBox) v
							.findViewById(android.R.id.checkbox);
					rb.setVisibility(View.VISIBLE);
					if (position == finalSelection) {
						rb.setChecked(true);
					} else {
						rb.setChecked(false);
					}
					return v;
				}
			};

			builder.setSingleChoiceItems(dialogAdapter, currentSelection,
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							int selectedValue = valueList.get(which);
							if (selectedValue == currentValue) {
								return;
							}
							View v = (View) ((AlertDialog) dialog)
									.getListView().getAdapter()
									.getView(which, null, null);
							CheckBox rb = (CheckBox) v
									.findViewById(android.R.id.checkbox);
							rb.setChecked(true);

							setStatusPreferenceSummary(preference,
									selectedValue);
							/*
							 * Save in local preferences.
							 */
							Editor editor = PreferenceManager
									.getDefaultSharedPreferences(
											HikePreferences.this).edit();
							editor.putInt(HikeConstants.STATUS_PREF,
									selectedValue);
							editor.commit();

							/*
							 * Send value to server
							 */
							JSONObject object = new JSONObject();
							JSONObject data = new JSONObject();
							try {
								data.put(HikeConstants.PUSH_SU,
										indexList.get(which));
								object.put(HikeConstants.DATA, data);
								object.put(
										HikeConstants.TYPE,
										HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);

								HikeMessengerApp.getPubSub().publish(
										HikePubSub.MQTT_PUBLISH, object);
							} catch (JSONException e) {
								Log.w(getClass().getSimpleName(),
										"Invalid JSON", e);
							}
							dialog.dismiss();
						}
					});
			builder.show();
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
		Intent dltIntent = new Intent(this, MessagesList.class);
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
}
