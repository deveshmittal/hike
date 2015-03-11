package com.bsb.hike.offline;

import android.net.wifi.ScanResult;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;

public interface DeviceActionListener {
    void callDisconnect();

    void connect(WifiP2pConfig config, int numOfTries, WifiP2pDevice ConnectingToDevice,  int mode);

    void disconnect();
    
    void connectToHotspot(ScanResult scanResult);
}