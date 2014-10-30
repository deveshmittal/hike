package com.bsb.hike.view;

import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.bsb.hike.adapters.EmoticonAdapter;
import com.bsb.hike.adapters.StickerAdapter;
import com.viewpagerindicator.IconPageIndicator;
import com.viewpagerindicator.IconPagerAdapter;

public class StickerEmoticonIconPageIndicator extends IconPageIndicator
{

	private int screenWidth;

	private int minWidth;

	public StickerEmoticonIconPageIndicator(Context context)
	{
		this(context, null);
	}

	public StickerEmoticonIconPageIndicator(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		screenWidth = context.getResources().getDisplayMetrics().widthPixels;

		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		minWidth = (int) (48 * metrics.density);
	}

	/*
	 * TODO : This function is called twice, it should be handled properly so that it should run just once. Also inorder to remove the red icon once stickers gets downloaded, we
	 * should handle it properly instead of calling "notifyDataSetChanged" this again and again.
	 */

	@Override
	public void notifyDataSetChanged()
	{
		mIconsLayout.removeAllViews();
		StickerEmoticonIconPagerAdapter iconAdapter = (StickerEmoticonIconPagerAdapter) mViewPager.getAdapter();

		LayoutInflater inflater = LayoutInflater.from(getContext());

		int count = iconAdapter.getCount();

		int itemWidth = (int) (screenWidth / count);

		if (itemWidth < minWidth)
		{
			itemWidth = minWidth;
		}

		for (int i = 0; i < count; i++)
		{
			View stickerParent = inflater.inflate(R.layout.sticker_btn, null);

			ImageView icon = (ImageView) stickerParent.findViewById(R.id.category_btn);
			ImageView updateAvailable = (ImageView) stickerParent.findViewById(R.id.update_available);
			if (iconAdapter instanceof EmoticonAdapter)
			{
				icon.setImageResource(iconAdapter.getIconResId(i));
			}
			else if(iconAdapter instanceof StickerAdapter)
			{
				icon.setImageDrawable(iconAdapter.getPalleteIconDrawable(i));
			}
			updateAvailable.setVisibility(iconAdapter.isUpdateAvailable(i) ? View.VISIBLE : View.GONE);

			stickerParent.setTag(i);
			stickerParent.setOnClickListener(mTabClickListener);

			LayoutParams layoutParams = new LayoutParams(itemWidth, LayoutParams.MATCH_PARENT);
			stickerParent.setLayoutParams(layoutParams);

			mIconsLayout.addView(stickerParent);
		}
		if (mSelectedIndex > count)
		{
			mSelectedIndex = count - 1;
		}
		setCurrentItem(mSelectedIndex);
		requestLayout();
	}

	public interface StickerEmoticonIconPagerAdapter extends IconPagerAdapter
	{
		boolean isUpdateAvailable(int index);
		StateListDrawable getPalleteIconDrawable(int index);
		
	}

	private final OnClickListener mTabClickListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			Integer currentIndex = (Integer) view.getTag();
			final int newSelected = currentIndex;
			setCurrentItem(newSelected);
		}
	};

}
