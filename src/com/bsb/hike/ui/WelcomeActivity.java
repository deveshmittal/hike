package com.bsb.hike.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.WelcomeTutorial;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.tasks.SignupTask;
import com.bsb.hike.tasks.SignupTask.StateValue;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
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
		mPager.setAdapter(new TutorialPagerAdapter());

		IconPageIndicator mIndicator = (IconPageIndicator) findViewById(R.id.tutorial_indicator);
		mIndicator.setViewPager(mPager);
		mIndicator.setOnPageChangeListener(onPageChangeListener);
	}

	public void onHikeIconClicked(View v)
	{
		changeHost();
	}

	private void changeHost()
	{
		Logger.d(getClass().getSimpleName(), "Hike Icon CLicked");

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
		Logger.d("WelcomeActivity", "showError");
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

	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{
		
		@Override
		public void onPageSelected(int position)
		{
			SharedPreferences sharedPreferences = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE);
			int viewed = sharedPreferences.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1); 
			Editor editor = sharedPreferences.edit();
			switch (position)
			{
			case 0:
				if(viewed < HikeConstants.WelcomeTutorial.INTRO_VIEWED.ordinal())
				{
					editor.putInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, WelcomeTutorial.INTRO_VIEWED.ordinal());
				}
				break;
			case 1:
				if(viewed < HikeConstants.WelcomeTutorial.STICKER_VIEWED.ordinal())
				{
					editor.putInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, WelcomeTutorial.STICKER_VIEWED.ordinal());
				}
				break;
			case 2:
				if(viewed < HikeConstants.WelcomeTutorial.CHAT_BG_VIEWED.ordinal())
				{
					editor.putInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, WelcomeTutorial.CHAT_BG_VIEWED.ordinal());
				}
				break;
			}
			editor.commit();
		}
		
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
			// TODO Auto-generated method stub
		}
		
		@Override
		public void onPageScrollStateChanged(int position)
		{
			// TODO Auto-generated method stub
		}
	};
	private class TutorialPagerAdapter extends PagerAdapter implements IconPagerAdapter
	{
		private int mCount = 3;

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

		@Override
		public boolean isViewFromObject(View view, Object object)
		{
			// TODO Auto-generated method stub
			return view == object;
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			View parent = LayoutInflater.from(WelcomeActivity.this).inflate(R.layout.tutorial_fragments, null);
			TextView tutorialHeader = (TextView) parent.findViewById(R.id.tutorial_title);
			ImageView tutorialImage = (ImageView) parent.findViewById(R.id.tutorial_img);
			ImageView micromaxImage = (ImageView) parent.findViewById(R.id.ic_micromax);
			switch (position)
			{
			case 0:
				tutorialHeader.setText(R.string.tutorial1_header_title);
				tutorialImage.setImageResource(R.drawable.tutorial1_img);
				micromaxImage.setVisibility(isMicromaxDevice ? View.VISIBLE : View.GONE);
				break;
			case 1:
				tutorialHeader.setText(R.string.tutorial2_header_title);
				tutorialImage.setImageResource(R.drawable.tutorial2_img);
				micromaxImage.setVisibility(View.GONE);
				break;
			case 2:
				tutorialHeader.setText(R.string.tutorial3_header_title);
				tutorialImage.setImageResource(R.drawable.tutorial3_img);
				micromaxImage.setVisibility(View.GONE);
				break;
			}
			((ViewPager) container).addView(parent);
			return parent;
		}
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			Logger.d(getClass().getSimpleName(), "Item removed from position : " + position);
			((ViewPager) container).removeView((View) object);
		}


	}
	
	private void showNetworkErrorPopup()
	{
		errorDialog = new Dialog(this, R.style.Theme_CustomDialog);
		errorDialog.setContentView(R.layout.no_internet_pop_up);
		errorDialog.setCancelable(true);
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
		if(!this.isFinishing())
		{
			errorDialog.show();
		}
	}

}
