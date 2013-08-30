package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.ConversationFragment;
import com.bsb.hike.ui.fragments.FriendsFragment;
import com.bsb.hike.ui.fragments.UpdatesFragment;
import com.bsb.hike.utils.AppRater;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class HomeActivity extends HikeAppStateBaseFragmentActivity {

	public static final int UPDATES_TAB_INDEX = 0;
	public static final int CHATS_TAB_INDEX = 1;
	public static final int FRIENDS_TAB_INDEX = 2;

	private enum DialogShowing {
		SMS_CLIENT, SMS_SYNC_CONFIRMATION, SMS_SYNCING
	}

	private ViewPager viewPager;
	private DialogShowing dialogShowing;

	private int[] headers = { R.string.updates, R.string.chats,
			R.string.friends };

	private int[] tabIcons = { R.drawable.ic_updates, R.drawable.ic_chats,
			R.drawable.ic_friends };

	private boolean deviceDetailsSent;

	private View parentLayout;
	private Dialog dialog;
	private SharedPreferences accountPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setDensityMultiplier(this);
		if (Utils.requireAuth(this)) {
			return;
		}
		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				0);

		boolean justSignedUp = accountPrefs.getBoolean(
				HikeMessengerApp.JUST_SIGNED_UP, false);

		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setIcon(R.drawable.hike_logo_top_bar);

		setContentView(R.layout.home);

		parentLayout = findViewById(R.id.parent_layout);

		if (savedInstanceState != null) {
			deviceDetailsSent = savedInstanceState
					.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);
			int dialogShowingOrdinal = savedInstanceState
					.getInt(HikeConstants.Extras.DIALOG_SHOWING);
			if (dialogShowingOrdinal != -1) {
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
			} else {
				dialogShowing = null;
			}
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

		if (savedInstanceState == null && dialogShowing == null) {
			/*
			 * Only show app rater if the tutorial is not being shown an the app
			 * was just launched i.e not an orientation change
			 */
			AppRater.appLaunched(this);
		} else if (dialogShowing != null) {
			switch (dialogShowing) {
			case SMS_CLIENT:
				showSMSClientDialog();
				break;

			case SMS_SYNC_CONFIRMATION:
			case SMS_SYNCING:
				showSMSSyncDialog();
				break;
			}
		}

		if (!AppRater.showingDialog() && dialogShowing == null) {
			if (!accountPrefs.getBoolean(
					HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, true)) {
				showSMSClientDialog();
			}
		}
	}

	private void showSMSClientDialog() {
		dialogShowing = DialogShowing.SMS_CLIENT;

		dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sms_with_hike_popup);
		dialog.setCancelable(false);

		Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) dialog.findViewById(R.id.btn_cancel);

		okBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Utils.setReceiveSmsSetting(getApplicationContext(), true);

				Editor editor = PreferenceManager.getDefaultSharedPreferences(
						getApplicationContext()).edit();
				editor.putBoolean(HikeConstants.SEND_SMS_PREF, true);
				editor.commit();

				dialogShowing = null;
				dialog.dismiss();
				if (!accountPrefs.getBoolean(
						HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false)) {
					showSMSSyncDialog();
				}
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Utils.setReceiveSmsSetting(getApplicationContext(), false);
				dialogShowing = null;
				dialog.dismiss();
			}
		});

		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				Editor editor = accountPrefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, true);
				editor.commit();
			}
		});
		dialog.show();
	}

	private void showSMSSyncDialog() {
		if (dialogShowing == null) {
			dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION;
		}

		dialog = Utils.showSMSSyncDialog(this,
				dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION);
	}

	@Override
	protected void onResume() {
		super.onResume();
		HikeMessengerApp.getPubSub().publish(
				HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
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
		outState.putInt(HikeConstants.Extras.DIALOG_SHOWING,
				dialogShowing != null ? dialogShowing.ordinal() : -1);
		super.onSaveInstanceState(outState);
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
		TabPageIndicator tabIndicator = (TabPageIndicator) findViewById(R.id.titles);
		tabIndicator.setViewPager(viewPager,
				getIntent().getIntExtra(HikeConstants.Extras.TAB_INDEX, 1));
		tabIndicator.setOnPageChangeListener(onPageChangeListener);

		setBackground();
	}

	int initialRed = 231;
	int initialGreen = 226;
	int initialBlue = 214;

	int finalRed = 255;
	int finalGreen = 255;
	int finalBlue = 255;

	private void setBackground() {
		int position = viewPager.getCurrentItem();
		if (position == 0) {
			parentLayout.setBackgroundColor(Color.argb(255, initialRed,
					initialGreen, initialBlue));
		} else {
			parentLayout.setBackgroundColor(Color.argb(255, finalRed,
					finalGreen, finalBlue));
		}
	}

	/*
	 * Implemented to add a fade change in color when switching between updates
	 * tab and other tabs
	 */
	OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {

		@Override
		public void onPageSelected(int position) {
			setBackground();
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
			if (position != 0) {
				return;
			}

			int percent = (int) (positionOffset * 100);
			if (percent % 2 != 0) {
				return;
			}

			int red = initialRed
					+ (int) (((finalRed - initialRed) * percent) / 100);
			int green = initialGreen
					+ (int) (((finalGreen - initialGreen) * percent) / 100);
			int blue = initialBlue
					+ (int) (((finalBlue - initialBlue) * percent) / 100);

			parentLayout.setBackgroundColor(Color.argb(255, red, green, blue));
		}

		@Override
		public void onPageScrollStateChanged(int state) {

		}
	};

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
			return getString(headers[position]);
		}

		@Override
		public int getIconResId(int index) {
			return tabIcons[index];
		}

	}

}
