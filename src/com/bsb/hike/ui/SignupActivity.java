package com.bsb.hike.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
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
	private TextView wrongNumText;
	private TextView tapHereText;

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

		setAnimation();

		restartService();
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
			}, 100);
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
		wrongNumText = (TextView) layout.findViewById(R.id.txt_wrong_number);
		tapHereText = (TextView) layout.findViewById(R.id.txt_tap_here);
	}

	private void prepareLayoutForFetchingNumber()
	{
		setStepNo(num1Text);

		hideAllViews();
		mainIcon.setVisibility(View.VISIBLE);
		mainIcon.setImageResource(R.drawable.ic_phone_big);
		loadingLayout.setVisibility(View.VISIBLE);
		loadingText.setText(R.string.pulling_digits);
	}

	private void prepareLayoutForScanningContacts()
	{
		hideAllViews();
		mainIcon.setVisibility(View.VISIBLE);
		mainIcon.setImageResource(R.drawable.ic_scanning_big);
		loadingLayout.setVisibility(View.VISIBLE);
		loadingText.setText(R.string.scanning_contacts);
	}

	private void prepareLayoutForGettingName()
	{
		hideAllViews();
		prepareLayoutForAcceptingInput();
		mainIcon.setImageResource(R.drawable.ic_name_big);
		enterEditText.setBackgroundResource(R.drawable.tb_name);
		enterText.setText(R.string.what_name);
		enterEditText.setInputType(EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS);
	}

	private void prepareLayoutForGettingPin()
	{
		setStepNo(num1Text);

		hideAllViews();
		prepareLayoutForAcceptingInput();
		mainIcon.setImageResource(R.drawable.ic_phone_big);
		enterText.setText(R.string.enter_pin);
		enterEditText.setBackgroundResource(R.drawable.tb_pin);
		enterEditText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
		wrongNumText.setVisibility(View.VISIBLE);
		tapHereText.setVisibility(View.VISIBLE);
		tapHereText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				restartService();
			}
		});
	}
	
	private void prepareLayoutForAcceptingInput()
	{
		mainIcon.setVisibility(View.VISIBLE);
		enterText.setVisibility(View.VISIBLE);
		enterEditText.setVisibility(View.VISIBLE);
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
		hideAllViews();
		mainIcon.setVisibility(View.VISIBLE);
		mainIcon.setImageResource(R.drawable.ic_tick_big);
		successText1.setVisibility(View.VISIBLE);
		successText1.setText(R.string.all_set);
	}
	
	private void hideAllViews()
	{
		mainIcon.setImageDrawable(null);
		mainIcon.setVisibility(View.GONE);
		loadingLayout.setVisibility(View.GONE);
		successText1.setVisibility(View.GONE);
		successText2.setVisibility(View.GONE);
		enterText.setVisibility(View.GONE);
		enterEditText.setVisibility(View.GONE);
		wrongNumText.setVisibility(View.GONE);
		tapHereText.setVisibility(View.GONE);
	}
	
	private void restartService()
	{
		initializeViews(pullingNoLayout);
		prepareLayoutForFetchingNumber();
		if(mTask != null)
		{
			mTask.cancelTask();
			mTask = null;
		}
		mTask = new SignupTask(this);
		mTask.execute();
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
	
	public void onProgressUpdate(StateValue stateValue)
	{
		String value = stateValue.value;
		mCurrentState = stateValue;
		Log.w("SignupActivity", "Current State " + mCurrentState.state.name() +" VALUE: "+value);
		if (mCurrentState.state == State.ERROR)
		{
			mTask.cancelTask();
			mTask = null;

			hideAllViews();
			/*
			 * In case the state is ERROR we are restart the SignupTask when the user clicks on the OK button, but for other states we need to start the task here itself
			 */
			Builder builder = new Builder(SignupActivity.this);
			builder.setMessage("Unable to proceed. Check your network and try again.");
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					restartService();
				}
			});
			AlertDialog alertDialog = builder.create();
			try 
			{
				alertDialog.show();
			} catch (BadTokenException e) 
			{
				restartService();
				Log.e("SignupActivity", "Random crash for the alert dialog", e);
			}
			return;
		}

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
				/* couldn't auto-detect MSISDN, prompt the user via a popup */
				initializeViews(pullingNoLayout);
				hideAllViews();
				prepareLayoutForAcceptingInput();
				mainIcon.setImageResource(R.drawable.ic_phone_big);
				enterText.setText(R.string.enter_number);
				enterEditText.setBackgroundResource(R.drawable.tb_phone);
				enterEditText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
			}
			else if (value.equals("Done"))
			{
				removeAnimation();
				viewFlipper.setDisplayedChild(1);
				initializeViews(scanContactsLayout);
				hideAllViews();
				setStepNo(num2Text);
				prepareLayoutForScanningContacts();
				setAnimation();
			}
			else
			{
				Log.d("SignupActivity", "HAVE MSISDN");
				/* yay, got the actual MSISDN */
				initializeViews(pullingNoLayout);
				hideAllViews();
				mainIcon.setVisibility(View.VISIBLE);
				mainIcon.setImageResource(R.drawable.ic_tick_big);
				successText1.setVisibility(View.VISIBLE);
				successText2.setVisibility(View.VISIBLE);
				successText1.setText(R.string.phone_number);
				successText2.setText(value);

				mHandler.postDelayed(flipView, 1500);
			}
			break;
		case ADDRESSBOOK:
			if (value.equals("Done"))
			{
				removeAnimation();
				viewFlipper.setDisplayedChild(2);
				initializeViews(getNameLayout);
				hideAllViews();
				setStepNo(num3Text);
				prepareLayoutForGettingName();
				setAnimation();
			}
			else
			{
				// Finished scanning for contacts
				initializeViews(scanContactsLayout);
				hideAllViews();
				setStepNo(num2Text);
				mainIcon.setVisibility(View.VISIBLE);
				mainIcon.setImageResource(R.drawable.ic_tick_big);
				successText1.setVisibility(View.VISIBLE);
				successText2.setVisibility(View.VISIBLE);
				successText1.setText(R.string.got_it);
				successText2.setText(R.string.one_more_step);
				mHandler.postDelayed(flipView, 1500);
			}
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
					if(mCurrentState.state == State.MSISDN || mCurrentState.state == State.PIN)
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
				Log.w("SignupActivity", "Current State in RUNNABLE " + mCurrentState.state.name());	
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
