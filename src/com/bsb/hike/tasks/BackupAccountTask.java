package com.bsb.hike.tasks;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.bsb.hike.db.DBBackupRestore;

public class BackupAccountTask extends AsyncTask<Void, Void, Boolean> implements ActivityCallableTask
{
	private Context ctx;

	public static interface BackupAccountListener
	{
		public void accountBacked(boolean isSuccess);
	}

	private BackupAccountListener listener;
	
	public BackupAccountTask(Context context, BackupAccountListener activity)
	{
		this.ctx = context;
		this.listener = activity;
	}

	@Override
	protected Boolean doInBackground(Void... unused)
	{
		return DBBackupRestore.getInstance(ctx).backupDB();
	}

	@Override
	protected void onPostExecute(Boolean result)
	{
		listener.accountBacked(result);
	}

	@Override
	public void setActivity(Activity activity)
	{
		this.listener = (BackupAccountListener) activity;
		
	}

	@Override
	public boolean isFinished()
	{
		// TODO Auto-generated method stub
		return false;
	}
}
