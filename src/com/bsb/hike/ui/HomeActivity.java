package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
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
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.OverFlowMenuItem;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.snowfall.SnowFallView;
import com.bsb.hike.tasks.DownloadAndInstallUpdateAsyncTask;
import com.bsb.hike.ui.fragments.ConversationFragment;
import com.bsb.hike.ui.fragments.FriendsFragment;
import com.bsb.hike.ui.fragments.UpdatesFragment;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.AppRater;
import com.bsb.hike.utils.ChatBgFtue;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.Utils;
import com.viewpagerindicator.IconPagerAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class HomeActivity extends HikeAppStateBaseFragmentActivity implements
		Listener {

	public static final int UPDATES_TAB_INDEX = 0;
	public static final int CHATS_TAB_INDEX = 1;
	public static final int FRIENDS_TAB_INDEX = 2;
	public static List<ContactInfo> ftueList = new ArrayList<ContactInfo>(0);
	private static final boolean TEST = false; // TODO: Test flag only, turn off
												// for Production

	private enum DialogShowing {
		SMS_CLIENT, SMS_SYNC_CONFIRMATION, SMS_SYNCING, UPGRADE_POPUP, FREE_INVITE_POPUP, CHAT_BG_FTUE
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
	private Dialog updateAlert;
	private Button updateAlertOkBtn;
	private static int updateType;
	private boolean showingProgress = false;
	private PopupWindow overFlowWindow;
	private TextView topBarIndicator;
	private Drawable myProfileImage;
	private SnowFallView snowFallView;
	private ContactInfo chatThemeFTUEContact;
	
	private String[] homePubSubListeners = {
			HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT,
			HikePubSub.SMS_SYNC_COMPLETE, HikePubSub.SMS_SYNC_FAIL,
			HikePubSub.FAVORITE_TOGGLED, HikePubSub.USER_JOINED,
			HikePubSub.USER_LEFT, HikePubSub.FRIEND_REQUEST_ACCEPTED,
			HikePubSub.REJECT_FRIEND_REQUEST,
			HikePubSub.UPDATE_OF_MENU_NOTIFICATION, HikePubSub.SERVICE_STARTED,
			HikePubSub.UPDATE_PUSH, HikePubSub.REFRESH_FAVORITES };

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
		
		if(!accountPrefs.getBoolean(
				HikeMessengerApp.SHOWN_CHAT_BG_FTUE, false)){
			Utils.blockOrientationChange(HomeActivity.this);
			//if chat bg ftue is not shown show this on the highest priority
			dialogShowing = DialogShowing.CHAT_BG_FTUE;
			findViewById(R.id.action_bar_img).setVisibility(View.VISIBLE);
			getSupportActionBar().hide();
		} else {
			// check the preferences and show update
			updateType = accountPrefs.getInt(HikeConstants.Extras.UPDATE_AVAILABLE,
					HikeConstants.NO_UPDATE);
			showUpdatePopup(updateType);
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
			case UPGRADE_POPUP:
				showUpdatePopup(updateType);
				break;
			case FREE_INVITE_POPUP:
				showFreeInviteDialog();
			}
		}

		if (!AppRater.showingDialog() && dialogShowing == null) {
			if (!accountPrefs.getBoolean(
					HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, true)) {
				showSMSClientDialog();
			} else if (accountPrefs.getBoolean(
					HikeMessengerApp.SHOW_FREE_INVITE_POPUP, false)) {
				showFreeInviteDialog();
			}
		}

		HikeMessengerApp.getPubSub().addListeners(this, homePubSubListeners);

		GetFTUEContactsTask getFTUEContactsTask = new GetFTUEContactsTask();
		Utils.executeContactInfoListResultTask(getFTUEContactsTask);
		
	}

	public void setChatThemeFTUEContact(ContactInfo contactInfo) {
		this.chatThemeFTUEContact = contactInfo;
	}

	public ContactInfo getChatThemeFTUEContact() {
		return this.chatThemeFTUEContact;
	}

	public void OnChatBgFtueOverlayClick(View v){
		return;
	}
	
	public void onChatBgOpenItUpClick(View v){
		ChatBgFtue.onChatBgOpenItUpClick(HomeActivity.this, v, snowFallView);
	}

	public void onChatBgGiveItASpinClick(View v){
		(new Handler()).postDelayed(new Runnable()
		{
			
			@Override
			public void run()
			{
				/*
				 * This handler is to fix the issue
				 * When user taps on give it spin button there is a
				 * delay in opening chatthread. And There is also a
				 * delay in showing actionbar when we call actionbar
				 * show() method. 
				 */

				findViewById(R.id.action_bar_img).setVisibility(View.GONE);
				getSupportActionBar().show();
			}
		}, 2000);
		
		Editor editor = accountPrefs.edit();
		editor.putBoolean(HikeMessengerApp.SHOWN_CHAT_BG_FTUE, true);
		editor.commit();
		Utils.unblockOrientationChange(HomeActivity.this);
		ChatBgFtue.onChatBgGiveItASpinClick(this, v, snowFallView);
		return;
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

			MenuItem searchItem = menu.add(Menu.NONE, Menu.NONE, 1,
					R.string.search_hint);

			searchItem
					.setIcon(R.drawable.ic_top_bar_search)
					.setActionView(searchView)
					.setShowAsAction(
							MenuItem.SHOW_AS_ACTION_ALWAYS
									| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

			searchItem
					.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

						@Override
						public boolean onMenuItemActionExpand(MenuItem item) {
							return true;
						}

						@Override
						public boolean onMenuItemActionCollapse(MenuItem item) {
							searchView.setQuery("", true);
							return true;
						}
					});
			break;
		default:
			return false;
		}
		topBarIndicator = (TextView) menu.findItem(R.id.overflow_menu)
				.getActionView().findViewById(R.id.top_bar_indicator);
		updateOverFlowMenuNotification();
		menu.findItem(R.id.overflow_menu).getActionView()
				.setOnClickListener(new OnClickListener() {
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

			Utils.sendUILogEvent(HikeConstants.LogEvent.NEW_CHAT_FROM_TOP_BAR);
			break;
		case R.id.new_update:
			intent = new Intent(this, StatusUpdate.class);
			intent.putExtra(HikeConstants.Extras.FROM_CONVERSATIONS_SCREEN,
					true);

			Utils.sendUILogEvent(HikeConstants.LogEvent.POST_UPDATE_FROM_TOP_BAR);
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

	private void showFreeInviteDialog() {
		/*
		 * We don't send free invites for non indian users.
		 */
		if (!HikeMessengerApp.isIndianUser()) {
			return;
		}

		dialogShowing = DialogShowing.FREE_INVITE_POPUP;

		dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.free_invite_popup);
		dialog.setCancelable(false);

		TextView header = (TextView) dialog.findViewById(R.id.header);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		ImageView image = (ImageView) dialog.findViewById(R.id.image);

		String headerText = accountPrefs.getString(
				HikeMessengerApp.FREE_INVITE_POPUP_HEADER, "");
		String bodyText = accountPrefs.getString(
				HikeMessengerApp.FREE_INVITE_POPUP_BODY, "");

		if (TextUtils.isEmpty(headerText)) {
			headerText = getString(R.string.free_invite_header);
		}

		if (TextUtils.isEmpty(bodyText)) {
			bodyText = getString(R.string.free_invite_body);
		}

		header.setText(headerText);
		body.setText(bodyText);

		Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) dialog.findViewById(R.id.btn_cancel);

		final boolean showingRewardsPopup = !accountPrefs.getBoolean(
				HikeMessengerApp.FREE_INVITE_POPUP_DEFAULT_IMAGE, true);

		if (image != null) {
			image.setImageResource(!showingRewardsPopup ? R.drawable.ic_free_sms_default
					: R.drawable.ic_free_sms_rewards);
		}

		okBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();

				Intent intent = new Intent(HomeActivity.this,
						HikeListActivity.class);
				startActivity(intent);

				Utils.sendUILogEvent(showingRewardsPopup ? HikeConstants.LogEvent.INVITE_FRIENDS_FROM_POPUP_REWARDS
						: HikeConstants.LogEvent.INVITE_FRIENDS_FROM_POPUP_FREE_SMS);
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				Editor editor = accountPrefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP,
						false);
				editor.commit();

				dialogShowing = null;
			}
		});

		dialog.show();
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
		if((!accountPrefs.getBoolean(
				HikeMessengerApp.SHOWN_CHAT_BG_FTUE, false))&&snowFallView==null){
			(new Handler()).postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					snowFallView = ChatBgFtue.startAndSetSnowFallView(HomeActivity.this);
				}
			}, 300);
		}
		
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
		JSONObject obj = Utils.getDeviceDetails(HomeActivity.this);
		if (obj != null) {
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, obj);
		}
		Utils.requestAccountInfo(false, true);
		Utils.sendLocaleToServer(HomeActivity.this);
		deviceDetailsSent = true;
	}

	private void updateApp(int updateType) {
		if (TextUtils.isEmpty(this.accountPrefs.getString(
				HikeConstants.Extras.UPDATE_URL, ""))) {
			Intent marketIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id=" + getPackageName()));
			marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
					| Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			try {
				startActivity(marketIntent);
			} catch (ActivityNotFoundException e) {
				Log.e(HomeActivity.class.getSimpleName(),
						"Unable to open market");
			}
			if (updateType == HikeConstants.NORMAL_UPDATE) {
				updateAlert.dismiss();
			}
		} else {
			// In app update!

			updateAlertOkBtn.setText(R.string.downloading_string);
			updateAlertOkBtn.setEnabled(false);

			DownloadAndInstallUpdateAsyncTask downloadAndInstallUpdateAsyncTask = new DownloadAndInstallUpdateAsyncTask(
					this, accountPrefs.getString(
							HikeConstants.Extras.UPDATE_URL, ""));
			downloadAndInstallUpdateAsyncTask.execute();
		}
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

			if (position != CHATS_TAB_INDEX) {
				long firstViewFtueTs = accountPrefs.getLong(
						HikeMessengerApp.FIRST_VIEW_FTUE_LIST_TIMESTAMP, 0);
				if (firstViewFtueTs == 0) {
					Editor editor = accountPrefs.edit();
					editor.putLong(
							HikeMessengerApp.FIRST_VIEW_FTUE_LIST_TIMESTAMP,
							System.currentTimeMillis() / 1000);
					editor.commit();
				}
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

	private void showUpdatePopup(final int updateType) {
		if (updateType == HikeConstants.NO_UPDATE) {
			return;
		}

		if (updateType == HikeConstants.NORMAL_UPDATE) {
			// Here we check if the user cancelled the update popup for this
			// version earlier
			String updateToIgnore = accountPrefs.getString(
					HikeConstants.Extras.UPDATE_TO_IGNORE, "");
			if (!TextUtils.isEmpty(updateToIgnore)
					&& updateToIgnore.equals(accountPrefs.getString(
							HikeConstants.Extras.LATEST_VERSION, ""))) {
				return;
			}
		}

		// If we are already showing an update we don't need to do anything else
		if (updateAlert != null && updateAlert.isShowing()) {
			return;
		}
		dialogShowing = DialogShowing.UPGRADE_POPUP;
		updateAlert = new Dialog(HomeActivity.this, R.style.Theme_CustomDialog);
		updateAlert.setContentView(R.layout.operator_alert_popup);

		updateAlert.findViewById(R.id.body_checkbox).setVisibility(View.GONE);
		TextView updateText = ((TextView) updateAlert
				.findViewById(R.id.body_text));
		TextView updateTitle = (TextView) updateAlert.findViewById(R.id.header);

		updateText.setText(accountPrefs.getString(
				HikeConstants.Extras.UPDATE_MESSAGE, ""));

		updateTitle
				.setText(updateType == HikeConstants.CRITICAL_UPDATE ? R.string.critical_update_head
						: R.string.normal_update_head);

		Button cancelBtn = null;
		updateAlertOkBtn = (Button) updateAlert.findViewById(R.id.btn_ok);
		if (updateType == HikeConstants.CRITICAL_UPDATE) {
			((Button) updateAlert.findViewById(R.id.btn_cancel))
					.setVisibility(View.GONE);

			updateAlertOkBtn.setVisibility(View.VISIBLE);
		} else {
			cancelBtn = (Button) updateAlert.findViewById(R.id.btn_cancel);
			cancelBtn.setText(R.string.cancel);
		}
		updateAlertOkBtn.setText(R.string.update_app);

		updateAlert.setCancelable(true);

		updateAlertOkBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateApp(updateType);
			}
		});

		if (cancelBtn != null) {
			cancelBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					updateAlert.cancel();
					dialogShowing = null;
				}
			});
		}

		updateAlert.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (updateType == HikeConstants.CRITICAL_UPDATE) {
					finish();
				} else {
					Editor editor = accountPrefs.edit();
					editor.putString(HikeConstants.Extras.UPDATE_TO_IGNORE,
							accountPrefs.getString(
									HikeConstants.Extras.LATEST_VERSION, ""));
					editor.commit();
				}
			}
		});

		updateAlert.show();
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
		} else if (HikePubSub.FAVORITE_TOGGLED.equals(type)
				|| HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type)
				|| HikePubSub.REJECT_FRIEND_REQUEST.equals(type)) {
			if (ftueList.isEmpty()) {
				return;
			}
			Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;
			ContactInfo favoriteToggleContact = favoriteToggle.first;

			for (ContactInfo contactInfo : ftueList) {
				if (contactInfo.getMsisdn().equals(
						favoriteToggleContact.getMsisdn())) {
					contactInfo.setFavoriteType(favoriteToggle.second);
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
					break;
				}
			}

		} else if (HikePubSub.USER_JOINED.equals(type)
				|| HikePubSub.USER_LEFT.equals(type)) {
			if (ftueList.isEmpty()) {
				return;
			}
			String msisdn = (String) object;
			for (ContactInfo contactInfo : ftueList) {
				if (contactInfo.getMsisdn().equals(msisdn)) {
					contactInfo.setOnhike(HikePubSub.USER_JOINED.equals(type));
					HikeMessengerApp.getPubSub().publish(
							HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
					break;
				}
			}
		} else if (HikePubSub.UPDATE_OF_MENU_NOTIFICATION.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateOverFlowMenuNotification();
				}
			});
		} else if (HikePubSub.SERVICE_STARTED.equals(type)) {
			boolean justSignedUp = accountPrefs.getBoolean(
					HikeMessengerApp.JUST_SIGNED_UP, false);
			if (justSignedUp) {

				Editor editor = accountPrefs.edit();
				editor.remove(HikeMessengerApp.JUST_SIGNED_UP);
				editor.commit();

				if (!deviceDetailsSent) {
					sendDeviceDetails();
					if (accountPrefs.getBoolean(HikeMessengerApp.FB_SIGNUP,
							false)) {
						Utils.sendUILogEvent(HikeConstants.LogEvent.FB_CLICK);
					}
				}
			}
		} else if (HikePubSub.UPDATE_PUSH.equals(type)) {
			final int updateType = ((Integer) object).intValue();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showUpdatePopup(updateType);
				}
			});
		} else if (HikePubSub.REFRESH_FAVORITES.equals(type)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					GetFTUEContactsTask ftueContactsTask = new GetFTUEContactsTask();
					Utils.executeContactInfoListResultTask(ftueContactsTask);
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

	private class GetFTUEContactsTask extends
			AsyncTask<Void, Void, List<ContactInfo>> {

		@Override
		protected List<ContactInfo> doInBackground(Void... params) {
			List<ContactInfo> contactList = HikeUserDatabase.getInstance()
					.getFTUEContacts(accountPrefs);
			/*
			 * This msisdn type will be the identifier for ftue contacts in the
			 * friends tab.
			 */
			for (ContactInfo contactInfo : contactList) {
				contactInfo.setMsisdnType(HikeConstants.FTUE_MSISDN_TYPE);
			}
			return contactList;
		}

		@Override
		protected void onPostExecute(List<ContactInfo> result) {
			ftueList = result;
			HikeMessengerApp.getPubSub().publish(
					HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(getClass().getSimpleName(), "Key Event is triggered");
		if(dialogShowing == DialogShowing.CHAT_BG_FTUE){
			return super.onKeyUp(keyCode, event);
		}
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

	private void showOverFlowMenu() {

		ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		SharedPreferences appPref = PreferenceManager
				.getDefaultSharedPreferences(this);

		String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING,
				null);
		myProfileImage = IconCacheManager.getInstance().getIconForMSISDN(
				msisdn, true);

		optionsList
				.add(new OverFlowMenuItem(getString(R.string.my_profile), 0));

		if (appPref.getBoolean(HikeConstants.FREE_SMS_PREF, true)) {
			optionsList.add(new OverFlowMenuItem(
					getString(R.string.free_sms_txt), 1));
		}

		optionsList.add(new OverFlowMenuItem(
				getString(R.string.invite_friends), 2));

		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_GAMES, false)) {
			optionsList.add(new OverFlowMenuItem(getString(R.string.games), 3));
		}
		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_REWARDS, false)) {
			optionsList
					.add(new OverFlowMenuItem(getString(R.string.rewards), 4));
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
				if (item.getKey() == 0) {
					itemImageView.setImageDrawable(myProfileImage);
					convertView.findViewById(R.id.profile_image_view)
							.setVisibility(View.VISIBLE);
				} else {
					convertView.findViewById(R.id.profile_image_view)
							.setVisibility(View.GONE);
				}

				int currentCredits = accountPrefs.getInt(
						HikeMessengerApp.SMS_SETTING, 0);
				int totalCredits = Integer.parseInt(accountPrefs.getString(
						HikeMessengerApp.TOTAL_CREDITS_PER_MONTH, "100"));

				TextView freeSmsCount = (TextView) convertView
						.findViewById(R.id.free_sms_count);
				freeSmsCount.setText(currentCredits + "/" + totalCredits);
				if (item.getKey() == 1) {
					freeSmsCount.setVisibility(View.VISIBLE);
				} else {
					freeSmsCount.setVisibility(View.GONE);
				}

				TextView newGamesIndicator = (TextView) convertView
						.findViewById(R.id.new_games_indicator);
				newGamesIndicator.setText("1");
				boolean isGamesClicked = accountPrefs.getBoolean(
						HikeConstants.IS_GAMES_ITEM_CLICKED, false);
				boolean isRewardsClicked = accountPrefs.getBoolean(
						HikeConstants.IS_REWARDS_ITEM_CLICKED, false);
				if ((item.getKey() == 3 && !isGamesClicked)
						|| (item.getKey() == 4 && !isRewardsClicked)) {
					newGamesIndicator.setVisibility(View.VISIBLE);
				} else
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
				OverFlowMenuItem item = (OverFlowMenuItem) adapterView
						.getItemAtPosition(position);
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
					updateOverFlowMenuNotification();
					intent = getGamingIntent();
					break;
				case 4:
					editor.putBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED,
							true);
					editor.commit();
					updateOverFlowMenuNotification();
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
		overFlowWindow.setWidth(getResources().getDimensionPixelSize(
				R.dimen.overflow_menu_width));
		overFlowWindow.setHeight(LayoutParams.WRAP_CONTENT);
		/*
		 * In some devices Activity crashes and a BadTokenException is thrown by
		 * showAsDropDown method. Still need to find out exact repro of the bug.
		 */
		try {
			overFlowWindow.showAsDropDown(findViewById(R.id.overflow_anchor));
		} catch (BadTokenException e) {
			Log.e(getClass().getSimpleName(),
					"Excepetion in HomeActivity Overflow popup", e);
		}
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
			// showOverFlowMenu() method should not be called until all
			// lifecycle
			// methods of activity creation have executed successfully otherwise
			// activity will
			// crash while looking for anchor view of popup menu
			findViewById(R.id.overflow_anchor).post(new Runnable() {
				public void run() {
					showOverFlowMenu();
				}
			});

		super.onRestoreInstanceState(savedInstanceState);
	}

	public void updateOverFlowMenuNotification() {
		if (accountPrefs.getBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, true)
				&& accountPrefs.getBoolean(
						HikeConstants.IS_REWARDS_ITEM_CLICKED, true)) {
			topBarIndicator.setVisibility(View.INVISIBLE);
		} else {
			topBarIndicator.setVisibility(View.VISIBLE);
		}
	}
}
