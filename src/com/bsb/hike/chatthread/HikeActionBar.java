package com.bsb.hike.chatthread;

import java.util.List;
import java.util.Set;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverFlowMenuLayout;
import com.bsb.hike.media.OverflowItemClickListener;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class HikeActionBar
{

	private HikeAppStateBaseFragmentActivity mActivity;

	public OverFlowMenuLayout overFlowMenuLayout;
	
	private Menu mMenu;

	/**
	 * @generated
	 * @ordered
	 */

	public HikeActionBar(HikeAppStateBaseFragmentActivity activity)
	{
		this.mActivity = activity;

	}

	public void onCreateOptionsMenu(Menu menu, int menuLayout)
	{
		MenuInflater menuInflater = mActivity.getSupportMenuInflater();
		menuInflater.inflate(menuLayout, menu);
		this.mMenu = menu;
	}

	public void onCreateOptionsMenu(Menu menu, int menuLayout, List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener, OnDismissListener onDismissListener)
	{
		onCreateOptionsMenu(menu, menuLayout);
		onCreateOverflowMenu(overflowItems, listener, onDismissListener);
	}

	private void onCreateOverflowMenu(List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener, OnDismissListener onDismissListener)
	{
		overFlowMenuLayout = new OverFlowMenuLayout(overflowItems, listener, onDismissListener, mActivity);
	}

	public boolean isOverflowMenuPopulated()
	{
		return overFlowMenuLayout != null;
	}

	public void onPreareOptionsMenu(Menu menu)
	{

	}

	/**
	 * If something is your activity specific then handle that click on your side pass click if it is some sort of utility OR common case which this object can handle for example
	 * copy
	 * 
	 * @param menuItem
	 */
	public boolean onOptionsItemSelected(MenuItem menuItem)
	{

		return false;
	}

	/**
	 * This function is used to inflate a custom layout for action bar. 
	 * It returns the view inflated. The calling classes have to set the View in the ActionBar themselves.
	 * @param layoutResId
	 */
	public View inflateCustomActionBarView(int layoutResId, int color)
	{
		ActionBar sherlockActionBar = mActivity.getSupportActionBar();
		sherlockActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		
		View actionBarView = LayoutInflater.from(mActivity.getApplicationContext()).inflate(layoutResId, null);
		
		sherlockActionBar.setCustomView(actionBarView);
		
		/**
		 * Setting the custom color here
		 */
		if(color != -1)
		{
			mActivity.updateActionBarColor(color);
		}
		
		return actionBarView;
	}
	
	/**
	 * Called when we use the default color for action bar's background
	 * @param layoutResId
	 * @return
	 */
	public View setCustomActionBarView(int layoutResId)
	{
		return inflateCustomActionBarView(layoutResId, -1);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public boolean enableMenuItem(int parameter)
	{
		// TODO implement me
		return false;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public boolean disableMenuItem(int parameter)
	{
		// TODO implement me
		return false;
	}

	/**
	 * Returns the list of overflow menu items held by this ActionBar
	 * @return
	 */
	public List<OverFlowMenuItem> getOverFlowMenuItems()
	{
		if(overFlowMenuLayout != null)
		{
			return overFlowMenuLayout.getOverFlowMenuItems(); 
		}
		
		else
		{
			return null;
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void setOverFlowMenuItems(Set<OverFlowMenuItem> parameter, OverflowItemClickListener parameter2)
	{
		// TODO implement me
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void setOverFloeClickListener(OverflowItemClickListener parameter)
	{
		// TODO implement me
	}

	public void showOverflowMenu(int width, int height, int xOffset, int yOffset, View anchor)
	{
		overFlowMenuLayout.show(width, height, xOffset, yOffset, anchor, PopupWindow.INPUT_METHOD_NOT_NEEDED);
	}
	
	/**
	 * Returns a menuItem for a given resId
	 * 
	 * @param resId
	 * @return
	 */
	public MenuItem getMenuItem(int resId)
	{
		MenuItem menuItem = null;
		if (mMenu != null)
		{
			menuItem = mMenu.findItem(resId);
		}

		return menuItem;
	}

}
