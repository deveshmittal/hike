package com.bsb.hike.tasks;

import android.app.Activity;

public interface ActivityCallableTask
{
	public void setActivity(Activity activity);

	public boolean isFinished();
}
