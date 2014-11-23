package com.bsb.hike.chatthread;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.R;
import com.bsb.hike.chatthread.ThemePicker.ThemePickerListener;
import com.bsb.hike.utils.ChatTheme;
import com.bsb.hike.utils.Utils;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class ChatThread implements OverflowItemClickListener,View.OnClickListener, ThemePickerListener,BackPressListener {

	protected ChatThreadActivity activity;
	protected ThemePicker themePicker;

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
	public void itemClicked(OverFlowMenuItem item) {
		switch(item.uniqueness){
		case R.string.clear_chat:
			break;
		case R.string.email_chat:
			break;
		}
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
	
	protected void showThemePicker(){
		if(themePicker == null){
			themePicker = new ThemePicker(activity, this);
		}
		themePicker.showThemePicker(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0, 0, activity.findViewById(R.id.cb_anchor), null);
	}

	@Override
	public void themeClicked(ChatTheme theme) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void themeSelected(ChatTheme theme) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void themeCancelled() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onBackPressed() {
		if(themePicker!=null && themePicker.isShowing()){
			return themePicker.onBackPressed();
		}
		return false;
	}
	

}
