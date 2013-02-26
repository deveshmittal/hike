package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.tasks.DownloadImageTask;
import com.bsb.hike.tasks.DownloadImageTask.ImageDownloadResult;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.State;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.fiksu.asotracking.FiksuTrackingManager;

public class SignupActivity extends Activity implements
		SignupTask.OnSignupTaskProgressUpdate, OnEditorActionListener,
		OnClickListener, FinishableEvent, OnCancelListener,
		DialogInterface.OnClickListener, Listener {

	private SignupTask mTask;

	private StateValue mCurrentState;

	private ViewFlipper viewFlipper;
	private ViewGroup numLayout;
	private ViewGroup pinLayout;
	private ViewGroup nameLayout;
	private ViewGroup booBooLayout;

	private TextView infoTxt;
	private TextView loadingText;
	private ViewGroup loadingLayout;
	private EditText enterEditText;
	private Button tapHereText;
	private Button submitBtn;
	private TextView invalidNum;
	private ImageView errorImage;
	private Button countryPicker;
	private Button callmeBtn;
	private ImageView mIconView;

	private Button tryAgainBtn;
	private Handler mHandler;

	private boolean addressBookError = false;
	private boolean msisdnErrorDuringSignup = false;

	private final int NAME = 2;
	private final int PIN = 1;
	private final int NUMBER = 0;

	private String[] countryNamesAndCodes;

	private String[] countryISOAndCodes;

	private String countryCode;

	private final String defaultCountryCode = "IN +91";

	private boolean showingSecondLoadingTxt = false;

	private SharedPreferences accountPrefs;

	private ActivityState mActivityState;

	private Dialog dialog;

	private Session.StatusCallback statusCallback = new SessionStatusCallback();

	private CountDownTimer countDownTimer;

	private class ActivityState {
		public HikeHTTPTask task; /* the task to update the global profile */
		public DownloadImageTask downloadImageTask; /*
													 * the task to download the
													 * picasa image
													 */

		public String destFilePath = null;

		public Bitmap profileBitmap = null; /*
											 * the bitmap before the user saves
											 * it
											 */
		public String userName = null;

		public long timeLeft = 0;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.signup);

		mHandler = new Handler();

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				MODE_PRIVATE);

		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		numLayout = (ViewGroup) findViewById(R.id.num_layout);
		pinLayout = (ViewGroup) findViewById(R.id.pin_layout);
		nameLayout = (ViewGroup) findViewById(R.id.name_layout);
		booBooLayout = (ViewGroup) findViewById(R.id.boo_boo_layout);
		tryAgainBtn = (Button) findViewById(R.id.btn_try_again);
		errorImage = (ImageView) findViewById(R.id.error_img);

		Object o = getLastNonConfigurationInstance();
		if (o instanceof ActivityState) {
			mActivityState = (ActivityState) o;
			if (mActivityState.task != null) {
				mActivityState.task.setActivity(this);
				dialog = ProgressDialog.show(this, null,
						getString(R.string.calling_you));
				dialog.setCancelable(true);
				dialog.setOnCancelListener(this);
			} else if (mActivityState.downloadImageTask != null) {
				dialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.downloading_image));
			}
		} else {
			mActivityState = new ActivityState();
		}

		if (savedInstanceState != null) {
			msisdnErrorDuringSignup = savedInstanceState
					.getBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR);
			int dispChild = savedInstanceState
					.getInt(HikeConstants.Extras.SIGNUP_PART);
			showingSecondLoadingTxt = savedInstanceState
					.getBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT);
			removeAnimation();
			viewFlipper.setDisplayedChild(dispChild);
			switch (dispChild) {
			case NUMBER:
				countryCode = savedInstanceState
						.getString(HikeConstants.Extras.COUNTRY_CODE);
				prepareLayoutForFetchingNumber();
				if (showingSecondLoadingTxt) {
					loadingText.setText(R.string.almost_there_signup);
				}
				break;
			case PIN:
				prepareLayoutForGettingPin(mActivityState.timeLeft);
				break;
			case NAME:
				prepareLayoutForGettingName(savedInstanceState);
				break;
			}
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING)) {
				startLoading();
			}
			if (savedInstanceState
					.getBoolean(HikeConstants.Extras.SIGNUP_ERROR)) {
				showErrorMsg();
			}
			enterEditText.setText(savedInstanceState
					.getString(HikeConstants.Extras.SIGNUP_TEXT));
		} else {
			if (getIntent().getBooleanExtra(HikeConstants.Extras.MSISDN, false)) {
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName(savedInstanceState);
			} else {
				prepareLayoutForFetchingNumber();
			}

		}
		setAnimation();
		setListeners();
		mTask = SignupTask.startTask(this);

		AnimationDrawable ad = new AnimationDrawable();
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large0), 600);
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large1), 600);
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large2), 600);
		ad.setOneShot(false);
		ad.setVisible(true, true);

		errorImage.setImageDrawable(ad);
		ad.start();

		HikeMessengerApp.getPubSub().addListener(
				HikePubSub.FACEBOOK_IMAGE_DOWNLOADED, this);
	}

	@Override
	public void onFinish(boolean success) {
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
		if (mActivityState.task == null) {
			if (success) {
				/*
				 * Setting the app value as to if the user is Indian or not.
				 */
				String countryCode = accountPrefs.getString(
						HikeMessengerApp.COUNTRY_CODE, "");
				HikeMessengerApp.setIndianUser(HikeConstants.INDIA_COUNTRY_CODE
						.equals(countryCode));

				Editor accountEditor = accountPrefs.edit();
				accountEditor.putBoolean(HikeMessengerApp.JUST_SIGNED_UP, true);
				accountEditor.commit();

				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(this);
				Editor editor = prefs.edit();
				editor.putBoolean(HikeConstants.FREE_SMS_PREF,
						HikeMessengerApp.isIndianUser());
				editor.putBoolean(HikeConstants.SSL_PREF,
						!HikeMessengerApp.isIndianUser());
				editor.remove(HikeMessengerApp.TEMP_COUNTRY_CODE);
				editor.commit();

				/*
				 * Update the urls to use ssl or not.
				 */
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.SWITCHED_DATA_CONNECTION, null);

				if (!HikeMessengerApp.isIndianUser()) {
					FiksuTrackingManager.initialize(getApplication());
				}
				// Tracking the registration event for Fiksu
				FiksuTrackingManager.uploadRegistrationEvent(this, "");

				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						Intent i = new Intent(SignupActivity.this,
								MessagesList.class);
						startActivity(i);
						finish();
					}
				}, 2500);
			} else if (mCurrentState != null && mCurrentState.value != null
					&& mCurrentState.value.equals(HikeConstants.CHANGE_NUMBER)) {
				restartTask();
			}
		} else {
			mActivityState = new ActivityState();
		}
	}

	public void onClick(View v) {
		if (v.getId() == submitBtn.getId()) {
			submitClicked();
		} else if (v.getId() == tryAgainBtn.getId()) {
			restartTask();
		} else if (tapHereText != null && v.getId() == tapHereText.getId()) {
			if (countDownTimer != null) {
				countDownTimer.cancel();
			}
			mTask.addUserInput("");
		} else if (callmeBtn != null && v.getId() == callmeBtn.getId()) {
			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/pin-call",
					new HikeHttpRequest.HikeHttpCallback() {
						public void onFailure() {
						}

						public void onSuccess(JSONObject response) {
						}
					});
			JSONObject request = new JSONObject();
			try {
				request.put("msisdn", accountPrefs.getString(
						HikeMessengerApp.MSISDN_ENTERED, null));
			} catch (JSONException e) {
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
			hikeHttpRequest.setJSONData(request);

			mActivityState.task = new HikeHTTPTask(this, R.string.call_me_fail,
					false);
			mActivityState.task.execute(hikeHttpRequest);

			dialog = ProgressDialog.show(this, null,
					getResources().getString(R.string.calling_you));
			dialog.setCancelable(true);
			dialog.setOnCancelListener(this);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mActivityState;
	}

	protected void onDestroy() {
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListener(
				HikePubSub.FACEBOOK_IMAGE_DOWNLOADED, this);
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
		if (countDownTimer != null) {
			countDownTimer.cancel();
			countDownTimer = null;
		}
	}

	private void startLoading() {
		loadingLayout.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.GONE);
		if (invalidNum != null) {
			invalidNum.setVisibility(View.GONE);
		}
		if (tapHereText != null) {
			tapHereText.setVisibility(View.GONE);
		}
		if (countryPicker != null) {
			countryPicker.setEnabled(false);
		}
		if (callmeBtn != null) {
			callmeBtn.setVisibility(View.GONE);
		}
	}

	private void submitClicked() {
		if (TextUtils.isEmpty(enterEditText.getText())) {
			int displayedChild = viewFlipper.getDisplayedChild();
			int stringRes;
			if (displayedChild == NUMBER) {
				stringRes = R.string.enter_num;
			} else if (displayedChild == PIN) {
				stringRes = R.string.enter_pin;
			} else {
				stringRes = R.string.enter_name;
			}
			Toast toast = Toast.makeText(this, stringRes, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			return;
		}
		if (viewFlipper.getDisplayedChild() != NUMBER) {
			startLoading();
		}
		if (!addressBookError) {
			if (viewFlipper.getDisplayedChild() == NUMBER
					&& !enterEditText.getText().toString()
							.matches(HikeConstants.VALID_MSISDN_REGEX)) {
				loadingLayout.setVisibility(View.GONE);
				submitBtn.setVisibility(View.VISIBLE);
				invalidNum.setVisibility(View.VISIBLE);
			} else {
				final String input = enterEditText.getText().toString();
				if (viewFlipper.getDisplayedChild() == NUMBER) {
					String codeAndIso = countryPicker.getText().toString();
					final String code = codeAndIso.substring(
							codeAndIso.indexOf("+"), codeAndIso.length());

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.number_confirm_title);
					builder.setMessage(code + input);
					builder.setPositiveButton(R.string.confirm,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									String number = code + input;
									Editor editor = accountPrefs.edit();
									editor.putString(
											HikeMessengerApp.TEMP_COUNTRY_CODE,
											code);
									editor.commit();

									mTask.addUserInput(number);

									startLoading();
								}
							});
					builder.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
					builder.show();
				} else {
					if (!TextUtils.isEmpty(mActivityState.destFilePath)) {
						mTask.addProfilePicPath(mActivityState.destFilePath,
								mActivityState.profileBitmap);
					}
					mTask.addUserInput(input);
				}
			}
		} else {
			showErrorMsg();
			addressBookError = false;
		}
	}

	private void initializeViews(ViewGroup layout) {
		switch (layout.getId()) {
		case R.id.name_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_name);
			break;
		case R.id.num_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_num);
			break;
		case R.id.pin_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_pin);
			break;
		}
		infoTxt = (TextView) layout.findViewById(R.id.txt_img1);
		loadingText = (TextView) layout.findViewById(R.id.txt_loading);
		loadingLayout = (ViewGroup) layout.findViewById(R.id.loading_layout);
		tapHereText = (Button) layout.findViewById(R.id.wrong_num);
		submitBtn = (Button) layout.findViewById(R.id.btn_continue);
		invalidNum = (TextView) layout.findViewById(R.id.invalid_num);
		countryPicker = (Button) layout.findViewById(R.id.country_picker);
		callmeBtn = (Button) layout.findViewById(R.id.btn_call_me);
		mIconView = (ImageView) layout.findViewById(R.id.profile);

		loadingLayout.setVisibility(View.GONE);
		submitBtn.setVisibility(View.VISIBLE);
	}

	private void prepareLayoutForFetchingNumber() {
		initializeViews(numLayout);

		countryPicker.setEnabled(true);

		String prevCode = accountPrefs.getString(
				HikeMessengerApp.TEMP_COUNTRY_CODE, "");
		countryNamesAndCodes = getResources().getStringArray(
				R.array.country_names_and_codes);
		countryISOAndCodes = getResources().getStringArray(
				R.array.country_iso_and_codes);

		if (TextUtils.isEmpty(countryCode)) {
			TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String countryIso = TextUtils.isEmpty(prevCode) ? manager
					.getNetworkCountryIso().toUpperCase() : prevCode;
			for (String s : countryISOAndCodes) {
				if (!TextUtils.isEmpty(countryIso) && s.contains(countryIso)) {
					Log.d(getClass().getSimpleName(), "COUNTRY CODE: " + s);
					countryCode = s;
					break;
				}
			}
			countryPicker
					.setText(TextUtils.isEmpty(countryCode) ? defaultCountryCode
							: countryCode);
		} else {
			countryPicker.setText(countryCode);
		}
		formatCountryPickerText(countryPicker.getText().toString());

		infoTxt.setText(msisdnErrorDuringSignup ? R.string.enter_phone_again_signup
				: R.string.enter_num_signup);
		invalidNum.setVisibility(View.INVISIBLE);
		loadingText.setText(R.string.verifying_num_signup);
	}

	private void formatCountryPickerText(String code) {
		if (countryPicker == null) {
			return;
		}
		SpannableStringBuilder ssb = new SpannableStringBuilder(code);
		ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, code.indexOf("+"),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		countryPicker.setText(ssb);
		countryCode = code;
	}

	public void onCountryPickerClick(View v) {

		AlertDialog.Builder builder = new AlertDialog.Builder(
				SignupActivity.this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				countryNamesAndCodes) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);

				String text = tv.getText().toString();
				SpannableStringBuilder spannable = new SpannableStringBuilder(
						text);
				spannable.setSpan(new ForegroundColorSpan(getResources()
						.getColor(R.color.country_code)), text.indexOf("+"),
						text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				tv.setText(spannable);

				return v;
			}
		};

		builder.setAdapter(dialogAdapter,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						formatCountryPickerText(countryISOAndCodes[which]);
					}
				});

		AlertDialog dialog = builder.create();
		dialog.getListView().setFastScrollEnabled(true);
		dialog.show();
	}

	private void prepareLayoutForGettingPin(long timeLeft) {
		initializeViews(pinLayout);

		callmeBtn.setVisibility(View.VISIBLE);

		enterEditText.setText("");
		infoTxt.setText(R.string.enter_pin_signup);
		tapHereText.setOnClickListener(this);

		String tapHere = getString(R.string.tap_here_signup);
		String tapHereString = getString(R.string.wrong_num_signup);

		SpannableStringBuilder ssb = new SpannableStringBuilder(tapHereString);
		ssb.setSpan(new ForegroundColorSpan(0xff6edcff),
				tapHereString.indexOf(tapHere), tapHereString.indexOf(tapHere)
						+ tapHere.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		tapHereText.setText(ssb);

		if (timeLeft > 0) {
			countDownTimer = new CountDownTimer(timeLeft, 1000) {

				@Override
				public void onTick(long millisUntilFinished) {
					long secondsUntilFinished = millisUntilFinished / 1000;
					int minutes = (int) (secondsUntilFinished / 60);
					int seconds = (int) (secondsUntilFinished % 60);
					String text = String.format("%1$02d:%2$02d", minutes,
							seconds);
					callmeBtn.setText(text);
					mActivityState.timeLeft = millisUntilFinished;
					callmeBtn.setEnabled(false);
				}

				@Override
				public void onFinish() {
					callmeBtn.setText(R.string.call_me_signup);
					callmeBtn.setEnabled(true);
					mActivityState.timeLeft = 0;
				}
			};
			countDownTimer.start();
		} else {
			callmeBtn.setText(R.string.call_me_signup);
			callmeBtn.setEnabled(true);
		}
	}

	private void prepareLayoutForGettingName(Bundle savedInstanceState) {
		initializeViews(nameLayout);

		Session session = Session.getActiveSession();
		if (session == null) {
			if (savedInstanceState != null) {
				session = Session.restoreSession(this, null, statusCallback,
						savedInstanceState);
			}
			if (session == null) {
				session = new Session(this);
			}
			Session.setActiveSession(session);
			if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
				session.openForRead(new Session.OpenRequest(this)
						.setCallback(statusCallback));
			}
		}

		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(enterEditText.getWindowToken(), 0);

		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING,
				null);
		if (TextUtils.isEmpty(msisdn)) {
			Utils.logEvent(SignupActivity.this,
					HikeConstants.LogEvent.SIGNUP_ERROR);
			msisdnErrorDuringSignup = true;
			resetViewFlipper();
			restartTask();
			return;
		}
		String nameText = getString(R.string.all_set_signup, msisdn);
		SpannableStringBuilder ssb = new SpannableStringBuilder(nameText);
		ssb.setSpan(new StyleSpan(Typeface.BOLD), nameText.indexOf(msisdn),
				nameText.indexOf(msisdn) + msisdn.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		infoTxt.setText(ssb);

		if (mActivityState.profileBitmap == null) {
			mIconView.setImageDrawable(IconCacheManager.getInstance()
					.getIconForMSISDN(msisdn));
		} else {
			mIconView.setImageBitmap(mActivityState.profileBitmap);
		}
	}

	private void resetViewFlipper() {
		booBooLayout.setVisibility(View.GONE);
		viewFlipper.setVisibility(View.VISIBLE);
		removeAnimation();
		viewFlipper.setDisplayedChild(NUMBER);
		prepareLayoutForFetchingNumber();
		setAnimation();
	}

	private void restartTask() {
		resetViewFlipper();
		mTask = SignupTask.restartTask(this);
	}

	private void showErrorMsg() {
		loadingLayout.setVisibility(View.GONE);
		submitBtn.setVisibility(View.VISIBLE);
		booBooLayout.setVisibility(View.VISIBLE);
		viewFlipper.setVisibility(View.GONE);
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(enterEditText.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private void setListeners() {
		enterEditText.setOnEditorActionListener(this);
		enterEditText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				return loadingLayout.getVisibility() == View.VISIBLE
						&& (event == null || event.getKeyCode() != KeyEvent.KEYCODE_BACK);
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Session session = Session.getActiveSession();
		Session.saveSession(session, outState);

		outState.putInt(HikeConstants.Extras.SIGNUP_PART,
				viewFlipper.getDisplayedChild());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING,
				loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR,
				booBooLayout.getVisibility() == View.VISIBLE);
		outState.putString(HikeConstants.Extras.SIGNUP_TEXT, enterEditText
				.getText().toString());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR,
				msisdnErrorDuringSignup);
		outState.putBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT,
				showingSecondLoadingTxt);
		if (viewFlipper.getDisplayedChild() == NUMBER) {
			outState.putString(HikeConstants.Extras.COUNTRY_CODE, countryPicker
					.getText().toString());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		if (mTask != null) {
			mTask.cancelTask();
			mTask = null;
		}
		SignupTask.isAlreadyFetchingNumber = false;
		super.onBackPressed();
	}

	private void removeAnimation() {
		viewFlipper.setInAnimation(null);
		viewFlipper.setOutAnimation(null);
	}

	private void setAnimation() {
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.slide_in_right));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.slide_out_left));
	}

	@Override
	public void onProgressUpdate(StateValue stateValue) {
		/*
		 * Making sure the countdown timer doesn't keep running when the state
		 * values changes.
		 */
		if (countDownTimer != null && stateValue.state != State.PIN) {
			countDownTimer.cancel();
		}
		String value = stateValue.value;
		mCurrentState = stateValue;
		Log.d("SignupActivity", "Current State " + mCurrentState.state.name()
				+ " VALUE: " + value);

		showingSecondLoadingTxt = false;
		switch (stateValue.state) {
		case MSISDN:
			if (TextUtils.isEmpty(value)) {
				prepareLayoutForFetchingNumber();
			} else if (value.equals(HikeConstants.DONE)) {
				removeAnimation();
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName(null);
				setAnimation();
			} else {
				/* yay, got the actual MSISDN */
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName(null);
			}
			break;
		case PULLING_PIN:
			if (viewFlipper.getDisplayedChild() == NUMBER) {
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (viewFlipper != null
								&& viewFlipper.getDisplayedChild() == NUMBER) {
							showingSecondLoadingTxt = true;
							loadingText.setText(R.string.almost_there_signup);
						}
					}
				}, HikeConstants.PIN_CAPTURE_TIME / 2);
			}
			break;
		case PIN:
			viewFlipper.setDisplayedChild(PIN);

			// Wrong Pin
			if (value != null && value.equals(HikeConstants.PIN_ERROR)) {
				prepareLayoutForGettingPin(mActivityState.timeLeft);
				infoTxt.setText(R.string.wrong_pin_signup);
				loadingLayout.setVisibility(View.GONE);
				callmeBtn.setVisibility(View.VISIBLE);
				submitBtn.setVisibility(View.VISIBLE);
				if (tapHereText != null) {
					tapHereText.setVisibility(View.VISIBLE);
				}
				enterEditText.setText("");
			}
			// Manual entry for pin
			else {
				prepareLayoutForGettingPin(HikeConstants.CALL_ME_WAIT_TIME);
				setAnimation();
			}
			break;
		case NAME:
			if (TextUtils.isEmpty(value)) {
				prepareLayoutForGettingName(null);
			}
			break;
		case PROFILE_IMAGE:
			if (SignupTask.START_UPLOAD_PROFILE.equals(value)) {
				mHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						loadingText.setText(R.string.setting_profile);
					}
				}, 500);
			} else if (SignupTask.FINISHED_UPLOAD_PROFILE.equals(value)) {
				mHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						loadingText.setText(R.string.getting_in_signup);
					}
				}, 500);
			}
			break;
		case ERROR:
			if (value != null && value.equals(HikeConstants.ADDRESS_BOOK_ERROR)) {
				addressBookError = true;
				if (loadingLayout.getVisibility() == View.VISIBLE) {
					showErrorMsg();
				}
			} else if (value == null
					|| !value.equals(HikeConstants.CHANGE_NUMBER)) {
				showErrorMsg();
			}
			break;
		}
		setListeners();
	}

	@Override
	public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
		if ((actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				&& !TextUtils
						.isEmpty(enterEditText.getText().toString().trim())
				&& loadingLayout.getVisibility() != View.VISIBLE) {
			submitClicked();
		}
		return true;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		Log.d(getClass().getSimpleName(), "Dialog cancelled");
		if (mActivityState.task != null) {
			mActivityState.task.setActivity(null);
			mActivityState = new ActivityState();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Session session = Session.getActiveSession();
		if (session != null) {
			session.addCallback(statusCallback);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		Session session = Session.getActiveSession();
		if (session != null) {
			session.removeCallback(statusCallback);
		}
	}

	boolean fbClicked = false;

	public void onFacebookConnectClick(View v) {
		fbClicked = true;
		Session session = Session.getActiveSession();
		Log.d(getClass().getSimpleName(), "FB CLICKED");
		if (!session.isOpened() && !session.isClosed()) {
			session.openForRead(new Session.OpenRequest(this)
					.setCallback(statusCallback));
			Log.d(getClass().getSimpleName(), "Opening for read");
		} else {
			Session.openActiveSession(this, true, statusCallback);
			Log.d(getClass().getSimpleName(), "Opening active session");
		}
	}

	private class SessionStatusCallback implements Session.StatusCallback {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			if (fbClicked && session.isOpened()) {
				updateView();
				fbClicked = false;
			}
		}
	}

	public void updateView() {
		Session session = Session.getActiveSession();
		if (session.isOpened()) {
			Request.executeMeRequestAsync(session, new GraphUserCallback() {
				@Override
				public void onCompleted(final GraphUser user, Response response) {
					if (user != null) {
						final String fbProfileUrl = String
								.format(HikeConstants.FACEBOOK_PROFILEPIC_URL_FORMAT,
										user.getId(),
										HikeConstants.MAX_DIMENSION_FULL_SIZE_PROFILE_PX);

						String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
								+ HikeConstants.PROFILE_ROOT;
						String fileName = Utils.getTempProfileImageFileName(accountPrefs
								.getString(HikeMessengerApp.MSISDN_SETTING, ""));

						final File destFile = new File(directory, fileName);
						mActivityState.downloadImageTask = new DownloadImageTask(
								getApplicationContext(), destFile, Uri
										.parse(fbProfileUrl),
								new ImageDownloadResult() {

									@Override
									public void downloadFinished(boolean result) {
										mActivityState = new ActivityState();
										if (!result) {
											Toast.makeText(
													getApplicationContext(),
													R.string.fb_fetch_image_error,
													Toast.LENGTH_SHORT).show();
										} else {
											mActivityState.destFilePath = destFile
													.getPath();
											mActivityState.userName = user
													.getName();
										}
										HikeMessengerApp
												.getPubSub()
												.publish(
														HikePubSub.FACEBOOK_IMAGE_DOWNLOADED,
														result);
									}
								});
						mActivityState.downloadImageTask.execute();
						dialog = ProgressDialog.show(
								SignupActivity.this,
								null,
								getResources().getString(
										R.string.fetching_info));
					}
				}
			});
		}
	}

	public void onChangeImageClicked(View v) {
		/*
		 * The wants to change their profile picture. Open a dialog to allow
		 * them pick Camera or Gallery
		 */
		final CharSequence[] items = getResources().getStringArray(
				R.array.profile_pic_dialog);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.choose_picture);
		builder.setItems(items, this);
		builder.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int item) {
		Intent intent = null;
		switch (item) {
		case HikeConstants.PROFILE_PICTURE_FROM_CAMERA:
			if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE) {
				Toast.makeText(getApplicationContext(),
						R.string.no_external_storage, Toast.LENGTH_SHORT)
						.show();
				return;
			}
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File selectedFileIcon = Utils.getOutputMediaFile(
					HikeFileType.PROFILE, null, null); // create a file to save
														// the image
			if (selectedFileIcon != null) {
				intent.putExtra(MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(selectedFileIcon));

				/*
				 * Saving the file path. Will use this to get the file once the
				 * image has been captured.
				 */
				Editor editor = accountPrefs.edit();
				editor.putString(HikeMessengerApp.FILE_PATH,
						selectedFileIcon.getAbsolutePath());
				editor.commit();

				startActivityForResult(intent, HikeConstants.CAMERA_RESULT);
				overridePendingTransition(R.anim.slide_in_right_noalpha,
						R.anim.slide_out_left_noalpha);
			} else {
				Toast.makeText(this, getString(R.string.no_sd_card),
						Toast.LENGTH_LONG).show();
			}
			break;
		case HikeConstants.PROFILE_PICTURE_FROM_GALLERY:
			if (Utils.getExternalStorageState() == ExternalStorageState.NONE) {
				Toast.makeText(getApplicationContext(),
						R.string.no_external_storage, Toast.LENGTH_SHORT)
						.show();
				return;
			}
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, HikeConstants.GALLERY_RESULT);
			overridePendingTransition(R.anim.slide_in_right_noalpha,
					R.anim.slide_out_left_noalpha);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		String path = null;
		if (resultCode != RESULT_OK) {
			return;
		}

		Session session = Session.getActiveSession();
		session.onActivityResult(this, requestCode, resultCode, data);
		if (fbClicked) {
			onFacebookConnectClick(null);
		}

		File selectedFileIcon;
		boolean isPicasaImage = false;
		Uri selectedFileUri = null;

		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT
				+ HikeConstants.PROFILE_ROOT;
		String fileName = Utils.getTempProfileImageFileName(accountPrefs
				.getString(HikeMessengerApp.MSISDN_SETTING, ""));
		final String destFilePath = directory + "/" + fileName;

		switch (requestCode) {
		case HikeConstants.CAMERA_RESULT:
			/* fall-through on purpose */
		case HikeConstants.GALLERY_RESULT:
			Log.d("ProfileActivity", "The activity is " + this);
			if (requestCode == HikeConstants.CAMERA_RESULT) {
				String filePath = accountPrefs.getString(
						HikeMessengerApp.FILE_PATH, "");
				selectedFileIcon = new File(filePath);

				/*
				 * Removing this key. We no longer need this.
				 */
				Editor editor = accountPrefs.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
				if (!selectedFileIcon.exists()) {
					Toast.makeText(getApplicationContext(),
							R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				} else {
					path = selectedFileIcon.getAbsolutePath();
				}
			} else {
				selectedFileUri = data.getData();
				if (Utils.isPicasaUri(selectedFileUri.toString())) {
					isPicasaImage = true;
					path = Utils.getOutputMediaFile(HikeFileType.PROFILE, null,
							null).getAbsolutePath();
				} else {
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart)) {
						selectedFileIcon = new File(URI.create(fileUriString));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						path = selectedFileIcon.getAbsolutePath();
					} else {
						path = Utils.getRealPathFromUri(selectedFileUri, this);
					}
				}
			}
			if (TextUtils.isEmpty(path)) {
				Toast.makeText(getApplicationContext(), R.string.error_capture,
						Toast.LENGTH_SHORT).show();
				return;
			}
			if (!isPicasaImage) {
				Utils.startCropActivity(this, path, destFilePath);
			} else {
				final File destFile = new File(path);
				mActivityState.downloadImageTask = new DownloadImageTask(
						getApplicationContext(), destFile, selectedFileUri,
						new ImageDownloadResult() {

							@Override
							public void downloadFinished(boolean result) {
								if (dialog != null) {
									dialog.dismiss();
									dialog = null;
								}
								mActivityState = new ActivityState();
								if (!result) {
									Toast.makeText(getApplicationContext(),
											R.string.error_download,
											Toast.LENGTH_SHORT).show();
								} else {
									Utils.startCropActivity(
											SignupActivity.this,
											destFile.getAbsolutePath(),
											destFilePath);
								}
							}
						});
				mActivityState.downloadImageTask.execute();
				dialog = ProgressDialog.show(this, null, getResources()
						.getString(R.string.downloading_image));
			}
			break;
		case HikeConstants.CROP_RESULT:
			mActivityState.destFilePath = data
					.getStringExtra(MediaStore.EXTRA_OUTPUT);
			setProfileImage();
			break;
		}
	}

	private void setProfileImage() {
		if (mIconView == null) {
			return;
		}
		mActivityState.profileBitmap = Utils.scaleDownImage(
				mActivityState.destFilePath,
				HikeConstants.PROFILE_IMAGE_DIMENSIONS, true);
		mIconView.setImageBitmap(mActivityState.profileBitmap);
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.FACEBOOK_IMAGE_DOWNLOADED.equals(type)) {
			final boolean result = (Boolean) object;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (dialog != null) {
						dialog.dismiss();
						dialog = null;
					}
					if (!result) {
						return;
					}
					setProfileImage();

					enterEditText.setText(mActivityState.userName);
					enterEditText
							.setSelection(mActivityState.userName.length());
				}
			});
		}
	}
}