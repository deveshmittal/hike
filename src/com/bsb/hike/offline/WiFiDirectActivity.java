/*
cfcf * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bsb.hike.offline;

import java.util.HashMap;

import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.utils.Logger;
/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends Activity implements WifiP2pConnectionManagerListener, DeviceActionListener{

    public static final String TAG = "wifidirectdemo";
    private WifiP2pConnectionManager connectionManager = null;
    private BroadcastReceiver receiver = null;
    private IntentFilter intentFilter;
    
    public static boolean isOfflineFileTransferOn = false;
    private final int MAXTRIES = 5;
    // remember connection details for re-connection
    public static WifiP2pDevice connectingToDevice = null;
    public static WifiP2pConfig connectingDeviceConfig = null;
    public static int tries;
    private CheckInvitedStuckTask checkTask = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        intentFilter = new IntentFilter();
    	intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        
        DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        connectionManager = new WifiP2pConnectionManager(this, this, (PeerListListener)fragment, (GroupInfoListener) fragment);
        receiver = new WiFiDirectBroadcastReceiver(connectionManager);
        registerReceiver(receiver, intentFilter);
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        //registerReceiver(receiver, intentFilter);
        connectionManager.enableDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
       // unregisterReceiver(receiver);
    }
    
    @Override
    protected void onRestart() {
    	DeviceListFragment.intent =  null;
    	// give time to disconnect
    	try {
			Thread.sleep(1*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	super.onRestart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
    	clearData();
    	new OnFileTransferCompleteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
    	super.onNewIntent(intent);
    }
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (connectionManager.checkChannel() && connectionManager.checkManager()) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;
            case R.id.atn_direct_discover:
                connectionManager.enableDiscovery();
                return true ;
            
            case R.id.atn_direct_wifinetworks:
            	 WifiNetworksListFragment wififragment = (WifiNetworksListFragment) getFragmentManager().findFragmentById(R.id.wifi_list);
            	 wififragment.updateWifiNetworks(getDistinctWifiNetworks());  	 
            	 return true ;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public HashMap<String, ScanResult> getDistinctWifiNetworks()
    {
    	return connectionManager.getDistinctWifiNetworks();
    }
    
    @Override
    public void connect(WifiP2pConfig config, int numOfTries, WifiP2pDevice ConnectingToDevice,int mode) {
	    if(mode==0)
	    {
	    	if(numOfTries >= MAXTRIES)
	    	{
	    		Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
	                    Toast.LENGTH_SHORT).show();
	    		DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
	    		if(fragment.progressDialog!=null && fragment.progressDialog.isShowing())
	    			fragment.progressDialog.dismiss();
	    		return;
	    	}
	    	WiFiDirectActivity.connectingToDevice = ConnectingToDevice;
	    	WiFiDirectActivity.connectingDeviceConfig = config;
	    	WiFiDirectActivity.tries = numOfTries;
	    	new CheckInvitedStuckTask(connectingToDevice).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
	        connectionManager.connect(config);    
	    }
	    else
	    {
	          connectionManager.createhotspot(ConnectingToDevice);  
	    }
    }

    @Override
    public void disconnect() {
        connectionManager.disconnect();
    }
    
    @Override
    public void onBackPressed() 
    {
    	isOfflineFileTransferOn = false;
    	clearData();
    	new OnFileTransferCompleteTask().executeOnExecutor((AsyncTask.THREAD_POOL_EXECUTOR));    	
    	super.onBackPressed();
    }
    
    /*
     * A cancel abort request by user. Disconnect i.e. removeGroup if
     * already connected. Else, request WifiP2pManager to abort the ongoing
     * request
     */
    @Override
    public void callDisconnect() { 
        if (connectionManager.checkManager()) 
        {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment != null && (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED)) 
            {
                connectionManager.disconnect();
            }
            else if (fragment != null && (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED)) 
            {
            	connectionManager.cancelConnect();
            }
            connectionManager.enableDiscovery();
        }

    }
    
    public void clearData()
    {
    	connectingToDevice = null;
    	connectingDeviceConfig = null;
    	tries = 0;
    }
    
    /*
     * This Async task resets the Wifi settings if device gets blocked in
     * Invited status
     */
    public class CheckInvitedStuckTask extends AsyncTask<Void, Void, Void>
    {
    	
    	private WifiP2pDevice connectingToDevice;
    	private WifiP2pDevice latestInstance;
    	private DeviceListFragment fragment;
    	private boolean destroy = false;
    	public CheckInvitedStuckTask(WifiP2pDevice connectingToDevice)
    	{
    		this.connectingToDevice = connectingToDevice;
    		fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
    	}
    	
    	@Override
    	protected Void doInBackground(Void... params) 
    	{
    		try 
    		{
    			
    			latestInstance = fragment.getLatestPeerInstance(connectingToDevice.deviceAddress);
	    		if( (latestInstance != null) && 
	    				 (latestInstance.status != WifiP2pDevice.CONNECTED) )
	    		{
	    			Thread.sleep(10*1000);
	    		}
	    		/*
    			if(DeviceListFragment.groupInfo==null)
    			{
    				Thread.sleep(10*1000);
    			}
    			if(DeviceListFragment.groupInfo==null){
    				connectingToDevice = null;
    				fragment = null;
    				this.destroy = true;
    				return null;
    			}*/
    				
    		}
    		catch (InterruptedException e) 
			{
    			Logger.e(TAG, "Sleep failed in CheckInvitedStuckTask");
				e.printStackTrace();
			}
			return null;
    	}
    	
    	@Override
    	protected void onPostExecute(Void result) {
    		latestInstance = fragment.getLatestPeerInstance(connectingToDevice.deviceAddress);
			if((latestInstance == null) || (latestInstance.status != WifiP2pDevice.CONNECTED)) {
				connectingToDevice = null;
				fragment = null;
				DeviceListFragment.isReconnecting = true;
    			callDisconnect();
				this.destroy = true;
			}
    	}
    }
    
    public class OnFileTransferCompleteTask extends AsyncTask<Void, Void, Void>
    {
		@Override
		protected Void doInBackground(Void... params) {
			while(!OfflineFileTransferManager.getInstance().getIsOfflineFileTransferFinished() ||
					!OfflineFileTransferManager.getInstance().getIsOfflineTextTransferFinished())
	    	{
				try {
					Thread.sleep(1*1000);
				} catch (InterruptedException e) {
					Logger.e(TAG, "Sleep failed in OnFileTransferCompleteTask");
					e.printStackTrace();
				}
	    	}
			return null;
		}
		
		@Override
    	protected void onPostExecute(Void result) {
    		callDisconnect();
    	}	  
    }

	@Override
	public void channelDisconnectedFailure() {
		Toast.makeText(this,
                "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                Toast.LENGTH_LONG).show();
		
	}

	@Override
	public void connectSuccess() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connectFailure(int reasonCode) {
		 Toast.makeText(this, "Connect failed. Reason: "+reasonCode+ ".Retry.",
                 Toast.LENGTH_SHORT).show();
		
	}

	@Override
	public void cancelConnectSuccess() {
		Toast.makeText(this, "Aborting connection. It will take some time to recover.",
                Toast.LENGTH_SHORT).show();
		
	}

	@Override
	public void cancelConnectFailure(int reasonCode) {
		Toast.makeText(this,
                "Connect abort request failed. Reason Code: " + reasonCode,
                Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void discoverySuccess() {
		Toast.makeText(this, "Discovery enabled!!", Toast.LENGTH_SHORT).show();
		
	}
	  
	@Override
	public void notifyWifiNotEnabled()
	{
		Toast.makeText(this, "Enabling Wifi. Please Wait.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void discoveryFailure(int reasonCode) {
		String err=new String();
        if(reasonCode==WifiP2pManager.BUSY) err="BUSY";
        if(reasonCode==WifiP2pManager.ERROR)err="ERROR";
        if(reasonCode==WifiP2pManager.P2P_UNSUPPORTED) err="P2P_UNSUPPORTED";
        Log.e(TAG,"FAIL - couldnt start to discover peers code: "+err);
        
        try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		connectionManager.enableDiscovery();
	}
	
	@Override
	public void resetData()
	{
		DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        if (fragmentList != null) {
            fragmentList.clearPeers();
            fragmentList.clearConnectionDetails();
        }
        connectionManager.enableDiscovery();
	}

	@Override
	public void updateMyDevice(WifiP2pDevice device) {
		((DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list)).updateThisDevice(device);
	}
	
	@Override
	protected void onDestroy() {
	    unregisterReceiver(receiver);
		super.onDestroy();
		
	}

	@Override
	public Boolean connectToHotspot(ScanResult result) {
		return connectionManager.connectToHotspot(result.SSID);
	}
}


