package com.bsb.hike.media;

import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow.OnDismissListener;

public class KeyboardPopupLayout extends PopUpLayout implements OnDismissListener
{
	private View mainView;

	private int possibleKeyboardHeightLand, possibleKeyboardHeight, originalBottomPadding;

	private boolean isKeyboardOpen;

	private int firstTimeHeight;

	/**
	 * 
	 * @param mainView
	 *            - This should be top most view of your activity which get resized when soft keyboard is toggled
	 * @param firstTimeHeight
	 *            - This is the height which will be used before keyboard opens
	 * @param context
	 */
	public KeyboardPopupLayout(View mainView, int firstTimeHeight, Context context)
	{
		super(context);
		this.mainView = mainView;
		this.firstTimeHeight = firstTimeHeight;
		originalBottomPadding = mainView.getPaddingBottom();
		registerOnGlobalLayoutListener();
	}

	private void registerOnGlobalLayoutListener()
	{
		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{

			@Override
			public void onGlobalLayout()
			{
				Log.i("chatthread", "global layout listener");

				Log.i("chatthread", "global layout listener rootHeight " + mainView.getRootView().getHeight() + " new height " + mainView.getHeight());
				Rect r = new Rect();
				mainView.getWindowVisibleDisplayFrame(r);
				// this is height of view which is visible on screen
				int rootViewHeight = mainView.getRootView().getHeight();
				int temp = rootViewHeight - r.bottom;
				Logger.i("chatthread", "keyboard  height " + temp);
				boolean islandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
				if (temp > 0)
				{
					if (islandScape)
					{
						possibleKeyboardHeightLand = temp;
					}
					else
					{
						possibleKeyboardHeight = temp;
					}
					isKeyboardOpen = true;
					updateDimension(LayoutParams.MATCH_PARENT, temp);
				}
				else
				{
					// when we change orientation , from portrait to landscape and keyboard is open , it is possible that screen does adjust its size more than once until it
					// stabilize
					if (islandScape)
						possibleKeyboardHeightLand = 0;
					isKeyboardOpen = false;
				}
			}
		});
	}

	private void updatePadding(int bottomPadding)
	{
		if (mainView.getPaddingBottom() != bottomPadding)
		{
			Logger.i("chatthread", "resize main height with bottom padding " + bottomPadding);
			mainView.setPadding(0, 0, 0, bottomPadding);
		}
	}

	public void showKeyboardPopup(View view)
	{
		boolean islandScape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		int height = islandScape ? possibleKeyboardHeightLand : possibleKeyboardHeight;
		if (height == 0)
		{
			height = firstTimeHeight;
		}
		if (popup == null)
		{
			initPopUpWindow(LayoutParams.MATCH_PARENT, height, view, context);
			// this is a strange bug in Android, if we set focusable true, GRAVITY BOTTOM IS NOT working
			popup.setFocusable(false);
		}
		setOnDismissListener(this);
		if (isKeyboardOpen)
		{
			updatePadding(0);
		}
		else
		{
			updatePadding(popup.getHeight());
		}
		popup.showAtLocation(mainView, Gravity.BOTTOM, 0, 0);
	}

	@Override
	public void onDismiss()
	{
		/**
		 * Whenever this pop up is dismissed, we want bottom padding of mainview to be reset
		 */
		updatePadding(originalBottomPadding);
	}

}
