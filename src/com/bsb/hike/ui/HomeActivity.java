package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.ui.fragments.ConversationFragment;
import com.bsb.hike.ui.fragments.FriendsFragment;
import com.bsb.hike.ui.fragments.UpdatesFragment;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.AppRater;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class HomeActivity extends HikeAppStateBaseFragmentActivity implements
		Listener {

	public static final int UPDATES_TAB_INDEX = 0;
	public static final int CHATS_TAB_INDEX = 1;
	public static final int FRIENDS_TAB_INDEX = 2;
	private static final boolean TEST = false;  //TODO: Test flag only, turn off for Production

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
	private ProgressDialog progDialog;
	private boolean showingProgress = false;

	private String[] pubSubListeners = {
			HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT,
			HikePubSub.RESET_UNREAD_COUNT, 
			HikePubSub.FINISHED_AVTAR_UPGRADE};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Utils.requireAuth(this)) {
			return;
		}
		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				0);

		//Checking whether the state of the avatar and conv DB Upgrade settings is 1
		//If it's 1, it means we need to show a progress dialog and then wait for the
		// pub sub thread event to cancel the dialog once the upgrade is done.
		if (((accountPrefs.getInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1) == 1)
				&& (accountPrefs.getInt(
						HikeConstants.UPGRADE_AVATAR_PROGRESS_USER, -1) == 1 ))|| TEST) {
			progDialog = ProgressDialog.show(this,
					getString(R.string.work_in_progress),
					getString(R.string.upgrading_to_a_new_and_improvd_hike), true);
			showingProgress=true;
		}
		
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
			int dialogShowingOrdinal = savedInstanceState.getInt(
					HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1) {
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
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

		showUpdateIcon = Utils.getNotificationCount(accountPrefs, false) > 0;

		initialiseViewPager();
		//make the view pager visibility GONE , as the spinner is showing
		//We do not want to see messages till we are ready ! 
		if (showingProgress) {
			viewPager.setVisibility(View.GONE);
		}
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

		HikeMessengerApp.getPubSub().addListeners(this, pubSubListeners);
	}

	@Override
	protected void onDestroy() {
		HikeMessengerApp.getPubSub().removeListeners(this, pubSubListeners);
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (Utils.requireAuth(this)) {
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return setupMenuOptions(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem gamesItem = menu.findItem(R.id.games);
		MenuItem rewardsItem = menu.findItem(R.id.rewards);
		MenuItem muteItem = menu.findItem(R.id.mute_notification);
		MenuItem freeSmsItem = menu.findItem(R.id.free_sms);

		SharedPreferences prefs = this.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		if (rewardsItem != null) {
			rewardsItem.setVisible(prefs.getBoolean(
					HikeMessengerApp.SHOW_REWARDS, false));
		}

		if (gamesItem != null) {
			gamesItem.setVisible(prefs.getBoolean(HikeMessengerApp.SHOW_GAMES,
					false));
		}

		SharedPreferences appPref = PreferenceManager
				.getDefaultSharedPreferences(this);

		if (muteItem != null) {
			int preference = appPref.getInt(HikeConstants.STATUS_PREF, 0);
			muteItem.setTitle(preference == 0 ? R.string.mute_notifications
					: R.string.unmute_notifications);
		}

		if (freeSmsItem != null) {
			boolean preference = appPref.getBoolean(
					HikeConstants.FREE_SMS_PREF, true);
			freeSmsItem.setVisible(preference);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	private boolean setupMenuOptions(Menu menu) {
		switch (viewPager.getCurrentItem()) {
		case UPDATES_TAB_INDEX:
			getSupportMenuInflater().inflate(R.menu.updates_menu, menu);
			return true;
		case CHATS_TAB_INDEX:
			getSupportMenuInflater().inflate(R.menu.chats_menu, menu);
			return true;
		case FRIENDS_TAB_INDEX:
			getSupportMenuInflater().inflate(R.menu.friends_menu, menu);

			final SearchView searchView = new SearchView(getSupportActionBar()
					.getThemedContext());
			searchView.setQueryHint(getString(R.string.search_hint));
			searchView.setIconifiedByDefault(false);
			searchView.setIconified(false);
			searchView.setOnQueryTextListener(onQueryTextListener);
			searchView.clearFocus();

			menu.add(Menu.NONE, Menu.NONE, 1, R.string.search_hint)
					.setIcon(R.drawable.ic_top_bar_search)
					.setActionView(searchView)
					.setShowAsAction(
							MenuItem.SHOW_AS_ACTION_ALWAYS
									| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;

		switch (item.getItemId()) {
		case R.id.new_conversation:
			intent = new Intent(this, ComposeActivity.class);
			intent.putExtra(HikeConstants.Extras.EDIT, true);
			break;
		case R.id.new_update:
			intent = new Intent(this, StatusUpdate.class);
			intent.putExtra(HikeConstants.Extras.FROM_CONVERSATIONS_SCREEN,
					true);
			break;
		case R.id.settings:
			intent = new Intent(this, SettingsActivity.class);
			break;
		case R.id.invite:
			intent = new Intent(this, TellAFriend.class);
			break;
		case R.id.free_sms:
			intent = new Intent(this, CreditsActivity.class);
			break;
		case R.id.my_profile:
			intent = new Intent(this, ProfileActivity.class);
			break;
		case R.id.rewards:
			intent = getRewardsIntent();
			break;
		case R.id.games:
			intent = getGamingIntent();
			break;
		case R.id.mute_notification:
			toggleMute();
			return true;
		}

		if (intent != null) {
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private OnQueryTextListener onQueryTextListener = new OnQueryTextListener() {

		@Override
		public boolean onQueryTextSubmit(String query) {
			return false;
		}

		@Override
		public boolean onQueryTextChange(String newText) {
			HikeMessengerApp.getPubSub().publish(HikePubSub.FRIENDS_TAB_QUERY,
					newText);
			return true;
		}
	};
	private TabPageIndicator tabIndicator;

	private Intent getGamingIntent() {

		SharedPreferences prefs = this.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(this.getApplicationContext(),
				WebViewActivity.class);
		intent.putExtra(HikeConstants.Extras.GAMES_PAGE, true);
		/*
		 * using the same token as rewards token, as per DK sir's mail
		 */
		intent.putExtra(HikeConstants.Extras.URL_TO_LOAD, AccountUtils.gamesUrl
				+ prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
		intent.putExtra(HikeConstants.Extras.TITLE, getString(R.string.games));
		return intent;
	}

	private Intent getRewardsIntent() {
		SharedPreferences prefs = this.getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		Intent intent = new Intent(this.getApplicationContext(),
				WebViewActivity.class);
		intent.putExtra(
				HikeConstants.Extras.URL_TO_LOAD,
				AccountUtils.rewardsUrl
						+ prefs.getString(HikeMessengerApp.REWARDS_TOKEN, ""));
		intent.putExtra(HikeConstants.Extras.TITLE, getString(R.string.rewards));
		return intent;
	}

	private void toggleMute() {
		SharedPreferences settingPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		int preference = settingPref.getInt(HikeConstants.STATUS_PREF, 0);

		int newValue;

		Editor editor = settingPref.edit();
		if (preference == 0) {
			newValue = -1;
			editor.putInt(HikeConstants.STATUS_PREF, newValue);
		} else {
			newValue = 0;
			editor.putInt(HikeConstants.STATUS_PREF, newValue);
		}
		editor.commit();

		try {
			JSONObject jsonObject = new JSONObject();
			JSONObject data = new JSONObject();
			data.put(HikeConstants.PUSH_SU, newValue);
			jsonObject.put(HikeConstants.DATA, data);
			jsonObject.put(HikeConstants.TYPE,
					HikeConstants.MqttMessageTypes.ACCOUNT_CONFIG);
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH,
					jsonObject);

			Toast.makeText(
					this,
					newValue == 0 ? R.string.status_notification_on
							: R.string.status_notification_off,
					Toast.LENGTH_SHORT).show();
		} catch (JSONException e) {
			Log.w(getClass().getSimpleName(), e);
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
		if (dialog != null && dialog.isShowing()) {
			outState.putInt(HikeConstants.Extras.DIALOG_SHOWING,
					dialogShowing != null ? dialogShowing.ordinal() : -1);
		}
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
		tabIndicator = (TabPageIndicator) findViewById(R.id.titles);
		tabIndicator.setViewPager(viewPager,
				getIntent().getIntExtra(HikeConstants.Extras.TAB_INDEX, 1));
		tabIndicator.setOnPageChangeListener(onPageChangeListener);

		invalidateOptionsMenu();
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
			parentLayout.setBackgroundColor(getResources().getColor(
					R.color.updates_bg));
		} else {
			parentLayout.setBackgroundColor(getResources().getColor(
					R.color.white));
		}
	}

	/*
	 * Implemented to add a fade change in color when switching between updates
	 * tab and other tabs
	 */
	OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {

		@Override
		public void onPageSelected(int position) {
			invalidateOptionsMenu();
			setBackground();
			/*
			 * Sending a blank query search to ensure all friends are shown.
			 */
			HikeMessengerApp.getPubSub().publish(HikePubSub.FRIENDS_TAB_QUERY,
					"");
			if (position == UPDATES_TAB_INDEX) {
				showUpdateIcon = false;
				tabIndicator.notifyDataSetChanged();
			}
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
	private boolean showUpdateIcon;

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
			if (index == UPDATES_TAB_INDEX && showUpdateIcon) {
				return R.drawable.ic_new_update;
			} else {
				return tabIcons[index];
			}
		}

	}

	@Override
	public void onEventReceived(String type, Object object) {
		super.onEventReceived(type, object);
		if (HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT.equals(type)) {
			showUpdateIcon = true;
			runOnUiThread(refreshTabIcon);
		} else if (HikePubSub.RESET_UNREAD_COUNT.equals(type)) {
			showUpdateIcon = false;
			runOnUiThread(refreshTabIcon);
		} else if (type.equals(HikePubSub.FINISHED_AVTAR_UPGRADE)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progDialog.dismiss();
							showingProgress = false;
							viewPager.setVisibility(View.VISIBLE);
						}
					});
				}
			}).start();
		}

	}

	Runnable refreshTabIcon = new Runnable() {

		@Override
		public void run() {
			tabIndicator.notifyDataSetChanged();
		}
	};
}
