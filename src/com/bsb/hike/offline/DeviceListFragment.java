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
import java.text.DecimalFormat;
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
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
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
public class DeviceListFragment extends ListFragment implements PeerListListener ,GroupInfoListener{

	public static final String IP_SERVER = "192.168.49.1";
	public static int fileTransferPort = 18988;
	public static int textMessagePort = 18999;
	private ServerSocket textReceiveServerSocket;
    private ServerSocket fileReceiveServerSocket;
	
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private HashMap<WifiP2pDevice, String> peersStatus = new HashMap<WifiP2pDevice, String>();
    public ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;
    private int mIconImageSize;
    private IconLoader iconLoader;
    public static  WifiP2pGroup groupInfo = null;
    public static Intent intent;
    private Object syncMsisdn;
    private WifiP2pDevice connectedDevice = null;
    private static int currentSizeReceived = 0;
    private List<String> peers_msisdn = new ArrayList<String>();
    private static  int numOfIterations = 0;
    private int fileSize = 0;
    private int textSize = 0;
    public static boolean isReconnecting = false;
    private boolean isConnected = false;
    
    

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
    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	if(syncMsisdn == null)
    		syncMsisdn = new Object();
    	synchronized(syncMsisdn){
    		Log.d("DeviceListFragment", peers.get(position).deviceAddress + "-----> " + peers.get(position).status );
    		if(!OfflineFileTransferManager.getInstance().getIsOfflineFileTransferFinished() ||
    				!OfflineFileTransferManager.getInstance().getIsOfflineTextTransferFinished())
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
    				Toast.makeText(getActivity(), "You are currently sending file to " + device.deviceAddress, Toast.LENGTH_LONG).show();
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
        //if (progressDialog != null && progressDialog.isShowing()) {
        //    progressDialog.dismiss();
        //}
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
	        		Logger.d(WiFiDirectActivity.TAG, "No peers found");
	        	}
	            return;
	        }
        }
        
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
        															++(WiFiDirectActivity.tries), WiFiDirectActivity.connectingToDevice);
        		else
        			Toast.makeText(getActivity(), "Device not present in peer list!!", Toast.LENGTH_SHORT).show();
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
    public interface DeviceActionListener {
        void callDisconnect();

        void connect(WifiP2pConfig config, int numOfTries, WifiP2pDevice ConnectingToDevice);

        void disconnect();
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
	
	public void clearConnectionDetails() {
		connectedDevice = null;
		//switch off AsyncTasks
		isConnected = false;
		try {
			if (fileReceiveServerSocket != null)
				fileReceiveServerSocket.close();
			if (textReceiveServerSocket != null)
				textReceiveServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
		ConvMessage convMessage = new ConvMessage(fileName, msisdn, time, ConvMessage.State.RECEIVED_READ);
		convMessage.setMetadata(metadata);
		convMessage.setSMS(!isRecipientOnhike);
		convMessage.setState(ConvMessage.State.RECEIVED_READ);
		HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
		return convMessage;
	}
	
	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */
	
	public class TextReceiveAsyncTask extends AsyncTask<Void,Void, Void>
	{
		private final Context context;
		byte type;
		
		public TextReceiveAsyncTask(Context context) {
			this.context = context;
		}
		@Override
		protected Void doInBackground(Void... params) {
			String message = "";
			try 
			{
				textReceiveServerSocket = new ServerSocket(textMessagePort);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                
                while(isConnected)
                {
	                Socket client = textReceiveServerSocket.accept();
	                OfflineFileTransferManager.getInstance().setOfflineTextTransferFinished(false);
	                InputStream inputstream = client.getInputStream();
	                byte[] typeArr = new byte[1];
	                inputstream.read(typeArr, 0, typeArr.length);
	                byte[] intArray = new byte[4];
	                inputstream.read(intArray, 0, 4);
	                textSize = com.bsb.hike.offline.Utils.byteArrayToInt(intArray);
	                Log.d(WiFiDirectActivity.TAG, ""+textSize);
	                type = typeArr[0];
	                byte[] msg  =  new byte[textSize];
	            	inputstream.read(msg,0,textSize);
	            	client.close();
	            	message = (new String(msg));
	            	
	            	if(connectedDevice == null)
	                {
	                	Toast.makeText(getActivity(), "Proper Connection could not be established. Disconnecting.. Please Retry!!", Toast.LENGTH_SHORT).show();
	                	((DeviceActionListener) getActivity()).disconnect();
	                	return null;
	                }
	    			ConvMessage convMessage =  new ConvMessage(message,connectedDevice.deviceName,System.currentTimeMillis()/1000,ConvMessage.State.RECEIVED_UNREAD);
	    			convMessage.setMappedMsgID(System.currentTimeMillis());
	    			if(convMessage.getMessage().compareTo("Nudge!")==0)
	    			{
	    				try {
	    					JSONObject md = ((convMessage.getMetadata() != null) ? convMessage.getMetadata().getJSON() : new JSONObject());
	    					md.put(HikeConstants.POKE, true);
	    					convMessage.setMetadata(md);
	    				} catch (JSONException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    			
	    			HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);
	    			HikeMessengerApp.getPubSub().publish(HikePubSub.MESSAGE_RECEIVED, convMessage);
	    			OfflineFileTransferManager.getInstance().setOfflineTextTransferFinished(true);
                }
			}
			catch (IOException e) 
			{
				Log.e(WiFiDirectActivity.TAG, e.getMessage());
			}
			return null;
		}
	}
	
	public class FileReceiveServerAsyncTask extends AsyncTask<Void,Void, Void> {

		private final Context context;
		byte type;
		private File f = null;
        private File dirs = null;
        private Thread publishProgressThread;
		/**
		 * @param context
		 */
		public FileReceiveServerAsyncTask(Context context) {
			this.context = context;
		}

		@Override
		protected Void doInBackground(Void... params) {
			String path = "";
			try 
			{
				fileReceiveServerSocket = new ServerSocket(fileTransferPort);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                while(isConnected)
                {
	                Socket client = fileReceiveServerSocket.accept();
	                OfflineFileTransferManager.getInstance().setIsOfflineFileTransferFinished(false);
	                Log.d("OfflineFileTransferManager", "Receiving");
	                InputStream inputstream = client.getInputStream();
	                byte[] typeArr = new byte[1];
	                inputstream.read(typeArr, 0, typeArr.length);
	                byte[] intArray = new byte[4];
	                inputstream.read(intArray, 0, 4);
	                fileSize = com.bsb.hike.offline.Utils.byteArrayToInt(intArray);
	                Log.d(WiFiDirectActivity.TAG, ""+fileSize);
	                currentSizeReceived = 0;
	                numOfIterations = fileSize/1024 + ((fileSize%1024!=0)?1:0);
	                type = typeArr[0];
	                if(publishProgressThread == null)
		                	publishProgressThread = (new Thread() {
		                	public void run()
		                	{
		                		while(!Thread.interrupted())
		                		{
			                		publishProgress();
			                		try {
										Thread.sleep(1*1000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
		                		}
		                	}
		                });
	                if(!(publishProgressThread.isAlive()))
	                	publishProgressThread.start();
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
	                client.close();
	                //serverSocket.close();
	                //fileTransferServerRunning = false;
	                path =  f.getAbsolutePath();
					String result = path;
					if (path != null) {
			            if(connectedDevice == null)
			            {
			            	Toast.makeText(getActivity(), "Proper Connection could not be established. Disconnecting.. Please Retry!!", Toast.LENGTH_SHORT).show();
			            	((DeviceActionListener) getActivity()).disconnect();
			            	return null;
			            }
			            
						if(type==1)
						{
							Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
							String Foldername =  result.substring(0, result.lastIndexOf("/"));
							intent.setDataAndType(Uri.parse(Foldername), "*/*");
							context.startActivity(Intent.createChooser(intent, "Open folder"));
							
						}
						else if(type==2)
						{
							 
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
								Logger.d(getClass().getSimpleName(), "Thumbnail fileSize : " + tBytes.length);
								JSONObject metadata = null;
								ConvMessage convMessage = null;
								try {
									metadata = getFileTransferMetadata(f.getName(), null, HikeFileType.IMAGE, thumbnailString, thumbnail, -1, f.getPath(), (int) f.length(), quality);
									convMessage = createConvMessage(f.getName(), metadata, connectedDevice.deviceName, false);
									convMessage.setMappedMsgID(System.currentTimeMillis());
									String name = connectedDevice.deviceName;
									HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);
									long msgId = convMessage.getMsgID();
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
								Logger.d(getClass().getSimpleName(), "Thumbnail fileSize : " + tBytes.length);
								JSONObject metadata = null;
								ConvMessage convMessage = null;
								try {
									metadata = getFileTransferMetadata(f.getName(), null, HikeFileType.VIDEO, thumbnailString, thumbnail, -1, f.getPath(), (int) f.length(), quality);
									convMessage = createConvMessage(f.getName(), metadata, connectedDevice.deviceName, false);
									convMessage.setMappedMsgID(System.currentTimeMillis());
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
					OfflineFileTransferManager.getInstance().setIsOfflineFileTransferFinished(true);
					if(publishProgressThread != null)
						publishProgressThread.interrupt();
				}
			}
			catch (IOException e) 
			{
				Log.e(WiFiDirectActivity.TAG, e.getMessage());
			}
			return null;
		}
					
		@Override
		protected void onProgressUpdate(Void... values) {
        		Double percentage = 0.0;
    			if(numOfIterations != 0)
    				percentage = (currentSizeReceived/(double)numOfIterations);
    			else
    				percentage = 100.0;
    			
    			if( (percentage*100)>=100)
    				percentage *= 100.0;
    			if (percentage >= 100)
    			{
    				peersStatus.put(connectedDevice, "Available for file transfer");
    				((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    			}
    			else
    			{
	    			DecimalFormat df = new DecimalFormat("#.##");
	    			percentage =  Double.valueOf(df.format(percentage));
	    			peersStatus.put(connectedDevice, "Receiveing file " + (percentage)+ "%");
					((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    			}
				super.onProgressUpdate(values);
		}

	}

	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		if(group != null)
    	{
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
		    }
		    DeviceListFragment.groupInfo = group;
		    isConnected = true;
		    
		    new FileReceiveServerAsyncTask(getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		    new TextReceiveAsyncTask(getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
		    
		    WiFiDirectActivity.connectingDeviceConfig = null;
		    WiFiDirectActivity.connectingToDevice = null;
		    WiFiDirectActivity.tries = 0;
		    
	    	if(group.isGroupOwner())
	    	{
	    		for(WifiP2pDevice  client :  group.getClientList()){
	    			connectedDevice = client;
				}
	    	}
	    	else
	    	{
				WifiP2pDevice groupOwner  =  group.getOwner();
	    		connectedDevice = groupOwner;
	    	}
    	}
	    if(intent != null)
	    {
	    	startActivity(intent);
	    	getActivity().sendBroadcast(new Intent("SHOW_OFFLINE_CONNECTED"));
	    }
	}

}
