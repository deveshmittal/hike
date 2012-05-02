package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.UpdateAppBaseActivity;
import com.bsb.hike.utils.Utils;

public class WelcomeActivity extends UpdateAppBaseActivity implements SignupTask.OnSignupTaskProgressUpdate
{
	private Button mAcceptButton;
	private SignupTask mTask;
	private ViewGroup loadingLayout;
	private TextView tcText;
	
	private ViewGroup tcContinueLayout;
	private ViewGroup commLayout;
	private ViewGroup booBooLayout;
	private Button tryAgainBtn;
	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);
		Utils.setCorrectOrientation(this);
		mAcceptButton = (Button) findViewById(R.id.btn_continue);
		loadingLayout = (ViewGroup) findViewById(R.id.loading_layout);
		tcText = (TextView) findViewById(R.id.terms_and_conditions);

		tcContinueLayout = (ViewGroup) findViewById(R.id.tc_continue_layout);
		commLayout = (ViewGroup) findViewById(R.id.comm_layout);
		booBooLayout = (ViewGroup) findViewById(R.id.boo_boo_layout);
		tryAgainBtn = (Button) findViewById(R.id.btn_try_again);

		tcContinueLayout.setVisibility(View.VISIBLE);
		commLayout.setVisibility(View.VISIBLE);
		booBooLayout.setVisibility(View.GONE);
		// For starting the service and binding it to the app.
		HikeMessengerApp app = (HikeMessengerApp) getApplicationContext();
		app.connectToService();

		tcText.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				startActivity(new Intent(WelcomeActivity.this, TermsAndConditionsActivity.class));
			}
		});
	}

	public void onClick(View v)
	{
		if (v.getId() == mAcceptButton.getId())
		{
			loadingLayout.setVisibility(View.VISIBLE);
			mAcceptButton.setVisibility(View.GONE);
			mTask = SignupTask.startTask(this);
			Log.d("WelcomeActivity", "SIGNUP TASK: " + mTask);
		}
		else if(v.getId() == tryAgainBtn.getId())
		{
			tcContinueLayout.setVisibility(View.VISIBLE);
			commLayout.setVisibility(View.VISIBLE);
			booBooLayout.setVisibility(View.GONE);
			mTask = SignupTask.startTask(WelcomeActivity.this);
		}
	}

	@Override
	public void onFinish(boolean success) 
	{
	}

	@Override
	public void onProgressUpdate(StateValue value) 
	{
		if(value.state == SignupTask.State.ERROR)
		{
			tcContinueLayout.setVisibility(View.GONE);
			commLayout.setVisibility(View.GONE);
			booBooLayout.setVisibility(View.VISIBLE);
		}
		else if(value.state == SignupTask.State.MSISDN)
		{
			Intent intent = new Intent(this, SignupActivity.class);
			if(TextUtils.isEmpty(value.value))
			{
				intent.putExtra(HikeConstants.Extras.MSISDN, false);
			}
			else
			{
				intent.putExtra(HikeConstants.Extras.MSISDN, true);
			}
			startActivity(intent);
			finish();
		}
	}
}