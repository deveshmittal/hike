package com.bsb.hike.ui;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.viewpagerindicator.IconPageIndicator;
import com.viewpagerindicator.IconPagerAdapter;

public class FtueCardsActivity extends HikeAppStateBaseFragmentActivity
{
	private ViewPager mPager;
	TextView skipButton;

	private enum FtueCards
	{
		STICKER, FAVORITES, HIDDEN_MODE, HIKE_OFFLINE, ATTACHMENT, THEMES 
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		initialiseScreen(savedInstanceState);
	}

	private void initialiseScreen(Bundle savedInstanceState)
	{
		setContentView(R.layout.ftue_cards);
		skipButton = (TextView) findViewById(R.id.skip);

		mPager = (ViewPager) findViewById(R.id.tutorial_pager);
		mPager.setAdapter(new TutorialPagerAdapter());

		IconPageIndicator mIndicator = (IconPageIndicator) findViewById(R.id.tutorial_indicator);
		mIndicator.setOnPageChangeListener(onPageChangeListener);
		mIndicator.setViewPager(mPager);
		skipButton.setText(R.string.next_signup);
		skipButton.setTextColor(getResources().getColor(R.color.white_50));
		
		findViewById(R.id.skip).setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				if(mPager.getCurrentItem() < FtueCards.values().length -1)
				{
					mPager.setCurrentItem(mPager.getCurrentItem()+1, true);
				}
				else
				{
					finish();
				}
			}
		});
	}
	
	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{
		
		@Override
		public void onPageSelected(int arg0)
		{
			if(arg0 < FtueCards.values().length - 1)
			{
				skipButton.setText(R.string.next_signup);
			}
			else
			{
				skipButton.setText(R.string.done);
			}
		}
		
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
			
		}
		
		@Override
		public void onPageScrollStateChanged(int arg0)
		{
			
		}
	};

	private class TutorialPagerAdapter extends PagerAdapter implements IconPagerAdapter
	{
		@Override
		public int getCount()
		{
			return FtueCards.values().length;
		}

		@Override
		public int getIconResId(int index)
		{
			return R.drawable.ftue_icon_page_indicator;
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
			View parent = null;

			switch (FtueCards.values()[position])
			{
			case HIDDEN_MODE:
				parent = LayoutInflater.from(FtueCardsActivity.this).inflate(R.layout.ftue_hidden_mode_card_content, null);
				break;
			case FAVORITES:
				parent = LayoutInflater.from(FtueCardsActivity.this).inflate(R.layout.ftue_favorites_card_content, null);
				break;
			case ATTACHMENT:
				parent = LayoutInflater.from(FtueCardsActivity.this).inflate(R.layout.ftue_attachment_card_content, null);
				break;
			case HIKE_OFFLINE:
				parent = LayoutInflater.from(FtueCardsActivity.this).inflate(R.layout.ftue_hike_offline_card_content, null);
				break;
			case STICKER:
				parent = LayoutInflater.from(FtueCardsActivity.this).inflate(R.layout.ftue_sticker_card_content, null);
				break;
			case THEMES:
				parent = LayoutInflater.from(FtueCardsActivity.this).inflate(R.layout.ftue_themes_card_content, null);
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
}
