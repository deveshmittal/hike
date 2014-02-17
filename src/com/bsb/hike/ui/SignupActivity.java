package com.bsb.hike.ui;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
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
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
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
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.State;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class SignupActivity extends HikeAppStateBaseFragmentActivity implements SignupTask.OnSignupTaskProgressUpdate, OnEditorActionListener, OnClickListener, FinishableEvent,
		OnCancelListener, DialogInterface.OnClickListener, Listener
{

	private SignupTask mTask;

	private StateValue mCurrentState;

	private ViewFlipper viewFlipper;

	private ViewGroup numLayout;

	private ViewGroup pinLayout;

	private ViewGroup nameLayout;

	private TextView header;

	private TextView infoTxt;

	private TextView loadingText;

	private ViewGroup loadingLayout;

	private EditText enterEditText;

	private Button tapHereText;

	private Button submitBtn;

	private TextView invalidNum;

	private Button countryPicker;

	private Button callmeBtn;

	private ImageView mIconView;

	private TextView birthdayText;

	private TextView maleText;

	private TextView femaleText;

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

	private boolean showingNumberConfimationDialog;

	private class ActivityState
	{
		public HikeHTTPTask task; /* the task to update the global profile */

		public Thread downloadImageTask; /*
										 * the task to download the picasa image
										 */

		public String destFilePath = null;

		public Bitmap profileBitmap = null; /*
											 * the bitmap before the user saves it
											 */

		public String userName = null;

		public long timeLeft = 0;

		public boolean fbConnected = false;

		public boolean isFemale = false;

		public Birthday birthday = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.signup);

		mHandler = new Handler();

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		header = (TextView) findViewById(R.id.header);
		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		numLayout = (ViewGroup) findViewById(R.id.num_layout);
		pinLayout = (ViewGroup) findViewById(R.id.pin_layout);
		nameLayout = (ViewGroup) findViewById(R.id.name_layout);

		Object o = getLastCustomNonConfigurationInstance();
		if (o instanceof ActivityState)
		{
			mActivityState = (ActivityState) o;
			if (mActivityState.task != null)
			{
				mActivityState.task.setActivity(this);
				dialog = ProgressDialog.show(this, null, getString(R.string.calling_you));
				dialog.setCancelable(true);
				dialog.setOnCancelListener(this);
			}
			else if (mActivityState.downloadImageTask != null && mActivityState.downloadImageTask.getState() != java.lang.Thread.State.TERMINATED)
			{
				dialog = ProgressDialog.show(this, null, getResources().getString(R.string.downloading_image));
			}
		}
		else
		{
			mActivityState = new ActivityState();
		}

		if (savedInstanceState != null)
		{
			msisdnErrorDuringSignup = savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR);
			int dispChild = savedInstanceState.getInt(HikeConstants.Extras.SIGNUP_PART);
			showingSecondLoadingTxt = savedInstanceState.getBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT);
			removeAnimation();
			viewFlipper.setDisplayedChild(dispChild);
			switch (dispChild)
			{
			case NUMBER:
				countryCode = savedInstanceState.getString(HikeConstants.Extras.COUNTRY_CODE);
				prepareLayoutForFetchingNumber();
				if (showingSecondLoadingTxt)
				{
					loadingText.setText(R.string.almost_there_signup);
				}
				break;
			case PIN:
				prepareLayoutForGettingPin(mActivityState.timeLeft);
				break;
			case NAME:
				prepareLayoutForGettingName(savedInstanceState, false);
				break;
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING))
			{
				startLoading();
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_ERROR))
			{
				showErrorMsg();
			}
			enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
		}
		else
		{
			if (getIntent().getBooleanExtra(HikeConstants.Extras.MSISDN, false))
			{
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName(savedInstanceState, false);
			}
			else
			{
				prepareLayoutForFetchingNumber();
			}

		}
		setAnimation();
		setListeners();
		mTask = SignupTask.startTask(this);

		HikeMessengerApp.getPubSub().addListener(HikePubSub.FACEBOOK_IMAGE_DOWNLOADED, this);
	}

	@Override
	public void onFinish(boolean success)
	{
		if (dialog != null)
		{
			dialog.dismiss();
			dialog = null;
		}
		if (mActivityState.task == null)
		{
			if (success)
			{
				/*
				 * Setting the app value as to if the user is Indian or not.
				 */
				String countryCode = accountPrefs.getString(HikeMessengerApp.COUNTRY_CODE, "");
				StickerManager.setStickersForIndianUsers(HikeConstants.INDIA_COUNTRY_CODE.equals(countryCode), accountPrefs);

				Editor accountEditor = accountPrefs.edit();
				accountEditor.putBoolean(HikeMessengerApp.JUST_SIGNED_UP, true);
				accountEditor.putBoolean(HikeMessengerApp.FB_SIGNUP, true);
				accountEditor.commit();

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = prefs.edit();
				editor.putBoolean(HikeConstants.FREE_SMS_PREF, HikeMessengerApp.isIndianUser());
				editor.putBoolean(HikeConstants.SSL_PREF, !HikeMessengerApp.isIndianUser());
				editor.remove(HikeMessengerApp.TEMP_COUNTRY_CODE);
				editor.commit();

				/*
				 * Update the urls to use ssl or not.
				 */
				HikeMessengerApp.getPubSub().publish(HikePubSub.SWITCHED_DATA_CONNECTION, null);

				mHandler.removeCallbacks(startWelcomeScreen);
				mHandler.postDelayed(startWelcomeScreen, 2500);

			}
			else if (mCurrentState != null && mCurrentState.value != null && mCurrentState.value.equals(HikeConstants.CHANGE_NUMBER))
			{
				restartTask();
			}
		}
		else
		{
			mActivityState = new ActivityState();
		}
	}

	Runnable startWelcomeScreen = new Runnable()
	{
		@Override
		public void run()
		{
			Intent i = new Intent(SignupActivity.this, HomeActivity.class);
			i.putExtra(HikeConstants.Extras.NEW_USER, true);
			startActivity(i);
			finish();
		}
	};

	public void onClick(View v)
	{
		if (v.getId() == submitBtn.getId())
		{
			submitClicked();
		}
		else if (v.getId() == tryAgainBtn.getId())
		{
			restartTask();
			/*
			 * Delaying this by 100 ms to allow the signup task to setup to the last input point.
			 */
			this.mHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					submitClicked();
				}
			}, 100);
		}
		else if (tapHereText != null && v.getId() == tapHereText.getId())
		{
			if (countDownTimer != null)
			{
				countDownTimer.cancel();
			}
			mTask.addUserInput("");
		}
		else if (callmeBtn != null && v.getId() == callmeBtn.getId())
		{
			HikeHttpRequest hikeHttpRequest = new HikeHttpRequest("/pin-call", RequestType.OTHER, new HikeHttpRequest.HikeHttpCallback()
			{
				public void onFailure()
				{
				}

				public void onSuccess(JSONObject response)
				{
				}
			});
			JSONObject request = new JSONObject();
			try
			{
				request.put("msisdn", accountPrefs.getString(HikeMessengerApp.MSISDN_ENTERED, null));
			}
			catch (JSONException e)
			{
				Log.e(getClass().getSimpleName(), "Invalid JSON", e);
			}
			hikeHttpRequest.setJSONData(request);

			mActivityState.task = new HikeHTTPTask(this, R.string.call_me_fail, false);
			Utils.executeHttpTask(mActivityState.task, hikeHttpRequest);

			dialog = ProgressDialog.show(this, null, getResources().getString(R.string.calling_you));
			dialog.setCancelable(true);
			dialog.setOnCancelListener(this);
		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return mActivityState;
	}

	protected void onDestroy()
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.FACEBOOK_IMAGE_DOWNLOADED, this);
		if (dialog != null)
		{
			dialog.dismiss();
			dialog = null;
		}
		if (countDownTimer != null)
		{
			countDownTimer.cancel();
			countDownTimer = null;
		}
	}

	private void startLoading()
	{
		loadingLayout.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.GONE);
		if (invalidNum != null)
		{
			invalidNum.setVisibility(View.GONE);
		}
		if (tapHereText != null)
		{
			tapHereText.setVisibility(View.GONE);
		}
		if (countryPicker != null)
		{
			countryPicker.setEnabled(false);
		}
		if (callmeBtn != null)
		{
			callmeBtn.setVisibility(View.GONE);
		}
	}

	private void submitClicked()
	{
		if (TextUtils.isEmpty(enterEditText.getText()))
		{
			int displayedChild = viewFlipper.getDisplayedChild();
			int stringRes;
			if (displayedChild == NUMBER)
			{
				stringRes = R.string.enter_num;
			}
			else if (displayedChild == PIN)
			{
				stringRes = R.string.enter_pin;
			}
			else
			{
				stringRes = R.string.enter_name;
			}
			Toast toast = Toast.makeText(this, stringRes, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			return;
		}
		if (viewFlipper.getDisplayedChild() != NUMBER)
		{
			startLoading();
		}
		if (!addressBookError)
		{
			if (viewFlipper.getDisplayedChild() == NUMBER && !enterEditText.getText().toString().matches(HikeConstants.VALID_MSISDN_REGEX))
			{
				loadingLayout.setVisibility(View.GONE);
				submitBtn.setVisibility(View.VISIBLE);
				invalidNum.setVisibility(View.VISIBLE);
			}
			else
			{
				final String input = enterEditText.getText().toString();
				if (viewFlipper.getDisplayedChild() == NUMBER)
				{
					/*
					 * Adding this check since some device's IME call this part of the code twice.
					 */
					if (showingNumberConfimationDialog)
					{
						return;
					}

					String codeAndIso = countryPicker.getText().toString();
					final String code = codeAndIso.substring(codeAndIso.indexOf("+"), codeAndIso.length());
					String number = code + input;

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.number_confirm_title);
					builder.setMessage(getString(R.string.number_confirmation_string, number));
					builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String number = code + input;
							Editor editor = accountPrefs.edit();
							editor.putString(HikeMessengerApp.TEMP_COUNTRY_CODE, code);
							editor.commit();

							mTask.addUserInput(number);

							startLoading();
							dialog.cancel();
						}
					});
					builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.cancel();
						}
					});
					Dialog dialog = builder.show();
					dialog.setOnCancelListener(new OnCancelListener()
					{

						@Override
						public void onCancel(DialogInterface dialog)
						{
							showingNumberConfimationDialog = false;
						}
					});
					showingNumberConfimationDialog = true;
				}
				else
				{
					if (!TextUtils.isEmpty(mActivityState.destFilePath))
					{
						mTask.addProfilePicPath(mActivityState.destFilePath, mActivityState.profileBitmap);
					}
					if (viewFlipper.getDisplayedChild() == NAME)
					{
						Utils.hideSoftKeyboard(this, enterEditText);
					}
					mTask.addUserInput(input);
				}
			}
		}
		else
		{
			showErrorMsg();
			addressBookError = false;
		}
	}

	private void initializeViews(ViewGroup layout)
	{
		switch (layout.getId())
		{
		case R.id.name_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_name);
			birthdayText = (TextView) layout.findViewById(R.id.birthday);
			maleText = (TextView) layout.findViewById(R.id.male);
			femaleText = (TextView) layout.findViewById(R.id.female);
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
		invalidNum = (TextView) layout.findViewById(R.id.invalid_num);
		countryPicker = (Button) layout.findViewById(R.id.country_picker);
		callmeBtn = (Button) layout.findViewById(R.id.btn_call_me);
		mIconView = (ImageView) layout.findViewById(R.id.profile);
		tryAgainBtn = (Button) layout.findViewById(R.id.btn_try_again);
		submitBtn = (Button) findViewById(R.id.btn_continue);

		loadingLayout.setVisibility(View.GONE);
		submitBtn.setVisibility(View.VISIBLE);
	}

	private void prepareLayoutForFetchingNumber()
	{
		initializeViews(numLayout);

		header.setText(R.string.phone);

		countryPicker.setEnabled(true);

		String prevCode = accountPrefs.getString(HikeMessengerApp.TEMP_COUNTRY_CODE, "");
		countryNamesAndCodes = getResources().getStringArray(R.array.country_names_and_codes);
		countryISOAndCodes = getResources().getStringArray(R.array.country_iso_and_codes);

		if (TextUtils.isEmpty(countryCode))
		{
			TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String countryIso = TextUtils.isEmpty(prevCode) ? manager.getNetworkCountryIso().toUpperCase() : prevCode;
			for (String s : countryISOAndCodes)
			{
				if (!TextUtils.isEmpty(countryIso) && s.contains(countryIso))
				{
					Log.d(getClass().getSimpleName(), "COUNTRY CODE: " + s);
					countryCode = s;
					break;
				}
			}
			countryPicker.setText(TextUtils.isEmpty(countryCode) ? defaultCountryCode : countryCode);
		}
		else
		{
			countryPicker.setText(countryCode);
		}
		formatCountryPickerText(countryPicker.getText().toString());

		infoTxt.setText(msisdnErrorDuringSignup ? R.string.enter_phone_again_signup : R.string.enter_num_signup);
		invalidNum.setVisibility(View.INVISIBLE);
		loadingText.setText(R.string.verifying_num_signup);
	}

	private void formatCountryPickerText(String code)
	{
		if (countryPicker == null)
		{
			return;
		}
		SpannableStringBuilder ssb = new SpannableStringBuilder(code);
		ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, code.indexOf("+"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		countryPicker.setText(ssb);
		countryCode = code;
	}

	public void onCountryPickerClick(View v)
	{

		AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);

		ListAdapter dialogAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.select_dialog_item, android.R.id.text1, countryNamesAndCodes)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				TextView tv = (TextView) v.findViewById(android.R.id.text1);

				String text = tv.getText().toString();
				SpannableStringBuilder spannable = new SpannableStringBuilder(text);
				spannable.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.country_code)), text.indexOf("+"), text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				tv.setText(spannable);

				return v;
			}
		};

		builder.setAdapter(dialogAdapter, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				formatCountryPickerText(countryISOAndCodes[which]);
			}
		});

		AlertDialog dialog = builder.create();
		dialog.getListView().setFastScrollEnabled(true);
		dialog.show();
	}

	private void prepareLayoutForGettingPin(long timeLeft)
	{
		initializeViews(pinLayout);

		header.setText(R.string.verify);

		callmeBtn.setVisibility(View.VISIBLE);

		enterEditText.setText("");
		infoTxt.setText(R.string.enter_pin_signup);
		tapHereText.setOnClickListener(this);

		String tapHere = getString(R.string.tap_here_signup);
		String tapHereString = getString(R.string.wrong_num_signup);

		SpannableStringBuilder ssb = new SpannableStringBuilder(tapHereString);
		if (tapHereString.indexOf(tapHere) != -1)
		{
			ssb.setSpan(new ForegroundColorSpan(0xff6edcff), tapHereString.indexOf(tapHere), tapHereString.indexOf(tapHere) + tapHere.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		tapHereText.setText(ssb);

		if (timeLeft > 0)
		{
			countDownTimer = new CountDownTimer(timeLeft, 1000)
			{

				@Override
				public void onTick(long millisUntilFinished)
				{
					long secondsUntilFinished = millisUntilFinished / 1000;
					int minutes = (int) (secondsUntilFinished / 60);
					int seconds = (int) (secondsUntilFinished % 60);
					String text = String.format("%1$02d:%2$02d", minutes, seconds);
					callmeBtn.setText(text);
					mActivityState.timeLeft = millisUntilFinished;
					callmeBtn.setEnabled(false);
				}

				@Override
				public void onFinish()
				{
					callmeBtn.setText(R.string.call_me_signup);
					callmeBtn.setEnabled(true);
					mActivityState.timeLeft = 0;
				}
			};
			countDownTimer.start();
		}
		else
		{
			callmeBtn.setText(R.string.call_me_signup);
			callmeBtn.setEnabled(true);
		}
	}

	private void prepareLayoutForGettingName(Bundle savedInstanceState, boolean addressBookScanningDone)
	{
		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		/*
		 * Making sure we create the profile picture folder before the user sets one.
		 */
		File dir = new File(directory);
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		initializeViews(nameLayout);

		setupNameViewForGender();
		if (mActivityState.birthday != null)
		{
			onDateSetListener.onDateSet(null, mActivityState.birthday.year, mActivityState.birthday.month - 1, mActivityState.birthday.day);
		}

		Session session = Session.getActiveSession();
		if (session == null)
		{
			if (savedInstanceState != null)
			{
				session = Session.restoreSession(this, null, statusCallback, savedInstanceState);
			}
			if (session == null)
			{
				session = new Session(this);
			}
			Session.setActiveSession(session);
			if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED))
			{
				session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
			}
		}

		if (!addressBookScanningDone)
		{
			Utils.hideSoftKeyboard(this, enterEditText);
		}

		header.setText(R.string.profile_title);

		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		if (TextUtils.isEmpty(msisdn))
		{
			Utils.logEvent(SignupActivity.this, HikeConstants.LogEvent.SIGNUP_ERROR);
			msisdnErrorDuringSignup = true;
			resetViewFlipper();
			restartTask();
			return;
		}

		if (mActivityState.profileBitmap == null)
		{
			mIconView.setImageDrawable(HikeMessengerApp.getLruCache().getIconFromCache(msisdn, true));
			// mIconView.setImageDrawable(IconCacheManager.getInstance()
			// .getIconForMSISDN(msisdn, true));
		}
		else
		{
			mIconView.setImageBitmap(mActivityState.profileBitmap);
		}

		if (mActivityState.fbConnected)
		{
			Button fbBtn = (Button) findViewById(R.id.connect_fb);
			fbBtn.setEnabled(false);
			fbBtn.setText(R.string.connected);
		}
	}

	public void onGenderClick(View v)
	{
		if (v.getId() == R.id.female)
		{
			if (mActivityState.isFemale)
			{
				return;
			}
			mActivityState.isFemale = true;
		}
		else
		{
			if (!mActivityState.isFemale)
			{
				return;
			}
			mActivityState.isFemale = false;
		}
		setupNameViewForGender();
	}

	private void setupNameViewForGender()
	{
		if (mActivityState.isFemale)
		{
			femaleText.setSelected(true);
			maleText.setSelected(false);

			enterEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_name_female, 0, 0, 0);
			birthdayText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_birthday_female, 0, 0, 0);
		}
		else
		{
			femaleText.setSelected(false);
			maleText.setSelected(true);

			enterEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_name_male, 0, 0, 0);
			birthdayText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_birthday_male, 0, 0, 0);
		}
		if (mTask != null)
		{
			mTask.addGender(mActivityState.isFemale);
		}
	}

	public void onBirthdayClick(View v)
	{

		int day;
		int month;
		int year;

		Calendar calendar = Calendar.getInstance();

		if (mActivityState.birthday == null)
		{
			/*
			 * Default values for birthday
			 */
			day = 1;
			month = 0;
			year = 1990;
		}
		else
		{
			day = mActivityState.birthday.day;
			month = mActivityState.birthday.month;
			year = mActivityState.birthday.year;
		}

		DatePickerDialog dialog = new DatePickerDialog(this, onDateSetListener, year, month, day);
		if (Utils.isHoneycombOrHigher())
		{
			dialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
		}
		dialog.show();
	}

	private OnDateSetListener onDateSetListener = new OnDateSetListener()
	{

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
		{
			if (birthdayText == null)
			{
				return;
			}
			Calendar calendar = Calendar.getInstance();
			boolean thisYear = false;

			if (year >= calendar.get(Calendar.YEAR))
			{
				year = calendar.get(Calendar.YEAR);
				thisYear = true;
			}

			if (thisYear)
			{
				boolean thisMonth = false;
				if (monthOfYear >= calendar.get(Calendar.MONTH))
				{
					monthOfYear = calendar.get(Calendar.MONTH);
					thisMonth = true;
				}

				if (thisMonth)
				{
					if (dayOfMonth > calendar.get(Calendar.DAY_OF_MONTH))
					{
						dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
					}
				}
			}

			birthdayText.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);

			mActivityState.birthday = new Birthday(dayOfMonth, monthOfYear + 1, year);
			if (mTask != null)
			{
				mTask.addBirthdate(mActivityState.birthday);
			}

			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		}
	};

	private void resetViewFlipper()
	{
		tryAgainBtn.setVisibility(View.GONE);
		submitBtn.setVisibility(View.VISIBLE);
		viewFlipper.setVisibility(View.VISIBLE);
		removeAnimation();
		viewFlipper.setDisplayedChild(NUMBER);
		prepareLayoutForFetchingNumber();
		setAnimation();
	}

	private void restartTask()
	{
		resetViewFlipper();
		mTask = SignupTask.restartTask(this);
	}

	private void showErrorMsg()
	{
		submitBtn.setVisibility(View.GONE);
		tryAgainBtn.setVisibility(View.VISIBLE);
		loadingLayout.setVisibility(View.GONE);
	}

	private void setListeners()
	{
		enterEditText.setOnEditorActionListener(this);
		enterEditText.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				return loadingLayout.getVisibility() == View.VISIBLE && (event == null || event.getKeyCode() != KeyEvent.KEYCODE_BACK);
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		Session session = Session.getActiveSession();
		Session.saveSession(session, outState);

		outState.putInt(HikeConstants.Extras.SIGNUP_PART, viewFlipper.getDisplayedChild());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, tryAgainBtn.getVisibility() == View.VISIBLE);
		outState.putString(HikeConstants.Extras.SIGNUP_TEXT, enterEditText.getText().toString());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR, msisdnErrorDuringSignup);
		outState.putBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT, showingSecondLoadingTxt);
		if (viewFlipper.getDisplayedChild() == NUMBER)
		{
			outState.putString(HikeConstants.Extras.COUNTRY_CODE, countryPicker.getText().toString());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		if (mTask != null)
		{
			mTask.cancelTask();
			mTask = null;
		}
		SignupTask.isAlreadyFetchingNumber = false;
		super.onBackPressed();
	}

	private void removeAnimation()
	{
		viewFlipper.setInAnimation(null);
		viewFlipper.setOutAnimation(null);
	}

	private void setAnimation()
	{
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_right));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_left));
	}

	@Override
	public void onProgressUpdate(StateValue stateValue)
	{
		/*
		 * Making sure the countdown timer doesn't keep running when the state values changes.
		 */
		if (countDownTimer != null && stateValue.state != State.PIN)
		{
			countDownTimer.cancel();
		}
		String value = stateValue.value;
		mCurrentState = stateValue;
		Log.d("SignupActivity", "Current State " + mCurrentState.state.name() + " VALUE: " + value);

		showingSecondLoadingTxt = false;
		switch (stateValue.state)
		{
		case MSISDN:
			if (TextUtils.isEmpty(value))
			{
				prepareLayoutForFetchingNumber();
			}
			else if (value.equals(HikeConstants.DONE))
			{
				removeAnimation();
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName(null, false);
				setAnimation();
			}
			else
			{
				/* yay, got the actual MSISDN */
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName(null, false);
			}
			break;
		case PULLING_PIN:
			if (viewFlipper.getDisplayedChild() == NUMBER)
			{
				mHandler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						if (viewFlipper != null && viewFlipper.getDisplayedChild() == NUMBER)
						{
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
			if (value != null && value.equals(HikeConstants.PIN_ERROR))
			{
				if (countDownTimer != null)
				{
					countDownTimer.cancel();
				}
				prepareLayoutForGettingPin(mActivityState.timeLeft);
				infoTxt.setText(R.string.wrong_pin_signup);
				loadingLayout.setVisibility(View.GONE);
				callmeBtn.setVisibility(View.VISIBLE);
				submitBtn.setVisibility(View.VISIBLE);
				if (tapHereText != null)
				{
					tapHereText.setVisibility(View.VISIBLE);
				}
				enterEditText.setText("");
			}
			// Manual entry for pin
			else
			{
				prepareLayoutForGettingPin(HikeConstants.CALL_ME_WAIT_TIME);
				setAnimation();
			}
			break;
		case NAME:
			if (TextUtils.isEmpty(value))
			{
				prepareLayoutForGettingName(null, true);
			}
			break;
		case PROFILE_IMAGE:
			if (SignupTask.START_UPLOAD_PROFILE.equals(value))
			{
				mHandler.postDelayed(new Runnable()
				{

					@Override
					public void run()
					{
						loadingText.setText(R.string.setting_profile);
					}
				}, 500);
			}
			else if (SignupTask.FINISHED_UPLOAD_PROFILE.equals(value))
			{
				mHandler.postDelayed(new Runnable()
				{

					@Override
					public void run()
					{
						loadingText.setText(R.string.getting_in_signup);
					}
				}, 500);
			}
			break;
		case ERROR:
			if (value != null && value.equals(HikeConstants.ADDRESS_BOOK_ERROR))
			{
				addressBookError = true;
				if (loadingLayout.getVisibility() == View.VISIBLE)
				{
					showErrorMsg();
				}
			}
			else if (value == null || !value.equals(HikeConstants.CHANGE_NUMBER))
			{
				showErrorMsg();
			}
			break;
		}
		setListeners();
	}

	@Override
	public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event)
	{
		if ((actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && !TextUtils.isEmpty(enterEditText.getText().toString().trim())
				&& loadingLayout.getVisibility() != View.VISIBLE)
		{
			if (viewFlipper.getDisplayedChild() == NAME)
			{
				Utils.hideSoftKeyboard(this, enterEditText);
			}
			else
			{
				submitClicked();
			}
		}
		return true;
	}

	@Override
	public void onCancel(DialogInterface dialog)
	{
		Log.d(getClass().getSimpleName(), "Dialog cancelled");
		if (mActivityState.task != null)
		{
			mActivityState.task.setActivity(null);
			mActivityState = new ActivityState();
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();
		Session session = Session.getActiveSession();
		if (session != null)
		{
			session.addCallback(statusCallback);
		}
	}

	@Override
	public void onStop()
	{
		super.onStop();
		Session session = Session.getActiveSession();
		if (session != null)
		{
			session.removeCallback(statusCallback);
		}
	}

	boolean fbClicked = false;

	boolean fbAuthing = false;

	public void onFacebookConnectClick(View v)
	{
		fbClicked = true;
		Session session = Session.getActiveSession();
		if (session == null)
		{
			fbClicked = false;
			return;
		}

		Log.d(getClass().getSimpleName(), "FB CLICKED");
		if (!session.isOpened() && !session.isClosed())
		{
			session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback).setPermissions(Arrays.asList("basic_info", "user_birthday")));
			Log.d(getClass().getSimpleName(), "Opening for read");
			fbAuthing = true;
		}
		else
		{
			Session.openActiveSession(this, true, statusCallback);
			Log.d(getClass().getSimpleName(), "Opening active session");
		}
	}

	private class SessionStatusCallback implements Session.StatusCallback
	{
		@Override
		public void call(Session session, SessionState state, Exception exception)
		{
			if (fbClicked && session.isOpened())
			{
				updateView();
				fbClicked = false;
			}
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d(getClass().getSimpleName(), "OnResume Called");
		if (fbAuthing)
		{
			Session session = Session.getActiveSession();
			if (session != null)
			{
				Log.d(getClass().getSimpleName(), "Clearing token");
				session.closeAndClearTokenInformation();
			}
		}
	}

	public void updateView()
	{
		Session session = Session.getActiveSession();
		if (session != null && session.isOpened())
		{
			Request.executeMeRequestAsync(session, new GraphUserCallback()
			{
				@Override
				public void onCompleted(final GraphUser user, Response response)
				{
					if (user != null)
					{
						final String fbProfileUrl = String.format(HikeConstants.FACEBOOK_PROFILEPIC_URL_FORMAT, user.getId(), HikeConstants.MAX_DIMENSION_FULL_SIZE_PROFILE_PX);

						String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
						String fileName = Utils.getTempProfileImageFileName(accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, ""));

						try
						{
							String gender = (String) user.getProperty("gender");

							mActivityState.isFemale = "female".equalsIgnoreCase(gender);
							setupNameViewForGender();

						}
						catch (Exception e)
						{
							Log.w(getClass().getSimpleName(), "Exception while fetching gender", e);
						}
						try
						{
							String birthdayString = user.getBirthday();
							if (!TextUtils.isEmpty(birthdayString))
							{
								Date date = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(user.getBirthday());
								Calendar calendar = Calendar.getInstance();
								calendar.setTime(date);

								onDateSetListener.onDateSet(null, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
							}

						}
						catch (Exception e)
						{
							Log.w(getClass().getSimpleName(), "Exception while fetching birthday", e);
						}

						final File destFile = new File(directory, fileName);
						downloadImage(destFile, Uri.parse(fbProfileUrl), new ImageDownloadResult()
						{

							@Override
							public void downloadFinished(boolean result)
							{
								mActivityState = new ActivityState();
								if (!result)
								{
									runOnUiThread(new Runnable()
									{

										@Override
										public void run()
										{
											Toast.makeText(getApplicationContext(), R.string.fb_fetch_image_error, Toast.LENGTH_SHORT).show();
										}
									});
								}
								else
								{
									mActivityState.destFilePath = destFile.getPath();
									mActivityState.userName = user.getName();
								}
								HikeMessengerApp.getPubSub().publish(HikePubSub.FACEBOOK_IMAGE_DOWNLOADED, result);
							}
						});
						dialog = ProgressDialog.show(SignupActivity.this, null, getResources().getString(R.string.fetching_info));
					}
				}
			});
		}
	}

	private void downloadImage(final File destFile, Uri picasaUri, ImageDownloadResult imageDownloadResult)
	{
		mActivityState.downloadImageTask = new Thread(new DownloadImageTask(getApplicationContext(), destFile, picasaUri, imageDownloadResult));

		mActivityState.downloadImageTask.start();
	}

	public interface ImageDownloadResult
	{
		public void downloadFinished(boolean result);
	}

	private class DownloadImageTask implements Runnable
	{

		private File destFile;

		private Uri imageUri;

		private Context context;

		private ImageDownloadResult imageDownloadResult;

		public DownloadImageTask(Context context, File destFile, Uri picasaUri, ImageDownloadResult imageDownloadResult)
		{
			this.destFile = destFile;
			this.imageUri = picasaUri;
			this.context = context;
			this.imageDownloadResult = imageDownloadResult;
		}

		@Override
		public void run()
		{
			Log.d(getClass().getSimpleName(), "Downloading profileImage");
			try
			{
				Utils.downloadAndSaveFile(context, destFile, imageUri);
				imageDownloadResult.downloadFinished(true);
			}
			catch (Exception e)
			{
				Log.e(getClass().getSimpleName(), "Error while fetching image", e);
				imageDownloadResult.downloadFinished(false);
			}
		}

	}

	public void onChangeImageClicked(View v)
	{
		/*
		 * The wants to change their profile picture. Open a dialog to allow them pick Camera or Gallery
		 */
		final CharSequence[] items = getResources().getStringArray(R.array.profile_pic_dialog);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.choose_picture);
		builder.setItems(items, this);
		builder.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int item)
	{
		Intent intent = null;
		switch (item)
		{
		case HikeConstants.PROFILE_PICTURE_FROM_CAMERA:
			if (Utils.getExternalStorageState() != ExternalStorageState.WRITEABLE)
			{
				Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!Utils.hasEnoughFreeSpaceForProfilePic())
			{
				Toast.makeText(getApplicationContext(), R.string.not_enough_space_profile_pic, Toast.LENGTH_SHORT).show();
				return;
			}
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File selectedFileIcon = Utils.getOutputMediaFile(HikeFileType.PROFILE, null); // create a file to save
			// the image
			if (selectedFileIcon != null)
			{
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(selectedFileIcon));

				/*
				 * Saving the file path. Will use this to get the file once the image has been captured.
				 */
				Editor editor = accountPrefs.edit();
				editor.putString(HikeMessengerApp.FILE_PATH, selectedFileIcon.getAbsolutePath());
				editor.commit();

				startActivityForResult(intent, HikeConstants.CAMERA_RESULT);
			}
			else
			{
				Toast.makeText(this, getString(R.string.no_sd_card), Toast.LENGTH_LONG).show();
			}
			break;
		case HikeConstants.PROFILE_PICTURE_FROM_GALLERY:
			if (Utils.getExternalStorageState() == ExternalStorageState.NONE)
			{
				Toast.makeText(getApplicationContext(), R.string.no_external_storage, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!Utils.hasEnoughFreeSpaceForProfilePic())
			{
				Toast.makeText(getApplicationContext(), R.string.not_enough_space_profile_pic, Toast.LENGTH_SHORT).show();
				return;
			}
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, HikeConstants.GALLERY_RESULT);
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		String path = null;
		if (resultCode != RESULT_OK)
		{
			return;
		}

		Session session = Session.getActiveSession();
		if (session != null)
		{
			session.onActivityResult(this, requestCode, resultCode, data);
		}
		if (fbClicked)
		{
			onFacebookConnectClick(null);
			fbAuthing = false;
		}

		File selectedFileIcon;
		boolean isPicasaImage = false;
		Uri selectedFileUri = null;

		String directory = HikeConstants.HIKE_MEDIA_DIRECTORY_ROOT + HikeConstants.PROFILE_ROOT;
		String fileName = Utils.getTempProfileImageFileName(accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, ""));
		final String destFilePath = directory + "/" + fileName;

		switch (requestCode)
		{
		case HikeConstants.CAMERA_RESULT:
			/* fall-through on purpose */
		case HikeConstants.GALLERY_RESULT:
			Log.d("ProfileActivity", "The activity is " + this);
			if (requestCode == HikeConstants.CAMERA_RESULT)
			{
				String filePath = accountPrefs.getString(HikeMessengerApp.FILE_PATH, "");
				selectedFileIcon = new File(filePath);

				/*
				 * Removing this key. We no longer need this.
				 */
				Editor editor = accountPrefs.edit();
				editor.remove(HikeMessengerApp.FILE_PATH);
				editor.commit();
				if (!selectedFileIcon.exists())
				{
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				}
				else
				{
					path = selectedFileIcon.getAbsolutePath();
				}
			}
			else
			{
				selectedFileUri = data.getData();
				if (Utils.isPicasaUri(selectedFileUri.toString()))
				{
					isPicasaImage = true;
					path = Utils.getOutputMediaFile(HikeFileType.PROFILE, null).getAbsolutePath();
				}
				else
				{
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart))
					{
						selectedFileIcon = new File(URI.create(fileUriString));
						/*
						 * Done to fix the issue in a few Sony devices.
						 */
						path = selectedFileIcon.getAbsolutePath();
					}
					else
					{
						path = Utils.getRealPathFromUri(selectedFileUri, this);
					}
				}
			}
			if (TextUtils.isEmpty(path))
			{
				Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
				return;
			}
			if (!isPicasaImage)
			{
				Utils.startCropActivity(this, path, destFilePath);
			}
			else
			{
				final File destFile = new File(path);
				downloadImage(destFile, selectedFileUri, new ImageDownloadResult()
				{

					@Override
					public void downloadFinished(boolean result)
					{
						runOnUiThread(new Runnable()
						{

							@Override
							public void run()
							{
								if (dialog != null)
								{
									dialog.dismiss();
									dialog = null;
								}
							}
						});
						mActivityState = new ActivityState();
						if (!result)
						{
							runOnUiThread(new Runnable()
							{

								@Override
								public void run()
								{
									Toast.makeText(getApplicationContext(), R.string.error_download, Toast.LENGTH_SHORT).show();
								}
							});
						}
						else
						{
							Utils.startCropActivity(SignupActivity.this, destFile.getAbsolutePath(), destFilePath);
						}
					}
				});

				dialog = ProgressDialog.show(this, null, getResources().getString(R.string.downloading_image));
			}
			break;
		case HikeConstants.CROP_RESULT:
			mActivityState.destFilePath = data.getStringExtra(MediaStore.EXTRA_OUTPUT);
			setProfileImage();
			break;
		}
	}

	private void setProfileImage()
	{
		if (mIconView == null)
		{
			return;
		}
		if (!new File(mActivityState.destFilePath).exists())
		{
			Toast.makeText(getApplicationContext(), R.string.image_failed, Toast.LENGTH_SHORT).show();
			return;
		}
		Bitmap tempBitmap = Utils.scaleDownImage(mActivityState.destFilePath, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS, true);

		mActivityState.profileBitmap = Utils.getCircularBitmap(tempBitmap);
		mIconView.setImageBitmap(mActivityState.profileBitmap);

		tempBitmap.recycle();
		tempBitmap = null;
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		if (HikePubSub.FACEBOOK_IMAGE_DOWNLOADED.equals(type))
		{
			final boolean result = (Boolean) object;
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (dialog != null)
					{
						dialog.dismiss();
						dialog = null;
					}
					if (!result)
					{
						return;
					}
					if (mActivityState.destFilePath == null)
					{
						return;
					}
					setProfileImage();

					enterEditText.setText(mActivityState.userName);
					enterEditText.setSelection(mActivityState.userName.length());
					Button fbBtn = (Button) findViewById(R.id.connect_fb);
					if (fbBtn != null)
					{
						fbBtn.setEnabled(false);
						fbBtn.setText(R.string.connected);
						mActivityState.fbConnected = true;
					}
				}
			});
		}
	}
}