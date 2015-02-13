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

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bsb.hike.R;
import com.bsb.hike.offline.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

	public static final String IP_SERVER = "192.168.49.1";
	public static int PORT = 8988;
	private static boolean server_running = false;

	protected static final int CHOOSE_FILE_RESULT_CODE = 20;
	private final int RESULT_OK = -1;
	private View mContentView = null;
	private WifiP2pDevice device;
	private WifiP2pInfo info;
	ProgressDialog progressDialog = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mContentView = inflater.inflate(R.layout.device_detail, null);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
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
				
				((DeviceActionListener) getActivity()).connect(config);

			}
		});

		mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						((DeviceActionListener) getActivity()).disconnect();
					}
				});

		mContentView.findViewById(R.id.btn_launch_gallery).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// Allow user to pick an image from Gallery or other
						// registered apps
						Intent intent = new Intent(getActivity(),Explorerwindow.class );
						//intent.putExtra(FileExplorer.DEVICE_ADDRESS, device.deviceAddress);
						startActivityForResult(intent, FileExplorer.APP_PATH);
					}
				});

		return mContentView;
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
					TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
					statusText.setText("Sending: " + uri);
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

	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.info = info;
		this.getView().setVisibility(View.VISIBLE);

		// The owner IP is now known.
		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(getResources().getString(R.string.group_owner_text)
				+ ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
						: getResources().getString(R.string.no)));

		// InetAddress from WifiP2pInfo struct.
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

		mContentView.findViewById(R.id.btn_launch_gallery).setVisibility(View.VISIBLE);

		if (!server_running){
			new ServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();
			server_running = true;
		}

		// hide the connect button
		//mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
	}

	/**
	 * Updates the UI with device data
	 * 
	 * @param device the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(device.deviceAddress);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(device.toString());

	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.status_text);
		view.setText(R.string.empty);
		mContentView.findViewById(R.id.btn_launch_gallery).setVisibility(View.GONE);
		this.getView().setVisibility(View.GONE);
	}

	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */
	public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

		private final Context context;
		private final TextView statusText;

		/**
		 * @param context
		 * @param statusText
		 */
		public ServerAsyncTask(Context context, View statusText) {
			this.context = context;
			this.statusText = (TextView) statusText;
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
				statusText.setText("File copied - " + result);
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
			statusText.setText("Opening a server socket");
		}

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

}
