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
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public abstract View getView();

	public abstract void initView();

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	protected void showPopUpWindow(int width, int height, View viewToShow,
			View anchor, Context context) {
		showPopUpWindow(width, height, 0, 0, viewToShow, anchor, context);
	}

	protected void showPopUpWindow(int width, int height, int xoffset,
			int yoffset, View viewToShow, View anchor, Context context) {
		if(popup==null){
		getPopUpWindow(width, height, viewToShow, context);
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
		if (popup != null && popup.isShowing()) {
			popup.update(width, height);
		}
	}

	public boolean isShowing() {
		return popup != null && popup.isShowing();
	}
}
