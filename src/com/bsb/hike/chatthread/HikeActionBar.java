package com.bsb.hike.chatthread;

import java.util.List;
import java.util.Set;

import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.media.OverFlowMenuItem;
import com.bsb.hike.media.OverFlowMenuLayout;
import com.bsb.hike.media.OverflowItemClickListener;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class HikeActionBar
{

	private SherlockFragmentActivity mActivity;

	public OverFlowMenuLayout overFlowMenuLayout;

	/**
	 * @generated
	 * @ordered
	 */

	public HikeActionBar(SherlockFragmentActivity activity)
	{
		this.mActivity = activity;

	}

	public void onCreateOptionsMenu(Menu menu, int menuLayout)
	{
		MenuInflater menuInflater = mActivity.getSupportMenuInflater();
		menuInflater.inflate(menuLayout, menu);
	}

	public void onCreateOptionsMenu(Menu menu, int menuLayout, List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener)
	{
		onCreateOptionsMenu(menu, menuLayout);
		onCreateOverflowMenu(overflowItems, listener);
	}

	private void onCreateOverflowMenu(List<OverFlowMenuItem> overflowItems, OverflowItemClickListener listener)
	{
		overFlowMenuLayout = new OverFlowMenuLayout(overflowItems, listener, mActivity);
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
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void setActionbarXML(int parameter)
	{
		// TODO implement me
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
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public Set<OverFlowMenuItem> getOverFlowMenuItems()
	{
		// TODO implement me
		return null;
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
		overFlowMenuLayout.show(width, height, xOffset, yOffset, anchor);
	}

}
