package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.ConversationFragment;
import com.bsb.hike.ui.fragments.FriendsFragment;
import com.bsb.hike.ui.fragments.ImageViewerFragment;
import com.bsb.hike.ui.fragments.UpdatesFragment;
import com.bsb.hike.utils.Utils;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class HomeActivity extends SherlockFragmentActivity implements Listener {

	private static final String IMAGE_FRAGMENT_TAG = "imageFragmentTag";

	private ViewPager viewPager;

	private String[] headers = { "Updates", "Messages", "Friends" };

	private int[] tabIcons = { R.drawable.ic_updates, R.drawable.ic_chats,
			R.drawable.ic_friends };

	private boolean deviceDetailsSent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setDensityMultiplier(this);
		if (Utils.requireAuth(this)) {
			return;
		}
		SharedPreferences accountPrefs = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		boolean justSignedUp = accountPrefs.getBoolean(
				HikeMessengerApp.JUST_SIGNED_UP, false);

		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setIcon(R.drawable.hike_logo_top_bar);

		setContentView(R.layout.home);

		if (savedInstanceState != null) {
			deviceDetailsSent = savedInstanceState
					.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);
		}

		if (justSignedUp) {

			Editor editor = accountPrefs.edit();
			editor.remove(HikeMessengerApp.JUST_SIGNED_UP);
			editor.commit();

			if (!deviceDetailsSent) {
				sendDeviceDetails();
			}
		}

		initialiseViewPager();
		initialiseTabs();
	}

	@Override
	protected void onStart() {
		super.onStart();
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SHOW_IMAGE, this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		HikeMessengerApp.getPubSub()
				.removeListener(HikePubSub.SHOW_IMAGE, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT,
				deviceDetailsSent);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(
				IMAGE_FRAGMENT_TAG);
		if (fragment != null && fragment.isVisible()) {
			FragmentTransaction fragmentTransaction = getSupportFragmentManager()
					.beginTransaction();
			fragmentTransaction.remove(fragment);
			fragmentTransaction.commit();
		} else {
			super.onBackPressed();
		}
	}

	private void sendDeviceDetails() {
		// We're adding this delay to ensure that the service is alive before
		// sending the message
		(new Handler()).postDelayed(new Runnable() {
			@Override
			public void run() {
				JSONObject obj = Utils.getDeviceDetails(HomeActivity.this);
				if (obj != null) {
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.MQTT_PUBLISH, obj);
				}
				Utils.requestAccountInfo(false);
				Utils.sendLocaleToServer(HomeActivity.this);
			}
		}, 5 * 1000);
		deviceDetailsSent = true;
	}

	private void initialiseTabs() {
		TabPageIndicator TabIndicator = (TabPageIndicator) findViewById(R.id.titles);
		TabIndicator.setViewPager(viewPager, 1);
	}

	private void initialiseViewPager() {
		viewPager = (ViewPager) findViewById(R.id.viewpager);

		List<Fragment> fragmentList = new ArrayList<Fragment>(headers.length);
		fragmentList.add(getFragmentForIndex(0));
		fragmentList.add(getFragmentForIndex(1));
		fragmentList.add(getFragmentForIndex(2));

		viewPager.setAdapter(new HomeAdapter(getSupportFragmentManager(),
				fragmentList));
		viewPager.setCurrentItem(1);
	}

	private Fragment getFragmentForIndex(int index) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(
				"android:switcher:" + viewPager.getId() + ":" + index);
		if (fragment == null) {
			switch (index) {
			case 0:
				fragment = new UpdatesFragment();
				break;

			case 1:
				fragment = new ConversationFragment();
				break;

			case 2:
				fragment = new FriendsFragment();
				break;
			}
		}
		return fragment;
	}

	private class HomeAdapter extends FragmentPagerAdapter implements
			IconPagerAdapter {

		List<Fragment> fragments;

		public HomeAdapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return headers[position];
		}

		@Override
		public int getIconResId(int index) {
			return tabIcons[index];
		}

	}

	@Override
	public void onEventReceived(String type, final Object object) {
		if (HikePubSub.SHOW_IMAGE.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Bundle arguments = (Bundle) object;

					ImageViewerFragment imageViewerFragment = new ImageViewerFragment();
					imageViewerFragment.setArguments(arguments);

					FragmentTransaction fragmentTransaction = getSupportFragmentManager()
							.beginTransaction();
					fragmentTransaction.add(R.id.parent_layout,
							imageViewerFragment, IMAGE_FRAGMENT_TAG);
					fragmentTransaction.commit();
				}
			});
		}
	}

}
