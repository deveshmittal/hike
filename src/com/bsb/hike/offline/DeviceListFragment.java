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
import java.util.HashMap;
import java.util.List;

import org.apache.http.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
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
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.R;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;

import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ContactInfo;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.Conversation;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.modules.contactmgr.ContactManager;
import com.bsb.hike.smartImageLoader.IconLoader;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

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
    private HashMap<WifiP2pDevice, String> peersStatus = new HashMap<WifiP2pDevice, String>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;
    private int mIconImageSize;
    private IconLoader iconLoader;
    private WifiP2pInfo info;
    public static Intent intent;
    private Object syncMsisdn;
    private WifiP2pDevice currentDevice;
    private static int currentSizeReceived = 0;
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

		String localIP = com.bsb.hike.offline.Utils.getLocalIPAddress();
		Log.d(WiFiDirectActivity.TAG,"localip " + localIP);
		
		// Trick to find the ip in the file /proc/net/arp
		try
		{
			switch(resultCode)
			{
				case RESULT_OK:
					String client_mac_fixed = new String(currentDevice.deviceAddress).replace("99", "19");
					String clientIP = com.bsb.hike.offline.Utils.getIPFromMac(client_mac_fixed);
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
    		Log.d("DeviceListFragment", peers.get(position).deviceAddress + "-----> " + peers.get(position).status );
    		if(FileTransferService.isOfflineFileTransferFinished == false)
    		{
    			if(peers.get(position).deviceAddress.compareTo(currentDevice.deviceAddress)==0)
    			{
    				ContactInfo deviceContact = ContactManager.getInstance().getContact(peers_msisdn.get(position));
    		    	String phoneNumber = "";
    		    	if(deviceContact == null)
    		    		phoneNumber = peers_msisdn.get(position);
    		    	else
    		    		phoneNumber = deviceContact.getName();
    				
    		    	Conversation conv = new Conversation(peers_msisdn.get(position), phoneNumber, false);
    	        	intent = com.bsb.hike.utils.Utils.createIntentForConversation(getActivity(), conv);
    	        	intent.putExtra("OfflineDeviceName", currentDevice.deviceAddress);
    	        	startActivity(intent);
    	        	return;
    			}
    			else
    			{
    				Toast.makeText(getActivity(), "You are currently sending file to " + device.deviceAddress, Toast.LENGTH_LONG).show();
    				return;
    			}
    		}
    		
	        currentDevice = peers.get(position);
	    	ContactInfo deviceContact = ContactManager.getInstance().getContact(peers_msisdn.get(position));
	    	String phoneNumber = "";
	    	if(deviceContact == null)
	    		phoneNumber = peers_msisdn.get(position);
	    	else
	    		phoneNumber = deviceContact.getName();

	    	if(currentDevice.status != WifiP2pDevice.CONNECTED)
	    	{	
	    		Conversation conv = new Conversation(peers_msisdn.get(position), phoneNumber, false);
	        	intent = com.bsb.hike.utils.Utils.createIntentForConversation(getActivity(), conv);
	        	intent.putExtra("OfflineDeviceName", currentDevice.deviceAddress);
	        	
	        	WifiP2pConfig config = new WifiP2pConfig();
	    		config.deviceAddress = currentDevice.deviceAddress;
	    		config.wps.setup = WpsInfo.PBC;
	    		if (progressDialog != null && progressDialog.isShowing()) {
	    			progressDialog.dismiss();
	    		}
	    		progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
	    				"Connecting to :" + currentDevice.deviceAddress, true, true,
	    				new DialogInterface.OnCancelListener(){
	                @Override
	                public void onCancel(DialogInterface dialog) {
	                    Log.d("wifidirectdemo", "Hello cancelled");
	                }
	            });
	    		
	    		((DeviceActionListener) getActivity()).connect(config, 0, currentDevice);
    	     }
	    	
	    	else
	    	{
	    		Conversation conv = new Conversation(peers_msisdn.get(position), phoneNumber, false);
	        	intent = com.bsb.hike.utils.Utils.createIntentForConversation(getActivity(), conv);
	        	intent.putExtra("OfflineDeviceName", currentDevice.deviceAddress);
	        	
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
        		//v.findViewById(id)
            	ImageView avatarView =  (ImageView) v.findViewById(R.id.avatar);
        		iconLoader.loadImage(device.deviceName, true, avatarView, false, true, true);
        		TextView deviceStatus =  (TextView) v.findViewById(R.id.last_message_timestamp);
        		deviceStatus.setText(getDeviceStatus(device.status));
        		TextView chatStatus = (TextView) v.findViewById(R.id.last_message);
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
	        	peersStatus.put(peers.get(i), "Available for File Transfer");
	        }
	        
	        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
	        if (peers.size() == 0) {
	            Log.d(WiFiDirectActivity.TAG, "No devices found");
	            return;
	        }
        }
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
    public void clearPeers() {
        peers.clear();
        peersStatus.clear();
        peers_msisdn.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * Show a progress bar to let the know the user know discoverPeers is running
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

        void callDisconnect();

        void connect(WifiP2pConfig config, int numOfTries, WifiP2pDevice connectingToDevice);

        void disconnect();
    }

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
    	this.info = info;
    	// No check for HoneyComb since WiFi Direct runs only on devices with Android 4+
    	if (!server_running){
			new ServerAsyncTask(getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
				currentSizeReceived++;	
			}
			out.close();
			inputStream.close();
		} catch (IOException e) {
			Log.d(WiFiDirectActivity.TAG, e.toString());
			return false;
		}
		return true;
	}
	
	private JSONObject getFileTransferMetadata(String fileName, String fileType, HikeFileType hikeFileType, String thumbnailString, Bitmap thumbnail, long recordingDuration,
			String sourceFilePath, int fileSize, String img_quality) throws JSONException
	{
		JSONArray files = new JSONArray();
		files.put(new HikeFile(fileName, TextUtils.isEmpty(fileType) ? HikeFileType.toString(hikeFileType) : fileType, thumbnailString, thumbnail, recordingDuration,
				sourceFilePath, fileSize, true, img_quality).serialize());
		JSONObject metadata = new JSONObject();
		metadata.put(HikeConstants.FILES, files);
		return metadata;
	}
	
	private ConvMessage createConvMessage(String fileName, JSONObject metadata, String msisdn, boolean isRecipientOnhike) throws JSONException
	{
		long time = System.currentTimeMillis() / 1000;
		ConvMessage convMessage = new ConvMessage(fileName, msisdn, time, ConvMessage.State.SENT_UNCONFIRMED);
		convMessage.setMetadata(metadata);
		convMessage.setSMS(!isRecipientOnhike);
		convMessage.setIsSent(false);
		convMessage.setState(ConvMessage.State.RECEIVED_READ);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		return convMessage;
	}
	
	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */
	public class ServerAsyncTask extends AsyncTask<Void,Void, String> {

		private final Context context;
		byte type;
		private int size = 0;
		private int numOfIterations = 0;
		private File f = null;
        private File dirs = null;
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
               
                //Log.d(WiFiDirectActivity.TAG, "Server: connection done.. Receiving File");
                //Toast.makeText(, text, duration)
                //DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                FileTransferService.isOfflineFileTransferFinished = false;
                InputStream inputstream = client.getInputStream();
                byte[] typeArr = new byte[1];
                inputstream.read(typeArr, 0, typeArr.length);
                byte[] intArray = new byte[4];
                inputstream.read(intArray, 0, 4);
                size = com.bsb.hike.offline.Utils.byteArrayToInt(intArray);
                Log.d(WiFiDirectActivity.TAG, ""+size);
                currentSizeReceived = 0;
                numOfIterations = size/1024;
                type = typeArr[0];
                (new Thread() {
                	public void run()
                	{
                		publishProgress();
                		try {
							Thread.sleep(1*1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
                	}
                }).start();
                
                switch(type){
                case 1:
                    f = new File(Environment.getExternalStorageDirectory() + "/"
                            + "Hike/Media/hike Others" + "/wifip2pshared-" + System.currentTimeMillis() + ".apk");
                    dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();
                    break;
                case 2:
                	 f = new File(Environment.getExternalStorageDirectory() + "/"
                             + "Hike/Media/hike Images" + "/wifip2pshared-" + System.currentTimeMillis() + ".jpg");
                     dirs = new File(f.getParent());
                     if (!dirs.exists())
                         dirs.mkdirs();
                     f.createNewFile();
                     break;
                case 3:
                	f = new File(Environment.getExternalStorageDirectory() + "/"
                            + "Hike/Media/hike Videos" + "/wifip2pshared-" + System.currentTimeMillis() + ".mp4");
                    dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();
                    break;
                case 4:
                	f = new File(Environment.getExternalStorageDirectory() + "/"
                            + "Hike/Media/hike Audio" + "/wifip2pshared-" + System.currentTimeMillis() + ".mp3");
                    dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();
                    break;
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
				if(type==1)
				{
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
					String Foldername =  result.substring(0, result.lastIndexOf("/"));
					intent.setDataAndType(Uri.parse(Foldername), "*/*");
					context.startActivity(Intent.createChooser(intent, "Open folder"));
					
				}
				else if(type==2)
				{
					/*Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse("file://" + result), "image/*");
					context.startActivity(intent);*/
					Bitmap thumbnail = null;
					String thumbnailString = null;
					String quality = null;
					Bitmap.Config config = Bitmap.Config.RGB_565;
					if(Utils.hasJellyBeanMR1()){
						config = Bitmap.Config.ARGB_8888;
					}
					thumbnail = HikeBitmapFactory.scaleDownBitmap(result, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX, HikeConstants.MAX_DIMENSION_THUMBNAIL_PX,
							config, false, false);
					thumbnail = Utils.getRotatedBitmap(result, thumbnail);
					quality = ImageQuality.IMAGE_QUALITY_MEDIUM;
					if(thumbnail != null)
					{
						int compressQuality = 25;
						byte [] tBytes = BitmapUtils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG, compressQuality);
						thumbnail = HikeBitmapFactory.decodeByteArray(tBytes, 0, tBytes.length);
						thumbnailString = Base64.encodeToString(tBytes, Base64.DEFAULT);
						Logger.d(getClass().getSimpleName(), "Thumbnail Size : " + tBytes.length);
						JSONObject metadata = null;
						ConvMessage convMessage = null;
						try {
							metadata = getFileTransferMetadata(f.getName(), null, HikeFileType.IMAGE, thumbnailString, thumbnail, -1, f.getPath(), (int) f.length(), quality);
							convMessage = createConvMessage(f.getName(), metadata, peers_msisdn.get(0), false);
							HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					else
					{
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse("file://" + result), "image/*");
						context.startActivity(intent);
					}
				}
				else if(type==3)
				{
					Bitmap thumbnail = null;
					String thumbnailString = null;
					String quality = null;
					thumbnail = ThumbnailUtils.createVideoThumbnail(f.getPath(), MediaStore.Images.Thumbnails.MICRO_KIND);
					if(thumbnail != null)
					{
						int compressQuality = 75;
						byte [] tBytes = BitmapUtils.bitmapToBytes(thumbnail, Bitmap.CompressFormat.JPEG, compressQuality);
						thumbnail = HikeBitmapFactory.decodeByteArray(tBytes, 0, tBytes.length);
						thumbnailString = Base64.encodeToString(tBytes, Base64.DEFAULT);
						Logger.d(getClass().getSimpleName(), "Thumbnail Size : " + tBytes.length);
						JSONObject metadata = null;
						ConvMessage convMessage = null;
						try {
							metadata = getFileTransferMetadata(f.getName(), null, HikeFileType.VIDEO, thumbnailString, thumbnail, -1, f.getPath(), (int) f.length(), quality);
							convMessage = createConvMessage(f.getName(), metadata, peers_msisdn.get(0), false);
							HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					else
					{
						Intent intent = new Intent();
						intent.setAction(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse("file://" + result), "video/*");
						context.startActivity(intent);
					}
				}
				else if(type==4)
				{
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.parse("file://" + result), "audio/*");
					context.startActivity(intent);
				}
				
			}
			FileTransferService.isOfflineFileTransferFinished = true;
			if(!server_running)
			{
				new ServerAsyncTask(getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				server_running = true;
			}
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
			
			if(fragment!=null)
            {
            	for(WifiP2pDevice cpeer: fragment.peers)
            	{
            		if(cpeer.status==WifiP2pDevice.CONNECTED)
            		{
            			int percentage = 0;
        				if(numOfIterations != 0)
        					percentage = currentSizeReceived/numOfIterations;
        				peersStatus.put(cpeer, "Receiveing file " + percentage + "%");
        				((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
            		}
            	}
            }
			super.onProgressUpdate(values);
		}

	}

}
