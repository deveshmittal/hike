package com.bsb.hike.chatthread;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.ui.HomeActivity;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;

public class ChatThreadActivity extends HikeAppStateBaseFragmentActivity
{

	private ChatThread chatThread;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);

		if (filter())
		{
			init();
			chatThread.onCreate(savedInstanceState);
		}
	}

	private boolean filter()
	{
		String msisdn = getIntent().getStringExtra(HikeConstants.Extras.MSISDN);
		if (HikeMessengerApp.isStealthMsisdn(msisdn))
		{
			if (HikeSharedPreferenceUtil.getInstance(this).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF) != HikeConstants.STEALTH_ON)
			{
				Intent intent = new Intent(this, HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				this.startActivity(intent);
				this.finish();
				return false;
			}
		}
		return true;
	}

	private void init()
	{
		String whichChatThread = getIntent().getStringExtra(HikeConstants.Extras.WHICH_CHAT_THREAD);
		if (HikeConstants.Extras.ONE_TO_ONE_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new OneToOneChatThread(this, getIntent().getStringExtra(HikeConstants.Extras.MSISDN));
		}
		else if (HikeConstants.Extras.GROUP_CHAT_THREAD.equals(whichChatThread))
		{
			chatThread = new GroupChatThread(this, getIntent().getStringExtra(HikeConstants.Extras.MSISDN));
		}
		else
		{
			throw new IllegalArgumentException("Which chat thread I am !!! Did you pass proper arguments?");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return chatThread.onCreateOptionsMenu(menu) ? true : super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{

		return chatThread.onPrepareOptionsMenu(menu) ? true : super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		return chatThread.onOptionsItemSelected(item) ? true : super.onOptionsItemSelected(item);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		init();
	}

	@Override
	public void onBackPressed()
	{
		if (chatThread.onBackPressed())
		{
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		chatThread.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy()
	{
		chatThread.onDestroy();
		super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		chatThread.onPause();
		super.onPause();
	}
	
	@Override
	protected void onResume()
	{
		chatThread.onResume();
		super.onResume();
	}
	
	@Override
	protected void onRestart()
	{
		chatThread.onRestart();
		super.onRestart();
	}
	
	public String getContactNumber()
	{
		return chatThread.getContactNumber();
	}
}
