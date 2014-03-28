package com.bsb.hike.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bsb.hike.filetransfer.FileTransferManager;

public class ConnectivityChangeReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		// TODO Auto-generated method stub
		Log.d(getClass().getSimpleName(), "Connectivity Change Occured");
		FileTransferManager.getInstance(context).networkSwitched();
	}

}