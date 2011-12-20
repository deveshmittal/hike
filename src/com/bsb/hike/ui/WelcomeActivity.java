package com.bsb.hike.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;

public class WelcomeActivity extends Activity {
	public class AccountCreateActivity extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... arg0) {
			if (mMSISDNText.getVisibility() == View.VISIBLE) {
				String msisdn = mMSISDNText.getText().toString();
				AccountUtils.MSISDN = msisdn;
			}

			AccountUtils.AccountInfo accountInfo = AccountUtils.registerAccount();

			if (accountInfo != null) {
				String token = accountInfo.token;
				String msisdn = accountInfo.msisdn;
				AccountUtils.setToken(token);
				SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString(HikeMessengerApp.TOKEN_SETTING, token);
				editor.putString(HikeMessengerApp.MSISDN_SETTING, msisdn);
				editor.commit();
			}

			return Boolean.TRUE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			startActivity(new Intent(WelcomeActivity.this, AccountCreateSuccess.class));
			finish();
		}
	}

	private ProgressDialog mDialog;
	private Button mAcceptButton;
	private ImageView mIconView;
	private EditText mMSISDNText;

	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);
		mAcceptButton = (Button) findViewById(R.id.accept_tc);
		mIconView = (ImageView) findViewById(R.id.ic_edit_message);
		mMSISDNText = (EditText) findViewById(R.id.debug_msisdn_input);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDialog != null) {
           mDialog.dismiss();
           mDialog = null;
		}
	}
	public void onClick(View v) {
		if (v == mAcceptButton) {
			mDialog = ProgressDialog.show(this, null, getText(R.string.determining_phone_number));
			AccountCreateActivity aca = new AccountCreateActivity();
			aca.execute();
		} else if (v == mIconView) {
			Log.w("DEBGUG", "Adding Debug Input MSISDN");
			mMSISDNText.setVisibility(View.VISIBLE);
		}
	}
}
