package com.bsb.hike.ui;

import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import com.bsb.hike.tasks.DeleteAccountTask;

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
		

		Preference blockPreference = getPreferenceScreen().findPreference(getString(R.string.block_key));
		if (blockPreference != null)
		{
			blockPreference.setOnPreferenceClickListener(this);
		}
		Preference deletePreference = getPreferenceScreen().findPreference(getString(R.string.delete_key));
		if(deletePreference != null)
		{
			deletePreference.setOnPreferenceClickListener(this);
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
		Log.d("HikePreferences", "Preference clicked: "+preference.getKey());
		if (preference.getKey().equals(getString(R.string.block_key))) 
		{
			Intent intent = new Intent(this, HikeListActivity.class);
			intent.putExtra(HikeConstants.ADAPTER_NAME,
					HikeBlockedUserAdapter.class.getName());
			startActivity(intent);
		}
		else if(preference.getKey().equals(getString(R.string.delete_key)))
		{
			Builder builder = new Builder(HikePreferences.this);
			builder.setMessage("Are you sure you want to delete your account?");
			builder.setPositiveButton("Yes", new OnClickListener() 
			{
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					DeleteAccountTask task = new DeleteAccountTask(HikePreferences.this);
					setBlockingTask(task);
					task.execute();
				}
			});
			builder.setNegativeButton("No", new OnClickListener() 
			{
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		}
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
