package com.bsb.hike.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
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
import android.widget.ImageView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeAppStateBaseActivity;
import com.bsb.hike.utils.Utils;

public class WelcomeActivity extends HikeAppStateBaseActivity implements SignupTask.OnSignupTaskProgressUpdate
{
	private Button mAcceptButton;

	private ViewGroup loadingLayout;

	private Button tcText;

	private ViewGroup tcContinueLayout;

	private Button tryAgainBtn;

	private View hiLogoView;

	private View hikeLogoContainer;

	private ImageView micromaxImage;

	private boolean isMicromaxDevice;

	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);

		Utils.setupServerURL(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.PRODUCTION, true),
				Utils.switchSSLOn(getApplicationContext()));

		mAcceptButton = (Button) findViewById(R.id.btn_continue);
		loadingLayout = (ViewGroup) findViewById(R.id.loading_layout);
		tcText = (Button) findViewById(R.id.terms_and_conditions);
		hiLogoView = findViewById(R.id.ic_hi_logo);
		hikeLogoContainer = findViewById(R.id.hike_logo_container);
		micromaxImage = (ImageView) findViewById(R.id.ic_micromax);

		tcContinueLayout = (ViewGroup) findViewById(R.id.tc_continue_layout);
		tryAgainBtn = (Button) findViewById(R.id.btn_try_again);

		String model = Build.MODEL;
		String manufacturer = Build.MANUFACTURER;

		if (model != null)
		{
			model = model.toUpperCase();

			if (HikeConstants.MICROMAX.equalsIgnoreCase(manufacturer))
			{
				isMicromaxDevice = true;
			}
			else
			{
				if (model.contains(HikeConstants.MICROMAX))
				{
					isMicromaxDevice = true;
				}
			}
		}

		if (getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).getBoolean(HikeMessengerApp.SPLASH_SEEN, false))
		{
			hikeLogoContainer.setVisibility(View.VISIBLE);
			tcContinueLayout.setVisibility(View.VISIBLE);
			micromaxImage.setVisibility(isMicromaxDevice ? View.VISIBLE : View.GONE);
			hiLogoView.setVisibility(View.GONE);
		}
		else
		{
			(new Handler()).postDelayed(new Runnable()
			{
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
	}

	public void onHikeIconClicked(View v)
	{
		changeHost();
	}

	private void changeHost()
	{
		Log.d(getClass().getSimpleName(), "Hike Icon CLicked");

		SharedPreferences sharedPreferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
		boolean production = sharedPreferences.getBoolean(HikeMessengerApp.PRODUCTION, true);

		Utils.setupServerURL(!production, Utils.switchSSLOn(this));

		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
		editor.putBoolean(HikeMessengerApp.PRODUCTION, !production);
		editor.commit();

		Toast.makeText(WelcomeActivity.this, AccountUtils.base, Toast.LENGTH_SHORT).show();
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
						hikeLogoContainer.setVisibility(View.VISIBLE);
						micromaxImage.setVisibility(isMicromaxDevice ? View.VISIBLE : View.GONE);
					}
				});
				tcContinueLayout.startAnimation(fadeInAnimation);
				SharedPreferences.Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0).edit();
				editor.putBoolean(HikeMessengerApp.SPLASH_SEEN, true);
				editor.commit();
			}
		});
		hikeLogoContainer.startAnimation(slideUpAlphaIn);
		hiLogoView.startAnimation(slideUpAlphaout);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, tryAgainBtn.getVisibility() == View.VISIBLE);
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
		else if (v.getId() == tryAgainBtn.getId())
		{
			micromaxImage.setVisibility(isMicromaxDevice ? View.VISIBLE : View.GONE);
			tryAgainBtn.setVisibility(View.GONE);
			onClick(mAcceptButton);
		}
	}

	@Override
	public void onFinish(boolean success)
	{
	}

	private void showError()
	{
		Log.d("WelcomeActivity", "showError");
		tryAgainBtn.setVisibility(View.VISIBLE);
	}

	@Override
	public void onProgressUpdate(StateValue value)
	{
		if (value.state == SignupTask.State.ERROR)
		{
			showError();
		}
		else if (value.state == SignupTask.State.MSISDN)
		{
			Intent intent = new Intent(this, SignupActivity.class);
			if (TextUtils.isEmpty(value.value))
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

	public void onBackPressed()
	{
		SignupTask mTask = SignupTask.getSignupTask(this);
		if (mTask != null)
		{
			mTask.cancelTask();
			mTask = null;
		}
		super.onBackPressed();
	}
}
