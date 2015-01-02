package com.bsb.hike.voip;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.util.Log;

public class VoIPClient  {		
	
	private String phoneNumber;
	private String internalIPAddress, externalIPAddress;
	private int internalPort, externalPort;
	private boolean initiator;
	private ConnectionMethods preferredConnectionMethod = ConnectionMethods.UNKNOWN;
	private InetAddress cachedInetAddress = null;
	private String relayAddress;
	
	public enum ConnectionMethods {
		UNKNOWN,
		PRIVATE,
		PUBLIC,
		RELAY
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getInternalIPAddress() {
		return internalIPAddress;
	}
	public void setInternalIPAddress(String internalIPAddress) {
		this.internalIPAddress = internalIPAddress;
		cachedInetAddress = null;
	}
	public String getExternalIPAddress() {
		return externalIPAddress;
	}
	public void setExternalIPAddress(String externalIPAddress) {
		this.externalIPAddress = externalIPAddress;
		cachedInetAddress = null;
	}
	public int getInternalPort() {
		return internalPort;
	}
	public void setInternalPort(int internalPort) {
		this.internalPort = internalPort;
	}
	public int getExternalPort() {
		return externalPort;
	}
	public void setExternalPort(int externalPort) {
		this.externalPort = externalPort;
	}
	public boolean isInitiator() {
		return initiator;
	}
	public void setInitiator(boolean initiator) {
		this.initiator = initiator;
	}
	public ConnectionMethods getPreferredConnectionMethod() {
		return preferredConnectionMethod;
	}
	public void setPreferredConnectionMethod(
			ConnectionMethods preferredConnectionMethod) {
		this.preferredConnectionMethod = preferredConnectionMethod;
		cachedInetAddress = null;
		Log.d(VoIPConstants.TAG, "Setting preferred connection method to: " + preferredConnectionMethod.toString());
	}
	public String getPreferredIPAddress() {
		String ip;
		if (preferredConnectionMethod == ConnectionMethods.PRIVATE)
			ip = getInternalIPAddress();
		else if (preferredConnectionMethod == ConnectionMethods.PUBLIC)
			ip = getExternalIPAddress();
		else
			ip = relayAddress;
		return ip;
	}
	public int getPreferredPort() {
		int port;
		if (preferredConnectionMethod == ConnectionMethods.PRIVATE)
			port = getInternalPort();
		else if (preferredConnectionMethod == ConnectionMethods.PUBLIC)
			port = getExternalPort();
		else
			port = VoIPConstants.ICEServerPort;
		return port;
	}
	
	public InetAddress getCachedInetAddress() {
		if (cachedInetAddress == null) {
			try {
//				Log.d(VoIPActivity.logTag, "preferred address: " + getPreferredIPAddress());
				cachedInetAddress = InetAddress.getByName(getPreferredIPAddress());
			} catch (UnknownHostException e) {
				Log.e(VoIPConstants.TAG, "VoIPClient UnknownHostException: " + e.toString());
			}
		}
		// Log.d(VoIPActivity.logTag, "cached address: " + cachedInetAddress.toString());
		return cachedInetAddress;
	}

	public String getRelayAddress() {
		return relayAddress;
	}
	public void setRelayAddress(String relayAddress) {
		this.relayAddress = relayAddress;
	}

}

