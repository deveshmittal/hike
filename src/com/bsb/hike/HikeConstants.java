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

	public static final String SOUND_PREF = "soundPref";
	public static final String VIBRATE_PREF = "vibratePref";
	public static final String HIKEBOT = "TD-HIKE";
	
	public static final String DONE = "Done";
	public static final String PIN_ERROR = "PinError";
	public static final String ADDRESS_BOOK_ERROR = "AddressBookError";
	public static final String CHANGE_NUMBER = "ChangeNumber"; 
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
	}
}
