package com.bsb.hike.ui;

import java.io.UnsupportedEncodingException;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.AccountUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

public class AccountCreateStep extends Activity implements OnCheckedChangeListener, OnClickListener {

	private String mName;
	private EditText mNameField;
	private CheckBox mCheckBox;
	private Button mNextButton;
	private ProgressDialog mDialog;

	private class AccountTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... arg0) {
			String token = null;
			Log.d("AccountCreate", "creating account for: "+mName);
			try {
				token = AccountUtils.registerAccount(mName);
			} catch (UnsupportedEncodingException e) {
				//their name was all weird ... shouldn't happen
				e.printStackTrace();
			}

			Log.d("AccountCreate", "account created token: "+token);
			if (token == null) {
				return Boolean.FALSE;
			}

			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(HikeMessengerApp.TOKEN_SETTING, token);
			editor.commit();

			return Boolean.TRUE;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mDialog.dismiss();
			finish();
			//start new intent
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

 	   setContentView(R.layout.accountcreatestep);

       mNameField = (EditText) findViewById(R.id.name_field);
       mCheckBox = (CheckBox) findViewById(R.id.checkbox_tc);
       mNextButton = (Button) findViewById(R.id.next);

       SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
       String msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
       String carrierName = settings.getString(HikeMessengerApp.CARRIER_SETTING, null);

        //format the label
        Resources res = getResources();
        String text = String.format(res.getString(R.string.enter_name_message), carrierName, msisdn);
        Log.d("AccountCreateStep", "Formatted text is" + text);
        TextView label = (TextView) findViewById(R.id.enter_name_message);
        label.setText(text);

        mNameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
        	@Override
        	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        		if (actionId == EditorInfo.IME_ACTION_DONE) {
        			Log.d("create", "called");
        			updateNextButtonState();
        			return false;
        		}
        		return false;
        	}
        });

        mCheckBox.setOnCheckedChangeListener(this);
        mNextButton.setOnClickListener(this);
	}

	private void updateNextButtonState() {
		mNextButton.setEnabled((mNameField.getText().length() > 0) && mCheckBox.isChecked());
	}

	@Override
	public void onCheckedChanged(CompoundButton btn, boolean val) {
		Log.d("AccountCreateStep", "onCheck: " +btn);
		updateNextButtonState();
	}

	@Override
	public void onClick(View view) {
		Log.d("AccountCreateStep", "onClick called: "+view);
		if (view == mNextButton) {
			SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		    SharedPreferences.Editor editor = settings.edit();
		    mName = mNameField.getText().toString().trim();
		    editor.putString(HikeMessengerApp.NAME_SETTING, mName);
		    editor.commit();

		    mDialog = ProgressDialog.show(this, "almost done", "creating account");
		    AccountTask task = new AccountTask();
		    task.execute();
		}
	}

}
