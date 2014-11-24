package com.bsb.hike.chatthread;

import android.content.Context;
import android.view.View;
import android.widget.PopupWindow;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public abstract class PopUpLayout {

	protected PopupWindow popup;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public PopUpLayout() {
		super();

	}

	/**
	 * It should return view which you want to show inside popup window, it will
	 * be called every time you call showPopUpWindow
	 */

	public abstract View getView();

	/**
	 * do processing to inflate your view, it will be called again and again
	 * whenever showPopUpWindow is called
	 */
	public abstract void initView();

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	protected void showPopUpWindow(int width, int height, View anchor,
			Context context) {
		showPopUpWindow(width, height, 0, 0, anchor, context);
	}

	protected void showPopUpWindow(int width, int height, int xoffset,
			int yoffset, View anchor, Context context) {
		initView();
		if (popup == null) {
			getPopUpWindow(width, height, getView(), context);
		}
		popup.showAsDropDown(anchor, xoffset, yoffset);
	}

	protected PopupWindow getPopUpWindow(int width, int height,
			View viewToShow, Context context) {
		popup = new PopupWindow(context);
		popup.setWidth(width);
		popup.setHeight(height);
		if (popup.getContentView() != viewToShow) {
			popup.setContentView(viewToShow);
		}
		return popup;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void updateDimension(int width, int height) {
		if (isShowing()) {
			popup.update(width, height);
		}
	}

	public boolean isShowing() {
		return popup != null && popup.isShowing();
	}
}
