package com.bsb.hike.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
	private AlertDialog mDialog;
	private StateValue mCurrentState;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup);

		mPullingDigitsView = (ViewGroup) findViewById(R.id.signup_digits);
		mScanningContactsView = (ViewGroup) findViewById(R.id.signup_addressbook);
		mGettingNameView = (ViewGroup) findViewById(R.id.signup_name);

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
			/*
			 * operation successsful,  signal the next screen
			 */
			Intent intent = new Intent(this, MessagesList.class);
			startActivity(intent);
			finish();
		}
	}

	private void createProgressDialog(String title, String labelString)
	{
		assert(mDialog == null);

		/* couldn't auto-detect MSISDN, prompt the user via a popup */
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.customdialog, null);

		TextView label = (TextView) layout.findViewById(R.id.dialog_label);
		label.setText(labelString);

		final EditText editText = (EditText) layout.findViewById(R.id.dialog_edittext);
		editText.setHint(title);

		Button button = (Button) layout.findViewById(R.id.dialog_proceed);
		button.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String text = editText.getText().toString();
				mDialog.dismiss();
				mDialog = null;
				if (mTask != null)
				{
					mTask.addUserInput(text);
				}
			}
		});
		editText.addTextChangedListener(new DialogTextWatcher(button));

		mDialog = new AlertDialog.Builder(this).setView(layout).show();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
		}
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
			/*TODO add a dialog prompting the user to try again */
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
		}

		TextView text;

		switch(stateValue.state)
		{
		case MSISDN:
			Log.d("SignupActivity", "Received state " + value);
			if (TextUtils.isEmpty(value))
			{
				/* couldn't auto-detect MSISDN, prompt the user via a popup */
				createProgressDialog("Phone Number", "old fashion way sucks");
			}
			else
			{
				/* yay, got the actual MSISDN */
				Log.d("SignupActivity", "Setting this view " + mPullingDigitsView + " to visible " + value);
				mScanningContactsView.setVisibility(View.VISIBLE);
				text = (TextView) mPullingDigitsView.findViewById(R.id.signup_text);
				String msisdn = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getString(HikeMessengerApp.MSISDN_SETTING, null);
				text.setText("Great we got you on " + msisdn);
			}
			break;
		case ADDRESSBOOK:
			mGettingNameView.setVisibility(View.VISIBLE);
			text = (TextView) mScanningContactsView.findViewById(R.id.signup_text);
			text.setText("Addressbook Scanned");
			break;
		case NAME:
			if (TextUtils.isEmpty(value))
			{
				createProgressDialog("Name", "What's your name?");
			}
			else
			{
				text = (TextView) mGettingNameView.findViewById(R.id.signup_text);
				text.setText("Hi " + value);
			}
			break;
		}
	}
}
