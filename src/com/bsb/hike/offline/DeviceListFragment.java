/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.util.TextUtils;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends ListFragment implements PeerListListener ,GroupInfoListener{

	private final String TAG = "DeviceListFragment";
	public static final String IP_SERVER = "192.168.49.1";
	public static int fileTransferPort = 18988;
	public static int textMessagePort = 18999;
	
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    HashMap<WifiP2pDevice, String> peersStatus = new HashMap<WifiP2pDevice, String>();
    public ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;
    private int mIconImageSize;
    private IconLoader iconLoader;
    public static  WifiP2pGroup groupInfo = null;
    public static Intent intent;
    private Object syncMsisdn;
    private WifiP2pDevice connectedDevice = null;
    public List<String> peers_msisdn = new ArrayList<String>();
    public static boolean isReconnecting = false;
    private int mode = 0;
    
    // variables of WifiNetworkListFragment
    private final int POST_TO_FRAGMENT = 0;
	private List<ScanResult> wifipeers = new ArrayList<ScanResult>();
	private Handler mHandler;
	Thread updateNetwork =  null ; 
	private boolean requestDialogShown = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	if(syncMsisdn == null)
    		syncMsisdn = new Object();
    	mHandler = new Handler(getActivity().getMainLooper())
		{
			@Override
			public void handleMessage(Message msg)
			{
				switch(msg.what)
				{
					case POST_TO_FRAGMENT:
						@SuppressWarnings("unchecked")
						HashMap<String, ScanResult> nearbyNetworks = (HashMap<String, ScanResult>) msg.obj;
						updateFragment(nearbyNetworks); //update fragment list here
						break;
					default:
						super.handleMessage(msg);
				}
			}
			
		};
    	this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.conversation_item, peers));
    }
    
    public void updateFragment(HashMap<String, ScanResult> strength)
    {
		// update UI and also notify the user that a new network is available using HikeNotifications
    }
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }
    
    @Override
    public void onPause() {
    	updateNetwork.interrupt();
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	runNetworkScan();
    	super.onResume();
    }
    
 // starts a network scan for every 2sec
 	private void runNetworkScan()
 	{
 		if(isAdded())
 		{
 		   //((DeviceActionListener) getActivity()).resetWifi();
 		}
 		if(updateNetwork==null)
 		{
 			updateNetwork =  (new Thread()
 			{
 				@Override
 				public void run() {
 					while(!isInterrupted())
 					{
 						if(isAdded())
 						{
 							HashMap<String, ScanResult> nearbyNetworks = ((DeviceActionListener) getActivity()).getDistinctWifiNetworks();
 							Message targetMessage = mHandler.obtainMessage(POST_TO_FRAGMENT, nearbyNetworks);
 							targetMessage.sendToTarget();	
 							try 
 							{
 								sleep(2*1000);
 							} 
 							catch (InterruptedException e) 
 							{
 								e.printStackTrace();
 							}	
 						}
 						
 					}
 				}
 				
 			});
 			updateNetwork.start();
 		}
 	}
  
    public WifiP2pDevice getDevice() {
        return device;
    }

    private String getDeviceStatus(int deviceStatus) {
        Log.d(TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }
    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	
    	if(syncMsisdn == null)
    		syncMsisdn = new Object();
    	synchronized(syncMsisdn){
    		Log.d("DeviceListFragment", peers.get(position).deviceAddress + "-----> " + peers.get(position).status );
    		if((!OfflineFileTransferManager.getInstance().getIsOfflineFileTransferFinished() ||
    				!OfflineFileTransferManager.getInstance().getIsOfflineTextTransferFinished()) && (connectedDevice != null))
    		{
    			if(peers.get(position).deviceAddress.compareTo(connectedDevice.deviceAddress)==0)
    			{
    				ContactInfo deviceContact = ContactManager.getInstance().getContact(peers_msisdn.get(position));
    		    	String phoneNumber = "";
    		    	if(deviceContact == null)
    		    		phoneNumber = peers_msisdn.get(position);
    		    	else
    		    		phoneNumber = deviceContact.getName();
    				
    		    	Conversation conv = new Conversation(peers_msisdn.get(position), phoneNumber, false);
    	        	intent = com.bsb.hike.utils.Utils.createIntentForConversation(getActivity(), conv);
    	        	intent.putExtra("OfflineDeviceName", connectedDevice.deviceAddress);
    	        	intent.putExtra("OfflineActivity", "");
    	        	startActivity(intent);
    	        	return;
    			}
    			else
    			{
    				Toast.makeText(getActivity(), "You are currently sending file to " + connectedDevice.deviceAddress, Toast.LENGTH_LONG).show();
    				return;
    			}
    		}
    		
	        WifiP2pDevice currentDevice = peers.get(position);
	    	ContactInfo deviceContact = ContactManager.getInstance().getContact(peers_msisdn.get(position));
	    	String phoneNumber = "";
	    	if(deviceContact == null)
	    		phoneNumber = peers_msisdn.get(position);
	    	else
	    		phoneNumber = deviceContact.getName();

	    	if(currentDevice.status == WifiP2pDevice.CONNECTED || currentDevice.status == WifiP2pDevice.UNAVAILABLE)
	    	{
	    		Conversation conv = new Conversation(peers_msisdn.get(position), phoneNumber, true);
	        	intent = com.bsb.hike.utils.Utils.createIntentForConversation(getActivity(), conv);
	        	intent.putExtra("OfflineDeviceName", currentDevice.deviceAddress);
	        	intent.putExtra("OfflineActivity", "");
	        	startActivity(intent);
	    	}
	    	else
	    	{	
	    		Conversation conv = new Conversation(peers_msisdn.get(position), phoneNumber, false);
	        	intent = com.bsb.hike.utils.Utils.createIntentForConversation(getActivity(), conv);
	        	intent.putExtra("OfflineDeviceName", currentDevice.deviceAddress);
	        	intent.putExtra("OfflineActivity", "");
	        	
	        	WifiP2pConfig config = new WifiP2pConfig();
	    		config.deviceAddress = currentDevice.deviceAddress;
	    		config.wps.setup = WpsInfo.PBC;
	    		config.groupOwnerIntent = 0;
	    		
	    		//  mode - 0  wifi-direct 
	    		//  mode  - 1 wifi hotspot
	    		mode = 1;
	    		((DeviceActionListener) getActivity()).connect(config, 0, currentDevice,mode, intent);
    	     }
    	}
    }
    

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.conversation_item, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
            	Context context = getContext();
            	mIconImageSize = context.getResources().getDimensionPixelSize(R.dimen.icon_picture_size);
        		iconLoader = new IconLoader(context, mIconImageSize);
        		
        		ContactInfo deviceContact = ContactManager.getInstance().getContact(device.deviceName);
        		TextView contact_name = (TextView) v.findViewById(R.id.contact);
        		String phoneName = "";
        		if(deviceContact != null && !(TextUtils.isEmpty(deviceContact.getName())))
        			phoneName = deviceContact.getName();
        		else
        			phoneName = device.deviceName;
        		
        		contact_name.setText(phoneName);
        		//v.findViewById(id)
            	ImageView avatarView =  (ImageView) v.findViewById(R.id.avatar);
        		iconLoader.loadImage(device.deviceName, true, avatarView, false, true, true);
        		TextView deviceStatus =  (TextView) v.findViewById(R.id.last_message_timestamp);
        		String stat =   peersStatus.get(device);
        		deviceStatus.setText(stat);
        		
            }

            return v;

        }
    }

    /**
     * Update UI for this device.
     * 
     * @param device WifiP2pDevice object
     */
    /*public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device.status));
    }
*/
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if(syncMsisdn == null)
        	syncMsisdn = new Object();
        synchronized(syncMsisdn)
        {
	        peers.clear();
	        peers_msisdn.clear();
	        peersStatus.clear();
	        WifiP2pDevice device;
	        for(int i=0; i<peerList.getDeviceList().size(); i++)
	        {
	        	device = (WifiP2pDevice) (peerList.getDeviceList().toArray())[i];
	        	if(device.deviceName.startsWith("+"))
	        		peers.add(device);
	        }
	        
	        
	        for(int i=0; i<peers.size(); i++){
	        	peers_msisdn.add(peers.get(i).deviceName);
	        	peersStatus.put(peers.get(i), getDeviceStatus(peers.get(i).status));
	        }
	        
	        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
	        
	        /* no need to store groupInfo
	        if (peers.size() == 0) 
	        {
	        	if(groupInfo != null)
	        	{
		        	if(groupInfo.isGroupOwner())
		 		   	{
		 				for(WifiP2pDevice  client : groupInfo.getClientList()){
		 					peers.add(client);
		 					peers_msisdn.add(client.deviceName);
		 				    peersStatus.put(client,getDeviceStatus(client.status));
		 				}
		 		   	}
		 		   	else
		 		   	{
		 				WifiP2pDevice groupOwner = groupInfo.getOwner();
		 				peers.add(groupOwner);
		 				peers_msisdn.add(groupOwner.deviceName);
		 				peersStatus.put(groupOwner,getDeviceStatus(groupOwner.status));
		 		   	}
		 		   	((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
	        	}
	        	else
	        	{
	        		Logger.d(TAG, "No peers found");
	        	}
	            return;
	        }*/
        }
        
        /* Not doing reconnection for hotspot
        if(WiFiDirectActivity.connectingToDevice != null && isReconnecting == true && 
        		peers_msisdn.contains(WiFiDirectActivity.connectingToDevice.deviceName))
        {
        	if(peers.size() == 0)
        		Toast.makeText(getActivity(), "Device List Empty!!", Toast.LENGTH_SHORT).show();
        	else
        	{
        		isReconnecting = false;
        		if(peers.contains(WiFiDirectActivity.connectingToDevice))
        			((DeviceActionListener) getActivity()).connect(WiFiDirectActivity.connectingDeviceConfig,
        															++(WiFiDirectActivity.tries), WiFiDirectActivity.connectingToDevice, mode,null);
        		else
        			Toast.makeText(getActivity(), "Device not present in peer list!!", Toast.LENGTH_SHORT).show();
        	}
        }*/
    }
    
    public WifiP2pDevice getLatestPeerInstance(String deviceAddress)
    {
    	WifiP2pDevice latestInstance = null;
    	synchronized(syncMsisdn)
    	{
    		for(int i=0; i<peers.size(); i++)
    		{
    			if(peers.get(i).deviceAddress.compareTo(deviceAddress)==0)
    			{
    				latestInstance = peers.get(i);
    				return latestInstance;
    			}
    		}
    	}
    	return latestInstance;
    }
    
    public List<String> getLatestPeers()
    {
    	return peers_msisdn;
    }
    
    public void clearPeers() {
        peers.clear();
        peersStatus.clear();
        peers_msisdn.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
   
	
	public void clearConnectionDetails() {
		connectedDevice = null;
		OfflineFileTransferManager.getInstance().switchOffReceivers();
	}
	
	public void updatePeerStatus(WifiP2pDevice connectedDevice, String status)
	{
		peersStatus.put(connectedDevice, status);
		((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
	}

	// TODO : can delete everything related to GroupInfoListener
	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) 
	{
		if(group != null)
    	{
			if (progressDialog != null && progressDialog.isShowing()) 
			{
				progressDialog.dismiss();
		    }
		    groupInfo = group;
		    
		    //OfflineFileTransferManager.getInstance().switchOnReceivers(this);
		    //new FileReceiveServerAsyncTask(getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		    //new TextReceiveAsyncTask(getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		    
		    //WiFiDirectActivity.connectingDeviceConfig = null;
		    //WiFiDirectActivity.connectingToDevice = null;
		    //WiFiDirectActivity.tries = 0;
		    
	    	if(group.isGroupOwner())
	    	{
	    		for(WifiP2pDevice  client :  group.getClientList())
	    		{
	    			connectedDevice = client;
				}
	    	}
	    	else
	    	{
				WifiP2pDevice groupOwner  =  group.getOwner();
	    		connectedDevice = groupOwner;
	    	}
	    	OfflineFileTransferManager.getInstance().switchOnReceivers(this.getActivity(), connectedDevice.deviceName);
    	}
	    if(intent != null)
	    {
	    	startActivity(intent);
	    	getActivity().sendBroadcast(new Intent("SHOW_OFFLINE_CONNECTED"));
	    }
	}

}
