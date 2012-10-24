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

	private String[] pubSubListeners = 
			{
			HikePubSub.SMS_CREDIT_CHANGED,
			HikePubSub.PROFILE_PIC_CHANGED,
			HikePubSub.PROFILE_NAME_CHANGED,
			HikePubSub.ICON_CHANGED,
			HikePubSub.RECENT_CONTACTS_UPDATED
			};

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
		parentLayout.setUpRightDrawerView();
		
		findViewById(R.id.topbar_menu).setVisibility(View.VISIBLE);
		findViewById(R.id.menu_bar).setVisibility(View.VISIBLE);
		if(savedInstanceState != null)
		{
			if(savedInstanceState.getBoolean(HikeConstants.Extras.IS_LEFT_DRAWER_VISIBLE))
			{
				parentLayout.toggleSidebar(true, true);
			}
			else if(savedInstanceState.getBoolean(HikeConstants.Extras.IS_RIGHT_DRAWER_VISIBLE))
			{
				parentLayout.toggleSidebar(true, false);
			}
		}

//		findViewById(R.id.title_image_btn2).setVisibility(View.VISIBLE);
//		findViewById(R.id.button_bar3).setVisibility(View.VISIBLE);

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
	}

	public void onToggleLeftSideBarClicked(View v)
	{
		Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_BUTTON);
		parentLayout.toggleSidebar(false, true);
	}

	public void onTitleIconClick(View v)
	{
		Utils.logEvent(this, HikeConstants.LogEvent.DRAWER_BUTTON);
		parentLayout.toggleSidebar(false, false);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.IS_LEFT_DRAWER_VISIBLE, this.parentLayout != null && this.parentLayout.isLeftOpening());
		outState.putBoolean(HikeConstants.Extras.IS_RIGHT_DRAWER_VISIBLE, this.parentLayout != null && this.parentLayout.isRightOpening());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed()
	{
		if(parentLayout.isLeftOpening())
		{
			parentLayout.closeLeftSidebar(false);
		}
		else if(parentLayout.isRightOpening())
		{
			parentLayout.closeRightSidebar(false);
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
	public boolean onContentTouchedWhenOpeningRightSidebar() 
	{
		parentLayout.closeRightSidebar(false);
		return true;
	}

	@Override
	public void onEventReceived(String type, final Object object) 
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
		else if (HikePubSub.ICON_CHANGED.equals(type))
		{
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					parentLayout.refreshFavoritesDrawer();
				}
			});
		}
		else if (HikePubSub.RECENT_CONTACTS_UPDATED.equals(type))
		{
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					parentLayout.updateRecentContacts((String) object);
				}
			});
		}
	}

}
