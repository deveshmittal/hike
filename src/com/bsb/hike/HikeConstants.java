package com.bsb.hike;

public class HikeConstants
{
	public static final String MESSAGE = "msg";
	public static final String UI_TOPIC = "/u";
	public static final String APP_TOPIC = "/a";
	public static final String SERVICE_TOPIC = "/s";
	public static final String PUBLISH_TOPIC = "/p";

	public static final String TYPE = "t";
	public static final String DATA = "d";
	public static final String TO = "to";
	public static final String FROM = "f";

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

	public static final String SOUND_PREF = "soundPref";
	public static final String VIBRATE_PREF = "vibratePref";
	public static final String HIKEBOT = "TD-HIKE";
	
	public static final String DONE = "Done";
	public static final String PIN_ERROR = "PinError";
	public static final String ADDRESS_BOOK_ERROR = "AddressBookError";
	public static final String CHANGE_NUMBER = "ChangeNumber"; 

	public static final String SEPARATOR = " - ";

	/* how long to wait between sending publish and receiving an acknowledgement */
	public static final long MESSAGE_DELIVERY_TIMEOUT = 5*1000;

	/* how long to wait for a ping confirmation */
	public static final long PING_TIMEOUT = 5*1000;

	/* how long to wait to resend message. This should significantly greathr than PING_TIMEOUT */
	public static final long MESSAGE_RETRY_INTERVAL = 15*1000;

	/* quiet period of no changes before actually updating the db */
	public static final long CONTACT_UPDATE_TIMEOUT = 10*1000;

	/* how often to ping the server */
	public static final short KEEP_ALIVE = 10 * 60; /* 10 minutes */

	/* how often to ping after a failure */
	public static final int RECONNECT_TIME = 10; /* 10 seconds */

	public static final int HIKE_SYSTEM_NOTIFICATION = 0;
	public static final String ADAPTER_NAME = "hikeadapter";
	
	/* constants for defining what to do after checking for updates*/
	public static final int UPDATE_AVAILABLE = 2;
	public static final int CRITICAL_UPDATE = 1;
	public static final int NO_UPDATE = 0;
	
	// More explanation required?
	public static final int NUM_TIMES_SCREEN_SHOULD_OPEN_BEFORE_TOOL_TIP = 2;

	public static int NUM_SMS_PER_FRIEND = 10;
	public static int INITIAL_NUM_SMS = 100;

	public static final class Extras
	{
		public static final String MSISDN = "msisdn";
		public static final String ID = "id";
		public static final String NAME = "name";
		public static final String INVITE = "invite";
		public static final String MSG = "msg";
		public static final String PREF = "pref";
		public static final String EDIT = "edit";
		public static final String IMAGE_PATH ="image-path";
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
	}

	public static final class LogEvent
	{
		public static final String TAG = "tag";
		public static final String DEVICE_ID = "device_id";
		public static final String OS = "_os";
		public static final String OS_VERSION = "_os_version";
		public static final String DEVICE = "_device";
		public static final String RESOLUTION = "_resolution";
		public static final String CARRIER = "_carrier";

		// Home screen events
		public static final String MENU = "homeMenu";
		public static final String PROFILE_MENU = "homeMenuProfile";
		public static final String INVITE_MENU = "homeMenuInvite";
		public static final String FEEDBACK_MENU = "homeMenuFeedback";
		public static final String DELETE_ALL_CONVERSATIONS_MENU = "homeMenuDeleteAllConversations";
		public static final String DELETE_CONVERSATION = "homeDeleteConversation";
		public static final String COMPOSE_BUTTON = "homeComposeBtn";
		public static final String ADD_SHORTCUT = "homeAddShortcut";
		public static final String HOME_INVITE_TOP_BUTTON = "homeInviteTopBtn";
		public static final String HOME_TOOL_TIP_CLOSED = "homeInviteToolTipClosed";
		public static final String HOME_TOOL_TIP_CLICKED = "homeInviteToolTipClicked";

		// Profile screen events
		public static final String EDIT_PROFILE = "profileEdit";
		public static final String CREDITS_SCREEN = "profileCreditScreen";
		public static final String NOTIFICATION_SCREEN = "profileNotificationScreen";
		public static final String PRIVACY_SCREEN = "profilePrivacyScreen";
		public static final String BLOCK_SCREEN = "profileBlockScreen";
		public static final String HELP_SCREEN = "profileHelpScreen";

		// Invite screen events
		public static final String INVITE_TOOL_TIP_CLOSED = "inviteToolTipClosed";
		public static final String INVITE_TOOL_TIP_CLICKED = "inviteToolTipClicked";
		public static final String CREDIT_TOP_BUTTON = "inviteCreditTopBtn";
		
		// Chat thread screen events
		public static final String CHAT_INVITE_TOOL_TIP_CLOSED = "chatInviteToolTipClosed";
		public static final String CHAT_GROUP_INFO_TOOL_TIP_CLOSED = "chatGroupInfoToolTipClosed";
		public static final String CHAT_INVITE_TOP_BUTTON = "chatInviteBtn";
		public static final String CHAT_GROUP_INFO_TOP_BUTTON = "chatGroupInfoBtn";
		public static final String MENU_BLOCK = "chatMenuBlock";
		public static final String FORWARD_MSG = "chatForwardMsg";
		public static final String I_BUTTON = "chatIBtn";
		public static final String INVITE_OVERLAY_BUTTON = "chatInviteOverlayBtn";
		public static final String INVITE_OVERLAY_DISMISS = "chatInviteOverlayDismiss";
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
	}
}
