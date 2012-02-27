package com.bsb.hike.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.bsb.hike.R;
import com.bsb.hike.tasks.ActivityCallableTask;

public class HikePreferences extends PreferenceActivity
{

	private ActivityCallableTask mTask;
	ProgressDialog mDialog;

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return ((mTask != null) && (!mTask.isFinished())) ? mTask : null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof ActivityCallableTask)
		{
			setBlockingTask((ActivityCallableTask) retained);
			mTask.setActivity(this);
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}

		mTask = null;
	}

	public void setBlockingTask(ActivityCallableTask task)
	{
		Log.d("HikePreferences", "setting task:"+task.isFinished());
		if (!task.isFinished())
		{
			mTask = task;
			mDialog = ProgressDialog.show(this, "Account", "Deleting Account");	
		}
	}

	public void dismissProgressDialog()
	{
		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
		}
	}
}
