package com.bsb.hike.offline;

import android.net.wifi.p2p.WifiP2pDevice;

public interface WifiP2pConnectionManagerListener {
	
	void channelDisconnectedFailure();
	
	void updateMyDevice(WifiP2pDevice device);

	void resetData();
	
	void notifyWifiNotEnabled();
	
	void connectSuccess();
	
	void connectFailure(int reasonCode);
	
	void cancelConnectSuccess();
	
	void cancelConnectFailure(int reasonCode);
	
	void discoverySuccess();
	
	void discoveryFailure(int reasonCode);
}
