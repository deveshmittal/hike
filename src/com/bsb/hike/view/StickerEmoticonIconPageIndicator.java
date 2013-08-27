package com.bsb.hike.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.bsb.hike.R;
import com.viewpagerindicator.IconPageIndicator;
import com.viewpagerindicator.IconPagerAdapter;

public class StickerEmoticonIconPageIndicator extends IconPageIndicator {

	public StickerEmoticonIconPageIndicator(Context context) {
		super(context, null);
	}

	public StickerEmoticonIconPageIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void notifyDataSetChanged() {
		mIconsLayout.removeAllViews();
		StickerEmoticonIconPagerAdapter iconAdapter = (StickerEmoticonIconPagerAdapter) mViewPager
				.getAdapter();

		LayoutInflater inflater = LayoutInflater.from(getContext());

		int count = iconAdapter.getCount();
		for (int i = 0; i < count; i++) {
			View stickerParent = inflater.inflate(R.layout.sticker_btn, null);

			ImageView icon = (ImageView) stickerParent
					.findViewById(R.id.category_btn);
			ImageView updateAvailable = (ImageView) stickerParent
					.findViewById(R.id.update_available);

			icon.setImageResource(iconAdapter.getIconResId(i));
			updateAvailable
					.setVisibility(iconAdapter.isUpdateAvailable(i) ? View.VISIBLE
							: View.GONE);

			stickerParent.setTag(i);
			stickerParent.setOnClickListener(mTabClickListener);

			mIconsLayout.addView(stickerParent);
		}
		if (mSelectedIndex > count) {
			mSelectedIndex = count - 1;
		}
		setCurrentItem(mSelectedIndex);
		requestLayout();
	}

	public interface StickerEmoticonIconPagerAdapter extends IconPagerAdapter {
		boolean isUpdateAvailable(int index);
	}

	private final OnClickListener mTabClickListener = new OnClickListener() {
		public void onClick(View view) {
			Integer currentIndex = (Integer) view.getTag();
			final int newSelected = currentIndex;
			setCurrentItem(newSelected);
		}
	};

}
