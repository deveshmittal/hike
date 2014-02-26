package com.bsb.hike.tasks;

import android.os.AsyncTask;

import com.bsb.hike.db.HikeUserDatabase;

public class SyncContactExtraInfo extends AsyncTask<Void, Void, Void>
{

	@Override
	protected Void doInBackground(Void... params)
	{
		HikeUserDatabase.getInstance().syncContactExtraInfo();
		return null;
	}
}
