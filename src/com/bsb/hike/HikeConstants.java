package com.bsb.hike;

import android.net.Uri;
import android.os.Environment;

public class HikeConstants {
	public static final String APP_PUSH_ID = "768395314950";
	public static final String APP_FACEBOOK_ID = "425850510764995";
	public static final String APP_TWITTER_ID = "7LFaGIe5QXj05WN1YDDVaA";
	public static final String APP_TWITTER_SECRET = "LhgJVQ9eAmbb3EGdXpLD8B4RHf9SGPrzSqaOjuKL5o4";

	public static final String MAP_API_KEY_DEBUG = "0Luu6V6IYSC0UpLSUZe7oO-bvd392OgrXSnY8aA";
	public static final String MAP_API_KEY_PROD = "0Luu6V6IYSC3olBhRm5jHLKDIn5_CA3P17l_3Mw";

	public static final String MA_TRACKER_AD_ID = "9198";
	public static final String MA_TRACKER_KEY = "00774f47f97b5173432b958a986f0061";
	public static final String MA_TRACKER_USERID = "bsb_hike";
	public static final String MA_TRACKER_REF_ID_PREFIX = "bsb_hike_";
	
	public static final String ANDROID = "android";

	public static final String MESSAGE = "msg";
	public static final String UI_TOPIC = "/u";
	public static final String APP_TOPIC = "/a";
	public static final String SERVICE_TOPIC = "/s";
	public static final String PUBLISH_TOPIC = "/p";

	public static final String TYPE = "t";
	public static final String DATA = "d";
	public static final String TO = "to";
	public static final String FROM = "f";
	public static final String SUB_TYPE = "st";

	public static final String HIKE_MESSAGE = "hm";
	public static final String SMS_MESSAGE = "sm";
	public static final String TIMESTAMP = "ts";
	public static final String MESSAGE_ID = "i";
	public static final String METADATA = "md";
	public static final String METADATA_DND = "dnd";
	public static final String ANALYTICS_EVENT = "ae";
	public static final String ALL_INVITEE = "ai";
	public static final String ALL_INVITEE_JOINED = "aij";
	public static final String ALL_INVITEE_2 = "all_invitee";
	public static final String ALL_INVITEE_JOINED_2 = "all_invitee_joined";
	public static final String NAME = "name";
	public static final String MSISDN = "msisdn";
	public static final String NEW_USER = "nu";
	public static final String EMAIL = "email";
	public static final String GENDER = "gender";
	public static final String VERSION = "v";
	public static final String CRITICAL = "c";
	public static final String INVITE_TOKEN = "invite_token";
	public static final String TOTAL_CREDITS_PER_MONTH = "tc";
	public static final String DND_NUMBERS = "dndnumbers";
	public static final String FILES = "files";
	public static final String CONTENT_TYPE = "ct";
	public static final String THUMBNAIL = "tn";
	public static final String FILE_NAME = "fn";
	public static final String FILE_KEY = "fk";
	public static final String CREDITS = "credits";
	public static final String ON_HIKE = "onhike";
	public static final String DND = "dnd";
	public static final String DND_USERS = "dndUsers";
	public static final String LATITUDE = "lat";
	public static final String LONGITUDE = "long";
	public static final String ZOOM_LEVEL = "zoom";
	public static final String ADDRESS = "add";
	public static final String POKE = "poke";
	public static final String ID = "id";
	public static final String TOKEN = "token";
	public static final String EXPIRES = "expires";
	public static final String POST = "post";
	public static final String ACCOUNT = "account";
	public static final String ACCOUNTS = "accounts";
	public static final String FAVORITES = "favorites";
	public static final String PENDING = "pending";
	public static final String MSISDNS = "msisdns";
	public static final String REWARDS_TOKEN = "reward_token";
	public static final String SHOW_REWARDS = "show_rewards";
	public static final String REWARDS = "rewards";
	public static final String TALK_TIME = "tt";
	public static final String PHONE_NUMBERS = "phone_numbers";
	public static final String EMAILS = "emails";
	public static final String ADDRESSES = "addresses";
	public static final String EVENTS = "events";
	public static final String STATUS_ID = "statusid";
	public static final String MOOD = "mood";
	public static final String STATUS_MESSAGE = "msg";
	public static final String PROFILE = "profile";
	public static final String ICON = "icon";
	public static final String MUTED = "muted";
	public static final String POST_AB = "postab";
	public static final String PUSH = "push";
	public static final String JOIN_TIME = "jointime";
	public static final String STATUS_MESSAGE_2 = "status-message";
	public static final String FACEBOOK_STATUS = "fb";
	public static final String TWITTER_STATUS = "twitter";
	public static final String TIME_OF_DAY = "timeofday";
	public static final String REQUEST_PENDING = "requestpending";
	public static final String LOCALE = "locale";
	public static final String ENABLE_PUSH_BATCHING_STATUS_NOTIFICATIONS = "enablepushbatchingforsu";
	public static final String PUSH_SU = "pushsu";
	public static final String BATCH_HEADER = "h";
	public static final String BATCH_MESSAGE = "m";
	public static final String UPGRADE = "upgrade";
	public static final String DEV_TYPE = "dev_type";
	public static final String APP_VERSION = "app_version";
	public static final String DEVICE_VERSION = "deviceversion";
	public static final String CRICKET_MOODS = "cmoods";
	public static final String COUNT = "c";
	public static final String DEFAULT_SMS_CLIENT_TUTORIAL = "dsctutorial";
	public static final String CATEGORY_ID = "catId";
	public static final String STICKER_ID = "stId";
	public static final String STICKER_IDS = "stIds";
	public static final String STICKER = "stk";
	public static final String RESOLUTION_ID = "resId";
	public static final String NUMBER_OF_STICKERS = "nos";
	public static final String DATA_2 = "data";
	public static final String ADD_STICKER = "addStk";
	public static final String REMOVE_STICKER = "remStk";
	public static final String REMOVE_CATEGORY = "remCat";
	public static final String STATUS = "stat";
	public static final String OK = "ok";
	public static final String REACHED_STICKER_END = "st";
	public static final String PLAYTIME = "pt";
	public static final String FOREGROUND = "fg";
	public static final String BACKGROUND = "bg";
	public static final String JUST_OPENED = "justOpened";
	public static final String LAST_SEEN = "ls";
	public static final String LAST_SEEN_SETTING = "lastseen";
	public static final String PROTIP_HEADER = "h";
	public static final String PROTIP_TEXT = "t";
	public static final String PROTIP_IMAGE_URL = "img";
	public static final String PROTIP_WAIT_TIME = "wt";
	public static final String NO_SMS = "nosms";
	public static final String RETURNING_USER = "ru";

	public static final String SOUND_PREF = "soundPref";
	public static final String VIBRATE_PREF = "vibratePref";
	public static final String FREE_SMS_PREF = "freeSmsPref";
	public static final String LED_PREF = "ledPref";
	public static final String NATIVE_JINGLE_PREF = "jinglePref";
	public static final String SSL_PREF = "sslPref";
	public static final String STATUS_PREF = "statusPref";
	public static final String SEND_SMS_PREF = "sendSmsPref";
	public static final String RECEIVE_SMS_PREF = "receiveSmsPref";
	public static final String SEND_UNDELIVERED_AS_NATIVE_SMS_PREF = "sendUndeliveredAsNativeSmsPref";
	public static final String LAST_SEEN_PREF = "lastSeenPref";
	public static final String HIKEBOT = "TD-HIKE";

	public static final String DONE = "Done";
	public static final String PIN_ERROR = "PinError";
	public static final String ADDRESS_BOOK_ERROR = "AddressBookError";
	public static final String CHANGE_NUMBER = "ChangeNumber";

	public static final String SEPARATOR = " - ";
	public static final String GROUP_PARTICIPANT_SEPARATOR = ", ";

	public static final String HELP_URL = "http://www.hike.in/help/android";
	public static final String T_AND_C_URL = "http://www.hike.in/terms/android";
	public static final String SYSTEM_HEALTH_URL = "http://www.twitter.com/hikestatus";

	public static final String IS_TYPING = "is typing...";

	public static final String NEW_GROUP = "new_group";

	/* Constant used to name the preference file which saves the drafts */
	public static final String DRAFT_SETTING = "draftSetting";

	/* how long to wait between sending publish and receiving an acknowledgement */
	public static final long MESSAGE_DELIVERY_TIMEOUT = 5 * 1000;

	/* how long to wait for a ping confirmation */
	public static final long PING_TIMEOUT = 5 * 1000;

	/*
	 * how long to wait to resend message. This should significantly greathr
	 * than PING_TIMEOUT
	 */
	public static final long MESSAGE_RETRY_INTERVAL = 15 * 1000;

	/* quiet period of no changes before actually updating the db */
	public static final long CONTACT_UPDATE_TIMEOUT = 10 * 1000;

	/* how often to ping the server */
	public static final short KEEP_ALIVE = 5 * 60; /* 10 minutes */

	/* how often to ping after a failure */
	public static final int RECONNECT_TIME = 10; /* 10 seconds */

	/* how often to ping after a server unavailable failure */
	public static final int SERVER_UNAVAILABLE_MAX_CONNECT_TIME = 9; /* 9 minutes */

	/* the max amount (in seconds) the reconnect time can be */
	public static final int MAX_RECONNECT_TIME = 120;

	/* the max amount of time we allow the service to run in case of no activity */
	public static final int DISCONNECT_TIME = 10 * 60;

	/* the max amount of time we wait for the PIN */
	public static final int PIN_CAPTURE_TIME = 60 * 1000;
	public static final int CALL_ME_WAIT_TIME = 120 * 1000;
	/*
	 * the amount to wait before showing the PIN screen on non sim devices.
	 */
	public static final int NON_SIM_WAIT_TIME = 30 * 1000;

	public static final int HIKE_SYSTEM_NOTIFICATION = 0;
	public static final String ADAPTER_NAME = "hikeadapter";

	/* constants for defining what to do after checking for updates */
	public static final int NORMAL_UPDATE = 2;
	public static final int CRITICAL_UPDATE = 1;
	public static final int NO_UPDATE = 0;

	// More explanation required?
	public static final int NUM_TIMES_SCREEN_SHOULD_OPEN_BEFORE_TOOL_TIP = 2;
	public static final String APP_API_VERSION = "2";

	public static final int NUM_SMS_PER_FRIEND = 10;
	public static final int INITIAL_NUM_SMS = 100;

	public static final int MAX_CHAR_IN_NAME = 20;

	public static final int MAX_CONTACTS_IN_GROUP = 20;
	public static final int MAX_SMS_CONTACTS_IN_GROUP = MAX_CONTACTS_IN_GROUP;

	public static final int PROFILE_IMAGE_DIMENSIONS = 120;

	public static final String VALID_MSISDN_REGEX = "\\+?[0-9]{1,15}";

	public static final int MAX_BUFFER_SIZE_KB = 100;
	public static final int MAX_FILE_SIZE = 15 * 1024 * 1024;

	public static final int IMAGE_CAPTURE_CODE = 1187;
	public static final int IMAGE_TRANSFER_CODE = 1188;
	public static final int VIDEO_TRANSFER_CODE = 1189;
	public static final int AUDIO_TRANSFER_CODE = 1190;
	public static final int SHARE_LOCATION_CODE = 1192;
	public static final int SHARE_CONTACT_CODE = 1193;

	public static final int MAX_DURATION_RECORDING_SEC = 360;

	public static final int MAX_DIMENSION_THUMBNAIL_PX = 180;

	public static final int MAX_DIMENSION_LOCATION_THUMBNAIL_PX = 220;

	public static final int MAX_DIMENSION_FULL_SIZE_PROFILE_PX = 500;

	public static final int MAX_DIMENSION_FULL_SIZE_PX = 800;

	public static final int INITIAL_PROGRESS = 5;

	public static final int NO_CHANGE = 0;
	public static final int PARTICIPANT_STATUS_CHANGE = 1;
	public static final int NEW_PARTICIPANT = 2;

	public static final String MAIL = "support@hike.in";

	// Had to add this constant since its only available in the android API for
	// Honeycomb and higher.
	public static final int FLAG_HARDWARE_ACCELERATED = 16777216;

	public static final int LOCAL_CLEAR_TYPING_TIME = 20 * 1000;

	// Number of recent contacts to show in the favorites drawer.
	public static final int RECENT_COUNT_IN_FAVORITE = 10;
	// Number of auto recommend contacts to show in favorites.
	public static final int MAX_AUTO_RECOMMENDED_FAVORITE = 5;

	// Fiksu Currency
	public static final String CURRENCY = "INR";

	// Fiksu Usernames
	public static final String FACEBOOK = "facebook";
	public static final String TWITTER = "twitter";
	public static final String INVITE = "invite";
	public static final String FIRST_MESSAGE = "first_message";

	// Fiksu Prices
	public static final int FACEBOOK_CONNECT = 100;
	public static final int TWITTER_CONNECT = 100;
	public static final int INVITE_SENT = 50;
	public static final int FIRST_MSG_IN_DAY = 10;

	/*
	 * Maximum number of conversations to be made automatically when the user
	 * signs up
	 */
	public static final int MAX_CONVERSATIONS = 6;

	/*
	 * Constant used as a type to signify that this message was added locally by
	 * the client when the user signed up
	 */
	public static final String INTRO_MESSAGE = "im";

	public static final String LOCATION_CONTENT_TYPE = "hikemap/location";
	public static final String LOCATION_FILE_NAME = "Location";

	public static final String CONTACT_CONTENT_TYPE = "contact/share";
	public static final String CONTACT_FILE_NAME = "Contact";

	public static final int DEFAULT_ZOOM_LEVEL = 12;

	// Picasa URI start for JB devices
	public static final String JB_PICASA_URI_START = "content://com.sec.android.gallery3d";
	// Picasa URI start for other devices
	public static final String OTHER_PICASA_URI_START = "content://com.google.android.gallery3d";

	public static final int MAX_MESSAGES_TO_LOAD_INITIALLY = 40;
	public static final int MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME = 20;
	public static final int MIN_INDEX_TO_LOAD_MORE_MESSAGES = 10;

	public static final int MAX_STATUSES_TO_LOAD_INITIALLY = 30;
	public static final int MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME = 20;

	public static final int SHOW_CREDITS_AFTER_NUM = 10;

	public static final String HIKE_MEDIA_DIRECTORY_ROOT = Environment
			.getExternalStorageDirectory() + "/Hike/Media";
	public static final String PROFILE_ROOT = "/hike Profile Images";
	public static final String IMAGE_ROOT = "/hike Images";
	public static final String VIDEO_ROOT = "/hike Videos";
	public static final String AUDIO_ROOT = "/hike Audios";
	public static final String AUDIO_RECORDING_ROOT = "/hike Voice Messages";
	public static final String STICKERS_ROOT = "/stickers";

	public static final String LARGE_STICKER_ROOT = "/large";
	public static final String SMALL_STICKER_ROOT = "/small";

	public static final String HIKE_FILE_LIST_NAME = "hikeFiles";

	public static final String STATUS_MESSAGE_HEADER = "hike-status-message";

	public static final String BOLLYWOOD_CATEGORY = "bollywood";
	/*
	 * Contact Type
	 */
	public static final int ON_HIKE_VALUE = 1;
	public static final int NOT_ON_HIKE_VALUE = 0;
	public static final int BOTH_VALUE = -1;

	public static final String INDIA_COUNTRY_CODE = "+91";

	public static final int MDPI_TIMES_10 = 11;

	public static final String NAMESPACE = "http://schemas.android.com/apk/res/com.bsb.hike";

	public static final String FONT = "font";

	public static final int MAX_MESSAGE_PREVIEW_LENGTH = 40;

	public static final String FACEBOOK_PROFILEPIC_URL_FORMAT = "https://graph.facebook.com/%1$s/picture?height=%2$d&width=%2$d";

	/*
	 * Constants for Profile Pic
	 */
	/* dialog IDs */
	public static final int PROFILE_PICTURE_FROM_CAMERA = 0;
	public static final int PROFILE_PICTURE_FROM_GALLERY = 1;

	/* activityForResult IDs */
	public static final int CAMERA_RESULT = 0;
	public static final int GALLERY_RESULT = 1;
	public static final int CROP_RESULT = 2991;

	public static final int MIN_STATUS_COUNT = 5;

	public static final int MAX_NUX_CONTACTS = 30;
	public static final int MAX_PRECHECKED_CONTACTS = 15;

	public static final int MAX_TWITTER_POST_LENGTH = 140;
	public static final int MAX_MOOD_TWITTER_POST_LENGTH = 130;

	public static final int MAX_NUM_STICKER_REQUEST = 10;

	/* In seconds */
	public static final int DEFAULT_PROTIP_WAIT_TIME = 300;
	public static final String PROTIP_STATUS_NAME = "hike team";

	/* In seconds */
	public static final int DEFAULT_UNDELIVERED_WAIT_TIME = 20;

	public static final int MAX_FALLBACK_NATIVE_SMS = 19;

	public static final int MAX_SMS_PULL_IN_INBOX = 2000;

	public static final int MAX_SMS_PULL_IN_SENTBOX = 1000;

	public static final class Extras {
		public static final String MSISDN = "msisdn";
		public static final String NAME = "name";
		public static final String PREV_MSISDN = "prevMsisdn";
		public static final String PREV_NAME = "prevName";
		public static final String INVITE = "invite";
		public static final String MSG = "msg";
		public static final String PREF = "pref";
		public static final String EDIT = "edit";
		public static final String IMAGE_PATH = "image-path";
		public static final String SCALE = "scale";
		public static final String OUTPUT_X = "outputX";
		public static final String OUTPUT_Y = "outputY";
		public static final String ASPECT_X = "aspectX";
		public static final String ASPECT_Y = "aspectY";
		public static final String DATA = "data";
		public static final String RETURN_DATA = "return-data";
		public static final String BITMAP = "bitmap";
		public static final String CIRCLE_CROP = "circleCrop";
		public static final String SCALE_UP = "scaleUpIfNeeded";
		public static final String UPDATE_AVAILABLE = "updateAvailable";
		public static final String KEEP_MESSAGE = "keepMessage";
		public static final String SHOW_CREDITS_HELP = "showCreditsHelp";
		public static final String CREDITS_HELP_COUNTER = "CreditsHelpCounter";
		public static final String SIGNUP_TASK_RUNNING = "signupTaskRunning";
		public static final String SIGNUP_PART = "signupPart";
		public static final String SIGNUP_TEXT = "signupText";
		public static final String SIGNUP_ERROR = "signupError";
		public static final String TOOLTIP_SHOWING = "tooltipShowing";
		public static final String FADE_OUT = "fadeOut";
		public static final String FADE_IN = "fadeIn";
		public static final String ANIMATED_ONCE = "animatedOnce";
		public static final String EDIT_PROFILE = "editProfile";
		public static final String EMAIL = "email";
		public static final String GENDER = "gender";
		public static final String OVERLAY_SHOWING = "overlayShowing";
		public static final String GROUP_CHAT = "groupChat";
		public static final String EMOTICON_SHOWING = "emoticonShowing";
		public static final String EXISTING_GROUP_CHAT = "existingGroupChat";
		public static final String LEAVE_GROUP_CHAT = "leaveGroupChat";
		public static final String APP_STARTED_FIRST_TIME = "appStartedFirstTime";
		public static final String LATEST_VERSION = "latestVersion";
		public static final String SHOW_UPDATE_OVERLAY = "showUpdateOverlay";
		public static final String SHOW_UPDATE_TOOL_TIP = "showUpdateToolTip";
		public static final String UPDATE_TOOL_TIP_SHOWING = "updateToolTipShowing";
		public static final String UPDATE_MESSAGE = "updateMessage";
		public static final String URL_TO_LOAD = "urlToLoad";
		public static final String TITLE = "title";
		public static final String FIRST_TIME_USER = "firstTimeUser";
		public static final String IS_DELETING_ACCOUNT = "isDeletingAccount";
		public static final String SMS_MESSAGE = "incomingSMSMessage";
		public static final String GROUP_LEFT = "groupLeft";
		public static final String ALERT_CANCELLED = "alertCancelled";
		public static final String DEVICE_DETAILS_SENT = "deviceDetailsSent";
		public static final String SIGNUP_MSISDN_ERROR = "signupMsisdnError";
		public static final String FILE_TRANSFER_DIALOG_SHOWING = "fileTransferDialogShowing";
		public static final String FILE_PATH = "filePath";
		public static final String FILE_KEY = "fileKey";
		public static final String FILE_TYPE = "fileType";
		public static final String FILE_NAME = "fileName";
		public static final String RECORDER_DIALOG_SHOWING = "recorderDialogShowing";
		public static final String RECORDER_START_TIME = "recorderStartTime";
		public static final String IS_LEFT_DRAWER_VISIBLE = "isLeftDrawerVisible";
		public static final String IS_RIGHT_DRAWER_VISIBLE = "isRightDrawerVisible";
		public static final String FORWARD_MESSAGE = "forwardMessage";
		public static final String HELP_PAGE = "helpPage";
		public static final String WHICH_EMOTICON_CATEGORY = "whichEmoticonCategory";
		public static final String WHICH_EMOTICON_SUBCATEGORY = "whichEmoticonSubcategory";
		public static final String COUNTRY_CODE = "countryCode";
		public static final String GOING_BACK_TO_HOME = "goingBackToHome";
		public static final String UPDATE_URL = "updateURL";
		public static final String UPDATE_TO_IGNORE = "updateToIgnore";
		public static final String INTRO_MESSAGE_ADDED = "introMessageAdded";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String ZOOM_LEVEL = "zoomLevel";
		public static final String CONTACT_INFO = "contactInfo";
		public static final String ON_HIKE = "onHike";
		public static final String SHOWING_SECOND_LOADING_TXT = "showingSecondLoadingTxt";
		public static final String FACEBOOK_POST_POPUP_SHOWING = "facebookPostPopupShowing";
		public static final String GPS_DIALOG_SHOWN = "gpsDialogShown";
		public static final String REWARDS_PAGE = "rewardsPage";
		public static final String CUSTOM_LOCATION_SELECTED = "customLocationSelected";
		public static final String CUSTOM_LOCATION_LAT = "customLocationLat";
		public static final String CUSTOM_LOCATION_LONG = "customLocationLong";
		public static final String OPEN_FAVORITES = "openFavorites";
		public static final String CONTACT_METADATA = "contactMetadata";
		public static final String FROM_CENTRAL_TIMELINE = "fromCentralTimeline";
		public static final String BLOCKED_LIST = "blockedList";
		public static final String NUX1_NUMBERS = "nux1Numbers";
		public static final String NUX_NUMBERS_INVITED = "nuxNumbersInvited";
		public static final String FROM_CONVERSATIONS_SCREEN = "fromConversationsScreen";
		public static final String DIALOG_SHOWING = "dialogShowing";
		public static final String SMS_ID = "smsId";
		public static final String RECORDED_TIME = "recordedTime";
		public static final String SHOW_FRIENDS_TUTORIAL = "showFriendsTutorial";
		public static final String FWD_STICKER_ID = "fwdStickerId";
		public static final String FWD_CATEGORY_ID = "fwdCategoryId";
		public static final String POST_TO_TWITTER = "postToTwitter";
	}

	public static final class LogEvent {
		// Common tags for Countly. Don't change.

		public static final String TAG = "tag";
		public static final String DEVICE_ID = "device_id";
		public static final String OS = "_os";
		public static final String OS_VERSION = "_os_version";
		public static final String DEVICE = "_device";
		public static final String RESOLUTION = "_resolution";
		public static final String CARRIER = "_carrier";
		public static final String APP_VERSION = "_app_version";

		/*
		 * Naming Convention - <screen><event><sub-event>
		 */

		/*
		 * Home screen events <screen> = hoS <event> = profS, invS, feedS,
		 * delAC, delC, compB, addSC, creDtiPN, creDtiPY, upDtiPN, upDtipY,
		 * upDOBD, upDOB, smSY, smSN, groupS, dRB
		 */
		public static final String MENU = "hoS";
		public static final String DELETE_ALL_CONVERSATIONS_MENU = "hoSdelAC";
		public static final String DELETE_CONVERSATION = "hoSdelC";
		public static final String COMPOSE_BUTTON = "hoScompB";
		public static final String ADD_SHORTCUT = "hoSaddSC";
		public static final String HOME_TOOL_TIP_CLOSED = "hoScreDtiPN";
		public static final String HOME_TOOL_TIP_CLICKED = "hoScreDtiPY";
		public static final String HOME_UPDATE_TOOL_TIP_CLOSED = "hoSupDtiPN";
		public static final String HOME_UPDATE_TOOL_TIP_CLICKED = "hoSupDtiPY";
		public static final String HOME_UPDATE_OVERLAY_DISMISSED = "hoSupDOBD";
		public static final String HOME_UDPATE_OVERLAY_BUTTON_CLICKED = "hoSupDOB";
		public static final String DEFAULT_SMS_DIALOG_YES = "hoSsmSY";
		public static final String DEFAULT_SMS_DIALOG_NO = "hoSsmSN";
		public static final String DRAWER_BUTTON = "hoSdRB";

		/*
		 * Profile screen events <screen> = profS <event> = proES, credS, notyS,
		 * privS, helpS
		 */
		public static final String EDIT_PROFILE = "profSproES";
		public static final String NOTIFICATION_SCREEN = "profSnotyS";
		public static final String PRIVACY_SCREEN = "profSprivS";

		/*
		 * Invite screen events <screen> = invS <event> = credB, creDtiPN,
		 * creDtiPY
		 */
		public static final String INVITE_TOOL_TIP_CLOSED = "invScreDtiPN";
		public static final String INVITE_TOOL_TIP_CLICKED = "invScreDtiPY";
		public static final String CREDIT_TOP_BUTTON = "invScredB";

		/*
		 * Chat thread screen events <screen> = chatS <event> = inVtiPN,
		 * inVtopB, blocK, forMsg, infoB, invOB, invOBD, opTiNtaP, calL, adD,
		 * grPinfO
		 */
		public static final String CHAT_INVITE_TOOL_TIP_CLOSED = "chatSinVtiPN";
		public static final String CHAT_INVITE_TOP_BUTTON = "chatSinVtopB";
		public static final String MENU_BLOCK = "chatSblocK";
		public static final String FORWARD_MSG = "chatSforMSG";
		public static final String I_BUTTON = "chatSinfoB";
		public static final String INVITE_OVERLAY_BUTTON = "chatSinvOB";
		public static final String INVITE_OVERLAY_DISMISS = "chatSinvOBD";
		public static final String OPT_IN_TAP_HERE = "chatSopTiNtaP";
		public static final String MENU_CALL = "chatScalL";
		public static final String MENU_ADD_TO_CONTACTS = "chatSadD";
		public static final String GROUP_INFO_TOP_BUTTON = "chatSgrPinfO";

		/*
		 * Credits screen events <screen> = credS <event> = inVB
		 */
		public static final String INVITE_BUTTON_CLICKED = "credSinVB";

		/*
		 * Group Info screen <screen> = groupS <event> = adDparT
		 */
		public static final String ADD_PARTICIPANT = "groupSadDparT";

		/*
		 * SignUp screen <screen> = signupS <event> = erroR
		 */
		public static final String SIGNUP_ERROR = "signupSerroR";

		/*
		 * Drawer screen <screen> = drS <event> = homE, gC, inV, reW, creD,
		 * proF, settinG
		 */
		public static final String DRAWER_HOME = "drShomE";
		public static final String DRAWER_GROUP_CHAT = "drSgC";
		public static final String DRAWER_INVITE = "drSinV";
		public static final String DRAWER_REWARDS = "drSreW";
		public static final String DRAWER_CREDITS = "drScreD";
		public static final String DRAWER_PROFILE = "drSproF";
		public static final String DRAWER_SETTINGS = "drSsettinG";

		/*
		 * Rewards screen <screen> = rewS <event> = inV, clM, faQ
		 */
		public static final String REWARDS_INVITE = "rewSinV";
		public static final String REWARDS_CLAIM = "rewSclM";
		public static final String REWARDS_FAQ = "rewSfaQ";

		/*
		 * Help screen <screen> = helpS <event> = conT, faQ
		 */
		public static final String HELP_CONTACT = "helpSconT";
		public static final String HELP_FAQ = "helpSfaQ";

	}

	public static final class MqttMessageTypes {
		public static final String MESSAGE_READ = "mr";
		public static final String MESSAGE = "m";
		public static final String SMS_CREDITS = "sc";
		public static final String DELIVERY_REPORT = "dr";
		public static final String USER_JOINED = "uj";
		public static final String USER_LEFT = "ul";
		public static final String START_TYPING = "st";
		public static final String END_TYPING = "et";
		public static final String INVITE = "i";
		public static final String ICON = "ic";
		public static final String INVITE_INFO = "ii";
		public static final String GROUP_CHAT_JOIN = "gcj";
		public static final String GROUP_CHAT_LEAVE = "gcl";
		public static final String GROUP_CHAT_END = "gce";
		public static final String GROUP_CHAT_NAME = "gcn";
		public static final String ANALYTICS_EVENT = "le";
		public static final String UPDATE_AVAILABLE = "ua";
		public static final String ACCOUNT_INFO = "ai";
		public static final String REQUEST_ACCOUNT_INFO = "rai";
		public static final String USER_OPT_IN = "uo";
		public static final String BLOCK_INTERNATIONAL_SMS = "bis";
		public static final String ADD_FAVORITE = "af";
		public static final String REMOVE_FAVORITE = "rf";
		public static final String MUTE = "mute";
		public static final String UNMUTE = "unmute";
		public static final String GROUP_CHAT_KICK = "gck";
		public static final String ACCOUNT_CONFIG = "ac";
		public static final String REWARDS = "rewards";
		public static final String DISPLAY_PIC = "dp";
		public static final String STATUS_UPDATE = "su";
		public static final String ACTION = "action";
		public static final String DELETE_STATUS = "dsu";
		public static final String POSTPONE_FAVORITE = "pf";
		public static final String BATCH_STATUS_UPDATE = "bsu";
		public static final String FORCE_SMS = "fsms";
		public static final String STICKER = "stk";
		public static final String APP_STATE = "app";
		public static final String LAST_SEEN = "ls";
		public static final String SERVER_TIMESTAMP = "sts";
		public static final String REQUEST_SERVER_TIMESTAMP = "rsts";
		public static final String PROTIP = "pt";
	}

	public static final class SMSNative {
		/*
		 * SMS URIs
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://sms");

		public static final Uri INBOX_CONTENT_URI = Uri.withAppendedPath(
				CONTENT_URI, "inbox");

		public static final Uri SENTBOX_CONTENT_URI = Uri.withAppendedPath(
				CONTENT_URI, "sent");

		public static final String NUMBER = "address";
		public static final String DATE = "date";
		public static final String MESSAGE = "body";
		public static final String READ = "read";

	}

	public static final class SocialPostResponse {
		public static final String NO_TOKEN = "notoken";
		public static final String INVALID_TOKEN = "invalidtoken";
		public static final String FAILURE = "failure";
		public static final String SUCCESS = "success";
	}

	public static enum FTResult {
		SUCCESS, UPLOAD_FAILED, FILE_TOO_LARGE, READ_FAIL, DOWNLOAD_FAILED, CANCELLED, FILE_EXPIRED
	}

	public static enum SMSSyncState {
		SUCCESSFUL, NO_CHANGE, UNSUCCESSFUL
	}

	public static enum TipType {
		EMOTICON, STICKER, WALKIE_TALKIE, LAST_SEEN, STATUS, MOOD
	}

	public static final int[] INVITE_STRINGS = { R.string.native_sms_invite_1,
			R.string.native_sms_invite_2, R.string.native_sms_invite_3 };

	public static final String[] BOLLYWOOD_COUNTRY_CODES = { "+91", "+94",
			"+880", "+977", "+93", "+92", "+975", "+960", "+968", "+966",
			"+961", "+962", "+965", "+973", "+971", "+974" };
}
