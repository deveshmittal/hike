package com.bsb.hike.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager.BadTokenException;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.bsb.hike.AppConfig;
import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.FtueContactsData;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.Sticker;
import com.bsb.hike.models.ContactInfo.FavoriteType;
import com.bsb.hike.models.OverFlowMenuItem;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.tasks.DownloadAndInstallUpdateAsyncTask;
import com.bsb.hike.ui.HikeDialog.HikeDialogListener;
import com.bsb.hike.tasks.SendLogsTask;
import com.bsb.hike.ui.fragments.ConversationFragment;
import com.bsb.hike.ui.utils.LockPattern;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.AppRater;
import com.bsb.hike.utils.HikeAppStateBaseFragmentActivity;
import com.bsb.hike.utils.HikeTip;
import com.bsb.hike.utils.StickerManager;
import com.bsb.hike.utils.HikeTip.TipType;
import com.bsb.hike.utils.StickerManager.StickerCategoryId;
import com.bsb.hike.utils.HikeSharedPreferenceUtil;
import com.bsb.hike.utils.IntentManager;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

public class HomeActivity extends HikeAppStateBaseFragmentActivity implements Listener
{

	public static FtueContactsData ftueContactsData = new FtueContactsData();

	private static final boolean TEST = false; // TODO: Test flag only, turn off
												// for Production

	private static final int DIWALI_YEAR = 2014;
	private static final int DIWALI_MONTH = Calendar.OCTOBER;
	private static final int DIWALI_DAY = 23;

	private enum DialogShowing
	{
		SMS_CLIENT, SMS_SYNC_CONFIRMATION, SMS_SYNCING, UPGRADE_POPUP, FREE_INVITE_POPUP, STEALTH_FTUE_POPUP, STEALTH_FTUE_EMPTY_STATE_POPUP, DIWALI_POPUP
	}

	private DialogShowing dialogShowing;

	private boolean deviceDetailsSent;

	private View parentLayout;

	private TextView networkErrorPopUp;

	private Dialog dialog;

	private SharedPreferences accountPrefs;

	private ProgressDialog progDialog;

	private Dialog updateAlert;

	private Button updateAlertOkBtn;

	private static int updateType;

	private boolean showingProgress = false;

	private PopupWindow overFlowWindow;

	private TextView topBarIndicator;
	
	private TextView timelineTopBarIndicator;

	private Drawable myProfileImage;

	private View ftueAddFriendWindow;

	private boolean isAddFriendFtueShowing = false;

	private int hikeContactsCount = -1;

	private int friendsListCount = -1;

	private int recommendedCount = -1;

	private HikeTip.TipType tipTypeShowing;

	private FetchContactsTask fetchContactsTask;
	
	private ConversationFragment mainFragment;
	
	private Handler mHandler = new Handler();

	private String[] homePubSubListeners = { HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT, HikePubSub.SMS_SYNC_COMPLETE, HikePubSub.SMS_SYNC_FAIL, HikePubSub.FAVORITE_TOGGLED,
			HikePubSub.USER_JOINED, HikePubSub.USER_LEFT, HikePubSub.FRIEND_REQUEST_ACCEPTED, HikePubSub.REJECT_FRIEND_REQUEST, HikePubSub.UPDATE_OF_MENU_NOTIFICATION,
			HikePubSub.SERVICE_STARTED, HikePubSub.UPDATE_PUSH, HikePubSub.REFRESH_FAVORITES, HikePubSub.UPDATE_NETWORK_STATE, HikePubSub.CONTACT_SYNCED,
			HikePubSub.SHOW_STEALTH_FTUE_SET_PASS_TIP, HikePubSub.SHOW_STEALTH_FTUE_ENTER_PASS_TIP, HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP, HikePubSub.FAVORITE_COUNT_CHANGED, HikePubSub.STEALTH_UNREAD_TIP_CLICKED };

	private String[] progressPubSubListeners = { HikePubSub.FINISHED_AVTAR_UPGRADE };

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (Utils.requireAuth(this))
		{
			return;
		}
		accountPrefs = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);

		HikeMessengerApp app = (HikeMessengerApp) getApplication();
		app.connectToService();

		setupActionBar();

		// Checking whether the state of the avatar and conv DB Upgrade settings
		// is 1
		// If it's 1, it means we need to show a progress dialog and then wait
		// for the
		// pub sub thread event to cancel the dialog once the upgrade is done.
		if (((accountPrefs.getInt(HikeConstants.UPGRADE_AVATAR_CONV_DB, -1) == 1) && (accountPrefs.getInt(HikeConstants.UPGRADE_AVATAR_PROGRESS_USER, -1) == 1)) || TEST)
		{
			progDialog = ProgressDialog.show(this, getString(R.string.work_in_progress), getString(R.string.upgrading_to_a_new_and_improvd_hike), true);
			showingProgress = true;
			HikeMessengerApp.getPubSub().addListeners(this, progressPubSubListeners);
		}

		if (!showingProgress)
		{
			initialiseHomeScreen(savedInstanceState);
		}
	}

	private void setupActionBar()
	{
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setTitle("");
		actionBar.setLogo(R.drawable.home_screen_top_bar_logo);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
	}

	private void initialiseHomeScreen(Bundle savedInstanceState)
	{

		setContentView(R.layout.home);

		parentLayout = findViewById(R.id.parent_layout);

		networkErrorPopUp = (TextView) findViewById(R.id.network_error);

		if (savedInstanceState != null)
		{
			deviceDetailsSent = savedInstanceState.getBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT);
			int dialogShowingOrdinal = savedInstanceState.getInt(HikeConstants.Extras.DIALOG_SHOWING, -1);
			if (dialogShowingOrdinal != -1)
			{
				dialogShowing = DialogShowing.values()[dialogShowingOrdinal];
			}
		}
		else
		{
			// check the preferences and show update
			updateType = accountPrefs.getInt(HikeConstants.Extras.UPDATE_AVAILABLE, HikeConstants.NO_UPDATE);
			showUpdatePopup(updateType);
		}

		setupMainFragment(savedInstanceState);
		initialiseTabs();

		if (savedInstanceState == null && dialogShowing == null)
		{
			if (HikeMessengerApp.isIndianUser() &&  !accountPrefs.getBoolean(HikeMessengerApp.SHOWN_DIWALI_POPUP, false) && isDiwaliDate())
			{
				showDiwaliPopup();
			}
			else
			{
				/*
				 * Only show app rater if the tutorial is not being shown an the app was just launched i.e not an orientation change
				 */
				AppRater.appLaunched(this);
			}
		}
		else if (dialogShowing != null)
		{
			showAppropriateDialog();
		}

		if (!AppRater.showingDialog() && dialogShowing == null)
		{
			if (!accountPrefs.getBoolean(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, true))
			{
				showSMSClientDialog();
			}
			else if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP, false))
			{
				showFreeInviteDialog();
			}
		}

		HikeMessengerApp.getPubSub().addListeners(this, homePubSubListeners);

		GetFTUEContactsTask getFTUEContactsTask = new GetFTUEContactsTask();
		Utils.executeContactInfoListResultTask(getFTUEContactsTask);

	}

	private void showDiwaliPopup()
	{
		dialogShowing = DialogShowing.DIWALI_POPUP;

		HikeDialogListener dialogListener = new HikeDialogListener()
		{
			@Override
			public void positiveClicked(Dialog dialog)
			{
				sendDiwaliSticker();
				dialog.dismiss();
			}

			@Override
			public void onSucess(Dialog dialog)
			{
			}

			@Override
			public void neutralClicked(Dialog dialog)
			{
			}

			@Override
			public void negativeClicked(Dialog dialog)
			{
				dialog.dismiss();
			}
		};

		dialog = HikeDialog.showDialog(this, HikeDialog.DIWALI_DIALOG, dialogListener, null);

		dialog.setOnDismissListener(new OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				onDismissDiwaliDialog();
			}
		});

		dialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				onDismissDiwaliDialog();
			}
		});

		dialog.show();
	}

	private void sendDiwaliSticker()
	{
		Intent intent = IntentManager.getForwardStickerIntent(this, "078_happydiwali.png", StickerCategoryId.humanoid.name());
		startActivity(intent);
	}

	private void onDismissDiwaliDialog()
	{
		dialogShowing = null;

		HikeSharedPreferenceUtil.getInstance(this).saveData(HikeMessengerApp.SHOWN_DIWALI_POPUP, true);
	}

	private boolean isDiwaliDate()
	{
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);

		if (year == DIWALI_YEAR && month == DIWALI_MONTH && day == DIWALI_DAY)
		{
			return true;
		}

		return false;
	}

	private void setupMainFragment(Bundle savedInstanceState)
	{
		if (savedInstanceState != null) {
            return;
        }
        mainFragment = new ConversationFragment();
        
        getSupportFragmentManager().beginTransaction()
                .add(R.id.home_screen, mainFragment).commit();
		
	}

	private void showStealthFtueTip(final boolean isSetPasswordTip)
	{
		ViewStub stealthTipViewStub = (ViewStub) findViewById(R.id.stealth_double_tap_tip_viewstub);
		if (stealthTipViewStub != null)
		{
			stealthTipViewStub.setOnInflateListener(new ViewStub.OnInflateListener()
			{
				@Override
				public void onInflate(ViewStub stub, View inflated)
				{
					showStealthFtueTip(isSetPasswordTip);
				}
			});
			stealthTipViewStub.inflate();
		}
		else
		{
			tipTypeShowing = isSetPasswordTip ? TipType.STEALTH_FTUE_TIP_2 : TipType.STEALTH_FTUE_ENTER_PASS_TIP;
			HikeTip.showTip(HomeActivity.this, tipTypeShowing, findViewById(R.id.stealth_double_tap_tip));
			Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_animation);
			anim.setDuration(300);
			findViewById(R.id.stealth_double_tap_tip).startAnimation(anim);
		}
	}

	@Override
	protected void onDestroy()
	{
		if (progDialog != null)
		{
			progDialog.dismiss();
			progDialog = null;
		}
		if (overFlowWindow != null && overFlowWindow.isShowing())
			overFlowWindow.dismiss();
		HikeMessengerApp.getPubSub().removeListeners(this, homePubSubListeners);
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		if (Utils.requireAuth(this))
		{
			return;
		}
		if (mainFragment != null)
		{
			mainFragment.onNewintent(intent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (showingProgress)
		{
			return false;
		}
		else
		{
			return setupMenuOptions(menu);
		}

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{

		return super.onPrepareOptionsMenu(menu);
	}

	private boolean setupMenuOptions(Menu menu)
	{
		getSupportMenuInflater().inflate(R.menu.chats_menu, menu);

		topBarIndicator = (TextView) menu.findItem(R.id.overflow_menu).getActionView().findViewById(R.id.top_bar_indicator);
		updateOverFlowMenuNotification();
		menu.findItem(R.id.overflow_menu).getActionView().setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showOverFlowMenu();
			}
		});
		
		timelineTopBarIndicator = (TextView) menu.findItem(R.id.show_timeline).getActionView().findViewById(R.id.top_bar_indicator);
		menu.findItem(R.id.show_timeline).getActionView().findViewById(R.id.overflow_icon_image).setContentDescription("Timeline");
		((ImageView)menu.findItem(R.id.show_timeline).getActionView().findViewById(R.id.overflow_icon_image)).setImageResource(R.drawable.ic_show_timeline);
		updateTimelineNotificationCount(Utils.getNotificationCount(accountPrefs, false), 1000);
		menu.findItem(R.id.show_timeline).getActionView().setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Utils.sendUILogEvent(HikeConstants.LogEvent.SHOW_TIMELINE_TOP_BAR);
				Intent intent = new Intent(HomeActivity.this, TimelineActivity.class);
				startActivity(intent);
			}
		});

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent = null;

		switch (item.getItemId())
		{
		case R.id.new_conversation:
			intent = new Intent(this, ComposeChatActivity.class);
			intent.putExtra(HikeConstants.Extras.EDIT, true);
			Utils.sendUILogEvent(HikeConstants.LogEvent.NEW_CHAT_FROM_TOP_BAR);
			break;
		case android.R.id.home:
			hikeLogoClicked();
			break;
		}

		if (intent != null)
		{
			startActivity(intent);
			return true;
		}
		else
		{
			return super.onOptionsItemSelected(item);
		}
	}


	

	private void showSMSClientDialog()
	{
		dialogShowing = DialogShowing.SMS_CLIENT;

		dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.sms_with_hike_popup);
		dialog.setCancelable(false);

		Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) dialog.findViewById(R.id.btn_cancel);

		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Utils.setReceiveSmsSetting(getApplicationContext(), true);

				Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
				editor.putBoolean(HikeConstants.SEND_SMS_PREF, true);
				editor.commit();

				dialogShowing = null;
				dialog.dismiss();
				if (!accountPrefs.getBoolean(HikeMessengerApp.SHOWN_SMS_SYNC_POPUP, false))
				{
					showSMSSyncDialog();
				}
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Utils.setReceiveSmsSetting(getApplicationContext(), false);
				dialogShowing = null;
				dialog.dismiss();
			}
		});

		dialog.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss(DialogInterface dialog)
			{
				Editor editor = accountPrefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOWN_SMS_CLIENT_POPUP, true);
				editor.commit();
			}
		});
		dialog.show();
	}

	private void showSMSSyncDialog()
	{
		if (dialogShowing == null)
		{
			dialogShowing = DialogShowing.SMS_SYNC_CONFIRMATION;
		}

		dialog = Utils.showSMSSyncDialog(this, dialogShowing == DialogShowing.SMS_SYNC_CONFIRMATION);
	}

	private void showFreeInviteDialog()
	{
		/*
		 * We don't send free invites for non indian users.
		 */
		if (!HikeMessengerApp.isIndianUser())
		{
			return;
		}

		dialogShowing = DialogShowing.FREE_INVITE_POPUP;

		dialog = new Dialog(this, R.style.Theme_CustomDialog);
		dialog.setContentView(R.layout.free_invite_popup);
		dialog.setCancelable(false);

		TextView header = (TextView) dialog.findViewById(R.id.header);
		TextView body = (TextView) dialog.findViewById(R.id.body);
		ImageView image = (ImageView) dialog.findViewById(R.id.image);

		String headerText = accountPrefs.getString(HikeMessengerApp.FREE_INVITE_POPUP_HEADER, "");
		String bodyText = accountPrefs.getString(HikeMessengerApp.FREE_INVITE_POPUP_BODY, "");

		if (TextUtils.isEmpty(headerText))
		{
			headerText = getString(R.string.free_invite_header);
		}

		if (TextUtils.isEmpty(bodyText))
		{
			bodyText = getString(R.string.free_invite_body);
		}

		header.setText(headerText);
		body.setText(bodyText);

		Button okBtn = (Button) dialog.findViewById(R.id.btn_ok);
		Button cancelBtn = (Button) dialog.findViewById(R.id.btn_cancel);

		final boolean showingRewardsPopup = !accountPrefs.getBoolean(HikeMessengerApp.FREE_INVITE_POPUP_DEFAULT_IMAGE, true);

		if (image != null)
		{
			image.setImageResource(!showingRewardsPopup ? R.drawable.ic_free_sms_default : R.drawable.ic_free_sms_rewards);
		}

		okBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();

				Intent intent = new Intent(HomeActivity.this, HikeListActivity.class);
				startActivity(intent);

				Utils.sendUILogEvent(showingRewardsPopup ? HikeConstants.LogEvent.INVITE_FRIENDS_FROM_POPUP_REWARDS : HikeConstants.LogEvent.INVITE_FRIENDS_FROM_POPUP_FREE_SMS);
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				dialog.dismiss();
			}
		});

		dialog.setOnDismissListener(new OnDismissListener()
		{

			@Override
			public void onDismiss(DialogInterface dialog)
			{
				Editor editor = accountPrefs.edit();
				editor.putBoolean(HikeMessengerApp.SHOW_FREE_INVITE_POPUP, false);
				editor.commit();

				dialogShowing = null;
			}
		});

		dialog.show();
	}

	private class FetchContactsTask extends AsyncTask<Void, Void, Void>
	{
		List<ContactInfo> hikeContacts = new ArrayList<ContactInfo>();

		List<ContactInfo> friendsList = new ArrayList<ContactInfo>();

		List<ContactInfo> recommendedContacts = new ArrayList<ContactInfo>();

		@Override
		protected Void doInBackground(Void... arg0)
		{
			Utils.getRecommendedAndHikeContacts(HomeActivity.this, recommendedContacts, hikeContacts, friendsList);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			hikeContactsCount = hikeContacts.size();
			recommendedCount = recommendedContacts.size();
			friendsListCount = friendsList.size();
			super.onPostExecute(result);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		checkNShowNetworkError();
		HikeMessengerApp.getPubSub().publish(HikePubSub.CANCEL_ALL_NOTIFICATIONS, null);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		HikeMessengerApp.getPubSub().addListener(HikePubSub.SHOW_IMAGE, this);
		long t1, t2;
		t1 = System.currentTimeMillis();
		Utils.clearJar(this);
		t2 = System.currentTimeMillis();
		Logger.d("clearJar", "time : " + (t2 - t1));
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		HikeMessengerApp.getPubSub().removeListener(HikePubSub.SHOW_IMAGE, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(HikeConstants.Extras.DEVICE_DETAILS_SENT, deviceDetailsSent);
		if (dialog != null && dialog.isShowing())
		{
			outState.putInt(HikeConstants.Extras.DIALOG_SHOWING, dialogShowing != null ? dialogShowing.ordinal() : -1);
		}
		outState.putBoolean(HikeConstants.Extras.IS_HOME_POPUP_SHOWING, overFlowWindow != null && overFlowWindow.isShowing());
		outState.putInt(HikeConstants.Extras.FRIENDS_LIST_COUNT, friendsListCount);
		outState.putInt(HikeConstants.Extras.HIKE_CONTACTS_COUNT, hikeContactsCount);
		outState.putInt(HikeConstants.Extras.RECOMMENDED_CONTACTS_COUNT, recommendedCount);
		super.onSaveInstanceState(outState);
	}

	private void sendDeviceDetails()
	{
		JSONObject obj = Utils.getDeviceDetails(HomeActivity.this);
		if (obj != null)
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.MQTT_PUBLISH, obj);
		}
		Utils.requestAccountInfo(false, true);
		Utils.sendLocaleToServer(HomeActivity.this);
		deviceDetailsSent = true;
	}

	private void updateApp(int updateType)
	{
		if (TextUtils.isEmpty(this.accountPrefs.getString(HikeConstants.Extras.UPDATE_URL, "")))
		{
			Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
			marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			try
			{
				startActivity(marketIntent);
			}
			catch (ActivityNotFoundException e)
			{
				Logger.e(HomeActivity.class.getSimpleName(), "Unable to open market");
			}
			if (updateType == HikeConstants.NORMAL_UPDATE)
			{
				updateAlert.dismiss();
			}
		}
		else
		{
			// In app update!

			updateAlertOkBtn.setText(R.string.downloading_string);
			updateAlertOkBtn.setEnabled(false);

			DownloadAndInstallUpdateAsyncTask downloadAndInstallUpdateAsyncTask = new DownloadAndInstallUpdateAsyncTask(this, accountPrefs.getString(
					HikeConstants.Extras.UPDATE_URL, ""));
			downloadAndInstallUpdateAsyncTask.execute();
		}
	}

	@SuppressLint("NewApi")
	private void initialiseTabs()
	{
		invalidateOptionsMenu();
	}

	private boolean showUpdateIcon;

	private void showUpdatePopup(final int updateType)
	{
		if (updateType == HikeConstants.NO_UPDATE)
		{
			return;
		}

		if (updateType == HikeConstants.NORMAL_UPDATE)
		{
			// Here we check if the user cancelled the update popup for this
			// version earlier
			String updateToIgnore = accountPrefs.getString(HikeConstants.Extras.UPDATE_TO_IGNORE, "");
			if (!TextUtils.isEmpty(updateToIgnore) && updateToIgnore.equals(accountPrefs.getString(HikeConstants.Extras.LATEST_VERSION, "")))
			{
				return;
			}
		}

		// If we are already showing an update we don't need to do anything else
		if (updateAlert != null && updateAlert.isShowing())
		{
			return;
		}
		dialogShowing = DialogShowing.UPGRADE_POPUP;
		updateAlert = new Dialog(HomeActivity.this, R.style.Theme_CustomDialog);
		updateAlert.setContentView(R.layout.operator_alert_popup);

		updateAlert.findViewById(R.id.body_checkbox).setVisibility(View.GONE);
		TextView updateText = ((TextView) updateAlert.findViewById(R.id.body_text));
		TextView updateTitle = (TextView) updateAlert.findViewById(R.id.header);

		updateText.setText(accountPrefs.getString(HikeConstants.Extras.UPDATE_MESSAGE, ""));

		updateTitle.setText(updateType == HikeConstants.CRITICAL_UPDATE ? R.string.critical_update_head : R.string.normal_update_head);

		Button cancelBtn = null;
		updateAlertOkBtn = (Button) updateAlert.findViewById(R.id.btn_ok);
		if (updateType == HikeConstants.CRITICAL_UPDATE)
		{
			((Button) updateAlert.findViewById(R.id.btn_cancel)).setVisibility(View.GONE);

			updateAlertOkBtn.setVisibility(View.VISIBLE);
		}
		else
		{
			cancelBtn = (Button) updateAlert.findViewById(R.id.btn_cancel);
			cancelBtn.setText(R.string.cancel);
		}
		updateAlertOkBtn.setText(R.string.update_app);

		updateAlert.setCancelable(true);

		updateAlertOkBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				updateApp(updateType);
			}
		});

		if (cancelBtn != null)
		{
			cancelBtn.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					updateAlert.cancel();
					dialogShowing = null;
				}
			});
		}

		updateAlert.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				if (updateType == HikeConstants.CRITICAL_UPDATE)
				{
					finish();
				}
				else
				{
					Editor editor = accountPrefs.edit();
					editor.putString(HikeConstants.Extras.UPDATE_TO_IGNORE, accountPrefs.getString(HikeConstants.Extras.LATEST_VERSION, ""));
					editor.commit();
				}
			}
		});

		updateAlert.show();
	}

	@Override
	public void onEventReceived(String type, Object object)
	{
		super.onEventReceived(type, object);
		if (HikePubSub.INCREMENTED_UNSEEN_STATUS_COUNT.equals(type))
		{
			runOnUiThread( new Runnable()
			{

				@Override
				public void run()
				{
					updateTimelineNotificationCount(Utils.getNotificationCount(accountPrefs, false), 0);
				}
			});
		}
		else if (type.equals(HikePubSub.FINISHED_AVTAR_UPGRADE))
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					runOnUiThread(new Runnable()
					{
						@SuppressLint("NewApi")
						@Override
						public void run()
						{
							HikeMessengerApp.getPubSub().removeListeners(HomeActivity.this, progressPubSubListeners);

							showingProgress = false;
							if (progDialog != null)
							{
								progDialog.dismiss();
								progDialog = null;
							}
							invalidateOptionsMenu();
							initialiseHomeScreen(null);
						}
					});
				}
			}).start();
		}
		else if (HikePubSub.SMS_SYNC_COMPLETE.equals(type) || HikePubSub.SMS_SYNC_FAIL.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					if (dialog != null)
					{
						dialog.dismiss();
					}
					dialogShowing = null;
				}
			});
		}
		else if (HikePubSub.FAVORITE_TOGGLED.equals(type) || HikePubSub.FRIEND_REQUEST_ACCEPTED.equals(type) || HikePubSub.REJECT_FRIEND_REQUEST.equals(type))
		{
			Pair<ContactInfo, FavoriteType> favoriteToggle = (Pair<ContactInfo, FavoriteType>) object;

			if (ftueContactsData.isEmpty())
			{
				return;
			}
			ContactInfo favoriteToggleContact = favoriteToggle.first;

			for (ContactInfo contactInfo : ftueContactsData.getCompleteList())
			{
				if (contactInfo.getMsisdn().equals(favoriteToggleContact.getMsisdn()))
				{
					contactInfo.setFavoriteType(favoriteToggle.second);
					HikeMessengerApp.getPubSub().publish(HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
					break;
				}
			}
		}
		else if (HikePubSub.USER_JOINED.equals(type) || HikePubSub.USER_LEFT.equals(type))
		{
			if (ftueContactsData.isEmpty())
			{
				return;
			}
			String msisdn = (String) object;
			for (ContactInfo contactInfo : ftueContactsData.getCompleteList())
			{
				if (contactInfo.getMsisdn().equals(msisdn))
				{
					contactInfo.setOnhike(HikePubSub.USER_JOINED.equals(type));
					HikeMessengerApp.getPubSub().publish(HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
					break;
				}
			}
		}
		else if (HikePubSub.UPDATE_OF_MENU_NOTIFICATION.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateOverFlowMenuNotification();
				}
			});
		}
		else if (HikePubSub.SERVICE_STARTED.equals(type))
		{
			boolean justSignedUp = accountPrefs.getBoolean(HikeMessengerApp.JUST_SIGNED_UP, false);
			if (justSignedUp)
			{

				Editor editor = accountPrefs.edit();
				editor.remove(HikeMessengerApp.JUST_SIGNED_UP);
				editor.commit();

				if (!deviceDetailsSent)
				{
					sendDeviceDetails();
					if (accountPrefs.getBoolean(HikeMessengerApp.FB_SIGNUP, false))
					{
						Utils.sendUILogEvent(HikeConstants.LogEvent.FB_CLICK);
					}
					if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) > -1)
					{
						if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) == HikeConstants.WelcomeTutorial.STICKER_VIEWED.ordinal())
						{
							Utils.sendUILogEvent(HikeConstants.LogEvent.FTUE_TUTORIAL_STICKER_VIEWED);
						}
						else if (accountPrefs.getInt(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED, -1) == HikeConstants.WelcomeTutorial.CHAT_BG_VIEWED.ordinal())
						{
							Utils.sendUILogEvent(HikeConstants.LogEvent.FTUE_TUTORIAL_CBG_VIEWED);
						}
						editor = accountPrefs.edit();
						editor.remove(HikeMessengerApp.WELCOME_TUTORIAL_VIEWED);
						editor.commit();
					}

				}
			}
		}
		else if (HikePubSub.UPDATE_PUSH.equals(type))
		{
			final int updateType = ((Integer) object).intValue();
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showUpdatePopup(updateType);
				}
			});
		}
		else if (HikePubSub.REFRESH_FAVORITES.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					GetFTUEContactsTask ftueContactsTask = new GetFTUEContactsTask();
					Utils.executeContactInfoListResultTask(ftueContactsTask);
				}
			});
		}
		else if (HikePubSub.UPDATE_NETWORK_STATE.equals(type))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					animateNShowNetworkError();
				}
			});
		}
		else if (HikePubSub.CONTACT_SYNCED.equals(type))
		{
			Boolean[] ret = (Boolean[]) object;
			final boolean manualSync = ret[0];
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if(manualSync)
						Toast.makeText(getApplicationContext(), R.string.contacts_synced, Toast.LENGTH_SHORT).show();
				}
			});
		}
		else if (HikePubSub.SHOW_STEALTH_FTUE_SET_PASS_TIP.equals(type))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showStealthFtueTip(true);
				}
			});
		}
		else if (HikePubSub.SHOW_STEALTH_FTUE_ENTER_PASS_TIP.equals(type))
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showStealthFtueTip(false);
				}
			});
		}
		else if (HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP.equals(type))
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
				}
			});
		}
		else if (HikePubSub.STEALTH_UNREAD_TIP_CLICKED.equals(type))
		{
			runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					hikeLogoClicked();
				}
			});
		}
		else if(HikePubSub.FAVORITE_COUNT_CHANGED.equals(type))
		{
			runOnUiThread( new Runnable()
			{
				@Override
				public void run()
				{
					updateTimelineNotificationCount(Utils.getNotificationCount(accountPrefs, false), 0);
				}
			});
		}
	}

	private void updateHomeOverflowToggleCount(final int count, int delayTime)
	{

		if (count < 1)
		{
			topBarIndicator.setVisibility(View.GONE);
		}
		else
		{
			mHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					if (topBarIndicator != null)
					{
						/*
						 * Fetching the count again since it could have changed after the delay. 
						 */
						int newCount = Utils.updateHomeOverflowToggleCount(accountPrefs);
						if (newCount < 1)
						{
							topBarIndicator.setVisibility(View.GONE);
						}
						else if (newCount > 9)
						{
							topBarIndicator.setVisibility(View.VISIBLE);
							topBarIndicator.setText("9+");
							topBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
						else if (newCount > 0)
						{
							topBarIndicator.setVisibility(View.VISIBLE);
							topBarIndicator.setText(String.valueOf(count));
							topBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
					}
				}
			}, delayTime);
		}

	}

	private class GetFTUEContactsTask extends AsyncTask<Void, Void, FtueContactsData>
	{

		@Override
		protected FtueContactsData doInBackground(Void... params)
		{
			FtueContactsData ftueContactsDataResult = ContactManager.getInstance().getFTUEContacts(accountPrefs);
			/*
			 * This msisdn type will be the identifier for ftue contacts in the friends tab.
			 */
			ftueContactsDataResult.setCompleteList();
			for (ContactInfo contactInfo : ftueContactsDataResult.getCompleteList())
			{
				contactInfo.setMsisdnType(HikeConstants.FTUE_MSISDN_TYPE);
			}

			return ftueContactsDataResult;
		}

		@Override
		protected void onPostExecute(FtueContactsData result)
		{
			ftueContactsData = result;
			Logger.d("GetFTUEContactsTask","ftueContactsData = "+ ftueContactsData.toString());
			HikeMessengerApp.getPubSub().publish(HikePubSub.FTUE_LIST_FETCHED_OR_UPDATED, null);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 && ViewConfiguration.get(this).hasPermanentMenuKey()))
		{
			if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU)
			{
				if (ftueAddFriendWindow != null && ftueAddFriendWindow.getVisibility() == View.VISIBLE)
				{
					return true;
				}
				if (overFlowWindow == null || !overFlowWindow.isShowing())
				{
					showOverFlowMenu();
				}
				else
				{
					overFlowWindow.dismiss();
				}
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	private void checkNShowNetworkError()
	{
		if (networkErrorPopUp == null)
			return;
		Logger.d(getClass().getSimpleName(), "visiblity for: " + HikeMessengerApp.networkError);
		// networkErrorPopUp.clearAnimation();
		if (HikeMessengerApp.networkError)
		{
			networkErrorPopUp.setText(R.string.no_internet_connection);
			networkErrorPopUp.setBackgroundColor(getResources().getColor(R.color.red_no_network));
			networkErrorPopUp.setVisibility(View.VISIBLE);
		}
		else
		{
			networkErrorPopUp.setVisibility(View.GONE);
		}
	}

	private void animateNShowNetworkError()
	{
		if (networkErrorPopUp == null)
			return;
		Logger.d(getClass().getSimpleName(), "animation for: " + HikeMessengerApp.networkError);
		if (HikeMessengerApp.networkError)
		{
			Animation alphaIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up_noalpha);
			alphaIn.setDuration(400);
			networkErrorPopUp.setText(R.string.no_internet_connection);
			networkErrorPopUp.setBackgroundColor(getResources().getColor(R.color.red_no_network));
			networkErrorPopUp.setAnimation(alphaIn);
			networkErrorPopUp.setVisibility(View.VISIBLE);
			alphaIn.start();
		}
		else if (networkErrorPopUp.getVisibility() == View.VISIBLE)
		{
			Animation alphaIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down_noalpha);
			alphaIn.setStartOffset(1000);
			alphaIn.setDuration(400);
			networkErrorPopUp.setText(R.string.connected);
			networkErrorPopUp.setBackgroundColor(getResources().getColor(R.color.green_connected));
			networkErrorPopUp.setVisibility(View.GONE);
			networkErrorPopUp.setAnimation(alphaIn);
			alphaIn.start();
		}
		checkNShowNetworkError();
	}

	public void showOverFlowMenu()
	{

		ArrayList<OverFlowMenuItem> optionsList = new ArrayList<OverFlowMenuItem>();

		final String msisdn = accountPrefs.getString(HikeMessengerApp.MSISDN_SETTING, null);
		myProfileImage = HikeMessengerApp.getLruCache().getIconFromCache(msisdn, true);
		// myProfileImage = IconCacheManager.getInstance().getIconForMSISDN(
		// msisdn, true);

		/*
		 * removing out new chat option for now
		 */
		optionsList.add(new OverFlowMenuItem(getString(R.string.new_group), 6));
		
		optionsList.add(new OverFlowMenuItem(getString(R.string.invite_friends), 2));

		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_GAMES, false))
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.hike_extras), 3));
		}
		if (accountPrefs.getBoolean(HikeMessengerApp.SHOW_REWARDS, false))
		{
			optionsList.add(new OverFlowMenuItem(getString(R.string.rewards), 4));
		}

		optionsList.add(new OverFlowMenuItem(getString(R.string.settings), 5));
		
		optionsList.add(new OverFlowMenuItem(getString(R.string.status), 8));

		addEmailLogItem(optionsList);

		overFlowWindow = new PopupWindow(this);

		FrameLayout homeScreen = (FrameLayout) findViewById(R.id.home_screen);

		View parentView = getLayoutInflater().inflate(R.layout.overflow_menu, homeScreen, false);

		overFlowWindow.setContentView(parentView);

		ListView overFlowListView = (ListView) parentView.findViewById(R.id.overflow_menu_list);
		overFlowListView.setAdapter(new ArrayAdapter<OverFlowMenuItem>(this, R.layout.over_flow_menu_item, R.id.item_title, optionsList)
		{

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = getLayoutInflater().inflate(R.layout.over_flow_menu_item, parent, false);
				}

				OverFlowMenuItem item = getItem(position);

				TextView itemTextView = (TextView) convertView.findViewById(R.id.item_title);
				itemTextView.setText(item.getName());

				ImageView itemImageView = (ImageView) convertView.findViewById(R.id.item_icon);
				if (item.getKey() == 0)
				{
					if (myProfileImage != null)
					{
						itemImageView.setImageDrawable(myProfileImage);
					}
					else
					{
						itemImageView.setScaleType(ScaleType.CENTER_INSIDE);
						itemImageView.setBackgroundResource(BitmapUtils.getDefaultAvatarResourceId(msisdn, true));
						itemImageView.setImageResource(R.drawable.ic_default_avatar);
					}
					convertView.findViewById(R.id.profile_image_view).setVisibility(View.VISIBLE);
				}
				else
				{
					
					convertView.findViewById(R.id.profile_image_view).setVisibility(View.GONE);
				}

				int currentCredits = accountPrefs.getInt(HikeMessengerApp.SMS_SETTING, 0);

				TextView freeSmsCount = (TextView) convertView.findViewById(R.id.free_sms_count);
				freeSmsCount.setText(Integer.toString(currentCredits));
				if (item.getKey() == 1)
				{
					freeSmsCount.setVisibility(View.VISIBLE);
				}
				else
				{
					freeSmsCount.setVisibility(View.GONE);
				}

				TextView newGamesIndicator = (TextView) convertView.findViewById(R.id.new_games_indicator);
				newGamesIndicator.setText("1");

				/*
				 * Rewards & Games indicator bubble are by default shown even if the keys are not stored in shared pref. 
				 */
				boolean isGamesClicked = accountPrefs.getBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, false);
				boolean isRewardsClicked = accountPrefs.getBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, false);
				if ((item.getKey() == 3 && !isGamesClicked) || (item.getKey() == 4 && !isRewardsClicked))
				{
					newGamesIndicator.setVisibility(View.VISIBLE);
				}
				else
					newGamesIndicator.setVisibility(View.GONE);
				
				
				return convertView;
			}
		});

		overFlowListView.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
			{
				Logger.d(getClass().getSimpleName(), "Onclick: " + position);

				overFlowWindow.dismiss();
				OverFlowMenuItem item = (OverFlowMenuItem) adapterView.getItemAtPosition(position);
				Intent intent = null;
				Editor editor = accountPrefs.edit();

				switch (item.getKey())
				{
				case 1:
					intent = new Intent(HomeActivity.this, CreditsActivity.class);
					break;
				case 2:
					intent = new Intent(HomeActivity.this, TellAFriend.class);
					break;
				case 3:
					editor.putBoolean(HikeConstants.IS_GAMES_ITEM_CLICKED, true);
					editor.commit();
					updateOverFlowMenuNotification();
					intent = IntentManager.getGamingIntent(HomeActivity.this);
					break;
				case 4:
					editor.putBoolean(HikeConstants.IS_REWARDS_ITEM_CLICKED, true);
					editor.commit();
					updateOverFlowMenuNotification();
					intent = IntentManager.getRewardsIntent(HomeActivity.this);
					break;
				case 5:
					intent = new Intent(HomeActivity.this, SettingsActivity.class);
					break;
				case 6:
					intent = new Intent(HomeActivity.this, CreateNewGroupActivity.class);
					break;
				case 8:
					Utils.sendUILogEvent(HikeConstants.LogEvent.STATUS_UPDATE_FROM_OVERFLOW);
					intent = new Intent(HomeActivity.this, StatusUpdate.class);
					break;
				case 9:
					SendLogsTask logsTask = new SendLogsTask(HomeActivity.this);
					Utils.executeAsyncTask(logsTask);
					break;	
				}

				if (intent != null)
				{
					startActivity(intent);
				}
			}
		});

		overFlowWindow.setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));
		overFlowWindow.setOutsideTouchable(true);
		overFlowWindow.setFocusable(true);
		overFlowWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.overflow_menu_width));
		overFlowWindow.setHeight(LayoutParams.WRAP_CONTENT);
		/*
		 * In some devices Activity crashes and a BadTokenException is thrown by showAsDropDown method. Still need to find out exact repro of the bug.
		 */
		try
		{
			int rightMargin = getResources().getDimensionPixelSize(R.dimen.overflow_menu_right_margin);
			overFlowWindow.showAsDropDown(findViewById(R.id.overflow_anchor), -rightMargin, 0);
		}
		catch (BadTokenException e)
		{
			Logger.e(getClass().getSimpleName(), "Excepetion in HomeActivity Overflow popup", e);
		}
		overFlowWindow.getContentView().setFocusableInTouchMode(true);
		overFlowWindow.getContentView().setOnKeyListener(new View.OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				return onKeyUp(keyCode, event);
			}
		});
	}

	private void addEmailLogItem(List<OverFlowMenuItem> overFlowMenuItems)
	{
		if (AppConfig.SHOW_SEND_LOGS_OPTION)
		{
			overFlowMenuItems.add(new OverFlowMenuItem("Send logs", 9));
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		if (savedInstanceState.getBoolean(HikeConstants.Extras.IS_HOME_POPUP_SHOWING))
			// showOverFlowMenu() method should not be called until all
			// lifecycle
			// methods of activity creation have executed successfully otherwise
			// activity will
			// crash while looking for anchor view of popup menu
			findViewById(R.id.overflow_anchor).post(new Runnable()
			{
				public void run()
				{
					showOverFlowMenu();
				}
			});
		super.onRestoreInstanceState(savedInstanceState);
	}

	public void updateOverFlowMenuNotification()
	{
		final int count = Utils.updateHomeOverflowToggleCount(accountPrefs);
		if (topBarIndicator != null)
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					updateHomeOverflowToggleCount(count, 1000);
				}
			});

		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		return fetchContactsTask;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		// handle dialogs here
		if (dialogShowing != null)
		{
			showAppropriateDialog();
		}
		if (overFlowWindow != null && overFlowWindow.isShowing())
		{
			overFlowWindow.dismiss();
			showOverFlowMenu();
		}
	}

	private void showAppropriateDialog()
	{
		if (dialog != null)
		{
			if (dialog.isShowing())
			{
				dialog.dismiss();
			}
			else
			{
				return;
			}
		}
		switch (dialogShowing)
		{
		case DIWALI_POPUP:
			showDiwaliPopup();
			break;
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
			break;
		case STEALTH_FTUE_POPUP:
			dialogShowing = DialogShowing.STEALTH_FTUE_POPUP;
			dialog = HikeDialog.showDialog(this, HikeDialog.STEALTH_FTUE_DIALOG, getHomeActivityDialogListener());
			break;
		case STEALTH_FTUE_EMPTY_STATE_POPUP:
			dialogShowing = DialogShowing.STEALTH_FTUE_EMPTY_STATE_POPUP;
			dialog = HikeDialog.showDialog(this, HikeDialog.STEALTH_FTUE_EMPTY_STATE_DIALOG, getHomeActivityDialogListener());
			break;
		}
	}

	private HikeDialogListener getHomeActivityDialogListener()
	{
		return new HikeDialog.HikeDialogListener()
		{

			@Override
			public void positiveClicked(Dialog dialog)
			{

			}

			@Override
			public void neutralClicked(Dialog dialog)
			{
				switch (dialogShowing)
				{
				case STEALTH_FTUE_POPUP:
					HikeMessengerApp.getPubSub().publish(HikePubSub.SHOW_STEALTH_FTUE_CONV_TIP, null);
					Utils.sendUILogEvent(HikeConstants.LogEvent.QUICK_SETUP_CLICK);
					break;

				default:
					break;
				}

				dialogShowing = null;
				dialog.dismiss();
				HomeActivity.this.dialog = null;
			}

			@Override
			public void negativeClicked(Dialog dialog)
			{
			}

			@Override
			public void onSucess(Dialog dialog)
			{
				// TODO Auto-generated method stub
				
			}
		};
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		LockPattern.onLockActivityResult(this, requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}
	

	public void updateTimelineNotificationCount(int count, int delayTime)
	{
		if (count < 1)
		{
			timelineTopBarIndicator.setVisibility(View.GONE);
		}
		else
		{
			mHandler.postDelayed(new Runnable()
			{

				@Override
				public void run()
				{
					if (timelineTopBarIndicator != null)
					{
						int count = Utils.getNotificationCount(accountPrefs, false);
						if (count > 9)
						{
							timelineTopBarIndicator.setVisibility(View.VISIBLE);
							timelineTopBarIndicator.setText("9+");
							timelineTopBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
						else if (count > 0)
						{
							timelineTopBarIndicator.setVisibility(View.VISIBLE);
							timelineTopBarIndicator.setText(String.valueOf(count));
							timelineTopBarIndicator.startAnimation(Utils.getNotificationIndicatorAnim());
						}
					}
				}
			}, delayTime);
		}
	}

	private void hikeLogoClicked()
	{
		if (HikeSharedPreferenceUtil.getInstance(HomeActivity.this).getData(HikeMessengerApp.SHOW_STEALTH_UNREAD_TIP, false))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_STEALTH_UNREAD_TIP, null);
		}
		if (HikeSharedPreferenceUtil.getInstance(HomeActivity.this).getData(HikeMessengerApp.SHOW_STEALTH_INFO_TIP, false))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_STEALTH_INFO_TIP, null);
		}
		if (!HikeSharedPreferenceUtil.getInstance(HomeActivity.this).getData(HikeMessengerApp.SHOWN_WELCOME_HIKE_TIP, false))
		{
			HikeMessengerApp.getPubSub().publish(HikePubSub.REMOVE_WELCOME_HIKE_TIP, null);
		}
		if (HikeSharedPreferenceUtil.getInstance(HomeActivity.this).getData(HikeMessengerApp.SHOWING_STEALTH_FTUE_CONV_TIP, false))
		{
			return;
		}
		if (!HikeSharedPreferenceUtil.getInstance(HomeActivity.this).getData(HikeMessengerApp.STEALTH_MODE_SETUP_DONE, false))
		{
			if (tipTypeShowing != null && tipTypeShowing == TipType.STEALTH_FTUE_TIP_2)
			{
				findViewById(R.id.stealth_double_tap_tip).setVisibility(View.GONE);
				tipTypeShowing = null;
				LockPattern.createNewPattern(HomeActivity.this, false);
			}
			else
			{
				if (!(dialog != null && dialog.isShowing()) && mainFragment != null)
				{
					if (!mainFragment.hasNoConversation())
					{

						dialogShowing = DialogShowing.STEALTH_FTUE_POPUP;
						dialog = HikeDialog.showDialog(HomeActivity.this, HikeDialog.STEALTH_FTUE_DIALOG, getHomeActivityDialogListener());
					}
					else
					{
						dialogShowing = DialogShowing.STEALTH_FTUE_EMPTY_STATE_POPUP;
						dialog = HikeDialog.showDialog(HomeActivity.this, HikeDialog.STEALTH_FTUE_EMPTY_STATE_DIALOG, getHomeActivityDialogListener());
					}
				}
			}
		}
		else
		{
			if (tipTypeShowing != null && tipTypeShowing == TipType.STEALTH_FTUE_ENTER_PASS_TIP)
			{
				findViewById(R.id.stealth_double_tap_tip).setVisibility(View.GONE);
				tipTypeShowing = null;
			}
			final int stealthType = HikeSharedPreferenceUtil.getInstance(HomeActivity.this).getData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
			if (stealthType == HikeConstants.STEALTH_OFF)
			{
				LockPattern.confirmPattern(HomeActivity.this, false);
			}
			else
			{
				HikeSharedPreferenceUtil.getInstance(HomeActivity.this).saveData(HikeMessengerApp.STEALTH_MODE, HikeConstants.STEALTH_OFF);
				HikeMessengerApp.getPubSub().publish(HikePubSub.STEALTH_MODE_TOGGLED, true);
				Utils.sendUILogEvent(HikeConstants.LogEvent.EXIT_STEALTH_MODE);
			}
		}
	}

	
}
