package com.bsb.hike.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.TextView;

import com.bsb.hike.HikeConstants;
import com.bsb.hike.R;
import com.bsb.hike.adapters.HikeBlockedUserAdapter;
import com.bsb.hike.tasks.ActivityCallableTask;

public class HikePreferences extends PreferenceActivity implements OnPreferenceClickListener
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
		setContentView(R.layout.hikepreferences);

		Intent intent = getIntent();
		int preferences = intent.getIntExtra(HikeConstants.Extras.PREF, -1);

		addPreferencesFromResource(preferences);

		TextView titleView = (TextView) findViewById(R.id.title);
		titleView.setText(getTitle());

		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof ActivityCallableTask)
		{
			setBlockingTask((ActivityCallableTask) retained);
			mTask.setActivity(this);
		}

		Preference preference = getPreferenceScreen().findPreference("block");
		if (preference != null)
		{
			preference.setOnPreferenceClickListener(this);
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

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		Intent intent = new Intent(this, HikeListActivity.class);
		intent.putExtra(HikeConstants.ADAPTER_NAME, HikeBlockedUserAdapter.class.getName());
		startActivity(intent);
		return true;
	}
	
	/**
	 * For redirecting back to the Welcome Screen.
	 */
	public void accountDeleted()
	{
		dismissProgressDialog();
		/*
		 * First we send the user to the Main Activity(MessagesList) from there we redirect him to the welcome screen.
		 */
		Intent dltIntent = new Intent(this, MessagesList.class);
		dltIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(dltIntent);
	}
}
