package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.UpdateAppBaseActivity;
import com.bsb.hike.view.MSISDNView;

public class SignupActivity extends UpdateAppBaseActivity implements SignupTask.OnSignupTaskProgressUpdate, OnEditorActionListener, TextWatcher, OnClickListener
{

	private SignupTask mTask;

	private StateValue mCurrentState;

	private ViewFlipper viewFlipper;
	private ViewGroup numLayout;
	private ViewGroup pinLayout;
	private ViewGroup nameLayout;
	private ViewGroup booBooLayout;
	private ViewGroup numberContainer;

	private ImageView infoTxt;
	private ImageView loadingText;
	private ViewGroup loadingLayout;
	private EditText enterEditText;
	private ImageButton tapHereText;
	private ImageButton submitBtn;
	private ImageView invalidNum;

	private ImageButton tryAgainBtn;
	private Handler mHandler;

	private boolean addressBookError = false;

	private final int NAME = 2;
	private final int PIN = 1;
	private final int NUMBER = 0;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.signup);
		
		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		numLayout = (ViewGroup) findViewById(R.id.num_layout);
		pinLayout = (ViewGroup) findViewById(R.id.pin_layout);
		nameLayout = (ViewGroup) findViewById(R.id.name_layout);
		booBooLayout = (ViewGroup) findViewById(R.id.boo_boo_layout);
		tryAgainBtn = (ImageButton) findViewById(R.id.btn_try_again);

		if(savedInstanceState != null)
		{
			int dispChild = savedInstanceState.getInt(HikeConstants.Extras.SIGNUP_PART);
			removeAnimation();
			viewFlipper.setDisplayedChild(dispChild);
			switch(dispChild)
			{
			case NUMBER:
				prepareLayoutForFetchingNumber();
				break;
			case PIN:
				prepareLayoutForGettingPin();
				break;
			case NAME:
				prepareLayoutForGettingName();
				break;
			}
			if(savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING))
			{
				startLoading();
			}
			if(savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_ERROR))
			{
				showErrorMsg();
			}
			enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
		}
		else
		{
			if(getIntent().getBooleanExtra(HikeConstants.Extras.MSISDN, false))
			{
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName();
			}
			else
			{
				prepareLayoutForFetchingNumber();
			}
			
		}
		setAnimation();
		setListeners();
		mTask = SignupTask.startTask(this);
	}

	@Override
	public void onFinish(boolean success)
	{
		if (success)
		{	
			mHandler.postDelayed(new Runnable() 
			{
				@Override
				public void run() 
				{
					Intent intent = new Intent(SignupActivity.this, MessagesList.class);
					startActivity(intent);
					finish();
				}
			}, 2500);
		}
		else if(mCurrentState.value != null && mCurrentState.value.equals(HikeConstants.CHANGE_NUMBER))
		{
			restartTask();
		}
	}

	public void onClick(View v)
	{
		if (v.getId() == submitBtn.getId()) 
		{
			submitClicked();
		}
		else if(v.getId() == tryAgainBtn.getId())
		{
			restartTask();
		}
		else if(tapHereText != null && v.getId() == tapHereText.getId())
		{
			mTask.addUserInput("");
		}
	}
	
	private void startLoading()
	{
		loadingLayout.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.GONE);
		if(invalidNum != null)
		{
			invalidNum.setVisibility(View.GONE);
		}
		if (tapHereText != null) {
			tapHereText.setVisibility(View.GONE);
		}
	}

	private void submitClicked()
	{
		startLoading();
		if (!addressBookError) 
		{
			if (viewFlipper.getDisplayedChild() == NUMBER && !enterEditText.getText().toString().matches("[0-9]{10,13}")) 
			{
				loadingLayout.setVisibility(View.GONE);
				submitBtn.setVisibility(View.VISIBLE);
				invalidNum.setVisibility(View.VISIBLE);
			}
			else
			{
				mTask.addUserInput(enterEditText.getText().toString());
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
			break;
		case R.id.num_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_num);
			break;
		case R.id.pin_layout:
			enterEditText = (EditText) layout.findViewById(R.id.et_enter_pin);
			break;
		}
		infoTxt = (ImageView) layout.findViewById(R.id.txt_img1);
		loadingText = (ImageView) layout.findViewById(R.id.txt_loading);
		loadingLayout = (ViewGroup) layout.findViewById(R.id.loading_layout);
		tapHereText = (ImageButton) layout.findViewById(R.id.wrong_num);
		submitBtn = (ImageButton) layout.findViewById(R.id.btn_continue);
		numberContainer = (LinearLayout) layout.findViewById(R.id.msisdn_container);
		invalidNum = (ImageView) layout.findViewById(R.id.invalid_num);

		loadingLayout.setVisibility(View.GONE);
		submitBtn.setVisibility(View.VISIBLE);
	}

	private void prepareLayoutForFetchingNumber()
	{
		initializeViews(numLayout);
		invalidNum.setVisibility(View.INVISIBLE);
	}

	private void prepareLayoutForGettingPin()
	{
		initializeViews(pinLayout);
		infoTxt.setImageResource(R.drawable.enter_pin);
		tapHereText.setOnClickListener(this);
	}

	private void prepareLayoutForGettingName()
	{
		initializeViews(nameLayout);

		String msisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, null);
		MSISDNView v = new MSISDNView(SignupActivity.this, msisdn);
		numberContainer.addView(v);
	}
	
	private void resetViewFlipper()
	{
		booBooLayout.setVisibility(View.GONE);
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
		loadingLayout.setVisibility(View.GONE);
		submitBtn.setVisibility(View.VISIBLE);
		booBooLayout.setVisibility(View.VISIBLE);
		viewFlipper.setVisibility(View.GONE);
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(enterEditText.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private void setListeners()
	{
		if (this.enterEditText.getText().length() == 0) 
		{
			submitBtn.setEnabled(false);
		}
		enterEditText.setOnEditorActionListener(this);
		enterEditText.addTextChangedListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(HikeConstants.Extras.SIGNUP_PART, viewFlipper.getDisplayedChild());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, booBooLayout.getVisibility() == View.VISIBLE);
		outState.putString(HikeConstants.Extras.SIGNUP_TEXT, enterEditText.getText().toString());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		if(mTask!=null)
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
		String value = stateValue.value;
		mCurrentState = stateValue;
		Log.d("SignupActivity", "Current State " + mCurrentState.state.name() +" VALUE: "+value);

		if(mHandler == null)
		{
			mHandler = new Handler();
		}
		
		switch(stateValue.state)
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
				prepareLayoutForGettingName();
				setAnimation();
			}
			else
			{
				/* yay, got the actual MSISDN */
				viewFlipper.setDisplayedChild(NAME);
				prepareLayoutForGettingName();
			}
			break;
		case PIN:
			//Wrong Pin
			if(value != null && value.equals(HikeConstants.PIN_ERROR))
			{
				infoTxt.setImageResource(R.drawable.wrong_pin);
				loadingLayout.setVisibility(View.GONE);
				submitBtn.setVisibility(View.VISIBLE);
				if (tapHereText != null) 
				{
					tapHereText.setVisibility(View.VISIBLE);
				}
				enterEditText.setText("");
			}
			//Manual entry for pin
			else
			{
				viewFlipper.setDisplayedChild(PIN);
				prepareLayoutForGettingPin();
				setAnimation();
			}
			break;
		case NAME:
			if (TextUtils.isEmpty(value))
			{
				prepareLayoutForGettingName();
			}
			else
			{
				mHandler.postDelayed(new Runnable() 
				{
					
					@Override
					public void run() 
					{
						loadingText.setImageResource(R.drawable.getting_you_in);
					}
				}, 1000);
			}
			break;
		case ERROR:
			if (value != null && value.equals(HikeConstants.ADDRESS_BOOK_ERROR)) 
			{
				addressBookError = true;
				if(loadingLayout.getVisibility() == View.VISIBLE)
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
	public void afterTextChanged(Editable s) {
		if(enterEditText.getText().length()==0)
		{
			submitBtn.setEnabled(false);
		}
		else
		{
			submitBtn.setEnabled(true);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
		if((actionId == EditorInfo.IME_ACTION_DONE 
				|| event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				&& enterEditText.getText().length()>0 
				&& loadingLayout.getVisibility() != View.VISIBLE)
		{
			submitClicked();
		}
		return true;
	}
}