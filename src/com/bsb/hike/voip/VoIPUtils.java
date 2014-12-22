package com.bsb.hike.voip;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class VoIPUtils {

    public static boolean isWifiConnected(Context context) {
    	ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    	if (mWifi.isConnected()) {
    		return true;
    	}    	
    	else
    		return false;
    }	
	
    public static String getLocalIpAddress(Context c) {
    	
    	if (isWifiConnected(c)) {
    		WifiManager wifiMgr = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
    		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
    		int ipAddress = wifiInfo.getIpAddress();
    		
    	    // Convert little-endian to big-endianif needed
    	    if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
    	        ipAddress = Integer.reverseBytes(ipAddress);
    	    }

    	    byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

    	    String ipAddressString;
    	    try {
    	        ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
    	    } catch (UnknownHostException ex) {
    	        Log.e("WIFIIP", "Unable to get host address.");
    	        ipAddressString = null;
    	    }

    	    return ipAddressString;    		
    	} else {
	        try {
	            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	                NetworkInterface intf = en.nextElement();
	                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                    InetAddress inetAddress = enumIpAddr.nextElement();
	                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
	                        return inetAddress.getHostAddress();
	                    }
	                }
	            }
	        } catch (NullPointerException ex) {
	            ex.printStackTrace();
	        } catch (SocketException ex) {
	            ex.printStackTrace();
	        }
        return null;
    	}
    }	
	
}
