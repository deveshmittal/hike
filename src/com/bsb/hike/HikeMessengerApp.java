package com.bsb.hike;

import static org.acra.ACRA.LOG_TAG;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.acra.ACRA;
import org.acra.CrashReportData;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.HttpRequest;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.ConfigurationContext;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.bsb.hike.db.DbConversationListener;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.db.HikeMqttPersistence;
import com.bsb.hike.db.HikeUserDatabase;
import com.bsb.hike.models.utils.IconCacheManager;
import com.bsb.hike.service.HikeMqttManager.MQTTConnectionStatus;
import com.bsb.hike.service.HikeService;
import com.bsb.hike.service.HikeServiceConnection;
import com.bsb.hike.ui.WelcomeActivity;
import com.bsb.hike.utils.AccountUtils;
import com.bsb.hike.utils.ActivityTimeLogger;
import com.bsb.hike.utils.SmileyParser;
import com.bsb.hike.utils.ToastListener;
import com.bsb.hike.utils.Utils;
import com.facebook.android.Facebook;
import com.fiksu.asotracking.FiksuTrackingManager;

@ReportsCrashes(formKey = "", customReportContent = {
		ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
		ReportField.PHONE_MODEL, ReportField.BRAND, ReportField.PRODUCT,
		ReportField.ANDROID_VERSION, ReportField.STACK_TRACE,
		ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE })
public class HikeMessengerApp extends Application {
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

	public static final String AUTO_RECOMMENDED_FAVORITES_ADDED = "autoRecommendedFavoritesAdded";

	public static final String FILE_PATH = "filePath";

	public static final String TEMP_NAME = "tempName";

	public static final String TEMP_NUM = "tempNum";

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

	private static Facebook facebook;

	private static Twitter twitter;

	private static HikePubSub mPubSubInstance;

	private static boolean isIndianUser;

	private static Messenger mMessenger;

	private Messenger mService;

	private HikeServiceConnection mServiceConnection;

	private boolean mInitialized;

	private String token;

	private String msisdn;

	private DbConversationListener dbConversationListener;

	private ToastListener toastListener;

	private ActivityTimeLogger activityTimeLogger;

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
				final String reportUrl = AccountUtils.BASE + "/logs/android";
				Log.d(LOG_TAG, "Connect to " + reportUrl.toString());

				final String login = msisdn;
				final String password = token;

				if (login != null && password != null) {
					final HttpRequest request = new HttpRequest(login, password);
					String paramsAsString = getParamsAsString(crashReportData);
					Log.e(getClass().getSimpleName(), "Params: "
							+ paramsAsString);
					request.sendPost(reportUrl, paramsAsString);
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
		FiksuTrackingManager.initialize(this);

		SharedPreferences settings = getSharedPreferences(
				HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
		msisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);

		Utils.setupServerURL(settings.getBoolean(HikeMessengerApp.PRODUCTION,
				true));

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

		initialiseListeners();

		mMessenger = new Messenger(new IncomingHandler());

		if (token != null) {
			AccountUtils.setToken(token);
		}
		try {
			AccountUtils.setAppVersion(getPackageManager().getPackageInfo(
					getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			Log.e(getClass().getSimpleName(), "Invalid package", e);
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

	public static void setIndianUser(boolean isIndianUser) {
		HikeMessengerApp.isIndianUser = isIndianUser;
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
			Log.e("HikeMessengerApp", "Invalid format", e);
			return;
		}
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
}
