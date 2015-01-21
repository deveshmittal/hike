package com.bsb.hike.voip;

public class VoIPConstants {
	public static final String TAG = "VoIP";
	
	/*
	// Connection setup constants
	public static final String COMM_UDP_SYN_PUBLIC = "synpublic";
	public static final String COMM_UDP_SYN_PRIVATE = "synprivate";
	public static final String COMM_UDP_SYN_RELAY = "synrelay";
	public static final String COMM_UDP_SYNACK_PUBLIC = "synackpublic";
	public static final String COMM_UDP_SYNACK_PRIVATE = "synackprivate";
	public static final String COMM_UDP_SYNACK_RELAY = "synackrelay";
	public static final String COMM_UDP_ACK_PRIVATE = "ackprivate";
	public static final String COMM_UDP_ACK_PUBLIC = "ackpublic";
	public static final String COMM_UDP_ACK_RELAY = "ar";
	*/
	
	// Relay and ICE server 
	public static final String ICEServerName = "relay.hike.in";
	public static final int ICEServerPort = 9999;

	/**
	 * Time (ms) to wait before the client being called replies with its
	 * own socket information.
	 */
	public static final int TIMEOUT_PARTNER_SOCKET_INFO = 15000;
	
	/**
	 * Time (ms) to wait for the person being called to accept or decline a call.
	 */
	public static final int TIMEOUT_PARTNER_ANSWER = 30000;

	// Intent actions
	public static final String PARTNER_REQUIRES_UPGRADE = "pru";
	public static final String PARTNER_INCOMPATIBLE = "pi";
	public static final String PARTNER_IN_CALL = "incall";
	public static final String PARTNER_HAS_BLOCKED_YOU = "blocked";
	public static final String INCOMING_NATIVE_CALL_HOLD = "hold";
	
	// Default bitrates
	public static final int BITRATE_2G = 12000;
	public static final int BITRATE_3G = 16000;
	public static final int BITRATE_WIFI = 48000;

	public static final String CALL_ID = "callId";
	public static final String IS_CALL_INITIATOR = "isCallInitiator";

	public static final class Analytics
	{
		public static final String CALL_RATING = "rate";

		public static final String CALL_ID = "callid";

		public static final String IS_CALLER = "caller";

		public static final String PARTNER_MSISDN = "pmsisdn";

		public static final String NETWORK_TYPE = "net";

		public static final String OLD_NETWORK_TYPE = "oldnet";

		public static final String CALL_SOURCE = "source";

		public static final String DATA_SENT = "dsent";

		public static final String DATA_RECEIVED = "drec";

		public static final String STATE = "state";
	}

	
	/**
	 * Call quality
	 */
	
	public static enum CallQuality {
		EXCELLENT,
		GOOD,
		FAIR,
		WEAK,
		UNKNOWN
	}
	
	/**
	 * Track packets received for last X seconds
	 */
	public static final int QUALITY_WINDOW = 3;
	
}
