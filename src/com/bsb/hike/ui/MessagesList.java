package com.bsb.hike.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.R;

public class MessagesList extends Activity implements OnClickListener {
	/** Called when the activity is first created. */
	private boolean mConversationsViewHidden;
	private View mNewMessageView;
	private View mConversationsView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

		SharedPreferences settings = getSharedPreferences(HikeMessengerApp.ACCOUNT_SETTINGS, 0);
		String token = settings.getString(HikeMessengerApp.TOKEN_SETTING, null);
	    if (token == null) {
	    	startActivity(new Intent(this, WelcomeActivity.class));
	    	finish();
	    }

    	mNewMessageView = findViewById(R.id.new_user_message);
    	mConversationsView = findViewById(R.id.conversations);

    	View searchIconView = findViewById(R.id.search);
    	searchIconView.setOnClickListener(this);
    	View editMessageIconView = findViewById(R.id.edit_message);
    	editMessageIconView.setOnClickListener(this);
	    Intent intent = getIntent();
	    if (intent.getBooleanExtra("first", false)) {
	    	mNewMessageView.setVisibility(View.VISIBLE);
	    	mConversationsView.setVisibility(View.GONE);
	    	mConversationsViewHidden = true;
	    }
	}

	private void checkForWelcomeMessage() {
		if (mConversationsViewHidden) {
			mConversationsViewHidden = false;
	    	mNewMessageView.setVisibility(View.GONE);
	    	mConversationsView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onClick(View v) {
		Log.d("MessagesList", "OnClick called: "+v);		
	}
}

