package com.bsb.hike.offline;

import java.util.HashMap;

import android.content.Intent;
import android.net.wifi.ScanResult;

public interface DeviceActionListener {
    void callDisconnect();

    void connect(int numOfTries, String  deviceName,  int mode, Intent intent);

    void disconnect();
    
    Boolean connectToHotspot(String SSID);
    
    HashMap<String, ScanResult> getDistinctWifiNetworks();

	void startScan();

	void resetWifi();
	
	void forgetWifiNetwork();
	
	String getMsisdn();
}