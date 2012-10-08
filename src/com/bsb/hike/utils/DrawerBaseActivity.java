package com.bsb.hike.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.ui.MessagesList;
import com.bsb.hike.view.DrawerLayout;

public class DrawerBaseActivity extends Activity implements DrawerLayout.Listener, HikePubSub.Listener{

	private DrawerLayout parentLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		// This does not apply to pre-Honeycomb devices, 
		getWindow().setFlags(HikeConstants.FLAG_HARDWARE_ACCELERATED, 
				HikeConstants.FLAG_HARDWARE_ACCELERATED);
	}

	public void afterSetContentView(Bundle savedInstanceState)
	{
		parentLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		parentLayout.setListener(this);
		parentLayout.setUpLeftDrawerView();
		
		findViewById(R.id.topbar_menu).setVisibility(View.VISIBLE);
		findViewById(R.id.menu_bar).setVisibility(View.VISIBLE);
		if(savedInstanceState != null && savedInstanceState.getBoolean(HikeConstants.Extras.IS_DRAWER_VISIBLE))
		{
			parentLayout.toggleLeftSidebar(true);
		}
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.PROFILE_PIC_CHANGED, this);
		HikeMessengerApp.getPubSub().addListener(HikePubSub.PROFILE_NAME_CHANGED, this);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SMS_CREDIT_CHANGED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.PROFILE_PIC_CHANGED, this);
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.PROFILE_NAME_CHANGED, this);
	}

	public void onToggleSideBarClicked(View v)
	{
		Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_BUTTON);
		parentLayout.toggleLeftSidebar(false);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.IS_DRAWER_VISIBLE, this.parentLayout != null && this.parentLayout.isLeftOpening());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		if(parentLayout.isLeftOpening())
		{
			parentLayout.closeLeftSidebar(false);
		}
		else
		{
			if(!(this instanceof MessagesList))
			{
				Intent intent = new Intent(this, MessagesList.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				overridePendingTransition(R.anim.alpha_in, R.anim.slide_out_right_noalpha);
			}
			else
			{
				finish();
			}
		}
	}

	@Override
	public boolean onContentTouchedWhenOpeningLeftSidebar() 
	{
		parentLayout.closeLeftSidebar(false);
		return true;
	}

	@Override
	public void onEventReceived(String type, Object object) 
	{
		if (HikePubSub.SMS_CREDIT_CHANGED.equals(type))
		{
			final int credits = (Integer) object;
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					parentLayout.updateCredits(credits);
				}
			});
		}
		else if (HikePubSub.PROFILE_PIC_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() 
				{
					parentLayout.setProfileImage();
				}
			});
		}
		else if (HikePubSub.PROFILE_NAME_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() 
				{
					parentLayout.setProfileName();
				}
			});
		}
	}

}
