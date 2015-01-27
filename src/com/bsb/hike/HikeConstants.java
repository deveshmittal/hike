package com.bsb.hike;

import com.bsb.hike.models.StatusMessage.StatusMessageType;

import android.net.Uri;
import android.os.Environment;

public class HikeConstants
{
	public static final String APP_PUSH_ID = "297726274089";

	public static final String APP_FACEBOOK_ID = "425850510764995";

	public static final String APP_TWITTER_ID = "7LFaGIe5QXj05WN1YDDVaA";

	public static final String APP_TWITTER_SECRET = "LhgJVQ9eAmbb3EGdXpLD8B4RHf9SGPrzSqaOjuKL5o4";

	public static final String MAP_API_KEY_DEBUG = "0Luu6V6IYSC0UpLSUZe7oO-bvd392OgrXSnY8aA";

	public static final String MAP_API_KEY_PROD = "0Luu6V6IYSC3olBhRm5jHLKDIn5_CA3P17l_3Mw";

	public static final String MA_TRACKER_AD_ID = "9198";

	public static final String MA_TRACKER_KEY = "00774f47f97b5173432b958a986f0061";

	public static final String MA_TRACKER_USERID = "bsb_hike";

	public static final String MA_TRACKER_REF_ID_PREFIX = "bsb_hike_";

	public static final String MA_SITE_ID = "27576";

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
	
	public static final String PIN_MESSAGE = "pin";

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

	public static final String CRITICAL_UPDATE_KEY = "critical";

	public static final String INVITE_TOKEN = "invite_token";

	public static final String TOTAL_CREDITS_PER_MONTH = "tc";

	public static final String DND_NUMBERS = "dndnumbers";

	public static final String SOURCE = "source";

	public static final String FILES = "files";

	public static final String CONTENT_TYPE = "ct";

	public static final String THUMBNAIL = "tn";

	public static final String SOURCE_FILE_PATH = "srcPath";

	public static final String FILE_NAME = "fn";

	public static final String FILE_KEY = "fk";

	public static final String FILE_PATH = "fp";
	
	public static final String DOWNLOAD_FILE_URL_KEY = "url";

	public static final String CREDITS = "credits";

	public static final String ON_HIKE = "onhike";

	public static final String DND = "dnd";

	public static final String DND_USERS = "dndUsers";

	public static final String LATITUDE = "lat";

	public static final String LONGITUDE = "long";

	public static final String ZOOM_LEVEL = "zoom";

	public static final String ADDRESS = "add";

	public static final String POKE = "poke";
	
	public static final String IS_GHOST = "is_ghost";
	
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
	
	public static final String MQTT_IP_ADDRESSES = "ip";

	public static final String SHOW_REWARDS = "show_rewards";

	public static final String REWARDS = "rewards";

	public static final String GAMES_TOKEN = "games_token";

	public static final String SHOW_GAMES = "show_games";

	public static final String GAMES = "games";

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

	public static final String POST_INFO = "postinfo";

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

	public static final String UPDATE_VERSION = "version";

	public static final String DEVICE_VERSION = "deviceversion";

	public static final String CRICKET_MOODS = "cmoods";

	public static final String COUNT = "c";
	
	public static final String UPDATED_SIZE = "s";
	
	public static final String PALLETE_POSITION = "idx";

	public static final String DEFAULT_SMS_CLIENT_TUTORIAL = "dsctutorial";

	public static final String STICKER_IDS = "stIds";

	public static final String STICKER = "stk";

	public static final String RESOLUTION_ID = "resId";

	public static final String NUMBER_OF_STICKERS = "nos";
	
	public static final String DOWNLOAD_SOURCE = "dsrc";
	
	public static final String TOTAL_STICKERS = "totalStickers";

	public static final String DATA_2 = "data";

	public static final String ADD_STICKER = "addStk";

	public static final String REMOVE_STICKER = "remStk";

	public static final String REMOVE_CATEGORY = "remCat";

	public static final String DISABLED_ST = "disabled";

	public static final String STATUS = "stat";

	public static final String OK = "ok";

	public static final String REACHED_STICKER_END = "st";

	public static final String PLAYTIME = "pt";

	public static final String FOREGROUND = "fg";

	public static final String BACKGROUND = "bg";

	public static final String JUST_OPENED = "justOpened";

	public static final String BULK_LAST_SEEN = "bulklastseen";

	public static final String LAST_SEEN = "ls";

	public static final String LAST_SEEN_SETTING = "lastseen";

	public static final String UJ_NOTIF_SETTING = "ujn";

	public static final String BULK_LAST_SEEN_KEY = "lastseens";

	public static final String PROTIP_HEADER = "h";

	public static final String PROTIP_TEXT = "t";

	public static final String PROTIP_IMAGE_URL = "img";

	public static final String PROTIP_WAIT_TIME = "wt";

	public static final String PROTIP_GAME_DOWNLOAD_URL = "url";

	public static final String PROTIP_SHOW_PUSH = "fp";

	public static final String NO_SMS = "nosms";

	public static final String RETURNING_USER = "ru";

	public static final String NUMBER_OF_SMS = "no_of_sms";

	public static final String OFFLINE = "offline";

	public static final String SENDER = "sender";

	public static final String RECIPIENT = "recipient";

	public static final String IS_H2H = "is_h2h";

	public static final String NATIVE_SMS = "native_sms";

	public static final String UNIFIED_INBOX = "unified_inbox";

	public static final String PULL_OLD_SMS = "pull_old_sms";

	public static final String TIP_ID = "tip_id";

	public static final String TIP_URL = "tip_url";

	public static final String SMS = "sms";

	public static final String UI_EVENT = "uiEvent";

	public static final String CRC_EVENT = "crc";

	public static final String MD5_HASH = "md5";

	public static final String FILE_SIZE = "fs";

	public static final String DOWNLOAD = "dwnld";

	public static final String LOGEVENT_TAG = "mob";

	public static final String FREE_SMS_ON = "free_sms_on";

	public static final String NATIVE_INVITES = "nativeinvites";

	public static final String PREF = "pref";

	public static final String CONTACTS = "contacts";

	public static final String SENDBOT = "sendbot";

	public static final String EVENT_TYPE = "et";

	public static final String EVENT_KEY = "ek";

	public static final String PACKAGE = "pkg";

	public static final String POPUP_SUBTYPE = "pst";

	public static final String SIGNUP_IC = "signupIc";

	public static final String DEVICE_KEY = "device_key";

	public static final String ENABLE_FREE_INVITES = "enable_free_invites";

	public static final String SHOW_FREE_INVITES = "show_free_invites";

	public static final String FREE_INVITE_POPUP_TITLE = "free_invite_popup_title";

	public static final String FREE_INVITE_POPUP_TEXT = "free_invite_popup_text";

	public static final String LIST = "list";

	public static final String BG_ID = "bg_id";

	public static final String CHAT_BACKGROUNDS = "cbgs";

	public static final String CHAT_BACKGROUND = "cbg";

	public static final String CHAT_BACKGROUD_NOTIFICATION = "pushcbg";

	public static final String RESOLUTION = "res";

	public static final String OPERATOR = "op";

	public static final String CIRCLE = "circle";

	public static final String PIXEL_DENSITY_MULTIPLIER = "pdm";

	public static final String CUSTOM = "custom";

	public static final String SOUND_PREF = "soundPref";

	public static final String VIBRATE_PREF = "vibratePref";

	public static final String HIKE_JINGLE_PREF = "jinglePref";

	public static final String NOTIF_SOUND_PREF = "notifSoundPref";

	public static final String VIBRATE_PREF_LIST = "vibratePrefList";

	public static final String TICK_SOUND_PREF = "tickSoundPref";

	public static final String FREE_SMS_PREF = "freeSmsPref";

	public static final String LED_PREF = "ledPref";
	
	public static final String COLOR_LED_PREF = "colorLedPref";

	public static final String NATIVE_JINGLE_PREF = "jinglePref";

	public static final String SSL_PREF = "sslPref";

	public static final String STATUS_PREF = "statusPref";

	public static final String SEND_SMS_PREF = "sendSmsPref";

	public static final String RECEIVE_SMS_PREF = "receiveSmsPref";

	public static final String SEND_UNDELIVERED_AS_NATIVE_SMS_PREF = "sendUndeliveredAsNativeSmsPref";

	public static final String LAST_SEEN_PREF = "lastSeenPref";
	
	public static final String PROFILE_PIC_PREF = "profilePicPref";

	public static final String CHAT_BG_NOTIFICATION_PREF = "chatBgNotificationPref";

	public static final String RESET_STEALTH_PREF = "resetStealthPref";
	
	public static final String CHANGE_STEALTH_PASSCODE = "changeStealthPasscode";

	public static final String SEND_UNDELIVERED_AS_NATIVE_PREF = "sendUndeliveredAsNativePref";

	public static final String SEND_UNDELIVERED_ALWAYS_AS_SMS_PREF = "sendUndeliveredAlwaysAsSmsPref";

	public static final String REQUEST_DP = "rdp";

	public static final String C_TIME_STAMP = "cts";
	
	public static final String HEADER = "h";
	
	public static final String BODY = "b";
	
	public static final String MESSAGES = "msgs";
	
	public static final String FILE_IMAGE_QUALITY = "img_quality";
	
	// @GM
	// public static final String AUTO_DOWNLOAD_IMAGE_PREF = "autoDownloadImagePref"
	public static final String AUTO_DOWNLOAD_MEDIA_PREF = "AutoDownloadMediaPref";

	public static final String MD_AUTO_DOWNLOAD_IMAGE_PREF = "mdAutoDownloadImagePref";

	public static final String MD_AUTO_DOWNLOAD_AUDIO_PREF = "mdAutoDownloadAudioPref";

	public static final String MD_AUTO_DOWNLOAD_VIDEO_PREF = "mdAutoDownloadVideoPref";

	public static final String WF_AUTO_DOWNLOAD_IMAGE_PREF = "wfAutoDownloadImagePref";

	public static final String WF_AUTO_DOWNLOAD_AUDIO_PREF = "wfAutoDownloadAudioPref";

	public static final String WF_AUTO_DOWNLOAD_VIDEO_PREF = "wfAutoDownloadVideoPref";
	
	public static final String IMAGE_QUALITY = "imageQuality";
	
	public static final String REMEMBER_IMAGE_CHOICE = "rememberImageChoice";
	
	public static final String SHOW_IMAGE_QUALITY_TIP = "showImageQualityTip";

	public static final String HIKEBOT = "TD-HIKE";

	public static final String HIKEBOT_CONV_STATE = "isHikeBotConvState";

	public static final String FTUE_TEAMHIKE_MSISDN = "+hike+";

	public static final String FTUE_HIKEBOT_MSISDN = "+hike1+";

	public static final String FTUE_GAMING_MSISDN = "+hike2+";

	public static final String FTUE_HIKE_DAILY = "+hike3+";
	
	public static final String FTUE_HIKE_SUPPORT = "+hike4+";

	public static final String BACKUP_PREF = "backupAccount";

    public static final String NUX_BOT = "+hike5+";

    public static final String NUX = "nux";

    public static final String STICKER_NUX = "stkNux";

	public static final String UNLINK_PREF = "unlinkAccount";

	public static final String DELETE_PREF = "deleteAccount";

	public static final String UNLINK_FB = "unlinkFacebook";

	public static final String UNLINK_TWITTER = "unlinkTwitter";

	public static final String BLOKED_LIST_PREF = "blockedList";

	public static final String SYSTEM_HEALTH_PREF = "systemHealth";

	public static final String HELP_FAQS_PREF = "helpFaqs";

	public static final String HELP_FEEDBACK_PREF = "helpFeedback";

	public static final String STATUS_BOOLEAN_PREF = "statusBooleanPref";
	
	public static final String NUJ_NOTIF_BOOLEAN_PREF = "hikeNUJNotificationPref";
	
	public static final String H2O_NOTIF_BOOLEAN_PREF = "hikeOfflineNotificationPref";

	public static final String UPGRADE_AVATAR_PROGRESS_USER = "upgradeAvtarProgressUser";
	
	public static final String UPGRADING = "upgrading";

	public static final String UPGRADE_MSG_HASH_GROUP_READBY = "upgradeMsgHashGroupReadby";
	
	public static final String UPGRADE_FOR_DATABASE_VERSION_28 = "upgradeForDatabaseVersion28";

	public static final String UPGRADE_AVATAR_CONV_DB = "upgradeAvtarProgressConv";

	public static final String NO_CALL_ALERT_CHECKED = "noCallAlertChecked";

	public static final String CRITICAL_STRING = "critical";

	public static final String OPERATOR_SMS_ALERT_CHECKED = "opSmsAlertChecked";

	public static final String SINGLE_INVITE_SMS_ALERT_CHECKED = "singleSmsAlertChecked";
	
	public static final String HIKE_CONTACTS_COUNT = "hikeContactsCount";
	
	public static final String SHOW_NUX_SCREEN = "show_nux_screen";
		
	public static final String NUX_STICKER_DETAILS = "nuxStickerDetails";
	
	public static final String FTUE_ADD_SMS_ALERT_CHECKED = "ftueSmsAlertChecked";

	public static final String IS_GAMES_ITEM_CLICKED = "isGamesItemClicked";

	public static final String IS_REWARDS_ITEM_CLICKED = "isRewardsItemClicked";
	
	public static final String IS_HOME_OVERFLOW_CLICKED = "isHomeOverflowClicked";

	public static final String SHOW_RECENTLY_JOINED_DOT = "showRecentlyJoinedDot";

	public static final String SHOW_TIMELINE_RED_DOT = "showTimelineRedDot";
	
	public static final String IS_OF_ICON_CLICKED = "isOfIconClicked";

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
	
	public static final String AVATAR = "avatar";

	/* Constant used to name the preference file which saves the drafts */
	public static final String DRAFT_SETTING = "draftSetting";

	public static final int CONNECT_TIMEOUT = 6 * 1000;

	public static final int SOCKET_TIMEOUT = 30 * 1000;

	/* how long to wait between sending publish and receiving an acknowledgement */
	public static final long MESSAGE_DELIVERY_TIMEOUT = 5 * 1000;

	/* how long to wait for a ping confirmation */
	public static final long PING_TIMEOUT = 5 * 1000;

	/*
	 * how long to wait to resend message. This should significantly greathr than PING_TIMEOUT
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
	public static final int PIN_CAPTURE_TIME = 10 * 1000;

	public static final int CALL_ME_WAIT_TIME = 150 * 1000;

	public static final int NETWORK_ERROR_POP_UP_TIME = 120 * 1000;

	public static final int BACKUP_RESTORE_UI_DELAY = 3 * 1000; 

	public static final int HIKE_SYSTEM_NOTIFICATION = 0;

	public static final String ADAPTER_NAME = "hikeadapter";

	/* constants for defining what to do after checking for updates */
	public static final int NORMAL_UPDATE = 2;

	public static final int CRITICAL_UPDATE = 1;

	public static final int NO_UPDATE = 0;

	// More explanation required?
	public static final int NUM_TIMES_SCREEN_SHOULD_OPEN_BEFORE_TOOL_TIP = 2;

	public static final String APP_API_VERSION = "3";

	public static final int NUM_SMS_PER_FRIEND = 10;

	public static final int INITIAL_NUM_SMS = 100;

	public static final int MAX_CHAR_IN_NAME = 20;

	public static final int MAX_CONTACTS_IN_GROUP = 100;

	public static final int MAX_SMS_CONTACTS_IN_GROUP = MAX_CONTACTS_IN_GROUP;

	public static final int PROFILE_IMAGE_DIMENSIONS = 120;

	public static final int SIGNUP_PROFILE_IMAGE_DIMENSIONS = 200;

	public static final String VALID_MSISDN_REGEX = "\\+?[0-9]{1,15}";

	public static final int MAX_BUFFER_SIZE_KB = 100;

	public static final int MAX_FILE_SIZE = 100 * 1024 * 1024;

	public static final int IMAGE_CAPTURE_CODE = 1187;

	public static final int IMAGE_TRANSFER_CODE = 1188;

	public static final int VIDEO_TRANSFER_CODE = 1189;

	public static final int AUDIO_TRANSFER_CODE = 1190;

	public static final int SHARE_LOCATION_CODE = 1192;

	public static final int SHARE_CONTACT_CODE = 1193;

	public static final int SHARE_FILE_CODE = 1194;

	public static final int FACEBOOK_REQUEST_CODE = 64206;

	public static final int MAX_DURATION_RECORDING_SEC = 360;

	public static final int MAX_DIMENSION_THUMBNAIL_PX = 270;

	public static final int MAX_DIMENSION_LOCATION_THUMBNAIL_PX = 220;

	public static final int MAX_DIMENSION_FULL_SIZE_PROFILE_PX = 500;

	public static final int MAX_DIMENSION_MEDIUM_FULL_SIZE_PX = 800;
	
	public static final int MAX_DIMENSION_LOW_FULL_SIZE_PX = 600;

	public static final int INITIAL_PROGRESS = 5;

	public static final int NO_CHANGE = 0;

	public static final int PARTICIPANT_STATUS_CHANGE = 1;

	public static final int NEW_PARTICIPANT = 2;

	public static final String MAIL = "support@hike.in";
	
	public static final int MAX_RECENTS_TO_SHOW = 10;
	
	public static final int MAX_RECENTLY_JOINED_HIKE_TO_SHOW = 5;
	
	// Had to add this constant since its only available in the android API for
	// Honeycomb and higher.
	public static final int FLAG_HARDWARE_ACCELERATED = 16777216;

	public static final int LOCAL_CLEAR_TYPING_TIME = 6 * 1000;

	public static final int RESEND_TYPING_TIME = 4 * 1000;

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
	 * Maximum number of conversations to be made automatically when the user signs up
	 */
	public static final int MAX_CONVERSATIONS = 6;

	/*
	 * Constant used as a type to signify that this message was added locally by the client when the user signed up
	 */
	public static final String INTRO_MESSAGE = "im";

	public static final String LOCATION_CONTENT_TYPE = "hikemap/location";

	public static final String LOCATION_FILE_NAME = "Location";

	public static final String CONTACT_CONTENT_TYPE = "contact/share";

	public static final String CONTACT_FILE_NAME = "Contact";

	public static final int DEFAULT_ZOOM_LEVEL = 16;

	public static final String VOICE_MESSAGE_CONTENT_TYPE = "audio/voice";

	// Picasa URI start for JB devices
	public static final String JB_PICASA_URI_START = "content://com.sec.android.gallery3d";

	// Picasa URI start for other devices
	public static final String OTHER_PICASA_URI_START = "content://com.google.android.gallery3d";

	// Picasa URI prefix for creating convMessage
	public static final String PICASA_PREFIX = "picasaUri:";

	// Gmail URI prefix for all devices
	public static final String GMAIL_PREFIX = "content://gmail-ls";

	// Google Plus URI prefix for all devices
	public static final String GOOGLE_PLUS_PREFIX = "content://com.google.android.apps.photos.content";

	public static final int MAX_MESSAGES_TO_LOAD_INITIALLY = 40;

	public static final int MAX_PINS_TO_LOAD_INITIALLY = 20;

	public static final int MAX_OLDER_PINS_TO_LOAD_EACH_TIME = 10;

	public static final int MAX_OLDER_MESSAGES_TO_LOAD_EACH_TIME = 20;

	public static final int MIN_INDEX_TO_LOAD_MORE_MESSAGES = 10;

	public static final int MAX_STATUSES_TO_LOAD_INITIALLY = 30;

	public static final int MAX_OLDER_STATUSES_TO_LOAD_EACH_TIME = 20;

	public static final int SHOW_CREDITS_AFTER_NUM = 10;

	public static final String HIKE_MEDIA_DIRECTORY_ROOT = Environment.getExternalStorageDirectory() + "/Hike/Media";

	public static final String HIKE_BACKUP_DIRECTORY_ROOT = Environment.getExternalStorageDirectory() + "/Hike/Backup";

	public static final String PROFILE_ROOT = "/hike Profile Images";

	public static final String IMAGE_ROOT = "/hike Images";

	public static final String VIDEO_ROOT = "/hike Videos";

	public static final String AUDIO_ROOT = "/hike Audios";

	public static final String AUDIO_RECORDING_ROOT = "/hike Voice Messages";

	public static final String OTHER_ROOT = "/hike Others";

	public static final String SENT_ROOT = "/sent";

	public static final String STICKERS_ROOT = "/stickers";

	public static final String LARGE_STICKER_FOLDER_NAME = "stickers_l";

	public static final String SMALL_STICKER_FOLDER_NAME = "stickers_s";
	
	public static final String OLD_LARGE_STICKER_FOLDER_NAME = "large";

	public static final String OLD_SMALL_STICKER_FOLDER_NAME = "small";
	
	public static final String LARGE_STICKER_ROOT = "/"+LARGE_STICKER_FOLDER_NAME;

	public static final String SMALL_STICKER_ROOT = "/"+SMALL_STICKER_FOLDER_NAME;

	public static final String HIKE_FILE_LIST_NAME = "hikeFiles";

	public static final String STATUS_MESSAGE_HEADER = "hike-status-message";
	
	public static final String PIN_HISTORY_FRAGMENT_TAG = "pin-history-fragment";

	public static final String VOIP_CALL_RATE_FRAGMENT_TAG = "voipCallRateFragmentTag";

	/*
	 * Contact Type
	 */
	public static final int ON_HIKE_VALUE = 1;

	public static final int NOT_ON_HIKE_VALUE = 0;

	public static final int BOTH_VALUE = -1;

	public static final String INDIA_COUNTRY_CODE = "+91";

	public static final String SAUDI_ARABIA_COUNTRY_CODE = "+966";

	public static final int MDPI_TIMES_10 = 11;

	public static final String NAMESPACE = "http://schemas.android.com/apk/res/com.bsb.hike";

	public static final String FONT = "font";
	
	public static final String MAX_LINES = "maxLines";

	public static final int MAX_MESSAGE_PREVIEW_LENGTH = 300;

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
	public static final int DEFAULT_UNDELIVERED_WAIT_TIME = 30;

	public static final int MAX_FALLBACK_NATIVE_SMS = 19;

	public static final int MAX_SMS_PULL_IN_INBOX = 2000;

	public static final int MAX_SMS_PULL_IN_SENTBOX = 1000;

	public static final String MICROMAX = "MICROMAX";

	public static final int MAX_READ_BY_NAMES = 4;

	public static final int LDPI_ID = 4;

	public static final int MDPI_ID = 3;

	public static final int HDPI_ID = 2;

	public static final int XHDPI_ID = 1;

	public static final int XXHDPI_ID = 0;
	
	public static final int XXXHDPI_ID = 12;

	public static final int LDPI_CACHE = 15;

	public static final int MDPI_CACHE = 20;

	public static final int HDPI_CACHE = 25;

	public static final int XHDPI_CACHE = 30;

	public static final int XXHDPI_CACHE = 30;

	public static final int EMPTY_CONVERSATIONS_PREFILL_LIMIT = 6;

	public static final int FTUE_LIMIT = 5;
	
	public static final int FTUE_CONTACT_CARD_LIMIT = 5;

	public static final String FTUE_MSISDN_TYPE = "ftueContact";

	public static final double PROFILE_PIC_FREE_SPACE = 3 * 1024 * 1024;

	// LED light Notifications constants
	public static final int LED_LIGHTS_ON_MS = 300;

	public static final int LED_LIGHTS_OFF_MS = 1000;

	public static final int JOINED_HIKE_STATUS_ID = -1;

	public static final int SELECT_COUNTRY_REQUEST_CODE = 4001;

	public static final int MAX_VELOCITY_FOR_LOADING_IMAGES_SMALL = 25;

	public static final int MAX_VELOCITY_FOR_LOADING_IMAGES = 10;

	public static final int MAX_VELOCITY_FOR_LOADING_TIMELINE_IMAGES = 5;

	public static final String SCREEN = "screen";

	public static final String FTUE = "ftue";

	public static final int MAX_FAST_SCROLL_VISIBLE_POSITION = 6;

	public static final int STEALTH_OFF = 0;

	public static final int STEALTH_ON_FAKE = 1;

	public static final int STEALTH_ON = 2;

	public static final long RESET_COMPLETE_STEALTH_TIME_MS = 30 * 60 * 1000;
	
	public static final String ENABLED = "enabled";
	
	public static final String ENABLED_STEALTH = "en";

	public static final String DISABLED_STEALTH = "di";
	
	public static final String RESET = "reset";
	
	public static final String STEALTH = "stlth";

	public static final String FILE_SHARE_PREFIX = "file://";

	public static final String SHARE_CONTACT_CONTENT_TYPE = "text/x-vcard";

	public static final int MAX_LAST_SEEN_RETRY_COUNT = 3;

	public static final int RETRY_WAIT_ADDITION = 2;

	public static final String IMAGE_FRAGMENT_TAG = "imageFragmentTag";
	
	public static final String SHOW_STEALTH_POPUP = "stlthrmd";
	
	public static final String SHOW_FESTIVE_POPUP = "showFestivePopup";

	public static final String REPUBLIC_DAY_POPUP = "indiarepublic";

	public static final String HIKE_OFFLINE_NOTIFICATION_PREF = "hikeOfflineNotificationPref";


	public static final String PIN = "pin";

	public static final String UNREAD_COUNT = "unreadCount";

	public static final String TO_SHOW = "toShow";
	
	public static final String PIN_DISPLAYED = "displayed";
	
	public static final int MAX_MEDIA_ITEMS_TO_LOAD_INITIALLY = 51;
	
	
	public static final String TOTAL_SELECTIONS = "t";
	
	public static final String SUCCESSFUL_SELECTIONS = "s";
	
	public static final String DRAWABLE = "drawable";
	
	public static final String NUX_STICKERS = "stickers";
	
	public static final String SHOW_NUX_INVITE_MODE = "showNuxInviteMode";

	public static final String NUX_HOME_INVITE_FOOTER = "nuxHomeInviteFooter";

	public static final String NUX_INVITE_FORWARD = "nuxInviteForward";
	
	public static final String SELF_HIKE_ID = "-1";

	public static final String VOIP_CALL_DURATION = "vcd";

	public static final String VOIP_CALL_INITIATOR = "vci";
	
	public static final String VOIP_BITRATE_2G = "vb2g";
	
	public static final String VOIP_BITRATE_3G = "vb3g";
	
	public static final String VOIP_BITRATE_WIFI = "vbw";

	public static final String VOIP_ACTIVATED = "voip";

	public static final String VOIP_CALL_RATE_POPUP_SHOW = "vrmcs";

	public static final String VOIP_CALL_RATE_POPUP_FREQ = "vrmcf";

	public static final String VOIP_RELAY_SERVER_PORT = "rsport";

	public static final class ResultCodes
	{
		public static final int SELECT_COUNTRY = 4001;
		
		public static final int CREATE_LOCK_PATTERN = 4002;

		public static final int CONFIRM_LOCK_PATTERN = 4003;
		
		public static final int CONFIRM_AND_ENTER_NEW_PASSWORD = 4004;
	}

	public static final class Extras
	{
		public static final String MSISDN = "msisdn";

		public static final String NAME = "name";

		public static final String PREV_MSISDN = "prevMsisdn";

		public static final String PREV_NAME = "prevName";

		public static final String INVITE = "invite";

		public static final String MSG = "msg";
		public static final String POKE = "poke";

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

		public static final String RESTORE_STATUS = "restoreStatus";

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

		public static final String APPLICATIONSPUSH_MESSAGE = "applicationsPushMessage";

		public static final String URL_TO_LOAD = "urlToLoad";

		public static final String TITLE = "title";
		
		public static final String WEBVIEW_ALLOW_LOCATION = "allocLoc";

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
		
		public static final String CONTACT_INFO_TIMELINE = "contactInfoTimeline";

		public static final String CONTACT_ID = "contactId";

		public static final String ON_HIKE = "onHike";

		public static final String SHOWING_SECOND_LOADING_TXT = "showingSecondLoadingTxt";

		public static final String FACEBOOK_POST_POPUP_SHOWING = "facebookPostPopupShowing";

		public static final String GPS_DIALOG_SHOWN = "gpsDialogShown";

		public static final String REWARDS_PAGE = "rewardsPage";

		public static final String GAMES_PAGE = "gamesPage";

		public static final String CUSTOM_LOCATION_SELECTED = "customLocationSelected";

		public static final String CUSTOM_LOCATION_LAT = "customLocationLat";

		public static final String CUSTOM_LOCATION_LONG = "customLocationLong";

		public static final String OPEN_FAVORITES = "openFavorites";

		public static final String CONTACT_METADATA = "contactMetadata";

		public static final String FROM_CENTRAL_TIMELINE = "fromCentralTimeline";

		public static final String BLOCKED_LIST = "blockedList";

		public static final String NUX1_NUMBERS = "nux1Numbers";

		public static final String NUX_NUMBERS_INVITED = "nuxNumbersInvited";

		public static final String DIALOG_SHOWING = "dialogShowing";

		public static final String SMS_ID = "smsId";

		public static final String RECORDED_TIME = "recordedTime";

		public static final String SHOW_FRIENDS_TUTORIAL = "showFriendsTutorial";

		public static final String POST_TO_TWITTER = "postToTwitter";

		public static final String RECORDING_TIME = "recordingTime";

		public static final String MAPPED_ID = "mappedId";

		public static final String IS_STATUS_IMAGE = "isStatusImage";

		public static final String URL = "url";

		public static final String TAB_INDEX = "tabIndex";

		public static final String IS_TEXT_SEARCH = "isTextSearch";

		public static final String HTTP_SEARCH_STR = "searchStr";

		public static final String IS_FACEBOOK = "isFacebook";

		public static final String EMOTICON_TYPE = "emoticonType";

		public static final String CREATE_GROUP = "createGroup";

		public static final String FROM_CREDITS_SCREEN = "fromCreditsScreen";

		public static final String SHOW_KEYBOARD = "ShowKeyboard";

		public static final String IS_HOME_POPUP_SHOWING = "isHomePopupShowing";

		public static final String LAST_UPDATE_PACKET_ID = "lastUpdatePacketId";

		public static final String LAST_APPLICATION_PUSH_PACKET_ID = "lastApplicationPushPacketId";

		public static final String FREE_SMS_POPUP_BODY = "freeSMSPopupBody";

		public static final String FREE_SMS_POPUP_HEADER = "freeSMSPopupHeader";

		public static final String SHOW_STICKER_TIP_FOR_EMMA = "showStickerTipForEmma";

		public static final String FROM_CHAT_THEME_FTUE = "fromChatThemeFtue";

		public static final String NEW_USER = "newUser";

		public static final String CHAT_THEME_WINDOW_OPEN = "chatThemeWindowOpen";

		public static final String SELECTED_THEME = "selectedTheme";

		public static final String BLOKING_TASK_TYPE = "blockingTaskType";

		public static final String MULTIPLE_MSG_OBJECT = "multipleMsgObject";

		public static final String SELECTED_BUCKET = "selectedBucket";

		public static final String GALLERY_SELECTIONS = "gallerySelections";

		public static final String FILE_PATHS = "filePaths";

		public static final String IS_ACTION_MODE_ON = "isActionModeOn";

		public static final String SELECTED_POSITIONS = "selectedPositions";

		public static final String SELECTED_NON_FORWARDABLE_MSGS = "selectedNonForwadableMsgs";

		public static final String SELECTED_NON_TEXT_MSGS = "selectedNonTextMsgs";

		public static final String SELECTED_CANCELABLE_MSGS = "selectedCancelableMsgs";

		public static final String SELECTED_SHARABLE_MSGS_COUNT = "selectedCancelableMsgsCount";
		
		public static final String SELECTED_SHARABLE_MSGS_PATH = "selectedCancelableMsgsPath";

		public static final String SELECTED_COUNTRY = "selectedCountry";

		public static final String MANUAL_SYNC = "manualSync";

		public static final String IS_FTUT_ADD_FRIEND_POPUP_SHOWING = "isFtueAddFriendPopup";

		public static final String GROUP_NAME = "groupName";

		public static final String GROUP_ID = "groupId";

		public static final String SHOWING_INVALID_PIN_ERROR = "showingInvalidPinError";

		public static final String CALLED_FROM_FTUE_POPUP = "calledFromFtuePopUP";

		public static final String FRIENDS_LIST_COUNT = "friendsListCount";

		public static final String HIKE_CONTACTS_COUNT = "hikeContactsCount";

		public static final String RECOMMENDED_CONTACTS_COUNT = "recommendedContactsCount";

		public static final String SELECTED_SHARABLE_MSGS_MIME_TYPE = "selectedCancelableMsgsMimeType";

		public static final String FROM_NOTIFICATION = "fromNotification";
		
		public static final String LAST_STEALTH_POPUP_ID = "lastStealthPopupId";
		
		public static final String STEALTH_PUSH_HEADER = "stealthPushHeader";
		
		public static final String STEALTH_PUSH_BODY = "stleathPushBody";
		
		public static final String OFFLINE_PUSH_KEY = "failed";
		
		public static final String OFFLINE_MSISDNS = "msisdns";

		public static final String STEALTH_PASS_RESET = "stealthPasswordReset";

		public static final String IS_RESET_PASS = "isResetStealthPasswordFlow";
		
		public static final String HAS_TIP = "hasTip";

		public static final String TOTAL_MSGS_CURRENTLY_LOADED = "totalMsgsCurrentlyLoaded";
		
		public static final String PIN_TYPE_SHOWING="pinTypeShowing";

		public static final String SHARED_FILE_ITEMS = "sharedFileItems";
		
		public static final String IS_LAST_MESSAGE = "isLastMessage";

		public static final String IS_GROUP_CONVERSATION = "isGroupCoversation";

		public static final String PARTICIPANT_MSISDN_ARRAY = "participantMsisdnArray";

		public static final String PARTICIPANT_NAME_ARRAY = "participantNameArray";

		public static final String CONVERSATION_NAME = "conversationName";

		public static final String DELETE_MEDIA_FROM_PHONE = "deleteMediaFromPhone";

		public static final String DELETED_MESSAGE_TYPE = "deletedMessageType";

		public static final String CURRENT_POSITION = "currentPosition";
		
		public static final String FTUE_FORWARD = "ftueForward";
		
		public static final String FTUE_STICKER_ID_LIST = "ftueStickerIdsList";
		
		public static final String FTUE_STICKER_CATEGORY_LIST = "ftueStickerCategoryList";
		
		public static final String SDK_THIRD_PARTY_PKG = "third_party_app_pkg";
		
		public static final String SDK_CONNECTION_TYPE = "connection_type";

		public static final String SELECT_ALL_INITIALLY = "selectAllInitially";
	}

	public static final class LogEvent
	{
		// Common tags for Countly. Don't change.

		public static final String SOURCE_APP = "source_app";
		
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
		 * Home screen events <screen> = hoS <event> = profS, invS, feedS, delAC, delC, compB, addSC, creDtiPN, creDtiPY, upDtiPN, upDtipY, upDOBD, upDOB, smSY, smSN, groupS, dRB
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

		public static final String EMAIL_CONVERSATION = "hoSecON";

		/*
		 * Profile screen events <screen> = profS <event> = proES, credS, notyS, privS, helpS
		 */
		public static final String EDIT_PROFILE = "profSproES";

		public static final String NOTIFICATION_SCREEN = "profSnotyS";

		public static final String PRIVACY_SCREEN = "profSprivS";
		
		public static final String MEDIA_THUMBNAIL_VIA_PROFILE = "profSthumb";
		
		public static final String OPEN_GALLERY_VIA_PROFILE = "profSopenGallery";
		
		public static final String SHARED_FILES_VIA_PROFILE = "profSsharedFiles";
		
		public static final String ADD_TO_FAVOURITE = "profSAddToFav";
		
		public static final String INVITE_TO_HIKE_VIA_PROFILE = "profSinvite";

		public static final String SET_PROFILE_PIC_GALLERY = "profSpicGallery";

		/*
		 * Invite screen events <screen> = invS <event> = credB, creDtiPN, creDtiPY
		 */
		public static final String INVITE_TOOL_TIP_CLOSED = "invScreDtiPN";

		public static final String INVITE_TOOL_TIP_CLICKED = "invScreDtiPY";

		public static final String CREDIT_TOP_BUTTON = "invScredB";

		/*
		 * Chat thread screen events <screen> = chatS <event> = inVtiPN, inVtopB, blocK, forMsg, infoB, invOB, invOBD, opTiNtaP, calL, adD, grPinfO
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
		 *  Compose screen <screen> = compS <event> = refContcts
		 */
		public static final String COMPOSE_REFRESH_CONTACTS = "compSrefContcts";

		public static final String SELECT_ALL_HIKE_CONTACTS = "compSslctAllFwd";

		public static final String CONFIRM_FORWARD = "compSconfFwd";

		/*
		 * Drawer screen <screen> = drS <event> = homE, gC, inV, reW, creD, proF, settinG
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

		/*
		 * FTUE
		 */
		public static final String CLICK = "click";

		public static final String GRID_6 = "grid6";

		public static final String ADD_FRIENDS_CLICK = "addFriends";

		public static final String ADD_UPDATES_CLICK = "addUpdates";

		public static final String INVITE_FTUE_FRIENDS_CLICK = "inviteFTUEFriendsClick";

		public static final String REMIND_FTUE_FRIENDS_CLICK = "remindFTUEFriendsClick";

		public static final String INVITE_SMS_CLICK = "inviteSMSClick";

		public static final String REMIND_SMS_CLICK = "remindSMSClick";

		public static final String INVITE_FTUE_UPDATES_CLICK = "inviteFTUEUpdatesClick";

		public static final String REMIND_FTUE_UPDATES_CLICK = "remindFTUEUpdatesClick";

		public static final String POST_UPDATE_FROM_CARD = "postUpdateFromCard";

		public static final String POST_UPDATE_FROM_TOP_BAR = "postUpdateFromTopBar";

		public static final String NEW_CHAT_FROM_GRID = "newChatFromGrid";

		public static final String NEW_CHAT_FROM_TOP_BAR = "newChatFromTopBar";

		public static final String INVITE_FROM_GRID = "inviteFromGrid";

		public static final String FB_CLICK = "fbClick";

		/*
		 * Invite keys
		 */
		public static final String INVITE_FRIENDS_FROM_POPUP_FREE_SMS = "inviteFriendsFromPopupFreeSMS";

		public static final String INVITE_FRIENDS_FROM_POPUP_REWARDS = "inviteFriendsFromPopupRewards";

		public static final String INVITE_SMS_SCREEN_FROM_INVITE = "inviteSMSScreenFromInvite";

		public static final String INVITE_SMS_SCREEN_FROM_CREDIT = "inviteSMSScreenFromCredit";

		public static final String SELECT_ALL_INVITE = "selectAllInvite";

		/*
		 * Sticker Ftue
		 */
		public static final String STICKER_FTUE_BTN_CLICK = "stickerFtueBtnClick";

		public static final String FTUE_TUTORIAL_STICKER_VIEWED = "ftueTutorialStickerViewed";

		public static final String FTUE_TUTORIAL_CBG_VIEWED = "ftueTutorialCbgViewed";

		public static final String START_HIKING = "startHiking";
		
		public static final String QUICK_SETUP_CLICK = "quickSetupClick";
		
		public static final String STEALTH_FTUE_DONE = "stlthFtueDone";
		
		public static final String RESET_STEALTH_INIT = "resetStlthInit";
		
		public static final String RESET_STEALTH_CANCEL = "resetStlthCancel";
		
		public static final String ENTER_WRONG_STEALTH_MODE = "entWrongStlth";
		
		public static final String EXIT_STEALTH_MODE = "exitStlthMode";

		public static final String FTUE_WELCOME_CARD_CLICKED = "ftueWelcomeCardClicked";

		public static final String FTUE_CARD_START_CHAT_CLICKED = "ftueCardStartChatClicked";
		
		public static final String FTUE_FAV_CARD_START_CHAT_CLICKED = "ftueFavCardStartChatClicked";

		public static final String FTUE_CARD_SEEL_ALL_CLICKED = "ftueCardSeeAllClicked";

		public static final String FTUE_FAV_CARD_SEEL_ALL_CLICKED = "ftueFavCardSeeAllClicked";

		public static final String FIRST_OFFLINE_TIP_CLICKED = "firstOfflineTipClicked";
		
		public static final String SECOND_OFFLINE_TIP_CLICKED = "secondOfflineTipClicked";

		public static final String SMS_POPUP_ALWAYS_CLICKED = "smsPopupAlwaysClicked";

		public static final String SMS_POPUP_JUST_ONCE_CLICKED = "smsPopupJustOnceClicked";

		public static final String SMS_POPUP_REGULAR_CHECKED = "smsPopupRegularChecked";
		
		public static final String FTUE_CARD_LAST_SEEN_CLICKED = "ftueCardLastSeenClicked";

		public static final String FTUE_CARD_GROUP_CLICKED = "ftueCardGroupClicked";

		public static final String FTUE_CARD_HIDDEN_MODE_CLICKED = "ftueCardHiddenModeClicked";
		
		public static final String WATS_APP_INVITE = "inv_wa";

		public static final String FTUE_CARD_INVITE_CLICKED = "ftueCardInviteClicked";
		
 		public static final String SHOW_TIMELINE_TOP_BAR = "showTimelineTopBar";

		public static final String FAVOURITE_FROM_OVERFLOW = "favoriteFromOverflow";
		
		public static final String STATUS_UPDATE_FROM_OVERFLOW = "statusUpdateFromOverflow";

		public static final String FTUE_CARD_HIKE_OFFLINE_CLICKED = "ftueCardHikeOfflineClicked";
		
		public static final String PIN_POSTED_VIA_ICON = "pinPostedViaIcon";
		
		public static final String PIN_POSTED_VIA_HASH_PIN = "pinPostedViaHashPin";
		
		public static final String PIN_HISTORY_VIA_MENU = "pinHistoryViaMenu";
		
		public static final String PIN_HISTORY_VIA_PIN_CLICK = "pinHistoryViaPinClick";
		
		public static final String STICKER_BTN_CLICKED = "stickerBtnClicked";
		
		public static final String STICKER_SETTING_BTN_CLICKED = "stickerSettingBtnClicked";
		
		public static final String SEEN_REORDERING_TIP = "seenReorderingTip";
		
		public static final String UPDATE_ALL_CONFIRM_CLICKED = "updateAllConfirmClicked";
		
		public static final String UPDATE_ALL_CANCEL_CLICKED = "updateAllCancelClicked";
		
		public static final String STICKER_CHECK_BOX_CLICKED = "stickerChkBoxClicked";
		
		public static final String STICKER_UNCHECK_BOX_CLICKED = "stickerUnchkBoxClicked";
		/*
		 * Settings screen <screen> = settingsS <event> = notifNUJEnabled, notifH2OEnabled, notifNUJDisabled, notifH2ODisabled
		 */
		public static final String SETTINGS_NOTIFICATION_H2O_ON = "settingsSNotifH2OEnabled";
		
		public static final String SETTINGS_NOTIFICATION_H2O_OFF = "settingsSNotifH2ODisabled";
		
		/*
		 * HikeSharedFiles Activity Screen = sharedMediaS<event>
		 * */
		public static final String OPEN_THUMBNAIL_VIA_GALLERY = "sharedMediaSthumbnailClick";

		/*
		 * Image selection from gallery event.
		 */
		public static final String GALLERY_SELECTION = "gallery";

		/*
		 * File transfer success and failure event.
		 */
		public static final String FILE_TRANSFER_STATUS = "ftStatus";

		/*
		 * Atomic tips click events
		 */
		public static final String ATOMIC_FAVOURITES_TIP_CLICKED = "atomicFavTClick";

		public static final String ATOMIC_INVITE_TIP_CLICKED = "atomicInviteTClick";

		public static final String ATOMIC_PROFILE_PIC_TIP_CLICKED = "atomicProPicTClick";

		public static final String ATOMIC_STATUS_TIP_CLICKED = "atomicStatusTClick";

		public static final String ATOMIC_HTTP_TIP_CLICKED = "atomicHttpTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_CLICKED = "atomicAppSttngsTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_NOTIF_CLICKED = "atomicAppSttngsNotifTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_PRIVACY_CLICKED = "atomicAppSttngsPrivTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_SMS_CLICKED = "atomicAppSttngsSmsTClick";

		public static final String ATOMIC_APP_TIP_SETTINGS_MEDIA_CLICKED = "atomicAppSttngsMediaTClick";

		public static final String ATOMIC_APP_TIP_INVITE_FREE_SMS_CLICKED = "atomicAppInvFreeSmsTClick";

		public static final String ATOMIC_APP_TIP_INVITE_WHATSAPP_CLICKED = "atomicAppInvWaTClick";

		public static final String ATOMIC_APP_TIP_TIMELINE_CLICKED = "atomicAppTmlineTClick";

		public static final String ATOMIC_APP_TIP_HIKE_EXTRA_CLICKED = "atomicAppHikeExtraClick";

		public static final String ATOMIC_APP_TIP_HIKE_REWARDS_CLICKED = "atomicAppHikeRewardsClick";

		/*
		 * NUX Events 
		 */
		public static final String NUX_STICKER_CLICKED = "nuxStckrClk";

		public static final String NUX_STICKER_FORWARD = "nuxStckrFwd";

		public static final String NUX_INVITE_BUTTON_CLICKED = "nuxInvteBtnClk";

		public static final String NUX_INVITE_SENT = "nuxInvteSent";

		public static final String NUX_BOT_FORWARD = "nuxBotFwd";
		
		/*
		 * Account Backup Events
		 */
		public static final String BACKUP = "backupClk";

		public static final String BACKUP_RESTORE = "backupRestoreClk";

		public static final String BACKUP_RESTORE_RETRY = "backupRestoreRetryClk";

		public static final String BACKUP_RESTORE_SKIP = "backupRestoreSkipClk";

		/*
		 * Platform Events
		 */
		public static final String SDK_AUTH_DIALOG_VIEWED = "sdkAuthDialogViewed";
		
		public static final String SDK_AUTH_DIALOG_DECLINED = "sdkAuthDialogDeclined";
		
		public static final String SDK_AUTH_DIALOG_CONNECT = "sdkAuthDialogConnect";
		
		public static final String SDK_AUTH_FAILURE = "sdkAuthFailure";
		
		public static final String SDK_AUTH_SUCCESS = "sdkAuthSuccess";
		
		public static final String SDK_SEND_MESSAGE = "sdkSendMessage";
		
		public static final String SDK_INSTALL_HIKE_MESSENGER = "sdkDialogInstallHikeMessenger";
		
		public static final String SDK_INSTALL_HIKE_ACCEPT = "sdkInstallHikeAccept";
		
		public static final String SDK_INSTALL_HIKE_DECLINE = "sdkInstallHikeDeclined";
		
		public static final String SDK_DISCONNECT_APP = "sdkDisconnectApp";
		
        /*
		 * Content Card events
		 */
        public static final String CONTENT_CARD_TAPPED = "contentCardTapped";

		public static final String RETRY_NOTIFICATION_SENT = "retryNotificationSent";
		/*
		 * Festive Popup events
		 */
		public static final String FESTIVE_POPUP_WISH = "fstvepopwish";

		public static final String FESTIVE_POPUP_DISMISS = "fstvepopdsmss";

		/*
		 * VOIP events
		 */
		public static final String VOIP = "voip";

		public static final String VOIP_CALL_RATE_POPUP_SUBMIT = "vrmcSbmt";

		public static final String VOIP_CALL_CLICK = "cs";

		public static final String VOIP_CALL_ACCEPT = "ca";

		public static final String VOIP_CALL_REJECT = "cr";

		public static final String VOIP_CALL_SPEAKER = "spk";

		public static final String VOIP_CALL_MUTE = "mut";

		public static final String VOIP_CALL_HOLD = "hld";

		public static final String VOIP_CALL_END = "ce";

		public static final String VOIP_CALL_DROP = "cd";

		public static final String VOIP_NATIVE_CALL_INTERRUPT = "tci";

		public static final String VOIP_CALL_RELAY = "cpur";
	}

	public static final class MqttMessageTypes
	{
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

		public static final String GAMES = "games";

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

		public static final String BULK_LAST_SEEN = "bls";

		public static final String FACEBOOK = "fb";

		public static final String UPDATE_PUSH = "update";

		public static final String APPLICATIONS_PUSH = "applications";

		public static final String MULTI_INVITE = "mi";

		public static final String CHAT_BACKGROUD = "cbg";

		public static final String GROUP_OWNER_CHANGE = "goc";

		public static final String REQUEST_DP = "rdp";
		
		public static final String STEALTH = "stlth";

		public static final String TOGGLE_STEALTH = "ts";
		
		public static final String POPUP = "popup"; //this is a generic popup type
		
		public static final String BULK_MESSAGE = "bm";

		public static final String REMOVE_PIC = "icr";
		
		public static final String MULTIPLE_FORWARD = "mm";
		
		public static final String TIP = "tip";

		public static final String VOIP_SOCKET_INFO = "ve";
		
		/**
		 * VoIP data packet with QoS 0. This packet will either be delivered immediately
		 * or never. 
		 */
		public static final String MESSAGE_VOIP_0 = "v0";

		/**
		 * VoIP data packet with QoS 1. 
		 */
		public static final String MESSAGE_VOIP_1 = "v1";

		/**
		 * The person we are calling is on a compatible platform, but is
		 * using an old version of the client which does not support VoIP. 
		 */
		public static final String VOIP_ERROR_CALLEE_INCOMPATIBLE_UPGRADABLE = "e0";

		public static final String CREATE_BOT = "cb";
		
		public static final String DELETE_BOT = "db";
		
		public static final String GCM_ECHO = "gcmecho";

		/**
		 * The person we are calling is on a client that cannot be upgraded
		 * to support VoIP. For example, might be on iOS and we have no iOS client.
		 */
		public static final String VOIP_ERROR_CALLEE_INCOMPATIBLE_NOT_UPGRADABLE = "e1";

		/**
		 * The person you are calling has blocked you. 
		 */
		public static final String VOIP_ERROR_CALLEE_HAS_BLOCKED_YOU = "e2";
		
		/**
		 * If you receive a packet of this subtype, it implies that the person
		 * you are calling is already on a call.
		 */
		public static final String VOIP_ERROR_ALREADY_IN_CALL = "mc";

		public static final String VOIP_MSG_TYPE_CALL_SUMMARY = "vcs";

		public static final String VOIP_MSG_TYPE_MISSED_CALL_INCOMING = "vmci";

		public static final String VOIP_MSG_TYPE_MISSED_CALL_OUTGOING = "vmco";
	}
	
	public static final class SMSNative
	{
		/*
		 * SMS URIs
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://sms");

		public static final Uri INBOX_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, "inbox");

		public static final Uri SENTBOX_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, "sent");

		public static final String NUMBER = "address";

		public static final String DATE = "date";

		public static final String MESSAGE = "body";

		public static final String READ = "read";

	}

	public static final class SocialPostResponse
	{
		public static final String NO_TOKEN = "notoken";

		public static final String INVALID_TOKEN = "invalidtoken";

		public static final String FAILURE = "failure";

		public static final String SUCCESS = "success";
	}

	public static enum FTResult
	{
		SUCCESS, UPLOAD_FAILED, FILE_TOO_LARGE, READ_FAIL, DOWNLOAD_FAILED, CANCELLED, FILE_EXPIRED, PAUSED, SERVER_ERROR, FAILED_UNRECOVERABLE, CARD_UNMOUNT, NO_SD_CARD
	}

	public static enum SMSSyncState
	{
		SUCCESSFUL, NO_CHANGE, UNSUCCESSFUL
	}

	public static enum EmoticonType
	{
		EMOTICON, STICKERS
	}

	public static final int[] INVITE_STRINGS = { R.string.native_sms_invite_1, R.string.native_sms_invite_2, R.string.native_sms_invite_3, R.string.native_sms_invite_4, R.string.native_sms_invite_5, R.string.native_sms_invite_6 };

	// TODO need to finalize this with AM
	public static final int FRIENDS_LIMIT_MAGIC_NUMBER = 8;

	public static final StatusMessageType[] STATUS_TYPE_LIST_TO_FETCH = {StatusMessageType.TEXT, StatusMessageType.TEXT_IMAGE};

	public static enum WelcomeTutorial
	{
		INTRO_VIEWED, STICKER_VIEWED, CHAT_BG_VIEWED
	}

	/**
	 * while updating from sound/vibrate single pref to list pref (from app 2.9.0 to ) , we need to respect old setting set by user, so if are done this transition , we will set
	 * this key to true in preference
	 */
	public static final String PREFERENCE_TRANSITION_SOUND_VIB_TO_LIST = "soundVibTransitionDone";

	public static final long[] SHORT_VIB_PATTERN = new long[] { 0, 200, 100, 250 };

	public static final long[] LONG_VIB_PATTERN = new long[] { 0, 1000 };

	public static final int FTUE_HIKE_CONTACT_MIN_LIMIT = 3;

	public static final String PACKAGE_WATSAPP = "com.whatsapp";

	public static final long IMAGE_SIZE_SMALL = 50 * 1024;

	public static final long IMAGE_SIZE_MEDIUM = 80 * 1024;

	public static final String WATSAPP_INVITE_MESSAGE_KEY = "wa_msg";

	public static final String WATSAPP_INVITE_ENABLED = "i_wa";
	
	public static final class ImageQuality
	{
		public static final int QUALITY_ORIGINAL = 1;

		public static final int QUALITY_MEDIUM = 2;

		public static final int QUALITY_SMALL = 3;

		public static final int QUALITY_DEFAULT = QUALITY_SMALL;

		public static final String IMAGE_QUALITY_ORIGINAL = "O";

		public static final String IMAGE_QUALITY_MEDIUM = "M";

		public static final String IMAGE_QUALITY_SMALL = "S";
		
		public static final String IMAGE_QUALITY_DEFAULT = IMAGE_QUALITY_SMALL;
	}

	public static class MESSAGE_TYPE
	{
		public static final String MESSAGE_TYPE = "messageType";
				
		public static final int PLAIN_TEXT = 0;

		public static final int TEXT_PIN = 1;
		
		public static final int CONTENT = 2;
	}
	
	public static interface ConvMessagePacketKeys{
		public static final String CONTENT_TYPE = "c";
		public static final String PIN_TYPE = "pin";
		public static final String LOVE_ID = "loveID";
	}
	
	public static class HASH_MESSAGE_TYPE
	{
		public static final int DEFAULT_MESSAGE = 0;

		public static final int HASH_PIN_MESSAGE = 1;
	}
	
	public static class REQUEST_BASE_URLS
	{
		public static final String HTTP_REQUEST_PROFILE_BASE_URL = "/account/profile/";
	}
	public static final String TEXT_PINS = "text_pins";

	public static final String EXTRA_CONV_ID = "conv_id";
	
	public static final int MAX_PIN_CONTENT_LINES_IN_HISTORY = 10;
	
	public static final String URL = "url";
	public static final String MEDIA_POSITION = "position";
	
	public static final String FROM_CHAT_THREAD = "ct";

	public static final String LARGE = "large";
	
	public static final int ATOMIC_APP_TIP_SETTINGS = 1;
	public static final int ATOMIC_APP_TIP_SETTINGS_NOTIF = 2;
	public static final int ATOMIC_APP_TIP_SETTINGS_MEDIA = 3;
	public static final int ATOMIC_APP_TIP_SETTINGS_SMS = 4;
	public static final int ATOMIC_APP_TIP_SETTINGS_PRIVACY = 5;
	public static final int ATOMIC_APP_TIP_TIMELINE = 6;
	public static final int ATOMIC_APP_TIP_INVITE_FREE_SMS = 8;
	public static final int ATOMIC_APP_TIP_INVITE_WATSAPP = 9;
	public static final int ATOMIC_APP_TIP_HIKE_EXTRA = 10;
	public static final int ATOMIC_APP_TIP_HIKE_REWARDS = 11;

	public static final int SHARED_MEDIA_TYPE = 1;

	public static enum STResult
	{
		SUCCESS, FILE_TOO_LARGE, READ_FAIL, DOWNLOAD_FAILED, CANCELLED, SERVER_ERROR, CARD_UNMOUNT, NO_SD_CARD, FILE_EXPIRED
	}
	
	public static final String PREVIEW_IMAGE = "previewimg";
	public static final String ENABLE_IMAGE = "enableimg";
	public static final String DISABLE_IMAGE = "disableimg";
	public static final String SIZE = "size";	
	public static final int NORMAL_MESSAGE_TYPE = 0;
	public static final int MULTI_FORWARD_MESSAGE_TYPE = 1;
	public static final int SHARED_PIN_TYPE = 2;
	public static final String SHOWN_MULTI_FORWARD_TIP = "shownMultiForwardTip";

	public static final String VISIBLITY = "visibility";

	public static final String INDEX = "index";
	
	public static final String CAT_NAME = "name";
	
	public static final String DELIMETER = ":";
		
	public static final String HIKE_EXTRAS_NAME = "hike_extras_name";
	
	public static final String HIKE_EXTRAS_URL = "hike_extras_url";
	
	public static final String REWARDS_NAME = "rewards_name";
		
	public static final String REWARDS_URL = "rewards_url";

	public static final String CALL_LOG_ANALYTICS = "cl";

	public static final String APP_LOG_ANALYTICS = "al";

	public static final String LOCATION_LOG_ANALYTICS = "ll";

	public static final String SHOP = "shop";

	public static final String BADGE = "badge";

	public static final long DEFAULT_RETRY_NOTIF_TIME = 20 * 60 * 1000; // 20 minutes

	public static final String RETRY_COUNT = "retryCount";
	
	public static final String REPLY_NOTIFICATION_RETRY_TIMER = "rnrt";
	
	// Intent send to register gcm before and after signup
	public static final String REGISTER_GCM_SIGNUP = "register_gcm_signup";
	
	public static final int REGISTEM_GCM_BEFORE_SIGNUP = 345;
	
	public static final int REGISTEM_GCM_AFTER_SIGNUP = 346;
	
	public static final String INCENTIVE_ID = "incentive_id";
	
	public static final String GCM_ID = "gcm_id";
	
	public static final String ADD_CATEGORY = "addCat";

	public static final int LED_DEFAULT_WHITE_COLOR = 0xffffffff;
	
	public static final int LED_NONE_COLOR = -2; /**
													Any Change In this Value Should be double checked 
													as this may coincide with any other color value
												   */

	public static long STOP_NOTIF_SOUND_TIME = 3000; // In milliseconds

}
