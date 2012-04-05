package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.tasks.FinishableEvent;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;

public class SignupActivity extends Activity implements FinishableEvent
{

	private class DialogTextWatcher implements TextWatcher
	{

		private Button button;
		public DialogTextWatcher(Button b)
		{
			this.button = b;
		}

		@Override
		public void afterTextChanged(Editable editable)
		{
			this.button.setEnabled(!TextUtils.isEmpty(editable.toString()));
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
		{
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
		{
		}
		
	}

	private SignupTask mTask;
	private ViewGroup mPullingDigitsView;
	private ViewGroup mScanningContactsView;
	private ViewGroup mGettingNameView;
	private ViewGroup mOperatorView;
	private View mDialogOverlay;
	private StateValue mCurrentState;
	private EditText editText;
	
	private ImageView numberStatus;
	private ImageView addressBookStatus;
	private ImageView nameStatus;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup);

		mPullingDigitsView = (ViewGroup) findViewById(R.id.signup_digits);
		mScanningContactsView = (ViewGroup) findViewById(R.id.signup_addressbook);
		mGettingNameView = (ViewGroup) findViewById(R.id.signup_name);
		mOperatorView = (ViewGroup) findViewById(R.id.operator_layout);
		mDialogOverlay = findViewById(R.id.dialog_overlay);

		editText = (EditText) findViewById(R.id.dialog_edittext);
		numberStatus = (ImageView) findViewById(R.id.signup_digits_status);
		addressBookStatus = (ImageView) findViewById(R.id.signup_address_status);
		nameStatus = (ImageView) findViewById(R.id.signup_name_status);
		
		Button button = (Button) mDialogOverlay.findViewById(R.id.dialog_proceed);
		button.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				buttonClickEvent();
			}
		});

		editText.addTextChangedListener(new DialogTextWatcher(button));

		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof SignupTask)
		{
			mTask = (SignupTask) retained;
		}
		else
		{
			mTask = new SignupTask(this);
			mTask.execute();
		}
	}

	@Override
	public void onFinish(boolean success)
	{
		if (success)
		{	
			/**
			 * delaying the next activity from being displayed by a few secs
			 */
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				
				@Override
				public void run()
				{
					/*
					 * operation successsful,  signal the next screen
					 */
					Intent intent = new Intent(SignupActivity.this, MessagesList.class);
					startActivity(intent);
					finish();
				}
			}, 2000);
			
		}
	}
	
	

	private void createProgressDialog(boolean isName)
	{
		/* ensure we're not currently showing a dialog */
		assert(mDialogOverlay.getVisibility() == View.GONE);

		/* couldn't auto-detect MSISDN, prompt the user via a popup */
		

		TextView label = (TextView) mDialogOverlay.findViewById(R.id.dialog_label);
		

		if (editText == null)
		{
			editText = (EditText) mDialogOverlay.findViewById(R.id.dialog_edittext);
		}
		
		if(isName)
		{
			editText.setBackgroundDrawable(getResources().getDrawable(R.drawable.tb_name));
			editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			editText.setHint("Name");
			label.setText("What's your name?");
		} 
		else 
		{
			editText.setBackgroundDrawable(getResources().getDrawable(R.drawable.tb_phone));
			editText.setInputType(InputType.TYPE_CLASS_PHONE);
			editText.setHint("Phone Number");
			label.setText("old fashion way sucks");
		}
		
		editText.setOnKeyListener(new OnKeyListener()
		{
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
				{
					buttonClickEvent();
				}
				return false;
			}
		});
		
		mDialogOverlay.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onDestroy()
	{
		Log.d("SignupActivity", "onDestroy being called");
		super.onDestroy();
		if(mTask!=null){
			mTask.cancel(true);
			mTask.cancelTask();
		}
	}
	
	private void buttonClickEvent() {
		String text = editText.getText().toString();
		editText.setText("");
		mDialogOverlay.setVisibility(View.INVISIBLE);
		mTask.addUserInput(text);
		
		/**
		 * To hide the soft keyboard when the "DONE" key is pressed.
		 */
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
	}
	
	public void onProgressUpdate(StateValue stateValue)
	{
		String value = stateValue.value;
		mCurrentState = stateValue;
		if (value == null)
		{
			Log.w("SignupActivity", "Error in state " + mCurrentState.state.name());
			mTask.cancel(true);
			mTask = null;
			/*TODO add a dialog prompting the user to try again 
			mDialog = new AlertDialog.Builder(this)
			.setTitle("Error")
			.setMessage("Something bad happened, try again?")
			.setCancelable(false)
			.setPositiveButton("OK!", new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if (mTask != null)
					{
						mTask = new SignupTask(SignupActivity.this);
						mTask.execute();	
					}
				}

			})
			.show();
			*/
		}

		TextView text;

		switch(stateValue.state)
		{
		case MSISDN:
			Log.d("SignupActivity", "Received state " + value);
			if (TextUtils.isEmpty(value))
			{
				/* couldn't auto-detect MSISDN, prompt the user via a popup */
				createProgressDialog(false);
			}
			else
			{
				/* yay, got the actual MSISDN */
				Log.d("SignupActivity", "Setting this view " + mPullingDigitsView + " to visible " + value);
				mScanningContactsView.setVisibility(View.VISIBLE);
				text = (TextView) mPullingDigitsView.findViewById(R.id.signup_text);
				String msisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, null);
				text.setText(msisdn);
				text.setTextColor(getResources().getColor(R.color.white));
				mOperatorView.setVisibility(View.VISIBLE);
				numberStatus.setVisibility(View.VISIBLE);
			}
			break;
		case ADDRESSBOOK:
			mGettingNameView.setVisibility(View.VISIBLE);
			text = (TextView) mScanningContactsView.findViewById(R.id.signup_text);
			text.setText("Addressbook Scanned");
			text.setTextColor(getResources().getColor(R.color.white));
			addressBookStatus.setVisibility(View.VISIBLE);
			break;
		case NAME:
			if (TextUtils.isEmpty(value))
			{
				createProgressDialog(true);
			}
			else
			{
				text = (TextView) mGettingNameView.findViewById(R.id.signup_text);
				text.setText("Hi " + value);
				text.setTextColor(getResources().getColor(R.color.white));
				nameStatus.setVisibility(View.VISIBLE);
			}
			break;
		}
	}
}
