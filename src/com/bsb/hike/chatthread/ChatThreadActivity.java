package com.bsb.hike.chatthread;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;

public class ChatThreadActivity extends HikeAppStateBaseFragmentActivity {

	private ChatThread chatThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		init();
		chatThread.setContentView();
	}

	private void init() {
		boolean isOneToOne = false;
		if (isOneToOne) {
			chatThread = new OneToOneChatThread(this);
		} else {
			chatThread = new GroupChatThread(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return chatThread.onCreateOptionsMenu(menu) ? true : super
				.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		return chatThread.onPrepareOptionsMenu(menu) ? true : super
				.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		return chatThread.onOptionsItemSelected(item) ? true : super
				.onOptionsItemSelected(item);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		init();
	}

	@Override
	public void onBackPressed() {
		if(chatThread.onBackPressed()){
			return;
		}
		super.onBackPressed();
	}
}
