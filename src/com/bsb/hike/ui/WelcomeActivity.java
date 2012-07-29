package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.AccountUtils;

public class WelcomeActivity extends Activity implements SignupTask.OnSignupTaskProgressUpdate
{
	private ImageButton mAcceptButton;
	private ViewGroup loadingLayout;
	private Button tcText;
	
	private ViewGroup tcContinueLayout;
	private ViewGroup booBooLayout;
	private ImageButton tryAgainBtn;
	private View hiLogoView;
	private ImageView hikeWelcomeView;
	private ViewGroup headerLayout;
	private ImageView errorImage;

	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);

		mAcceptButton = (ImageButton) findViewById(R.id.btn_continue);
		loadingLayout = (ViewGroup) findViewById(R.id.loading_layout);
		tcText = (Button) findViewById(R.id.terms_and_conditions);
		hiLogoView = findViewById(R.id.ic_hi_logo);
		hikeWelcomeView = (ImageView) findViewById(R.id.ic_hike_welcome_image);

		tcContinueLayout = (ViewGroup) findViewById(R.id.tc_continue_layout);
		booBooLayout = (ViewGroup) findViewById(R.id.boo_boo_layout);
		tryAgainBtn = (ImageButton) findViewById(R.id.btn_try_again);
		errorImage = (ImageView) findViewById(R.id.error_img);

		headerLayout = (ViewGroup) booBooLayout.findViewById(R.id.header_layout);
		headerLayout.setVisibility(View.VISIBLE);

		if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SPLASH_SEEN, false))
		{
			hikeWelcomeView.setVisibility(View.VISIBLE);
			tcContinueLayout.setVisibility(View.VISIBLE);
			hiLogoView.setVisibility(View.GONE);
		}
		else
		{
			(new Handler()).postDelayed(new Runnable() {
				public void run()
				{
					startAnimations();
				}

			}, (long) 1.5 * 1000);
		}
		if ((savedState != null) && (savedState.getBoolean(HikeConstants.Extras.SIGNUP_ERROR)))
		{
			showError();
		}
		else if ((savedState != null) && (savedState.getBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING)))
		{
			onClick(mAcceptButton);
		}

		tcText.setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Intent intent = new Intent(WelcomeActivity.this, WebViewActivity.class);
				intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, HikeConstants.T_AND_C_URL);
				intent.putExtra(HikeConstants.Extras.TITLE, getString(R.string.terms_privacy));
				startActivity(intent);
			}
		});

		AnimationDrawable ad = new AnimationDrawable();
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large0), 600);
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large1), 600);
		ad.addFrame(getResources().getDrawable(R.drawable.ic_tower_large2), 600);
		ad.setOneShot(false);
		ad.setVisible(true, true);

		errorImage.setImageDrawable(ad);
		ad.start();
	}

	public void onHikeIconClicked(View v)
	{
		changeHost();
	}

	private void changeHost()
	{
		Log.d(getClass().getSimpleName(), "Hike Icon CLicked");

		SharedPreferences sharedPreferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		String currentHost = sharedPreferences.getString(HikeMessengerApp.HOST, AccountUtils.DEFAULT_HOST);

		AccountUtils.HOST =  AccountUtils.DEFAULT_HOST.equals(currentHost) ? AccountUtils.STAGING_HOST : AccountUtils.DEFAULT_HOST;
		AccountUtils.BASE = "http://" + AccountUtils.HOST + ":" + Integer.toString(AccountUtils.PORT) + "/v1";

		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
		editor.putString(HikeMessengerApp.HOST, AccountUtils.HOST);
		editor.commit();
		Log.d(getClass().getSimpleName(), "Host Changed to " + AccountUtils.HOST);
		Log.d(getClass().getSimpleName(), "Base is " + AccountUtils.BASE);
	}

	private void startAnimations()
	{


		Animation slideUpAlphaout = AnimationUtils.loadAnimation(this, R.anim.welcome_alpha_out);
		Animation slideUpAlphaIn = AnimationUtils.loadAnimation(this, R.anim.welcome_alpha_in);
		slideUpAlphaout.setAnimationListener(new Animation.AnimationListener()
		{
			
			@Override
			public void onAnimationStart(Animation animation)
			{
			}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				hiLogoView.setVisibility(View.INVISIBLE);
				Animation fadeInAnimation = AnimationUtils.loadAnimation(WelcomeActivity.this, R.anim.fade_in_animation);
				fadeInAnimation.setAnimationListener(new Animation.AnimationListener()
				{
					@Override
					public void onAnimationStart(Animation animation)
					{
						tcContinueLayout.setVisibility(View.VISIBLE);
					}
					
					@Override
					public void onAnimationRepeat(Animation animation)
					{
					}
					
					@Override
					public void onAnimationEnd(Animation animation)
					{
						hikeWelcomeView.setVisibility(View.VISIBLE);
					}
				});
				tcContinueLayout.startAnimation(fadeInAnimation);
				SharedPreferences.Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
				editor.putBoolean(HikeMessengerApp.SPLASH_SEEN, true);
				editor.commit();
			}
		});
		hikeWelcomeView.startAnimation(slideUpAlphaIn);
		hiLogoView.startAnimation(slideUpAlphaout);
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
			// Disable the t and c button
			tcText.setEnabled(false);
			loadingLayout.setVisibility(View.VISIBLE);
			mAcceptButton.setVisibility(View.GONE);
			SignupTask.startTask(this);
		}
		else if(v.getId() == tryAgainBtn.getId())
		{
			tcContinueLayout.setVisibility(View.VISIBLE);
			hikeWelcomeView.setImageResource(R.drawable.hike_welcome_image);
			booBooLayout.setVisibility(View.GONE);
			onClick(mAcceptButton);
		}
	}

	@Override
	public void onFinish(boolean success) 
	{
	}

	private void showError() {
		Log.d("WelcomeActivity", "showError");
		tcContinueLayout.setVisibility(View.GONE);
		hikeWelcomeView.setImageDrawable(null);
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
