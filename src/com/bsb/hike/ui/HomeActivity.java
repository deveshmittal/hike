package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

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
import com.bsb.hike.models.utils.IconCacheManager;
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
	private static final boolean TEST = false; // TODO: Test flag only, turn off
												// for Production

	private enum DialogShowing {
		SMS_CLIENT, SMS_SYNC_CONFIRMATION, SMS_SYNCING
	}

	private ViewPager viewPager;
	private DialogShowing dialogShowing;

	private int[] headers = { R.string.updates, R.string.chats,
			R.string.friends };

	private int[] tabIcons = { R.drawable.updates_tab, R.drawable.chats_tab,
			R.drawable.friends_tab };

	private boolean deviceDetailsSent;

	private View parentLayout;
	private Dialog dialog;
	private SharedPreferences accountPrefs;
	private ProgressDialog progDialog;
	private boolean showingProgress = false;
	private PopupWindow overFlowWindow;
	private TextView topBarIndicator;
	private Drawable myProfileImage;
	private String[] homePubSubListeners = {
			HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT,
			HikePubSub.SMS_SYNC_COMPLETE, HikePubSub.SMS_SYNC_FAIL };

	private String[] progressPubSubListeners = { HikePubSub.FINISHED_AVTAR_UPGRADE };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Utils.requireAuth(this)) {
			return;
		}
		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS,
				0);

		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setIcon(R.drawable.hike_logo_top_bar);

		// Checking whether the state of the avatar and conv DB Upgrade settings
		// is 1
		// If it's 1, it means we need to show a progress dialog and then wait
		// for the
		// pub sub thread event to cancel the dialog once the upgrade is done.
		if (((accountPrefs.getInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1) == 1) && (accountPrefs
				.getInt(HikeConstants.UPGRADE_AVATAR_PROGRESS_USER, -1) == 1))
				|| TEST) {
			progDialog = ProgressDialog.show(this,
					getString(R.string.work_in_progress),
					getString(R.string.upgrading_to_a_new_and_improvd_hike),
					true);
			showingProgress = true;
			HikeMessengerApp.getPubSub().addListeners(this,
					progressPubSubListeners);
		}

		if (!showingProgress) {
			initialiseHomeScreen(savedInstanceState);
		}
	}

	private void initialiseHomeScreen(Bundle savedInstanceState) {
		boolean justSignedUp = accountPrefs.getBoolean(
				HikeMessengerApp.JUST_SIGNED_UP, false);

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

		HikeMessengerApp.getPubSub().addListeners(this, homePubSubListeners);

	}

	@Override
	protected void onDestroy() {
		if (progDialog != null) {
			progDialog.dismiss();
			progDialog = null;
		}
		if (overFlowWindow != null && overFlowWindow.isShowing())
			overFlowWindow.dismiss();
		HikeMessengerApp.getPubSub().removeListeners(this, homePubSubListeners);
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
		if (showingProgress) {
			return false;
		} else {
			return setupMenuOptions(menu);
		}

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		return super.onPrepareOptionsMenu(menu);
	}

	private boolean setupMenuOptions(Menu menu) {

		if (viewPager == null) {
			return false;
		}

		switch (viewPager.getCurrentItem()) {
		case UPDATES_TAB_INDEX:
			getSupportMenuInflater().inflate(R.menu.updates_menu, menu);
			break;
		case CHATS_TAB_INDEX:
			getSupportMenuInflater().inflate(R.menu.chats_menu, menu);
			break;
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
			break;
		default:
			return false;
		}
		topBarIndicator = (TextView) menu.findItem(R.id.overflow_menu).getActionView().findViewById(R.id.top_bar_indicator);
		menu.findItem(R.id.overflow_menu).getActionView().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showOverFlowMenu();
			}
		});
		
		return true;
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
		outState.putBoolean(HikeConstants.Extras.IS_HOME_POPUP_SHOWING,
				overFlowWindow != null && overFlowWindow.isShowing());
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

		int position = getIntent().getIntExtra(HikeConstants.Extras.TAB_INDEX,
				1);
		tabIndicator.setViewPager(viewPager, position);
		tabIndicator.setOnPageChangeListener(onPageChangeListener);

		onPageChangeListener.onPageSelected(position);

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

				SharedPreferences prefs = getSharedPreferences(
						HikeMessengerApp.ACCOUNT_SETTINGS, 0);

				if (prefs.getInt(HikeMessengerApp.UNSEEN_STATUS_COUNT, 0) > 0
						|| prefs.getInt(
								HikeMessengerApp.UNSEEN_USER_STATUS_COUNT, 0) > 0) {
					Utils.resetUnseenStatusCount(prefs);
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.RESET_NOTIFICATION_COUNTER, null);
				}
				HikeMessengerApp.getPubSub().publish(
						HikePubSub.CANCEL_ALL_STATUS_NOTIFICATIONS, null);

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
		} else if (type.equals(HikePubSub.FINISHED_AVTAR_UPGRADE)) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							HikeMessengerApp.getPubSub().removeListeners(
									HomeActivity.this, progressPubSubListeners);

							showingProgress = false;
							if (progDialog != null) {
								progDialog.dismiss();
								progDialog = null;
							}
							invalidateOptionsMenu();
							initialiseHomeScreen(null);
						}
					});
				}
			}).start();
		} else if (HikePubSub.SMS_SYNC_COMPLETE.equals(type)
				|| HikePubSub.SMS_SYNC_FAIL.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (dialog != null) {
						dialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		}

	}

	Runnable refreshTabIcon = new Runnable() {

		@Override
		public void run() {
			tabIndicator.notifyDataSetChanged();
		}
	};

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(getClass().getSimpleName(), "Key Event is triggered");
		if (Build.VERSION.SDK_INT <= 10
				|| (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(this)
						.hasPermanentMenuKey())) {
			if (event.getAction() == KeyEvent.ACTION_UP
					&& keyCode == KeyEvent.KEYCODE_MENU) {
				if (overFlowWindow == null || !overFlowWindow.isShowing()) {
					showOverFlowMenu();
				} else {
					overFlowWindow.dismiss();
				}
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private class OverFlowMenuItem{
		private String name;
		private int key;
		OverFlowMenuItem(String name, int key){
			this.name = name;
			this.key = key;
		}
		public String getName() {
			return name;
		}
		public int getKey() {
			return key;
		}
	}
	private void showOverFlowMenu() {

		ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		SharedPreferences appPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		
		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		myProfileImage = IconCacheManager.getInstance()
				.getIconForMSISDN(msisdn, true);

		optionsList.add(new OverFlowMenuItem(getString(R.string.my_profile), 0));

		if (appPref.getBoolean(HikeConstants.FREE_SMS_PREF, true)) {
			optionsList.add(new OverFlowMenuItem(getString(R.string.free_sms_txt), 1));
		}

		optionsList.add(new OverFlowMenuItem(getString(R.string.invite_friends), 2));

		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_GAMES, false)) {
			optionsList.add(new OverFlowMenuItem(getString(R.string.games), 3));
		}
		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_REWARDS, false)) {
			optionsList.add(new OverFlowMenuItem(getString(R.string.rewards), 4));
		}

		optionsList.add(new OverFlowMenuItem(getString(R.string.settings), 5));

		overFlowWindow = new PopupWindow(this);

		LinearLayout homeScreen = (LinearLayout) findViewById(R.id.home_screen);

		View parentView = getLayoutInflater().inflate(R.layout.overflow_menu,
				homeScreen, false);

		overFlowWindow.setContentView(parentView);

		ListView overFlowListView = (ListView) parentView
				.findViewById(R.id.overflow_menu_list);
		overFlowListView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(this,
				R.layout.over_flow_menu_item, R.id.item_title, optionsList) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(
							R.layout.over_flow_menu_item, parent, false);
				}
				
				OverFlowMenuItem item = getItem(position);

				TextView itemTextView = (TextView) convertView
						.findViewById(R.id.item_title);
				itemTextView.setText(item.getName());
				
				ImageView itemImageView = (ImageView) convertView
						.findViewById(R.id.item_icon);
				if(item.getKey() == 0){
					itemImageView.setImageDrawable(myProfileImage);
					convertView.findViewById(R.id.profile_image_view).setVisibility(View.VISIBLE);
				} else{
					convertView.findViewById(R.id.profile_image_view).setVisibility(View.GONE);
				}

				int currentCredits = accountPrefs.getInt(HikeMessengerApp.SMS_SETTING, 0);
				int totalCredits = Integer.parseInt(accountPrefs.getString(
						HikeMessengerApp.TOTAL_CREDITS_PER_MONTH, "100"));
				
				TextView freeSmsCount = (TextView) convertView.findViewById(R.id.free_sms_count);
				freeSmsCount.setText(currentCredits+"/"+totalCredits);
				if(item.getKey() == 1){
					freeSmsCount.setVisibility(View.VISIBLE);
				} else{
					freeSmsCount.setVisibility(View.GONE);
				}
				
				TextView newGamesIndicator = (TextView) convertView.findViewById(R.id.new_games_indicator);
				newGamesIndicator.setText("1");
				boolean isGamesClicked = accountPrefs.getBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, false);
				boolean isRewardsClicked = accountPrefs.getBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, false);
				if((item.getKey()==3 && !isGamesClicked) || (item.getKey()==4&& !isRewardsClicked) ){
					newGamesIndicator.setVisibility(View.VISIBLE);
				}
				else
					newGamesIndicator.setVisibility(View.GONE);
				return convertView;
			}
		});

		overFlowListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				Log.d(getClass().getSimpleName(), "Onclick: " + position);

				overFlowWindow.dismiss();
				OverFlowMenuItem item = (OverFlowMenuItem) adapterView.getItemAtPosition(position);
				Intent intent = null;
				Editor editor = accountPrefs.edit();
				
				switch (item.getKey()) {
				case 0:
					intent = new Intent(HomeActivity.this,
							ProfileActivity.class);
					break;
				case 1:
					intent = new Intent(HomeActivity.this,
							CreditsActivity.class);
					break;
				case 2:
					intent = new Intent(HomeActivity.this, TellAFriend.class);
					break;
				case 3:
					editor.putBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, true);
					editor.commit();
					intent = getGamingIntent();
					break;
				case 4:
					editor.putBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, true);
					editor.commit();
					intent = getRewardsIntent();
					break;
				case 5:
					intent = new Intent(HomeActivity.this,
							SettingsActivity.class);
					break;
				}

				if (intent != null) {
					startActivity(intent);
				}
			}
		});

		overFlowWindow.setBackgroundDrawable(getResources().getDrawable(
				android.R.color.transparent));
		overFlowWindow.setOutsideTouchable(true);
		overFlowWindow.setFocusable(true);
		overFlowWindow.setWidth((int) (Utils.densityMultiplier * 184));
		overFlowWindow.setHeight(LayoutParams.WRAP_CONTENT);
		overFlowWindow.showAsDropDown(findViewById(R.id.overflow_anchor));
		overFlowWindow.getContentView().setFocusableInTouchMode(true);
		overFlowWindow.getContentView().setOnKeyListener(
				new View.OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						return onKeyUp(keyCode, event);
					}
				});
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState
				.getBoolean(HikeConstants.Extras.IS_HOME_POPUP_SHOWING))
			//showOverFlowMenu() method should not be called until all lifecycle 
			//methods of activity creation have executed successfully otherwise activity will
			//crash while looking for anchor view of popup menu
			findViewById(R.id.overflow_anchor).post(new Runnable() {
				public void run() {
					showOverFlowMenu();
				}
			});

		super.onRestoreInstanceState(savedInstanceState);
	}
}
