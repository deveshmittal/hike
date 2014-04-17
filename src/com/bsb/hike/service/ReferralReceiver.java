package com.bsb.hike.service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.bsb.hike.utils.Logger;
import com.bsb.hike.utils.Utils;

/**
 * 
 * @author Rishabh Triggered when the app is activated for the first time. Contains info on which channel promoted the download of this app.
 */
public class ReferralReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		// Workaround for Android security issue:
		// http://code.google.com/p/android/issues/detail?id=16006
		try
		{
			Bundle extras = intent.getExtras();
			if (extras != null)
			{
				extras.containsKey(null);
			}
		}
		catch (Exception e)
		{
			return;
		}

		List<NameValuePair> referralParams = new ArrayList<NameValuePair>();

		String referrer = intent.getStringExtra("referrer");
		if (TextUtils.isEmpty(referrer))
		{
			Logger.w(getClass().getSimpleName(), "No referrer");
			return;
		}

		Logger.d(getClass().getSimpleName(), "Referrer: " + referrer);
		Scanner referrerScanner = new Scanner(referrer);
		URLEncodedUtils.parse(referralParams, referrerScanner, Charset.defaultCharset().displayName());

		Utils.storeReferralParams(context, referralParams);
	}
}
