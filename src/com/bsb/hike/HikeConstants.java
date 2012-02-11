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

	public static final String SOUND_PREF = "soundPref";
	public static final String VIBRATE_PREF = "vibratePref";
	public static final String HIKEBOT = "TD-HIKE";

	/* how long to wait between sending publish and receiving an acknowledgement */
	public static final long MESSAGE_DELIVERY_TIMEOUT = 5*1000;

	/* how long to wait for a ping confirmation */
	public static final long PING_TIMEOUT = 5*1000;

	/* how long to wait to resend message. This should significantly greathr than PING_TIMEOUT */
	public static final long MESSAGE_RETRY_INTERVAL = 15*1000;

	/* quiet period of no changes before actually updating the db */
	public static final long CONTACT_UPDATE_TIMEOUT = 10*1000;
	public static final short KEEP_ALIVE = 10 * 60;

}
