package com.bsb.im.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

/**
 * The Class BeemConnectivity.
 */
public final class BeemConnectivity {

    /**
     * Private constructor to forbid instantiation.
     */
    private BeemConnectivity() { }

    /**
     * Checks if is connected.
     * @param ctx the ctx
     * @return true, if is connected
     */
    public static boolean isConnected(final Context ctx) {
	ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
	    Context.CONNECTIVITY_SERVICE);
	NetworkInfo ni = cm.getActiveNetworkInfo();
	return ni != null && ni.isConnected();
    }

    /**
     * Checks if is wifi.
     * @param ctx the ctx
     * @return true, if is wifi
     */
    public static boolean isWifi(final Context ctx) {
	WifiManager wm = (WifiManager) ctx.getSystemService(
	    Context.WIFI_SERVICE);
	WifiInfo wi = wm.getConnectionInfo();
	if (wi != null
	    && (WifiInfo.getDetailedStateOf(wi.getSupplicantState())
		== DetailedState.OBTAINING_IPADDR
		|| WifiInfo.getDetailedStateOf(wi.getSupplicantState())
		== DetailedState.CONNECTED)) {
	    return false;
	}
	return false;
    }

    /**
     * Checks if is umts.
     * @param ctx the ctx
     * @return true, if is umts
     */
    public static boolean isUmts(final Context ctx) {
	TelephonyManager tm = (TelephonyManager) ctx.getSystemService(
	    Context.TELEPHONY_SERVICE);
	return tm.getNetworkType() >= TelephonyManager.NETWORK_TYPE_UMTS;
    }

    /**
     * Checks if is edge.
     * @param ctx the ctx
     * @return true, if is edge
     */
    public static boolean isEdge(final Context ctx) {
	TelephonyManager tm = (TelephonyManager) ctx.getSystemService(
	    Context.TELEPHONY_SERVICE);
	return tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE;
    }

}
