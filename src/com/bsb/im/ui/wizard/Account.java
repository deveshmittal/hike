package com.bsb.im.ui.wizard;

import com.bsb.im.service.ContactMap;
import com.bsb.im.ui.CreateAccount;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.RadioGroup;

import com.bsb.im.R;

/**
 * The first activity of an user friendly wizard to configure a XMPP account.
 * 
 */
public class Account extends Activity implements OnTouchListener {

	public Button createButton, loginButton;

	/**
	 * Constructor.
	 */
	public Account() {
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wizard_account);
		createButton = (Button) findViewById(R.id.button_create);
		loginButton = (Button) findViewById(R.id.button_login);
		createButton.setOnTouchListener(this);
		loginButton.setOnTouchListener(this);
		// mConfigureGroup = (RadioGroup) findViewById(R.id.configure_group);
		// mConfigureGroup.setOnCheckedChangeListener(this);
		// mNextButton = (Button) findViewById(R.id.next);
		// mNextButton.setOnClickListener(this);
	}

	@Override
	public boolean onTouch(View v, MotionEvent arg1) {
		// TODO Auto-generated method stub.
		if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
			//v.setBackgroundResource(R.drawable.button_pressed);
		} else if (arg1.getAction() == MotionEvent.ACTION_UP) {
			//v.setBackgroundResource(R.drawable.button);
			Intent i = null;
			if (v == createButton) {
				i = new Intent(this, CreateAccount.class);
				finish();
			} else if (v == loginButton) {
				i = new Intent(this, AccountConfigure.class);
				finish();
			}
			if (i != null) {
				startActivity(i);
			}
		}
		return false;
	}

}
