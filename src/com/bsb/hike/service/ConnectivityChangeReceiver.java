package com.bsb.hike.service;

import com.bsb.hike.filetransfer.FileTransferManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ConnectivityChangeReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		// TODO Auto-generated method stub
		Log.d(getClass().getSimpleName(), "Connectivity Change Occured");
		FileTransferManager.getInstance(context).setChunkSize();
	}

}