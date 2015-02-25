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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;
import com.bsb.hike.offline.DeviceListFragment.DeviceActionListener;
import com.bsb.hike.utils.Logger;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceActionListener{

    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    private SharedPreferences settings;
    private String myMsisdn;
    public static boolean isOfflineFileTransferOn = false;
    private final int MAXTRIES = 5;
    private WifiManager wifiManager;
    private ProgressDialog enablingWifi = null;
    // remember connection details for re-connection
    private WifiP2pDevice connectingToDevice = null;
    private WifiP2pConfig connectingDeviceConfig = null;
    private int tries;
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       
        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        isOfflineFileTransferOn = true;
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(wifiManager.isWifiEnabled() == false)
        	wifiManager.setWifiEnabled(true);
        
      
        /*
         * Setting device Name to msisdn
         */
        try {
        	settings = getApplicationContext().getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
            myMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
			Method m = manager.getClass().getMethod("setDeviceName", new Class[]{channel.getClass(), String.class,
						WifiP2pManager.ActionListener.class});
			m.invoke(manager, channel, myMsisdn, new WifiP2pManager.ActionListener() {
				
				@Override
				public void onSuccess() {
					Log.d(TAG, "Device Name changed to " + myMsisdn);
				}
				@Override
				public void onFailure(int reason) {
					Logger.e(TAG, "Unable to set device name as msisdn");
				}
			});
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        
        
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
        enableDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    
    @Override
    protected void onRestart() {
    	//disconnect();
    	DeviceListFragment.intent =  null;
    	// give time to disconnect
    	try {
			Thread.sleep(1*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	enableDiscovery();
    	super.onRestart();
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
    	new OnFileTransferCompleteTask().executeOnExecutor((AsyncTask.THREAD_POOL_EXECUTOR));
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
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:
                enableDiscovery();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        //DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
          //      .findFragmentById(R.id.frag_detail);
        //fragment.showDetails(device);

    }
	
    @Override
    public void connect(WifiP2pConfig config, int numOfTries, WifiP2pDevice ConnectingToDevice) {
    	if(numOfTries >= MAXTRIES)
    	{
    		Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                    Toast.LENGTH_SHORT).show();
    		return;
    	}
    	this.connectingToDevice = ConnectingToDevice;
    	this.connectingDeviceConfig = config;
    	this.tries = numOfTries;
    	(new CheckInvitedStuckTask(connectingToDevice)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
            	// In case of successful connection remove all these information
            	clearData();
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

			@Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        	manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }
            
            @Override
            public void onSuccess() {
                Log.d(TAG, "Disconnect successful.");
            }

        });
    }
    
    @Override
    public void onBackPressed() 
    {
    	isOfflineFileTransferOn = false;
    	new OnFileTransferCompleteTask().executeOnExecutor((AsyncTask.THREAD_POOL_EXECUTOR));    	
    	super.onBackPressed();
    }

    @Override
    public void onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /*
     * A cancel abort request by user. Disconnect i.e. removeGroup if
     * already connected. Else, request WifiP2pManager to abort the ongoing
     * request
     */
    @Override
    public void callDisconnect() { 
        if (manager != null) 
        {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) 
            {
            	clearData();
                disconnect();
            }
            else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) 
            {
            	clearData();
                manager.cancelConnect(channel, new ActionListener() {
                	
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection. It will take some time to recover.",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            enableDiscovery();
        }

    }
    
    public void clearData()
    {
    	connectingToDevice = null;
    	connectingDeviceConfig = null;
    	tries = 0;
    }
    
    public void enableDiscovery()
    {
    	if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(WiFiDirectActivity.this, "Starting Wifi Please Wait",
                    Toast.LENGTH_SHORT).show();
        }
        final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        //fragment.onInitiateDiscovery();
        fragment.clearPeers();
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
        	
            @Override
            public void onSuccess() {
            	if(enablingWifi != null && enablingWifi.isShowing())
            		enablingWifi.dismiss();
                Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
                if (connectingToDevice != null)
                {
                	// this means previous attempt of connection did not succeed
                	List<String> peerList = fragment.getLatestPeers();
                	if(peerList.size() == 0)
                		Toast.makeText(getApplicationContext(), "Device List empty", Toast.LENGTH_SHORT).show();
                	else
                	{
                		if(peerList.contains(connectingToDevice))
                			connect(connectingDeviceConfig, ++tries, connectingToDevice);
                		else
                			Toast.makeText(getApplicationContext(), "Device not present in peer List", Toast.LENGTH_SHORT).show();
                	}
                }
            }

            @Override
            public void onFailure(int reasonCode) {
                //Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                //        Toast.LENGTH_SHORT).show();
                String err=new String();
                if(reasonCode==WifiP2pManager.BUSY) err="BUSY";
                if(reasonCode==WifiP2pManager.ERROR)err="ERROR";
                if(reasonCode==WifiP2pManager.P2P_UNSUPPORTED) err="P2P_UNSUPPORTED";
                Log.e(TAG,"FAIL - couldnt start to discover peers code: "+err);
                if(enablingWifi == null || enablingWifi.isShowing() == false)
                {
                	try
                	{
                		enablingWifi = ProgressDialog.show(getApplication(), "Press back to cancel", "Restarting Discovery", true,
                			true, new DialogInterface.OnCancelListener() {

		                    @Override
		                    public void onCancel(DialogInterface dialog) {
		                        
		                    }
		                });
                	}
                	catch (Exception e)
                	{
                		e.printStackTrace();
                	}
                	
                }
                enableDiscovery();
            }
        });
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
    		while(true)
    		{
	    		try 
	    		{
	    			latestInstance = fragment.getLatestPeerInstance(connectingToDevice.deviceAddress);
		    		if((latestInstance != null) && 
		    				(latestInstance.status == WifiP2pDevice.INVITED))
		    		{
		    			Thread.sleep(5*1000);
		    			// check if even after 5 seconds it is still in invited state
		    			// then call reset
		    			latestInstance = fragment.getLatestPeerInstance(connectingToDevice.deviceAddress);
		    			if((latestInstance != null) && (latestInstance.status == WifiP2pDevice.INVITED)) {
		    				connectingToDevice = null;
		    				fragment = null;
		    				this.destroy = true;
		    				return null;
		    			}
		    		}
		    		else
		    		{
		    			Thread.sleep(5*1000);
		    		}
	    		}
	    		catch (InterruptedException e) 
				{
	    			Logger.e(TAG, "Sleep failed in CheckInvitedStuckTask");
					e.printStackTrace();
				}
    		}
    	}
    	
    	@Override
    	protected void onPostExecute(Void result) {
    		if(destroy)
    		{
    			//Toast.makeText(getApplicationContext(), "Got Stuck in Invited mode. Resetting..!!", Toast.LENGTH_SHORT).show();
    			callDisconnect();
    		}
    	}
    }
    
    public class OnFileTransferCompleteTask  extends AsyncTask<Void, Void, Void>
    {

		@Override
		protected Void doInBackground(Void... params) {
			while(!FileTransferService.isOfflineFileTransferFinished)
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

	
}
