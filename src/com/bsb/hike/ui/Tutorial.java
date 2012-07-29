package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

public class Tutorial extends Activity 
{
	private ViewPager tutorialPager;
	private ImageView pageIndicator1;
	private ImageView pageIndicator2;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signup_tutorial_base);

		tutorialPager = (ViewPager) findViewById(R.id.signup_tutorial_pager);
		pageIndicator1 = (ImageView) findViewById(R.id.page1_indicator);
		pageIndicator2 = (ImageView) findViewById(R.id.page2_indicator);

		tutorialPager.setAdapter(new TutorialPagerAdapter());

		tutorialPager.setOnPageChangeListener(new OnPageChangeListener() 
		{
			@Override
			public void onPageSelected(int position) 
			{
				pageIndicator1.setImageResource(position == 0 ? R.drawable.page_indicatore_unselected : R.drawable.page_indicatore_selected);
				pageIndicator2.setImageResource(position == 1 ? R.drawable.page_indicatore_unselected : R.drawable.page_indicatore_selected);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
	}

	public void onGetStartedClicked(View v)
	{
		Editor editor = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, MODE_PRIVATE).edit();
		editor.putBoolean(HikeMessengerApp.SHOWN_TUTORIAL, true);
		editor.commit();

		Intent i = new Intent(Tutorial.this, MessagesList.class);
		i.putExtra(HikeConstants.Extras.FIRST_TIME_USER, true);
		startActivity(i);
		finish();
	}

	private class TutorialPagerAdapter extends PagerAdapter
	{
		private static final int TUTORIAL_PAGE_COUNT = 2;
		private LayoutInflater inflater;

		public TutorialPagerAdapter() 
		{
			inflater = (LayoutInflater) LayoutInflater.from(Tutorial.this);
		}

		@Override
		public int getCount() 
		{
			return TUTORIAL_PAGE_COUNT;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) 
		{
			return view == object;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) 
		{
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) 
		{
			ViewGroup tutorialPage = (ViewGroup) inflater.inflate(R.layout.signup_tutorial_page, null);

			((ImageView)tutorialPage.findViewById(R.id.img)).setImageResource(position == 0 ? R.drawable.hike_to_hike_img : R.drawable.hike_to_sms);
			((ImageView)tutorialPage.findViewById(R.id.heading)).setImageResource(position == 0 ? R.drawable.hike_to_hike_txt : R.drawable.hike_to_sms_txt);
			((ImageView)tutorialPage.findViewById(R.id.info)).setImageResource(position == 0 ? R.drawable.hike_to_hike_info_txt : R.drawable.hike_to_sms_info_txt);
			tutorialPage.findViewById(R.id.swipe_left).setVisibility(position == 0 ? View.VISIBLE : View.GONE);
			tutorialPage.findViewById(R.id.btn_continue).setVisibility(position == 1 ? View.VISIBLE : View.GONE);

			((ViewPager) container).addView(tutorialPage);
			return tutorialPage;
		}

	}
}
