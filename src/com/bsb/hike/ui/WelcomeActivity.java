package com.bsb.hike.ui;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.utils.UpdateAppBaseActivity;

public class WelcomeActivity extends UpdateAppBaseActivity
{
	private Button mAcceptButton;

	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);
		mAcceptButton = (Button) findViewById(R.id.btn_continue);
		
		// For starting the service and binding it to the app.
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();
	}

	public void onClick(View v)
	{
		if (v == mAcceptButton)
		{
			Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
			editor.putBoolean(HikeMessengerApp.ACCEPT_TERMS, true);
			editor.commit();
			startActivity(new Intent(this, SignupActivity.class));
			finish();
		}
	}
}
