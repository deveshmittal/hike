package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

public class AccountCreateSuccess extends Activity implements TextWatcher
{
	private EditText mEditText;
	private TextView mFriendlyNameView;
	private Button mNextButton;

	public void onCreate(Bundle state)
	{
		super.onCreate(state);
		setContentView(R.layout.accountcreatesuccess);
		mEditText = (EditText) findViewById(R.id.nameView);
		mFriendlyNameView = (TextView) findViewById(R.id.namePreview);
		mNextButton = (Button) findViewById(R.id.btnScanAB);

		mEditText.addTextChangedListener(this);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String name = settings.getString(HikeMessengerApp.NAME, null);

		if (!TextUtils.isEmpty(name))
		{
			mEditText.setText(name);
		}

		Intent intent = getIntent();
		String rendered;
		Resources res = getResources();

		/* previous attempt failed, signal user to try again? */
		if (intent.getBooleanExtra("failed", false))
		{
			rendered = res.getString(R.string.address_book_failed);
		}
		else
		{
			/* format message using retrieved msisdn and carrier */
			String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
			TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String carrierName = manager.getNetworkOperatorName();

			rendered = String.format(res.getString(R.string.we_got_you_on_carrier), msisdn, carrierName);
		}

		TextView view = (TextView) findViewById(R.id.textWeGotYouOnCarrier);
		view.setText(rendered);
	}

	public void onClick(View v)
	{
		Log.d("AccountCreateSuccess", "View Clicked");

		/* shouldn't be done on UI thread, but it's complicated */
		SharedPreferences.Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
		editor.putString(HikeMessengerApp.NAME, mEditText.getText().toString());
		editor.commit();

		startActivity(new Intent(this, ScanningAddressBook.class));
		finish();
	}

	@Override
	public void afterTextChanged(Editable chars)
	{
		/* enable the next button only if we have a name */
		mNextButton.setEnabled(!TextUtils.isEmpty(chars));
		/* format the preview field */
		String friendlyName = friendlyName(chars.toString());
		if (TextUtils.isEmpty(friendlyName))
		{
			friendlyName = friendlyName(mEditText.getHint().toString());
		}
		mFriendlyNameView.setText(friendlyName);
	}

	private String friendlyName(String str)
	{
		String string = str.trim();
		String[] parts = string.split(" ");
		int last = parts.length - 1;
		if ((last > 0) && (parts[last].length() >= 1))
		{
			return parts[0] + parts[last].substring(0,1);
		}
		else
		{
			return string;
		}
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

