package com.bsb.hike.offline;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.HikePubSub;
import com.bsb.hike.BitmapModule.BitmapUtils;
import com.bsb.hike.BitmapModule.HikeBitmapFactory;
import com.bsb.hike.HikeConstants.ImageQuality;
import com.bsb.hike.db.HikeConversationsDatabase;
import com.bsb.hike.models.ConvMessage;
import com.bsb.hike.models.HikeFile;
import com.bsb.hike.models.HikeFile.HikeFileType;
import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

public class OfflineFileTransferManager {
	private final String TAG = "OfflineFileTransferManager";
	public static final String IP_SERVER = "192.168.49.1";
	private static OfflineFileTransferManager _instance;
	private BlockingQueue<OfflineInfoPacket> textMessageQueue = new LinkedBlockingQueue<OfflineInfoPacket>();
	private BlockingQueue<OfflineInfoPacket> fileTransferQueue = new LinkedBlockingQueue<OfflineInfoPacket>();
	private TextThread textThread;
	private FileTransferThread fileTransferThread;
	private volatile boolean isOfflineFileTransferFinished;
	private volatile boolean isOfflineTextTransferFinished;
	private WifiP2pDevice connectedDevice;
	private final int fileTransferPort = 18988;
	private final int textMessagePort = 18999;
	private final int SOCKET_TIMEOUT = 5000;
	
	private ServerSocket fileReceiveServerSocket = null;
	private ServerSocket textReceiveServerSocket = null;
	private boolean isConnected = false;
	private static int currentSizeReceived;
	
	private  OfflineFileTransferManager() {
		initFileTransferManager();
		isOfflineFileTransferFinished = true;
		isOfflineTextTransferFinished = true;
		textMessageQueue.clear();
		fileTransferQueue.clear();
	}
	
	public static OfflineFileTransferManager getInstance(){
		if(_instance == null)
		{
			synchronized (OfflineFileTransferManager.class)
			{
				if(_instance == null)
				{
					_instance = new OfflineFileTransferManager();
				}
			}
		}
		return _instance;
	}
	
	public void initFileTransferManager() { 
		(textThread = new TextThread()).start();
		(fileTransferThread = new FileTransferThread()).start();
	}
	
	public boolean getIsOfflineFileTransferFinished()
	{
		return isOfflineFileTransferFinished;
	}
	
	public void setIsOfflineFileTransferFinished(boolean value)
	{
		isOfflineFileTransferFinished = value;
	}
	
	public void sendMessage(OfflineInfoPacket offlineInfoPacket){
		if(offlineInfoPacket.getIsText())
		{
			textMessageQueue.add(offlineInfoPacket);
		}
		else
		{
			//Log.d(TAG, offlineInfoPacket.getFilePath());
			fileTransferQueue.add(offlineInfoPacket);
		}
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
	
	private boolean sendOfflineText(OfflineInfoPacket offlineInfoPacket) {
		isOfflineTextTransferFinished = false;
		boolean isSent = true;
		String message = offlineInfoPacket.getMsgText();
		String host    = offlineInfoPacket.getHost();   
		Socket socket = new Socket();
		int port = textMessagePort;
		InputStream is = null;
		try {
			Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
			Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
			
			OutputStream stream = socket.getOutputStream();
			is = new ByteArrayInputStream(message.getBytes());
			byte[]  type  =  new byte[1];
			type[0] =  (byte)offlineInfoPacket.getType();
			stream.write(type,0,type.length);
			byte[] intToBArray = com.bsb.hike.offline.Utils.intToByteArray((int)message.length());
			int s = intToBArray.length;
			stream.write(intToBArray, 0, s);
			copyFile(is, stream);
			Log.d(WiFiDirectActivity.TAG, "Client: Data written");
		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			isOfflineTextTransferFinished = true;
		}
		return isSent;
	}
	
	private boolean transferFile(OfflineInfoPacket offlineInfoPacket) {
		isOfflineFileTransferFinished = false;
		boolean isSent = true;
		String fileUri = offlineInfoPacket.getFilePath();
		String host    = offlineInfoPacket.getHost();   
		Socket socket = new Socket();
		int port = fileTransferPort;
		Log.d(TAG, "Start");
		InputStream is = null;
		try {
			Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
			Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
			
			OutputStream stream = socket.getOutputStream();
			File f = new File(fileUri);
			Log.d(TAG, "Middle "+f.getPath());
			try {
				is = new FileInputStream(fileUri);
			} catch (FileNotFoundException e) {
				Log.d(WiFiDirectActivity.TAG, e.toString());
			}
			byte[]  type  =  new byte[1];
			type[0] =  (byte)offlineInfoPacket.getType();
			stream.write(type,0,type.length);
			byte[] intToBArray = com.bsb.hike.offline.Utils.intToByteArray((int)f.length());
			int s = intToBArray.length;
			stream.write(intToBArray, 0, s);
			isSent = copyFile(is, stream);
			//Log.d(TAG, "Client: Data written");
		} catch (IOException e) {
			Log.d(TAG, e.getMessage());
			isSent = false;
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			isOfflineFileTransferFinished = true;
		}
		Log.d(TAG, "Completed!!");
		return isSent;
	}
	
	public boolean getIsOfflineTextTransferFinished() {
		return isOfflineTextTransferFinished;
	}

	public void setOfflineTextTransferFinished(boolean isOfflineTextTransferFinished) {
		this.isOfflineTextTransferFinished = isOfflineTextTransferFinished;
	}
	
	public void switchOnReceivers(Activity UIToUpdate, WifiP2pDevice connectedDevice)
	{
		isConnected = true;
	    new FileReceiveServerAsyncTask(UIToUpdate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
	    new TextReceiveAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
	    this.connectedDevice = connectedDevice;
	}
	
	public void switchOffReceivers()
	{
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
	
	public class TextReceiveAsyncTask extends AsyncTask<Void,Void, Void>
	{
		byte type;
		int textSize;
		/*
		public TextReceiveAsyncTask(Context context) {
			this.context = context;
		}*/
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
	                setOfflineTextTransferFinished(false);
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
	                	//Toast.makeText(getActivity(), "Proper Connection could not be established. Disconnecting.. Please Retry!!", Toast.LENGTH_SHORT).show();
	                	//((DeviceActionListener) getActivity()).disconnect();
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
	    			setOfflineTextTransferFinished(true);
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

		byte type;
		private File f = null;
        private File dirs = null;
        private int fileSize;
        private int numOfIterations;
        private Activity activity;
        
        private Thread publishProgressThread;
		/**
		 * @param context
		 */
		public FileReceiveServerAsyncTask(Activity activity) {
			this.activity = activity;
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
	                setIsOfflineFileTransferFinished(false);
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
		                		while(!interrupted())
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
	                path =  f.getAbsolutePath();
					String result = path;
					if (path != null) {
			            if(connectedDevice == null)
			            {
			            	//Toast.makeText(getActivity(), "Proper Connection could not be established. Disconnecting.. Please Retry!!", Toast.LENGTH_SHORT).show();
			            	//((DeviceActionListener) getActivity()).disconnect();
			            	return null;
			            }
			            
						if(type==1)
						{
							JSONObject metadata = null;
							ConvMessage convMessage = null;
							String quality = null;
							try {
								metadata = getFileTransferMetadata(f.getName(), null, HikeFileType.APK, null, null, -1, f.getPath(), (int) f.length(), quality);
								convMessage = createConvMessage(f.getName(), metadata, connectedDevice.deviceName, false);
								convMessage.setMappedMsgID(System.currentTimeMillis());
								HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);
								
							} catch (JSONException e) {
								e.printStackTrace();
							}
							//Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
							//String Foldername =  result.substring(0, result.lastIndexOf("/"));
							//intent.setDataAndType(Uri.parse(Foldername), "*/*");
							//context.startActivity(Intent.createChooser(intent, "Open folder"));
							
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
								try 
								{
									metadata = getFileTransferMetadata(f.getName(), null, HikeFileType.IMAGE, thumbnailString, thumbnail, -1, f.getPath(), (int) f.length(), quality);
									convMessage = createConvMessage(f.getName(), metadata, connectedDevice.deviceName, false);
									convMessage.setMappedMsgID(System.currentTimeMillis());
									HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);
									long msgId = convMessage.getMsgID();
								} 
								catch (JSONException e) 
								{
									e.printStackTrace();
								}
							}
							else
							{
								/*Intent intent = new Intent();
								intent.setAction(Intent.ACTION_VIEW);
								intent.setDataAndType(Uri.parse("file://" + result), "image/*");
								context.startActivity(intent);*/
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
								/*
								Intent intent = new Intent();
								intent.setAction(Intent.ACTION_VIEW);
								intent.setDataAndType(Uri.parse("file://" + result), "video/*");
								context.startActivity(intent);*/
							}
						}
						else if(type==4)
						{
							JSONObject metadata = null;
							ConvMessage convMessage = null;
							String quality = null;
							try {
								metadata = getFileTransferMetadata(f.getName(), null, HikeFileType.AUDIO, null, null, -1, f.getPath(), (int) f.length(), quality);
								convMessage = createConvMessage(f.getName(), metadata, connectedDevice.deviceName, false);
								convMessage.setMappedMsgID(System.currentTimeMillis());
								HikeConversationsDatabase.getInstance().addConversationMessages(convMessage);
								
							} catch (JSONException e) {
								e.printStackTrace();
							}
							/*Intent intent = new Intent();
							intent.setAction(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.parse("file://" + result), "audio/*");
							context.startActivity(intent);*/
						}
						
					}
					setIsOfflineFileTransferFinished(true);
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
    				
    				Intent intent = new Intent("SHOW_PROGRESS");
	    			intent.putExtra("STATUS", "CONNECTED OFFLINE");
	    			if(activity != null)
	    				activity.sendBroadcast(intent);
    				
    				//fragmentToUpdate.peersStatus.put(connectedDevice, "Available for file transfer");
    				//((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    			}
    			else
    			{
	    			DecimalFormat df = new DecimalFormat("#.##");
	    			percentage *= 100;
	    			percentage =  Double.valueOf(df.format(percentage));
	    			Intent intent = new Intent("SHOW_PROGRESS");
	    			intent.putExtra("STATUS",   percentage + "% File Received");
	    			if(activity != null)
	    				activity.sendBroadcast(intent);
	    			//peersStatus.put(connectedDevice, "Receiveing file " + (percentage)+ "%");
					//((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    			}
				super.onProgressUpdate(values);
		}
	}

	class TextThread extends Thread
	{
		OfflineInfoPacket packet;
		@Override
		public void run() {
			try
			{
				while((packet = textMessageQueue.take()) != null)
				{
					boolean val;
					do {
						val = sendOfflineText(packet); //transferFile(packet);
						sleep(50);
					} while (val == false);
					
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	class FileTransferThread extends Thread
	{
		OfflineInfoPacket packet;
		@Override
		public void run() {
			try 
			{
				while((packet = fileTransferQueue.take()) != null)
				{
					boolean val;
					do {
						val = transferFile(packet);
						sleep(200);
					} while (val == false);
					
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	class Test extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			
			super.onPostExecute(result);
		}
	}
}

