package com.bsb.hike.offline;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

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
	private final int fileTransferPort = 18988;
	private final int textMessagePort = 18999;
	private final int SOCKET_TIMEOUT = 5000;
	
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
			byte[] intToBArray = Utils.intToByteArray((int)message.length());
			int s = intToBArray.length;
			stream.write(intToBArray, 0, s);
			DeviceListFragment.copyFile(is, stream);
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
			byte[] intToBArray = Utils.intToByteArray((int)f.length());
			int s = intToBArray.length;
			stream.write(intToBArray, 0, s);
			isSent = DeviceListFragment.copyFile(is, stream);
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
	
}

