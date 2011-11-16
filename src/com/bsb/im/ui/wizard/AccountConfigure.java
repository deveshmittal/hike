package com.bsb.im.ui.wizard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.LoginFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;

import org.jivesoftware.smack.util.StringUtils;

import com.bsb.im.BeemApplication;
import com.bsb.im.ui.CreateAccount;
import com.bsb.im.ui.Login;
import com.bsb.im.ui.Settings;

import com.bsb.im.R;

/**
 * Activity to enter the information required in order to configure a XMPP
 * account.
 * 
 */
public class AccountConfigure extends Activity implements OnTouchListener {

	private static final int MANUAL_CONFIGURATION = 1;
	private Button loginButton;
	private Button backButton;
	private EditText mAccountJID;
	private EditText mAccountPassword;
	private final JidTextWatcher mJidTextWatcher = new JidTextWatcher();
	private final PasswordTextWatcher mPasswordTextWatcher = new PasswordTextWatcher();
	private boolean mValidJid;
	private boolean mValidPassword;

	/**
	 * Constructor.
	 */
	public AccountConfigure() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard_account_configure);
		backButton = (Button) findViewById(R.id.back_button);
		backButton.setOnTouchListener(this);
		loginButton = (Button) findViewById(R.id.create_button);
		loginButton.setOnTouchListener(this);
		mAccountJID = (EditText) findViewById(R.id.account_username);
		mAccountPassword = (EditText) findViewById(R.id.account_password);

		InputFilter[] orgFilters = mAccountJID.getFilters();
		InputFilter[] newFilters = new InputFilter[orgFilters.length + 1];
		int i;
		for (i = 0; i < orgFilters.length; i++)
			newFilters[i] = orgFilters[i];
		newFilters[i] = new LoginFilter.UsernameFilterGeneric();
		mAccountJID.setFilters(newFilters);
		mAccountJID.addTextChangedListener(mJidTextWatcher);
		mAccountPassword.addTextChangedListener(mPasswordTextWatcher);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == MANUAL_CONFIGURATION) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(this);
			String login = settings.getString(
					BeemApplication.ACCOUNT_USERNAME_KEY, "");
			String password = settings.getString(
					BeemApplication.ACCOUNT_PASSWORD_KEY, "");
			mAccountJID.setText(login);
			mAccountPassword.setText(password);
			checkUsername(login);
			checkPassword(password);
			loginButton.setEnabled(mValidJid && mValidPassword);
		}

	}

	/**
	 * Store the account in the settings.
	 */
	private void configureAccount() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor edit = settings.edit();
		edit.putString(BeemApplication.ACCOUNT_USERNAME_KEY, mAccountJID
				.getText().toString() + "@boco.jp");
		edit.putString(BeemApplication.ACCOUNT_PASSWORD_KEY, mAccountPassword
				.getText().toString());
		edit.commit();
	}

	/**
	 * Check that the username is really a JID.
	 * 
	 * @param username
	 *            the username to check.
	 */
	private void checkUsername(String username) {
		String name = StringUtils.parseName(username);
		String server = StringUtils.parseServer(username);
		/*
		 * if (TextUtils.isEmpty(name) || TextUtils.isEmpty(server)) { mValidJid
		 * = false; } else { mValidJid = true; }
		 */
		if (username.length() > 0) {
			mValidJid = true;
		} else {
			mValidJid = false;
		}
	}

	/**
	 * Check password.
	 * 
	 * @param password
	 *            the password to check.
	 */
	private void checkPassword(String password) {
		/*
		 * if (password.length() > 0) mValidPassword = true; else mValidPassword
		 * = false;
		 */
		mValidPassword = true;
	}

	/**
	 * Text watcher to test the existence of a password.
	 */
	private class PasswordTextWatcher implements TextWatcher {

		/**
		 * Constructor.
		 */
		public PasswordTextWatcher() {
		}

		@Override
		public void afterTextChanged(Editable s) {
			checkPassword(s.toString());
			loginButton.setEnabled(mValidJid && true);// mValidPassword);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	}

	/**
	 * TextWatcher to check the validity of a JID.
	 */
	private class JidTextWatcher implements TextWatcher {

		/**
		 * Constructor.
		 */
		public JidTextWatcher() {
		}

		@Override
		public void afterTextChanged(Editable s) {
			checkUsername(s.toString());
			loginButton.setEnabled(mValidJid && true);// mValidPassword);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent arg1) {
		// TODO Auto-generated method stub
		if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
			//v.setBackgroundResource(R.drawable.button1_pressed);
		} else if (arg1.getAction() == MotionEvent.ACTION_UP) {
			//v.setBackgroundResource(R.drawable.button1);
			Intent i = null;
			if (v == loginButton) {
				configureAccount();
				i = new Intent(this, Login.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				finish();
			} else if (v == backButton) {
				i = new Intent(this, Account.class);
				startActivity(i);
				finish();
			}
		}
		return false;
	}
}
