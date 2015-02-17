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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.TextUtils;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.platform.CardRenderer.ViewHolder;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.musicg.processor.IntensityProcessor;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends ListFragment implements PeerListListener, ConnectionInfoListener {

	public static final String IP_SERVER = "192.168.49.1";
	public static int PORT = 8988;
	private static boolean server_running = false;
	private final int RESULT_OK = -1;
	
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;
    private int mIconImageSize;
    private IconLoader iconLoader;
    private WifiP2pInfo info;
    Intent intent;
    private Object syncMsisdn;
    List<String> peers_msisdn = new ArrayList<String>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	if(syncMsisdn == null)
    		syncMsisdn = new Object();
    	this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.conversation_item, peers));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }

    private static String getDeviceStatus(int deviceStatus) {
        Log.d(WiFiDirectActivity.TAG, "Peer status :" + deviceStatus);
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

    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		String localIP = Utils.getLocalIPAddress();
		Log.d(WiFiDirectActivity.TAG,"localip " + localIP);
		
		// Trick to find the ip in the file /proc/net/arp
		try
		{
			switch(resultCode)
			{
				case RESULT_OK:
					String client_mac_fixed = new String(device.deviceAddress).replace("99", "19");
					String clientIP = Utils.getIPFromMac(client_mac_fixed);
			        Log.d(WiFiDirectActivity.TAG,"client_mac_address: " +  client_mac_fixed);
			        Log.d(WiFiDirectActivity.TAG,"clientIP" +  clientIP);
			        
					String uri = data.getStringExtra(FileTransferService.EXTRAS_FILE_PATH);
					//TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
					//statusText.setText("Sending: " + uri);
					Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
					Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
					serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
					serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
			
					if(localIP.equals(IP_SERVER)){
						serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, clientIP);
					}else{
						serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, IP_SERVER);
					}
			
					serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
					long start = System.currentTimeMillis();
					getActivity().startService(serviceIntent);
					long end = System.currentTimeMillis();
					Toast.makeText(getActivity(), "Time taken: "+(end-start) +"ms", Toast.LENGTH_SHORT).show();
					break;
				default:
					Toast.makeText(getActivity(), "File not selected!", Toast.LENGTH_SHORT).show();
			}
		}
		catch(NullPointerException e)
		{
			Log.e("wifidirectdemo", "Something went wrong. Please select the file again!");
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
	        WifiP2pDevice device = peers.get(position);
	    	ContactInfo deviceContact = ContactManager.getInstance().getContact(peers_msisdn.get(position));
	    	String phoneNumber = "";
	    	if(deviceContact == null)
	    		phoneNumber = peers_msisdn.get(position);
	    	else
	    		phoneNumber = deviceContact.getName();
	    	if(device.status!=WifiP2pDevice.CONNECTED){
	    		
	    		Conversation conv = new Conversation(peers_msisdn.get(position), phoneNumber, false);
	        	intent = com.bsb.hike.utils.Utils.createIntentForConversation(getActivity(), conv);
	        	intent.putExtra("OfflineDeviceName", device.deviceAddress);
	        	WifiP2pConfig config = new WifiP2pConfig();
	    		config.deviceAddress = device.deviceAddress;
	    		config.wps.setup = WpsInfo.PBC;
	    		if (progressDialog != null && progressDialog.isShowing()) {
	    			progressDialog.dismiss();
	    		}
	    		progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
	    				"Connecting to :" + device.deviceAddress, true, true,
	    				new DialogInterface.OnCancelListener(){
	                @Override
	                public void onCancel(DialogInterface dialog) {
	                    Log.d("wifidirectdemo", "Hello cancelled");
	                }
	            });
	    		
	    		((DeviceActionListener) getActivity()).connect(config, 0);
	    	}
	    	else
	    	{
	    		Conversation conv = new Conversation(peers_msisdn.get(position), phoneNumber, false);
	        	intent = com.bsb.hike.utils.Utils.createIntentForConversation(getActivity(), conv);
	        	intent.putExtra("OfflineDeviceName", device.deviceAddress);
	        	startActivity(intent);
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
            	
            	
                //TextView top = (TextView) v.findViewById(R.id.device_name);
                
                //TextView bottom = (TextView) v.findViewById(R.id.device_details);
            	
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
            	ImageView avatarView =  (ImageView) v.findViewById(R.id.avatar);
        		iconLoader.loadImage(device.deviceName, true, avatarView, false, true, true);
        		
        		/*
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status));
                }*/
            }

            return v;

        }
    }

    /**
     * Update UI for this device.
     * 
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device.status));
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if(syncMsisdn == null)
        	syncMsisdn = new Object();
        synchronized(syncMsisdn){
	        peers.clear();
	        WifiP2pDevice device;
	        for(int i=0; i<peerList.getDeviceList().size(); i++)
	        {
	        	device = (WifiP2pDevice) (peerList.getDeviceList().toArray())[i];
	        	if(device.deviceName.startsWith("+"))
	        		peers.add(device);
	        }
	        
	        
	        for(int i=0; i<peers.size(); i++){
	        	peers_msisdn.add(peers.get(i).deviceName);
	        }
	        
	        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
	        if (peers.size() == 0) {
	            Log.d(WiFiDirectActivity.TAG, "No devices found");
	            return;
	        }
        }
    }

    public void clearPeers() {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * 
     */
    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        
                    }
                });
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config, int numOfTries);

        void disconnect();
    }

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
    	this.info = info;
    	if (!server_running){
			new ServerAsyncTask(getActivity()).execute();
			server_running = true;
		}
    	if(intent != null)
    		startActivity(intent);	
	}
	
	public static boolean copyFile(InputStream inputStream, OutputStream out) {
		byte buf[] = new byte[1024];
		int len;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);

			}
			out.close();
			inputStream.close();
		} catch (IOException e) {
			Log.d(WiFiDirectActivity.TAG, e.toString());
			return false;
		}
		return true;
	}
	
	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */
	public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

		private final Context context;

		/**
		 * @param context
		 */
		public ServerAsyncTask(Context context) {
			this.context = context;
		}

		@Override
		protected String doInBackground(Void... params) {
			String path = "";
			try {
				ServerSocket serverSocket = new ServerSocket(PORT);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                InputStream inputstream = client.getInputStream();
                byte[] typeArr = new byte[1];
                inputstream.read(typeArr, 0, typeArr.length);
                byte type = typeArr[0];
                File f = null;
                switch(type){
                case 1:
                    f = new File(Environment.getExternalStorageDirectory() + "/"
                            + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis() + ".apk");
                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();
                }
                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                server_running = false;
                path =  f.getAbsolutePath();
			} catch (IOException e) {
				Log.e(WiFiDirectActivity.TAG, e.getMessage());
			}
			return path;
		}

		/*
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				//statusText.setText("File copied - " + result);
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				String Foldername =  result.substring(0, result.lastIndexOf("/"));
				intent.setDataAndType(Uri.parse(Foldername), "*/*");
				context.startActivity(Intent.createChooser(intent, "Open folder"));
				
			}

		}

		/*
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			//statusText.setText("Opening a server socket");
		}

	}

}
