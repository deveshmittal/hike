package com.bsb.hike.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
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
import com.bsb.hike.ui.fragments.WelcomeTutorialFragment;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.viewpagerindicator.IconPageIndicator;
import com.viewpagerindicator.IconPagerAdapter;

public class WelcomeActivity extends HikeAppStateBaseFragmentActivity implements SignupTask.OnSignupTaskProgressUpdate
{
	private Button mAcceptButton;

	private ViewGroup loadingLayout;

	private View tcText;

	private ViewGroup tcContinueLayout;

	private View hikeLogoContainer;

	private boolean isMicromaxDevice;

	private ViewPager mPager;
	
	private Dialog errorDialog;

	@Override
	public void onCreate(Bundle savedState)
	{
		super.onCreate(savedState);
		setContentView(R.layout.welcomescreen);

		Utils.setupServerURL(getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).getBoolean(HikeMessengerApp.PRODUCTION, true),
				Utils.switchSSLOn(getApplicationContext()));

		mAcceptButton = (Button) findViewById(R.id.btn_continue);
		loadingLayout = (ViewGroup) findViewById(R.id.loading_layout);
		tcText = findViewById(R.id.terms_and_conditions);
		hikeLogoContainer = findViewById(R.id.hike_logo_container);

		tcContinueLayout = (ViewGroup) findViewById(R.id.tc_continue_layout);

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

		hikeLogoContainer.setVisibility(View.VISIBLE);
		tcContinueLayout.setVisibility(View.VISIBLE);
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

		mPager = (ViewPager) findViewById(R.id.tutorial_pager);
		mPager.setAdapter(new TutorialFragmentAdapter(getSupportFragmentManager()));

		IconPageIndicator mIndicator = (IconPageIndicator) findViewById(R.id.tutorial_indicator);
		mIndicator.setViewPager(mPager);
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

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(HikeConstants.Extras.SIGNUP_TASK_RUNNING, loadingLayout.getVisibility() == View.VISIBLE);
		outState.putBoolean(HikeConstants.Extras.SIGNUP_ERROR, errorDialog!=null);
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
	}

	@Override
	public void onFinish(boolean success)
	{
	}

	private void showError()
	{
		Log.d("WelcomeActivity", "showError");
		loadingLayout.setVisibility(View.GONE);
		mAcceptButton.setVisibility(View.VISIBLE);
		showNetworkErrorPopup();
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

	class TutorialFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter
	{
		private int mCount = 3;

		public TutorialFragmentAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int position)
		{
			return WelcomeTutorialFragment.newInstance(position, isMicromaxDevice);
		}

		@Override
		public int getCount()
		{
			return mCount;
		}

		@Override
		public int getIconResId(int index)
		{
			return R.drawable.welcome_tutorial_icon_indecator;
		}

	}
	
	private void showNetworkErrorPopup()
	{
		errorDialog = new Dialog(this, R.style.Theme_CustomDialog);
		errorDialog.setContentView(R.layout.no_internet_pop_up);
		errorDialog.setCancelable(false);
		Button btnOk = (Button) errorDialog.findViewById(R.id.btn_ok);
		btnOk.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				mAcceptButton.performClick();
				errorDialog.dismiss();
			}
		});
		errorDialog.show();
	}

}
