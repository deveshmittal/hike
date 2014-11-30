package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.http.HikeHttpRequest;
import com.bsb.hike.http.HikeHttpRequest.RequestType;
import com.bsb.hike.models.Birthday;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.HikeHTTPTask;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.State;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.ChangeProfileImageBaseActivity;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;
import com.bsb.hike.utils.Utils.ExternalStorageState;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.TextView.OnEditorActionListener;

public class AddAccountActivity extends ChangeProfileImageBaseActivity  implements SignupTask.OnSignupTaskProgressUpdate, OnEditorActionListener, OnClickListener, FinishableEvent,
OnCancelListener, Listener{
	
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
	
	private class ViewProperties
	{
		public int left;
		
		public int top;
		
		public int width;
		
		public int height;
	}

	
	private Handler mHandler;
	private SharedPreferences accountPrefs;
	private ViewFlipper viewFlipper;
	private ViewGroup numLayout;
	private ViewGroup pinLayout;
	private ActivityState mActivityState;
	private ProgressDialog dialog;
	private boolean msisdnErrorDuringSignup;
	private boolean showingSecondLoadingTxt;
	private TextView loadingText;
	private EditText enterEditText;
	private TextView invalidPin;
	private String countryCode;
	private SignupTask mTask;
	private TextView mActionBarTitle;
	private View nextBtn;
	private ImageView arrow;
	private TextView postText;
	private View nextBtnContainer;
	private EditText countryPicker;
	private View selectedCountryPicker;
	private TextView selectedCountryName;
	private ArrayList<String> countriesArray = new ArrayList<String>();

	private HashMap<String, String> countriesMap = new HashMap<String, String>();

	private HashMap<String, String> codesMap = new HashMap<String, String>();

	private HashMap<String, String> languageMap = new HashMap<String, String>();
	private TextView infoTxt;
	private TextView invalidNum;
	private ViewGroup layout;
	private View verifiedPin;
	private ViewGroup loadingLayout;
	private Button callmeBtn;
	private ImageView mIconView;
	private boolean enabled;
	private CountDownTimer countDownTimer;
	private Dialog errorDialog;
	private StateValue mCurrentState;
	
	public static final int PIN = 1;

	public static final int NUMBER = 0;

	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.signup);

		mHandler = new Handler();

		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);

		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		numLayout = (ViewGroup) findViewById(R.id.num_layout);
		pinLayout = (ViewGroup) findViewById(R.id.pin_layout);
		
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
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING))
			{
				startLoading();
			}
			if (savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_ERROR))
			{
				showErrorMsg();
			}
			mTask = SignupTask.startTask(this);
		} else {
			prepareLayoutForFetchingNumber();
		}

		mTask = SignupTask.startTask(this);
		
		setAnimation();
		setListeners();
		
	}

	private void setListeners() {
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

	private void setAnimation() {
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_animation));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out_animation));
	}

	private void showErrorMsg() {
		toggleActionBarElementsEnable(true);
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

	private void showNetworkErrorPopup() {
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
					
						/*
						 * Delaying this by 100 ms to allow the signup task to setup to the last input point.
						 */
						AddAccountActivity.this.mHandler.postDelayed(new Runnable()
						{

							@Override
							public void run()
							{
								Logger.d("tesst", "submit clicked");
								submitClicked();
							}
						}, 100);
					
					restartTask();

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
		if (!AddAccountActivity.this.isFinishing())
		{
			errorDialog.show();
		}
	}

	protected void endLoading() {
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

	protected void restartTask() {
		resetViewFlipper();
		mTask = SignupTask.restartTask(this);
		
	}

	private void resetViewFlipper() {
		errorDialog = null;
		toggleActionBarElementsEnable(true);
		viewFlipper.setVisibility(View.VISIBLE);
		removeAnimation();
		viewFlipper.setDisplayedChild(NUMBER);
		prepareLayoutForFetchingNumber();
		setAnimation();
		
	}

	private void startLoading() {
		if (loadingLayout != null)
		{
			loadingLayout.setVisibility(View.VISIBLE);
		}
		if(infoTxt != null)
		{
			infoTxt.setVisibility(View.GONE);
		}
		toggleActionBarElementsEnable(true);
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

	private void prepareLayoutForGettingPin(long timeLeft) {
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

	private void prepareLayoutForFetchingNumber() {
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

	private void initializeViews(ViewGroup layout) {
			switch (layout.getId())
			{
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

	private void toggleActionBarElementsEnable(boolean b) {
		nextBtn.setEnabled(enabled);
		arrow.setEnabled(enabled);
		postText.setEnabled(enabled);
		
	}

	private void setupActionBarTitle() {
		int displayedChild = viewFlipper.getDisplayedChild();
		if (displayedChild == NUMBER)
		{
			mActionBarTitle.setText(R.string.phone_num);
		}
		else if (displayedChild == PIN)
		{
			mActionBarTitle.setText(R.string.pin);
		}
		
	}

	private void removeAnimation() {
		viewFlipper.setInAnimation(null);
		viewFlipper.setOutAnimation(null);
		
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setIcon(R.drawable.hike_logo_top_bar);

		View actionBarView = LayoutInflater.from(this).inflate(R.layout.signup_activity_action_bar, null);

		mActionBarTitle = (TextView) actionBarView.findViewById(R.id.title);
		nextBtn = actionBarView.findViewById(R.id.done_container);
		arrow = (ImageView) actionBarView.findViewById(R.id.arrow);
		postText = (TextView) actionBarView.findViewById(R.id.next_btn);
		nextBtnContainer = actionBarView.findViewById(R.id.next_btn_container);
		nextBtn.setEnabled(true);

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
	}


	private boolean isInvalidCountryCode() {
		String countryName = codesMap.get(countryPicker.getText().toString());
		return !(countryName != null && countriesArray.indexOf(countryName) != -1);
	}

	@Override
	public void onCancel(DialogInterface arg0) {
		Logger.d(getClass().getSimpleName(), "Dialog cancelled");
		if (mActivityState.task != null)
		{
			mActivityState.task.setActivity(null);
			mActivityState = new ActivityState();
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
				boolean isIndianUser = countryCode.equals(HikeConstants.INDIA_COUNTRY_CODE);
				boolean isSAUser = countryCode.equals(HikeConstants.SAUDI_ARABIA_COUNTRY_CODE);

				Editor accountEditor = accountPrefs.edit();
//				accountEditor.putInt(key, value);
				accountEditor.commit();

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				Editor editor = prefs.edit();
				editor.putBoolean(HikeConstants.FREE_SMS_PREF, isIndianUser);
				editor.putBoolean(HikeConstants.SSL_PREF, !(isIndianUser || isSAUser));
				editor.remove(HikeMessengerApp.TEMP_COUNTRY_CODE);
				editor.commit();

				HikeMessengerApp.setIndianUser(isIndianUser);
				/*
				 * Update the urls to use ssl or not.
				 */
				Utils.setupUri(this.getApplicationContext());


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

	@Override
	public void onClick(View v) {
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
	public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event)
	{
		if ((actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				&& !TextUtils.isEmpty(enterEditText.getText().toString().trim()) && (loadingLayout == null || loadingLayout.getVisibility() != View.VISIBLE))
		{
			{
				submitClicked();
				Utils.hideSoftKeyboard(this, enterEditText);
			}
		}
		return true;
	}

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
		Logger.d("AddAccountActivity", "Current State " + mCurrentState.state.name() + " VALUE: " + value);

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
				//TODO: close activity properly???
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
				toggleActionBarElementsEnable(true);
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

		case ERROR:
			if (value == null || !value.equals(HikeConstants.CHANGE_NUMBER))
			{
				showErrorMsg();
			}
			break;

		}
		setListeners();
	}

	public void autoFillPin(String pin)
	{
		if (viewFlipper.getDisplayedChild() == PIN)
		{
			enterEditText.setText(pin);
			submitClicked();
		}
	}

	public int getDisplayItem()
	{
		return viewFlipper.getDisplayedChild();
	}
	

}
