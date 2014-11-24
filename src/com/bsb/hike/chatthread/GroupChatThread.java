package com.bsb.hike.chatthread;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bsb.hike.R;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */

public class GroupChatThread extends ChatThread implements HashTagModeListener {
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public GroupChatThread(ChatThreadActivity activity) {
		super(activity);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void hashTagModeEnded(String parameter) {
		// TODO implement me
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */

	public void hashTagModeStarted(String parameter) {
		// TODO implement me
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		chatThreadActionBar.onCreateOptionsMenu(menu,
				R.menu.group_chat_thread_menu, getOverFlowItems(), this);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onOptionsItemSelected(item);
	}

	private List<OverFlowMenuItem> getOverFlowItems() {

		List<OverFlowMenuItem> list = new ArrayList<OverFlowMenuItem>();
		list.add(new OverFlowMenuItem(getString(R.string.group_profile), 0, 0,
				R.string.group_profile));

		for (OverFlowMenuItem item : super.getOverFlowMenuItems()) {
			list.add(item);
		}
		list.add(new OverFlowMenuItem(
				isMuted() ? getString(R.string.unmute_group)
						: getString(R.string.mute_group), 0, 0,
				R.string.mute_group));
		list.add(new OverFlowMenuItem(getString(R.string.chat_theme_small), 0,
				0, R.string.chat_theme));
		return list;
	}

	@Override
	public void setContentView() {
		activity.setContentView(R.layout.chatthread);
	}
	private boolean isMuted(){
		return false;
	}

	@Override
	public void itemClicked(OverFlowMenuItem item) {
		if(item.uniqueness == R.string.chat_theme){
			showThemePicker();
		}
		super.itemClicked(item);
	}
}
