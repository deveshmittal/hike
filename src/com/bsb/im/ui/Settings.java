package com.bsb.im.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.bsb.im.R;

/**
 * This class represents an activity which allows the user to change his account or proxy parameters.
 */
public class Settings extends PreferenceActivity {

    private static final Intent SERVICE_INTENT = new Intent();

    static {
	SERVICE_INTENT.setComponent(new ComponentName("com.bsb.im", "com.bsb.im.BeemService"));
    }

    /**
     * Constructor.
     */
    public Settings() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.layout.preferences);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater mInflater = getMenuInflater();
	mInflater.inflate(R.menu.edit_settings, menu);
	return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	Intent i = null;
	switch (item.getItemId()) {
	    case R.id.settings_menu_create_account:
		i = new Intent(this, CreateAccount.class);
		startActivity(i);
		return true;
	    case R.id.settings_menu_privacy_lists:
		i = new Intent(this, PrivacyList.class);
		startActivity(i);
		return true;
	    default:
		return false;
	}
    }
}
