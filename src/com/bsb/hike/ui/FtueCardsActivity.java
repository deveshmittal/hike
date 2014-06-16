package com.bsb.hike.ui;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.smartImageLoader.FTUECardImageLoader;
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
		mPager.setOffscreenPageLimit(5);
		mPager.setAdapter(new TutorialPagerAdapter());

		/*
		 * Calling the GC to clear the the junk created when items from the cache are evicted to make room for the new images here.
		 */
		System.gc();

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
					clearFTUECardsFromCache();
					finish();
				}
			}
		});
	}
	
	public void onBackPressed()
	{
		clearFTUECardsFromCache();
		super.onBackPressed();
	};

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
		FTUECardImageLoader imageLoader;

		public TutorialPagerAdapter()
		{
			imageLoader = new FTUECardImageLoader(FtueCardsActivity.this);
		}

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
			View parent = LayoutInflater.from(FtueCardsActivity.this).inflate(R.layout.ftue_cards_content, null);

			switch (FtueCards.values()[position])
			{
			case HIDDEN_MODE:
				setupCard(parent, R.color.hidden_mode_ftue, R.drawable.ftue_hidden_mode_card_img, 
						R.drawable.ftue_hidden_mode_card_small_img, R.string.hidden_mode, R.string.ftue_hidden_mode_card_msg);
				break;
			case FAVORITES:
				setupCard(parent, R.color.favorite_ftue, R.drawable.ftue_favorites_card_img, 
						R.drawable.ftue_favorites_card_small_img, R.string.favorites, R.string.ftue_favorites_card_msg);
				break;
			case ATTACHMENT:
				setupCard(parent, R.color.attachments_ftue, R.drawable.ftue_attachment_card_img, 
						R.drawable.ftue_attachment_card_small_img, R.string.attachments, R.string.ftue_attachment_card_msg);
				break;
			case HIKE_OFFLINE:
				setupCard(parent, R.color.hike_offline_ftue, R.drawable.ftue_hike_offline_card_img, 
						R.drawable.ftue_hike_offline_card_small_img, R.string.hike_offline, R.string.ftue_hike_offline_card_msg);
				break;
			case STICKER:
				setupCard(parent, R.color.sticker_ftue, R.drawable.ftue_sticker_card_img, 
						R.drawable.ftue_sticker_card_small_img, R.string.stickers, R.string.ftue_sticker_card_msg);
				break;
			case THEMES:
				setupCard(parent, R.color.themes_ftue, R.drawable.ftue_themes_card_img, 
						R.drawable.ftue_themes_card_small_img, R.string.themes, R.string.ftue_themes_card_msg);
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

		private void setupCard(View parent, int cardBgResId, int cardImgResId, int cardSmallImgResId, int cardTextHeaderResId, int cardTextMsgResId)
		{
			ImageView cardBg = (ImageView) parent.findViewById(R.id.card_header_img_bg);
			ImageView cardImage = (ImageView) parent.findViewById(R.id.card_header_img_content);
			ImageView cardSmallImage = (ImageView) parent.findViewById(R.id.card_small_img_content);
			TextView cardTextHeader = (TextView) parent.findViewById(R.id.card_txt_header);
			TextView cardTextMsg = (TextView) parent.findViewById(R.id.card_txt_msg);
			
			cardBg.setBackgroundColor(getResources().getColor(cardBgResId));
			imageLoader.loadImage(FTUECardImageLoader.FTUE_CARD_IMAGE_PREFIX + cardImgResId, cardImage);
			cardSmallImage.setImageResource( cardSmallImgResId );
			cardTextHeader.setText( cardTextHeaderResId );
			cardTextMsg.setText( cardTextMsgResId );		
		}
	}

	/**
	 * Making sure we clear all the FTUE card images from the cache. Since we won't need this anymore.
	 */
	private void clearFTUECardsFromCache()
	{
		HikeMessengerApp.getLruCache().removeItemForKey(FTUECardImageLoader.FTUE_CARD_IMAGE_PREFIX + R.drawable.ftue_hidden_mode_card_img);
		HikeMessengerApp.getLruCache().removeItemForKey(FTUECardImageLoader.FTUE_CARD_IMAGE_PREFIX + R.drawable.ftue_favorites_card_img);
		HikeMessengerApp.getLruCache().removeItemForKey(FTUECardImageLoader.FTUE_CARD_IMAGE_PREFIX + R.drawable.ftue_attachment_card_img);
		HikeMessengerApp.getLruCache().removeItemForKey(FTUECardImageLoader.FTUE_CARD_IMAGE_PREFIX + R.drawable.ftue_hike_offline_card_img);
		HikeMessengerApp.getLruCache().removeItemForKey(FTUECardImageLoader.FTUE_CARD_IMAGE_PREFIX + R.drawable.ftue_sticker_card_img);
		HikeMessengerApp.getLruCache().removeItemForKey(FTUECardImageLoader.FTUE_CARD_IMAGE_PREFIX + R.drawable.ftue_themes_card_img);

		/*
		 * Again trying to call the gc since we have evicted a few items from the cache.
		 */
		System.gc();
	}
}
