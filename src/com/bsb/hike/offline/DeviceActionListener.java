package com.bsb.hike.offline;

import java.util.HashMap;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;

public interface DeviceActionListener {
    void callDisconnect();

    void connect(WifiP2pConfig config, int numOfTries, WifiP2pDevice ConnectingToDevice,  int mode, Intent intent);

    void disconnect();
    
    Boolean connectToHotspot(ScanResult scanResult);
    
    HashMap<String, ScanResult> getDistinctWifiNetworks();

	void startScan();

	void resetWifi();
}