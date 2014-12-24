package com.bsb.hike.media;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;

/**
 * This class is used when we have to share KeyBoardPopup layout with other views. It's an aggregator class, and has the KeyBoard popup layout object, as well as a list of
 * {@link ShareablePopup} interfaces. The class uses intelligence in the {@link #showPopup(ShareablePopup)} to display the views.
 * 
 * @author piyush
 */
public class ShareablePopupLayout
{
	private KeyboardPopupLayout mKeyboardPopupLayout;

	private View mViewToDisplay;

	private View prevVisibleView;

	private static String TAG = "ShareablePopupLayout";

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatOuterTouchIds
	 */

	public ShareablePopupLayout(Context context, View mainView, int firstTimeHeight, int[] eatOuterTouchIds)
	{
		initViewToDisplay(context);
		initPopupLayout(context, mainView, firstTimeHeight, eatOuterTouchIds);
	}

	private void initViewToDisplay(Context context)
	{
		this.mViewToDisplay = LayoutInflater.from(context).inflate(R.layout.shared_keyboard_layout, null);
	}

	/**
	 * Initializes the shared keyboard popup layout for Sticker Picker and Emoticon Picker
	 * 
	 * @param context
	 * @param mainView
	 * @param firstTimeHeight
	 * @param eatOuterTouchIds
	 */

	private void initPopupLayout(Context context, View mainView, int firstTimeHeight, int[] eatOuterTouchIds)
	{
		if (mKeyboardPopupLayout == null)
		{
			mKeyboardPopupLayout = (eatOuterTouchIds == null) ? new KeyboardPopupLayout(mainView, firstTimeHeight, context) : new KeyboardPopupLayout(mainView, firstTimeHeight,
					context, eatOuterTouchIds);
		}
	}


	/**
	 * Utility method used for displaying the Popups using the Keyboard Popup layout. Appropriate comments have been added in the code flow for easily readability
	 * 
	 * @param popup
	 */

	public void showPopup(ShareablePopup popup)
	{
		View popupView = popup.getView();

		addPopupView(popupView);

		swapViews(popupView);

		showKeyboardPopup();
	}

	/**
	 * Check whether we already had this view in the layout or not, else add it
	 * 
	 */

	private void addPopupView(View popupView)
	{
		FrameLayout frameLayout = (FrameLayout) mViewToDisplay.findViewById(R.id.shared_keyboard_parent);

		for (int i = 0; i < frameLayout.getChildCount(); i++)
		{
			if (frameLayout.getChildAt(i) == popupView)
			{
				return;
			}
		}

		/**
		 * New view. Add it to the layout
		 */
		Logger.i(TAG, "Adding popupView in frameLayout");
		frameLayout.addView(popupView);
	}

	private void showKeyboardPopup()
	{
		if (!mKeyboardPopupLayout.isShowing())
		{
			mKeyboardPopupLayout.showKeyboardPopup(mViewToDisplay);
		}
	}

	/**
	 * Used for swapping the prevVisible view with the new view which is to be made visible
	 * 
	 * @param popupView
	 */
	private void swapViews(View popupView)
	{
		if (prevVisibleView == null)
		{
			prevVisibleView = popupView;
			popupView.setVisibility(View.VISIBLE);
		}

		else if (prevVisibleView != popupView)
		{
			prevVisibleView.setVisibility(View.GONE);
			popupView.setVisibility(View.VISIBLE);
			prevVisibleView = popupView;
		}
	}

	/**
	 * Used for dismissing the popup window
	 */
	public void dismiss()
	{
		mKeyboardPopupLayout.dismiss();
	}

	public boolean isShowing()
	{
		return mKeyboardPopupLayout.isShowing();
	}

	public void updateMainView(View view)
	{
		this.mKeyboardPopupLayout.updateMainView(view);
	}

}