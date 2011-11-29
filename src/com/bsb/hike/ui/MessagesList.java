package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.bsb.hike.R;

public class MessagesList extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

	    //TODO check to see if we've configured an account
	    if (true) {
	    	startActivity(new Intent(this, WelcomeActivity.class));
	    	finish();
	    }
	}
}

