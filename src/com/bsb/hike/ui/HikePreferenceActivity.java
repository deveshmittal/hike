package com.bsb.hike.ui;

import com.bsb.hike.tasks.ActivityCallableTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;

public class HikePreferenceActivity extends Activity
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
