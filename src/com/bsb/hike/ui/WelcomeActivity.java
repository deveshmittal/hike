package com.bsb.hike.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.UpdateAppBaseActivity;

public class WelcomeActivity extends UpdateAppBaseActivity implements SignupTask.OnSignupTaskProgressUpdate
{
	private Button mAcceptButton;
	private ViewGroup loadingLayout;
	private Button tcText;
	
	private ViewGroup tcContinueLayout;
	private ViewGroup commLayout;
	private ViewGroup booBooLayout;
	private Button tryAgainBtn;
	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);

		mAcceptButton = (Button) findViewById(R.id.btn_continue);
		loadingLayout = (ViewGroup) findViewById(R.id.loading_layout);
		tcText = (Button) findViewById(R.id.terms_and_conditions);

		tcContinueLayout = (ViewGroup) findViewById(R.id.tc_continue_layout);
		commLayout = (ViewGroup) findViewById(R.id.comm_layout);
		booBooLayout = (ViewGroup) findViewById(R.id.boo_boo_layout);
		tryAgainBtn = (Button) findViewById(R.id.btn_try_again);

		tcContinueLayout.setVisibility(View.VISIBLE);
		commLayout.setVisibility(View.VISIBLE);
		booBooLayout.setVisibility(View.GONE);

		if(savedState!= null)
		{
			if (savedState.getBoolean(HikeConstants.Extras.SIGNUP_ERROR)) {
				showError();
			}
			else if (savedState.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING)) {
				onClick(mAcceptButton);
			}
		}
		
		tcText.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				startActivity(new Intent(WelcomeActivity.this, TermsAndConditionsActivity.class));
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, booBooLayout.getVisibility() == View.VISIBLE);
		super.onSaveInstanceState(outState);
	}

	public void onClick(View v)
	{
		if (v.getId() == mAcceptButton.getId())
		{
			loadingLayout.setVisibility(View.VISIBLE);
			mAcceptButton.setVisibility(View.GONE);
			SignupTask.startTask(this);
		}
		else if(v.getId() == tryAgainBtn.getId())
		{
			tcContinueLayout.setVisibility(View.VISIBLE);
			commLayout.setVisibility(View.VISIBLE);
			booBooLayout.setVisibility(View.GONE);
			onClick(mAcceptButton);
		}
	}

	@Override
	public void onFinish(boolean success) 
	{
	}

	private void showError() {
		tcContinueLayout.setVisibility(View.GONE);
		commLayout.setVisibility(View.GONE);
		booBooLayout.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onProgressUpdate(StateValue value) 
	{
		if(value.state == SignupTask.State.ERROR)
		{
			showError();
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
