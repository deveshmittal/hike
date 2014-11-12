package com.bsb.hike.tasks;

import android.os.AsyncTask;

import com.bsb.hike.modules.contactmgr.ContactManager;

public class SyncContactExtraInfo extends AsyncTask<Void, Void, Void>
{

	@Override
	protected Void doInBackground(Void... params)
	{
		ContactManager.getInstance().syncContactExtraInfo();
		return null;
	}
}
