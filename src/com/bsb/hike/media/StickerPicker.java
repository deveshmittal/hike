package com.bsb.hike.media;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.adapters.StickerAdapter;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.ui.StickerShopActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.view.StickerEmoticonIconPageIndicator;

public class StickerPicker implements OnClickListener, ShareablePopup, StickerPickerListener
{
	private StickerPickerListener listener;

	private Context mContext;

	private KeyboardPopupLayout popUpLayout;

	private StickerAdapter stickerAdapter;

	private View viewToDisplay;

	private int mLayoutResId = -1;
	
	private StickerEmoticonIconPageIndicator mIconPageIndicator;
	
	private SherlockFragmentActivity mActivity;

	private static final String TAG = "StickerPicker";

	/**
	 * Constructor
	 * 
	 * @param activity
	 * @param listener
	 */
	public StickerPicker(Context context, StickerPickerListener listener, SherlockFragmentActivity activity)
	{
		this.mContext = context;
		this.listener = listener;
		this.mActivity = activity;
	}

	/**
	 * Another constructor. The popup layout is passed to this, rather than the picker instantiating one of its own.
	 * 
	 * @param context
	 * @param listener
	 * @param popUpLayout
	 */
	public StickerPicker(int layoutResId, Context context, StickerPickerListener listener, KeyboardPopupLayout popUpLayout, SherlockFragmentActivity activity)
	{
		this(context, listener, activity);
		this.mLayoutResId = layoutResId;
		this.popUpLayout = popUpLayout;
	}

	/**
	 * The view to display is also passed to this constructor
	 * 
	 * @param view
	 * @param context
	 * @param listener
	 * @param popUpLayout
	 */
	public StickerPicker(View view, Context context, StickerPickerListener listener, KeyboardPopupLayout popUpLayout, SherlockFragmentActivity activity)
	{
		this(context, listener, activity);
		this.viewToDisplay = view;
		this.popUpLayout = popUpLayout;
		initViewComponents(viewToDisplay);
		Logger.d(TAG, "Sticker Picker instantiated with views");
	}

	/**
	 * Basic constructor. Constructs the popuplayout on its own.
	 * 
	 * @param context
	 * @param listener
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatTouchEventViewIds
	 */

	public StickerPicker(Context context, StickerPickerListener listener, View mainView, int firstTimeHeight, int[] eatTouchEventViewIds, SherlockFragmentActivity activity)
	{
		this(context, listener, activity);
		popUpLayout = new KeyboardPopupLayout(mainView, firstTimeHeight, context.getApplicationContext(), eatTouchEventViewIds);
	}

	/**
	 * 
	 * @param context
	 * @param listener
	 * @param mainview
	 *            this is your activity Or fragment root view which gets resized when keyboard toggles
	 * @param firstTimeHeight
	 */
	public StickerPicker(Context context, StickerPickerListener listener, View mainView, int firstTimeHeight, SherlockFragmentActivity activity)
	{
		this(context, listener, mainView, firstTimeHeight, null, activity);
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

	/**
	 * Used for instantiating the views
	 */
	private void initView()
	{
		if (viewToDisplay != null)
		{
			return;
		}

		/**
		 * Use default view. or the view passed in the constructor
		 */

		mLayoutResId = (mLayoutResId == -1) ? R.layout.sticker_layout : mLayoutResId;

		viewToDisplay = (ViewGroup) LayoutInflater.from(mContext).inflate(mLayoutResId, null);

		initViewComponents(viewToDisplay);
	}

	/**
	 * Initialises the view components from a given view
	 * 
	 * @param view
	 */
	private void initViewComponents(View view)
	{
		ViewPager mViewPager = ((ViewPager) view.findViewById(R.id.sticker_pager));

		if (null == mViewPager)
		{
			throw new IllegalArgumentException("View Pager was not found in the view passed.");
		}

		stickerAdapter = new StickerAdapter(mContext, this);

		mIconPageIndicator = (StickerEmoticonIconPageIndicator) view.findViewById(R.id.sticker_icon_indicator);

		View shopIcon = (view.findViewById(R.id.shop_icon));

		shopIcon.setOnClickListener(this);

		if (HikeSharedPreferenceUtil.getInstance(mContext).getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false))
		{
			// The shop icon would be blue unless the user clicks on it once
			view.findViewById(R.id.shop_icon_badge).setVisibility(View.VISIBLE);
		}
		else
		{
			view.findViewById(R.id.shop_icon_badge).setVisibility(View.GONE);
		}

		mViewPager.setVisibility(View.VISIBLE);

		mViewPager.setAdapter(stickerAdapter);

		mIconPageIndicator.setViewPager(mViewPager);

		mIconPageIndicator.setOnPageChangeListener(onPageChangeListener);
	}

	/**
	 * Interface mehtod. Check {@link ShareablePopup}
	 */

	@Override
	public View getView()
	{
		if (viewToDisplay == null)
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
		if (arg0.getId() == R.id.shop_icon)
		{
			// shop icon clicked
			shopIconClicked();
		}
	}

	private void shopIconClicked()
	{
		if (!HikeSharedPreferenceUtil.getInstance(mContext.getApplicationContext()).getData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, false)) // The shop icon would be blue unless the user clicks
		// on it once
		{
			HikeSharedPreferenceUtil.getInstance(mContext.getApplicationContext()).saveData(HikeMessengerApp.SHOWN_SHOP_ICON_BLUE, true);
		}
		if (HikeSharedPreferenceUtil.getInstance(mContext.getApplicationContext()).getData(StickerManager.SHOW_STICKER_SHOP_BADGE, false)) // The shop icon would be blue unless the user clicks
		// on it once
		{
			HikeSharedPreferenceUtil.getInstance(mContext.getApplicationContext()).saveData(StickerManager.SHOW_STICKER_SHOP_BADGE, false);
		}
		Intent i = new Intent(mContext, StickerShopActivity.class);
		mActivity.startActivity(i);
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

	/**
	 * Interface method. Check {@link ShareablePopup}
	 */

	@Override
	public int getViewId()
	{
		return viewToDisplay.getId();
	}

	/**
	 * Interface method. Check {@link ShareablePopup}
	 */
	@Override
	public void releaseViewResources()
	{
		// TODO Implement this.
		viewToDisplay = null;
		stickerAdapter = null;
		mActivity = null;
	}

	public void updateListener(StickerPickerListener mListener, SherlockFragmentActivity activity)
	{
		this.listener = mListener;
		this.mActivity = activity;
	}
	
	public void updateStickerAdapter()
	{
		if (stickerAdapter != null)
		{
			stickerAdapter.instantiateStickerList();
			stickerAdapter.notifyDataSetChanged();
		}
	}

	public void updateIconPageIndicator()
	{
		if (mIconPageIndicator != null)
		{
			mIconPageIndicator.notifyDataSetChanged();
		}
	}

	@Override
	public void stickerSelected(Sticker sticker, String source)
	{
		if (listener != null)
		{
			listener.stickerSelected(sticker, source);
		}
	}
}