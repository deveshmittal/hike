package com.bsb.hike.ui;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.bsb.hike.tasks.DeleteAccountTask;

public class DeleteDialogPreference extends DialogPreference
{

	private Context context;
	public DeleteDialogPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.context = context;
	}

	@Override
	protected void onDialogClosed(boolean shouldDelete)
	{
		if (shouldDelete)
		{
			HikePreferences preferences = (HikePreferences) context;
			DeleteAccountTask task = new DeleteAccountTask(preferences);
			preferences.setBlockingTask(task);
			task.execute();
		}
	}
}
