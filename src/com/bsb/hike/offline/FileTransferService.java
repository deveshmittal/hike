// Copyright 2011 Google Inc. All Rights Reserved.

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
import java.nio.charset.StandardCharsets;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

	public static boolean isOfflineFileTransferFinished = true;
	private static final int SOCKET_TIMEOUT = 5000;
	public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
	public static final String EXTRAS_FILE_PATH = "file_url";
	public static final String EXTRAS_ADDRESS = "go_host";
	public static final String EXTRAS_PORT = "go_port";
	public static final String ACTION_SEND_TEXT = "SEND_TEXT";
	public FileTransferService(String name) {
		super(name);
	}

	public FileTransferService() {
		super("FileTransferService");
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {

		Context context = getApplicationContext();
		if (intent.getAction().equals(ACTION_SEND_FILE)) {
			isOfflineFileTransferFinished =  false;
			String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
			String host = intent.getExtras().getString(EXTRAS_ADDRESS);
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_PORT);

			try {
				Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
				socket.bind(null);
				if(host == null)
					host = "0.0.0.0";
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
				OutputStream stream = socket.getOutputStream();
				ContentResolver cr = context.getContentResolver();
				File f = new File(fileUri);
				Log.d(WiFiDirectActivity.TAG, ""+f.length());
				InputStream is = null;
				try {
					is = new FileInputStream(fileUri);
				} catch (FileNotFoundException e) {
					Log.d(WiFiDirectActivity.TAG, e.toString());
				}
				byte[]  type  =  new byte[1];
				type[0] =  (byte) intent.getExtras().getInt("fileType");
				stream.write(type,0,type.length);
				byte[] intToBArray = Utils.intToByteArray((int)f.length());
				int s = intToBArray.length;
				stream.write(intToBArray, 0, s);
				OfflineFileTransferManager.copyFile(is, stream);
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
				isOfflineFileTransferFinished =  true;
			}

		}
		else  
		{
			isOfflineFileTransferFinished =  false;
			String message = intent.getExtras().getString("message");
			String host = intent.getExtras().getString(EXTRAS_ADDRESS);
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_PORT);

			try {
				Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
				OutputStream stream = socket.getOutputStream();
				ContentResolver cr = context.getContentResolver();
				
				InputStream is = null;
				is = new ByteArrayInputStream(message.getBytes());
				byte[]  type  =  new byte[1];
				type[0] =  (byte) intent.getExtras().getInt("fileType");
				stream.write(type,0,type.length);
				byte[] intToBArray = Utils.intToByteArray((int)message.length());
				int s = intToBArray.length;
				stream.write(intToBArray, 0, s);
				OfflineFileTransferManager.copyFile(is, stream);
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
				isOfflineFileTransferFinished =  true;
			}
		}
	}
}
