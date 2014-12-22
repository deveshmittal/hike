package com.bsb.hike.voip;

public class VoIPConstants {
	public static final String TAG = "VoIP";
	
	// Connection setup constants
	public static final String COMM_UDP_SYN_ALL = "synall";
	public static final String COMM_UDP_SYN_PUBLIC = "synpublic";
	public static final String COMM_UDP_SYN_PRIVATE = "synprivate";
	public static final String COMM_UDP_SYN_RELAY = "synrelay";
	public static final String COMM_UDP_SYNACK_PUBLIC = "synackpublic";
	public static final String COMM_UDP_SYNACK_PRIVATE = "synackprivate";
	public static final String COMM_UDP_SYNACK_RELAY = "synackrelay";
	public static final String COMM_UDP_ACK_PRIVATE = "ackprivate";
	public static final String COMM_UDP_ACK_PUBLIC = "ackpublic";
	public static final String COMM_UDP_ACK_RELAY = "ar";

	// Relay and ICE server 
	public static final String ICEServerName = "54.255.209.97";
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
}
