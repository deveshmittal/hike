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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.Animation.AnimationListener;
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
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
       
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
            	 //WifiNetworksListFragment wififragment = (WifiNetworksListFragment) getFragmentManager().findFragmentById(R.id.wifi_list);
            	 //wififragment.updateWifiNetworks(getDistinctWifiNetworks());  	 
            	getDistinctWifiNetworks();
            	 return true ;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
    public HashMap<String, ScanResult>  getDistinctWifiNetworks(){
        return connectionManager.getDistinctWifiNetworks();
    }
    
    @Override
    public void connect(WifiP2pConfig config, int numOfTries, WifiP2pDevice ConnectingToDevice,int mode, Intent intent) {
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
		    WiFiDirectActivity.connectingToDevice = ConnectingToDevice;
		    WiFiDirectActivity.connectingDeviceConfig = config;
		    WiFiDirectActivity.tries = numOfTries;
	        connectionManager.createHotspot(ConnectingToDevice); 
	        new checkConnectedHotspotTask(this,intent).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
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
				try 
				{
					Thread.sleep(1*1000);
				} 
				catch (InterruptedException e) 
				{
					Logger.e(TAG, "Sleep failed in OnFileTransferCompleteTask");
					e.printStackTrace();
				}
	    	}
			return null;
		}
		
		@Override
    	protected void onPostExecute(Void result) 
		{
    		callDisconnect();
    	}	  
    }
    
    public class checkConnectedHotspotTask extends AsyncTask<Void, Void ,ArrayList<ClientScanResult>>
    {
    	Context context;
    	Dialog dialog;
    	Intent intent ; 
    	Boolean isConnected;
    	ArrayList<ClientScanResult> temp;
    	public checkConnectedHotspotTask(Context context, Intent intent) {
			this.context =  context;
			isConnected = false;
			dialog  = new Dialog(context, R.style.Theme_CustomDialog);
			this.intent = intent;
			temp = null;
		}

    	protected void onPreExecute() {
    		/*HikeDialog.showDialog(context,HikeDialog.SHOW_OFFLINE_CONNECTION_STATUS,  new HikeDialog.HikeDialogListener()
			{
				@Override
				public void onSucess(Dialog dialog)
				{
					Log.d("dfsf", "afdaf");
				}
<<<<<<< HEAD

				@Override
				public void negativeClicked(Dialog dialog)
				{
					// TODO Auto-generated method stub
					
				}

				@Override
				public void positiveClicked(Dialog dialog)
				{
					
				}

				@Override
				public void neutralClicked(Dialog dialog)
				{
					// TODO Auto-generated method stub
					
				}
			});*/
    	   
    		dialog.setContentView(R.layout.connecting_offline);
   			dialog.setCancelable(true);
   			dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if(isConnected)
					{
					  startActivity(intent);
					  OfflineFileTransferManager.getInstance().switchOnReceivers((Activity) context, connectingToDevice.deviceName);
					}
					else
					{
						connectionManager.closeHotspot(WiFiDirectActivity.connectingToDevice);
						connectionManager.startWifi();
					}
					
				}
			});
   			dialog.show();
   			
   			long smileyOffset = 300;
   			long smileyDuration = 300;
   			long onsetTime  =  300;
   			setupDotsAnimation(dialog.getWindow(),smileyOffset, smileyDuration, onsetTime);
   			return;
    	}
    	
		@Override
		protected ArrayList<ClientScanResult> doInBackground(Void... params) 
		{
			int tries = 0;
			
			while(tries<15)
			{
				
				tries++;
			    if(temp!=null)
			    	break;
			    
			    else
			    {
				    BufferedReader br = null;
					final ArrayList<ClientScanResult> result = new ArrayList<ClientScanResult>();
					
					try 
					{
						br = new BufferedReader(new FileReader("/proc/net/arp"));
						String line;
						while ((line = br.readLine()) != null) 
						{
							String[] splitted = line.split(" +");
	
							if ((splitted != null) && (splitted.length >= 4)) 
							{
								String mac = splitted[3];
								if (mac.matches("..:..:..:..:..:..")) {
									boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(500);
	
									if (isReachable) 
									{
										result.add(new ClientScanResult(splitted[0], splitted[3], splitted[5], isReachable));
									}
								}
							}
						}
					}
					catch (Exception e) 
					{
						Log.e(this.getClass().toString(), e.toString());
					} 
					finally 
					{
						try 
						{
							br.close();
						} 
						catch (IOException e) 
						{
							Log.e(this.getClass().toString(), e.getMessage());
						}
					}
					if(result.size()>0)
						temp = result;
					else
					{
						try 
						{
							Thread.sleep(2000);
						} 
						catch (InterruptedException e) 
						{
							e.printStackTrace();
						}
					}
			    }
			}
				/*connectionManager.getClientList(false, new FinishScanListener() {
	
		  			@Override
		  			public void onFinishScan(final ArrayList<ClientScanResult> clients) {
		  				if(clients.size()>0)
		  				{
		  					Log.d(TAG, "YOYO");
		  					//Toast.makeText(getApplicationContext(), clients.get(0).getIpAddr(), Toast.LENGTH_SHORT).show();
		  					temp  =   clients;
		  					
		  				}
		  				else
		  				{
		  					Log.d(TAG, "Issue");
		  					try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		  					temp =  null;
		  					
		  				}
		  			}
		  		});*/
		   
            return temp;
		}
		
		@Override
		protected void onPostExecute(ArrayList<ClientScanResult> result) 
		{
			if(result!=null && result.size()>0)
			{
				//startActivity(intent);
				isConnected = true;
				dialog.dismiss();
				//OfflineFileTransferManager.getInstance().switchOnReceivers(this, ClientScanResult);  
				//Toast.makeText(getApplicationContext(), "Start Chattting man", Toast.LENGTH_LONG).show();
			}
			else
			{
				dialog.dismiss();
				Toast.makeText(getApplicationContext(),"Sorry Mate !!! Chat Later", Toast.LENGTH_LONG).show();
			}	
			super.onPostExecute(result);
		}

		
    	
    }

    private void setupDotsAnimation(Window window, long smileyOffset, long smileyDuration, long onsetTime)
	{

		final View dot0 = (View) window.findViewById(R.id.dot_left);
		final View dot1 = (View) window.findViewById(R.id.dot_center);
		final View dot2 = (View) window.findViewById(R.id.dot_right);
		
		ShapeDrawable circle0 = new ShapeDrawable(new OvalShape());
		circle0.getPaint().setColor(this.getResources().getColor(R.color.restoring_red));
		dot0.setBackground(circle0);
		ShapeDrawable circle1 = new ShapeDrawable(new OvalShape());
		circle1.getPaint().setColor(this.getResources().getColor(R.color.restoring_green));
		dot1.setBackground(circle1);
		ShapeDrawable circle2 = new ShapeDrawable(new OvalShape());
		circle2.getPaint().setColor(this.getResources().getColor(R.color.restoring_orange));
		dot2.setBackground(circle2);
		
		AlphaAnimation dotIn0 = new AlphaAnimation(0, 1);
		dotIn0.setDuration(100);
		AlphaAnimation dotOut0 = new AlphaAnimation(1, 0);
		dotOut0.setDuration(100);
		dotOut0.setStartOffset(200);
		RotateAnimation dotStay0 = new RotateAnimation(0, 360);
		dotStay0.setDuration(400);
		dotStay0.setStartOffset(300);
		
		final AnimationSet dota0 = new AnimationSet(true);
		dota0.addAnimation(dotIn0);
		dota0.addAnimation(dotOut0);
		dota0.addAnimation(dotStay0);
		dota0.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				AlphaAnimation dotIn = new AlphaAnimation(0, 1);
				dotIn.setDuration(100);
				AlphaAnimation dotOut = new AlphaAnimation(1, 0);
				dotOut.setDuration(100);
				dotOut.setStartOffset(200);
				RotateAnimation dotStay = new RotateAnimation(0, 360);
				dotStay.setDuration(400 + 200);
				dotStay.setStartOffset(300);
				AnimationSet dot = new AnimationSet(true);
				dot.addAnimation(dotIn);
				dot.addAnimation(dotOut);
				dot.addAnimation(dotStay);
				dot.setAnimationListener(this);
				dot0.startAnimation(dot);
			}
		});

		AlphaAnimation dotIn1 = new AlphaAnimation(0, 1);
		dotIn1.setDuration(100);
		AlphaAnimation dotOut1 = new AlphaAnimation(1, 0);
		dotOut1.setDuration(100);
		dotOut1.setStartOffset(200);
		RotateAnimation dotStay1 = new RotateAnimation(0, 360);
		dotStay1.setDuration(200);
		dotStay1.setStartOffset(300);
		
		final AnimationSet dota1 = new AnimationSet(true);
		dota1.addAnimation(dotIn1);
		dota1.addAnimation(dotOut1);
		dota1.addAnimation(dotStay1);
		dota1.setStartOffset(200);
		dota1.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				AlphaAnimation dotIn = new AlphaAnimation(0, 1);
				dotIn.setDuration(100);
				AlphaAnimation dotOut = new AlphaAnimation(1, 0);
				dotOut.setDuration(100);
				dotOut.setStartOffset(200);
				RotateAnimation dotStay = new RotateAnimation(0, 360);
				dotStay.setDuration(200 + 200);
				dotStay.setStartOffset(300);
				AnimationSet dot = new AnimationSet(true);
				dot.addAnimation(dotIn);
				dot.addAnimation(dotOut);
				dot.addAnimation(dotStay);
				dot.setStartOffset(200);
				dot.setAnimationListener(this);
				dot1.startAnimation(dot);
			}
		});
		
		AlphaAnimation dotIn2 = new AlphaAnimation(0, 1);
		dotIn2.setDuration(100);
		AlphaAnimation dotOut2 = new AlphaAnimation(1, 0);
		dotOut2.setDuration(100);
		dotOut2.setStartOffset(200);
		
		final AnimationSet dota2 = new AnimationSet(true);

		dota2.addAnimation(dotIn2);
		dota2.addAnimation(dotOut2);
		dota2.setStartOffset(400);
		dota2.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationStart(Animation animation)
			{}
			
			@Override
			public void onAnimationRepeat(Animation animation)
			{}
			
			@Override
			public void onAnimationEnd(Animation animation)
			{
				AlphaAnimation dotIn = new AlphaAnimation(0, 1);
				dotIn.setDuration(100);
				AlphaAnimation dotOut = new AlphaAnimation(1, 0);
				dotOut.setDuration(100);
				dotOut.setStartOffset(200);
				RotateAnimation dotStay = new RotateAnimation(0, 360);
				dotStay.setDuration(0 + 200);
				dotStay.setStartOffset(300);
				AnimationSet dot = new AnimationSet(true);
				dot.addAnimation(dotIn);
				dot.addAnimation(dotOut);
				dot.addAnimation(dotStay);
				dot.setAnimationListener(this);
				dot.setStartOffset(400);
				dot2.startAnimation(dot);
			}
		});
		
		//dot0.startAnimation(dota0);
		//dot1.startAnimation(dota1);
		//dot2.startAnimation(dota2);
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
		String err = new String();
        if(reasonCode==WifiP2pManager.BUSY) err="BUSY";
        if(reasonCode==WifiP2pManager.ERROR)err="ERROR";
        if(reasonCode==WifiP2pManager.P2P_UNSUPPORTED) err="P2P_UNSUPPORTED";
        //Log.e(TAG,"FAIL - couldnt start to discover peers code: "+err);
        
        try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//connectionManager.enableDiscovery();
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


	@Override
	public void startScan() {
		connectionManager.startScan();
	}

	@Override
	public void resetWifi() {
		connectionManager.resetWifi();
	}

	@Override
	public void forgetWifiNetwork() {
	   connectionManager.forgetWifiNetwork();
	}
}


