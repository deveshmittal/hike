package com.bsb.hike.offline;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.util.Log;

public class WifiP2pConnectionManager implements ChannelListener
{
	private final String TAG = "WifiP2pConnectionManager";
	private Context context;
	private WifiP2pManager manager;
	private WifiManager wifiManager;
	private Channel channel;
    private SharedPreferences settings;
    private String myMsisdn;
    private boolean retryChannel = false;
    private boolean isWifiP2pEnabled = false;
    private PeerListListener peerListListener;
    private GroupInfoListener groupInfoListener;
    private WifiP2pConnectionManagerListener wifiP2pConnectionManagerListener; 
    private volatile boolean isOfflineFileTransferOn = false;
    
	public WifiP2pConnectionManager(Context context, WifiP2pConnectionManagerListener wListener, PeerListListener pListener, GroupInfoListener gListener)
	{
		this.context = context;
		this.peerListListener = pListener;
		this.groupInfoListener = gListener;
		this.wifiP2pConnectionManagerListener = wListener;
		initialise(context);
	}
	
	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
	
	private void initialise(Context context)
	{
        isOfflineFileTransferOn = true;
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, context.getMainLooper(), this);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager.isWifiEnabled() == false)
        	wifiManager.setWifiEnabled(true);
        //wifiManager.startScan(); 
        /*manager.createGroup(channel, new ActionListener() {
			
			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onFailure(int reason) {
				// TODO Auto-generated method stub
				
			}
		});*/
        setDeviceNameAsMsisdn();   
	}
	
	private void setDeviceNameAsMsisdn() {
		/*
         * Setting device Name to msisdn
         */
        try {
        	settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
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
	}

	public boolean getIsOfflineFileTransferOn()
	{
		return isOfflineFileTransferOn;
	}
	
	public void setIsOfflineFileTransferOn(boolean isOfflineFileTransferOn)
	{
		synchronized(WifiP2pConnectionManager.class)
		{
			this.isOfflineFileTransferOn = isOfflineFileTransferOn;
		}
	}
	
	public void connect(WifiP2pConfig config)
	{
		manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

			@Override
            public void onFailure(int reason) {
               wifiP2pConnectionManagerListener.connectFailure(reason);
            }
        });
	}
	
	public Boolean createHotspot(WifiP2pDevice wifiP2pDevice)
	{
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
        myMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		String targetMsisdn =  wifiP2pDevice.deviceName;
		Boolean result = false;
		try
		{
			//Method getWifiConfig = wifiManager.getClass().getMethod("getWifiApConfiguration",null);
			//WifiConfiguration myConfig = (WifiConfiguration) getWifiConfig.invoke(wifiManager,null);
			 
			wifiManager.setWifiEnabled(false);
			Method enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			String ssid  =   "h_" +  myMsisdn + "_" + targetMsisdn;
			String pass  =   new StringBuffer(ssid).reverse().toString();
			WifiConfiguration  myConfig =  new WifiConfiguration();
			myConfig.SSID = ssid;
			myConfig.preSharedKey  = pass ;
			myConfig.status =   WifiConfiguration.Status.ENABLED;
			myConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP); 
			myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP); 
			myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP); 
			myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			result = (Boolean) enableWifi.invoke(wifiManager, myConfig, true);
		} 
		catch (Exception e) 
		{
			Log.e(this.getClass().toString(), "", e);
			result = false;
		}
		
		return result;
	}
	
	
	
	public Boolean closeHotspot(WifiP2pDevice wifiP2pDevice)
	{
		settings = context.getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
        myMsisdn = settings.getString(HikeMessengerApp.MSISDN_SETTING, null);
		String targetMsisdn =  wifiP2pDevice.deviceName;
		Boolean result = false;
		try
		{
			//Method getWifiConfig = wifiManager.getClass().getMethod("getWifiApConfiguration",null);
			//WifiConfiguration myConfig = (WifiConfiguration) getWifiConfig.invoke(wifiManager,null);
			 
			//wifiManager.setWifiEnabled(false);
			Method enableWifi = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			String ssid  =   "h_" +  myMsisdn + "_" + targetMsisdn;
			String pass  =   new StringBuffer(ssid).reverse().toString();
			WifiConfiguration  myConfig =  new WifiConfiguration();
			myConfig.SSID = ssid;
			myConfig.preSharedKey  = pass ;
			myConfig.status =   WifiConfiguration.Status.ENABLED;
			myConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			myConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			myConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP); 
			myConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP); 
			myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP); 
			myConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			result = (Boolean) enableWifi.invoke(wifiManager, myConfig, false);
		} 
		catch (Exception e) 
		{
			Log.e(this.getClass().toString(), "", e);
			result = false;
		}
		
		return result;
	}
	
	public Boolean connectToHotspot(String ssid) {	
		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = "\"" +ssid +"\"";
		wc.preSharedKey  = "\"" + new StringBuffer(ssid).reverse().toString()  +  "\"";
		//wc.preSharedKey  = "\"testing123\"";
		wifiManager.addNetwork(wc);
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {
		    if(i!=null && i.SSID != null && i.SSID.equals(wc.SSID)) {
		         wifiManager.disconnect();
		         boolean status  =  wifiManager.enableNetwork(i.networkId, true);
		         wifiManager.reconnect();               
		         return status;
		    }           
		 }
		return false;
	}
	
	public HashMap<String, ScanResult> getDistinctWifiNetworks()
	{
		
		//wifiManager.startScan();
   	 	List<ScanResult> results = wifiManager.getScanResults();
   	
	   	HashMap<String,ScanResult> distinctNetworks = new HashMap<String, ScanResult>();
	   	for(ScanResult scanResult :  results)
	   	{
	   		//if(scanResult.SSID.startsWith("hike_"))
	   		if((scanResult.SSID)!=null &&  !scanResult.SSID.isEmpty() && scanResult.SSID.contains(myMsisdn) ){
		   		if(!distinctNetworks.containsKey(scanResult))
		   		{
		   			distinctNetworks.put(scanResult.SSID, scanResult); 
		   		}
		   		else
		   		{
		   			if(WifiManager.compareSignalLevel(scanResult.level, distinctNetworks.get(scanResult.SSID).level)>0)
		   			{
		   				distinctNetworks.put(scanResult.SSID, scanResult);
		   			}
		   		}
	   		}
	   	}
	   	
	   	wifiP2pConnectionManagerListener.updateWifiNetworks(distinctNetworks);
	   	return distinctNetworks;
		//return null;
   }
	
	/*public void getClientList(boolean onlyReachables, FinishScanListener finishListner) {
		getClientList(onlyReachables, 300, finishListner );
	}

	
	 
	public void getClientList(final boolean onlyReachables, final int reachableTimeout, final FinishScanListener finishListener) {


		Runnable runnable = new Runnable() {
			public void run() {

				BufferedReader br = null;
				final ArrayList<ClientScanResult> result = new ArrayList<ClientScanResult>();
				
				try {
					br = new BufferedReader(new FileReader("/proc/net/arp"));
					String line;
					while ((line = br.readLine()) != null) {
						String[] splitted = line.split(" +");

						if ((splitted != null) && (splitted.length >= 4)) {
							String mac = splitted[3];

							if (mac.matches("..:..:..:..:..:..")) {
								boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(reachableTimeout);

								if (!onlyReachables || isReachable) {
									result.add(new ClientScanResult(splitted[0], splitted[3], splitted[5], isReachable));
								}
							}
						}
					}
				} catch (Exception e) {
					Log.e(this.getClass().toString(), e.toString());
				} finally {
					try {
						br.close();
					} catch (IOException e) {
						Log.e(this.getClass().toString(), e.getMessage());
					}
				}

				// Get a handler that can be used to post to the main thread
				Handler mainHandler = new Handler(context.getMainLooper());
				Runnable myRunnable = new Runnable() {
					@Override
					public void run() {
						finishListener.onFinishScan(result);
					}
				};
				mainHandler.post(myRunnable);
			}
		};

		Thread mythread = new Thread(runnable);
		mythread.start();
		
	}
	*/
	public void disconnect()
	{
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
	
	public void cancelConnect()
	{
		manager.cancelConnect(channel, new ActionListener() {
        	
            @Override
            public void onSuccess() {
                wifiP2pConnectionManagerListener.cancelConnectSuccess();
            }

            @Override
            public void onFailure(int reasonCode) {
                wifiP2pConnectionManagerListener.cancelConnectFailure(reasonCode);
            }
        });
	}
	
	public void enableDiscovery()
	{
		if (!wifiManager.isWifiEnabled()) {
			//wifiManager.setWifiEnabled(true);
            //wifiP2pConnectionManagerListener.notifyWifiNotEnabled();
        }
		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
        	
            @Override
            public void onSuccess() {
            	wifiP2pConnectionManagerListener.discoverySuccess();
            }

            @Override
            public void onFailure(int reasonCode) {
            	wifiP2pConnectionManagerListener.discoveryFailure(reasonCode);
            }
		});
	}
	public void resetData()
	{
		wifiP2pConnectionManagerListener.resetData();
	}
	
	public Context getContext()
	{
		return this.context;
	}
	
	public void updateDevice(WifiP2pDevice device)
	{
		if(device != null)
			wifiP2pConnectionManagerListener.updateMyDevice(device);
	}
	public boolean checkChannel()
	{
		return (channel != null);
	}
	
	public boolean checkManager()
	{
		return (manager != null);
	}
	
	public void requestGroupInfo()
	{
		manager.requestGroupInfo(channel, groupInfoListener);
	}
	
	public void requestPeers()
	{
		if (manager != null) {
            manager.requestPeers(channel, peerListListener);
		}
	}
	
	@Override
	public void onChannelDisconnected() {
		if (manager != null && !retryChannel) {
            retryChannel = true;
            manager.initialize(context, context.getMainLooper(), this);
        } else {
        	wifiP2pConnectionManagerListener.channelDisconnectedFailure();
        }
		
	}

	public void startScan() {
		wifiManager.startScan();
	}
	
	public void resetWifi(){
		wifiManager.setWifiEnabled(false);
		wifiManager.setWifiEnabled(true);
	}
	
	public void startWifi()
	{
		wifiManager.setWifiEnabled(true);
	}
}

interface WifiP2pConnectionManagerListener
{
	void channelDisconnectedFailure();
	
	void updateMyDevice(WifiP2pDevice device);

	void resetData();
	
	void notifyWifiNotEnabled();
	
	void connectSuccess();
	
	void connectFailure(int reasonCode);
	
	void cancelConnectSuccess();
	
	void cancelConnectFailure(int reasonCode);
	
	void discoverySuccess();
	
	void discoveryFailure(int reasonCode);
	
	void updateWifiNetworks(HashMap<String, ScanResult> distinctNetworks);
}

