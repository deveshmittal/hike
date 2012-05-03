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
import com.bsb.hike.utils.Utils;

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

	private static int NAME = 2;
	private static int PIN = 1;
	private static int NUMBER = 0;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.signup);
		
		Utils.setCorrectOrientation(this);
		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		numLayout = (ViewGroup) findViewById(R.id.num_layout);
		pinLayout = (ViewGroup) findViewById(R.id.pin_layout);
		nameLayout = (ViewGroup) findViewById(R.id.name_layout);
		booBooLayout = (ViewGroup) findViewById(R.id.boo_boo_layout);
		tryAgainBtn = (Button) findViewById(R.id.btn_try_again);

		startTask();

		if(getIntent().getBooleanExtra(HikeConstants.Extras.MSISDN, false))
		{
			viewFlipper.setDisplayedChild(NAME);
			initializeViews(nameLayout);
			prepareLayoutForGettingName();
		}
		else
		{
			initializeViews(numLayout);
			prepareLayoutForFetchingNumber();
		}
		setAnimation();
		setListeners();
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
	
	private void submitClicked()
	{
		loadingLayout.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.GONE);
		wrongNumText.setVisibility(View.GONE);
		tapHereText.setVisibility(View.GONE);
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
		
		enterText1.setVisibility(View.VISIBLE);
		enterText2.setVisibility(View.VISIBLE);
		enterEditText.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.VISIBLE);
		
		enterText1.setText(R.string.old_fashioned);
		enterText2.setText(R.string.enter_num);
		enterEditText.setText("");
		enterEditText.requestFocus();
		enterEditText.setHint(R.string.phone_num);
		enterEditText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
		loadingText.setText(R.string.waiting_pin);
		submitBtn.setText(R.string.next);
	}

	private void prepareLayoutForGettingPin()
	{
		initializeViews(pinLayout);
		hideAllViews();

		enterText1.setVisibility(View.VISIBLE);
		enterText2.setVisibility(View.VISIBLE);
		enterEditText.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.VISIBLE);
		wrongNumText.setVisibility(View.VISIBLE);
		tapHereText.setVisibility(View.VISIBLE);

		enterText1.setText(R.string.enter_pin1);
		enterText2.setText(R.string.enter_pin2);
		loadingText.setText(R.string.verifying_pin);
		submitBtn.setText(R.string.next);
		enterEditText.setText("");
		enterEditText.requestFocus();
		enterEditText.setHint(R.string.pin);
		enterEditText.setBackgroundResource(R.drawable.tb_pin);
		enterEditText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);

		tapHereText.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Log.d("SignupActivity", "CHANGE MY NUMBER!!!");
				mTask.addUserInput("");
			}
		});
	}

	private void prepareLayoutForGettingName()
	{
		initializeViews(nameLayout);
		hideAllViews();

		enterText1.setVisibility(View.VISIBLE);
		enterText2.setVisibility(View.VISIBLE);
		enterEditText.setVisibility(View.VISIBLE);
		submitBtn.setVisibility(View.VISIBLE);
		
		if (mTask != null) 
		{
			enterText1.setText(getString(R.string.current_num) + " "
					+ mTask.msisdn);
		}
		enterText2.setText(R.string.enter_name);
		enterEditText.requestFocus();
		enterEditText.setHint(R.string.name);
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
	
	private void startTask()
	{
		resetViewFlipper();
		mTask = SignupTask.startTask(this);
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
	protected void onDestroy() {
		// Manually canceling the task since sometimes the task would not start after exiting and starting the app again.
		if(mTask!=null)
		{
			mTask.cancelTask();
			mTask = null;
		}
		super.onDestroy();
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
			Log.d("SignupActivity", "Received state " + value);
			if (TextUtils.isEmpty(value))
			{
				Log.d("SignupActivity", "NO MSISDN");
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
				Log.d("SignupActivity", "HAVE MSISDN");
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
		if((actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && enterEditText.getText().length()>0)
		{
			submitClicked();
		}
		return true;
	}
}