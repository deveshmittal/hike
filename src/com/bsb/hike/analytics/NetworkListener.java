/**
 * 
 */
package com.bsb.hike.analytics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * @author rajesh
 *
 */
public class NetworkListener extends BroadcastReceiver 
{
	Context context = null;
	
	enum NetworkType
	{
		NO_NETWORK,
		TWO_G,
		THREE_G,
		FOUR_G,
		WIFI
	}
	
	public NetworkListener(Context context) 
	{
		this.context = context.getApplicationContext();
	}

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			if(isUserConnected(context))
			{
				NetworkType networkType = getNetworkType();
				
				if(networkType == NetworkType.WIFI)
				{
					// TODO try sending all the logged data if this case occurs
					
				}
			}
		}
	}	
	
	private boolean isUserConnected(Context context)
	{
		if (context == null)
		{
			return false;
		}
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		return (cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected());
	}
	
	// Fetches the type of Internet connection the device is using
	public NetworkType getNetworkType()
	{
		int networkType = -1;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		// Contains all the information about current connection
		NetworkInfo info = cm.getActiveNetworkInfo();
		
		if (info != null)
		{
			if (!info.isConnected())
				return NetworkType.NO_NETWORK;
			
			// If device is connected via WiFi
			if (info.getType() == ConnectivityManager.TYPE_WIFI)
				return NetworkType.WIFI; // return 1024 * 1024;
			else
				networkType = info.getSubtype();
		}

		// There are following types of mobile networks
		switch (networkType)
		{
		case TelephonyManager.NETWORK_TYPE_HSUPA: // ~ 1-23 Mbps
		case TelephonyManager.NETWORK_TYPE_LTE: // ~ 10+ Mbps // API level 11
		case TelephonyManager.NETWORK_TYPE_HSPAP: // ~ 10-20 Mbps // API level 13
		case TelephonyManager.NETWORK_TYPE_EVDO_B: // ~ 5 Mbps // API level 9
			return NetworkType.FOUR_G;
		case TelephonyManager.NETWORK_TYPE_EVDO_0: // ~ 400-1000 kbps
		case TelephonyManager.NETWORK_TYPE_EVDO_A: // ~ 600-1400 kbps
		case TelephonyManager.NETWORK_TYPE_HSDPA: // ~ 2-14 Mbps
		case TelephonyManager.NETWORK_TYPE_HSPA: // ~ 700-1700 kbps
		case TelephonyManager.NETWORK_TYPE_UMTS: // ~ 400-7000 kbps
		case TelephonyManager.NETWORK_TYPE_EHRPD: // ~ 1-2 Mbps // API level 11
			return NetworkType.THREE_G;
		case TelephonyManager.NETWORK_TYPE_1xRTT: // ~ 50-100 kbps
		case TelephonyManager.NETWORK_TYPE_CDMA: // ~ 14-64 kbps
		case TelephonyManager.NETWORK_TYPE_EDGE: // ~ 50-100 kbps
		case TelephonyManager.NETWORK_TYPE_GPRS: // ~ 100 kbps
		case TelephonyManager.NETWORK_TYPE_IDEN: // ~25 kbps // API level 8
		case TelephonyManager.NETWORK_TYPE_UNKNOWN:
		default:
			return NetworkType.TWO_G;
		}
	}
}
