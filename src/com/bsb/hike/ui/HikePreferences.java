package com.bsb.hike.ui;

import com.bsb.hike.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class HikePreferences extends PreferenceActivity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
