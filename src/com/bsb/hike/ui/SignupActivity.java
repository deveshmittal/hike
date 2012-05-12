package com.bsb.hike.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.UpdateAppBaseActivity;

public class SignupActivity extends UpdateAppBaseActivity implements SignupTask.OnSignupTaskProgressUpdate, OnEditorActionListener, TextWatcher
{

	private SignupTask mTask;

	private StateValue mCurrentState;

	private ViewFlipper viewFlipper;
	private ViewGroup numLayout;
	private ViewGroup pinLayout;
	private ViewGroup nameLayout;
	private ViewGroup booBooLayout;
	
	private TextView loadingText;
	private ViewGroup loadingLayout;
	private TextView enterText1;
	private TextView enterText2;
	private EditText enterEditText;
	private TextView wrongNumText;
	private Button tapHereText;
	private Button submitBtn;

	private Button tryAgainBtn;
	private Handler mHandler;

	private boolean addressBookError = false;

	private static final int NAME = 2;
	private static final int PIN = 1;
	private static final int NUMBER = 0;
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
		tryAgainBtn = (Button) findViewById(R.id.btn_try_again);

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
			setAnimation();
			if(savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING))
			{
				startLoading();
			}
			enterEditText.setText(savedInstanceState.getString(HikeConstants.Extras.SIGNUP_TEXT));
			if(savedInstanceState.getBoolean(HikeConstants.Extras.SIGNUP_ERROR))
			{
				showErrorMsg();
			}
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
			setAnimation();
			setListeners();
		}
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
	}
	
	private void startLoading()
	{
		loadingLayout.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.GONE);
		wrongNumText.setVisibility(View.GONE);
		tapHereText.setVisibility(View.GONE);
	}

	private void submitClicked()
	{
		startLoading();
		if (!addressBookError) 
		{
			mTask.addUserInput(enterEditText.getText().toString());
		} 
		else 
		{
			showErrorMsg();
			addressBookError = false;
		}
	}

	private void initializeViews(ViewGroup layout)
	{
		enterText1 = (TextView) layout.findViewById(R.id.enter_txt1);
		enterText2 = (TextView) layout.findViewById(R.id.enter_txt2);
		loadingText = (TextView) layout.findViewById(R.id.txt_loading);
		loadingLayout = (ViewGroup) layout.findViewById(R.id.loading_layout);
		enterEditText = (EditText) layout.findViewById(R.id.et_enter);
		wrongNumText = (TextView) layout.findViewById(R.id.txt_wrong_number);
		tapHereText = (Button) layout.findViewById(R.id.txt_tap_here);
		submitBtn = (Button) layout.findViewById(R.id.btn_continue);
	}

	private void prepareLayoutForFetchingNumber()
	{
		initializeViews(numLayout);
		hideAllViews();
		showCommonViews();
		
		enterText1.setText(R.string.old_fashioned);
		enterText2.setText(R.string.enter_num);
		enterEditText.setHint(R.string.phone_num);
		enterEditText.setBackgroundResource(R.drawable.tb_phone);
		enterEditText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
		loadingText.setText(R.string.waiting_pin);
		submitBtn.setText(R.string.next);
	}

	private void prepareLayoutForGettingPin()
	{
		initializeViews(pinLayout);
		hideAllViews();
		showCommonViews();

		wrongNumText.setVisibility(View.VISIBLE);
		tapHereText.setVisibility(View.VISIBLE);

		enterText1.setText(R.string.enter_pin1);
		enterText2.setText(R.string.enter_pin2);
		loadingText.setText(R.string.verifying_pin);
		submitBtn.setText(R.string.next);
		enterEditText.setHint(R.string.pin);
		enterEditText.setBackgroundResource(R.drawable.tb_pin);
		enterEditText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);

		tapHereText.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				mTask.addUserInput("");
			}
		});
	}

	private void prepareLayoutForGettingName()
	{
		initializeViews(nameLayout);
		hideAllViews();
		showCommonViews();

		if (mTask != null) 
		{
			String currentNum = getString(R.string.current_num);
			SpannableString spannableString = new SpannableString(currentNum + " " + mTask.msisdn);
			
			int start = currentNum.length() + 1;
			spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, start + mTask.msisdn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			enterText1.setText(spannableString);
		}
		enterText2.setText(R.string.enter_name);
		enterEditText.setHint(R.string.name);
		enterEditText.setBackgroundResource(R.drawable.tb_name);
		enterEditText.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
		loadingText.setText(R.string.scanning_contacts);
		submitBtn.setText(R.string.done);
	}

	private void hideAllViews() {
		enterText1.setVisibility(View.GONE);
		enterText2.setVisibility(View.GONE);
		loadingLayout.setVisibility(View.GONE);
		enterEditText.setVisibility(View.GONE);
		wrongNumText.setVisibility(View.GONE);
		tapHereText.setVisibility(View.GONE);
		submitBtn.setVisibility(View.GONE);
	}
	
	private void showCommonViews()
	{
		enterText1.setVisibility(View.VISIBLE);
		enterText2.setVisibility(View.VISIBLE);
		enterEditText.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.VISIBLE);
		enterEditText.requestFocus();
		setListeners();
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
		outState.putString(HikeConstants.Extras.SIGNUP_TEXT, enterEditText.getText().toString());
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, booBooLayout.getVisibility() == View.VISIBLE);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		if(mTask!=null)
			{
				mTask.cancelTask();
				mTask = null;
			}
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
			//Manual entry for pin
			viewFlipper.setDisplayedChild(PIN);
			initializeViews(pinLayout);
			prepareLayoutForGettingPin();
			setAnimation();
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
						loadingText.setText(R.string.saving_name);
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
		if((actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && enterEditText.getText().length()>0 && loadingLayout.getVisibility() != View.VISIBLE)
		{
			submitClicked();
		}
		return true;
	}
}