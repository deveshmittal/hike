package com.bsb.hike.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

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
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.State;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class SignupActivity extends ChangeProfileImageBaseActivity implements SignupTask.OnSignupTaskProgressUpdate, OnEditorActionListener, OnClickListener, FinishableEvent,
		OnCancelListener, Listener
{

	private SignupTask mTask;

	private StateValue mCurrentState;

	private ViewFlipper viewFlipper;

	private ViewGroup numLayout;

	private ViewGroup pinLayout;

	private ViewGroup nameLayout;

	private ViewGroup genderLayout;

	private ViewGroup scanningContactsLayout;

	private TextView infoTxt;

	private TextView loadingText;

	private ViewGroup loadingLayout;

	private EditText enterEditText;

	private TextView invalidNum;

	private EditText countryPicker;

	private TextView selectedCountryName;

	private Button callmeBtn;

	private ImageView mIconView;

	private TextView birthdayText;

	private TextView maleText;

	private TextView femaleText;

	private ImageView profilePicCamIcon;

	private TextView genderDesctribeText;

	private Handler mHandler;

	private boolean addressBookError = false;

	private boolean msisdnErrorDuringSignup = false;

	public static final int SCANNING_CONTACTS = 4;

	public static final int GENDER = 3;

	public static final int NAME = 2;

	public static final int PIN = 1;

	public static final int NUMBER = 0;

	private String countryCode;

	private final String defaultCountryName = "India";

	private boolean showingSecondLoadingTxt = false;

	private SharedPreferences accountPrefs;

	private ActivityState mActivityState;

	private Dialog dialog;

	private Session.StatusCallback statusCallback = new SessionStatusCallback();

	private CountDownTimer countDownTimer;

	private boolean showingNumberConfimationDialog;

	TextView mActionBarTitle;

	private Dialog errorDialog;

	View nextBtn;

	View nextBtnContainer;

	View selectedCountryPicker;

	private TextView invalidPin;

	private View verifiedPin;

	private ArrayList<String> countriesArray = new ArrayList<String>();

	private HashMap<String, String> countriesMap = new HashMap<String, String>();

	private HashMap<String, String> codesMap = new HashMap<String, String>();

	private HashMap<String, String> languageMap = new HashMap<String, String>();

	private ImageView arrow;

	private TextView postText;

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

		public Boolean isFemale = null;

		public Birthday birthday = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.signup);

		mHandler = new Handler();

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		numLayout = (ViewGroup) findViewById(R.id.num_layout);
		pinLayout = (ViewGroup) findViewById(R.id.pin_layout);
		nameLayout = (ViewGroup) findViewById(R.id.name_layout);
		genderLayout = (ViewGroup) findViewById(R.id.gender_layout);
		scanningContactsLayout = (ViewGroup) findViewById(R.id.scanning_contacts_layout);

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

		setupActionBar();
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
				enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
				break;
			case PIN:
				prepareLayoutForGettingPin(mActivityState.timeLeft);
				enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
				if (savedInstanceState.getBoolean(HikeConstants.Extras.SHOWING_INVALID_PIN_ERROR, false))
				{
					invalidPin.setVisibility(View.VISIBLE);
				}
				break;
			case NAME:
				prepareLayoutForGettingName(savedInstanceState, false);
				enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
				break;
			case GENDER:
				prepareLayoutForGender(savedInstanceState);
				break;
			case SCANNING_CONTACTS:
				prepareLayoutForScanning(savedInstanceState);
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
			mTask = SignupTask.startTask(this, mActivityState.userName, mActivityState.isFemale, mActivityState.birthday, mActivityState.profileBitmap);
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

			mTask = SignupTask.startTask(this);
		}
		setAnimation();
		setListeners();

		HikeMessengerApp.getPubSub().addListener(HikePubSub.FACEBOOK_IMAGE_DOWNLOADED, this);
		setWindowSoftInputState();
	}

	private void setWindowSoftInputState()
	{
		int displayedChild = viewFlipper.getDisplayedChild();
		switch (displayedChild)
		{
		case GENDER:
		case SCANNING_CONTACTS:
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
			break;
		default:
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			break;
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setIcon(R.drawable.hike_logo_top_bar);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.signup_activity_action_bar, null);

		mActionBarTitle = (TextView) actionBarView.findViewById(R.id.title);
		nextBtn = actionBarView.findViewById(R.id.done_container);
		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.next_btn);
		nextBtnContainer = actionBarView.findViewById(R.id.next_btn_container);

		nextBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				submitClicked();
			}
		});
		actionBar.setCustomView(actionBarView);
	}

	private void setupActionBarTitle()
	{
		int displayedChild = viewFlipper.getDisplayedChild();
		if (displayedChild == NUMBER)
		{
			mActionBarTitle.setText(R.string.phone_num);
		}
		else if (displayedChild == PIN)
		{
			mActionBarTitle.setText(R.string.pin);
		}
		else if (displayedChild == NAME)
		{
			mActionBarTitle.setText(R.string.about_you);
		}
		else if (displayedChild == GENDER)
		{
			mActionBarTitle.setText(R.string.tell_us_more);
		}
		else if (displayedChild == SCANNING_CONTACTS)
		{
			mActionBarTitle.setText("");
		}
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
				boolean isSAUser = countryCode.equals(HikeConstants.SAUDI_ARABIA_COUNTRY_CODE);

				StickerManager.setStickersForIndianUsers(HikeConstants.INDIA_COUNTRY_CODE.equals(countryCode), accountPrefs);

				Editor accountEditor = accountPrefs.edit();
				accountEditor.putBoolean(HikeMessengerApp.JUST_SIGNED_UP, true);
				if (mActivityState != null)
				{
					accountEditor.putBoolean(HikeMessengerApp.FB_SIGNUP, mActivityState.fbConnected);
				}
				accountEditor.commit();

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = prefs.edit();
				editor.putBoolean(HikeConstants.FREE_SMS_PREF, HikeMessengerApp.isIndianUser());
				editor.putBoolean(HikeConstants.SSL_PREF, !(HikeMessengerApp.isIndianUser() || isSAUser));
				editor.remove(HikeMessengerApp.TEMP_COUNTRY_CODE);
				editor.commit();

				/*
				 * Update the urls to use ssl or not.
				 */
				Utils.setupUri(this.getApplicationContext());

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
		if (callmeBtn != null && v.getId() == callmeBtn.getId())
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
				Logger.e(getClass().getSimpleName(), "Invalid JSON", e);
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
		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.VISIBLE);
		}
		if(infoTxt != null)
		{
			infoTxt.setVisibility(View.GONE);
		}
		toggleActionBarElementsEnable(false);
		if (invalidNum != null)
		{
			invalidNum.setVisibility(View.GONE);
		}
		if (invalidPin != null)
		{
			invalidPin.setVisibility(View.INVISIBLE);
		}
		if (countryPicker != null)
		{
			countryPicker.setEnabled(false);
			selectedCountryPicker.setEnabled(false);
			enterEditText.setEnabled(false);
		}
		if (callmeBtn != null)
		{
			callmeBtn.setEnabled(false);
		}
	}

	private void endLoading()
	{
		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.GONE);
		}
		if (infoTxt != null)
		{
			infoTxt.setVisibility(View.VISIBLE);
		}
		toggleActionBarElementsEnable(true);
		if (countryPicker != null)
		{
			countryPicker.setEnabled(true);
			selectedCountryPicker.setEnabled(true);
			enterEditText.setEnabled(true);
		}
		if (callmeBtn != null)
		{
			callmeBtn.setEnabled(true);
		}
	}

	private void submitClicked()
	{
		if (invalidNum != null)
		{
			invalidNum.setVisibility(View.GONE);
		}
		if (viewFlipper.getDisplayedChild() == NUMBER && isInvalidCountryCode())
		{
			loadingLayout.setVisibility(View.GONE);
			infoTxt.setVisibility(View.VISIBLE);
			toggleActionBarElementsEnable(true);
			invalidNum.setVisibility(View.VISIBLE);
			return;
		}

		if (enterEditText != null && TextUtils.isEmpty(enterEditText.getText().toString().replaceAll(" ", "")))
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
		if (viewFlipper.getDisplayedChild() == PIN)
		{
			startLoading();
		}
		if (!addressBookError || viewFlipper.getDisplayedChild() == NAME)
		{
			if (viewFlipper.getDisplayedChild() == NUMBER && !enterEditText.getText().toString().matches(HikeConstants.VALID_MSISDN_REGEX))
			{
				loadingLayout.setVisibility(View.GONE);
				infoTxt.setVisibility(View.VISIBLE);
				toggleActionBarElementsEnable(true);
				invalidNum.setVisibility(View.VISIBLE);
			}
			else if (viewFlipper.getDisplayedChild() == GENDER)
			{
				if (mActivityState.isFemale != null)
				{
					mTask.addGender(mActivityState.isFemale);
					mTask.addUserInput(mActivityState.isFemale.toString());
					viewFlipper.setDisplayedChild(SCANNING_CONTACTS);
					prepareLayoutForScanning(null);
				}
				else
				{
					Toast toast = Toast.makeText(this, "please select your gender", Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
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

					final String code = "+" + countryPicker.getText().toString();
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
							if (Utils.densityMultiplier < 1.5)
							{
								Utils.hideSoftKeyboard(SignupActivity.this, enterEditText);
							}
						}
					});
					builder.setNegativeButton(R.string.edit, new DialogInterface.OnClickListener()
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
						mActivityState.userName = input;
						mTask.addUserName(mActivityState.userName);
						viewFlipper.setDisplayedChild(GENDER);
						prepareLayoutForGender(null);
					}
					mTask.addUserInput(input);
					if (birthdayText != null && !TextUtils.isEmpty(birthdayText.getText().toString()))
					{
						Calendar calendar = Calendar.getInstance();
						int currentYear = calendar.get(Calendar.YEAR);
						mActivityState.birthday = new Birthday(0, 0, currentYear - Integer.valueOf(birthdayText.getText().toString()));
						mTask.addBirthdate(mActivityState.birthday);
					}
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
			profilePicCamIcon = (ImageView) layout.findViewById(R.id.profile_cam);
			break;
		case R.id.num_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_num);
			infoTxt = (TextView) layout.findViewById(R.id.txt_img1);
			infoTxt.setVisibility(View.VISIBLE);
			verifiedPin = layout.findViewById(R.id.verified_pin);
			break;
		case R.id.pin_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_pin);
			infoTxt = (TextView) layout.findViewById(R.id.txt_img1);
			invalidPin = (TextView) layout.findViewById(R.id.invalid_pin);
			verifiedPin = layout.findViewById(R.id.verified_pin);
			infoTxt.setVisibility(View.VISIBLE);
			invalidPin.setVisibility(View.INVISIBLE);
			break;
		case R.id.gender_layout:
			break;
		}
		infoTxt = (TextView) layout.findViewById(R.id.txt_img1);
		loadingText = (TextView) layout.findViewById(R.id.txt_loading);
		loadingLayout = (ViewGroup) layout.findViewById(R.id.loading_layout);
		invalidNum = (TextView) layout.findViewById(R.id.invalid_num);
		countryPicker = (EditText) layout.findViewById(R.id.country_picker);
		selectedCountryName = (TextView) layout.findViewById(R.id.selected_country_name);
		selectedCountryPicker = layout.findViewById(R.id.selected_country);
		callmeBtn = (Button) layout.findViewById(R.id.btn_call_me);
		mIconView = (ImageView) layout.findViewById(R.id.profile);

		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.GONE);
		}
		toggleActionBarElementsEnable(true);
		setupActionBarTitle();
	}

	private void prepareLayoutForFetchingNumber()
	{
		initializeViews(numLayout);

		countryPicker.setOnFocusChangeListener(new OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View arg0, boolean isFocus)
			{
				if (isFocus)
				{
					findViewById(R.id.country_code_view_group).setBackgroundResource(R.drawable.bg_phone_num_selected);
				}
				else
				{
					findViewById(R.id.country_code_view_group).setBackgroundResource(R.drawable.bg_phone_num_unselected);
				}

			}
		});

		countryPicker.setEnabled(true);
		selectedCountryPicker.setEnabled(true);
		enterEditText.setEnabled(true);

		Utils.setupCountryCodeData(this, countryCode, countryPicker, selectedCountryName, countriesArray, countriesMap, codesMap, languageMap);
		TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String number = manager.getLine1Number();
		if (number != null && number.startsWith("+91"))
		{
			number = number.replace("+91", "");
			enterEditText.setText(number);
			/*
			 * Saw a few crashes here. Catching the exception since we don't seem to be doing anything wrong here.
			 */
			try
			{
				enterEditText.setSelection(number.length());
			}
			catch (IndexOutOfBoundsException e)
			{
				Logger.w(getClass().getSimpleName(), "IOOB thrown while setting the number's textbox selection");
			}
		}
		infoTxt.setText(msisdnErrorDuringSignup ? R.string.enter_phone_again_signup : R.string.whats_your_number);
		invalidNum.setVisibility(View.INVISIBLE);
		loadingText.setText(R.string.verifying);
	}

	public void onCountryPickerClick(View v)
	{

		Intent intent = new Intent(this, CountrySelectActivity.class);
	    this.startActivityForResult(intent, HikeConstants.ResultCodes.SELECT_COUNTRY);
	}

	private void prepareLayoutForGettingPin(long timeLeft)
	{
		initializeViews(pinLayout);

		callmeBtn.setVisibility(View.VISIBLE);

		enterEditText.setText("");
		infoTxt.setText(R.string.enter_pin_signup);

		String tapHere = getString(R.string.tap_here_signup);
		String tapHereString = getString(R.string.wrong_num_signup);

		SpannableStringBuilder ssb = new SpannableStringBuilder(tapHereString);
		if (tapHereString.indexOf(tapHere) != -1)
		{
			ssb.setSpan(new ForegroundColorSpan(0xff6edcff), tapHereString.indexOf(tapHere), tapHereString.indexOf(tapHere) + tapHere.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

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
					String text = "(" + String.format("%1$02d:%2$02d", minutes, seconds) + " )";
					callmeBtn.setText(getResources().getString(R.string.call_me_for_the_pin, text));
					mActivityState.timeLeft = millisUntilFinished;
					callmeBtn.setEnabled(false);
				}

				@Override
				public void onFinish()
				{
					callmeBtn.setText(getResources().getString(R.string.call_me_for_the_pin, ""));
					callmeBtn.setEnabled(true);
					mActivityState.timeLeft = 0;
				}
			};
			countDownTimer.start();
		}
		else
		{
			callmeBtn.setText(getResources().getString(R.string.call_me_for_the_pin, ""));
			callmeBtn.setEnabled(true);
		}
		loadingText.setText(R.string.verify_pin_signup);
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
			BitmapDrawable bd = HikeMessengerApp.getLruCache().getIconFromCache(msisdn, true);
			if (bd != null)
			{
				mIconView.setImageDrawable(bd);
			}
			else
			{
				mIconView.setScaleType(ScaleType.CENTER_INSIDE);
				mIconView.setBackgroundResource(R.drawable.avatar_03_rounded);
				mIconView.setImageResource(R.drawable.ic_default_avatar);
			}
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
		nextBtnContainer.setVisibility(View.VISIBLE);
	}

	private void prepareLayoutForGender(Bundle savedInstanceState)
	{
		femaleText = (TextView) genderLayout.findViewById(R.id.female);
		maleText = (TextView) genderLayout.findViewById(R.id.male);
		genderDesctribeText = (TextView) genderLayout.findViewById(R.id.describe_txt);
		if (savedInstanceState != null && savedInstanceState.containsKey(HikeConstants.Extras.GENDER))
		{
			mActivityState.isFemale = savedInstanceState.getBoolean(HikeConstants.Extras.GENDER);
			selectGender(mActivityState.isFemale);
		}
		if (mActivityState.isFemale == null)
		{
			genderDesctribeText.setText("");
		}
		nextBtnContainer.setVisibility(View.VISIBLE);
		setupActionBarTitle();
	}

	private void prepareLayoutForScanning(Bundle savedInstanceState)
	{
		infoTxt = (TextView) scanningContactsLayout.findViewById(R.id.txt_img1);
		loadingText = (TextView) scanningContactsLayout.findViewById(R.id.txt_loading);
		loadingLayout = (ViewGroup) scanningContactsLayout.findViewById(R.id.loading_layout);
		nextBtnContainer.setVisibility(View.GONE);
		setupActionBarTitle();
	}

	public void onGenderClick(View v)
	{
		if (v.getId() == R.id.female)
		{
			if (mActivityState.isFemale != null && mActivityState.isFemale)
			{
				return;
			}
			mActivityState.isFemale = true;
		}
		else
		{
			if (mActivityState.isFemale != null && !mActivityState.isFemale)
			{
				return;
			}
			mActivityState.isFemale = false;
		}

		selectGender(mActivityState.isFemale);
	}

	private void selectGender(Boolean isFemale)
	{
		femaleText.setSelected(mActivityState.isFemale);
		maleText.setSelected(!mActivityState.isFemale);

		setGenderDescribeRandomText(mActivityState.isFemale);
	}

	private void setGenderDescribeRandomText(boolean isFemale)
	{
		int size = 0;
		int describeStringRes;
		Random random = new Random();
		if (isFemale)
		{
			size = HikeConstants.FEMALE_SELECTED_STRINGS.length;
			describeStringRes = HikeConstants.FEMALE_SELECTED_STRINGS[random.nextInt(size)];
		}
		else
		{
			size = HikeConstants.MALE_SELECTED_STRINGS.length;
			describeStringRes = HikeConstants.MALE_SELECTED_STRINGS[random.nextInt(size)];
		}
		genderDesctribeText.setText(describeStringRes);
	}

	private void resetViewFlipper()
	{
		errorDialog = null;
		toggleActionBarElementsEnable(true);
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

	private void restartTask(String userName, Boolean isFemale, Birthday birthday)
	{
		resetViewFlipper();
		mTask = SignupTask.restartTask(this, userName, isFemale, birthday, mActivityState.profileBitmap);
	}

	private void showErrorMsg()
	{
		toggleActionBarElementsEnable(false);
		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.GONE);
		}
		if (infoTxt != null)
		{
			infoTxt.setVisibility(View.VISIBLE);
		}
		if (errorDialog == null)
		{
			showNetworkErrorPopup();
		}
	}

	private void showNetworkErrorPopup()
	{
		errorDialog = new Dialog(this, R.style.Theme_CustomDialog);
		errorDialog.setContentView(R.layout.no_internet_pop_up);
		errorDialog.setCancelable(true);
		Button btnOk = (Button) errorDialog.findViewById(R.id.btn_ok);
		btnOk.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (errorDialog != null)
				{
					errorDialog.dismiss();
					v.setEnabled(false);
					if (viewFlipper.getDisplayedChild() != SCANNING_CONTACTS)
					{
						/*
						 * Delaying this by 100 ms to allow the signup task to setup to the last input point.
						 */
						SignupActivity.this.mHandler.postDelayed(new Runnable()
						{

							@Override
							public void run()
							{
								Logger.d("tesst", "submit clicked");
								submitClicked();
							}
						}, 100);
					}
					restartTask(mActivityState.userName, mActivityState.isFemale, mActivityState.birthday);

				}
			}
		});

		errorDialog.setOnCancelListener(new OnCancelListener()
		{

			@Override
			public void onCancel(DialogInterface dialog)
			{
				endLoading();

			}
		});
		if (!SignupActivity.this.isFinishing())
		{
			errorDialog.show();
		}
	}

	private void setListeners()
	{
		if (enterEditText != null)
		{
			enterEditText.setOnEditorActionListener(this);
			enterEditText.setOnKeyListener(new OnKeyListener()
			{
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					return loadingLayout != null && loadingLayout.getVisibility() == View.VISIBLE && (event == null || event.getKeyCode() != KeyEvent.KEYCODE_BACK);
				}
			});
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		Session session = Session.getActiveSession();
		Session.saveSession(session, outState);

		outState.putInt(HikeConstants.Extras.SIGNUP_PART, viewFlipper.getDisplayedChild());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout != null && loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, errorDialog != null);
		if (enterEditText != null)
		{
			outState.putString(HikeConstants.Extras.SIGNUP_TEXT, enterEditText.getText().toString());
		}
		outState.putBoolean(HikeConstants.Extras.SIGNUP_MSISDN_ERROR, msisdnErrorDuringSignup);
		outState.putBoolean(HikeConstants.Extras.SHOWING_SECOND_LOADING_TXT, showingSecondLoadingTxt);
		outState.putBoolean(HikeConstants.Extras.SHOWING_INVALID_PIN_ERROR, invalidPin != null && invalidPin.getVisibility() == View.VISIBLE);
		if (viewFlipper.getDisplayedChild() == NUMBER)
		{
			outState.putString(HikeConstants.Extras.COUNTRY_CODE, countryPicker.getText().toString());
		}
		if (viewFlipper.getDisplayedChild() == GENDER && mActivityState.isFemale != null)
		{
			outState.putBoolean(HikeConstants.Extras.GENDER, mActivityState.isFemale);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		if (viewFlipper.getDisplayedChild() == PIN)
		{
			if (countDownTimer != null)
			{
				countDownTimer.cancel();
			}
			mTask.addUserInput("");
			return;
		}
		if (mTask != null)
		{
			mTask.cancelTask();
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
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_animation));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out_animation));
	}

	public int getDisplayItem()
	{
		return viewFlipper.getDisplayedChild();
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
		Logger.d("SignupActivity", "Current State " + mCurrentState.state.name() + " VALUE: " + value);

		showingSecondLoadingTxt = false;
		switch (stateValue.state)
		{
		case MSISDN:
			if (TextUtils.isEmpty(value))
			{
				prepareLayoutForFetchingNumber();
			}
			else
			{
				if (Utils.getExternalStorageState() == ExternalStorageState.WRITEABLE)
				{
					// we should delete old profile image of the returning user
					String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
					Utils.removeLargerProfileImageForMsisdn(msisdn);
				}
				if (value.equals(HikeConstants.DONE))
				{
					removeAnimation();
				}
				/* yay, got the actual MSISDN */
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName(null, false);
				if (value.equals(HikeConstants.DONE))
				{
					setAnimation();
				}

			}
			break;
		case PULLING_PIN:
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
				invalidPin.setVisibility(View.VISIBLE);
				loadingLayout.setVisibility(View.GONE);
				callmeBtn.setVisibility(View.VISIBLE);
				callmeBtn.setEnabled(true);
				toggleActionBarElementsEnable(true);
				enterEditText.setText("");
			}
			// Manual entry for pin
			else
			{
				prepareLayoutForGettingPin(HikeConstants.CALL_ME_WAIT_TIME);
				setAnimation();
			}
			break;
		case PIN_VERIFIED:
			if (verifiedPin != null)
			{
				verifiedPin.setVisibility(View.VISIBLE);
				loadingLayout.setVisibility(View.GONE);
				toggleActionBarElementsEnable(false);
				/*
				 * after verifying pin we would wait for 2 second to get user to the next screen and show him/her that pin is verified
				 */
				mHandler.postDelayed(new Runnable()
				{

					@Override
					public void run()
					{
						if (mTask != null)
						{
							mTask.addUserInput("");
						}
					}
				}, 1500);
			}
			break;
		case NAME:
			if (TextUtils.isEmpty(value))
			{
				prepareLayoutForGettingName(null, true);
			}
			break;
		case GENDER:
			if (TextUtils.isEmpty(value))
			{
				prepareLayoutForGender(null);
				viewFlipper.setDisplayedChild(GENDER);
			}
			break;
		case SCANNING_CONTACTS:
			if (TextUtils.isEmpty(value) && viewFlipper.getDisplayedChild() != SCANNING_CONTACTS)
			{
				viewFlipper.setDisplayedChild(SCANNING_CONTACTS);
				prepareLayoutForScanning(null);
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
						if (loadingText != null)
						{
							loadingText.setText(R.string.setting_profile);
						}
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
						if (loadingText != null)
						{
							loadingText.setText(R.string.you_are_all_set);
						}
					}
				}, 1000);
			}
			break;
		case ERROR:
			if (value != null && value.equals(HikeConstants.ADDRESS_BOOK_ERROR))
			{
				addressBookError = true;
				if (viewFlipper.getDisplayedChild() == SCANNING_CONTACTS)
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
		if ((actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				&& !TextUtils.isEmpty(enterEditText.getText().toString().trim()) && (loadingLayout == null || loadingLayout.getVisibility() != View.VISIBLE))
		{
			if (viewFlipper.getDisplayedChild() == NAME)
			{
				if (enterEditText.isFocused())
				{
					birthdayText.requestFocus();
				}
				else
				{
					Utils.hideSoftKeyboard(this, enterEditText);
				}
			}
			else
			{
				submitClicked();
				Utils.hideSoftKeyboard(this, enterEditText);
			}
		}
		return true;
	}

	@Override
	public void onCancel(DialogInterface dialog)
	{
		Logger.d(getClass().getSimpleName(), "Dialog cancelled");
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

		Logger.d(getClass().getSimpleName(), "FB CLICKED");
		if (!session.isOpened() && !session.isClosed())
		{
			session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback).setPermissions(Arrays.asList("basic_info", "user_birthday")));
			Logger.d(getClass().getSimpleName(), "Opening for read");
			fbAuthing = true;
		}
		else
		{
			Session.openActiveSession(this, true, statusCallback);
			Logger.d(getClass().getSimpleName(), "Opening active session");
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
		Logger.d(getClass().getSimpleName(), "OnResume Called");
		if (fbAuthing)
		{
			Session session = Session.getActiveSession();
			if (session != null)
			{
				Logger.d(getClass().getSimpleName(), "Clearing token");
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
						}
						catch (Exception e)
						{
							Logger.w(getClass().getSimpleName(), "Exception while fetching gender", e);
						}
						try
						{
							String birthdayString = user.getBirthday();
							if (!TextUtils.isEmpty(birthdayString))
							{
								Date date = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(user.getBirthday());
								if (date.compareTo(Calendar.getInstance().getTime()) <= 0)
								{
									Calendar calendar = Calendar.getInstance();
									calendar.setTime(date);
									mActivityState.birthday = new Birthday(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
									mTask.addBirthdate(mActivityState.birthday);
									birthdayText.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR) - mActivityState.birthday.year));
								}
							}

						}
						catch (Exception e)
						{
							Logger.w(getClass().getSimpleName(), "Exception while fetching birthday", e);
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
			Logger.d(getClass().getSimpleName(), "Downloading profileImage");
			try
			{
				Utils.downloadAndSaveFile(context, destFile, imageUri);
				imageDownloadResult.downloadFinished(true);
			}
			catch (Exception e)
			{
				Logger.e(getClass().getSimpleName(), "Error while fetching image", e);
				imageDownloadResult.downloadFinished(false);
			}
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
			Logger.d("ProfileActivity", "The activity is " + this);
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
				if (data == null)
				{
					Toast.makeText(getApplicationContext(), R.string.error_capture, Toast.LENGTH_SHORT).show();
					return;
				}
				selectedFileUri = data.getData();
				if (Utils.isPicasaUri(selectedFileUri.toString()))
				{
					isPicasaImage = true;
					path = Utils.getOutputMediaFile(HikeFileType.PROFILE, null, false).getAbsolutePath();
				}
				else
				{
					String fileUriStart = "file://";
					String fileUriString = selectedFileUri.toString();
					if (fileUriString.startsWith(fileUriStart))
					{
						selectedFileIcon = new File(URI.create(Utils.replaceUrlSpaces(fileUriString)));
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
		case HikeConstants.ResultCodes.SELECT_COUNTRY:	
			if (resultCode == RESULT_OK) {
				String countryName = data.getStringExtra(HikeConstants.Extras.SELECTED_COUNTRY);
				selectCountry(countryName);
			}
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

		Bitmap tempBitmap = HikeBitmapFactory.scaleDownBitmap(mActivityState.destFilePath, HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS,
				HikeConstants.SIGNUP_PROFILE_IMAGE_DIMENSIONS, Bitmap.Config.RGB_565, true, false);

		mActivityState.profileBitmap = HikeBitmapFactory.getCircularBitmap(tempBitmap);
		mIconView.setImageBitmap(mActivityState.profileBitmap);
		mIconView.setBackgroundResource(R.color.transparent);
		profilePicCamIcon.setImageResource(R.drawable.ic_signup_editphoto);

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

					/*
					 * Saw a few crashes here. Catching the exception since we don't seem to be doing anything wrong here.
					 */
					try
					{
						enterEditText.setSelection(mActivityState.userName.length());
					}
					catch (IndexOutOfBoundsException e)
					{
						Logger.w(getClass().getSimpleName(), "IOOB thrown while setting the name's textbox selection");
					}

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

	private boolean selectCountry(String countryName)
	{
		int index = countriesArray.indexOf(countryName);
		if (index != -1)
		{
			countryCode = countriesMap.get(countryName);
			countryPicker.setText(countryCode);
			selectedCountryName.setText(countryName);
		}
		return !TextUtils.isEmpty(countryCode);
	}

	private boolean isInvalidCountryCode()
	{
		String countryName = codesMap.get(countryPicker.getText().toString());
		return !(countryName != null && countriesArray.indexOf(countryName) != -1);
	}

	public void autoFillPin(String pin)
	{
		if (viewFlipper.getDisplayedChild() == PIN)
		{
			enterEditText.setText(pin);
			submitClicked();
		}
	}
	
	public void toggleActionBarElementsEnable( boolean enabled)
	{
		nextBtn.setEnabled(enabled);
		arrow.setEnabled(enabled);
		postText.setEnabled(enabled);
	}

}