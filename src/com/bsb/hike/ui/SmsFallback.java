package com.bsb.hike.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;

public class SmsFallback extends Activity
{
	private AccountCreateAsyncTask mTask;
	
	private ProgressDialog mDialog;

	private TextView mTextMsg;

	private TextView mFallbackMsg;

	private EditText mEditTextbox;

	private Button mSendButton; 
	
	private Button mResendPhoneButton;

	private TextView mErrorText;

	boolean isPinCodeScreen;

	public class AccountCreateAsyncTask extends AsyncTask<Void, Void, Boolean>
	{

		@Override
		protected Boolean doInBackground(Void... arg0)
		{
			String text = mEditTextbox.getText().toString();
			if (TextUtils.isEmpty(text))
			{
				return Boolean.FALSE;
			}
			
			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			SharedPreferences.Editor editor = settings.edit();
			if (isPinCodeScreen)
			{
				AccountUtils.AccountInfo accountInfo = null;
				String oldMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
				accountInfo = AccountUtils.registerAccount(text,oldMsisdn);
				if (accountInfo == null)
					return Boolean.FALSE;

				String token = accountInfo.token;
				String uid = accountInfo.uid;
				AccountUtils.setToken(token);
				editor.putString(HikeMessengerApp.TOKEN_SETTING, token);
				editor.putString(HikeMessengerApp.UID_SETTING, uid);
			}
			else
			{
				String msisdn = AccountUtils.validateNumber(text);
				if (TextUtils.isEmpty(msisdn))
				{
					return Boolean.FALSE;
				}
				editor.putString(HikeMessengerApp.MSISDN_SETTING, msisdn);
				editor.putBoolean(HikeMessengerApp.PHONE_NUMBER_ENTERED, true);
			}
			editor.commit();
			/* set the async task to null so the UI doesn't think we're still looking for the MSISDN */
			return Boolean.TRUE;
		}

		@Override
		protected void onPostExecute(Boolean result)
		{
			if (result.booleanValue())
			{
				if (isPinCodeScreen)
				{
					startActivity(new Intent(SmsFallback.this, AccountCreateSuccess.class));
					finish();
				}
				else
				{
					if (mDialog != null)
					{
						mDialog.dismiss();
					}
					isPinCodeScreen = true;
					resetParams(isPinCodeScreen);
				}
			}
			else
			{
				if (mDialog != null)
				{
					mDialog.dismiss();
				}

				mErrorText.setVisibility(View.VISIBLE);
			}
			mTask = null;
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return mTask;
	}
	
	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.smsfallbackscreen);
		mTextMsg = (TextView) findViewById(R.id.enterTextId);
		mEditTextbox = (EditText) findViewById(R.id.insert_text_id);
		mSendButton = (Button) findViewById(R.id.send_text_id);
		mResendPhoneButton = (Button)findViewById(R.id.resendPhoneId);
		mErrorText = (TextView) findViewById(R.id.correctPhoneId);
		mFallbackMsg = (TextView) findViewById(R.id.smsFallbackTextId);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		isPinCodeScreen = settings.getBoolean(HikeMessengerApp.PHONE_NUMBER_ENTERED, false);
		resetParams(isPinCodeScreen);
		
		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof AccountCreateAsyncTask)
		{
			mTask = (AccountCreateAsyncTask) retained;
			mDialog = ProgressDialog.show(this, null, getText(R.string.processing_request));
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
		mTask = null;
	}

	public void onClick(View v)
	{
		if (v == mSendButton)
		{
			mErrorText.setVisibility(View.INVISIBLE);
			mDialog = ProgressDialog.show(this, null, getText(R.string.processing_request));
			mTask = new AccountCreateAsyncTask();
			mTask.execute();
		}
		else if(v == mResendPhoneButton)
		{ 
			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.remove(HikeMessengerApp.PHONE_NUMBER_ENTERED);
			editor.remove(HikeMessengerApp.MSISDN_SETTING);
			editor.commit();
			isPinCodeScreen = false;
			resetParams(isPinCodeScreen);
		}
	}

	private void resetParams(boolean isPinScreen)
	{
		mEditTextbox.setText("");
		mErrorText.setVisibility(View.INVISIBLE);
		if (isPinScreen)
		{
			mTextMsg.setText(getResources().getString(R.string.enter_pin));
			mErrorText.setText(getResources().getString(R.string.invalid_pin));
			mFallbackMsg.setText(getResources().getString(R.string.sms_pin_msg));
			mEditTextbox.setInputType(InputType.TYPE_CLASS_NUMBER);
			mResendPhoneButton.setVisibility(View.VISIBLE);
			mResendPhoneButton.setClickable(true);
			mEditTextbox.setHint(getResources().getString(R.string.pin_hint));
		}
		else
		{
			mTextMsg.setText(getResources().getString(R.string.enter_phone));
			mErrorText.setText(getResources().getString(R.string.invalid_phone));
			mFallbackMsg.setText(getResources().getString(R.string.smsFallbackText));
			mEditTextbox.setInputType(InputType.TYPE_CLASS_PHONE);
			mResendPhoneButton.setVisibility(View.INVISIBLE);
			mResendPhoneButton.setClickable(false);
			mEditTextbox.requestFocus();
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			mEditTextbox.setHint(getResources().getString(R.string.phone_hint));
		}
	}
}
