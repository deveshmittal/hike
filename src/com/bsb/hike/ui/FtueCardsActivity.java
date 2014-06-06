package com.bsb.hike.ui;

import android.graphics.Bitmap;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Logger;
import com.viewpagerindicator.IconPageIndicator;
import com.viewpagerindicator.IconPagerAdapter;

public class FtueCardsActivity extends HikeAppStateBaseFragmentActivity
{
	private ViewPager mPager;

	private enum FtueCards
	{
		HIDDEN_MODE
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

		mPager = (ViewPager) findViewById(R.id.tutorial_pager);
		mPager.setAdapter(new TutorialPagerAdapter());

		IconPageIndicator mIndicator = (IconPageIndicator) findViewById(R.id.tutorial_indicator);
		mIndicator.setViewPager(mPager);
		AlphaAnimation anim = new AlphaAnimation(0.5f, 0.5f);
		anim.setDuration(0);
		anim.setFillAfter(true);
		findViewById(R.id.skip).startAnimation(anim);
	}

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
			View parent = LayoutInflater.from(FtueCardsActivity.this).inflate(R.layout.ftue_cards_content, null);
			ImageView cardBg = (ImageView) parent.findViewById(R.id.card_header_img_bg);
			ImageView cardImage = (ImageView) parent.findViewById(R.id.card_header_img_content);
			ImageView cardSmallImage = (ImageView) parent.findViewById(R.id.card_small_img_content);
			TextView cardTextHeader = (TextView) parent.findViewById(R.id.card_txt_header);
			TextView cardTextMsg = (TextView) parent.findViewById(R.id.card_txt_msg);

			switch (FtueCards.values()[position])
			{
			case HIDDEN_MODE:
				setTiledBackground(cardBg, R.drawable.ftue_hidden_mode_card_bg_tile);
				cardImage.setImageResource(R.drawable.ftue_hidden_mode_card_img);
				cardSmallImage.setImageResource(R.drawable.ftue_hidden_mode_card_small_img);
				cardTextHeader.setText(R.string.hidden_mode);
				cardTextMsg.setText(R.string.ftue_hidden_mode_card_msg);
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

	private void setTiledBackground(ImageView imageView, int resId)
	{
		Bitmap b = HikeBitmapFactory.decodeSampledBitmapFromResource(getResources(), resId, 1);
		BitmapDrawable bd = HikeBitmapFactory.getBitmapDrawable(getResources(), b);
		bd.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		imageView.setImageDrawable(bd);

	}

}
