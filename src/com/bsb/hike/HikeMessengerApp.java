package com.bsb.hike;

import static org.acra.ACRA.LOG_TAG;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.sender.HttpSender;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.HttpRequest;
import org.json.JSONArray;
import org.json.JSONException;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.ConfigurationContext;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.bsb.hike.HikePubSub.Listener;
import com.bsb.hike.db.DbConversationListener;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.StickerCategory;
import com.bsb.hike.models.TypingNotification;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.service.HikeMqttManager.MQTTConnectionStatus;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.service.HikeServiceConnection;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ActivityTimeLogger;
import com.bsb.hike.utils.EmoticonConstants;
import com.bsb.hike.utils.FileTransferTaskBase;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.StickerTaskBase;
import com.bsb.hike.utils.ToastListener;
import com.bsb.hike.utils.TrackerUtil;
import com.bsb.hike.utils.Utils;
import com.facebook.android.Facebook;

@ReportsCrashes(formKey = "", customReportContent = {
		ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
		ReportField.PHONE_MODEL, ReportField.BRAND, ReportField.PRODUCT,
		ReportField.ANDROID_VERSION, ReportField.STACK_TRACE,
		ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE })
public class HikeMessengerApp extends Application implements Listener {

	public static enum CurrentState {
		OPENED, RESUMED, BACKGROUNDED, CLOSED, NEW_ACTIVITY, BACK_PRESSED
	}

	public static final String ACCOUNT_SETTINGS = "accountsettings";

	public static final String MSISDN_SETTING = "msisdn";

	public static final String CARRIER_SETTING = "carrier";

	public static final String NAME_SETTING = "name";

	public static final String TOKEN_SETTING = "token";

	public static final String MESSAGES_SETTING = "messageid";

	public static final String UID_SETTING = "uid";

	public static final String UPDATE_SETTING = "update";

	public static final String ANALYTICS = "analytics";

	public static final String REFERRAL = "referral";

	public static final String ADDRESS_BOOK_SCANNED = "abscanned";

	public static final String CONTACT_LIST_EMPTY = "contactlistempty";

	public static final String SMS_SETTING = "smscredits";

	public static final String NAME = "name";

	public static final String ACCEPT_TERMS = "acceptterms";

	public static final String CONNECTED_ONCE = "connectedonce";

	public static final String MESSAGES_LIST_TOOLTIP_DISMISSED = "messageslist_tooltip";

	public static final String SPLASH_SEEN = "splashseen";

	public static final String INVITE_TOOLTIP_DISMISSED = "inviteToolTip";

	public static final String EMAIL = "email";

	public static final String CHAT_INVITE_TOOL_TIP_DISMISSED = "chatInviteToolTipDismissed";

	public static final String INVITED = "invited";

	public static final String INVITED_JOINED = "invitedJoined";

	public static final String CHAT_GROUP_INFO_TOOL_TIP_DISMISSED = "chatGroupInfoToolTipDismissed";

	public static final String NUM_TIMES_HOME_SCREEN = "numTimesHomeScreen";

	public static final String NUM_TIMES_CHAT_THREAD_INVITE = "numTimesChatThreadInvite";

	public static final String NUM_TIMES_CHAT_THREAD_GROUP = "numTimesChatThreadGroup";

	public static final String NUM_TIMES_INVITE = "numTimesInvite";

	public static final String SHOW_CREDIT_SCREEN = "showCreditScreen";

	public static final String CONTACT_EXTRA_INFO_SYNCED = "contactExtraInfoSynced2";

	public static final String SHOWN_TUTORIAL = "showTutorial";

	public static final String SHOW_GROUP_CHAT_TOOL_TIP = "showGroupChatToolTip";

	public static final String TOTAL_CREDITS_PER_MONTH = HikeConstants.TOTAL_CREDITS_PER_MONTH;

	public static final String PRODUCTION = "production";

	public static final String COUNTRY_CODE = "countryCode";

	public static final String FILE_PATH = "filePath";

	public static final String TEMP_NAME = "tempName";

	public static final String TEMP_NUM = "tempNum";

	public static final String TEMP_COUNTRY_CODE = "tempCountryCode";

	public static final String GCM_ID_SENT = "gcmIdSent";

	/*
	 * Setting name for the day the was logged on fiksu for
	 * "First message sent in day"
	 */
	public static final String DAY_RECORDED = "dayRecorded";

	public static final String LAST_BACK_OFF_TIME = "lastBackOffTime";

	public static final String FACEBOOK_TOKEN = "facebookToken";
	public static final String FACEBOOK_TOKEN_EXPIRES = "facebookTokenExpires";
	public static final String FACEBOOK_USER_ID = "facebookUserId";
	public static final String FACEBOOK_AUTH_COMPLETE = "facebookAuthComplete";

	public static final String TWITTER_TOKEN = "twitterToken";
	public static final String TWITTER_TOKEN_SECRET = "twitterTokenSecret";
	public static final String TWITTER_AUTH_COMPLETE = "twitterAuthComplete";

	public static final String MSISDN_ENTERED = "msisdnEntered";

	public static final String BROKER_HOST = "brokerHost";
	public static final String BROKER_PORT = "brokerPort";

	public static final String FAVORITES_INTRO_SHOWN = "favoritesIntroShown";
	public static final String NUDGE_INTRO_SHOWN = "nudgeIntroShown";

	public static final String REWARDS_TOKEN = "rewardsToken";
	public static final String SHOW_REWARDS = "showRewards";
	public static final String TALK_TIME = "talkTime";
	
	public static final String GAMES_TOKEN = "gamesToken";
	
	public static final String SHOW_GAMES = "showGames";
	
	public static final String GCK_SHOWN = "gckShown";
	public static final String ADD_CONTACT_SHOWN = "addContactShown";

	public static final String LAST_STATUS = "lastStatus";
	public static final String LAST_MOOD = "lastMood";

	public static final String APP_LAUNCHES = "appLaunches";
	public static final String DONT_SHOW_APP_RATER = "dontShowAppRater";

	public static final String INTRO_DONE = "introDone";

	public static final String JUST_SIGNED_UP = "justSignedUp";

	public static final String INVITED_NUMBERS = "invitedNumbers";

	public static final String UNSEEN_STATUS_COUNT = "unseenStatusCount";

	public static final String UNSEEN_USER_STATUS_COUNT = "unseenUserStatusCount";

	public static final String BATCH_STATUS_NOTIFICATION_VALUES = "batchStatusNotificationValues";

	public static final String USER_JOIN_TIME = "userJoinTime";

	public static final String DEVICE_DETAILS_SENT = "deviceDetailsSent";
	public static final String LAST_BACK_OFF_TIME_DEV_DETAILS = "lastBackOffTimeDevDetails";

	public static final String SHOW_CRICKET_MOODS = "showCricketMoods";

	public static final String FRIEND_INTRO_SHOWN = "friendIntroShown";

	public static final String STATUS_NOTIFICATION_SETTING = "statusNotificationSetting";

	public static final String STATUS_IDS = "statusIds";

	public static final String SHOWN_SMS_CLIENT_POPUP = "shownSMSClientPopup";
	public static final String SHOWN_SMS_SYNC_POPUP = "shownSMSSyncPopup";

	public static final String REMOVED_CATGORY_IDS = "removedCategoryIds";

	public static final String SHOWN_DEFAULT_STICKER_DOGGY_CATEGORY_POPUP = "shownDefaultStickerCategoryPopup";

	public static final String SHOWN_DEFAULT_STICKER_HUMANOID_CATEGORY_POPUP = "shownDefaultStickerHumanoidCategoryPopup";

	public static final String FIRST_CATEGORY_INSERT_TO_DB = "firstCategoryInsertedToDB";

	public static final String SECOND_CATEGORY_INSERT_TO_DB = "secondCategoryInsertedToDB";

	public static final String SERVER_TIME_OFFSET = "serverTimeOffset";

	public static final String SHOWN_EMOTICON_TIP = "shownEmoticonTip";

	public static final String SHOWN_STICKERS_TIP = "shownStickerTip";

	public static final String SHOWN_MOODS_TIP = "shownMoodsTip";

	public static final String SHOWN_WALKIE_TALKIE_TIP = "shownWalkieTalkieTip";

	public static final String SHOWN_STATUS_TIP = "shownStatusTip";

	public static final String SHOWN_LAST_SEEN_TIP = "shownLastSeenTip";

	public static final String PROTIP_DISMISS_TIME = "protipDismissTime";

	public static final String PROTIP_WAIT_TIME = "protipWaitTime";

	public static final String CURRENT_PROTIP = "currentProtip";

	public static final String SHOWN_NATIVE_SMS_INVITE_POPUP = "shownNativeSmsInvitePopup";

	public static final String BUTTONS_OVERLAY_SHOWN = "buttonsOverlayShown";

	public static final String SHOWN_FRIENDS_TUTORIAL = "shownFriendsTutorial";

	public static final String SHOWN_STICKERS_TUTORIAL = "shownStickersTutorial";

	public static final String SHOWN_NATIVE_INFO_POPUP = "shownNativeInfoPopup";

	public static final String SHOW_BOLLYWOOD_STICKERS = "showBollywoodStickers";

	public static final String INVITED_FACEBOOK_FRIENDS_IDS = "invitedFacebookFriendsIds";

	public static List<StickerCategory> stickerCategories;

	public static CurrentState currentState = CurrentState.CLOSED;

	private static Facebook facebook;

	private static Twitter twitter;

	private static HikePubSub mPubSubInstance;

	private static boolean isIndianUser;

	private static Messenger mMessenger;

	private static Map<String, TypingNotification> typingNotificationMap;

	private Messenger mService;

	private HikeServiceConnection mServiceConnection;

	private boolean mInitialized;

	private String token;

	private String msisdn;

	private DbConversationListener dbConversationListener;

	private ToastListener toastListener;

	private ActivityTimeLogger activityTimeLogger;
	
	public static Map<Long, FileTransferTaskBase> fileTransferTaskMap;

	public static Map<String, StickerTaskBase> stickerTaskMap;

	public static Map<String, Long> lastSeenFriendsMap;	
	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d("HikeMessengerApp", "In handleMessage " + msg.what);
			switch (msg.what) {
			case HikeService.MSG_APP_MESSAGE_STATUS:
				boolean success = msg.arg1 != 0;
				Long msgId = (Long) msg.obj;
				Log.d("HikeMessengerApp", "received msg status msgId:" + msgId
						+ " state: " + success);
				// TODO handle this where we are saving all the mqtt messages
				String event = success ? HikePubSub.SERVER_RECEIVED_MSG
						: HikePubSub.MESSAGE_FAILED;
				mPubSubInstance.publish(event, msgId);
				break;
			case HikeService.MSG_APP_CONN_STATUS:
				Log.d("HikeMessengerApp", "received connection status "
						+ msg.arg1);
				int s = msg.arg1;
				MQTTConnectionStatus status = MQTTConnectionStatus.values()[s];
				mPubSubInstance.publish(HikePubSub.CONNECTION_STATUS, status);
				break;
			case HikeService.MSG_APP_INVALID_TOKEN:
				Log.d("HikeMessengerApp",
						"received invalid token message from service");
				HikeMessengerApp.this.disconnectFromService();
				HikeMessengerApp.this.stopService(new Intent(
						HikeMessengerApp.this, HikeService.class));
				HikeMessengerApp.this.startActivity(new Intent(
						HikeMessengerApp.this, WelcomeActivity.class));
			}
		}
	}

	static {
		mPubSubInstance = new HikePubSub();
		if (HikeMessengerApp.fileTransferTaskMap == null) {
			HikeMessengerApp.fileTransferTaskMap = new HashMap<Long, FileTransferTaskBase>();
		}
		if (HikeMessengerApp.stickerTaskMap == null) {
			HikeMessengerApp.stickerTaskMap = new HashMap<String, StickerTaskBase>();
		}
		if (HikeMessengerApp.lastSeenFriendsMap == null) {
			HikeMessengerApp.lastSeenFriendsMap = new HashMap<String, Long>();
		}
	}

	public void sendToService(Message message) {
		try {
			mService.send(message);
		} catch (RemoteException e) {
			Log.e("HikeMessengerApp", "Unable to connect to service", e);
		}
	}

	public void disconnectFromService() {
		if (mInitialized) {
			synchronized (HikeMessengerApp.class) {
				if (mInitialized) {
					mInitialized = false;
					unbindService(mServiceConnection);
					mServiceConnection = null;
				}
			}
		}
	}

	public void connectToService() {
		Log.d("HikeMessengerApp", "calling connectToService:" + mInitialized);
		if (!mInitialized) {
			synchronized (HikeMessengerApp.class) {
				if (!mInitialized) {
					mInitialized = true;
					Log.d("HikeMessengerApp", "Initializing service");
					mServiceConnection = HikeServiceConnection
							.createConnection(this, mMessenger);
				}
			}
		}
	}

	/*
	 * Implement a Custom report sender to add our own custom msisdn and token
	 * for the username and password
	 */
	private class CustomReportSender implements ReportSender {
		@Override
		public void send(CrashReportData crashReportData)
				throws ReportSenderException {
			/* only send ACRA reports if we're in release mode */
			if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
				return;
			}

			try {
				final String reportUrl = AccountUtils.base + "/logs/android";
				Log.d(LOG_TAG, "Connect to " + reportUrl.toString());

				final String login = msisdn;
				final String password = token;

				if (login != null && password != null) {
					final HttpRequest request = new HttpRequest();
					request.setLogin(login);
					request.setPassword(password);
					String paramsAsString = getParamsAsString(crashReportData);
					Log.e(getClass().getSimpleName(), "Params: "
							+ paramsAsString);
					request.send(new URL(reportUrl), HttpSender.Method.POST,
							paramsAsString, HttpSender.Type.FORM);
				}
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), "IOException", e);
			}
		}

	}

	/**
	 * Converts a Map of parameters into a URL encoded Sting.
	 * 
	 * @param parameters
	 *            Map of parameters to convert.
	 * @return URL encoded String representing the parameters.
	 * @throws UnsupportedEncodingException
	 *             if one of the parameters couldn't be converted to UTF-8.
	 */
	private String getParamsAsString(Map<?, ?> parameters)
			throws UnsupportedEncodingException {

		final StringBuilder dataBfr = new StringBuilder();
		for (final Object key : parameters.keySet()) {
			if (dataBfr.length() != 0) {
				dataBfr.append('&');
			}
			final Object preliminaryValue = parameters.get(key);
			final Object value = (preliminaryValue == null) ? ""
					: preliminaryValue;
			dataBfr.append(URLEncoder.encode(key.toString(), "UTF-8"));
			dataBfr.append('=');
			dataBfr.append(URLEncoder.encode(value.toString(), "UTF-8"));
		}

		return dataBfr.toString();
	}

	public void onCreate() {

		SharedPreferences settings = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		String uid = settings.getString(HikeMessengerApp.UID_SETTING, null);

		ACRA.init(this);
		CustomReportSender customReportSender = new CustomReportSender();
		ErrorReporter.getInstance().setReportSender(customReportSender);

		super.onCreate();

		HikeConversationsDatabase.init(this);
		HikeUserDatabase.init(this);
		HikeMqttPersistence.init(this);

		SmileyParser.init(this);

		IconCacheManager.init();

		facebook = new Facebook(HikeConstants.APP_FACEBOOK_ID);
		makeFacebookInstance(settings);

		String twitterToken = settings.getString(
				HikeMessengerApp.TWITTER_TOKEN, "");
		String twitterTokenSecret = settings.getString(
				HikeMessengerApp.TWITTER_TOKEN_SECRET, "");
		makeTwitterInstance(twitterToken, twitterTokenSecret);

		isIndianUser = settings.getString(COUNTRY_CODE, "").equals(
				HikeConstants.INDIA_COUNTRY_CODE);

		SharedPreferences preferenceManager = PreferenceManager
				.getDefaultSharedPreferences(this);

		// adding a check here for MobileAppTracker SDK update case
		// we use this preference to check if this is a fresh install case or an
		// update case
		// in case of an update the SSL pref would not be null
		TrackerUtil tUtil = TrackerUtil.getInstance(this
				.getApplicationContext());
		if (tUtil != null) {
			tUtil.setTrackOptions(!preferenceManager
					.contains(HikeConstants.SSL_PREF));
			Log.d(getClass().getSimpleName(),
					"Init for apptracker sdk finished"
							+ !preferenceManager
									.contains(HikeConstants.SSL_PREF));
		}

		if (!preferenceManager.contains(HikeConstants.SSL_PREF)) {
			Editor editor = preferenceManager.edit();
			editor.putBoolean(HikeConstants.SSL_PREF, !isIndianUser);
			editor.commit();
		}

		if (!preferenceManager.contains(HikeConstants.RECEIVE_SMS_PREF)) {
			Editor editor = preferenceManager.edit();
			editor.putBoolean(HikeConstants.RECEIVE_SMS_PREF, false);
			editor.commit();
		}

		Utils.setupServerURL(
				settings.getBoolean(HikeMessengerApp.PRODUCTION, true),
				Utils.switchSSLOn(getApplicationContext()));

		typingNotificationMap = new HashMap<String, TypingNotification>();

		initialiseListeners();

		mMessenger = new Messenger(new IncomingHandler());

		if (token != null) {
			AccountUtils.setToken(token);
		}
		if (uid != null) {
			AccountUtils.setUID(uid);
		}
		try {
			AccountUtils.setAppVersion(getPackageManager().getPackageInfo(
					getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			Log.e(getClass().getSimpleName(), "Invalid package", e);
		}

		if (!settings.contains(SHOW_BOLLYWOOD_STICKERS)) {
			setupBollywoodCategoryVisibility(settings);
		}
		setupStickerCategoryList(settings);

		if (!preferenceManager.getBoolean(FIRST_CATEGORY_INSERT_TO_DB, false)) {
			HikeConversationsDatabase.getInstance()
					.insertDoggyStickerCategory();
			Editor editor = preferenceManager.edit();
			editor.putBoolean(FIRST_CATEGORY_INSERT_TO_DB, true);
			editor.commit();
		}

		if (!preferenceManager.getBoolean(SECOND_CATEGORY_INSERT_TO_DB, false)) {
			HikeConversationsDatabase.getInstance()
					.insertHumanoidStickerCategory();
			Editor editor = preferenceManager.edit();
			editor.putBoolean(SECOND_CATEGORY_INSERT_TO_DB, true);
			editor.commit();
		}

		HikeMessengerApp.getPubSub().addListener(
				HikePubSub.SWITCHED_DATA_CONNECTION, this);
	}

	private static void setupBollywoodCategoryVisibility(SharedPreferences prefs) {
		String countryCode = prefs.getString(COUNTRY_CODE, "");

		if (TextUtils.isEmpty(countryCode)) {
			return;
		}

		boolean showBollywoodCategory = false;
		for (String bollywoodCountryCode : HikeConstants.BOLLYWOOD_COUNTRY_CODES) {
			if (bollywoodCountryCode.equals(countryCode)) {
				showBollywoodCategory = true;
				break;
			}
		}
		Editor editor = prefs.edit();
		editor.putBoolean(SHOW_BOLLYWOOD_STICKERS, showBollywoodCategory);
		if (!showBollywoodCategory) {
			try {
				JSONArray removedIdArray = new JSONArray(prefs.getString(
						REMOVED_CATGORY_IDS, "[]"));
				removedIdArray.put(HikeConstants.BOLLYWOOD_CATEGORY);
				editor.putString(REMOVED_CATGORY_IDS, removedIdArray.toString());
			} catch (JSONException e) {
				editor.remove(REMOVED_CATGORY_IDS);
				Log.w("HikeMessengerApp", "Removed id array pref corrupted", e);
			}
		}
		editor.commit();
		if (!showBollywoodCategory) {
			setupStickerCategoryList(prefs);
		}
	}

	public static Facebook getFacebook() {
		return facebook;
	}

	public static HikePubSub getPubSub() {
		return mPubSubInstance;
	}

	public void setService(Messenger service) {
		this.mService = service;
	}

	public static void setIndianUser(boolean isIndianUser,
			SharedPreferences prefs) {
		HikeMessengerApp.isIndianUser = isIndianUser;
		if (!prefs.contains(SHOW_BOLLYWOOD_STICKERS)) {
			setupBollywoodCategoryVisibility(prefs);
		}
	}

	public static boolean isIndianUser() {
		return isIndianUser;
	}

	public static void makeFacebookInstance(SharedPreferences settings) {
		facebook.setAccessExpires(settings.getLong(
				HikeMessengerApp.FACEBOOK_TOKEN_EXPIRES, 0));
		facebook.setAccessToken(settings.getString(
				HikeMessengerApp.FACEBOOK_TOKEN, ""));
	}

	public static void makeTwitterInstance(String token, String tokenSecret) {
		AccessToken accessToken = null;
		try {
			accessToken = new AccessToken(token, tokenSecret);

			OAuthAuthorization authorization = new OAuthAuthorization(
					ConfigurationContext.getInstance());
			authorization.setOAuthAccessToken(accessToken);
			authorization.setOAuthConsumer(HikeConstants.APP_TWITTER_ID,
					HikeConstants.APP_TWITTER_SECRET);

			twitter = new TwitterFactory().getInstance(authorization);
		} catch (IllegalArgumentException e) {
			Log.w("HikeMessengerApp", "Invalid format", e);
			return;
		}
	}

	public static Twitter getTwitterInstance(String token, String tokenSecret) {

		try {
			makeTwitterInstance(token, tokenSecret);

			return twitter;
		} catch (IllegalArgumentException e) {
			Log.w("HikeMessengerApp", "Invalid format", e);
			return null;
		}
	}

	public static Map<String, TypingNotification> getTypingNotificationSet() {
		return typingNotificationMap;
	}

	public void initialiseListeners() {
		if (dbConversationListener == null) {
			dbConversationListener = new DbConversationListener(
					getApplicationContext());
		}
		if (toastListener == null) {
			toastListener = new ToastListener(getApplicationContext());
		}
		if (activityTimeLogger == null) {
			activityTimeLogger = new ActivityTimeLogger();
		}
	}

	@Override
	public void onEventReceived(String type, Object object) {
		if (HikePubSub.SWITCHED_DATA_CONNECTION.equals(type)) {
			SharedPreferences settings = getSharedPreferences(
					HikeMessengerApp.ACCOUNT_SETTINGS, 0);
			boolean isWifiConnection = object != null ? (Boolean) object
					: Utils.switchSSLOn(getApplicationContext());

			Utils.setupServerURL(
					settings.getBoolean(HikeMessengerApp.PRODUCTION, true),
					isWifiConnection);
		}
	}

	public static void setupStickerCategoryList(SharedPreferences preferences) {
		stickerCategories = new ArrayList<StickerCategory>();

		for (int i = 0; i < EmoticonConstants.STICKER_CATEGORY_IDS.length; i++) {
			boolean isUpdateAvailable = HikeConversationsDatabase.getInstance()
					.isStickerUpdateAvailable(
							EmoticonConstants.STICKER_CATEGORY_IDS[i]);
			stickerCategories.add(new StickerCategory(
					EmoticonConstants.STICKER_CATEGORY_IDS[i],
					EmoticonConstants.STICKER_CATEGORY_RES_IDS[i],
					EmoticonConstants.STICKER_DOWNLOAD_PREF[i],
					EmoticonConstants.STICKER_CATEGORY_PREVIEW_RES_IDS[i],
					isUpdateAvailable));
		}
		String removedIds = preferences.getString(
				HikeMessengerApp.REMOVED_CATGORY_IDS, "[]");

		try {
			JSONArray removedIdArray = new JSONArray(removedIds);
			for (int i = 0; i < removedIdArray.length(); i++) {
				String removedCategoryId = removedIdArray.getString(i);
				StickerCategory removedStickerCategory = getStickerCategoryForCategoryId(removedCategoryId);

				stickerCategories.remove(removedStickerCategory);
			}
		} catch (JSONException e) {
			Log.w("HikeMessengerApp", "Invalid JSON", e);
		}
	}

	public static StickerCategory getStickerCategoryForCategoryId(
			String categoryId) {
		return new StickerCategory(categoryId, 0, null, 0, false);
	}

	public static void setStickerUpdateAvailable(String categoryId,
			boolean updateAvailable) {
		int index = stickerCategories
				.indexOf(getStickerCategoryForCategoryId(categoryId));
		if (index == -1) {
			return;
		}
		StickerCategory stickerCategory = stickerCategories.get(index);
		stickerCategory.updateAvailable = updateAvailable;
	}
}
