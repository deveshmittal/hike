package com.bsb.hike.media;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.HikeConstants.EmoticonType;
import com.bsb.hike.adapters.StickerAdapter;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.ui.ChatThread;
import com.bsb.hike.ui.StickerShopActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;

public class StickerPicker implements OnClickListener
{

	public interface StickerPickerListener
	{
		public void stickerSelected(Sticker sticker);
	}

	private StickerPickerListener listener;

	private Activity activity;

	private KeyboardPopupLayout popUpLayout;

	private StickerAdapter stickerAdapter;

	private View viewToDisplay;

	private View[] viewsToEatTouch;

	/**
	 * 
	 * @param context
	 * @param listener
	 * @param mainview
	 *            this is your activity Or fragment root view which gets resized when keyboard toggles
	 */
	public StickerPicker(Activity context, StickerPickerListener listener, View mainView,int firstTimeHeight)
	{
		this.activity = context;
		this.listener = listener;
		popUpLayout = new KeyboardPopupLayout(mainView, firstTimeHeight,context);
	}

	public void showStickerPicker()
	{
		showStickerPicker(0, 0);
	}

	public void showStickerPicker(int xoffset, int yoffset)
	{
		initView();

		popUpLayout.showKeyboardPopup(viewToDisplay);
	}

	private void initView()
	{
		if (viewToDisplay != null)
		{
			return;
		}
		viewToDisplay = (ViewGroup) LayoutInflater.from(activity.getApplicationContext()).inflate(R.layout.emoticon_layout, null);
		View shopIcon = (viewToDisplay.findViewById(R.id.erase_key));
		shopIcon.setBackgroundResource(R.color.sticker_pallete_bg_color);
		shopIcon.setOnClickListener(this);
		((ImageView) viewToDisplay.findViewById(R.id.erase_key_image)).setImageResource(R.drawable.ic_sticker_shop);
		if (HikeSharedPreferenceUtil.getInstance(activity).getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false))
		{
			// The shop icon would be blue unless the user clicks on it once
			viewToDisplay.findViewById(R.id.sticker_shop_badge).setVisibility(View.VISIBLE);
		}
		else
		{
			viewToDisplay.findViewById(R.id.sticker_shop_badge).setVisibility(View.GONE);
		}

		stickerAdapter = new StickerAdapter(activity);
		ViewPager pager = ((ViewPager) viewToDisplay.findViewById(R.id.emoticon_pager));
		pager.setAdapter(stickerAdapter);

		StickerEmoticonIconPageIndicator iconPageIndicator = (StickerEmoticonIconPageIndicator) viewToDisplay.findViewById(R.id.icon_indicator);
		iconPageIndicator.setViewPager(pager);
		iconPageIndicator.setOnPageChangeListener(onPageChangeListener);
	}

	public View getView()
	{
		if (viewToDisplay != null)
		{
			initView();
		}
		return viewToDisplay;
	}

	public boolean isShowing()
	{
		return popUpLayout.isShowing();
	}

	@Override
	public void onClick(View arg0)
	{
		if (arg0.getId() == R.id.erase_key)
		{
			// shop icon clicked
			shopIconClicked();
		}
	}

	private void shopIconClicked()
	{
		if (!HikeSharedPreferenceUtil.getInstance(activity).getData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, false)) // The shop icon would be blue unless the user clicks
		// on it once
		{
			HikeSharedPreferenceUtil.getInstance(activity).saveData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, true);
		}
		if (HikeSharedPreferenceUtil.getInstance(activity).getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false)) // The shop icon would be blue unless the user clicks
		// on it once
		{
			HikeSharedPreferenceUtil.getInstance(activity).saveData(StickerManager.SHOW_STICKER_SHOP_BADGE, false);
		}
		Intent i = new Intent(activity, StickerShopActivity.class);
		activity.startActivity(i);
	}

	public void updateDimension(int width, int height)
	{
		popUpLayout.updateDimension(width, height);
	}

	public void dismiss()
	{
		popUpLayout.dismiss();
	}

	OnPageChangeListener onPageChangeListener = new OnPageChangeListener()
	{

		@Override
		public void onPageSelected(int pageNum)
		{
			Logger.d("ViewPager", "Page number: " + pageNum);
			StickerCategory category = stickerAdapter.getStickerCategory(pageNum);
			if (category.getState() == StickerCategory.DONE)
			{
				category.setState(StickerCategory.NONE);
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
}
