package com.bsb.hike.chatthread;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.R;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class ChatThread implements OverflowItemClickListener,View.OnClickListener {

	protected ChatThreadActivity activity;

	public ChatThread(ChatThreadActivity activity) {
		this.activity = activity;
		init();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public MessageSenderLayout messageSenderLayout;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public ChatThreadActionBar chatThreadActionBar;

	public void onCreate(Bundle arg0) {

		init();
	}

	private void init() {
		chatThreadActionBar = new ChatThreadActionBar(activity);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.findItem(R.id.overflow_menu).getActionView().setOnClickListener(this);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}

	@Override
	public void itemClicked(OverFlowMenuItem parameter) {

	}

	protected String getString(int stringId) {
		return activity.getString(stringId);
	}

	protected Resources getResources() {
		return activity.getResources();
	}

	public void setContentView() {
	}

	protected OverFlowMenuItem[] getOverFlowMenuItems() {
		return new OverFlowMenuItem[] {
				new OverFlowMenuItem(getString(R.string.clear_chat), 0, 0,
						R.string.clear_chat),
				new OverFlowMenuItem(getString(R.string.email_chat), 0, 0,
						R.string.email_chat) };
	}

	protected void showOverflowMenu() {
		int width = getResources().getDimensionPixelSize(
				R.dimen.overflow_menu_width);
		int rightMargin = width
				+ getResources().getDimensionPixelSize(
						R.dimen.overflow_menu_right_margin);
		chatThreadActionBar.overFlowMenuLayout.showPopUpWindow(width,
				LayoutParams.WRAP_CONTENT, -rightMargin,
				-(int) (0.5 * Utils.densityMultiplier),
				activity.findViewById(R.id.attachment_anchor));
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.overflowmenu:
			showOverflowMenu();
			break;
		}
		
	}

}
