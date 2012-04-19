package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;

import com.bsb.hike.R;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.State;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.UpdateAppBaseActivity;
import com.bsb.hike.view.CustomLinearLayout;
import com.bsb.hike.view.CustomLinearLayout.OnSoftKeyboardListener;

public class SignupActivity extends UpdateAppBaseActivity implements FinishableEvent
{

	private SignupTask mTask;
	
	private StateValue mCurrentState;
	
	private boolean isErrorMsg = false;

	private ViewFlipper viewFlipper;
	private CustomLinearLayout pullingNoLayout;
	private CustomLinearLayout scanContactsLayout;
	private CustomLinearLayout getNameLayout;
	
	private ImageView mainIcon;
	private TextView loadingText;
	private RelativeLayout loadingLayout;
	private TextView successText1;
	private TextView successText2;
	private TextView enterText;
	private EditText enterEditText;

	private TextView num1Text;
	private TextView num2Text;
	private TextView num3Text;

	private Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup);

		viewFlipper = (ViewFlipper) findViewById(R.id.signup_viewflipper);
		pullingNoLayout = (CustomLinearLayout) findViewById(R.id.pulling_no_layout);
		scanContactsLayout = (CustomLinearLayout) findViewById(R.id.scanning_contact_layout);
		getNameLayout = (CustomLinearLayout) findViewById(R.id.getting_name_layout);
		
		pullingNoLayout.setOnSoftKeyboardListener(onSoftKeyboardListener);
		scanContactsLayout.setOnSoftKeyboardListener(onSoftKeyboardListener);
		getNameLayout.setOnSoftKeyboardListener(onSoftKeyboardListener);
		
		num1Text = (TextView) findViewById(R.id.num1);
		num2Text = (TextView) findViewById(R.id.num2);
		num3Text = (TextView) findViewById(R.id.num3);

		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_right));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_left));

		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof SignupTask)
		{
			mTask = (SignupTask) retained;
		}
		else
		{
			initializeViews(pullingNoLayout);
			prepareLayoutForFetchingNumber();
			mTask = new SignupTask(this);
			mTask.execute();
		}
	}

	@Override
	public void onFinish(boolean success)
	{
		if (success)
		{	
			
			mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					Intent intent = new Intent(SignupActivity.this, MessagesList.class);
					startActivity(intent);
					finish();
				}
			}, 1500);
		}
	}

	CustomLinearLayout.OnSoftKeyboardListener onSoftKeyboardListener = new OnSoftKeyboardListener() {

		@Override
		public void onShown() {
			mainIcon.setVisibility(View.GONE);
		}

		@Override
		public void onHidden() {
			mainIcon.setVisibility(View.VISIBLE);
		}
	};

	private void initializeViews(LinearLayout layout)
	{
		mainIcon = (ImageView) layout.findViewById(R.id.ic_big);
		loadingText = (TextView) layout.findViewById(R.id.txt_loading);
		loadingLayout = (RelativeLayout) layout.findViewById(R.id.loading_layout);
		successText1 = (TextView) layout.findViewById(R.id.txt_success_1);
		successText2 = (TextView) layout.findViewById(R.id.txt_success_2);
		enterText = (TextView) layout.findViewById(R.id.txt_enter);
		enterEditText = (EditText) layout.findViewById(R.id.et_enter);
	}

	private void prepareLayoutForFetchingNumber()
	{
		setStepNo(num1Text);

		mainIcon.setVisibility(View.VISIBLE);
		mainIcon.setImageResource(R.drawable.ic_phone_big);
		loadingLayout.setVisibility(View.VISIBLE);
		loadingText.setText(R.string.pulling_digits);
		successText1.setVisibility(View.GONE);
		successText2.setVisibility(View.GONE);
		enterText.setVisibility(View.GONE);
		enterEditText.setVisibility(View.GONE);
	}

	private void prepareLayoutForScanningContacts()
	{
		mainIcon.setVisibility(View.VISIBLE);
		mainIcon.setImageResource(R.drawable.ic_scanning_big);
		loadingLayout.setVisibility(View.VISIBLE);
		loadingText.setText(R.string.scanning_contacts);
		successText1.setVisibility(View.GONE);
		successText2.setVisibility(View.GONE);
		enterText.setVisibility(View.GONE);
		enterEditText.setVisibility(View.GONE);
	}

	private void prepareLayoutForGettingName()
	{
		setStepNo(num3Text);
		mainIcon.setVisibility(View.VISIBLE);
		mainIcon.setImageResource(R.drawable.ic_name_big);
		loadingLayout.setVisibility(View.GONE);
		successText1.setVisibility(View.GONE);
		successText2.setVisibility(View.GONE);
		enterText.setVisibility(View.VISIBLE);
		enterEditText.setVisibility(View.VISIBLE);
		enterEditText.setBackgroundResource(R.drawable.tb_name);
		enterText.setText(R.string.what_name);
		enterEditText.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
	}

	private void prepareLayoutForGettingPin()
	{
		setStepNo(num1Text);

		mainIcon.setVisibility(View.VISIBLE);
		mainIcon.setImageResource(R.drawable.ic_phone_big);
		loadingLayout.setVisibility(View.GONE);
		successText1.setVisibility(View.GONE);
		successText2.setVisibility(View.GONE);
		enterText.setVisibility(View.VISIBLE);
		enterEditText.setVisibility(View.VISIBLE);
		enterText.setText(R.string.enter_pin);
		enterEditText.setBackgroundResource(R.drawable.tb_pin);
		enterEditText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
	}
	
	private void setStepNo(TextView tv)
	{
		num1Text.setBackgroundDrawable(null);
		num1Text.setTextColor(getResources().getColor(R.color.white));
		num2Text.setBackgroundDrawable(null);
		num2Text.setTextColor(getResources().getColor(R.color.white));
		num3Text.setBackgroundDrawable(null);
		num3Text.setTextColor(getResources().getColor(R.color.white));
		
		tv.setBackgroundResource(R.drawable.bg_number);
		tv.setTextColor(getResources().getColor(R.color.signup_blue));
	}
	
	private void finishSignupProcess()
	{
		mainIcon.setVisibility(View.VISIBLE);
		mainIcon.setImageResource(R.drawable.ic_tick_big);
		loadingLayout.setVisibility(View.GONE);
		successText1.setVisibility(View.VISIBLE);
		successText2.setVisibility(View.GONE);
		successText1.setText(R.string.all_set);
		enterText.setVisibility(View.GONE);
		enterEditText.setVisibility(View.GONE);
	}

	public void onProgressUpdate(StateValue stateValue)
	{
		String value = stateValue.value;
		mCurrentState = stateValue;
		if (value == null)
		{
			Log.w("SignupActivity", "Error in state " + mCurrentState.state.name());
			mTask.cancelTask();
			mTask = null;
			
			prepareLayoutForFetchingNumber();
			/*
			 * In case the state is ERROR we are restart the SignupTask when the user clicks on the OK button, but for other states we need to start the task here itself
			 */
			if (mCurrentState.state != State.ERROR)
			{
				mTask = new SignupTask(this);
				mTask.execute();
			}

			return;
		}

		TextView text;
		
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
				/* couldn't auto-detect MSISDN, prompt the user via a popup */
				initializeViews(pullingNoLayout);
				mainIcon.setVisibility(View.VISIBLE);
				mainIcon.setImageResource(R.drawable.ic_phone_big);
				loadingLayout.setVisibility(View.GONE);
				successText1.setVisibility(View.GONE);
				successText2.setVisibility(View.GONE);
				enterText.setVisibility(View.VISIBLE);
				enterEditText.setVisibility(View.VISIBLE);
				enterText.setText(R.string.enter_number);
				enterEditText.setBackgroundResource(R.drawable.tb_phone);
				enterEditText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
			}
			else
			{
				/* yay, got the actual MSISDN */
				initializeViews(pullingNoLayout);
				mainIcon.setVisibility(View.VISIBLE);
				mainIcon.setImageResource(R.drawable.ic_tick_big);
				loadingLayout.setVisibility(View.GONE);
				successText1.setVisibility(View.VISIBLE);
				successText2.setVisibility(View.VISIBLE);
				successText1.setText(R.string.phone_number);
				successText2.setText(value);
				enterText.setVisibility(View.GONE);
				enterEditText.setVisibility(View.GONE);

				mHandler.postDelayed(flipView, 1500);
			}
			break;
		case ADDRESSBOOK:
			// Finished scanning for contacts
			initializeViews(scanContactsLayout);
			setStepNo(num2Text);
			mainIcon.setVisibility(View.VISIBLE);
			mainIcon.setImageResource(R.drawable.ic_tick_big);
			loadingLayout.setVisibility(View.GONE);
			successText1.setVisibility(View.VISIBLE);
			successText2.setVisibility(View.VISIBLE);
			successText1.setText(R.string.got_it);
			successText2.setText(R.string.one_more_step);
			enterText.setVisibility(View.GONE);
			enterEditText.setVisibility(View.GONE);

			mHandler.postDelayed(flipView, 1500);
			break;
		case NAME:
			if (TextUtils.isEmpty(value))
			{
				//Manual entry for name
				initializeViews(getNameLayout);
				prepareLayoutForGettingName();
			}
			break;
		case PIN:
			//Manual entry for pin
			initializeViews(pullingNoLayout);
			prepareLayoutForGettingPin();
			break;
		}
		enterEditText.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_DONE && enterEditText.getText().length()>0)
				{
					mTask.addUserInput(enterEditText.getText().toString());
					if (mCurrentState.state != State.NAME) 
					{
						enterEditText.setText("");
					}
					else
					{
						finishSignupProcess();
					}
					if(mCurrentState.state == State.MSISDN)
					{
						prepareLayoutForFetchingNumber();
					}
				}
				return false;
			}
		});
	}
	
	private Runnable flipView = new Runnable() {
		
		@Override
		public void run() {
			if(viewFlipper != null)
			{
				viewFlipper.showNext();
				switch (mCurrentState.state) {
				case MSISDN:
					initializeViews(scanContactsLayout);
					prepareLayoutForScanningContacts();
					setStepNo(num2Text);
					break;

				case NAME:
					setStepNo(num3Text);
					break;
				}
			}
		}
	};
}
